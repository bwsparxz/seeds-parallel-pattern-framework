/* * Copyright (c) Jeremy Villalobos 2009
 *  * All rights reserved 
 * 
 *  
 */
package edu.uncc.grid.pgaf.p2p;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.rendezvous.RendezVousService;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DependencyAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.advertisement.NetworkInstructionAdvertisement;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.MultiModePipeMapper;
import edu.uncc.grid.pgaf.communication.nat.TunnelBoringModule;
import edu.uncc.grid.pgaf.p2p.compute.PatternRepetitionException;
import edu.uncc.grid.pgaf.p2p.compute.Worker;
import edu.uncc.grid.seeds.comm.dependency.DependencyMapper;
/**
 * <h2>Purpose</h2>
 *  <p>DirectorRDV works like a router in some cases.  It gives a routing service.  It is used to get around
 *  complicated network settings such as NAT's.  JXSE does a good job by using Relays, but that tool is 
 *  not designed with performance in mind.  The Relay gives a network constrained node access to the network
 *  by using the http protocol.  The consequence is that the network builder has to have a designated Relay server
 *  somewhere in the "visible" Internet, and also, this reduces the usefulness of the node that is inside the
 *  NAT.</p>
 *<h2> DirectorRDV in more detail</h2>
 *<p>
 *  The DirectorRDV is either an EDGE or an RDV.  It will figure this out based on 
 *  the Interfaces's Address.  If it is inside a NAT (with a 192.168.x.x) it will be an EDGE which will connect
 *  to a RDV that is in the visible Internet.  It will provide the routing service to the other EDGES that are
 *  inside the network.  If the Node finds itself with an Internet IP, it will set itself as a RDV.  It will
 *  still offer Routing service, but using JXSE's EndPointService will be the first choice.
 * </p>
 * <p>
 *  There should be one DirectorRDV per Grid Node.  It should work as the gateway to the nodes on other Grid 
 *  Nodes, depending on the network setup, either JXSE routines or PGAF routines will be used.  Of course, the node
 *  will give priority to a direct connection.
 *  </p>
 */
/*
 *  log
 *  April 1, 2009
 *  I have being adding the routing system.  The routing system will create three types of conections between nodes.  The
 *  connections are based on tcp sockets:
 *  
 *  WAN to WAN:
 *  	This includes WAN to WAN and NAT+UPNP to WAN, the SocketManager, ClientSocketManager, ServerSocketManager, and
 *   JxtaSocketDispatcher take the job of stablishing and managing communication on the socket.
 *  
 *  NAT to WAN:
 *  	This uses the TunnelManager, TunnelClient, TunnelServer, TunnelContainer, TunnelInstructions, TunnelDispatcher,
 *   to establish a tunnel between the Leaf Worker and the Director RDV (the Local node that has direct connection to the
 *   Internet though WAN access or NAT+UPNP access).  The tunnel produces VirtualSocketManager objects that can be used
 *   to manage individual connections between the LeafWorker (LW) and external or Remote LeafWorkers (RLW's). 
 *  
 *  Shared Memory:
 *  	It is a series of classes that manages communication withing the same CPU for multi-core systems.  The main class that 
 *  does this is UserThread.  At the time, there is no plans to add WorkerLeaf threads to the directory rdv, but if the number
 *  of CPU resource is low, it makes sence to use the DirectorRDV's as workers as well.
 *  
 *  The three systems would be wrapped with the MultiModePipe which would be used by the advanced user to create the 
 *  connections between the nodes for the templates that would ultimately be used by the user-programmer.
 * 
 *  @author jfvillal
 */
