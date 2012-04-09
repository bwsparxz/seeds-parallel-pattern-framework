/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *   * All rights reserved  
 *  
 */
package edu.uncc.grid.pgaf.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.DiscoveryResponseMsg;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.advertisement.NetworkInstructionAdvertisement;
import edu.uncc.grid.pgaf.communication.MultiModePipeMapper;
import edu.uncc.grid.pgaf.communication.nat.TunnelClient;
import edu.uncc.grid.pgaf.interfaces.advanced.Template;
import edu.uncc.grid.pgaf.p2p.compute.PatternRepetitionException;
import edu.uncc.grid.pgaf.p2p.compute.Worker;
import edu.uncc.grid.seeds.comm.dependency.DependencyMapper;

/**
 * The work horse of the framework.  The LeafWorker receives code execution from the {@link Node} class once the network 
 * has being identified and the DirectorRDV task has being handed out.  The LeafWorker can create multiple threads
 * that run the user's computation tasks.
 * 
 * @author jfvillal
 *
 */
public class LeafWorker { 
	static Node ParentNode;
	private Logger Log;
	private RoutineAdvertPublisherQuerier Querier;
	public final long WORKPOOL_PIPE_TIMEOUT = 300000L;
	/**
	 * Keeps a list of pipes that can be used to communicate the advanced user's application
	 */
	
	
	/**The {@link ClientTunnel} variable used to manage the NAT tunnel.  */
	//public static TunnelClient Tunnel;
	/**The {@link PipeID} used to connect the tunnel socket*/
	//public static PipeID TunnelPipeID;
	public static DirectorRDVAdvertisement DirectorRDVInfo;
	
	/**
	 * The {@link BasicLayerInterface} instance is used to get the Basic User's class and run his/her 
	 * module.
	 */
	//BasicLayerInterface UserApp;
	/**
	 * A LeafWorker may be a sink source, which is the one responsible to get and save the data.  At present,
	 * we only consider a single source and a single sink, and both happen on the same node.
	 */
	//private boolean IAmDataSinkSource;
	
	/**
	 * Constructor used {@link Deployer}.  It is assumed the sink/source runs on the client computer.
	 * Future version can port the location of this node.
	 * @param node
	 * @param bli
	 * @throws Exception
	 */
	/*public LeafWorker( Node node, BasicLayerInterface bli) throws Exception{
		this(node);
		UserApp = bli;
		this.IAmDataSinkSource = true;
		
		SMDispatcher = new SharedMemDispatcher();
		Stop = false;
	}*/
	
	/**
	 * Default Constructor.  This is used at the remote hosts.
	 * @param node
	 * @throws Exception
	 */
	public LeafWorker(Node node) throws Exception{
		ParentNode = node;
		Log = ParentNode.getLog();
		
		/* The Mapper has to be started before any communication lines by the Worker are atempted.*/
		MultiModePipeMapper.initMapper();
		
		/* Starting the Dependency mapper */
		DependencyMapper.initDependencyMapper();
		
		DirectorRDVInfo = null;
		//Tunnel = null;
	}
	
