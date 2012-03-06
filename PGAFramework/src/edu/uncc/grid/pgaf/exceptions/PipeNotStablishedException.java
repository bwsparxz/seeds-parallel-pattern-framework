/* * Copyright (c) Jeremy Villalobos 2009  
 *   * All rights reserved
 */
package edu.uncc.grid.pgaf.exceptions;
/**
 * This class is deprecated.
 * @author jfvillal
 *
 */
public class PipeNotStablishedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4274248419295452601L;

	@Override
	public String getMessage() {
		return "The Object Input or Output Stream are not open, the socket may have failed or was never opened";
	}

}
