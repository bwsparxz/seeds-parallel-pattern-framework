package edu.uncc.grid.test.operator.interfaces;

import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;

public class TestPlusOperator {
	BasicLayerInterface APattern;
	BasicLayerInterface BPattern;
	public void PatternAddition( BasicLayerInterface a, BasicLayerInterface b) {
		APattern = a;
		BPattern = b;
	}
	/**
	 * The function return a text version of the expression.  
	 * Using preffix notation because I think it could be easier to parse.
	 * @return
	 */
	public String getPatternExpression(){
		return "plus," + APattern.getClass().getName() + "," + BPattern.getClass().getName();
	}
}
