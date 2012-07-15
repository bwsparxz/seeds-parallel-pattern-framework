package edu.uncc.grid.seeds.example.stencil.split;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.p2p.Types.DataFlowRole;
import edu.uncc.grid.seeds.comm.dataflow.DataflowGatherThread;
/**
 * This class runs the stencil pattern for the heat distribution problem using a constant number of 
 * iterations
 * 
 */
public class RunHeatDistributionStencilOnly {																					
	public static void main(String[] args) {																		
		try {		
			//start the framework
			long framework_deploy_run_shutdown_time_set = System.currentTimeMillis() ;
			if( args.length < 5){
				System.out.println(" argments are: \nSeeds folder path, Anchor host, width, height, matrix file");
			}
			Seeds.start(args[0], false);
				long total_pattern_run_set_time = System.currentTimeMillis();
				//instantiate User module 
				HeatDistribution hd = new HeatDistribution(Integer.parseInt(args[2])
														, Integer.parseInt(args[3]));
				//load matrix
				long start = System.currentTimeMillis();
				hd.loadMatrix(args[4]);
				long time = System.currentTimeMillis() - start;
				System.out.println("Time to load big matrix: " + time);
				//create operand for the module
				Operand f = new Operand(
							new String[]{args[2], args[3]} //initialization argument for module
							, new Anchor(args[1], DataFlowRole.SINK_SOURCE) //sink and source for
									//the 2D matrix data
							, hd );//the actual module
				PipeID p_id = Seeds.startPattern( f );//star pattern, store the id
						//to keep track of it
				Seeds.waitOnPattern(p_id);//wait until pattern is done
				long total_pattern_run_take_time = System.currentTimeMillis() - total_pattern_run_set_time; 
				System.out.println(" time taken to run the pattern: " + total_pattern_run_take_time );
				FileWriter w = new FileWriter( "./stencil_run_time.txt", true);
				
				Set<Long> stop_version_list = DataflowGatherThread.DbgHibernationVersion.keySet();
				String stop_version = "";
				for( long i : stop_version_list){
					stop_version += i + ":";
				}
				
				w.write("time to run pattern:" + total_pattern_run_take_time + ":" + stop_version + "\n");
				w.close();
				hd.saveImage();//save an color coded image of the 2D matrix.
			//stop the framework.
			Seeds.stop();
			long framework_run_time = System.currentTimeMillis() - framework_deploy_run_shutdown_time_set;
			System.out.println(" time taken to deploy,run,and shutdown framework : " + framework_run_time );
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}								
	}
}
