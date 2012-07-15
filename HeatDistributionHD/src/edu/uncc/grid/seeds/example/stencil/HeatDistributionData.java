package edu.uncc.grid.seeds.example.stencil;

import java.io.Serializable;

import edu.uncc.grid.pgaf.datamodules.StencilData;
import edu.uncc.grid.seeds.dataflow.pattern.stencil.Stencil;
/**
 * This class manages the heat distribution data.  it has the main
 * core matrix data. And it also manages the ghost zones from the neigbors
 * 
 */
public class HeatDistributionData implements StencilData {
	private static final long serialVersionUID = 1L;
	public double [][] matrix;
	public int Width;
	public int Height;
	public double[][] Sides;
	//public static final int CellCount = 4;
	//public static final int WidthCellCount = 1;
	//public static final int HeightCellCount = 1;
	public int Loop; 
	public HeatDistributionData( double [][]m ){
		Width = m.length;
		Height = m[0].length;
		matrix = m;
		Sides = new double[4][];
	}
	/**
	 * @returns the bottom part of the matrix
	 */
	@Override
	public Serializable getBottom() {
		return SyncData.getBorder(matrix, Width, Height, Stencil.BOTTOM);
	}
	/**
	 * @returns the left part of the matrix
	 */
	@Override
	public Serializable getLeft() {
		return SyncData.getBorder( matrix, Width, Height, Stencil.LEFT);
	}
	/**
	 * @returns the right part of the matrix
	 */
	@Override
	public Serializable getRight() {
		return SyncData.getBorder(matrix, Width, Height, Stencil.RIGHT);
	}
	/**
	 * @returns the top part of the matrix
	 */
	@Override
	public Serializable getTop() {
		return SyncData.getBorder(matrix, Width, Height, Stencil.TOP);
	}
	/**
	 * sets the bottom part of the matrix
	 */
	@Override
	public void setBottom(Serializable data) {
		this.Sides[Stencil.BOTTOM] = (double[]) data;
	}
	/**
	 * sets the left part of the matrix
	 */
	@Override
	public void setLeft(Serializable data) {
		this.Sides[Stencil.LEFT] = (double[])data;
	}
	/**
	 * sets the right part of the matrix
	 */
	@Override
	public void setRight(Serializable data) {
		this.Sides[Stencil.RIGHT] = (double[])data;
	}
	/**
	 * sets the top part of the matrix
	 */
	@Override
	public void setTop(Serializable data) {
		this.Sides[Stencil.TOP] = (double[])data;
	}
	
}
