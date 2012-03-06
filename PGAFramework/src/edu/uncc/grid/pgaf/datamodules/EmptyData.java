package edu.uncc.grid.pgaf.datamodules;

public class EmptyData implements Data {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int seg;
	@Override
	public byte getControl() {
		return 0;
	}

	@Override
	public int getSegment() {
		return seg;
	}

	@Override
	public void setSegment(int segment) {
		seg = segment;

	}

	@Override
	public void setControl(byte set) {
		// TODO Auto-generated method stub
		
	}

}
