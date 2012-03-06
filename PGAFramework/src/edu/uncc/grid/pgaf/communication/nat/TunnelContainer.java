/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.communication.nat;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.datamodules.Data;
/**
 * Used to send and received information between the TunnelClient and 
 * TunnelServer.  It's overhead may range in the hundreds of bytes.  the implementation
 * can improve in the future.  At the moment only considered for future work.  One thing could
 * be to cast the String PipeID into a Long ( going from 33+ bytes to 8).  But real improvement
 * would require elimination some of the serialization.
 * @author jfvillal
 */
public class TunnelContainer implements Externalizable {
	
	/**
	 * The id of the peer that created the container.
	 */
	SlimJxtaID PeerId;
	/**
	 * The id of the connection pipe 
	 */
	SlimJxtaID PipeId;
	/**
	 * The users data object
	 */
	Serializable Payload;
	/**
	 * 
	 * @param pipe_id
	 * @param peer_id
	 * @param t
	 */
	public TunnelContainer(SlimJxtaID pipe_id,  SlimJxtaID peer_id, Serializable t){
		PipeId = pipe_id;
		PeerId = peer_id;
		Payload = t;
	}
	/**
	 * Required by Externalizable 
	 */
	public TunnelContainer(){
		
	}
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		PeerId = new SlimJxtaID( CommunicationConstants.TUNNEL_ID_ACC, in);
		PipeId = new SlimJxtaID( CommunicationConstants.TUNNEL_ID_ACC, in);
		Payload = (Serializable) in.readObject();
	}	
	public void writeExternal(ObjectOutput out) throws IOException {
		if( PeerId == null || PipeId == null){
			throw new NullPointerException();
		}
		PeerId.putInStream(out);
		PipeId.putInStream(out);
		out.writeObject(Payload);
	}
	
	public Serializable getPayload() {
		return Payload;
	}
	public void setPayload(Serializable payload) {
		Payload = payload;
	}

	public SlimJxtaID getPeerId() {
		return PeerId;
	}

	public void setPeerId(SlimJxtaID peerId) {
		PeerId = peerId;
	}

	public SlimJxtaID getPipeId() {
		return  PipeId;
	}

	public void setPipeId(SlimJxtaID pipeId) {
		PipeId = pipeId;
	}

	
	
}
