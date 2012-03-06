/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * 
 *   * All rights reserved
 */
package edu.uncc.grid.pgaf.p2p;

import edu.uncc.grid.pgaf.advertisement.DirectorRDVAdvertisement;
import edu.uncc.grid.pgaf.advertisement.DirectorRDVCompetitionAdvertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
/**
 * Used to create most often used advertisemsents for PGAF
 * @author jfvillal
 *
 */
public class AdvertFactory {
	
	static String DebugPipeNameLeaftoRDV = "DebugPipeNameLeaftoRDV";
	/**
	 * This advertisement is used to organize the debugging pipes
	 * @return
	 */
	public static PipeAdvertisement getDebugPipeAdvertLeaf2RDV() {
	    PipeAdvertisement PipeAdv = (PipeAdvertisement) 
	    	AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
	    PipeID pipe_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, DebugPipeNameLeaftoRDV.getBytes());
	    PipeAdv.setPipeID(pipe_id);
	    PipeAdv.setType(PipeService.UnicastType);
	    PipeAdv.setName("Debug Leaf to RDV");
	    PipeAdv.setDescription("Debug Leaf to RDV");
	    return PipeAdv;
    }
	static String DebugPipeNameRDVtoLeaf = "DebugPipeNameRDVtoLeaf";
	/**
	 * This advertisement is used to organize the debugging pipes
	 * @return
	 */
	public static PipeAdvertisement getDebugPipeAdvertRDV2Leaf() {
	    PipeAdvertisement PipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
	    PipeID pipe_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, DebugPipeNameRDVtoLeaf.getBytes());
	    PipeAdv.setPipeID(pipe_id);
	    PipeAdv.setType(PipeService.UnicastType);
	    PipeAdv.setName("Debug RDV to Leaf");
	    PipeAdv.setDescription("Debug RDV to Leaf");
	    return PipeAdv;
    }
	
	/**
	 * This Advertisement is used to transport simple text messages from all the nodes to a special node
	 * called an Observer node.  This node will receive any Java Exception from the user's application.
	 * Some of the errors from the framework will not be routed either because they should be resolved 
	 * by the developer (me) or because the user's application managed to create an exception in a 
	 * place where the exception will not be routed.
	 * @param pipe
	 * @return
	 */
	public static PipeAdvertisement getDebugLogPipeAdvertisement(PipeID pipe){
		PipeAdvertisement PipeAdv = (PipeAdvertisement) 
			AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
		PipeAdv.setType(PipeService.UnicastType);
		PipeAdv.setPipeID( pipe );
		PipeAdv.setName("Debug Log Pipe");
		PipeAdv.setDescription("Debug Log Pipe");
		return PipeAdv;
	}
	/**
	 * This Advertisment is used to conect the Data Link Sockets, this advertisements are used to conect
	 * the work pool process so that data can be moved around.  But will also be used for pipes and for
	 * sync programs
	 * @param pipe
	 * @return
	 */
	public static PipeAdvertisement GetDataLinkPipeAdvertisement(PipeID pipe) {
        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) 
        	AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        MyPipeAdvertisement.setPipeID(pipe);
        MyPipeAdvertisement.setType(PipeService.UnicastType);
        MyPipeAdvertisement.setName("Data Link Socket");
        MyPipeAdvertisement.setDescription("Socket used to communicate user's processes");
        
        return MyPipeAdvertisement;
	}
	
	
	/**
	 * This Advertisement is used by the DirectorRDV to announce the existence of a Socket dispatcher for 
	 * the conection of VirtualRoutes.  The virtual route allows a NAT node to communicate with other 
	 * WAN or NAT_UPNP nodes through the use of its WAN-connected Director RDV
	 * 
	 */
	public static PipeAdvertisement getVirtualRouteAdvertisement(PipeID pipe) {
        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) 
        	AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        MyPipeAdvertisement.setPipeID(pipe);
        MyPipeAdvertisement.setType(PipeService.UnicastType);
        MyPipeAdvertisement.setName("Virtual Route Pipe");
        MyPipeAdvertisement.setDescription("Socket used to indirectly route Objects from a NAT network");
        
        return MyPipeAdvertisement;
	}
	
	/**Advertisements made for testing*/
	
	static String ConnectionTestPipeAdvert = "DebugPipeNameRDVtoLeaf";
	public static PipeAdvertisement getConnectionTestPipeAdvert() {
	    PipeAdvertisement PipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
	    PipeID pipe_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, ConnectionTestPipeAdvert.getBytes());
	    PipeAdv.setPipeID(pipe_id);
	    PipeAdv.setType(PipeService.UnicastType);
	    PipeAdv.setName("Connection Test Pipe");
	    PipeAdv.setDescription("Connection Test Pipe");
	    return PipeAdv;
    }
	
	static String ConnectionTestMulticastAdvert = "ConnectionTestMulticastAdvert";
    public static PipeAdvertisement getConnectionTestMulticastAdvert() {
        PipeAdvertisement Advert = (PipeAdvertisement) 
        	AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID MyPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, ConnectionTestMulticastAdvert.getBytes());
        Advert.setPipeID(MyPipeID);
        Advert.setType(PipeService.PropagateType);
        Advert.setName("Connection Test Multicast");
        Advert.setDescription("Created by PGAFAdvertisementFactory");
        return Advert;
    }
	
	static String TestMulticastAdvert = "TestMulticastAdvert";
    public static PipeAdvertisement getTestMulticastAdvert() {
        
        // Creating a Pipe Advertisement
        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID MyPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, TestMulticastAdvert.getBytes());

        MyPipeAdvertisement.setPipeID(MyPipeID);
        MyPipeAdvertisement.setType(PipeService.PropagateType);
        MyPipeAdvertisement.setName("Test Multicast");
        MyPipeAdvertisement.setDescription("Created by PGAFAdvertisementFactory");
        
        return MyPipeAdvertisement;
        
    }
    
}
