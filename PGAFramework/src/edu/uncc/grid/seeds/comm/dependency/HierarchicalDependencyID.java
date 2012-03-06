package edu.uncc.grid.seeds.comm.dependency;

import java.io.Serializable;

/**
 * <p>
 * The HierarchicalDependencyID is used to manage the id's for the dependencies.
 * The string representation is of the form
 * </p>
 * 
 * #/T.#/T 
 * 
 * <p>where # is the ide number and
 * T is the total number of dependencies needed to make up the parent.  The symbol
 *  / is used to separate the numbers.
 *  </p>
 *  <p>
 * The HierarchicalSegmentID has the same nomenclature, and it is the first 
 * Hierarchical id in the string representation.  the Hierarchical Segment is 
 * separated from the Hierarchical dependency using a colon (:)
 * </p>
 * 
 * <p>A sample id would the be:</p> 
 * 
 * 0/4.3/4:1/-1.2/2
 * 
 * <p>So we have a segment grain size whose id is 1 and it part of a four part segment that 
 * can be put together to form segment 0 of a four part segment.  Note, the user would 
 * decide what the data is and how it is split and put back together, which makes it useful
 * for both skeletons and patterns.
 * </p>
 * <p>
 * Now the dependency is id 2, to get it parent back together , parent 1, it need one other piece
 * The parent is id 1 and 1 is a root dependency. -1 is used to denote a root dependency.
 * </p>
 * <p>
 * A root segment can be implied since it would always be 0/-1. 
 * However, a root dependency is always needed because a segment may create multiple root dependencies
 * and each of them needs a unique identifier, even though they are whole.  Additionally the 
 * Hierarchical dependency id contains a segment id to prevent conflicts with dependencies from other
 * segments.
 * </p>
 * @author jfvillal
 *
 */

