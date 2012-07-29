package edu.uncc.grid.pgaf.p2p.compute;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.id.ID;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.DiscoveryResponseMsg;
import edu.uncc.grid.pgaf.advertisement.SpawnPatternAdvertisement;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.advanced.UnorderedTemplate;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
/**
 * 
 * The Worker class manages multiple threads, which all are involved in a pattern.
 * 
 * @author jfvillal
 *
 */
public class DesktopWorker extends Worker implements DiscoveryListener{
	/**
	 * The user module for this node.
	 */
	String UserModule;  
	UserThread ModThread[];
	/**
	 * Used to store the threads for the sink and the source.  I made it a MAP because we 
	 * don't want to have multiple sinks for the same pattern ID, but at the same time
	 * we want to allow for multiple sinks from different patterns to be created because the
	 * sinks and sources could be a source for deadlock.
	 * 
	 * TODO when the source and sink are separated, two maps will be needed
	 */
	Map<ID, UserThread> SinkSourceThread;
	Map<ID, Boolean> ActivePattern;
	public Map<PipeID, BasicLayerInterface> SourceSinkAvailableOnThisNode;
	//Map<ID, UserThread> SourceThread;
	//Map<ID, UserThread> SinkThread;
	/**
	 * Initializes the Worker design to work on a single multicore computer.
	 * @throws IOException
	 */
	public DesktopWorker( ) throws IOException{
		Node.CpuCount = 8;
		ModThread = new UserThread[Node.CpuCount];
		SinkSourceThread = new HashMap<ID, UserThread>();
		HaveIWorkedOnThisPatternAlready = Collections.synchronizedMap(new HashMap<ID,Boolean>());
		SourceSinkAvailableOnThisNode = new HashMap<PipeID, BasicLayerInterface>();
	}
	public void joinWorker() throws InterruptedException{
		synchronized( ModThread ){
			for(int i = 0; i < ModThread.length; i++ ){
				if( ModThread[i] != null){
					ModThread[i].join();
				}
			}
			Iterator<UserThread> it = SinkSourceThread.values().iterator();
			while(it.hasNext()){
				UserThread ut = it.next();
				ut.join();
			}
		}
	}
	
	public boolean isIdling(){
		boolean idle = true;
		synchronized(ModThread ){
			Iterator<UserThread> it= SinkSourceThread.values().iterator();
			
			while(it.hasNext()){
				UserThread i = it.next();
				if( i.isAlive() ){
					idle = false;
					//Node.getLog().log(Level.FINEST, "----------------------------not idling a SourceSink is still on" );
					return idle;
				}
			}
			for(int i = 0; i < ModThread.length; i++){
				if( ModThread[i] != null){
					if( ModThread[i].isAlive() ){
						idle = false;
						//Node.getLog().log(Level.FINEST, "----------------------------not idling a thread is on " + i + " is still on " );
						return idle;
					}
				}
			}
		}
		return idle;
	}
	
