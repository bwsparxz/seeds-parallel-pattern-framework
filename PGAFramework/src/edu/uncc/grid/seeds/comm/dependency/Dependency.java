package edu.uncc.grid.seeds.comm.dependency;

import java.io.IOException;
import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DependencyAdvertisement;
import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.ConnectionChangeListener;
import edu.uncc.grid.pgaf.communication.ConnectionChangedMessage;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeClient;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.shared.ConnectionHibernatedException;
import edu.uncc.grid.pgaf.p2p.Node;

/**
 * The connection is established based on an identification system that is supported by this class.  the node requesting the 
 * connection has a dependency with the ID of the decency it needs to connect to. Once the source and sink for that depency are
 * found, the handshake is done and the transfer of information can start.
 * 
 * This class is hierarchical, this allows the source or the sink of the connection to further split the connection.  the 
 * connection can also converge to its original form, but not beyond the original form.  The preliminary convention to 
 * split the depencies is by a dot-spared string.
 * 
 * For example the depency are first enumerated
 * 1, 2 and 3.
 * 
 * As the need to split the appearch, the dependencies can then further split using dots.
 * 1.1, 1.2, 1.3 
 * 1.1, 2.2, 2.3 ... 
 *
 * June 30:  This class will manage a stream.  The only data will be pass on along with the data 
 * is the clock number.
 * 
 * 
 * 
 * 
 * @author jfvillal
 *
 */
public class Dependency implements ConnectionChangeListener{
	private static final long serialVersionUID = 1L;
	/*Notes:
	 * 
	 * This class will grow in complexity as I add the hierarchical dependency attributes.  for now, it looks like
	 * a wrapper class for the ConnectionManger classes.
	 * 
	 * 
	 */
	/**
	 * 
	 * Used to uniquely identify this dependency.  Even the HierarchicalID will conflict for different patterns.
	 * This ID makes it very hard for the dependencies to conflict.
	 * 
	 */
	PipeID UniqueId; //unique id
	/**
	 * 
	 * dot-separated number used to describe the hierarchy of this dependency
	 * 
	 */
	HierarchicalDependencyID LevelID; //dot-separated hierarchical id.
	/**
	 * The hierarchical dependency id of the sink for this dependency's output.
	 * This is necesary to determine wheter to split the data packet or not.
	 * 
	 * This is acomplished by comparing the HierarchicalID of a leaf dependency
	 * to the HidSink 
	 * the HidSink is used by the source node (the server node).  it stores the id
	 * of the dependency that should be serviced by this dependency tree.
	 */
	HierarchicalDependencyID HidSink;
	/**
	 * Does a similar job as the HidSink, but it is a separate variable just
	 * to be able to better track bugs.
	 * 
	 * HidSink is used by the sink node (the client node). it stores the id
	 * that should be service by that dependency tree.
	 */
	HierarchicalDependencyID HidSource;
	/**
	 * 
	 * For synchronous programs, the cycle_version is used to match the iterations
	 * across the network for a particular pattern.
	 * 
	 */
	private long CycleVersion; //use to match the data payload with the appropiate cycle function
	/**
	 * 
	 * if Hibernation is eminent, this variable sets when the stop will happen.
	 * 
	 */
	private long CycleVersionStop;
	/**
	 *
	 * Data payload is used for the leaf dependencies.  
	 *
	 */
	private ConnectionManager Stream; 
	/**
	 *
	 * The children dependencies make this object automatically a "trunk" dependency.
	 *
	 */
	private Dependency[] Children;
	/**
	 * The pointer to the parent is necessary to manage connection hibernation.  The
	 * pointer points to the parent of this dependency.
	 */
	private Dependency Parent;
	private DependencyEngine Engine;
	
	
	/**
	 * The hibernation uses an echo to shutdown the line on both sides of the 
	 * connection.  This boolean prevents the echo from going on until it
	 * creates a Network exception.
	 */
	public boolean HibernateEmitter = false;
	
	/**
	 * This boolean will let the manager thread and the user thread communicate about
	 * the status of this dependency and the child dependencies.
	 */
	private boolean Hibernated;
	/**
	 * Hibernation Eminnent will be set when a packet that is organizing a hibernation
	 * is going around the dependency tree.  The version at which point the dependency
	 * tree will be hibernated is set in CycleVesionStop.
	 * Hibernated will be set once CycleVersion reaches CycleVersionStop.
	 */
	private boolean HibernationEminnent = false;
	
	/**
	 * 
	 * Determines if this is a source data dependency or a sink data dependency.
	 * 
	 */
	private boolean Source; //says if this object is a source of data (true) or a sink (false)
	/**
	 * 
	 * This is used to give extra information to the advanced user or the user programmer
	 * about level of partitioning at which the data is at this moment.  If we split the 
	 * first time, the level is 1 if we split the next time the level will be 2 and so 
	 * forth.  This give extra control to the advance user to determine if at some level
	 * the data would not be succesfully split any more.  i.e. when the data is already
	 * at an atomic level.
	 * 
	 * TODO doublecheck this variable is being maintained on send and recv methods.
	 */
	private int DependencySplitLevel;
	
