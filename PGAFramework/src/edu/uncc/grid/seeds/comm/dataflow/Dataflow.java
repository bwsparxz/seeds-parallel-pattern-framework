package edu.uncc.grid.seeds.comm.dataflow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.comm.dependency.Dependency;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;
import edu.uncc.grid.seeds.comm.dependency.SplitCoalesceHandler;

/**
 * The Dataflow class is an interface used by the advance user to create 
 * patterns and skeletons.  The Dataflow as designed in this framework will help 
 * the advanced user provide automatic scalability and automatic grain size to the
 * basic user. 
 * Oct 21: moved dependencies to this class to leave the StateFull Object 
 * to the basic programmer.
 * @author jfvillal
 *
 */
public abstract class Dataflow  implements Data, Comparable<Dataflow>{
	/**
	 * Special arbitrary negative number used to send the dataflow object to the 
	 * source node.  positive numbers are left for worker nodes (COMPUTE).
	 */
	public static final int DATAFLOW_SOURCE = -12;
	/**
	 * Special arbitrary negative number used to send a dataflow object to the
	 * sink node.  positive numbers are left for worker nodes (COMPUTE)
	 */
	public static final int DATAFLOW_SINK = -13;
	/**
	 * This identifies the hierarchical ide of the statefull data (if any) managed 
	 * by this dataflow.
	 */
	protected HierarchicalSegmentID SegID;
	/**
	 * InputIDs defines the hierarchical id's for the input connections to be managed
	 * by this dataflow object.  The object is used by the framework to create the 
	 * Dependency that are used to manage the actual network or shared memory connection.
	 */
	private HierarchicalDependencyID[] InputIDs = null;
	/**
	 * OutputIDs defines the hierarchical id's for the output connections to be 
	 * managed by this object.  This are data structures in list form, that travel
	 * a path down a tree of dependencies.  This object is used by the framework to
	 * create the Dependency object that do the actual management of the connections.
	 */
	private HierarchicalDependencyID[] OutputIDs = null;
	/**
	 * Like the MPI-Like pattern prototype, this prototype needs the dataflow to 
	 * be identified as COMPUTE, SOURCE, or SINK.
	 */
	protected Types.DataFlowRole mDataflowRoll;
	/**
	 * Determines if the dataflow is in hibernation status.  The hibernation status
	 * is used to signal the dataflow either needs to be split, or it needs to be
	 * coalesced.
	 */
	private boolean Hibernated;

	/**
	 * CycleVersion is used to keep the cycles synchronized.  This in turn is used to 
	 * synchronize hibernation procedures.
	 */
	private long CycleVersion;
	//TRANSICENTS ARE NEXT 
	/**
	 * Inputs manages the connection dependencies from this dataflow.  the Object will
	 * not transfer over the network since the core datastructure would need to be 
	 * recreated at the new host. 
	 */	
	transient protected Dependency[] Inputs = null;
	/**
	 * Output manages the connection dependencies from this dataflow.  the Object will
	 * not transfer over the network since the core datastructure would need to be 
	 * recreated at the new host. 
	 */
	transient protected Dependency[] Outputs = null;
	/**
	 * This is used to set the version at which point the dataflow should be 
	 * disengaged and return to the dataflow master.
	 */
	transient public long CycleVersionStop;
	/**
	 * Has the user module.  To prevent the user module to be inadvertenly shared by threads 
	 * on the same computer, this Object is left transient.  That means the object is assigned
	 * upon arrival to the remote node.
	 */
	transient private BasicLayerInterface UserModule;
	
