package edu.uncc.grid.pgaf.communication;

import java.io.Serializable;
import java.util.HashMap;

import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;
/**
 * This is used to transmit a message about the connection being hibernated.
 * 
 * This will happen when the dataflow needs to rearrange the processes, or when 
 * teh dependencies get split, coalesced.
 * @author jfvillal
 *
 */
public class ConnectionChangedMessage extends HashMap<String, Serializable> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The hierarchical id for the node requesting to be disconnected
	 */
	HierarchicalDependencyID HibernatingDeptID;
	long VersionStop;
	boolean Dirty = false;
	
	public HierarchicalDependencyID getHibernatingDeptID() {
		return HibernatingDeptID;
	}
	public void setHibernatingDeptID(HierarchicalDependencyID hibernatingDeptID) {
		HibernatingDeptID = hibernatingDeptID;
	}
	public synchronized long getVersionStop() {
		return VersionStop;
	}
	public synchronized void setVersionStop(long versionStop) {
		VersionStop = versionStop;
	}
	public synchronized boolean isDirty() {
		return Dirty;
	}
	public synchronized void setDirty(boolean dirty) {
		Dirty = dirty;
	}
	
	
}
