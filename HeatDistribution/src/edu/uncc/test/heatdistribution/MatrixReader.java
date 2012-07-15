package edu.uncc.test.heatdistribution;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

public class MatrixReader {

	/**
	 * @param args
	 */
	public static final String DIV = ",";
	
	public static void main(String[] args) {
		
		int div = 9;
		
		double [][] matrix = getMainMatrix();
		//saveImage( matrix );
		printMatrix( matrix);
		System.out.println();
		
		Map<Integer, double [][]> matrices = new HashMap< Integer, double[][]>();
		for( int i = 0; i < div ; i++){
			double [][] m = getSubMatrix( matrix, i, div); 
			matrices.put(i, m );
			printMatrix( m );
			System.out.println();
		}
		
		double [][] check_matrix = new double[matrix.length][matrix[0].length];
		for( int i = 0; i < div; i++){
			double [][] m = matrices.get(i);
			setSubMatrix( check_matrix, m, i, div);
			System.out.println("Check matrix integrators");
			printMatrix( check_matrix );
		}
		
		saveImage( check_matrix);
		
	}
	public static void saveImage(double[][] matrix){
		BufferedImage img = new BufferedImage(matrix.length, matrix[0].length, BufferedImage.TYPE_INT_ARGB);
		
		
		
		for( int i = 0; i < matrix.length; i++){
			for( int j = 0; j < matrix[0].length; j++){
				if( ( .76f - (float)(matrix[j][i] / 800) ) < 0.0f ){
					img.setRGB(i, j, Color.HSBtoRGB( 0.0f , 1.0f, 1.0f));
				}else{
				img.setRGB(i, j, Color.HSBtoRGB( .76f - (float)(matrix[j][i] / 800 ) , 1.0f, 1.0f));
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
		for( int w = 0; w < matrix.length; w++){
			for( int h = 0; h < matrix[0].length; h++){
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
	public static void  setSubMatrix( double[][]MainMatrix, double[][] m, int segment, int cell_count){
		int block_w = (int) Math.sqrt((double) cell_count);
		int block_h = block_w;
		
		int block_n_w = segment % block_w;  //width index
		int block_n_h = segment / block_h ; //heigh index
		
		int cell_w = MainMatrix.length / block_w; //cells per block
		int cell_h = MainMatrix[0].length / block_h; //cells per block
	
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
	
	public static double[][] getSubMatrix( double[][]MainMatrix, int segment, int cell_count){
		int block_w = (int) Math.sqrt((double) cell_count);
		int block_h = block_w;
		
		int block_n_w = segment % block_w;  //width index
		int block_n_h = segment / block_h ; //heigh index
		
		int cell_w = MainMatrix.length / block_w; //cells per block
		int cell_h = MainMatrix[0].length / block_h; //cells per block
	
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
	
	
	public static double[][] getMainMatrix(){
		double [][] matrix = null;
		try {
			InputStream stream = MatrixReader.class.getResourceAsStream("sample.csv");
			DataInputStream input = new DataInputStream(stream);
			
			BufferedReader input_stream = new BufferedReader( new InputStreamReader(input) );
			
			String dimensions = input_stream.readLine();
			String [] dim = dimensions.split(DIV);
			int width = Integer.parseInt( dim[0] );
			int height = Integer.parseInt( dim[1]);
			
			matrix = new double[width][height];
			for( int w = 0; w < width; w++){
				String row = input_stream.readLine();
				String cols[] = row.split(DIV);
				for( int h = 0; h < height; h++){	
					matrix [w][h] = Double.parseDouble(cols[h]);
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
