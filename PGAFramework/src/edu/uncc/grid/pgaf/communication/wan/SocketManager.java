/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *   * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.communication.wan;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.mortbay.http.EOFException;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionChangeListener;
import edu.uncc.grid.pgaf.communication.ConnectionChangedMessage;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataInstructionContainer;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.Types.LinkType;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

/**
 * SocketManager is a super class for ClientSocketManager and 
 * ServerSocketManager.  
 * @author jfvillal
 *
 */
public class SocketManager implements ConnectionManager{
	private BlockingQueue<Serializable> RecvingQueue;
	private BlockingQueue<Serializable> SendingQueue;
	SlimJxtaID HCommPipeID;
	/**
	 * Stores a hash of the remote peer id.  This can be used to map the link to the node
	 */
	protected SlimJxtaID HRemotePeerID;
	/**
	 * This is the data id the remote host is using for this connection. or a flag
	 * if a data id is not being used
	 */
	protected Long RemoteNodeConnectionDataID;
	/**
	 * This is the pipe id the remote node is using to host its own connections.
	 */
	protected String RemoteNodeConnectionPipeID;
	/**
	 * set to true if the connection is established
	 */
	protected boolean Bound;
	/**
	 * PipeId used to identify the Dependency ID for the data flow behavior
	 */
	protected PipeID UniqueDependencyID;
	protected HierarchicalDependencyID DependencyID;
	protected ConnectionChangeListener CCListener;

	/**
	 * NBRecv is a queue of object that are waiting for the next piece of data.
	 * The Queue is used to reduce the number of threads.  The Receiver thread
	 * will immediately dispatch the next piece of data to those NonBlockReceiver
	 * objects that are on the queue.  If the queue is empty the data block
	 * is stored in RecvingQueue.
	 */
	private BlockingQueue<NonBlockReceiver> NBRecv;
	RecverThread R;		//receiver thread object
	SenderThread S;		//sender thread object
	Thread RT;			//thread manager for receiver
	Thread ST;			//thread manager for sender
	/**
	 * stops the receiving thread
	 */
	public volatile boolean RStop;
	/**
	 * stops the sending thread
	 */
	public volatile boolean SStop;
	
	String ExceptionMessage;
	private boolean DataflowSource;
	public InputStream InputS;
	public OutputStream OutputS;
	//public ObjectOutputStream OutStream;
	//public ObjectInputStream InStream;
	
	public String ThreadName;
	/**
	 * Used to encode user's data into byte form that is more efficient
	 */
	RawByteEncoder Encoder = null;
	
	public SocketManager(String thread_name, RawByteEncoder enc ){
		RecvingQueue = new ArrayBlockingQueue<Serializable>(CommunicationConstants.QUEUE_SIZE, true);
		SendingQueue = new ArrayBlockingQueue<Serializable>(CommunicationConstants.QUEUE_SIZE, true);
		NBRecv = new ArrayBlockingQueue<NonBlockReceiver>(CommunicationConstants.QUEUE_SIZE, true);
		RStop = false;
		SStop = false;
		ExceptionMessage = null;
		HRemotePeerID = null;
		Bound = false;
		InputS = null;
		OutputS = null;
		CCListener = null;
		ThreadName = thread_name;
		Encoder = enc;
	}
	/**
	 * returns the received objects in the order they were received.
	 * </br>
	 * it will <b>wait</b> if the queue is empty
	 * </br>
	 * <font color="red">Use nonblockRecvingObject for non-blocking receive</font>
	 * 
	 * @return
	 * @throws InterruptedException 
	 */
	@Override
	public Serializable takeRecvingObject() throws InterruptedException{
		return RecvingQueue.take();
	}
	/**
	 * return one item from the queue, return null if the queue is empty.
	 * @return
	 */
	@Override
	public Serializable pollRecvingObject() {
		return RecvingQueue.poll();
	}
	/**
	 * The nonblockRecvingObject function will return one of the objects in 
	 * RecvingQueue if RecvingQueue is not empty.  If the queue is empty, then
	 * the NonBlockReceiver Object is queue up and the Receiver thread will
	 * wait up for the information.  as soon as the information is available
	 * NonBlockReceiver's Update function is called.
	 * @param rcv
	 * @throws InterruptedException
	 */
	@Override
	public void nonblockRecvingObject( NonBlockReceiver rcv) throws InterruptedException{
		if( ! RecvingQueue.isEmpty() ){
			rcv.Update( RecvingQueue.take() );
		}else{
			NBRecv.put(rcv);
		}
	}

