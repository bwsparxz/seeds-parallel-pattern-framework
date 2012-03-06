package edu.uncc.grid.pgaf.communication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.pipe.PipeID;

import edu.uncc.grid.pgaf.advertisement.DataLinkAdvertisement;
import edu.uncc.grid.pgaf.communication.shared.SharedMemDispatcher;
import edu.uncc.grid.pgaf.communication.wan.ConnectionEstablishedListener;

public class MultiModePipeMapper {
	/**Maps segment ids to the DataLinkAdvertisements
	 * Nov 10, I added the patter id to this map. Otherwise, the patterns with their simple integer
	 * id for communication will conflict with each other at the framework leve.  the 
	 * Jxta id is pretty big for them to conflict at the JXTA lever.
	 *  */
	public static Map<PipeID,Map<Long, DataLinkAdvertisement>> DataLinkPipeAdvertisementList;
	public static Map<Integer,ConnectionEstablishedListener> ConEstList;
	/**The {@link SharedMemDispatcher} instance is used to manage the shared memory pipe connections.*/
	/*
	 * the mem dispatcher does not need changes because it maps directy to the jxta id, which is 
	 * highly unlikely to conflict with each other.
	 */
	public static SharedMemDispatcher SMDispatcher;
	
	
	public static void initMapper(){
		MultiModePipeMapper.DataLinkPipeAdvertisementList = Collections.synchronizedMap(
				new    HashMap<PipeID,Map<Long, DataLinkAdvertisement>>());
		ConEstList = Collections.synchronizedMap( new HashMap<Integer, ConnectionEstablishedListener>() );
		SMDispatcher = new SharedMemDispatcher();
	}
	
	/**
	 * Will validate the advertisement.  If it is of type DataLinkAdvertisement, the advertisement gets stored
	 * in a Map to be used by the local node processes and shared memory processes.
	 * 
	 * @param TheAdv
	 * @param service the DiscoveryService, it can be null if not used within JXTA platform.
	 * @return returns true if the advertisement was stored on the map. false if nothing was done
	 * @throws IOException 
	 */
	public static void checkAdvertisement( Advertisement TheAdv, DiscoveryService service ) {
		/*
		 * September 26. Note: the adverts for a connection that is moving to another process will 
		 * always have a higher sergment number, so no conflict should emerge.
		 */
		 if(  TheAdv instanceof DataLinkAdvertisement ){
          	DataLinkAdvertisement advert = (DataLinkAdvertisement) TheAdv;
          	
          	Map<Long, DataLinkAdvertisement> pattern_data_links = DataLinkPipeAdvertisementList.get(advert.getPatternID());
          	
          	if( pattern_data_links == null){
          		//this is the first time we store a data link in it
          		pattern_data_links = Collections.synchronizedMap(new HashMap<Long,DataLinkAdvertisement>());
          		pattern_data_links.put(advert.getDataID(), advert);
          		DataLinkPipeAdvertisementList.put( advert.getPatternID(), pattern_data_links);
          	}else{
          		//we already have a hash map for this pattern id
          		//so we can just add advertisement to the list
          		DataLinkAdvertisement current_advert = pattern_data_links.get( advert.getDataID() );
          		if( current_advert == null){
          			pattern_data_links.put(advert.getDataID(), advert);	
          		}else{
	          		if( current_advert.isDirty() ){
	          			//System.out.println("MultiModePipeMapper:checkAdvertisement -- Dirty Link Advert... flushing... " + advert.getDataID() );
	          			if(service != null){
							try {
								service.flushAdvertisement(TheAdv) ;
							} catch (IOException e) {
						
								e.printStackTrace();
							}
	          			}
	          		}
          		}
          		
          	}
          	//DataLinkPipeAdvertisementList.put( advert.getDataID(), advert);	
			/*try {
				FileWriter w = new FileWriter( "./MultiModePipeMapper:checkAdvertisement.dump.txt", true);
				w.write( advert.toString() );
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
         }
	}
	
}
