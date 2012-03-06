package edu.uncc.grid.seeds.otemplate.pattern.alltoall;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.basic.CompleteSyncGraph;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

/**
 * this is an all-to-all implementation using the Ordered Template 
 * programming approach, also know as MPI-like because you can use rank id to
 * organize the parallel application.
 * @author jfvillal
 *
 */
public class CompleteSyncGraphTemplate extends OrderedTemplate {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//private int LoopCount;
	AllToAllData LocalData;
	private BufferedWriter PerformanceReportWriter;
	public CompleteSyncGraphTemplate(Node n) {
		super(n);
	//LoopCount = 0;
		// no initiation of user's module stuff can go in this 
		//function, use the method setUserModule to do that.
		PerformanceReportWriter = null;
	}

	@Override
	public boolean ComputeSide(Communicator comm) {
		CompleteSyncGraph module = (CompleteSyncGraph) this.getUserModule();
		
		long set_comm_timer = System.nanoTime();
		
		Serializable dat = LocalData.getSyncData();
		for( long i = 0; i < module.getCellCount(); i++ ){
			//skip myself
			if( i != this.CommunicationID){
				try {
					comm.Send(new DataContainer( (int)this.CommunicationID,dat), i);
				} catch (Exception  e) {
					Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
				}
			}
		}
		List<Serializable> list = new ArrayList<Serializable>();
		for( long i = 0; i < module.getCellCount(); i++){
			if( i != this.CommunicationID){
				try {
					DataContainer con = (DataContainer)comm.BlockReceive(i);
					list.add( con.getPayload()  );
				} catch (Exception  e) {
					Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
				}
			}
		}
		LocalData.setSyncDataList(list);
		//++LoopCount;
		
		long stop_comm_timer = System.nanoTime() - set_comm_timer;
		set_comm_timer = System.nanoTime();
		
		boolean ans = module.OneIterationCompute(LocalData);
		
		long stop_compt_timer = System.nanoTime() - set_comm_timer;
		
		try {
			this.PerformanceReportWriter.append("(nanos)comm: " + stop_comm_timer + ":compute:" + stop_compt_timer + "\n");
		} catch (IOException e) {
			Node.getLog().log(Level.FINEST,Node.getStringFromErrorStack(e));
		}
		
		return ans;
	}

	@Override
	public void Configure(DataMap<String, Serializable> configuration) {
		
		DataContainer container = (DataContainer)configuration.get(PatternLoader.INIT_DATA);
		LocalData = (AllToAllData) container.getPayload();
		this.CommunicationID = container.getSegment();
		
		//performance measurement code
		try {
			FileWriter performance_report_file;
			performance_report_file = new FileWriter ( Node.getLogFolder() + "/CompleteGraph.performance." + this.CommunicationID + ".txt" );
			PerformanceReportWriter = new BufferedWriter(performance_report_file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void SourceSinkSide(Communicator comm) {
		//not in use.
	}

	@Override
	public Class getLoaderModule(){
		return CompleteSyncGraphLoader.class;
	}

	@Override
	public String getSuportedInterface(){
		return CompleteSyncGraph.class.getName();
	}

	@Override
	public void FinalizeObject() {
		try {
			this.PerformanceReportWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
