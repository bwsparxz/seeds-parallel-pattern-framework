package edu.uncc.grid.seeds.comm.dependency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.advertisement.DependencyAdvertisement;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeClient;
import edu.uncc.grid.pgaf.communication.MultiModePipeDispatcher;
import edu.uncc.grid.pgaf.communication.NATNotSupportedException;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.communication.wan.ConnectionEstablishedListener;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;

/**
 * The Dependency Engine will manage a single socket with multiple connection.  One socket server is created for
 * each of the nodes in the network.  Each connection is to have a different output stream.  <br>
 * 
 * In this first implementation
 * we will not consider multiple output streams, which will be need for ghost zoning and redundant processes.
 * 
 *This class in effect, will wrap the MultiModePipe classes so that we can add the dataflow concept to a single 
 *class that can then be used by an advance developer.
 * 
 * @author jfvillal
 *
 */
public class DependencyEngine implements ConnectionEstablishedListener{
	/**
	 * 
	 * Starts the socket server that can feed stream to the outputs available on this node.
	 * 
	 */
	MultiModePipeDispatcher Dispatcher;
	Node Context;
	/**
	 * The unique segment id used by this node.  
	 */
	long Segment;
	PipeID  PatternID;
	List<ConnectionManager> ConnectionServers;
	/**
	 * Map<String, HierarchicalDependencyID> HostedHids; (old data structure)
	 * 
	 * It is used to keep track of the hierarchical dependencies that are being serviced
	 * by this Engine.  each socket stored on ConnectionServers List should have a
	 * Hierarchical ID that was previously registered on this Map.
	 * 
	 * This map maps the segment id to a hida tree.
	 * <SegmentID str, Hida tree>
	 */
	Map<String, Hida> HostedHids;
	
	boolean EngineClosed;
	
	List<DependencyAdvertisement> HostedAdverts;
	
	RawByteEncoder Encoder;
	
	public DependencyEngine( Node n, long seg, PipeID pattern_id, RawByteEncoder enc ){
		Context = n;
		Segment = seg;
		PatternID = pattern_id;
		EngineClosed = false;
		HostedAdverts = new ArrayList<DependencyAdvertisement>();
		Encoder = enc;
	}
	
	public void startServer() throws InterruptedException, IOException
									, ClassNotFoundException
									, NoPortAvailableToOpenException, NATNotSupportedException{
		ConnectionServers = Collections.synchronizedList(new ArrayList<ConnectionManager>());
		
		HostedHids = new HashMap<String,Hida>();
		
		Dispatcher = new MultiModePipeDispatcher(Context, "DeptEngine" + Segment, Segment, ConnectionServers, PatternID, Encoder);
		Dispatcher.setOnConnectionEstablishedListener( (int)Segment, this );
		//System.out.println( "DependencyEngine:startServer() new Engine. Port " + Dispatcher.getPort() + " seg_id: " + Segment);
	}
	
