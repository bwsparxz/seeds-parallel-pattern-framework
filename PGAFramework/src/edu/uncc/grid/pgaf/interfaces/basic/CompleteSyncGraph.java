package edu.uncc.grid.pgaf.interfaces.basic;

import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.templates.CompleteSyncGraphTemplate;

public abstract class CompleteSyncGraph extends BasicLayerInterface{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public  abstract boolean OneIterationCompute( AllToAllData data);
	public abstract AllToAllData DiffuseData( int segment);
	public abstract void GatherData( int segment, AllToAllData data);
	
	public abstract int getCellCount();
	//public abstract int getIterations();
	
	@Override
	public String getHostingTemplate() {
		return CompleteSyncGraphTemplate.class.getName();
	}
	
	//implemented by the user.
	//public void initializeModule(String[] args) {
}
