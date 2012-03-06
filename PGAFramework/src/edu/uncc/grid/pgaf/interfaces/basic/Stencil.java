package edu.uncc.grid.pgaf.interfaces.basic;

import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.templates.StencilTemplate;
/**
 * 
 * It can be used to implement an stencil.  the blocks should be cut with respect to the segment in this way <br>
 * 
 * <img src=http://coit-grid01.uncc.edu/pgaf/JavadocImages/stencil_segment_layout.jpg />
 * 
 * <p>The number of cells has to be a perfect square</p>
 * @author jfvillal
 *
 */
public abstract class Stencil extends BasicLayerInterface{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract boolean OneIterationCompute( StencilData data);
	/**
	 * Used to get sync data from the user.  the user needs to return
	 * the data that will be sent to the node with segment number
	 * @param segment
	 * @return
	 */
	
	public abstract StencilData DiffuseData(int segment);
	/**
	 * GatherData does the opposite of DiffuseData().  It will return Data objects with
	 * Chunks of the original data processed into a user defined output Data object.
	 * The user is responsible for taking the output and putting it back together into 
	 * the final answer.
	 * @param segment
	 * @param dat
	 */
	public abstract void GatherData(int segment, StencilData dat);
	/**
	 * This function should tell the framework what is the total number of pieces in which 
	 * the user decided to divide the Input data.
	 * 
	 * (has to be a perfect square)
	 * 
	 * @return
	 */
	
	public abstract int getCellCount();
	
	@Override
	public String getHostingTemplate() {
		return StencilTemplate.class.getName();
	}
	//public abstract int getIterations();
}
