/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *  * All rights reserved
 *  
 */
package edu.uncc.grid.pgaf.p2p;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.communication.wan.SocketManager;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataComm;
import edu.uncc.grid.pgaf.exceptions.PipeNotStablishedException;
import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;
/**
 * The class manages communicaiton among process for the generic template and similar template.  It has not
 * being update with the latest MultiMode Pipe. (Apr 2009).
 * TODO
 * the variable awaits PGA modification should be an array.
 * 
 * @author jfvillal
 *
 */
public class Communicator {
	DataComm UserData;
	HashMap<Long,SocketManager> Pipes;
	List<Nonblocker> NonblockerList;
	public Communicator(HashMap<Long, SocketManager> man , DataComm data){
		UserData = data;
		Pipes = man;
		NonblockerList = new ArrayList<Nonblocker>();
	}
	/**
	 * Communicate with no arguments will communicate with all
	 * the neighbors in the list
	 * @throws IOException 
	 */
	public void Communicate() {
		List<Long> list = UserData.getNeighborsList();
		Iterator<Long> it = list.iterator();
		while( it.hasNext()){
			Long id = it.next();
			Communicate( id);
		}
	}
	public void Communicate(long neighbor) {
		
		Data send = UserData.getCommSubData(neighbor);
		send.setSegment(UserData.getSegment());
		SocketManager man = Pipes.get(neighbor);
		try {
			Node.getLog().log(Level.FINE, "My segment id: " + UserData.getSegment() + " Sending Object to neighbor: " + neighbor ) ;
			man.SendObject(send);
		}catch (InterruptedException e) {
			Node.getLog().log(Level.SEVERE, "InterruptedException " + Node.getStringFromErrorStack(e)) ;
			e.printStackTrace();
		}

		
		UserData.setAwaitPGAFModification(true);
		/** Non-blcoking receive */
		
		try {
			Nonblocker b = new Nonblocker(neighbor);
			man.nonblockRecvingObject( b );
			NonblockerList.add(b);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	public void Communicate(long neighbors[]) {
		for(int i =0 ; i < neighbors.length; i++){
			Communicate( neighbors[i]);
		}
	}
	public void waitOnRecv() throws InterruptedException{
		/**
		 * We get the list of communications made by the users object.
		 * If the communication is not done, we wait until the 
		 * SocketManager tells us that the transaction is done
		 * if the communication is done, we remove the item from the 
		 * array.
		 */
		Iterator<Nonblocker> it = NonblockerList.iterator();
		while( it.hasNext() ){
			Nonblocker b = it.next();
			synchronized( b ){
				if( ! b.done ){
					b.wait();
					it.remove();
				}else{
					it.remove();
				}
			}
		}
		List<Long> id_list = UserData.getNeighborsList();
		Iterator<Long> ot = id_list.iterator();
		while( ot.hasNext() ){
			SocketManager man = Pipes.get(ot.next());
			while( !man.hasSendData() ){
				Thread.sleep(40);
			}
		}
	
	}
	public class Nonblocker implements NonBlockReceiver{
		long segment;
		boolean done;
		//DataComm UserData;
		public Nonblocker(long s){
			segment = s;
			done = false;
			//UserData = dat;
		}
		
		@Override
		public void Update(Serializable dat) throws InterruptedException {
			// TODO Auto-generated method stub
			UserData.setCommSubData(segment,(Data) dat);
			UserData.setAwaitPGAFModification(false);
			done = true;
		}
		
	}
}
