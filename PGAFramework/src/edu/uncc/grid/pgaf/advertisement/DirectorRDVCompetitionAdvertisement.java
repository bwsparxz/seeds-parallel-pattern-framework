/* * Copyright (c) Jeremy Villalobos 2009
 *   * All rights reserved
 */

package edu.uncc.grid.pgaf.advertisement;

import java.util.Enumeration;

import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.p2p.Types;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.TextElement;
import net.jxta.id.ID;
/**
 * This Advertisement is used to "compete" with the Local Network nodes to see who gets to 
 * be a director.  The contest is held over multi-cast mode because none of the nodes have
 * established Rendezvous connections at this point.
 * The contest is as follows:  
 * 	1. Create a random number
 * 	2. Send the random number to neighbors using this advertisement.
 *  3. The node with the highest number wins.
 * @author jfvillal
 *
 */
public class DirectorRDVCompetitionAdvertisement extends Advertisement {
	public final static String AdvertisementType = "jxta:ConnectionTestAdvertisement";
	
	private final static String Name = "DirectorRDV Competition Advertisement";
	
	public final static String RandomNumberTag = "RandomNumberTag";
	public final static String GridNameTag = CommunicationConstants.GridNameTag;
	public final static String PeerIDTag = "PeerIDTag";
	public final static String NetworkTypeTag = "NetworkTypeTag";
	/**
	 * Random number for competition
	 */
	private Integer RandomNumber;
	/**
	 * Grid node name
	 */
	private String GridName;
	/**
	 * PeerID of the peer emitting the advertisement
	 */
	private String PeerID;
	/**
	 * Network type variable
	 */
	private Types.WanOrNat NetType;
	
	private final static String[] IndexableFields = 
		{ RandomNumberTag, GridNameTag, PeerIDTag, NetworkTypeTag};
	public DirectorRDVCompetitionAdvertisement(){
		
	}
	public DirectorRDVCompetitionAdvertisement(Element root){
		TextElement text_elements = (TextElement) root;
		Enumeration elements = text_elements.getChildren();
		while( elements.hasMoreElements()){
			TextElement el = (TextElement) elements.nextElement();
			ProcessElement(el);
		}
	}
	public void ProcessElement( TextElement el){
		String el_name = el.getName();
		String val = el.getTextValue();
		if( el_name.compareTo(RandomNumberTag) == 0 ){
			RandomNumber = Integer.decode(val);
		}
		if( el_name.compareTo(GridNameTag) == 0){
			GridName = val;
		}
		if( el_name.compareTo(PeerIDTag) == 0){
			PeerID = val;
		}
		if(el_name.compareTo(NetworkTypeTag) == 0){
			NetType = Types.WanOrNat.valueOf(val);
		}
	}
	@Override
	public Document getDocument(MimeMediaType arg0) {
		 StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(
	                arg0, AdvertisementType);
		 Element el;
		 el = doc.createElement(RandomNumberTag, RandomNumber.toString());
		 doc.appendChild(el);
		 el = doc.createElement(GridNameTag, GridName);
		 doc.appendChild(el);
		 el = doc.createElement(PeerIDTag, PeerID);
		 doc.appendChild(el);
		 el = doc.createElement(NetworkTypeTag, NetType.toString());
		 doc.appendChild(el);
		return doc;
	}
	public static class Instantiator implements  AdvertisementFactory.Instantiator{
		public String getAdvertisementType() {
            return DirectorRDVCompetitionAdvertisement.getAdvertisementType();
        }
        public Advertisement newInstance() {
        	return new DirectorRDVCompetitionAdvertisement();
        }

        public Advertisement newInstance(net.jxta.document.Element root) {
            return new DirectorRDVCompetitionAdvertisement(root);
        }
        
	}
	public String getAdvType(){
		return DirectorRDVCompetitionAdvertisement.class.getName();
	}
	@Override
	public ID getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getIndexFields() {
		// TODO Auto-generated method stub
		return null;
	}
	public String getName() {
		return Name;
	}
	public String getGridName() {
		return GridName;
	}
	public void setGridName(String gridName) {
		GridName = gridName;
	}
	public static String getRandomNumberTag() {
		return RandomNumberTag;
	}
	public static String getPeerIDTag() {
		return PeerIDTag;
	}
	public String getPeerID() {
		return PeerID;
	}
	public void setPeerID(String peerID) {
		PeerID = peerID;
	}
	public Integer getRandomNumber() {
		return RandomNumber;
	}
	public void setRandomNumber(Integer randomNumber) {
		RandomNumber = randomNumber;
	}
	public Types.WanOrNat getNetworkType() {
		return NetType;
	}
	public void setNetworkType(Types.WanOrNat networkType) {
		NetType = networkType;
	}
	public static String getAdvertisementType() {
		return AdvertisementType;
	}

}