	public void setStop(boolean set){
		RStop = set;
		SStop = set;
	}
	@Override
	public boolean hasRecvData(){
		return !this.RecvingQueue.isEmpty();
	}
	@Override
	public boolean hasSendData(){
		
		return !this.SendingQueue.isEmpty();
	}
	@Override
	public boolean isSendFull() {
		return this.SendingQueue.size() >= CommunicationConstants.QUEUE_SIZE;
	}
	/**
	 * The data object will be placed in a queue to be sent over the socket.  if the 
	 * queue is full, the send will block until the queue gets space.
	 * @param obj
	 * @throws InterruptedException
	 */
	@Override
	public void SendObject(Serializable obj ) throws InterruptedException{
		SendingQueue.put(obj);
	}
	public void  runThreads(){
		R = new RecverThread();
		S = new SenderThread();
		RT = new Thread(R);
		ST = new Thread(S);
		RT.setName( ThreadName + "Recv" );
		ST.setName( ThreadName + "Send" );
		RT.setDaemon(true);
		ST.setDaemon(true);
		RT.start();
		ST.start();
	}	
	/**
	 * close action is to close the socket only, not node will shut down
	 */
	protected final byte CLOSE_ACTION = 10;
	/**
	 * Hibernate Action is to be used to let the dependency know some node will
	 * come back to connect to this dependency.
	 */
	private final byte HIBERNATE_ACTION = 11;
	/**
	 * Default action is used to let normal send/receive continue.
	 */
	private final byte DEFAULT_ACTION = 1;
	/**
	 * This tells the receiver that a stream of bytes will be sent instead 
	 * of a java boject.
	 */
	//private final byte RAWBYTE_ACTION = 5;
	
	
	/**
	 * To be used by a master node to shutdown another node upon closing the socket
	 */
	private final byte NODE_SHUTDOWN_ACTION = 2; 
	
	@Override
	public void close() throws IOException, InterruptedException{
		if(Bound){
		
			Node.getLog().log(Level.INFO, "SOCKETMANAGER -- close() -- closing this socket" );
			//Bound = false;
			SStop = true;
			//wait untill all packets have being sent
			//Node.getLog().log(Level.FINE, " wating for sender to finish close() ");
			//ST.join();
			//ST = null;
			Node.getLog().log(Level.FINE, " informing other end of socket close ");
			RT.join();
			OutputS.close();
			//RT = null;
		
		}
	}

