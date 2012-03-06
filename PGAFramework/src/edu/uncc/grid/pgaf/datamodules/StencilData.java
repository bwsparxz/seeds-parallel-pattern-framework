package edu.uncc.grid.pgaf.datamodules;

import java.io.Serializable;

/**
 * This interface is used to coordinate synchronization steps for a 2D stencil allong with the user.
 * @author jfvillal
 *
 */
public interface StencilData extends Serializable{
	public Serializable getTop();
	public void setTop( Serializable data );
	
	public Serializable getBottom();
	public void setBottom( Serializable data);
	
	public Serializable getLeft();
	public void setLeft(Serializable data);
	
	public Serializable getRight();
	public void setRight(Serializable data);
}
