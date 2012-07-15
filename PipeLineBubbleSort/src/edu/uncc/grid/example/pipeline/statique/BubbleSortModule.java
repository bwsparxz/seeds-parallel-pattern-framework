package edu.uncc.grid.example.pipeline.statique;

import java.io.Serializable;
import java.util.logging.Level;

import edu.uncc.grid.example.pipeline.dynamic.Bucket;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline.PipeLine;


public class BubbleSortModule extends PipeLine {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int[][] ListsOfLists;
	
	@Override
	public Serializable Compute(int stage, Serializable dat) {
		Bucket bucket = (Bucket) dat;
		int index = stage;
		for( int i = index; i <  bucket.d.length; i++){
			if( bucket.d[index] > bucket.d[i] ){
				int temp = bucket.d[index];
				bucket.d[index] = bucket.d[i];
				bucket.d[i] = temp;
			}
		}
		//System.out.print("W:"+stage+":");
		//for( int i = 0; i < input.length; i++){
		//	System.out.print(input[i]+ ",");
		//}
		//System.out.println();
		return bucket;
	}
	@Override
	public Serializable DiffuseData(int segment) {
		Bucket bucket = new Bucket();
		bucket.segment = segment;
		bucket.d = ListsOfLists[segment];
		return bucket;
	}
	@Override
	public void GatherData( Serializable dat) {
		Bucket bucket = (Bucket) dat ;
		int[] m = ( int[] )bucket.d;
		ListsOfLists[bucket.segment] = m;
	}
	public void printLists(){
		for( int i = 0; i < ListsOfLists.length; i++){
			for( int j= 0; j < ListsOfLists[i].length; j++){
				System.out.print( ListsOfLists[i][j] + ",");
			}
			System.out.println();
		}
	}
	int stages;
	int data_count;
	int cpu_count;
	@Override
	public int getDataCount() {
		return data_count;
	}
	@Override
	public int getStageCount() {
		return stages;
	}
	@Override
	public void initializeModule(String[] args) {
		Node.getLog().setLevel(Level.INFO);
		int w_h = Integer.parseInt(args[0]);
		int cpu_c =Integer.parseInt(args[1]);
		stages = w_h;
		data_count = w_h;
		cpu_count = cpu_c;
	}
	
	@Override
	public int getMinProcessCount() {
		return cpu_count;
	}
}
