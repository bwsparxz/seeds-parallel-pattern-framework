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
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

public class PatternAdderLoader extends PatternLoader {
	
	
	
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
	public boolean instantiateSourceSink() {
		return ((ModuleAdder) this.OTemplate.getUserModule())
					.getFirstLoaderMod(this.PatternID)
					.instantiateSourceSink();
	}

	@Override
	public void initializeModule(String[] args) {
		((ModuleAdder) this.OTemplate.getUserModule())
			.getFirstLoaderMod(this.PatternID)
			.initializeModule(args);
	}

}
