/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 */
package edu.uncc.grid.pgaf.communication;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;

import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.communication.shared.ConnectionHibernatedException;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.p2p.Types.LinkType;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;

/**
 * This interface is a generic form to refer to a connection.
 * SocketManager implements this interface with two queue.
 * The other options will accommodate the Virtual Pipes, Socket pipes, 
 * and redirected pipes.  
 * 
 * @author jfvillal
 *
 */

public interface ConnectionManager {
	/**
	 * take will block until an object is available.  If there are objects, it will
	 * return the object at the head of the queue.
	 * @return
	 * @throws InterruptedException
	 */
	public Serializable takeRecvingObject() throws InterruptedException;
	/**
	 * poll is non-blocking.  It returns the object at the head of the queue and 
	 * returns null if the queue is empty
	 * @return
	 * @throws InterruptedException 
	 */
	public Serializable pollRecvingObject() throws InterruptedException;
	/**
	 * This function receives a listener, which is called when an object is available
	 * this is used if the user wants non-blocking behavior but do not want to have 
	 * a constantly running loop polling the connection.  Instead, the user submits the 
	 * listener, and the listener is called as soon as an object is available.
	 * @param rcv
	 * @throws InterruptedException
	 */
	public void nonblockRecvingObject( NonBlockReceiver rcv) throws InterruptedException;
	
	/**
	 *TODO Continue this code 
	 * This sets the Listener that will respond to a connection change request.  the 
	 * other side of this connection can request a connection change by sending a
	 * ConnectionChangedMessage.
	 * 
	 * @param monitor
	 * TODO
	 */
	public void setConnectionChangeListener( ConnectionChangeListener monitor);
	/**
	 * return true if data is available, false otherwise.
	 * @return
	 */
	public boolean hasRecvData();
	
	/**
	 * Puts the obj in a queue to be sent 
	 * @param obj
	 * @throws InterruptedException
	 * @throws ConnectionHibernatedException 
	 */
	public void SendObject(Serializable obj) throws InterruptedException, IOException;
	/**
	 * returns true if there are objects in the queue, false otherwise.
	 * @return
	 */
	public boolean hasSendData();
	/**
	 * returns true if the sending queue is full.
	 * @return
	 */
	public boolean isSendFull();
	
	/**
	 * Returns the dependency id. For the data-flow behavior.
	 * @return
	 */
	public HierarchicalDependencyID getDependencyID();
	
	/**
	 * Sets the dependency id for the data flow behavior.
	 */
	public void setDependencyID( HierarchicalDependencyID id);
	
	public PipeID getUniqueDependencyID( );
	public void setUniqueDependencyID( PipeID pipe_id);
	
	/**
	 * For a controling class to add objects that have being received.
	 * @param obj
	 * @throws InterruptedException 
	 */
	public void addRecvingObject( Serializable obj) throws InterruptedException;
	public void addSendingObject( Serializable obj) throws InterruptedException;
	/**
	 * gets the PipeId which is needed for tunneling
	 * @return
	 */
	public SlimJxtaID getHashPipeId();
	/**
	 * gets the PeerId which is needed for tunneling
	 * @return
	 */
	public SlimJxtaID getHashRemotePeerId();
	/**
	 * Sets the PipeID this ConnectionManager is under.  A JXSE socket starts with a PipeID.
	 * This class stores it to be used as part of an identifier
	 * @param id
	 */
	public void setHashPipeId(SlimJxtaID id );
	/**
	 * Sets the RemotePeerID.  This is part of the local and remote PipeID system.  The 
	 * local id is used to connect directly to the node proposing the connection.  The 
	 * remote PipeId is used to connect to a DirectorRDV that redirects the conection
	 * for a NAT, and in the future for a Firewalled node.
	 * @param id
	 */
	public void setHashRemotePeerId(SlimJxtaID id );
	/**
	 * returns if the connection (or socket) is bound.
	 * @return
	 */
	public boolean isBound();
	/**
	 * Sets bound variable.
	 * @param bound
	 */
	public void setBound(boolean bound);
	/**
	 * Closes the conections.  It tries to give a heads up to the other node that this action is 
	 * being done.  However, at the time IOErrors are not uncommon.  However, if caught properly 
	 * should not be a problem.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void close() throws IOException, InterruptedException;
	/**
	 * return the connection type.  See edu.uncc.grid.pgaf.interfaces.LinkType for 
	 * values
	 * @return
	 */
	public LinkType getConnectionType();
	/**
	 * returns the DataID for the comunication link.  or a flag if no data id is being used
	 * @return
	 */
	public Long getRemoteNodeConnectionDataID();
	/**
	 * returns the pipe id the remote node is used to host its own connections.
	 * 
	 * @return
	 */
	public String getRemoteNodeConnectionPipeID();
	/**
	 * sets whether this connection manager is a source or sink for 
	 * the dataflow functionality
	 */
	public void setDataflowSource(boolean set);
	/**
	 * returns true if this connection manager is the source in
	 * dataflow mode.
	 * @return
	 */
	public boolean isDataflowSource();
	/**
	 * Should return the number of objects in the cache for debuggin purposes.
	 * @return
	 */
	public String getCachedObjSize();
	
	/**
	 * Returns the raw byte encoder.
	 * @return
	 */
	public RawByteEncoder getRawByteEncoder( );
	

}
