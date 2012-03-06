/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * * All rights reserved
 */
package edu.uncc.grid.pgaf.communication.nat;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.RawByteEncoder;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.ConnectionChangeListener;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.datamodules.DataInstructionContainer;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.Types.LinkType;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

/**
 * This class is used to represent the dispatchers and clients as units to the advanced and 
 * basic user.  VirtualSocketManager implements ConnectionManager and it is used to 
 * interface the Tunnel to the user.  Even though there is only one tunnel between the 
 * LeafWorker and the DirectorRDV, the user will see multiple connections represented by a list
 * of VirtualSocketManagers.
 * 
 * @deprecated  This class is deprecated for this branck DO NOT USE NAT ROUTING ONE DATA FLOW BRANCH
 * 
 * @author jfvillal
 *
 */
public class VirtualSocketManager implements ConnectionManager {
	/* The sending que is held at TunnelManager*/
	private BlockingQueue<Serializable> RecvingQueue;
	private BlockingQueue<NonBlockReceiver> NBRecv;
	private BlockingQueue<Serializable> SendingQueue;
	TunnelManager Tunnel;
	SlimJxtaID PID; /*peer id*/
	SlimJxtaID CommPipeID; /*pipe id*/
	private boolean Bound;/* The tunnel sets this value to indicate if
						   * it is bounded.
						   **/
	protected boolean SentUserInitClass;
	
	private Long RemoteNodeConnectionDataID; //used to hold the remote pipe id
	private String RemoteNodeConnectionPipeID; //used to hold the remote data id.
	
	public VirtualSocketManager(SlimJxtaID pipe_id, SlimJxtaID id, TunnelManager t ){
		RecvingQueue = new ArrayBlockingQueue<Serializable>(TunnelClient.QUEUE_MAX, true);
		SendingQueue = new ArrayBlockingQueue<Serializable>(TunnelClient.QUEUE_MAX, true);
		NBRecv = new ArrayBlockingQueue<NonBlockReceiver>(TunnelClient.QUEUE_MAX, true);
		Tunnel = t;
		PID = id;
		CommPipeID = pipe_id;
	}
	
	@Override
	public void SendObject(Serializable obj) throws InterruptedException, IOException {
		if( !this.isBound() ){
			throw new IOException(" Virtual Socket not bounded ");
		}
		this.SendingQueue.put(obj);
		//TunnelContainer con =  new TunnelContainer(CommPipeID.toString(), PID.toString(), obj);
		//Tunnel.SendObject(con);
	}

	@Override
	public boolean hasRecvData() {
		return !RecvingQueue.isEmpty();
	}

	@Override
	public boolean hasSendData() {
		return !SendingQueue.isEmpty();
	}
	@Override
	public boolean isSendFull() {
		return this.SendingQueue.size() >= CommunicationConstants.QUEUE_SIZE;
	}
	@Override
	public void nonblockRecvingObject(NonBlockReceiver rcv)
			throws InterruptedException {
		if( ! RecvingQueue.isEmpty() ){
			rcv.Update( RecvingQueue.take());
		}else{
			NBRecv.put(rcv);
		}

	}

	@Override
	public Serializable pollRecvingObject() {
		return RecvingQueue.poll();
	}

	@Override
	public Serializable takeRecvingObject() throws InterruptedException {
		return RecvingQueue.take();
	}
	
	public void addRecvingObject( Serializable obj) throws InterruptedException{
		this.RecvingQueue.put(obj);
	}
	public BlockingQueue<NonBlockReceiver> getNBRecv(){
		return this.NBRecv;
	}
	@Override
	public boolean isBound() {
		return Bound;
	}

	public void setBound(boolean bound) {
		Bound = bound;
	}
/*
	@Override
	public BlockingQueue<Serializable> getSendingQueue() {
		
		return this.SendingQueue;
	}*/

	@Override
	public SlimJxtaID getHashPipeId() {
		return this.CommPipeID;
	}
	public SlimJxtaID getSlimPipeID(){
		return CommPipeID;
	}
	@Override
	public SlimJxtaID getHashRemotePeerId() {
		return this.PID;
	}
	public SlimJxtaID getSlimRemotePeerId(){
		return PID;
	}
	@Override
	public void setHashPipeId(SlimJxtaID id ) {
		this.CommPipeID = id;
		
	}

	@Override
	public void setHashRemotePeerId(SlimJxtaID id) {
		this.PID= id;
		
	}
/*
	@Override
	public BlockingQueue<Serializable> getRecvingQueue() {
		return this.RecvingQueue;
	}*/

	@Override
	public void addSendingObject(Serializable obj) throws InterruptedException {
		this.SendingQueue.put(obj);
		
	}

	@Override
	public void close() throws IOException, InterruptedException {
		DataInstructionContainer d = new DataInstructionContainer();
		d.setInstruction(Types.DataInstruction.JOBDONE);
		this.SendingQueue.put(d);
		this.Bound = false;
	}

	@Override
	public LinkType getConnectionType() {
		return Types.LinkType.INDIRECT;
	}
/*
	@Override
	public synchronized boolean hasSentUserInitClass(){
		return SentUserInitClass;
	}
	public synchronized void setSentUserInitClass(boolean bol){
		SentUserInitClass = bol;
	}
*/

	@Override
	public Long getRemoteNodeConnectionDataID() {
		return RemoteNodeConnectionDataID;
	}

	@Override
	public String getRemoteNodeConnectionPipeID() {
		return RemoteNodeConnectionPipeID;
	}



	@Override
	public PipeID getUniqueDependencyID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDependencyID(HierarchicalDependencyID id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniqueDependencyID(PipeID pipeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HierarchicalDependencyID getDependencyID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConnectionChangeListener(ConnectionChangeListener monitor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDataflowSource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDataflowSource(boolean set) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCachedObjSize() {
		// TODO Auto-generated method stub
		return "deprecated connection manager";
	}


	@Override
	public RawByteEncoder getRawByteEncoder() {
		// TODO Auto-generated method stub
		return null;
	}
}
