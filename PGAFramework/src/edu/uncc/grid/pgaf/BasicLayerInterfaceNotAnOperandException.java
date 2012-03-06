package edu.uncc.grid.pgaf;
/**
 * <p>
 * The operators need to be treated differently when we are getting ready to deploy them.  For 
 * example, the operator will provide their own set of intitializing argument, which have the 
 * information about the modules they are going to be running in the remote nodes.  They also
 * have to decide the anhors.  And of course, they report their own module class name as 
 * the module that is going to be running.  
 * </p>
 * <p>
 * For those reasons, the AdderConteiner (and other OperatorContainers in the future) need to 
 * be used to handle the operator before loading them into the framework.  But the user
 * could accidentally give an operator module to the Operand Pattern, which would run 
 * incorrectly without given a fatal error.  So this exception will make it clearer that
 * the user made a mistake.
 * </p>
 * @author jfvillal
 *
 */
public class BasicLayerInterfaceNotAnOperandException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
