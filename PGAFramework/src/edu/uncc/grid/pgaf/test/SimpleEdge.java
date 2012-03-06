/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * This file is part of PGAFramework.
 *
 *   PGAFramework is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PGAFramework is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PGAFramework.  If not, see <http://www.gnu.org/licenses/>.
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
package edu.uncc.grid.pgaf.test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;

import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVCompetitionAdvertisement;
import edu.uncc.grid.pgaf.advertisement.NetworkInstructionAdvertisement;

import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class SimpleEdge {
	public String Name;
	public static int Port = 50000;  //randevous default port
	public static final PeerID PID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID);
	public static File ConfigurationFile; 
	static NetworkManager NetManager;
	static PeerGroup NetPeerGroup;
	//public String BugContainer;
	//public boolean BugPipeOpen;
	//public boolean KeepGoing;
	public Integer CompetitionRandomNumber;
	
   public static void RecursiveDelete(File TheFile) {
        
        File[] SubFiles = TheFile.listFiles();
        
        if (SubFiles!=null) {
        
            for(int i=0;i<SubFiles.length;i++) {
            	//System.out.println("deleting..." + TheFile.toString() );
                if (SubFiles[i].isDirectory()) {
                
                    RecursiveDelete(SubFiles[i]);
                    
                }
            
                SubFiles[i].delete();
            
            }
            
        TheFile.delete();

        }
        
    }
	
	public SimpleEdge(String seed, int port_arg, String name /*, String addrr*/) throws Exception{
		//BugPipeOpen = false;
		//KeepGoing = true;
		Name = name;
		Port = port_arg;
		ConfigurationFile = new File("." + System.getProperty("file.separator") + Name + "_" + Port);
		
		RecursiveDelete( ConfigurationFile );
		
		
		System.out.println("port: " + Port );
		long start = System.currentTimeMillis();
		NetManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, Name, ConfigurationFile.toURI());
		NetManager.setUseDefaultSeeds(false);
		NetworkConfigurator MyNetworkConfigurator = NetManager.getConfigurator();
		
		MyNetworkConfigurator.clearRendezvousSeeds();
		if(seed.compareTo("none") != 0){
			URI SeedURI = URI.create(seed);
			MyNetworkConfigurator.addSeedRendezvous(SeedURI);
		}
		MyNetworkConfigurator.setTcpPort(Port);
        MyNetworkConfigurator.setTcpEnabled(true);
        MyNetworkConfigurator.setTcpIncoming(true);
        MyNetworkConfigurator.setTcpOutgoing(true);
        MyNetworkConfigurator.setUseMulticast(true);
        
		MyNetworkConfigurator.setPeerID(PID);
		MyNetworkConfigurator.save();		
        System.out.println("My Id is: " + PID.toString() );
        
        AdvertisementFactory.registerAdvertisementInstance(
				DirectorRDVAdvertisement.getAdvertisementType(), 
				new DirectorRDVAdvertisement.Instantiator()
				);
        AdvertisementFactory.registerAdvertisementInstance(
				DirectorRDVCompetitionAdvertisement.getAdvertisementType(), 
				new DirectorRDVCompetitionAdvertisement.Instantiator()
				);
		/*AdvertisementFactory.registerAdvertisementInstance(
				ObserverSinkAdvertisement.getAdvertisementType()
				, new ObserverSinkAdvertisement.Instantiator());*/
		
		AdvertisementFactory.registerAdvertisementInstance(
				DataLinkAdvertisement.getAdvertisementType()
				, new DataLinkAdvertisement.Instantiator()
				);
		
		/*AdvertisementFactory.registerAdvertisementInstance(
				DebugLogPipeAdvertisement.getAdvertisementType()
				, new DebugLogPipeAdvertisement.Instantiator()
				);*/
		
		AdvertisementFactory.registerAdvertisementInstance(
				NetworkInstructionAdvertisement.getAdvertisementType()
				, new NetworkInstructionAdvertisement.Instantiator());
		
		NetPeerGroup = NetManager.startNetwork();

		boolean HaveRDV = false;
		if(seed.compareTo("none") != 0){
			if( NetManager.waitForRendezvousConnection(60000)){
					System.out.println("Got it!  --> " );
					HaveRDV = true;
			}else{
				System.out.println(" No peer detected " );
			}
		}
		double stop = (double)(System.currentTimeMillis() - start) / 1000.0;
		System.out.println("Took " + stop + " seconds to start");
		
		//PipeService MyPipeService = NetPeerGroup.getPipeService();
		//LeafWorker.OutputListener pipe_handler = new LeafWorker.OutputListener();
        //MyPipeService.createOutputPipe(DirectorRDV.GetPipeAdvertisement(),
         //      pipe_handler );
        //LeafWorker.OutputListener pipe_handler_2 = new LeafWorker.OutputListener();
        //MyPipeService.createOutputPipe(Orchestrator.GetPipeAdvertisement(),  pipe_handler_2);
        /*int i = 0;
		
        Signal.handle(new Signal("INT"), new SignalHandler () {
		      public void handle(Signal sig) {
		    	  
		    	  System.out.println("Exiting the program " + sig.getName());
		    	  KeepGoing = false;
		      }
		});*/
        
        /*while( KeepGoing ){
			i ++;
			Thread.sleep(2000);
			if(BugPipeOpen){
				BugContainer = " hello " + i + " port: " + Port;
				//pipe_handler.addBugReport(BugContainer);
				pipe_handler_2.addBugReport(BugContainer);
			}else{
				
			}
			if(!HaveRDV){
				System.out.println("looking for RDV... ");
				NetManager.waitForRendezvousConnection(60000);
			}else{
				Vector<net.jxta.id.ID> TheList = NetPeerGroup.getRendezVousService().getConnectedPeerIDs();
				for( int j = 0; j < TheList.size(); j++){
					System.out.println("Got it!  --> " + TheList.get(j).toString());
				}
				HaveRDV = true;
			}
			
			System.out.println("looping HaveRDV= " + HaveRDV );
		}*/
       // pipe_handler.closePipe();
       //  pipe_handler_2.closePipe();
       //NetManager.stopNetwork();
		
	}
	public void CloseConection(){
		NetManager.stopNetwork();
	}
	public NetworkManager getNetManager(){
		return NetManager;
	}
	public PeerGroup getNetPeerGroup(){
		return NetPeerGroup;
	}
	public void StopOnINITSignal(){
		Signal.handle(new Signal("INT"), new SignalHandler () {
		      public void handle(Signal sig) {
		    	  System.out.println("Exiting the program " + sig.getName());
		    	  CloseConection();
		    	  System.exit(0);
		      }
		});
	}
	/*
	private class OutputListener implements OutputPipeListener{
		OutputPipe MyOutputPipe = null;
		public void outputPipeEvent(OutputPipeEvent arg0){
			try {

		    
				System.out.println("Pipe conected...");
   
				MyOutputPipe = arg0.getOutputPipe();
				
				BugPipeOpen = true;

				Message MyMessage = new Message();
				StringMessageElement MyStringMessageElement = new StringMessageElement("HelloElement", "hello one" , null);
				MyMessage.addMessageElement("DummyNameSpace", MyStringMessageElement);

				// Sending the message
				MyOutputPipe.send(MyMessage);
			} catch (IOException Ex) {
		            
		         
		    }
			
		}
		public void closePipe(){
			if( MyOutputPipe != null){
				MyOutputPipe.close();
			}
		}
		public void addBugReport(String bug_report){
			try {
				if(!MyOutputPipe.isClosed()){
					Message MyMessage = new Message();
					StringMessageElement MyStringMessageElement = new StringMessageElement("HelloElement", bug_report , null);
					MyMessage.addMessageElement("DummyNameSpace", MyStringMessageElement);
					
					MyOutputPipe.send(MyMessage);
				}else{
					System.out.println(" I could not send the message: BugPipeOpen = " + BugPipeOpen);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}*/
}
