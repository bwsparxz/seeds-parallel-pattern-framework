package edu.uncc.grid.seeds.dataflow.pattern.stencil.split;

import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;
import edu.uncc.grid.seeds.dataflow.pattern.stencil.split.Stencil;

public class LoadStencil extends DataflowLoader {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	StencilDataflow [][] DataflowMatrix;
	

	
	public LoadStencil(Stencil mod){
		this.setUserMod(mod);
		//int size = mod.getCellCount();
		int width = mod.getWidthCellCount();
		int height = mod.getHeightCellCount();
		// col row
		//width height
		DataflowMatrix = new StencilDataflow[width][height];
		
		int segment = 0;
		for( int r = 0; r < height ; r++){
			for(int c = 0; c < width; c++){
				DataflowMatrix[c][r] = new StencilDataflow();
				HierarchicalSegmentID seg = new HierarchicalSegmentID(segment , 1);
				DataflowMatrix[c][r].setSegID(seg);
				HierarchicalDependencyID [] outs = new HierarchicalDependencyID[4];
				
				if( r - 1 >= 0){
					outs[Stencil.TOP] = new HierarchicalDependencyID(seg, Stencil.TOP, 1);
				}else{
					outs[Stencil.TOP] = null;
				}
				if( c+1 < width){
					outs[Stencil.RIGHT] = new HierarchicalDependencyID(seg, Stencil.RIGHT, 1);
				}else{
					outs[Stencil.RIGHT] = null;
				}
				if( r+1 < height ){
					outs[Stencil.BOTTOM] = new HierarchicalDependencyID(seg, Stencil.BOTTOM, 1);
				}else{
					outs[Stencil.BOTTOM] = null;
				}
				if( c-1 >= 0 ){
					outs[Stencil.LEFT] = new HierarchicalDependencyID(seg, Stencil.LEFT, 1);
				}else{
					outs[Stencil.LEFT] = null;
				}
				
				DataflowMatrix[c][r].setOutputDependencyIDs(outs);
				++segment;
			}
		}
		
		// [0,1,2]
		// [3,4,5]
		// [6,7,8]
		
		for( int r = 0; r < height ; r++){
			for(int c = 0; c < width; c++){
				HierarchicalDependencyID [] ins = new HierarchicalDependencyID[4];
				if( r-1 >= 0 ){
					ins[Stencil.TOP] = DataflowMatrix[c][r-1].getOutputDependencyIDs()[Stencil.BOTTOM];
				}else{
					ins[Stencil.TOP] = null;
				}
				if( c+1 < width){
					ins[Stencil.RIGHT] = DataflowMatrix[c+1][r].getOutputDependencyIDs()[Stencil.LEFT];	
				}else{
					ins[Stencil.RIGHT] = null;
				}
				if( r+1 < height){
					ins[Stencil.BOTTOM] = DataflowMatrix[c][r+1].getOutputDependencyIDs()[Stencil.TOP];
				}else{
					ins[Stencil.BOTTOM] = null;
				}
				if( c-1 >= 0 ){
					ins[Stencil.LEFT] = DataflowMatrix[c-1][r].getOutputDependencyIDs()[Stencil.RIGHT];
				}else{
					ins[Stencil.LEFT] = null;
				}
				DataflowMatrix[c][r].setInputDependencyIDs(ins);
			}
		}
	}
	

	@Override
	public boolean instantiateSourceSink() {
		// Don't since this is not a stream based framework.
		return false;
	}

	@Override
	public Dataflow onLoadPerceptron(int segment) {
		Stencil mod = (Stencil) this.getUserMod();
		//int sqrt = (int) Math.sqrt( mod.getCellCount() );
		int r = segment / mod.getWidthCellCount();
		int c = segment % mod.getWidthCellCount();
		StencilData state = mod.DiffuseData(segment);
		DataflowMatrix[c][r].setState( state );
		DataflowMatrix[c][r].CellSegment = segment;
		return DataflowMatrix[c][r];
	}

	@Override
	public void onUnloadPerceptron(int segment, Dataflow perceptron) {
		//put dataflow back in user mod.
		Stencil mod = (Stencil) this.getUserMod();
		StencilDataflow flow = (StencilDataflow) perceptron;
		StencilData state = (StencilData) flow.getState();
		mod.GatherData(flow.CellSegment, state);
	}

	@Override
	public int getMinimunCPUCount() {
		Stencil mod = (Stencil) this.getUserMod();
		return mod.getWidthCellCount() * mod.getHeightCellCount();
	}

}
