package edu.uncc.grid.pgaf.templates;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.basic.PipeLine;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

public class PipeLineLoader extends PatternLoader {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//The advanced user can choose to implement a deploy method or not
	/**
	 * This will give each compute unit a stage number.  and the maximun stage number, so
	 * that the last node knows to return the answer back to the sink node.
	 */
	@Override
	public DataMap DiffuseDataUnit(int segment) {
		
		DataMap<String, Integer> map = new DataMap<String,Integer>();
		Integer i = segment;
		map.put(PipeLineTemplate.STAGE_NUMBER, i);
		map.put(PipeLineTemplate.MAX_STAGE,((PipeLine)this.OTemplate.getUserModule()).getStageCount());
		return map;
	}
	/**
	 * returns the number of stages on this pipeline.  If this was a stencil, the user would be
	 * able to return the number of nodes on that stencil etc.
	 */
	@Override
	public int getDataUnitCount() {
		//this has to be set by the advanced user.
		return ( (PipeLine)OTemplate.getUserModule()).getStageCount();
	}
	/**
	 * The gather method does not do anything.  This is because once the pattern is deployed the user
	 * is mostly concerned with the action of the actual pattern, and not the deployment pattern.
	 * However, we know this pattern is done, once we get acknoldegement from the nodes that they are done.  
	 * this is communicated by sending back the data unit in this case.
	 */
	@Override
	public void GatherDataUnit(int segment, Data dat) {
		//do nothing
		//the pipeline is finished.
		//if this was the stencil, we can call the gather method from the basic user interface.
		
	}
	/**
	 * this is used to communiate initial command line argument to the other nodes.  this is neceary
	 * because the classes get instantiated on remote nodes, they are not sent over the network as is
	 * done with the data objects.  So, this function allows for the initialization of any variables
	 * on the main pattern class once it has being instatiated.
	 */
	@Override
	public void initializeModule(String[] args) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean instantiateSourceSink() {
		return true;
	}
	@Override
	public boolean isOperator() {
		return false;
	}

}
