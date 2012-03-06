/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  
 */
package edu.uncc.grid.pgaf.interfaces;

import java.io.Serializable;

/**Interface to be used by a class that wants to listen for future addition to the
 * recv queue in a connection manager.  This style of litener is used extensively in 
 * Java for GUI and other interaction.  The user should
 * 
 *  1. implement the interface
 *  2. pass the object to a recv call
 *  3. when an object is received, the object will be given to the classes 
 *  	Update() function for user code to deal with the event.
 *  
 *  As of April 2009 the feature has not being extensively tested, also the code
 *  has being reviewed for it proper functioning.  Code dealing with NonBlockReceiver
 *  should be present in the receiver thread of any  class implementing the 
 *  ConnectionManager Interface.
 * 
 * @author jfvillal
 *
 */
public interface NonBlockReceiver {
	public void Update(Serializable dat)throws InterruptedException;
}
