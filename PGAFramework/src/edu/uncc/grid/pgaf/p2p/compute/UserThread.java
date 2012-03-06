package edu.uncc.grid.pgaf.p2p.compute;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.id.ID;
import net.jxta.pipe.PipeID;

import edu.uncc.grid.pgaf.communication.MultiModePipeMapper;
import edu.uncc.grid.pgaf.interfaces.advanced.Template;
import edu.uncc.grid.pgaf.interfaces.advanced.UnorderedTemplate;
import edu.uncc.grid.pgaf.p2p.LeafWorker;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.Types.DataFlowRoll;
import edu.uncc.grid.seeds.comm.dependency.DependencyMapper;
/**
 * 
 * Used to manage multiple user threads on one LeafWorker.
 * 
 * This class manages a single node for a pattern.
 * 
 * @author jfvillal
 *
 */
public class UserThread extends Thread {
	/**
	 * A reference to the Woker thread
	 */
	Worker LWorker;
	/**
	 * The Advanced Template
	 */
	UnorderedTemplate AdvancedTemplate;
	/**
	 * The DataFlow roll for this node
	 */
	Types.DataFlowRoll DataFlow;
	/**
	 * The pattern id for the pattern that will be performed
	 */
	PipeID PatternID;
	/**
	 * Done is used to indicate to the framework if this thread is done.  isAlive() was used before, but it 
	 * may not be working fast enough so that it lets the framework create multiple threads with the same
	 * core number.
	 */
	boolean ProcessDone;
	boolean PatternIsDone;
	
	public UserThread( Worker w, UnorderedTemplate adv_temp, Types.DataFlowRoll data_flow, PipeID pattern_id){
		LWorker = w;
		AdvancedTemplate = adv_temp;
		DataFlow = data_flow;
		PatternID = pattern_id;
		ProcessDone = false;
		
	}
	@Override
	public void run() {
		try{
			switch( DataFlow){
				case COMPUTE:
					Node.getLog().log(Level.FINE, "LEAF Giving control to Template ");
					PatternIsDone = AdvancedTemplate.ClientSide(PatternID);
					AdvancedTemplate.getUserModule().setDone(true);
					Node.getLog().log(Level.FINE, "LEAF Getting back control from Template ");
					
				break;
				case SINK_SOURCE:
					Node.getLog().log(Level.FINE, " Running as DATASINKSOURCE");
					AdvancedTemplate.ServerSide(PatternID);
					AdvancedTemplate.getUserModule().setDone(true);
					Node.getLog().log(Level.FINE, " Getting back control from  DATASINKSOURCE");
					PatternIsDone = true;
				break;	
			}
			//the folowing lines are done if the program exists predictably.
			AdvancedTemplate.FinalizeObject();
			
			//these next two lines where added after 2 days of debuggin using coit-grid01,2,3,4.  
			//BE VERY CAREFULL BEFORE REMING THIS LINES WITH AN ACTUALLY TOUGHT OUT ALGORITHM.
		//	MultiModePipeMapper.initMapper(); //this is debugin code.  the point is that a node in route to hibernation with dependency X
			//DependencyMapper.initDependencyMapper(); //can have a node that is newly online access it because it has dependency x
												//this would create a problem.  The real solution, is to add an algorith that prevents
												//this from happening.  But for now, I am just trying to increase the amount of time
												//tolerance.  Since it will take a few more seconds for the new node to load up with data
												//and the old dependency advertisements may be out of the network.
			
			LWorker.HaveIWorkedOnThisPatternAlready.put(PatternID, true);
			
		}catch( Exception e){
			Node.getLog().log(Level.SEVERE, this.getId() + "while working on template  \n"
					+ this.AdvancedTemplate.getClass().getName() + Node.getStringFromErrorStack(e));
			//RemoteLogger.printToObserver(  "while working on template  \n"
			//		+ this.AdvancedTemplate.getClass().getName() + Node.getStringFromErrorStack(e));
			
		}
		ProcessDone = true;
	}
	public synchronized boolean isProcessDone() {
		return ProcessDone;
	}
	public synchronized void setProcessDone(boolean done) {
		ProcessDone = done;
	}
	public synchronized boolean isPatternIsDone() {
		return PatternIsDone;
	}
	public synchronized void setPatternIsDone(boolean patternIsDone) {
		PatternIsDone = patternIsDone;
	}
	
}