	/**
	 * Stops the engine, and shuts down all current connections to it.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void close() throws InterruptedException, IOException{
		Dispatcher.close();
		/*TODO this should also close shared memory */
		EngineClosed = true;
	}
	/**
	 * stops the engine from receiving new connections, but does not stop the 
	 * current connections.
	 * 
	 * @throws InterruptedException
	 */
	public void stopEngine() throws InterruptedException{
		Dispatcher.stopDispatcher();
		DiscoveryService service = Context.getNetPeerGroup().getDiscoveryService();
		for( DependencyAdvertisement adv : this.HostedAdverts ){
			try {
				service.flushAdvertisement(adv);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		EngineClosed = true;
	}
	
	private void registerAdvertOnHidaMap( HierarchicalDependencyID id, DependencyAdvertisement advert) {
		synchronized(HostedHids){//register dependency
			Hida hida = HostedHids.get(id.getSid().toString());
			if( hida == null){ //if hida tree is null, create one
				// add new hida
				hida = new Hida( id, advert);
				//add new tree to map
				HostedHids.put( id.getSid().toString(), hida);
			}else{//if exists, add the leaf or branch to the tree.
				//add to existing hida
				
				hida.addChild(id, advert, Context.getNetPeerGroup().getDiscoveryService());
			}
		}
	}
	
	/**
	 * 
	 * Registers an output depency with the server
	 * 
	 * The dependency should be provided with all the necesary varialble filled.  The stream should
	 * be left null since this function will fill that in.
	 * 
	 * <b>this method blocks until a client connects to the dependency</b>
	 * 
	 * @throws IOException 
	 * @return The parent dependency with the necesar children to service connection
	 * @throws EngineClosedException 
	 */
	public Dependency registerOutputDependency(HierarchicalDependencyID id, long cycle_version
			, SplitCoalesceHandler handler
			) throws IOException, EngineClosedException{
		//this sends an advetisement that we have an output for this stream
		if(EngineClosed ) throw new EngineClosedException( " registerOutputDependency " + id.toString() );
		PipeID uniqueId  =  IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		DependencyAdvertisement advert = (DependencyAdvertisement) 
				AdvertisementFactory.newAdvertisement(DependencyAdvertisement.getAdvertisementType());
		advert.setDataID(Segment);
		advert.setDependencyID(uniqueId);
		advert.setHyerarchyID(id);
		advert.setPatternID(PatternID);
		//advert.setDirty( false );
		advert.setCycleVersion( cycle_version );
		//keep track of adverts running around on network.
		HostedAdverts.add(advert);
		//register this output dependency with the internal map structure.
		registerAdvertOnHidaMap( id, advert);
		
		//this blocks until some node starts feeding from the output.
		//get the servers from the connection and put then into hash maps.
		if( Context != null ){ //Could be null if running in multi-core mode.
			DiscoveryService service = Context.getNetPeerGroup().getDiscoveryService();
			synchronized( service ){
				service.publish(advert);
			}
		}
		boolean connected = false;
		Dependency dependency = null;
		int i = 0;
		while( !connected ){
			DependencyMapper.checkAdvertisement(advert, null);//the client may delete the advertisement in shared memory if
			//the time when the client is close to the time when this server recreates this line
			// because it is a map, the value would not be duplicated, just overwritten
			//this took me a couple of hours of study.  Simple things often time aren't !
			
			//TODO change so that it can receive multiple connection to the same dependency.
			//republish this engines connection advertisment
			++i;
			
			synchronized( ConnectionServers ){
			
				for( ConnectionManager man : ConnectionServers ){
					//handshake for dataflow.
					//int m = id.hierarchicalCompareTo(man.getDependencyID());
					
					if( id.hierarchicalCompareTo(man.getDependencyID()) != 0 /*> 0*/  ){//if it's compatible
							//compatible if this is among the parents or among the children
							//or the exact dependency the client is looking for.
					
						HierarchicalDependencyID tree = (id.getLevel() > man.getDependencyID().getLevel())
														? id 
														: man.getDependencyID();
						if( dependency == null){ //create one connection or first connection
							dependency = new Dependency( man, id, tree, man.getUniqueDependencyID(), this, cycle_version
									, handler, advert);
						}else{ //add another branch to the connection
							dependency.addChild(man, id, tree, man.getUniqueDependencyID(), advert);
						}
						//boolean test = dependency.isConnected(id);
						if( dependency.isConnected(id) ){
							connected = true;
							Node.getLog().log(Level.FINER, Thread.currentThread().getName() + " <> DependencyEngine:registerOutputDependency." +
									"Hid " + id.toString() + " got connected to : " + 
									man.getRemoteNodeConnectionDataID() );
							break;
						}
					}
				}
			}
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return dependency;
	}
	/**
	 * This method is mainly used to reconnect a dependency that was hibernated.  
	 * 
	 * @param parent the parent dependency to which the children neceary to make the connection work
	 * 			will be attashed.
	 * @param id the ID that should be serviced by this dependency tree. 
	 * @throws IOException
	 * @throws EngineClosedException 
	 */
	public void registerOutputDependencyChild(Dependency parent, HierarchicalDependencyID id, int cycle_version) throws IOException, EngineClosedException{
		//this sends an advetisement that we have an output for this stream
		if(EngineClosed ) throw new EngineClosedException( " registerOutputDependency " + id.toString() );
		PipeID uniqueId  =  IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		
		DependencyAdvertisement advert = (DependencyAdvertisement) 
				AdvertisementFactory.newAdvertisement(DependencyAdvertisement.getAdvertisementType());
		advert.setDataID(Segment);
		advert.setDependencyID(uniqueId);
		advert.setHyerarchyID(id);
		advert.setPatternID(PatternID);
		advert.setCycleVersion( cycle_version );
		
		//register this output dependency with the internal map structure.
		registerAdvertOnHidaMap( id, advert );
		
		DiscoveryService service = Context.getNetPeerGroup().getDiscoveryService();
		
		DependencyMapper.checkAdvertisement(advert, null);
		
		//service.remotePublish(advert);
		service.publish( advert );
		//this blocks until some node starts feeding from the output.
		//get the servers from the connection and put then into hash maps.
		boolean connected = false;
		int i = 0;
		while( !connected ){
			//TODO change so that it can receive multiple connection to the same dependency.
			/*if( i % 5 == 0){
				service.remotePublish(advert);
				//service.publish(advert);
			}*/
			++i;
			synchronized( ConnectionServers ){
				for( ConnectionManager man : ConnectionServers ){
					//handshake for dataflow.
					//int m = id.hierarchicalCompareTo(man.getDependencyID());
					
					if( id.hierarchicalCompareTo(man.getDependencyID()) != 0 /*> 0*/  ){//if it's compatible
							//compatible if this is among the parents or among the children
							//or the exact dependency the client is looking for.
					
						HierarchicalDependencyID tree = (id.getLevel() > man.getDependencyID().getLevel())
														? id 
														: man.getDependencyID();
						if( parent == null){
							//dependency = new Dependency( man, id, tree, true, man.getUniqueDependencyID());
							throw new NullPointerException();
						}else{
							parent.addChild(man, id, tree, man.getUniqueDependencyID(), advert);
						}
						//boolean test = parent.isConnected(id);
						if( parent.isConnected(id) ){
							connected = true;
							break;
						}
					}
					
					/*if( man.getDependencyID().equals( uniqueId )){
						dependency = new Dependency(man, id, true, uniqueId );
						connected = true;
						break;
					}else{
						//the ide may not be the same, but it may be compatible.
						if( id.compareTo(man.getDependencyID()) > 1){
							HierarchicalDependencyID next_child = man.getDependencyID().getLevel(id.getLevel()+1);
							dependency = new Dependency(man, id, true, uniqueId );
						}
					}*/
				}
			}
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//System.out.println("sleeping --------------------------------------------dataflow " );
		}
	}
	
	/**
	 * 
	 * @param adv
	 * @param id This is the ide this node expects to be serviced by the server.  this is important because
	 * this object may be of higher level than the server.  So we need to let the server know that the 
	 * dependencies must be split before sending to this higher level object.
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws TunnelNotAvailableException
	 * @throws CommunicationLinkTimeoutException
	 * @throws NATNotSupportedException 
	 */
	public ConnectionManager getConnection( DependencyAdvertisement adv
			,HierarchicalDependencyID id) throws IOException, ClassNotFoundException
																		, InterruptedException, TunnelNotAvailableException
																		, CommunicationLinkTimeoutException, NATNotSupportedException{
		
		ConnectionManager m_manager = null;
		long time = System.currentTimeMillis();
		while( 
				(m_manager = MultiModePipeClient.getClientConnection(Context
																	, adv.getDataID()
																	, PatternID
																	, Dispatcher.getCommunicationPipe().toString()
																	, this.Segment
																	, adv.getDependencyID()
																	, id, Encoder)
				) == null
			 )
		{
			
			Thread.sleep(CommunicationConstants.DEPENDENCY_RECONNECT_WAIT_INTERVAL);
			long timeout_elapsed = System.currentTimeMillis() - time;
			if( timeout_elapsed >= CommunicationConstants.DEPENDENCY_TIMEOUT){  
				//we have TIMEOUT time to get a connection from the receiving end.
				System.err.println(" DataID: " + adv.getDataID() + " Dependency: " + adv.getHyerarchyID().toString() );
				throw new CommunicationLinkTimeoutException("  Time alloted not enough: timeout at: " 
						+ CommunicationConstants.DEPENDENCY_TIMEOUT );
			}
			Node.getLog().log(Level.FINEST, "wait for socket... my id: " + this.Segment + " remote id: " + adv.getDataID() );
		}
		return m_manager;
	}
	/**
	 * Will wait for timeout milliseconds.  if the time runs out it will throw a TimeoutException.
	 * 
	 * Other than that, the method is the same as {@linkplain #getInputDependency(String) this one }  
	 * @param UniqueId
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws TunnelNotAvailableException
	 * @throws CommunicationLinkTimeoutException
	 * @throws TimeoutException
	 * @throws AdvertsMissingException 
	 * @throws EngineClosedException 
	 * @throws NATNotSupportedException 
	 */
	//TODO change UniqueID fro string to a HierarchicalDependency 
	public Dependency getInputDependency( PipeID pattern_id, HierarchicalDependencyID id, long timeout, long cycle_version
				, SplitCoalesceHandler handler) 
																			throws 
																				  IOException, ClassNotFoundException
																				, TunnelNotAvailableException
																				, TimeoutException, AdvertsMissingException
																				, EngineClosedException, NATNotSupportedException{
		//TODO add more complex algorithm that will create the hierarchical dependencies.
		
		
		
		
		Dependency ans = null;
		long time =  System.currentTimeMillis();
		while( ans == null ){
			try {
				ans = getInputDependency(pattern_id, id, cycle_version, handler);
				
				if( ans == null){ 
					Thread.sleep(100);
				}else{
					//Node.getLog().log(Level.INFO, 
					//		"MySeg " + this.Segment + " DependencyEngine:getInputDependency... connected to dept : "
					//			+ id.toString() + " seg " + ans.getSegs() );
				}
				
				if( (System.currentTimeMillis() - time) > timeout ){
					
					Hida dept_adverts = DependencyMapper.getAdvertForDependency( pattern_id, id);
					
					dept_adverts.testPrintTree("");
					
					throw new TimeoutException("Time expired while waiting for depency connection " 
							+ Thread.currentThread().getName() + " id " + id.toString() 
							);
				}
			}catch (InterruptedException e) {
				e.printStackTrace();
			} catch ( IOException e){
				//This error can be a connection refused.  So we will need to clean up the caches to make sure
				//we are not trying to contact a server that no longer has the dependencies we are looking for.
				//get hida tree
				
				//september 25, deleting existing adverts this may cause more harm than good.
				//So, I'll let advert continue to exist but a version number will distinguish them.
				//At the moment the segment number is the version number since new perceptrons are
				//always assigned segments that are higher than the perceptrons they are replacing.
				//This helps prevent conflicts with the DataLink Connections.
				//And new code also allow the framework to distiguish between a new dependency advert and
				//an old one (the newer one has higher segment id).
				
				/*DiscoveryService service = Context.getNetPeerGroup().getDiscoveryService();
				
				Hida hida = DependencyMapper.getAdvertForDependency(pattern_id, id);
				List<DependencyAdvertisement> list = hida.getAdvertisements();
				//get datalink connection map for this pattern id
				Map<Long, DataLinkAdvertisement> pattern_link_map = 
						MultiModePipeMapper.DataLinkPipeAdvertisementList.get(pattern_id); //segment);
				if( pattern_link_map == null){
					Node.getLog().log(Level.FINER, " The advert is not in the map. " + pattern_id.toString());
				}else{
					//delete datalink advertisement since they are outdated.
					for( DependencyAdvertisement dept_adv : list){
						DataLinkAdvertisement data_link_advert = pattern_link_map.get(dept_adv.getDataID());
						//flush old data link from jxta cache.
						service.flushAdvertisement(data_link_advert);
						pattern_link_map.remove( dept_adv.getDataID());
						//flush old dependency advertisement from jxta cache.
						service.flushAdvertisement(dept_adv);
					}
				}
				//delete the hida dependency tree since it must also be outdated.
				DependencyMapper.deleteAdvertFromMap(pattern_id, id);
				*/
				
				Node.getLog().log(Level.WARNING, "Socket was refused.  " +
						" waiting 200 millis for HidaMap to refresh.  ID: " + id.toString());
				ans = null;
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} catch (CommunicationLinkTimeoutException e) {
				/**
				 * september 26 2010
				 * This exception is caught here so that we can get a change to query for advertisements and 
				 * try to connect to the dependency again.  So, ever 5 seconds we try the dependency, and 
				 * the datalink advert that connects to it would have 6 opportunities to be refreshed.
				 */
				e.printStackTrace();
				if( (System.currentTimeMillis() - time) > timeout ){
					throw new TimeoutException("Time expired while waiting for depency connection " 
							+ Thread.currentThread().getName() + " id " + id.toString());
				}
			} 
		}
		return ans;
	}
	

	
	
	
	
	
	/**
	 *  * The advance user is to get the dependency id from an advertisement.  then use that to connect to that
	 * dependency output.  the method return a dependency object, which at the moment is a wrapper for 
	 * a connection manager.
	 * 
	 * 
	 * @param UniqueId
	 * @return returns null if no connection was stablished
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws TunnelNotAvailableException
	 * @throws CommunicationLinkTimeoutException
	 * @throws AdvertsMissingException 
	 * @throws EngineClosedException 
	 * @throws NATNotSupportedException 
	 * @throws DirtyAdvertisementException 
	 */
	private Dependency getInputDependency( PipeID pattern_id, HierarchicalDependencyID id, long cycle_version
			, SplitCoalesceHandler handler) throws IOException
																			, ClassNotFoundException
																			, InterruptedException, TunnelNotAvailableException
																			, CommunicationLinkTimeoutException
																			, AdvertsMissingException, EngineClosedException, NATNotSupportedException
																			{
		if(EngineClosed ) throw new EngineClosedException( " getInputDependency " + id.toString()  );
		//TODO add intermediate step that returns the ID that will be found on the Map based on the dependency that is requested
		//for example, if the user needs 1.2/3.4 and we have 1.2, we should connect to 1.2
		//DependencyAdvertisement adv = DependencyMapper.DependencyMap.get(UniqueId);
		
		//TODO if it works, the query needs to be 
		new DependencyQuery( DependencyAdvertisement.HyerarchyIDTag, id.getRoot().toString() +"*" , this.Context);
		
		Hida dept_adverts = DependencyMapper.getAdvertForDependency( pattern_id, id);
		
		if( dept_adverts == null){
			return null;
		}
		if( !dept_adverts.allAdvertsPresent(id, cycle_version) ){
			return null;
		}
		
		Dependency den = new Dependency( dept_adverts, this, id, cycle_version, handler);
		//den.setUniqueId(adv.getDependencyID());
		
		
		
		//den.setPayloadStream(man);
		return den;
	}

	/**
	 * This will be called by the Dispatcher.  The dispacher give as the change to 
	 * make sure this socket will be used by this node before adding it to 
	 * the list.
	 * @return true if accepted 
	 */
	@Override
	public boolean onConnectionEstablished(ConnectionManager man) {
		synchronized(HostedHids){
			HierarchicalDependencyID id = man.getDependencyID();
			Hida hida = HostedHids.get(id.getSid().toString());
			//HierarchicalDependency ID ans = this.HostedHids.get(id.toString());
			return hida != null;
		}
	}

	public synchronized long getSegment() {
		return Segment;
	}

	public synchronized void setSegment(long segment) {
		Segment = segment;
	}
	
	/**
	 * 
	 * The advance user is to get the dependency id from an advertisement.  then use that to connect to that
	 * dependency output.  the method return a dependency object, which at the moment is a wrapper for 
	 * a connection manager.
	 * 
	 * @param dependency_id
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 * @throws TunnelNotAvailableException
	 * @throws CommunicationLinkTimeoutException
	 */
	/*public Dependency getInputDependency(PipeID dependency_id, Long remote_segment ) 
																		throws   IOException
																		, ClassNotFoundException
																		, InterruptedException
																		, TunnelNotAvailableException
																		, CommunicationLinkTimeoutException{
		// use MultiMode Client to get connection
		
		ConnectionManager man = MultiModePipeClient.getClientConnection(
				Context, remote_segment , PatternID
				, Dispatcher.getCommunicationPipe().toString(), CommunicationConstants.DYNAMIC
				, dependency_id, 30000);
		//hanshake for the dataflow information.
		Dependency den = new Dependency();
		den.setUniqueId(dependency_id);
		den.setPayloadStream(man);
		return den;
	}*/

}
