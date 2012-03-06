package edu.uncc.grid.test.operator.interfaces;

/**
 * this is a test Interface to replace the current Template Interface.  It separates Source and Sink
 * 
 * The interface introduces the separation of the compute node into three phases.  a connection stablishing phase,
 * a connection disengaging phase, and a compute phase.  This is done to allow the framework to implement the 
 * operator concep.
 * 
 * if the framework receives an opearator, and x coeficient for pattern a and y coeficient for pattern b, the 
 * framework will allow the first pattern to run a cycles on the main loop for a pattern, and b cycles on the 
 * main loop for b pattern.
 * 
 * @author jfvillal
 *
 */
public interface TestNewAdvancedLayerTemplateInterface {
	/**
	 * Sink should be used to manage the DiffuseData() or the source for the pattern
	 */
	void Sink();
	/**
	 * source should be used to manage GatherData() or the sink for the pattern
	 */
	void Source();
	/**
	 *
	 * The computation can be divided into three parts. First the client connection to the 
	 * Source and the Sink and connecting to the neighbors if any.
	 *
	 */
	void ComputeBoxStartConnections();
	/**
	 * The computation can be divided into three parts. 
	 * thirs t the client should shutdown the connection and return 
	 * control to the framework
	 */
	void ComputeBoxFinilizePattern();
	/**
	 * The compute box runs the main loop for the pattern.  In order for us to introduce the operator 
	 * approach, this template should perform max_i_allowed cycles at a time.  the framework helps 
	 * the pattern know where it is by giving the loop count.  However, the object for the pattern
	 * should be able to keep track.
	 * @param i
	 */
	void ComputBox(int i, int max_i_allowed);
}
