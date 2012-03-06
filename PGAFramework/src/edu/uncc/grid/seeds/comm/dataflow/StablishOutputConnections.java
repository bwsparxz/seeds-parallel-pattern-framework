package edu.uncc.grid.seeds.comm.dataflow;

import java.io.IOException;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.seeds.comm.dependency.DependencyEngine;
import edu.uncc.grid.seeds.comm.dependency.EngineClosedException;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.Dependency;
import edu.uncc.grid.seeds.comm.dependency.SplitCoalesceHandler;

public class StablishOutputConnections extends Thread {
	Dataflow Flow;
	DependencyEngine Engine;
	PipeID PatternId;
	Dependency[] Outputs;
	public StablishOutputConnections( Dataflow flow, DependencyEngine engine, PipeID pattern_id){
		Flow = flow;
		Engine = engine;
		PatternId = pattern_id;
	}

	@Override
	public void run() {
		//Note: we can't connect the connection linearly, because some pattern (stencil) can deadlock
		//if the linear deployment locks on one connection while not deploying a connection that the neighbor
		//cell needs.  So this will be replace with n different threads connecting.
		//TODO
		//TODO
		//TODO
		HierarchicalDependencyID[] lst = Flow.getOutputDependencyIDs();
		if( lst != null){
			Outputs = new Dependency[lst.length];
			StablishOneOutputDependency[] dependency_thread = new StablishOneOutputDependency[lst.length];
			if(lst.length == 0){
				System.err.println("StablishInputConnection:run() WARNING NO INPUT DEPENDENCIES ON THIS PERCEPTRON ");
			}
			for( int i = 0; i < lst.length; i++){
				if( lst[i] != null){
					
					dependency_thread[i] = new StablishOneOutputDependency(lst[i], Flow.getCycleVersion(), Flow.getSplitCoalesceHander(), i);
					dependency_thread[i].start();
					//outputs[i] = Engine.registerOutputDependency(lst[i],  Flow.getCycleVersion()
					//	, Flow.getSplitCoalesceHander() );
				}else{
					Node.getLog().log(Level.FINEST, "getting output dependency: " + i + " NULL " );
					//outputs[i] = null;
					Outputs[i] = null;
				}
			}
			boolean all_done = false;
			while( !all_done){
				all_done = true;
				for( int i = 0; i < lst.length; i++){
					if( lst[i] != null){
						if( !dependency_thread[i].isConnected() ){
							all_done = false;
							try {
								Thread.sleep(50);
								break;
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}//else move on to the next one.
				}
			}
			
			Flow.setOutputs(Outputs);
			Node.getLog().log(Level.FINEST, "output dependencies set." );
		}
	}
	/**
	 * This class manages each connection separately.  This resolves the problem where by starting one output after 
	 * the last in linear fashion, the framework would deadlock.  The test example was with a proces A spliting
	 * while connected to process b
	 * ___________
	 * |    |     |
	 * |  A |  B  |
	 * |____|_____|
	 * 
	 * A then splits.
	 * ___________
	 * |A.1 |     |
	 * |____|  B  |
	 * |A.2 |     |
	 * |____|_____|
	 * 
	 * If A.1 and A.2 start with the output linearly starting from top.  Now, A.1 does not wait for the top, but
	 * A.2 waits for the top output, A has the ouput for rith and input as well.  B will connect, but deadlock 
	 * waiting for the other half of the connection (since it needs A.1 and A.2 to create A connection).  A.2
	 * never provides its output because it is waiting for A.1 to respond to its output.  But A.1 is stuck
	 * at input connection with B, beause B won't ack the connection until it has both halfs.
	 * 
	 * By allowing all the output to wait for connection, the deadlock should not happen, and the next testing phase
	 * would be to make sure that we don't have concurrency faults.  
	 * 
	 * @author jfvillal
	 *
	 */
	private class StablishOneOutputDependency extends Thread{
		HierarchicalDependencyID ConnectionID;
		long CycleVersion;
		SplitCoalesceHandler Handler;
		int Index;
		boolean Connected;
		public StablishOneOutputDependency( HierarchicalDependencyID id,  long cycle_version, SplitCoalesceHandler handler, int index){
			ConnectionID = id;
			CycleVersion = cycle_version;
			Handler = handler;
			Index = index;
			Connected = false;
			setName( "SettingOutput: " + id.toString() );
		}
		
		@Override
		public void run() {
			try {
				Node.getLog().log(Level.FINEST, "myseg: " + Flow.getSegID().toString() + "getting output dependency: " 
						+ " index: " + Index +  " id: " + ConnectionID.toString() );
				Outputs[Index] = Engine.registerOutputDependency(ConnectionID,  Flow.getCycleVersion()
					, Flow.getSplitCoalesceHander() );
				Connected = true;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (EngineClosedException e) {
				e.printStackTrace();
			}
		}
		public boolean isConnected(){
			return Connected;
		}
		
	}
	
}