	//static ClientSocketManager GenericLink;
	public static void wait( int segment, int timeout){
		
	}
	//TODO the update for these functions should go on the communication packet
	public static void  commSend(List<Integer> IDs){
		/*try {
			
			GenericLink.SendObject(dat);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PipeNotStablishedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	public static void  commSend(){
		/*try {
			GenericLink.SendObject(dat);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PipeNotStablishedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	/**
	 * This function returns the tunnel instance.  Only one instance per node.  if the 
	 * Tunnel is null, it creates an instance.
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	/*public static TunnelClient getTunnel() throws IOException, ClassNotFoundException{
		///
		/// Create a Tunnel if the I am NAT and we have a TunnelPipeID and The Tunnel has not being created 
		///
		if( 	Node.getNetworkType() == Types.WanOrNat.NAT_NON_UPNP  //I am NAT so I need the Tunnel 
				&& DirectorRDVInfo  != null					  //I have a DRDV to connect the Tunnel to
				&& LeafWorker.Tunnel == null						  //I have not initialized the Tunnel yet
		   ){
			
				
				LeafWorker.Tunnel = new TunnelClient(LeafWorker.ParentNode, DirectorRDVInfo);
				Node.getLog().log(Level.FINE, " Created the TunnelClient ");
			
		}
		return LeafWorker.Tunnel;
	}
	public static void DeactivateTunnel() throws IOException, InterruptedException{
		if( Tunnel != null){
			synchronized( Tunnel){
				LeafWorker.Tunnel.close();
				//
				// I use null as a way to tell the tunnel is not activate
				// so, lets make sure the Tunnel is set to null when not
				// connected
				//
				LeafWorker.Tunnel = null;
			}
		}
	}
	*/

	/**
	 * It run the main method from LeafWorker.  A similar method exists on DirectorRDV.
	 * It is equivalent to a main() method.  This method is not to be used by the user
	 * it is used by class Node, which would be used by the Deployer.  The deployer would
	 * be by the user.
	 * @throws NetworkTypeNotSetException 
	 * @throws IOException 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void runNode() throws NetworkTypeNotSetException, IOException {
		/*start the thread that will communicate debug information and will publish adverts*/
		Thread.currentThread().setName("LeafWorker Main Thread");
		if( Node.getNetworkType().compareTo(Types.WanOrNat.UNKNOWN) == 0){
			throw new NetworkTypeNotSetException();
		}
		
		Log("Uptime: " + ParentNode.getUptime() );
		
		RoutineAdvertPublisherQuerier querier = new RoutineAdvertPublisherQuerier(this);
		
		/**
		 * Find and connect to local RDV.  Wait for RDV only if in debug mode.
		 * Debug mode is when the NetworkRoll, the NodeRoll or when explicitly 
		 * setting debug var to true.
		 */
		/*   
		if( this.ParentNode.isDebugMode() ){
			while( !querier.isRDVReady() ){
				Node.getLog().log(Level.INFO, " waiting for RDV Debug is " + ParentNode.isDebugMode() 
						+ " this is only done in debug mode ");
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}*/
		
		/**
		 * this function is not doing anything worth putting on another thread.  So, it now directly
		 * calles the querier's function.  The code can be cleaned by integratingthe querier bac into the 
		 * LeafWorker thereby eliminating the extra internal class.
		 */
		querier.run();
		
		/*Node.getLog().log(Level.FINE, "LEAF Closing Tunnel Client ");
		try {
			LeafWorker.DeactivateTunnel();
		} catch (IOException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (InterruptedException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		}*/
		Node.getLog().log(Level.INFO, "Shutting down... Uptime: " + ParentNode.getUptime() );
	}
	public void Log( String str ){
		//RoutineAdvertPublisherQuerier.addtoDebugErrorMessage(str);
		Node.getLog().log(Level.FINEST, "\nsend the stuff below to Observer\n" + str);
	}
	
	/**
	 *  This class does a similar job as its counterpart on the Director Code. 
	 */
	public static class RoutineAdvertPublisherQuerier implements Runnable, DiscoveryListener{
		LeafWorker worker;
		
		Worker W;
		IncrementalSleeper DicoverySleeper;
		//private boolean DebugPipeAckReceived;
		
		private PeerID RDVID;
		
		PipeID DebugLogPipeID;
	
		public RoutineAdvertPublisherQuerier(LeafWorker w) throws IOException{
			this.worker = w;
			this.DicoverySleeper = new IncrementalSleeper();
			RDVID = null;
			W = new Worker(ParentNode);
		}
		public boolean isRDVReady(){
			return RDVID != null;
		}
		
