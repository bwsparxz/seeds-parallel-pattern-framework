package edu.uncc.grid.example.workpool;

import java.io.IOException;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.p2p.Types;

public class RunNumericalIntegrationDesktop {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for( int i = 2; i < 9; i++){
			long start = System.currentTimeMillis();
			try {
				Thread.currentThread().setName("Main App");
				NumericalIntegrationModule mod = new NumericalIntegrationModule();
				String host = "Kronos";
				Operand pattern = new Operand( (String[])null, new Anchor( host  , Types.DataFlowRole.SINK_SOURCE), mod );
				Thread t = Seeds.startPatternMulticore( pattern , i );
				t.join();
				System.out.println( "The area is: " + mod.getArea() ) ;
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			long end = System.currentTimeMillis() - start;
			System.out.println( "Time: " + end );
		}
	}
}
