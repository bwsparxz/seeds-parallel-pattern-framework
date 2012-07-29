/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *    * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.communication;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.communication.nat.TunnelClient;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.wan.ConnectionEstablishedListener;
import edu.uncc.grid.pgaf.communication.wan.java.JavaSocketDispatcher;
import edu.uncc.grid.pgaf.p2p.LeafWorker;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
/*
 * * ------------------------------------------from Director RDV Introduction----------------------------------------------
 *  *  April 1, 2009
 *  I have being adding the routing system.  The routing system will create three types of connections between nodes.  The
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
 *  	This has not being implemented jet, but it should be a series fo classes that manager communication withing the 
 *  same CPU for multi-core systems.
 *  
 *  The three systems would be wrapped with the MultiModePipe which would be used by the advanced user to create the 
 *  connections between the nodes for the templates that would ultimately be used by the user-programmer.
 * 
 */
/**
 * <p>The MultiModePipeDispatcher provides a server for a PipeID connection to the Node proposing the connection.  The Advanced
 * user would call this class on the server side and MultiModePipeClient on the client side.  The required list of ConnectionManagers
 * will be filled with connections as they are established.
 * </p>
 * <p>
 * NOTES ON HOW THE ADVERTISEMENT WORKS (DATA LINK ADVERTISEMENT)
 * The data link advertisement has two pipes, one local and one remote.  If the nodes is WAN, 
 * all other nodes will know this and use the DataLinkPipeID to connect to the dispatcher created
 * by the nodes.  If the node is NAT-NON-UPNP, then the nodes will connect to the remote data
 * link pipe.  The dispatcher for that pipe will be hosted by the DRDV in the same grid name as the
 * LLW
 * The Dispatcher can also be set to use Java Sockets which are faster.  This update does not include the NAT tunnels.
 * The NAT tunnel will continue to use JXTA Socket exclusively.
 * </p>
 * @author jfvillal
 *
 */
public class MultiModePipeDispatcher {
	/**
	 * PipeID for the communication channel at the local level, within the same GridName.
	 */
	PipeID CommPipeID;
	/**
	 * PipeID for the communication channel at the remote level, the other peer is uner a different GridName.
	 */
	PipeID RemoteCommPipeID;
	/**
	 * A Dispatcher for the direct socket side of this class
	 */
	//JxtaSocketDispatcher Dispatcher;
	JavaSocketDispatcher JavaDispatcher;
	//Map<String, ConnectionManager> Sockets;
	/**
	 * The AdvanceUsersList keeps all the server_socket the multi-pipe dispatcher creates.  The user passes this list to the application.  
	 * Because the dispatcher may handle up to three different connection types, the user needs to provide the list.  This list will
	 * be filled in with all three types of connections as they become available.
	 */
	List<ConnectionManager> AdvanceUsersList;
	
	DataLinkAdvertisement LinkAdvertisement;
	Node Context;
	
