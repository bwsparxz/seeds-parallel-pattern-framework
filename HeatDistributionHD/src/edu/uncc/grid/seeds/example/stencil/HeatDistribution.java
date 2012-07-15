package edu.uncc.grid.seeds.example.stencil;
import java.io.Serializable;

import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.seeds.dataflow.pattern.stencil.Stencil;
public class HeatDistribution extends Stencil {
	private static final long serialVersionUID = 1L;
	double[][] MainMatrix;
	/**
	 * The Width and Height for the matrix of processors.
	 */
	public int Width, Height;
	public HeatDistribution(){	
	}
	public HeatDistribution(int w, int h){
		Width = w;
		Height = h;
	}
	public void loadMatrix( ){
		MainMatrix = MatrixReader.getMainMatrix( );
	}
	public long Start;
	@Override
	public StencilData DiffuseData(int segment) {
		if( segment == 0){
			Start = System.currentTimeMillis();
		}
		if( segment == 3){
			long time = System.currentTimeMillis() - Start ;
			System.out.println("Time to load pattern is: " + time  + " matrix dimension(WxH)=" + this.getWidthCellCount() + "x" + this.getHeightCellCount());
		}
		double [][] m = MatrixReader.getSubMatrix(MainMatrix, segment, this.getWidthCellCount(), this.getHeightCellCount() );
		HeatDistributionData heat  = new HeatDistributionData(m);
		return heat;
	}
	public void saveImage(){
		MatrixReader.saveImage(this.MainMatrix );
	}
	@Override
	public void GatherData(int segment, StencilData dat) {
		HeatDistributionData heat = (HeatDistributionData) dat;
		MatrixReader.setSubMatrix(this.MainMatrix, heat.matrix, segment, this.getWidthCellCount(), this.getHeightCellCount());
	}
	public double function( double r, double l, double b, double t){
		return 0.25 * (  l + r + t + b ); 
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
				m[c][r] =     function(  heat.matrix[c + 1][r] ,													
										heat.matrix[c - 1][r] ,												
										heat.matrix[c][r + 1] , 												
										heat.matrix[c][r - 1]  ); 												
			}																									
		}																										
		// do the sides  																						
		//do bottom																								
		if( heat.Sides[Stencil.BOTTOM ] != null){ 																
			double[] d = heat.Sides[Stencil.BOTTOM];															
			for( int i = 1; i  < heat.Width -1; i++){															
				m[i][heat.Height - 1] =   function ( heat.matrix[i + 1][heat.Height - 1] ,					
													heat.matrix[i - 1][heat.Height - 1] ,						
													heat.matrix[i][heat.Height - 2] ,						
													d[i] );													
			}																									
		}else{																									
			for( int i = 1; i  < heat.Width - 1; i++){															
				m[i][heat.Height - 1] =   heat.matrix[i][heat.Height -1];										
			}																									
		}																										
		//do top 																								
		if( heat.Sides[Stencil.TOP ] != null){																	
			double[] d = heat.Sides[Stencil.TOP];																
			for( int i = 1; i  < heat.Width -1; i++){															
				m[i][0] =    function ( heat.matrix[i + 1][0] ,													
													heat.matrix[i - 1][0] ,										
													heat.matrix[i][1]  	,									
													   d[i]); 													
			}																									
		}else{																									
			for( int i = 1; i  < heat.Width -1; i++){															
				m[i][0] =   heat.matrix[i][0];																	
			}																									
		}																										
		//do left 																								
		if( heat.Sides[Stencil.LEFT] != null){ 																
			double[] l = heat.Sides[Stencil.LEFT];
			for( int i = 1; i  < heat.Height -1; i++){
				m[0][i] =    function (  heat.matrix[0][i + 1 ], 
									   heat.matrix[0][i - 1] ,
									   heat.matrix[1][i]	,										
									   l[i]               ) ;
			}
		}else{
			for( int i = 1; i  < heat.Height -1; i++){
				m[0][i] =  heat.matrix[0][i];
			}
		}																							
		//do right
		if( heat.Sides[Stencil.RIGHT ] != null){
			double[] d = heat.Sides[Stencil.RIGHT];
			for( int i = 1; i  < heat.Height -1; i++){						
				m[heat.Width - 1][i] =  function (  heat.matrix[heat.Width - 1][i + 1 ] ,
									   				heat.matrix[heat.Width - 1][i - 1] ,
									   				heat.matrix[heat.Width - 2][i] ,
									   				d[i]);
			}
		}else{
			for( int i = 1; i  < heat.Height -1; i++){
				m[heat.Width - 1][i] =   heat.matrix[heat.Width -1 ][i];
			}
		}
		//do corners
		// 0,0
		if( heat.Sides[Stencil.LEFT] != null){
			double[] l = heat.Sides[Stencil.LEFT];
			if( heat.Sides[Stencil.TOP] != null){
				// lef top
				double[] t = heat.Sides[Stencil.TOP];
				m[0][0] =  function ( heat.matrix[1][0 ] ,
					   				heat.matrix[0][1] ,
					   				l[0] ,
					   				t[0]);
			}else{
				// LEFT  !TOP
				m[0][0] =  heat.matrix[0][0];
			}
		}else{
			m[0][0] =  heat.matrix[0][0];
		}
		// RIGHT TO
		if( heat.Sides[Stencil.RIGHT] != null){
			double[] r = heat.Sides[Stencil.RIGHT];
			if( heat.Sides[Stencil.TOP] != null){
				// right  top
				double[] t = heat.Sides[Stencil.TOP];
				m[heat.Width - 1][0] = function(heat.matrix[heat.Width - 2][0 ] ,
					   				heat.matrix[heat.Width - 1][1] ,
					   				r[0] ,
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
		if( heat.Sides[Stencil.RIGHT] != null){
			double[] r = heat.Sides[Stencil.RIGHT];
			if( heat.Sides[Stencil.BOTTOM] != null){
				// RIGHT  BOTTOM
				double[] b = heat.Sides[Stencil.BOTTOM];
				m[heat.Width - 1][heat.Height - 1] =  function (
									heat.matrix[heat.Width - 2][ heat.Height - 1 ] ,
					   				heat.matrix[heat.Width - 1][ heat.Height - 2] ,
					   				r[heat.Height - 1] ,
					   				b[heat.Width - 1]);
			}else{
				// RIGHT  !BOTTOM
				m[heat.Width - 1][heat.Height - 1] = heat.matrix[heat.Width -1][heat.Height -1];
			}
		}else{
			m[heat.Width - 1][heat.Height - 1] = heat.matrix[heat.Width -1][heat.Height -1];
		}
		// LEFT BOTTO  																						
		// 0, HEIGHT - 1
		if( heat.Sides[Stencil.LEFT] != null){
			double[] l = heat.Sides[Stencil.LEFT];
			if( heat.Sides[Stencil.BOTTOM] != null){
				// LEFT BOTTOM
				double[] b = heat.Sides[Stencil.BOTTOM];
				m[0][heat.Height - 1] = function (
									heat.matrix[1][heat.Height -1  ] ,
					   				heat.matrix[0][heat.Height - 2] ,
					   				l[heat.Height - 1] ,
					   				b[0] );
			}else{
				// LEFT  !BOTTOM
				m[0][heat.Height - 1] = heat.matrix[0][heat.Height - 1];
			}
		}else{
			m[0][heat.Height - 1] = heat.matrix[0][heat.Height - 1];
		}
		heat.matrix = m;
		++heat.Loop;
		
		
		return  heat.Loop < 3000;
	}
	
	@Override
	public int getWidthCellCount() {
		return Width;
	}
	@Override
	public int getHeightCellCount() {
		return Height;
	}
	
	/**
	 * should split horizontally
	 *  __________
	 * | part one |
	 * |__________|
	 * | part two |
	 * |__________|
	 */
	@Override
	public StencilData[] onSplitState(StencilData data, int level) {
		HeatDistributionData main = (HeatDistributionData) data;
		double[][] matrix = main.matrix;
		int half = main.Height/2;
		double[][] matrix_one = new double[main.Width][half];
		double[][] matrix_two = new double[main.Width][main.Height - half];
		for( int w = 0; w < main.Width; w++){
			for( int h = 0; h < half ; h++){
				matrix_one[w][h] = matrix[w][h];
			}
		}
		for( int w = 0; w < main.Width; w++){
			for( int h = half; h < main.Height; h++){
				matrix_two[w][h-half] = matrix[w][h];
			}
		}
		HeatDistributionData one = new HeatDistributionData(matrix_one);
		HeatDistributionData two = new HeatDistributionData(matrix_two);
		one.Loop = main.Loop;
		two.Loop = main.Loop;
		StencilData[] ans = {one, two};
		return ans;
	}
	@Override
	public StencilData onCoalesceState(StencilData[] dats, int level) {
		HeatDistributionData one = (HeatDistributionData)dats[0];
		HeatDistributionData two = (HeatDistributionData)dats[1];
		double[][] matrix_one = one.matrix;
		double[][] matrix_two = two.matrix ;
		
		double[][] matrix = new double[one.Width][one.Height + two.Height];
		for( int w = 0 ; w < one.Width; w++){
			for( int h = 0 ; h < one.Height; h++){
				matrix[w][h] = matrix_one[w][h];
			}
		}
		for( int w = 0 ; w < two.Width; w++){
			for( int h = one.Height; h < one.Height + two.Height ; h++){
				matrix[w][h] = matrix_two[w][h-one.Height];
			}
		}
		HeatDistributionData united = new HeatDistributionData( matrix);
		united.Loop = one.Loop;
		return united;
	}
	@Override
	public Serializable[] splitData(Serializable serial) {
		double[] packet = (double[]) serial;
		int half = packet.length / 2;
		int whole = packet.length;
		double[] one = new double[half];
		double[] two = new double[whole - half];
		for( int i = 0 ; i < half ; i++){
			one[i] = packet[i];
		}
		for( int i = half; i < whole; i++){
			two[i - half] = packet[i];
		}
		Serializable[] ans = {one, two};
		return ans;
	}
	@Override
	public Serializable coalesceData(Serializable[] packets) {
		double[] one = (double[]) packets[0];
		double[] two = (double[]) packets[1];
		double[] united = new double[one.length + two.length];
		for( int i = 0 ; i < one.length; i++){
			united[i] = one[i];
		}
		for( int i = one.length; i < one.length + two.length; i++){
			united[i] = two[i - one.length];
		}	
		return united;
	}
	@Override
	public void initializeModule(String[] args) {
		Width = Integer.parseInt(args[0]);
		Height = Integer.parseInt(args[1]);
		// TODO Auto-generated method stub
		//Node.getLog().setLevel(Level.OFF);
	}	
}
