package edu.uncc.grid.pgaf.communication;

public class CommunicationLinkTimeoutException extends Exception {
	String Message;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public CommunicationLinkTimeoutException( String msg){
		Message = msg;
	}
	public CommunicationLinkTimeoutException(){
		Message = " The time alotted for this connection has expired. ";
	}
	@Override
	public String getMessage() {
		return Message;
	}

}