		/**
		 * This thread publishes advertisements, and sends debug data to the Observer node which is at the
		 * user's computer.  
		 * 
		 * For the workpool implementation, it mostly deals with sending debug data to the RDV
		 * 
		 */
		public void run() {
			try{
				DiscoveryService discovery_service = ParentNode.getNetPeerGroup().getDiscoveryService();
				
				/**
				 * The RoutineAdvertPublisherQuerier needs to cycle faster at the start of the network to 
				 * be able to stablishe the debug pipes and other framework settings before the user's 
				 * application or while the user's application is starting to run.  Once this is done, the
				 * cycle can slow down since most publishing can be done every 30 secs and the debug info
				 * can flow at a rate that looks fast enough to a human.  In this case it is 3 seconds.
				 */
				IncrementalSleeper sleeper = new IncrementalSleeper();
				while( ! ParentNode.isStopNetwork() ){
					if( RDVID != null ){
						//this is where the remote logger pipes where created
						//now it has been moved to its own optional thread.
					}else{
						 /*
						  * this codes tries to get the rdv advertisement from the file system
						  * it looks ate $HOME_DIR/rdv.advert
						  * 
						  * there is a small problem with this code.  if the file was written by a previous run, this 
						  * can conflict with the current run.  I will fix that when a group id is create for the 
						  * JXTA session, thiw will help us run multiple instances of Seeds that do not conflict 
						  * with each other when sending the messages.
						  * The Id can also be used at the end of rdv.advert to defirentate between session.
						  */
						 String home_dir = System.getProperty("user.home");
						 String div = System.getProperty("file.separator");
						 File rdv_input_stream_file = new File(home_dir + div + "rdv.advert");
						 if( rdv_input_stream_file.exists()){
							 try{
								 FileInputStream stream = new FileInputStream(rdv_input_stream_file);
								 XMLDocument doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, stream);
								 DirectorRDVAdvertisement rdv_adv = new DirectorRDVAdvertisement(doc.getRoot());
								 /*by publishing the advert we can get the normal process get the rdv advertiment.*/
								 
								 this.processRDVAdvertisement(rdv_adv);
								 
							     //discovery_service.publish(rdv_adv);           
							     Node.getLog().log(Level.FINEST, "Got RDV from FS and published" );
							 }catch( IllegalArgumentException e){
								  Node.getLog().log(Level.WARNING , Node.getStringFromErrorStack(e));
							 }
						 }else{
							 Node.getLog().log(Level.FINEST, "Waiting for RDV Advertisement + did not find any in FS" );	 
						 }
						 
					}
					
					
					//the code below may be deprecated as of April 10 2010. the shared memory is now administrated with threads
					//which is faster.
					/*Enumeration<Advertisement> TheAdvEnum;	
					try {
						TheAdvEnum = discovery_service.getLocalAdvertisements(DiscoveryService.ADV, null, null);
				        while (TheAdvEnum.hasMoreElements()) { 
				            Advertisement TheAdv = TheAdvEnum.nextElement();
			 	            //this advertisement is to establish a data connection for the advanced user's application
				            //the local cached is check to be able to create share memory links
			 	           MultiModePipeMapper.checkAdvertisement(TheAdv);
			 	           
			 	           //Dec 5, may need to add this back to get dependencies to work on LeafWorker
			 	           
			 	           try{
				            	if( W.checkAdvertisement(TheAdv)){
				            		discovery_service.flushAdvertisement(TheAdv);
				            	}
			            	}catch ( PatternRepetitionException e){
			            		discovery_service.flushAdvertisement(TheAdv);
			            	} catch (IllegalArgumentException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							} catch (SecurityException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							} catch (InstantiationException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							} catch (NoSuchMethodException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							}
			 	           
			 	        }
					} catch (IOException e1) {
						Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e1) );
					}*/
					
					
					//Node.getLog().log(Level.FINER, " quering for advertisements" );
					discovery_service.getRemoteAdvertisements( null , DiscoveryService.ADV , null//CommunicationConstants.GridNameTag
																		, null , 200,this);
					
					//make a fine query for patterns.
					W.queryNetForPatterns();
					
					sleeper.sleep();
					
					//ThreadMXBean thread_num = ManagementFactory.getThreadMXBean();
					//long[] all = thread_num.getAllThreadIds();
					//System.out.println("threads: " + all.length );
					/**
					 * process Tunnel Instruction.  Socket Boundness and closings
					 * The tunnel is for nodes behind NAT's.
					 * 
					 */
					/*if( Tunnel != null ){
						synchronized(Tunnel){
								Tunnel.handleInstructionContainer();		
						}
					}*/
					
				}//end while
				
				Node.getLog().log(Level.FINEST, "LEAFWORKER Out of Run function -------------FasterV---------------" );
			}catch(Exception e){
				Node.getLog().log(Level.SEVERE, " in LeafWorker:RoutineAdverPublisherQuerier \n" + Node.getStringFromErrorStack( e));
			}
		}
		

		
		
