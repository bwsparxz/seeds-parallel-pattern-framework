/* * Copyright (c) Jeremy Villalobos 2009
 *   
 *   * All rights reserved
 */
package edu.uncc.grid.pgaf.deployment;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;

import org.globus.cog.abstraction.impl.common.ProviderMethodException;
import org.globus.cog.abstraction.impl.common.task.InvalidProviderException;

import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.advertisement.SpawnPatternAdvertisement;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.Types.DataFlowRole;


/**
 * This class will schedule multiple Node Objects on different systems.
 * It will use Globus to access the Grid Nodes job scheduler
 * It will also use ssh, but in this case it assumes the user has configured passwordless access
 * the ssh access has not being implemented.
 * The shared midleware allows the program to run on the user's workstation.  The feature is 
 * still being test.  The DataSinkSource running on the user's workstation has being tested with 
 * good results.
 *
 * The file used to specify the servers will have the following format
 * 
 * servername middleware(globus,ssh) jobscheduler(fork,condor) number_of_cpus cores_per_cpu
 * 
 * @author jfvillal
 *
 *
 *TODO add proxy delegation to deployer
 *
 *
 */

public class Deployer{
	/**
	 * The tags used to define the attributes from the grid node list
	 */
	final int SERVER_NAME = 0;
	final int MIDDLEWARE = 1;
	final int GSIFTP_PORT = 2;
	final int GRAM_PORT = 3;
	final int JOB_SCHEDULER = 4;
	final int CPU_COUNT = 5;
	final int CORES_PER_CPU = 6;
	final int GRID_NAME = 7;
	final int ROLE = 8;
	final int ATT_LENGTH = 9;

	public static String Name = "Deployer";
	
	Set<String> AvailableServers;
	String PathOfPGAFLibs;
	//Class UserAppClass;
	Logger Log;
	//String ServerListFile;
	List<Thread> ThreadList;
	boolean JavaSocketUse;
	/**
	 * The pattern id for the first patters started by this object
	 */
	PipeID PatternId; 
	/**
	 * Nov 8 2009
	 * Nested is used to separate the nested Deployer from the seed Deployer.  The seed deployer will
	 * send out jobs and it start a bunch of idle nodes that search for pattern to be part of.
	 * The nested deployer will, on this implementation, just publish an advertisement for the new patter.
	 * 
	 * The future implementation will include the ability for the nested deployer to use proxy delation
	 * to pass credential from one node to the next.  So that new nodes can be created if necesary.
	 * 
	 */
	boolean Nested;
	
