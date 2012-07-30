package edu.uncc.grid;

import java.io.Serializable;
import java.util.List;

import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.datamodules.ReduceDataState;
/**
 * Shows a simple program that takes the number count from its neighbors
 * and adds one to the count.
 * 
 * This class is used to manage the data for each of the slave nodes.
 * The framework will ask for the data that has to be shared with all the neighbors 
 * through the method getSyncData()
 * Likewise, the framework will provide the programmer with the data from all 
 * the neighbor processes through the method setSyncDataList()
 * 
 * The list dat contains the data object for each of the neighbors.  and that 
 * list was filled in using the data that is returned in getSynchData.
 * 
 * 
 * @author jfvillal
 */
public class DataForAll implements AllToAllData, ReduceDataState {
  /**
	* 
	*/
	private static final long serialVersionUID = 1L;
	private static final String ReportIteration  = null;
	private int count;
	private int IterationCount;
	private long mReportIteration;
	
	
	public int getProblemIteration() {
		return IterationCount;
	}
	public void advanceProblemIteration(){
		++IterationCount;
	}
	public DataForAll( ){
	    count = 1;
	    IterationCount = 0;
	    mReportIteration = 0;
	}
	public void setCount(int c ){
		count = c;
	}
	public int getCount(){
		return count;
	}
	@Override
	public void setSyncDataList(List<Serializable> dat) {
		for( Serializable  i : dat){
			count += (Integer) i ;
		}
	}
	@Override
	public Serializable getSyncData() {
		return count;
	}
	@Override
	public void advanceIteration() {
		++mReportIteration;
		
	}
	@Override
	public long getIteration() {
		return mReportIteration;
	}

}
