package edu.uncc.grid.seeds.dataflow.pattern.alltoall;

import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;

public class LoadAlltoAll extends DataflowLoader {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	AlltoAllDataflow[] Units;
	public LoadAlltoAll(AlltoAll mod ){
		this.setUserMod(mod);
		int cell_count = mod.getCellCount();
		Units =  new AlltoAllDataflow[cell_count];
		
		
		for( int i = 0 ; i < Units.length ; i++){
			HierarchicalSegmentID seg = new HierarchicalSegmentID( i , 1);
			HierarchicalDependencyID [] outs = new HierarchicalDependencyID[Units.length ];
			for( int j = 0; j < Units.length; j++){
				if( j == i ){
					outs[i] = null;
				}else{
					outs[j] = new HierarchicalDependencyID(seg, j, 1);
				}
			}
			Units[i] = new AlltoAllDataflow();
			Units[i].setSegID( seg );
			Units[i].setOutputDependencyIDs(outs);
		}
		
		for( int i = 0 ; i < Units.length ; i++){
			HierarchicalSegmentID seg = new HierarchicalSegmentID(i , 1);
			HierarchicalDependencyID [] ins = new HierarchicalDependencyID[Units.length];
			for( int j = 0; j < Units.length; j++){
				if( j == i ){
					ins[j] = null;
				}else{
					ins[j] = new HierarchicalDependencyID(seg, j, 1);
				}
			}
			Units[i].setInputDependencyIDs(ins);
		}		
	}
	
	@Override
	public boolean instantiateSourceSink() {
		return false;
	}

	@Override
	public Dataflow onLoadPerceptron(int segment) {
		AlltoAll mod = (AlltoAll) this.getUserMod();
		AllToAllData ans = mod.DiffuseData( segment) ;
		
		Units[segment].setState( ans );
		Units[segment].setSegment(segment);
		return Units[segment];
	}

	@Override
	public void onUnloadPerceptron(int segment, Dataflow perceptron) {
		AlltoAll mod = (AlltoAll) this.getUserMod();
		AlltoAllDataflow flow = (AlltoAllDataflow) perceptron;
		AllToAllData state = (AllToAllData) flow.getState();
		mod.GatherData(flow.getSegment(), state);
	}

	@Override
	public int getMinimunCPUCount() {
		AlltoAll mod = (AlltoAll) this.getUserMod();
		return mod.getCellCount();
	}

}