	/**
	 * This node class is used to start a node thread on the client computer.
	 */
	Node LocalMachineNode;
	boolean ClientMachineInAvailableServers;
	/**
	 * This string gets the concatenated list of arguments.
	 */
	public String[] RemoteModuleArguments;

	
	boolean DebugMode;
	/**
	 * 
	 * Used to set the anchors 
	 * 
	 */
	//Map<String, Types.DataFlowRoll> Anchors;
	/**
	 * 
	 * @param server_list_file the list of servers where this application should be deployed. the file should have the format:<br>
	 * 	hostname service gsiftp_port gram_port fork_or_condor cpu_count cpu_core grid_name.<br>
	 * <ol>
		 * <li>	<b>hostname:</b>  The hostname can also be an IP address <br> </li> 
		 * <li> <b>service:</b>  service is always globus <br></li>
		 * <li> <b>gsiftp_port:</b>  gsiftp should be 2811 if it is the default port <br></li>
		 * <li> <b>gram_port:</b>  gram should be 8443 if it is the default port <br></li>
		 * <li> <b>fork_or_condor:</b>  fork is used if it is a single server, condor if it is server managing a list of computer.  no support for PBS yet <br></li>
		 * <li> <b>cpu_count:</b> the number of cpu's on the server</br></li>
		 * <li> <b>cpucore:</b> the number of cores per cpu assuming the server has a homogeneous cpus </br></li>
		 * <li> <b>grid_name:</b> the name the user give to that grid node </br><br></li>
	 * </ol>
	 * Future implementation may have this information be created automatically.  
	 * @param path_of_pgaf_libs  The path to the "shuttle" folder. the folder that will be shared with the other grid nodes
	 * @param workpool  a workpool implementing class, most of the user's application should be here
	 * @param debug_mode	it will not test the network card to figure out if it is on a WAN or LAN and it will allow RDV's
	 * 		on multiple ports.  default should be false;
	 * @throws SecurityException
	 * @throws IOException
	 */
	public Deployer(String path_of_pgaf_libs ) throws SecurityException, IOException{
		
		//set logger
		Log = Logger.getLogger( "edu.uncc.grid.pgaf.deployment");
		FileHandler handler = new FileHandler("Deployer.log" );
		Log.addHandler(handler);
		SimpleFormatter formatter = new SimpleFormatter();
	    handler.setFormatter(formatter);
		Log.setLevel(Level.ALL);
			
		Set<String> list = new HashSet<String>();
		FileInputStream file = new FileInputStream(path_of_pgaf_libs + System.getProperty("file.separator") + "AvailableServers.txt");
		DataInputStream stream = new DataInputStream(file);
		while( stream.available() != 0){
			String line = stream.readLine();
			if( line.length() > 1){ //prevent last line error
				Log.log(Level.FINEST, "*** line read from file AvailableServers.txt line: " + line);
				if( line.charAt(0 ) != '#'){
					Log.log(Level.INFO, "adding line " + line);
					list.add(line);
				}
			}
		}
		file.close();
		stream.close();
		AvailableServers = list;
		PathOfPGAFLibs = path_of_pgaf_libs;
		
		ThreadList = new ArrayList<Thread>();
		
		PatternId = null;
	}
	/**
	 * Sets anchors.  The anchors are computers with unique resources, so a specialized node would only run there.<br>
	 * Anchors are also used to direct the framework where the source data and sink data sould be taken from and placed in.
	 * <br>
	 * The Anchors are to be set by the deployer when the user is ready to deploy the application.<br>
	 * 
	 * @param anchor
	 * @return returns true if the server is present in this computer. 
	 * @throws ServerNameNotInAvailableServerFile 
	 */
	
	public boolean validateAnchor(Anchor anchor ) throws ServerNameNotInAvailableServerFile{
		boolean ServerPresent = false;
		Iterator<String> it = AvailableServers.iterator();
		
		Log.log(Level.FINER, "------------- server present: " + ServerPresent + "server_name##" + anchor.getHostname() +"##");
		while(it.hasNext()){
			String str = it.next();
			String attributes[] = str.split(" " );
			
			if( anchor.getHostname().toLowerCase().compareTo(attributes[SERVER_NAME].toLowerCase()) == 0){
				ServerPresent = true;
				break;
			}
		}
		
		
		
		/*if(ServerPresent){
			Anchors.put( server_name , roll);
		}else{
			throw new ServerNameNotInAvailableServerFile(
					"The server " + server_name + "  is not in the Available Servers file.  Add the server in that file first"
					);
		}*/
		return ServerPresent;
	}
	/**
	 * This function is used to send init arguments to the remote modules.  The users specifies the 
	 * initiation arguments at the deployer, and they are transmitted to each Module at each remote location.
	 *   The the user may opt to leave the list of argument empty.
	 * @param args
	 */
	public void setModuleInitArguments(String args[]){
		this.RemoteModuleArguments = args;
		/*for(int i = 0; i < args.length ; i++){
			this.RemoteModuleArguments += args[i] + ArgumentDivider;
		}
		this.RemoteModuleArguments.replace(" ", SpacePlaceHolder);*/
	}
	
	/**
	 * Starts the DatasourceSink Node.  For now it is convenient to have the 
	 * Data source and sink in the same location as the Deployer.
	 * <br>
	 * It is also convenient for now to have the SourceSink data also distribute
	 * the initiated User Module.
	 */
	public void startDataSourceSinkThread(){
		
	}
	
