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
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;

import net.jxta.document.Advertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocket;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.instruction.ConnectionOperation;
import edu.uncc.grid.pgaf.communication.nat.instruction.ConnectionRequest;
import edu.uncc.grid.pgaf.communication.wan.SocketManager;
import edu.uncc.grid.pgaf.p2p.AdvertFactory;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
/**
 * This class manages the connection between a NAT Leaf Worker and the 
 * Director RDV that provides an indirect route
 * @author jfvillal
 *@deprecated  We are no longer seeking inter-grid connections.  We are now focusing on the cloud and multi-core
 *  This class will be removed from the framework soon !
 */
public class TunnelClient extends TunnelManager{
	/**
	 * Socket used for the Tunnel
	 * once the hand shake is done, control is mostly done by
	 * TunnelManager class 
	 ***/
	Socket ClientSocket; 
	/**TODO*/
	
	public TunnelClient( Node network, DirectorRDVAdvertisement rdv_advert ) throws IOException, ClassNotFoundException {
		super(true);
		if(network.isJavaSocketPort()){
			String hostname = rdv_advert.getLanAddress();
			int port = rdv_advert.getPort();
			ClientSocket = new Socket( hostname, port);
		}else{
			
			PipeAdvertisement pipe_adv = 
				AdvertFactory.GetDataLinkPipeAdvertisement( rdv_advert.getTunnelPipeID() );	
			
			ClientSocket = new JxtaSocket( network.getNetPeerGroup()
				,null
				,pipe_adv
				, 30000, true);
		}
		
		
		if(ClientSocket != null){
			this.Bound = true;
			ClientSocket.setSoTimeout(CommunicationConstants.SOCKET_TIMEOUT);
			StreamIn = new ObjectInputStream( ClientSocket.getInputStream() );
			StreamOut = new ObjectOutputStream(ClientSocket.getOutputStream());
			/**TODO*/
			
			ConnectionOperation ins = (ConnectionOperation) StreamIn.readObject();
			if( ins.getOperation() != ConnectionOperation.HAND_SHAKE){
				throw new IOException( " problem with hand shake ");
			}
			 Node.getLog().log(Level.FINE, "ClientTunnelManager: first read " );
			ins = new ConnectionOperation(); //new TunnelInstructor();
			//ins.setRouteAdv(null);
			ins.setHashCPipeID(new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, Node.getPID()) );
			ins.setHashSrcPeerID(new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, Node.getPID()) );
			ins.setOperation(ConnectionOperation.HAND_SHAKE); 
			
