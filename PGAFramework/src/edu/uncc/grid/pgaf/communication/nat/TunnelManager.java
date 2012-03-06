/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * * All rights reserved
 */
package edu.uncc.grid.pgaf.communication.nat;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;

import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;

import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.instruction.CloseTunnelInstruction;
import edu.uncc.grid.pgaf.communication.nat.instruction.ConnectionOperation;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
/**
 * This class manages the Input and Output Streams for the Tunnel between a LeafWorker and the 
 * DirectorRDV.
 * 
 * 
 * @author jfvillal
 *
 */

public class TunnelManager {
	public static final int QUEUE_MAX = 100;
	//hash maps of PID to Virtual Sockets
	/** 
	 * The map holds the VirtualSocketManager's that have queue for the respective packet<br>
	 * <br>
	 * The first key is the PipeID<br> 
	 * <br>
	 * The second key is either the SrcNode ID if it corresponds to a ClientSocket or 
	 *  the RemoteNode ID if it is a ServerSocket<br> 
	 * 
	 * */
	Map<SlimJxtaID,Map<SlimJxtaID,ConnectionManager>> ConnectionManagerMap;
	protected BlockingQueue<Serializable> RecvInstructions;
	protected Map<SlimJxtaID, List<ConnectionManager>> AdvUserList;
	//private BlockingQueue<Transferable> SendingQueue;
	
	public volatile boolean RStop;
	public volatile boolean SStop;
	public boolean isTunnelClient;
	
	//public ObjectOutputStream OutStream;
	
	public ObjectInputStream StreamIn;
	public ObjectOutputStream StreamOut;
	//public ObjectInputStream InStream;
	
	RecverThread R;
	SenderThread S;
	
	protected volatile boolean Bound;
	
