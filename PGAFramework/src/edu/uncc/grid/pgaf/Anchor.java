package edu.uncc.grid.pgaf;

import edu.uncc.grid.pgaf.p2p.Types;

public class Anchor {
	private static final String divider = "::";
	
	String Hostname;
	Types.DataFlowRole AnchorDFR;
	
	public Anchor( String hostname, Types.DataFlowRole anchor_type){
		Hostname = hostname;
		AnchorDFR = anchor_type;
	}
	public static Anchor valueOf( String str){
		String[] arg = str.split(divider);
		String hostname = arg[0];
		Types.DataFlowRole anchor_dfr = Types.DataFlowRole.valueOf(arg[1]);
		Anchor ans = new Anchor( hostname, anchor_dfr);
		return ans;
	}
	public String getHostname() {
		return Hostname;
	}
	public void setHostname(String hostname) {
		Hostname = hostname;
	}
	public Types.DataFlowRole getAnchorDFR() {
		return AnchorDFR;
	}
	public void setAnchorDFR(Types.DataFlowRole anchorDFR) {
		AnchorDFR = anchorDFR;
	}
	@Override
	public String toString() {
		return Hostname.toString() + "::" + AnchorDFR.toString();
	}
	
	
}
