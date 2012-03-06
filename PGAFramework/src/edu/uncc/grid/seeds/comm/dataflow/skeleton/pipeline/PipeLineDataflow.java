package edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
import edu.uncc.grid.seeds.comm.dependency.CycleVersionMissmatch;
import edu.uncc.grid.seeds.comm.dependency.EngineClosedException;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;
import edu.uncc.grid.seeds.comm.dependency.SplitCoalesceHandler;

public class PipeLineDataflow extends Dataflow {
	/**
	 * Used by the dataflow to supply chunks of data to the pipeline.
	 */
	int DataNum;
	boolean Hibernated;
	public PipeLineDataflow() {
		DataNum = 0;
		Hibernated = false;
	}
	
	/********
	 *      *
	 ********/
	private static final long serialVersionUID = 1L;
	int counter = 0;
	@Override
	public boolean computeOneCycle() {
		PipeLine UserMod = (PipeLine) this.getUserModule();
		try {
			if( getDataFlowRoll() == Types.DataFlowRoll.SOURCE ){
				Thread.currentThread().setName("SOURCE");
				if(DataNum < UserMod.getDataCount()){
					Serializable obj = UserMod.DiffuseData( DataNum );
				
					getOutputs()[0].sendObj(obj);
					
					DataNum ++;
				}else{
					Serializable obj = new PipeFinish(); //tell stages the work is done.
					
					getOutputs()[0].sendObj(obj);
					
					DataNum ++;
					return false;
				}
			} else if( getDataFlowRoll() == Types.DataFlowRoll.SINK ){
				Thread.currentThread().setName("SINK");
				Serializable packet = null;
				try {
					++counter;
					packet =  getInputs()[0].takeRecvObj();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (CycleVersionMissmatch e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if( packet instanceof PipeFinish){
					return false;
				}else{
					UserMod.GatherData( packet);
				}
			}else{
				//PipeLineStage pipeline_stage = (PipeLineStage) stateData;
					
				PipeLine pipe = (PipeLine)this.getUserModule();
				Thread.currentThread().setName("STAGE " + getStartStage());
				if( getInputs() != null){
					Serializable input = (Serializable) getInputs()[0].takeRecvObj();
					if( input instanceof PipeFinish ){
						getOutputs()[0].sendObj(input);
						return false;
					}
					
					
					PipeLineStage stage_state = (PipeLineStage) getState(); 
					Serializable[] state = stage_state.State;
					for( int stage = StartStage; stage < StopStage; stage++){
						pipe.setStateFull( state[stage - StartStage] );
						input = pipe.Compute(stage, input);
					}
				
					if( getOutputs() != null){
						getOutputs()[0].sendObj(input);
					}
					
					/*if( //2
						 stateData.getCycleVersion() == 1000
						//4	
						|| stateData.getCycleVersion() == 2000
						//8
						//|| stateData.getCycleVersion() == 3000
						//16
						//|| stateData.getCycleVersion() == 4000
						//32
						//|| stateData.getCycleVersion() == 5000
						//64
					){
						this.setHibernated(true);
					}*/
				

				}else{
					return false;
				}
			
				// this would compute based on the stage number.
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			System.err.println("PipeLineDataflow.computeOneCycle():  Stage " + getStartStage() + ":" +
					getStopStage() );
			e.printStackTrace();
		} catch (CycleVersionMissmatch e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch( Exception e){
			PipeLineStage pipeline_stage = (PipeLineStage) stateData;
			System.err.println("PipeLineDataflow.computeOneCycle():  Stage " + pipeline_stage.getStartStage() + ":" +
					pipeline_stage.getStopStage() );
			e.printStackTrace();
		}*/ catch (EngineClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	@Override
	public DataflowLoader getDataflowLoaderInstance() {
		return new PipeLineLoader();
	}

	byte control;
	@Override
	public byte getControl() {
		return control;
	}
	@Override
	public void setControl(byte set) {
		control = set;
	}

	
	@Override
	public boolean isHibernated() {
		// TODO Auto-generated method stub
		return Hibernated;
	}

	@Override
	public void setHibernated(boolean set) {
		Hibernated = set;
	}

	@Override
	public Dataflow getNewInstance() {
		return new PipeLineDataflow();
	}

	/**
	 * StartStage is the first stage that this dataflow perceptron has to compute
	 */
	public int StartStage;
	/**
	 * StopStage is the last stage this perceptron has to compute.
	 * Each perceptron can comput multiple stages.  The number of stages allocated
	 * to each perceptron dependes on the number of resources available and the 
	 * computation time that it takes to process one stage.
	 */
	public int StopStage;
	public PipeLineDataflow( int start_stage, int stop_stage){
		StartStage = start_stage;
		StopStage = stop_stage;
	}
	
	@Override
	public List<Dataflow> onGrainSizeSplit(int level) {
		/**
		 * Return two dataflows
		 */
		List<Dataflow> ans = new ArrayList<Dataflow>();
		if( StartStage == StopStage ){
			ans = null;
		}else{
			int length = StopStage - StartStage;
			int dist = StartStage + (length / 2);
			
			//create the seg-id for the first dataflow
			HierarchicalSegmentID seg_id_one = new HierarchicalSegmentID( 1, 2, this.getSegID());
			//create first dataflow
			PipeLineDataflow one = new PipeLineDataflow( StartStage, dist);
			
			PipeLine mod = (PipeLine) this.getUserModule();
			Serializable state_one[] = new Serializable[dist-StartStage];
			for( int i = StartStage; i < dist; i++){
				state_one[i-StartStage] = mod.onLoadStage( i );	
			}
			PipeLineStage s = new PipeLineStage();
			s.StartStage = StartStage;
			s.EndStage = dist;
			s.State = state_one;
			
			one.setState( s );
			
			//assign the seg id
			one.setSegID(seg_id_one);
			
			//the input for one is the input for this dataflow, the output is a new dependency we have to create.
			one.setInputDependencyIDs( this.getInputDependencyIDs());
			HierarchicalDependencyID[] out = {new HierarchicalDependencyID(seg_id_one, 1, 1)};
			one.setOutputDependencyIDs(out);
			one.setCycleVersion(this.getCycleVersion());
			one.setDataFlowRoll(Types.DataFlowRoll.COMPUTE);
			//System.out.println(" one in:"  + one.getInputDependencyIDs()[0].toString() + " out: " + one.getOutputDependencyIDs()[0].toString() );
			
			HierarchicalSegmentID seg_id_two = new HierarchicalSegmentID( 2, 2, this.getSegID());
			PipeLineDataflow two = new PipeLineDataflow( dist, StopStage);
			
			Serializable state_two[] = new Serializable[StopStage-dist];
			for( int i = dist; i < StopStage; i++){
				state_two[i-dist] = mod.onLoadStage( i );	
			}
			
			s = new PipeLineStage();
			s.StartStage = dist;
			s.EndStage = StopStage;
			s.State = state_two;
			one.setState( s );
			
			two.setSegID(seg_id_two);
			two.setInputDependencyIDs( out );
			two.setOutputDependencyIDs(this.getOutputDependencyIDs());
			two.setCycleVersion(this.getCycleVersion());
			two.setDataFlowRoll(Types.DataFlowRoll.COMPUTE);
			//System.out.println(" two in:"  + two.getInputDependencyIDs()[0].toString() + " out: " + two.getOutputDependencyIDs()[0].toString() );
			ans.add(one);
			ans.add(two);
		}
		return ans;
	}
	public int getStartStage() {
		return StartStage;
	}
	public void setStartStage(int startStage) {
		StartStage = startStage;
	}
	public int getStopStage() {
		return StopStage;
	}
	public void setStopStage(int stopStage) {
		StopStage = stopStage;
	}

	@Override
	public Dataflow onGrainSizeCoalesce(List<Dataflow> perceptrons, int level) {
		return null;
	}

	@Override
	public SplitCoalesceHandler getSplitCoalesceHander() {
		return null;
	}

	@Override
	public String DbgGetCurrVersion() {
		// TODO Auto-generated method stub
		return "not implemented";
	}
}
