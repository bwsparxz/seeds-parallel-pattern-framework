package edu.uncc.grid.pgaf;

import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.operators.ModuleAdder;

public class AdderOperator implements Pattern{
	ModuleAdder Adder;
	
	public AdderOperator( ModuleAdder adder){
		Adder = adder;
	}

	@Override
	public Anchor getPatternAnchor() {
		return Adder.getFirstOperand().getPatternAnchor();
	}

	@Override
	public String[] getPatternArguments() {
		return Adder.getInitializingArguments();
	}

	@Override
	public String getPatternArgumentsinString() {
		if( Adder.getInitializingArguments() == null){
			return null;
		}
		String str = "";
		for( int i =0; i < Adder.getInitializingArguments().length; i++){
			str += Adder.getInitializingArguments()[i] + divisor;
		}
		return str;
	}

	@Override
	public BasicLayerInterface getPatternModule() {
		return Adder;
	}
}
