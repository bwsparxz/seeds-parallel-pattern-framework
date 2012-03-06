package edu.uncc.grid.seeds.comm.dataflow;

public class DataflowNoCoalesceableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return "The Dataflow cannot be coalesce any further.  It has not parent or It consist of just one sibling.";
	}
	
}
