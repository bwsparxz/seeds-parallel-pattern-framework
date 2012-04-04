/* * Copyright (c) Jeremy Villalobos 2009
 *  
 */
package edu.uncc.grid.pgaf.templates;

public class CommunicationLinesNotSetException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1236616623797124038L;
	
	public String getMessage() {
	
		return "Communication lines are not set.  To set then to none, return an empty list from your code";
	}
}
