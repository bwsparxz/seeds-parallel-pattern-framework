package edu.uncc.grid.seeds.dataflow.pattern.alltoall;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.seeds.comm.dataflow.Dataflow;
import edu.uncc.grid.seeds.comm.dataflow.DataflowLoader;
import edu.uncc.grid.seeds.comm.dependency.CycleVersionMissmatch;
import edu.uncc.grid.seeds.comm.dependency.Dependency;
import edu.uncc.grid.seeds.comm.dependency.EngineClosedException;
import edu.uncc.grid.seeds.comm.dependency.SplitCoalesceHandler;
import edu.uncc.grid.seeds.dataflow.pattern.stencil.LoadStencil;
import edu.uncc.grid.seeds.dataflow.pattern.stencil.Stencil;

public class AlltoAllDataflow extends Dataflow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
	public boolean computeOneCycle() {
		try {
			AlltoAll user_mod = (AlltoAll)this.getUserModule();
			AllToAllData data = (AllToAllData) this.getState();
			
			for( Dependency dept : this.Outputs ){
				if( dept != null){
					dept.sendObj( data.getSyncData() );
				}
			}
			
			Serializable obj;
			ArrayList<Serializable> lst = new ArrayList<Serializable>();
			for( Dependency dept: this.Inputs){
				if( dept != null){
					obj = dept.takeRecvObj();
					lst.add(obj);
				}
			}
			data.setSyncDataList( lst );
			return user_mod.OneIterationCompute( data );
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (EngineClosedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CycleVersionMissmatch e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public DataflowLoader getDataflowLoaderInstance() {
		// TODO Auto-generated method stub
		return new LoadAlltoAll((AlltoAll)this.getUserModule());
	}

	@Override
	public Dataflow getNewInstance() {
		return new AlltoAllDataflow();
	}

	@Override
	public List<Dataflow> onGrainSizeSplit(int level) {
		return null;
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
		return null;
	}

}
