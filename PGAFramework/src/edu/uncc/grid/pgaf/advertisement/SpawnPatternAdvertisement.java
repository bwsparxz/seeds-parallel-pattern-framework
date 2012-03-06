package edu.uncc.grid.pgaf.advertisement;

import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

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
import net.jxta.pipe.PipeID;
/**
 * This advertisement is used to inform the idle nodes that there is a request for a new 
 * pattern.  When the nodes receive this advertisement they can decide to be used for 
 * the new pattern.
 * 
 * If they decide to join, they use the PatterIDTag and comm_id zero to connect to the 
 * new pattern's diffuse node.
 * 
 * (load-balancing idea)  to decise if this node should be part of the new pattern.
 * Lets get the DirectorRDV's to compute bandwidth and latency every 60 seconds. The
 * DirectorRDV can broadcast this advertisement.
 * The node that is interested in the new pattern can use that information to see if 
 * the node requesting the pattern is far away or close.  Also, the advertisement 
 * can indicate the level of proximity.
 * @author jfvillal
 */
public class SpawnPatternAdvertisement extends Advertisement {
	public final static String AdvertisementType = "jxta:SpawnPatternAdvertisement";
	public final static String PatternClassNameTag = "PatternClassNameTag";
	//public final static String GridNameTag = "GridNameTag";
	public final static String GridNameTag = CommunicationConstants.GridNameTag;
	public final static String PatternIDTag = "PatternIDTag";
	public final static String AdvertIDTag = "AdvertIDTag";
	public final static String SourceAnchorTag = "SourceAnchorTag";
	public final static String SinkAnchorTag = "SinkAnchorTag";
	public final static String RemoteArgumentsTag = "RemoteArgumentsTag";
	//TODO add achors for other nodes ?
	/**
	 * Used to store the Pattern Class Name.  a Java Class with full canonical name
	 * example: edu.uncc.grid.MyNewClass
	 */
	String PatternClassName;
	/**
	 * Grid Name
	 */
	String GridName;
	/**
	 * Pattern id
	 */
	PipeID PatternID;
	/**
	 * Used to set a Source Anchor if one exist.  if default this can be empty but not null
	 */
	String SourceAnchor;
	/**
	 * Used to set the Sink Anchor if one exist.  if default, this can be empty but not null.
	 */
	String SinkAnchor;
	/**
	 * Used to pass the initiation arguments for the module.  It behaves the save as if the 
	 * command was being called from the command line.
	 * TODO add the arguments information.
	 */
	ArrayList<String> Arguments;
	/**
	 * DataFlowRoll:  the node that receives this advertisement will know it Data Flow Roll by doing
	 * an algorithm like this.  If I am in the list for an anchor, then data flow will be the roll
	 * for that anchor.  If I am in the list for the SourceAnchor, the I'll be source anchor.  If
	 * I am not in the list of anchors, then I am a compute node.
	 * 
	 * In the future, when the source anchor is not in the same node, there will be a need for the
	 * node that see's itself in the list, to make sure that, if there are other nodes in that 
	 * GN that also are on the list, for the process to only pick one node out of the bunch.
	 * 
	 */
	
	
	
	private final static String[] IndexableFields = {PatternClassNameTag, GridNameTag, PatternIDTag, AdvertIDTag
		, SourceAnchorTag, SinkAnchorTag, RemoteArgumentsTag};
	
