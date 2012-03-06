/* * Copyright (c) Jeremy Villalobos 2009
 *  
 * This file is part of PGAFramework.
 *
 *   PGAFramework is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  PGAFramework is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with PGAFramework.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Other libraries and code used.
 *  
 *  This framework also used code and libraries from the Java
 *  Cog Kit project.  http://www.cogkit.org
 *  
 *  This product includes software developed by Sun Microsystems, Inc. 
 *  for JXTA(TM) technology.
 *  
 *  Also, code from UPNPLib is used
 *  
 *  And some extremely modified code from Practical JXTA is used.  
 *  This product includes software developed by DawningStreams, Inc.    
 *  
 */
package edu.uncc.grid.pgaf.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.uncc.grid.pgaf.exceptions.DirectoryDoesNotExist;



public class TestListDirectory {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<String> list;
		try {
			list = getJarFromDir("/data/jfvillal/Documents/GridComputing/lab/gpaf");
			
			Iterator<String> it = list.iterator();
			System.out.println("The jars in the dire are:   ");
			while(it.hasNext()){
				System.out.println(it.next());
			}
		} catch (DirectoryDoesNotExist e) {
			e.printStackTrace();
		}
	}
	
	public static List<String> getJarFromDir(String directory_path)
		throws DirectoryDoesNotExist{
		List<String> list = new ArrayList<String>();
		File dir = new File(directory_path);
		String[] children = dir.list();
		if( children == null){
			System.err.println("Directory does not exist or is not a directory");
			throw new DirectoryDoesNotExist();
		}else{
			for(int i = 0; i < children.length; i++){
				System.out.println(children[i]);
				list.add(children[i]);
			}
		}
		return list;
	}

}
