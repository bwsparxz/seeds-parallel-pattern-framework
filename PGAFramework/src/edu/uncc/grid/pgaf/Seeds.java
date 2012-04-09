/*
 * Copyright 2009 Jeremy Villalobos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package edu.uncc.grid.pgaf;

import java.io.IOException;
import java.util.logging.Level;

import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.deployment.Deployer;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.p2p.Node;

public class Seeds {
	public static Deployer SeedsDeployer;
	static Thread LocalNodeThread;
	/**
	 * Starts the framework
	 * @param ShuttleFolderPath the location of the shutle folder
	 * @param skip_transfer if true, the framework will skip transfer of the shuttle folder.
	 * @param return_control_asap if true, Seeds will be running in the background, so the user can tap on parallel pattern when needed.
	 * @throws Exception 
	 */
	public static void start(String ShuttleFolderPath, boolean skip_transfer ) throws Exception{
		if( SeedsDeployer != null){
			throw new ExistingDeployerInstanceException();
		}
		SeedsDeployer = new Deployer( ShuttleFolderPath);
		SeedsDeployer.setJavaSocketUse(true); //JXTA socket are deprecated for this framework.  Only use Java Sockets.
		LocalNodeThread = SeedsDeployer.Spawn(skip_transfer);
	}
	/**
	 * Will start a pattern.
	 * @param module
	 * @param args
	 * @return
	 * @throws IOException 
	 */
	public static PipeID startPattern( Pattern pattern) throws IOException{
		// BasicLayerInterface module, String args[], Anchor anchor)
		pattern.getPatternModule().initializeModule(pattern.getPatternArguments());
		return SeedsDeployer.getLocalMachineNode().spawnPattern( pattern) ;
	}
	/**
	 * Will wait until the pattern has completed.
	 * @param id
	 * @throws InterruptedException 
	 */
	public static void waitOnPattern( PipeID id) throws InterruptedException{
		BasicLayerInterface basic = SeedsDeployer.getLocalMachineNode().SourceSinkAvailableOnThisNode.get(id);
		if( basic != null){
			Node.getLog().log(Level.INFO, " Waiting for Pattern Complete ");
			basic.watiOnPatternDone();
			Node.getLog().log(Level.INFO, " Pattern Completed ");
		}else{
			Node.getLog().log(Level.INFO, " Waiting for Pattern Complete Advertisement [NOT IMPLEMENTED JET]");
			//the source-sink has to send a patter_done_advertisement
			//feb 23 2010
			//TODO create pattern_done_advertisement.  every source_sink node has to send this after
			//the pattern is done (this is the task of the advanced user)
		}
	}
	
	/**
	 * Stops the framework.
	 */
	public static void stop( ){
		if( SeedsDeployer != null){
			SeedsDeployer.getLocalMachineNode().bringNetworkDown();
		}
		try {
			SeedsDeployer.getLocalMachineNode().setStopNetwork(true);
			LocalNodeThread.join(1000);
			System.out.println(" Done waiting for the localnodethread ");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	/**
	 * returns the deployer, advance feature.  Should not be neede by
	 * the user programmer.
	 * @return
	 */
	public static Deployer getDeployer(){
		return SeedsDeployer;
	}
	
}
