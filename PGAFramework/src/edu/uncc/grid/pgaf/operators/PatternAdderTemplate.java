package edu.uncc.grid.pgaf.operators;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.advanced.Template;
import edu.uncc.grid.pgaf.p2p.Node;

public class PatternAdderTemplate extends OrderedTemplate {
	OrderedTemplate FirstOperandAdvancedTemplate;
	OrderedTemplate SecondOperandAdvancedTemplate;
	public PatternAdderTemplate(Node n) {
		super(n);
		//some Template variable are null until the framework sets them
		//and that happens after the this object has been instantiated.
	}
	/**
	 * This is where we do the addition pattern operator's main purpose.
	 */
	@Override
	public boolean ComputeSide(Communicator comm) {
		//not used
		return false;
	}

	@Override
	public void Configure(DataMap<String, Serializable> configuration) {
		FirstOperandAdvancedTemplate.Configure(configuration);
		SecondOperandAdvancedTemplate.Configure(configuration);
	}

	@Override
	public boolean SourceSinkSide(Communicator comm) {
		// Not used for this pattern. (because the Stencil and 
		// the complete patterns use the loading pattern to 
		//distribute the initial data.
		
		//a future modification to run a pipeline with an synch pattern 
		//may require additional code in this method
		return true;
	}
	/**
	 * This method sets the first operands source and sink as the source and sink for 
	 * the addition operator.
	 */
	@Override
	public Class getLoaderModule() {
		return PatternAdderLoader.class;
	}

	@Override
	public String getSuportedInterface() {
		return ModuleAdder.class.getName();
	}
	@Override
	public void setUserModule(BasicLayerInterface userModule) {
		super.setUserModule(userModule);
		/**
		 * By putting the code here, we will get a non-null UserModule
		 */
		
		try {
			ModuleAdder a = (ModuleAdder) this.UserModule;
			
			BasicLayerInterface first_operand = a.getFirstOperand().getPatternModule();
			BasicLayerInterface second_operand = a.getSecondOperand().getPatternModule();
			
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			Class[] node_arg = {Node.class};
			
			Class first_ordered_template;
			
			first_ordered_template = loader.loadClass( first_operand.getHostingTemplate() );
			Constructor first_cons = first_ordered_template.getConstructor( node_arg );
			FirstOperandAdvancedTemplate = (OrderedTemplate) first_cons.newInstance(this.Network);
			FirstOperandAdvancedTemplate.setUserModule( first_operand );
			
			
			Class second_ordered_template;
			
			second_ordered_template = loader.loadClass( second_operand.getHostingTemplate() );
			Constructor second_cons = second_ordered_template.getConstructor( node_arg );
			this.SecondOperandAdvancedTemplate = (OrderedTemplate) second_cons.newInstance(this.Network);
			SecondOperandAdvancedTemplate.setUserModule( second_operand);
		
		} catch (ClassNotFoundException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		} catch (SecurityException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		} catch (InstantiationException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Node.getLog().log(Level.SEVERE, Node.getStringFromErrorStack(e) );
			e.printStackTrace();
		}
	}
	@Override
	public void FinalizeObject() {
		this.FirstOperandAdvancedTemplate.FinalizeObject();
		this.SecondOperandAdvancedTemplate.FinalizeObject();
	}
	
	

}
