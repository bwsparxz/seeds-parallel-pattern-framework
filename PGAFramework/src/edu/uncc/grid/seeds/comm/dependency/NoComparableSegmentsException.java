package edu.uncc.grid.seeds.comm.dependency;

public class NoComparableSegmentsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getMessage() {
		return "The Hierarchical Segments are not siblings, so they can't be compared.";
	}
	
}
