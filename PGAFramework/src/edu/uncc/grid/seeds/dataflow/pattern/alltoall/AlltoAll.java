package edu.uncc.grid.seeds.dataflow.pattern.alltoall;

import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;

public abstract class AlltoAll extends BasicLayerInterface {

	private static final long serialVersionUID = 1L;

	public abstract boolean OneIterationCompute( AllToAllData data);
	/**
	 * Used to get sync data from the user.  the user needs to return
	 * the data that will be sent to the node with segment number
	 * @param segment
	 * @return
	 */
	
	public abstract AllToAllData DiffuseData(int segment);
	/**
	 * GatherData does the opposite of DiffuseData().  It will return Data objects with
	 * Chunks of the original data processed into a user defined output Data object.
	 * The user is responsible for taking the output and putting it back together into 
	 * the final answer.
	 * @param segment
	 * @param dat
	 */
	public abstract void GatherData(int segment, AllToAllData dat);

	public abstract int getCellCount();
		
	
	
	@Override
	public String getHostingTemplate() {
		return AlltoAllDataflow.class.getName();
	}

}
