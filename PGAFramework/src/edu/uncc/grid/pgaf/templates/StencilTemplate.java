package edu.uncc.grid.pgaf.templates;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.basic.Stencil;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

/**
 * 
 * This is an example of a 2D stencil. It does not have any load-balancing
 * techniques in it.  But I have tought of techniques that will finally
 * avoid involving the user programmer into the techniques.
 * 
 * The algorith is state-full.  The user is to be provided with a state
 * for each compute iteration.  the algorithm is "loop parallel", whcih means
 * that the same state data is given to the user programmer's function 
 * several times until an arbitrary number of loops have being done.
 * @author jfvillal
 *
 */

public class StencilTemplate extends OrderedTemplate {
	/**
	 * The data object used to hold the user programmer's cell.  Note that
	 * we will not try to impose a structure on the data object.  This allows
	 * the user programmer to create a matrix, or list, or other tipes of 
	 * data structures.  However, for this example, we do need to constrain
	 * the pattern to synchronize with 4 other neighbors.  Another idea I
	 * will explore March 2010 will avoid this constraint.
	 */
	StencilData LocalData;
	int LoopCount;
	int Segment;
	
	BufferedWriter PerformanceReportWriter;
	
	public StencilTemplate(Node n) {
		super(n);
		LoopCount = 0;
		PerformanceReportWriter = null;
		
	}

	@Override
	public String getSuportedInterface() {
		return Stencil.class.getName();
	}

	@Override
	public boolean ComputeSide(Communicator comm) {
		
		Stencil user_mod = (Stencil)this.UserModule;
		long set_comm_timer = System.nanoTime();
		int my_seg = (int)this.CommunicationID;
		int sqrt = (int) Math.sqrt( (double) user_mod.getCellCount() );
		/*
		 * for a 2x2 block
		 * 
		 * it cound this way
		 *    _  _
		 *   |0||2|
		 *   |1||3| 
		 */
		
		int row = (my_seg / sqrt);
		int col = (my_seg % sqrt);
		
		//this takes some computation, but it is straight forward.
		//note on coordinates:  i am using graphics based coordinates.
		//so top left is 0,0
		
		//get left limiting cells
		if( col != 0) {
			//send left cell
			int neighbor_col = col - 1;
			int neighbor_row = row;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				comm.Send(new DataContainer(Segment, LocalData.getLeft()), (long)neighbor_segment);
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}
		//get right limiting cells
		if( col != sqrt -1){
			//send right cell
			int neighbor_col = col + 1;
			int neighbor_row = row;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				comm.Send(new DataContainer(Segment,LocalData.getRight()), (long)neighbor_segment);
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}
		//get top limiting cells
		if( row != 0){
			//send  top cell
			int neighbor_col = col;
			int neighbor_row = row - 1;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				comm.Send(new DataContainer(Segment, LocalData.getTop()), (long)neighbor_segment);
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}
		//get bottom limitting cells
		if( row != sqrt -1){
			//send  bottom cell
			int neighbor_col = col;
			int neighbor_row = row + 1;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				comm.Send(new DataContainer(Segment, LocalData.getBottom()), (long)neighbor_segment);
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}
		//Ok, so we sent all the information.  now lets wait to get back the information 
		//from the neighbors
		
		if( col != 0) {
			//receive  and receive left cell
			int neighbor_col = col - 1;
			int neighbor_row = row;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				DataContainer con = (DataContainer) comm.BlockReceive((long)neighbor_segment);
				LocalData.setLeft( con.getPayload() );
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}else{
			LocalData.setLeft( null);
		}
		//get right limiting cells
		if( col != sqrt -1 ){
			//receive right cell
			int neighbor_col = col + 1;
			int neighbor_row = row;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				DataContainer con = (DataContainer) comm.BlockReceive((long)neighbor_segment);
				LocalData.setRight( con.getPayload());
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}else{
			LocalData.setRight(null);
		}
		//get top limiting cells
		if( row != 0){
			// receive top cell
			int neighbor_col = col;
			int neighbor_row = row - 1;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				DataContainer con = (DataContainer) comm.BlockReceive((long)neighbor_segment);
				LocalData.setTop(  con.getPayload());
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}else{
			LocalData.setTop( null);
		}
		//get bottom limitting cells
		if( row != sqrt -1){
			//receive bottom cell
			int neighbor_col = col;
			int neighbor_row = row + 1;
			int neighbor_segment = neighbor_row * sqrt + neighbor_col;
			try {
				DataContainer con = (DataContainer) comm.BlockReceive((long)neighbor_segment);
				LocalData.setBottom(  con.getPayload());
			} catch (Exception  e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			}
		}else{
			LocalData.setBottom( null );
		}
		long stop_comm_timer = System.nanoTime() - set_comm_timer;
		set_comm_timer = System.nanoTime();
		boolean ans = user_mod.OneIterationCompute( LocalData );
		long stop_compt_timer = System.nanoTime() - set_comm_timer;
		
		try {
			this.PerformanceReportWriter.append("(nanos) comm: " + stop_comm_timer + ":compute:" + stop_compt_timer + "\n");
		} catch (IOException e) {
			Node.getLog().log(Level.FINEST,Node.getStringFromErrorStack(e));
		}
		//Done computing.  ready for the next round.
		
		//return !(++LoopCount < user_mod.getIterations() ); //the user could use this to tell the framework, this cell is done.
			//but it won't be implemented for a while.
		return ans;
	}

	@Override
	public void SourceSinkSide(Communicator comm) {
		// not in used, the Loader pattern takes care of this.
		
	}

	@Override
	public Class getLoaderModule() {
		return StencilLoader.class;
	}

	@Override
	public void Configure(DataMap<String, Serializable> configuration) {
		DataContainer container = (DataContainer)configuration.get(PatternLoader.INIT_DATA);
		LocalData = (StencilData) container.getPayload();
		this.CommunicationID = container.getSegment();
		Segment = container.getSegment();
		
		//performance measurement code
		try {
			FileWriter performance_report_file;
			performance_report_file = new FileWriter ( Node.getLogFolder() + "/Stencil.performance." + container.getSegment() + ".txt" );
			PerformanceReportWriter = new BufferedWriter(performance_report_file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
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
