package edu.uncc.grid.pgaf.communication;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.shared.ConnectionHibernatedException;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;

/**
 * The Communicator is used to give an MPI-like communication interface to the advance layer.  This is done to 
 * better manage communication among the nodes.  With the old system where the advanced user was in charge of
 * setting up and bringing down the communication links, it was harder to add to it the operator idea.  If the 
 * operator idea was to be added to the old system, we would have to divide the ClientSide function into three
 * function, communication_setup, compute, and communication_shutdown.  This would be necesary in order to 
 * add the operators to the compute function.  But, with the MPI-like communication, the communication links are
 * created as they are needed and kept alive until the pattern is no longer needed.  New patterns create new
 * communication links.
 * 
 * The deplyment and workpool patterns will still need a closer access to the connection because one of the requirements
 * for this framework is to manage a pool of nodes that is not known.  So, during the deployment, and the workpool patterns
 * the number of nodes involved in the computation can change.  
 * 
 * @author jfvillal
 *
 */
public class Communicator {

	/**
	 * OTHER CONSTANTS
	 */
	public static final long TIMEOUT = 120000; // TEN SECONDS
	public static final long WAIT_TIME = 50;
	public PipeID PatternID;
	Node Network;
	/**
	 * All connections including the Server sockets and client sockets.
	 * a mapping of the pipe_id to the respective connection manager.
	 * 
	 * The pipe_id is the pipe used to create the connection link for the dispatcher on 
	 * the server side.  since we also need a pipe_id on the client side so that the pipe can
	 * be located using the Map class, a pipe_id on the client side is also created.
	 * 
	 * If the pipe_id on the client side is done as part of a deployment pattern, the corresponding
	 * dispatcher is not activated (the node will not be reachable directly, only trhough the 
	 * client connection).  But if the node is part of a data segmented patterns such as the pipeline 
	 * or a stencil, then a dispatcher will be created along with the pipe.  This will advertise the
	 * connection which will make it reachable by other client nodes.
	 * 
	 */
	private Map<String,ConnectionManager> AllConnections;
	/**
	 * This is used to find the appropiate pipe to send the data.  To 
	 * reuse the link, if there one already. 
	 * 
	 * The PipeMap is used by the Communicator which is working on a pattern with
	 * identifiable data segments.  If this is a Communicator that is working on
	 * a deployment or workpool pattern, the PipeMap is ignored.
	 */
	private Map<Long, String> PipeMap;
	/**
	 * A special place to store the sink and source id
	 */
	//PipeID SinkSourceId;
	
	private MultiModePipeDispatcher PatternDispatcher;
	/**
	 * List of connections hosted by this Communicator.  This will hold the list of 
	 * Server links.  We have AllConnections holding both client and serverlinks.
	 * The ServerLinks are managed by the dispatcher, which means that new connections
	 * can be created by a separate thread.  If we need to communicate with some node, we 
	 * first look at the AllConnections map, if the connection is not already open, we 
	 * can look at the connections from the ServerLinks.  If the connection is not there
	 * we can either wait or trow an exception.
	 */
	List<ConnectionManager> ServerLinks;
	Long MySegment;
	/**
	 * 
	 * @param net
	 * @param pattern_id
	 * @param this_nodes_segment
	 * @throws NoPortAvailableToOpenException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public Communicator( Node net, PipeID pattern_id, long local_segment) throws InterruptedException, IOException, ClassNotFoundException, NoPortAvailableToOpenException{
		PatternID = pattern_id;
		Network = net;
		PatternDispatcher = null;  //set dispatcher to inactive
		ServerLinks = null;			//set server links to inactive
		
		AllConnections = Collections.synchronizedMap(new HashMap<String,ConnectionManager>());
		ServerLinks = Collections.synchronizedList(new ArrayList<ConnectionManager>());
		/**
		 * This communication pipe id is used to host my own connection (where me is the node)
		 * I connect to the comm id that is lower than mine, and the ones that are higher 
		 * connect to me.  This way there is only one connection.
		 */
		MySegment = local_segment;
		