	boolean TunnelAvailable;
	/**
	 * Make sure the List is thread save by initializing it this way:
	 * 
	 * Collections.synchronizedList(new ArrayList<ConnectionManager>() );
	 * 
	 * @param network the Context that holds global variables for Seeds
	 * @param segment  the segment or integer id for this node
	 * @param adv_user_list this list will be filled with new ConnectionManagers as they are requested from the network.
	 * @throws TunnelNotAvailableException
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws NoPortAvailableToOpenException 
	 * @throws NATNotSupportedException 
	 */
	public MultiModePipeDispatcher(Node network, String thread_name, long segment
							, List<ConnectionManager> adv_user_list, PipeID pattern_id
			,RawByteEncoder enc )  
		throws InterruptedException, IOException, ClassNotFoundException, NoPortAvailableToOpenException, NATNotSupportedException{
		/**
		 * Note:  the network can now be null for exclusive communication using shared memory.
		 */
		Context = network;
		this.TunnelAvailable = false;
		CommPipeID =  IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		RemoteCommPipeID =  IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		this.AdvanceUsersList = adv_user_list;
		
		
		/**
		 * If the user set Java Socket use Java socket, else use JxtaSocket Dispatcher
		 *  
		 *  create wan dispatcher */
		if( network != null ){
			Node.getLog().log(Level.FINER, "Client's Socket type is " + (network.isJavaSocketPort()?"Java Socket":"Jxta Socket") );
		}
		/**
		 * The internal (WAN) pipe id is being used to map the connection.  The new class (1/3/10) Communicator is being
		 * used to provide an mpi-like connection. to do this, we need to have a pipe_id for each node even if they are not
		 * planning to start and manage a dispatcher.
		 */
		if( network != null ){
			String comp_pipe_id = CommPipeID.toString();
			JavaDispatcher = new JavaSocketDispatcher(network, thread_name, this.AdvanceUsersList, comp_pipe_id, segment, enc);
			JavaDispatcher.setName( "Seeds Java Dispatcher id-end-in:" 
					+ comp_pipe_id.substring(comp_pipe_id.length() - 5, comp_pipe_id.length() - 1) + " port: " 
					+ JavaDispatcher.getPort() );
			JavaDispatcher.setDaemon(true);
			JavaDispatcher.start();
		}
		
		
		/**Create the advertisement */
		LinkAdvertisement = (DataLinkAdvertisement) 
			AdvertisementFactory.newAdvertisement(DataLinkAdvertisement.getAdvertisementType());
		LinkAdvertisement.setDataID(segment);
		LinkAdvertisement.setGridName(Node.getGridName());
		LinkAdvertisement.setWanOrNat(Node.getNetworkType().toString());
		LinkAdvertisement.setDataLinkPipeID( CommPipeID );
		LinkAdvertisement.setRDataLinkPipeID(RemoteCommPipeID);
		LinkAdvertisement.setPatternID( pattern_id);
		if( network != null ){
			LinkAdvertisement.setLanAddress(network.getNetDetective().getLANAddress());
			LinkAdvertisement.setWanAddress(network.getNetDetective().getWANAddress());
			if( network.isJavaSocketPort()){
				LinkAdvertisement.setPort( JavaDispatcher.getPort() );
				
			}else{
				LinkAdvertisement.setPort(-1);
			}
		}
		if( network != null ){
			Node.getLog().log(Level.FINER, "MultiModeDispatcher \nLocal pipe: " +  CommPipeID.toURI().toString() 
				+ "\nRemote pipe: " + RemoteCommPipeID.toURI().toString() 
				+ "\nPeer ID: " + Node.PID.toURI().toString() 
				+ "\nPattern ID: " + pattern_id.toString() 
				+ "\nPort: " + JavaDispatcher.getPort() 
				+ "\ncode: FISK08");
		}
		
		/**if in NAT nat, request a hosted dispatcher. */
		if( Node.getNetworkType() == Types.WanOrNat.NAT_NON_UPNP){
			throw new NATNotSupportedException();
			//create tunnel dispatcher
			/*Node.getLog().log(Level.FINER, "Creating hosted dispatcher");
			//
			// Networks are known for their unpredictability.  If the computer is inside a very restricted network,
			// or the computer is running stand-alone, the tunnel may throw exceptions when it finds the network
			// is not up.  If this happens, the method getTunnel() will manage the exception and return a 
			// null.  
			//
			TunnelClient c = LeafWorker.getTunnel();
			if( c != null){
					c.requestSocketDispatcher(LinkAdvertisement, this.AdvanceUsersList);
					TunnelAvailable = true;
			}else{
				TunnelAvailable = false;
			}*/
		}
	
		
		/** Add the pipe id to the shared memory dispathcer */
		MultiModePipeMapper.SMDispatcher.addDispatcher( CommPipeID, adv_user_list, segment);
		
		/**
		 * feb 26 2010
		 * This next line will put the advertisement in the shared list of adverts and links.  This is 
		 * used by the node threads in the same cpu.  This will accomplish two things, first it will
		 * speedup the communication withing the same node.  Second, I won't have to modify the 
		 * last 5 lines of code of this function where the advertisment is not published if this
		 * node is NAT_NON_UPNP because it may be dependent on a RDV to communicate with other nodes.
		 * 
		 * On the other hand, if the node is really just a stand-alone application, then we need
		 * the advertisement to get to the other threads in the computer.  And the line below 
		 * accomplishes that.
		 */
		MultiModePipeMapper.checkAdvertisement(LinkAdvertisement, null);

		/** publish advertisement
		 * Only publish if we don't have a remote dispatcher.  Otherwise, the advertisement will be 
		 * publish on the RDV, this is done because the dispatcher has to start before and request
		 * is made.  Otherwise, the requests will time out and would require more complicated 
		 * code to do multiple trials to stablish connection.
		 * 
		 * 
		 */
		if( network != null ){
			if( Node.getNetworkType() != Types.WanOrNat.NAT_NON_UPNP ){
				Node.getLog().log(Level.FINER, "publishing datalink advert with data_id : " + segment);	
				DiscoveryService service = network.getNetPeerGroup().getDiscoveryService();
				///service.remotePublish(LinkAdvertisement, 30000 );
				service.publish(LinkAdvertisement);
			}
		}
	}
	
