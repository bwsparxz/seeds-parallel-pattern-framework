/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * * All rights reserved
 */
package edu.uncc.grid.pgaf.communication.nat;
/**
 * This exception is thrown if the Tunnel is not bounded and the user requests an action that
 * requires the Tunnel.
 * @author jfvillal
 *@deprecated  We are no longer seeking inter-grid connections.  We are now focusing on the cloud and multi-core
 *  This class will be removed from the framework soon !
 *
 */
public class TunnelNotAvailableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -645534757175868229L;

	@Override
	public String getMessage() {
		
		return "The Tunnel has not being stablished";
	}
	
}
