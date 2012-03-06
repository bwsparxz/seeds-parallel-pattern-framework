/* * Copyright (c) Jeremy Villalobos 2009
 *  
 */
package edu.uncc.grid.pgaf.interfaces;
/**
 * this class is deprecated.  The advanced user is to access JXSE utilities directly using
 * JXSE calls.
 */
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;

public interface AdvertCatcher {
	/**
	 * What type of advertisement are we looking for
	 * @return
	 */
	public Class getAdvertisementType();
	/**
	 * Action to be performed if advertisemente if caught
	 */
	public void Action(Advertisement adv , DiscoveryService discovery_service); 
}
