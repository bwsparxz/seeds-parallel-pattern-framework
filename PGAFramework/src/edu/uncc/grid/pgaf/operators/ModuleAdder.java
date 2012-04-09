package edu.uncc.grid.pgaf.operators;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;

import edu.uncc.grid.pgaf.Anchor;
import edu.uncc.grid.pgaf.BasicLayerInterfaceNotAnOperandException;
import edu.uncc.grid.pgaf.Operand;
import edu.uncc.grid.pgaf.Pattern;
import edu.uncc.grid.pgaf.communication.Communicator;
import edu.uncc.grid.pgaf.datamodules.DataMap;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.interfaces.advanced.OrderedTemplate;
import edu.uncc.grid.pgaf.interfaces.advanced.Template;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.compute.PatternLoader;

public class ModuleAdder extends OperatorModule {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Pattern FirstOperand;
	Pattern SecondOperand;
	int FirstOperandCoeficient;
	int SecondOperandCoeficient;
	
	PatternLoader FirstLoaderMod;
	PatternLoader SecondLoaderMod;
	/**
	 * For now only working on second argument.  it holds the 
	 * communicator to be used by the source sink if the 
	 * second argument has a source-sink stream as is the case
	 * with a pipeline.
	 */
	Communicator StreamSourceSinkComm;
	
	public ModuleAdder(){
		FirstOperand = null;
		SecondOperand = null;
	}
	/**
	 * The first pattern first will be called continustly for first_coeficient number of times.  Then
	 * the second patter "second" is called continuesly for "second-coeficient" number of times.  the 
	 * loop repeat again until either onde of the patterns return true in their computation methods.
	 * 
	 * @param first_coeficient
	 * @param first
	 * @param second_coeficient
	 * @param second
	 */
	public ModuleAdder( int first_coeficient, Pattern first, int second_coeficient, Pattern second){
		FirstOperand = first;
		SecondOperand = second;
		FirstOperandCoeficient = first_coeficient;
		SecondOperandCoeficient = second_coeficient;
	}
	
	public static final int FIRST_COEFICIENT = 0;
	public static final int FIRST_OPERAND = 1;
	public static final int FIRST_ANCHOR = 2;
	public static final int FIRST_ARGS = 3;
	
	public static final int SECOND_COEFICIENT = 4;
	public static final int SECOND_OPERAND = 5;
	public static final int SECOND_ANCHOR = 6;
	public static final int SECOND_ARGS = 7;
	public static final int INITIAL_ARGS_NUM = 8;
	public static final int USER_MOD_INIT_ARGS = 8;//after this, all the otehr arguments are feed to both of the patterns
	/**
	 * returns the arguments needed to initialize the operator.
	 * @return
	 */
	public String[] getInitializingArguments(){
		String[] args = new String[ 8 ];
		
		args[FIRST_COEFICIENT]  = "" + FirstOperandCoeficient;
		args[FIRST_OPERAND] = FirstOperand.getPatternModule().getClass().getName();
		args[FIRST_ANCHOR] = FirstOperand.getPatternAnchor().toString();
		args[FIRST_ARGS] = FirstOperand.getPatternArgumentsinString();
		
		args[SECOND_COEFICIENT] = "" + SecondOperandCoeficient;
		args[SECOND_OPERAND] = SecondOperand.getPatternModule().getClass().getName();
		args[SECOND_ANCHOR] = SecondOperand.getPatternAnchor().toString();
		args[SECOND_ARGS] = SecondOperand.getPatternArgumentsinString();
		
		return args;
	}
	
	
	@Override
	public String getHostingTemplate() {
		return PatternAdderTemplate.class.getName();
	}

