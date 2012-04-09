package edu.uncc.grid.seeds.comm.dataflow;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeClient;
import edu.uncc.grid.pgaf.communication.MultiModePipeDispatcher;
import edu.uncc.grid.pgaf.communication.NATNotSupportedException;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.interfaces.advanced.UnorderedTemplate;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;
import edu.uncc.grid.seeds.comm.dependency.Dependency;
import edu.uncc.grid.seeds.comm.dependency.DependencyEngine;
/**
 * 
 * The Dataflow Loader is an UnorderedTemplate that is used to load the dataflow 
 * computationa units as designed by the advanced user.
 * 
 * @author jfvillal
 *
 */
public class DataflowLoaderTemplate extends UnorderedTemplate {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	DependencyEngine Engine;

	/**
	 * The version stop step is used eliminate problems with nodes folling out of step
	 * because the enter into hibernation at the same time at diferent points int the
	 * dataflow graph.
	 * The step also adds a tolerance needed for the dataflow network to reorganized 
	 * after a hibernated perceptron is added back.  Once the network stabilizes, another
	 * percerptron or group of perceptrons can go into hibernation.
	 */
	public static final long VERSION_STOP_STEP_SIZE = 500L;
	
	public DataflowLoaderTemplate(Node n) {
		super(n);
		Engine = null;
		DataflowDone = false;
	}
	/**
	 * This will bring down the connections gracefully.<br>
	 * 
	 * It will shutdown each of the output dependencies followed by the input dependencies.<br>
	 * 
	 * It will close down the Dependency Engine also. <br>
	 * 
	 * @param state
	 */
	void bringDownConnections( Dataflow flow){
		try {
			Dependency[] lst = flow.getOutputs();
			if( lst != null){
				for( int i = 0; i < lst.length; i++){
					if( lst[i] != null){ //can be null if adv user wants it
						if( lst[i].isBound() ){
							lst[i].waitOnSentData();
							lst[i].close();
						}
					}
				}
			}
			lst = flow.getInputs();
			if( lst != null){
				for( int i = 0; i < lst.length; i++){
					if( lst[i] != null){
						if( lst[i].isBound() ){
							lst[i].waitOnRecvData();	
							lst[i].close();	
						}
					}
				}
			}
			Engine.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void bringDownConnectionsSkipRecv( Dataflow flow){
		try {
			Dependency[] lst = flow.getOutputs();
			if( lst != null){
				for( int i = 0; i < lst.length; i++){
					if( lst[i] != null){
						
						if( lst[i].isBound() ){
							lst[i].waitOnSentData();
							lst[i].close();
						}
					}
				}
			}
			//Engine.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * this method returns the closest version where the neighbor node will hibernate.
	 * or it will return -1 if none of the neighbors plan to hibernate.
	 * 
	 * @param perceptron
	 * @return
	 */
	public static long getClosestVersionStop(Dataflow perceptron){
		long neighbor_stop_version = -1;
		
		boolean neighbor_stop_version_set = false;
		
		if( perceptron.getOutputs() != null){
			if( perceptron.getOutputs().length > 0){
				for( int i =0 ; i < perceptron.getOutputs().length ;i++){
					if( perceptron.getOutputs()[i] != null){
						if( perceptron.getOutputs()[i].getCycleVersionStop() == -1) continue;
						if( !neighbor_stop_version_set ){
							neighbor_stop_version = perceptron.getOutputs()[i].getCycleVersionStop();
							neighbor_stop_version_set = true;
						}
						if( neighbor_stop_version > perceptron.getOutputs()[i].getCycleVersionStop() ){
							neighbor_stop_version = perceptron.getOutputs()[i].getCycleVersionStop();
						}
					}
				}
			}
		}
		if( perceptron.getInputs() != null){
			for( int i =0 ; i < perceptron.getInputs().length ;i++){
				if( perceptron.getInputs()[i] != null){
					if( perceptron.getInputs()[i].getCycleVersionStop() == -1) continue;
					if( !neighbor_stop_version_set ){
						neighbor_stop_version = perceptron.getInputs()[i].getCycleVersionStop();
						neighbor_stop_version_set = true;
					}
					if( neighbor_stop_version > perceptron.getInputs()[i].getCycleVersionStop() ){
						neighbor_stop_version = perceptron.getInputs()[i].getCycleVersionStop();
					}
				}
			}
		}
		return neighbor_stop_version;
	}
	/**
	 * returns the farthes version deadline, this is used to ajust the hibernation plan if another
	 * dataflow just happened to enter hibernation cycles at the same time as this dataflow.
	 * @param perceptron
	 * @return
	 */
	public static long getFarthestVersionStop(Dataflow perceptron){
		long neighbor_stop_version = -1;
		boolean neighbor_stop_version_set = false;
		if( perceptron.getOutputs().length > 0){
			
			for( int i =0 ; i < perceptron.getOutputs().length ;i++){
				if( perceptron.getOutputs()[i] != null){
					if( perceptron.getOutputs()[i].getCycleVersionStop() == -1) continue;
					if( ! neighbor_stop_version_set ){
						neighbor_stop_version = perceptron.getOutputs()[i].getCycleVersionStop();
						neighbor_stop_version_set = true;
					}
					
					if( neighbor_stop_version < perceptron.getOutputs()[i].getCycleVersionStop() ){
						neighbor_stop_version = perceptron.getOutputs()[i].getCycleVersionStop();
					}
				}
			}
		}
		for( int i =0 ; i < perceptron.getInputs().length ;i++){
			if( perceptron.getInputs()[i] != null){
				if( perceptron.getInputs()[i].getCycleVersionStop() == -1) continue;
				if( !neighbor_stop_version_set){
					neighbor_stop_version = perceptron.getInputs()[i].getCycleVersionStop();
					neighbor_stop_version_set = true;
				}
				if( neighbor_stop_version < perceptron.getInputs()[i].getCycleVersionStop() ){
					neighbor_stop_version = perceptron.getInputs()[i].getCycleVersionStop();
				}
			}
		}
		return neighbor_stop_version;
	}
	
	public static String printCommLineStatus(Dataflow perceptron){
		String str = ":input:";
		if( perceptron.getInputs() != null){
			for( int i =0 ; i < perceptron.getInputs().length ;i++){
				if(perceptron.getInputs()[i] != null){
					str += perceptron.getInputs()[i].getHierarchicalID().toString() + 
					        ":v:" + perceptron.getInputs()[i].getCycleVersion()
					        + ":c:" + perceptron.getInputs()[i].getCachedObjSize();
				}
			}
		}
		str += ":output:";
		if( perceptron.getOutputs() != null){
			for( int i =0 ; i < perceptron.getOutputs().length ;i++){
				if( perceptron.getOutputs()[i] != null){
					str += perceptron.getOutputs()[i].getHierarchicalID().toString() 
						+ ":v:" + perceptron.getOutputs()[i].getCycleVersion()
						+ ":c:" + perceptron.getOutputs()[i].getCachedObjSize() ;
				}
			}
		}
		
		return str;
	}
	private ConnectionManager getConnectiontoServer(PipeID pattern_id ) throws 
								IOException, ClassNotFoundException, InterruptedException
								, TunnelNotAvailableException, CommunicationLinkTimeoutException, NATNotSupportedException{
		ConnectionManager m_manager = null;

		PipeID my_connection = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		Long my_segment = CommunicationConstants.DYNAMIC; //unorder
		m_manager = MultiModePipeClient.getClientConnection(Network, -4L, pattern_id, my_connection.toString(), my_segment, null, null, null,120000/*10 seconds timeout*/);
		
		while( !m_manager.isBound() && !ShutdownOrder ){
			System.out.println( Thread.currentThread().getId() + " Socket Not Bounded... Waiting pipe_id " + m_manager.getHashPipeId().toString() 
					+ " peer id " + Node.PID.toURI().toString());
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//could add a timeout routine here
		}

		return m_manager;
	}
	
	@Override
	public boolean ClientSide(PipeID pattern_id) {
		boolean pattern_done = true;
		try {
				
				//Node.getLog().log(Level.FINEST, "Creating Socket Connection..." );
				ConnectionManager m_manager = getConnectiontoServer( pattern_id);;
			
				
				DataflowLoader loader = (DataflowLoader) this.getUserModule();
				loader.setFramework(this.Network);
				//jan 13 changed so that the client side only receives one job.
					
				try {
					// get Data Object from socket 
					Serializable trans = m_manager.takeRecvingObject();
					Dataflow perceptron = (Dataflow) trans;
					//if( perceptron.getUserModule() == null){
						perceptron.setUserModule(loader.getUserMod() );
					/*}else{
						loader.setUserMod(perceptron.getUserModule());
					}*/
					if( perceptron.getState() != null){
						perceptron.getUserModule().setStateFull( perceptron.getState() );
					}
					
					perceptron.setHibernated(false);
					Engine  = new DependencyEngine(this.Network, perceptron.getSegment(), pattern_id
												, perceptron.getUserModule().getRawByteEncoder());			
					Engine.startServer();
					
					
					if( ! (perceptron instanceof NoMoreDataflowPerceptrons )){// data.getControl() != Types.DataControl.NO_WORK_FOR_YOU ){
						
						Thread.currentThread().setName( Thread.currentThread().getName() + " seg: " + perceptron.getSegID().toString() );
						
						//int segment = data.getSegment();
						//give it to the user's application Compute()
						Node.getLog().log( Level.FINER, Thread.currentThread().getName() +" : \n" +
								"got -> " + perceptron.getSegment() 
								+ " seg: " + perceptron.getSegID().toString() 
								+ " inputs " + perceptron.inputsAsString() + " outputs " + perceptron.outputsAsString());	
						//feeding it its own object for now, but adding the operator feature would allow use to 
						//pass the object from one pattern to the next pattern.
						/*this is done using threads so that there won't be a dead lock when all the nodes are 
						 * waiting for the input connections while not broadcasting their own output connections. 
						 * */
						FileWriter w = new FileWriter( "./dept_conect_time.txt", true);
						
						long dept_start = System.currentTimeMillis();
						Thread stablish_inputs = new StablishInputConnections(perceptron, Engine, pattern_id);
						stablish_inputs.start();
						StablishOutputConnections stablish_outputs = new StablishOutputConnections(perceptron, Engine, pattern_id);
						stablish_outputs.run(); //output can run on this thread since all this thread would do is wait.
						stablish_inputs.join();
						
						w.write( (System.currentTimeMillis() - dept_start ) + "\n" );
						w.close();
						
						long version_stop = -1;
						//this is to measure the feeding rate for this datalfow
						long start = System.currentTimeMillis();
						long version_measure_start = perceptron.getCycleVersion();
						/**
						 * number of cycles to wait until attempting to hibernate again.
						 */
						long hibernate_wait_until_version = 0;
						
						//used in extreme cases where processes are getting dissynchronized.
						//FileWriter w = new FileWriter( "./dataflow_version_debug"+ Thread.currentThread().getName() +".txt", true);
						//w.write("version" + version_measure_start + "\n");
						
						boolean cycle_again = true;
						while( cycle_again = perceptron.computeOneCycle(/*perceptron.State*/)){
							//pool the loader socket to see if we should split.
							Serializable control = m_manager.pollRecvingObject();
							if( control != null ){
								if( control instanceof HibernatePerceptron ){//received from naive load balancer
									//disconnect and return to the base.
									//perceptron.setHibernated(true);
									//break;//this does not work, wait for the version stop iteration.
								}
							}
							/*Returns true if it wants to loop
							 *the dataflow should have a stream of inputs
							 *and a stream of output.*/
							/**
							 * We may want to advance the pointer before we check for hibernate status.  this is 
							 * because the dependencies will hibernate after the ith iteration.
							 * If we don't advance the counter here.  the hibernation algorith in the next if
							 * would allow action for one more iteration ith+1 which will reestablish the line.
							 * 
							 */
							perceptron.setCycleVersion( perceptron.getCycleVersion() + 1);
							//w.write("version" + perceptron.getCycleVersion() 
							//		+ ":D:"+ perceptron.DbgGetCurrVersion() + "\n");
							/**
							 * if I am hibernating, I want to make sure I don't step into another's dataflow cycle.
							 * In response, I may delay my hibernation calls, or if time allows, I can join
							 * the version dateline inposed by the neighbor dataflow.
							 * 
							 * However, there can still be the posibility that the neighbor issues the call at the 
							 * same time as I do.  to adjust to that, I will re-issue the hibernate call to match
							 * the neighbor's version if that version is higher.  Note that this won't keep happening
							 * because at some point, the dataflows will escape that instant in time, and they would
							 * hit the firs ifs, the ones that check for the neighbors status.  using both algorithms
							 * prevents the echos of hibernation from growing too big.
							 * 
							 * so the intant problem is solve by reissuing the call. and the instant problem of 
							 * having a dataflow extend the version dateline just as a neighbor dataflow is going
							 * off-line is soved by this algorithm hre.
							 */
							if( perceptron.isHibernated() ){
								--hibernate_wait_until_version;
								if(  hibernate_wait_until_version <= 0 ){
									if( version_stop == -1){
										long time_lapsed = System.currentTimeMillis() - start;
										long versions_done = perceptron.getCycleVersion() - version_measure_start;
										//slot size determines the time slot we are using to consider a hibernation action
										//500 milliseconds should be enough
										long slot_size = 200L; //200 msec slots (very high latency (200)
											//the slot size could be modified depending on the communicatin medium.
											//shared, cluster, or Internet.  This affects delay, which affects the
											//time slot used to calculate the prudent disengage version.
										double time_lapsed_slots = (double)time_lapsed / (double) slot_size;
										
										//the 2 is to prevent it from being zero, mostly needed during testing and debuggin
										//should have no effect on experiments with thousands of cycles.
										//if(perceptron.State.getInputs() != null){
										//	for( int k = 0 ; k < perceptron.State.getInputs().length ){
										//		perceptron.State.getInputs()[k].
										//	}
										//}
										//TODO
										//THE TO LINES BELOW IS THE REAL THING, THEY ARE REPLACE TEMPORALY FOR DEBUGGING
										long versions_per_time_slots_lapsed = (long)(versions_done / time_lapsed_slots) + 
												CommunicationConstants.QUEUE_SIZE*2;
												//QUEUE_SIZE * 2 to take into account my queue buffer and the neighbor's
										//TODO
										//long versions_per_time_slots_lapsed = 2; //when debugging
										//TODO
										//adding packets that are on the way
										//times two to be conservative
										
										//get the neighbor hibernate that is closest to happen.
										long neighbor_stop_version = getClosestVersionStop(perceptron);
										
										/**
										 * Note September 21 2010.  The main problem with the old code (saved on cvs) 
										 * is that it will become hard to synchronize the nodes to stop at a certain 
										 * version once there are many nodes.  One form to resolve this is tro centralize
										 * the decistion process to the dataflow loader, however that idea is very different
										 * from what it is already implemented.
										 * 
										 * The other idea, that should be implemented here next (after sept 21), is to create
										 * steps so that if a hibernation request is requested during that step, the version_stop
										 * is moved to the roof of that step.  Say we have 1000 iteration steps and a 
										 * hibernation request is received at iteration 500, the version stop for that would be
										 * 1000+(prudent version(PV) ) compute.  Prudent version is the number of cycle we should wait
										 * to get the nearest neighbors organized.  if it is 1001, the version stop would be 
										 * 2000+PV.  Now, if a node wants to hibernate at 998, and there are other nodes already 
										 * scheduled to hibernate by 1000+PV, the current algorithm should be able to relay to 
										 * that node that there are hiberantaion processes already in the list, at that point, 
										 * as is done in the current algorithm, the node can either wait for the next step, or 
										 * it can joing the group "Joing the party".
										 * 
										 * The only new feature is including the step to reduce out-of-step hibernation in really
										 * small time windows.
										 */
										
										if( neighbor_stop_version != -1){
											//if neighbor stop version is between current version + 2 my_stop_version and 3 * my_stop_version
											//then lets join the party
											long low_end = getVersionStep(perceptron.getCycleVersion() + versions_per_time_slots_lapsed * 2 );
											
											if( neighbor_stop_version > low_end   ){
												
												version_stop = neighbor_stop_version;
												
												System.out.println(Thread.currentThread().getName() 
														+ "time_lapse_slots: " + time_lapsed_slots
														+ " versions_per_time_slots_lapsed " + versions_per_time_slots_lapsed
														+ ": DataflowLoaderTemplate:ClidentSide() Prudent version :" 
														+ version_stop + " curr_version: " + perceptron.getCycleVersion()
														+ " step: " + VERSION_STOP_STEP_SIZE
														+ " JOINING THE PARTY ");
												
												for( int i =0 ; i < perceptron.getOutputs().length ;i++){
													if( perceptron.getOutputs()[i] != null){
														perceptron.getOutputs()[i].hibernateConnection(version_stop);
													}
												}
												for( int i =0 ; i < perceptron.getInputs().length ;i++){
													if( perceptron.getInputs()[i] !=null){
														perceptron.getInputs()[i].hibernateConnection(version_stop);
													}
												}
												Engine.stopEngine();  //we are hibernating close the potential for other nodes to connect to me.
											}else{
												version_stop = -1;
												// wait one time step plus some verstion_slots to make sure the timing is not ruined
												//TODO, all these paremeters can be optimized.
												//TODO Oct 11, check the line below with a test.  this
												//bug was found while writing the technical report.
												hibernate_wait_until_version = 
													perceptron.getCycleVersion() + 
													VERSION_STOP_STEP_SIZE + (versions_per_time_slots_lapsed * 5);
												System.out.println(Thread.currentThread().getName() 
														+ "hibernate_wait_until_verson : " + hibernate_wait_until_version  
														+ "neighbor: " + neighbor_stop_version
														+ " my_version: " + perceptron.getCycleVersion()
														+ " step: " + VERSION_STOP_STEP_SIZE
												) ;
											}
											//else, wait x5 my_stop_version before issueing my hybernate stop
										}else{
											//no neighbor has eminent hibernation.
											version_stop = getVersionStep( perceptron.getCycleVersion() + versions_per_time_slots_lapsed * 3 );
											System.out.println(Thread.currentThread().getName() 
													+ "time_lapse_slots: " + time_lapsed_slots
													+ " versions_per_time_slots_lapsed " + versions_per_time_slots_lapsed
													+ ": DataflowLoaderTemplate:ClidentSide() Prudent version :" 
													+ version_stop + " curr_version: " + perceptron.getCycleVersion()
													+ " step: " + VERSION_STOP_STEP_SIZE
													);
											for( int i =0 ; i < perceptron.getOutputs().length ;i++){
												if( perceptron.getOutputs()[i] != null){
													perceptron.getOutputs()[i].hibernateConnection(version_stop);
												}
											}
											for( int i =0 ; i < perceptron.getInputs().length ;i++){
												if( perceptron.getInputs()[i] !=null){
													perceptron.getInputs()[i].hibernateConnection(version_stop);
												}
											}
											Engine.stopEngine();  //we are hibernating close the potential for other nodes to connect to me.
										}
										/**
										 * New code.  The next challenge is to handle interleaving calls to hibernate among different
										 * nodes.  if A, and B decide independently to hybernate themsleves to split into more
										 * nodes, an algorithm should make sure that does not crash the network of dataflows.
										 * This is what we are going to do.
										 * 
										 * First, the existing code works out beautiful if all nodes hibernate at the same version
										 * So to increases the likelyhood of that happening, the hibernation will happen at multiples 
										 * of 10 or 100 or some other constant
										 * 
										 * However, there will be times that a call from a load balancer will make two nodes hibernate
										 * at very close hibernation version.   If this is the case, the second third and so forth
										 * node to notice that its neighbors are about to hibernate, should wait 5x the version stop
										 * amount before issuing the hibernate call. 
										 * 
										 */
										
									
										//TODO loop throught list to get all connections ready to hibernate.
										//perceptron.State.getOutputs()[0].hibernateConnection(version_stop);
										//perceptron.State.getInputs()[0].hibernateConnection(version_stop);
									}else{
										long neighbor_version = getFarthestVersionStop(perceptron);
										//the ifs above prevent this node from steping onto anothers dataflows hibernation cycle
										//this next check is for the case where two or more datflows step on eachoder cycles
										//because they issued the calls at the same time.
										if( version_stop < neighbor_version){
											//re-send hibernate call
											System.out.println(Thread.currentThread().getName() 
													+ " DataflowLoaderTemplate:ClientSide(): " 
													+ " resending the hibernation call " 
													+  " neighbor_version: " + neighbor_version 
													+ " stop_cycle is " + version_stop );
											version_stop = neighbor_version;
											for( int i =0 ; i < perceptron.getOutputs().length ;i++){
												perceptron.getOutputs()[i].hibernateConnection(version_stop);
											}
											for( int i =0 ; i < perceptron.getInputs().length ;i++){
												perceptron.getInputs()[i].hibernateConnection(version_stop);
											}
										}
										if( perceptron.getCycleVersion() == version_stop ){
											System.out.println(Thread.currentThread().getName() 
													+ " DataflowLoaderTemplate:ClientSide(): " 
													+ "stopping at cycle " 
													+ perceptron.getCycleVersion()
													+ " stop_cycle is " + version_stop 
													+ " comm lines status " + printCommLineStatus( perceptron ) );
											perceptron.DbgStoreHibernationVersions.add(perceptron.getCycleVersion());
											break;
											
										}
										/*for debuggin only.  This should help mee see the state of the peceptrons one iteration
										 * before hibernation.*/
										if( perceptron.getCycleVersion() + 1 == version_stop){
											Node.getLog().log(Level.FINEST, Thread.currentThread().getName() 
													+ " DataflowLoaderTemplate:ClientSide(): " 
													+ " one iteration before stoping. curr version: " 
													+ perceptron.getCycleVersion()
													+ " stop_cycle is " + version_stop 
													+ " comm lines status " + printCommLineStatus( perceptron ) );
										}
									}
								}
							}
							
						}//end while
						//close engine as soon as possible to prevent other nodes from establishing with 
						//the wrong engine.
						//w.close();
						
						Engine.close(); //TODO close the engine as soon as we know we are going into hibernation
									//this could prevent the faster processes from connecting to the processes that are
									//slower and take longer to hibernate.  When this occurs it deadlocks the app.
						
						if( !cycle_again && perceptron.getControl() == Types.DataControl.SPLIT){
							//if we are done, make sure it is not a split.
							
							perceptron.setControl(Types.DataControl.INSTRUCTION_DATA);
							perceptron.setHibernated(false);
							//*** Thoughts about recursive spliting and what is left to implement. ***
							//The Instruction_data works only for the first split.  I'll need an integer to keep
							//track of the number of splits.  when a dataflow is returns to the matrix, the 
							//split number would be brought down to zero. So, while the perceptron is out in 
							//computation spaces, it ca split multiple time (future implementation).  Each
							//split adds one to the split variable.  the control would be used to tell of the 
							//next step in the sequence, but the natural state of the perceptron would be 
							//INSTRUCTION-DATA.  At the moment, I am setting a split perceptron to COALESCE
							//so that it is put back together when the application finishes.
						}
						m_manager.SendObject(perceptron);
						
						Log(Thread.currentThread().getId() + " Done running work unit  perceptron.State.SegID.toString()" );
						
						// skip this shutdown if we are to split
						//the hibernation procedure would have done this already.
						Thread.sleep(500);
						
						if( !perceptron.isHibernated()){
							pattern_done = true;
							bringDownConnections( perceptron);
						}else{//the if statement is no longer needed, take out when it is confirmed the  
							//change does not causes a new bug
							pattern_done = false;
							bringDownConnections( perceptron);
							//bringDownConnectionsSkipRecv( perceptron);
						}
						/**
						 * The receive thread should be fine, because we would not be at this point had it not
						 * been because all the received data has been exhausted and we are ready to quit.
						 * However, the timing is different for the sent data.  The node closed with some
						 * data left in the send queue.  the next method makes sure we don't have send data
						 * when we close the node.
						 */
						System.out.println("DataflowLoaderTemplate:ClientSide() s:" + perceptron.getSegID().toString() +":v:" 
								+ perceptron.getCycleVersion() );
					}//else{
						//m_manager.SendObject( perceptron );
					//}
					
					
					
					Node.getLog().log(Level.FINER, Thread.currentThread().getName() + "************* " +
							" pattern_done " + pattern_done + "perceptron id:" + perceptron.getSegment()
							+ "************");
					
				} catch (InterruptedException e) {
					e.printStackTrace();
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
				} catch (IOException e) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
				}
				
				while( m_manager.hasSendData() ){
					try{
						Node.getLog().log(Level.FINEST, " wait for send data" );	
						Thread.sleep(100);
					}catch( InterruptedException e){
						e.printStackTrace();
					}
				}
				
				
				
				m_manager.close();
				//set the dataflow template to done, so that the emitting application can 
				//continue to run ( if it was waiting on this thread)
				loader.getUserMod().setDone(true);
				
			
				
				Log("GOING BACK TO IDLING " );
		} catch (IOException e1) {
			//Node.getLog().log(Level.SEVERE, "AA "+Node.getStringFromErrorStack(e1));
			Node.getLog().log(Level.INFO, "IOException, this could also be a node trying to connect to a dead pattern.");
		} catch (ClassNotFoundException e1) {
			Node.getLog().log(Level.SEVERE, "BB "+Node.getStringFromErrorStack(e1));
		} catch (InterruptedException e1) {
			Node.getLog().log(Level.SEVERE, "CC "+Node.getStringFromErrorStack(e1));
		} catch (TunnelNotAvailableException e1) {
			Node.getLog().log(Level.SEVERE, "DD " +Node.getStringFromErrorStack(e1));
		} catch (CommunicationLinkTimeoutException e) {
			Node.getLog().log(Level.SEVERE, "EE " +Node.getStringFromErrorStack(e));
		} catch (Exception e) {
			System.out.println(" I cannot work  in these conditions");
			e.printStackTrace();
			
		}
		
		return pattern_done;
	}
	/**
	 * returns the next version step.  
	 * This is used to hibernate a perceptron.
	 * @param version
	 * @return
	 */
	public static long getVersionStep( long version){
		long step = (version / VERSION_STOP_STEP_SIZE ) + 1; //get current step, then add one to get next step
		return step * VERSION_STOP_STEP_SIZE;
	}

	/**
	 * Counts the dataflows managed by this class that have not been taken back by the gather thread.
	 * 
	 */
	int OutstandingDataflows;
	Boolean DataflowDone;
	private ArrayBlockingQueue<Dataflow> PerceptronLauncherQueue;
	Map<String, Boolean> NodesWithDataUnit;
	public synchronized int getOutstandingDataflow(){
		return OutstandingDataflows;
	}
	public synchronized void advanceOutstandingDataflows(){
		++OutstandingDataflows;
	}
	public synchronized void reduceOutstandingDataflows(){
		--OutstandingDataflows;
	}
	public synchronized Boolean isDataflowDone(){
		return DataflowDone;
	}
	public synchronized void setDataflowDone(Boolean set){
		DataflowDone = set;
	}
	@Override
	public void ServerSide(PipeID pattern_id) {
		/**this method also has to call the sink-source from the pattern template
		 * 
		 */
		Thread.currentThread().setName("DataflowLoaderThread");
		try{
			DataflowLoader DPattern = (DataflowLoader) this.UserModule;
			/*This thread will listen for more workers that want to joing the job
			 * at any time during the computation. */
			List<ConnectionManager> lst = new ArrayList<ConnectionManager>();
			MultiModePipeDispatcher m_dispatcher = new MultiModePipeDispatcher(Network, Thread.currentThread().getName(), -4, lst, pattern_id, null);
			/**
			 * All the exception within the next try-catch statement get sent to the user.
			 * We assume this exceptions are caused by the user's application, and therefore
			 * will be useful to troubleshoot the program.
			 */
			//try{
			//Log("Hello from DataSourceSink " + Node.PID);
			int segment = 0;//SEGMENT_OFFSET;				//segment: data chunk from the user's app
			long jobs_completed = 0;		//segments that have being processed and returned
			//send data and process loop
			Log( "jobs_completed " + jobs_completed + " getMinimumCPUCount(): " + DPattern.getMinimunCPUCount() );
																		//change to int so we know what segment it has
			NodesWithDataUnit = Collections.synchronizedMap(new HashMap<String, Boolean>());
			
			PerceptronLauncherQueue = new ArrayBlockingQueue<Dataflow>(100);//100 max new perceptrons
			
			OutstandingDataflows = 0;
		
			/*
			 * Distribute perceptron  units
			 */
			//int ds = DPattern.getMinimunCPUCount();
			while( segment  < DPattern.getMinimunCPUCount() && !ShutdownOrder){
				//only allow additions to the list at the end of each cycle
				synchronized( lst ){
					for( ConnectionManager manager : lst ){
						//only one data unit per node
						if( NodesWithDataUnit.get(manager.getRemoteNodeConnectionPipeID()) == null){
							//ClientSocketManager manager = it.next();
							//if there are exception, let the Observer know about it and skip that manager
							//if the manager is not too busy, send another piece of work
							
							if( segment >= DPattern.getMinimunCPUCount() ){ //if we are out of work units break
								break;
							}	
							if( manager.isBound() ){
								//get segment from the user's application
								try{
									
									Dataflow perceptron = DPattern.onLoadPerceptron(segment);//.DiffuseDataUnit(segment);
									if( perceptron == null){
										throw new DataflowPerceptronNullException();
									}
									//input.setInstruction(Types.DataInstruction.DATACONTAINER);
									perceptron.setSegment(segment);
									
									perceptron.setControl(Types.DataControl.INSTRUCTION_DATA);  //eliminate this.
									//send it
									
									manager.SendObject(perceptron);
									perceptron = null;
									Log( " Sending segment : " + segment );
									System.out.println( "segment" + segment + " id " + manager.getRemoteNodeConnectionPipeID().toString());
									NodesWithDataUnit.put(manager.getRemoteNodeConnectionPipeID(), true);
									++segment;
									advanceOutstandingDataflows();
									
									
								}catch( InterruptedException e){
									Node.getLog().log(Level.FINE, "Interrupted");
								}catch( Exception e){
									Node.getLog().log(Level.FINE, Node.getStringFromErrorStack(e));
									//RemoteLogger.printToObserver("When working on template's DiffuseData() : " + Node.getStringFromErrorStack(e));
								}
							}
						}
					}
				}
				
				try {
					if( segment < DPattern.getMinimunCPUCount() ){//.getDataUnitCount() ){
						Node.getLog().log(Level.FINE, "wait while more nodes connect to me.  Segment so far: " + segment + " Exptected total: " + DPattern.getMinimunCPUCount());
						//republish patern advertisement
						DiscoveryService service = this.getNetwork().getNetPeerGroup().getDiscoveryService();
						service.remotePublish( this.getNetwork().getCurrentPatternAdvert());
						
						//if we still need to wait for more nodes to come online
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//HostedSourceSinkThread source_sink = null;
			RunSinkSourceDataflow source = null;
			RunSinkSourceDataflow sink = null;
			if( DPattern.instantiateSourceSink() ){
				try {
				    /**
					 * this thread should be active for workpools and pipelines.
					 */
						
					Engine  = new DependencyEngine(getNetwork(), Dataflow.DATAFLOW_SOURCE , pattern_id 
							, DPattern.getUserMod().getRawByteEncoder());			
			
					Engine.startServer();
					
					Dataflow source_dataflow = DPattern.onLoadPerceptron(Dataflow.DATAFLOW_SOURCE);
					source_dataflow.setUserModule(DPattern.getUserMod());
					source = new RunSinkSourceDataflow(source_dataflow, Engine, pattern_id , Dataflow.DATAFLOW_SOURCE);
					source.start();
					
					Dataflow sink_dataflow = DPattern.onLoadPerceptron(Dataflow.DATAFLOW_SINK);
					sink_dataflow.setUserModule(DPattern.getUserMod());
					sink = new RunSinkSourceDataflow(sink_dataflow, Engine, pattern_id , Dataflow.DATAFLOW_SINK);
					sink.start();
					
					//Engine.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (NoPortAvailableToOpenException e) {
					e.printStackTrace();
				}
			}
			/**
			 * start gather and recover thread.
			 */
			DataflowGatherThread gather = new DataflowGatherThread(lst, this );
			gather.start();
			
			/**
			 * Start loop to accept and dispatch splitted dataflow perceptrons
			 * 
			 */
			
			while(true){//the loop checks the queue of dataflow peceptrons.  if there is a new perceptron, it will
				//deploy it
					synchronized( lst){
						boolean delete = false;
						for( ConnectionManager manager : lst){
							//only one data unit per node
							if( NodesWithDataUnit.get(manager.getRemoteNodeConnectionPipeID()) == null){
								//if there are exception, let the Observer know about it and skip that manager
								//if the manager is not too busy, send another piece of work
								if( manager.isBound() ){
									//String str = manager.getRemoteNodeConnectionPipeID();
									Dataflow splt = this.getPerceptronLauncherQueue().poll(2L, TimeUnit.SECONDS);
									if( splt != null){
										manager.SendObject(splt);
										Node.getLog().log(Level.FINER, " DataflowLoaderTemplate:ServerSide() segment" 
												+ splt.getSegment() + " id " 
												+ manager.getRemoteNodeConnectionPipeID().toString());
										NodesWithDataUnit.put(manager.getRemoteNodeConnectionPipeID(), true);
										this.advanceOutstandingDataflows();
									//get segment from the user's application
								/*	*/
								}else{
									delete = true;
									break;
								}
							}
						}
						if( delete ){
							lst.remove(manager);//if not bound, take it out, we don't need it.
						}
					}
				}
				Thread.sleep(100);
				if( this.isDataflowDone() ){
					break;
				}
			}
			
			/*begging the shutdown.*/
			m_dispatcher.StopListeningForNewConnections();
			/**
			 * loop to send no more work for you here is needed
			 */
			synchronized( lst){
				for( ConnectionManager manager : lst){
					//only one data unit per node
					if( NodesWithDataUnit.get(manager.getRemoteNodeConnectionPipeID()) == null){
						try{
							NoMoreDataflowPerceptrons done = new NoMoreDataflowPerceptrons();
							done.setControl(Types.DataControl.NO_WORK_FOR_YOU);		
							manager.SendObject(done);
							Log( " Sending no more work unit:" + segment );
						}catch( InterruptedException e){
							Node.getLog().log(Level.FINE, "Interrupted");
						}catch( Exception e){
							Node.getLog().log(Level.FINE,"When working on template's DiffuseData() : " + Node.getStringFromErrorStack(e));
						}
					}
				}
			}
			
			if( DPattern.instantiateSourceSink() ){
				source.join();
				sink.join();
				/**
				 * Sept 20, 2010
				 * Make sure the Engine is off only once the source and sink are
				 * turn off.  This allows the thread to reconnect if one or more
				 * of the connection goes into hibernation.
				 * 
				 */
				Engine.close();
			}
			
			gather.join();
			
			DPattern.setDone(true);
			DPattern.getUserMod().setDone(true);
			Log( "Data Sink Source is shuting down now ... ");
			m_dispatcher.close();
			Log( "Going back to idleling");
		}catch(IOException e ){
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		}catch (ClassNotFoundException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (NoPortAvailableToOpenException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		}catch (InterruptedException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		}catch( Exception e ){
			e.printStackTrace();
		}
	}
	
	@Override
	public String getSuportedInterface() {
		return PatternLoader.class.getName();  
	}
	public void Log( String str ){
		//RoutineAdvertPublisherQuerier.addtoDebugErrorMessage(str);
		//RemoteLogger.printToObserver(str);
		Node.getLog().log(Level.FINEST, "\n" + str);
	}
	@Override
	public void FinalizeObject() {
		// TODO Auto-generated method stub
		
	}
	public synchronized ArrayBlockingQueue<Dataflow> getPerceptronLauncherQueue() {
		return PerceptronLauncherQueue;
	}
	public synchronized void setPerceptronLauncherQueue(ArrayBlockingQueue<Dataflow> perceptronLauncherQueue) {
		PerceptronLauncherQueue = perceptronLauncherQueue;
	}
	
}
