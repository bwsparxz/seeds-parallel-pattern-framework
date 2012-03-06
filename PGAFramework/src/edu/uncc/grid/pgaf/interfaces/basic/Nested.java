package edu.uncc.grid.pgaf.interfaces.basic;

import edu.uncc.grid.pgaf.deployment.Deployer;

public interface Nested {
	/**
	 * This function is called by the framework to let the user set up 
	 * patterns that should be active throughout the patterns livecycle.
	 * @param d
	 */
	public void startPatterns( Deployer d);
}
