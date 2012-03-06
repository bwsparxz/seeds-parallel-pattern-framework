package edu.uncc.grid.pgaf.communication.wan;

import edu.uncc.grid.pgaf.communication.ConnectionManager;

public interface ConnectionEstablishedListener {
	public boolean onConnectionEstablished(ConnectionManager man);
}
