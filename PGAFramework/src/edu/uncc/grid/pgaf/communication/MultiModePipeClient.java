/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 */
package edu.uncc.grid.pgaf.communication;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.communication.nat.TunnelClient;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.nat.VirtualSocketManager;
import edu.uncc.grid.pgaf.communication.wan.java.JavaClientSocketManager;
import edu.uncc.grid.pgaf.p2p.LeafWorker;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

/**
 * This class at the moment presents a unified hand shake method to create 
 * A Client Socket connection.  It will provide the service to 
 * ClientSocketManager, 
 * VirtualSocketManager, and
 * SharedMemManager
 * 
 *  The class uses the ConnectionManager interface to return either of the 
 *  connection to the user in a way that the user can interact independent
 *  of the type of connection.
 * 
 * @author jfvillal
 *
 */

public abstract class MultiModePipeClient {
	public static final long WAIT_TIME = 1;
	/**
	 * provided for convenience.  it will block until the connection is established or throw a number of possible exceptions
	 * @param net  Network context.
	 * @param segment  Data id for the remote node
	 * @param pattern_id  pattern id for the pattern being used
	 * @param local_pipe pipe id for the local pipe
	 * @param local_segment ID for the local data
	 * @param timeout maximum time to wait before desist connection attempt 
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws TunnelNotAvailableException
	 * @throws CommunicationLinkTimeoutException
	 * @throws NATNotSupportedException 
	 * @throws DirtyAdvertisementException 
	 */
	public static ConnectionManager getClientConnection(	Node net
															, Long segment
															, PipeID pattern_id
															, String local_pipe
															, Long local_segment
															, PipeID unique_dependency_id
															, HierarchicalDependencyID dependency_id
															, RawByteEncoder enc
															, long timeout) 
										throws 	IOException, ClassNotFoundException
												, InterruptedException, TunnelNotAvailableException
												, CommunicationLinkTimeoutException, NATNotSupportedException{
		ConnectionManager m_manager = null;
		long time = System.currentTimeMillis();
		while( 
				(m_manager = MultiModePipeClient.getClientConnection(net
																	, segment
																	, pattern_id
																	, local_pipe
																	, local_segment
																	, unique_dependency_id
																	, dependency_id
																	, enc)
				) == null
			 )
		{
			Thread.sleep(WAIT_TIME);
			long timeout_elapsed = System.currentTimeMillis() - time;
			if( timeout_elapsed >= timeout){  //we have TIMEOUT time to get a connection from the receiving end.
				throw new CommunicationLinkTimeoutException("  Time alloted not enough: timeout at: " + timeout );
			}
			//Node.getLog().log(Level.FINEST, "wait for socket... my id: " + local_segment + " remote id: " + segment );
		}
		return m_manager;
	}
	/**
	 * returns null if there is no connection for that DataID.  Otherwise return
	 * a ConnectionManager wish is used to interact with the connection.
	 * 
	 * TODO create a getClientConnection that will block for t time or indefinetly while it wait for the 
	 * connections.
	 * 
	 * @param network network context
	 * @param segment
	 * @param pattern_id id for the pattern being used
	 * @param local_pipe id for the local pipe
	 * @param local_segment the data id used for the local data 
	 * @param dependency_id the id for the dependency (data flow behavior)
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws TunnelNotAvailableException 
	 * @throws NATNotSupportedException 
	 * @throws DirtyAdvertisementException 
	 */
	public static ConnectionManager getClientConnection(	Node network          //network context
															, Long segment			//remote segment number ( data id)
															, PipeID pattern_id		// pattern id
															, String local_pipe		// local pipe id
															, Long local_segment	// local segment number
															, PipeID unique_dependency_id  // data-flow dependency id.
															, HierarchicalDependencyID dependency_id
															, RawByteEncoder enc
														) 
				throws IOException, ClassNotFoundException, InterruptedException
				, TunnelNotAvailableException, NATNotSupportedException{
		ConnectionManager ans = null;
		/** look through the map to see if there is a pipe available.
		 * The Map is filled on the LeafWoker.RoutineAdvertPublisherQuerier */
		Map<Long, DataLinkAdvertisement> pattern_link_map = MultiModePipeMapper.DataLinkPipeAdvertisementList.get(pattern_id); //segment);
		if( pattern_link_map == null){
			Node.getLog().log(Level.FINER, " The advert is not here yet (pattern id not in map) " + pattern_id.toString());
			return null;
		}
		DataLinkAdvertisement adv = pattern_link_map.get(segment); 
		if( adv == null){
			Node.getLog().log(Level.FINER, " The advert is not here yet (advert not in map) " + segment);
			return null;
		}
		if( adv.isDirty() ){
			DiscoveryService s = network.getNetPeerGroup().getDiscoveryService();
			s.flushAdvertisement(adv);
			
			if( dependency_id != null){
				System.out.println("MultiModPipeClient:getClientConnection() DataID segment dirty: " + dependency_id.toString()
						+ " seg: " + adv.getDataID() 
						);	
			}else{
				System.out.println("MultiModPipeClient:getClientConnection() DataID segment dirty ");
			}
			//throw new DirtyAdvertisementException(  "DataID segment dirty: " + dependency_id.toString() );
			return null; //return null since it is outdated. (september 28 2010)
		}
		
		/**
		 * First try to get the client from shared memory.  If there is nothing there, try
		 * the direct and indirect pipes
		 */
		ans = MultiModePipeMapper.SMDispatcher.getClientSharedMemManager( 
							adv.getDataLinkPipeID(), local_pipe, local_segment, unique_dependency_id, dependency_id	);
		
		if( ans == null){
			new LinkQuery( DataLinkAdvertisement.DataIDTag, ""+segment, network);
			
			Node.getLog().log(Level.FINER, "Client's Socket type is " + (network.isJavaSocketPort()?"Java Socket":"Jxta Socket") );	
			if( Node.getNetworkType() == Types.WanOrNat.WAN || 
					Node.getNetworkType() == Types.WanOrNat.NAT_UPNP){
					/**if I am WAN or NAT-UPNP
					 *	if server is a NAT && the GridName is not mine, then we connect using a 
					 *	ClientSocketManager.  The other three cases are: 
					 **/
					if( Types.WanOrNat.valueOf(adv.getWanOrNat()) == Types.WanOrNat.NAT_NON_UPNP && 
							adv.getGridName().compareTo(Node.getGridName()) != 0){
						
						//if( network.isJavaSocketPort()){
							/**  the WAN address is that of the RDV.  this is done as soon as an RDV
							 * is known to the LeafWorker on the Advertisement Discovery method.
							 */
							Node.getLog().log(Level.FINER, "Client is WAN connection to NAT using remote pipe Java Socket" +
									"\n Host: " + adv.getWanAddress() + " port: " + adv.getPort() );
							
							JavaClientSocketManager man = new JavaClientSocketManager( adv.getWanAddress()
																			, adv.getPort()
																			, local_pipe
																			,local_segment
																			, unique_dependency_id
																			, dependency_id, enc) ;
							ans = man;
						/*}else{
					
							JxtaClientSocketManager man = new JxtaClientSocketManager(network
									, AdvertFactory.GetDataLinkPipeAdvertisement(adv.getRDataLinkPipeID())
									, local_pipe, local_segment	);
							ans = man;
							Node.getLog().log(Level.FINER, "Client is WAN connection to NAT using remote pipe JXTA Socket");
						}*/
						
					}else{
					/**
					 * The Remote being WAN and the GridName not the same
					 *     Remote being NAN and     GridName the same		^^
					 *     Remote being NAN and     GridName the same       ^^
					 * If the Gridname is the same ^^ , we connect as if all nodes where WAN, using
					 * ClientSocketManager.  if Grid the GridName is not the same, but the remote is
					 * WAN as well as I am, then we can use ClientSocketManager too.
					 */
						//if( network.isJavaSocketPort()){
							/*This soket uses Java Socket */
							JavaClientSocketManager man;
							if( adv.getGridName().compareTo(Node.getGridName()) == 0){
								/*if we have the same grid name, we shared the Local network*/
								man = new JavaClientSocketManager(adv.getLanAddress()
																	, adv.getPort()
																	, local_pipe
																	, local_segment
																	, unique_dependency_id
																	, dependency_id, enc);
							}else{
								/*if we don't have the same grid name, we can connect using WAN addresses.*/
								man = new JavaClientSocketManager(adv.getWanAddress()
																, adv.getPort()
																, local_pipe
																, local_segment
																, unique_dependency_id
																, dependency_id , enc);
							}
							ans = man;
							Node.getLog().log(Level.FINER, "Client is WAN connection to WAN using standard pipe (Java Socket)");
						/*}else{
							/This uses JXTA socket.  either same or different grid does not matter, the same
							  pipe id is used./
							JxtaClientSocketManager man = new JxtaClientSocketManager( network 
									,  AdvertFactory.GetDataLinkPipeAdvertisement(adv.getDataLinkPipeID() )
									, local_pipe, local_segment	);
							ans = man;
							Node.getLog().log(Level.FINER, "Client is WAN connection to WAN using standard pipe (Jxta Socket)");
						}*/
					}
			}else{
				throw new NATNotSupportedException();
				/** if I am NAT
				 * Then We need to requestJxtaVirtualSocket to the RDV's Tunnel except if the
				 * Connection Server has the same GridName, in that case we can connect as if 
				 * both of us where WAN.
				 * 
				 * The first case is 
				 * 		Remote is NAT and the GridName is not the same:  Since I am a NAT too
				 * 		I have to request a VirtualSocket to the RemotePipe of the server.  This
				 * 		is the case where the conection goes through two Tunnels, the Socket's
				 * 		Server Tunnel, and the Client Sockte's Tunnel
				 */
				/*TunnelClient c = null; 
				if( adv.getGridName().compareTo(Node.getGridName()) != 0 ){ //this if statement makes absolute sure the tunnel is needed.
					for( int i = 0; i < 5; i++){
						c = LeafWorker.getTunnel();
						if( c != null){ //try to be patient in gettin a Tunnel, this might be the 
							break;		//first access request.
						}else{
							Thread.sleep(100);
						}
					}
					if( c == null){
						throw new TunnelNotAvailableException();
					}
				}				
				if( Types.WanOrNat.valueOf(adv.getWanOrNat()) == Types.WanOrNat.NAT_NON_UPNP &&
						adv.getGridName().compareTo(Node.getGridName()) != 0){
					VirtualSocketManager man = c.requestVirtualSocket(adv, true
										, local_pipe, local_segment);//.getWanAddress(), adv.getPort(), adv.getRDataLinkPipeID());
					Node.getLog().log(Level.FINER, "Client is NAT connection to NAT using remote pipe");
					ans = man;
				}else{*/
					/**
					 *The three remaining cases are treated in two cases:
					 * 1.Remote WAN and GridName not the same
					 * 
					 * 2.Remote NAT and GridName the same
					 * 3.Remote WAN and GridName the same
					 * 
					 * For 2 and 3, if the GridName is the same, when can connect using a 
					 *   ClientSocketManager because we assume they belong to the same LAN IP range.
					 *   We also assume that two clusters of computers on the same organization would
					 *   be given different GridName.
					 * For 1, We need to reques a VirtualSocketManager.  The Remote is WAN, but I 
					 *   Cannot take the change of directly connect to it even though some NAT 
					 *   restricted Nodes will allow this connection if it is initiated on the 
					 *   local node.
					 */
				/*	if( Node.getGridName().compareTo(adv.getGridName()) == 0){
						//if( network.isJavaSocketPort()){
							JavaClientSocketManager man = new JavaClientSocketManager( adv.getLanAddress() 
																					, adv.getPort()
																					, local_pipe
																					, local_segment
																					, unique_dependency_id
																					,dependency_id, enc);
							ans = man;
							Node.getLog().log(Level.FINER, "Client is NAT connection to WAN using standar, we have the same GridName (Java Socket)");
						
					}else{
						// Jul 28
						// This case could also try to connect directly using a ClientsocketManager.  This may work for nodes like workstation
						// that can connect to servers on the internet, but would not be able to receive connection from outside.
						// Still, a test would be need to know if it is a LAN_LOCKED node or a NAT node.  LAN_LOCKEd would be like coit-grid05 that
						// can reach other servers with access to the Internet, but does not have any type of gateway to reach
						// the Internet itself.
						// 
						// This node is NAT conecting to a WAN remote pipe.
						///
						VirtualSocketManager man = c.requestVirtualSocket(adv, false
														, local_pipe, local_segment);//.getDataLinkPipeID());
						Node.getLog().log(Level.FINER, "Client is NAT connection to WAN using remote pipe we have diferent GridNames");
						ans = man;
					}
				}*/
			}
		}
		
		//make sure the connection manager is bounded.
		if( ans.isBound() ){
			return ans;
		}else{
			return null;
		}
	}
	
	public static DataLinkAdvertisement getDataAdvertisement(PipeID pattern_id, long segment ){
		/** look through the map too see if there is a pipe available.
		 * The Map is filled on the LeafWoker.RoutineAdvertPublisherQuerier */
		Map<Long, DataLinkAdvertisement> pattern_link_map = MultiModePipeMapper.DataLinkPipeAdvertisementList.get(pattern_id); //segment);
		if( pattern_link_map == null){
			Node.getLog().log(Level.FINER, " The advert is not in the map " + pattern_id.toString());
			return null;
		}
		DataLinkAdvertisement adv = pattern_link_map.get(segment); 
		
		return adv;
	}
	
	
	/**
	 * The listener will be called as soon as a pipe containing the DataID
	 * is found
	 * low priority method
	 * @param lst
	 */
	//public static void getClientConnectionListener( ClientConnectionListener lst){
		//Add listener to a list hosted by LeafWorker
	//}
}
