package edu.uncc.grid.seeds.comm.dependency;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.advertisement.DependencyAdvertisement;

/**
 * The DependencyMapper is similar to the Shared Memory Mapper.  It will store the advertisement for the
 * Dependencies.  
 * 
 * The Advance user can then retrive the dependencies from the mapper.  The main id used to store and retrive
 * the data is the string dot-separated id
 * 
 * All methods are static so that this structure is shared by all the threads in a single Node.
 * 
 * @author jfvillal
 *
 */
public class DependencyMapper  {
	/**
	 * The first map maps the segment ide to a root dependency id
	 * then the root dependency id maps to the Hida object
	 * the hida object is a tree data structure that manages
	 * all the advertisements for that dependency.
	 * 
	 * 				PatternId,     SegmentID    Root Dependency
	 */
	static public String SynchronizeAnchor = "hello";
	static private  Map<String, Map<String, Map<String,Hida>>> DependencyMap;
	
	public static void initDependencyMapper(){
		//only used to store root dependencies.
		DependencyMap = Collections.synchronizedMap(new HashMap<String, Map<String, Map<String,Hida>>>());
	}
	public static void closeDependencyMapper(){
		DependencyMap = null;
	}
	
	/**
	 * returns hida.  And hida has all the necesary advertisements to connect to a root
	 * dependency.
	 * @param id
	 * @return
	 */
	public static synchronized Hida getAdvertForDependency(PipeID pattern_id, HierarchicalDependencyID id){
		Map<String,Map<String,Hida>> seg_hida_map = DependencyMap.get(pattern_id.toString());
		if( seg_hida_map == null){
			return null;
		}
		
		HierarchicalSegmentID seg_id = id.getSid();
		Map<String, Hida> hida_map = seg_hida_map.get(seg_id.toString() );
		if( hida_map != null){
			Hida hida = hida_map.get(id.getRoot().getDeptIdString());
			if( hida == null){
				return null;
			}
			return hida.getHida(id); //this returns the hida that is closest to id
			//this may be a parent hida, or the exact hida we are looking for.
		}else{
			return null;
		}
	}
	/**
	 * 
	 * Stores Dependency advertisements in a tree data structure that makes the 
	 * dependency creation much easier.
	 * 
	 * The algorith will store the latest advertisement overriding the old one.
	 * This makes dataflow network reorganization feasable withtou adding extra
	 * code on this function. 
	 * 
	 * However, the Dependency Engine, had to be modified so that it can reject
	 * connections from nodes that had old advert data that pointed to the Node's 
	 * Engine, which by that time would not be hosting the dependency advertised.
	 * 
	 * @param adv
	 * @return
	 * @throws IOException 
	 */
	public static synchronized void checkAdvertisement( Advertisement adv, DiscoveryService service) {
		if( adv instanceof DependencyAdvertisement ){
			//store this dependency
			DependencyAdvertisement DAdv = (DependencyAdvertisement) adv;
			//DAdv.setDirty(false);
			Map<String,Map<String,Hida>> seg_hida_map = DependencyMap.get(DAdv.getPatternID().toString());
			if( seg_hida_map == null){
				seg_hida_map = new HashMap<String,Map<String,Hida>>();
				DependencyMap.put(DAdv.getPatternID().toString(), seg_hida_map);
			}
			
			/*if( dadv.gethyerarchyid().tostring().compareto("15/1:0/1")==0){
				system.out.println(" dependencymapper:checkadvertisement: found it " );
			}*/
			
			HierarchicalDependencyID id =  DAdv.getHyerarchyID() ;
			HierarchicalSegmentID seg_id = id.getSid();
			//<SegID str>, <Hida tree>
			Map<String, Hida> hida_map = seg_hida_map.get(seg_id.toString() );
			
			if( hida_map == null){//if null create new hida tree
				hida_map = new HashMap<String,Hida>(); //create hida map
				//add advertisement
				String root = id.getRoot().getDeptIdString();
				hida_map.put( root , new Hida( id, DAdv ) ); //add new hida to hida_map
				seg_hida_map.put(seg_id.toString(), hida_map); //add hida to the seg map
			}else{ //if exists, get the hida tree from it.
				Hida hida = hida_map.get(id.getRoot().getDeptIdString());
				if( hida == null){ //if hida tree is null, create one
					// add new hida
					hida = new Hida( id, DAdv);
					//add new tree to map
					hida_map.put( id.getRoot().getDeptIdString(), hida);
				}else{//if exists, add the leaf or branch to the tree.
					//add to existing hida
					hida.addChild(id, DAdv, service );
				}
			}
			/*try {
				FileWriter w = new FileWriter( "./DependencyMapper:checkAdvertisement.dump.txt", true);
				w.write( DAdv.toString() );
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
		}
	}
	/**
	 * Removes the hida information for a specific segment.
	 * @deprecated
	 * @param pattern_id
	 * @param id
	 * @return
	 */
	/*public static synchronized boolean deleteAdvertFromMap(PipeID pattern_id, HierarchicalDependencyID id){
		Map<String,Map<String,Hida>> seg_hida_map = DependencyMap.get(pattern_id.toString());
		if( seg_hida_map == null){
			return false;
		}
		
		HierarchicalSegmentID seg_id = id.getSid();
		seg_hida_map.remove(seg_id.toString());
		return true;
	}*/

}
