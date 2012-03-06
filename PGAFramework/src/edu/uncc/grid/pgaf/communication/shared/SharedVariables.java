/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * All rights reserved
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
package edu.uncc.grid.pgaf.communication.shared;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.communication.nat.SlimJxtaID;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
import edu.uncc.grid.seeds.comm.dataflow.skeleton.pipeline.PipeFinish;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;

/**
 * SharedVariables has the variables that are shared between two threads that are 
 * Establishing a connection through PGAF
 * @author jfvillal
 *
 */
public class SharedVariables {
	/**
	 * The instant queue.  It has two queues, one send and receive.
	 */
	private ArrayBlockingQueue<Serializable> Queue[]; // I used to be using ConcurrentLinkedQueue, but had no size limit
	/**
	 * This is the queue for the non-blocking interfaces
	 */
	private ArrayBlockingQueue<NonBlockReceiver> NBRecv[];
	/**
	 * The pipe we are connecting to
	 */
	public  SlimJxtaID HCommPipeID;
	/**
	 * The Peer ID for the remote node, the one that is connecting to this node.
	 * or the node we are connecting to.
	 */
	public SlimJxtaID HRemotePeerID;
	/**
	 * boolean to denote if the communication line has been created.
	 */
	public boolean Bound;
	public PipeID UniqueDependencyID;
	public HierarchicalDependencyID DependencyID;

	/**
	 * Constructor to create the shared variables.
	 */
	public SharedVariables(){
		Queue = new ArrayBlockingQueue[2];
		NBRecv = new ArrayBlockingQueue[2];
		for(int i = 0; i < 2; i++){
			Queue[i] = new ArrayBlockingQueue<Serializable>(CommunicationConstants.QUEUE_SIZE);
			NBRecv[i] = new ArrayBlockingQueue<NonBlockReceiver>(CommunicationConstants.QUEUE_SIZE);
		}
	}
	/**
	 * Returns a Serializable from the queue.  If the quue is empty it will wait
	 * until a Serializable is available.  The addQueue() function will make this 
	 * blocking method waik up.  Use peekQueue to non-blockingly check the queue.
	 * @param index
	 * @return
	 * @throws InterruptedException
	 */
	public Serializable takeQueue(int index) throws InterruptedException{
		Serializable s = null;
		synchronized( Queue[index]){
			s = Queue[index].poll();
			/*if( s instanceof PipeFinish){
				System.out.println();
			}*/
			while( s == null){
				Queue[index].wait(100); //if queue is empty wait
				s = Queue[index].poll();
				/*if( s instanceof PipeFinish){
					System.out.println();
				}*/
			}
			Queue[index].notify(); //if queue was full, notify producer there is one spot open
		}
		return s;
	}
	/**
	 * Adds a Serializable to the queue.
	 * @param index either zero or one, it denotes whether it is the server node or the receiving node.
	 * @param s
	 * @throws InterruptedException 
	 */
	public void addQueue( int index, Serializable s) throws InterruptedException{
		synchronized( Queue[index]){
			boolean sucess = false;
			while( ! sucess){
				sucess = Queue[index].offer(s);
				if( !sucess){//if full wait
					Queue[index].wait(500);
				}
			}
			Queue[index].notify(); //if queue was empty, notif consumer.
		}
	}
	/**
	 * Wrapper for Queue.poll()
	 * @param index
	 * @return
	 */
	public Serializable pollQueue(int index){
		return Queue[index].poll();
	}
	/**
	 * wrapper for Queue.peek()
	 * @param index
	 * @return
	 */
	public Serializable peekQueue(int index){
		return Queue[index].peek();
	}
	/**
	 * wrapper for Queue.peek()
	 * @param index
	 * @return
	 */
	public NonBlockReceiver peekNBRecv(int index){
		return NBRecv[index].peek();
	}
	/**
	 * wrapper for Queue.add();
	 * @param index
	 * @param non_blocker
	 */
	public void addNBRecv( int index, NonBlockReceiver non_blocker){
		NBRecv[index].add( non_blocker);
	}
	/**
	 * wrapper for Queue.poll()
	 * @param index
	 * @return
	 */
	public NonBlockReceiver pollNBRecv(int index){
		return NBRecv[index].poll();
	}
	/**
	 * wrapper for Queue.isEmpty()
	 * @param index
	 * @return
	 */
	public boolean isEmptyQueue( int index){
		synchronized( Queue[index]){
			return Queue[index].isEmpty();
		}
		//return Queue[index].isEmpty();
	}
	/**
	 * wrapper for Queue.size()
	 * @param index
	 * @return
	 */
	public int sizeQueue( int index){
		return Queue[index].size();
	}
	
}
