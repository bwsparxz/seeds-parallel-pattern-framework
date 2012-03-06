package edu.uncc.grid.pgaf.datamodules;

import java.util.HashMap;
/**
 * This Data class is provided for convenience.  The user can store data in it as in a Map class since it 
 * iherits HashMap.  There is a performance cost because the packet will transport the key plust the 
 * key value, for best performance the user should create a class that implements Data interface.
 * @author jfvillal
 *
 * @param <V> value
 * @param <K> key
 */
public class DataMap<K, V> extends HashMap<K, V> implements Data {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int seg;
	byte Control;
	@Override
	public int getSegment() {
		return seg;
	}

	@Override
	public void setSegment(int segment) {
		seg = segment;
	}
	@Override
	public byte getControl() {
		return Control;
	}
	public void setControl(byte set){
		Control = set;
	}

}
