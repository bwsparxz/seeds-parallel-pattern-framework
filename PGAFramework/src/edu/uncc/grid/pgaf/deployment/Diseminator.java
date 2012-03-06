package edu.uncc.grid.pgaf.deployment;

public interface Diseminator {
	public static final String RDV_SEED_FILE = "/seed_file";
	public static final String AVAILABLE_SERVERS = "/AvailableServers.txt";
	/**
	 * put the directory path from the local machine into the remote host
	 * @param str
	 */
	public void putDirectory(String str);
	/**
	 * Returns the home directory on the remote host
	 * @return
	 */
	public String getHomeDir();
	/**
	 * Submits the job with the Node class to start one node on 
	 * the remote host
	 */
	public void SubmitPeerJob();
	/**
	 * Returns the absolute path to the pgaf libraries on 
	 * the remote host
	 * @return
	 */
	public String getPGAF_PATH();
	
}
