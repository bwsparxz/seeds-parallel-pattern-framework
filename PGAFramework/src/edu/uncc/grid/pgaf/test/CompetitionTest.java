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
import java.util.ArrayList;
import java.util.Enumeration;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.protocol.DiscoveryResponseMsg;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVCompetitionAdvertisement;
import edu.uncc.grid.pgaf.p2p.Types;


/***
 * Nov 25 2008
 * This class was made to test the competition process.  This is used to decide which node gets to be a Director.
 * There were other features added to it in the actual implementaiton code.  For example, the port for the node
 * has to be the DEFAULT_PORT which it is usually 50000, if not, the algorithm assumes there is more than one 
 * process in the CPU (multicore cpu) and the process drops out of the competition.
 * 
 * This code was used for testing.
 * 
 * @author jfvillal
 *
 */

public class CompetitionTest extends SimpleEdge{
	public ArrayList<String> neighbors;
	public boolean IamWinning;
	public CompetitionTest(String seed, int port_arg)
			throws Exception {
		super(seed, port_arg, "CompetitionTest" + port_arg);
		
		neighbors =  new ArrayList<String>();
		IamWinning = true;
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
try {
			
			CompetitionTest leaf = new CompetitionTest(args[0] , Integer.parseInt(args[1]));
			
			//PipeService pipe = leaf.getNetPeerGroup().getPipeService();
			/*pipe.createInputPipe(PGAFAdvertisementFactory.getDebugPipeAdvertRDV2Leaf()
					, new PipeListener() );*/
			//pipe.createInputPipe(PGAFAdvertisementFactory.getDebugPipeAdvertLeaf2RDV()
			//		, new PipeListener() );
			/*RendezVousService serv =  leaf.getNetPeerGroup().getRendezVousService();
	        System.out.println("is rendezvous: " + serv.isRendezVous());*/
			
			leaf.StopOnINITSignal();
			leaf.CompetitionRandomNumber = new Integer( (int)(Math.random()*Integer.MAX_VALUE));
			
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
			Thread.sleep(500);
			
			DirectorRDVCompetitionAdvertisement adv = new DirectorRDVCompetitionAdvertisement();
			DiscoveryService diserv = leaf.getNetPeerGroup().getDiscoveryService();
			System.out.println("my random num is: " + leaf.CompetitionRandomNumber);
			adv.setRandomNumber(leaf.CompetitionRandomNumber);
			adv.setPeerID(leaf.PID.toString());
			adv.setGridName("hellohello");
			adv.setNetworkType(Types.WanOrNat.NAT_NON_UPNP);
			System.out.println( "++"+ adv.getGridName() );
			//diserv.remotePublish(adv, 120000L);
			 
			diserv.publish(adv);
			
			DiscoveryService disserv = leaf.getNetPeerGroup().getDiscoveryService();
			/**
			 * it takes about 10 seconds to get 10 nodes organized with this menthod
			 * if there are more nodes, then, they should first randomly decide weather
			 * to participate or not, the remaining one can participate for 
			 * leadership.
			 * */
			//for( int i = 0; i < 2; i++){
				System.out.println("--" + adv.getGridName() );
				disserv.getRemoteAdvertisements(null, DiscoveryService.ADV,
	                null , null , 20, new discoveryClass(leaf));
			//}
			int i = 0;
			for(int j = 0; j < 100; j++){
				i = j;
				if(leaf.IamWinning){
					Thread.sleep(50);
				}else{
					break;
				}
			}
			if( leaf.IamWinning){
				System.out.println("I won.  I will be the LEADER, it took me " + (double)(i * 100) / (double)1000+ " secs to find out!");
			}else{
				System.out.println("I Lost.  It took me " + + (double)(i * 100) / (double)1000 + " to find oiut ");
			}
			for(int j = 0; j < 50; j++){
				Thread.sleep(100);
			}
			
			leaf.CloseConection();
			//leaf.CloseConection();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static class discoveryClass implements DiscoveryListener{
		CompetitionTest Leaf;
		public discoveryClass( CompetitionTest leaf){
			Leaf = leaf;
		}
		public void discoveryEvent(DiscoveryEvent arg0) {
			DiscoveryResponseMsg TheDiscoveryResponseMsg = arg0.getResponse();
			System.out.println("called" );
	        if (TheDiscoveryResponseMsg!=null) {
	            System.out.println("called" );
	            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
	         
	            while (TheEnumeration.hasMoreElements()) {
	            	Advertisement TheAdv = TheEnumeration.nextElement();
	            	//String ToDisplay = "Found " + TheAdv.getClass().getSimpleName();
	            	if ( TheAdv.getClass().getName().compareTo(DirectorRDVCompetitionAdvertisement.class.getName()) == 0){
	 	            	DirectorRDVCompetitionAdvertisement temp = (DirectorRDVCompetitionAdvertisement) TheAdv;
	 	            	//ToDisplay = ToDisplay + "\n\nof " + temp.getGridNodeName();
	 	            	if( temp.getPeerID().compareTo(Leaf.PID.toString()) == 0){
	 	            		//System.out.println("ignoring my own advertisements") ;
	 	            	}else{
	 	            		//Leaf.neighbors.add(temp.getHostName());
	 	            		if( temp.getRandomNumber() > Leaf.CompetitionRandomNumber ){
	 	            			//System.out.println(" I lost :-( " );
	 	            			Leaf.IamWinning = false;
	 	            			DiscoveryService disserv = Leaf.getNetPeerGroup().getDiscoveryService();
		 	           			
	 	            			try {
									disserv.publish(temp);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
	 	            		}else{
	 	            			//System.out.println(" I won one :-) " );
	 	            			
	 	            		}
	 	            		
	 	            		//System.out.println("random: " + temp.getRandomNum() + "\n------------------------------------------");
	 	            	}
	 	            }
	 	           // System.out.println(ToDisplay);
	 	            
	                
	            }
	            
	        }
		}
		
	}
}
