package edu.uncc.grid.test;

import edu.uncc.grid.newpattern.Workpool;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;

public class WorkpoolTest extends Workpool {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WorkpoolTest(){
		
	}
	
	@Override
	public Data Compute(Data input) {
		DataMap<String,Integer> d = (DataMap<String,Integer>) input;
		d.put("num",  d.get("num") + 1 );
		return d;
	}

	@Override
	public Data DiffuseData(int segment) {
		DataMap<String,Integer> d = new DataMap<String,Integer>();
		d.put("num", 5);
		return d;
	}

	@Override
	public void GatherData(int segment, Data dat) {
		DataMap<String,Integer> d = (DataMap<String,Integer> ) dat;
		System.out.println("got: " + d.get("num"));
	}

	@Override
	public int getDataCount() {
		return 10;
	}

	@Override
	public void initializeModule(String[] args) {
		
	}

}
