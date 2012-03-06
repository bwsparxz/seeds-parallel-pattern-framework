/* * Copyright (c) Jeremy Villalobos 2009
 *   * All rights reserved
 */
package edu.uncc.grid.pgaf.advertisement;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import edu.uncc.grid.pgaf.communication.CommunicationConstants;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.TextElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;
/***
 * This Advertisement is used to tell the nodes that this node is a Director.  The 
 * Director is used to route secure communication( not implemented )
 *  and to route communication from
 * NAT's (implemented) and other networks that have restricted access to the Internet 
 * like fire walls.
 * 
 * Nodes that receive this Advertisement will know to whom to send debug information
 * and where the routing services are.
 * 
 * @author Jeremy Villalobos
 *
 */
public class DirectorRDVAdvertisement extends Advertisement {
	public final static String AdvertisementType = "jxta:DirectorRDVAdvertisement";
	//labels
	public final static String IDTag = "PGATag";
	public final static String NameTag = "PGANameTag";
	public final static String GridNodeNameTag = CommunicationConstants.GridNameTag;
	public final static String PipeIDURITag = "PGAPipeIDURITag";
	public final static String DebugPipeIDTag = "DebugPipeIDTag";
	public final static String TunnelPipeIDTag = "TunnelPipeIDTag";
	public final static String LanAddressTag = "LanAddressTag";
	public final static String WanAddressTag = "WanAddressTag";
	public final static String PortTag = "PortTag";
	/**
	 * Advertisement class name
	 */
	private String Name = "DirectorRDV Advertisement";
	/**
	 * Grid node name.
	 */
	private String GridNodeName = "none";
	/**
	 * The director RDV's id
	 */
	private PeerID RDVID;
	/**
	 * The PipeID used to connect the UDP packet pipe used for 
	 * debug messages
	 */
	private PipeID DebugPipeID;
	/**
	 * PipeID used for the Socket created to route communication around NAT's.
	 */
	private PipeID TunnelPipeID;
	/**
	 * The Lan Address used for the node that is starting the communication
	 */
	private String LanAddress;
	/**
	 * The Wan address used by the node starting the communication
	 */
	private String WanAddress;
	/**
	 * The port used by the socket connection if TCP Java Sockets are being used.
	 */
	private int Port;
	
	private final static String[]  IndexableFields = 
		{ IDTag, NameTag, GridNodeNameTag, PipeIDURITag, DebugPipeIDTag, TunnelPipeIDTag, 
		LanAddressTag, WanAddressTag, PortTag};
	public DirectorRDVAdvertisement(){
		
	}
	public DirectorRDVAdvertisement(Element Root){
		  // Retrieving the elements
        TextElement MyTextElement = (TextElement) Root;

        Enumeration TheElements = MyTextElement.getChildren();
        
        while (TheElements.hasMoreElements()) {
            
            TextElement TheElement = (TextElement) TheElements.nextElement();
            
            ProcessElement(TheElement);
            
        }        
	}
	public void ProcessElement(TextElement el){
		String el_name = el.getName();
		String val = el.getTextValue();

		if( el_name.compareTo(NameTag) == 0){
			Name = val;
		}else
		if( el_name.compareTo(PipeIDURITag) == 0){
			URI uri = URI.create(val);
			RDVID = PeerID.create(uri);
		}else
		if( el_name.compareTo(GridNodeNameTag) == 0){
			GridNodeName = val;
		}else
		if( el_name.compareTo(DebugPipeIDTag) == 0){
			URI uri = URI.create(val);
			DebugPipeID = PipeID.create(uri);
		}else
		if( el_name.compareTo(TunnelPipeIDTag) == 0){
			URI uri = URI.create(val);
			TunnelPipeID = PipeID.create(uri);
		}else
		if( el_name.compareTo(this.LanAddressTag) == 0){
			this.LanAddress = val;
		}else
		if( el_name.compareTo(this.WanAddressTag) == 0){
			this.WanAddress = val;
		}else
		if( el_name.compareTo(this.PortTag) == 0){
			this.Port = Integer.parseInt( val );
		}
	}
	@Override
	public Document getDocument(MimeMediaType arg0) {
		 StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(
	                arg0, AdvertisementType);
	     Element el;
	     el = doc.createElement(NameTag, Name);
	     doc.appendChild(el);
	     el = doc.createElement(GridNodeNameTag, GridNodeName);
	     doc.appendChild(el);
	     el = doc.createElement(PipeIDURITag, RDVID.toURI().toString());
	     doc.appendChild(el);
	     el = doc.createElement(DebugPipeIDTag, DebugPipeID.toURI().toString());
	     doc.appendChild(el);
	     el = doc.createElement(TunnelPipeIDTag, TunnelPipeID.toURI().toString());
	     doc.appendChild(el);
	     el = doc.createElement(this.LanAddressTag, this.LanAddress);
	     doc.appendChild(el);
	     el = doc.createElement(this.WanAddressTag, this.WanAddress);
	     doc.appendChild(el);
	     el = doc.createElement(this.PortTag, "" + this.Port);
	     doc.appendChild(el);
	     return doc;
	}

	@Override
	public Advertisement clone() throws CloneNotSupportedException {
		DirectorRDVAdvertisement Result = (DirectorRDVAdvertisement) super.clone();
		Result.setGridNodeName(this.getGridNodeName());
		Result.setRDVID(RDVID);
		Result.setName(this.getName());
		Result.setDebugPipeID(this.getDebugPipeID());
		Result.setTunnelPipeID(this.getTunnelPipeID());
		Result.setLanAddress(this.getLanAddress());
		Result.setWanAddress(this.getWanAddress());
		Result.setPort(this.getPort());
		return Result;
	}

	@Override
	public String getAdvType() {
		return DirectorRDVAdvertisement.class.getName();
	}

	@Override
	public ID getID() {
		return this.RDVID;
	}

	@Override
	public String[] getIndexFields() {
		return IndexableFields;
	}

	
	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}
	
	public static String getPipeIDURITag() {
		return PipeIDURITag;
	}
	public String getGridNodeName() {
		return GridNodeName;
	}

	public void setGridNodeName(String gridNodeName) {
		GridNodeName = gridNodeName;
	}
	public static String getAdvertisementType() {
		return AdvertisementType;
	}
	
	public static class Instantiator implements AdvertisementFactory.Instantiator {

        public String getAdvertisementType() {
            return DirectorRDVAdvertisement.getAdvertisementType();
        }

        public Advertisement newInstance() {
        	return new DirectorRDVAdvertisement();
        }

        public Advertisement newInstance(net.jxta.document.Element root) {
            return new DirectorRDVAdvertisement(root);
        }
        
    }

	public String getLanAddress() {
		return LanAddress;
	}
	public void setLanAddress(String lanAddress) {
		LanAddress = lanAddress;
	}
	public String getWanAddress() {
		return WanAddress;
	}
	public void setWanAddress(String wanAddress) {
		WanAddress = wanAddress;
	}
	public int getPort() {
		return Port;
	}
	public void setPort(int port) {
		Port = port;
	}
	public PeerID getRDVID() {
		return RDVID;
	}
	public void setRDVID(PeerID rdv_id) {
		RDVID = rdv_id;
	}
	public PipeID getDebugPipeID() {
		return DebugPipeID;
	}
	public void setDebugPipeID(PipeID debugPipeID) {
		DebugPipeID = debugPipeID;
	}
	public PipeID getTunnelPipeID() {
		return TunnelPipeID;
	}
	public void setTunnelPipeID(PipeID tunnelPipeID) {
		TunnelPipeID = tunnelPipeID;
	}
	
}
