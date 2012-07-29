/*
 * Copyright 2009 Jeremy Villalobos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package edu.uncc.grid.pgaf.p2p;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.sbbi.upnp.messages.UPNPResponseException;
import edu.uncc.grid.pgaf.Pattern;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DependencyAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVCompetitionAdvertisement;
import edu.uncc.grid.pgaf.advertisement.NetworkInstructionAdvertisement;
import edu.uncc.grid.pgaf.advertisement.SpawnPatternAdvertisement;
import edu.uncc.grid.pgaf.deployment.Deployer;
import edu.uncc.grid.pgaf.deployment.Diseminator;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
/**
 *  
 * <p>
 * The Node Class is the class responsible for running the PGAFramework on the remote hosts.  This class
 * is initiated by globus after a job submit. The Deployer/Diseminator deploy this class on each host.
 * </p>
 * <p>
 * The Node class builds the Network.  It figures out if the node should be a DirectorRDV or a LeafWorker.
 * As documented elsewhere, the DirectorRDV may be a Rendezvous Node.  It will also help route pipes
 * that are behind NAT's.  That is work done on Mar 29, 2009.
 * </p>
 * <p>
 * Once the Node figures out the Network, it passes executing to classes LeafWorker or DirectorRDV.
 * 
 * The class uses NetworkDetective to figure out the external WAN ip if it exists, to figure out the LAN
 * IP, and to open a port on the routers if the router is UPNP compatible and the node is inside a NAT.
 * UPNP routers are common for users that are accessing the Grid from their home computers.  Institution
 * Computers are expected to be under a more strict port forwarding management.
 * 
 * </p>
 * @author Jeremy Villalobos
 */
/*
 * 
 * TODO 
 * *  Mar 29
 *  	*Add MultiModePipe													DONE
 *  	*Use thread instead of TCP comm on multi-core CPU's.				DONE
 *  Nov 25
 *  	*Create Threads that keep Directors and LeafWorkers communicated 	DONE
 *  	*Add Observer to the Debug system									DONE
 *  	*Test Debug System
 * 	Oct 28
 * 		*Create and test Director Advertising	DONE
 * 		*Test Debug/Info feature.  the workers should send info to Director. 
 * 			and directors should find the observer. (needs Observer Advertising, 
 * 			with type synkdirector and syncleaf		IN_PROGRESS
 * 		*Create command line observer.				DONE
 * 
 * 		--Longer term--
 * 		*Create Observer GUI
 * 		*Create Deployer (ssh assuming passwordless and Grid with Cog)
 */
public class Node {
	/*
	 * The normal competition takes too long
	 */
	public static final int CHECK_I_AM_WINNING_COUNT = 5; //sets the number of times to check if this node is winning the competition
		//to be a DirectorRDV,  each interval waits 100ms before it checks again.
	public static final int SLEEP_RDV_COMP = 100; //IN MILLISECONDS
	
	/**
	 * Timeout on waiting for Rendezvous.
	 */
	public static long TIMEOUT = 30000;
	/**
	 * The name of the host running this node
	 */ 
	public String Hostname;
	/*
	 * up to 50050 because port range has to be shared with globus
	 * Default ports for RDV can be 50000 or 50005 because we may need two RDV in one host for 
	 * debuggin
	 */
	/**
	 * Default port for the Node.  Nodes in the same host will increase the 
	 * Default port by one for each additional node.
	 */
	public static final int DEFAULT_JXTA_NODE_PORT = 50040; 
	/*Name also used to create the folder where the p2p network information is saved
	 * the information is saved to Node[port number] folder*/
	/**
	 * Name used for JXSE Configuration folder.
	 */
	public static String Name = "Seed";
	/**
	 * This node's port number
	 */
	public static int Port = DEFAULT_JXTA_NODE_PORT;
	/**
	 * This nodes Peer ID.
	 */
	public final static PeerID PID= IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
	/**
	 * JXSE Configuratin file.
	 */
	public static File ConfigurationFile; //jxta configuration file
	/**
	 * JXSE Network manager.
	 */
	NetworkManager NetManager = null;		//jxta network manager
	/**
	 * JXSE PeerGroup
	 */
	PeerGroup NetPeerGroup = null;			//jxta peer group
	/**
	 * JXSE Configurator
	 */
	static NetworkConfigurator NetConfig = null;
	/**
	 * Class log
	 */
	public static Logger Log = Logger.getLogger( "edu.uncc.grid.pgaf.p2p.FrameworkLogger");
	/**
	 * PGAF Network Type identifier.  Holds the type of network then Node was spawned in.
	 */
	public static Types.WanOrNat NetworkType;
	/**
	 * Holds the Basic layer user module class canonical name.
	 * With this information one can also extract the Advance Template 
	 * module
	 * nov 7 09 | moved to higher level using advertisement and idle nodes.
	 */
	//private String UserModuleClass;
	/**
	 * For benchmark tests.
	 */
	public long TimerStart;
	/**
	 * For the Debug Observer Messages.
	 */
	public static Message ReceivedMessage;
	/**
	 * This Node's Grid Name.  Used for routing.
	 */
	public static  String GridName;//all processors from a grid node are assigned the same
	
	public Types.PGANodeRole NodeRole;
	/**
	 * This Node's CpuCount.  Used in calculated the number of threads used for the User's 
	 * applicaiton.
	 */
	public static int CpuCount;
	
	//private static String[] UserInitArguments; moved to higher level using adverts and IN
	/**
	 * Debug variable.  This variable means that we are manually set the NodeRoll
	 * and NetworkRoll.
	 */
	private boolean DebugManuallySetNetworkSettings;
	/**
	 * Debug Node Roll (LeafWorker or DirectorRDV)
	 */
	public Types.PGANodeRole DebugPGANodeRole;
	/**
	 * Debug Network Type roll.
	 */
	//public Types.WanOrNat DebugNetworkTypeRoll;
		//grid name.
	/**
	 * This set if the Java Socket should be used instead of the Jxta Socket.  It also 
	 * enables port forwarding routines for NAT_UPNP nodes
	 */
	private boolean UseJavaSocketPort = false;
	
	
	private boolean StopNetwork = false;
	/*
	 * this adjust the number of seconds and number of checks that are done to the neighbors advertisements before
	 * determining that I am an winner.  It should be lower now that most of the nodes are excluded because of the 
	 * port number.
	*/
	final int NUMBER_OF_TIMES_CHECK_WINNING_STATUS = 4;
	/**
	 * <p>The net detective dianoses the network characterists for this node.  that includes testing for Internet access.
	 * Getting LAN address.  Getting the WAN address (either from the network interface or the address used by internet
	 * devices to reach this machine).  The class also open port on UPNP routers and will have a port open algorithm
	 * in the near future.</p>
	 * <p>
	 *The InternetGatewayDevice will try to control the routers if they are UPNP compliant.  the code for this is in 
	 *functions createPortForwardOnRouter() and closePortForwardOnRouter();</p> 
	 */
	private NetworkDetective NetDetective; 
	
