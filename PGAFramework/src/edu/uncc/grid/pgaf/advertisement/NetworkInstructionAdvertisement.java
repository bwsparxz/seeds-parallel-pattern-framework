/* * Copyright (c) Jeremy Villalobos 2009
 *   * All rights reserved
 */
package edu.uncc.grid.pgaf.advertisement;

import java.net.URI;
import java.util.Enumeration;

import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.Types.Instruction;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.TextElement;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;
import net.jxta.pipe.PipeID;
/**
 * 
 * This advertisement is used to issue an instruction to the p2p nodes.
 * It is primarily used by the Observer to shutdown the Network.  As the framework gets more complex, this
 * class will be used to control the network.
 * @author jfvillal
 *
 */
public class NetworkInstructionAdvertisement extends Advertisement {
	
	public final static String AdvertisementType = "jxta:NetworkInstructionAdvertisement";
	public final static String NameTag = "NameTag";
	public final static String InstructionTag = "InstructionTag";
	public final static String GridNodeNameTag = CommunicationConstants.GridNameTag;
	public final static String PipeIDTag = "PipeIDTag";
	private static String Name = "Network Instruction Advertisement";
	
	/**
	 * Grid node name
	 */
	private String GridNodeName;
	/**
	 * An object of Network instruction.  This object carries the instructions on it
	 */
	private Instruction NetworkInstruction;
	/**
	 * This advertisement unique ID.
	 */
	PipeID AdvID;
	
	private final static String[]  IndexableFields =
		{ NameTag, GridNodeNameTag, InstructionTag, PipeIDTag };
	public NetworkInstructionAdvertisement(){
		AdvID = null;
		NetworkInstruction = Types.Instruction.SHUTDOWN;
	}
	public NetworkInstructionAdvertisement(Element Root){
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
		}
		if( el_name.compareTo(PipeIDTag) == 0){
			URI uri = URI.create(val);
			AdvID = PipeID.create(uri);
		}
		if( el_name.compareTo(InstructionTag) == 0){
			NetworkInstruction = Types.Instruction.valueOf(val);
		}
		if( el_name.compareTo(GridNodeNameTag) == 0 ){
			GridNodeName = val;
		}
		
	}
	@Override
	public Document getDocument(MimeMediaType asMimeType) {
		 StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(
	                asMimeType, AdvertisementType);
	     Element el;
	     el = doc.createElement(NameTag, Name);
	     doc.appendChild(el);
	     el = doc.createElement(GridNodeNameTag, GridNodeName);
	     doc.appendChild(el);
	     el = doc.createElement(PipeIDTag, AdvID.toURI().toString());
	     doc.appendChild(el);
	     el = doc.createElement(InstructionTag, NetworkInstruction.toString() );
	     //System.out.println(" SinkType: " + SinkType.toString() );
	     doc.appendChild(el);
	     
		return doc;
	}

	@Override
	public ID getID() {
		return AdvID;
	}

	@Override
	public String[] getIndexFields() {
		// TODO Auto-generated method stub
		return this.IndexableFields;
	}
	public Advertisement clone() throws CloneNotSupportedException {
		NetworkInstructionAdvertisement Result = (NetworkInstructionAdvertisement) super.clone();
		
		Result.setGridNodeName(this.getGridNodeName());
		Result.setName(this.getName());
		Result.setNetworkInstruction(this.getNetworkInstruction());
		Result.setAdvID(this.getAdvID());
	
		return Result;
	}
	
	public static class Instantiator implements AdvertisementFactory.Instantiator {

        public String getAdvertisementType() {
            return NetworkInstructionAdvertisement.getAdvertisementType();
        }

        public Advertisement newInstance() {
        	return new NetworkInstructionAdvertisement();
        }

        public Advertisement newInstance(net.jxta.document.Element root) {
            return new NetworkInstructionAdvertisement(root);
        }
        
    }
	
	public static String getName() {
		return Name;
	}
	public static void setName(String name) {
		Name = name;
	}
	public String getGridNodeName() {
		return GridNodeName;
	}
	public void setGridNodeName(String gridNodeName) {
		GridNodeName = gridNodeName;
	}
	public Instruction getNetworkInstruction() {
		return NetworkInstruction;
	}
	public void setNetworkInstruction(Instruction networkInstruction) {
		NetworkInstruction = networkInstruction;
	}
	public PipeID getAdvID() {
		return AdvID;
	}
	public void setAdvID(PipeID advID) {
		AdvID = advID;
	}
	public static String getAdvertisementType() {
		return AdvertisementType;
	}
	
}
