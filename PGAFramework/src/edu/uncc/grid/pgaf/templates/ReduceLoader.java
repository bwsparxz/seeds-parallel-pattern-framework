package edu.uncc.grid.pgaf.templates;

import java.io.Serializable;

import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.datamodules.EmptyData;
import edu.uncc.grid.pgaf.datamodules.ReduceDataState;
import edu.uncc.grid.pgaf.datamodules.ReduceDataStateImpl;
import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.pgaf.interfaces.basic.Reduce;
import edu.uncc.grid.pgaf.interfaces.basic.Stencil;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;
/**
 * The PatternLoader should create the classes that are then sent to the compute modules
 * to build the pattern.  The MPI-like and the Dataflow techniques rely on computation units
 * that are represented by classes.  The Loaders create these units that are then sent to 
 * the multicore processors or the over the netowork to other computation nodes.  the 
 * Computation nodes then start to follow the instruction from the computation units, which
 * include a combination of computation and communication steps, oftentime synchronized with 
 * other computation units in the network or the multi-core computer.
 * @author jfvillal
 *
 */
public class ReduceLoader extends PatternLoader{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public DataMap<String, Serializable> DiffuseDataUnit(int segment) {
		DataMap<String, Serializable> map = new DataMap<String, Serializable>();
		ReduceDataState data = new ReduceDataStateImpl();
		DataContainer container = new DataContainer( segment, data );
		map.put(PatternLoader.INIT_DATA , container );
		map.put(ReduceTemplate.CELL_COUNT
				, ((Reduce)this.OTemplate.getUserModule()).getCellCount());
		return map;
	}

	@Override
	public void GatherDataUnit(int segment, Data dat) {
		// Nothing of value to gather once the reduce pattern if finished.
		// all the data was sent to the actual template ( this is the 
		// unit loader )
	}

	@Override
	public int getDataUnitCount() {
		return ( (Reduce)OTemplate.getUserModule()).getCellCount();
	}
	/**
	 * There are two API that provide streams of data distribution.
	 * The two are one done during the loading of computation 
	 * units, which is done by this class.  The other is as part of the mpi-like API.
	 * In the example of a pipeline, the pipeline can load the persistent data through 
	 * the DiffisuseDataUnit and GatherDataUnit methods, but then the flow of data that is
	 * part of a pipeline is done with DiffuseData and GatherData that are part of the pattern
	 * interface.
	 * 
	 * If a pattern is going to use the GatherData and DiffuseData methods to distribute 
	 * streams of data after the data units are in place, they should return true on this 
	 * method (instantiateSourcesink ), otherwise, they can return false, and the diffuse
	 * and gather methods will only be used once at the beggining and the end of the pattern.
	 * This is the case of the Stencil pattern, it can use the DiffuseDataUnit method to 
	 * distribute the initial data, and it can use the GatherDataUnit to gather back the 
	 * persisten data.  Because, it does not need data to be streamed from the master during 
	 * the computation as the case of the pipeline, it can return false on this method.
	 * 
	 * To make clear this is not just in relation to these two pattern.  Consider other patterns
	 * such as the workpool and the all-to-all patter.  the workpool does need access to a 
	 * stream of data comming from the master during computation while the all-to-all pattern 
	 * does not.  A divide and conquer pattern would need a flow of data from the master
	 * unit while a wavefron algorithm would not need this feature.
	 */
	@Override
	public boolean instantiateSourceSink() {
		return true;
	}

	@Override
	public void initializeModule(String[] args) {
		//no init data here
	}
	@Override
	public boolean isOperator(){
		return false;
	}
	/**
	 * Used to tell the loader or the operator manager that this class does support
	 * a data stream source and sink ( should be better named in the future )
	 */
	@Override
	public boolean hasComm() {
		return true;
	}

	
}
