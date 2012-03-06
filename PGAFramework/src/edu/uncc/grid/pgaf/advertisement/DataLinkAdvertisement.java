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
import net.jxta.pipe.PipeID;

/**
 * This Custom Advertisement class is used to advertise a data link connection.  The Advertisement is passed with 
 * attributes that are needed by the peers to connect to the advertisement issuing the advertisement.
 * See JXSE documentation for more on Advertisments.
 * 
 * The Advertisement notifies the network of this nodes resources.  In the new data-flow model, the advertisement simple
 * notifies about the existence of a socket server.  A socket server can service multple requests for different 
 * dependencies that are hosted at this server.  This reduces the number of ports used. 
 * 
 * 
 * @author jfvillal
 *
 */
public class DataLinkAdvertisement extends Advertisement {
	private String Name = "Data Link Advertisement";
	
	public final static String AdvertisementType = "jxta:DataLinkAdvertisement";
	public final static String NameTag = "Name";
	public final static String DataIDTag = "DataIDTag";
	public final static String GridNameTag = CommunicationConstants.GridNameTag;
	public final static String WanOrNatTag = "WanOrNatTag";
	public final static String DataLinkPipeIDTag = "DataLinkPipeIDTag";
	public final static String RoutedDataLinkPipeIDTag = "RoutedDataLinkPipeIDTag";
	public final static String LanAddressTag = "LanAddressTag";
	public final static String WanAddressTag = "WanAddressTag";
	public final static String PortTag = "PortTag";
	public final static String PatternIDTag = "PatternIDTag";
	/**
	 * User defined numeric id for the connections
	 */
	private Long DataID;
	/**
	 * The Grid node's name
	 */
	private String GridName;
	/**
	 * The nodes type of network.
	 */
	private String WanOrNat; //set this using a Type.WanOrNat Enumeration
	/**
	 * the pipe id used to comunicate directly with the node proposing the connection
	 */
	private PipeID DataLinkPipeID;
	/**
	 * The PipeID used for redirected connection such as NAT and Fire walled connections
	 */
	private PipeID RDataLinkPipeID; //routed pipe id 
	//private DataLinkComm Comm;
	/**
	 * The Lan Address used for the node that is starting the communication
	 */
	private String LanAddress;
	/**
	 * The Wan address used by the node stating the communication
	 */
	private String WanAddress;
	/**
	 * The port used by the socket connection if TCP Java Sockets are being used.
	 */
	private int Port;
	/**
	 * Used for the Pattern ID.  A Jxta ID is used so that it can identify the Patter 
	 * Advertisemetn.  If an integer value is needed, we can use the hashCode() 
	 * function.
	 */
	private PipeID PatternID;
	 
	
	private final static String[] IndexableFields = 
	{ NameTag, DataIDTag, GridNameTag, WanOrNatTag
		, DataLinkPipeIDTag, RoutedDataLinkPipeIDTag, LanAddressTag, WanAddressTag, PortTag, PatternIDTag};
	
