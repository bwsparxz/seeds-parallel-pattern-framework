package edu.uncc.grid.example.workpool;

import java.io.IOException;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.p2p.Types;

public class RunNumericalIntegration {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			NumericalIntegrationModule mod = new NumericalIntegrationModule();
			Seeds.start( "/rzone/Academia/Seeds2.0/lab/pgaf" , false);
				String host = "Kronos";
				PipeID id = Seeds.startPattern( new Operand( (String[])null, new Anchor( host  , Types.DataFlowRole.SINK_SOURCE), mod ) );
				System.out.println(id.toString() );
				Seeds.waitOnPattern(id);
				System.out.println( "The area is: " + mod.getArea() ) ;
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
