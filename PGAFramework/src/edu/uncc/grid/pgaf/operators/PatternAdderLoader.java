package edu.uncc.grid.pgaf.operators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.logging.Level;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.communication.NATNotSupportedException;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

public class PatternAdderLoader extends PatternLoader {
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Data Deploy(DataMap input) {

		try {
			this.OTemplate.Configure((DataMap<String,Serializable>)input);
			
			/**coeficient has been implemented here*/
			
			boolean done = false;
			ModuleAdder a = (ModuleAdder) ((PatternAdderTemplate)OTemplate).getUserModule();
			
			//create communication lines
			PipeID child_pattern_id_one = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, this.PatternID.toString().getBytes());
			Node.getLog().log(Level.FINEST, "new chind pattern id one: " + child_pattern_id_one.toString());
			Communicator comm1 = new Communicator(Framework, child_pattern_id_one, ((PatternAdderTemplate)OTemplate).FirstOperandAdvancedTemplate.getCommunicationID());
			
			
			//create communication lines
			//Note: only two more id's can be derived from the original jxta id.
			byte[] bytes = OperatorModule.sumByteArrays(PatternID.toString().getBytes(), child_pattern_id_one.toString().getBytes());
			PipeID child_pattern_id_two = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, bytes);
			Node.getLog().log(Level.FINEST, "new chind pattern id two: " + child_pattern_id_two.toString());
			Communicator comm2 = new Communicator(Framework, child_pattern_id_two, ((PatternAdderTemplate)OTemplate).SecondOperandAdvancedTemplate.getCommunicationID());

			Node.getLog().log(Level.FINEST,  Node.getLogFolder() + "/operator_stats."
					+ ((PatternAdderTemplate)OTemplate).FirstOperandAdvancedTemplate.getCommunicationID() +".txt");
			
			FileWriter io = new FileWriter (  Node.getLogFolder() + "/operator_stats."
					+ ((PatternAdderTemplate)OTemplate).FirstOperandAdvancedTemplate.getCommunicationID() +".txt" );
			BufferedWriter write = new BufferedWriter(io);
			
			/**
			 * This is the main computation loop for the pattern adder operator.  Seeds will call each of the pattern for a number
			 * of iteration specified by the factors included in he pattern adder operator's arguments.
			 */
			while( ! done ){	

				long time = System.nanoTime();
				
				for( int i = 0; i < a.getFirstOperandCoeficient(); i++){
					//long seg = ((PatternAdderTemplate)OTemplate).FirstOperandAdvancedTemplate.getCommunicationID();
					done = ((PatternAdderTemplate)OTemplate).FirstOperandAdvancedTemplate.ComputeSide(comm1);
					if( done ){
						break;
					}
				}
				
				for( int i = 0; i < a.getSecondOperandCoeficient(); i++){
					done = ((PatternAdderTemplate)OTemplate).SecondOperandAdvancedTemplate.ComputeSide(comm2);
					if( done ){
						break;
					}
				}	
				
				long stop = System.nanoTime() - time;
				write.append("(nano) time is : " + stop + "\n");
			}
			write.close();
			//while ( ! ((PatternAdderTemplate)OTemplate).FirstOperandAdvancedTemplate.ComputeSide(comm1) ) ;
			//while ( ! ((PatternAdderTemplate)OTemplate).SecondOperandAdvancedTemplate.ComputeSide(comm2) ) ;
			
			comm1.hasSent();//wait for all sent data to be sent before closing the connections
			comm2.hasSent();
			comm1.close();
			comm2.close();
			//return done;
			
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
		} catch (NATNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return input;
	}

	@Override
	public DataMap DiffuseDataUnit(int segment) {
		return ((ModuleAdder) this.OTemplate.getUserModule())
						.getFirstLoaderMod(this.PatternID)
						.DiffuseDataUnit(segment);
	}

	@Override
	public void GatherDataUnit(int segment, Data dat) {
		((ModuleAdder) this.OTemplate.getUserModule())
						.getFirstLoaderMod(this.PatternID)
						.GatherDataUnit(segment, dat);
	}

	@Override
	public int getDataUnitCount() {
		return ((ModuleAdder) this.OTemplate.getUserModule())
					.getFirstLoaderMod(this.PatternID)
					.getDataUnitCount();
	}

	@Override
	public boolean hasComm() {
		//TODO a hard coded true value should not happen.  this class should get the answer from 
		//the second OTemplate in the pattern adder operator.
		return true;
	}

	@Override
	public Communicator getComm() throws InterruptedException, IOException,
			ClassNotFoundException, NoPortAvailableToOpenException, NATNotSupportedException {
		PipeID child_pattern_id_one = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, this.PatternID.toString().getBytes());
		byte[] bytes = OperatorModule.sumByteArrays(PatternID.toString().getBytes(), child_pattern_id_one.toString().getBytes());
		PipeID child_pattern_id_two = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, bytes);
		Communicator comm2 = new Communicator(Framework, child_pattern_id_two
				, -1 ); 
		/*
		 * this is a pratical solution to a problem.  the communicator should return the communicator for the sink/source node
		 * At this point, I don'e expect the client/workers to use this method.
		 * 
		 * Also, note that only the second operator is used for a sink/source for a data flow.  The first operator is ignored.
		 * This can change in the future, but for now only the second operator can be used for a data flow 
		 * To be clear on terminology, a synchronouse pattern has a source and sink, but is is only used to distribute the 
		 * initial data and gather the final data.  The workpool and pipeline has a sinck and source that send data througout 
		 * the computation.  This is what we refer to as a data flow.  
		 * 
		 * So, if a pattern has a data_flow, it should return true in hasComm() and its communicator will be created by this 
		 * method.  This method is helpfull to enable the pattern adder operator to work for interactivity controls and 
		 * for visualization.
		 * 
		 */
		//((PatternAdderTemplate)OTemplate).SecondOperandAdvancedTemplate.getCommunicationID());
		return comm2;
	}

	@Override
	public boolean instantiateSourceSink() {
		ModuleAdder module_adder = ((ModuleAdder) this.OTemplate.getUserModule());
		PatternLoader loader = module_adder.getSecondLoaderMod(this.PatternID);
		
		
		
		
		
		return loader.instantiateSourceSink();
	}

	@Override
	public void initializeModule(String[] args) {
		((ModuleAdder) this.OTemplate.getUserModule())
			.getFirstLoaderMod(this.PatternID)
			.initializeModule(args);
	}
	
	
	

}
