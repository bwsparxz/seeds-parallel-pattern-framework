package edu.uncc.grid.pgaf.datamodules;

import java.io.Serializable;

import edu.uncc.grid.pgaf.p2p.Types;

public class DataInstructionContainer implements Data{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2102833579568984034L;
	
	Types.DataInstruction instruction;
	int Segment;
	byte Control;
	public DataInstructionContainer(){
		Control = Types.DataControl.INSTRUCTION_DEFAULT;
	}
	
	public Types.DataInstruction getInstruction() {
		return instruction;
	}

	public void setInstruction(Types.DataInstruction instruction) {
		this.instruction = instruction;
	}

	@Override
	public byte getControl() {
		return Control;
	}
	public void setControl(byte set){
		Control = set;
	}
	@Override
	public int getSegment() {
		return 0;
	}

	@Override
	public void setSegment(int segment) {
		
	}
	
	
}
