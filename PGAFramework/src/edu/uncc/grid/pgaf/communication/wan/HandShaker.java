/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.communication.wan;

import java.io.Serializable;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.InvalidHierarchicalDependencyIDException;

public class HandShaker implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 601903173212116544L;
	private SlimJxtaID HRemotePeerID;
	private String RemoteCommPipeID;
	private PipeID UniqueDependencyID;
	private Long RemoteDataID;
	private HierarchicalDependencyID DependencyID;
	
	public HandShaker( SlimJxtaID h_remote_peer_id, String remote_comm_pipe_id, Long remote_data_id
			, PipeID unique_dependency_id, HierarchicalDependencyID dependency_id){
		HRemotePeerID = h_remote_peer_id;
		RemoteCommPipeID = remote_comm_pipe_id;
		RemoteDataID = remote_data_id;
		if( dependency_id == null){
			//to prevent the thing from crashing, enter an "empty" id if the module uisng the handshaker does
			//not have a use for the dependency id.
			UniqueDependencyID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, new String("empty").getBytes() );
			try {
				DependencyID = HierarchicalDependencyID.fromString("0/0:0/0");
			} catch (InvalidHierarchicalDependencyIDException e) {
				e.printStackTrace();
			}//setting it to null
		}else{
			UniqueDependencyID = unique_dependency_id;
			DependencyID = dependency_id;
		}
	}
	
	public SlimJxtaID getHRemotePeerID() {
		return HRemotePeerID;
	}
	
	public String getRemoteCommPipeID() {
		return RemoteCommPipeID;
	}
	
	public Long getRemoteDataID() {
		return RemoteDataID;
	}
	public HierarchicalDependencyID getDependencyID(){
		return this.DependencyID;
	}
	public PipeID getUniqueDependencyID() {
		return UniqueDependencyID;
	}
	
	
	
	
}
