package edu.uncc.grid.pgaf.p2p;

import java.util.logging.Level;

public class IncrementalSleeper {

	long Sleep;
	long SleepCeiling;
	long Increment;
	
	public IncrementalSleeper(){
		Sleep = 1;
		SleepCeiling = 3000;
		Increment = 40;
	}
	/**
	 * This function was created because the P2P network should check the Advertisement very often at the 
	 * beginning while it sets up.  But, then, it should slow down to leave the resources to the 
	 * user's module
	 */
	public void sleep(){
		/*this is not the main thread, so it is good not to take too many resources*/
		try {
			long star = System.currentTimeMillis();
			Thread.sleep(Sleep);
			if( Sleep < SleepCeiling){
				Sleep += Increment;
			}
			//Node.getLog().log(Level.FINE, "slep for: " + (double)(System.currentTimeMillis() - star) / 1000.0 );
			
		} catch (InterruptedException e) {
			Node.getLog().log(Level.FINEST, Node.getStringFromErrorStack(e) );
		}
	}
	
}
