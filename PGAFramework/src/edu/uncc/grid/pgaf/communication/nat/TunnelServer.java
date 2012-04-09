/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 */
package edu.uncc.grid.pgaf.communication.nat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.protocol.PipeAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.instruction.ConnectionOperation;
import edu.uncc.grid.pgaf.communication.nat.instruction.ConnectionRequest;
import edu.uncc.grid.pgaf.communication.wan.ClientSocketManager;
import edu.uncc.grid.pgaf.communication.wan.ServerSocketManager;
import edu.uncc.grid.pgaf.communication.wan.SocketDispatcher;
import edu.uncc.grid.pgaf.communication.wan.java.JavaClientSocketManager;
import edu.uncc.grid.pgaf.p2p.AdvertFactory;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;


/**
 * 
 * This class is used by the Director RDV to manage the connection sockets for each of the 
 * Virtual Pipes
 * @author jfvillal
 *@deprecated  We are no longer seeking inter-grid connections.  We are now focusing on the cloud and multi-core
 *  This class will be removed from the framework soon !
 */
                                 //replace socketmanager with more efficient manager
public class TunnelServer extends TunnelManager{
	Socket Connection;
	SlimJxtaID ClientPeerID;
	Node 	Network;
	/**TODO*/
	private List<SocketDispatcher> DispatcherList;
	//private Map<String, Map<String, SocketManager>> SMMap;
	
	
	public TunnelServer( Node network, Socket socket) throws IOException, ClassNotFoundException{
		super(false);
		//SMMap = new HashMap<String, Map<String,SocketManager>>();
		DispatcherList = new ArrayList<SocketDispatcher>();
		
		Network = network;
		if( socket != null){
			Connection = socket;
			//StreamOut = Connection.getOutputStream();
			
			Connection.setSoTimeout(CommunicationConstants.SOCKET_TIMEOUT);
			//ObjectOutputStream stream = new ObjectOutputStream( StreamOut );
			StreamOut = new ObjectOutputStream( Connection.getOutputStream());
			
			ConnectionOperation ins = new ConnectionOperation();
			//ins.setRouteAdv(null);
			//ins.setSrcPeer(Node.getPID());
			ins.setHashSrcPeerID(new SlimJxtaID( CommunicationConstants.TUNNEL_ID_ACC, Node.getPID()));
			ins.setHashCPipeID(new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, Node.getPID()));
			
			//ins.setVPInstruction(Types.VirtualPipeInstruction.HAND_SHAKE);
			ins.setOperation(ConnectionOperation.HAND_SHAKE);
			StreamOut.writeObject( ins );
			StreamOut.flush();
			
			StreamIn = new ObjectInputStream(Connection.getInputStream());
			//ObjectInputStream i_stream = new ObjectInputStream( StreamIn );
			
			ins = (ConnectionOperation) StreamIn.readObject();
			ClientPeerID = ins.getHashSrcPeerID(); //getSrcPeer();
			if( ins.getOperation() != ConnectionOperation.HAND_SHAKE){//.getVPInstruction() != Types.VirtualPipeInstruction.HAND_SHAKE){
				throw new IOException(" The VPINstructon was not HAND_SHAKE ....");
			}
			/*********** starts receive and send threads ***********/
			Bound = true;	
			this.runThreads(); 
		}else{
			throw new IOException(" socket cannot be null ");
		}
		
	}
	/**
	 * Function to handle requests for the creation of hosted clients and hosted dispatchers.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws NoPortAvailableToOpenException 
	 */
	public void handleInstructionContainer() throws IOException, ClassNotFoundException, InterruptedException, NoPortAvailableToOpenException{
		Serializable obj = null;
		/**Handle all the Instruction container gathered so far*/
		while((obj = this.RecvInstructions.poll()) != null){
			if( obj.getClass().getName().compareTo(ConnectionRequest.class.getName()) == 0){
				ConnectionRequest instruction = (ConnectionRequest) obj;
				/*
				 * ******************************REQUEST CONNECT*********************************************** 
				 * will make this server create a socket between here and the destination node.
				 * Then the packets from that other node will be routed to the client on the other side of the 
				 * tunnel..
				 * Since we are createing a client connection, the other side on the WAN network we have 
				 * a dispatcher waitin for this server tunnel to contact.
				 */
				if( instruction.getVPInstruction() == instruction.REQUEST_CONNECT){
					Node.getLog().log(Level.FINER, "Creating a ClientSM pipe: " + instruction.getCPipeID());
					PipeAdvertisement p_adv = AdvertFactory.GetDataLinkPipeAdvertisement(instruction.getCPipeID());
					
					/**TODO check the advertisement, to see if a Java or Jxta socket should be used*/
					
					ClientSocketManager client = null;
					
					//if( Network.isJavaSocketPort()){
						client = new JavaClientSocketManager(instruction.getWanAddress(), instruction.getPort()
								, instruction.getLocalPipeID(), instruction.getLocalDataID(), null, null, null);
					/*}else{
						client = new JxtaClientSocketManager(Network, p_adv
								, instruction.getLocalPipeID(), instruction.getLocalDataID());
					}*/
					client.setHashPipeId(new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, instruction.getCPipeID()) );
					client.setHashRemotePeerId(new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, instruction.getSrcPeer()));				
					
					if(client.isConected() ){
						Node.getLog().log(Level.FINER, "Creating a ClientSM ... pipe connected to remote host ");
						//if connected, put the socket manager in a place where we can accesses it
						Map<SlimJxtaID, ConnectionManager> m = new HashMap<SlimJxtaID, ConnectionManager>();
						m.put(new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, instruction.getSrcPeer()), client);
						this.ConnectionManagerMap.put(new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, instruction.getCPipeID()), m);
						
						//send bounded update to the Tunnel Client
						ConnectionOperation ins = new ConnectionOperation();
						ins.setHashCPipeID(client.getHashPipeId());
						ins.setHashSrcPeerID(client.getHashRemotePeerId());
						//ins.setRouteAdv(null);
						ins.setOperation(ConnectionOperation.SET_BOUND_TRUE);
						
						this.SendObject(ins);
					}else{
						Node.getLog().log(Level.FINER, "Creating a ClientSM .. pipe did not connect");
					}
				}else
					/*
					 * ************************************REQUEST_DISPATCHER********************************** 
					 * means we will create a dispatcher on this DirectorRDV and the connection
					 * that connect to it will receive a socket.  a list of those socket is kept a 
					 * DispatcherList.  all the objects receive to that list will be routed to the tunnel client.
					 * because it is a list of dispatcher. that could include multple sockets created from 
					 * the multiple dispatcher.
					 * */
				if(instruction.getVPInstruction() == ConnectionRequest.REQUEST_DISPATCHER){
					Node.getLog().log(Level.FINER, "Creating a SM Dispatcher ");
					// create and start dispatcher
					List<ConnectionManager> lst =  new ArrayList<ConnectionManager>();
					
					SocketDispatcher dispatcher = null;
					//if( Network.isJavaSocketPort()){
						dispatcher = new TunnelCustomJavaSocketDispatcher( Network,  instruction.getRCPipeID(), lst, this, instruction.getLocalDataID());
					/*}else{
						dispatcher = new TunnelCustomJxtaSocketDispatcher( Network, instruction.getRCPipeID(), lst, this, instruction.getLocalDataID() );
					}*/
						
					dispatcher.start();//start the dispatcher thread
					DispatcherList.add(dispatcher);
					
					//publish advertisement
					
					
					
					DataLinkAdvertisement advert =  (DataLinkAdvertisement) 
						AdvertisementFactory.newAdvertisement(DataLinkAdvertisement.getAdvertisementType());
					advert.setDataID(instruction.getDataID());
					advert.setDataLinkPipeID(instruction.getCPipeID());
					advert.setGridName(Node.getGridName());
					advert.setWanOrNat(instruction.getWanOrNat().toString());
					advert.setRDataLinkPipeID(instruction.getRCPipeID());
					advert.setLanAddress(instruction.getLanAddress());
					advert.setWanAddress(instruction.getWanAddress());
					advert.setPatternID(instruction.getPatternID());
					/*create a port for this dispatcher (if Java Sockets are being used) */
					if( Network.isJavaSocketPort()){
						int port = ((TunnelCustomJavaSocketDispatcher)dispatcher).getPort();
						advert.setPort( port );
						Node.getLog().log(Level.FINER, "Creating a SM Dispatcher... publishing NAT advertisement JavaSocket info: port: "+ port  
								+ "\n address: " + instruction.getWanAddress() );
					}else{
						advert.setPort( -1 );
						Node.getLog().log(Level.FINER, "Creating a SM Dispatcher... publishing NAT advertisement " );
					}
					
					
					DiscoveryService discovery_service = Network.getNetPeerGroup().getDiscoveryService();
					discovery_service.publish(advert, 35000, 35000);		
					discovery_service.remotePublish(advert);
				}
			}else if(obj.getClass().getName().compareTo(ConnectionOperation.class.getName()) == 0){
				ConnectionOperation instruction = (ConnectionOperation) obj;
				if(instruction.getOperation() == ConnectionOperation.REQUEST_CLOSE){
					Node.getLog().log(Level.FINER, " Closing conections for pipe " + instruction.getHashCPipeID() );
					/**
					 * The ClientTunnel has sent the instruction to close the conection Dispatcher or normal socket
					 */
					//this.close();
					//this.Bound = false;
					Map<SlimJxtaID,ConnectionManager> man = this.ConnectionManagerMap.get(instruction.getHashCPipeID());
					
					Collection<ConnectionManager> connection_list = man.values();
					Iterator<ConnectionManager> it = connection_list.iterator();
					while(it.hasNext()){
						ConnectionManager curr_man = it.next();
						Node.getLog().log(Level.FINER, " Closing pipe " + instruction.getHashCPipeID()  + " peer id " + instruction.getHashSrcPeerID() );
						curr_man.close();
					}
					
				}
			}	
		}
	}
	/**
	 * This moves the ConnectionManager from the ServerSocketManager to the list of 
	 * ConnectionManager the TunnelServer has.  The TunnelServer keeps the ConnectionsManagers
	 * in a Map.  the objects are not duplicated, but they are referenced both on the stand 
	 * allone socke managers and on the TunnelServer side.
	 */
	public void addServerSMListToMap(){
		Iterator<SocketDispatcher> d_it = DispatcherList.iterator();
		boolean add_this_map_to_the_map = false;
		/**go through every dispatcher created so far*/
		while( d_it.hasNext()){
			/**TODO create a general SocketDispatcher so that it can be used on the List. the interface 
			 * should have the functions that JxtaSocketDispatcher and JavaSocketDispatcher have in common.*/
			
			SocketDispatcher d = d_it.next();
			/** get the map for this pipe. If it does not exist, create it*/
			Map<SlimJxtaID,ConnectionManager> map = this.ConnectionManagerMap.get(d.getServiceHPipeID());
			if( map == null){
				map = new HashMap<SlimJxtaID,ConnectionManager>();
				add_this_map_to_the_map = true;
			}
			/** Move the SokcetManager from the Dispatcher to the map*/
			Iterator<ConnectionManager> l_sm = d.getServerSMList().iterator();
			while( l_sm.hasNext()){
				ServerSocketManager manager = (ServerSocketManager) l_sm.next();
				map.put( manager.getHashRemotePeerId(), manager);
				l_sm.remove();
			}
			/** If a new map was created, add it to the map*/
			if(add_this_map_to_the_map){
				this.ConnectionManagerMap.put(d.getServiceHPipeID(), map);
			}
		}
	}
	/**
	 * Not implmented.  Should check if ConnectionManagers are bounded.
	 */
	public void checkBoundnes(){
		//go through each socket and send a list of sockets that are bounded to 
		//the tunnel client
	}
	/**
	 * Closes Tunnel.
	 */
	public void close() throws IOException, InterruptedException{
		if(isBound()){
			Iterator<SocketDispatcher> it = this.DispatcherList.iterator();
			while( it.hasNext() ){
				SocketDispatcher cher = it.next();
				cher.setStop(true);
				cher.interrupt();
			}
			super.close();
			Connection.close();
		}
	}
	/**
	 * Checks boundness for the tunnel.
	 * @return
	 */
	public boolean isPipeStablished(){
		return Connection.isBound();
	}


}
