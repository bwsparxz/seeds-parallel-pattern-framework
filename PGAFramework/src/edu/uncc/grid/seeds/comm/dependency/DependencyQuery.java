package edu.uncc.grid.seeds.comm.dependency;

import java.util.Enumeration;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.protocol.DiscoveryResponseMsg;
import edu.uncc.grid.pgaf.p2p.Node;

/**
 * This class does a fine query on the network.   The finer the query the more likely
 * it is we will get a response.
 * @author jfvillal
 *
 */
public class DependencyQuery implements DiscoveryListener{
	Node Network;
	//String Value;
	public DependencyQuery( String atribute, String value, Node e ){
		Network = e;
		//Value = value;
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
            //int i = 0;
            while (TheEnumeration.hasMoreElements()) {
            	Advertisement TheAdv = TheEnumeration.nextElement();
            	DependencyMapper.checkAdvertisement( TheAdv , discovery_service);
            	
            	//the debug code will store the adverts returned by the query to a file
            	/*synchronized( DependencyMapper.SynchronizeAnchor ){
	            	if( TheAdv instanceof DependencyAdvertisement ){
	        			//store this dependency
		            	try {
		            		DependencyAdvertisement DAdv = (DependencyAdvertisement) TheAdv;
		            		String file_name =  "./" + Value.replace("*", "").replace("/", "%") + "DependencyQuery:checkAdvertisement.dump.txt";
		    				FileWriter w = new FileWriter(file_name, true);
		    				w.write( "hid: " + DAdv.getHyerarchyID().toString() + " sid: " + DAdv.getDataID() + " list size " +  ++i + "\n");
		    				w.close();
		    			} catch (IOException e) {
		    				e.printStackTrace();
		    			}
	            	}
            	}*/
            }
        }
	}
}