package edu.uncc.grid;

import edu.uncc.grid.pgaf.AdderOperator;
import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Seeds;
import edu.uncc.grid.pgaf.deployment.Deployer;
import edu.uncc.grid.pgaf.p2p.Types.DataFlowRoll;
import net.jxta.pipe.PipeID;

public class RunAllToAllExample{
  public static void main(String[] args) {
    Deployer deploy;
    try {
      Seeds.start("/rzone/Academia/Seeds2.0/lab/pgaf", false);
        Operand s = new Operand(   (String) null
                , new Anchor("Kronos", DataFlowRoll.SINK_SOURCE)
                , new PlusOneNeighbors() );
        /**start pattern and get tracking id*/
        PipeID p_id = Seeds.startPattern( s );
        /**wait for pattern to finish*/
        Seeds.waitOnPattern(p_id);
      Seeds.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
