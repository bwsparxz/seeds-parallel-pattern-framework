/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 *  Other libraries and code used.
 *  
 *  This framework also used code and libraries from the Java
 *  Cog Kit project.  http://www.cogkit.org
 *  
 *  This product includes software developed by Sun Microsystems, Inc. 
 *  for JXTA(TM) technology.
 *  
 *  Also, code from UPNPLib is used
 *  
 *  And some extremely modified code from Practical JXTA is used.  
 *  This product includes software developed by DawningStreams, Inc.    
 *  
 */
package edu.uncc.grid.pgaf.communication.shared;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;

import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionChangeListener;
import edu.uncc.grid.pgaf.communication.ConnectionChangedMessage;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.Types.LinkType;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;


/**
 * This class implement the ConnectionManager using shared memory.  It allows multiple User threads
 * to communicate within a multi-core or multi-cpu motherboard without incurring the network communication
 * overhead.
 * 
 * @author jfvillal
 *
 */
public class SharedMemManager implements ConnectionManager{
	/**
	 * shared variables
	 */

	SharedVariables Vars;
	/**
	 * individual variables
	 */
	/**
	 * index value for the sender queue
	 */
	protected int Sender;
	/**
	 * index value for the receiver queue
	 */
	protected int Receiver;
	protected int LocalNBR;
	protected int SharedNBR;
	boolean DataflowSource;
	//TODO
	protected ConnectionChangeListener CCListener;
	protected boolean SentUserInitClass;
	/**
	 * Remote Node Connection Data ID is used to send the data id used for this connection
	 * on the networked connection.  I don't know if they are of much use for the shared memory, but
	 * they are implemented to avoid potential problems with null pointers
	 */
	private Long RemoteNodeConnectionDataID;
	/**
	 * holds the pipeid for the dispatcher on the remote host.
	 */
	private String RemoteNodeConnectionPipeID;
	/**
	 * The boolean is clien sets if the object behaves a the server side or the client side of the connection.
	 * the remote_pipe_id, is the pipe id of the connections hosted by the remote node.
	 * the remote data id is the id of the segment that the remote node has.
	 * @param is_client
	 * @param var
	 * @param remote_pipe_id
	 * @param remote_data_id
	 */
	public SharedMemManager( boolean is_client, SharedVariables var, String remote_pipe_id, Long remote_data_id){
		Vars = var;
		RemoteNodeConnectionDataID = remote_data_id;
		RemoteNodeConnectionPipeID = remote_pipe_id;
		/**
		 * The client's sender queue is the servers receiver queue and 
		 * the server's sender queue is the client's receiver queue
		 * 
		 * also, the LocalNBR is the Q we use to store users listeners for packets that will arrive in 
		 * the future.  The SharedNBR is used to inform the other object Object that there is new 
		 * data in the receiver side 
		 */
		if(is_client){
			Sender = 0;
			Receiver = 1;
			LocalNBR = 0;
			SharedNBR  = 1;
		}else{
			Sender = 1;
			Receiver = 0;
			LocalNBR = 1;
			SharedNBR  = 0;
		}
	}
	/**
	 * Sends object to the shared memory node.
	 * @throws ConnectionHibernatedException 
	 */
	@Override
	public void SendObject(Serializable obj) throws InterruptedException,IOException {
		/**
		 * because the shared memory does not have two threads to manage a "socket" as is the case
		 * of the socket connection manager.  I need to add an algorithm here that will inform the other side that 
		 * a connection hibernation call was issued.
		 */
		if( this.isDataflowSource() ){//no data should be lost as long as it is a source 
			//because a source only produces, and the receiving end is for control messages only.
			this.pollRecvingObject();
		}
		this.addSendingObject(obj);
	}

	@Override
	public void addRecvingObject(Serializable obj) throws InterruptedException {
		//Vars.Queue[Receiver].add(obj);
		Vars.addQueue(Receiver, obj);
	}

	@Override  //synchronized taken out Jan 22 2011
	public /*synchronized*/ void addSendingObject(Serializable obj) throws InterruptedException {
		/**
		 * if the user has a listener waiting for the message, send the object to 
		 * that listener, which should stored on the ShareNBR (the NBR from the
		 * other object).
		 * Else, add it to the sender Q
		 */
		
		if( Vars.peekNBRecv(this.SharedNBR) != null ){    // Vars.NBRecv[this.SharedNBR].peek() != null){ //if not empty
			NonBlockReceiver rcv =  Vars.pollNBRecv( SharedNBR );//Vars.NBRecv[SharedNBR].poll();
			rcv.Update(obj);
		}else{
			Vars.addQueue(Sender, obj);
			//Vars.Queue[Sender].add( obj);
		}
	//	notify(); //taken out jan 22 2011
	}
	
