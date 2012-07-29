package edu.uncc.grid.pgaf.communication;

import java.util.Enumeration;

import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.seeds.comm.dependency.DependencyMapper;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.protocol.DiscoveryResponseMsg;
/**
 * Queries network for a specific connection advertisement.
 * @author jfvillal
 *
 */
public class LinkQuery implements DiscoveryListener{
	Node Network;
	public LinkQuery( String atribute, String value, Node e ){
		Network = e;
		DiscoveryService discovery_service = Network.getNetPeerGroup().getDiscoveryService();
		discovery_service.getRemoteAdvertisements( null , DiscoveryService.ADV , atribute
				, value , 100,this);
	}
	@Override
	public void discoveryEvent(DiscoveryEvent event) {
	DiscoveryResponseMsg TheDiscoveryResponseMsg = event.getResponse();
        
        if (TheDiscoveryResponseMsg!=null) {
            
            Enumeration<Advertisement> TheEnumeration = TheDiscoveryResponseMsg.getAdvertisements();
            DiscoveryService discovery_service = Network.getNetPeerGroup().getDiscoveryService();
            while (TheEnumeration.hasMoreElements()) {
            	Advertisement TheAdv = TheEnumeration.nextElement();
            	MultiModePipeMapper.checkAdvertisement(TheAdv, discovery_service);	
            }
        }
	}

}
