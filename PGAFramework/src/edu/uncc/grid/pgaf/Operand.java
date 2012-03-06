package edu.uncc.grid.pgaf;

import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.p2p.Types;
/**
 * Since a pattern requires some argument along with it ( the anchor and the initialization arguments)
 * I have created this object which basically holds everything together.  This will be given to the 
 * pattern operators to simplify the interface for the pattern operator.
 * @author jfvillal
 *
 */
public class Operand implements Pattern{
	String[] PatternArguments;
	Anchor PatternAnchor;
	BasicLayerInterface PatternModule;
	
	public Operand(String[] patternArguments, Anchor patternAnchor,
			BasicLayerInterface patternModule) throws BasicLayerInterfaceNotAnOperandException {
		PatternArguments = patternArguments;
		PatternAnchor = patternAnchor;
		PatternModule = patternModule;
		if( patternModule.isOperator() ){
			throw new BasicLayerInterfaceNotAnOperandException();
		}
	}
	public Operand( String patternArguments, Anchor patternAnchor
			, BasicLayerInterface patternModule) throws BasicLayerInterfaceNotAnOperandException{
		this( 	patternArguments == null ? null : patternArguments.split(divisor)
				, patternAnchor, patternModule);
	}
	
	public String getPatternArgumentsinString(){
		if( PatternArguments == null){
			return null;
		}
		String str = "";
		for( int i =0; i < PatternArguments.length; i++){
			str += PatternArguments[i] + divisor;
		}
		return str;
	}
	public void setPatternArguments( String str){
		PatternArguments  = str.split(divisor);
	}
	
	public String[] getPatternArguments() {
		return PatternArguments;
	}
	public void setPatternArguments(String[] patternArguments) {
		PatternArguments = patternArguments;
	}
	public Anchor getPatternAnchor() {
		return PatternAnchor;
	}
	public void setPatternAnchor(Anchor patternAnchor) {
		PatternAnchor = patternAnchor;
	}
	public BasicLayerInterface getPatternModule() {
		return PatternModule;
	}
	public void setPatternModule(BasicLayerInterface patternModule) {
		PatternModule = patternModule;
	}

	
}
