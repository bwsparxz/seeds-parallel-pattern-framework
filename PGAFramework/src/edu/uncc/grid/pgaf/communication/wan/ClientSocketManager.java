package edu.uncc.grid.pgaf.communication.wan;

import edu.uncc.grid.pgaf.RawByteEncoder;


public abstract class ClientSocketManager extends SocketManager {
	public ClientSocketManager(String threadName, RawByteEncoder enc) {
		super(threadName, enc);
	}
	public abstract void waitUntilConected();
	public abstract boolean isConected();
}
