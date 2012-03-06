package edu.uncc.grid.pgaf.interfaces.advanced;

import java.io.Serializable;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.p2p.Node;
/**
 * Note: remember not to use static variables since this template can be
 * part oth other template processes when running on a multicore.
 * @author jfvillal
 *
 */
public abstract class Template implements Serializable{
	/**
	 * The Network Object is used to give the User important network classes
	 * Among these is the NetworkManager and the PeerGroup
	 */
	transient protected Node Network;
	/**
	 * The users Modoule
	 */
	protected BasicLayerInterface UserModule;
	/**
	 * This variable is used to indicate to the temlate that the framework has
	 * received a shutdown order.
	 * The template should find a confterble way to shutdown as soon as possible.
	 */
	protected static volatile Boolean ShutdownOrder;
	/**
	 * Template(Node n) should be the only constructor.  This constructor is the one used by the
	 * PGAFramework, other constructors will not be used by the framework.  Also, note that the 
	 * Temaplate is not instantiated by the user program or the Advanced user.  The Template is
	 * Instantiated inside the Framework, so there is no way to add another type of Constructor.
	 * 
	 * To add other types of Constructors to the Template, you must program at the Expert Layer
	 * level
	 * 
	 * note 2:  The user's module is given to the template after it has been instantiated; therefore
	 * don't use the UserModule variable int he constructors.  Instead, what you can do is to
	 * use that variable when setUserModule() is called, this happens almost immediately following
	 * the instantiation.
	 * @param n
	 */
	public Template( Node n){
		Network = n;
		ShutdownOrder = false; 
	}
	public Node getNetwork(){
		return Network;
	}
	/**
	 * returns the user interface that this Template handles
	 * @return
	 */
	public abstract String getSuportedInterface();
	
	/**
	 * returns if a shutdown order has being received.
	 * @return
	 */
	public boolean isShutdownOrder() {
		return ShutdownOrder;
	}
	/**
	 * Sets the shutdown order flag.
	 * @param shutdownOrder
	 */
	public static void setShutdownOrder(boolean shutdownOrder) {
		ShutdownOrder = shutdownOrder;
	}
	public BasicLayerInterface getUserModule() {
		return UserModule;
	}
	public void setUserModule(BasicLayerInterface userModule) {
		UserModule = userModule;
	}
	/**
	 * can be used to close any pending transactions.  adding code to this method is
	 * optional
	 */
	public abstract void FinalizeObject();
}