	/**
	 * The advertisement used to create the connection is kept in this class so that it can
	 * be flushed from the jxta cache when the dependency is closed.  This is done to 
	 * reduce the probability of having an old dependency advertisement being used to create
	 * a new connection.
	 */
	DependencyAdvertisement DeptAdvertisement = null;
	/**
	 * Used to split and coales data packets
	 */
	SplitCoalesceHandler SplitCoalesceMod;
	//TODO check thid method
	
	/**
	 * Client Side
	 * @throws DirtyAdvertisementException 
	 */
	public Dependency(Hida connection_info, DependencyEngine engine
						, HierarchicalDependencyID hid_source, long cycle_version
						, SplitCoalesceHandler handler
																	) throws IOException, ClassNotFoundException
																	, InterruptedException, TunnelNotAvailableException
																	, CommunicationLinkTimeoutException, AdvertsMissingException
																	{ 
		this( null, connection_info,  engine, hid_source, cycle_version, handler);
	}
	
	
	
	/**
	 * CLIENT SIDE
	 * This construction is intended for a client side connection.  It will create a dependency tree
	 * necesary for the connection to be considered stable and connected. 
	 * 
	 * @throws AdvertsMissingException 
	 * @param engine  an instance of the dependency engine.
	 * @param connection_info a Hida object that has all the advertisements necesary to connect to this 
	 * 		dependency.  If an advert is missing, a {@link AdvertsMissingException} is thrown.
	 * @param hid_source stores the actual dependency to which the advanced user wants to connect.
	 */
	private Dependency( Dependency parent, Hida connection_info, DependencyEngine engine
						, HierarchicalDependencyID hid_source , long cycle_version
						, SplitCoalesceHandler handler
																	) throws IOException, ClassNotFoundException
																	, InterruptedException, TunnelNotAvailableException
																	, CommunicationLinkTimeoutException, AdvertsMissingException
																	{
		//if this is top hida, just create a connetion children null
		
		//TODO what do I do if the hida is missing a dependency advertisement.
		Engine = engine;
		Parent = parent;
		Source = false;
		CycleVersion = cycle_version;
		CycleVersionStop = -1;
	
		SplitCoalesceMod = handler;
		this.HibernationEminnent = false;
		this.Hibernated = false;
		this.HibernateEmitter = false;
		if( !connection_info.allAdvertsPresent(hid_source, cycle_version)){
			throw new AdvertsMissingException();
		}
		if( connection_info.getChildren() == null){ //top hida
			DeptAdvertisement = connection_info.Advert;
			Stream = engine.getConnection(DeptAdvertisement, hid_source);
			Stream.setDataflowSource(false);
			Stream.setConnectionChangeListener(this);
			LevelID = DeptAdvertisement.getHyerarchyID();
			HidSource = hid_source;
			UniqueId = DeptAdvertisement.getDependencyID();
			Children = null;
		}else{//if this is not top hida create the children dependencies
			LevelID = connection_info.LevelId;
			Hida[] hida_children = connection_info.getChildren();
			Children = new Dependency[hida_children.length];
			HidSource = hid_source;
			Stream = null;
			for( int i = 0; i < hida_children.length; i++){
				Children[i] = new Dependency( this, hida_children[i] , engine, hid_source, cycle_version, handler);
			}
		}
		
		//if this plugs into a big depende of which I only need a little bit
		//we do the same as top hida for now to keep things simpler
	
	}
	
	/**
	 * SERVER SIDE
	 * This constructor is intended for serve side connection.
	 * 
	 * this is a front-end for the recursive version, which will create a dependency tree.  The tree
	 * needs to be filled in with all the necessary connections before it can be used for communication.
	 * @param con
	 * @param id
	 * @param source
	 * @param unique_id
	 */
	public Dependency(ConnectionManager con, HierarchicalDependencyID id
			, HierarchicalDependencyID tree
			, PipeID unique_id, DependencyEngine engine, long cycle_version 
			, SplitCoalesceHandler handler, DependencyAdvertisement advert){
		this( null, con, id, tree, unique_id, 0, engine, cycle_version, handler, advert );
	}
	/**
	 * SERVER SIDE
	 * This constructor is inteded for a server side connection.
	 * 
	 * This is the recursive version, and is only used internally.
	 * 
	 * @param con
	 * @param server_id this is the server id.  the dept supplied by the object using this method.
	 * @param tree this is the dept id with highest level between the server_id and the client_id
	 * @param source
	 * @param unique_id
	 */
	private Dependency(Dependency parent, ConnectionManager con, HierarchicalDependencyID server_id
			, HierarchicalDependencyID tree
			, PipeID unique_id, int level, DependencyEngine engine, long cycle_version
			, SplitCoalesceHandler handler
			, DependencyAdvertisement advert
			){
		LevelID  = tree.getLevel(level);
		HidSink = server_id;
		Engine = engine;
		Parent = parent;
		CycleVersion = cycle_version;
		CycleVersionStop = -1;
		this.DeptAdvertisement = advert;
		
		SplitCoalesceMod = handler;
		
		this.HibernationEminnent = false;
		this.Hibernated = false;
		this.HibernateEmitter = false;
		if( LevelID.getLevel() < tree.getLevel()){
			//this is the parent create child
			HierarchicalDependencyID next_child = tree.getLevel(LevelID.getLevel() + 1);
			Children = new Dependency[next_child.getTotal()];
			Children[next_child.getId()] = new Dependency( this, con, server_id, tree
										, unique_id, level + 1, engine, cycle_version, handler, advert);
			Stream = null;
			UniqueId = null;
			Source = true;
		}else{
			Stream = con;
			Stream.setConnectionChangeListener(this);
			Stream.setDataflowSource(true);
			Children = null;
			Source = true;//source;
			UniqueId = unique_id;
		}
	}
	
