package edu.uncc.grid.pgaf.operators;

import edu.uncc.grid.pgaf.Pattern;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;

public abstract class OperatorModule extends BasicLayerInterface{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public abstract void setFirstOperand( Pattern set);
	public abstract void setSecondOperand(Pattern set);
	
	public abstract Pattern getFirstOperand();
	public abstract Pattern getSecondOperand();
	
	public abstract int getFirstOperandCoeficient();
	public abstract int getSecondOperandCoeficient();

	/**
	 * This function allows me to create at least one more pipe_id using a parent id as its
	 * seed.  Originally, I can have one parent id, I can get a second id using the 
	 * byte array from the parent id.  After this, all the other derived ids will be the same.
	 * This function extends the list by one. 
	 * 
	 * There should be a function that allows for multiple ids to be derived from the orignial
	 * source.  but I could not come up with the algorithm right away.
	 * 
	 * @param b
	 * @param c
	 * @return
	 */
	public static byte[] sumByteArrays( byte [] b, byte [] c ){
		byte [] a = new byte [b.length];
		for(int i=0 ; i < b.length; i++ ){
			a[i] = (byte)( (int)b[i] + (int)c[i]) ;
		}
		return a;
	}
	
}
