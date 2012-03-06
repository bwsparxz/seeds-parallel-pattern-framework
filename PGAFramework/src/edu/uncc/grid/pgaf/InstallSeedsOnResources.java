package edu.uncc.grid.pgaf;

import java.io.IOException;

import edu.uncc.grid.pgaf.deployment.Deployer;

/**
 * This command distributes the shuttle folder to all the servers in Available Servers file.
 * The Seeds framework will not transport the shuttle folder during startup to save time.
 * Instead, the command will do the task separetely.
 * @author jfvillal
 *
 */
public class InstallSeedsOnResources {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String seeds_folder = null;
			String module_jar = null;
			try{
			for( int i = 0; i < args.length ; i ++){
				if( args[i].equals("-seeds_folder")){
					seeds_folder = args[++i];
				}else if(args[i].equals("-mod_jar")){
					module_jar = args[++i];
				}else if( args[i].equals("-help")){
					printHelp();
					System.exit(0);
				}
			}
			}catch( IndexOutOfBoundsException e){
				System.err.println("Bad argument");
				printHelp();
				System.exit(1);
			}
			if( seeds_folder == null ){
				System.err.println("Seeds folder path not specified");
				printHelp();
				System.exit(1);
			}
			System.out.println( "seeds folder: " + seeds_folder );
			System.out.println( "mod jar: " + module_jar );
			Deployer SeedsDeployer;
		
			SeedsDeployer = new Deployer( seeds_folder );
			SeedsDeployer.InstallSeedsonResources();
			System.out.println("Done");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public static void printHelp(){
		System.out.println(
				"Usage:  command [argument] [value] ...\n"
				+ "-seeds_folder sets the path to the seeds folder.  the folder will be sent to each Grid resource (required).\n"
				+ "-mod_jar sets the path to the module jar if the module jar is not with the seeds libraries (optional).\n"
				+ "-help prints this help information."
				
		);
	}

}
