package edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline.sideways;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
import edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline.PipeFinish;
import edu.uncc.grid.seeds.comm.dependency.CycleVersionMissmatch;
import edu.uncc.grid.seeds.comm.dependency.Dependency;
import edu.uncc.grid.seeds.comm.dependency.EngineClosedException;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;
import edu.uncc.grid.seeds.comm.dependency.SplitCoalesceHandler;

public class PipeLineDataflow extends Dataflow implements SplitCoalesceHandler {
	/**
	 * Used by the dataflow to supply chunks of data to the pipeline.
	 */
	int DataNum;
	
	/**
	 * version not the problem, the var can be deleted
	 */
	//int DebugCountCycles;
	public PipeLineDataflow() {
		DataNum = 0;
		//DebugCountCycles = 0;
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
				if(DataNum < UserMod.getDataCount()){
					Serializable obj = UserMod.DiffuseData( DataNum );
					
					getOutputs()[0].sendObj(obj);
					
					++DataNum;
				}else{
					Serializable obj = new PipeFinish(); //tell stages the work is done.
					
					getOutputs()[0].sendObj(obj);
					
					++DataNum;
					return false;
				}
			} else if( getDataFlowRoll() == Types.DataFlowRoll.SINK ){
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
					UserMod.GatherData( packet );
				}
			}else{
				//PipeLineStage pipeline_stage = (PipeLineStage) stateData;
					
				PipeLine pipe = (PipeLine)this.getUserModule();
				Thread.currentThread().setName("STAGE " + getStartStage());
				if( getInputs() != null){
					Serializable input =  getInputs()[0].takeRecvObj();
					/*if( input.getVersion()!= DebugCountCycles){
						throw new CycleVersionMissmatch( DebugCountCycles, input.getVersion());
					}*/
					if( input instanceof PipeFinish ){
						System.out.println("version: " + this.getCycleVersion() );
						getOutputs()[0].sendObj(input);
						this.setHibernated(false);
						//TODO fix this proble with sending split/coalesce directives to nodes
						if( this.getControl() == Types.DataControl.SPLIT){
							this.setControl(Types.DataControl.INSTRUCTION_DATA);
						}
						
						return false;
					}
					
					
					PipeLineStage stage_state = (PipeLineStage) getState(); 
					Serializable[] state = stage_state.State;
					
					for( int stage = StartStage; stage < StopStage; stage++){
						pipe.setStateFull( state[stage - StartStage] );
						input = pipe.Compute(stage, input);
					}
					
					//++DebugCountCycles;
					
				
					if( getOutputs() != null){
						getOutputs()[0].sendObj(input);
					}
					
					if( //2
							this.getCycleVersion() == 3
						 
							//4	
						//	|| stateData.getCycleVersion() == 2000
							//8
							//|| stateData.getCycleVersion() == 3000
							//16
							//|| stateData.getCycleVersion() == 4000
							//32
							//|| stateData.getCycleVersion() == 5000
							//64
						){
							this.setHibernated(true);
							this.setControl(Types.DataControl.SPLIT);
					}
				

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
			
			e.printStackTrace();
			System.exit(1);
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
	/**
	 * this function will split the pipe line stages side ways so that the
	 * dependency feature of spliting the data can be tested and benchmarked.
	 */
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
			PipeLine mod = (PipeLine) this.getUserModule();
			
			PipeLineStage mod_state = (PipeLineStage) this.getState();
			Serializable[] stage_states = mod_state.State;
			/*
			 * first index is stage number
			 * second index is the splited state
			 */
			int SIZE = 2;
			
			PipeLineStage[] splits = new PipeLineStage[SIZE];//stage_states.length];
			splits[0] = new PipeLineStage();
			splits[1] = new PipeLineStage();
			
			splits[0].StartStage = mod_state.StartStage;
			splits[1].StartStage = mod_state.StartStage;
			
			splits[0].EndStage = mod_state.EndStage;
			splits[1].EndStage = mod_state.EndStage;
			
			splits[0].State = new Serializable[stage_states.length];
			splits[1].State = new Serializable[stage_states.length];
			
			for( int i = 0 ; i < stage_states.length ; i++){
				Serializable[] sideways = mod.splitStateFull(stage_states[i], level);
				if(sideways != null){
					splits[0].State[i] = sideways[0];
					splits[1].State[i] = sideways[1];
				}else{
					Integer zero = 0;
					splits[0].State[i] = zero;
					splits[1].State[i] = zero;
				}
			}
			
			HierarchicalDependencyID[] parent_in = this.getInputDependencyIDs();
			HierarchicalDependencyID[] parent_out = this.getOutputDependencyIDs();
			/**
			 * this loop will assign the split dependencies to the to the data flow 
			 * perceptrons
			 */
			for( int i = 0; i < SIZE; i++){
				PipeLineDataflow flow = new PipeLineDataflow( StartStage, StopStage);
				HierarchicalSegmentID seg_id = new HierarchicalSegmentID( i, SIZE, this.getSegID());
				HierarchicalDependencyID[] in = {    /*Use original segment id since this is an existing dependency*/
						new HierarchicalDependencyID(parent_in[0].getSid(), i, SIZE , parent_in[0])
				};
				HierarchicalDependencyID[] out = {   /*Use original segment id since this is an existing dependency*/
						new HierarchicalDependencyID(parent_out[0].getSid(), i, SIZE , parent_out[0])
				};
				flow.setSegID(seg_id);
				
				flow.setState( splits[i] );
				flow.setInputDependencyIDs(in);
				flow.setOutputDependencyIDs(out);
				flow.setCycleVersion(this.getCycleVersion());
				flow.setDataFlowRoll(Types.DataFlowRoll.COMPUTE);
				//flow.DebugCountCycles = this.DebugCountCycles;
				ans.add(flow);
			}
		}
		return ans;
	}
	@Override
	public Dataflow onGrainSizeCoalesce(List<Dataflow> perceptrons, int level) {
		PipeLineDataflow one = (PipeLineDataflow) perceptrons.get(0);
		PipeLineDataflow two = (PipeLineDataflow) perceptrons.get(1);
		PipeLine mod = (PipeLine) this.getUserModule();
		
		PipeLineStage one_stages = (PipeLineStage)one.getState();
		Serializable[] one_state = one_stages.State;
		
		PipeLineStage two_stages = (PipeLineStage)two.getState();
		Serializable[] two_state =  two_stages.State;
		
		Serializable[] state = new Serializable[one_state.length ];
		for( int i = 0 ; i < one_state.length ; i++){
			Serializable[] one_two = {one_state[i], two_state[i]};
			state[i] = mod.coalesceStateFull(one_two, 1);
		}
		
		PipeLineStage integrated = new PipeLineStage();
		integrated.StartStage = one_stages.StartStage; //both the old perceptron start and end
		integrated.EndStage = two_stages.EndStage; //on the same stages
		integrated.State = state;
		
		PipeLineDataflow state_dataflow = new PipeLineDataflow( one.getStartStage(), two.getStopStage() );
		state_dataflow.setInputDependencyIDs(one.getInputDependencyIDs() ); //TODO this is wrong !!!
		state_dataflow.setOutputDependencyIDs(two.getOutputDependencyIDs());
		state_dataflow.setState(integrated);
		
		return state_dataflow;
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
	public SplitCoalesceHandler getSplitCoalesceHander() {
		return this;
	}
	public static int SPLIT_SIZE = 2;  //this is temporary while the interfaces are fixed
	@Override
	public Serializable[] onSplit(Serializable packet) {
		/**
		 * TODO get rid of the DependencyPacket, and use only Serializables 
		 */
		
		if( packet instanceof PipeFinish ){
			Serializable arr[] = new Serializable[2 ]; //assuming two pieces
			for( int i = 0; i < 2; i++){
				arr[i] = new PipeFinish();
			}
			return arr;
		}else{
			PipeLine mod = (PipeLine) this.getUserModule();
			
			Serializable[] pieces = mod.splitData(packet, 1); 
			
			return pieces;
		}	
	}
	@Override
	public Serializable onCoalesce(Serializable[] pakets) {
		Serializable obj = pakets[0];
		if( obj instanceof PipeFinish){
			return new PipeFinish();
		}else{
			PipeLine mod = (PipeLine) this.getUserModule();
			Serializable out = mod.coalesceData(pakets, 1);
			return out;
		}
	}

	@Override
	public String DbgGetCurrVersion() {
		String str = "in:";
		Dependency[] in = this.getInputs();
		for( int i = 0; i < in.length ; i++){
			str += in[i].getCycleVersion() + ",";
		}
		str += ":ouit:";
		Dependency[] out = this.getInputs();
		for( int i = 0; i < out.length ; i++){
			str += out[i].getCycleVersion() + ",";
		}
		PipeLineStage stages = (PipeLineStage) this.getState();
		Serializable[] state = stages.State;
		str += ":count:";
		int same = -1;
		/*for( int i = 0 ; i < state.length  ; i++){
			PipeLine pipe = (PipeLine)this.getUserModule();
			
			pipe.setStateFull(state[i]);
			int v = pipe.getCout();
			if( v != same ){
				str += "("+i +":"+ v +")";
			}
			same = v ;
		}*/
		
		return str;
	}
}