	public RawByteEncoder getRawByteEncoder( ){
		return Encoder;
	}
	//int debugi = 0;
	//int p_control = 0;
	public class RecverThread implements Runnable{
		public void run(){
			try {
				
	            if (InputS != null) {
	            	//int progressive_wait = 1;
	            	ObjectInputStream stream = new ObjectInputStream( InputS );
	                while(!RStop){
	                	
	                	try {
	                		//Node.getLog().log(Level.FINE, "waiting for a Object ------------- available " + InputS.available() );
	                		
	                		
	                		//TODO replace the byte with the use of the Control byte in every data object.
	                		/*
	                		 *  
	                		 * this connection right now works by sending a "control" byte before it sends the object
	                		 * This was done to avoid the use of reflecion.  This should be changed,  we can 
	                		 * take out the control byte and use the control interface you have created for the 
	                		 * tunnel manager.
	                		 * 
	                		 * This contites taking out pipe_control byte
	                		 * and then, the functions that where sending a control byte should be modified so that they don't
	                		 * and instead of the control byte.  They send a data object when one is available, and a 
	                		 * control object when the conection needs to be closed.
	                		 * 
	                		 * This is the second priority after the tunnel manager modification.
	                		 */
	                		byte pipe_control = stream.readByte();
	                		
	                		if(pipe_control == CLOSE_ACTION){
	                			//we should stop receiving, the pipe has being closed.
	                			RStop = true;
	                			break;
	                		}else if( pipe_control == HIBERNATE_ACTION){
	                			ConnectionChangedMessage trans = (ConnectionChangedMessage) stream.readObject();	
	                			if( CCListener != null){
	                				//let the object know we will be comming back
	                				CCListener.onHibernateConnection(trans);
	                				//RecvingQueue.put(trans); //The dependency will can use the object
	                						//as a last resort effort to understand the connection is frozen.
	                				continue;
	                			}
	                		}
	                		
	                		Serializable trans = null;
	                		
	                		//++debugi;
	                		//p_control = pipe_control;
	                		//System.out.println("control" + pipe_control);
	                		
	                		/*
	                		if( pipe_control == RAWBYTE_ACTION){
                				int size = stream.readInt();
	                			byte[] buff = new byte[size];
	                			stream.readFully(buff, 0, size);
	                			//System.out.println(" act : " + act );
	                			trans = buff;
	                		}else */
	                		if( pipe_control == DEFAULT_ACTION ){
	                			trans = (Serializable) stream.readObject();
	                		}else{
	                			//this is custom stream.
	                			if( SocketManager.this.getRawByteEncoder() != null){
	                				trans = SocketManager.this.getRawByteEncoder().fromRawByteStream(pipe_control, stream);
	                			}else{
	                				System.out.println("Danger,  this thing will crash bad ! pipe_control " + pipe_control );
	                			}
	                			if( trans == null){
	                				System.out.println();
	                			}
	                		}
		                	
	                		
		                	//run control code for the data flow classes ... canceled this can be done during hand shake.
		              
		                	
		                	if( !NBRecv.isEmpty() ){
		                		NonBlockReceiver temp = NBRecv.take();
		                		/**
		                		 * update value on the user's data and notify the communicator
		                		 * that the transaction is done
		                		 */
		                		synchronized (temp){
			                		temp.Update(trans);
			                		temp.notify();
		                		}
		                		temp =null;
		                	}else{
		                		RecvingQueue.put(trans);
		                	}
						}catch( EOFException e ){
							e.printStackTrace();
						}catch (ClassCastException e){
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e) );
						} catch (ClassNotFoundException e) {
							Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e) );
						}catch (InterruptedException e) {	
							Node.getLog().log(Level.WARNING, "INTERRUPT IN THE IN_STREAM THREAD \n" + Node.getStringFromErrorStack(e) );
						}catch ( SocketTimeoutException e ){
							//this is trown if the connection is idle for a while, but the connection would still be ok after it.
							Node.getLog().log(Level.WARNING, "INTERRUPT IN THE IN_STREAM THREAD \n" + Node.getStringFromErrorStack(e) );
						}
	                }
	                //close sending thread
		            Bound = false;
					SStop = true;
					//wait untill all packets have being sent, and if sending threads is still active
					//shut it down.
					if(ST != null){
						if(ST.isAlive()){
							Node.getLog().log(Level.FINE, " wating for sender to finish");
							ST.join();
						}
					}
					stream.close();
					InputS.close();
	            }
			}catch (SocketException e){
				System.err.println("SocketManager.RecverThread.run(). ThreadName: " + Thread.currentThread().getName() 
						+ " RecvingQueue:obj present  " + RecvingQueue.size() 
						+ " SendingQueue:obj present " + SendingQueue.size()
						+ " Isbound " + Bound 
						);
				//e.printStackTrace();
				Bound = false;
				RStop = true;
				SStop = true;
			}catch (OptionalDataException e){
				//System.out.println("Error happened in packet " + debugi + " pipe_control: " + p_control);
				e.printStackTrace();
			}
			catch (Exception e) {
				/**
				 * Unrecoverable exception, try to close the socket
				 */
				Bound = false;
				RStop = true;
				SStop = true;
				try {
					close();
				} catch (IOException e1) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				} catch (InterruptedException e1) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				}
				
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				
			}
			Node.getLog().log(Level.FINE, "CLOSING THIS SOCKET MANAGER... RECV THREAD");
		}
	}
	
	public class SenderThread implements Runnable{
		@Override
		public void run() {
			try {
				
	            if (OutputS != null) {
	            	ObjectOutputStream stream = new ObjectOutputStream( OutputS);
	            	//boolean hibernate = false;
	                while(!SStop){
	                	try {
	                		Object obj;
							obj = SendingQueue.poll(1, TimeUnit.SECONDS );
							if(obj != null){
								Serializable data = (Serializable) obj; //data_reader.readObject();
								if( data instanceof ConnectionChangedMessage){
									stream.writeByte(HIBERNATE_ACTION);//hibernates connection
				                	stream.writeObject(data);
				                	stream.flush();
				                	stream.reset();
				                	//hibernate = true;
								}else if( SocketManager.this.getRawByteEncoder() != null
										&& !(data instanceof Data) ){
									//I will now use Data more as a framework variable, leaving the 
									//user just using a Serializable, or using the RawByteEncoder.
									
									SocketManager.this.getRawByteEncoder().toRawByteStream(stream, data);
									
									//byte[] raw = SocketManager.this.getRawByteEncoder().toRawBytePacket( data );
									//stream.writeByte(RAWBYTE_ACTION);
									//stream.writeInt(raw.length);
									//stream.write(raw);
				                	//stream.writeObject(data);
									//System.out.println("byte");
				                	stream.flush();
				                	stream.reset();
								}/*else if( data instanceof byte[]){
									//if instence of byte[]
									stream.writeByte(RAWBYTE_ACTION);
									byte[] raw = (byte[])data;
									stream.writeInt(raw.length);
									stream.write(raw);
				                	//stream.writeObject(data);
									//System.out.println("byte");
				                	stream.flush();
				                	stream.reset();
								}*/else{
									stream.writeByte(DEFAULT_ACTION);
				                	stream.writeObject(data);
				                	stream.flush();
				                	stream.reset();
								}
							}
		                	
						}catch (ClassCastException e){
							Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
						}
	                }	                
	                //This is mostly for the dataflow dependency classes. the ClientSocketManager is always a 
	                //consumer, and the ServerSocketManager is always a feeder.  Because the feeder never
	                //receives a packet, or very rarely, this will keep the thread open until it Exceptions
	                //out.  With this line below, the the connection line should drop with less 
	                //knwon errors.
	                //if( SocketManager.this instanceof ServerSocketManager){
	                if( Bound){ //if bound to prevent the echo from going forever.
	                			//should reduce the change of getting send thread stuck sending a goodbye.
		            	stream.writeByte(CLOSE_ACTION);	
		    			stream.flush();
		    			Bound = false;
	                }
	                //}
	           
	    			Thread.sleep(200);
	    			stream.close();
	    			OutputS.close();

	            }
	            
			}catch (Exception e) {
				Bound = false;
				RStop = true;
				SStop = true;
	
				Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e) );
			
			}
			Node.getLog().log(Level.FINE, "CLOSING SENDER SOCKET MANAGER -- SENDER THREAD");	
		}
		
	}
	


	/**
	 * report the exception from the run method. null if no error
	 * @return
	 */
	public String getExceptionMessage(){
		return ExceptionMessage;
	}
	public void setExceptionMessage(String str){
		ExceptionMessage = str;
	}
