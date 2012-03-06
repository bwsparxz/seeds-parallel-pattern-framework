/* * Copyright (c) Jeremy Villalobos 2009
 *
 *   * All rights reserved
 */
package edu.uncc.grid.pgaf.templates;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeClient;
import edu.uncc.grid.pgaf.communication.MultiModePipeDispatcher;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.advanced.UnorderedTemplate;
import edu.uncc.grid.pgaf.p2p.Node;
/**
 * This class will use two node to run bandwidth and latency tests.
 * 
 * This  class was used for the MPI vs MPJ vs Seeds benchmark peformed 2009
 * 
 * The template is used to benchmark the framework.
 * 
 * @author jfvillal
 *
 */
public class BandwidthTestTemplate extends UnorderedTemplate {
	int LIMIT = 100;
	int double_size = 100;
	public static final int LAT_TEST = 30;
	
	public BandwidthTestTemplate(Node n) {
		super(n);		
	}	
	
	@Override
	public boolean ClientSide(PipeID pattern_id) {
		
		ConnectionManager client;
		
		try {
			
			PipeID local_pipe = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
			Long local_segment = 2L;
			
			while( (client = MultiModePipeClient.getClientConnection(Network, 0L, pattern_id, local_pipe.toString(), local_segment, null, null,null) ) == null){
				Node.getLog().log(Level.FINE, "Waiting for the pipe to be available");
				Thread.sleep(300);
				
			}
		
			while( !client.isBound()){
				Node.getLog().log(Level.FINE, " Socket Not Bounded... Waiting pipe_id " + client.getHashPipeId().toString() 
						+ " peer id " + Node.PID.toURI().toString());
				Thread.sleep(1000);
			}
			
			InetAddress addr = InetAddress.getLocalHost();
			DataMap map = new DataMap();
			map.put("hostname", addr.getHostName());
			client.SendObject(map);
			
	    	for( int i = 0; i < LAT_TEST; i++){
		    	  
		    	  DataObject dat = (DataObject)client.takeRecvingObject();
		    	  //MPI.COMM_WORLD.Recv(obj, 0 ,1, MPI.DOUBLE, 0, TAG);
		    	  //double[] obj2 = obj;
		    	  //MPI.COMM_WORLD.Send(obj2, 0, 1, MPI.DOUBLE, 0, TAG + 1);
		    	  
		    	  //send new object to prevent java from taking shortcuts.
		    	  DataObject nobj = new DataObject( dat.d.length);
		    	  Node.getLog().log(Level.FINE, " got obj " + dat.d[0] );
		    	  nobj.d[0] = dat.d[0] + 1.0;
		    	  client.SendObject(nobj);
	    	}
		    
		    
		      try{
		    	  int exp = 2;
			      for( int i = 1; i < 22; i++){
			    	  	int data_size = exp; // exponential 
			    	  	DataObject d;
			    	  
			    		for( int k = 0; k < 10; k++){
				    		
				    		//MPI.COMM_WORLD.Recv(d, 0 ,d.length, MPI.DOUBLE, 0, TAG + i);
				    		d = (DataObject) client.takeRecvingObject();
				    		
				    		DataObject nobj = new DataObject( d.d.length);
				    		client.SendObject(nobj);
				    		//MPI.COMM_WORLD.Send(d, 0 ,1, MPI.DOUBLE, 0, TAG + i + 1);
			    		}
			    	  
			    		exp *= 2;
			      }
		      }catch( ArrayIndexOutOfBoundsException e){
					e.printStackTrace();
		      }
		      
		      System.out.println("Hi from <ClientSide>");
		      
		      client.close();
		
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TunnelNotAvailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		return true;
	}
		
		
	
	
	@Override
	public void ServerSide(PipeID pattern_id) {	 
	
		try {
			//Node.Log.setLevel(Level.WARNING);
			
			
			List<ConnectionManager> lst = Collections.synchronizedList(new ArrayList<ConnectionManager>());
			//System.out.println("1.  sending object... lst size: " + lst.size());
			MultiModePipeDispatcher dispatcher = new MultiModePipeDispatcher(this.Network, "d", 0, lst, pattern_id, null );
			//System.out.println("2.  sending object... lst size: " + lst.size());
			while( lst.size() < 1 && !ShutdownOrder){
				Thread.sleep(1000);
				//System.out.print("S");
			}
			
			//System.out.println("\ndata: network up... time: " + (double)(System.currentTimeMillis() - start)/1000.0 );
			
			//System.out.println("Working... list size " + lst.size());
			
			//Debug dbg = (Debug)this.UserModule;
			
			
			
			
		    
		    double avg_latency = 0.0;
		      
		    DataMap<String,String> map = (DataMap<String,String>) lst.get(0).takeRecvingObject();
		    System.out.println("Testing with" + map.get("hostname"));
		    
		    //ping
		    double mem = 0.0;
		    for( int i = 0; i < LAT_TEST; i++){
		    	  double latency = 0;  
		    	  
		    	  long start = System.nanoTime();
		    	  
		    	  DataObject m = new DataObject(1);
		    	  m.d[0] = 1.0 + mem;
		    	  lst.get(0).SendObject(m);
		    	  
		    	  DataObject d;
					d = (DataObject) lst.get(0).takeRecvingObject();
		    	  mem = d.d[0];
		    	  //System.out.println("d " + d.d[0]);
		    	  long time = System.nanoTime() - start ;
		    	  //System.out.println("Latency: " + ((double)time/ 1000000.0 )+ "ms");
		    	  latency= (double)time / 1000000.0; // in milliseconds
		    	  avg_latency += latency;
		    }	  
		      
		    avg_latency /= LAT_TEST;
		  	System.out.println("Avg Latency: " + avg_latency+ "ms");
		    //bandwidth
		    try{
		    	  int exp = 2;
			      for( int i = 1; i < 22; i++){
			    	  int data_size = exp; // exponential 
			    	  
			    	  DataObject d = new DataObject(data_size);
			    	  
			    		double avg_bandwidth = 0.0;
			    		double data_packet_size = (data_size * 8 * 8) / 1000.0 ; // in kilo bits
			    		for( int k = 0; k < 10; k++){
				    		
				    		long start = System.nanoTime();//System.currentTimeMillis();
				    		
				    		 
					    	lst.get(0).SendObject(d);
					    	  
					    	DataObject e;
								e = (DataObject) lst.get(0).takeRecvingObject();
				    		
				    		
				    		//MPI.COMM_WORLD.Send(d, 0, d.length, MPI.DOUBLE, 1, TAG + i); 
				    		//MPI.COMM_WORLD.Recv(d, 0 ,1, MPI.DOUBLE, 1, TAG + i + 1);
								
				    		long time = System.nanoTime() - start;//System.currentTimeMillis() - start;
				    		/*bandwisth in KB per second. the original data is in double (8 bytes) and in milliseconds*/
				    		
				    		double total_time = (double)time * 1e-9F ; // in seconds
				    		double bandwidth = data_packet_size/( ( total_time) );
				    		//System.out.println("data: " + data_packet_size + "Kb  T: " + total_time + "ms  B: " + bandwidth + "Kb/s" );
				    		avg_bandwidth += bandwidth;
			    		}
			    		avg_bandwidth /= 10.0;
			    		System.out.println("Packet:" + data_packet_size + ":Kb:B:" + avg_bandwidth + ":kb/s" );
			    	
			    	  exp *= 2;
			      }
		      }catch( ArrayIndexOutOfBoundsException e){
					e.printStackTrace();
			}
		      
		    System.out.println("Hi from <ServerSide>");
		  
		    
			
			lst.get(0).close();
			
			
			
			dispatcher.close();
			
			//Network.bringNetworkDown();
			
			
			
		} catch (Exception e) {
			Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
		}
	}
	
	@Override
	public String getSuportedInterface() {
		// TODO Auto-generated method stub
		return null;
	}
	public void TestMultiModePipeClient(PipeID pattern_id){
		try {
			//Node.Log.setLevel(Level.WARNING);
			//RemoteLogger.printToObserver( "hostname: " + InetAddress.getLocalHost().getHostName() + "\n" );
			ConnectionManager client;
			
			PipeID local_pipe = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
			Long local_segment = 2L;
			
			
			while( (client = MultiModePipeClient.getClientConnection(Network, 0L, pattern_id, local_pipe.toString(), local_segment ,null,null, null)) == null){
				Node.getLog().log(Level.FINE, "Waiting for the pipe to be available");
				Thread.sleep(300);
				
			}
			while( !client.isBound()){
				Node.getLog().log(Level.FINE, " Socket Not Bounded... Waiting pipe_id " + client.getHashPipeId().toString() 
						+ " peer id " + Node.PID.toURI().toString());
				Thread.sleep(1000);
			}
			
			
			//Debug debug_mod = (Debug) this.UserModule;
			
			
			//Node.getLog().log(Level.FINE, Thread.currentThread().getId() + " limit: " + LIMIT + " double size: " + double_size );
			for( int warm = 0; warm < 1001; warm++){
				/**
				 * Send
				 */
				for( int i = 0; i < LIMIT; i++){
					DataObject d = new DataObject( double_size );
					d.setSegment(33);
						
					client.SendObject(d);
					//System.out.print("X");
				}
				/**
				 * Recv
				 */
				for( int i = 0; i < LIMIT; i++){
					try{
						
						DataObject m;
						while( (m= (DataObject) client.pollRecvingObject() ) == null){
							Thread.sleep(30);
							//System.out.println(" Waiting for object ");
						}
					}catch( Exception e){
						Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));		
					}
					//System.out.print("#");
				}
			}
			Node.getLog().log(Level.FINE, "DONE...");
			
			client.close();
			
		} catch (Exception e) {
			Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
			
		} 
	}

	@Override
	public void FinalizeObject() {
		// TODO Auto-generated method stub
		
	}
}






