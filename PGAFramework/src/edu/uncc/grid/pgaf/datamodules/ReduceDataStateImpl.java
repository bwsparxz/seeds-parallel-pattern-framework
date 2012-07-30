package edu.uncc.grid.pgaf.datamodules;

public class ReduceDataStateImpl implements ReduceDataState {
	public int Iteration;
	public ReduceDataStateImpl(){
		Iteration = 0;
	}
	@Override
	public void advanceIteration() {
		++Iteration;
	}
	@Override
	public long getIteration() {
		return Iteration;
	}
}
