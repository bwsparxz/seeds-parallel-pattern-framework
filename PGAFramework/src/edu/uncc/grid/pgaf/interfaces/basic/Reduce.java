package edu.uncc.grid.pgaf.interfaces.basic;

import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.ReduceDataState;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.templates.ReduceTemplate;

public abstract class Reduce extends BasicLayerInterface {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * This pattern is strictly used by a pattern adder operator.  It falls under 
	 * monitoring and interactive patters.  The worker node that implements this method
	 * should get the input data from the "computational" pattern.  This is a stencil or an
	 * all-to-all graph in the current implementation.  It then prepares the data to be sent 
	 * to the master and returns this data.
	 * 
	 * This pattern is helpfull for check pointing synchronous patterns, and for visualizing 
	 * intermediate results.  The opposite of this pattern, a map pattern, can be used to
	 * "stir" a parallel computation with interactive input.
	 * @param input
	 * @return the data object to be sent to the sink node.  return null to end the pattern. 
	 *   All nodes should end at the same time.
	 */
	public abstract  Data WorkerSend(ReduceDataState input);
	/**
	 * This method is called on the server-side to receive the data emmitted by the workers.
	 * The total number of pattern nodes is the number of data units returned in this method.  
	 * For example, if there are 10 units computing, when this pattern is called, the 
	 * ServerReduce method will be called 10 times.
	 * 
	 * The process is repeated until WorkerSend Data sends a null object (which is null
	 * on WorkerSend, but the framework actually uses a DummObject to inform the Master
	 * the computation should end.
	 * 
	 * The programmer should add enumeration to the data units if there is a need to identify
	 * the individual pieces before being displayed or saved.
	 * @param segment
	 * @param data
	 */
	public abstract void ServerReduce( Data data);
	
	@Override
	public String getHostingTemplate() {
		return ReduceTemplate.class.getName();
	}
	/**
	 * should return the number of cells that send the reduce information to the master.
	 * If used with a pattern operator, this number should be equal to the number used
	 * by the other pattern.
	 * @return
	 */
	public abstract int getCellCount();
}
