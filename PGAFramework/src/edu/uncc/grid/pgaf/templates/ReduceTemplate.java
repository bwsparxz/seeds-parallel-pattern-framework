package edu.uncc.grid.pgaf.templates;

import java.io.IOException;
import java.io.Serializable;

import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.shared.ConnectionHibernatedException;
import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataInstructionContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.datamodules.ReduceDataState;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.basic.Reduce;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

public class ReduceTemplate extends OrderedTemplate{
	public static final String CELL_COUNT = "reduce_cell_count";
	/**
	 * This pattern can be called by itself, in which case this 
	 * persistent data object is given to the programmer with a null
	 * value.  If it is called in a pattern adder operator, the programmer
	 * receives the persistent data object being used on the other 
	 * pattern.
	 */
	ReduceDataState LocalData;
	public ReduceTemplate(Node n) {
		super(n);
		JobDone = 0;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean ComputeSide(Communicator comm) {
		try {
			Reduce user_mod  = (Reduce) this.UserModule;
			
			Data data = user_mod.WorkerSend(  LocalData );
			++LocalData.IterationCount;
			if( data == null ){
				DataInstructionContainer dat = new DataInstructionContainer();
				dat.setControl(Types.DataControl.INSTRUCTION_JOBDONE);
				dat.setInstruction(Types.DataInstruction.JOBDONE);
				comm.Send( dat, -1L );
				return true;
			}else{
				comm.Send( data, -1L );
				return false;
			}
		} catch ( Exception e) {
			e.printStackTrace();
			return true;
		} 
	}
	int JobDone;
	@Override
	public boolean SourceSinkSide(Communicator comm) {
		/**
		 * Note that this pattern does not have a Gather method.
		 */
		try {
			Reduce user_mod  = (Reduce) this.UserModule;
			/**
			 * Receives the data from every node.
			 */
			for( long i = 0; i < user_mod.getCellCount(); i++ ){
				Data data = (Data)comm.BlockReceive( i );
				if( data.getControl() == Types.DataControl.INSTRUCTION_JOBDONE ){
					++JobDone;
				}else{
					user_mod.ServerReduce( data );
				}
			}
			if( JobDone == user_mod.getCellCount() ){
				return true;
			}else{
				return false;
			}
		} catch ( Exception e) {
			e.printStackTrace();
			return true;
		} 
	}

	@Override
	public void Configure(DataMap<String, Serializable> configuration) {
		DataContainer container = (DataContainer)configuration.get(PatternLoader.INIT_DATA);
		LocalData = (ReduceDataState) container.getPayload();
		this.CommunicationID = container.getSegment();
	}

	@Override
	public Class getLoaderModule() {
		return ReduceLoader.class;
	}

	@Override
	public String getSuportedInterface() {
		return Reduce.class.toString();
	}

	@Override
	public void FinalizeObject() {
		// Add here code to finlaize the pattern 
		//like a destructor in C++
	}

}
