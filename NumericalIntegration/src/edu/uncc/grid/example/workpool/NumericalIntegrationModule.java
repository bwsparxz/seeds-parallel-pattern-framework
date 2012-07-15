package edu.uncc.grid.example.workpool;

import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.basic.Workpool;

public class NumericalIntegrationModule extends Workpool {
	double Start; //integral start
	double Stop;  //integral stop
	long Pieces;  //number of discrete squares to fit to function
	double Width; //holds the width of each piece
	double TotalArea;  //accumulates final value
	public static int UNIT_SIZE = 100000;  //static number of 
			//pieces processed by each job unit.
	public NumericalIntegrationModule(){
		Start = 0;  //start for integral
		Stop = 2;   //stop for integral
		Pieces = 1000000000; //number of rectangles
		//compute width of rectangle
		Width = (Stop - Start) / Pieces ;
		TotalArea = 0.0;
	}
	@Override
	public Data Compute(Data data) {
		
		DataMap<String, Object> input = (DataMap<String,Object>) data;
		long start = (Long)input.get("start");
		double width = (Double)input.get("width");
		
		double x = start * width; //set x one width below
		double area = 0.0; //set area to zero.
		for( long i = 0; i < UNIT_SIZE ; i++){
			area += squareArea( x , width);
			x += width;
		}
		//System.out.println("Area: " + area);
		DataMap<String,Double> output = new DataMap<String,Double>();
		output.put("sub_area", area);
		
		return output;
	}

	@Override
	public Data DiffuseData(int segment) {
		long start = (long)segment * (long)UNIT_SIZE;
		DataMap<String,Object> packet= new DataMap<String,Object>();
		packet.put("width", Width);
		packet.put("start", start);
		return packet;
	}

	@Override
	public void GatherData(int seg, Data result) {
		DataMap<String,Double> in = (DataMap<String,Double>) result;
		TotalArea += in.get("sub_area"); 
	}

	@Override
	public int getDataCount() {
		//number of jobs is divided into 10,000 piece units
		return (int) (Pieces / UNIT_SIZE);
	}

	@Override
	public void initializeModule(String[] arg0) {
		//this can be used to pass values to all the nodes
		//for example UNIT_SIZE could be set dynamically at
		//runtime and passed to the remote nodes using this
		//method.  The values for String[] is given to the
		//Anchor class in the RunModule executable class
	}
	/**
	 * Calculates the area of a square
	 * @param x_1
	 * @param width
	 * @return
	 */
	public static double squareArea( double x_1, double width){
		return formula( x_1 ) * width;
	}
	/**
	 * The formula used for this example.
	 * @param x
	 * @return
	 */
	public static double formula( double x){
		return Math.sin( x * Math.PI ) + 2;
	}
	public double getArea(){
		return TotalArea;
	}
}
