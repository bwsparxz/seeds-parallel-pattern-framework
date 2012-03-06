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
package edu.uncc.grid.pgaf.deployment;

public class HomeDirNotSetException extends Exception {

	/**
	 * thrown if the home directory has not being set.  This is used by 
	 * the Disseminator class.
	 */
	private static final long serialVersionUID = 6036206427683388793L;

	@Override
	public String getMessage() {
		
		return "Home dir is not set";
	}

}