	//Queue<DependencyPacket>[] HibernationPreparationQueue; 
	/**
	 * 
	 * Gets the object from the stream.
	 * This method can manage a tree of dependencies, which allows for automatic
	 * scalability.
	 * 
	 * Code checked for consistency on Agust 26 for the version stop organization algorithm
	 * 
	 * @return
	 * @throws InterruptedException
	 * @throws TimeoutException 
	 * @throws CycleVersionMissmatch 
	 * @throws IOException 
	 * @throws EngineClosedException 
	 */
	public  Serializable takeRecvObj() throws InterruptedException, TimeoutException
															, CycleVersionMissmatch, IOException
															, EngineClosedException{
		Serializable packet = null;
		/*
		 * Children would equal null if the dependency id is the same.
		 * Also, it will be null if the depencency id being serviced by this 
		 * object is of higher level than the dependency we are connected to
		 * Example:  if we are servicing 0/1.0/2.  But the dependency on the network
		 * is 0/1.  Then Children = null.  The spliting of the data to make this 
		 * work is done on the server side of this connection.
		 */
		if( !Hibernated ){
			if( Children == null){
				//if the queue is empty
				Serializable ans = Stream.takeRecvingObject();
				//if( ans instanceof ConnectionChangedMessage){
					//System.out.println(Thread.currentThread().getName() + " Dependency.takeRecvObj() : 
					//  got ConnectionChangedMessage. v:" +CycleVersion + " SV: " + this.getRootCycleVersionStop());
					//packet = takeRecvObj();//keep going with the next packet.  onHibernateConnection() should
					//have taken care of that packet by now, and we should be schedule to stop at some
					//version in the near future.
				//}else{
					//Serializable object = (Serializable) ans;
					/*if( this.getCycleVersion() < object.getVersion()){
						throw new CycleVersionMissmatch( this.getCycleVersion(), object.getVersion());
					}else if( this.getCycleVersion() > object.getVersion() ){
						//september 27
						throw new CycleVersionMissmatch( this.getCycleVersion(), object.getVersion());
						//Thread.sleep(100);
						//packet = takeRecvObj();  //TODO this could deadlock or timeout. 
					}else{*/
						packet =  ans;
						++CycleVersion;
					//}
				//}
			}else{
				
				/*
				 * This if statement basically says that any LevelID that is less than 
				 * what is being serviced should not worry about their children.
				 * Example:  say we have a tree 0/1.0/4.0/3.02
				 *               ->
				 *     ->    ->--->
				 * --> -> ---->
				 * 	   ->    ->
				 *     ->
				 *  In this example the level if Level ID is 0 for 0/1, 1 for 0/1.0/2 and so forth
				 *  
				 *  We will get the object from the children dependencies only if LevelID.compare(HidSource) <= 1
				 *  
				 *  Based on compareTo for HyerarchicalDependencyID.  If not, those children can be 
				 *  null since we don't need them.
				 *  
				 *  This is done this way because all the dependencies start at the root dependency.
				 *  
				 *  so if we need 0/1.1/4.0/3 we only need dependencies
				 *  
				 *  0/1.1/4.0/3.0/2 and 
				 *  0/1.1/4.0/3.1/2
				 * 
				 */
				if( LevelID.hierarchicalCompareTo( HidSource) > 1 ){//keep climbing the tree until we really need the input from children.
					//children don't matter
					HierarchicalDependencyID next_child = HidSource.getLevel( LevelID.getLevel() + 1);
					if(Children[next_child.getId()] == null){
						Node.getLog().log(Level.WARNING, " Children[" + next_child.getId() +"] is null, and I was not expecting that.");
					}else{
						packet = Children[next_child.getId()].takeRecvObj();
					}
				}else if( LevelID.hierarchicalCompareTo( HidSource) <= 1){ //the same, or lower level.  Ok, the id I am servicing requires the input from my children.
					//children do matter.
					/**
					 * The last problem to resolve.  this happens when hibernating the connection 
					 * with two sources and a sink ( this node)
					 * 
					 * We need a boolean to tell us that we have already told the source that we are getting 
					 * ready to hibernate.  The problem is that if we have more than one source.  One of 
					 * the sources may stop before the other, and the remaining split packets can get lost.
					 * --the problem with this idea is that we are going to lose the providers for the data
					 * in that packaging,  so if we are left with 3 pieces of 1/4 data, but the new 
					 * node has one whole packet, how does this work out ? it doesn't
					 * 
					 * So, we'll do this other thing.  When the receiver receives or emits a hibernation call
					 * it will anounce to the sources what verstion number it has choosen. When the sources
					 * of that same root dept reach the version number, they all stop, and can rearrange.
					 * 
					 * August 26, 2010
					 * Solution:  the solution implemented right now consists of sending organization packets
					 * that will propagate a cycle version at which point all the dependencies involved will
					 * hibernate their connections.  If the user programmer is done sending packet by that point
					 * it is ok, because the reorganization would be unnecesary als well.  However, if communication
					 * is still required by the time the cycle version is reached, the node can re realocated 
					 * with minimal disruption to the user programmer's code.
					 * 
					 */
					
					Serializable[] lst = new Serializable[Children.length];
					for( int i = 0; i < Children.length; i++ ){
						lst[i] =  Children[i].takeRecvObj() ;
						/*
						 * **Update**
						 * The new version will not stop the depency upons sending a hibernation call.  instead the
						 * packet that will be sent and echoed around the dependency tree is an organization tool 
						 * so that all the dependencies involved in the tree can close after some x cycle version.
						 *
						 */
						//Jan 23 2011, to work with asserting versions on the packets
						//use the code branch dependency_assertion
						//if(  lst[i].getVersion() < this.getCycleVersion() ){//lets hope it double sent the last packet
						//	lst[i] = Children[i].takeRecvObj(); //Lets try it again. TODO this could timeout.
						/*}else if( lst[i].getVersion() > this.getCycleVersion()) {
							//I either skipped a packet, or something weird is going on.
							throw new CycleVersionMissmatch(lst[i].getVersion() , this.getCycleVersion()); 
						}*/
					}
					packet = SplitCoalesceMod.onCoalesce(lst);
					//packet.setVersion(CycleVersion);
					++CycleVersion;
					
					//++CycleVersion; //the branches will propagate the version number.
				}//a brother is also ignored
			}
		}else{
			//if hibernate is true, and I was reached, this means I need to be connected...
			//so reconnect
			try {
				//read the comment on reestablishing the connection for the sendObj() method
				Node.getLog().log(Level.FINER, Thread.currentThread().getName() + " reestablishing connection version: " 
										+ this.getRootCycleVersionStop()
										+ " id: " + HidSource.toString() );
				Dependency d = Engine.getInputDependency( Engine.PatternID , HidSource,60000,  this.CycleVersion, this.SplitCoalesceMod);
				//copy the information from the d dependency
				this.copyDependency( d );
				//update the Hidsource on the new branch to point to what the dependency tree points
				packet = takeRecvObj();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (TunnelNotAvailableException e) {
				e.printStackTrace();
			} catch (AdvertsMissingException e) {
				e.printStackTrace();
			} //50 seconds
		}
		
		
		if( this.HibernationEminnent ){
			/*stop when cycle_version has reached cycle_version_stop. */
			if( CycleVersion == this.getRootCycleVersionStop() ){
				Node.getLog().log(Level.FINER, Thread.currentThread().getName() + " -- it is time to hibernate " 
						+  CycleVersion );
				
				if(Stream != null){
					try {
						Stream.close();
						Stream = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				//flush my own advert from jxta cache.
				//flushDeptAdvert();
				DiscoveryService service = Engine.Context.getNetPeerGroup().getDiscoveryService();
				service.flushAdvertisement(DeptAdvertisement);
				
				Hibernated = true;
				//axe the tree
				if( Children != null){
					for( int i = 0; i < Children.length; i++){
						Children[i] = null;
					}
					Children = null;
				}
				
				//Nov 2 debug line
				//DependencyMapper.deleteAdvertFromMap(Engine.PatternID, this.LevelID);
				//Thread.sleep(1500);
			}
		}
		
		return packet;
	}
	/**
	 * This function will copy a dependency to this one.  this dependency may be a root dependency 
	 * or it can be a branch of the tree.  The new branch should conserver the hid_source id of
	 * the root dependency.
	 * 
	 * It will preserve the CycleVersion of the class calling this method.  so it is not 
	 *   a full copy of d.
	 * @param d
	 */
	public void copyDependency( Dependency d){
		this.HibernationEminnent = d.HibernationEminnent;
		this.Hibernated = d.Hibernated;
		this.HibernateEmitter = d.HibernateEmitter;
		this.CycleVersionStop = d.CycleVersionStop;
		if( d.Children != null){
			//Children = new Dependency[d.Children.length];
			Children = d.Children;
			for( int i =0; i < Children.length ; i++){
				//Children[i] = d.Children[i];
				if(Children[i] !=null){
					Children[i].Parent = this;  //transfer children.
					Children[i].setDeptTreeCycleVersion(this.getCycleVersion());
				}
			}
		}
		Stream = d.Stream;
		if( Stream != null){
			Stream.setConnectionChangeListener(this);
		}
		Engine = d.Engine;
		//Source reamin the same
		if( this.HidSource != null){
			d.setHidSource(this.HidSource);
		}
		if( this.HidSink != null){
			d.setHidSink(this.HidSink);
		}
	}
	
	/**
	 * Recursively sets the HidSink
	 * @param id
	 */
	public void setHidSink( HierarchicalDependencyID id){
		HidSink = id;
		if(Children != null){
			for( int i = 0; i < Children.length; i++){
				if(Children[i] != null){
					Children[i].setHidSink(id);
				}
			}
		}
	}
	/**
	 * Recursively sets the HidSource on this Dependency branch.  
	 * The method is primary made to assist in reestablishing a connection after 
	 * it has being pruned by a hibernation call.
	 * @param id
	 */
	public void setHidSource(HierarchicalDependencyID id){
		HidSource = id;
		if(Children != null){
			for( int i = 0; i < Children.length; i++){
				if(Children[i] != null){
					Children[i].setHidSource(id);
				}
			}
		}
	}
	public void sendObj( Serializable obj) throws InterruptedException, IOException, EngineClosedException{
		//obj.setVersion(CycleVersion); //ensures consistency for split and coalesce as well as for network reconnections.
		sendObjRecursive(obj);
	}
	/**
	 * 
	 * @param obj
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws EngineClosedException 
	 * @throws ConnectionHibernatedException 
	 * @throws ConnectionHibernatedException 
	 */
	private void sendObjRecursive( Serializable obj) throws InterruptedException, IOException, EngineClosedException{
		if( !Hibernated){
			if( Children == null){
				//I am a leaf, just send the object
				Stream.SendObject(obj);
				++CycleVersion; //if it does not throw.
				//if the integer stop cycle is reached then take this of
				if( this.HibernationEminnent){
					if( CycleVersion == this.getRootCycleVersionStop() ){
						synchronized( Engine.HostedHids ){ //we want to set a clear distinction between the time when this nodes
								//was hosting the dependency and when it stopped doing it.
							Node.getLog().log(Level.FINER, Thread.currentThread().getName() + "  -- it is time to hibernate " 
									+  CycleVersion );
							if(Source){
								//remove stream from list os streams
								Engine.ConnectionServers.remove(Stream);
								DiscoveryService service = Engine.Context.getNetPeerGroup().getDiscoveryService();
								service.flushAdvertisement(DeptAdvertisement);
							}
							this.Hibernated = true;
							//axe the tree
							/*November 15, 2010
							 * I am hoping this code is not necesary since the same check is done at DataflowTemplate for all
							 * connection.  It is also deadlocking the thread right before going into hibernation for 
							 * the stencil pattern.
							 * if( Stream != null){
								
								while( Stream.hasSendData() ){
									try{
										Thread.sleep(50);
									}catch(InterruptedException e){
										e.printStackTrace();
									}
								}	
								Stream.close();
								Stream = null;
								//flush my advert from jxta cache.
								//flushDeptAdvert();
							}*/			
							//Thread.sleep(1500);//sleep a while to let the client catchup.
						}
					}
				}
			}else{
				//int test = this.HidSink.hierarchicalCompareTo(LevelID);
				/**
				 * this if determines if the dependency should send the object as given by the user, or if
				 * it should split up the DependencyPacket into pieces and then send the pieces.
				 */
				if(this.HidSink.hierarchicalCompareTo(LevelID) < 1){
					HierarchicalDependencyID next_child = HidSink.getLevel( this.LevelID.getLevel() + 1);
					if( Children[next_child.getId() ] != null){
						//don't split because 
						//TODO add try catch similar to the one below
						Children[next_child.getId()].sendObjRecursive(obj);
						++CycleVersion; //if it does not throw.
					}else{
						Node.getLog().log(Level.WARNING, "Children[" + next_child.getId() +"] was null, and I did not expect that " );
					}
				}else{
					//I am a trunk, split the object and send each piece in the approbate leaf
					Serializable[] lst = SplitCoalesceMod.onSplit( obj );//obj.splitDependency(DependencySplitLevel);
					if( lst.length != Children.length ){
						Node.getLog().log(Level.WARNING, " The children list and the dependency packet list don't match");
					}
					/*
					 * if the connection fails, because one object can go through after the connection was shutdown, we 
					 * will reestablish the connection.  But, that will repeat this whole function, so we don't want 
					 * to double count the CycleVersion.  If we hit the exception, don't increase the counter.
					 * 
					 * Another point, is that the counter sould be increase only once per packet collected from all
					 * the children.
					 */
					
					for( int i = 0; i < (Children.length < lst.length  ? Children.length : lst.length); i++ ){
						if( Children[i] != null){
							//lst[i].setVersion(obj.getVersion());
							Children[i].sendObjRecursive(lst[i]);
						}else{
							Node.getLog().log(Level.WARNING, "Children[" + i +"] was null, and I did not expect that " );
						}
					}
					++CycleVersion; 
					
				}
				/*
				 * Now shop off the branches so that we don't over-split the dependencies.
				 */
				if( this.HibernationEminnent){
					if( CycleVersion == this.getRootCycleVersionStop() ){
						if( Children != null){
							for( int i = 0; i < Children.length; i++){
								Children[i] = null;
							}
							Children = null;
						}
						this.Hibernated = true;
					}
				}
			}
			
		}else{//hibernate routine starts here.
			//get new connection dependency
			
			/* TODO
			 * This wait statement works very well.  Even though it is for debugging, make sure to replace it 
			 * with something more robust.  The main problem with this fix is that first it prolongs the 
			 * running time of the application.  And second, it may not be enough time for some dataflow
			 * configuration.
			 * 
			 * I don't really understand why these wait statments are really necesary, more study of the
			 * algoirtm's behavior especially in the edges of time windows whre the behabor to close the current
			 * connection and the behavior to establish the new connection can mix to some extentt.
			 * Sept 26.  It is like that the remote process can go into hibernation and comeback before this
			 * node finishes going out.se
			 */
			//Thread.sleep(500); //to debug.
			Node.getLog().log(Level.FINER, Thread.currentThread().getName() + " reseablishing connection " + CycleVersion
					+ " dept_id: " + this.getHierarchicalID());
			Dependency n = Engine.registerOutputDependency(this.HidSink, this.CycleVersion, this.SplitCoalesceMod);
			this.copyDependency(n);
			
			//Engine.registerOutputDependencyChild(this, this.HidSink);
			//
			//this.copy(new_dep);
			sendObjRecursive( obj);
			//TODO 
			//TODO
		}
		
	}

	/**
	 * The same cycle version stop should be used by all the dependency branches and leafs.
	 * Because we want to all stop at the same version.
	 * 
	 * Avoid using the CycleVersionStop variable directly.
	 * @param num
	 */
	public void setRootCycleVersionStop(long num){
		this.getRoot().CycleVersionStop  = num;
	}
	/**
	 * return the cycle version stop from the parent.
	 * @return
	 */
	public long getRootCycleVersionStop(){
		return this.getRoot().CycleVersionStop;
	}
	
	
	/**
	 * For use by the advance or expert users.  it will also tell the client if the dept advert should be set 
	 * to dirty.
	 * @param proper_version_hibernation
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void hibernateConnection( long proper_version_hibernation) throws InterruptedException, IOException{
		if( Source ){
			/**
			 * If this dept is my output, set the dept advertisement as dirty
			 */
			hibernateConnectionEcho( proper_version_hibernation, true);
			//Very important! take out the dependency id from the engine, so that no shared mem or distributed
			//memory node tries to connect to it aft it has being set for hibernation.
			//TODO this only works for root dependencies, needs to be extended to work for tree dependencies
			//Nov 16, 2010 ack Dec 10
			
			Engine.HostedHids.remove(Stream.getDependencyID().toString());
			
		}else{
			/**
			 * if I am connecting to that dept, I can't decide wheather the dept is dirty.
			 */
			hibernateConnectionEcho( proper_version_hibernation, false);
		}
	}
	
	/**
	 * For internal use. it will echo the call in an algorithm that organizes the hierarchical links 
	 * 
	 * This will inform the the feader or consumer that the connection will be interrupted.
	 * 
	 * the following methods are synchronized to make sure the manager thread and the
	 * user thread does not corrupt the connection.  The manager thread can be a thread on 
	 * the advance layer that decides to move a perceptron or to adjusts connections.
	 * The user thread is the thread running the user's basic layer.
	 * The methods are:
	 * SendObj -> sends object
	 * TakeRecvObj -> receives object
	 * hibernateConnection -> will wait for outstanding sends, then it will send a message 
	 * 						to hibernate the connection
	 * onHibernateConnection -> will receive a call to hibernate connection.
	 * 
	 * TODO 
	 * TODO
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ConnectionHibernatedException 
	 */
	private  void hibernateConnectionEcho( long proper_version_hibernation , boolean dirty_dept ) throws InterruptedException, IOException{
		//synchronized was taken out because the send and recv threads from the socket connection
		//will need to access these function while the consumer threads may have locked the object
		//waiting for new data.
		HibernateEmitter = true; //this is to prevent the echo from looping indefinitely
		this.HibernationEminnent = true;
		if( Stream == null){
			if( Children != null){
				for( int i = 0; i < Children.length; i++){
					if( Children[i] != null){
						Children[i].hibernateConnectionEcho(proper_version_hibernation, dirty_dept);
					}
				}
			}
		}else{
			//inform other side
			//if( this.getRootCycleVersionStop() == -1){
				//calculate a good version number to stop
				//TODO A better way to calculate stop cycle.
				//this would be roughtly 2000 1MB objects  for performance testing
				//for debuggin 10 is a good number, but remember to add sleepers to the code.
			this.setRootCycleVersionStop( proper_version_hibernation ); //CycleVersion + 10 ); //TODO
				//TODO
			//}else{
			System.out.println( Thread.currentThread().getName() + " hibernateConnection() set at cycle :  " + this.getRootCycleVersionStop() + " Current cycle: " + this.getCycleVersion());
			//}
			
			ConnectionChangedMessage message = new ConnectionChangedMessage();
			message.setHibernatingDeptID(LevelID);
			message.setVersionStop(this.getRootCycleVersionStop());
			message.setDirty( dirty_dept);
			//get ack
			
			Stream.SendObject(message);
			
			
		}	
	}
	
	/**
	 * 
	 * This function will go back one step using the parent dependency.  The pointer was added 
	 * to this class so that this method can work efficiently.  The methods goes back to the root
	 * dependency and starts working from there.  The goal is to take down the siblings of this
	 * dependency if there are any.
	 * 
	 * This is called from the SocketManger ( And soon from a the Shared memory manager)
	 * The method is basically sending an acknowledgment message to the node that requested
	 * to end the connection.  Additionally, the method is part of an echo that should
	 * make sure enough of the tree is pruned in order to allow the dependency connections
	 * to coalesce as well as to split.
	 * @param message This will be called from a ConnetionManager implementer. the message has 
	 * 		a request to stop at some version number in the future.
	 */
	public void onHibernateConnection(ConnectionChangedMessage message ){	
		//synchronized was taken out because the send and recv threads from the socket connection
		//will need to access these function while the consumer threads may have locked the object
		//waiting for new data.
		try {
			if( Parent == null){ //this is root
				//so, I will propagate the call also, if the stop version is moving to a later version
				//this can only happen if two or more dataflows emit the call at very close to the same time 
				//the same time being influence by network delay.
				//Note: that this won't happen when the deadline is close because by then the dataflow that
				//is planning to hibernate would check its neighbor connection and notice that its neighbor
				//is already on the hiberantion cycle.  At that point, the dataflow would either join 
				//the same version stop number, or it could decide to wait until the new nodes are online.
				if( !HibernateEmitter || this.getRootCycleVersionStop() < message.getVersionStop() ){
					this.setRootCycleVersionStop( message.getVersionStop() ) ;
					this.hibernateConnectionEcho( message.getVersionStop(), message.isDirty());//echo
				}
				
			}else{
				
				if( !HibernateEmitter  || this.getRootCycleVersionStop() < message.getVersionStop() ){
					//If this node was not the one who starte this.
					//parent will echo, which should freeze the siblings as well.
					this.setRootCycleVersionStop( message.getVersionStop() ) ;
					Parent.hibernateConnectionEcho( message.getVersionStop(), message.isDirty() );	
				}
			}
			
			//set advert to dirty only if the dept source indicated it.
			/*if( message.isDirty() ){
				this.DeptAdvertisement.setDirty( true );
				//the datalink advert is the link used by the dept engine.
				DataLinkAdvertisement adv = MultiModePipeClient.getDataAdvertisement(Engine.PatternID, DeptAdvertisement.getDataID());
				if( adv != null){
					adv.setDirty(true); //
				}
			}*/
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	/**
	 * Returns the root for the dependency tree.  this is mainly used by the hibernate connection
	 * method, which needs to cut down the tree at a point that is easier to reach by way of 
	 * this dependency's parent.  in that method {@link Dependency#onHibernateConnection(ConnectionChangedMessage)}
	 * this dependency and its siblings are cut off the tree.
	 * @return
	 */
	public Dependency getRoot(){
		if( Parent == null){
			return this;
		}else{
			return Parent.getRoot();
		}
	}
	/**
	 * Used by the Dependency Engine to add a child once the main truck dependency has been created.
	 * The Dependency Engine will first create a dependency for the first dependency id if such dependency does
	 * not exists.  If a sub-dependency is created after that, the dependency gets appendend to the parent 
	 * dependency.
	 * @param con
	 * @param server_id
	 * @param tree
	 * @param source
	 * @param unique_id
	 */
	public void addChild( ConnectionManager con, HierarchicalDependencyID server_id
						, HierarchicalDependencyID tree, PipeID unique_id, DependencyAdvertisement advert ){
		addChild(con, server_id, tree, unique_id, 0, advert);
	}
	/**
	 * Used by the Dependency Engine to add a child once the main truck dependency has been created.
	 * The Dependency Engine will first create a dependency for the first dependency id if such dependency does
	 * not exists.  If a sub-dependency is created after that, the dependency gets appendend to the parent 
	 * dependency.
	 * @param con
	 * @param server_id
	 * @param tree
	 * @param source
	 * @param unique_id
	 * @param level
	 */
	public void addChild( ConnectionManager con, HierarchicalDependencyID server_id
							, HierarchicalDependencyID tree
							, PipeID unique_id, int level
							, DependencyAdvertisement advert)
	{
		LevelID  = tree.getLevel(level);
		if( LevelID.getLevel() < tree.getLevel()){
			//this is the parent create child
			HierarchicalDependencyID next_child = tree.getLevel(LevelID.getLevel() + 1);
			if( Children == null){
				Children = new Dependency[next_child.getTotal()];
			}
			if( Children[ next_child.getId()] == null){
				Children[next_child.getId()] = new Dependency( this, con, server_id, tree, unique_id, level + 1, Engine, CycleVersion
						,this.SplitCoalesceMod, advert);
			}else{
				Children[next_child.getId()].addChild(con, server_id, tree, unique_id, level + 1, advert);
			}
			Stream = null;
			UniqueId = null;
		}else{
			Stream = con;
			Stream.setConnectionChangeListener(this);
			Children = null;
			Source = true;
			UniqueId = unique_id;
		}
	}
	/**
	 * 
	 * Returns the total number of leaf dependencies that make up this dependency.
	 * @return
	 * 
	 */
	public int getConnectedLeafs(){
		int ans = 0;
		if(Children != null){
			for(int i=0; i < Children.length; i++){
				if( Children[i] != null){
					ans += Children[i].getConnectedLeafs();
				}
			}
		}else{
			if(Stream != null){
				return 1;
			}
		}
		return ans;
	}
	/**
	 * returns true if all the depenencies necesary to start using this dependency are present
	 * @return
	 */
	public boolean isConnected(HierarchicalDependencyID id){
		
		if(Children != null){
			for(int i=0; i < Children.length; i++){
				if( Children[i] == null){
					//if the children are null and they are needed by the dependency
					//return false
					if( LevelID.hierarchicalCompareTo(id) <= 1){
						return false;	
					}
				}else{
					if( !Children[i].isConnected(id) ){
						return false;
					}
				}
			}
		}else{
			if(Stream == null){
				return false;
			}
		}
		return true;
	}

	public PipeID getUniqueId() {
		return UniqueId;
	}

	public HierarchicalDependencyID getHierarchicalID() {
		return LevelID;
	}
	/**
	 * returns the cycle version for this dependency.
	 * @return
	 */
	public long getCycleVersion() {
		return CycleVersion;
	}
	/**
	 * returns the cycle version for the tree.  at time the 
	 * leaf dependencies may be of by one cycle, so the 
	 * version for the dependency tree is the one hold by 
	 * the root dependency.
	 * @return
	 */
	public long getParentCycleVersion(){
		if(Parent == null){
			return CycleVersion;
		}else{
			return Parent.getCycleVersion();
		}
	}

	/**
	 * Sets the cycle version for all the tree.  this will
	 * override the current values on the leaf depdencies,
	 * which can be off by one depending on their neighbors
	 * messages..
	 * 
	 * @param cycle_version
	 */
	public void setDeptTreeCycleVersion(long cycle_version){
		this.CycleVersion = cycle_version;
		if( Children != null){
			for ( int i = 0; i < Children.length; i++){
				if(Children[i] != null){
					Children[i].setDeptTreeCycleVersion(cycle_version);
				}
			}
		}
		
	}
	public void setCycleVersion(long cycleVersion) {
		CycleVersion = cycleVersion;
	}

	/**
	 * Will close the connection even if there are objects left in the queue
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void close() throws IOException, InterruptedException{
		try{
			if( Stream == null){
				if( Children != null){
					for( int i = 0 ; i < Children.length; i++){
						if( Children[i] == null){ //skip the null children.
							continue;
						}
						Children[i].close();
					}
				}
			}else{
				Stream.close();
			}
		}catch(IOException e){
			System.err.println("IOError while closing socket Dept id: " + this.LevelID.toString() + " version " + this.getCycleVersion() 
					+ " hibernated("+ this.Hibernated + ")" + " hibernate eminent(" + this.HibernationEminnent+ ")");
			throw e;
		}
	}
	
	public boolean isBound(){
		if( Stream == null){
			if( Children != null){
				boolean ans = true;
				for( int i = 0; i < Children.length; i++){
					if(Children[i] == null){ //if this child is null it probably 
						//is because it was not used as part of the connection 
						continue;
					}
					if( Children[i].isBound() == false){//if any of the non-null children
						//are not bound, then we consider this dependency not bounded.
						ans = false;
						break;
					}
				}
				return ans;
			}else{
				return false;
			}
		}else{
			return Stream.isBound();
		}
	}

	public boolean isSource() {
		return Source;
	}
	public void waitOnRecvData(){
		while( hasRecvData()){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public void waitOnSentData(){
		while( hasSendData()){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public boolean hasRecvData(){
		if( Stream != null){
			return Stream.hasRecvData();
		}else{
			if( Children != null){
				//if any of the children has not receive data return false
				for( int i = 0; i < Children.length; i++){
					if(Children[i] != null){
						if( Children[i].hasRecvData()){
							return true;
						}
					}
				}
			}
			//if we reach this point we either don't have children
			//or the all have recv their data.
			return false;
		}
	}
	public boolean hasSendData() {
		if( Stream != null){
			return Stream.hasSendData();
		}else{
			if( Children != null){
				//if any of the children has not sent data return false
				for( int i = 0; i < Children.length; i++){
					if(Children[i] != null){
						if( Children[i].hasSendData()){
							return true;
						}
					}
				}
			}
			//if we reach this point we either don't have children
			//or the all have sent their data.
			return false;
		}
	}
	public long getCycleVersionStop() {
		return CycleVersionStop;
	}
	public void setCycleVersionStop(long cycleVersionStop) {
		CycleVersionStop = cycleVersionStop;
	}

	public String getSegs(){
		if( Stream != null){
			return ""+Stream.getRemoteNodeConnectionDataID();
		}else{
			return "no_parent_string";
		}
	}

	public String getCachedObjSize() {
		if( Stream != null){
			return Stream.getCachedObjSize();
		}else{
			String str = "P";
			if(Children != null){
				for(int i = 0; i < Children.length; i++){
					str += "C[:"+i + ":" + Children[i].getCachedObjSize() + "]";
				}
			}
			return str;
		}
	}
}