		public void processRDVAdvertisement(DirectorRDVAdvertisement adv ){
			//get RDV ID to send adverts directly to my directorRDV
     		RDVID = adv.getRDVID();
     		this.DebugLogPipeID = adv.getDebugPipeID();
     		//TunnelPipeID = temp.getTunnelPipeID();
     		
     		/*
     		 *With DirectorRDVInfo, RDVID and DebugLogPipeID can be eliminated and use this variable.
     		 *This is not high priority, just makes the code look more efficient, but it must be a 
     		 *few extra bytes used in main memory ( 80)
     		 *7/21 
     		 */
     		DirectorRDVInfo = adv;
     		/*
     		 * if we don't have a wan address.  then set the Wan address to that of the RDV.  this 
     		 * becomes usefull when using Java Sockets.  The socket gets routed through the RDV 
     		 * node.
     		 */
     		if( ParentNode.getNetDetective().getWANAddress() == null){
     			ParentNode.getNetDetective().setWANAddress(adv.getWanAddress());
     		}
		}
		/**
		 * This function is used to catch the advertisements that this node should be interested in.
		 */
		public void discoveryEvent(DiscoveryEvent arg0) {
			DiscoveryResponseMsg TheDiscoveryResponseMsg = arg0.getResponse();
	        
	        if (TheDiscoveryResponseMsg!=null) {
	            
	            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
	            DiscoveryService discovery_service = ParentNode.getNetPeerGroup().getDiscoveryService();
	            while (TheEnumeration.hasMoreElements()) {
	            	Advertisement TheAdv = TheEnumeration.nextElement();
	            	try{
		            	/**
		            	 * This advertisement means I have a Director on my cluster
		            	 * I should set the pipe so I can send debug, error messages
		            	 */
		            	if ( TheAdv.getClass().getName().compareTo( DirectorRDVAdvertisement.class.getName()) == 0 ){
		            		DirectorRDVAdvertisement temp = (DirectorRDVAdvertisement) TheAdv;
		            		//if we already have the pipe, skip this step
		 	            	if(temp.getGridNodeName().compareTo(ParentNode.getGridName()) == 0){
		 	            		this.processRDVAdvertisement(temp);
		 	            		discovery_service.flushAdvertisement(temp);
		 	            	}
		 	            }
		 	            
		 	            /**
		 	             * Advertisements caught for a specific template should be caught here.	
		 	             */
		 	            	
		 	            /**
		 	             * 
		 	             * this advertisement is to establish a data connection for the advanced user's application
		 	             * 
		 	             * 
		 	             * */
		            	MultiModePipeMapper.checkAdvertisement(TheAdv, discovery_service);
		            		
		            	
		            	/**
		            	 * This will store the Dependency advertisements on the dependeny mapper, where they can be
		            	 * picked from by the local processes to stablish connections.
		            	 */
		            	DependencyMapper.checkAdvertisement(TheAdv, discovery_service);
		            	
		            	/*try{
			            	if( W.checkAdvertisement(TheAdv)){
			            		discovery_service.flushAdvertisement(TheAdv);
			            	}
		            	}catch ( PatternRepetitionException e){
		            		discovery_service.flushAdvertisement(TheAdv);
		            	} catch (IllegalArgumentException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e.printStackTrace();
						} catch (SecurityException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e.printStackTrace();
						} catch (InstantiationException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e.printStackTrace();
						}
						*/
		            	
		            	
		            	/**
		            	 * 
		            	 * Check to see if any of the Server side needs to use some advertisements.
		            	 * I think it is redundant to offer an Advertisement Catcher since the Advanced user
		            	 * can just implement the Discovery interface and be able to catch Advertisements 
		            	 * from her own code. 
		            	 * 4/7/09
		            	 * But today I am putting it back to be able to test the basic features.  I need to 
		            	 * make sure the multimode pipe works before I change the way the Advanced User 
		            	 * Interface works.
		            	 */
		            	/*List<AdvertCatcher> list = this.worker.AdvancedTemplate.getClientSideAdvertCatchers();
		            	if( list != null){
			            	Iterator<AdvertCatcher> it = list.iterator();
			            	while(it.hasNext()){
			            		AdvertCatcher adv_catch = it.next();
			            		if( TheAdv.getClass().getName().compareTo( adv_catch.getAdvertisementType().getName()) == 0){
			            			adv_catch.Action(TheAdv, discovery_service);
			            		}
			            	}
		            	}*/
		            	
			        	/**
			        	 * Network Instruction catcher
			        	 */
		        		if( TheAdv instanceof NetworkInstructionAdvertisement){
		            		NetworkInstructionAdvertisement advert = (NetworkInstructionAdvertisement) TheAdv;
		            		
		            		if( advert.getNetworkInstruction() == Types.Instruction.TERMINATE ){
		            			//set end lprogram true
		            			/**
		            			 * make sure the main algorithm has finished running, the user programmer 
		            			 * can do this by waiting for the pattern to finish before shutting down 
		            			 * the framework, failling to do this could trucate the answer.
		            			 */
		            			Template.setShutdownOrder(true);
		            			Node.getLog().log(Level.FINE, "got a Network Instruction  ADVERTISEMENT -- TERMINATE ");	
		            			PipeID id = IDFactory.newPipeID( PeerGroupID.defaultNetPeerGroupID);
		            			advert.setAdvID(id);
		            			
		            			
		            			discovery_service.publish(advert);
		            			discovery_service.remotePublish(advert);
								try {
									Thread.sleep(200);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
		            			
		            			
								ParentNode.getNetManager().stopNetwork();
								/*if( Tunnel != null){
									LeafWorker.Tunnel.close();
								}*/
								System.exit(1);
								
		            		}
		            		discovery_service.flushAdvertisement(advert);
		            	}
	            	}catch(IOException e){
	            		e.printStackTrace();
	            	}
	            }
	        }
	        this.DicoverySleeper.sleep();
		}	
	}
	/*public static void setTunnel(TunnelClient tunnel) {
		Tunnel = tunnel;
	}*/
}
