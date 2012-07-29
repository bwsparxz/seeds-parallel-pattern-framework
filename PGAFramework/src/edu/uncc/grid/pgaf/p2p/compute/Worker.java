package edu.uncc.grid.pgaf.p2p.compute;

import java.util.Map;

import net.jxta.id.ID;
/**
 * The worker class manages the threads on a multicore server.  Each server has one node that connects to 
 * the P2P network.  This parent class is the basic class needed by UserThread to work.  The UserThread
 * manages one thread out of multiple threads in the multicore.
 * 
 * The DesktopWorker inherits this class to manage Seeds when running on a single client ( a Desktop )
 * 	 This class communicates using shared memory techniques.  No JXTA connection is necessary although
 *   JXTA classes are still used to reuse the network code.
 * The NetworkWorker is the older implementation.  This manages the node in any server even the client computer.
 *   This class assumes a running Node with an active JXTA connection.
 * @author jfvillal
 *
 */
public class Worker {
	Map<ID, Boolean> HaveIWorkedOnThisPatternAlready;
}