	/**
	 * The function will spawn multiple processes in the grid nodes specified in the class constructor.
	 *   The function is non-blocking, so waitOnPGAFtoFinish() should be used if you want to wait for 
	 *   the result.
	 * @param client_grid_node_name: the name of the grid node where the deployment is happening.
	 * 	
	 * @param skip_transfer:  the option to skip the transfer of the data from the shuttle folder
	 *  
	 * @throws Exception
	 */
	public Thread Spawn(boolean skip_transfer /*for faster debug*/) throws Exception{
		if( AvailableServers.isEmpty()){
			Log.log(Level.FINE, "No servers on the file, exiting...");
			System.exit(1);
		}
		
		int data_sink_source_cpus = 1;
	
		Iterator<String> it = AvailableServers.iterator();
	
		//List<Integer> grid_names = new ArrayList<Integer>();
		
		Map<Integer, Thread> grid_name_thread = new HashMap<Integer,Thread>();
		
		while(it.hasNext()){
			String str = it.next();
			String attributes[] = str.split(" " );
			
			int computer_node_count = Integer.parseInt(attributes[CPU_COUNT]); 
			int cpu_core_count =  Integer.parseInt(attributes[CORES_PER_CPU]);
			//Types.DataFlowRoll d_roll = Anchors.get(attributes[SERVER_NAME]);
			
			if( attributes[MIDDLEWARE].compareTo("globus") == 0 ){
				//send a jobsubmit to globus for the amount of jobs				
				try {
					
					/**
					 *Create a diseminator which will handle one server.  give it 
					 *the information and start a new thread.  The loop will spawn
					 **/
		
					GLOBUSDiseminator dis = new GLOBUSDiseminator(
							attributes[SERVER_NAME]
							, Integer.parseInt( attributes[GSIFTP_PORT] )
							, Integer.parseInt( attributes[GRAM_PORT])
							, attributes[GRID_NAME]
							, attributes[JOB_SCHEDULER]
							, this.PathOfPGAFLibs
							//, UserAppClass		//template name
							, cpu_core_count
							, this.JavaSocketUse
							, Types.PGANodeRole.valueOf(attributes[ROLE])
							, this.Log );
					
					/*if( this.RemoteModuleArguments != null){
						dis.setRemoteArguments(this.RemoteModuleArguments);
					}*/
					
					
					
					DiseminatorThread dis_thread = new DiseminatorThread(
							dis
							, this.PathOfPGAFLibs
							, GLOBUSDiseminator.getPGAF_PATH()
							, attributes[GRID_NAME]
							, computer_node_count
							, RUN_SEEDS
							, grid_name_thread.get(attributes[GRID_NAME].hashCode())
							);
					
					//debug option could be available to users as well
					
					/**
					 * Nov 25.  A little explanation on shuttle folder policy.
					 * 
					 * if the user wants to skip transfer, we skip transfer.  if
					 * the user does not want to skip transfer, then it is all 
					 * up to send_shuttle_folder.  send_shuttle_folder is used to 
					 * make sure the folder is sent only once per grid.  giving 
					 * the same grid node name to multiple servers means they are in the
					 * same grid node.  we define a grid node as server or computers
					 * sharing a file resource.  IF the user is not using the shared 
					 * file resource in those server, he should give different grid 
					 * name to the servers. the grid node name is arbitrary.
					 * 
					 * this is done by giving the previous thread that is trnasfering files to 
					 * this thread, since both have the same grid name.  if there is no such
					 * thread, this thread will proceed to send the data and be put into the 
					 * map.  if a preceding thread does exist, then the DiseminatorThread
					 * will wait for the first thread to finish, and it will skip transfering
					 * files
					 * 
					 */
					//dis_thread.setSkipTransfer(skip_transfer );
					
					
					Thread thread = new Thread(dis_thread);
					ThreadList.add(thread);
					
					
					thread.start();
					//grid_names.add( attributes[GRID_NAME].hashCode());
					/*
					 * all should wait for the first thread, but after that, everybody can start 
					 * submitting a job to the same file system.
					 */
					if( !grid_name_thread.containsKey(attributes[GRID_NAME].hashCode())){
						grid_name_thread.put(attributes[GRID_NAME].hashCode(), thread);
					}
					
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (InvalidProviderException e) {
					e.printStackTrace();
				} catch (ProviderMethodException e) {
					e.printStackTrace();
				}
					
					
			}else if( attributes[MIDDLEWARE].compareTo("ssh") == 0){
				//deploy job using for cpu_count * cpu_cores.
				SSHDiseminator diseminator = new SSHDiseminator(
									attributes[SERVER_NAME]
								, 	attributes[GRID_NAME]
								, 	this.PathOfPGAFLibs
								,  	cpu_core_count
								,   this.JavaSocketUse
								,   Types.PGANodeRole.valueOf(attributes[ROLE])
								,  Log );
				
				SSHDiseminatorThread thread = new SSHDiseminatorThread(diseminator
							, this.PathOfPGAFLibs, SSHDiseminator.getPGAF_PATH()
							, attributes[GRID_NAME], computer_node_count
							, RUN_SEEDS
							, grid_name_thread.get( attributes[GRID_NAME].hashCode()) );
				

				//thread.setSkipDirTransfer(skip_transfer);
				thread.start();
				
				if( !grid_name_thread.containsKey(attributes[GRID_NAME].hashCode())){
					grid_name_thread.put(attributes[GRID_NAME].hashCode(), thread);
				}
			}else if( attributes[MIDDLEWARE].compareTo("shared") == 0){
				data_sink_source_cpus = Integer.parseInt(attributes[CORES_PER_CPU]);
			}else{
				Log.log(Level.FINE, "Protocol " + attributes[MIDDLEWARE] + " not recoginized, skipping... " );
			}
		}
		
		Iterator<Thread> ot = ThreadList.iterator();
		while( ot.hasNext()){
			Thread t = ot.next();
			t.join();
		}
		//next function deploys the local node.  If the user puts the localhost in the 
		//server file.  the framework assumes that means the user wants to 
		//deploy a node on the local machine.
		/*
		 * Only start the cient node if we are on the client.  If Spawn was called by a nested Deployer, we 
		 * already have a node running on this computer.
		 */
		if( ! this.isNested() ){
			return startClientComputerNode();
		}
		return null;
	}
	
	/**
	 * 
	 * August 5 2011.  Now the installation of the frameworks is separated from running the framework to
	 * speed up deployment.  This method needs to be run through the InstallSeedsOnResources command 
	 * every time the user needs to refresh the state of the jars in the Grid resources.
	 * 
	 * @return
	 * @throws Exception
	 */
	public void InstallSeedsonResources( ) throws Exception{
		if( AvailableServers.isEmpty()){
			Log.log(Level.FINE, "No servers on the file, exiting...");
			System.exit(1);
		}
		
		int data_sink_source_cpus = 1;
	
		Iterator<String> it = AvailableServers.iterator();
		
		Map<Integer, Thread> grid_name_thread = new HashMap<Integer,Thread>();
		
		while(it.hasNext()){
			String str = it.next();
			String attributes[] = str.split(" " );
			
			int computer_node_count = Integer.parseInt(attributes[CPU_COUNT]); 
			int cpu_core_count =  Integer.parseInt(attributes[CORES_PER_CPU]);
			//Types.DataFlowRoll d_roll = Anchors.get(attributes[SERVER_NAME]);
			if( attributes.length < ATT_LENGTH){
				System.out.println("There is a new attribute with Seeds 2.0.  Make sure your AvailableServers file has a " +
						"Node type [DIRECTOR_RDV or LEAFWORKER].  This replaces the runtime that used to set this automatically" +
						".  The new approach is more straight forward and faster.");
				System.exit(1);
			}
			
			if( attributes[MIDDLEWARE].compareTo("globus") == 0 ){
				//send a jobsubmit to globus for the amount of jobs				
				try {
					
					/**
					 *Create a diseminator which will handle one server.  give it 
					 *the information and start a new thread.  The loop will spawn
					 **/
		
					GLOBUSDiseminator dis = new GLOBUSDiseminator(
							attributes[SERVER_NAME]
							, Integer.parseInt( attributes[GSIFTP_PORT] )
							, Integer.parseInt( attributes[GRAM_PORT])
							, attributes[GRID_NAME]
							, attributes[JOB_SCHEDULER]
							, this.PathOfPGAFLibs
							, cpu_core_count
							, this.JavaSocketUse
							,   Types.PGANodeRole.valueOf(attributes[ROLE])
							, this.Log );
					
					/*if( this.RemoteModuleArguments != null){
						dis.setRemoteArguments(this.RemoteModuleArguments);
					}*/
					
					
					
					DiseminatorThread dis_thread = new DiseminatorThread(
							dis
							, this.PathOfPGAFLibs
							, GLOBUSDiseminator.getPGAF_PATH()
							, attributes[GRID_NAME]
							, computer_node_count
							, INSTALL_SEEDS
							, grid_name_thread.get(attributes[GRID_NAME].hashCode())
							);
					
					//debug option could be available to users as well
					
					/**
					 * Nov 25.  A little explanation on shuttle folder policy.
					 * 
					 * if the user wants to skip transfer, we skip transfer.  if
					 * the user does not want to skip transfer, then it is all 
					 * up to send_shuttle_folder.  send_shuttle_folder is used to 
					 * make sure the folder is sent only once per grid.  giving 
					 * the same grid node name to multiple servers means they are in the
					 * same grid node.  we define a grid node as server or computers
					 * sharing a file resource.  IF the user is not using the shared 
					 * file resource in those server, he should give different grid 
					 * name to the servers. the grid node name is arbitrary.
					 * 
					 * this is done by giving the previous thread that is trnasfering files to 
					 * this thread, since both have the same grid name.  if there is no such
					 * thread, this thread will proceed to send the data and be put into the 
					 * map.  if a preceding thread does exist, then the DiseminatorThread
					 * will wait for the first thread to finish, and it will skip transfering
					 * files
					 * 
					 */
					//dis_thread.setSkipTransfer(skip_transfer );
					
					
					Thread thread = new Thread(dis_thread);
					ThreadList.add(thread);
					
					
					thread.start();
					//grid_names.add( attributes[GRID_NAME].hashCode());
					/*
					 * all should wait for the first thread, but after that, everybody can start 
					 * submitting a job to the same file system.
					 */
					if( !grid_name_thread.containsKey(attributes[GRID_NAME].hashCode())){
						grid_name_thread.put(attributes[GRID_NAME].hashCode(), thread);
					}
					
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (InvalidProviderException e) {
					e.printStackTrace();
				} catch (ProviderMethodException e) {
					e.printStackTrace();
				}
					
					
			}else if( attributes[MIDDLEWARE].compareTo("ssh") == 0){
				//deploy job using for cpu_count * cpu_cores.
				SSHDiseminator diseminator = new SSHDiseminator(attributes[SERVER_NAME]
				                                                , attributes[GRID_NAME]
				                                                , this.PathOfPGAFLibs
				                                                ,  cpu_core_count
				                                                , this.JavaSocketUse
				                                                , Types.PGANodeRole.valueOf(attributes[ROLE])
				                                                , Log );
				
				SSHDiseminatorThread thread = new SSHDiseminatorThread(
							diseminator, 
							this.PathOfPGAFLibs, 
							SSHDiseminator.getPGAF_PATH(), 
							attributes[GRID_NAME], 
							computer_node_count,
							INSTALL_SEEDS,
							grid_name_thread.get( attributes[GRID_NAME].hashCode()) 
							);
			
				thread.start();
				
				if( !grid_name_thread.containsKey(attributes[GRID_NAME].hashCode())){
					grid_name_thread.put(attributes[GRID_NAME].hashCode(), thread);
				}
			}else if( attributes[MIDDLEWARE].compareTo("shared") == 0){
				data_sink_source_cpus = Integer.parseInt(attributes[CORES_PER_CPU]);
			}else{
				Log.log(Level.FINE, "Protocol " + attributes[MIDDLEWARE] + " not recoginized, skipping... " );
			}
		}
		
		Iterator<Thread> ot = ThreadList.iterator();
		while( ot.hasNext()){
			Thread t = ot.next();
			t.join();
		}
		
	}
	
	/*
	 * Returns the local source sink if the user set the local machine as the source and sink node.
	 */
	@Deprecated
	public BasicLayerInterface getModule(){
		if( PatternId != null){
			return LocalMachineNode.SourceSinkAvailableOnThisNode.get(PatternId);
		}else{
			return null;
		}
	}
	/**
	 * This method starts a node on the client computer.
	 * 
	 * Even though the client computer can work just to deploy the pattern, it is still
	 * require to have a p2p node in the client computer so that the pattern advertisements
	 * get created.
	 * 
	 * @throws Exception
	 */
	private Thread startClientComputerNode( ) throws Exception{
		String grid_node_name = null;
		int process_num = 0;
		Types.PGANodeRole role = null;
		//Iterator<String> it = AvailableServers.iterator();
		for( String str: AvailableServers){
		//while(it.hasNext()){
			//String str = it.next();
			String attributes[] = str.split(" ");
			
			String t = InetAddress.getLocalHost().getHostName();
			String s = attributes[SERVER_NAME];
			/*to lower case ensures Seeds work on (MS) Windows since Win host names are not case sensitive.*/
			if( attributes[SERVER_NAME].toLowerCase().compareTo(InetAddress.getLocalHost().getHostName().toLowerCase()) == 0 ){
				grid_node_name = attributes[GRID_NAME];
				process_num = Integer.parseInt(attributes[CORES_PER_CPU]);
				role = Types.PGANodeRole.valueOf( attributes[ROLE] );
			}
		}
		if( grid_node_name == null){
			
			Log.log(Level.WARNING, "localhost has to be in available server file in order to use this function.");
			
			throw new ServerNameNotInAvailableServerFile("localhost has to be in available server file in order to use this function.");
		}
		//UserAppClass.getName();
		
		LocalMachineNode = new Node(
				PathOfPGAFLibs
				, grid_node_name
				, Node.DEFAULT_JXTA_NODE_PORT
				,  process_num
				, this.JavaSocketUse
				, role );
		
		//Types.DataFlowRoll roll = Anchors.get("localhost");
		
		//Log.log(Level.FINER, "The DataFlowRoll is: " + roll.toString() );
		
		//LocalMachineNode.setDataFlowRollProperty( roll == null ? Types.DataFlowRoll.COMPUTE : roll );
		
		//if( RemoteModuleArguments != null){
		//	Node.setUserInitArgumets(RemoteModuleArguments);
		//}
		/*DataSinkSource node = new DataSinkSource(
				PathOfPGAFLibs + "/seed_file"
				, client_grid_node_name
				, SINK_SOURCE_PORT
				, this.UserApp
				);*/
		LocalMachineNode.setDebugMode(this.DebugMode);
		
		Log.log(Level.FINE,"run node");
		//LocalMachineNode.DebugPGANodeRoll = Types.PGANodeRoll.LEAFWORKER;
		//LocalMachineNode.DebugNetworkTypeRoll = Types.WanOrNat.NAT_NON_UPNP;
		
		//create a new spawn advertisement
		
		//SpawnPatternAdvertisement Advert = (SpawnPatternAdvertisement)
		//		AdvertisementFactory.newAdvertisement(SpawnPatternAdvertisement.getAdvertisementType());
		/*
		 * Create the Pattern Seed Advertisement.
		 * this advertisement will be published from the Worker.  The Worker does things that both
		 * the LeafNode and the DirectorRDV should perform for the user's module.
		 */
		/*Advert.setArguments(RemoteModuleArguments != null? RemoteModuleArguments : new String[]{""});
		Advert.setGridName(grid_node_name);
		PatternId = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		Advert.setPatternID(PatternId);
		Advert.setPatternClassName(UserAppClass.getName());
		DataFlowRoll anchor =  this.Anchors.get(grid_node_name);
		
		Advert.setSourceAnchor(grid_node_name); //by default it is 'this' grid node
		Advert.setSinkAnchor("");
		*
		 * the next code searches the list of anchors submitted by the user.  That information should override 
		 * the defaults. 
		 *
		Set<String> anchor_list = Anchors.keySet();
		Iterator<String> anchor_it = anchor_list.iterator();
		while( anchor_it.hasNext()){
			String host = anchor_it.next();
			Types.DataFlowRoll dat_flow = Anchors.get(host);
			switch( dat_flow){
			case SINK_SOURCE:
				Advert.setSourceAnchor(host);
				break;
			case SINK:
				Advert.setSinkAnchor(host);
				break;
			case SOURCE:
				Advert.setSourceAnchor(host);
				break;
			}
		}
		
		LocalMachineNode.setSeedPatternAdvertisement(Advert);
		*/
		
		Thread t = new Thread( new Runnable(){
			@Override
			public void run() {
				Thread.currentThread().setName("TheLocalNode");
				LocalMachineNode.runNode();
				Log.log(Level.FINE,"The local node is done");
			}
		} );
		t.start();
		LocalMachineNode.waitNodeStarted();
		return t;
	}
	
	
	
	public Node getLocalMachineNode() {
		return LocalMachineNode;
	}
	public void setLocalMachineNode(Node localMachineNode) {
		LocalMachineNode = localMachineNode;
	}
	/**
	 * It will wait until the remote processes are done with the users application before returning.
	 * This is so that the user can monitor the progress of the application even if no node is running
	 * on the local machine.
	 * @throws InterruptedException
	 */
	public void waitOnPGAFtoFinish() throws InterruptedException{
		
		Log.log(Level.FINE,"...threads size: " + ThreadList.size());
		Iterator<Thread> iter = ThreadList.iterator();
		while(iter.hasNext()){
			Thread thread = iter.next();
			thread.join();
			Log.log(Level.FINE,"thread...1");
		}
		//NodeThread.join();	
	}
	public static final int INSTALL_SEEDS = 22;
	public static final int RUN_SEEDS = 23;
	
	public static class SSHDiseminatorThread extends Thread{
		SSHDiseminator Diseminator;
		String SourceDir, DestDir, GridName;
		int ComputerNodeCount;
		Thread WaitOnThisThread;
		int Action;
		
		public SSHDiseminatorThread(SSHDiseminator dis
				, String source_dir
				, String dest_dir
				, String grid_name
				, int computer_node_count
				, int action
				, Thread t){
			SourceDir = source_dir;
			DestDir = dest_dir;
			GridName = grid_name;
			ComputerNodeCount = computer_node_count;
			WaitOnThisThread = t;
			Diseminator = dis;
			Action = action;
		}
		@Override
		public void run() {
			try {
				if(Action == INSTALL_SEEDS){
					long set = System.currentTimeMillis();
					if( WaitOnThisThread != null){
						Diseminator.Log.log(Level.FINE, "Waiting for the previous thread to DIE");
						WaitOnThisThread.join();
						Diseminator.Log.log(Level.FINE, "The previous thread is DEAD.  I can run now.");
					}
					/*
					 * if WaitOnThisThread is not null, it means another thread
					 * has already sent the folder. so this thread should 
					 * skip deleting and sending a new folder
					 */
					if( WaitOnThisThread == null){
						Diseminator.Log.log(Level.FINE,"deleting temp files on remote host");
						Diseminator.DeleteDirectory( DestDir );
						Diseminator.Log.log(Level.FINE,"waiting one second");
					}
					Diseminator.Log.log(Level.FINE,"setConection");
					
					if( WaitOnThisThread == null){	
						Diseminator.putDirector( DestDir  );
					}
					long time = System.currentTimeMillis() - set ;
					Diseminator.Log.log(Level.FINE, "Time taken for transfer: " + time);
				}else if( Action == RUN_SEEDS){
					for(int i = 0; i < ComputerNodeCount; i++){
						Diseminator.SubmitPeerJob( );
					}
				}
				
			} catch (Exception e ) {
				e.printStackTrace();
				StackTraceElement[] error_stack = e.getStackTrace();
				String err = "on Disseminator.Spawn()\n" + e.getMessage() + "\n"; 
						
				for(int i = 0; i < error_stack.length; i++){
					err += error_stack[i] +"\n";
				}
				Diseminator.Log.log(Level.SEVERE, err);
			}
			
		}
	}
	/**
	 * This class is taskED with starting multiple Diseminator threads to transfer the Shuttle foder to multple
	 * remote computers and start the framework.
	 * @author jfvillal
	 *
	 */
	public static class DiseminatorThread implements Runnable{
		GLOBUSDiseminator Dis;
		String SourceDir, DestDir, GridName;
		int ComputerNodeCount;
		Thread WaitOnThisThread;
		int Action;
		public DiseminatorThread( GLOBUSDiseminator dis
				, String source_dir
				, String dest_dir
				, String grid_name
				, int computer_node_count
				, int action
				, Thread t){
			Dis = dis;
			SourceDir = source_dir;
			DestDir = dest_dir;
			GridName = grid_name;
			ComputerNodeCount = computer_node_count;
			WaitOnThisThread = t;
			Action = action;
		}
		public void run() {
			try {
				if( Action == INSTALL_SEEDS){
					if( WaitOnThisThread != null){
						Dis.Log.log(Level.FINE, "Waiting for the previous thread to DIE");
						WaitOnThisThread.join();
						Dis.Log.log(Level.FINE, "The previous thread is DEAD.  I can run now.");
					}
					/*
					 * if WaitOnThisThread is not null, it means another thread
					 * has already sent the folder. so this thread should 
					 * skip deleting and sending a new folder
					 */
					if( WaitOnThisThread == null){
						Dis.Log.log(Level.FINE,"deleting temp files on remote host");
						Dis.DeleteDirectory( DestDir );
						if( !Dis.isDeletedFile() ){
							Dis.Log.log(Level.FINE,"file not deleted yet");
						}
						Dis.Log.log(Level.FINE,"waiting one second");
						try {
							Thread.sleep(750);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					Dis.Log.log(Level.FINE,"setConection");
					Dis.setConection(); //only needed for file manipulation
					Dis.Log.log(Level.FINE,"getHomeDir");
					Dis.getHomeDirectory(); //gets the home dir on remote server
					
					if( WaitOnThisThread == null){	
						Dis.putDirector( DestDir  );
					}
				}else if( Action == RUN_SEEDS){
					for(int i = 0; i < ComputerNodeCount; i++){
						Dis.SubmitPeerJob( );
					}
				}
				
			} catch (Exception e ) {
				e.printStackTrace();
				StackTraceElement[] error_stack = e.getStackTrace();
				String err = "on Disseminator.Spawn()\n" + e.getMessage() + "\n"; 
						
				for(int i = 0; i < error_stack.length; i++){
					err += error_stack[i] +"\n";
				}
				Dis.Log.log(Level.SEVERE, err);
			}
		}
		
	}

	public void setDebugMode( boolean b){
		GLOBUSDiseminator.setFrameworkDebug(b);
	}
	public boolean getDebugMode( ){
		return GLOBUSDiseminator.isFrameworkDebug();
	}
	public String getName() {
		return Name;
	}
	public void setName(String name){
		Name = name;
	}
	public boolean isJavaSocketUse() {
		return JavaSocketUse;
	}
	/**
	 * Sets the use of Java sockets.  Use of Java sockets increases the use of ports, which requires greater coordination with firewall policies.  
	 * The use of Java sockets also provides a performance boost over the JXSE sockets.
	 * @param javaSocketUse
	 */
	public void setJavaSocketUse(boolean javaSocketUse) {
		JavaSocketUse = javaSocketUse;
	}
	public boolean isNested() {
		return Nested;
	}
	public void setNested(boolean nested) {
		Nested = nested;
	}
	
}