public class HierarchicalDependencyID implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String DIV = "/";
	public static final String COL = ":";
	public static final String DOT_REX = "\\.";
	public static final String DOT_PRT = ".";
	/**
	 * 
	 * ID for this dependency
	 *
	 */
	private int Id; 
	/**
	 * 
	 * Total number of these dependencies needed to make up the parent dependency
	 * 
	 */
	private int Total;
	/**
	 *
	 * The Data Segment that is emitting this dependency.
	 *
	 */
	private HierarchicalSegmentID SegId;
	/**
	 * 
	 * Child Dependencies if any.
	 * 
	 */
	private HierarchicalDependencyID Parent;
	/**
	 * 
	 * Used to manage the level of this dependency
	 * 
	 */
	
	/**
	 * Create a new dependency id where this dependency will be the root dependency.
	 */
	public HierarchicalDependencyID( HierarchicalSegmentID seg_id, int id, int total){
		this( seg_id, id, total,null);
	}
	/**
	 * Creates a new dependency id with segment seg_id, id id.  The total number of siblings 
	 * is total, this constructur also sets the parent to be parent.
	 * 
	 * @param seg_id the hierarchical segment id for this dependency 
	 * @param id id for this dependency
	 * @param total total number of siblings for this dependency.
	 * @param parent Parent Dependency ID
	 */
	public HierarchicalDependencyID( HierarchicalSegmentID seg_id, int id, int total, HierarchicalDependencyID parent){
		Id =id;
		Total = total;
		SegId = seg_id;
		Parent = parent;
	}
	/**
	 * Returns the level of this dependency.  0 if this is the parent.
	 *  
	 *
	 * @return
	 */
	public int getLevel(){
		if( Parent == null){
			return 0;
		}else{
			return Parent.getLevel() + 1;
		}
	}
	/**
	 * Returns the root dependency id for this dependency id.
	 * @return
	 */
	public HierarchicalDependencyID getRoot(){
		HierarchicalDependencyID ans = this;
		while( ans.getParent() != null){
			ans = ans.getParent();
		}
		return ans;
	}
	/**
	 * Returns parent ID with the specified level. The child nodes are lost in the process
	 * for the returned iteration.
	 * @param level
	 * @return
	 */
	public HierarchicalDependencyID getLevel( int level){
		HierarchicalDependencyID ans = this;
		int my_level = ans.getLevel();
		if( level > my_level || level < 0){
			return null;
		}
		if( my_level == level){
			return ans;
		}else{
			return Parent.getLevel( level);
		}
	}
	/**
	 * returns the dept id in string form without attaching the segment id
	 * @return
	 */
	public String getDeptIdString(){
		if( Parent != null){
			return ""+Parent.getDeptIdString()+ DOT_PRT + Id + DIV+Total; // parent_id/parent_total.id/total
		}else{
			return "" + Id + DIV+Total; // id/total 
		}
	}
	/**
	 * return the full-form dependency id of the form
	 * 
	 * segid:deptid
	 * 
	 * 
	 */
	@Override
	public String toString(){
		return SegId.toString() + COL + this.getDeptIdString();
	}
	
	/**
	 * Builds a HierarchicalDependencyID from a String as long as the string complies
	 * with the syntactical requirements.
	 * @param str
	 * @return
	 * @throws InvalidHierarchicalDependencyIDException 
	 */
	public static HierarchicalDependencyID fromString( String str ) throws InvalidHierarchicalDependencyIDException{
		if( !str.contains(COL) ){
			throw new InvalidHierarchicalDependencyIDException();
		}
		String[] t = str.split(COL);
		HierarchicalSegmentID seg_id = HierarchicalSegmentID.fromString(t[0]);
		String[] dep_id_list = t[1].split(DOT_REX);
		HierarchicalDependencyID curr_child = null;
		HierarchicalDependencyID return_this_child = null;
		for( int i = dep_id_list.length -1; i >= 0; i--){
			String[] attributes = dep_id_list[i].split(DIV);
			int id = Integer.parseInt(attributes[0]);
			int total = Integer.parseInt(attributes[1]);
			if( curr_child == null){
				curr_child = new HierarchicalDependencyID( seg_id, id, total);
				return_this_child = curr_child;
			}else{
				HierarchicalDependencyID h_d_id = new HierarchicalDependencyID( seg_id, id, total);
				curr_child.setParent(h_d_id);
				curr_child = h_d_id;
			}
		}
		return return_this_child;
	}
	
	/**
	 * Returns this dependency id's id.
	 * @return
	 */
	public int getId() {
		return Id;
	}

	/**
	 * Set the id for this dependency id.
	 * @param id
	 */
	public void setId(int id) {
		Id = id;
	}

	/**
	 * Returns the total number of siblings needed to make up the parent 
	 * dependency id
	 * @return
	 */
	public int getTotal() {
		return Total;
	}

	/**
	 * Sets the total number of sibligns needed to make up the parent
	 * dependency id
	 * @param total
	 */
	public void setTotal(int total) {
		Total = total;
	}
	/**
	 * Returns the Segment ID, which is an Object of HierarchicalSegmentID
	 * @return
	 */
	public HierarchicalSegmentID getSid() {
		return SegId;
	}

	/**
	 * Sets the HierarchicalSegment ID for this node.
	 * @param sid
	 */
	public void setSid(HierarchicalSegmentID sid) {
		SegId = sid;
	}

	/**
	 * Returns the parent HierarchicalDependencyID for this dependency id.
	 * @return
	 */
	public HierarchicalDependencyID getParent() {
		return Parent;
	}

	/**
	 * Sets the parent hierarchical id for this dependency id.
	 * @param parent
	 */
	public void setParent(HierarchicalDependencyID parent) {
		Parent = parent;
	}
	/**
	 * <p>This method return < 0 if the Hierarchical id that is compared to is a parent level.</p>
	 * <p>Example:  0/1.1/2.1/2.compareTo(0/1) </p>
	 * <p>it return 1 if the other object is equal to this one</p>
	 * <p>Example: 0/1.compareTo(0/1) </p>
	 * <p>it retuns > 1 if this is</p>
	 * <p>Example: 0/1.comapreTo(0/1.1/2.1/2</p>
	 * <p>The numbers higher than 1 or lower than -1 denotes the levels of hierarchy that remain after the last parent that was equal.
	 * The number -1 is not used </p> 
	 */
	public int hierarchicalCompareTo(Object arg0) {
		HierarchicalDependencyID other = (HierarchicalDependencyID) arg0;
		if( other.SegId.HierarchicalcompareTo( this.SegId ) != 1){
			return 0;// not equal
		}
		int ans = -1; //undefined
		
		int level_diff = other.getLevel() - this.getLevel();
		HierarchicalDependencyID mine_trim = this;
		HierarchicalDependencyID other_trim = other;
		if( level_diff > 0){
			//shed some of other levels
			for( int i = 0; i < level_diff; i++){
				other_trim = other_trim.getParent();
			}
		}else if(level_diff < 0){
			//shed some of my level
			for( int i = 0; i < Math.abs(level_diff); i++){
				mine_trim = mine_trim.getParent();
			}
		}
		//at this point both trims are at the same leve, now lets compare.
		//for( int i = 0; i < mine_trim.getLevel(); i++){
		while( mine_trim != null && other_trim != null){
			if(    mine_trim.getId() == other_trim.getId() 
				&& mine_trim.getTotal() == other_trim.getTotal()
			){
				mine_trim = mine_trim.getParent();
				other_trim = other_trim.getParent();
			}else{
				return 0; //trunk not shared
			}
		}
		//at this point we have verified the id's are the same on their trunk.
		//so now will add one if leve_diff is positive or substract one if negative
		//to comply with the rules for the expected answer.
		return level_diff < 0 ? level_diff - 1 : level_diff + 1;
	}


	
}
