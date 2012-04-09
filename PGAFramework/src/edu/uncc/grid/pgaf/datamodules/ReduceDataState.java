package edu.uncc.grid.pgaf.datamodules;

import java.io.Serializable;

public interface ReduceDataState extends Serializable {
	public void advanceIteration();
	public int getIteration();
}
