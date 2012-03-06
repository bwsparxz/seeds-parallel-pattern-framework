package edu.uncc.grid.seeds.comm.dependency;

public class InvalidHierarchicalDependencyIDException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1L;

	@Override
	public String getMessage() {
		return "The Hierarchical URL does not comply with syntax requirements.";
	}
	

}
