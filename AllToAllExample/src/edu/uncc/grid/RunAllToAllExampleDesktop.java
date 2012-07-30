package edu.uncc.grid;

import edu.uncc.grid.pgaf.AdderOperator;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.deployment.Deployer;
import edu.uncc.grid.pgaf.operators.ModuleAdder;
import edu.uncc.grid.pgaf.p2p.Types.DataFlowRole;
import net.jxta.pipe.PipeID;

public class RunAllToAllExampleDesktop{
  public static void main(String[] args) {
    try {
        Operand all_to_all = new Operand(   (String) null
                , new Anchor("Kronos", DataFlowRole.SINK_SOURCE)
                , new PlusOneNeighbors() );
        Operand report_to_stdout = new Operand(   (String) null
                , new Anchor("Kronos", DataFlowRole.SINK_SOURCE)
                , new ReportProgress()  );
        System.out.println(" Create pattern ");
        AdderOperator add = new AdderOperator(new ModuleAdder(4,all_to_all,1,report_to_stdout));
        /**start pattern and get tracking id*/
        System.out.println(" Start pattenr ");
        Thread p_id = Seeds.startPatternMulticore( add , 8);
        p_id.join();
        /**wait for pattern to finish*/
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}