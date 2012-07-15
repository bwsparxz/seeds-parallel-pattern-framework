package edu.uncc.grid.seeds.example.stencil.split;

import edu.uncc.grid.seeds.dataflow.pattern.stencil.Stencil;

/**
 * This static class will return a strip of data points from a 2D matrix.
 * The stip can be the left, right, top, or bottom.
 * 
 * @author jfvillal
 *
 */
public class SyncData {					
	
	public static double[] getBorder( double number[][], int cols, int rows , int side ){
		double [] sync = null;			
		switch( side){					
		case Stencil.LEFT:						
			sync = new double[rows];	
			for( int i =0 ; i < rows ; i++){
				sync[i] = number[0][i];		
			}								
			break;
		case Stencil.RIGHT:							
			sync = new double[rows];		
			for( int i =0 ; i < rows ; i++){
				sync[i] = number[cols -1][i];
			}						
			break;					
		case Stencil.TOP:					
			sync = new double[cols];
			for( int i =0 ; i < cols ; i++){
				sync[i] = number[i][0];
			}			
			break;		
		case Stencil.BOTTOM:	
			sync = new double[cols];
			for( int i =0 ; i < cols ; i++){
				sync[i] = number[i][rows - 1];
			}		
			break;
		}			
		return sync;
	}				
}					
