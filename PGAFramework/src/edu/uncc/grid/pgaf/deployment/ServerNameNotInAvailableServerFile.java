package edu.uncc.grid.pgaf.deployment;

public class ServerNameNotInAvailableServerFile extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7070969789002466273L;
	String msg = "The server name specified is not in the Available Servers file.  Add the server in that file first"; 
	
	public ServerNameNotInAvailableServerFile(String m){
		super();
		msg = m;
	}
	public ServerNameNotInAvailableServerFile(){
		super();
	}
	@Override
	public String getMessage() {
		return msg;
	}

}
