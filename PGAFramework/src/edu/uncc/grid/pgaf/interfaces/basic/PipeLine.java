package edu.uncc.grid.pgaf.interfaces.basic;

import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.templates.PipeLineTemplate;

public abstract class PipeLine extends BasicLayerInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3064822902900141442L;
	/**
	 * User should return true if the next stage is dependent on the data order.  data independent 
	 * algorithms are easier to load balance.  
	 * Ask your self, should the data 4 should needs information from 3,2,1 to get computed.
	 * if not, then data is independent.  there may be dependent stages mixed with independent stages.
	 * (note:  only implementing data independent for now)
	 * @param stage
	 * @return
	 */
	//public abstract boolean DataDependant( int stage);
	/**
	 * The Compute method represents a unit of computation.  the framework will load the 
	 * data units using DiffuseData().  It then is passed to stage one using 
	 * Compute( 0, Data), then Comput(1, Data)... etc.  The stages are chained together
	 * until the last stage is reached, at that point, the data is sent to GatherData 
	 * function for final answer processing or storage.
	 * 
	 * More elaborate pipelines will be created using Nested/Recursive Templates in the future.
	 * 
	 * @param stage
	 * @param input
	 * @return
	 */
	public abstract  Data Compute(int stage, Data input);
	/**
	 * This function is used to request the user for chunks of that will be send 
	 * through the network.  The user should inherit class Data into a custom class. 
	 *   For example, MyData.  The class MyData can have any type of object and variable.  
	 *   Be aware that the Data object needs to be serializable so that it can be 
	 *   Transfered over the network.
	 * @param segment
	 * @return
	 */
	public abstract Data DiffuseData(int segment);
	/**
	 * GatherData does the opposite of DiffuseData().  It will return Data objects with
	 * Chunks of the original data processed into a user defined output Data object.
	 * The user is responsible for taking the output and putting it back together into 
	 * the final answer.
	 * @param segment
	 * @param dat
	 */
	public abstract void GatherData(int segment, Data dat);
	/**
	 * The programmer divides the problem into many pieces.  The framework can 
	 * Retrieve the number of pieces by calling this method.
	 * The framework will then call compute() getDataCount(), starting from 
	 * zero, and stopping at getDataCount() - 1 
	 * this is called inside the framework similar to the code
	 * for( int i = 0; i < PipeLine.getDataCount(); i++){
	 * 	//run compute()
	 * }
	 * 
	 * @return returns the number of data pieces for the problem.
	 */
	public abstract int getDataCount();
	
	/**
	 * This method should return the number of stages used by the framework.
	 * Suppose getStageCount() return 3
	 * The compute() method will be called by 3 difference processes.  the 
	 * first will run compute( 0, 0).  The output from that will be 
	 * sent to the second node, which computes compute( 1, 0).  It then
	 * sends the output to the third node, which computes compute( 2, 0).
	 * 
	 * 
	 * 
	 * The segment number is increased according getDataCount().  
	 * @return
	 */
	public abstract int getStageCount();
	
	/**
	 * Seeds is a three-tier framework.  This pattern plugs into the 
	 * "advanced layer."  The advanced layer is another tier with the 
	 * lowest tier being the infrastructure layer.  
	 * 
	 * This pattern must specify which advanced layer class can 
	 * mount and run this module.
	 */
	@Override
	public String getHostingTemplate() {
		return PipeLineTemplate.class.getName();
	}

}
