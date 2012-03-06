package edu.uncc.grid.pgaf.templates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.sbbi.upnp.messages.UPNPResponseException;
import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.communication.ConnectionManager;
import edu.uncc.grid.pgaf.communication.MultiModePipeMapper;
import edu.uncc.grid.pgaf.communication.wan.java.JavaClientSocketManager;
import edu.uncc.grid.pgaf.communication.wan.java.JavaSocketDispatcher;
import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.interfaces.advanced.UnorderedTemplate;
import edu.uncc.grid.pgaf.p2p.NoPortAvailableToOpenException;
import edu.uncc.grid.pgaf.p2p.Node;
/**
 * This template simply sends a group of data object from the server side to 
 * the client side
 * @author jfvillal
 *
 */
public class DebugTestConections extends UnorderedTemplate {

	public DebugTestConections(Node n) {
		super(n);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean ClientSide(PipeID pattern_id) {
		boolean b = true;
		DataLinkAdvertisement adv = null;
		while( b ){
			Map<Long, DataLinkAdvertisement> pattern_map = MultiModePipeMapper.DataLinkPipeAdvertisementList.get(pattern_id);
			if( pattern_map != null){
				adv = pattern_map.get(0L);			
				if( adv == null){
					Node.getLog().log(Level.FINER, " The advert is not here yet ");		
				}else{
					b = false; //patter_map not null, and adv not null, we have the advert for that link
				}
			}
			if(b){//if b still true... wait
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		String hostname = adv.getLanAddress();
		int port = adv.getPort();
		
		try {
			System.out.println( "Connecting " + hostname + " port " + port  );
			
			PipeID local_pipe = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
			Long local_segment = 2L;
			
			ConnectionManager client = new JavaClientSocketManager( hostname, port, local_pipe.toString(), local_segment
					, null, null, null);
			
			DataObject obj = new DataObject();
			System.out.println( "Sending" );
			client.SendObject(obj);
			System.out.println( "Receiving" );
			obj = (DataObject) client.takeRecvingObject();
			
			System.out.println( "DONE" );
			
			client.close();
			
		} catch (IOException e) {
			System.out.println( Network.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println( Network.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println( Network.getStringFromErrorStack(e));
			e.printStackTrace();
		} 
		
		
		
		
		
		return true;
		
		
	}
	public static class DataObject implements Data{
		/**
		 * 
		 */
		private static final long serialVersionUID = -5605965255083885118L;
		public int segment;
		public DataObject( ){
			segment = 1;
		}
		@Override
		public int getSegment() {
			return segment;
		}
		@Override
		public void setSegment(int segment) {
			this.segment = segment;
			
		}
		@Override
		public byte getControl() {
			return 0;
		}
		@Override
		public void setControl(byte set) {
			// TODO Auto-generated method stub
			
		}
		
	}
	@Override
	public void ServerSide(PipeID pattern_id) {
		List<ConnectionManager> lst = Collections.synchronizedList(new ArrayList<ConnectionManager>());
		try {
			PipeID local_pipe = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
			Long local_segment = 1L;
			
			JavaSocketDispatcher disp = new JavaSocketDispatcher(Network, "d", lst, local_pipe.toString(), local_segment, null);
			disp.start();
			
			PipeID CommPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
			PipeID RemoteCommPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
			
			DataLinkAdvertisement advert = (DataLinkAdvertisement) 
			AdvertisementFactory.newAdvertisement(DataLinkAdvertisement.getAdvertisementType());
			advert.setDataID(0L);
			advert.setGridName(Node.getGridName());
			advert.setWanOrNat(Node.getNetworkType().toString());
			advert.setDataLinkPipeID( CommPipeID );
			advert.setRDataLinkPipeID(RemoteCommPipeID);
			advert.setLanAddress(this.Network.getNetDetective().getLANAddress());
			advert.setWanAddress(this.Network.getNetDetective().getWANAddress());
			advert.setPatternID(pattern_id);
			try {
				advert.setPort(Network.generateNewJavaSocketPort());
			} catch (NoPortAvailableToOpenException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}/* catch (UPNPResponseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			System.out.println("Publishing Advertisement");
			DiscoveryService service = Network.getNetPeerGroup().getDiscoveryService();
			service.remotePublish(advert, 30000 );
			service.publish(advert);
			
			while( lst.size() < 1 && !ShutdownOrder){
				Thread.sleep(1000);
				System.out.print("S");
			}
			System.out.println( "Receiving" );
			DataObject d;
			d = (DataObject) lst.get(0).takeRecvingObject();
			
			
			DataObject m = new DataObject();
			m.setSegment(4);
			
			System.out.println( "Sending" );
			lst.get(0).SendObject(m);
			
			System.out.println("DONE");
			
			disp.closeServers();
			
		} catch (IOException e) {
			System.out.println( Network.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoPortAvailableToOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

	@Override
	public String getSuportedInterface() {
		//need to create a new Debug Basic Layer interface, I accidentally deleted the 
		//old one.
		//return DebugTwo.class.getName();
		return null;
	}

	@Override
	public void FinalizeObject() {
		// TODO Auto-generated method stub
		
	}

}
