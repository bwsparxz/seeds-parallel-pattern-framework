/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 *  
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

public class ParticleSystemData extends  DataComm{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5178041518060771972L;

	@Override
	public Data getCommSubData(long segment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCommSubData(long segment, Data obj) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Long> getNeighborsList() {
		// TODO Auto-generated method stub
		return super.getNeighborsList();
	}

	@Override
	public boolean isAwaitingPGAFModification() {
		// TODO Auto-generated method stub
		return super.isAwaitingPGAFModification();
	}

	@Override
	public void setAwaitPGAFModification(boolean awaitsPGAFModification) {
		// TODO Auto-generated method stub
		super.setAwaitPGAFModification(awaitsPGAFModification);
	}

	@Override
	public void setNeighborsList(List<Long> neighborsList) {
		// TODO Auto-generated method stub
		super.setNeighborsList(neighborsList);
	}

	@Override
	public int getSegment() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSegment(int segment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte getControl() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setControl(byte set) {
		// TODO Auto-generated method stub
		
	}


}
