package edu.uncc.grid.pgaf.p2p;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;

public class NetDetectiveEmulator extends NetworkDetective {
	private Types.WanOrNat NetRoll;
	public NetDetectiveEmulator( Types.WanOrNat net_roll) throws IOException{
		NetRoll = net_roll;
	}
	@Override
	public boolean closePortForwardOnRouter(int port) {
		Node.getLog().log(Level.FINE, "emulating closing the port");
		return true;
	}

	@Override
	public boolean createPortForwardOnRouter(int port) {
		Node.getLog().log(Level.FINE, "emulating creation of port foward");
		return (this.NetRoll == Types.WanOrNat.NAT_UPNP);
	}

	@Override
	public String getLANAddress() {	
		return LANAddress;
	}

	@Override
	public String getWANAddress() {
		return WANAddress;
	}

	@Override
	public String findOutWANIP() {
		return WANAddress;
	}

	@Override
	public boolean isInsideaNATNetwork() throws UnknownHostException,
			SocketException {
		Node.getLog().log(Level.FINE, "emulating closing the port");
		return (this.NetRoll != Types.WanOrNat.WAN);
	}

	@Override
	public void setLANAddress(String address) {
		LANAddress = address;
	}

	@Override
	public void setWANAddress(String address) {
		WANAddress = address;
	}

}
