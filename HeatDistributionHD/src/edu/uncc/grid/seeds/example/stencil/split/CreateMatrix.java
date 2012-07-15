package edu.uncc.grid.seeds.example.stencil.split;

import java.io.FileWriter;
import java.io.IOException;

public class CreateMatrix {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String COMMA = ",";
			FileWriter writer = new FileWriter(args[0]);
			int size = Integer.parseInt(args[1]);
			writer.write("" + size + COMMA + size + "\n");
			for( int h = 0; h < size ; h++){
				for( int w = 0; w < size ; w++){
					if( h == 0){
						writer.write( 400 + COMMA);
					}else if( h == size - 1){
						writer.write( 800 + COMMA);
					}else if( w == 0){
						writer.write( 200 + COMMA);
					}else if( w == size -1){
						writer.write( 50 + COMMA);
					}else{
						writer.write( 1 + COMMA);
					}
				}
				writer.write("\n");
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