	@Override
	public void initializeModule(String[] args) {
		try {
 			this.FirstOperandCoeficient = Integer.parseInt( args[ this.FIRST_COEFICIENT ]);
			this.SecondOperandCoeficient = Integer.parseInt(args[this.SECOND_COEFICIENT]);
			
			String first_operand = args[this.FIRST_OPERAND];
			String second_operand = args[this.SECOND_OPERAND];
			
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			
			Class first_operand_mod;
			BasicLayerInterface first_mod = null;
			if( this.FirstOperand == null){
				first_operand_mod = loader.loadClass(first_operand);
			
				first_mod = (BasicLayerInterface) first_operand_mod.newInstance();
				this.FirstOperand = new Operand( args[this.FIRST_ARGS] , Anchor.valueOf( args[this.FIRST_ANCHOR] ) , first_mod);
				first_mod.initializeModule( this.FirstOperand.getPatternArguments() );
			}
			
			{
				String first_otemplate_str = FirstOperand.getPatternModule().getHostingTemplate();
				Class otemplate_class = loader.loadClass( first_otemplate_str );
				Class[] node_arg = {Node.class};
				Constructor cons = otemplate_class.getConstructor( node_arg );
				
				OrderedTemplate advanced_template = (OrderedTemplate) cons.newInstance( this.getFramework() );
				advanced_template.setUserModule( FirstOperand.getPatternModule() );
				
				FirstLoaderMod = (PatternLoader) advanced_template.getLoaderModule().newInstance();
				FirstLoaderMod.setOTemplate( advanced_template ); //give o_template to loader module
				FirstLoaderMod.setFramework( this.Framework );
			
			}
			
			
			BasicLayerInterface second_mod =null;
			if( this.SecondOperand == null){
				Class second_operand_mod = loader.loadClass(second_operand);
				second_mod = (BasicLayerInterface) second_operand_mod.newInstance();
				this.SecondOperand = new Operand( args[this.SECOND_ARGS], Anchor.valueOf( args[this.SECOND_ANCHOR]), second_mod);
				second_mod.initializeModule( this.SecondOperand.getPatternArguments() );
			}
			{
				String second_otemplate_str = SecondOperand.getPatternModule().getHostingTemplate();
				Class otemplate_class = loader.loadClass( second_otemplate_str );
				Class[] node_arg = {Node.class};
				Constructor cons = otemplate_class.getConstructor( node_arg );
				OrderedTemplate advanced_template_two = (OrderedTemplate) cons.newInstance( this.getFramework() );
				advanced_template_two.setUserModule( SecondOperand.getPatternModule() );
				
				SecondLoaderMod = (PatternLoader) advanced_template_two.getLoaderModule().newInstance();
				SecondLoaderMod.setOTemplate( advanced_template_two ); //give o_template to loader module
				SecondLoaderMod.setFramework( this.Framework );
				
			}
			int remaining_arguments = args.length - this.USER_MOD_INIT_ARGS; 
			if( remaining_arguments > 0){
				String[] user_mod_args = new String[  remaining_arguments ];
				for( int i = remaining_arguments; i < args.length; i++){
					user_mod_args[i-remaining_arguments] = args[i];
				}
				
				first_mod.initializeModule(user_mod_args);
				second_mod.initializeModule(args);
			}
			
		} catch (ClassNotFoundException e) {
			Node.getLog().log(Level.SEVERE , Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (InstantiationException e) {
			Node.getLog().log(Level.SEVERE , Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Node.getLog().log(Level.SEVERE , Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			Node.getLog().log(Level.SEVERE , Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Node.getLog().log(Level.SEVERE , Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (SecurityException e) {
			Node.getLog().log(Level.SEVERE , Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			Node.getLog().log(Level.SEVERE , Node.getStringFromErrorStack(e));
			e.printStackTrace();
		} catch (BasicLayerInterfaceNotAnOperandException e) {
			Node.getLog().log(Level.SEVERE , "NOTE: Nesting operators is not supported jet.  I hope this application failed gracefully for you :-)" +Node.getStringFromErrorStack(e));
			e.printStackTrace();
		}
		
	}
	
	
	public PatternLoader getFirstLoaderMod(PipeID PatternID) {
		FirstLoaderMod.setPatternID(PatternID);
		return FirstLoaderMod;
	}
	public PatternLoader getSecondLoaderMod( PipeID PatternID){
		SecondLoaderMod.setPatternID(PatternID);
		return SecondLoaderMod;
	}
	@Override
	public int getFirstOperandCoeficient() {
		return FirstOperandCoeficient;
	}
	@Override
	public int getSecondOperandCoeficient() {
		return SecondOperandCoeficient;
	}
	@Override
	public Pattern getFirstOperand() {
		return this.FirstOperand;
	}
	@Override
	public Pattern getSecondOperand() {
		return this.SecondOperand;
	}
	@Override
	public void setFirstOperand(Pattern set) {
		this.FirstOperand = set;
	}
	@Override
	public void setSecondOperand(Pattern set) {
		this.SecondOperand = set;
	}
	@Override
	public boolean isOperator() {
		return true;
	}
}
