package edu.uncc.grid.pgaf.communication.nat;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.p2p.Node;

import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;

/**
 * SlimJxtaID is used to reduce the size of a JXTA ID to reduce the network overhead.  The 
 * JXTA ID is 298 btes big.  A smaller String version is 77 bytes.  However the total number of
 * information bytes is 28.  This class only takes the important bytes so that they can be sent
 * over the network.  It also allows the accuracy to be reduced from 28 to 1 bytes.  Since the 
 * class is used to create a hash map of connections created by a communication link, rarely will
 * there be a need for full accuracy.  The recommended setting is 4 bytes, and the high setting
 * for thousands of nodes is 8.
 * 
 * TODO the maximum accuracy as of Aug/09 is 32 bits because the Java Mab uses hashCode(), which
 * only returns an integer (approx 4 billion values max)
 * 
 * If a higher capacity is need, there would be a need to create a customized map that can 
 * use big integers as key values.  Or maybe a long which would increase then number of values to
 * 1.8e19 values
 * 
 * @author jfvillal
 *
 */
public class SlimJxtaID implements Serializable {
	BigInteger StoredID;
	int Accuracy;
	/**
	 * Max accuracy can be used to debug and make sure collisions
	 * are not to blame for a hard to find error.  This level
	 * should not be needed.  It can guarantee low probability of collision
	 * to trillions of nodes, but there is no evidence the framework can 
	 * scale to that amount of nodes.
	 */
	public final static byte MAX_ACCURACY = 28;
	/**
	 * Recommended accuracy in use for hundreds of nodes
	 */
	public final static byte RECOMMENDED_ACCURACY = 4; 
	/**
	 * Recommended for thousands of nodes 
	 */
	public final static byte HIGH_NODE_COUNT_ACCURACY = 8;
	/*
	 * Minimum accuracy.  for less than 10 nodes 
	 */
	public final static byte MIN_ACCURACY = 1;

	/**
	 * Create a SlimJxtaID to an accuracy parameter.  if the parameter is
	 * MAX_ACCURACY big, no information is lost.
	 * @param j
	 * @param the_id
	 */
	public SlimJxtaID( int j, ID the_id){
		if( j < 1 || j > MAX_ACCURACY){
			System.out.println("out of bounds argument (should be between 1 and 28");
		}
		
		
		String TheNumber = the_id.toString();
    	
    	Accuracy = j;
    	
    	/*
    	 * concatenate the original ID to something of the accuracy specified
    	 */
    	BigInteger temp = new BigInteger(TheNumber.substring(14), 16);
    	byte[] temp_byte = temp.toByteArray();
    	byte[] shorter_version = new byte[Accuracy];
		for(int i = (MAX_ACCURACY - Accuracy); i < MAX_ACCURACY; i++){
			shorter_version[i- (MAX_ACCURACY - Accuracy)] = temp_byte[i];
		}
		
		StoredID = new BigInteger(1, shorter_version);
	}
	/**
	 * Constructructs the SlimJxtaID from a stream of bytes
	 * @param the_id
	 */
	public SlimJxtaID( int acc, byte[] the_id){
		StoredID = new BigInteger(1 ,the_id);
		Accuracy = acc;
	}
	/**
	 * Returns the byte equivalent of the ID depending on the accurcy specified.
	 * The numbers that get shopped off are the most significant digits
	 * @return
	 */
	public byte[] getStoredID(){
		
		return StoredID.toByteArray();
	}
	public PipeID toPipeID(){
		PipeID ans = null;
		try {
			ans = (PipeID) IDFactory.fromURI(URI.create(this.toString()));
		} catch (URISyntaxException e) {
			Node.getLog().log(Level.WARNING, "The URI is not legible " + this.toString() + Node.getStringFromErrorStack(e));
		}
		return ans;	
	}
	public PeerID toPeerID() {
		PeerID ans = null;
		try {
			ans = (PeerID) IDFactory.fromURI(URI.create(this.toString()));
		} catch (URISyntaxException e) {
			Node.getLog().log(Level.WARNING, "The URI is not legible " + this.toString() + Node.getStringFromErrorStack(e));
		} 
		return ans;
	}
	
	public SlimJxtaID( int acc, ObjectInput in) throws IOException{
		this.Accuracy = acc;
		byte size = in.readByte();
		byte[] b = new byte[size];
		in.read(b, 0, size);
		this.StoredID = new BigInteger(1,b);
	}
	public void putInStream( ObjectOutput out) throws IOException{
		byte[] b = this.getStoredID();
		byte l = (byte) b.length;
		out.writeByte(l);
		out.write(b, 0, b.length);
	}
	
	
	@Override
	public String toString() {
		
		return "urn:jxta:uuid-" + this.StoredID.toString(16);
	}
	
	@Override
	public boolean equals(Object obj) {
		BigInteger other = ((SlimJxtaID) obj).StoredID;
		return this.StoredID.equals(other);
	}
	
	
	@Override
	public int hashCode() {
		int ans = this.StoredID.intValue(); 
		return ans;
	}
}
