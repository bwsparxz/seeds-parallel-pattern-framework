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
package edu.uncc.grid.pgaf.interfaces.advanced;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.uncc.grid.pgaf.interfaces.AdvertCatcher;
import edu.uncc.grid.pgaf.p2p.Node;
import java.net.Socket;

import net.jxta.pipe.PipeID;

/**
 * The UnorderedTemaplate is used by the advanced user.  the template does not provide mpi-like
 * communication since all the nodes are un-numbered.  The advanced user has to organized them 
 * using sockets.  The MultiModePipe classes provide three types of communicatin, but simple
 * sockets or jxta sockets can be used as well.
 * 
 * @author jfvillal
 *
 */
public abstract class UnorderedTemplate extends Template{
	
	public UnorderedTemplate(Node n) {
		super(n);
	}
	/**
	 * This method is run on the server side of the application.  This includes the Diffuse and Gather methods.
	 * By default, all pattern have an input, a compute section, and an output.  This method should implement
	 * the input and output.  Note that you are responsible to handle deadlocks at this level, you may need 
	 * to handle the input and output processes on separate threads depending on the natute of the pattern.
	 * @param pattern_id  specifies the unique patter for the pattern.  This allows multiple pattern to be executed by the framework.
	 */
	public abstract void ServerSide(PipeID pattern_id);
	/**
	 * this is a generic leaf worker doing some work
	 * This is the function that is executed on remote systems
	 * @param pattern_id  Each pattern has its own unique id to allow multiple patterns to run at the same time, even instances of the same
	 *   patterns without having the communication lines conflicting.
	 * @return returns true if the pattern is done, false otherwise
	 */
	public abstract boolean ClientSide(PipeID pattern_id);

	
	
}
