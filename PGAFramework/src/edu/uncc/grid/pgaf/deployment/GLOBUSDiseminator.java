/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 *  
 *  Other libraries and code used.
 *  
 *  This framework also used code and libraries from the Java
 *  Cog Kit project.  http://www.cogkit.org
 *  
 *  This product includes software developed by Sun Microsystems, Inc. 
 *  for JXTA(TM) technology.
 *  
 *  Also, code from UPNPLib is used
 *  
 *  And some extremely modified code from Practical JXTA is used.  
 *  This product includes software developed by DawningStreams, Inc.    
 *  
 */
package edu.uncc.grid.pgaf.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.globus.cog.abstraction.impl.common.AbstractionFactory;
import org.globus.cog.abstraction.impl.common.ProviderMethodException;
import org.globus.cog.abstraction.impl.common.StatusEvent;
import org.globus.cog.abstraction.impl.common.task.ExecutionServiceImpl;
import org.globus.cog.abstraction.impl.common.task.FileOperationSpecificationImpl;
import org.globus.cog.abstraction.impl.common.task.GenericTaskHandler;
import org.globus.cog.abstraction.impl.common.task.IllegalSpecException;
import org.globus.cog.abstraction.impl.common.task.InvalidProviderException;
import org.globus.cog.abstraction.impl.common.task.InvalidSecurityContextException;
import org.globus.cog.abstraction.impl.common.task.InvalidServiceContactException;
import org.globus.cog.abstraction.impl.common.task.JobSpecificationImpl;
import org.globus.cog.abstraction.impl.common.task.ServiceContactImpl;
import org.globus.cog.abstraction.impl.common.task.ServiceImpl;
import org.globus.cog.abstraction.impl.common.task.TaskImpl;
import org.globus.cog.abstraction.impl.common.task.TaskSubmissionException;
import org.globus.cog.abstraction.impl.file.GridFileImpl;
import org.globus.cog.abstraction.interfaces.ExecutionService;
import org.globus.cog.abstraction.interfaces.FileOperationSpecification;
import org.globus.cog.abstraction.interfaces.Identity;
import org.globus.cog.abstraction.interfaces.JobSpecification;
import org.globus.cog.abstraction.interfaces.SecurityContext;
import org.globus.cog.abstraction.interfaces.Service;
import org.globus.cog.abstraction.interfaces.ServiceContact;
import org.globus.cog.abstraction.interfaces.Status;
import org.globus.cog.abstraction.interfaces.StatusListener;
import org.globus.cog.abstraction.interfaces.Task;
import org.globus.cog.abstraction.interfaces.TaskHandler;

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
/*
 * Cog lib problem note: RMDIR on the file operation task does not work.
 * we are using a job submit to do the job instead
 */
