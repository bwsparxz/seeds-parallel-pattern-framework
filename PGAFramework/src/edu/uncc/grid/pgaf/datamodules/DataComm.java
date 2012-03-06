/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * * All rights reserved
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
package edu.uncc.grid.pgaf.datamodules;

import java.util.List;


public abstract class DataComm implements Data{

	/**
	 * this  
	 */
	private static final long serialVersionUID = -9074402147843768966L;
	private List<Long> NeighborsList;
	private boolean AwaitsPGAFModification;
	
	public DataComm(){
		AwaitsPGAFModification = false;
	}
	
	public abstract  Data getCommSubData(long segment);
	public abstract void setCommSubData(long segment, Data obj);
	
	public List<Long> getNeighborsList() {
		return NeighborsList;
	}
	public void setNeighborsList(List<Long> neighborsList) {
		NeighborsList = neighborsList;
	}
	public boolean isAwaitingPGAFModification() {
		return AwaitsPGAFModification;
	}
	public void setAwaitPGAFModification(boolean awaitsPGAFModification) {
		AwaitsPGAFModification = awaitsPGAFModification;
	}
	
}
