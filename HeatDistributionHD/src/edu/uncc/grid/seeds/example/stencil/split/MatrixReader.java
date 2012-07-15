package edu.uncc.grid.seeds.example.stencil.split;


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
/**
 * This utility class load the matrix from a comma-separated file, and it can 
 * save the 2D matrix to a color coded image.
 * 
 * Although necessary for the exercise, the student can safely ignore the code
 * in this class
 *
 */
public class MatrixReader {

	/**
	 * @param args
	 */
	public static final String DIV = ",";
	
	public static void main(String[] args) {
		
		int w = 2;
		int h = 4;
		
		double [][] matrix = getMainMatrix("./data/test.csv");
		
		//saveImage( matrix );
		printMatrix( matrix);
		System.out.println();
		
		Map<Integer, double [][]> matrices = new HashMap< Integer, double[][]>();
		for( int i = 0; i < w * h; i++){
			System.out.println("Segment: " + i );
			double [][] m = getSubMatrix( matrix, i, w, h); 
			matrices.put(i, m );
			printMatrix( m );
			System.out.println();
		}
		
		double [][] check_matrix = new double[matrix.length][matrix[0].length];
		for( int i = 0; i < w * h; i++){
			double [][] m = matrices.get(i);
			setSubMatrix( check_matrix, m, i, w, h);
			System.out.println("Check matrix integrators");
			printMatrix( check_matrix );
		}
		
		saveImage( check_matrix);
		
	}
	public static void saveImage(double[][] matrix){
		BufferedImage img = new BufferedImage(matrix.length, matrix[0].length, BufferedImage.TYPE_INT_ARGB);
		
		
		
		for( int h = 0; h < matrix.length; h++){
			for( int w = 0; w < matrix[0].length; w++){
				if( ( .76f - (float)(matrix[w][h] / 800) ) < 0.0f ){
					img.setRGB(w, h, Color.HSBtoRGB( 0.0f , 1.0f, 1.0f));
				}else{
				img.setRGB(w, h, Color.HSBtoRGB( .76f - (float)(matrix[w][h] / 800 ) , 1.0f, 1.0f));
				}
			}
		}
		
		File outputfile = new File("saved.png");
		try {
			ImageIO.write(img, "png", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	public static void printMatrix(double[][] matrix ){
		for( int h = 0; h < matrix[0].length; h++){
			for( int w = 0; w < matrix.length; w++){
				System.out.print( matrix[w][h] + ",");
			}
			System.out.println("");
		}
	}
	/**
	 * it will modify MainMatrix with the new values from m matrix.
	 * 
	 * @param MainMatrix the main matrix
	 * @param m the submatrix with modifications
	 * @param segment
	 * @param cell_count
	 */
	public static void  setSubMatrix( double[][]MainMatrix, double[][] m, int segment, int cell_width_count, int cell_height_count){
		
		int block_n_w = segment % cell_width_count;  //width index
		int block_n_h = segment / cell_width_count ; //heigh index
		
		int cell_w = MainMatrix.length / cell_width_count; //cells per block
		int cell_h = MainMatrix[0].length / cell_height_count; //cells per block
	
		//double[][] m = new double[cell_w][cell_h];
		
		int seg_c = 0;
		for( int c = block_n_w * cell_w; c < (block_n_w +1) * cell_w ; c++){
			int seg_h =0;
			for( int r = block_n_h * cell_h; r < (block_n_h + 1) * cell_h ; r++){
				 MainMatrix[c][r] = m[seg_c][seg_h++];
			}
			++seg_c;
		}
	}
	
	public static double[][] getSubMatrix( double[][]MainMatrix, int segment, int cell_width_count, int cell_height_count){
		
		int block_n_w = segment % cell_width_count;  //width index
		int block_n_h = segment / cell_width_count; //heigh index
		
		int cell_w = MainMatrix.length / cell_width_count; //cells per block
		int cell_h = MainMatrix[0].length / cell_height_count; //cells per block
	
		double[][] m = new double[cell_w][cell_h];
		
		int seg_c = 0;
		for( int c = block_n_w * cell_w; c < (block_n_w +1) * cell_w ; c++){
			int seg_h =0;
			for( int r = block_n_h * cell_h; r < (block_n_h + 1) * cell_h ; r++){
				m[seg_c][seg_h++] = MainMatrix[c][r];
			}
			++seg_c;
		}
		return m;
	}
	
	
	public static double[][] getMainMatrix(String path){
		double [][] matrix = null;
		FileReader input;
		try {
			input = new FileReader( path );
			BufferedReader input_stream = new BufferedReader( input );
			
			String dimensions = input_stream.readLine();
			String [] dim = dimensions.split(DIV);
			int width = Integer.parseInt( dim[0] );
			int height = Integer.parseInt( dim[1]);
			
			matrix = new double[width][height];
			for( int h = 0; h < height; h++){
				String row = input_stream.readLine();
				String cols[] = row.split(DIV);
				for( int w = 0; w < width; w++){	
					matrix [w][h] = Double.parseDouble(cols[w]);
				}
			}
			input_stream.close();
			input.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return matrix;
	}

}