public class GLOBUSDiseminator {
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
	boolean DeletedFile;
	
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
	/**
	 * stores the type of p2p node to be used for the remote process.
	 */
	String NodeRole;
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
	/**
	 * Debug, but I don't remember what.  The debug option will freeze until a Rendezvous is found.
	 */
	private static boolean FrameworkDebug;
	/**
	 * Constructor
	 * @param server_name name of the server. example: coit-grid02.uncc.edu
	 * @param port_number port number. example 2811
	 * @throws InvalidProviderException
	 * @throws ProviderMethodException
	 */
	public GLOBUSDiseminator(
			String server_name
			, int gsiftp_port_number
			, int gram_port_number
			, String grid_name
			, String job_scheduler
			, String local_source_directory
			//, Class user_mod_name
			, int cpu_core_count
			, boolean java_socket_used
			, Types.PGANodeRole role
			, Logger log) throws InvalidProviderException, ProviderMethodException{
		SessionID = null;
		ServerService = new ServiceImpl(Service.FILE_OPERATION);
		
		handler = new GenericTaskHandler();
		 
		 ServerService.setProvider(GLOBUSDiseminator.FILE_PROVIDER);
		 SecurityContext securityContext =
		   AbstractionFactory.newSecurityContext(GLOBUSDiseminator.FILE_PROVIDER);
		 securityContext.setCredentials(null);
		 ServerService.setSecurityContext(securityContext);
		 ServiceContact serviceContact =
		     new ServiceContactImpl( server_name +":" + gsiftp_port_number );
		 ServerService.setServiceContact(serviceContact);
		 
		 HomeDir = null;
		 PortCounter = 0;
		 
		 GridFtpPort = gsiftp_port_number;
		 GramPort = gram_port_number;
		 ServerName = server_name;
		 JobScheduler = job_scheduler;
		 
		 GridNodeName = grid_name;
		 LocalSourceDirectory = local_source_directory;
		 
		 NodeRole = role.toString();
		 
		 DeletedFile = false;
		 
		 
		 FrameworkDebug = false;
		 
		 CpuCoreCount = cpu_core_count;
		 
		 Log = log;
		 
		 JavaSocketUsed = java_socket_used;
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
	 */
	private String getClasspath(String source_directory) throws  DirectoryDoesNotExist, HomeDirNotSetException{
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
	 * Returns the home directory for that server.
	 * @return
	 * @throws HomeDirNotSetException
	 */
	private String getHomeDir() throws HomeDirNotSetException {
		if( HomeDir == null){
			throw new HomeDirNotSetException();
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
	 */
	public String getPGAFPath() throws HomeDirNotSetException{
		return getHomeDir() + PGAF_PATH;
	}
	
	/**
	 * Will delete a directory. <br>
	 * provide absolute path.
	 * 
	 * @param remote_dir
	 * @throws HomeDirNotSetException
	 * @throws InvalidProviderException
	 * @throws ProviderMethodException
	 * @throws IllegalSpecException
	 * @throws InvalidSecurityContextException
	 * @throws InvalidServiceContactException
	 * @throws TaskSubmissionException
	 * @throws DirectoryDoesNotExist
	 */
	public void DeleteDirectory(String remote_dir) throws HomeDirNotSetException, InvalidProviderException
	, ProviderMethodException, IllegalSpecException, InvalidSecurityContextException
	, InvalidServiceContactException, TaskSubmissionException, DirectoryDoesNotExist{
		
		this.setDeletedFile(false);
		
		Task task=new TaskImpl("NewJob",Task.JOB_SUBMISSION);
		JobSpecification spec=new JobSpecificationImpl();
		spec.setExecutable( "/bin/rm" );
		
		//int port = getPort();
		/*
		String run_peer_command = 
		" -classpath " + getClasspath(this.LocalSourceDirectory)  				//classpath 
		+ " edu.uncc.grid.pgaf.p2p.Node " 				//class name
		+ getSeedFilePath()								//jxta seed file
		+ " " + GridNodeName 							//grid node name
		+ " " + port									//port number
		+ " -debug "									//ignores network characteristics 
														//and allows multiple director 
														//rdv on the same host
		;	
		*/
		//Log.log(Level.FINE,remote_dir);
		
		spec.setArguments(" -rf " + remote_dir);
		
		spec.setStdOutput("pgaf_dir_del.out");
		spec.setStdError("pgaf_dir_del.err");
		
		spec.setBatchJob(true);
		//spec.setDirectory(getHomeDir());
		
		task.setSpecification(spec);
		
		ExecutionService service=new ExecutionServiceImpl();
		service.setProvider(GLOBUSDiseminator.JOB_PROVIDER);
		
		SecurityContext securityContext=
		AbstractionFactory.newSecurityContext(GLOBUSDiseminator.JOB_PROVIDER);
		securityContext.setCredentials(null);
		service.setSecurityContext(securityContext);
		
		ServiceContact serviceContact= new
		ServiceContactImpl(this.ServerName + ":" + this.GramPort );
		
		//System.out.println("after ServiceContact ");
			
		service.setServiceContact(serviceContact);
		service.setJobManager(this.JobScheduler);//fork or condor
		
		task.addService(service);
		
		TaskHandler handler=
		AbstractionFactory.newExecutionTaskHandler(GLOBUSDiseminator.JOB_PROVIDER);
		
		task.addStatusListener(new DeleteDirectoryListener(this, " DeleteDir() "));
		
		Log.log(Level.FINE,"submitting...");
		handler.submit(task);	  
		//System.out.println("end");

	}

	/**
	 * Submits a job to the server.  Most of the argument are obtain from the class's
	 * fields.  The fields in turned where loaded into the Disseminator by the Deploye who
	 * read them from the AvailableServers.txt file
	 * @throws HomeDirNotSetException
	 * @throws InvalidProviderException
	 * @throws ProviderMethodException
	 * @throws IllegalSpecException
	 * @throws InvalidSecurityContextException
	 * @throws InvalidServiceContactException
	 * @throws TaskSubmissionException
	 * @throws DirectoryDoesNotExist
	 */
	
	public void SubmitPeerJob() throws HomeDirNotSetException, InvalidProviderException
				, ProviderMethodException, IllegalSpecException, InvalidSecurityContextException
				, InvalidServiceContactException, TaskSubmissionException, DirectoryDoesNotExist{
		Task task=new TaskImpl("NewJob",Task.JOB_SUBMISSION);
		JobSpecification spec=new JobSpecificationImpl();
		spec.setExecutable( JAVA_VM_STANDARD_PATH );
		
		int port = getPort();
		
		String options = 	Node.ShellPGAFPathOption 			+ " " + getPGAFPath() 
							+" "+ Node.ShellGridNodeNameOption	+ " " + GridNodeName 
							+" "+ Node.ShellPortOption 			+ " " + port 
						//	+" "+ Node.ShellInterfaceNameOption + " " + this.UserModuleName.getName() 
							+" "+ Node.ShellCpuCountOption 		+ " " + CpuCoreCount
							+" "+ Node.ShellSetNodeRole 		+ " " + NodeRole
						//	+" "+ ( RemoteArguments != null ? Node.ShellUserRemoteArgumentsOption + " " + this.RemoteArguments : "")
						//	+" "+ Node.ShellDataFlowRollOption 	+ " " + this.DataFRoll.toString() 
							+" "+ ( this.JavaSocketUsed     ? Node.ShellJavaSocketOption                                       : "");
		
		
		
		
		String run_peer_command = " -classpath " + getClasspath(this.LocalSourceDirectory)	+ " edu.uncc.grid.pgaf.p2p.Node " + options ; 
		
		Log.log(Level.FINE,JAVA_VM_STANDARD_PATH + " " + run_peer_command);
		
		spec.setArguments(run_peer_command);
		
		spec.setStdOutput("stdout_pgaf_"+  this.ServerName +".txt");
		spec.setStdError("stderr_pgaf" + this.ServerName + ".txt");
		
		spec.setBatchJob(true);
		spec.setDirectory(getHomeDir());
		
		
		task.setSpecification(spec);
		
		ExecutionService service=new ExecutionServiceImpl();
		service.setProvider(GLOBUSDiseminator.JOB_PROVIDER);
		
		SecurityContext securityContext=
		AbstractionFactory.newSecurityContext(GLOBUSDiseminator.JOB_PROVIDER);
		securityContext.setCredentials(null);
		service.setSecurityContext(securityContext);
		
		ServiceContact serviceContact= new
			ServiceContactImpl(this.ServerName + ":" + this.GramPort );
				
		service.setServiceContact(serviceContact);
		service.setJobManager(this.JobScheduler);//fork or condor
		task.addService(service);
		
		
		TaskHandler handler=
		AbstractionFactory.newExecutionTaskHandler(GLOBUSDiseminator.JOB_PROVIDER);
			
		task.addStatusListener(new JobSubmitListener());
		
		
		
		Log.log(Level.FINE,"submitting...");
		handler.submit(task);	  
		
	}
	/**
	 * will start a file operation to get the home dir.  the output is obtain in a 
	 * HomeDirListener, which then stores the information in HomeDir field.
	 * @throws IllegalSpecException
	 * @throws InvalidSecurityContextException
	 * @throws InvalidServiceContactException
	 * @throws TaskSubmissionException
	 * @throws SessionIDNotSetException
	 */
	public void getHomeDirectory() throws IllegalSpecException, InvalidSecurityContextException
		, InvalidServiceContactException, TaskSubmissionException, SessionIDNotSetException{
		Task task = new TaskImpl("FindOutHomeDirPath", Task.FILE_OPERATION);
		/* note:
		 * even though it is deprecated, it will create an error if not used*/
		task.setProvider(GLOBUSDiseminator.FILE_PROVIDER); 
		/*end note*/
		FileOperationSpecification spec = new FileOperationSpecificationImpl();
		spec.setOperation(FileOperationSpecification.PWD);
		 
		task.setSpecification(spec);
		if(SessionID == null){
			throw new SessionIDNotSetException();
		}
		task.setAttribute("sessionID", SessionID);
		task.setService(Service.DEFAULT_SERVICE, this.ServerService);
		task.addStatusListener( new HomeDirListener(task, this) );
		
		handler.submit(task);
	}
	/**
	 *This function does not work 
	 *DOES NOT WORK
	 *DOES NOT WORK 
	 */
	/*public void deleteDirectory( String remote_dir) throws SessionIDNotSetException, IllegalSpecException
		, InvalidSecurityContextException, InvalidServiceContactException
		, TaskSubmissionException{
		Task task = new TaskImpl("mySecondTask", Task.FILE_OPERATION);
		task.setProvider(Diseminator.FILE_PROVIDER); //even though it is deprecated, it will create an error if not used
		FileOperationSpecification spec = new FileOperationSpecificationImpl();
		spec.setOperation(FileOperationSpecification.RMDIR);
		
		//spec.addArgument(this.LocalSourceDirectory);
		spec.addArgument(remote_dir);
		
		task.setSpecification(spec);
		if(SessionID == null){
			throw new SessionIDNotSetException();
		}
		task.setAttribute("sessionID", SessionID);
		task.setService(Service.DEFAULT_SERVICE, this.ServerService);
		task.addStatusListener(new RemoveDirListener(task));     
		handler.submit(task);
	}
	public class RemoveDirListener implements StatusListener{
		Task task;
		public RemoveDirListener( Task task ){
			this.task = task;
		}
		public void statusChanged(StatusEvent event) {
				Status status = event.getStatus();
				System.out.println("Status changed to "
						+ status.getStatusString());
	
		if (status.getStatusCode() == Status.COMPLETED) {
			*ArrayList list = (ArrayList) task.getAttribute("output");
			*Iterator it = list.iterator();
			*while(it.hasNext()){
			*	GridFileImpl file = (GridFileImpl) it.next();
			*	System.out.println(file.getName());
			*}
		}
		}
	}
	*/
	
	/**
	 * Will list the contents of a directory on the remote server.  The 
	 * string answer is returned on the listener ListDirListener.
	 * 
	 * the method may not be in active use for the Disseminator 
	 * 
	 */
	/*
	 * Refer to putDirectory() to get a detail description of the code.
	 * this function simply changes from putting a directory to listing
	 * the contents of a directory.
	 */
	public void listDirectory( String remote_dir) throws SessionIDNotSetException, IllegalSpecException
					, InvalidSecurityContextException, InvalidServiceContactException
					, TaskSubmissionException{
		Task task = new TaskImpl("mySecondTask", Task.FILE_OPERATION);
		task.setProvider(GLOBUSDiseminator.FILE_PROVIDER); //even though it is deprecated, it will create an error if not used
		FileOperationSpecification spec = new FileOperationSpecificationImpl();
		spec.setOperation(FileOperationSpecification.LS);
		 
		//spec.addArgument(this.LocalSourceDirectory);
		spec.addArgument(remote_dir);
		 
		task.setSpecification(spec);
		if(SessionID == null){
			throw new SessionIDNotSetException();
		}
		task.setAttribute("sessionID", SessionID);
		task.setService(Service.DEFAULT_SERVICE, this.ServerService);
		task.addStatusListener(new ListDirListener(task));     
		handler.submit(task);
	}
	/**
	 * This Listener catches the status report from list directory.
	 * The status report include the output for the procedure.
	 * The cog javadoc has more information on this, but the Attribute returned by
	 * the list directory task can be casted to an ArrayList. The array is an 
	 * Array of GridFileImpl object, which has information about the file.  
	 * Refer to the javadoc for GriFileImpl to know more about the information
	 * included with the object.
	 * @author jfvillal
	 *
	 */
	public class ListDirListener implements StatusListener{
		Task task;
		public ListDirListener( Task task ){
			this.task = task;
		}
		public void statusChanged(StatusEvent event) {
			Status status = event.getStatus();
			Log.log(Level.FINE,"List Dir Listener:  Status changed to "
			         + status.getStatusString());
			 
			     if (status.getStatusCode() == Status.COMPLETED) {
			    	ArrayList list = (ArrayList) task.getAttribute("output");
			    	Iterator it = list.iterator();
			    	while(it.hasNext()){
			    		GridFileImpl file = (GridFileImpl) it.next();
			    		Log.log(Level.FINE,file.getName());
			    	}
			    	
					
					
			     }
			
		}
		
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
	 * @throws IllegalSpecException
	 * @throws InvalidSecurityContextException
	 * @throws InvalidServiceContactException
	 * @throws TaskSubmissionException
	 * @throws SessionIDNotSetException 
	 */
	public void putDirector(  String destination_dir ) throws 
				IllegalSpecException, InvalidSecurityContextException
				, InvalidServiceContactException, TaskSubmissionException, SessionIDNotSetException{
		/*Create a File_operation task.*/
		Task task = new TaskImpl("mySecondTask", Task.FILE_OPERATION);
		task.setProvider(GLOBUSDiseminator.FILE_PROVIDER); //even though it is deprecated, it will create an error if not used
		FileOperationSpecification spec = new FileOperationSpecificationImpl();
		/*this next line sets the type of operation that is going to be performed.*/
		spec.setOperation(FileOperationSpecification.PUTDIR);

		/*the next two line indicate the argument. the argument number depends on the action set in the previous line.*/
		spec.addArgument(this.LocalSourceDirectory);
		spec.addArgument(destination_dir);
		 
		task.setSpecification(spec);
		/*If the SessionID was not set, then we cannot do anything*/
		if(SessionID == null){
			throw new SessionIDNotSetException();
		}
		/*fill information for the task*/
		task.setAttribute("sessionID", SessionID);
		task.setService(Service.DEFAULT_SERVICE, this.ServerService);
		task.addStatusListener(new StatusListener() {
			/**
			 * This listener simply logs the status of the acction for troubleshooting.
			 */
			public void statusChanged( StatusEvent event){
				Status status = event.getStatus();
				Log.log(Level.FINE,"PutDirectory() Status changed to " + status.getStatusString() );
				     if (status.getStatusCode() == Status.COMPLETED) {
				    	//return positive result
				     }else if (status.getStatusCode() == Status.FAILED){
				    	
				    	Exception e = status.getException();
				    	Log.log(Level.FINE,"Job failed" + e.getMessage());
				    	e.printStackTrace();
				     }else if(status.getStatusCode() == Status.ACTIVE){
				
				     }
				 }

		});
     
		handler.submit(task);
	}
	
	/**
	 * 
	 * Establishes a connection.  the listener should get a sessionID of type identity
	 * 
	 * a null SessionID means the connection was not established.
	 * 
	 * The status from this task is listen by ConnectionListener class
	 * 
	 * @param service
	 * @throws InvalidProviderException
	 * @throws ProviderMethodException
	 * @throws TaskSubmissionException 
	 * @throws InvalidServiceContactException 
	 * @throws InvalidSecurityContextException 
	 * @throws IllegalSpecException 
	 */
	public void setConection() throws 
				InvalidProviderException, ProviderMethodException, IllegalSpecException
				, InvalidSecurityContextException, InvalidServiceContactException
				, TaskSubmissionException{
		Task task = new TaskImpl("ConectionStablishTask", Task.FILE_OPERATION);
		 task.setProvider(GLOBUSDiseminator.FILE_PROVIDER);
		 FileOperationSpecification spec = new FileOperationSpecificationImpl();
		 spec.setOperation(FileOperationSpecification.START);
		 task.setSpecification(spec);
		 task.setService(Service.DEFAULT_SERVICE, this.ServerService);
		 task.addStatusListener(new ConnectionListener( task, this ));
		 handler.submit(task);    
		}
	/**
	 * ConnectionListener is used to catch the response from setConnection().  The response includes
	 * an Identity object which is used to identify the session id.
	 * @author jfvillal
	 *
	 */
	public class ConnectionListener implements StatusListener{
		Task task;
		GLOBUSDiseminator diseminator;
		public ConnectionListener( Task task, GLOBUSDiseminator diseminator ){
			this.task = task;
			this.diseminator = diseminator;
		}
		/**
		 * ConnectionListener is used to catch the response from setConnection().  The response includes
	     * an Identity object which is used to identify the session id.
		 */
		public void statusChanged(StatusEvent event) {
			Status status = event.getStatus();
			Log.log(Level.FINE,"ConnectionListener: Status changed to "
			         + status.getStatusString());
			 
			     if (status.getStatusCode() == Status.COMPLETED) {
			 
			    	 Log.log(Level.FINE,"SessionID = "
			           + task.getAttribute("output"));
			       diseminator.setSessionID( (Identity)task.getAttribute("output"));
			       
			       
			     }else if (status.getStatusCode() == Status.FAILED){
				    	Exception e = status.getException();
				    	e.printStackTrace();
				 }
			
		}
		
	}
	/**
	 * Listens for the ouput of getHomeDir file operation.
	 * @author jfvillal
	 *
	 */
	public class HomeDirListener implements StatusListener{
		Task task;
		GLOBUSDiseminator diseminator;
		public HomeDirListener( Task task, GLOBUSDiseminator diseminator ){
			this.task = task;
			this.diseminator = diseminator;
		}
		public void statusChanged(StatusEvent event) {
			Status status = event.getStatus();
			Log.log(Level.FINE,"HomeDirListener:  Status changed to "
			         + status.getStatusString());
			 
			     if (status.getStatusCode() == Status.COMPLETED) {
			    	 diseminator.setHomeDir( (String) task.getAttribute("output") );
			    	 try {
			    		 Log.log(Level.FINE,"Home dir: " + diseminator.getHomeDir());
					} catch (HomeDirNotSetException e) {
						e.printStackTrace();
					}
			     }
			
		}
		
	}
	/**
	 * Listens for status updated of the JobSubmit operation
	 * @author jfvillal
	 *
	 */
	public class JobSubmitListener implements StatusListener{

		public void statusChanged(StatusEvent event) {
			Status status = event.getStatus();
			Log.log(Level.FINE," JobSubmitListener: Status changed to "
			         + status.getStatusString());
			 
			     if (status.getStatusCode() == Status.COMPLETED) {
			    	
			       
			     }else if (status.getStatusCode() == Status.FAILED){
				    	Exception e = status.getException();
				    	e.printStackTrace();
				 }
			
		}
		
	}
	/**
	 * Listens for status changes of the DeleteDirectory method.
	 * @author jfvillal
	 *
	 */
	public class DeleteDirectoryListener implements StatusListener{
		GLOBUSDiseminator parent ;
		String Description;
		public DeleteDirectoryListener( GLOBUSDiseminator dis , String Desc ){
			parent = dis;
			Description = Desc;
		}
		public void statusChanged(StatusEvent event) {
			Status status = event.getStatus();
			Log.log(Level.FINE,"DeleteDirecoty: Status changed to "
			         + status.getStatusString() + " " + Description);
			 
			     if (status.getStatusCode() == Status.COMPLETED) {
			    	 //Log.log(Level.FINE,"Delete file done");
			    	parent.setDeletedFile(true);
			       
			     }else if (status.getStatusCode() == Status.FAILED){
			    	 parent.setDeletedFile(false);
				    	Exception e = status.getException();
				    	e.printStackTrace();
				 }
			
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
	public synchronized boolean isDeletedFile() {
		return DeletedFile;
	}
	public synchronized void setDeletedFile(boolean deletedFile) {
		DeletedFile = deletedFile;
	}
	public static boolean isFrameworkDebug() {
		return FrameworkDebug;
	}
	public static void setFrameworkDebug(boolean frameworkDebug) {
		FrameworkDebug = frameworkDebug;
	}
	
}
