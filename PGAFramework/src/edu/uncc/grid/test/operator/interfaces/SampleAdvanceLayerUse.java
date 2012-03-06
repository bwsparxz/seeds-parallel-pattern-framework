package edu.uncc.grid.test.operator.interfaces;

import java.util.logging.Level;

import edu.uncc.grid.pgaf.interfaces.advanced.Template;
import edu.uncc.grid.pgaf.p2p.LeafWorker;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.Worker;

/**
 * This class is created to test the implementation for the plus and vdproduc operators.  the issue at hand is 
 * How do we add to patterns on the framework.  and how do we implement the vector direct product on the framework.
 * 
 * The approach that is tested in this package edu.uncc.grid.test.operator 
 * is to redesign the advanced layer so that it consist of an init part where the communication lines are stablished, 
 * a compute part, and a finalization part.  
 * The compute part, instead of managing the main loop itself, the advanced layer would need a "crancking" Class that 
 * will run the main loop for it.  This is so that if anoperator is used, we already have the point of manipulation.
 * 
 * For this to work, it has to be done for all classes.  So at the implementation level there would be a innert operator.
 * 
 * Another option could be with AOP, but we'll try to use AOP for the things that definetly cannot be done using OOP
 * 
 * This class copies a lot from the UserThread class in package edu.uncc.grid.pgaf.p2p.  Most of the detail to run it are 
 * missing.  This class is not intended to run, it is intended to should how the problem can be solved. 
 * 
 * @author jfvillal
 *
 */
public class SampleAdvanceLayerUse extends Thread {
	Worker LWorker;
	//Template AdvancedTemplate;
	TestNewAdvancedLayerTemplateInterface AdvTemplate;
	
	public SampleAdvanceLayerUse( Worker w, TestNewAdvancedLayerTemplateInterface adv_temp){
		LWorker = w;
		AdvTemplate = adv_temp;
	}
	@Override
	public void run() {
		try{

			Node.getLog().log(Level.FINE, "LEAF Giving control to Template ");
			//AdvancedTemplate.ClientSide();
			
			
			
		}catch( Exception e){
			Node.getLog().log(Level.SEVERE, this.getId() + "while working on template  ");
			//RemoteLogger.printToObserver(  "while working on template  " );
		}
	
	}
}