	//private Types.DataFlowRoll DataFlowRollProperty; // moved to higher level using adverts and IN
	
	/**
	 * Shell option name
	 */
	public static final String ShellPortOption = "-port";
	public static final String ShellPGAFPathOption = "-pgaf-path";
	public static final String ShellGridNodeNameOption = "-grid-node-name";
	//public static final String ShellInterfaceNameOption = "-interface-name";
	public static final String ShellHelpOption = "-help";
	public static final String ShellCpuCountOption = "-cpu-count";
	//public static final String ShellUserRemoteArgumentsOption = "-user-remote-arguments";
	//public static final String ShellDataFlowRollOption = "-data-flow-roll";
	public static final String ShellJavaSocketOption = "-java-socket";
	
	public static final String ShellEmulateNetAttributesOption = "-set-net-attributes";
	public static final String ShellSetNodeRole = "-set-node-role";
	
	
	/**
	 * This is the port used by the DirectorRDV to provide a tunnel for other nodes.
	 */
	public static final int DEFAULT_TUNNEL_DISPATCHER_INTERNAL_PORT = 50090;
	/**
	 * This is the default port used to host a Socket Server (Dispatcher) for the Java Socket
	 */
	public static final int DEFAULT_JAVA_SOCKET_PORT = 50091;
	/**
	 * Port used for DirectorRDV's NAT rerouting routines
	 */
	private int TunnelJavaSocketPortInternal;
	/**
	 * Port used for Java Sockets.
	 */
	private int JavaSocketPortStart;
	
	/**
	 * Used to start the first pattern for a run.
	 */
	//private SpawnPatternAdvertisement SeedAdvert;
	/**
	 * point to the location for the jars, the seed file and the 
	 * Available servers file
	 */
	public static String PgafPath;
	
	public int ShareFileCompetition;
	/**
	 * Used to tell the Seeds object when the network is up. 
	 * This is to prevent from returning control to the user before the
	 * network is up.
	 */
	Boolean LocalNodeStarted = false;
	/*****
	 *  The constructor creates a new Node.
	 *  @param seed_file file with rendezvous nodes
	 *  @param grid_name the grid name of this node
	 *  @param port port on which to listen for communicaiton
	 *  @param workpool Users Workpool module.  leave null if getting the module over the network
	 *  @throws Exception 
	 */
	public Node(String pgaf_path, String grid_name, int port, 
			 int cpu_count,  boolean java_socket
			, Types.PGANodeRole role) throws Exception{
		this();
		this.SourceSinkAvailableOnThisNode = Collections.synchronizedMap(new HashMap<PipeID, BasicLayerInterface>());
		this.PgafPath = pgaf_path;
		GridName = grid_name;
		Port = port;
		this.UseJavaSocketPort = java_socket;
		CpuCount = cpu_count;
		NodeRole = role;
		this.startNetwork();
	}
	
	synchronized void NodeHasStarted(){
		LocalNodeStarted = true;
		notify();
	}
	public synchronized void waitNodeStarted() throws InterruptedException{
		if( !LocalNodeStarted ){
			Node.getLog().log(Level.FINER, " waiting for node jxta peer to start " );
			wait();
			Node.getLog().log(Level.FINER, " node has started, I can move on" );
		}
	}
	public static String getLogFolder(){
		return ConfigurationFile.getAbsolutePath();
	}
	private Node() throws SecurityException, IOException{
		this.TimerStart = System.currentTimeMillis();
		InetAddress addr = InetAddress.getLocalHost();
		Hostname = addr.getHostName();
		
		String SeedsLogFolder = "." + System.getProperty("file.separator") + Name + "." + Hostname;
		ConfigurationFile = new File(SeedsLogFolder);
		
		/**
		 * This is just for debug.  The program should delete at the end, not a the begining.
		 */
		RecursiveDelete(this.ConfigurationFile);
		
		this.ConfigurationFile.mkdir();
		
		//Setup the logger
		FileHandler handler = new FileHandler(ConfigurationFile.toString()+ System.getProperty("file.separator") + "PGAFLogger.log" );
		getLog().addHandler(handler);
		getLog().setLevel(Level.ALL);
		SimpleFormatter formatter = new SimpleFormatter();
		
	    handler.setFormatter(formatter);
	    
	    this.ShareFileCompetition = 0;
	}
	