		PipeMap = new HashMap<Long, String>();
		PatternDispatcher = new MultiModePipeDispatcher(Network, "comm", MySegment, ServerLinks, pattern_id, null);
	}
	public void close(){
		if( PatternDispatcher != null){
			try {
				PatternDispatcher.close();
			} catch (InterruptedException e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				e.printStackTrace();
			} catch (IOException e) {
				Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
				e.printStackTrace();
			}
		}
		for( ConnectionManager m : AllConnections.values()){
			if( m.isBound()){ //presumably, the server connection will be down and won't be shutdown twice
				try {
					m.close();
				} catch (IOException e) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
					e.printStackTrace();
				} catch (InterruptedException e) {
					Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
					e.printStackTrace();
				}
			}
		}
	}
	/* MPI-Like connections to enable operator parallel programming idea.
	 * 1/4/10
	 * The algorithm used to give the MPI-like communication to these socket-based connections is as follows.  Each node has a dispatcher that can accept connections 
	 * from every other node.  This only works on the patterns that assign a data_id.  the connection can be done by having a client connect to a dispatcher, and that
	 * will create a server connection on the dispatcher side, which is assigned to a list of working connection.  Because, each node has a dispatcher, this creates
	 * the eventuality where to connection could be created for a single communication.  If 1 and 2 want to talk, 1 could creat a client, and two could create a client
	 * which creates two connections.  To prvent that, the lower number data_id will host the connection, and the upper number data_id will use the client.  Now,
	 * if one want to send or receive from two, and a connection does not exist, one has to continually check the list of exisiting server connection to see if 
	 * the desired client pops up.  If that does not happen, the connection times out.  This means that the connection have to be requested no more than 10 seconds appart.
	 * 
	 */
	/**
	 * Will send blocking.  Fot now it does the same as the regular send method.
	 * @throws CommunicationLinkTimeoutException 
	 * @throws TunnelNotAvailableException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ConnectionHibernatedException 
	 */
	public void BlockSend( Serializable obj, Long send_to_segment ) throws InterruptedException, IOException, ClassNotFoundException, TunnelNotAvailableException, CommunicationLinkTimeoutException, ConnectionHibernatedException{
		Send( obj, send_to_segment);
		/*TODO add contains method to connection manager so that we can check if the object is still in the send queue
		* to do this, I need to verify that equal() default implementation will work.  this in conjuction with 
		* a wait thread can be used to block until we are sure the object is no longer on the send queue.*/
	}
	/**
	 * Will send non-blocking
	 * @param obj
	 * @param segment
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws CommunicationLinkTimeoutException 
	 * @throws TunnelNotAvailableException 
	 * @throws ClassNotFoundException 
	 * @throws ConnectionHibernatedException 
	 */
	public void Send( Serializable obj, Long send_to_segment) throws InterruptedException, IOException, ClassNotFoundException, TunnelNotAvailableException, CommunicationLinkTimeoutException, ConnectionHibernatedException{
		//check to see if there is a pipe already for id segment
		// 1.  check cache
		ConnectionManager sender = null;
		String pipe_id = this.PipeMap.get(send_to_segment);
		if( pipe_id != null){
			sender = this.AllConnections.get(pipe_id);
		}
		if( sender != null){
			Node.getLog().log(Level.FINEST, "Cache hit, sent object. data_id: " + send_to_segment );
			sender.SendObject(obj);
		}else{
			// 2. check the connection hosted by this node
			
			if( MySegment < send_to_segment ){ //then I am suppose to host the connection for higher segments
				boolean sent = false;
				long time = System.currentTimeMillis();
				while( !sent){
					synchronized(ServerLinks){ //list of servers is shared with the dispatcher.
						for( ConnectionManager m : ServerLinks){
							long debug_b = m.getRemoteNodeConnectionDataID();
							if( m.getRemoteNodeConnectionDataID() == send_to_segment.longValue()){
								//send and add this connection to AllConnections
								Node.getLog().log(Level.FINEST, "Server link cache hit. data_id: " + send_to_segment);
								m.SendObject(obj);
								//cache it so we don't have to do the linear search next time
								//TODO could optimize this List into a Map on the Dispatchers
								AllConnections.put(m.getRemoteNodeConnectionPipeID(), m);
								PipeMap.put(send_to_segment, m.getRemoteNodeConnectionPipeID());
								sent =  true;
								break;
							}
						}
					}
					Thread.sleep(WAIT_TIME);
					long timeout = System.currentTimeMillis() - time;
					if( timeout >= TIMEOUT){  //we have TIMEOUT time to get a connection from the receiving end.
						throw new CommunicationLinkTimeoutException();
					}
				}
				
			}else{//if I am higher than send_to_segment, then I should creae a client to connect.
				// 3. check to see if we could create a client to it
			
				//try to create a connections to it.  this involves checking to see if there is an advertisement
				//for the connection, and trying to connect.
				long time = System.currentTimeMillis();
				
				
					sender = MultiModePipeClient.getClientConnection(Network, send_to_segment, this.PatternID
							, this.PatternDispatcher.getCommunicationPipe().toString(), this.MySegment, null,null, null, TIMEOUT) ;
				
				
				Node.getLog().log(Level.FINEST, "Connected as client  data_id: " + send_to_segment );
				AllConnections.put(sender.getRemoteNodeConnectionPipeID(), sender);
				PipeMap.put( send_to_segment , sender.getRemoteNodeConnectionPipeID() );
				sender.SendObject(obj);
			}
			
		}
	}
	/**
	 * Blocks until all remaining data in the queues has being sent.
	 */
	public void hasSent() {
		Collection<ConnectionManager> list = AllConnections.values();
		int size = list.size();
		for( ConnectionManager m: list){
			while( m.hasSendData() ){
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * Will block receive. 
	 * @param recv
	 * @param segment
	 * @throws InterruptedException 
	 * @throws TunnelNotAvailableException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws CommunicationLinkTimeoutException 
	 */
	public Serializable BlockReceive(  Long receive_from_segment) throws InterruptedException, IOException, ClassNotFoundException, TunnelNotAvailableException, CommunicationLinkTimeoutException{
		//TODO
		ConnectionManager receiver = null;
		Serializable ans = null;
		// 1. check the cache for existing connections
		String pipe_id = this.PipeMap.get(receive_from_segment);
		if( pipe_id != null){
			receiver = this.AllConnections.get(pipe_id);
		}
		if( receiver != null){
			Node.getLog().log(Level.FINEST, "Cache hit, received object. data_id: " + receive_from_segment );
			ans = receiver.takeRecvingObject();
		}else{
			// 2. check the hosted server connections
			if( MySegment < receive_from_segment){
				boolean received = false;
				long time = System.currentTimeMillis();
				while( !received){
					synchronized( ServerLinks){
						for( ConnectionManager m : ServerLinks){
							if( m.getRemoteNodeConnectionDataID() == receive_from_segment){
								ans = m.takeRecvingObject();
								AllConnections.put( m.getRemoteNodeConnectionPipeID(), m);
								PipeMap.put(receive_from_segment, m.getRemoteNodeConnectionPipeID());
								received = true;
								break;
							}
						}
					}
					Thread.sleep(WAIT_TIME);
					long timeout = System.currentTimeMillis() - time;
					if( timeout >= TIMEOUT){  //we have TIMEOUT time to get a connection from the receiving end.
						throw new CommunicationLinkTimeoutException();
					}
				}
			}else{
				// 3. check to see if we can create a client to the node
				long time = System.currentTimeMillis();
				
			
					receiver = MultiModePipeClient.getClientConnection(Network,receive_from_segment
							, this.PatternID, this.PatternDispatcher.getCommunicationPipe().toString(), MySegment, null, null,null, TIMEOUT);
			
				
				Node.getLog().log(Level.FINEST, "Connected as client  data_id: " + receive_from_segment );
				AllConnections.put(receiver.getRemoteNodeConnectionPipeID(), receiver);
				PipeMap.put( receive_from_segment , receiver.getRemoteNodeConnectionPipeID() );
				ans = receiver.takeRecvingObject();
			}
		}
		return ans;
	}
	/**
	 * Will block receive
	 * @param segment
	 * @return
	 * @throws InterruptedException 
	 * @throws CommunicationLinkTimeoutException 
	 * @throws TunnelNotAvailableException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public void Receive( NonBlockReceiver recv, Long receive_from_segment) throws InterruptedException, IOException
																				, ClassNotFoundException, TunnelNotAvailableException
																				, CommunicationLinkTimeoutException{
		ConnectionManager receiver = null;
		// 1. check the cache for existing connections
		String pipe_id = this.PipeMap.get(receive_from_segment);
		if( pipe_id != null){
			receiver = this.AllConnections.get(pipe_id);
		}
		if( receiver != null){
			Node.getLog().log(Level.FINEST, "Cache hit, received object. data_id: " + receive_from_segment );
			
			receiver.nonblockRecvingObject(recv);
		}else{
			// 2. check the hosted server connections
			// lower id host the connection, and larger id connect to it.
			if( MySegment < receive_from_segment){
				boolean received = false;
				long time = System.currentTimeMillis();
				while( !received){
					for( ConnectionManager m : ServerLinks){
						if( m.getRemoteNodeConnectionDataID() == receive_from_segment){
							m.nonblockRecvingObject(recv);
							AllConnections.put( m.getRemoteNodeConnectionPipeID(), m);
							PipeMap.put(receive_from_segment, m.getRemoteNodeConnectionPipeID());
							received = true;
							break;
						}
					}
					Thread.sleep(WAIT_TIME);
					long timeout = System.currentTimeMillis() - time;
					if( timeout >= TIMEOUT){  //we have TIMEOUT time to get a connection from the receiving end.
						throw new CommunicationLinkTimeoutException();
					}
				}
			}else{
				
				receiver = MultiModePipeClient.getClientConnection(Network,receive_from_segment
															, this.PatternID, this.PatternDispatcher.getCommunicationPipe().toString()
															, MySegment, null, null,null, TIMEOUT);
				
				
				Node.getLog().log(Level.FINEST, "Connected as client  data_id: " + receive_from_segment + " my segment: " + MySegment );
				AllConnections.put(receiver.getRemoteNodeConnectionPipeID(), receiver);
				PipeMap.put( receive_from_segment , receiver.getRemoteNodeConnectionPipeID() );
				receiver.nonblockRecvingObject(recv);		
			}
		}	
	}
	/*** setReceiver could be used to set a permanent packet receiver.*/
}
