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
package edu.uncc.grid.pgaf.interfaces.basic;


import edu.uncc.grid.pgaf.datamodules.Data;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.templates.WorkpoolTemplate;

/**
 * This interface sets the function that will be used by the GPAFramework to load and
 * run multiple process on the Grid.  The user should consider each of the functions 
 * independent from each other.  There should not be a global variable that is set from 
 * Compute() that then is accessed from GatherData().  This should not be done because
 * the different functions may be called from different nodes on the network.  So the 
 * update of global information on one node is not propagated to the other nodes.
 * @author jfvillal
 *
 */

public abstract class Workpool extends BasicLayerInterface{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	/**
	 * Main computation method.  It takes a user inherited Data object as input and
	 * return a user inherited Data object as output
	 * @param input
	 * @return
	 */
	public abstract  Data Compute(Data input);
	/**
	 * This function is used to request the user for chunks of that will be send 
	 * through the network.  The user should inherit class Data into a custom class. 
	 *   For example, MyData.  The class MyData can have any type of object and variable.  
	 *   Be aware that the Data object needs to be serializable so that it can be 
	 *   Transfered over the network.
	 * @param segment
	 * @return
	 */
	public abstract Data DiffuseData(int segment);
	/**
	 * GatherData does the opposite of DiffuseData().  It will return Data objects with
	 * Chunks of the original data processed into a user defined output Data object.
	 * The user is responsible for taking the output and putting it back together into 
	 * the final answer.
	 * @param segment
	 * @param dat
	 */
	public abstract void GatherData(int segment, Data dat);
	/**
	 * This function should tell the framework what is the total number of pieces in which 
	 * the user decided to divide the Input data.
	 * @return
	 */
	public abstract int getDataCount();	
	
	public String getHostingTemplate(){
		return WorkpoolTemplate.class.getName();
	}
	
	@Override
	public boolean isOperator() {
		return false;
	}
	

}
