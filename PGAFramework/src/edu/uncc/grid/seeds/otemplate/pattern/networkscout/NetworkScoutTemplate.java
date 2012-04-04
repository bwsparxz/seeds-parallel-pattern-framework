package edu.uncc.grid.seeds.otemplate.pattern.networkscout;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.communication.CommunicationLinkTimeoutException;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.communication.nat.TunnelNotAvailableException;
import edu.uncc.grid.pgaf.datamodules.AllToAllData;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.datamodules.DataContainer;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.basic.CompleteSyncGraph;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

/**
 * this is an all-to-all implementation using the Ordered Template 
 * programming approach, also know as MPI-like because you can use rank id to
 * organize the parallel application.
 * @author jfvillal
 *
 */
public class NetworkScoutTemplate extends OrderedTemplate {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//private int LoopCount;
	ScoutNetworkData LocalData;
	private BufferedWriter PerformanceReportWriter;
	/**
	 * Stors the RttStopTime for the rtt message.
	 */
	public long RttStopTime[];
	/**
	 * The inter is set to the number of rtt messages we are waiting on.
	 * Once the number is zero, we can continue.  The integer is handled 
	 * by multiple thread so proper synchronization practice should be 
	 * followed.
	 */
	public boolean ReceiveWaits[];
	
	public NetworkScoutTemplate(Node n) {
		super(n);
	//LoopCount = 0;
		// no initiation of user's module stuff can go in this 
		//function, use the method setUserModule to do that.
		PerformanceReportWriter = null;
	}

	int TesterId = 2;
	
	public void kindofBarrier(String host, ScoutModule module, Communicator comm){
		for( long i = 0; i < module.getCells(); i++ ){
			//skip myself
			if( i != this.CommunicationID){
				try {
					comm.Send( host, i);
				} catch (Exception  e) {
					Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
				}
			}
		}
		for( long i = 0; i < module.getCells(); i++){
			if( i != this.CommunicationID){
				try {
					LocalData.NeighborHosts[(int)i] = (String)comm.BlockReceive(i);
				} catch (Exception  e) {
					Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
				}
			}
		}
		comm.hasSent();
	}
	
