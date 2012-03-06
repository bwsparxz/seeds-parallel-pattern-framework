/* * Copyright (c) Jeremy Villalobos 2009
 *   * All rights reserved  
 *  
 */
package edu.uncc.grid.pgaf.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.globus.cog.abstraction.impl.common.ProviderMethodException;
import org.globus.cog.abstraction.impl.common.task.IllegalSpecException;
import org.globus.cog.abstraction.impl.common.task.InvalidProviderException;
import org.globus.cog.abstraction.impl.common.task.InvalidSecurityContextException;
import org.globus.cog.abstraction.impl.common.task.InvalidServiceContactException;
import org.globus.cog.abstraction.impl.common.task.TaskSubmissionException;
import org.globus.cog.abstraction.interfaces.Identity;
import org.globus.cog.abstraction.interfaces.Service;
import org.globus.cog.abstraction.interfaces.TaskHandler;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import edu.uncc.grid.pgaf.exceptions.DirectoryDoesNotExist;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;

/**
 * 
 * The diseminator will do jobs on a specific remote system.  The Deployer managers a list of Disseminators that work on multiple remote grid nodes.<br>
 * The diseminator only manages a single grid node.  It has the methods to transfer files, delete files and submit jobs to fork and condors. <br>
 * It does this by using Java Cog libraries</br>
 * 
 * 
 * 
 * @author jfvillal
 *
 */
public class SSHDiseminator {
	private static final String JAVA_VM_STANDARD_PATH = "/usr/java/jdk1.5/bin/java";
	private static final String FILE_PROVIDER = "gridftp";
	private static final String JOB_PROVIDER = "gt4.0.0";
	private static final String PGAF_PATH = "Pgaf"; 
	private static final String LIB_PATH = "/lib";
	
	//private static final int DEBUG_JXTA_SLEEP_WHEN_FINISHED = 100;
	
	private String HomeDir;
	/**
	 * The identity is used to identify a particullar gsiftp session.  this type of file
	 * manipulation procedures are much like ftp implementation.  The getSessionID gets
	 * a session ID, and that allows multiple file manipulation method to work on that server
	 * after that.
	 */
	private Identity SessionID;
	/**
	 * Manages a cog task 
	 */
	private TaskHandler handler;
	/**
	 * cog service manager
	 */
	Service ServerService;
	/**
	 * the server this disseminator is in charge of managing
	 */
	String ServerName;
	/**
	 * Gram port for this server
	 */
	int GramPort;
	/**
	 * GridFtp port fot his server
	 */
	int GridFtpPort;
	String JobScheduler;
	/**
	 * The Job scheduler used for this server
	 */
	String GridNodeName;
	/**
	 * Sets the type of p2p node for the remote process
	 */
	String NodeRole;
	/**
	 * This has the path of the shuttle folder on the local machine
	 */
	String LocalSourceDirectory;
	/**
	 * The number of cores on the server being managed
	 */
	int CpuCoreCount;
	/**
	 * Specifies which type of roll the server has.  Current types are
	 * specified in the enum's signature.
	 */
	//Types.DataFlowRoll DataFRoll;
	
	/**
	 * Used to specify if old PGAF shuttle was deleted from the server.
	 */
	//boolean DeletedFile;
	
	/**
	 * Logger to report errors during the deployment phase.  The 
	 * file is placed at ./Deployer.log
	 */
	Logger Log;
	/**
	 * We used to moe the port for each node in case there where multiple nodes on one 
	 * machine.  Since each machine now only run one node, and each node can have multiple threads
	 * to take advantage of multiple processors and cores.  This variable may not be of much use 
	 * anymore.
	 */
	int PortCounter;
	/**
	 * The class name for the Interface the user is created.
	 */
	//private Class UserModuleName;
	/**
	 * Useed to pass the user's remote arguments
	 */
	//private String[] RemoteArguments;
	/**
	 * Determines if the server being manage will deploy nodes using Java sockets or not.
	 */
	private boolean JavaSocketUsed;
	
	Connection SshConnection;
	
	/**
	 * Constructor.  creates an ssh connection the the server.
	 * @param server_name name of the server. example: coit-grid02.uncc.edu
	 * @param port_number port number. example 2811
	 * @throws IOException 
	 * @throws InvalidProviderException
	 * @throws ProviderMethodException
	 */
	public SSHDiseminator(
			String server_name
			, String grid_name
			, String local_source_directory
			, int cpu_core_count
			, boolean java_socket_used
			, Types.PGANodeRole role
			, Logger log) throws IOException{	 
		 HomeDir = null;
		 
		 ServerName = server_name;
		 
		 GridNodeName = grid_name;
		 LocalSourceDirectory = local_source_directory;
		 
		 
		 CpuCoreCount = cpu_core_count;
		 
		 Log = log;
		 
		 NodeRole = role.toString();
		 
		 JavaSocketUsed = java_socket_used;
		 
		SshConnection = new Connection( this.ServerName);
		SshConnection.connect();
			
		String keyfile_str = System.getProperty("user.home") + "/.ssh/id_dsa";
		File keyfile = new File( keyfile_str);
		String user_name = System.getProperty("user.name");
		boolean isAuthenticated = SshConnection.authenticateWithPublicKey(user_name, keyfile, "");

		if (isAuthenticated == false)
			throw new IOException("Authentication failed.");
		 
	}
	/**
	 * Sets the user's remote arguments
	 * @param args
	 */
	//public void setRemoteArguments(String args){
	//	this.RemoteArguments = args;
	//}
	