			StreamOut.writeObject(ins);
			StreamOut.flush();
			 Node.getLog().log(Level.FINE, "ClientTunnelManager: first write " );
			this.runThreads();
		}
	}
	/**
	 * Request a client connection to the TunnelServer
	 * @param advert the advertisemen of the node to connect to.
	 * @param external specifies if the connection is external (true) or internal (false).  External connects to the target node's directorRDV, and
	 * Internal connects directly to the target node.  
	 * @return a VirtualSocketManager for a virtual client connection
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public VirtualSocketManager requestVirtualSocket(DataLinkAdvertisement advert, boolean external, String local_pipe_id, Long local_data_id)
				throws InterruptedException, IOException{
		//request indirect route
		ConnectionRequest ins = new ConnectionRequest();
		ins.setSrcPeer(Node.getPID());
		ins.setVPInstruction(ConnectionRequest.REQUEST_CONNECT);//.setVPInstruction(Types.VirtualPipeInstruction.REQUEST_CONNECT);
		PipeID  id = null;
		if(external){						//decide which pipe to use to connect to the other node
			id = advert.getRDataLinkPipeID(); 		//connect to the node's DirectorRDV
		}else{
			id = advert.getDataLinkPipeID(); 		//connect to the node directly
		}
		ins.setCPipeID(id);
		ins.setWanAddress(advert.getWanAddress());
		ins.setLanAddress(advert.getLanAddress());
		ins.setPatternID(advert.getPatternID());
		ins.setPort(advert.getPort());
		ins.setLocalPipeID(local_pipe_id);
		ins.setLocalDataID(local_data_id);
		//ins.setRouteAdv(null);
		//add VirtualSocketManager to the queue
		this.SendObject(ins);
		
		/*Create Virtual Socket.  and retur it.  This is the socket tha would be used to 
		 * send and receive data.
		 * return VirtualSocketManager
		 */
		
		VirtualSocketManager man = new VirtualSocketManager( new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC,id)
														, new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC,Node.getPID()),this );
		HashMap<SlimJxtaID,ConnectionManager> h = new HashMap<SlimJxtaID,ConnectionManager>();
		h.put(man.getHashRemotePeerId(),man);
		ConnectionManagerMap.put(man.getHashPipeId(), h );
		return man; 
		//there needs to be a place where the boundness of the VirtualSocket is updated
	}
	/**
	 * Request a Dispatcher to the TunnelServer
	 * @param Adv  A data link advertisment
	 * @param adv_user_list the advanced user's list.   Make sure it is thread safe!
	 * @throws InterruptedException  
	 * @throws IOException
	 */
	public void requestSocketDispatcher(DataLinkAdvertisement Adv, List<ConnectionManager> adv_user_list) 
			throws InterruptedException, IOException{
		Node.getLog().log(Level.FINER, " Requesting Remote dispatcher  ");
		//Create sockets for the conections
		ConnectionRequest ins = new ConnectionRequest();
		ins.setCPipeID(Adv.getDataLinkPipeID());
		ins.setSrcPeer(null);//to be set on the other end
		ins.setVPInstruction(ConnectionRequest.REQUEST_DISPATCHER); //.setVPInstruction(Types.VirtualPipeInstruction.REQUEST_DISPATCHER);
		ins.setRCPipeID(Adv.getRDataLinkPipeID());
		ins.setWanOrNat(Adv.getWanOrNat());
		ins.setDataID(Adv.getDataID());
		ins.setPatternID(Adv.getPatternID());
		ins.setLocalPipeID(Adv.getDataLinkPipeID().toString()); //mpi-like comm
		ins.setLocalDataID(Adv.getDataID()); //mpi-like comm
		/*
		 * if this is a NAT node, which it has to be to call this function, the WAN address will be
		 * pointing to the DirectorRDV
		 */
		ins.setWanAddress(Adv.getWanAddress());
		ins.setLanAddress(Adv.getLanAddress());
		/*
		 * It is the Job of the DirectorRDV to determine which port will be used to host the dispatcher 
		 */
		ins.setPort( 0 );
		
		this.SendObject(ins);
		
		//this.addConnectionManagerContainer(ins.getRCPipeID());
		
		Map<SlimJxtaID,ConnectionManager> pipe_map = Collections.synchronizedMap(new HashMap<SlimJxtaID,ConnectionManager>());
		
		SlimJxtaID r_pipe_key = new SlimJxtaID( CommunicationConstants.TUNNEL_ID_ACC, ins.getRCPipeID());
		
		Node.getLog().log(Level.FINER, " Added dispatcher with pipe key : " + r_pipe_key + " peer key: ");
		
		this.ConnectionManagerMap.put( r_pipe_key, pipe_map );
		
		this.AdvUserList.put( r_pipe_key, adv_user_list);
		
		Map<SlimJxtaID,ConnectionManager> test_element = this.ConnectionManagerMap.get(r_pipe_key);
		if( test_element == null){
			Node.getLog().log(Level.FINER, " test element is null  : " + r_pipe_key + " peer key: ");
		}else{
			Node.getLog().log(Level.FINER, " test element exists : " + r_pipe_key + " peer key: ");
		}
		
		//make sure to set peer_id and pipe_id on the VirtualServerM before adding it to the Map  --DONE--
	}
	/**
	 * The function will process the instruction on the Maintenance thread, while being used on the User's thread
	 * 
	 */
	public void handleInstructionContainer(){
		Serializable obj = null;
		/**Handle all the Instruction container gathered so far*/
		while((obj = this.RecvInstructions.poll()) != null){
			if( obj.getClass().getName().compareTo(ConnectionOperation.class.getName()) == 0){
				ConnectionOperation instruction = (ConnectionOperation) obj;
				if( instruction.getOperation() == ConnectionOperation.SET_BOUND_TRUE){ //Types.VirtualPipeInstruction.SET_BOUND_TRUE){
					Node.getLog().log(Level.FINER, " Setting VSM Bound to TRUE pipe_id " + instruction.getHashCPipeID().toString() 
							+ " peer_id: " + instruction.getHashSrcPeerID().toString());
					
					Map<SlimJxtaID ,ConnectionManager> manager_container = this.ConnectionManagerMap.get( instruction.getHashCPipeID() );
					if( manager_container == null){
						throw new NullPointerException("manager_container is null");
					}else{
						ConnectionManager man = manager_container.get( instruction.getHashSrcPeerID() );
						if( man == null){
							throw new NullPointerException("man is null");
						}else{
							Node.getLog().log(Level.FINER, " Setting VSM Bound to TRUE pipe_id ");
							man.setBound(true);
						}
					}
				}else if ( instruction.getOperation() == ConnectionOperation.SET_BOUND_FALSE){//.getVPInstruction() == Types.VirtualPipeInstruction.SET_BOUND_FALSE){
					Node.getLog().log(Level.FINER, "Setting VSM Bound to FALSE");
					
					Map<SlimJxtaID,ConnectionManager> manager_container =  this.ConnectionManagerMap.get( instruction.getHashCPipeID() );
					if( manager_container == null){
						throw new NullPointerException("manager_container is null");//there most be something wrong
					}else{
						ConnectionManager man = manager_container.get( instruction.getHashSrcPeerID());
						if( man == null){
							throw new NullPointerException("man is null");//something is wrong
						}else{
							man.setBound(false);
						}
					}
				}else if ( instruction.getOperation() == ConnectionOperation.SOCKET_CREATED){//.getVPInstruction() == Types.VirtualPipeInstruction.SOCKET_CREATED){
					Node.getLog().log(Level.FINER, "Creating a VirtualSM from a remotely hosted Dispatcher ");
	
					VirtualSocketManager man = null;
					
					//SlimJxtaID pipe_key = new SlimJxtaID(TunnelServer.TunnelDefaultSlimJxtaIDAccuracy, instruction.getRCPipeID());
					
					Map<SlimJxtaID , ConnectionManager> pipe_map = this.ConnectionManagerMap.get( instruction.getHashCPipeID() );
					if( pipe_map == null){
						Node.getLog().log(Level.FINE,"TunnelClient: the pipe_map is null for some reason ");
						pipe_map = Collections.synchronizedMap(new HashMap<SlimJxtaID,ConnectionManager>());
						ConnectionManagerMap.put(instruction.getHashCPipeID() , pipe_map);
					}
					
					
					man = (VirtualSocketManager) pipe_map.get(instruction.getHashSrcPeerID());
					
					if( man == null){
						man = new VirtualSocketManager(instruction.getHashCPipeID(), instruction.getHashSrcPeerID(), this);
						man.setBound(true);
						Node.getLog().log(Level.FINE, " Adding new socket from hosted dispathcer ");	
						
						pipe_map.put( man.getSlimRemotePeerId() , man);
						
						//this.addConnectionManager(man);
						
						List<ConnectionManager> user_sm = AdvUserList.get(instruction.getHashCPipeID());
						synchronized( user_sm ){
							user_sm.add(man);	
						}
					}else{
						Node.getLog().log(Level.FINE, " Adding new socket from hosted dispathcer .. the manager existed previously ");
					}
					
				}
			}
		}
	}
	/**
	 * Closes the Tunnel
	 */
	public void close() throws IOException, InterruptedException{
		if( isBound()){
			super.close();
			this.ClientSocket.close();
			/**TODO send instruction to close the tunnel*/
		}
	}
	/**
	 * return true if the pipe is bounded
	 * @return
	 */
	public boolean isConnected(){
		return ClientSocket.isBound();
		/**TODO*/
	}
}
