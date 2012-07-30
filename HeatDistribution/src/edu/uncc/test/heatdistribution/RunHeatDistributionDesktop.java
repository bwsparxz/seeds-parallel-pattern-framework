package edu.uncc.test.heatdistribution;
import java.io.IOException;					
								
import net.jxta.pipe.PipeID;			 
import edu.uncc.grid.pgaf.AdderOperator; 
import edu.uncc.grid.pgaf.Anchor;	 
import edu.uncc.grid.pgaf.Operand;					 
import edu.uncc.grid.pgaf.Seeds;					 
import edu.uncc.grid.pgaf.operators.ModuleAdder;	 
import edu.uncc.grid.pgaf.p2p.Types.DataFlowRole;
/**
 * 
 * This demo shows how to implement a 5-point stencil to run a simple heat distribution problem on Seeds
 * the demo also shows how to add a termination detection using the pattern adder operator.
 * 
 * @author jfvillal
 *
 */
public class RunHeatDistributionDesktop {															
	public static void main(String[] args) {												
		try {																				
			long framework_deploy_run_shutdown_time_set = System.currentTimeMillis() ;		
			long total_pattern_run_set_time = System.currentTimeMillis();
			HeatDistribution hd = new HeatDistribution();
			hd.loadMatrix();							
			String host = "Kronos";
			Operand f = new Operand((String) null, new Anchor(host, DataFlowRole.SINK_SOURCE) , hd );
			Operand s = new Operand( (String) null, new Anchor(host, DataFlowRole.SINK_SOURCE), new TerminationDetection() );
			AdderOperator add = new AdderOperator( new ModuleAdder( 2, f, 1, s ) );
			Thread p_id = Seeds.startPatternMulticore( add , 5 );
			p_id.join();
			
			long total_pattern_run_take_time = System.currentTimeMillis() - total_pattern_run_set_time;
			System.out.println(" time taken to run the pattern: " + total_pattern_run_take_time );	 			
			hd.saveImage(); 		
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