	/**
	 * Creates the classpath for the job submitter.
	 * @return
	 * @throws HomeDirNotSetException 
	 * @throws DirectoryDoesNotExist 
	 * @throws HomeDirNotSetException 
	 * @throws IOException 
	 */
	private String getClasspath(String source_directory) throws  DirectoryDoesNotExist, HomeDirNotSetException, IOException{
		String classpath = getHomeDir() + PGAF_PATH + LIB_PATH   + ":";;
		List<String> jxta_libs = getJarFromDir( source_directory+  LIB_PATH);
		Iterator<String> it = jxta_libs.iterator();
		while( it.hasNext()){
			classpath += getHomeDir() + PGAF_PATH + LIB_PATH + "/" + it.next() + ":";
		}
		return classpath;
	}
	/**
	 * Is mainly used to get the names of the jar file that are at the source directory 
	 * the source directory is called pgaf and the libs are at directory lib
	 * pgaf/lib
	 * @param directory_path
	 * @return
	 * @throws DirectoryDoesNotExist
	 */
	private List<String> getJarFromDir(String directory_path)
		throws DirectoryDoesNotExist{
		List<String> list = new ArrayList<String>();
		File dir = new File(directory_path);
		String[] children = dir.list();
		if( children == null){
			Log.log(Level.SEVERE,"The pgaf Directory does not exist or is not a directory");
			throw new DirectoryDoesNotExist();
		}else{
			for(int i = 0; i < children.length; i++){
				//System.out.println(children[i]);
				list.add(children[i]);
			}
		}
	return list;
	}
	/**
	 * Returns the home directory for that server.  It figures out the home dir if not available. 
	 * If available, just returns the value.
	 * @return
	 * @throws HomeDirNotSetException
	 * @throws IOException 
	 * @throws HomeDirNotSetException 
	 */
	public String getHomeDir() throws IOException, HomeDirNotSetException {
		if( this.HomeDir == null){
			Session sess = SshConnection.openSession();
			sess.execCommand("echo $HOME");
			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
			HomeDir = br.readLine() + "/";
			if( HomeDir == null){
				throw new HomeDirNotSetException();
			}
			sess.close();
		}
		
		return HomeDir;
	}
	/**
	 * This function will advance the variable PortCounter
	 * use with care
	 * @return
	 */
	private int getPort(){
		return Node.DEFAULT_JXTA_NODE_PORT + PortCounter++;
	}
	/**
	 * returns the path for that seed file on the server being managed.
	 * This is used to submit a job with the appropiate arguments
	 * @return
	 * @throws HomeDirNotSetException
	 * @throws IOException 
	 */
	public String getPGAFPath() throws HomeDirNotSetException, IOException{
		return getHomeDir() + PGAF_PATH;
	}
	
	/**
	 * Will delete a directory. <br>
	 * provide absolute path.
	 * 
	 * @param remote_dir
	 * @throws IOException 
	 * 
	 */
	public void DeleteDirectory(String remote_dir) throws IOException {
		//this.setDeletedFile(false);
		Session sess = SshConnection.openSession();
		sess.execCommand("rm -rf " + remote_dir);
		Log.log(Level.FINER, "Delete status: " + sess.getExitStatus() );
		sess.close();
	}

	/**
	 * Submits a job to the server.  Most of the argument are obtain from the class's
	 * fields.  The fields in turned where loaded into the Disseminator by the Deploye who
	 * read them from the AvailableServers.txt file
	 * @throws IOException 
	 * @throws HomeDirNotSetException
	 * @throws InvalidProviderException
	 * @throws ProviderMethodException
	 * @throws IllegalSpecException
	 * @throws InvalidSecurityContextException
	 * @throws InvalidServiceContactException
	 * @throws TaskSubmissionException
	 * @throws DirectoryDoesNotExist
	 * @throws IOException 
	 * @throws HomeDirNotSetException 
	 * @throws DirectoryDoesNotExist 
	 */
	
