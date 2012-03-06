package edu.uncc.grid.seeds.comm.dependency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;

import edu.uncc.grid.pgaf.advertisement.DependencyAdvertisement;
import edu.uncc.grid.pgaf.p2p.Node;
/**
 * 
 * Hida: Hierarchical ID Advertisement.
 * 
 * This object is used to store the advertisements, so that they can be easily found if a dependency needs
 * to be created.
 * 
 * This object behaves like a linked tree.  and the hierarchy is from parent to child.
 * Which is opposite to the HierarchicalDependencyID.  This HierarchicalDepedencyID is done from child
 * to parent in order to make it easier for the advance user.  This class however, is only 
 * used by the expert layer.
 * 
 * @author jfvillal
 *
 */
public class Hida {
	/**
	 * The hierarchical level of this Hida.  The ID may be a branch or a 
	 * leaf node
	 */
	HierarchicalDependencyID LevelId;
	/**
	 * A copy of the advertisement for the dependecy stored in this hida
	 */
	DependencyAdvertisement Advert;
	/**
	 * The next branches or leafs that make up this tree.
	 */
	Hida[] Children;
	
	/**
	 * This private method is used to be called recursively to create branches and 
	 * leafs in the tree.  The tree can be used to query if a communicaiton line is
	 * made up of multiple leafs, or if this node is just one leaf of the needed
	 * data by the remote node.  The classic case is that a single leaf 
	 * corresponds to a single communication line.
	 * @param id
	 * @param advert
	 * @param level
	 */
	private Hida( HierarchicalDependencyID id , DependencyAdvertisement advert , int level){
		LevelId = id.getLevel( level);
		if( LevelId.hierarchicalCompareTo(id) > 1){
			Advert = null;
			HierarchicalDependencyID next_child = id.getLevel(level+1); 
			Children = new Hida[next_child.getTotal()];
			Children[next_child.getId()] = new Hida( id, advert, level+1);
			//all the other children stay null
		}else if(LevelId.hierarchicalCompareTo(id) == 1){
			Advert = advert;
			Children = null;
		}//else do nothing
	}
	/**
	 * Create new Hida object by calling a recursive constructor that will create a tree
	 * using the data in the id object.
	 * @param id
	 * @param advert
	 */
	public Hida( HierarchicalDependencyID id , DependencyAdvertisement advert){
		this(id, advert, 0);
	}
	/**
	 * Returns a Hida (Hierachical Id Advertisement )
	 * structure that contains the advertisements needed to construct
	 * a dependency.
	 * @param id the id of the communication line the requester is trying to build
	 * @return returns a hida structure that contain a leaf or a tree of id's that 
	 * 		will be used to establish the communication line.  or null if the id
	 * 		was not in this structure.
	 */
	public Hida getHida( HierarchicalDependencyID id ){
		if( LevelId.hierarchicalCompareTo(id) == 1 ){
			return this;
		}else if( LevelId.hierarchicalCompareTo(id) > 1){
			//get the one of my children that will get the id closer to the 
			//one we are loking for.
			if( Children == null){
				return this; //I am the highest level available
			}else{
				//try to get closer to what it was requested.
				//int m = id.getLevel(LevelId.getLevel() + 1).getId();
				if( Children[ id.getLevel(LevelId.getLevel() + 1).getId() ] == null){
					//this child is not in jet
					return this; //return this since it is the highes complete 
					//hida
				}else{
					//this child is in, get the hida from it.
					return Children[ id.getLevel(LevelId.getLevel() + 1).getId() ].getHida(id);
				}
			}
		}else{
			return null; //I don't have access to parents
			//or I am not who you are looking for.
		}
	}
	/**
	 * will report if all adverts are present starting from branch
	 * id
	 * @return
	 * @throws UnexpectedMissingChildException 
	 */
	public boolean allAdvertsPresent(HierarchicalDependencyID branch, long cycle_version){
		if( Children == null){
			/**
			 * I am replacing the dirty flag with a version because it is more accurate and 
			 * resolve consistency problems that the dirty flag was not able to resolve
			 */
			if( Advert != null){
				if( Advert.getCycleVersion() != cycle_version){
					return false;
				}else{
					return true;
				}
			}else{
				return false;
			}
			
			/*
			 * if children null and the advert is not null.  we are a leaf and should return true.
			 * this leaf has the avert that is needed in orther for it to be used to connect to the 
			 * dependency.
			 * 
			 */
			/*if( Advert != null){
				if( Advert.isDirty()){
					return false;
				}else{
					return true;
				}
			}*/
		}else{
			/**
			 * only care about the advertisements that are above the branch id. because the other lower
			 * level dependencies may not be needed by the user of this class.
			 */
			if( LevelId.hierarchicalCompareTo( branch) > 1 ){
				HierarchicalDependencyID next_child = branch.getLevel(LevelId.getLevel()+1);
				if( Children[next_child.getId()] == null){
					Node.getLog().log(Level.INFO, " Children[" + next_child.getId() +"] is null, advert may have to arrived yet.");
					return false;
				}else{
					return Children[next_child.getId()].allAdvertsPresent(branch, cycle_version);
				}
			}else{
				for( int i = 0; i < Children.length; i++){
					if( Children[i] == null){
						return false;
					}else{
						if( !Children[i].allAdvertsPresent(branch, cycle_version)){
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	public Hida( HierarchicalDependencyID id){
		this(id, null);
	}
	/**
	 * Adds child dependency advertisement.
	 * @param id
	 * @param advert
	 * @return true if the current advert is the most up to date, false if the received advert is dirty.
	 */
	public void addChild( HierarchicalDependencyID id , DependencyAdvertisement advert, DiscoveryService service){
		if( LevelId.hierarchicalCompareTo(id) > 1 ){
			Advert = null; //this is in case a previous advert was for the whole dependency (i.e 0/1)
			//but the new adverts should the dependency was split up (i.e 0/1.0/3 , 0/1.1/3 ....)
			HierarchicalDependencyID next_child = id.getLevel(LevelId.getLevel() + 1);
			if( Children == null){
				Children = new Hida[next_child.getTotal()];
			}
			if( Children[next_child.getId()] == null){
				Children[next_child.getId()] = new Hida( id, advert, LevelId.getLevel() + 1);
			}else{
				Children[next_child.getId()].addChild(id, advert, service);
			}
		}else if( LevelId.hierarchicalCompareTo(id) == 1){
			//september 25.  the newer advert will always have a higher segment number.
			
			/**
			 * Nov 8.  Because of so many moving parts, let me explain the Advert!=null statement.
			 * If we are creating a new hida (with root dependency), the object will be created using
			 * a constructor method.  Therefore, if this part is reached, and the Advert is null, is 
			 * because this hida is growing into higher level connection.  Those connections are 
			 * addressed in the part id > 1 above.  This part is only to update a segment id, while
			 * Advert is not null.  If Adver is null, we can savely ignore that adver since it is
			 * out of date.
			 */
			if( Advert != null){
				if(Advert.getCycleVersion() < advert.getCycleVersion()){
					System.out.println("Advert" +Advert.getHyerarchyID().toString() + " " + Advert.getDataID() 
									+ " advert "+ advert.getHyerarchyID().toString() +" "+ advert.getDataID() );
					//only replace if the incomming advert has a higher segment number.
					Advert = advert; //replaces the advert
					//Advert.setDirty(false);
					//ans = true;
				}else if( Advert.getCycleVersion() > advert.getCycleVersion() ){ //leaving out the == option
					//ans = false; //the framework should get rid of that old advert
					try {
						if(service != null) service.flushAdvertisement(advert);
						//System.out.println("Hida:addChild -- Dirty Dept Advert... flushing... " + advert.getDataID() 
						//		+ "current: " + Advert.getDataID() + " received: " + advert.getDataID() );
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}else{
				//This advert should be out of date, and should be flushed to prevent
				//waist of network bandwidth.
				try {
					if(service != null) service.flushAdvertisement(advert);
					//System.out.println("Hida:addChild -- Dirty Dept Advert... flushing... " + advert.getDataID() 
					//		+ "current: " + Advert.getDataID() + " received: " + advert.getDataID() );
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}//if 0 or below, the id is either a parent id or not related.
	}
	/**
	 * Used to test the class by printing its branches and leaves.
	 * @param prefix
	 */
	public void testPrintTree(String prefix){
		System.out.println(prefix +"> " + LevelId.toString() );
		if( Advert != null){
			//System.out.println(Advert.toString() );
		}
		if(  Children != null){
			for( int i = 0; i < Children.length; i++){
				if( Children[i] != null){
					Children[i].testPrintTree(prefix + "--");
		
				}else{
					System.out.println( prefix + "--" + " no advert for this child....");
				}
			}
		}
	}
	/**
	 * Returns the children for this Hida branch, or root.
	 * @return
	 */
	public Hida[] getChildren(){
		return Children;
	}
	
	public List<DependencyAdvertisement> getAdvertisements(){
		List<DependencyAdvertisement> list = new ArrayList<DependencyAdvertisement> ();
		if( this.Advert != null) {
			list.add(Advert);
		}
		if( Children != null){
			for(int i = 0 ; i < Children.length ; i++){
				if(Children[i] != null){
					List<DependencyAdvertisement> child_list = Children[i].getAdvertisements();
					list.addAll(child_list);
				}
			}
		}
		return list;
	}
}




