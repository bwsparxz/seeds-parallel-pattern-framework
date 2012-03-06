package edu.uncc.grid.seeds.comm.dataflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;

public class DataflowGatherThread extends Thread {

	List<ConnectionManager> ConnectionList;
	DataflowLoaderTemplate PatternLoaderT;
	int NewSegments ;
	public DataflowGatherThread( List<ConnectionManager> lst , DataflowLoaderTemplate data_flow_template ){
		ConnectionList = lst;
		PatternLoaderT = data_flow_template;
		//double the cpu count to stay away from the statically assignend segment id's.
		NewSegments = ((DataflowLoader)this.PatternLoaderT.getUserModule()).getMinimunCPUCount() * 2;
		SiblingMap = new HashMap<String, List<Dataflow>>();
		setName("DataflowGatherThread");
	}
	
	public static Map<Long, Long> DbgHibernationVersion = new HashMap<Long, Long>();
	
	@Override
	public void run() {
		/*
		 * Get the job units and end the patter.
		 * this routine needs to be modified.  
		 */
		//TODO this routine is rplace by the algorithm above
		
		while(true){
			ConnectionManager delete = null;
			synchronized( ConnectionList){
				for( ConnectionManager manager: ConnectionList){
					try {
						Dataflow output;
						
						output = (Dataflow) manager.pollRecvingObject();
						if( output != null){
							for( Long i : output.DbgStoreHibernationVersions){
								Long val = DbgHibernationVersion.get(i);
								if( val != null){
									DbgHibernationVersion.put(i, val+1L);
								}else{
									DbgHibernationVersion.put(i, 0L );
								}
							}
							//byte t = output.getControl() ;
							
							output.setUserModule(((DataflowLoader)PatternLoaderT.getUserModule()).getUserMod());
							
							if( output.getControl() != Types.DataControl.NO_WORK_FOR_YOU){
								if( output.isHibernated() ){
									//proceed to split the dataflow and put the sub-dataflows back into the queue.
									//add the number of new pieces to OutstandingDataflow.
									//add back in for testing.
									if( output.getControl() == Types.DataControl.SPLIT){
										List<Dataflow> states = output.onGrainSizeSplit(1);
										PatternLoaderT.NodesWithDataUnit.remove(manager.getRemoteNodeConnectionPipeID());
										if( states != null){
											
											for( Dataflow dataflow_state: states){
												dataflow_state.setSegment( ++NewSegments);
												dataflow_state.setUserModule(
														/*the loader's module is the user module*/
														((DataflowLoader)PatternLoaderT.getUserModule()).getUserMod()
														);
														//output.getUserModule());
												dataflow_state.setHibernated(false);
												//it should be coalesced when it comes back to the matrix.
												dataflow_state.setControl(Types.DataControl.COALESCE);
												PatternLoaderT.getPerceptronLauncherQueue().add( dataflow_state );
											}
										}else{
											output.setHibernated(false);
											PatternLoaderT.getPerceptronLauncherQueue().add( output );
										}
									}else if(output.getControl() == Types.DataControl.COALESCE){
										Dataflow dataflow = coalesceCheck( output );
										if( dataflow != null){
											PatternLoaderT.getPerceptronLauncherQueue().add( dataflow);
										}//if null, it will get stored for next receive.
										
									}
								}else{
									if( output.getControl() == Types.DataControl.COALESCE){
										Dataflow dataflow = coalesceCheck( output );
										if( dataflow != null){
											((DataflowLoader)PatternLoaderT.getUserModule()).onUnloadPerceptron(output.getSegment(), dataflow);
										}
									}else{
										((DataflowLoader)PatternLoaderT.getUserModule()).onUnloadPerceptron(output.getSegment(), output);
									}
									//boolean suc = PatternLoaderT.NodesWithDataUnit.remove(manager.getRemoteNodeConnectionPipeID());
								}
							}	
							delete = manager;
							PatternLoaderT.reduceOutstandingDataflows();
							break;
						} 
					} catch (InterruptedException e) {
						Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
					} catch (DataflowNoCoalesceableException e) {
						System.err.println("The Dataflow cannot coalesce any more.  The call to coalesce was misplaced.");
						e.printStackTrace();
					}
				}
				if( delete != null){
					try {
						delete.close();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					ConnectionList.remove(delete);
				}
				if( PatternLoaderT.getOutstandingDataflow() == 0
						&& PatternLoaderT.getPerceptronLauncherQueue().isEmpty()){
						//make sure we didn't end up with no outstanding perceptron because all of then
						//hibernated at the same time.
					PatternLoaderT.setDataflowDone(true);
					break;
				}	
			}
			
			if( delete == null){//don't stop to wait if we have more work to do
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}	//end run
	
	Map<String, List<Dataflow>> SiblingMap;
	
	private Dataflow coalesceCheck( Dataflow dataflow) throws DataflowNoCoalesceableException{
		HierarchicalSegmentID Parent = dataflow.getSegID().getParent();
		if(Parent == null){
			throw new DataflowNoCoalesceableException();
		}
		if( dataflow.getSegID().getTotal() < 2){
			throw new DataflowNoCoalesceableException();
		}
		List<Dataflow> sibling_list = SiblingMap.get( Parent.toString() );
		if( sibling_list == null){
			sibling_list = new ArrayList<Dataflow>();
			sibling_list.add( dataflow);
			SiblingMap.put( Parent.toString(), sibling_list );
		}else{
			sibling_list.add( dataflow);
			if( sibling_list.size() == dataflow.getSegID().getTotal()){
				//coales
				Collections.sort(sibling_list);
				
				DataflowLoader DPattern = (DataflowLoader) PatternLoaderT.getUserModule();
				
				dataflow.setUserModule( DPattern.getUserMod() );
				
				Dataflow new_dataflow = dataflow.onGrainSizeCoalesce(sibling_list , 1);
				//remove objects from map.
				SiblingMap.remove( dataflow.getSegID().getParent().toString() );
				return  new_dataflow;
			}
		}
		return null; // returned if no coalesce was done.
	}
}