	public void SubmitPeerJob() throws IOException, HomeDirNotSetException, DirectoryDoesNotExist{
		
		Session sess = SshConnection.openSession();
		int port = getPort();
		String options = 	Node.ShellPGAFPathOption 			+ " " + getPGAFPath() 
		+" "+ Node.ShellGridNodeNameOption	+ " " + GridNodeName 
		+" "+ Node.ShellPortOption 			+ " " + port  
		+" "+ Node.ShellCpuCountOption 		+ " " + CpuCoreCount
		+" "+ Node.ShellSetNodeRole 	    + " " + NodeRole
		+" "+ ( this.JavaSocketUsed     ? Node.ShellJavaSocketOption                                       : "");
		
		String stdout = "stdout_pgaf_"+  this.ServerName +".txt";
		String stderr = "stderr_pgaf" + this.ServerName + ".txt";
		
							/*java executable                     + classpath                                   + node class*/
		String command = JAVA_VM_STANDARD_PATH
							//+ " -Xdebug -Xrunjdwp:transport=dt_socket,address=50007,server=y,suspend=y  "
							+ " -classpath " + getClasspath(this.LocalSourceDirectory) + " edu.uncc.grid.pgaf.p2p.Node "
							/* and the options for the Node class*/
							/*TODO debuging flag is on */
							+ options + " > " + stdout + " 2> " + stderr  + " & ";
		
		this.Log.log(Level.FINE, command );
		sess.execCommand( command );
		
		sess.close(); //log off
		
		/**
		 * Jan 9 2009
		 * I thinks this only works on bash ssh shells.  No other shells, and no other OS, but it is enough for my goals.
		 */
	}
	
	/**
	 * The function puts a directory from source_dir on the client machine into 
	 * destination_dir on the server machine (with globus).  The destination directory should be specified in 
	 * the format //path/to/dir  Using no slashes at the beggining set the user's home directory
	 * the source_dir can be spcified as /path/to/dir
	 * by default  
	 * @param service
	 * @param source_dir
	 * @param destination_dir
	 * @throws IOException 
	 * @throws IllegalSpecException
	 * @throws InvalidSecurityContextException
	 * @throws InvalidServiceContactException
	 * @throws TaskSubmissionException
	 * @throws SessionIDNotSetException 
	 */
	public void putDirector(  String destination_dir ) throws IOException {
		try{
			SCPClient scp = new SCPClient( SshConnection );
			SFTPv3Client sftp = new SFTPv3Client( SshConnection );
			File source = new File( this.LocalSourceDirectory );
			Log.log(Level.INFO , " destination_dir = " + destination_dir );
			putDirRecursive( source, destination_dir, sftp, scp , false);
		}catch( IOException e ){
			/**
			 * If at first you don'g succeed, dust yourself off and try again.
			 */
			Log.log(Level.WARNING, "Hit error on transfer directory to " + this.ServerName + ", trying again ... ");
			try{ SshConnection.close();	}catch( Exception ee){}
			SshConnection = new Connection( this.ServerName);
			SshConnection.connect();
			String keyfile_str = System.getProperty("user.home") + "/.ssh/id_dsa";
			File keyfile = new File( keyfile_str);
			String user_name = System.getProperty("user.name");
			boolean isAuthenticated = SshConnection.authenticateWithPublicKey(user_name, keyfile, "");
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");
			SCPClient scp = new SCPClient( SshConnection );
			SFTPv3Client sftp = new SFTPv3Client( SshConnection );
			File source = new File( this.LocalSourceDirectory );
			Log.log(Level.INFO , " destination_dir = " + destination_dir );
			putDirRecursive( source, destination_dir, sftp, scp , false);
		}
	}
	public static void putDirRecursive( File source, String destination,  SFTPv3Client sftp, SCPClient  scp, boolean high_level) throws IOException{
		if( source == null) return ; 
		if( source.isDirectory() ){
			if( !high_level){
				String new_dir = destination;
				sftp.mkdir(new_dir , 0755);
				for(File f : source.listFiles()){
					putDirRecursive( f, destination , sftp, scp, true);
				}
			}else{
				String new_dir = destination + "/" + source.getName();
				sftp.mkdir(new_dir , 0755);
				for(File f : source.listFiles()){
					putDirRecursive( f, new_dir , sftp, scp, true);
				}
			}
		}else{
			long start = System.currentTimeMillis();
			scp.put(source.toString(), destination);
			long stop = System.currentTimeMillis() - start;
			System.out.println( "Transfer " + source  + " in " + stop + "ms.");
		}
	}
	/*
	 * The functions below are getters and setter. 
	 * 
	 */
	
	public boolean isSessionIDSet(){
		return SessionID != null;
	}
	public synchronized Identity getSessionID() {
		return SessionID;
	}
	public synchronized void setSessionID(Identity sessionID) {
		SessionID = sessionID;
	}

	public  synchronized void setHomeDir(String homeDir) {
		HomeDir = homeDir;
	}
	public static String getPGAF_PATH() {
		return PGAF_PATH;
	}
	/*public synchronized boolean isDeletedFile() {
		return DeletedFile;
	}
	public synchronized void setDeletedFile(boolean deletedFile) {
		DeletedFile = deletedFile;
	}*/
	
}
