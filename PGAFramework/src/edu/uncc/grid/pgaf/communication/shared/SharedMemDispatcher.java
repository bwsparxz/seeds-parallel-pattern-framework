/* * Copyright (c) Jeremy Villalobos 2009
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;

import java.util.Collections;


import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeMapper;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.communication.wan.ConnectionEstablishedListener;
import edu.uncc.grid.pgaf.communication.wan.SocketDispatcher;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

import java.io.IOException;
import java.io.Serializable;
/**
 * This class creates a sort of dispatcher for shared memory use.  The class has a hashmap that stores the
 * ID for the pipe and the connection manager.  The class also provides the client side function with
 * getClientSharedMemManager.
 * The main idea is to have a single pair of send and receive queue that are used by two processes.  Once
 * Nodes send queue is the other's received queue and vise versa.
 * @author jfvillal
 *
 */
public class SharedMemDispatcher  {
	/**
	* HostedDispatcher is a list of PipeID's to the thread id that is hosting the
	* pipe
	**/
	public Map<String, List<ConnectionManager>> HostedDispatchers; 
	public Map<String, Long> HostedDispatchersDataId;
	
	public SharedMemDispatcher(){
		/**
		 * TODO maybe take out the Collectin.synchronizedMap wrapper and 
		 * manage the synchronization by hand  (March 2, 2010
		 */
		HostedDispatchers = Collections.synchronizedMap(new HashMap<String, List<ConnectionManager>>());
		HostedDispatchersDataId = Collections.synchronizedMap(new HashMap<String,Long>());
	}
	public void addDispatcher( PipeID id, List<ConnectionManager> lst, Long data_id/*the id for the node hosting the data obj*/){
		HostedDispatchers.put(id.toURI().toString(), lst);
		HostedDispatchersDataId.put(id.toURI().toString(), data_id);
	}
	public void removeDispatcher(PipeID id){
		HostedDispatchers.remove(id.toString());
		HostedDispatchersDataId.remove(id.toString());
	}
	/**
	 * Returns a connection manager if the pipe is in the list, otherwise returns null. the algorithm will 
	 * check a list to see if the pipe is hosted on the same computer.  If it is, it will create variables 
	 * to be shared by the two nodes.  It then gives the variables to a server-type sharedMemManager and
	 * to a client-type shareMemManager.  The server is added to the list of connection for that node, and
	 * the client is return to the user that requested the connection to the particular PipeID.
	 * @param id
	 * @return	
	 **/
	 /*   		 										 ->The pipe we are connecting to. the remote pipe id.
	 * 													 |                   ->the local pipe	
	 *											         |                   |                ->local data id              */
	public ConnectionManager getClientSharedMemManager( PipeID id, String local_pipe_id, Long local_data_id 
										, PipeID unique_dependency_id, HierarchicalDependencyID dependency_id){
		SharedMemManager client = null;
		
		//check dispatcher to see if the PipeID host is in the same CPU
		List<ConnectionManager> man = HostedDispatchers.get(id.toURI().toString());
		/*get the data id used by the dispatcher holder.  the client should provide a data id when calling this service.*/
		Long host_data_id = HostedDispatchersDataId.get(id.toURI().toString());
		if( man != null && host_data_id != null){
			
			//create shared variables
			SharedVariables Vars;
			Vars = new SharedVariables();
			//Vars.Queue =  new ConcurrentLinkedQueue[2];
			//Vars.NBRecv = new ConcurrentLinkedQueue[2];
			Vars.Bound = true;
			Vars.HCommPipeID = new SlimJxtaID(CommunicationConstants.COMM_ID_ACC,id);
			Vars.HRemotePeerID = new SlimJxtaID(CommunicationConstants.COMM_ID_ACC,Node.PID);
			Vars.DependencyID = dependency_id;
			Vars.UniqueDependencyID = unique_dependency_id;
			/*
			for(int i = 0; i < 2; i++){
				Vars.Queue[i] = new ConcurrentLinkedQueue<Serializable>();
				Vars.NBRecv[i] = new ConcurrentLinkedQueue<NonBlockReceiver>();
			}*/
			
			//create client and server, and give then the same variables so they share them
			client = new SharedMemManager( true, Vars, id.toString(), host_data_id);
			SharedMemManager server = new SharedMemManager( false, Vars, local_pipe_id, local_data_id);
			
			//get the establishe connection object from the segment id server if it exists
			ConnectionEstablishedListener mConEstLis = MultiModePipeMapper.ConEstList.get(host_data_id);
			
			if( mConEstLis != null){
				if( !mConEstLis.onConnectionEstablished( server ) ){
					server.setBound(false);
					client.setBound(false);
					return null;
				}
			}
			//add server to the dispactcher list
			
			//add code here to complete data-flow handshake
			synchronized( man ){
				man.add(server);
			}
			
			
			
			Node.getLog().log(Level.FINE , " Created a Shared Memory Pipe to " + id.toString() + " HostedDispatcher " + this.HostedDispatchers.size() );
		}else{
			Node.getLog().log(Level.WARNING , " The id is not in the list " + id.toString());
		}
		//return client
		return client;
	}

	
	
}
