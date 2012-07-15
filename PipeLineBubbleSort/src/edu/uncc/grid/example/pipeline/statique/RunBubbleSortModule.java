package edu.uncc.grid.example.pipeline.statique;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.p2p.Types;

public class RunBubbleSortModule {
	public static void main(String[] args) {
		test(new String[]{"5000", ""+4}, "/rzone/Academia/Seeds2.0/lab/pgaf");	
	}
	public static void test(String[] args, String seeds_path){
		try {
			int w_h = Integer.parseInt(args[0]);
			int cpu_count = Integer.parseInt(args[1]);
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
			Seeds.start( seeds_path , false);
				long start = System.currentTimeMillis();
				PipeID id = Seeds.startPattern( new Operand( args, new Anchor( "Kronos"  , Types.DataFlowRole.SINK_SOURCE), bubble ) );
				System.out.println(id.toString() );
				Seeds.waitOnPattern(id);
				//bubble.printLists();
				long stop = System.currentTimeMillis() - start;
				FileWriter w = new FileWriter( "/home/jfvillal/cache/pipeline_dataflow_test.txt", true);
				w.write("w_h:" + w_h + ":cpu_count:" + cpu_count + ":Time:" + stop + "\n");
				w.close();
				//System.out.println(" Time " + stop);
			Seeds.stop();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}


