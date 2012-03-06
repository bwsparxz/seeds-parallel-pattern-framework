package edu.uncc.grid.seeds.comm.dependency;

import java.io.Serializable;

public class HierarchicalSegmentID implements Comparable, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Main id for this segment
	 */
	private int Segment;
	/**
	 * the number of siblings needed to make up the parent segment
	 */
	private int Total;
	/**
	 * Pointer to the parent segment
	 */
	private HierarchicalSegmentID Parent;
	/**
	 * basic constructor.  It sets the parent as null.
	 * @param segment
	 * @param total
	 */
	public HierarchicalSegmentID( int segment, int total){
		this( segment,total, null);
	}
	public HierarchicalSegmentID( int segment, int total, HierarchicalSegmentID parent){
		Segment = segment;
		Total = total;
		Parent = parent;
	}
	@Override
	public String toString(){
		if( Parent != null){
			return ""+Parent.toString()+ HierarchicalDependencyID.DOT_PRT + Segment + HierarchicalDependencyID.DIV +Total;
		}else{
			return "" + Segment + HierarchicalDependencyID.DIV +Total;
		}
	}
	public static HierarchicalSegmentID fromString( String str){
		String[] seg_id_list = str.split(HierarchicalDependencyID.DOT_REX);
		HierarchicalSegmentID curr_parent = null;
		HierarchicalSegmentID final_id = null;
		for( int i = 0; i < seg_id_list.length; i++){
			String[] attr = seg_id_list[i].split( HierarchicalDependencyID.DIV );
			int id = Integer.parseInt(attr[0]);
			int total = Integer.parseInt(attr[1]);
			if( final_id == null){ 
				final_id = new HierarchicalSegmentID(id, total);
				curr_parent = final_id;
			}else{
				final_id = new HierarchicalSegmentID(id, total, curr_parent);
				curr_parent = final_id;
			}
		}
		return final_id;
	}
	
	public int getSegment() {
		return Segment;
	}
	public void setSegment(int segment) {
		Segment = segment;
	}
	public int getTotal() {
		return Total;
	}
	public void setTotal(int total) {
		Total = total;
	}
	public HierarchicalSegmentID getParent() {
		return Parent;
	}
	public void setParent(HierarchicalSegmentID parent) {
		Parent = parent;
	}
	public HierarchicalSegmentID getRoot(){
		if( Parent != null){
			return Parent.getParent();
		}else{
			return this;
		}
	}
	public int getLevel(){
		if( Parent == null){
			return 0;
		}else{
			return Parent.getLevel() + 1;
		}
	}
	/**
	 * this will stay a a normal straight comparation.  Later, I will decided if it neeeds scrutiny similar to
	 * {@link HierarchicalDependencyID}
	 */
	@Override
	public int compareTo(Object o) {
		//make sure they are siblings, else throw exception
		HierarchicalSegmentID s = (HierarchicalSegmentID) o;
		if( !this.isSibling(  s )  ){
			throw new NoComparableSegmentsException();
		}else{
			
			return this.getSegment() - s.getSegment();
		}
	}
	public int HierarchicalcompareTo(Object arg0) {
		HierarchicalSegmentID other = (HierarchicalSegmentID) arg0;
		int ans = -1; //undefined
		int level_diff = other.getLevel() - this.getLevel();
		HierarchicalSegmentID mine_trim = this;
		HierarchicalSegmentID other_trim = other;
		
		if( level_diff > 0 ){
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
		//for( int i = 0; i <= mine_trim.getLevel(); i++){
		while( mine_trim != null && other_trim != null){
			
			if(    mine_trim.getSegment() == other_trim.getSegment() 
				&& mine_trim.getTotal() == other_trim.getTotal()
			){
				mine_trim = mine_trim.getParent();
				other_trim = other_trim.getParent();
			}else{
				return 0;
			}
		}
		//at this point we have verified the id's are the same on their trunk.
		//so now will add one if leve_diff is positive or substract one if negative
		//to comply with the rules for the expected answer.
		return level_diff < 0 ? level_diff - 1 : level_diff + 1;
	}
	
	public boolean isSibling( HierarchicalSegmentID other){
		if( HierarchicalcompareTo(other) != 0 ){ //have to be in same trunk
			return false;
		}else{
			HierarchicalSegmentID mine_trim = this;
			HierarchicalSegmentID other_trim = other;
			
			if(    mine_trim.getSegment() == other_trim.getSegment() 
					&& mine_trim.getTotal() == other_trim.getTotal()
			){
				return false;  //if they are the same, then it is the same id
				//and not sibling id's
			}else{
				mine_trim = mine_trim.getParent();
				other_trim = other_trim.getParent();
			}
			//at this point, they may be siblings
			//but first, lets check their trunks are the same.
			if( mine_trim == null){
				return true; //no trunk to check, they are siblings
			}else{
				for( int i = 0; i < mine_trim.getLevel(); i++){
					if(    mine_trim.getSegment() == other_trim.getSegment() 
						&& mine_trim.getTotal() == other_trim.getTotal()
					){
						mine_trim = mine_trim.getParent();
						other_trim = other_trim.getParent();
					}else{
						return false;//trunk did not check out
							//they are not siblings
					}
				}
				return true;//trunk checked out, they are siblings
			}
		}
	}
	
}
