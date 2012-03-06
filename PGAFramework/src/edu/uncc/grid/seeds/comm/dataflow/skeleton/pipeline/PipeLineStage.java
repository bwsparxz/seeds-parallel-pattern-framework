package edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline;

import java.io.Serializable;

public class PipeLineStage implements Serializable {
	public int StartStage;
	public int EndStage;
	public Serializable[] State;
}
