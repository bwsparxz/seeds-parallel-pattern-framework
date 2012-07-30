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
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.logging.Level;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import edu.uncc.grid.pgaf.advertisement.SpawnPatternAdvertisement;
import edu.uncc.grid.pgaf.communication.MultiModePipeMapper;
import edu.uncc.grid.pgaf.deployment.Deployer;
import edu.uncc.grid.pgaf.interfaces.advanced.BasicLayerInterface;
import edu.uncc.grid.pgaf.p2p.Node;
import edu.uncc.grid.pgaf.p2p.Types;
import edu.uncc.grid.pgaf.p2p.compute.DesktopWorker;
import edu.uncc.grid.pgaf.p2p.compute.PatternRepetitionException;
import edu.uncc.grid.seeds.comm.dependency.DependencyMapper;

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
	
	
	/*******Multi-core API calss ********/
	/**
	 * This spawns a pattern on the local machine.  This method does not start the JXTA network.  This method
	 * only runs the program on the local machine.  Because of this, the method does not need to be preceded by 
	 * start() method, and no connection needs to be closed with close() method.
	 * This code is a copy of {@link edu.uncc.grid.pgaf.p2p.Node#spawnPattern(Pattern)} 
	 * @param pattern
	 * @return returns the thread that is managing the source and sink.  use Thread.join() to wait for the pattern to finish.
	 * @throws IOException
	 * @throws PatternRepetitionException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public static Thread startPatternMulticore( Pattern pattern , int cores) throws IOException, IllegalArgumentException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, PatternRepetitionException{
		Node.registerAdvertisements();
		Node.NetworkType = Types.WanOrNat.MULTI_CORE;
		//Initialize the memory mapper
		MultiModePipeMapper.initMapper();
		//Initializes the Dataflow Hierarchical Dependency connection mapper.
		DependencyMapper.initDependencyMapper();
		//Initializes the pattern with the arguments provided by the user programmer.
		pattern.getPatternModule().initializeModule(pattern.getPatternArguments());
		SpawnPatternAdvertisement Advert = (SpawnPatternAdvertisement)
				AdvertisementFactory.newAdvertisement(SpawnPatternAdvertisement.getAdvertisementType());
		Advert.setArguments(pattern.getPatternArguments() != null? pattern.getPatternArguments() : new String[]{""});
		Advert.setGridName(Node.getGridName());
		PipeID pattern_id = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID);
		Advert.setPatternID(pattern_id);
		Advert.setPatternClassName( pattern.getPatternModule().getClass().getName() );
		if( pattern.getPatternAnchor() == null){
			Advert.setSourceAnchor(InetAddress.getLocalHost().getHostName()); //by default it is 'this' host
		}else{
			if( pattern.getPatternAnchor().getAnchorDFR() != Types.DataFlowRole.SINK_SOURCE){
				Advert.setSourceAnchor(InetAddress.getLocalHost().getHostName()); //by default it is 'this' host
			}else{
				Advert.setSourceAnchor(pattern.getPatternAnchor().getHostname());
			}
		}
		DesktopWorker worker = new DesktopWorker( cores );
		//add the module created by the user to the worker object.
		worker.SourceSinkAvailableOnThisNode.put(pattern_id, pattern.getPatternModule());
		return worker.checkAdvertisement( Advert );
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
