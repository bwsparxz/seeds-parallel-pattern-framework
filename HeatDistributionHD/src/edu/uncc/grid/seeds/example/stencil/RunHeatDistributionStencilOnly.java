package edu.uncc.grid.seeds.example.stencil;

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
			Seeds.start("/rzone/Academia/Seeds2.0/lab/pgaf", false);
				long total_pattern_run_set_time = System.currentTimeMillis();
				//instantiate User module
				int cpu_width = 2;
				int cpu_height = 2;
				HeatDistribution hd = new HeatDistribution(cpu_width, cpu_height );
				//load matrix
				long start = System.currentTimeMillis();
				hd.loadMatrix();
				long time = System.currentTimeMillis() - start;
				System.out.println("Time to load big matrix: " + time);
				//create operand for the module
				String host = "Kronos";
				
				Operand f = new Operand(
							new String[]{"" + cpu_width, "" + cpu_height } //initialization argument for module
							, new Anchor(host, DataFlowRole.SINK_SOURCE) //sink and source for
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
