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

import edu.uncc.grid.pgaf.datamodules.DataComm;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.p2p.Communicator;
//import edu.uncc.grid.pgaf.templates.GenericParallelTemplate;
/**
 * Not yet implemented.
 * @author jfvillal
 *
 */
public abstract class SpacialSimulation extends BasicLayerInterface {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2568695970587805168L;

	/**
	 * Main computation method.  It takes a user inherited Data object as input and
	 * return a user inherited Data object as output
	 * @param input
	 * @return
	 */
	public abstract DataComm Compute(DataComm input, Communicator comm);
	/**
	 * This function is used to request the user for chunks of that will be send 
	 * through the network.  The user should inherit class Data into a custom class. 
	 *   For example, MyData.  The class MyData can have any type of object and variable.  
	 *   Be aware that the Data object needs to be serializable so that it can be 
	 *   Transfered over the network.
	 * @param segment
	 * @return
	 */
	public abstract DataComm DiffuseData(long segment);
	/**
	 * GatherData does the opposite of DiffuseData().  It will return Data objects with
	 * Chunks of the original data processed into a user defined output Data object.
	 * The user is responsible for taking the output and putting it back together into 
	 * the final answer.
	 * @param segment
	 * @param dat
	 */
	public abstract void GatherData(long segment, DataComm dat);
	/**
	 * This function should tell the framework what is the total number of pieces in which 
	 * the user decided to divide the Input data.
	 * @return
	 */
	public  abstract long getIdealProcessCount();	
	
	public String getHostingTemplate(){
		//TODO
		return null;// GenericParallelTemplate.class.getName();
	}
}
