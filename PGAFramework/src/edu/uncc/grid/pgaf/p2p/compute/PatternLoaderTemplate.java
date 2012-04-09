/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 *  
 *  Other libraries and code used.
 *  
 *  This framework also used code and libraries from the Java
 *  Cog Kit project.  http://www.cogkit.org
 *  
 *  This product includes software developed by Sun Microsystems, Inc. 
 *  for JXTA(TM) technology.
 *  
 *  Also, code from UPNPLib is used
 *  
 *  And some extremely modified code from Practical JXTA is used.  
 *  This product includes software developed by DawningStreams, Inc.    
 *  
 */
package edu.uncc.grid.pgaf.p2p.compute;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeClient;
import edu.uncc.grid.pgaf.communication.MultiModePipeDispatcher;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.advanced.UnorderedTemplate;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
/**
 * This Pattern is used to deploy other patterns.  The pipeline, stencil, and graph patterns all need some type of initialization
 * data to be set on each node.  It could be the stage number, the initial matrixl or list of values for an stenci, or it can be 
 * other data.  The deployment face cannot be part of the patterns for two reason.  The first is that it increases code replication
 * and reduces code reuse.  The second one, and most important towards the research is that the operator idea requires a simplified
 * communication pattern, one that does not include the deployment and undeployment faces.
 * @author jfvillal
 *
 */
/*
 * This pattern should allocate a data object to each node.  It should avoid allocating multiple data units to the same node.
 * We will assume that there are enough nodes that can perform the task, and round robing will not be needed.
 */