	public int getPort(){
		return this.JavaDispatcher.getPort();
	}
	
	public void setOnConnectionEstablishedListener( Integer segment, ConnectionEstablishedListener set){
		//this should also set something for the shared memory manager.
		MultiModePipeMapper.ConEstList.put( segment , set);
		//add the listener to the socket dispatcher.
		if( JavaDispatcher != null){
			JavaDispatcher.setOnConnectionEstablishedListener(set);
		}
	}
	/**
	 * Returns true if a Tunnel is available to this connection. False otherwise
	 * @return
	 */
	public boolean isTunnelAvailable(){
		return this.TunnelAvailable;
	}
	/*the next algorithm may need improvement*/
	/*public Collection<ConnectionManager> getConnections(){
		Collection<ConnectionManager> ans;
		List<ConnectionManager> wan_list = Dispatcher.getServerSMList();
		/ / /
		 * if we are using a tunnel, add the tunnel plus the normal socket
		 * to the list
		 * / / /
		if( Node.getNetworkType() == Types.WanOrNat.NAT_NON_UPNP){
			Sockets.clear();
			Iterator<ConnectionManager> it = wan_list.iterator();
			while( it.hasNext()){
				ConnectionManager man = it.next();
				Sockets.put(man.getRemotePeerId().toString(), man);	
			}
			//create tunnel dispatcher
			TunnelClient c = LeafWorker.getTunnel();
			if( c != null){
				Map<String,ConnectionManager> map = c.getConnectionManMap( CommPipeID );
				Collection<ConnectionManager> col = map.values();
				Iterator<ConnectionManager> col_it = col.iterator();
				while( col_it.hasNext()){
					ConnectionManager man = col_it.next();
					Sockets.put(man.getRemotePeerId().toString(), man);
				}
			}
			ans = Sockets.values();
		}else{
			/  *  else, return the normal socket list only * /
			ans = wan_list;
		}
		 
		return ans;
	}*/
	
	
	/**
	 * This sends the close signal on the Socket and Virtual nodes.
	 * The close signal should not have a problem with the connection dropping 
	 * in the middle of the transaction, such as would be the case when two
	 * nodes request to close the same channel.
	 */
	private void closeServers() throws IOException, InterruptedException{
		synchronized( AdvanceUsersList ){
			Iterator<ConnectionManager> it = this.AdvanceUsersList.iterator();
			while( it.hasNext()){
				ConnectionManager m = it.next();
				m.close();
			}
		}
	}
	public void StopListeningForNewConnections(){
		MultiModePipeMapper.SMDispatcher.removeDispatcher( CommPipeID );
		if( JavaDispatcher != null){
			JavaDispatcher.setStop(true);
		}
		try {
			JavaDispatcher.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*if( Dispatcher != null){
			Dispatcher.setStop(true);
		}*/
	}
	/**
	 * Stops dispatcher and it also closes all the server pipe this dispatcher was managing.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void close() throws InterruptedException, IOException{
		this.stopDispatcher();
		this.closeServers();
		//Dispatcher = null;
		JavaDispatcher = null;
		//flush old advertisement from this node.
		if( Context != null ){//it could be null if running in multi-core mode.
			DiscoveryService service = Context.getNetPeerGroup().getDiscoveryService();
			service.flushAdvertisement( LinkAdvertisement );
		}
	}
	/**
	 * Stops the dispatcher from receiving new socket request, but the
	 * current pipes keep running.
	 * @throws InterruptedException
	 */
	public void stopDispatcher() throws InterruptedException{
		if( JavaDispatcher != null){
			JavaDispatcher.setStop(true);
			Node.getLog().log(Level.FINE, "Waiting for dispatcher to close...");
			JavaDispatcher.interrupt();
			JavaDispatcher.join();
			Node.getLog().log(Level.FINE, "Dispatcher closed.");
		}
		/*if( Dispatcher != null){
			Dispatcher.setStop(true);
			Dispatcher.interrupt();
			Node.getLog().log(Level.FINE, "Waiting for dispatcher to close...");
			Dispatcher.join();
			Node.getLog().log(Level.FINE, "Dispatcher closed.");
		}*/
	}
	
	
	
	public synchronized DataLinkAdvertisement getLinkAdvertisement() {
		return LinkAdvertisement;
	}

	public PipeID getCommunicationPipe(){
		return this.CommPipeID;
	}
}
