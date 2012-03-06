package edu.uncc.grid.seeds.comm.dependency;

import edu.uncc.grid.pgaf.datamodules.Data;
/**
 * This class is used to manage the dependenci channel.  
 * 
 *  This class is to coordinate the two points in the communication so that we can split and
 *  coaless multiple dependencies.
 * 
 * @author jfvillal
 *
 */
public class DependencyControl implements Data {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int Segment;
	byte Control;
	@Override
	public byte getControl() {
		return Control;
	}

	@Override
	public int getSegment() {
		return Segment;
	}

	@Override
	public void setControl(byte set) {
		Control = set;
	}

	@Override
	public void setSegment(int segment) {
		Segment = segment;
	}

}
