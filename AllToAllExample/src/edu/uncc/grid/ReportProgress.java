package edu.uncc.grid;

import java.io.Serializable;
import java.util.List;

import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.datamodules.ReduceDataState;
import edu.uncc.grid.pgaf.interfaces.basic.Reduce;

public class ReportProgress extends Reduce {

	@Override
	public Data WorkerSend(ReduceDataState input) {
		DataForAll all = (DataForAll) input;
		if( all.getIteration() > 20 ){
			return null; //this ends the parallel task
		}
		DataMap<String,Integer> map = new DataMap<String,Integer>();
		map.put("value", all.getCount() );
		return map;
	}

	@Override
	public void ServerReduce(List<Serializable> data) {
		for( Serializable dat : data ){
			DataMap<String,Integer> map = (DataMap<String,Integer>) dat;
			System.out.println( "Display with gui or otherwise " + map.get("value"));
		}
	}

	@Override
	public int getCellCount() {
		return 4;
	}

	@Override
	public void initializeModule(String[] args) {}


}
