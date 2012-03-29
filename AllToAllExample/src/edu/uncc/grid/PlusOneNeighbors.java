package edu.uncc.grid;
import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.interfaces.basic.CompleteSyncGraph;
/**
 * This class extends CompleteSyncGraph to implement the all-to-all pattern
 * 
 * The framework calls DiffuseData and GatherData on the same master process to 
 * get the data for each computation unit
 * 
 * The the framework calls the method OneIterationCompute on the slave nodes.  The 
 * Operation involves a synchronization phase that happens before each cycle.
 * Example:
 *   --Neighbor exchange their data
 *   --compute
 *   --Neighbors exchange their data
 *   --compute
 * The process is repeated until the method returns false.  All the save processes 
 * must end at the same iteration.
 * 
 * The AllToAllData object is used to by the framework to get the data that should
 * be exchanged among the slave nodes before each iteraion.
 * @author jfvillal
 *
 */
public class PlusOneNeighbors extends CompleteSyncGraph {
	@Override
	public AllToAllData DiffuseData(int segment) {
	    DataForAll for_all = new DataForAll();
	    return for_all;
	}
	@Override
	public void GatherData(int segment, AllToAllData data) {
	    System.out.println( "segment" + segment + " value: " + ((DataForAll)data).getCount() );
	}
	@Override
	public boolean OneIterationCompute(AllToAllData data) {
		DataForAll dat = (DataForAll) data;
		if( dat.getIterationCount() >= 3 ){
			return true;
		}
	    dat.advanceIteration();
	    return false;
	}
	@Override
	public int getCellCount() {
	    // not used really
	    return 4;
	}
	@Override
	public void initializeModule(String[] args) {
	}
	
}
