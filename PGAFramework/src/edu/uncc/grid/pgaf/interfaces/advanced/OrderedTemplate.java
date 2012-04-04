package edu.uncc.grid.pgaf.interfaces.advanced;

import java.io.Serializable;
import java.util.Map;

import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.p2p.Node;
/**
 * This Template provides the advance user with an mpi-like communication system.
 * The user can refer to process by the data id.  The initial idea is to deploy the 
 * patter using a unordered deploy pattern.  Then the ordered pattern kiks in and
 * starts to communicate.  each node that signed up for the pattern will have an 
 * id, and this id is used to communicate.
 * 
 * @author jfvillal
 *
 */
public abstract class OrderedTemplate extends Template{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected long CommunicationID;
	public OrderedTemplate(Node n) {
		super(n);
	}
	/**
	 * Compute side is called by the expert layer.  In it the advance layer user can create a pattern.
	 * A stencil, complete graph, pipeline, or a non-standard pattern
	 * @return true if job done, false if not done
	 * @param comm
	 * @param coeficient
	 */
	public abstract boolean ComputeSide( Communicator comm);
	/**
	 * This communicator has an id of (-1)  To send messages to the SourceSinkSide use 
	 * -1 id
	 * @param comm
	 * @param coeficient
	 * @return true to stop iteration. Otherwise the framework continues to call 
	 *   the method.
	 */
	public abstract boolean SourceSinkSide( Communicator comm);
	/**
	 * Used to configure the compute node.  Also used to load the initial stage.
	 * Like the stage number in the case of pipeline or the initial value
	 * matrix in the case of a stencil.
	 * 
	 * This function is called by a class that inherits the LoaderPatter
	 * @param configuration
	 */
	public abstract void Configure( DataMap<String, Serializable> configuration);
	
	
	public abstract Class getLoaderModule();
	
	public long getCommunicationID(){
		return CommunicationID;
	}
	
}
