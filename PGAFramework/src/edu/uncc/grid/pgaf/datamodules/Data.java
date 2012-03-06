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
package edu.uncc.grid.pgaf.datamodules;


import java.io.Serializable;

public interface Data extends Serializable{
	/**
	 * 
	 */	
	public int getSegment();
	public void setSegment(int segment);
	/**
	 * The Basic user programmer should always return 0 for this function.
	 * Only Expert and Advance users should need to worry about other values.
	 * this is used for network control at the lower layers.
	 * @return
	 */
	/**
	 * TODO expand control function to work for the lower level 
	 * experter communication link
	 */
	public byte getControl();
	public void setControl(byte set);
	//add function to control class id.  the purpose is to reduce the 
	//use of reflection in high performance line and on 
	//main loops 
}
