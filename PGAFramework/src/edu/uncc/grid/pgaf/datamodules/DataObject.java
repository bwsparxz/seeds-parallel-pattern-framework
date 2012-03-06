package edu.uncc.grid.pgaf.datamodules;
/**
 * This class fill in some of the information for the user for convenience at the expence of performance.
 * Each data packet will have an extra 20 bytes of overhead.  Use Data interface directly to avoid the
 * overhead penalty.
 * @author jfvillal
 *
 */
public class DataObject implements Data {
	byte Control;
	int segment;
	@Override
	public int getSegment() {
		return segment;
	}

	@Override
	public void setSegment(int ssegment) {
		segment = ssegment;

	}

	@Override
	public byte getControl() {
		return Control;
	}
	public void setControl(byte set){
		Control = set;
	}
}