public class PatternLoaderTemplate extends UnorderedTemplate {
	public PatternLoaderTemplate(Node n) {
		super(n);
		//RemoteLogger.printToObserver("started");
	}
	@Override
	public boolean ClientSide(PipeID pattern_id) {
		try {
				Node.getLog().log(Level.FINEST, "Creating Socket Connection..." );
				ConnectionManager m_manager;
			
				PipeID my_connection = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
				Long my_segment = CommunicationConstants.DYNAMIC; //unorder
				
				m_manager = MultiModePipeClient.getClientConnection(Network, 0L, pattern_id, my_connection.toString(), my_segment, null, null, null,30000/*10 seconds timeout*/);
				
				while( !m_manager.isBound() && !ShutdownOrder ){
					System.out.println( Thread.currentThread().getId() + " Socket Not Bounded... Waiting pipe_id " + m_manager.getHashPipeId().toString() 
							+ " peer id " + Node.PID.toURI().toString());
					Thread.sleep(500);
				}
				PatternLoader loader = (PatternLoader) this.getUserModule();
				loader.setFramework(this.Network);
				//jan 13 changed so that the client side only receives one job.
					
				try {
					// get Data Object from socket
					
					Serializable trans = m_manager.takeRecvingObject();
					DataMap data = (DataMap) trans;
					
					if( data.getControl() != Types.DataControl.NO_WORK_FOR_YOU ){
						int segment = data.getSegment();
						//give it to the user's application Compute()
						Data output = loader.Deploy(data);						
						output.setSegment(segment);
						//get the resulting object and give it to the SocketManager
						m_manager.SendObject(output);
						Log(Thread.currentThread().getId() + " Done running work unit " + segment);
						
					}else{
						
						m_manager.SendObject( data );
						
						//System.out.println("no work for me");//else go back to idling
					}
				} catch (InterruptedException e) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
				} catch (IOException e) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
				}
				while( m_manager.hasSendData() ){
					Log( " wait for send data" );
					try{
						Thread.sleep(400);
					}catch( InterruptedException e){
						
					}
				}
				Thread.sleep(300);
				m_manager.close();
				Log(" SHUTTING DOWN" );
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
		} catch (Exception e) {
			System.out.println(" I cannot work  in these conditions");
			e.printStackTrace();
		}
		return true;
	}
	@Override
	public void ServerSide(PipeID pattern_id) {
		/**this method also has to call the sink-source from the pattern template
		 * 
		 */
		Thread.currentThread().setName("PatternLoaderThread");
		try{
			PatternLoader DPattern = (PatternLoader) this.UserModule;
			/*This thread will listen for more workers that want to joing the job
			 * at any time during the computation. */
			List<ConnectionManager> lst = Collections.synchronizedList(new ArrayList<ConnectionManager>());
			MultiModePipeDispatcher m_dispatcher = new MultiModePipeDispatcher(Network,Thread.currentThread().getName() , 0L, lst, pattern_id, null);
			/**
			 * All the exception within the next try-catch statement get sent to the user.
			 * We assume this exceptions are caused by the user's application, and therefore
			 * will be useful to troubleshoot the program.
			 */
			//try{
			//Log("Hello from DataSourceSink " + Node.PID);
			int segment = 0;				//segment: data chunk from the user's app
			long jobs_completed = 0;		//segments that have being processed and returned
			//send data and process loop
			Log( " Jobs_completed " + jobs_completed + " getDataCount(): " + DPattern.getDataUnitCount());
			Map<String, Boolean> nodes_with_data_unit = new HashMap<String, Boolean>();
			/*
			 * Distribute job units
			 */
			while( segment  < DPattern.getDataUnitCount() && !ShutdownOrder){
				//only allow additions to the list at the end of each cycle
				synchronized( lst){
					for( ConnectionManager manager : lst){
						//only one data unit per node
						if( nodes_with_data_unit.get(manager.getRemoteNodeConnectionPipeID()) == null){
							//ClientSocketManager manager = it.next();
							//if there are exception, let the Observer know about it and skip that manager
							//if the manager is not too busy, send another piece of work
							
							if( segment >= DPattern.getDataUnitCount() ){ //if we are out of work units break
								break;
							}	
							if( manager.isBound() ){
								//get segment from the user's application
								try{
									
									Data input = DPattern.DiffuseDataUnit(segment);
									//input.setInstruction(Types.DataInstruction.DATACONTAINER);
									input.setSegment(segment);
									input.setControl(Types.DataControl.INSTRUCTION_DATA);
									//send it
									
									manager.SendObject(input);
									input = null;
									Log( " Sending segment : " + segment );
									nodes_with_data_unit.put(manager.getRemoteNodeConnectionPipeID(), true);
									++segment;
									
								}catch( InterruptedException e){
									Node.getLog().log(Level.FINE, "Interrupted");
								}catch( Exception e){
									Node.getLog().log(Level.FINE, Node.getStringFromErrorStack(e));
									//RemoteLogger.printToObserver("When working on template's DiffuseData() : " + Node.getStringFromErrorStack(e));
								}
							}
						}
					}
				}
				Node.getLog().log(Level.FINE, "wait while more nodes connect to me.  Segment so far: " + segment + " Exptected total: " + DPattern.getDataUnitCount());
				try {
					if( segment < DPattern.getDataUnitCount() ){
						//if we still need to wait for more nodes to come online
						Thread.sleep(500);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			/*
			 * Stop listening for new connection.  And let the last few nodes know that we don't have work for them.
			 */
			m_dispatcher.StopListeningForNewConnections();
			synchronized( lst){
				for( ConnectionManager manager : lst){
					//only one data unit per node
					if( nodes_with_data_unit.get(manager.getRemoteNodeConnectionPipeID()) == null){
						//if there are exception, let the Observer know about it and skip that manager
						//if the manager is not too busy, send another piece of work
						if( manager.isBound() ){
							//get segment from the user's application
							try{
								DataMap done = new DataMap();
								done.setControl(Types.DataControl.NO_WORK_FOR_YOU);		
								manager.SendObject(done);
								Log( " Sending no more work unit:" + segment );
							}catch( InterruptedException e){
								Node.getLog().log(Level.FINE, "Interrupted");
							}catch( Exception e){
								Node.getLog().log(Level.FINE, Node.getStringFromErrorStack(e));
								//RemoteLogger.printToObserver("When working on template's DiffuseData() : " + Node.getStringFromErrorStack(e));
							}
						}
					}
				}
			}
			
			/*
			 * 
			 * 
			 * create an organized communicaton environment for the data_sink_source
			 * 
			 * 
			 * 
			 */
			//A child pattern id is derived from the parent pipe_id, that we I can create many more pattern id's without
			//having to have then sychronize on a common id.
			if( DPattern.instantiateSourceSink() ){ //if true, instantiates the source and sink for the OrderedPattern.
				/*
				 * This is old code.  The new interfaces to request the communicator from the pattern loader hopefully makes
				 * it more clear who is in control of the communicator.
				 */
				Communicator comm = null;
				if( DPattern.hasComm() ){
					comm = DPattern.getComm();
				}else{
					PipeID child_pattern_id = IDFactory.newPipeID(
																PeerGroupID.defaultNetPeerGroupID
																, DPattern.PatternID.toString().getBytes()
																);
					comm = new Communicator(this.Network, child_pattern_id , CommunicationConstants.SINKSOURCE_SEGMENT);
				}
				/**
				 * The while loop allows for one pattern to be called multiple times in the pattern adder operator.
				 * This feature was extended for the Reduce pattern.
				 */
				while( !DPattern.OTemplate.SourceSinkSide( comm ) );
				comm.hasSent();
				comm.close ();
			}
			/*
			 * 
			 * Ok the Ordered Template is done, time to wrap things up.
			 * 
			 * */
			
			
			/*
			 * Get the job units and end the patter.  
			 */
			synchronized( lst ){
				for( ConnectionManager manager: lst){
					try {
						Data output;
						output = (Data) manager.takeRecvingObject();
						if( output.getControl() == Types.DataControl.NO_WORK_FOR_YOU){
							Log( " Received a NO_WORK_FOR_YOU acknoledgement");
						}else{
							Log( " Receiving segment : " + output.getSegment()  + " jobs_completed: " + (++jobs_completed));
							DPattern.GatherDataUnit(output.getSegment(), output);
						}
						
					} catch (InterruptedException e) {
						
						Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
					}
				}
			}
			Log( "Data Sink Source is shuting down now ... ");
			m_dispatcher.close();
			
			DPattern.OTemplate.getUserModule().setDone(true);
		
		
			Log( "Going back to idleling");
			
		}catch(IOException e ){
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		}catch (ClassNotFoundException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (NoPortAvailableToOpenException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		}catch (InterruptedException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public String getSuportedInterface() {
		return PatternLoader.class.getName();  
	}
	public void Log( String str ){
		//RoutineAdvertPublisherQuerier.addtoDebugErrorMessage(str);
		//RemoteLogger.printToObserver(str);
		Node.getLog().log(Level.FINEST, "\nThread Name:"+ Thread.currentThread().getName()+" Message: "  + str);
	}
	@Override
	public void FinalizeObject() {
		// TODO Auto-generated method stub
		
	}
}
