package edu.uncc.grid.pgaf.datamodules;

import java.io.Serializable;
/**
 * This class hides some information necesary to transfer the users data.
 * The Payload is the Serializable from the user programmer.
 * It can also be the Structured Data Object.
 * @author jfvillal
 *
 */
public class DataContainer implements Data {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	Serializable Payload;
	byte Control;
	int Segment;
	public DataContainer(int segment, Serializable obj ){
		Payload = obj;
		Segment = segment;
	}
	public DataContainer(){
		Payload = null;
	}
	
	public synchronized Serializable getPayload() {
		return Payload;
	}
	public synchronized void setPayload(Serializable payload) {
		Payload = payload;
	}
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
