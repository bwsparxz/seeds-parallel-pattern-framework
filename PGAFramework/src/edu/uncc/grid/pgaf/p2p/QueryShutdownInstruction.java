package edu.uncc.grid.pgaf.p2p;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.advertisement.NetworkInstructionAdvertisement;
import edu.uncc.grid.pgaf.communication.MultiModePipeMapper;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.protocol.DiscoveryResponseMsg;

public class QueryShutdownInstruction  implements DiscoveryListener{
	Node Network;
	boolean FinalShutdown;
	public QueryShutdownInstruction( Node e , boolean is_final_shutdown){
		Network = e;
		FinalShutdown = is_final_shutdown;
		DiscoveryService discovery_service = Network.getNetPeerGroup().getDiscoveryService();
		discovery_service.getRemoteAdvertisements( null , DiscoveryService.ADV , NetworkInstructionAdvertisement.InstructionTag
				, null , 100,this);
	}
	@Override
	public void discoveryEvent(DiscoveryEvent event) {
	DiscoveryResponseMsg TheDiscoveryResponseMsg = event.getResponse();
        
        if (TheDiscoveryResponseMsg!=null) {
            
            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
            DiscoveryService discovery_service = Network.getNetPeerGroup().getDiscoveryService();
            while (TheEnumeration.hasMoreElements()) {
            	Advertisement TheAdv = TheEnumeration.nextElement();
            
        		if( TheAdv instanceof NetworkInstructionAdvertisement && !this.FinalShutdown ){
            		NetworkInstructionAdvertisement advert = (NetworkInstructionAdvertisement) TheAdv;
            		if( advert.getNetworkInstruction() == Types.Instruction.SHUTDOWN 
            				||
            				advert.getNetworkInstruction() == Types.Instruction.TERMINATE	){
            			//set end lprogram true
            			
            			PipeID id = IDFactory.newPipeID( PeerGroupID.defaultNetPeerGroupID);
            			advert.setAdvID(id);

            			for( int i = 0; i < 5; i++){
	            			try {
	            				
								discovery_service.publish(advert);
							} catch (IOException e1) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e1));
							}
	            			discovery_service.remotePublish(advert);
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
								e.printStackTrace();
							}
            			}
            			
            			FinalShutdown  = true;
            		}
            	}	
            	
            	
            	
            	
            }
        }
	}
}
