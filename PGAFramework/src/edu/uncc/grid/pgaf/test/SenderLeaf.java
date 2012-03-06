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
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.socket.JxtaMulticastSocket;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.advertisement.NetworkInstructionAdvertisement;
import edu.uncc.grid.pgaf.p2p.AdvertFactory;
import edu.uncc.grid.pgaf.p2p.DirectorRDV;
import edu.uncc.grid.pgaf.p2p.LeafWorker;
import edu.uncc.grid.pgaf.p2p.Types;

public class SenderLeaf extends SimpleEdge {
	public static String message;
	public SenderLeaf(String seed, int port_arg) throws Exception {
		super(seed, port_arg, "SenderLeaf");
		// TODO Auto-generated constructor stub
	}
	public static void main(String args[]){
		try {
			SenderLeaf leaf = new SenderLeaf("none" , 50010);
			//message = args[2];
			
			leaf.CompetitionRandomNumber = new Integer( (int)(Math.random()*Integer.MAX_VALUE));
			/*NetworkConfigurator net_config = leaf.getNetManager().getConfigurator();
			net_config.setTcpPublicAddress("68.115.148.71", true);*/
			//PipeService service = leaf.getNetPeerGroup().getPipeService();
			//SenderLeaf.PipeHandler pipe_handler = new SenderLeaf.PipeHandler();
	        //service.createOutputPipe(PGAFAdvertisementFactory.getDebugPipeAdvertLeaf2RDV(),
	        //      pipe_handler );
	        //RendezVousService serv =  leaf.getNetPeerGroup().getRendezVousService();
	        //System.out.println("is rendezvous: " + serv.isRendezVous());

			//code below tests the DirectorRDV Advertisement
			if(false){ //**outdated !!!!!!!!!!!!!!!!!!!!!!!!!!**/
				DirectorRDVAdvertisement adv = new DirectorRDVAdvertisement();
				adv.setGridNodeName("Littleblue");
				String n = "lkdsjflk";
				PipeID id = IDFactory.newPipeID( PeerGroupID.defaultNetPeerGroupID, n.getBytes());
				//adv.setDebugLogPipeID(id);
				DiscoveryService diserv = leaf.getNetPeerGroup().getDiscoveryService();
		
				System.out.println( ""+ adv.getDocument(MimeMediaType.XMLUTF8).toString() );
				diserv.publish(adv); 
			}
			if(false){
				/*ObserverSinkAdvertisement adv = new ObserverSinkAdvertisement();
				adv.setGridNodeName("Littleblue");
				String n = "lkdsjflk";
				PipeID id = IDFactory.newPipeID( PeerGroupID.defaultNetPeerGroupID, n.getBytes());
				adv.setDebugLogPipeID(id);
				adv.setSinkType(Types.PGANodeType.DIRECTOR_SINK_REQUEST);
				DiscoveryService diserv = leaf.getNetPeerGroup().getDiscoveryService();
		
				System.out.println( ""+ adv.getDocument(MimeMediaType.XMLUTF8).toString() );
				diserv.publish(adv);*/
			}
			if(false){
				DataLinkAdvertisement adv = new DataLinkAdvertisement();
				adv.setGridName("littlelbue");
				String n = "jdslfj#sljf";
				PipeID id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, n.getBytes());
				adv.setDataLinkPipeID(id);
				adv.setDataID(4995L);
				adv.setWanOrNat("WAN");
				
				DiscoveryService diserv = leaf.getNetPeerGroup().getDiscoveryService();
				System.out.println("" + adv.getDocument(MimeMediaType.XMLUTF8).toString() );
				diserv.publish(adv);
				
			}
			if(false){
				
				
				//DebugLogPipeAdvertisement advert = new DebugLogPipeAdvertisement();
				
			/*	advert.setAcknowledge(false);
				advert.setGridName("GridOne");
				PipeID pipe = IDFactory.newPipeID(leaf.getNetPeerGroup().getPeerGroupID() );//, new String("lskjfsjljslfjs").getBytes());
				
				advert.setDebugPipeID( pipe);
				
				DiscoveryService serve = leaf.getNetPeerGroup().getDiscoveryService();
				System.out.println("printing ... " + pipe.toString());
				
				
				*/
				
				
				for(int i = 0; i < 5; i++){
				//	serve.publish(advert);
					//serve.remotePublish(advert, 1000L);
					Thread.sleep(2000);
				}
				
				
				PipeService service = leaf.getNetPeerGroup().getPipeService();
				PipeID pipe_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, RecvLeaf.n.getBytes());
				
				PipeAdvertisement pipe_adv = 
					AdvertFactory.getDebugLogPipeAdvertisement(pipe_id);
				
				InputPipe pipe_i = service.createInputPipe(pipe_adv);
		        Message msg = pipe_i.waitForMessage();
		        
		        Message ReceivedMessage = msg;
		        String TheText = ReceivedMessage.getMessageElement
		        	(DirectorRDV.DebugMessageNamespace, DirectorRDV.MessageTag).toString();
		        
		        System.out.println(" The message received is: " + TheText);
		        
		        pipe_i.close();
				
			}
			