public class DirectorRDV {
	/** Sleep time between publishes */
	//public static final long SLEEP_TIME = 1000;  
	/**Number of seconds the Director will stay on to make sure the 
	 * 		shutdown advert gets to all nodes
	 */
	public static final int FINAL_COUNT_DOWN = 2;
	/**
	 * Pointer to the Node object which has the JXSE services and runtime variables.
	 */
	static Node ParentNode;
	/**
	 * Log
	 */
	Logger Log;
	/**
	*Pipe used to connect the debug pipes.  The debug 
	*pipes connect to RDV and then to an Observer Node
		*/
	 


	
	//namespace to categorize the debug messages sent to the director
	public static final String DebugMessageNamespace = "DebugMessageNamespace";
	//message tag for the debugging structure. (centralization of error messages)
	public static final String MessageTag = "MessageTag";
	/**
	 * 
	 * @param node provides access to the parent object
	 * @param set_rendezvous sets if the DirectorRDV should be also a rendezvous
	 * @throws Exception
	 */
	public DirectorRDV(	Node node
						, boolean set_rendezvous ) throws Exception{
		ParentNode = node;
		Log = Node.getLog();
		if( set_rendezvous){
			//turn on the Rendezvous service.
			
			
			RendezVousService Rendezvous = ParentNode.getNetPeerGroup().getRendezVousService();
			Rendezvous.startRendezVous();
			Node.getLog().log(Level.FINEST, "I am a Rendezvous Now");
				
			//ConnectToFellowRendezVousDirectors();
			
			
			{Enumeration<ID> e = Rendezvous.getConnectedRendezVous();
			while (e.hasMoreElements()) {
            	PeerID id = (PeerID)e.nextElement();
            	Node.getLog().log(Level.FINEST, "rdv peer id: " + id.toString() );
			}}
			
		
			
			
		}else{
			//ConnectToFellowRendezVousDirectors();
			//leave as EDGE node
		}
		ConnectToFellowRendezVousDirectors();
	}
	public void runNode() throws Exception{
		//get the network related advertisement and organizational communication
		//start advertisement and query thread
		Thread.currentThread().setName("DirectorRDV Main Thread");
		RoutineAdvertPublisherQuerier routine = new RoutineAdvertPublisherQuerier(this);
		
		//Thread t = new Thread (routine);
		//t.start();
		
		/* *********************************************************************************
		 * This line below is very IMPORTANT.  It starts the shared memory mapper.  misplacing
		 * this line can cause error on the shared memory pipe.  Because this framework relies on 
		 * multiple threads, the safest thing to do is to start it before the main routine
		 * thread is started
		 */
		MultiModePipeMapper.initMapper();
		/* *******************************/
		
		/* *************************************************************************************
		 * July 22 2010
		 * The same thing has now being done to keep track of the dataflow dependencies.
		 * The reason there are two mappers at this point have to do more with legacy than with 
		 * design.  It is simply easier to add the dependencies on top of the existing communication
		 * infrastructure than to start from scratch.  However, the experiments will make sure
		 * this design decition does not affect the results.
		 */
		
		DependencyMapper.initDependencyMapper();
		
		
		/*the routine class could be integrated back into the rdv.  the worker class
		 * is assuming most of the thread management in relation to the cores.*/
		routine.run();
		//Thread worker = new Thread( new RunWorker());
		//worker.setDaemon(true);
		//worker.start();	
		
		
	}
	
	
	
	/**
	 * The class is used to routinely make sure that advertisements keep
	 * going through the network.  Advertisements like the Director advertisement.
	 * Also, the class is used to query the network for advertisement.  A director
	 * needs to query every so often for the existence of the Observer.
	 * @author jfvillal
	 *
	 */
	public static class RoutineAdvertPublisherQuerier implements Runnable /*, DiscoveryListener*/{
		private DirectorRDV rdv;
		Worker W;
		/**
		 * DirectorSource true means that the director will resend the error messags to some other director that is 
		 * the sink
		 * 
		 * Directorsource false means that the director will receive messages and will resend them to the Observer
		 * in addition to his own messages.
		 */
		
		
		
		private boolean stop;
		
		PipeID DebugPipeID;
		/**
		 * Creates and mainteing tunnel to LW that need them
		 **/
		TunnelBoringModule Tbm;
		/**
		  *The pipe is used to tunnel information to NAT-restricted
		  *nodes.
		  **/
		PipeID TunnelPipeID; 
		/**
		 * 
		 * @param rdv
		 * @throws IOException 
		 */
		public RoutineAdvertPublisherQuerier(DirectorRDV rdv) throws IOException{
			this.rdv = rdv;
			stop = false;
			
			
			W = new Worker( ParentNode);
			
			DebugPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
			TunnelPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		}
	
