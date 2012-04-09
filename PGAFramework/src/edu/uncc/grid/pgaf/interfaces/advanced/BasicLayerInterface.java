/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  
 *  
 */
package edu.uncc.grid.pgaf.interfaces.advanced;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.p2p.Node;

/**
 * This interface should be used by the Advance used.  The interface is called by
 * framework to see which Template should be used to run it.  The Advanced user
 * should create a Template and a User Interface.  The Template should implement
 * Template, and the UserInterface should implement BasicLayerInterface.
 * 
 * @author jfvillal
 *
 */
public abstract class BasicLayerInterface implements Serializable{
	/**
	 * It provides greater access to the user programer while at 
	 * the same time making the programmer more responsible for 
	 * the Grid environment.
	 */
	transient protected Node Framework;
	Serializable StateFull;
	/**
	 * Returns the Template that should be used to run this User
	 * 	Interface
	 * @return
	 */
	public abstract void initializeModule(String[] args);
	/**
	 * returns the Template that runs this module
	 * @return
	 */
	public abstract String getHostingTemplate();
	/**
	 * 
	 * @return returns the main class that controls the framework on this node.
	 */
	public Node getFramework() {
		return Framework;
	}
	public void setFramework(Node framework) {
		Framework = framework;
	}
	
	public void setStateFull( Serializable set){
		StateFull = set;
	}
	/**
	 * 
	 * @return returns the state loaded by onLoadState() method.
	 */
	public Serializable getStateFull( ){
		return StateFull;
	}
	
	boolean done = false;
	/**
	 * All the operator interfaces have to override this function and returned
	 * true.  This is necesary for the deployment of the pattern operator from
	 * the user programmer.  
	 * @return
	 */
	public boolean isOperator(){
		return false;
	}
	
	/**
	 * Used to communicate to local pattern sink/source that the node is node.
	 * it should be updated by Worker and PatternLoader classes
	 * @return
	 */
	synchronized public boolean PatternDone(){
		return done;
	}
	synchronized public void setDone( boolean set){
		done = set;
		notifyAll();
	}
	
	synchronized public void watiOnPatternDone() throws InterruptedException{
		while(!done){
			wait();
		}
		if( done ){
			Node.getLog().log(Level.FINEST, Thread.currentThread().getName() + " Done with Pattern ");
		}
	}
	/**
	 * The basic user can implement this method during the optimization phase.
	 * This eliminates much of the overhead inccured by using Serializables.
	 * Instead the user is given the tool to provide the data in raw byte 
	 * form.  The technique is only used for cluster transactions, and 
	 * share memory transactions are done without this extra step, which in 
	 * the end actually maintains the shared memory performance while boosting
	 * the distributed memory transaction.
	 * @return
	 */
	public RawByteEncoder getRawByteEncoder(){
		return null;
	}
	
}
