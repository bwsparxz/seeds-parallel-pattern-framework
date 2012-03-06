package edu.uncc.grid.pgaf.communication.wan;

import java.io.IOException;
import java.util.List;

import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;

/**
 * A general version of the JxtaSocketDispatcher and the JavaSocketDispatcher
 * @author jfvillal
 *
 */
public abstract class SocketDispatcher extends Thread{
	public abstract List<ConnectionManager> getServerSMList();
	public abstract void setStop(boolean set);
	public abstract void closeServers() throws IOException, InterruptedException;
	public abstract SlimJxtaID getServiceHPipeID() ;
	public abstract void setOnConnectionEstablishedListener(ConnectionEstablishedListener listener);
}
