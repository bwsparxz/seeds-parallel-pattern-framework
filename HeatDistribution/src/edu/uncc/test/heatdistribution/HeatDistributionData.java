package edu.uncc.test.heatdistribution;
import java.io.Serializable;		
import java.util.List;				
import edu.uncc.grid.pgaf.datamodules.AllToAllData;		
import edu.uncc.grid.pgaf.datamodules.StencilData;		
public class HeatDistributionData implements StencilData, AllToAllData {	
	private static final long serialVersionUID = 1L;	
	Boolean Terminated;				
	public double [][] matrix;		
	public int Width;				
	public int Height;				
	double[][] Sides;				
	public static final int CellCount = 4;				
	public HeatDistributionData(double [][]m){			
		Width = m.length;			
		Height = m[0].length;		
		matrix = m;
		Sides = new double[4][];	
	}			
	@Override
	public Serializable getBottom() {
		return SyncData.getBorder(matrix, Width, Height, SyncData.BOTTOM);		
	}			
	@Override
	public Serializable getLeft() {	
		return SyncData.getBorder( matrix, Width, Height, SyncData.LEFT);
	}			
	@Override
	public Serializable getRight() {
		return SyncData.getBorder(matrix, Width, Height, SyncData.RIGHT);	
	}			
	@Override
	public Serializable getTop() {	
		return SyncData.getBorder(matrix, Width, Height, SyncData.TOP);		
	}			
	@Override
	public void setBottom(Serializable data) {			
		this.Sides[SyncData.BOTTOM] = (double[]) data;	
	}			
	@Override
	public void setLeft(Serializable data) {			
		this.Sides[SyncData.LEFT] = (double[])data;		
	}			
	@Override
	public void setRight(Serializable data) {			
		this.Sides[SyncData.RIGHT] = (double[])data;	
	}			
	@Override
	public void setTop(Serializable data) {				
		this.Sides[SyncData.TOP] = (double[])data;		
	}			
	/**		 
	 * The All-to-All Data object needs better signature names 			
	 */ 	
	@Override
	public Serializable getSyncData() {
		return Terminated;			
	}			
	@Override
	public void setSyncDataList(List<Serializable> dat) {
		Boolean last = Terminated;	
		for( Serializable d : dat){
			Boolean all = (Boolean) d;				
			last = last.booleanValue() && all.booleanValue();			
		}	
		Terminated = last;		
		//only if all are sending true will the program terminate;  	
	}			
}				
