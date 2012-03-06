package edu.uncc.grid.pgaf;

import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;

public interface Pattern {
	public final String divisor = "#@#";
	public String[] getPatternArguments();
	public BasicLayerInterface getPatternModule();
	public Anchor getPatternAnchor();
	public String getPatternArgumentsinString();
}
