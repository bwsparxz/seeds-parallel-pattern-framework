package edu.uncc.grid.pgaf.communication.nat;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;

import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.instruction.ConnectionOperation;
import edu.uncc.grid.pgaf.communication.wan.ServerSocketManager;
import edu.uncc.grid.pgaf.communication.wan.SocketDispatcher;
import edu.uncc.grid.pgaf.communication.wan.java.JavaSocketDispatcher;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;

public class TunnelCustomJavaSocketDispatcher extends JavaSocketDispatcher{
	TunnelServer TServer; 
	public TunnelCustomJavaSocketDispatcher( Node Network,	PipeID pipe_id,
			List<ConnectionManager> adv_user_list, TunnelServer ts, Long local_data_id) throws IOException, NoPortAvailableToOpenException {
		super(Network, "deprecated class tunnel dispatcher", adv_user_list, pipe_id.toString(), local_data_id, null);
		TServer = ts;
		ServicePipeID = new SlimJxtaID( CommunicationConstants.TUNNEL_ID_ACC,pipe_id);
		TServer.addConnectionManagerContainer(pipe_id);
	}
	/**
	 * This thread waits around for a peer to connect to the socket. It times out every so often.    
	 */
	@Override
	public void run() {
		try{
			int count = 0;
			while(!Stop){	
				//users data stuff
				try {
					Node.getLog().log(Level.FINER, "wating for connection with id: " + this.ServicePipeID.toString() );
					Socket socket  =  DataLinkServer.accept();
					if( socket != null){
						System.out.println("Adding one socket to queue ... " + count ++ );
						socket.setSoTimeout(CommunicationConstants.SOCKET_TIMEOUT);
						ServerSocketManager manager = new ServerSocketManager("not used", socket 
								,this.LocalPipeID, this.LocalDataID , null, null);
								
						manager.setHashPipeId(ServicePipeID);
						
						
						/*
						 * inform TunnelClient that a socket was created
						 */
						ConnectionOperation ins = new ConnectionOperation();
						//ins.setCPipeID(manager.getPipeId());
						//ins.setRCPipeID(manager.getPipeId());
						//ins.setSrcPeer(manager.getRemotePeerId());
						ins.setHashCPipeID(manager.getHashPipeId());
						ins.setHashSrcPeerID(manager.getHashRemotePeerId());
						
						ins.setOperation(ConnectionOperation.SOCKET_CREATED);
						TServer.SendObject(ins);
						Node.getLog().log(Level.FINE, " Informing ClientTunnel a new socket was added to the hosted dispatcher pipe " 
								+ manager.getHashPipeId() + " peer " + manager.getHashRemotePeerId() );
						
						/**
						 * add the connection manager to the list of CM's used by 
						 * the user
						 */
						this.AdvUserList.add(manager);
						/**
						 * Add the connection manager to this tunnels map
						 */
						TServer.addConnectionManager(manager);
						
					}
				} catch (IOException e) {
					Node.getLog().log(Level.WARNING, "Custome Dispatcher " + Node.getStringFromErrorStack(e));
				} catch (ClassNotFoundException e) {
					Node.getLog().log(Level.WARNING, "Custome Dispatcher " + Node.getStringFromErrorStack(e));
				} 
			}
			try {
				DataLinkServer.close();
			} catch (IOException e) {
				Node.getLog().log(Level.WARNING, "Custome Dispatcher " + Node.getStringFromErrorStack(e));
			}
		}catch ( Exception e){
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
		}
		Node.getLog().log(Level.FINE, "TUNNEL DISPATCHER out of main RUN loop");
	}
	
}
