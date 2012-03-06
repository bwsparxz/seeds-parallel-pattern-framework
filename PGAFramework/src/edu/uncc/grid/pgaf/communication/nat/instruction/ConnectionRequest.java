package edu.uncc.grid.pgaf.communication.nat.instruction;

import java.io.Serializable;

import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;
/**
 * Connection request will request a connection to the tunnel server.
 * @author jfvillal
 *
 */
public class ConnectionRequest implements Serializable {
	public static final byte REQUEST_CONNECT = 1;
	public static final byte REQUEST_DISPATCHER = 2;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2144652389839100534L;
	/**
	 * PeerID of the peer creating the Instruction
	 */
	private PeerID SrcPeer;
	/**
	 * Local communication channel PipeID
	 */
	private PipeID CPipeID;
	/**
	 * Remote communication channel PipeID
	 */
	private PipeID RCPipeID;
	/**
	 * Data id (this is the remote data id, the id we want to connect to)
	 */
	private Long DataID;
	/**
	 * Wan address for the RDV hosting the connection for the LAN-locked node 
	 */
	private String WanAddress;
	/**
	 * Lan address for the RDV, this should not be necesary
	 */
	private String LanAddress;
	/**
	 * port for the Java socket. If a Java socket is used.
	 */
	private int Port;
	/**
	 * to specify if the target node is wan or nat
	 */
	private String WanOrNat;
	/**
	 * sets the pattern to which we are connecting
	 */
	private PipeID PatternID;
	
	/**
	 * The pipe id from the remote node.  this is new on 1/3/10 to enable mpi-like connections
	 */
	private String LocalPipeID;
	/**
	 * the Data id from the remote node. this is new on 1/3/10 to enable mpi-like connections.  this is the 
	 * id of the data object on this node.
	 */
	private Long LocalDataID;
	
	
	private byte VPInstruction;
	
	public PipeID getCPipeID() {
		return CPipeID;
	}
	
	public PeerID getSrcPeer() {
		return SrcPeer;
	}

	public void setSrcPeer(PeerID srcPeer) {
		SrcPeer = srcPeer;
	}
	public void setCPipeID(PipeID pipeID) {
		CPipeID = pipeID;
	}

	public PipeID getRCPipeID() {
		return RCPipeID;
	}

	public void setRCPipeID(PipeID pipeID) {
		RCPipeID = pipeID;
	}
	public Long getDataID() {
		return DataID;
	}

	public void setDataID(Long dataID) {
		DataID = dataID;
	}

	public String getWanAddress() {
		return WanAddress;
	}

	public void setWanAddress(String wanAddress) {
		WanAddress = wanAddress;
	}

	public String getLanAddress() {
		return LanAddress;
	}

	public void setLanAddress(String lanAddress) {
		LanAddress = lanAddress;
	}

	public int getPort() {
		return Port;
	}

	public void setPort(int port) {
		Port = port;
	}

	public byte getVPInstruction() {
		return VPInstruction;
	}

	public void setVPInstruction(byte vPInstruction) {
		VPInstruction = vPInstruction;
	}

	public String getWanOrNat() {
		return WanOrNat;
	}

	public void setWanOrNat(String wanOrNat) {
		WanOrNat = wanOrNat;
	}

	public PipeID getPatternID() {
		return PatternID;
	}

	public void setPatternID(PipeID patternID) {
		PatternID = patternID;
	}

	public String getLocalPipeID() {
		return LocalPipeID;
	}

	public void setLocalPipeID(String localPipeID) {
		LocalPipeID = localPipeID;
	}

	public Long getLocalDataID() {
		return LocalDataID;
	}

	public void setLocalDataID(Long localDataID) {
		LocalDataID = localDataID;
	}
	
}