	public SpawnPatternAdvertisement(){

	}
	public SpawnPatternAdvertisement(Element Root){
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
		if( el_name.compareTo(PatternClassNameTag) == 0){
			PatternClassName = val;
		}else
		if( el_name.compareTo(GridNameTag)== 0 ){
			GridName = val;
		}else
		if( el_name.compareTo(GridNameTag) == 0){
			GridName = val;
		}else
		if( el_name.compareTo(PatternIDTag) == 0){
			URI uri = URI.create(val);
			PatternID = PipeID.create(uri);
		}else if ( el_name.compareTo(SourceAnchorTag) == 0 ){
			SourceAnchor = val;
		}else if ( el_name.compareTo(SinkAnchorTag ) == 0){
			SinkAnchor = val;
		}else if ( el_name.compareTo(RemoteArgumentsTag) == 0){
			if( Arguments == null){
				Arguments = new ArrayList<String>();
			}
			Arguments.add(val);
		}
	}
	
	
	@Override
	public Document getDocument(MimeMediaType asMimeType) {
		StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(
                asMimeType, AdvertisementType);
	     Element el;
	     el = doc.createElement(PatternClassNameTag, PatternClassName);
	     doc.appendChild(el);
	     el = doc.createElement(GridNameTag, GridName);
	     doc.appendChild(el);
	     el = doc.createElement(PatternIDTag, "" + PatternID);
	     doc.appendChild(el);
	     el = doc.createElement(SourceAnchorTag, SourceAnchor );
	     doc.appendChild(el);
	     el = doc.createElement(SinkAnchorTag, SinkAnchor);
	     doc.appendChild(el);
	     if( Arguments != null){
	    	 Iterator<String> it = Arguments.iterator();
	    	 while( it.hasNext()){
	    		 String str = it.next();
	    		 el = doc.createElement(RemoteArgumentsTag, str);
	    		 doc.appendChild(el);
	    	 }
	     }
	     
		return doc;
	}

	
	
	@Override
	public Advertisement clone() throws CloneNotSupportedException {
		SpawnPatternAdvertisement Result = (SpawnPatternAdvertisement) super.clone();
		Result.setPatternClassName(this.getPatternClassName());
		Result.setGridName(this.getGridName());
		Result.setPatternID(this.getPatternID());
		Result.setSinkAnchor(this.getSinkAnchor());
		Result.setSourceAnchor(this.getSourceAnchor());
		Result.setArguments(this.getArguments());
		return super.clone();
	}
	@Override
	public ID getID() {
		return this.PatternID;
	}
	
	public static class Instantiator implements AdvertisementFactory.Instantiator {
        public String getAdvertisementType() {
            return SpawnPatternAdvertisement.getAdvertisementType();
        }
        public Advertisement newInstance() {
        	return new SpawnPatternAdvertisement();
        }
        public Advertisement newInstance(net.jxta.document.Element root) {
            return new SpawnPatternAdvertisement(root);
        }
    }

	@Override
	public String[] getIndexFields() {
		return SpawnPatternAdvertisement.IndexableFields;
	}
	public String getPatternClassName() {
		return PatternClassName;
	}
	public void setPatternClassName(String patternClassName) {
		PatternClassName = patternClassName;
	}
	public String getGridName() {
		return GridName;
	}
	public void setGridName(String gridName) {
		GridName = gridName;
	}
	public PipeID getPatternID() {
		return PatternID;
	}
	public void setPatternID(PipeID patternID) {
		PatternID = patternID;
	}
	
	public static String getAdvertisementType() {
		return AdvertisementType;
	}
	public String getSourceAnchor() {
		return SourceAnchor;
	}
	public void setSourceAnchor(String sourceAnchor) {
		SourceAnchor = sourceAnchor;
	}
	public String getSinkAnchor() {
		return SinkAnchor;
	}
	public void setSinkAnchor(String sinkAnchor) {
		SinkAnchor = sinkAnchor;
	}
	public String[] getArguments() {
		if(Arguments == null){
			return null;
		}
		String[] str = new String[Arguments.size()];
		Iterator<String> it = Arguments.iterator();
		int i =0;
		while(it.hasNext()){
			String x = it.next();
			str[i] = x;
			++i;
		}
		return str;
	}
	public void setArguments(String[] arguments) {
		if( Arguments == null){
			Arguments = new ArrayList<String>();
		}else{
			//clean list to start from the beginning.
			Arguments.clear();
		}
		for( int i = 0; i < arguments.length ; i++){
			Arguments.add( arguments[i]);
		}
	}

}
