package edu.uncc.test.heatdistribution;
import edu.uncc.grid.pgaf.datamodules.AllToAllData; 		
import edu.uncc.grid.pgaf.interfaces.basic.CompleteSyncGraph;
public class TerminationDetection extends CompleteSyncGraph { 
	private static final long serialVersionUID = 1L; 		
	public TerminationDetection(){	
	}		
	@Override 
	public AllToAllData DiffuseData(int segment) { 
		// not used
		return null;	
	}		
	@Override 
	public void GatherData(int segment, AllToAllData data) { 
		// not used
	}		
	@Override 
	public boolean OneIterationCompute(AllToAllData data) { 
		HeatDistributionData d = (HeatDistributionData) data; 
		if( d.Terminated ){
System.out.println("terminating .... count: "  );
		}	
		return d.Terminated;		
	}		
	@Override 
	public int getCellCount() {		
		return HeatDistributionData.CellCount;
	}		
	@Override  
	public void initializeModule(String[] args) {
		// not used.
	}		
}