	public TunnelManager(boolean is_tunnel_client){
		//SendingQueue = new ArrayBlockingQueue<Transferable>(QUEUE_MAX, true);
		RecvInstructions = new ArrayBlockingQueue<Serializable>(QUEUE_MAX, true);
		ConnectionManagerMap = Collections.synchronizedMap( new HashMap<SlimJxtaID, Map<SlimJxtaID, ConnectionManager> >() );
		/**
		 * The AdvUserList is to provide the Advanced user with a list of servers, and 
		 * not mix them with other Servers from other connections and threads
		 */
		AdvUserList = Collections.synchronizedMap( new HashMap<SlimJxtaID, List<ConnectionManager> >() );
		RStop = false;
		SStop = false;
		isTunnelClient = is_tunnel_client;
	}
	/**
	 * Sends object
	 * @param obj
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void SendObject(Serializable obj) throws InterruptedException, IOException {
		//ObjectOutputStream stream = new ObjectOutputStream( StreamOut);
		StreamOut.writeObject(obj);
		StreamOut.flush();
	}
	/**
	 * This class runs a separate thread that waits for new object from the InputStream
	 * @author jfvillal
	 *
	 */
	public class RecverThread extends Thread{
		TunnelManager Tunnel;
		public RecverThread( TunnelManager tunnel_manager){
			Tunnel = tunnel_manager;
			this.setDaemon(true);
		}
		public void run(){
			try {
	            if (StreamIn != null) { 
	            	//int progressive_wait = 1;
	            	
	                while(!RStop){
	                	try {
	                		//Node.getLog().log(Level.FINE, "TUNNEL MANAGER Waiting for an Object -------------");
	                		/**
	                		 * This algorithm below is somewhat inefficient and is not needed if the Framework works 
	                		 * perfectly, so there may be a FailSafe mode in which this algorithm can be used, but for
	                		 * performance mode, the code below should be skipped.
	                		 */
	                		/**
	                		 * Code does not work.  Sometimes, available reports 0 despite having data available
	                		 * if( IStream.available() == 0){
	                			Thread.sleep(progressive_wait > 500 ? progressive_wait = 500: ++progressive_wait);
	                			continue;
	                		}else{
	                			 progressive_wait = 1;
	                		}**/
	                		//ObjectInputStream stream = new ObjectInputStream( StreamIn );		
		                	Object obj = StreamIn.readObject();
		                	Serializable trans = (Serializable) obj; //data_reader.readObject();
		                	//Node.getLog().log(Level.FINE, "TUNNEL MANAGER Got One -------------");
		                	/* it is either a TunnelInstructor or a TunnelContainer */
		                	
		                	/**
		                	 * TODO take out reflection out of this loop
		                	 */
		                	
		                	if( trans.getClass().getName().compareTo(TunnelContainer.class.getName() ) != 0){
		                		//The instruction has to be processed at some point
		                		//could be here but haven't desided.
		                		Node.getLog().log(Level.FINE, "Putting Instructor in instructor Q");
		                		
		                		if( trans.getClass().getName().compareTo( CloseTunnelInstruction.class.getName()) == 0 ) {
		                			//t.getVPInstruction() == Types.VirtualPipeInstruction.CLOSE_TUNNEL){
		                			Node.getLog().log(Level.FINE, "Instructor says shut down.");
		                			close();
		                			break;//end loop
		                		}
		                		RecvInstructions.put(trans);
		                	}else{
		                		/**
		                		 * Make suer only one thread is modifying the ConnectionManagerMap at a time.
		                		 * The Recv releases the lock at the end of each cycle.  So does Send thread
		                		 * The user is encouraged to also set a lock to guarrantee thread safety
		                		 */
	                		
		                		//the container has the peer_id that is used to decide where
		                		//the Data packet goes
		                		//The VirtualSocket has to deal with the use of the data
		                		//System.out.println("TUNNELMANAGER -- received container ");
		                		Node.getLog().log(Level.FINE, " " + trans.getClass().getName());
		                		
		                		TunnelContainer con = (TunnelContainer) trans;
		                		Map<SlimJxtaID,ConnectionManager> map = ConnectionManagerMap.get(con.getPipeId());
		                		if( map == null){
		                			throw new NullPointerException("CMM Container is null.  pipe_id: " + con.getPipeId() );
		                		}
		                		ConnectionManager man = map.get( con.getPeerId() );
		                		/*
		                		 * If everything works as expected, we should only get a null man when the ClientTunnel
		                		 * is receiving a new ServerSocket.  So we create a new VirtualSocket and add it as
		                		 * ConnectionManager.
		                		 * 
		                		 * This does not happen with the Pipe key because the pipe key is added as soon as 
		                		 * the instruction is received from the Client by the Server.
		                		 * 
		                		 * 4/7/09 creating VSM's if they don't exist is not a good idea.  Instead, the boundness
		                		 * system will ensure they are created in a verification process. The code below should be
		                		 * deleted after testing of the new system
		                		 */
		                		if( man == null){
		                			
		                			man = new VirtualSocketManager( con.getPipeId(), con.getPeerId(), Tunnel);
		            				man.setBound(true);
		            				
		            				/**Add to CMM */
		            				//System.out.println("pipe ordered map size: " + ConnectionManagerMap.size());
		            				//System.out.println("peer ordered map size: " + map.size());
		            				map.put(man.getHashRemotePeerId(), man);
		            				
		            				//System.out.println("peer ordered map size: " + map.size());
		            				/**Add to user's list*/
		            				List<ConnectionManager> user_sm = AdvUserList.get(man.getHashPipeId());
		            				user_sm.add(man);	
		                		}		                		
	                			/**
	                			 * if we are a client
	                			 * 		TunnelSend = SM Send
		                		 * 		TunnelRecv = SM Recv
	                			 * if we are a server
	                			 * 		TunnelSend = SM Recv
	                			 * 		TunnelRecv = SM Send
	                			 */
		                		if( isTunnelClient){
		                			//add to recving queue
			                		//BlockingQueue<NonBlockReceiver> nbr = man.getNBRecv();
			                		//if( /* ! nbr.isEmpty()*/) {
			                		//	NonBlockReceiver r = nbr.take();
			                		//	synchronized( r){
			                		//		r.Update(con.getPayload());
			                		//		r.notify();
			                		//	}
			                		//}else{
			                			Node.getLog().log(Level.FINEST, "TUNNELCLIENT RECV THREAD Adding Object to ConnectionManager RECV Q");
			                			man.addRecvingObject( con.getPayload()		);
			                		//}
		                		}else{
		                			//add to send queue
		                			Node.getLog().log(Level.FINEST, "TUNNELSERVER RECV THREAD Adding Object to CM SEND Q ");
		                			man.addSendingObject( con.getPayload() );
		                		}
	                		}	
		                	
						}catch (ClassCastException e){
							Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
						} catch (ClassNotFoundException e) {
							Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
						} catch (InterruptedException e) {
						
							Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
						} catch(NullPointerException e) {
							Node.getLog().log(Level.SEVERE, e.getMessage() + " " + Node.getStringFromErrorStack(e) );
						}catch ( StreamCorruptedException curr){
							Node.getLog().log(Level.SEVERE, " Stream Corrupted, some data may be lost " + Node.getStringFromErrorStack(curr) );
							System.out.println(" Stream Corrupted, some data may be lost " + Node.getStringFromErrorStack(curr) );
						}catch ( SocketTimeoutException e){
							//this is trown if the connectio is idle for a while
							Node.getLog().log(Level.SEVERE, " " + Node.getStringFromErrorStack(e) );
						}
	                	
	                }
	            }
			}catch ( Exception e){
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				
				try {
					Tunnel.close();
				} catch (Exception e1) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				}
			}
			Node.getLog().log(Level.FINE, "TUNNEL RECEIVER THREAD out of main RUN loop");
		}
	}
	/**
	 * if the queue is too full, we'll send 100 item and wait until the next rotation before sending more
	 * objects.  This prevents one queue from busing the pipe.
	 */
	public static final int MAX_SEND_PER_TURN = 100;
	/**
	 * This thread constantly checks all the sockets to see if there is information that needs to be 
	 * sent across the tunnel.
	 * @author jfvillal
	 *
	 */
	public class SenderThread extends Thread{
		public SenderThread(){
			this.setDaemon(true);
		}
		@Override
		public void run() {
			
			try {
	            if (StreamOut != null) {
	            	
	                while(!SStop){
	                	//ObjectOutputStream stream = new ObjectOutputStream( StreamOut);	
	                	/**
	                	 * rest will prevent the loop  from spinning uncontrollably when there is no network
	                	 * trafic.  It is more efficient to use notify calls in the future.
	                	 */
	                	boolean rest = true;
	                	//System.out.println("SENDER RUNNING: " );
	                	Collection<Map<SlimJxtaID, ConnectionManager>> pipe_map = ConnectionManagerMap.values();
	                	//System.out.println(" ConnectionManager Pipe Map size: " + pipe_map.size() );
	                	Iterator<Map<SlimJxtaID, ConnectionManager>> pipe_map_i = pipe_map.iterator();
	                	/**
	                	 * go through each of the pipes on the Map
	                	 */
	                	while(pipe_map_i.hasNext()){
	                		Collection<ConnectionManager> connection_map = pipe_map_i.next().values();
	                		//System.out.println(" Conection Map size: " + connection_map.size() );
	                		Iterator<ConnectionManager> connection_map_i = connection_map.iterator();
	                		/**
	                		 * go through each of the peer in that pipe
	                		 */
	                		while( connection_map_i.hasNext()){
	                			ConnectionManager manager = connection_map_i.next();
	                			
	                			BlockingQueue<Serializable> Q = null;
	                			/**
	                			 * if we are a client
	                			 * 		TunnelSend = SM Send
		                		 * 		TunnelRecv = SM Recv
	                			 * if we are a server
	                			 * 		TunnelSend = SM Recv
	                			 * 		TunnelRecv = SM Send
	                			 */
	                			if( isTunnelClient ){
	                				//TODO TunnelManager needs to be broken down into the client and server parts
	                				//or some other implementation
	                				//Q = manager.getSendingQueue();
	                			}else{
	                				//TODO fix this class 
	                				//Q = manager.getRecvingQueue();
	                			}
	                			//System.out.println(" Q size: " + Q.size() );
	                			/**
	                			 * Transfer some of the Objects of that object, or until empty
	                			 * 
	                			 * In case some queues are loaded faster than they are being emptied, it 
	                			 * is better to transfer a certain amount and the move on to the next 
	                			 * queue in line
	                			 */
	                			for(int i = 0; i < MAX_SEND_PER_TURN; i++){
	                				
	                				Serializable obj;
	                				obj = Q.poll();//not using take to make sure we don't get deadlocked
	                				if( obj != null){
		                				TunnelContainer con =  new TunnelContainer(
		                						  manager.getHashPipeId()
		                						, manager.getHashRemotePeerId(), obj);
		                				
		                				//if( isTunnelClient){
		                				//	Node.getLog().log(Level.FINE, "<TunnelClient> Sending new TunnelContainer ");
		                				//}else{
		                				Node.getLog().log(Level.FINEST, "<TunnelServer> Sending new TunnelContainer " 
		                						+ " pipe: " + manager.getHashPipeId() + " peer " + manager.getHashRemotePeerId() );
		                				//}
		                				
		                				StreamOut.writeObject(con);
		                				StreamOut.flush();
		                				StreamOut.reset();
		                				//System.out.println("TUNNELMANAGER -- sent container ");
	                				}
	                			}
	                			if( !Q.isEmpty() ){
	                				rest = false;
	                			}
	                			/**
	                			 * Check the Connection is still bounded, if not report it to the TunnelClient
	                			 */
	                			if( !manager.isBound() ){
	                				ConnectionOperation ins = new ConnectionOperation();
	                				ins.setHashSrcPeerID(manager.getHashRemotePeerId());
	                				ins.setHashCPipeID(manager.getHashPipeId());
	                				//ins.setRouteAdv(null);
	                				ins.setOperation(ConnectionOperation.SET_BOUND_FALSE);
	                				StreamOut.writeObject(ins);
	                				StreamOut.flush();
	                			}
	                		}
	                	}
                	
	                	if( rest){
	                		try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
								Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
							}
	                	}
	                }   	
	                //System.out.println("Out of main TunnelSend Loop" );
	            	
	            }
			}catch (Exception e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				/**
				 * Irrecoverable error, try to clean up a bit.
				 */
				Bound = false;
				SStop = true;
				RStop = true;
			}
			Node.getLog().log(Level.FINE, "TUNNEL SENDER THREAD out of main RUN loop");
			Node.getLog().log(Level.SEVERE, "If you are reading this message, this means the TunnelManager has not been fixed !!!!!!!!!");
		}
	}
	/**
	 * starts sender and receiver threads
	 */
	public void  runThreads(){
		R = new RecverThread(this);
		S = new SenderThread();
		R.start();
		S.start();
	}
	/**
	 * closes connections as orderly as possible
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void closeConnections() throws IOException, InterruptedException{
		Collection<Map<SlimJxtaID, ConnectionManager>> pipe_map = ConnectionManagerMap.values();
    	//System.out.println(" ConnectionManager Pipe Map size: " + pipe_map.size() );
    	Iterator<Map<SlimJxtaID, ConnectionManager>> pipe_map_i = pipe_map.iterator();
    	/**
    	 * go through each of the pipes on the Map
    	 */
    	while(pipe_map_i.hasNext()){
    		Collection<ConnectionManager> connection_map = pipe_map_i.next().values();
    		//System.out.println(" Conection Map size: " + connection_map.size() );
    		Iterator<ConnectionManager> connection_map_i = connection_map.iterator();
    		/**
    		 * go through each of the peer in that pipe
    		 */
    		while( connection_map_i.hasNext()){
    			ConnectionManager manager = connection_map_i.next();
    			if( manager != null){
    				if( manager.isBound()){
    					manager.close();
    				}
    			}
    			connection_map_i.remove();
    		}
    	}
    	
	}
	/**
	 * A more general close.  It calls closeConnections().
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void close() throws IOException, InterruptedException{
		if( Bound){
			Bound = false;
			/**Send signal that this socket is closing*/
			CloseTunnelInstruction ins = new CloseTunnelInstruction();
			SStop = true;
			RStop = true;
			ObjectOutputStream stream = new ObjectOutputStream( StreamOut);
			stream.writeObject(ins);
			stream.flush();
			closeConnections();
			this.joinThreads(); //the sender and receiver threads
			StreamOut.close();
			StreamIn.close();
		}
	}
	/**
	 * Returns true if it has received data.
	 * @return
	 */
	public boolean hasRecvData() {
		//override on subclass
		return false;
	}
	/**
	 * Waits 200 milliseconds for the threads to join.
	 * @throws InterruptedException
	 */
	public void joinThreads() throws InterruptedException{
		R.interrupt();
		S.interrupt();
		R.join(200);
		S.join(200);
	}
	/**
	 * Adds a ConnectionManager to the Users List.  Mostly used by the ClientTunnel.
	 * @param key
	 * @param list
	 */
	public void putAdvanceUserList(SlimJxtaID key, List<ConnectionManager> list){
		this.AdvUserList.put(key, list);
	}
	/**
	 * True if bounded.
	 * @return
	 */
	public boolean isBound() {
		return Bound;
	}
	/**
	 * Sets bound.
	 * @param bound
	 */
	public void setBound(boolean bound) {
		Bound = bound;
	}
	/**
	 * Adds new connection manager to the Tunnels MAP.  The connection manager has to have the 
	 * peerid and the pipe id set.  Use this function with care.
	 * @param man
	 */
	public static byte DefaultIDAccuracy = SlimJxtaID.RECOMMENDED_ACCURACY; 
	public void addConnectionManager( ConnectionManager man ){
		Map<SlimJxtaID,ConnectionManager> manager_container = 
			this.ConnectionManagerMap.get( man.getHashPipeId());
		if( manager_container == null){
			throw new NullPointerException("manager_container is null");//there must be something wrong
		}
		
		this.ConnectionManagerMap.put(man.getHashPipeId(),manager_container);
	}
	
	/**
	 * Adds an empty Map to the CMM.  The Map will begin to fill once the dispatcher gets new 
	 * sockets for that pipe.
	 * @param id
	 * @param container
	 */
	public void addConnectionManagerContainer( PipeID id ){
		Map<SlimJxtaID,ConnectionManager> map = Collections.synchronizedMap(new HashMap<SlimJxtaID,ConnectionManager> ());
		SlimJxtaID key = new SlimJxtaID(DefaultIDAccuracy, id);
		ConnectionManagerMap.put(key, map);
	}
}