	@Override
	public void nonblockRecvingObject(NonBlockReceiver rcv)
			throws InterruptedException {
		
		
		/**
		 * This needs to be executed as a block because, a thread could put a packet in the queue just
		 * after we go into putting the NonBlockReceiver in the queue.  this coincidence would also
		 * make the other tread check the NonBlockReceiver queue just before it gets an Object in it.
		 */
		
		//Vars.NBRecv[LocalNBR].add(rcv);
		Vars.addNBRecv(LocalNBR, rcv);
		
		//Node.getLog().log(Level.FINEST, "Vars.Queue[Receiver=" + Receiver + " ] size: " + Vars.Queue[Receiver].peek() + " pipe_id: " + Vars.HRemotePeerID.toString() + " " + Vars.HCommPipeID.toString() );
		while(  Vars.peekQueue(Receiver)/*Vars.Queue[Receiver].peek()*/ != null 
				&& Vars.peekNBRecv(LocalNBR) != null /*Vars.NBRecv[LocalNBR].peek() !=null*/ ){ //if not empty
			
			NonBlockReceiver r = Vars.pollNBRecv(LocalNBR); //Vars.NBRecv[LocalNBR].poll();
			r.Update( Vars.pollQueue(Receiver) ) ; //Vars.Queue[Receiver].poll() );
		}	
		
		
		
	
	}
	@Override
	public Serializable pollRecvingObject() throws InterruptedException {
		//return Vars.Queue[Receiver].poll();
		Serializable ans = Vars.pollQueue(Receiver);
		if( ans instanceof ConnectionChangedMessage){
			CCListener.onHibernateConnection( (ConnectionChangedMessage) ans );
		}
		return ans;
	}
	
	@Override
	public Serializable takeRecvingObject() throws InterruptedException {
		/*Serializable s = Vars.Queue[Receiver].poll();
		while( s == null){
			wait();
			s = Vars.Queue[Receiver].poll();
		}*/
		Serializable ans = Vars.takeQueue(Receiver);  //Vars.Queue[Receiver].take();
		if( ans instanceof ConnectionChangedMessage){
			CCListener.onHibernateConnection( (ConnectionChangedMessage) ans );
			/**
			 * call this method recursively in case there are back to back 
			 * ConnectionChangedMessages, which does happen.
			 */
			ans = takeRecvingObject();
		}
		return ans;
	}
	@Override
	public void close() throws IOException, InterruptedException {
		/**
		 * does nothing there is no streams to manage
		 */
	}
	/**
	 * The method answer does it has data to be proceessed on queue?
	 * so true if it has yet to receive more data that is waiting on queue
	 * false if there is no more data on the queue.
	 * returns true when it has something on the queue.
	 * 
	 */
	@Override
	public boolean hasRecvData() {
		//return !this.Vars.Queue[Receiver].isEmpty();
		return !this.Vars.isEmptyQueue( Receiver );
	}
	/**
	 * true if it still has data
	 */
	@Override
	public boolean hasSendData() {
		//return !this.Vars.Queue[Sender].isEmpty();
		boolean ans = !this.Vars.isEmptyQueue(Sender);
		return ans;
	}
	@Override
	public boolean isBound() {
		return this.Vars.Bound;
	}
	

	@Override
	public void setBound(boolean bound) {
		this.Vars.Bound = bound;
	}

	@Override
	public edu.uncc.grid.pgaf.p2p.Types.LinkType getConnectionType() {
		return Types.LinkType.SHARED;
	}
	@Override
	public SlimJxtaID getHashPipeId() {
		return Vars.HCommPipeID;
	}

	@Override
	public SlimJxtaID getHashRemotePeerId() {
		return Vars.HRemotePeerID;
	}
	@Override
	public void setHashPipeId(SlimJxtaID id) {
		Vars.HCommPipeID = id;
		
	}
	@Override
	public void setHashRemotePeerId(SlimJxtaID id) {
		Vars.HRemotePeerID = id;	
	}
	@Override
	public Long getRemoteNodeConnectionDataID() {
		return RemoteNodeConnectionDataID;
	}
	@Override
	public String getRemoteNodeConnectionPipeID() {
		return RemoteNodeConnectionPipeID;
	}

	@Override
	public boolean isSendFull() {
		//int test = this.Vars.Queue[Sender].size();
		//return this.Vars.Queue[Sender].size() >= CommunicationConstants.QUEUE_SIZE;
		return Vars.sizeQueue(Sender) >= CommunicationConstants.QUEUE_SIZE;
	}

	@Override
	public PipeID getUniqueDependencyID() {
		return Vars.UniqueDependencyID;
	}

	@Override
	public void setUniqueDependencyID(PipeID id) {
		Vars.UniqueDependencyID = id;
	}

	@Override
	public HierarchicalDependencyID getDependencyID() {
		return Vars.DependencyID;
	}

	@Override
	public void setDependencyID(HierarchicalDependencyID id) {
		Vars.DependencyID = id;
	}

	@Override
	public void setConnectionChangeListener(ConnectionChangeListener monitor) {
		CCListener = monitor;
	}
	@Override
	public boolean isDataflowSource() {
		return this.DataflowSource;
	}
	@Override
	public void setDataflowSource(boolean set) {
		this.DataflowSource = set;
	}
	@Override
	public String getCachedObjSize() {
		return "S:" + Vars.sizeQueue(Sender) + "R:" + Vars.sizeQueue(Receiver);
	}
	
	@Override
	public RawByteEncoder getRawByteEncoder() {
		return null;
		//does nothing, this is used for a socket based connection only
	}
	
}
