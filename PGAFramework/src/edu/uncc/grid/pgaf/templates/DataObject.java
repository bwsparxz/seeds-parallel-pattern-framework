package edu.uncc.grid.pgaf.templates;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import edu.uncc.grid.pgaf.datamodules.Data;
/**
 * Creating a class by itself, and not inside another class reduces the serialization overhead by
 * about 40 bytes.  Each extension or nested class adds 40 bytes to the classes overhead when 
 * serializing it and transferring it over the network.
 * @author jfvillal
 *
 */
public class DataObject implements Data, Externalizable{
	/**
	 * 
	 */
	public static final long serialVersionUID = -5605965255083885118L;
	public double d[];
	public int segment;
	public byte control;
	public DataObject(){
		
	}
	public DataObject( int size){
		d = new double[size];
		for(int i = 0; i < d.length; i++){
			d[i] = 0.33;
		}
	}
	@Override
	public int getSegment() {
		return segment;
	}
	@Override
	public void setSegment(int segment) {
		this.segment = segment;
		
	}
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		int size = in.readInt();
		d = new double[size];
		for(int i = 0; i < size; i++){
			d[i] = in.readDouble();
		}
		
	}
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(d.length);
		for(int i = 0; i < d.length; i++){
			out.writeDouble(d[i]);
		}
		
	}
	@Override
	public byte getControl() {
		return control;
	}
	@Override
	public void setControl(byte set) {
		control = set;
	}
	
}