/*OLD BENCHMARK CODE*/


/*
Node.getLog().log(Level.FINE, Thread.currentThread().getId() + " limit: " + LIMIT  + " double_width: " + this.double_size);
for( int warm = 0; warm < 1001; warm++){
	long startup_time = System.currentTimeMillis() ;
	// Receive 
	 
	long start_send = System.currentTimeMillis();
	//System.out.println(" Starting the sends: " );
	for( int i = 0; i < LIMIT ; i++){
		if( !Template.ShutdownOrder ){
			DataObject d;
			d = (DataObject) lst.get(0).takeRecvingObject();
		}
	}
	// Send
	 				
	//System.out.println("Time taken to send: " + stop_send );
	
	for( int i = 0; i < LIMIT ; i++){
		if( !Template.ShutdownOrder ){
			DataObject m = new DataObject(double_size);
			
			m.setSegment(4);
			///System.out.println(".");
			lst.get(0).SendObject(m);
			
		}
	}
	//wait until all data has being sent
	while( lst.get(0).hasSendData() ){

	}

	//double stop_send = (double)(System.currentTimeMillis() - start_send)/1000.0;
	
	//System.out.println("Time taken for recv: " + stop_recv  );
	
	//Node.getLog().log(Level.FINE, "DONE...");
	
	double tot_time = (double)(System.currentTimeMillis() - startup_time)/1000.0; 
	System.out.println(this.LIMIT+"," +this.double_size+","+  + tot_time) ;
}
*/

//RemoteLogger.printToObserver( dbg.limit_iter + "," + dbg.double_size + "," + stop_send + "," + startup_time + "," + tot_time );
/*
while( lst.size() < 2){
	Thread.sleep(1000);
	System.out.println("Sleeping2");
}

Data e = (Data) lst.get(1).takeRecvingObject();



{
	Data m = new Data();
	m.setSegment(4);
	System.out.println("sending object...");
	lst.get(1).SendObject(m);
} */
/*

while( lst.size() < 3){
	Thread.sleep(1000);
	System.out.println("Sleeping2");
}

Data r = (Data) lst.get(2).takeRecvingObject();
lst.get(2).SendObject(r);
*/
//stop network
