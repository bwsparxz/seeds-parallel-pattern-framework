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

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Enumeration;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.protocol.RdvAdv;
import net.jxta.impl.protocol.RouteAdv;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.socket.JxtaMulticastSocket;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.advertisement.NetworkInstructionAdvertisement;
import edu.uncc.grid.pgaf.p2p.AdvertFactory;
import edu.uncc.grid.pgaf.p2p.DirectorRDV;
import edu.uncc.grid.pgaf.p2p.LeafWorker;
import edu.uncc.grid.pgaf.test.SimpleEdge;

public class RecvLeaf extends SimpleEdge {
	public static final String n = "ksjfl";
	public RecvLeaf(String seed, int port_arg) throws Exception {
		super(seed, port_arg, "RecvLeaf"+port_arg);
	}
	public static void main(String args[]){
		try {
			
			RecvLeaf leaf = new RecvLeaf("none" , 50011);
			
			//PipeService pipe = leaf.getNetPeerGroup().getPipeService();
			/*pipe.createInputPipe(PGAFAdvertisementFactory.getDebugPipeAdvertRDV2Leaf()
					, new PipeListener() );*/
			//pipe.createInputPipe(PGAFAdvertisementFactory.getDebugPipeAdvertLeaf2RDV()
			//		, new PipeListener() );
			/*RendezVousService serv =  leaf.getNetPeerGroup().getRendezVousService();
	        System.out.println("is rendezvous: " + serv.isRendezVous());*/
			
			leaf.StopOnINITSignal();
			//leaf.CompetitionRandomNumber = new Integer( (int)(Math.random()*Integer.MAX_VALUE));
			
			/*code to create socket.
			 */
			/*JxtaMulticastSocket socket = new JxtaMulticastSocket( leaf.getNetPeerGroup(),
					PGAFAdvertisementFactory.getTestMulticastAdvert());
			byte [] buffer = new byte[1000];
			java.net.DatagramPacket packet = new DatagramPacket( buffer, buffer.length);
			socket.receive(packet);
			String str = new String(packet.getData(), 0, packet.getLength());
			System.out.println("Received " + str);
			for(int i = 0; i < 10; i++){
				Thread.sleep(500);
			}
			socket.close();*/ /**/
			
			/*//to Publish an advertisement
			DiscoveryService diserv = leaf.getNetPeerGroup().getDiscoveryService();
			adv.setGridNodeName("hellohello");
			
			System.out.println("my random num is: " + leaf.CompetitionRandomNumber);
			adv.setRandomNum(leaf.CompetitionRandomNumber);
			adv.setHostName(leaf.Name);
			System.out.println( "++"+ adv.getGridNodeName() );
			//diserv.remotePublish(adv, 120000L);
			diserv.publish(adv);*/ 
			
			/**
			 * Testing the Network Instruction Advertisement
			 */
			if(true){
				
				DiscoveryService disserv = leaf.getNetPeerGroup().getDiscoveryService();
				
				System.out.println("before query for advertisements");
				disserv.getRemoteAdvertisements(null, DiscoveryService.ADV,
		                null , null , 4, new discoveryClass(leaf));
				System.out.println("AfterQuery for advertisements");
				
				
				
			}
			
			
			
			
			//The code below get Advertisemets to test
		
			if( false){ //pipe testing
				DiscoveryService disserv = leaf.getNetPeerGroup().getDiscoveryService();
				
				System.out.println("before query for advertisements");
				disserv.getRemoteAdvertisements(null, DiscoveryService.ADV,
		                null , null , 4, new discoveryClass(leaf));
				System.out.println("AfterQuery for advertisements");
				
				
				PipeService service = leaf.getNetPeerGroup().getPipeService();
				
				PipeID pipe_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, n.getBytes());
				
				PipeAdvertisement pipe_adv = 
					AdvertFactory.getDebugLogPipeAdvertisement(pipe_id);
				
				
				/*there is a chance the listener could be calle before this funciton is done*/
				 //this.setDebugPipeCreated( true );    
				
		        OutputPipe pipe = service.createOutputPipe(pipe_adv, 10000L);
		      
		        Message MyMessage = new Message();
				StringMessageElement MyStringMessageElement = 
					new StringMessageElement(DirectorRDV.MessageTag, "Hello world test dec 20 ", null);
				MyMessage.addMessageElement
					(DirectorRDV.DebugMessageNamespace, MyStringMessageElement);
		        pipe.send(MyMessage);
				
				pipe.close();
	        
			}
			
			while(true){
				Thread.sleep(2000);
				//leaf.printRoutedAdverts();
				//System.out.println("name: " + leaf.Name);
				
				
			}
			//leaf.CloseConection();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	public static class discoveryClass implements DiscoveryListener{
		SimpleEdge Leaf;
		public discoveryClass( SimpleEdge leaf){
			Leaf = leaf;
		}
		public void discoveryEvent(DiscoveryEvent arg0) {
			DiscoveryResponseMsg TheDiscoveryResponseMsg = arg0.getResponse();
	        
	        if (TheDiscoveryResponseMsg!=null) {
	            
	            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
	         
	            while (TheEnumeration.hasMoreElements()) {
	            	Advertisement TheAdv = TheEnumeration.nextElement();
	            	String ToDisplay = "Found " + TheAdv.getClass().getSimpleName();
	            	ToDisplay += " Name: " + TheAdv.getClass().getName();
	            	
	            	ToDisplay += " AdID: " + TheAdv.getID();
	            	
	            	//If the advertisement is a DirectorRDV Advertisement, test print.
	            	/*if ( TheAdv.getClass().getName().compareTo(DirectorRDVAdvertisement.class.getName()) == 0){
	 	            	DirectorRDVAdvertisement temp = (DirectorRDVAdvertisement) TheAdv;
	 	            	ToDisplay = ToDisplay + "\n\nof " + temp.getDocument(MimeMediaType.XMLUTF8).toString();
	 	            }*/
	            	
	            	
	            	/*if( TheAdv.getClass().getName().compareTo(DebugLogPipeAdvertisement.class.getName()) == 0){
	            		DebugLogPipeAdvertisement adv = (DebugLogPipeAdvertisement) TheAdv;
	            		ToDisplay += "\n\nof " + adv.getDocument(MimeMediaType.XMLUTF8).toString() ;
	            		
	            		ToDisplay += adv.getName() + "\n" + adv.getID() + "\n";
	            	}*/
	            	
	            	if( TheAdv.getClass().getName().compareTo(NetworkInstructionAdvertisement.class.getName()) == 0){
	            		NetworkInstructionAdvertisement advert = (NetworkInstructionAdvertisement) TheAdv;
	            		ToDisplay += "\n\nof " + advert.getDocument(MimeMediaType.XMLUTF8).toString() ;
	            		
	            		ToDisplay += advert.getName() + "\n" + advert.getID() + "\n";
	            	}
	            	
	            	
	 	            System.out.println(ToDisplay);
	            }
	            
	        }
		}
		
	}
	public void printRoutedAdverts(){
		try {
			DiscoveryService TheDiscoveryService = getNetPeerGroup().getDiscoveryService();
	
	        Enumeration<Advertisement> TheAdvEnum;
			
				TheAdvEnum = TheDiscoveryService.getLocalAdvertisements(DiscoveryService.ADV, null, null);
			
	        
	        while (TheAdvEnum.hasMoreElements()) { 
	            
	            Advertisement TheAdv = TheAdvEnum.nextElement();
	            
	            String ToDisplay = "Found " + TheAdv.getClass().getSimpleName();
	            
	            /*if (TheAdv.getClass().getName().compareTo(RouteAdv.class.getName())==0) {
	                
	                // We found a route advertisement
	                RouteAdv Temp = (RouteAdv) TheAdv;
	                ToDisplay = ToDisplay + "\n\nto " + Temp.getDestPeerID().toString();
	                
	            } else if (TheAdv.getClass().getName().compareTo(RdvAdv.class.getName())==0) {
	                
	                // We found a rendezvous advertisement
	                RdvAdv Temp = (RdvAdv) TheAdv;
	                ToDisplay = ToDisplay + "\n\nof " + Temp.getPeerID().toString();
	                
	            }else if ( TheAdv.getClass().getName().compareTo(DirectorRDVAdvertisement.class.getName()) == 0){
	            	DirectorRDVAdvertisement temp = (DirectorRDVAdvertisement) TheAdv;
	            	ToDisplay = ToDisplay + "\n\nof " + temp.getGridNodeName().toString();
	            }else
	            if ( TheAdv.getClass().getName().compareTo(DataLinkAdvertisement.class.getName()) == 0){
	            	DataLinkAdvertisement temp = (DataLinkAdvertisement) TheAdv;
	            	ToDisplay = ToDisplay + "\n\nof " + temp.getDocument(MimeMediaType.XMLUTF8).toString() ;
	            }
	            System.out.println(ToDisplay);
	            TheDiscoveryService.flushAdvertisement(TheAdv);
	                 */   
	        }
	        
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static class PipeListener implements PipeMsgListener{
		public void pipeMsgEvent(PipeMsgEvent arg0) {
			Message ReceivedMessage = arg0.getMessage();
	        String TheText = ReceivedMessage.getMessageElement("DummyNameSpace", "HelloElement").toString();
	        System.out.println(TheText);
		}
	}
	
}
