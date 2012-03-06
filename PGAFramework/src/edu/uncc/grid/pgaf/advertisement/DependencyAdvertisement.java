/* * Copyright (c) Jeremy Villalobos 2009
 *   * All rights reserved
 */
package edu.uncc.grid.pgaf.advertisement;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import edu.uncc.grid.pgaf.communication.CommunicationConstants;
import edu.uncc.grid.seeds.comm.dependency.HierarchicalDependencyID;
import edu.uncc.grid.seeds.comm.dependency.InvalidHierarchicalDependencyIDException;

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
 * The DependencyAdvertisement announces the dependency output at one node.  The dependencies manage streams of inputs and outputs
 * 
 * Subsequent implementations will include the hierarchical dependencies that are the goal for this research.
 * 
 * @author jfvillal
 *
 */
public class DependencyAdvertisement extends Advertisement {
	private String Name = "Dependency Advertisement";
	
	public final static String AdvertisementType = "jxta:DependencyAdvertisement";
	
	public final static String NameTag = "Name";
	public final static String DependencyIDTag = "DependencyIDTag";
	public final static String DataIDTag = "DataIDTag";
	public final static String HyerarchyIDTag = "HyerarchIDTag";
	public final static String PatternIDTag = "PatternIDTag";
	public final static String CycleVersionTag = "CycleVersionTag";
	/**
	 * PipeID used to uniquely indentify the Dependency
	 */
	PipeID UniqueDependencyID;
	/**
	 * User defined numeric id for the connections
	 * Id of the emitter or output process.
	 */
	private Long DataID;
	/**
	 * The hyerarch id for the Dependency
	 */
	private HierarchicalDependencyID HyerarchyID;
	/**
	 * Used for the Pattern ID.  A Jxta ID is used so that it can identify the Patter 
	 * Advertisemetn.  If an integer value is needed, we can use the hashCode() 
	 * function.
	 */
	private PipeID PatternID;
	/**
	 * 
	 * This is the version the client connecting to the dependency expects.
	 * 
	 */
	long CycleVersion;
	 
	
	private final static String[] IndexableFields =	{NameTag, DependencyIDTag, DataIDTag, HyerarchyIDTag, PatternIDTag, CycleVersionTag };
	
	public DependencyAdvertisement(){
		
	}
	public DependencyAdvertisement(Element Root){
		  // Retrieving the elements
    TextElement MyTextElement = (TextElement) Root;

    Enumeration TheElements = MyTextElement.getChildren();
    
	    while (TheElements.hasMoreElements()) {
	        
	        TextElement TheElement = (TextElement) TheElements.nextElement();
	        
	        try {
				ProcessElement(TheElement);
			} catch (InvalidHierarchicalDependencyIDException e) {
				e.printStackTrace();
			}
	        
	    }        
	}
	public void ProcessElement(TextElement el) throws InvalidHierarchicalDependencyIDException{
		String el_name = el.getName();
		String val = el.getTextValue();

		if( el_name.compareTo(NameTag) == 0){
			Name = val;
		}else
		if( el_name.compareTo(DataIDTag)== 0 ){
			DataID = Long.parseLong(val);
		}else
		if( el_name.compareTo(DependencyIDTag) == 0){
			URI uri = URI.create(val);
			this.UniqueDependencyID = PipeID.create(uri);
		}else
		if( el_name.compareTo(HyerarchyIDTag) == 0){
			HyerarchyID = HierarchicalDependencyID.fromString(val);
		}else
		if( el_name.compareTo(this.PatternIDTag ) == 0){
			URI uri = URI.create(val);
			this.PatternID = PipeID.create(uri);
		}else 
		if(el_name.compareTo(this.CycleVersionTag) == 0){
			this.CycleVersion = Long.parseLong(val);
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
	     el = doc.createElement(DependencyIDTag, UniqueDependencyID.toURI().toString());
	     doc.appendChild(el);
	     el = doc.createElement(HyerarchyIDTag, HyerarchyID.toString());
	     doc.appendChild(el);
	     el = doc.createElement(this.PatternIDTag, PatternID.toURI().toString() );
	     doc.appendChild( el);
	     el = doc.createElement(this.CycleVersionTag, ""+this.CycleVersion );
	     doc.appendChild( el);
		return doc;
	}
	@Override
	public Advertisement clone() throws CloneNotSupportedException {
		DependencyAdvertisement Result = (DependencyAdvertisement) super.clone();
		Result.setName(this.getName());
		Result.setDataID(this.getDataID());
		Result.setDependencyID(this.getDependencyID());
		Result.setHyerarchyID(this.getHyerarchyID());
		Result.setPatternID(this.getPatternID());
		Result.setCycleVersion( this.getCycleVersion());
		return Result;
	}
	public static class Instantiator implements AdvertisementFactory.Instantiator {
        public String getAdvertisementType() {
            return DependencyAdvertisement.getAdvertisementType();
        }
        public Advertisement newInstance() {
        	return new DependencyAdvertisement();
        }
        public Advertisement newInstance(net.jxta.document.Element root) {
            return new DependencyAdvertisement(root);
        }
    }
	@Override
	public ID getID() {
		return this.getDependencyID();
	}

	public synchronized long getCycleVersion() {
		return CycleVersion;
	}
	public synchronized void setCycleVersion(long cycleVersion) {
		CycleVersion = cycleVersion;
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
	
	public PipeID getPatternID() {
		return PatternID;
	}
	public void setPatternID(PipeID patternID) {
		PatternID = patternID;
	}
	public PipeID getDependencyID() {
		return UniqueDependencyID;
	}
	public void setDependencyID(PipeID dependencyID) {
		UniqueDependencyID = dependencyID;
	}
	public HierarchicalDependencyID getHyerarchyID() {
		return HyerarchyID;
	}
	public void setHyerarchyID(HierarchicalDependencyID hyerarchyID) {
		HyerarchyID = hyerarchyID;
	}
	
	/*boolean Dirty = false;
	public boolean isDirty(){
		return Dirty;
	}
	public void setDirty( boolean set){
		Dirty = set;
	}*/
	
}
