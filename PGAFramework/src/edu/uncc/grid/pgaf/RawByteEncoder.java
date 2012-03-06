package edu.uncc.grid.pgaf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
/**
 * A user's module can implement this interface to optimize the transfer of 
 * object packets across a cluster.  the interface is added to avoid adversely
 * affecting the shared memory infrastructure.
 * @author jfvillal
 *
 */
public interface RawByteEncoder {
	/**
	 * The user should start by sending a unique byte number
	 * 
	 * prohivited byte numbers are: <br>
	 * 	<ul> 
	 * <li>CLOSE_ACTION = 10;</li>
	 * <li> HIBERNATE_ACTION = 11;</li>
	 *	<li>DEFAULT_ACTION = 1;</li>
	 *	<li>RAWBYTE_ACTION = 5;</li>
	 *</ul>
	 * The user has to make sure the same number of bytes put in on one side
	 * come out of the other.  If the number changes, just make the first
	 * int in determine the size of the custom stream.
	 * 
	 * @param stream
	 * @param input
	 */
	public void toRawByteStream( ObjectOutputStream stream , Serializable input) throws IOException;
	/**
	 * 
	 * The user should start by sending a unique byte number
	 * 
	 * prohivited byte numbers are: <br>
	 * 	<ul> 
	 * <li>CLOSE_ACTION = 10;</li>
	 * <li> HIBERNATE_ACTION = 11;</li>
	 *	<li>DEFAULT_ACTION = 1;</li>
	 *	<li>RAWBYTE_ACTION = 5;</li>
	 *</ul>
	 * The user has to make sure the same number of bytes put in on one side
	 * come out of the other.  If the number changes, just make the first
	 * int in determine the size of the custom stream.
	 * @param identifier the custom byte identification you gave on toRawByteStream
	 * @param stream
	 * @return
	 */
	public Serializable fromRawByteStream( byte identifier, ObjectInputStream stream ) throws IOException;
}
