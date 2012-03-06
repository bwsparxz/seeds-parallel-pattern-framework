package edu.uncc.grid.seeds.otemplate.pattern.networkscout;

import java.io.Serializable;

public class ScoutNetworkData implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public double[] Latency;
	public double[] Bandwidth;
	public double Flops;
	public long HDDTime;
	public String Hostname;
	public String[] NeighborHosts;
	
	@Override
	public String toString() {
		for( int i = 0; i < Latency.length; i++){
			System.out.println("Latency to segment " + i + " is " + Latency[i] + " RTT in millis.");
		}
		
		for( int i = 0; i < Latency.length; i++){
			System.out.println("Bandwidth to segment " + i + " is " + Bandwidth[i] + " in Mbs/Sec");
		}
		
		System.out.println("Flops are: " + Flops + " in MFlops/Sec" );
		
		return super.toString();
	}
	
	
}
