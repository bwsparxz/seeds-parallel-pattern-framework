/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * * All rights reserved
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
package edu.uncc.grid.pgaf.interfaces.basic;

import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.templates.BandwidthTestTemplate;
/**
 * Used to test networking abilities.  should be considered at the 
 * expert layer.
 * @author jfvillal
 *
 */
public abstract class Debug extends BasicLayerInterface {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6196592047576202136L;

	@Override
	public String getHostingTemplate() {
		
		return BandwidthTestTemplate.class.getName();
	}

	


	public int DoubleSize;
	public int Limit;
	public long StartTime;
}
