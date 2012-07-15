package edu.uncc.test.heatdistribution;				
public class SyncData {								
	public static final int LEFT = 0;				
	public static final int RIGHT = 1;				
	public static final int TOP = 2;				
	public static final int BOTTOM = 3;				
	public static double[] getBorder( double number[][], int cols, int rows , int side ){
		double [] sync = null;						
		switch( side){								
		case LEFT:									
			sync = new double[rows];				
			for( int i =0 ; i < rows ; i++){		
				sync[i] = number[0][i];				
			}										
			break;									
		case RIGHT:									
			sync = new double[rows];				
			for( int i =0 ; i < rows ; i++){		
				sync[i] = number[cols -1][i];		
			}										
			break;									
		case TOP:									
			sync = new double[cols];				
			for( int i =0 ; i < cols ; i++){		
				sync[i] = number[i][0];				
			}										
			break;									
		case BOTTOM:								
			sync = new double[cols];				
			for( int i =0 ; i < cols ; i++){		
				sync[i] = number[i][rows - 1];		
			}										
			break;									
		}											
		return sync;								
	}												
}													
