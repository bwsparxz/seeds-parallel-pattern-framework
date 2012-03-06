package edu.uncc.grid.seeds.comm.dependency;

public class CycleVersionMissmatch extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	long Dept; 
	long Packet;
	public CycleVersionMissmatch(long dept, long packet){
		this.Dept = dept;
		this.Packet = packet;
	}
	@Override
	public String getMessage() {
		return "Version missmatch Dept=" + Dept + " and Packet=" + Packet ;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
	
}
