package edu.uncc.grid.seeds.comm.dataflow;

import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;


public abstract class DataflowLoader extends edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private BasicLayerInterface UserMod;
	
	public BasicLayerInterface getUserMod() {
		return UserMod;
	}

	public void setUserMod( BasicLayerInterface set ){
		UserMod = set;
	}
	
	public abstract boolean instantiateSourceSink();
	/**
	 * Called by the framework.  The Advance user returns a Dataflow.  The
	 * segment number goes from 0 to min_cpu.  min_cpu is the minimum number of 
	 * process set by the basic or advanced user.
	 * @param segment
	 * @return
	 */
	public abstract Dataflow onLoadPerceptron(int segment);
	
	public abstract void onUnloadPerceptron(int segment, Dataflow perceptron );
	
	@Override
	public String getHostingTemplate() {
		return DataflowLoaderTemplate.class.getName();
	}
	@Override
	public void initializeModule(String[] args) {
		// TODO Auto-generated method stub
		
	}
	public abstract int getMinimunCPUCount();
	
	public DataflowLoaderTemplate getDataflowLoaderTemplateInstance(){
		return new DataflowLoaderTemplate(this.Framework);
	}

}
