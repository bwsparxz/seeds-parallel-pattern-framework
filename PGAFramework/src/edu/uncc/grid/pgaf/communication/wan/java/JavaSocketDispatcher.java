/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * * All rights reserved
 */
package edu.uncc.grid.pgaf.communication.wan.java;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import net.sbbi.upnp.messages.UPNPResponseException;
import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.communication.wan.ConnectionEstablishedListener;
import edu.uncc.grid.pgaf.communication.wan.ServerSocketManager;
import edu.uncc.grid.pgaf.communication.wan.SocketDispatcher;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;


/**
 * 
 * It host a dispatcher.  That is what I calle a thread that will continually listend on a port
 * for incoming sockets.  The list of sockets get wrapped using a ConnecitonManager.  The 
 * Connection manager can be a server side or a client side, but once established, both behave the 
 * same. 
 * 
 * On August 22 2010, the onConnectionEstablishListner was added to allow the user of this class
 * to perform some extra tests before adding the new socket to the list.  Specifically for the 
 * use of datalfow connections, this interace allows me to make sure the new socket that is connecting
 * to this dispatcher has an hierarchical id that is acutally hosted on this node.
 * 
 * @author jfvillal
 *
 */
public class JavaSocketDispatcher extends  SocketDispatcher{
	protected ServerSocket DataLinkServer;
	public  Boolean Stop;
	protected int Port;
	/**
	 * The pipe in this class is mostly used to identify the communication line
	 */
	protected SlimJxtaID ServicePipeID; 
	/*we may not need our own list if the user keeps track of the sockets himself*/
	//private List<ConnectionManager> ServerSMList;
	protected List<ConnectionManager> AdvUserList;
	
	protected String LocalPipeID;
	protected Long LocalDataID;
	protected ConnectionEstablishedListener mConnectionEstablishListener;
	
	protected String ParentThreadName;
	RawByteEncoder Encoder;
	public JavaSocketDispatcher(  
				Node Network, String thread_name,  List<ConnectionManager> adv_user_list, String local_pipe_id, Long local_data_id
				, RawByteEncoder enc ) 
			throws IOException, NoPortAvailableToOpenException{
		//ServerSMList = Collections.synchronizedList(new ArrayList<ConnectionManager>() );
		Encoder = enc;
		ParentThreadName = thread_name;
		this.mConnectionEstablishListener = null;
		int backlog = 20;
		//int timeout = 50000;
		boolean binding_successfull = false;
		int max_try = 20;  //attempt to get a good por 20 times
		for(int i = 0 ;i < max_try; i++){
			try{
				Port =  Network.generateNewJavaSocketPort();
				DataLinkServer = new ServerSocket(Port, backlog);
				DataLinkServer.setSoTimeout(CommunicationConstants.DISPATCHER_TIMEOUT);
				binding_successfull = true;
			}catch( BindException e){
				Node.getLog().log(Level.WARNING , "Port Binding Failed ");
				binding_successfull = false;
				if( i >= max_try - 1){
					throw new BindException("Binding to port failed: port " + Port);
				}
				continue;
			} /*catch (UPNPResponseException e) {
				binding_successfull = false;
				Network.setRestrictedNetworkDetected(true);
				e.printStackTrace();
			} */catch (SocketException e ){
				Node.getLog().log(Level.WARNING, " Got SocketException with port " + Port + " on try " + i );
			}
			if(binding_successfull){
				break;
			}
		}
		//ServicePipeID = pipe_id;
		Stop = false;
		AdvUserList = adv_user_list;
		this.setDaemon(true);
		
		LocalPipeID = local_pipe_id;
		LocalDataID = local_data_id;
	}
	public List<ConnectionManager> getServerSMList(){
		return AdvUserList;
	}
	public static int BUFF = 262144;
	public void run() {
		int count = 0;
		try{
			int loop = 0;
			while(!Stop){
				++loop;
				
				try {
					//users data stuff
					
					Socket socket  =  DataLinkServer.accept();
					if( socket != null){
						//add to queue
						Node.getLog().log(Level.FINE,"Adding one socket to queue ... " + count ++ );
						//socket.setSoTimeout(CommunicationConstants.SOCKET_TIMEOUT);
						//socket.setKeepAlive(true);
						//TODO  keep alive ? 
						/*supplies the pipe_id and the data id so that we can enable an mpi-like behavior*/
						
						/*from
						 * http://www.google.com/url?sa=t&source=web&cd=2&ved=0CB0QFjAB&url=http%3A%2F%2Fciteseerx.ist.psu.edu%2Fviewdoc%2Fdownload%3Fdoi%3D10.1.1.107.7046%26rep%3Drep1%26type%3Dpdf&rct=j&q=Java%20socket%20proper%20buffer%20size%20for%20gigabit%20ethernet&ei=78vtTOGpGIH7lweul4jaDA&usg=AFQjCNEJD-xyzAvkK6h41CCyM8lzygYN8g&sig2=UXnR8Ey_TPLtYdtoPIYvag&cad=rja
						 * results, I increased the buffer size.  The empirical results are
						 * an improvement from 
						 * 14,618 to 
						 * 13,272 when increasing buffer size for the send routine 
						 * to 
						 * 12,272 when increasing buffer size for both send and receive routine
						 * this was done using the Matrix multiplication with sideways split, but with no split
						 * so just plain pipeline implementation using a data flow.
						 * The improvement is of 16.16% 
						 * 
						 */
						
						socket.setSendBufferSize(BUFF);
						socket.setReceiveBufferSize(BUFF);
						ServerSocketManager manager = new ServerSocketManager(
								ParentThreadName ,
								socket, LocalPipeID 
								, LocalDataID, this.mConnectionEstablishListener, Encoder);
						/**
						 * ConnectionEstablisheListener is used by the Dependencies, they need to check to see if the dependency id
						 * is in the list of dependencies they are feeding.
						 * The next code either adds the new connection_manager depeding on what the ConnectionExtablishListener decides.
						 * For the legacy MPI-like patterns, the ConnectionEstablishListener can be left null which will default to 
						 * the old behaviour.
						 */
						if( manager.isBound()){
							synchronized( AdvUserList){
								this.AdvUserList.add(manager);
							}
						}
					}	
				}catch (SocketTimeoutException e) {
					//Node.getLog().log(Level.INFO, "Java Socket Dispatcher Accept Timeout  " + Stop );
					
				} catch (ClassNotFoundException e) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e));
				}
			}
		}catch ( Exception e){
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		}
		try {
			DataLinkServer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int i = 0;
		int j = i;
	}

	public void setStop(boolean set){
		Stop = set;
	}
	synchronized public void closeServers() throws IOException, InterruptedException{
		Stop = true;
		Iterator<ConnectionManager> it = AdvUserList.iterator();
		while( it.hasNext()){
			ConnectionManager m = it.next();
			m.close();
		}
	}
	public SlimJxtaID getServiceHPipeID() {
		return ServicePipeID;
	}
	public void setServicePipeID(SlimJxtaID servicePipeID) {
		ServicePipeID = servicePipeID;
	}
	public int getPort() {
		return Port;
	}
	@Override
	public void setOnConnectionEstablishedListener(	ConnectionEstablishedListener listener) {
		this.mConnectionEstablishListener = listener;
	}
	
}
