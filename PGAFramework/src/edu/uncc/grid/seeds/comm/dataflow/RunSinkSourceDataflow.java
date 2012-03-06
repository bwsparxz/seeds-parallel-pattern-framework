package edu.uncc.grid.seeds.comm.dataflow;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.seeds.comm.dependency.DependencyEngine;

public class RunSinkSourceDataflow extends Thread {
	Dataflow Flow;
	DependencyEngine Engine;
	PipeID PatternID;
	int DrainType;
	public RunSinkSourceDataflow( Dataflow flow, DependencyEngine engine, PipeID pattern_id, int sink_source ){
		Flow = flow;
		Engine = engine;
		PatternID = pattern_id;
		DrainType = sink_source;
		if( DrainType == Dataflow.DATAFLOW_SOURCE ){
			setName("SourceThread");
		}else{
			setName(" SinkThread ");
		}
	}
	@Override
	public void run(){
		
		if(DrainType ==  Dataflow.DATAFLOW_SINK){	
			Thread stablish_inputs = new StablishInputConnections(Flow, Engine, PatternID);
			stablish_inputs.run();
		}
		if( DrainType == Dataflow.DATAFLOW_SOURCE ){
			Thread stablish_outputs = new StablishOutputConnections(Flow, Engine, PatternID);
			stablish_outputs.run(); //output can run on this thread since all this thread would do is wait.
		}
		//very bad patch.
		//long i = 0;
		//long hibernate = -1;
		//long HALF_STOP = DataflowLoaderTemplate.VERSION_STOP_STEP_SIZE/2;
		while( Flow.computeOneCycle() ){ // returns true if it wants to loop
			Flow.setCycleVersion( Flow.getCycleVersion() + 1);
			
			
		/*	if( hibernate != -1 ){
				if( i + 10 > hibernate ){
					try {
						Thread.sleep(20); //slow down so that the process does not get shocked during transition.
						System.out.println("RunSinSourceDataflow:run() ------------------------------ slowing down ");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if( (i -5) > hibernate){
						hibernate = -1; //reset hibernate.
						System.out.println("RunSinSourceDataflow:run() ------------------------------ back to full speec ");
					}
				}
			}else{
				if( i % HALF_STOP == 0){
					long var = DataflowLoaderTemplate.getClosestVersionStop(Flow);
					if( var != -1){
						hibernate = var;
					}
				}
			}
			++i;*/
		}
		
	}
}
