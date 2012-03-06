package edu.uncc.grid.pgaf.templates;


import java.io.Serializable;

import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.basic.CompleteSyncGraph;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;
/**
 * 
 * @author jfvillal
 *@deprecated used {@link edu.uncc.grid.seeds.otemplate.pattern.alltoall} instead.
 */
public class CompleteSyncGraphLoader extends PatternLoader {
	@Override
	public DataMap DiffuseDataUnit(int segment) {
		AllToAllData data = ((CompleteSyncGraph) this.OTemplate.getUserModule()).DiffuseData(segment);
		DataContainer container = new DataContainer( segment, data);
		
		DataMap<String, Serializable> m = new DataMap<String, Serializable>();
		m.put(PatternLoader.INIT_DATA, container);
		return m;
	}

	@Override
	public void GatherDataUnit(int segment, Data dat) {
		CompleteSyncGraph csg = (CompleteSyncGraph) this.OTemplate.getUserModule();
		DataMap<String, Serializable> m = (DataMap<String,Serializable>) dat;
		DataContainer container = (DataContainer) m.get(PatternLoader.INIT_DATA);
		csg.GatherData(segment, (AllToAllData) container.getPayload());
	}

	@Override
	public int getDataUnitCount() {
		CompleteSyncGraph g = (CompleteSyncGraph) this.OTemplate.getUserModule();
		return g.getCellCount();
	}

	@Override
	public boolean instantiateSourceSink() {
		//no thanks, don't needed
		return false;
	}

	@Override
	public void initializeModule(String[] args) {
		//not needed right now.
	}

	@Override
	public boolean isOperator() {
		// It is not an operator.
		return false;
	}
	
}
