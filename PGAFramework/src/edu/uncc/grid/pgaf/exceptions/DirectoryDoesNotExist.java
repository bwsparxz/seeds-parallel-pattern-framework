/* * Copyright (c) Jeremy Villalobos 2009
 *   * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.exceptions;
/**
 * no clear use
 * @author jfvillal
 *
 */
public class DirectoryDoesNotExist extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -340000030525746619L;

	@Override
	public String getMessage() {
		return "The remote directory does not exists";
	}
	
}