	/**
	 * Checks Advertisements to scan for Pattern Advertisements, and 
	 * join a pattern if this node does not have a pattern already.
	 * 
	 * Returns true if the pattern has already being run, and should be flushed.
	 * 
	 * @throws PatternRepetitionException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 * @throws UnknownHostException 
	 */
	public Thread checkAdvertisement( Advertisement TheAdv ) throws IllegalArgumentException, SecurityException
															, ClassNotFoundException, InstantiationException, IllegalAccessException
															, InvocationTargetException, NoSuchMethodException, PatternRepetitionException
															, UnknownHostException{
		/**This thread holds the source/sink for the application**/
		UserThread sink_source = null;
			if( TheAdv instanceof SpawnPatternAdvertisement ){
				SpawnPatternAdvertisement advert = (SpawnPatternAdvertisement) TheAdv;
	         	//Node.getLog().log(Level.FINE, "\n\n Got Pattern with ID: " + advert.getID().toString() +"\n\n");
	         	/**
	         	 * If there is a CPU with dataflow state idle, join the pattern.
	         	 */
	         	if( advert.getSinkAnchor() != null){
		         	if( advert.getSinkAnchor().toLowerCase().compareTo(Node.getGridName().toLowerCase()) == 0){
		         		//create a sink 
		         		//TODO separate source and sink
		         	}
	         	}
	         	/*
	         	 * if this node is in the list of the anchors for a source, a sink, or to both.
	         	 * 
	         	 */
	         	if( advert.getSourceAnchor() != null){
	         		InetAddress addr = InetAddress.getLocalHost();
	         		
		         	if( advert.getSourceAnchor().toLowerCase().compareTo(addr.getHostName().toLowerCase()) == 0){
		         		//create a source 
		         		//for now this means create source and sink, since the 
		         		//Template interface has not being updated.
		         		/*
		         		 * An advertisement can be caught multiple times even if it is flushed.  This algorithm makes
		         		 * sure that a sink is created the first time the advertisement is caught, but does not keep
		         		 * creating more sources and sinks for the same pattern.
		         		 */
		         		synchronized( SinkSourceThread ){
			         		if( !SinkSourceThread.containsKey(advert.getID()) ){
			         			//TODO check the nesting system to make sure it is dealing with Unordered and Ordered templates
				         		UnorderedTemplate T = getAdvancedTemplateInstance( advert.getPatternClassName()
				         				,(BasicLayerInterface) SourceSinkAvailableOnThisNode.get(advert.getID())
				         				, advert.getArguments(), advert.getPatternID(), true);
				         		
				         		/**
				         		 * If the module is 'in house' then we should use the same Object (instantiation of the class)
				         		 * that was provided by the user.  This allows the user to communicate with the module from
				         		 * the pattern that nested this module.  This should work most of the time, since I expect
				         		 * the source and sink to be on the same machine that started the new pattern.  But if the
				         		 * user sets the source or sink on another computer using an achor, then this will not 
				         		 * take effect.  But the user already knows that, since the source or sink would be on 
				         		 * another server.  
				         		 */
				         		if( SourceSinkAvailableOnThisNode.get(advert.getID()) == null){
				         			/**
				         			 * If the SourceSink is not available, then we can add it so that the user programmer
				         			 * can get it to interact with the global variable.
				         			 * 
				         			 * I'll give more detail on this piece of code:
				         			 * The user provides a instantiated module to the framework, but in most nodes (remote) 
				         			 * we will only use the class name to create new modules.  Except on the local node, 
				         			 * if this is the local node, that means that we can just use the users instantiated
				         			 * modules instead of creating a new one.  The main benefit, is that the user can
				         			 * interact with his instantiated module in a more intuitive fashion.  In effect, the
				         			 * user would interact as if the framework as a thread sharing some variable, and
				         			 * most of the skeletons/patterns running remotely would be behind the abstraction 
				         			 * layer provided by the framework.
				         			 */
				         			SourceSinkAvailableOnThisNode.put( advert.getPatternID(), T.getUserModule());
				         			
				         		}
				         		
				         		sink_source = new UserThread( this, T, Types.DataFlowRole.SINK_SOURCE, advert.getPatternID());
				         		sink_source.setName("Seeds SinkSource Thread");
				         		sink_source.setDaemon(true);
				         		sink_source.start();
				         		/*
				         		 * the reason we add the sink and source thread to a list, and not to the array for the cores
				         		 * is because I don't want to reject anchor request, because it can delay the job. the compute
				         		 * nodes are considered less important.  The algorithm should be able to scale to n number of 
				         		 * compute nodes.  But we always need a source and a sink. Particularly because the source and
				         		 * sink are more likely to be specified in an Anchor.
				         		 */
				         		this.SinkSourceThread.put(advert.getID(), sink_source);
				         		//Node.getLog().log(Level.FINER, "Created source sink for id " + advert.getID().toString() );
			         		}else{
			         			//Node.getLog().log(Level.FINER, "Already have a sink for pattern " + advert.getID().toString() );
			         		}
		         		}
		         	}else{
		         		Node.getLog().log(Level.FINER, "Source anchor is different than my grid name  " + advert.getSourceAnchor() 
		         				+ " and mine is " + addr.getHostName() );
		         	}
	         	}
	         	/*
	         	 * go through the available cores, but if there are sinks or sources in this node, limit the number 
	         	 * of cores to use for the compute operations.
	         	 * */
	         	synchronized(ModThread){
		         	for( int core_num = 0; core_num < Node.CpuCount - SinkSourceThread.size() ; core_num++){
		         		
		         			/*
		         			 * if the thread is not alive, or it is null, we can use it to allocate a new 
		         			 * module.  Else, try the next core.
		         			 * 
		         			 * 
		         			 * Also, if the thread is inactive, the patter_id is assumed to be done, and 
		         			 * future advertisements for the old pattern_id will be ignore and the 
		         			 * framework will be adviced to flushe (delete) the advertisement.
		         			 * 
		         			 */
		         		
				         	if( this.ModThread[core_num] == null){
				         		//give a new module to this thread
				         		
				         		Boolean is_this_a_pattern_we_already_worked_on = this.HaveIWorkedOnThisPatternAlready.get( advert.getID() );
				        		if( is_this_a_pattern_we_already_worked_on == null){
				        			//no
				        			this.HaveIWorkedOnThisPatternAlready.put(advert.getID(), false);
					         		createNewThread( core_num, advert);
					         		Node.getLog().log(Level.FINE, " This thread is null ");
				         		}else{
				         			if( is_this_a_pattern_we_already_worked_on ){
				         				Node.getLog().log(Level.FINE, " Reject pattern that has beeing worked on ");
				         			}else{
						         		createNewThread( core_num, advert);
						         		Node.getLog().log(Level.FINE, " This thread is null ");
				         			}
				         		}
				         	}else if( this.ModThread[core_num].isProcessDone() ){
				         		//set pattern as done
				         		boolean is_pattern_done = this.ModThread[core_num].isPatternIsDone();
				         		this.HaveIWorkedOnThisPatternAlready.put( this.ModThread[core_num].PatternID, is_pattern_done );
				         		//give a new module to this thread
				         		Boolean is_this_a_pattern_we_already_worked_on = this.HaveIWorkedOnThisPatternAlready.get( advert.getID() );
				        		if( is_this_a_pattern_we_already_worked_on == null){
				        			//no
				        			this.HaveIWorkedOnThisPatternAlready.put(advert.getID(), false);
					         		createNewThread( core_num, advert);
					         		Node.getLog().log(Level.FINE, " this thread was not active ");
				         		}else{
				         			if( is_this_a_pattern_we_already_worked_on ){
				         				Node.getLog().log(Level.FINE, " Reject pattern that has beeing worked on ");
				         			}else{
				         				this.HaveIWorkedOnThisPatternAlready.put(advert.getID(), false);
						         		createNewThread( core_num, advert);
						         		//Node.getLog().log(Level.FINE, " This thread is null ");
				         			}
				         		}
				         	}/*else, this thread is working on something.*/else{
				         	//	Node.getLog().log(Level.FINE, " thread " + core_num + " is active ");
				         	}
		         	}
	         	}
	        }
		return sink_source;
	}
	public void createNewThread(int core_num, SpawnPatternAdvertisement adv) throws 
														  IllegalArgumentException, SecurityException
														, ClassNotFoundException, InstantiationException
														, IllegalAccessException, InvocationTargetException
														, NoSuchMethodException, PatternRepetitionException{
		UnorderedTemplate T = getAdvancedTemplateInstance( adv.getPatternClassName()
															, null, adv.getArguments()
															, adv.getPatternID(), false);
																		//don't use local mod, these are worker mods.
		ModThread[core_num] = new UserThread(this, T, Types.DataFlowRole.COMPUTE, adv.getPatternID());
		ModThread[core_num].setName("Seeds Worker process: " + core_num);
		ModThread[core_num].start();
		//Node.getLog().log(Level.INFO, "Created a COMPUTE node for id: " + adv.getID().toString() );
		
	}
	/**
	 * Creates an instance of the User module and the Advance Template modules and returns it.
	 * 
	 * It will also load the loader patter if it is in the chain.
	 * 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws PatternRepetitionException  thown when we try to work on a pattern that is done, but the adverts continue to 
	 * 								float around in the net.
	 * @throws IllegalAccessException 
	 */
	public UnorderedTemplate getAdvancedTemplateInstance( String user_mod_name, BasicLayerInterface local_mod
															, String[] init_args, PipeID pattern_id
															, boolean use_local_mod //will set local mod instead of creating new one
														) throws 
														  ClassNotFoundException, InstantiationException
														, IllegalArgumentException
														, InvocationTargetException, SecurityException
														, NoSuchMethodException, PatternRepetitionException, IllegalAccessException{
		/* Jan 6, 2010 | updated Jan 20 2010
		 * This part gets a little complicated because of the deployer idea.  Basically, because the patterns can 
		 * be added or multiplied, we need a simplified pattern, one that does not include the deployment section.
		 * We will call the deployment of a pattern the loading pattern so that the wording does not conflict with 
		 * the deployer for the framework.
		 * 
		 * 
		 * OK, so the algorithm here is like this:  
		 * 		1.  Get the Template from the BasicLayerInterface class.
		 * 				if the template is Unordered, just run the template like it was done before
		 * 			else, get the loader module, and then the loader template and run those.
		 * 				once they are instantiated, give then the ordered template to run.
		 * 
		 * beow is a diagram of the interaction
		 * 
		 * BasicLayerInterface -> UnorderedTemplate -> Framework
		 * 		The framework load the basic layer interface into the unordered temaplate and runs the unordered template
		 * 
		 * BasicLayerInterface -> OrderedTemplate -> loaderpatter -> LoaderTemplate -> Framework
		 * 		in this case the basic layer interace provides the ordered template, the ordered template comes with
		 * 		a function that points to its loader pattern class.  the loader pattern request the use of the
		 *      loader template which is run by the framework.
		 *      Note that the loader pattern is a BasicUserLayer implementation, and the LoaderTemplate is 
		 *      an implementation of the UnorderedTemplate interface.
		 *      
		 *      The algorithms that use the OrderedTemplate are those that benefit from an MPI-Like communication scheme, like
		 *      a stencil, a pipeline, or other communication patterns.  The mpi-like communication at the moment requres a set
		 *      number of processors.  The unordered template can handle the nodes with more flexibility, so it can deal with
		 *      a dynamic number of processes, even during runtime.
		 *      
		 *      Example:
		 *      Suppose we want to create an ordered template for a pipeline.  you can look at the source code for the pipeline.  
		 *      It has being updated to this scheme.  But for this example, suppose you are going to implement it using the 
		 *      framework's interfaces.  you would need three classes.
		 *      PipeLine extends BasicUserLayerInterface (this is the simplified interface for a user programmer that wants 
		 *      											the easier possible way to interact with the Grid)
		 *      PipeLineTemplate extends OrderedTemplate (this run the PipeLine interface by creating the connection that
		 *      											make the pipeline pattern work)
		 *      PipeLineLoader extends PatternLoader (this is the loader for the pipeline, this is used to load preliminary information
		 *      										into the pipeline stages)
		 *      
		 *      That is it.  you make sure that the classes return the class that is needed to load them, and then you have a patterns.
		 *      
		 *      
		 * 
		 * */
		Node node = null;//placeholder for null Node.  The Node references in the affected classes should be replaced by
			// a more generic object.  The main purpose of allowing these classes to access the Node class was to give then
			// access to advance areas of the framework even if their point of entry is from the BasicLayerInterfaces.
		/**
		 * this next piece of code is to make sure we don't run the same pattern multiple times.
		 */
		UnorderedTemplate root_template = null;
		try{
		
			Class[] node_arg = {Node.class};
			
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			
			//the parent object is supposed to set the module name.
			Node.getLog().log(Level.FINER, "User module: " + user_mod_name );
			BasicLayerInterface user_mod = null;
			
			if( use_local_mod ){
				if( local_mod == null){
					Class user_mod_class = loader.loadClass(user_mod_name);
					user_mod = (BasicLayerInterface) user_mod_class.newInstance();
					user_mod.initializeModule(init_args);
				}else{
					user_mod = local_mod;
				}
			}else{
				Class user_mod_class = loader.loadClass(user_mod_name);
				user_mod = (BasicLayerInterface) user_mod_class.newInstance();
				user_mod.initializeModule(init_args);
			}
			
			user_mod.setFramework( node );

			Node.getLog().log(Level.FINER, "Template module: " + user_mod.getHostingTemplate() );
			
			Class adv_template_class = loader.loadClass( user_mod.getHostingTemplate() );
			
			if( checkTemplateType( adv_template_class, OrderedTemplate.class)){
				//if ordered template, then we should create a loader to deploy the pattern
				Constructor cons = adv_template_class.getConstructor( node_arg );
				Object  advanced_template =  cons.newInstance( node );	
				OrderedTemplate o_template = (OrderedTemplate) advanced_template;
				o_template.setUserModule(user_mod);
				/*get loader module*/
				PatternLoader loader_mod = (PatternLoader) o_template.getLoaderModule().newInstance();
				loader_mod.setOTemplate( o_template); //give o_template to loader module
				loader_mod.setPatternID(pattern_id); //give pattern id to loader module
				loader_mod.setFramework( node );
				/*create new instance of the loader template*/			
				Class o_template_class = loader.loadClass( loader_mod.getHostingTemplate() );
				Constructor o_template_cons = o_template_class.getConstructor( node_arg );
				UnorderedTemplate loader_template = (UnorderedTemplate) o_template_cons.newInstance( node );
				/*give the specifi loader to the unorder module*/
				loader_template.setUserModule(loader_mod);
				/*return the new loader template*/
				root_template = loader_template;
				
			}else if( checkTemplateType( adv_template_class, Dataflow.class) ){
				/**
				 * These three lines create a dataflow loader template.  it will create 
				 * the perceptrons and send them to each computer node.
				 */
				Object  advanced_template =  adv_template_class.newInstance();
				Dataflow d_template = ( Dataflow ) advanced_template;
				d_template.setUserModule(user_mod);
				DataflowLoader dataflow_loader = d_template.getDataflowLoaderInstance();
				dataflow_loader.setFramework( node );
				dataflow_loader.setUserMod(user_mod);//sets the main module to be able to tell the user the program is done.
				root_template = dataflow_loader.getDataflowLoaderTemplateInstance();
				root_template.setUserModule(dataflow_loader);
			}
			else{
				//if unordered, we can run directly
				Constructor cons = adv_template_class.getConstructor( node_arg );
				Object  advanced_template =  cons.newInstance( node );	
				UnorderedTemplate u_template =  (UnorderedTemplate) advanced_template;
				u_template.setUserModule(user_mod);
				root_template =  u_template; 
			}
		}catch ( IllegalAccessException e){
			Node.getLog().log(Level.SEVERE, " This is most likely because the user progammer forgot to add an empty argument constructor to the "
					+ "class that implements the skeleton/pattern" );
			throw e;
		}
		return root_template;
	}
	public boolean checkTemplateType( Class object, Class query_class){
		Class interfaces = object.getSuperclass();
		boolean ans = false;
		if( object.getName().compareTo(query_class.getName()) == 0){
			ans = true;
		}else if( interfaces != null){
			ans = checkTemplateType( interfaces, query_class);	
		}
		return ans;
	}
	
	
	@Override
	public void discoveryEvent(DiscoveryEvent event) {
		DiscoveryResponseMsg TheDiscoveryResponseMsg = event.getResponse();
        
        if (TheDiscoveryResponseMsg!=null) {
            
            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
            //DiscoveryService discovery_service = ParentNode.getNetPeerGroup().getDiscoveryService();
            while (TheEnumeration.hasMoreElements()) {
            	Advertisement TheAdv = TheEnumeration.nextElement();
            	try {
					this.checkAdvertisement( TheAdv );
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (PatternRepetitionException e) {
					e.printStackTrace();
				}
            }
        }
		
	}
	
}
