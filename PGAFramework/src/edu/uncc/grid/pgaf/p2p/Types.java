/* * Copyright (c) Jeremy Villalobos 2009
 * 
 */
package edu.uncc.grid.pgaf.p2p;
public class Types{
	/**
	 * Used on the Tunnels between NAT LeafWorkers and DirectorRDV
	 * @author jfvillal
	 *
	 */
	public enum VirtualPipeInstruction {
		HAND_SHAKE			//to start the pipe
		, REQUEST_CONNECT	//to request a single conection to the outside
		, REQUEST_DISPATCHER //to request hosting of socket sever
		, REQUEST_CLOSE 	//to request close of the pipe (will close all hosted connections)
		, SET_BOUND_TRUE	//	updates the boundness of the connection on the remote node
		, SET_BOUND_FALSE	//
		, SOCKET_CREATED 	//inform the TunnelClient a new VirtualSocketM was created by a 
							//dispatcher
		, CLOSE_TUNNEL
	}
	
	/**
	 * This is used to denote if the Node is a sink, source, or a compute node.
	 * The default is compute.  The Source and Sink "anchors" need to be 
	 * specified by the user.
	 * @author jfvillal
	 *
	 */
	public static enum DataFlowRole{
		SINK ,
		SOURCE,
		SINK_SOURCE,
		COMPUTE,
		IDLE //indicates a node that is waiting for a pattern.
	}
	/**
	 * Use to indicate if the Node (at the PGAF level) is a Director RDV or is a 
	 * LeafWorker.
	 * @author jfvillal
	 *
	 */
	public static enum PGANodeRole{
		DIRECTOR_RDV
		, LEAFWORKER
	}
	/**
	 * Used to show the type of communication link being created between two nodes
	 * @author jfvillal
	 *
	 */
	public static enum LinkType{
		SOCKET
		, INDIRECT
		, SHARED
	}
	/**
	 * Used to indicate what type of network exists around the node.
	 * @author jfvillal
	 *
	 */
	public static enum WanOrNat{
		UNKNOWN
		, WAN
		, NAT_UPNP
		, NAT_NON_UPNP
		, MULTI_CORE //used for Desktop run (only multi-core)
	}
	/**
	 * Used to pass instructions along with the users data
	 * @author jfvillal
	 *
	 */
	//TODO get rid of DataInstruction,  it is too big for the network
	//not a high priority though.
	public static enum DataInstruction{
		SHUTDOWN
		, JOBDONE
	}
	
	public class DataControl{
		/**
		 * byte instruction to tell the socket being closed that the network is shutting down as well.
		 */
		public static final byte INSTRUCTION_SHUTDOWN = 1;
		/**
		 * byte instruction to tell the node the job is done.  at this point the node can go back to 
		 * a state where it can load a new Template.
		 */
		public static final byte INSTRUCTION_JOBDONE = 2;
		/**
		 * This is the default instruction identification.  This should be used on any Instruction 
		 * class that is using the old form of the instruction system (enum)
		 */
		public static final byte INSTRUCTION_DEFAULT = 3;
		/**
		 * All user data should be ZERO (0) to tell the sistem to route the packet to the user's 
		 * interface.
		 */
		public static final byte INSTRUCTION_DATA = 0;
		
		/**
		 * Instruction control code to split 
		 * 
		 */
		public static final byte SPLIT = 5;
		
		/**
		 * Instruction control to coalesce
		 * 
		 */
		public static final byte COALESCE = 6;
		
		/**
		 * TODO use this class to also pass control directives to the lower level (Expert
		 * 
		 */
		/**
		 * This is to let know a node that all the work units have being allocated.
		 */
		public static final byte NO_WORK_FOR_YOU = 4;
	}
	/*public static enum DataLinkComm{
		DATALINK_REQ
		, DATALINK_ACK
	}*/
	public static enum Instruction{
		SHUTDOWN  /*Peaceful orderly shutdown */
		,TERMINATE /* exit the system immediately*/
	}
	public static enum TemplateType{
		WORKPOOL
		, GENERIC_PARALLEL
		, PIPE_LINE
		, TWOD_MATRIX
		, THREEMATRIX
		, COMPLETE_GRAPH
		
	}
}