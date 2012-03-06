package edu.uncc.grid.seeds.comm.dependency;

public class EngineClosedException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String msg;
	public EngineClosedException(String str){
		msg = str;
	}
	@Override
	public String getMessage() {
		return super.getMessage() + msg;
	}
	
}
