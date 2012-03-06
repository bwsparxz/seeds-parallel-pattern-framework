package edu.uncc.grid.pgaf.communication.wan.java;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;

import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.communication.wan.ClientSocketManager;
import edu.uncc.grid.pgaf.communication.wan.HandShaker;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

public class JavaClientSocketManager extends  ClientSocketManager{
	Socket JSocket;
	
	public JavaClientSocketManager(String hostname
								, int port 
								, String pipe_id
								, Long data_id
								, PipeID unique_dependency_id
								, HierarchicalDependencyID  dependency_id
								, RawByteEncoder enc) 
								throws IOException, ClassNotFoundException {
		super( Thread.currentThread().getName(), enc  );
		JSocket = null;
		
		/**
		 * the loop  will try to connect multiple times.  this is usefull if we don't know when
		 * the data_sink will start. But is should not be necesary if the data_sink will always
		 * start before the leaf workers
		 */	
		for(int t = 0; t < 8; t++){
			try{
				try{
					Node.getLog().log(Level.WARNING,
						"connecting to " + hostname + ":" + port   + " seg_int_id: "  
						+  data_id + " hierarchical id: " + dependency_id.toString()  );
						
				}catch(NullPointerException e){
					
				}
						//+ " dept_id" + dependency_id != null ? dependency_id.toString() : "null" );
				JSocket = new Socket(hostname, port );
				JSocket.setSendBufferSize(    JavaSocketDispatcher.BUFF );
				JSocket.setReceiveBufferSize( JavaSocketDispatcher.BUFF );
			}catch (SocketTimeoutException e){
				Node.getLog().log(Level.WARNING,"one socket timeout -- ClientSocketManager:Constructor. Socket null ? " + (JSocket == null) );
			}catch (IOException e){
				Node.getLog().log(Level.WARNING,"The connection was refuced " );//+ Node.getStringFromErrorStack(e));
				JSocket = null;
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			if( JSocket != null){
				break;
			}
		}
		if( JSocket != null){
			 /*Start input pipe*/
			Node.getLog().log(Level.FINE, "ClientSocketManager: socket established " + Node.PID);
			//note: the timeout would have to be really big since the dataflow loader keeps one
			//connection open and unused to manage the perceptrons
			//JSocket.setSoTimeout(CommunicationConstants.SOCKET_TIMEOUT);
			//JSocket.setKeepAlive(true);
			//TODO keep alive ?
			
			InputS = JSocket.getInputStream();
			OutputS = JSocket.getOutputStream();
			
			 ObjectInputStream in_stream = new ObjectInputStream( InputS);
			 ObjectOutputStream out_stream = new ObjectOutputStream( OutputS);
			 Node.getLog().log(Level.FINE, "ClientSocketManager: first read " + Node.PID);
			 
			 //System.out.println("available: " + stream.available());
			 
			 HandShaker d = (HandShaker) in_stream.readObject();
			 
			 //System.out.println("available: " + stream.available());
			 
			 this.HRemotePeerID = d.getHRemotePeerID();
			 this.RemoteNodeConnectionPipeID = d.getRemoteCommPipeID();
			 this.RemoteNodeConnectionDataID = d.getRemoteDataID();
		
			 
			 /*Start Output pipe*/
			 
			 out_stream = new ObjectOutputStream( JSocket.getOutputStream());
			 d = new HandShaker(new SlimJxtaID(CommunicationConstants.COMM_ID_ACC,Node.getPID())
			 					, pipe_id
			 					, data_id /*used for mpi-like communication*/
			 					, unique_dependency_id
			 					, dependency_id);
			 
			 Node.getLog().log(Level.FINE, "ClientSocketManager: first write " + Node.PID);
			 out_stream.writeObject(d);
			 out_stream.flush();
			 
			 /**
			  * September 15... explaining to myself...
			  * This next code is needed because all the dependency connection are done with the same
			  * socketsever in order to use just one port for the dependency structure.  But this means
			  * that some dependencies may try to connect o a node that may not be broadcasting the 
			  * needed dependency ID, or a node can try to connect to a node where the dependency id
			  * is obsolete so no longer on that node.  this algorithm gives the server a change to 
			  * reject the connection.
			  */
			 int connected =  (Integer) in_stream.readObject();
			 if( connected == CommunicationConstants.CONNECTION_OK){
				 this.Bound = true;
				 this.ThreadName = Thread.currentThread().getName() + "id:" 
				 					+ (dependency_id == null ? "no_dept" :dependency_id.toString())
				 					+ ":";
				 this.runThreads();
			 }else{
				 this.Bound = false;
			 }
		}else{
			throw new IOException("Socket is null --- ");
		}
	}
	public void waitUntilConected(){
		try {
			while( !this.isConected() ){
					Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public boolean isConected(){
		return JSocket.isBound();
	}
	public void close() throws IOException, InterruptedException{
		if( Bound){
			super.close();
			JSocket.close();
		}
	}
}
