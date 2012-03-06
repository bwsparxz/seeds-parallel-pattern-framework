package edu.uncc.grid.pgaf.p2p.compute;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.datamodules.DataObject;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.operators.PatternAdderTemplate;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.templates.PipeLineTemplate;
/**
 * This pattern is used by the advanced layer user and the expert layer user.
 * The pattern is used to deploy all other patterns.  The advanced user should extend this class to load a new OrderedTemplate.
 * <br>
 * OrderedTemplate -> PatternLoader -> PatternLoaderTemplate -> Framework.
 * <br>
 * The framework will traverse the list of classes until it hits the UnorderedPatterns, and then it runs that class, which by that 
 * point contains an instance of all the classes that were traverse to get to it. 
 *<br>
 *An example using the pipeline pattern.
 *<br>
 *PipeLine -> PipeLineTemplate -> PatternLoder -> PatternLoaderTemplate -> Framework.
 * 
 * This scheme allows for the use of operators, which would update the OrderedTemplate, and the PatternLoader to be implemented into 
 * an existing patterns.
 * 
 * All these tools are to be used by the advanced user.  The basic user would not see a difference between a framework that uses
 * operators and one that does not.
 * 
 * This pattern is where the operator will be implemented
 * @author jfvillal
 *
 */
public abstract class PatternLoader extends BasicLayerInterface{
	public static final String  INIT_DATA = "init_data";
	protected OrderedTemplate OTemplate;
	protected PipeID PatternID;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Data Deploy(DataMap input){
		try {
			this.OTemplate.Configure((DataMap<String,Serializable>)input);
			PipeID child_pattern_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, this.PatternID.toString().getBytes());
			Node.getLog().log(Level.FINER, " Created new patter_id for nested pattern... child_pattern_id: " + child_pattern_id.toString() );
			Communicator comm = new Communicator(Framework, child_pattern_id, OTemplate.getCommunicationID());
			/**
			 * The coeficient will not be implemented yet to keep my head from exploding.
			 * The template will be called until it returns true.  false means there is work to be done.
			 * and true means the work is done.
			 */
			
			FileWriter io = new FileWriter (  Node.getLogFolder() + "/operator_stats."
					+ OTemplate.getCommunicationID() +".txt" );
			BufferedWriter write = new BufferedWriter(io);
			boolean is_done = false;
			while ( !is_done ){ 
				long time = System.nanoTime();
					is_done = OTemplate.ComputeSide(comm) ;
					
				long stop = System.nanoTime() - time;
				write.append("(nano) time is : " + stop + "\n");
			}
			
			write.close();
			comm.hasSent();//wait for all sent data to be sent before closing the connections
			comm.close();
			OTemplate.getUserModule().setDone(true);  //to indicate the pattern is done computing.
			OTemplate.FinalizeObject();
		} catch (InterruptedException e) {
			Node.getLog().log(Level.FINER, Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (IOException e) {
			Node.getLog().log(Level.FINER, Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			Node.getLog().log(Level.FINER, Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (NoPortAvailableToOpenException e) {
			Node.getLog().log(Level.FINER, Node.getStringFromErrorStack(e));
			e.printStackTrace();
		}
		return input;
	}
	/**
	 * This function is used to request the user for chunks of that will be send 
	 * through the network.  The user should inherit class Data into a custom class. 
	 *   For example, MyData.  The class MyData can have any type of object and variable.  
	 *   Be aware that the Data object needs to be serializable so that it can be 
	 *   Transfered over the network.
	 * @param segment
	 * @return
	 */
	public abstract DataMap DiffuseDataUnit(int segment); //TODO could go back to simple Data
	/**
	 * GatherData does the opposite of DiffuseData().  It will return Data objects with
	 * Chunks of the original data processed into a user defined output Data object.
	 * The user is responsible for taking the output and putting it back together into 
	 * the final answer.
	 * @param segment
	 * @param dat
	 */
	public abstract void GatherDataUnit(int segment, Data dat);
	/**
	 * This function should tell the framework what is the total number of pieces in which 
	 * the user decided to divide the Input data.
	 * @return
	 */
	public abstract int getDataUnitCount();
	/**
	 * <p>
	 * This is a communication function between the experte layer and the advanced user.  if
	 * the user needs a source and sink instantiated in the deploying node once the pattern
	 * has being deployed, this function should return true.
	 * </p>
	 * <p>
	 * If the advanced user is deploying something like a stencil where the deployment 
	 * is also used as a source and sink action, then there is no need for source and sinks
	 * to be instantiated.
	 * </p>
	 * @return true if a source-sink needs to be instantiated once the pattern is deployed
	 */
	public abstract boolean instantiateSourceSink();
	
	public String getHostingTemplate(){
		return PatternLoaderTemplate.class.getName();
	}
	public void setOTemplate( OrderedTemplate tmp){
		OTemplate = tmp;
	}
	public void setPatternID( PipeID set){
		PatternID = set;
	}
	
	public boolean isOperator(){
		return false;
	}
	
}