	/**
	 * This is the constructor called from the shell bootstraper
	 * @param args
	 * @throws IOException
	 * @throws PeerGroupException
	 */
	public Node(String args[]) throws IOException, PeerGroupException{
		this();
		
		/**
		 * 
		 * Get all the arguments in order
		 * 
		 */
	
		this.SourceSinkAvailableOnThisNode = Collections.synchronizedMap(new HashMap<PipeID, BasicLayerInterface>());
		Port = -1; 
		CpuCount = -1;
		NetworkType = Types.WanOrNat.UNKNOWN;
		if( args.length > 0){
			for(int i = 0; i < args.length; i++){
				String str = args[i];
				if( str.compareTo( ShellPortOption ) == 0){
					Port = Integer.parseInt(args[++i]);
					Log.log(Level.FINE, " port : " + Port);
				}else
				if( str.compareTo(ShellPGAFPathOption) == 0){
					PgafPath = args[++i];
					Log.log(Level.FINE, " pgaf path : " + PgafPath);
				}else
				if( str.compareTo(ShellGridNodeNameOption) == 0){
					GridName = args[++i];
					Log.log(Level.FINE, "grid name: " + GridName);
				}else
				if( str.compareTo(ShellHelpOption) == 0 || str.compareTo("-h") == 0){
					printHelp();
					System.exit(1);
				}else
				if( str.compareTo(ShellCpuCountOption) == 0){
					CpuCount = Integer.parseInt(args[++i]);
					Log.log(Level.FINE, "cpu count: " + CpuCount );
				}else if( str.compareTo(Node.ShellJavaSocketOption) == 0){
					this.UseJavaSocketPort = true;
				}else if( str.equals(Node.ShellSetNodeRole)){
					NodeRole = Types.PGANodeRole.valueOf(args[++i]);
				}
				else if(str.compareTo( Node.ShellEmulateNetAttributesOption) == 0){
					this.DebugPGANodeRole = Types.PGANodeRole.valueOf(args[++i]);
					
					NetDetective = new NetDetectiveEmulator(Types.WanOrNat.valueOf(args[++i]) );
					NetDetective.setWANAddress(args[++i]);
					NetDetective.setLANAddress(args[++i]);
					this.TunnelJavaSocketPortInternal = Integer.parseInt(args[++i]);
					this.JavaSocketPortStart = Integer.parseInt(args[++i]);
					DebugManuallySetNetworkSettings = true;
					getLog().log(Level.FINE, "debug node roll: " + this.DebugPGANodeRole.toString());
				}
				
				
			}
		}
		
		Log.log(Level.INFO, "Using socket type: " + ( this.UseJavaSocketPort? " JavaSocket " : " JXTASocket " ) );
		
		if( PgafPath == null || this.GridName == null || CpuCount == -1
				|| NodeRole == null){
			System.out.println(" One of the required arguments was not provided !");
			Log.log(Level.SEVERE, " One of the required arguments was not provided !");
			printHelp();
			System.exit(1);
		}
		if( Port == -1){
			Port = Node.DEFAULT_JXTA_NODE_PORT; //default port 
		}
		this.startNetwork();
	}
	/**
	 * If multiple computers in a room use a share file system, adding the last 6 digits of the 
	 * Node's ID will create a JXTA folder that is original, therefore no weird conflicts in 
	 * advertisement management will arise.  This function just return the last 6 digits of 
	 * a String.
	 * @param str
	 * @return
	 */
	public static String getPeerIDEndingHex(String str){
		int begining = str.length() - 6;
		int end = str.length();
		return str.substring(begining, end);
	}
	/**
	 * 
	 * This variable is set to true if the algorithms used to see the place of this node
	 * in the network either find themselves inside a restricted network, or if they 
	 * encounter an error.  Examples are an UnknownHostException, IOException, and
	 * other error, particularly in the NetworkDetective exeption.
	 * 
	 */
	boolean RestrictedNetworkDetected;
	
	/**
	 * 
	 * Start the Peer-to-Peer network
	 * @throws IOException 
	 * @throws PeerGroupException 
	 * 
	 */
	private void startNetwork() throws IOException, PeerGroupException{
		//Set Network (JXSE) Defaults
		NetManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, Name
				, ConfigurationFile.toURI());
		NetManager.setUseDefaultSeeds(false);
		NetConfig = NetManager.getConfigurator();
		NetConfig.clearRendezvousSeeds();
		NetConfig.setTcpPort(Port);
		NetConfig.setTcpEnabled(true);
		NetConfig.setTcpIncoming(true);
		NetConfig.setTcpOutgoing(true);
		NetConfig.setUseMulticast(true);
		NetConfig.setUseMulticast(false);
		
		NetConfig.setPeerID(PID);
		
		NetConfig.setTcpStartPort(50000);
		NetConfig.setTcpEndPort(50100);
		/**
		 * Start of LAN/NAT related settings
		 */
		
		/**TODO Add a NetDetective child class that emulates the network characteristics found by Netdetective
		 * there should be a shell option to get this setting.  -set-net-attributes WAN LAN port
		 * The create an if here that will redirect to the controled environment NetDetective class*/
		
		/*
		 * if we are in debug mode, we already create an emulated net detective in the previous code. 
		 * if we are not in debug mode, run the normal net detective.
		 */
		if( !DebugManuallySetNetworkSettings){
			getLog().log(Level.FINE, "NO DEBUG MODE, RUNNING PRODUCTION MODE.");
			this.TunnelJavaSocketPortInternal = Node.DEFAULT_TUNNEL_DISPATCHER_INTERNAL_PORT;
			this.JavaSocketPortStart = Node.DEFAULT_JAVA_SOCKET_PORT;
			
			NetDetective = new NetworkDetective();
		}
		/**
		 * if we are in debug mode and DebugNetworkTypeRoll is set, then use that 
		 * as the NetworkTypeRoll
		 */
		/*if( this.DebugNetworkTypeRoll != null){
			NetDetective.isInsideaNATNetwork(); //this finds out the WAN address and the LAN address
				if( DebugNetworkTypeRoll == Types.WanOrNat.NAT_NON_UPNP){
					/// set settings same as NAT
					NetDetective.getWANIP();
					NetConfig.setHttpEnabled(false);
		            NetConfig.setTcpStartPort(-1);
		            NetConfig.setTcpEndPort(-1);
		            NetConfig.setTcpInterfaceAddress(NetDetective.getLANAddress() + ":" + Port );
		            if( NetDetective.getWANAddress() != null){
		            	NetConfig.setTcpPublicAddress(NetDetective.getWANAddress() + ":" + Port, false);
		            }else{
		            	NetConfig.setTcpPublicAddress(NetDetective.getLANAddress() + ":" + Port, false);
		            }
		           
		            this.NetworkType = Types.WanOrNat.NAT_NON_UPNP;
		            
				}else{
					this.NetworkType = DebugNetworkTypeRoll;
				}
		}else{*/
		if( NetDetective.isInsideaNATNetwork() ){
			/** if it is inside a NAT, we need to try open a port on the local gateway
			 * if that fails, we should inform the node so that it routes messages through
			 * the Director RDV
			 * 
			 * Feb 25, these network surveying algorithms are getting updated so that the 
			 * framework is able to work on restricted networks, or even no network at all
			 * effectively working on a single workstation.
			 **/
			try{
				NetConfig.setHttpEnabled(false);
	            NetConfig.setTcpStartPort(-1);
	            NetConfig.setTcpEndPort(-1);
	            NetConfig.setTcpInterfaceAddress(NetDetective.getLANAddress() + ":" + Port );
	            
	            //This next function may fail on restricted networks
				NetDetective.findOutWANIP();
				
	            if( NetDetective.getWANAddress() != null){
	            	NetConfig.setTcpPublicAddress(NetDetective.getWANAddress() + ":" + Port, false);
	            }else{
	            	NetConfig.setTcpPublicAddress(NetDetective.getLANAddress() + ":" + Port, false);
	            }
	            
	            //open port if compatible UNPN exists
	            if( NetDetective.createPortForwardOnRouter(Port)){
	            	this.NetworkType = Types.WanOrNat.NAT_UPNP;
	            }else{
	            	this.NetworkType = Types.WanOrNat.NAT_NON_UPNP;
	            }
	            
			}catch( java.net.UnknownHostException e){
				Node.getLog().log(Level.WARNING, " We probably don't have connectivity to the Internet !!!");
				this.RestrictedNetworkDetected = true;
				this.NetworkType = Types.WanOrNat.NAT_NON_UPNP;
				
			} /*catch (UPNPResponseException e) {
				Node.getLog().log(Level.WARNING, " We probably don't have connectivity to the Internet." +
						" \nA UPNP server was not found, or a port was not opened succesfully. ");
				this.RestrictedNetworkDetected = true;
				
				
				this.NetworkType = Types.WanOrNat.NAT_NON_UPNP;
				e.printStackTrace();
			}*/
            
		}else{
			this.NetworkType = Types.WanOrNat.WAN;
			
		}
		//}
		