	public Dataflow(){
		Inputs = null;
		Outputs = null;
		UserModule = null;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Serializable State;
	int Segment;
	/**
	 * 
	 * @param state_data
	 * @return returns false if the function does not need looping.
	 */
	public abstract boolean computeOneCycle( );
	
	/**
	 * Returns the Serializable used to store the basic programmers user's state information.
	 * The user is advised not to use global variables, sine the skeleton is executed on  multiple nodes that don't 
	 * share memory.  To still provide this service in a reliable manner, the State object is just a Serializable
	 * where the basic programmer can store "global" that that will behave predictably as global or class variables
	 * would.
	 * @return
	 */
	public Serializable getState() {
		return State;
	}
	/**
	 * Returns whether the dataflow has been set to hibernate.
	 * @return
	 */
	public boolean isHibernated(){
		return Hibernated;
	}
	/**
	 * Sets the Hibernated variable.
	 * @param set
	 */
	public void setHibernated(boolean set){
		Hibernated = set;
	}
	/**
	 * Sets the statefull object to be used by thid dataflow object.
	 * @param state
	 */
	public void setState(Serializable state) {
		this.State = state;
	}
	/**
	 * Returns the dataflow loader instance.
	 * @return
	 */
	public abstract DataflowLoader getDataflowLoaderInstance();
	/**
	 * Returns a new instance of the dataflow class.  This is for the advance user to 
	 * return the proper new class based on the extended class that was created to make 
	 * a new pattern.
	 * @return
	 */
	public abstract Dataflow getNewInstance();
	
	/**
	 * Returns the Segment <br> <br>
	 * Segment is used to identify this dataflow from other dataflows at the framework level.
	 * contrary to the hierarchical segment, this segment will change if the dataflow is 
	 * split, or redeployed, where the hierarchical segment stays the same on redeployment.
	 */
	public int getSegment(){
		return Segment;
	}
	/**
	 * Sets the Segment <br> <br>
	 * Segment is used to identify this dataflow from other dataflows at the framework level.
	 * contrary to the hierarchical segment, this segment will change if the dataflow is 
	 * split, or redeployed, where the hierarchical segment stays the same on redeployment.
	 */
	public void setSegment( int set ){
		Segment = set;
	}
	/**
	 * Returns the user's module.
	 * @return
	 */
	public BasicLayerInterface getUserModule() {
		return UserModule;
	}
	/**
	 * Sets the user's module.
	 * @param userModule
	 */
	public void setUserModule(BasicLayerInterface userModule) {
		UserModule = userModule;
		
	}
	/**
	 * Will be called so the Advance programmer can split the dataflow into 
	 * more dataflows.
	 * @param level
	 * @return
	 */
	public abstract List<Dataflow> onGrainSizeSplit(int level);
	/**
	 * Used to put together two sibling dataflows.
	 * @param perceptrons
	 * @param level
	 * @return
	 */
	public abstract Dataflow onGrainSizeCoalesce( List<Dataflow> perceptrons ,  int level);
	/**
	 * Sets the input dependency ids.
	 * @param dept_list
	 */
	public void setInputDependencyIDs(HierarchicalDependencyID[] dept_list){
		InputIDs = dept_list;
	}
	/**
	 * Sets the output dependency ids.
	 * @param dept_list
	 */
	public void setOutputDependencyIDs(HierarchicalDependencyID[] dept_list){
		OutputIDs = dept_list;
	}
	/**
	 * reuturn the input dependency ids.
	 * @return
	 */
	public HierarchicalDependencyID[] getInputDependencyIDs(){
		return InputIDs;
	}
	/**
	 * returns the output dependency id's.
	 * @return
	 */
	public HierarchicalDependencyID[] getOutputDependencyIDs(){
		return OutputIDs;
	}
	/**
	 * 
	 * returns the Output dependency list used by this data bucket 
	 * @return
	 */
	public Dependency[] getInputs() {
		return Inputs;
	}

	public abstract SplitCoalesceHandler getSplitCoalesceHander();
	/**
	 * Sets the Input dependency.  the index on the array correspond to the index 
	 * on the Dependency ID list
	 * @param inputs
	 */
	public void setInputs(Dependency[] inputs) {
		Inputs = inputs;
	}
	/**
	 * returns the Output dependency list used by this data bucket
	 * @return
	 */
	public Dependency[] getOutputs() {
		return Outputs;
	}
	/**
	 * Sets the Dependency.  the index on the array correspond to the index on 
	 * the Dependency ID list
	 * @param outputs
	 */
	public void setOutputs(Dependency[] outputs) {
		Outputs = outputs;
	}
	/**
	 * Returns the cycle version, used to count the number of compute cycle and
	 * to synchronized the processes.
	 * @return
	 */
	public synchronized long getCycleVersion() {
		return CycleVersion;
	}
	/**
	 * Sets the cycle version.
	 * @param cycleVersion
	 */
	public synchronized void setCycleVersion(long cycleVersion) {
		CycleVersion = cycleVersion;
	}
	/**
	 * Returns the cycle version stop used to synchronize the stop version 
	 * for dataflow.
	 * @return
	 */
	public synchronized long getCycleVersionStop() {
		return CycleVersionStop;
	}
	/**
	 * Sets the cycle version stop.
	 * @param cycleVersionStop
	 */
	public synchronized void setCycleVersionStop(long cycleVersionStop) {
		CycleVersionStop = cycleVersionStop;
	}
	/**
	 * Returns the segment id.
	 * @return
	 */
	public synchronized HierarchicalSegmentID getSegID() {
		return SegID;
	}
	/**
	 * Sets the segment id.
	 * @param segID
	 */
	public synchronized void setSegID(HierarchicalSegmentID segID) {
		SegID = segID;
	}
	/**
	 * Debug method to return input dependencies are strings.
	 * @return
	 */
	public String inputsAsString(){
		if(InputIDs != null){
			String ans = "";
			for( int i = 0; i < InputIDs.length ; i++){
				ans += InputIDs[i] + " ";
			}
			return ans;
		}
		return null;
	}
	/**
	 * Debug method to return output dependencies as strings.
	 * @return
	 */
	public String outputsAsString(){
		if( OutputIDs != null){
			String ans = "";
			for( int i = 0 ; i < OutputIDs.length; i++){
				ans += OutputIDs[i] + " "; 
			}
			return ans;
		}
		return null;
	}
	/**
	 * Returns the dataflow roll.
	 * @return
	 */
	public Types.DataFlowRole getDataFlowRoll() {
		return mDataflowRoll;
	}
	/**
	 *  Sets the roll  
	 * @param dataFlow
	 */
	public void setDataFlowRoll(Types.DataFlowRole dataFlow) {
		mDataflowRoll = dataFlow;
	}
	@Override
	public int compareTo(Dataflow o) {
		return this.SegID.compareTo(o.getSegID() );
	}
	/**
	 * Used to more accurately estimate ideal scalability.
	 */
	public List<Long> DbgStoreHibernationVersions = new ArrayList<Long>();
	public abstract String DbgGetCurrVersion();
	
}
