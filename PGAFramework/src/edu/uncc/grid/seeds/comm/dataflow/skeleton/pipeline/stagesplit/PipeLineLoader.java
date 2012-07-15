package edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline.stagesplit;

import java.io.Serializable;

import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;


public class PipeLineLoader extends DataflowLoader {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int getMinimunCPUCount() {
		//the minimum would be the number of stages plus two.
		return ((PipeLine)this.getUserMod()).getMinProcessCount();
	}

	@Override
	public Dataflow onLoadPerceptron(int segment) {
		//System.out.println("Loading perceptron");
		
		
		//System.out.println( "Creating stage: " + segment );
		
		
		PipeLine UserPipeInterface = (PipeLine) this.getUserMod();
		int stage_count = UserPipeInterface.getStageCount();
		int min_cpu = UserPipeInterface.getMinProcessCount();
		
		int stages_per_cpu = stage_count / min_cpu;
		int remainder_stage = stage_count % min_cpu;
		
		int start_seg = stages_per_cpu * segment;
		int end_seg = stages_per_cpu * (segment + 1);
		if( segment == min_cpu -1){
			end_seg += remainder_stage;
		}
		
		
		
		//give stage_per_cpu amount of stages to each dataflow perceptron
		//give stage_per_cpu + remainder_stage to the last perceptron
		
		
		
		PipeLineDataflow flow = null;
		
		if( segment == Dataflow.DATAFLOW_SOURCE){ //just output
			HierarchicalSegmentID seg_id = new HierarchicalSegmentID( 0, 1);
			HierarchicalDependencyID[] output_list = new HierarchicalDependencyID[1];
			output_list[0] = new HierarchicalDependencyID(seg_id, 0,1);
			flow = new PipeLineDataflow();
			flow.setCycleVersion(0L);
			flow.setSegID(seg_id);
			flow.setOutputDependencyIDs(output_list);
			flow.setInputDependencyIDs(null);
			flow.setDataFlowRoll(Types.DataFlowRole.SOURCE);
		}else if(segment == Dataflow.DATAFLOW_SINK){
			//just input
			HierarchicalSegmentID seg_id = new HierarchicalSegmentID( min_cpu, 1);
			HierarchicalDependencyID[] input_list = new HierarchicalDependencyID[1];
			input_list[0] = new HierarchicalDependencyID(seg_id, 0,1);
			flow = new PipeLineDataflow();
			flow.setSegID(seg_id);
			flow.setInputDependencyIDs( input_list );
			flow.setOutputDependencyIDs( null );
			flow.setDataFlowRoll(Types.DataFlowRole.SINK);
		}else{
			flow = new PipeLineDataflow( start_seg, end_seg );
			flow.setCycleVersion(0L);
			
			HierarchicalSegmentID in_seg_id = new HierarchicalSegmentID( segment, 1);
			HierarchicalDependencyID[] input_list = new HierarchicalDependencyID[1];
			input_list[0] = new HierarchicalDependencyID(in_seg_id, 0,1);
			
			HierarchicalSegmentID seg_id = new HierarchicalSegmentID( segment+1, 1);
			HierarchicalDependencyID[] output_list = new HierarchicalDependencyID[1];
			output_list[0] = new HierarchicalDependencyID(seg_id, 0, 1 );
			
			PipeLine mod = (PipeLine)this.getUserMod();
			
			Serializable state[] = new Serializable[end_seg - start_seg];
			for( int i = start_seg; i < end_seg; i++){
				state[i-start_seg] = mod.onLoadStage( i );	
			}
			PipeLineStage s = new PipeLineStage();
			s.StartStage = start_seg;
			s.EndStage = end_seg;
			s.State = state;
			flow.setState( s );
			
			flow.setSegID(seg_id);
			flow.setInputDependencyIDs(input_list);
			flow.setOutputDependencyIDs(output_list);
			flow.setDataFlowRoll(Types.DataFlowRole.COMPUTE);
			
		}
		
		return flow;
	}

	@Override
	public void onUnloadPerceptron(int segment, Dataflow perceptron) {
		PipeLine mod = (PipeLine)this.getUserMod();
		
		PipeLineStage s = (PipeLineStage) perceptron.getState();
		for( int i = s.StartStage; i < s.EndStage; i++){
			mod.onUnloadStage( i, s.State[i-s.StartStage]);
		}
	}

	@Override
	public boolean instantiateSourceSink() {
		return true;
	}

}
