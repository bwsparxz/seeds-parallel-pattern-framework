package edu.uncc.grid.seeds.comm.dataflow;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.NATNotSupportedException;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.seeds.comm.dependency.AdvertsMissingException;
import edu.uncc.grid.seeds.comm.dependency.Dependency;
import edu.uncc.grid.seeds.comm.dependency.DependencyEngine;
import edu.uncc.grid.seeds.comm.dependency.EngineClosedException;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

public class StablishInputConnections extends Thread {
	Dataflow Flow;
	DependencyEngine Engine;
	PipeID PatternId;
	public StablishInputConnections( Dataflow flow, DependencyEngine engine, PipeID pattern_id){
		Flow = flow;
		Engine = engine;
		PatternId = pattern_id;
		
	}

	public static final long INPUT_TIMEOUT = 1000L * 60L * 10L; 
	
	@Override
	public void run() {
		HierarchicalDependencyID[] list = Flow.getInputDependencyIDs();
		if( list != null){
			Dependency[] dept = new Dependency[list.length];
			if(list.length == 0){
				System.err.println("StablishInputConnection:run() WARNING NO INPUT DEPENDENCIES ON THIS PERCEPTRON ");
			}
			for( int i = 0; i < list.length ; i++){
				setName("InputConnections.seg" + Engine.getSegment() + " Connect to: " + i );
				try {
					
					if( list[i] != null){
						Node.getLog().log(Level.FINEST, "myseg: "+ Flow.getSegID().toString() + "getting input dependency: index:" 
								+ i + " id:" + list[i].toString() );
						dept[i] = Engine.getInputDependency(PatternId, list[i], INPUT_TIMEOUT, Flow.getCycleVersion()
							, Flow.getSplitCoalesceHander() );
						
						
					}else{
						//this is to allow certain connections to be disconnected on some nodes. i.e stencils.
						Node.getLog().log(Level.FINEST, "myseg: "+ Flow.getSegID().toString() + "getting input dependency: index:" 
								+ i + " NULL " );
						dept[i] = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (TunnelNotAvailableException e) {
					e.printStackTrace();
				} catch (AdvertsMissingException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (EngineClosedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NATNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			
			}
			
			Flow.setInputs( dept );
			Node.getLog().log(Level.FINEST, "input dependencies set. ");
		}
	}
	
	
}
