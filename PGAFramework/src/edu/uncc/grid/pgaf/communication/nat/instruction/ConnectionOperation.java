package edu.uncc.grid.pgaf.communication.nat.instruction;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;

public class ConnectionOperation implements Externalizable {
	public static final byte HAND_SHAKE = 1;
	public static final byte SET_BOUND_TRUE = 2;
	public static final byte SET_BOUND_FALSE = 3;
	public static final byte SOCKET_CREATED = 4;
	public static final byte REQUEST_CLOSE = 5;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1356206029225670088L;

	private SlimJxtaID HashSrcPeerID;
	private SlimJxtaID HashCPipeID;
	private byte Operation;

	public SlimJxtaID getHashSrcPeerID() {
		return HashSrcPeerID;
	}

	public void setHashSrcPeerID(SlimJxtaID hashSrcPeerID) {
		HashSrcPeerID = hashSrcPeerID;
	}

	public SlimJxtaID getHashCPipeID() {
		return HashCPipeID;
	}

	public void setHashCPipeID(SlimJxtaID hashCPipeID) {
		HashCPipeID = hashCPipeID;
	}

	public byte getOperation() {
		return Operation;
	}

	public void setOperation(byte operation) {
		Operation = operation;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		HashSrcPeerID = new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, in);
		HashCPipeID = new SlimJxtaID(CommunicationConstants.TUNNEL_ID_ACC, in);
		Operation = in.readByte();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		if( HashSrcPeerID == null || HashCPipeID == null){
			throw new NullPointerException();
		}
		HashSrcPeerID.putInStream(out);
		HashCPipeID.putInStream(out);
		out.writeByte(Operation);
	}
	
}
