package edu.uncc.grid.seeds.otemplate.pattern.networkscout;


import java.io.Serializable;

import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.basic.CompleteSyncGraph;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

public class NetworkScoutLoader extends PatternLoader {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public DataMap DiffuseDataUnit(int segment) {
		ScoutNetworkData data = ((ScoutModule) this.OTemplate.getUserModule()).DiffuseData(segment);
		DataContainer container = new DataContainer( segment, data);
		
		DataMap<String, Serializable> m = new DataMap<String, Serializable>();
		m.put(PatternLoader.INIT_DATA, container);
		return m;
	}

	@Override
	public void GatherDataUnit(int segment, Data dat) {
		ScoutModule csg = (ScoutModule) this.OTemplate.getUserModule();
		DataMap<String, Serializable> m = (DataMap<String,Serializable>) dat;
		DataContainer container = (DataContainer) m.get(PatternLoader.INIT_DATA);
		csg.GatherData(segment, (ScoutNetworkData) container.getPayload());
	}

	@Override
	public int getDataUnitCount() {
		ScoutModule g = (ScoutModule) this.OTemplate.getUserModule();
		return g.getCells();
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
