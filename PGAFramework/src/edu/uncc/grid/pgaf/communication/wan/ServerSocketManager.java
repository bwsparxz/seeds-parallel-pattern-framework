/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.communication.wan;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionChangeListener;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;


import net.jxta.pipe.PipeID;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;
/**
 * This class manages a server socket, the class is mostly being used by 
 * the DataSinkSource class at the moment.  
 * The class has a code inefficiency, there should only be one JxtaServerSocket
 * and multiple socket can be spawn from that one.  But, we will use this code for
 * now.  In the future, we should test using a single PipeID to be able to get 
 * multiple sockets out of the Master node.
 * @author jfvillal
 *
 */
public class ServerSocketManager extends SocketManager{
	//JxtaServerSocket JxtaSocket;
	Socket Connection;
	/**
	 * PipeStablished is used to return if the pipe has being stablished or not
	 * the function accept() can take a few seconds to be resolved and using the
	 * ServerSocketManager without checking PipeStablished can create exceptions
	 */
	//private boolean PipeStablished;
	//private boolean SentUserInitClass;  //to returned if the User's class has being sent

	public ServerSocketManager( String parent_thread_name, Socket socket, String pipe_id, Long data_id
			, ConnectionEstablishedListener listener, RawByteEncoder enc) throws IOException, ClassNotFoundException {
		super(parent_thread_name, enc );
		//PipeStablished = false;
		//SentUserInitClass = false;
		if( socket != null){
			Connection = socket;
			OutputS = Connection.getOutputStream();
			ObjectOutputStream out_stream  = new ObjectOutputStream( OutputS );
			 
			//set dependency id as null since we will know the id of this connection once the client responds.
			HandShaker d = new HandShaker(
					 new SlimJxtaID( CommunicationConstants.COMM_ID_ACC,Node.getPID())
					 , pipe_id
					 , data_id
					 , null
					 , null);
			 
			Node.getLog().log(Level.FINE, "ServerSocketManager: first write " + Node.PID);
			out_stream.writeObject(d);
			out_stream.flush();
			
			InputS = Connection.getInputStream();
			ObjectInputStream in_stream = new ObjectInputStream( InputS);
			in_stream = new ObjectInputStream( InputS);
			Node.getLog().log(Level.FINE, "ServerSocketManager: first read " + Node.PID);
			d = (HandShaker) in_stream.readObject();
			 
			this.HRemotePeerID = d.getHRemotePeerID();
			this.RemoteNodeConnectionPipeID = d.getRemoteCommPipeID();
			this.RemoteNodeConnectionDataID = d.getRemoteDataID();
			this.UniqueDependencyID = d.getUniqueDependencyID();
			this.DependencyID = d.getDependencyID();
			 
			if(  listener != null){
				//if true add to the list, else this manager dependency id is not hosted
				//by this dispatcher.
				if( listener.onConnectionEstablished( this ) ){
						
					out_stream.writeObject(CommunicationConstants.CONNECTION_OK);
						
					this.Bound = true;
				}else{
					out_stream.writeObject(CommunicationConstants.CONNECTION_REFUSE);
					out_stream.close();
					socket.close();
					this.Bound = false;
				}
					
			}else{
				//add code here to complete data-flow handshake
				out_stream.writeObject(CommunicationConstants.CONNECTION_OK);
				this.Bound = true;		
			}
			if(Bound){
				this.ThreadName = parent_thread_name + ":" + DependencyID +":" ;
				 this.runThreads();
			}
		}else{
			throw new IOException( "socket is null ");
		}
	}
	/**
	 * 
	 * This will close the connection imediately,  make sure you call hasSentData() before calling 
	 * this method to make sure all the data has been dispatched.
	 * 
	 */
	public void close() throws IOException, InterruptedException{
		if(Bound){
			
			
			super.close();
			Connection.close();
		}
	}
	
/*
	public synchronized boolean hasSentUserInitClass(){
		return SentUserInitClass;
	}
	public synchronized void setSentUserInitClass(boolean bol){
		SentUserInitClass = bol;
	}
*/
}
