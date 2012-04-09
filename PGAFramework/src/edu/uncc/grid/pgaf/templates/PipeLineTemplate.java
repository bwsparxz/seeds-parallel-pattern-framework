package edu.uncc.grid.pgaf.templates;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.communication.NATNotSupportedException;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.shared.ConnectionHibernatedException;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataInstructionContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.basic.PipeLine;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;

/**
 * <p>The pipeline pattern class consist of a SinkSource, and client nodes.  
 * The pipeline is a series of independent steps that are performed one after the other.
 * The task may be dependent on the data order.  The first implementation for this 
 * pattern is for data independently ordered.
 * Comput has an extra argument to allow the framework to reques on of n stages</p>
 * <br>
 * <p>
 * Source -> 0 -> 1 -> ... n -> Sink
 * </p><br>
 * <p>The data is moved from one node to the other.  The user may have different Data objects from
 * one stage to the other, but has to make sure the order outputed by n to be the same as the 
 * object inputed to stage n+1.</p>
 * @author jfvillal
 *
 */
public class PipeLineTemplate extends OrderedTemplate {
	/**
	 * constant used to get the stage from the Map
	 */
	public static final String STAGE_NUMBER = "stage_number";
	public static final String MAX_STAGE = "max_stage";
	
	Integer StageNumber;
	Integer MaxStage;
	public PipeLineTemplate(Node n) {
		super(n);
	}
	
	@Override
	public void Configure(DataMap<String, Serializable> configuration) {
		StageNumber = (Integer)configuration.get(STAGE_NUMBER);
		MaxStage = (Integer)configuration.get(MAX_STAGE);
		this.CommunicationID = StageNumber;
	}
	
	@Override
	public boolean ComputeSide(Communicator comm) {
		boolean job_done = false;
		try {
			Node.getLog().log(Level.FINEST, "Got stage number: " + StageNumber );
			
			///start loop that processe each data packet
			/*												->Coefficient not reached
			 * 						->network still up
			 *		->job not done												*/
			while( !job_done && !ShutdownOrder ){
				//get packet from input
				Data packet = (Data) comm.BlockReceive( (long) StageNumber -1 );
				//process
				if( packet.getControl() == Types.DataControl.INSTRUCTION_JOBDONE){	
					Node.getLog().log(Level.INFO, "got shutdown signl");
					job_done = true;
					//send the message to the others
					if( (StageNumber + 1) == MaxStage){ //send to sink source
						comm.Send(packet, (long) CommunicationConstants.SINKSOURCE_SEGMENT);
					}else{ //send to next in line
						comm.Send(packet, (long) StageNumber + 1);
					}
				}else{
					//send data to the next stage
					int segment = packet.getSegment();
								/*get the users pattern module*/
					Data out = ((PipeLine) this.getUserModule()).Compute(StageNumber, (Data)packet);
					out.setSegment(segment);
					
					if( (StageNumber + 1) == MaxStage){ //send to sink source
						comm.Send(packet, (long) CommunicationConstants.SINKSOURCE_SEGMENT);
					}else{ //send to next in line
						comm.Send(packet, (long) StageNumber + 1);
					}		
				}	
			}
		} catch (IOException e1) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e1));
		} catch (ClassNotFoundException e1) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e1));
		} catch (InterruptedException e1) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e1));
		} catch (TunnelNotAvailableException e1) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e1));
		} catch (CommunicationLinkTimeoutException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (ConnectionHibernatedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NATNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return job_done;
	}

	@Override
	public boolean SourceSinkSide(Communicator comm) {
		try {
			PipeLine UserPipeLine = (PipeLine) this.UserModule;
			long pipe_end = UserPipeLine.getStageCount() - 1;
			//all the stages have being claimed.  lets move on to feed the data
			//while( jobs_completed < UserPipeLine.getDataCount() && !Template.ShutdownOrder){
				//only allow additions to the list at the end of each cycle
			for( int segment = 0; segment < UserPipeLine.getDataCount(); segment++){
				if( ShutdownOrder ){
					break; 
				}
				try{
					Data input = UserPipeLine.DiffuseData(segment);
					input.setSegment(segment);
					comm.Send(input, 0L);	
				}catch( InterruptedException e){
					Node.getLog().log(Level.FINE, "Interrupted");
				}catch( Exception e){
					/*RemoteLogger.printToObserver("When working on template's DiffuseData() : Userclass "
							+ UserPipeLine.getClass().toString() + "\n"
							+ Node.getStringFromErrorStack(e));*/
				}
				//receive non blocking the response data
				comm.Receive(new NonBlockReceiver(){
					@Override
					public void Update(Serializable dat)
							throws InterruptedException {
						PipeLine UserPipeLine = (PipeLine) UserModule;
						Data packet = (Data) dat;
						try{
							UserPipeLine.GatherData(packet.getSegment(), packet);
							System.out.println(" Got one packet " + packet.getControl() );
						}catch(Exception e){
							Node.getLog().log(Level.SEVERE, "While working on template " + UserPipeLine.getClass().toString() +"\n"
									+ Node.getStringFromErrorStack(e)
									);
						}
					}
				}, pipe_end);
			}//end of main while loop
			
			//send end of job data
			DataInstructionContainer dat = new DataInstructionContainer();
			dat.setControl(Types.DataControl.INSTRUCTION_JOBDONE);
			dat.setInstruction(Types.DataInstruction.JOBDONE);
			comm.Send(dat, 0L);
			
			Data d = (Data) comm.BlockReceive(pipe_end);  //get a dump packet to signal the program is done.
			System.out.println(" got end of pipe " + d.getControl());
			
			Node.getLog().log(Level.FINEST, "Finish running the pipeline." );		
		} catch (InterruptedException e) {
			Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
		} catch (IOException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (ClassNotFoundException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (TunnelNotAvailableException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (CommunicationLinkTimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConnectionHibernatedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NATNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	@Override
	public String getSuportedInterface() {
		return PipeLine.class.getName();
	}
	public void Log( String str ){
		Node.getLog().log(Level.FINEST, "\n" + str);
	}

	@Override
	public Class getLoaderModule() {
		return PipeLineLoader.class;
	}

	@Override
	public void FinalizeObject() {
		// TODO Auto-generated method stub
		
	}

	
}
