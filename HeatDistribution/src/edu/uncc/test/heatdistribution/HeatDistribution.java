package edu.uncc.test.heatdistribution;															
import edu.uncc.grid.pgaf.datamodules.StencilData;												
import edu.uncc.grid.pgaf.interfaces.basic.Stencil;												
public class HeatDistribution extends Stencil {													
	private static final long serialVersionUID = 1L;											
	double[][] MainMatrix; 																	
	public HeatDistribution(){														
	}																				
	public void loadMatrix(){ 																
		MainMatrix = MatrixReader.getMainMatrix(); 											
	}																						
	@Override																			
	public StencilData DiffuseData(int segment) {												
		double [][] m = MatrixReader.getSubMatrix(MainMatrix, segment, this.getCellCount());	
		HeatDistributionData heat  = new HeatDistributionData(m);					
		return heat;																
	}																							
	public void saveImage(){									
		MatrixReader.saveImage(this.MainMatrix );				
	}															
	@Override																			
	public void GatherData(int segment, StencilData dat) {										
		HeatDistributionData heat = (HeatDistributionData) dat; 					
		MatrixReader.setSubMatrix(this.MainMatrix, heat.matrix, segment, this.getCellCount());	
	}																							
	@Override 																			
	public boolean OneIterationCompute(StencilData data) {										
		HeatDistributionData heat = (HeatDistributionData) data;					
		double[][] m = new double[heat.Width][heat.Height];									
		// do the core																	
		//start at one and end one before the last one									
		//because it uses those numbers.												
		for( int r = 1; r < heat.Height -1; r++){											
			for( int c = 1; c < heat.Width -1; c++){										
				m[c][r] =     0.25 * (  heat.matrix[c + 1][r] +								
										heat.matrix[c - 1][r] + 									
										heat.matrix[c][r + 1] + 									
										heat.matrix[c][r - 1]  ); 									
			}																				
		}																					
		// do the sides  																
		//do bottom																		
		if( heat.Sides[SyncData.BOTTOM ] != null){ 									
			double[] d = heat.Sides[SyncData.BOTTOM];								
			for( int i = 1; i  < heat.Width -1; i++){								
				m[i][heat.Height - 1] =    0.25 * ( heat.matrix[i + 1][heat.Height - 1] +		
													heat.matrix[i - 1][heat.Height - 1] +			
													heat.matrix[i][heat.Height - 2] 				
													+ d[i] );										
			}																		
		}else{																		
			for( int i = 1; i  < heat.Width - 1; i++){								
				m[i][heat.Height - 1] =   heat.matrix[i][heat.Height -1];			
			}																		
		}																			
		//do top 																		
		if( heat.Sides[SyncData.TOP ] != null){										
			double[] d = heat.Sides[SyncData.TOP];									
			for( int i = 1; i  < heat.Width -1; i++){								
				m[i][0] =    0.25 * ( heat.matrix[i + 1][0] +						
													heat.matrix[i - 1][0] +							
													heat.matrix[i][1]  								
													  + d[i]); 										
			}																		
		}else{																		
			for( int i = 1; i  < heat.Width -1; i++){								
				m[i][0] =   heat.matrix[i][0];										
			}																		
		}																			
		//do left 																		
		if( heat.Sides[SyncData.LEFT] != null){ 									
			double[] l = heat.Sides[SyncData.LEFT];									
			for( int i = 1; i  < heat.Height -1; i++){								
				m[0][i] =    0.25 *  (  heat.matrix[0][i + 1 ] +					
									   heat.matrix[0][i - 1] + 										
									   heat.matrix[1][i]+  											
									   l[i]               ) ; 										
			}																		
		}else{																		
			for( int i = 1; i  < heat.Height -1; i++){								
				m[0][i] =  heat.matrix[0][i];										
			}																		
		}																			
		//do right 																		
		if( heat.Sides[SyncData.RIGHT ] != null){									
			double[] d = heat.Sides[SyncData.RIGHT];								
			for( int i = 1; i  < heat.Height -1; i++){								
				m[heat.Width - 1][i] =  0.25 *  (  heat.matrix[heat.Width - 1][i + 1 ] +		
									   				heat.matrix[heat.Width - 1][i - 1] +			
									   				heat.matrix[heat.Width - 2][i] + 				
									   				d[i]); 											
			}																		
		}else{																		
			for( int i = 1; i  < heat.Height -1; i++){								
				m[heat.Width - 1][i] =   heat.matrix[heat.Width -1 ][i];			
			}																		
		}																			
		//do corners 																	
		// 0,0 																			
		if( heat.Sides[SyncData.LEFT] != null){ 									
			double[] l = heat.Sides[SyncData.LEFT];									
			if( heat.Sides[SyncData.TOP] != null){									
				// lef top 																
				double[] t = heat.Sides[SyncData.TOP];								
				m[0][0] =  0.25 * ( heat.matrix[1][0 ] +							
					   				heat.matrix[0][1] + 											
					   				l[0] +  														
					   				t[0]);															
			}else{ 																	
				// LEFT  !TOP 															
				m[0][0] =  heat.matrix[0][0]; 										 
			}																		
		}else{																		
			m[0][0] =  heat.matrix[0][0];											 
		}																			
		// RIGHT TOP 																	
		if( heat.Sides[SyncData.RIGHT] != null){									
			double[] r = heat.Sides[SyncData.RIGHT];								
			if( heat.Sides[SyncData.TOP] != null){									
				// right  top 															
				double[] t = heat.Sides[SyncData.TOP]; 								
				m[heat.Width - 1][0] =  0.25 *  (heat.matrix[heat.Width - 2][0 ] +	
					   				heat.matrix[heat.Width - 1][1] +								
					   				r[0] + 															
					   				t[heat.Width - 1]); 													
			}else{ 																	
				// right  !TOP 															
				m[heat.Width -1][0] =  heat.matrix[heat.Width -1][0]; 				
			}																		
		}else{																		
			m[heat.Width -1][0] =  heat.matrix[heat.Width -1][0];					
		}																			
		//right bottom																	
		// WIDTH - 1, HEIGHT - 1 														
		if( heat.Sides[SyncData.RIGHT] != null){									
			double[] r = heat.Sides[SyncData.RIGHT];								
			if( heat.Sides[SyncData.BOTTOM] != null){								
				// RIGHT  BOTTOM 														
				double[] b = heat.Sides[SyncData.BOTTOM];							
				m[heat.Width - 1][heat.Height - 1] =  0.25 * (						
									heat.matrix[heat.Width - 2][ heat.Height - 1 ] +
					   				heat.matrix[heat.Width - 1][ heat.Height - 2] + 				
					   				r[heat.Height - 1] +  											
					   				b[heat.Width - 1]);												
			}else{																	
				// RIGHT  !BOTTOM 														
				m[heat.Width - 1][heat.Height - 1] = heat.matrix[heat.Width -1][heat.Height -1]; 
			}																		
		}else{																		
			m[heat.Width - 1][heat.Height - 1] = heat.matrix[heat.Width -1][heat.Height -1];	
		}																			
		// LEFT BOTTOM  																
		// 0, HEIGHT - 1 																
		if( heat.Sides[SyncData.LEFT] != null){										
			double[] l = heat.Sides[SyncData.LEFT];									
			if( heat.Sides[SyncData.BOTTOM] != null){								
				// LEFT BOTTOM 															
				double[] b = heat.Sides[SyncData.BOTTOM];							
				m[0][heat.Height - 1] = 0.25 * (  									
									heat.matrix[1][heat.Height -1  ] + 								
					   				heat.matrix[0][heat.Height - 2] + 								
					   				l[heat.Height - 1] +  											
					   				b[0] );															
			}else{ 																	
				// LEFT  !BOTTOM 														
				m[0][heat.Height - 1] = heat.matrix[0][heat.Height - 1];			  		
			}																		
		}else{																		
			m[0][heat.Height - 1] = heat.matrix[0][heat.Height - 1];				
		}																			
		//termination check 															 
		double max_diff = 0.0;																
		for( int r = 0; r < heat.Height ; r++){												
			for( int c = 0; c < heat.Width ; c++){											
				double diff = m[c][r] - heat.matrix[c][r];									
				if( diff > max_diff) {														
					max_diff = diff;														
				}																			
			}																				
		}																					
		heat.Terminated = max_diff < 10.00;															
		heat.matrix = m;																	
		return  false;  															
	}																							
	@Override																			
	public int getCellCount() {																	
		return HeatDistributionData.CellCount; 										
	}																							
	@Override																			
	public void initializeModule(String[] args) {												
		//not used  																	
	}																							
}																								
