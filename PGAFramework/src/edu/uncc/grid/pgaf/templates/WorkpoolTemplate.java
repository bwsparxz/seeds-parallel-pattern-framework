/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * This file is part of PGAFramework.
 *
 * 
 *  
 */
package edu.uncc.grid.pgaf.templates;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeClient;
import edu.uncc.grid.pgaf.communication.MultiModePipeDispatcher;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.shared.ConnectionHibernatedException;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataInstructionContainer;
import edu.uncc.grid.pgaf.interfaces.advanced.Template;
import edu.uncc.grid.pgaf.interfaces.advanced.UnorderedTemplate;
import edu.uncc.grid.pgaf.interfaces.basic.Workpool;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
/**
 * The Template.ShutdownOrder should be used if there is a long loop or a piece of code where failure is more prone, like file access
 * and network access.  This is used as a friendly form to let the program know  the network is shutting down.  
 * Later, there will be forms to force the action.
 * @author jfvillal
 *
 */
public class WorkpoolTemplate extends UnorderedTemplate {
	public WorkpoolTemplate(Node n) {
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
				
				m_manager = MultiModePipeClient.getClientConnection(Network, 0L, pattern_id, my_connection.toString(), my_segment, null, null,null, 30000/*10 seconds timeout*/);
				
				while( !m_manager.isBound() && !ShutdownOrder ){
					//System.out.println( Thread.currentThread().getId() + " Socket Not Bounded... Waiting pipe_id " + m_manager.getHashPipeId().toString() 
					//		+ " peer id " + Node.PID.toURI().toString());
					Thread.sleep(50);
				}
				Workpool work_pool = (Workpool) this.getUserModule();
				work_pool.setFramework(this.Network);
				
				Node.getLog().log(Level.FINEST, "Got workpool app " + work_pool.getClass().getName() );
				//Enter code to run user's application
				boolean job_done = false;
				
				int jobs_done = 0;
				while( !job_done/*Socket not shutdown remotely*/ && !ShutdownOrder ){	
					try {
						// get Data Object from socket
						//Node.getLog().log(Level.FINEST, "Wait on Object ..." );
						Serializable trans = m_manager.takeRecvingObject();
						if( trans instanceof DataInstructionContainer ){
							DataInstructionContainer data = (DataInstructionContainer) trans;
							if( data.getInstruction() == Types.DataInstruction.JOBDONE){
								//System.out.println( "got End of job  Signal");
								job_done = true;
								break;
							}
						}else{
							Data data;
							data = (Data) trans;
							int segment = data.getSegment();
							//give it to the user's application Compute()
							Data output = work_pool.Compute(data);
							output.setSegment(segment);
							//get the resulting object and give it to the SocketManager	
							while( m_manager.isSendFull()){
								Thread.sleep(40);
							}
							m_manager.SendObject(output);
							
							//Log(Thread.currentThread().getId() + " proccessed one block of data " + segment);
							++jobs_done;
							output = null;
						}
					} catch (InterruptedException e) {
						Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
					} catch (IOException e) {
						Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
					} 
				}  
				while( m_manager.hasSendData() ){	
					Node.getLog().log(Level.FINEST, " wait for send data" );
					try{
						Thread.sleep(20);
					}catch( InterruptedException e){
					}
				}
				Node.getLog().log(Level.FINE, " WorkpoolTemplate:ClientSide: " + Thread.currentThread().getId() 
						+ " got " + jobs_done + " jobs done " + " id " + pattern_id.toString());
				//Log("Shutting Down pipe...");
				m_manager.close();
				//Log("Exiting Workpool Template");
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
		} 
		return true;
	}
	
	/**
	 * Runs the sink part of the workpool algorithm.
	 * @author jfvillal
	 *
	 */
	public static class GatherThread extends Thread{
		public List<ConnectionManager> Lst;
		Workpool WP;
		public GatherThread(List<ConnectionManager> lst, Workpool w){
			Lst = lst;
			WP = w;
		}
		@Override
		public void run(){	
			int jobs_completed = 0;
			boolean test =  jobs_completed < WP.getDataCount() && !Template.ShutdownOrder;
			while( jobs_completed < WP.getDataCount() && !Template.ShutdownOrder){
				synchronized( Lst){
					//for( ConnectionManager manager: Lst ){
					Iterator<ConnectionManager> it = Lst.iterator();
					while( it.hasNext()){
						ConnectionManager manager = it.next();
						try {
							//ConnectionManager manager = (ConnectionManager) it.next();
							//ClientSocketManager manager = it.next();
							if( manager.hasRecvData() ){
								Data output;
								output = (Data) manager.takeRecvingObject();
								WP.GatherData(output.getSegment() , output);
								//Node.getLog().log(Level.FINEST, " Got One of  : " + jobs_completed + " total: " + WP.getDataCount() + "\n ");
								//Log( " Got : " + jobs_completed );
								++jobs_completed;
							}
						} catch (InterruptedException e) {
							Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
						}
					}
				}
			}
			
		}
	}
	
	
	@Override
	public void ServerSide(PipeID pattern_id) {
		try{
			Workpool UsersWorkpool = (Workpool) this.UserModule;
			UsersWorkpool.setFramework(Network);
			/*This thread will listen for more workers that want to joing the job
			 * at any time during the computation. */
			List<ConnectionManager> lst = Collections.synchronizedList(new ArrayList<ConnectionManager>());
			MultiModePipeDispatcher m_dispatcher = new MultiModePipeDispatcher(Network, "master_thread", 0L, lst, pattern_id, null);
			GatherThread gather_thread = new GatherThread( lst, UsersWorkpool );
			gather_thread.start();
			int segment = 0;				//segment: data chunk from the user's app
			long jobs_completed = 0;		//segments that have being processed and returned
			boolean sending_done = false; 	//segments that have being sent.
			//send data and process loop
			//Log( "jobs_completed " + jobs_completed + " getDataCount(): " + UsersWorkpool.getDataCount());
			while( segment < UsersWorkpool.getDataCount() && !Template.ShutdownOrder){
				//only allow additions to the list of sockets at the end of each cycle
				synchronized( lst){
					//start thread to get connection to each of the worker nodes
					//this for loop creates new sockets for the pipes that have being 
					//added from discoveryEvent()
					//i=moving_start to make it more efficient sinde the sockets that get
					//Connected are < moving_start
					Iterator<ConnectionManager> it = lst.iterator();
					while( it.hasNext()){
						ConnectionManager manager = (ConnectionManager) it.next();
						if( manager.isBound() && !manager.isSendFull() ){
							//get segment from the user's application
							try{
								Data input = UsersWorkpool.DiffuseData(segment);
								//input.setInstruction(Types.DataInstruction.DATACONTAINER);
								input.setSegment(segment);
								//send it
								manager.SendObject(input);
								input = null;
								//Log( " Sending segment : " + segment );
								++segment;
							}catch( InterruptedException e){
								Node.getLog().log(Level.FINE, "Interrupted");
							}catch( Exception e){
								Node.getLog().log(Level.FINE,"When working on template's DiffuseData() : " + Node.getStringFromErrorStack(e));
							}
						}
						//if we are done.  let the recurrent part of the algorithm know.
						if( segment >= UsersWorkpool.getDataCount() ){
							sending_done = true;
							break;
						}	
					}
				}
			}//end of main while loop
			
			try {
				m_dispatcher.stopDispatcher();
			} catch (InterruptedException e) {
				Node.getLog().log(Level.FINEST, Node.getStringFromErrorStack(e));
				e.printStackTrace();
			}
			
			//tell the workes the job is done
			DataInstructionContainer dat = new DataInstructionContainer();
			dat.setInstruction(Types.DataInstruction.JOBDONE);
			synchronized( lst){
				for( ConnectionManager ot: lst ){
					try {
						ot.SendObject(dat);
					} catch (InterruptedException e) {
						
						Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
					} 
				}
			}
			

			//joing gather thread.
			gather_thread.join();
			m_dispatcher.close();
		
		}catch(IOException e ){
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (InterruptedException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (ClassNotFoundException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		} catch (NoPortAvailableToOpenException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
		}
	}
	@Override
	public String getSuportedInterface() {
		return Workpool.class.getName();
	}
	/*public void Log( String str ){
		//RoutineAdvertPublisherQuerier.addtoDebugErrorMessage(str);
		//RemoteLogger.printToObserver(str);
		Node.getLog().log(Level.FINEST, "\n" + str);
	}*/
	@Override
	public void FinalizeObject() {
		// TODO Auto-generated method stub
		
	}
}
