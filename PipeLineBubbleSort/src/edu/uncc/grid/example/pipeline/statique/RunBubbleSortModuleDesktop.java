package edu.uncc.grid.example.pipeline.statique;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.p2p.Types;

public class RunBubbleSortModuleDesktop {
	public static void main(String[] args) {
		test(new String[]{"5000", ""+4}, "/rzone/Academia/Seeds2.0/lab/pgaf");	
	}
	public static void test(String[] args, String seeds_path){
		try {
			int w_h = 10;
			int cpu_count = 7;
			int list[][] = new int[w_h][w_h];
			Random r = new Random();
			r.setSeed(837483748374L);
			for( int i = 0 ; i < w_h; i++){
				for ( int j = 0; j < w_h; j ++){
					list[i][j] = r.nextInt(1000);
				}
			}
			BubbleSortModule bubble = new BubbleSortModule();
			bubble.ListsOfLists = list;
			long start = System.currentTimeMillis();
			Thread id = Seeds.startPatternMulticore(
					new Operand( new String[]{w_h+"", cpu_count +""}, new Anchor( "Kronos"  , Types.DataFlowRole.SINK_SOURCE), bubble ) 
					, 8);
			id.join();
			long stop = System.currentTimeMillis() - start;
			for( int[] lst : bubble.ListsOfLists ){
				for( int i = 0 ; i < lst.length ; i++ ){
					System.out.print( lst[i] + ", " );
				}
				System.out.println();
			}
			
				
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}


