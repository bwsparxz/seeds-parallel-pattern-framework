package edu.uncc.grid.seeds.otemplate.pattern.networkscout;

import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;

public class ScoutModule extends BasicLayerInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ScoutNetworkData[] Results;
	
	int CellNum;
	int TesterId;
	
	public ScoutNetworkData DiffuseData(int segment){
		ScoutNetworkData dat = new ScoutNetworkData();
		return dat;
	}
	
	public void GatherData(int segment, ScoutNetworkData data ){
		Results[segment] = data;
	}
	
	public int getCells(){
		return CellNum;
	}
	public int getTesterId(){
		return TesterId;
	}
	
	@Override
	public void initializeModule(String[] args) {
		CellNum = Integer.parseInt(args[0]);
		Results = new ScoutNetworkData[CellNum];
		TesterId = Integer.parseInt(args[1]);
	}

	@Override
	public String getHostingTemplate() {
		return NetworkScoutTemplate.class.getName();
	}

}
