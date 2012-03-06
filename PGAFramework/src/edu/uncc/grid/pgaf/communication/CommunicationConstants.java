/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 */
package edu.uncc.grid.pgaf.communication;

import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;

/**
 * This class encapsulated communication constants that are 
 * used on multiple communication classes.
 * @author jfvillal
 *
 */
public class CommunicationConstants {
	/**
	 * This determines the size used in the send and receive queue for the SocketManager
	 */
	static public final int QUEUE_SIZE = 100;
	/**
	 * The Tunnel_id_acc will determine how much information from the pipe_id is kept and transfered over the 
	 * link.  only about 4 bytes are necesary.
	 */
	static public final int TUNNEL_ID_ACC = SlimJxtaID.RECOMMENDED_ACCURACY;
	/**
	 * Sets the communication id accuracy.
	 */
	static public final int COMM_ID_ACC = SlimJxtaID.RECOMMENDED_ACCURACY;
	/**
	 * Used for unordered links.  We are calling unorder links to purely socket-based links.  there is
	 * an MPI-like interface that can be used with "ordered" patterns.
	 */
	public static final long SINKSOURCE_SEGMENT = -1;
	/**
	 * The dyanmic is the identifcation for the client.  
	 */
	public static final long DYNAMIC = -2;
	/**
	 * SOCKET_TIMEOUT
	 */
	public static final int SOCKET_TIMEOUT = 60000; //10 SECONDS
	
	public static final int DISPATCHER_TIMEOUT = 10000;
	/**
	 * Used to identify all Seeds' advertisements
	 */
	public final static String GridNameTag = "GridNameTag";
	/**
	 * Used to accept or reject a connection after the handshake, this is mostly used
	 * for the dataflow idea where a dispatcher may still refuse a connection if it 
	 * does not have a registered hierarchical id for that connection
	 * TODO could do bytes on ObjectStreams to reduce the connection data.
	 */
	public final static int CONNECTION_OK = 44;
	/**
	 * same as CONNECTION_OK but this one refuses the connection.s
	 */
	public final static int CONNECTION_REFUSE = 55;
	/**
	 * Sets how long to wait before attempting a new reconnect to a dependency.
	 */
	public final static int DEPENDENCY_RECONNECT_WAIT_INTERVAL = 100;
	/**
	 * Sets how long to wait for the dependency to connect.
	 */
	public final static int DEPENDENCY_TIMEOUT = 120000;
	
	
}