		boolean FinalShutdown = false;
		/**
		 * Runs the main DirectorRDV loop, it checks for new log messages for the Debug's system
		 * Observer.  And it manages the Tunnel.  
		 */
		public void run() {
			int interval_to_publish_rdv_advert = 0;
			
			Node.getLog().log(Level.INFO, "Uptime for Rendezvous RDV : " + ParentNode.getUptime() );
			
			DiscoveryService discovery_service = ParentNode.getNetPeerGroup().getDiscoveryService();
			
			long final_count_down_num = FINAL_COUNT_DOWN;
			
			
			int log_interval_control = 0;  //this is just to manage loggin code
			
			try {
				/**
				 * Start the thread that create tunnels for the NAT nodes
				 */
				Node.getLog().log(Level.FINE, "Starting Tunnel Dispatcher...");
				Tbm = new TunnelBoringModule(this.TunnelPipeID, rdv.ParentNode);
				Tbm.startThreads();
				IncrementalSleeper sleeper = new IncrementalSleeper();
				while( !stop ){	
					/**
					 * 
					 * Publish the director's advertisement every so often
					 * 
					 */
					
					if( interval_to_publish_rdv_advert % 10000 == 0){
						/**publish the Director RDV advertisement*/
						
						
						try {
							DirectorRDVAdvertisement adv = (DirectorRDVAdvertisement) 
							AdvertisementFactory.newAdvertisement(DirectorRDVAdvertisement.getAdvertisementType());
							adv.setGridNodeName(Node.getGridName());
							adv.setRDVID(Node.getPID());
							adv.setDebugPipeID(DebugPipeID);
							adv.setTunnelPipeID(TunnelPipeID);
							if(ParentNode.getNetDetective().getLANAddress() != null){
								adv.setLanAddress(ParentNode.getNetDetective().getLANAddress());
							}else{
								Node.getLog().log(Level.WARNING, "\nRDV has LAN address as null");
							}
							if(ParentNode.getNetDetective().getWANAddress() != null){
								adv.setWanAddress(ParentNode.getNetDetective().getWANAddress());
							}else{
								Node.getLog().log(Level.WARNING, "\nRDV has WAN address as null");
							}
							adv.setPort(ParentNode.getTunnelJavaSocketPortInternal());
							
							discovery_service.publish(adv);
							discovery_service.remotePublish(adv);
							/*
							 * This next piece of code will store the rdv advertisement on the FS
							 */
							 XMLDocument doc = (XMLDocument) adv.getDocument(MimeMediaType.XMLUTF8);
							 String home_dir = System.getProperty("user.home");
							 String div = System.getProperty("file.separator");
							 OutputStream stream = new FileOutputStream(new File(home_dir + div + "rdv.advert"));
							 doc.sendToStream(stream);
							
							//discovery_service.remotePublish(adv, 30000);
							Node.getLog().log(Level.FINEST, "Published Director RDV Advertisement");
						} catch (IOException e1) {
							Node.getLog().log(Level.FINER, "Error publishing advetisement");
							e1.printStackTrace();
						}
					}
					
					//the remote logging code used to be here, it is not moved on to a new thread.
					
					
					
					/**
					 * 
					 * I should use getLocalAdvertisement() for the Rendezvous nodes since they cache the advertisement from
					 * other nodes.  But use getRemoteAdvertisements for LeafWorker since they depend on the RDV to get 
					 * the message.
					 *  
					 */
			        Enumeration<Advertisement> TheAdvEnum;	
					try {
						TheAdvEnum = discovery_service.getLocalAdvertisements(DiscoveryService.ADV, CommunicationConstants.GridNameTag, null);
				
				        while (TheAdvEnum.hasMoreElements()) { 
				            
				            Advertisement TheAdv = TheAdvEnum.nextElement();
				           
				            /*******************************************************************************************
				             * This catches the Observer Debug Log Pipe Advertisements
				             * the following is a pseudo code version of how to deal with the ObserverSinkAdvertisement
				             * if advert is LeafNode
				        	 *		if we have the same grid name, I am sink, else I am source
				        	 *	
				        	 * if adver is director sink request
				        	 *		if the advert was not mine, the PeerID does not match, then I am a source
				        	 *			publish my own pipeid directly to the direcotor sink
				        	 *		else, the advert is mine, do nothing
				        	 *	
				        	 *	if advert is director sink ack and I am source
				        	 *		if PeerID does not match, I am source
				        	 *			 set all thing GO for establishing pipe
				        	 *
				        	 *	if advert is director source and I am a sink
				        	 *		//add pipe to the list of pipe from the LeafWorkers
				             * 
				             * 
				             * 
				             ******************************************************************************************/
				            try{
					            /**
					             * Observer Sink Advertisement catcher
					             * 
					             */
					            /*If I am a Director Sink and the Advert is from the Observer (this is for the SinkDirector )*/
					            
					        	//the advertisement check for the Remote Logger should be here when reimplemented.
				            	
					        	/**
					        	 * Network Instruction catcher
					        	 */
				            															//if final count down is not checked
				            															//this advert catcher routine will 
				            															//keep pushing the advert over and over.
				        		if( TheAdv instanceof NetworkInstructionAdvertisement && !this.FinalShutdown){
				            		NetworkInstructionAdvertisement advert = (NetworkInstructionAdvertisement) TheAdv;
				            		if( advert.getNetworkInstruction() == Types.Instruction.SHUTDOWN 
				            				||
				            				advert.getNetworkInstruction() == Types.Instruction.TERMINATE	){
				            			//set end lprogram true
				            			
				            			PipeID id = IDFactory.newPipeID( PeerGroupID.defaultNetPeerGroupID);
				            			advert.setAdvID(id);

				            			for( int i = 0; i < 5; i++){
					            			discovery_service.publish(advert);
					            			discovery_service.remotePublish(advert);
											try {
												Thread.sleep(100);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
				            			}
				            			
				            			this.FinalShutdown = true;
				            		}
				            	}	
				        		
				        		/**
				        		 * Communication line catcher.  Used if there is a worker on this node  
				        		 */
				        		MultiModePipeMapper.checkAdvertisement(TheAdv, discovery_service);
				        		
				        		/**
				        		 * check for dependency variables.
				        		 */
				        		DependencyMapper.checkAdvertisement(TheAdv, discovery_service);
				        		
				        		
				        		try{
					        		if(W.checkAdvertisement(TheAdv)){
					        			//Flush only if the pattern advertisement is done
					        			//discovery_service.flushAdvertisement(TheAdv);
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
				        		
				        		
				            }catch(IOException e){
				            	e.printStackTrace();
				            }
				        }//end of while that iterates over all advertisements
					} catch (IOException e1) {
						e1.printStackTrace();
					} 
						
					/**
					 * This is to prevent necessary log information from cluttering my log file.
					 */
					if( log_interval_control++ % 1000 == 0){
						if (W.isIdling() ){
							Node.getLog().log(Level.FINE, " Worker is idling ");
						}else{
							Node.getLog().log(Level.FINE, " Worker is not idling");
						}
					}
					//query for pipes for the advance comunication service
					//discovery_service.getRemoteAdvertisements( null , DiscoveryService.ADV , null //CommunicationConstants.GridNameTag
					//													, null , 1000,this);
					
					//check to see if a shutdown instruction has been issued
					new QueryShutdownInstruction( ParentNode, FinalShutdown);
					
					//make a fine query for patterns.
					W.queryNetForPatterns();
					
//					discovery_service.getRemoteAdvertisements( null , DiscoveryService.ADV , DependencyAdvertisement.DependencyIDTag
	//						, null , 200,this);
					
					if( this.FinalShutdown ){
						--final_count_down_num;
						Node.getLog().log(Level.INFO, " It's the final count down! " + final_count_down_num);
						if( final_count_down_num < 0){
							Node.getLog().log(Level.INFO, " I'm done ! " + final_count_down_num);
							
							break;
						}
					}
					++interval_to_publish_rdv_advert;
					sleeper.sleep();
				}//end of while ( main loop for this thread) 
				
				Thread.sleep(200);
				
				//close all the VirtualPipes
				Node.getLog().log(Level.FINE, "RDV Closing Tunnel Boring Module ");
				this.Tbm.stopThreads();
				Node.getLog().log(Level.FINE, "RDV Closing Tunnel Boring Module -- DONE");
			
			}catch (IOException e){
				System.err.println("An irrecoverable error happened at initiation or an error happening while closing down.");
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}//end of run()
		/**
		 * Tells the main thread to stop looping
		 */
		public void stop(){
			stop = true;
		}

		public void DebugLogAdvert( Advertisement adv){
			if( adv instanceof DataLinkAdvertisement){
				DataLinkAdvertisement link_adv = (DataLinkAdvertisement) adv;
				try {
					FileWriter w = new FileWriter( "./LinkAdvertLog.txt", true);
					w.write( link_adv.getDataID() + ":" + link_adv.getLanAddress() + ":" + link_adv.getPort() + "\n");
					w.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if( adv instanceof DependencyAdvertisement){
				DependencyAdvertisement dept_adv = (DependencyAdvertisement) adv;
				try {
					FileWriter w = new FileWriter( "./DeptAdvertLog.txt", true);
					w.write( dept_adv.getDataID() + ": " + dept_adv.getHyerarchyID().toString() +"\n");
					w.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}

		/*This code was used when all the advert quering was done in one place.  This approach had the defect of 
		 * swaping the network with too many unnecesary responsed to a query about advertisements.  the new
		 * approach create queries athat are more specific, have a higher probability of being successuful
		 * and don't use up as much network bandwidth.
		 * @Override
		public void discoveryEvent(DiscoveryEvent event) {
			DiscoveryResponseMsg TheDiscoveryResponseMsg = event.getResponse();
	        
	        if (TheDiscoveryResponseMsg!=null) {
	            
	            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
	            DiscoveryService discovery_service = ParentNode.getNetPeerGroup().getDiscoveryService();
	            while (TheEnumeration.hasMoreElements()) {
	            	Advertisement TheAdv = TheEnumeration.nextElement();
	 	            
	 	             * 
	 	             * this advertisement is to establish a data connection for the advanced user's application
	 	             * 
	 	             * 
	 	             * 
	            	MultiModePipeMapper.checkAdvertisement(TheAdv, discovery_service) ;
					
	            	
	            	
	            	
	        		 * check for dependency variables.
	        		 *
	        		
					DependencyMapper.checkAdvertisement(TheAdv, discovery_service);
					
					
					DebugLogAdvert( TheAdv);
	            	
	            	
	              	**
		        	 * Network Instruction catcher
		        	 *
	        		if( TheAdv instanceof NetworkInstructionAdvertisement && !this.FinalShutdown ){
	            		NetworkInstructionAdvertisement advert = (NetworkInstructionAdvertisement) TheAdv;
	            		if( advert.getNetworkInstruction() == Types.Instruction.SHUTDOWN 
	            				||
	            				advert.getNetworkInstruction() == Types.Instruction.TERMINATE	){
	            			//set end lprogram true
	            			
	            			PipeID id = IDFactory.newPipeID( PeerGroupID.defaultNetPeerGroupID);
	            			advert.setAdvID(id);

	            			for( int i = 0; i < 5; i++){
		            			try {
		            				
									discovery_service.publish(advert);
								} catch (IOException e1) {
									Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e1));
								}
		            			discovery_service.remotePublish(advert);
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
									e.printStackTrace();
								}
	            			}
	            			
	            			FinalShutdown  = true;
	            		}
	            	}	
	            	
	            	 since Worker now has a query just for it, this would be redundant.
	            	try{
		            	W.checkAdvertisement(TheAdv);
		            }catch ( PatternRepetitionException e){
	            		try {
							discovery_service.flushAdvertisement(TheAdv);
						} catch (IOException e1) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							e1.printStackTrace();
						}
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
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            	
	            }
	        }
		}	*/		
	}
	

	/**
	 * Receiver for the SourceDirector.  Since it is a one way channel. this 
	 * Class does not do anything.
	 * 
	 * @author jfvillal
	 *
	 */
	public static class SourceDirectorDbgPipeReceiver implements PipeMsgListener{

		public void pipeMsgEvent(PipeMsgEvent event) {
			//do nothing
		}
	}
	/**
	 * This class does not do anything.  it has a receiver listener in a pipe where
	 * only the send command is used.
	 * @author jfvillal
	 *
	 */
	public static class SinkDirectorObserverDbgPipeReceiver implements PipeMsgListener{
		public void pipeMsgEvent(PipeMsgEvent event) {
			//do nothing
		}
	}
	/**
	 * Connects to other Rendezvous JXSE nodes.
	 * @return
	 */
	private boolean ConnectToFellowRendezVousDirectors() {
		boolean ans = false;
		//Log.log(Level.FINEST, "Funtion return true for Debugggin purposes ***");
		long start = System.currentTimeMillis();
		if( ParentNode.getNetManager().waitForRendezvousConnection(ParentNode.TIMEOUT)){
			ans = true;
		}
		long time = System.currentTimeMillis() - start;
		Log.log(Level.FINEST, "Waited for fellow redezvou : " + time );
		
		return ans;
	}
}
