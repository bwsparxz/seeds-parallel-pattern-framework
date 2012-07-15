package edu.uncc.grid.test;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.p2p.Types.DataFlowRole;

public class RunTest {
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		WorkpoolTest pi = new WorkpoolTest();
		
		Seeds.start("/rzone/Academia/Seeds2.0/lab/pgaf", false);
		
		Operand s = new Operand(   (String) null
                , new Anchor("Kronos", DataFlowRole.SINK_SOURCE)
                , pi );
		
		PipeID id = Seeds.startPattern( s );
		
		System.out.println( id.toString());
		
		Seeds.waitOnPattern(id);
		
		Seeds.stop();

	}



}