		//NetConfig.setTcpInterfaceAddress(NetDetective.getLANAddress());
		
		/**
		 * End of LAN/NAT related settings
		 */
		
		NetConfig.save();
		
		
	    //new XMLFormatter()
	    //Set seeds
		LoadSeedRdvs( PgafPath + Diseminator.RDV_SEED_FILE, NetConfig, NetDetective );
        //Register Custom Advertisements
		registerAdvertisements();
		/*AdvertisementFactory.registerAdvertisementInstance(
				DirectorRDVAdvertisement.getAdvertisementType(), 
				new DirectorRDVAdvertisement.Instantiator()
				);*/
				
		//Let the show begin!  delete this sleep and see if it affects anything
		//if so, write it here.
		NetPeerGroup = NetManager.startNetwork();
		
		//for(int i = 0 ; i < 10; i++){
		//	Thread.sleep(100);
		//}
		Node.getLog().log(
				Level.FINEST, " NetworkType: " 
				+ this.getNetworkType().toString() 
				+ " lan addr: " + (NetDetective.getLANAddress() == null ? "null" : NetDetective.getLANAddress() )
				+ " wan addr: " + (NetDetective.getWANAddress() == null ? "null" : NetDetective.getWANAddress() )
				);
		//Node.getLog().log(Level.FINE, " setting " + NetDetective.getLANAddress() + " as the interface " );
	}
	/**
	 * Registers all the advertisements used by Seeds.
	 */
	public static void registerAdvertisements(){
		AdvertisementFactory.registerAdvertisementInstance(
				DirectorRDVCompetitionAdvertisement.getAdvertisementType()
				, new DirectorRDVCompetitionAdvertisement.Instantiator() 	);
		AdvertisementFactory.registerAdvertisementInstance(DirectorRDVAdvertisement.getAdvertisementType()
				, new DirectorRDVAdvertisement.Instantiator());
		AdvertisementFactory.registerAdvertisementInstance( DataLinkAdvertisement.getAdvertisementType()
				, new DataLinkAdvertisement.Instantiator()	);
		
		AdvertisementFactory.registerAdvertisementInstance( SpawnPatternAdvertisement.getAdvertisementType()
				, new SpawnPatternAdvertisement.Instantiator()	);
		
		AdvertisementFactory.registerAdvertisementInstance( DependencyAdvertisement.getAdvertisementType()
				, new DependencyAdvertisement.Instantiator()	);
		
		AdvertisementFactory.registerAdvertisementInstance(
				NetworkInstructionAdvertisement.getAdvertisementType()
				, new NetworkInstructionAdvertisement.Instantiator());
	}
	public double getUptime(){
		return (double)(System.currentTimeMillis() - this.TimerStart ) / 1000.0;
	}
	/**
	 * Adds seeds from file seed_file to the NetworkConfigurator object
	 * the input file should be a text file with format:
	 * e.g
	 * tcp://coit-grid02.uncc.edu:9701
	 * tcp://littleblue.homelinux.org:9701
	 * 
	 * Only use ip addresseses, an algorithm will prevent the node from adding itselve to the seeds.
	 * 
	 * @param seed_file
	 * @param net_config
	 * @throws IOException
	 */
	private void LoadSeedRdvs(String seed_file, NetworkConfigurator net_config, NetworkDetective detective) throws IOException {
		if( seed_file == null){
			return;
		}else{
			Set<String> list = new HashSet<String>();
			FileInputStream file = new FileInputStream(seed_file);
			DataInputStream stream = new DataInputStream(file);
			while( stream.available() != 0){
				String line = stream.readLine();
				getLog().log(Level.FINEST, "*** line read from file " + seed_file + " line: " + line);
				//if( detective.getLANAddress().compareTo( line.split("/")[2].split(":")[0]) != 0 
				//		&&
				//	detective.getWANAddress().compareTo( line.split("/")[2].split(":")[0]) != 0
				//){
					list.add(line);
				//}
			}
			file.close();
			stream.close();
			
			net_config.setRendezvousSeeds(list);
		}
	}
	/**
	 * Outputs text help information to the command line.
	 */
	public static void printHelp(){
		String help =
			 "\n"
			+  "Usage: java edu.uncc.grid.pgaf.p2p.Node [OPTION]...                 \n"
			+ "\n"
			+ "Use java -classpath $LIB_PATH edu.uncc.grid.pgaf.p2p.Node to include\n"
			+ "  classes and packages not included in the default classpath        \n"
			+ "\n"
			+ "\n"
			+ "The following options are mandatory:								   \n"
			+ "     "+Node.ShellPGAFPathOption+"          The file that has the JXSE Rendezvous list \n"
			+ "     "+Node.ShellGridNodeNameOption+"     The name assigned to the grid 			   \n"
			+ "     "+Node.ShellCpuCountOption+"          The number of cores in the computer 	   \n"
			+ "     "+Node.ShellSetNodeRole +"     Sets the roll for this node  (DIRECTOR_RDV, LEAFWORKER)          \n"
			+ "\n"
			+ "The following options are not mandatory                             \n"
			+ "     "+Node.ShellPortOption+"               The port to be used by the node            \n"
			+ "     "+Node.ShellHelpOption+"               Prints this text                           \n"
			+ "     "+Node.ShellJavaSocketOption+"        Enables the use of Java Sockets         \n"
			+ "\n"
			+ "---------------------Debug Option-----------------------------------\n"
			+ "Setting the debug options will set the node to debug_mode, which    \n"
			+ "  makes the log data more verbose.                                  \n"
			+ "     "+Node.ShellEmulateNetAttributesOption+"    Sets emulation mode for debugging and to measure overhead.\n"
			+ "                        There are multiple attributes on this option, and all of them are required.  This options \n"
			+ "                        are necesary to debug or measure the Java Socket implementation:\n"
			+ "                        NodeRoll = sets if the node is a LeafWorker or a DirectorRDV.\n"
			+ "                        NetRoll = Weather the node is WAN, NAT_UPNP, or NAT_NON_UPNP.  check Types class for\n"
			+ "                                  the complete options.                                                      \n"
			+ "                        WAN address = The addressed used for WAN address for this node                       \n"
			+ "                        LAN address = The LAN address for this node.                                         \n"
			+ "                        Port External = Java Socket port used by DirectorRDV to host dispatcher for a NAT Client\n"
			+ "                        Port Internal = Java Socket port used by DirectorRDV for its Tunnel Dispatcher         \n"
			+ "                        Port Java     = Java Socket port used by all the nodes that communicate using Java Sockets";
		
		System.out.println( help);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			Node node = new Node( args );
			//on sinksource provide the workpool object
			
			Node.getLog().setLevel(Level.ALL);
			
			/**
			 *	setting debugging node
			 * 
			 * */
			if( node.DebugManuallySetNetworkSettings ){
				Node.getLog().setLevel(Level.ALL);
				Node.getLog().log(Level.INFO, 
						"\n" +
						"*********************************************************************************\n" +
						"******** RUNNING ON DEBUG MODE, NETWORK CHARACTERISTICS ARE EMULATED!! **********\n" +
						"*********************************************************************************\n");
			}
			/**
			 * Turn over execution to the Node Object
			 */
			node.runNode();
			
			Node.getLog().log(Level.INFO, "Node Main: runNode() is done.");
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * Returns the path to the shuttle folder for the local machine
	 */
	public static String getShuttleFolderPath(){
		return PgafPath;
	}
	
	/**
	 * 
	 * <p>
	 * The code inside the function organizes the nodes.  It determines if they 
	 * should be a LeafWorker, a DirectorRDV with EDGE, or a 
	 * DirectorRDV with Rendezvous.
	 * </p>
	 * <p>
	 * The processes is technically dependent on the popular networks we use today
	 * so it is necesary to explain what all the ifs do.
	 * </p>
	 * <p>
	 * We first separate the types of networks into NAT and WAN (Types.WanOrNat enum).
	 *   isInsideaNATNetwork() checks this by looking at the network interfaces and checking
	 *   that we have an address that is not 192.168.  The algorithm is simple, and it can
	 *   be made to better comply with standards that define Internet IP's and private network
	 *   addresses.
	 * </p>  
	 * <p>
	 * If we are on a WAN, then we can be a DirectoryRDV.  But we should also check to make
	 *   sure not all the nodes from this GridNode decide to be DirectorRDV.  To do that, the 
	 *   WAN nodes can compete to elect a single Rendezvous node.  we also make sure that only
	 *   one process per CPU competes, this is done by only allowing process runnin on 
	 *   DEFAULT_PORT (50000), 50001 and above are excluded.
	 *  </p>
	 *  <p>
	 * If we are on a LAN, that means nobody in the network can be Rendezvous, and they will 
	 * 	 depend on external Rendezvous to communicate. We check to see if we can connect to 
	 *   Rendezvous on another GridNode.  If we can, we can compete to be a DirectorRDV as
	 *   EDGE; else, we have to be an EDGE.
	 *   </p>
	 *   <p>
	 * In the end, we should have one DirectorRDV per GridNode, and all other nodes will be
	 *   EDGE's (LeafWorker).  The algorithm assumes somebody will be a WAN RDV, although 
	 *   process communicates OK in a LAN using multi-casting and RDV is not required.
	 *   </p>
	 *   <p>
	 * The DirectorRDV is somewhat like a Relay from JXSE, but it is made more versatile since 
	 *   The Relay idea take a performance told for its use of hypertext communication
	 *    and treats the peers that connect to it mostly
	 *   as file receivers, since most of the P2P idea is around distribution of files.  For 
	 *   our purpose, the Relay does not provide enough versatility to communicate.
	 * The DirectorRDV provides a route service.  The 
	 *   gateway has two goals, one is security.  The program can work with unencrypted 
	 *   communication within a GridNode, but use encryption for inter-GridNode communication
	 *   (not implemented).  The other service is to provides some
	 *   bandwidth-latency tolerant features such as compressing data between GridNodes to 
	 *   save bandwidth, or to channel broadcast messages so that the same message is sent 
	 *   only once from a node on GridNode One  to multiple peers on GridNode two
	 *   (not implemented).
	 *   </p>
	 */
	 /*   
	 *   On Dec 17, we added the data_sink_source part, it creaes a new Node handler like
	 *   LeafWorker and DirectorRDV that takes care of taking the information out of the
	 *   users program and into it.  Theoretically, the source and the sink could be 
	 *   different nodes, but since this idea has no academic significance, I am coding 
	 *   the easier option.
	 */
	public void runNode(){
		if( Seeds.SeedsDeployer == null){
			try {
				Seeds.SeedsDeployer = new Deployer( Node.getShuttleFolderPath());
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.NodeHasStarted();
		try {
			if( this.DebugPGANodeRole != null){
				/**
				 * This debug mode allows me to set LeafWorker or DirectorRDV (with or without RDV)
				 * from the command line.
				 */
				
				if( DebugPGANodeRole == Types.PGANodeRole.DIRECTOR_RDV) {
					/*if( this.DebugNetworkTypeRoll != null){
						getLog().log(Level.FINE, "DEBUG MODE:  Director RDV" );
						if( this.DebugNetworkTypeRoll == Types.WanOrNat.NAT_NON_UPNP){
							DirectorRDV director = new DirectorRDV( this, false );
							director.runNode();
						}else{
							DirectorRDV director = new DirectorRDV( this, true );
							director.runNode();
						}
					}else{*/
						//we assume WAN setup
						DirectorRDV director = new DirectorRDV( this, true );
						director.runNode();
					//}
				}else if( DebugPGANodeRole == Types.PGANodeRole.LEAFWORKER){
					getLog().log(Level.FINE, " DEBUG MODE: I am a LeafWorker " );
					LeafWorker leaf = new LeafWorker(this);
					leaf.runNode();
				}	 
				
			}else{
				if( NodeRole == Types.PGANodeRole.DIRECTOR_RDV){
					//start director rdv
					DirectorRDV director = new DirectorRDV(this,true);
					director.runNode();
				}else{
					//start node.
					//findDirectorCapableLocalNode();
					LeafWorker worker = new LeafWorker(this );
					worker.runNode();
				}
			}
			getLog().log(Level.FINE, "\n>>Framework is Exiting with No Error Ran for: " + getUptime());
			
			this.StopNode();
			
			getLog().log(Level.FINE, "\n>>Shuting down network");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	//TODO put this on top
	
	/**
	 * 
	 * Feb 7
	 * TODO I need a function that creates a pattern that has no father
	 * a code-flow pattern.
	 * 
	 * 
	 */
	
	/**
	 * Used to kee track of the modules on this node for shared-memory-like interaction with 
	 * them by the user.
	 */
	public Map<PipeID, BasicLayerInterface> SourceSinkAvailableOnThisNode;
	
	
	SpawnPatternAdvertisement CurrentPattern = null;
	public SpawnPatternAdvertisement getCurrentPatternAdvert(){
		return CurrentPattern;
	}
	/**
	 * feb 23.  The nestedPattern is good enough to start any type of patter, even the first pattern.
	 * So I changed the name to spawnPattern and it will used even if there only is one pattern 
	 * being started.
	 * 
	 * The method will advertise the new module to the idle nodes.<br>
	 * 
	 * The object submitted to this function will be used for the sink and source if the
	 * default values for the sink and source are used (null, null).  However, 
	 * if the source and sink are placed in other servers using anchors, this will not be
	 * the case.  This should be ovious, since the source and sink would be on another
	 * server, an this framework does not uses RMI to confuse the user.
	 * 
	 * @param mod  the user's module 
	 * @param source this is the sink_source righ now (this is a grid node name)
	 * @param sink this is not used jet (this is a grid node name)
	 * @throws IOException 
	 */
	public PipeID spawnPattern( Pattern pattern  ) throws IOException{
		SpawnPatternAdvertisement Advert = (SpawnPatternAdvertisement)
				AdvertisementFactory.newAdvertisement(SpawnPatternAdvertisement.getAdvertisementType());
		/*
		 * Create the Pattern Seed Advertisement.
		 * this advertisement will be published from the Worker.  The Worker does things that both
		 * the LeafNode and the DirectorRDV should perform for the user's module.
		 */
		/*Arguments are not required, if not given, set to dummy array.*/
		Advert.setArguments(pattern.getPatternArguments() != null? pattern.getPatternArguments() : new String[]{""});
		Advert.setGridName(Node.getGridName());
		/*create a new pattern id
		 * only here should you see a new pattern id created.  Every other piece of code should have a given pattern id.*/
		PipeID pattern_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		Advert.setPatternID(pattern_id);
		Advert.setPatternClassName( pattern.getPatternModule().getClass().getName() );
		
		if( pattern.getPatternAnchor() == null){
			Advert.setSourceAnchor(InetAddress.getLocalHost().getHostName()); //by default it is 'this' host
		}else{
			if( pattern.getPatternAnchor().getAnchorDFR() != Types.DataFlowRole.SINK_SOURCE){
				Advert.setSourceAnchor(InetAddress.getLocalHost().getHostName()); //by default it is 'this' host
			}else{
				Advert.setSourceAnchor(pattern.getPatternAnchor().getHostname());
			}
		}
		
		this.SourceSinkAvailableOnThisNode.put(pattern_id, pattern.getPatternModule());
		
		DiscoveryService discovery_service = getNetPeerGroup().getDiscoveryService();
		
		discovery_service.publish(Advert);
		//DONT PUBLISH EXPIRING ADVERTISEMENT
		//System.out.println( Advert.toString() );
		try{
			discovery_service.remotePublish(Advert);
		}catch( NullPointerException e ){
			//TODO this is oviously a work-around.  the stack trace is this 
			
			/*java.lang.NullPointerException
			at net.jxta.impl.rendezvous.rdv.RdvPeerRdvService.walk(RdvPeerRdvService.java:701)
			at net.jxta.impl.rendezvous.RendezVousServiceImpl.walk(RendezVousServiceImpl.java:696)
			at net.jxta.impl.rendezvous.RendezVousServiceInterface.walk(RendezVousServiceInterface.java:288)
			at net.jxta.impl.resolver.ResolverServiceImpl.propagateResponse(ResolverServiceImpl.java:904)
			at net.jxta.impl.resolver.ResolverServiceImpl.sendResponse(ResolverServiceImpl.java:576)
			at net.jxta.impl.resolver.ResolverServiceInterface.sendResponse(ResolverServiceInterface.java:176)
			at net.jxta.impl.discovery.DiscoveryServiceImpl.remotePublish(DiscoveryServiceImpl.java:1196)
			at net.jxta.impl.discovery.DiscoveryServiceImpl.remotePublish(DiscoveryServiceImpl.java:1133)
			at net.jxta.impl.discovery.DiscoveryServiceImpl.remotePublish(DiscoveryServiceImpl.java:768)
			at net.jxta.impl.discovery.DiscoveryServiceInterface.remotePublish(DiscoveryServiceInterface.java:220)
			at edu.uncc.grid.pgaf.p2p.Node.spawnPattern(Node.java:954)
			at edu.uncc.grid.pgaf.Seeds.startPattern(Seeds.java:54)
			at edu.uncc.grid.RunAllToAllExample.main(RunAllToAllExample.java:27)*/
			
			//when you have time, please attach the JXSE source code to the project and follow the rabbit
			//At least for mult-core runs, ignoring the exception works.
			
			e.printStackTrace();
		}
		CurrentPattern = Advert;
		
		return pattern_id;
		/*
		 * Nov 8, 09
		 * OK, the new pattern is published.  Now, it is up to the advanced layer interfaces to 
		 * share the available nodes.  In the future, we will have the ability to spawn new 
		 * nodes.  For now, the initial number of nodes will be constant.  no nodes can be added
		 * , and the nodes that don't have a pattern should keep running idling.  only when the 
		 * primary pattern is done do we shutdown all the nodes.
		 * 
		 * Feb 23.  Only when the user turn off the network, do we stop the network.  Later, there
		 * will be other load-balancing features.
		 */
	}
	
	/**
	 * Nov 8, 2009
	 * This function retuns a Deployer that can be used to spawn new nodes.
	 * 
	 * This function will be usefull, namely the spawn method when the proxy delegation procedure is 
	 * incorporated into the framework.
	 * 
	 * TODO Add proxy delegation to Deployer.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public Deployer getDeployer(BasicLayerInterface mod ) throws SecurityException, IOException{
		Deployer nested_deployer = new Deployer(this.PgafPath);
		return nested_deployer;
	}
	/**
	* Returns true if it find a Rendezvous.
	* @return
	*/
	private boolean haveRDVSeedAvailability() {
		boolean ans = false;
		if( NetManager.waitForRendezvousConnection(TIMEOUT)){
			ans = true;
		}	
		return ans;
	}
	/**
	 * Finds a local node that can be a DirectorRDV Rendezvous.  return false if it does not find it.
	 * @return
	 * @throws InterruptedException
	 */
	private boolean findDirectorCapableLocalNode() throws InterruptedException {
		//catch DirectorRDV advertisement
		boolean ans = false;
		DirectorRDVAdvertisementListener listener = new DirectorRDVAdvertisementListener(this);
		DiscoveryService disserv = NetPeerGroup.getDiscoveryService();
		/*this loop waits 10 * 60 600 milliseconds*/
		for( int i = 0; i < 10; i++){
			disserv.getRemoteAdvertisements(
				null, DiscoveryService.ADV,
                DirectorRDVAdvertisement.GridNodeNameTag, this.GridName
                , 4, listener);
			Thread.sleep(60);
		}
		if(listener.getWasCalled()){
			ans = true;
		}else{
			disserv.removeDiscoveryListener(listener);
		}
		return ans;
	}
	/**
	 * Returns the JXSE {@link NetworkManager} object for this node.
	 * @return
	 */
	public NetworkManager getNetManager(){
		return NetManager;
	}
	/**
	 * Returns the JXSE {@link PeerGroup} object for this node.
	 * @return
	 */
	public PeerGroup getNetPeerGroup(){
		return NetPeerGroup;
	}
	/**
	 * Returns the JXSE {@link NetworkConfigurator} for this node.
	 * @return
	 */
	public NetworkConfigurator getNetConfig(){
		return NetConfig;
	}
	/**
	 * Listens for Advertisements only during the initialization of the network to.  add 
	 * the local DirectorRDV Rendezvous to the list of Rendezvous seeds.
	 */
	public static class DirectorRDVAdvertisementListener implements DiscoveryListener{
		boolean WasCalled;
		Node ParentNode;
		public DirectorRDVAdvertisementListener( Node node ){
			WasCalled = false;
			ParentNode = node;
		}
		/**
		 * Scans advertisments for the {@link DirectorRDVAdvertisment}
		 */
		public void discoveryEvent(DiscoveryEvent arg0) {
			DiscoveryResponseMsg TheDiscoveryResponseMsg = arg0.getResponse();
        
	        if (TheDiscoveryResponseMsg!=null) {
	            
	            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
	         
	            while (TheEnumeration.hasMoreElements()) {
	            	Advertisement TheAdv = TheEnumeration.nextElement();
	            	if ( TheAdv instanceof DirectorRDVAdvertisement){
	 	            	DirectorRDVAdvertisement temp = (DirectorRDVAdvertisement) TheAdv;
	 	            	WasCalled = true;
	 	            	//should get the URI from the advertisement and put it as part 
	 	            	//of the seeds
	 	            	ParentNode.getNetConfig().addSeedRendezvous(
	 	            			URI.create("tcp://localhost:50000"));
	 	            }
	 	            
	                
	            }
	            
	        }
		}
		public boolean getWasCalled(){
			return WasCalled;
		} 
	}
	/**This class listens for advertisement with the random number of the other nodes*/
	/*
	public static class DirectorRDVCompetitionAdvertisementListener implements DiscoveryListener{
		boolean WasCalled;
		Node ParentNode;
		Integer Rand;
		public int TotalNeighbors;
		static boolean IamWinning = true;
		public DirectorRDVCompetitionAdvertisementListener( Node node, Integer rand ){
			WasCalled = false;
			ParentNode = node;
			Rand = rand;
			TotalNeighbors = 0;
		}
		public void discoveryEvent(DiscoveryEvent arg0) {
			DiscoveryResponseMsg TheDiscoveryResponseMsg = arg0.getResponse();
	        if (TheDiscoveryResponseMsg!=null) {
	            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
	         
	            while (TheEnumeration.hasMoreElements()) {
	            	Advertisement TheAdv = TheEnumeration.nextElement();
	            	
	            	if ( TheAdv instanceof DirectorRDVCompetitionAdvertisement){
	 	            	DirectorRDVCompetitionAdvertisement temp = (DirectorRDVCompetitionAdvertisement) TheAdv;
	 	            	String str ="";
	 	            	if( temp.getPeerID().compareTo(ParentNode.PID.toString()) == 0){
	 	            		getLog().log(Level.FINEST, "**Ignoring my own advertisement **");
	 	            		
	 	            	}else if( 
	 	            			temp.getGridName().compareTo(ParentNode.getGridName()) 		== 0 	&&
	 	            			temp.getNetworkType().compareTo(ParentNode.getNetworkType()) 	== 0 	){
	 	            		str = "Got Advertisement from neightbor.";
		 	            	WasCalled = true;
		 	            	//this piece it to give one process the Director status if there are 
		 	            	//multiple nodes running on a multi-core system
		 	            	//if we both have the seme hostname, and I don't have the DEFAULT_PORT
		 	            	//then I lost.
		 	            	++TotalNeighbors;
		 	            	if( temp.getRandomNumber() > Rand ){
	 	            			IamWinning = false;
	 	            			str += " I just lost to my neighbor.";
	 	            			DiscoveryService serv = ParentNode.getNetPeerGroup().getDiscoveryService();
	 	            			try {
									serv.publish(temp, 30000, 60000);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
	 	            		}else{
	 	            			str += " I am still winning the contest" ;
	 	            		}
		 	            	getLog().log(Level.FINEST, str);
	 	            	}

	 	            
	 	            }
	 	            
	                
	            }
	            
	        }
		}
		public boolean getWasCalled(){
			return WasCalled;
		}
		public boolean getIamWinning(){
			return IamWinning;
		}
	}*/
	/**
	 * to be used by the LeafWorker, DirectorRDV, or Advance Temaplate.  It will 
	 * send an advertisement which will shut down the network.  The Directors will
	 * broadcast the advertisments for a few seconds before shutting donw. 
	 */
	public void bringNetworkDown(){
		NetworkInstructionAdvertisement shutdown_instruction = 
			(NetworkInstructionAdvertisement)AdvertisementFactory.newAdvertisement(NetworkInstructionAdvertisement.getAdvertisementType());
		shutdown_instruction.setGridNodeName( getGridName());
		PipeID pipe_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		shutdown_instruction.setAdvID(pipe_id);
		shutdown_instruction.setNetworkInstruction(Types.Instruction.TERMINATE);
		
		DiscoveryService discovery_service = getNetPeerGroup().getDiscoveryService();
		
		/**
		 * This is to make sure the network gest the message when we want to turn it off
		 */
		for( int i = 0; i < 5; i++){
			try {
				discovery_service.publish(shutdown_instruction);
			} catch (IOException e1) {
				Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e1));
			}
			discovery_service.remotePublish(shutdown_instruction);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
			}
		}
		
		this.setStopNetwork(true);
	}
	/**
	 * Stops the JXSE network and the tunnel.
	 */
	public void StopNode(){
		/**
		 * Stop P2P Network
		 */
		NetManager.stopNetwork();
		/**
		 * Close the port we may have oppened
		 */
		NetDetective.closePortForwardOnRouter(Port);
		/**
		 * Destroy the JXTA configuration folder
		 */
		//this.RecursiveDelete(ConfigurationFile);
	}
	/**
	 * returns the Grid Node name.  All the cpus from a grid node should have the 
	 * same Grid Name.
	 * @return
	 */
	public static String getGridName() {
		return GridName;
	}
	/**
	 * Sets the Grid Name.
	 * @param gridName
	 */
	public static void setGridName(String gridName) {
		GridName = gridName;
	}
	/**
	 * Returns the class name
	 * @return
	 */
	public static String getName() {
		return Name;
	}
	public static void setName(String name) {
		Name = name;
	}
	/**
	 * Gets the Peer ID
	 * @return
	 */
	public static synchronized PeerID getPID() {
		return PID;
	}
	public static void setLog(Logger log) {
		Log = log;
	}
	public static Logger getLog() {
		return Log;
	}
	/**
	 * Returns the String of the Execptions stack trace
	 */
	public static String getStringFromErrorStack(Exception e){
		/*
		 * Jan 16, 2010
		 * Added error handling in case I forget to include a message for the exception
		 */
		StackTraceElement[] error_stack = e.getStackTrace();
		String message = e.getMessage();
		String err ="";
		if( message != null){
			err = message  + " Exception: " + e.getClass().getName() ; 
		}else{
			err = "NO error message given on this Exception, if you created that exceptions, put a message in it to HELP user programmers "  
				+ " Exception: " + e.getClass().getName() ;
		}
		for(int i = 0; i < error_stack.length; i++){
			err += error_stack[i] +"\n";
		}
		return err;
	}
	/**
	 * Returns the NetworkType for this node.
	 * @return
	 */
	public static Types.WanOrNat getNetworkType() {
		return NetworkType;
	}
	/**
	 * Sets the network type for this node
	 * @param networkType
	 */
	private static void setNetworkType(Types.WanOrNat networkType) {
		NetworkType = networkType;
	}

	public boolean isDebugManuallySetNetworkSettings() {
		return DebugManuallySetNetworkSettings;
	}
	public void setDebugMode(boolean debugMode) {
		DebugManuallySetNetworkSettings = debugMode;
	}

	public NetworkDetective getNetDetective() {
		return NetDetective;
	}
	/**
	 * Returns if Java sockets are being used
	 * @return
	 */
	public boolean isJavaSocketPort() {
		return UseJavaSocketPort;
	}

	public int getTunnelJavaSocketPortInternal() {
		return TunnelJavaSocketPortInternal;
	}

	public void setTunnelJavaSocketPortInternal(int tunnelJavaSocketPortInternal) {
		TunnelJavaSocketPortInternal = tunnelJavaSocketPortInternal;
	}

	
	
	public boolean isRestrictedNetworkDetected() {
		return RestrictedNetworkDetected;
	}

	public void setRestrictedNetworkDetected(boolean restrictedNetworkDetected) {
		RestrictedNetworkDetected = restrictedNetworkDetected;
	}

	public int getLastJavaSocketPort() {
		return JavaSocketPortStart - 1;
	}
	
	
	public synchronized boolean isStopNetwork() {
		return StopNetwork;
	}

	public synchronized void setStopNetwork(boolean stopNetwork) {
		StopNetwork = stopNetwork;
	}

	/**
	 * Advances the NextPort counter and returns.  Should be used instead of DefaultPort if 
	 * there will be more than one Java Socket Dispatcher.
	 * @return
	 * @throws NoPortAvailableToOpenException 
	 * @throws UPNPResponseException 
	 * @throws IOException 
	 */
	public int generateNewJavaSocketPort() throws NoPortAvailableToOpenException, IOException {//, UPNPResponseException{
		int port = JavaSocketPortStart;
		JavaSocketPortStart++;
		if( this.getNetworkType() == Types.WanOrNat.NAT_UPNP){
			boolean got_port = false;
			/*
			 * This will try 10 ports if the first attemps appear to be closed by the router.
			 */
			for(int i = 0; i < 10; i++){
				if( NetDetective.createPortForwardOnRouter(port) ){
					got_port = true;
					break;
				}else{
					port = JavaSocketPortStart;
					JavaSocketPortStart++;
				}
			}
			if(! got_port ){
				throw new NoPortAvailableToOpenException();
			}
		}
		return port;
	}


	/**
	 * This code is from PracticalJXTA book.  The fucntion will delete a directory 
	 * by deleling each file recursively.  caching advertisements creates problems
	 * with this type of program.  So, the configuration directory will be deleted
	 * every time the node is about to be started.
	 * @param TheFile
	 */
	private void RecursiveDelete(File TheFile) {
	        
	        File[] SubFiles = TheFile.listFiles();
	        
	        if (SubFiles!=null) {
	        
	            for(int i=0;i<SubFiles.length;i++) {
	            	//System.out.println("deleting..." + TheFile.toString() );
	                if (SubFiles[i].isDirectory()) {
	                    RecursiveDelete(SubFiles[i]);
	                }
	                SubFiles[i].delete();
	            }
	        TheFile.delete();
	        }       
	}
}
