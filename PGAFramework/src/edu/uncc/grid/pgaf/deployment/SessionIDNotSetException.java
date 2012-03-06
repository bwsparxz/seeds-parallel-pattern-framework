/* * Copyright (c) Jeremy Villalobos 2009
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
package edu.uncc.grid.pgaf.deployment;

public class SessionIDNotSetException extends Exception {

	/**
	 * thrown if the Java Cog Shell session ID has not being requested and actions
	 * that require the SessionID are called.
	 */
	private static final long serialVersionUID = -66371779543968694L;

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return "SessionID not set, probably because setConnection() failed or the method was not called.";
	}

}
