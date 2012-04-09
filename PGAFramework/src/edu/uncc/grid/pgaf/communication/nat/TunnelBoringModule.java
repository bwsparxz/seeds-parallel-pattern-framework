/* * Copyright (c) Jeremy Villalobos 2009
 *  
  * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.communication.nat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.communication.wan.java.JavaSocketDispatcher;
import edu.uncc.grid.pgaf.p2p.AdvertFactory;
import edu.uncc.grid.pgaf.p2p.Node;

import net.jxta.pipe.PipeID;
import net.jxta.socket.JxtaServerSocket;
/**
 * The TBM is a module that encapsulates the Tunnel management.  The DRDV is not involved in any of the transaction.
 * The module mostly routes messages between multiple parties where one node is inside a NAT and the other is 
 * on the WAN it can also be used for NAT-to-NAT, although it is slower.
 * 
 * The DRDV should instantiate this class and start it.  once started, this class will start a thread that receives
 * TunnelClient requests.  Each TunnelClient on the Remote Leaf Node (RLN) is managed by a TunnelServer.  The
 * TBM keeps a list of TunnelServers.
 * 
 * The other thread checks the TunnelInstroctor messages.  These messages request new dispatcher and new jxta sockets
 * 
 * In summary, the number of threats on this class are:
 * 		*Dispatcher Thread
 * 		*Instruction Thread
 * 
 * Each TunnelServer uses a SocketManager (Client or Server), which uses two threads.  Most of these threads are used
 * to deal with Interrupts, they don't use much CPU power.
 * 
 * @author jfvillal
 *@deprecated We are no longer seeking inter-grid connections.  We are now focusing on the cloud and multi-core
 *  This class will be removed from the framework soon !
 */
public class TunnelBoringModule {
	private List<TunnelServer> Q;
	TunnelDispatcher TDispatcher;
	TunnelInstructionsHandler TIHandler;
	public TunnelBoringModule(PipeID pipe_id, Node network) throws IOException{
		TDispatcher = new TunnelDispatcher(pipe_id, network);
		TDispatcher.setName("Tunnel Boring Machine Thread Router Server");
		
		TIHandler = new TunnelInstructionsHandler();
		TIHandler.setName("Tunnel Boring Machine Thread Instruction Handler");
	}
	/**
	 * 
	 * This is used by the TBM to accept requests for new Virtual pipes to the 
	 * local Nodes
	 * 
	 * @author jfvillal
	 *
	 */
	public class TunnelDispatcher extends Thread{
		/**
		 * The Jxta Server Object
		 */
		JxtaServerSocket JxtaVirtualRouteServer;
		/**
		 * The Java Server Object
		 */
		ServerSocket JavaVirtualRouteServer;
		
		
		Node N;
		boolean Stop;
		public TunnelDispatcher(PipeID pipe_id, Node network) throws IOException{
			int backlog = 20;
			int timeout = 2000;
			N = network;
			if( network.isJavaSocketPort()){
				JavaVirtualRouteServer = new ServerSocket( network.getTunnelJavaSocketPortInternal(), backlog);
				JavaVirtualRouteServer.setSoTimeout(10000);
				JxtaVirtualRouteServer = null;
			}else{
				JxtaVirtualRouteServer = new JxtaServerSocket( network.getNetPeerGroup()
						, AdvertFactory.getVirtualRouteAdvertisement(pipe_id)
						, backlog, timeout);	
				JavaVirtualRouteServer = null;
			}
			
			Q = Collections.synchronizedList(new ArrayList<TunnelServer>());
			this.setDaemon(true);
		}
		public void run(){
			int sockets_created = 0;
			while( !Stop){
				try {
					Socket socket = null;
					if( N.isJavaSocketPort()){
						socket = JavaVirtualRouteServer.accept();
					}else{
						socket = JxtaVirtualRouteServer.accept();
					}
					
					if( socket != null){
						TunnelServer ts = new TunnelServer( N , socket);
						synchronized(Q){
							Q.add(ts );
						}
						++sockets_created;
						Node.getLog().log(Level.FINEST, " Added Virtual socket " + sockets_created );
					}
				} catch (IOException e) {
					// most exception here are TimeOuts 
					//Node.getLog().log(Level.FINEST, " TIMEOUT - TunnelBoringModule " );
				} catch (ClassNotFoundException e) {
					
					e.printStackTrace();
				}
			}
			Node.getLog().log(Level.FINE, "DISPATCHER THREAD THREAD out of main RUN loop");
		}
		public void setStop(){
			Stop = true;
		}
		public List<TunnelServer> getQ(){
			return Q;
		}
	}
	/**
	 * This thread processes the Instruction Containers sent by each of the 
	 * TunnelServers
	 * 
	 * @author jfvillal
	 *
	 */
	public class TunnelInstructionsHandler extends Thread{
		boolean Stop;
		public TunnelInstructionsHandler(){
			Stop = false;
			this.setDaemon(true);
		}
		public void run(){
			try{
				while( !Stop ){
					/**
					 * The Q is synchronized to prevent a ConcurrentModificationException
					 * The other thread accessing it is TunnelDispatcher, which adds new 
					 * ServerTunnels to the List.  Now it would get a change form 200 millis
					 * before this loop starts over.
					 */
					synchronized( Q ){
						Iterator<TunnelServer> tunnel_it = Q.iterator();
						while( tunnel_it.hasNext()){
							try {
								TunnelServer server = tunnel_it.next();
							
								//for each instruction, provide a resolutions
								server.handleInstructionContainer();
									
								//if one of those instruction was a shutdown instruction
								//delete the server from the list
								if( !server.isBound() ){
									tunnel_it.remove();
								}else{		//continue checking the
								
								//add new SocketManagers to main Map
								server.addServerSMListToMap();
								}
								
							} catch (IOException e) {
								Node.getLog().log(Level.INFO, Node.getStringFromErrorStack(e));
							} catch (ClassNotFoundException e) {
								Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
							} catch (InterruptedException e) {
								Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
							}
						}
						
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}catch ( Exception e){
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
			Node.getLog().log(Level.FINE, "TBM INSTRUCTION HANDLE THREAD out of main RUN loop");
		}
		public boolean isStop() {
			return Stop;
		}
		public void setStop() {
			Stop = true;
		}
	}
	/**
	 * Stops the tunnels
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void stopTunnels() throws IOException, InterruptedException{
		Iterator<TunnelServer> tunnel_it = Q.iterator();
		while( tunnel_it.hasNext()){
	
				TunnelServer server = tunnel_it.next();
				server.closeConnections();
				server.close();
		}
		
	}
	/**
	 * Starts the dispatcher thread and the Instruction Handler thread.
	 */
	public void startThreads(){
		this.TDispatcher.start();
		this.TIHandler.start();
	}
	/**
	 * Stops dispatcher thread and Instruction Handler thread.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void stopThreads() throws InterruptedException, IOException{
		TDispatcher.setStop();
		TDispatcher.join(100);
		if( TDispatcher.isAlive()){
			TDispatcher.interrupt();
		}
		/**
		 * close each of the Tunnel Servers
		 * although they are expected to be ordered to close from the
		 * ClientTunnel
		 */
		stopTunnels();
		
		TIHandler.setStop();
		TIHandler.join(100);
		
		if( TIHandler.isAlive()){
			TIHandler.interrupt();
		}
		
	}
}
