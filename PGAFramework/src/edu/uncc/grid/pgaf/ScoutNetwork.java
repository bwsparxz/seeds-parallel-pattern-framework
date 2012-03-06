package edu.uncc.grid.pgaf;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.seeds.otemplate.pattern.networkscout.ScoutModule;


/**
 * This command will launch the Seeds framework with the task to scout the network.
 * The result from the command will allow Seeds to quickly select DirectorRDV and Worker Nodes
 * from the results.
 * 
 * The user should run this command whenever the AvailableServers file is changed.  If there is 
 * suspicion of a change in the network configuration.
 * 
 * This command will eliminate the need for a DirectorRDV competition which took too much time to 
 * execute and relied on the creation of files. 
 * @author jfvillal
 *
 */
public class ScoutNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Seeds.start( args[0] , false );
			Thread.sleep(40000);	
				
			long start = System.currentTimeMillis();
			ScoutModule net_scout = new ScoutModule();
			PipeID id = Seeds.startPattern(
					new Operand( 
							new String[]{ args[2], ""+0}  
							, new Anchor( args[1]  
							, Types.DataFlowRoll.SINK_SOURCE), net_scout ) 
					);
			Seeds.waitOnPattern(id);
			long time = System.currentTimeMillis() - start ;
			System.out.println(":Benchmark time: " + time);
			FileWriter w = new FileWriter( "./serialized_gray_scale.txt", true);
			w.write("Time:" + time + "\n");
			w.close();
			
			for( int j = 0 ; j < net_scout.Results.length ; j++){
				if( net_scout.Results[j] != null){
					System.out.println( net_scout.Results[j].toString() );
				}
			}
		
			DecimalFormat df = new DecimalFormat("###,###,###.##"); 
			String diagraph = "digraph G {\n";
			for( int j = 0 ; j < net_scout.Results.length ; j++){
				for( int k = 0; k < net_scout.Results[j].Latency.length; k++){
					if( j == k ){
						continue;
					}
					diagraph += "\t" + net_scout.Results[j].Hostname.replace("-", "").replace(".", "")
									 + " -> " 
									 + net_scout.Results[j].NeighborHosts[k].replace("-", "").replace(".", "") 
					            + "[label=\"L:"+ df.format(net_scout.Results[j].Latency[k]) + "ms" 
					            + "\\nB:" + df.format(net_scout.Results[j].Bandwidth[k]) + "Mbs"
					            + "\"]; \n";
				}
			}
			for( int j = 0 ; j < net_scout.Results.length ; j++){
				diagraph += "\t" + net_scout.Results[j].Hostname.replace("-", "").replace(".", "")
							+ " [label=\""
							+ net_scout.Results[j].Hostname + "\\n"
							+ df.format(net_scout.Results[j].Flops) + " MFlops"
					            + "\"]; \n";
				
			}
			diagraph += "}\n";
			FileWriter graph = new FileWriter( "graph.txt");
			graph.write(diagraph);
			graph.close();
			
			/*
			 * digraph G {
				 coitgrid01 -> coitgrid02 [label="100mbs"];
				 coitgrid02 -> coitgrid01 [label="55mbs"];
				 coitgrid01 -> coitgrid03 [label="20mbs"];
				  coitgrid03 -> coitgrid02 [label="44mbs"];
				  coitgrid01 [label="coit-grid01\n1TFlop"];
				}
			*/
			
			
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