	@Override
	public boolean ComputeSide(Communicator comm) {
		ScoutModule module = (ScoutModule) this.getUserModule();
		TesterId = module.getCells();
		
		try {
			InetAddress s = InetAddress.getLocalHost();
			LocalData.Hostname = s.getHostName();
			LocalData.NeighborHosts = new String[module.getCells()];
		
			for( int tester_id = 0; tester_id < TesterId; tester_id++){
				//send a round trip message to establish the connections
				//for each connection send one message
				kindofBarrier(LocalData.Hostname, module, comm);
				kindofBarrier(LocalData.Hostname, module, comm);
				//now do the RTT message.
				//start clock
				//start a receive thread for each of the connections.
				//then send rtt message for each of the connections.
				//wait.  and record the clock when the message arrives
				long start[] = new long[module.getCells()];
				RttStopTime = new long[module.getCells()];
				MessageReceiver[] Receivers = new MessageReceiver[module.getCells()];
				
				if( this.CommunicationID ==tester_id){
					LocalData.Latency = new double[module.getCells()];
					LocalData.Bandwidth = new double[module.getCells()];
					
					long start_t =  System.currentTimeMillis();
					for( int i = 0; i < module.getCells();i++){
						start[i] = start_t;
						Receivers[i] = new MessageReceiver( i, this );
						LocalData.Latency[i] = -1;
						LocalData.Bandwidth[i] = -1;
					}
					ReceiveWaits = new boolean[module.getCells()];
					for( int i = 0; i < module.getCells(); i++){
						if( i != this.CommunicationID){
							try {
								comm.Receive( Receivers[i], (long)i);
								ReceiveWaits[i] = true;
							} catch (Exception  e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							}
						}
					}	
					for( long i = 0; i < module.getCells(); i++ ){
						if( i != this.CommunicationID){
							Byte b = 0x2;	
							try {
								comm.Send( b, i);
							} catch (Exception  e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							}
						}
					}
					boolean wait = true;
					while( wait ){	
						wait = false;
						for( int i = 0 ; i < module.getCells(); i++){
							wait = wait || ReceiveWaits[i];
						}
						
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}else{
					//Participants in test_id's test
					
					try {
						Serializable obj = comm.BlockReceive((long)tester_id);
						Byte b = (Byte) obj;
						comm.Send(b, (long)tester_id);
					}catch( Exception e) {
						e.printStackTrace();
					} 
				}
				
				
				
				//process RTT results.
				if( this.CommunicationID ==tester_id){
					for( int i = 0; i < module.getCells(); i++){
						long time = RttStopTime[i] - start[i];
						LocalData.Latency[i] = (double)time;//in milliseconds
					}
				}
				//start bandwith test.
				//do same test as the one performed for RTT, but the
				Random rand = new Random();
				rand.setSeed(System.currentTimeMillis());
				//go 434 and 463 Mbit/sec between Kronos and Ixion which verifies expected Java socket performance over gigabit
				//TODO lower test to 5MB (checks out) to make it faster ans see if the results hold up ( they should since the time is no
				//counted in nanoseconds which give better precision so I can use a smaller packet. 
				//The multy core reading is giving me trouble presumably because the pointer is just getting moved, not the 
				//data, so the transfer time is measured to be high.
				int MEGABYTE= 1048576 * 10; //ten megabytes
				byte[] b= new byte[MEGABYTE];//one megabyte
				rand.nextBytes(b);
				
				long hdd_t = System.currentTimeMillis();
				try {
					FileWriter t = new FileWriter("mega.txt");
					
					for( int i =0; i < b.length; i++){
						t.write(b[i]);
					}
					t.close();
					
					FileReader h = new FileReader("mega.txt");
					while( h.read() != -1){
						
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				long hdd_s = System.currentTimeMillis();
				LocalData.HDDTime = hdd_s - hdd_t;
				
				BandwidthMessageReceiver[] b_receivers = new BandwidthMessageReceiver[module.getCells()];
				
				
				for( int i = 0; i < module.getCells();i++){
					b_receivers[i] = new BandwidthMessageReceiver( i, this );
					
				}
				
				if( this.CommunicationID ==tester_id){
					for( int i = 0; i < module.getCells(); i++){
						if( i != this.CommunicationID){
							try {
								comm.Receive( b_receivers[i], (long)i);
								ReceiveWaits[i] = true;
							} catch (Exception  e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							}
						}
					}
					for( long i = 0; i < module.getCells(); i++ ){
						if( i != this.CommunicationID){
							try {
								start[(int)i] = System.currentTimeMillis();
								comm.Send( b, i);
							} catch (Exception  e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
							}
						}
					}
					boolean wait = true;
					while( wait ){	
						wait = false;
						for( int i = 0 ; i < module.getCells(); i++){
							wait = wait || ReceiveWaits[i];
						}
						
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					//result should be computed to calculate bandwidth
					
					for( int i = 0; i < module.getCells(); i++){
						if( i != this.CommunicationID){
							long time = RttStopTime[i] - start[i];
							//the RTT makes it a 2 meg total transfer (both ways)
							// ( 2 * MEGABYTE ) / time
							//divide byt MEGABYTE to get Mbytes per millisecond
							//divide time by 1000 to get Mbytes per second
							//multiply times 8 to get Mbs/sec
							//divide that by the time to get megabits/second.
							System.out.println("Bandwidth time: " + time );
							//the 10 is produced by 10 * 2
							//two because the data goes round trip
							LocalData.Bandwidth[i] = (10.0*2.0*8.0*1.0e3)/((double)time); 
								//(20.0*8.0)/((double)time/(10e9) );//in nanoseconds
						}else{
							LocalData.Bandwidth[i] = 0.0;//in milliseconds
						}
						
					}
					
					Linpack l = new Linpack();
					double mflops = 0.0;
					//run a few times to evaluate post-JIT performance
					for( int i = 0; i < 20;i++){
						mflops = l.run_benchmark();
					}
				    LocalData.Flops = mflops;
					
				}else{
					try {
						Serializable obj = comm.BlockReceive( (long)tester_id);
						byte[] val = (byte[]) obj;
						comm.Send(val, (long)tester_id);
						
					} catch (Exception  e) {
						Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
					}
			
					
				}
				
				
				//lastly, perform an floating point operations test
				this.kindofBarrier(LocalData.Hostname, module, comm);
				
				//perform a operations per second.
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	public void Configure(DataMap<String, Serializable> configuration) {
		
		DataContainer container = (DataContainer)configuration.get(PatternLoader.INIT_DATA);
		LocalData = (ScoutNetworkData) container.getPayload();
		this.CommunicationID = container.getSegment();
		
		//performance measurement code
		try {
			FileWriter performance_report_file;
			performance_report_file = new FileWriter ( Node.getLogFolder() + "/CompleteGraph.performance." + this.CommunicationID + ".txt" );
			PerformanceReportWriter = new BufferedWriter(performance_report_file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean SourceSinkSide(Communicator comm) {
		return true;
	}

	@Override
	public Class getLoaderModule(){
		return NetworkScoutLoader.class;
	}

	@Override
	public String getSuportedInterface(){
		return CompleteSyncGraph.class.getName();
	}

	@Override
	public void FinalizeObject() {
		try {
			this.PerformanceReportWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
