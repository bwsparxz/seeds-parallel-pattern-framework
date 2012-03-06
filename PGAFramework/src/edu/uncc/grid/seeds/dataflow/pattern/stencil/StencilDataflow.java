package edu.uncc.grid.seeds.dataflow.pattern.stencil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
import edu.uncc.grid.seeds.comm.dependency.CycleVersionMissmatch;
import edu.uncc.grid.seeds.comm.dependency.EngineClosedException;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalSegmentID;
import edu.uncc.grid.seeds.comm.dependency.SplitCoalesceHandler;


public class StencilDataflow extends Dataflow implements  SplitCoalesceHandler{
	public int CellSegment;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long benchmark_start;
	@Override
	public boolean computeOneCycle() {
		Stencil user_mod = (Stencil)this.getUserModule();
		
		StencilData LocalData = (StencilData) this.getState();
		boolean repeat = false;
		try {
			// send out to
			//top
			{	
				if( this.Outputs[Stencil.TOP] != null){
					Serializable packet_top = LocalData.getTop();
					this.Outputs[Stencil.TOP].sendObj( packet_top );
				}
				//right
				if( this.Outputs[Stencil.RIGHT] != null){
					Serializable packet_right = LocalData.getRight();
					this.Outputs[Stencil.RIGHT].sendObj( packet_right );
				}
				//bottom
				if( this.Outputs[Stencil.BOTTOM] != null){
					Serializable packet_bottom = LocalData.getBottom();
					this.Outputs[Stencil.BOTTOM].sendObj( packet_bottom );
				}
				//left
				if( this.Outputs[Stencil.LEFT] != null){
					Serializable packet_left = LocalData.getLeft();
					this.Outputs[Stencil.LEFT].sendObj( packet_left );
				}
			}
			//take input
			{
				//top
				if(this.Inputs[Stencil.TOP] != null){
					LocalData.setTop( this.Inputs[Stencil.TOP].takeRecvObj());
				}
				//right
				if( this.Inputs[Stencil.RIGHT] != null){
					LocalData.setRight( this.Inputs[Stencil.RIGHT].takeRecvObj() );
				}
				//bottom
				if( this.Inputs[Stencil.BOTTOM] != null){
					LocalData.setBottom(this.Inputs[Stencil.BOTTOM].takeRecvObj());
				}
				//left
				if( this.Inputs[Stencil.LEFT] != null){
					LocalData.setLeft(this.Inputs[Stencil.LEFT].takeRecvObj());
				}
			}
			
			
			//compute
			
			if( this.getCycleVersion() == 2000 && this.getSegment() == 9){
				benchmark_start = System.currentTimeMillis();
			}
			
			if( this.getCycleVersion() == 2500 && this.getSegment() == 9 ){
				FileWriter w = new FileWriter( "./five_hundred_cycles.txt", true);
				long time = System.currentTimeMillis() - benchmark_start;
				w.write("2000 to 2500 cycles:" + time + "\n");
				w.close();
			}
			
			repeat = user_mod.OneIterationCompute( LocalData );
			
			
			
			
			//hibernate !!
			/*if( //2
					this.getCycleVersion() == 500  //start hibernation on iteration 1000
				   // &&
				   // this.getSegment() == 0   // only for top left cell.
					//4	
					//|| this.getCycleVersion() == 2000
					//&& this.getSegment() == 9
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
					System.out.println("StencilDataflow:computeOneCycle ---> Split");
			}*/
		
			
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (EngineClosedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (CycleVersionMissmatch e) {
			e.printStackTrace();
		}
		return repeat;
	}

	

	@Override
	public DataflowLoader getDataflowLoaderInstance() {
		return new LoadStencil((Stencil)this.getUserModule());
	}

	@Override
	public Dataflow getNewInstance() {
		return new StencilDataflow();
	}

	@Override
	public List<Dataflow> onGrainSizeSplit(int level) {
		List<Dataflow> list = new ArrayList<Dataflow>();
		//create the new child hierarchical segments.
		HierarchicalSegmentID seg_one = new HierarchicalSegmentID(0, 2, this.getSegID() );
		HierarchicalSegmentID seg_two = new HierarchicalSegmentID(1, 2, this.getSegID() );
		
		//create the two new dataflows
		StencilDataflow one = new StencilDataflow();
		StencilDataflow two = new StencilDataflow();
		
		one.setSegID(seg_one);
		two.setSegID(seg_two);
		
		//create output dependencies for each dataflow
		HierarchicalDependencyID one_lst_out[] = new HierarchicalDependencyID[4];
		HierarchicalDependencyID two_lst_out[] = new HierarchicalDependencyID[4];
		//create inputs dependencies for each dataflow
		HierarchicalDependencyID one_lst_in[] = new HierarchicalDependencyID[4];
		HierarchicalDependencyID two_lst_in[] = new HierarchicalDependencyID[4];
		
		//outputs 
		//top and bottom
		//TODO setting dependency to null WON'T work for network seeds
		//the top dependency is given to one, the bottom dependency is givent to two
		//the top dependency for two is new, the bottom output for one is new as well
		if( this.getOutputDependencyIDs()[Stencil.TOP] != null){
			one_lst_out[Stencil.TOP] = this.getOutputDependencyIDs()[Stencil.TOP];
		}else{
			one_lst_out[Stencil.TOP] = null;
		}
		two_lst_out[Stencil.TOP] = new HierarchicalDependencyID(seg_two, 0, 1); 
		
		if( this.getOutputDependencyIDs()[Stencil.BOTTOM] != null){
			two_lst_out[Stencil.BOTTOM] = this.getOutputDependencyIDs()[Stencil.BOTTOM];
		}else{
			two_lst_out[Stencil.BOTTOM] = null;
		}
		one_lst_out[Stencil.BOTTOM] = new HierarchicalDependencyID(seg_one, 0, 1);
		
		//sides
		//the sides are split between one and two. one gets R/1:0/2 and two gets R/1:1/2
		//where L = 1 for this implementation
		if( this.getOutputDependencyIDs()[Stencil.RIGHT] != null){
																	/* | original segment because it is an existent dependency  */
																	/* |						| Existent dept as parent		*/
																    /* V						V								*/
			HierarchicalDependencyID right = this.getOutputDependencyIDs()[Stencil.RIGHT];
			one_lst_out[Stencil.RIGHT] = new HierarchicalDependencyID(right.getSid(), 0, 2, right );
			two_lst_out[Stencil.RIGHT] = new HierarchicalDependencyID(right.getSid(), 1, 2, right );
		}else{
			one_lst_out[Stencil.RIGHT] = null;
			two_lst_out[Stencil.RIGHT] = null;
		}
		if( this.getOutputDependencyIDs()[Stencil.LEFT] != null){
			HierarchicalDependencyID left = this.getOutputDependencyIDs()[Stencil.LEFT];
			one_lst_out[Stencil.LEFT] = new HierarchicalDependencyID(left.getSid() , 0, 2, left );
			two_lst_out[Stencil.LEFT] = new HierarchicalDependencyID(left.getSid() , 1, 2, left );
		}else{
			one_lst_out[Stencil.LEFT] = null;
			two_lst_out[Stencil.LEFT] = null;
		}
		one.setOutputDependencyIDs(one_lst_out);
		two.setOutputDependencyIDs(two_lst_out);
		one.CellSegment = this.CellSegment;
		two.CellSegment = this.CellSegment;
		//inputs
		//top and bottom 
		//one assumes same top  input as parent's top input
		if( this.getInputDependencyIDs()[Stencil.TOP] != null){
			one_lst_in[Stencil.TOP] = this.getInputDependencyIDs()[Stencil.TOP];
		}else{
			one_lst_in[Stencil.TOP] = null;
		}
		//two assumes as top input the bottom output from one
		two_lst_in[Stencil.TOP] = one_lst_out[Stencil.BOTTOM];
		
		//two assumes bottom input the same as parent bottom input
		if( this.getInputDependencyIDs()[Stencil.BOTTOM] != null){
			two_lst_in[Stencil.BOTTOM] = this.getInputDependencyIDs()[Stencil.BOTTOM];
		}else{
			two_lst_in[Stencil.BOTTOM] = null;
		}
		//one assumes as bottom input the top output from two.
		one_lst_in[Stencil.BOTTOM] = two_lst_out[Stencil.TOP];
		
		//sides
		if( this.getInputDependencyIDs()[Stencil.RIGHT] != null){
			//one and to take child input dependencies for left and right from parent
			//that is why we use the parents seg id and the old dept as parent
			HierarchicalDependencyID right = this.getInputDependencyIDs()[Stencil.RIGHT];
			one_lst_in[Stencil.RIGHT] = new HierarchicalDependencyID(right.getSid(), 0, 2, right );
			two_lst_in[Stencil.RIGHT] = new HierarchicalDependencyID(right.getSid(), 1, 2, right );
		}else{
			one_lst_in[Stencil.RIGHT] = null;
			two_lst_in[Stencil.RIGHT] = null;
		}
		if( this.getInputDependencyIDs()[Stencil.LEFT] != null){
			HierarchicalDependencyID left = this.getInputDependencyIDs()[Stencil.LEFT];
			one_lst_in[Stencil.LEFT] = new HierarchicalDependencyID(left.getSid(), 0, 2, left );
			two_lst_in[Stencil.LEFT] = new HierarchicalDependencyID(left.getSid(), 1, 2, left );
		}else{
			one_lst_in[Stencil.LEFT] = null;
			two_lst_in[Stencil.LEFT] = null;
		}
		one.setInputDependencyIDs(one_lst_in);
		two.setInputDependencyIDs(two_lst_in);
		
		Stencil user_mod = (Stencil)this.getUserModule();
		//get the split matrix from the user.
		Serializable[] data = user_mod.onSplitState((StencilData) this.getState(), 1);
		
		//set cycle
		one.setCycleVersion(this.getCycleVersion());
		two.setCycleVersion(this.getCycleVersion());
		
		//add matrix to the new dataflow perceptrons and return.
		one.setState(data[0]);
		two.setState(data[1]);
		list.add(one);
		list.add(two);
		return list;
	}

	@Override
	public Dataflow onGrainSizeCoalesce( List<Dataflow> perceptrons, int level) {
		StencilDataflow one = (StencilDataflow) perceptrons.get(0);
		StencilDataflow two = (StencilDataflow) perceptrons.get(1);
		
		StencilDataflow united = new StencilDataflow();
		Stencil user_mod = (Stencil)this.getUserModule();
		StencilData[] l = { (StencilData) one.getState(), (StencilData)two.getState()};
		StencilData unit = user_mod.onCoalesceState( l , 1);
		united.setState(unit);
		united.CellSegment = one.CellSegment;
		united.setCycleVersion( one.getCycleVersion() );
		//TODO the dependencies have to be united if this is to be used on dynamic estrching and 
		//shrinking configuration, for the upcoming test, this will not be needed.
		
		return united;
	}

	@Override
	public SplitCoalesceHandler getSplitCoalesceHander() {
		return this;
	}
	
	@Override
	public String DbgGetCurrVersion() {
		return null;
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
	public Serializable[] onSplit(Serializable packet) {
		Stencil user_mod = (Stencil)this.getUserModule();
		Serializable[] units = user_mod.splitData( packet );
		return units;
	}



	@Override
	public Serializable onCoalesce(Serializable[] pakets) {
		Stencil user_mod = (Stencil)this.getUserModule();
		Serializable unit = user_mod.coalesceData( pakets );
		return unit;
	}
}