	public DataLinkAdvertisement(){
		
	}
	public DataLinkAdvertisement(Element Root){
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
		if( el_name.compareTo(DataIDTag)== 0 ){
			DataID = Long.parseLong(val);
		}else
		if( el_name.compareTo(GridNameTag) == 0){
			GridName = val;
		}else
		if( el_name.compareTo(WanOrNatTag) == 0){
			WanOrNat = val;
		}else
		if( el_name.compareTo(DataLinkPipeIDTag) == 0){
			URI uri = URI.create(val);
			DataLinkPipeID = PipeID.create(uri);
		}else
		if( el_name.compareTo(this.RoutedDataLinkPipeIDTag) == 0){
			URI uri = URI.create(val);
			this.RDataLinkPipeID = PipeID.create(uri);
		}else
		if( el_name.compareTo(this.PatternIDTag ) == 0){
			URI uri = URI.create(val);
			this.PatternID = PipeID.create(uri);
		}
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
	public Document getDocument(MimeMediaType asMimeType) {
		StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(
                asMimeType, AdvertisementType);
	     Element el;
	     el = doc.createElement(NameTag, Name);
	     doc.appendChild(el);
	     el = doc.createElement(DataIDTag, DataID.toString());
	     doc.appendChild(el);
	     el = doc.createElement(GridNameTag, GridName);
	     doc.appendChild(el);
	     el = doc.createElement(WanOrNatTag, WanOrNat);
	     doc.appendChild(el);
	     el = doc.createElement(DataLinkPipeIDTag, DataLinkPipeID.toURI().toString());
	     doc.appendChild(el);
	     el = doc.createElement(this.RoutedDataLinkPipeIDTag, this.RDataLinkPipeID.toURI().toString());
	     doc.appendChild(el);
	     el = doc.createElement(this.LanAddressTag, this.LanAddress);
	     doc.appendChild(el);
	     el = doc.createElement(this.WanAddressTag, this.WanAddress);
	     doc.appendChild(el);
	     el = doc.createElement(this.PortTag, "" + this.Port);
	     doc.appendChild(el);
	     el = doc.createElement(this.PatternIDTag, PatternID.toURI().toString() );
	     doc.appendChild( el);
	     
	     //el = doc.createElement(CommTag, this.Comm.toString());
	     //doc.appendChild(el);
     
		return doc;
	}
	@Override
	public Advertisement clone() throws CloneNotSupportedException {
		DataLinkAdvertisement Result = (DataLinkAdvertisement) super.clone();
		Result.setName(this.getName());
		Result.setDataID(this.getDataID());
		Result.setGridName(this.getGridName());
		Result.setWanOrNat(this.getWanOrNat());
		Result.setDataLinkPipeID(this.getDataLinkPipeID());
		Result.setRDataLinkPipeID(this.getRDataLinkPipeID());
		Result.setLanAddress(this.getLanAddress());
		Result.setWanAddress(this.getWanAddress());
		Result.setPort(this.getPort());
		Result.setPatternID(this.getPatternID());
		//Result.setComm(this.getComm());
		return Result;
	}
	public static class Instantiator implements AdvertisementFactory.Instantiator {

        public String getAdvertisementType() {
            return DataLinkAdvertisement.getAdvertisementType();
        }

        public Advertisement newInstance() {
        	return new DataLinkAdvertisement();
        }

        public Advertisement newInstance(net.jxta.document.Element root) {
            return new DataLinkAdvertisement(root);
        }
        
    }
	@Override
	public ID getID() {
		return this.getDataLinkPipeID();
	}

	@Override
	public String[] getIndexFields() {
		return this.IndexableFields;
	}
	
	public static String getAdvertisementType(){
		return AdvertisementType;
	}
	
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public Long getDataID() {
		return DataID;
	}
	public void setDataID(Long dataID) {
		DataID = dataID;
	}
	public String getGridName() {
		return GridName;
	}
	public void setGridName(String gridName) {
		GridName = gridName;
	}
	public String getWanOrNat() {
		return WanOrNat;
	}
	public void setWanOrNat(String wanOrNat) {
		WanOrNat = wanOrNat;
	}
	public PipeID getDataLinkPipeID() {
		return DataLinkPipeID;
	}
	public void setDataLinkPipeID(PipeID dataLinkPipeID) {
		DataLinkPipeID = dataLinkPipeID;
	}
	public PipeID getRDataLinkPipeID() {
		return RDataLinkPipeID;
	}
	public void setRDataLinkPipeID(PipeID dataLinkPipeID) {
		RDataLinkPipeID = dataLinkPipeID;
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
	public PipeID getPatternID() {
		return PatternID;
	}
	public void setPatternID(PipeID patternID) {
		PatternID = patternID;
	}
	transient boolean Dirty = false;
	public void setDirty(boolean set){
		Dirty = set;
	}
	public boolean isDirty() {
		return Dirty;
	}
	
}