/*public void addtoErrorMessage( Exception e){
		e.printStackTrace();
		StackTraceElement[] error_stack = e.getStackTrace();
		String err = "\n SOCKETMANAGER >" + Node.getPID().toString() + " SOCKETMANAGER \n";
		err += e.getMessage() +"\n";
		for(int i = 0; i < error_stack.length; i++){
			err += error_stack[i] +"\n";
		}
		if( this.ExceptionMessage == null){
			this.ExceptionMessage = err;
		}else{
			this.ExceptionMessage += err;
		}
		
		System.err.println( err );
	}*/
	/**
	 * Checks to see if the Object it itself a Data object or if it  has inherited the Data object.  
	 * The method goes just one level above. So if the class is inheriting a class that inherits a 
	 * Data object, the method will not pick up on this.
	 * @deprecated
	 * @param obj
	 * @return
	 */
	public static boolean checkImplementsDataInstructionContainer( Object obj){
		Class interfaces = obj.getClass().getSuperclass();
		boolean ans = false;
		if( obj.getClass().getName().compareTo(DataInstructionContainer.class.getName()) == 0){
			ans = true;
		}else
		if( interfaces != null){
			if(interfaces.getName().compareTo(DataInstructionContainer.class.getName()) == 0){
				ans = true;
			
			}
			
		}
		return ans;
	}
	
	@Override
	public void addRecvingObject(Serializable obj) throws InterruptedException {
		
		this.RecvingQueue.put(obj);
	}
	/*
	public BlockingQueue<NonBlockReceiver> getNBRecv(){
		return this.NBRecv; //not used on this implementation
	}*/
	/*@Override
	public BlockingQueue<Serializable> getSendingQueue() {
		return this.SendingQueue;
	}*/
	@Override
	public boolean isBound() {
		
		return Bound;
	}
	@Override
	public void setBound(boolean bound) {
		Bound = bound;
		
	}
	/*@Override
	public BlockingQueue<Serializable> getRecvingQueue() {
		return this.RecvingQueue;
	}*/
	@Override
	public void addSendingObject(Serializable obj) throws InterruptedException {
		this.SendingQueue.put(obj);
	}
	@Override
	public LinkType getConnectionType() {
		return Types.LinkType.SOCKET;
	}
	@Override
	public SlimJxtaID getHashPipeId() {
		return this.HCommPipeID;
	}
	@Override
	public SlimJxtaID getHashRemotePeerId() {
		return this.HRemotePeerID;
	}
	@Override
	public void setHashPipeId(SlimJxtaID id) {
		this.HCommPipeID = id;
		
	}
	@Override
	public void setHashRemotePeerId(SlimJxtaID id) {
		this.HRemotePeerID = id;
	}
	@Override
	public Long getRemoteNodeConnectionDataID() {
		return this.RemoteNodeConnectionDataID;
	}
	@Override
	public String getRemoteNodeConnectionPipeID() {
		return this.RemoteNodeConnectionPipeID;
	}
	@Override
	public HierarchicalDependencyID getDependencyID() {
		return this.DependencyID;
	}
	@Override
	public PipeID getUniqueDependencyID() {
		return this.UniqueDependencyID;
	}
	@Override
	public void setDependencyID(HierarchicalDependencyID id) {
		DependencyID = id;
	}
	@Override
	public void setUniqueDependencyID(PipeID pipeId) {
		UniqueDependencyID = pipeId;
	}
	//@Override
	//public void setConnectionManagerMonitor(ConnectionManagerMonitor monitor) {
		// TODO Auto-generated method stub
		//TODO
	@Override
	public void setConnectionChangeListener(ConnectionChangeListener monitor) {
		CCListener = monitor;
	}
	@Override
	public boolean isDataflowSource() {
		return this.DataflowSource;
	}
	@Override
	public void setDataflowSource(boolean set) {
		this.DataflowSource = set;	
	}
	@Override
	public String getCachedObjSize() {
		return "S:" + SendingQueue.size() + ":R:" + RecvingQueue.size() ;
	}
	
}
