package edu.uncc.grid.seeds.comm.dataflow;

import java.util.List;

import edu.uncc.grid.seeds.comm.dependency.SplitCoalesceHandler;

public class NoMoreDataflowPerceptrons extends Dataflow {

	public NoMoreDataflowPerceptrons() {
		
		
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean computeOneCycle() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte getControl() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setControl(byte set) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DataflowLoader getDataflowLoaderInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isHibernated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setHibernated(boolean set) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Dataflow getNewInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Dataflow> onGrainSizeSplit(int level) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dataflow onGrainSizeCoalesce(List<Dataflow> perceptrons, int level) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SplitCoalesceHandler getSplitCoalesceHander() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String DbgGetCurrVersion() {
		// TODO Auto-generated method stub
		return "NoMoreDataflowPerceptron";
	}


}
