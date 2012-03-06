package edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline.sideways;

import java.io.Serializable;

public class PipeLineStage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int StartStage;
	public int EndStage;
	public Serializable[] State;
}