			/**
			 * testing Network Instruction Advertisement
			 */
			
			if(true){
				NetworkInstructionAdvertisement advert = 
					(NetworkInstructionAdvertisement) AdvertisementFactory.newAdvertisement(NetworkInstructionAdvertisement.getAdvertisementType());
				advert.setGridNodeName("GridOne");
				advert.setNetworkInstruction(Types.Instruction.SHUTDOWN);
				PipeID  pipe_id= IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
				
				advert.setAdvID(pipe_id);
				
				DiscoveryService diserv = leaf.getNetPeerGroup().getDiscoveryService();
				System.out.println("" + advert.getDocument(MimeMediaType.XMLUTF8).toString() );
				diserv.publish(advert);
				
			}
			
			
			
			
			/*DiscoveryService disserv = leaf.getNetPeerGroup().getDiscoveryService();
			disserv.getRemoteAdvertisements(null, DiscoveryService.ADV,
					  DirectorRDVAdvertisement.GridNodeNameTag, "hellohello", 4, new disClass(leaf)); */
			//code to create a socket
			
			
			
			/*  JxtaMulticastSocket multicast_socket = new JxtaMulticastSocket(leaf.getNetPeerGroup(), PGAFAdvertisementFactory.getTestMulticastAdvert());
			String msg = message + "\t\t*** from " + leaf.Name;
			DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length());
			multicast_socket.send(packet);
			for(int i = 0; i < 10; i++){
				Thread.sleep(500);
			}
			multicast_socket.close();*/
			
			
			
			leaf.StopOnINITSignal();
			while(true){
				Thread.sleep(2000);
				
				
			}
			//leaf.CloseConection();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static class disClass implements DiscoveryListener{
		SimpleEdge Leaf;
		public disClass( SimpleEdge leaf){
			Leaf = leaf;
		}
		public void discoveryEvent(DiscoveryEvent arg0) {
			DiscoveryResponseMsg TheDiscoveryResponseMsg = arg0.getResponse();
	        
	        if (TheDiscoveryResponseMsg!=null) {
	            
	            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
	         
	            while (TheEnumeration.hasMoreElements()) {
	            	Advertisement TheAdv = TheEnumeration.nextElement();
	            	String ToDisplay = "Found " + TheAdv.getClass().getSimpleName();
	            	if ( TheAdv.getClass().getName().compareTo(DirectorRDVAdvertisement.class.getName()) == 0){
	 	            	DirectorRDVAdvertisement temp = (DirectorRDVAdvertisement) TheAdv;
	 	            	ToDisplay = ToDisplay + "\n\nof " + temp.getGridNodeName();
	 	            	
	 	            }
	 	            System.out.println(ToDisplay);
	 	            
	                
	            }
	            
	        }
		}
		
	}
	public static class PipeHandler implements OutputPipeListener{

		public void outputPipeEvent(OutputPipeEvent arg0) {
			try {
				OutputPipe MyOutputPipe;
				MyOutputPipe = arg0.getOutputPipe();
				Message MyMessage = new Message();
				StringMessageElement MyStringMessageElement = new StringMessageElement("HelloElement", message , null);
				MyMessage.addMessageElement("DummyNameSpace", MyStringMessageElement);
				MyOutputPipe.send(MyMessage);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
}
