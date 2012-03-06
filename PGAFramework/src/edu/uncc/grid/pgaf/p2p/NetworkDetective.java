/* * Copyright (c) Jeremy Villalobos 2009
 *  
 *
 */
package edu.uncc.grid.pgaf.p2p;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.logging.Level;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

/**
 * 
 * The class uses some neat tricks to get network characteristics such as if the network is 
 * WAN, NAT (UPNP or NON_UPNP).  It also uses www.yougetsignal.com to figure out the WAN address for a 
 * computer inside a UPNP network.  Future work will include a port opened algorith to try to find a hole in a firewall
 * or to downgrade a WAN node to NAT if a hole in the firewall is not found.  As NAT, the node would depend on a 
 * local DRDV to connect to the network.  No algorithm currently considers a completely disconected node.
 * 
 * @author jfvillal
 *
 */
public class NetworkDetective {
	private InternetGatewayDevice GatewayDev;
	protected String LANAddress;
	protected String WANAddress;
	
	int MapNumber;
	
	InternetGatewayDevice[] IGDs;
	boolean haveUPNPDevice;
	public NetworkDetective(){
		LANAddress = null;
		WANAddress = null;
		MapNumber = 1;
		int discoveryTiemout = 3000; // 5 secs
		
		try {
			IGDs = InternetGatewayDevice.getDevices( discoveryTiemout );
			haveUPNPDevice = true;
		} catch (IOException e) {
			// No UPNP devices detected
			Node.getLog().log(Level.FINER, "No UPNP devices detected" );
			IGDs = null;
			haveUPNPDevice = false;
			e.printStackTrace();
		}
		
	}
	
	public boolean doesHaveUPNPDevice(){
		return haveUPNPDevice;
	}
	/**
	 * This code is based on a sample from http://www.sbbi.net/site/upnp/
	 * closePortForwardOnRouter will make sure the port is closed as the application
	 * exits.
	 * @param port
	 * @return
	 */
    public boolean closePortForwardOnRouter(int port){
    	if( !this.haveUPNPDevice ){
    		return false;
    	}
    	boolean unmapped = false;
    	if( this.GatewayDev != null){
			try {
				unmapped = GatewayDev.deletePortMapping( null, port, "TCP" );
			
		        if ( unmapped ) {
		          Node.getLog().log(Level.FINER, "Port " + port + " unmapped" );
		        }
			} catch (IOException e) {
				Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
				
			} catch (UPNPResponseException e) {
				Node.getLog().log(Level.WARNING, Node.getStringFromErrorStack(e));
				
			}
    	}
        return unmapped;
    }
    /**
     * This code is based on a sample from http://www.sbbi.net/site/upnp/.  The code will search for UNPN
     * Complaint routers which are typical if the user starts the service from a workstation or at home.
     * Once it detects a UNPN complaint router, it open the necessary ports
     * 
     * returns true if the port was successfully opened.
     * @param localHostIP
     * @param port
     * @return
     * @throws UPNPResponseException 
     * @throws IOException 
     */
    public boolean createPortForwardOnRouter( int port) throws IOException{ //, UPNPResponseException{
    	return true;
    	/**
    	 * TODO THE CODE BELOW IS GOOD DON'T DELETE
    	 * 
    	 * But, because I am using the SocketServer the wrong way, this code is creating a HUGE amount of overhead.
    	 * To continue with testing this week ( april 10, 2010) I will disable this code.
    	 * 
    	 * But at some point, this will be needed.
    	 * 
    	 * Aug 14 2011
    	 * This code will no longer be supported.  It will fall on the user or the system administrator to
    	 * make sure the ports are open.  This research is moving away from connectivity and toward 
    	 * programability and auto-scalability.
    	 * 
    	 */
/*
    	if( !this.haveUPNPDevice ){
    		return false;
    	}
    	String localHostIP = this.LANAddress;
		
		boolean ans = false;
        
        if ( IGDs != null ) {
        	for ( int i = 0; i < IGDs.length; i++ ) {
        		GatewayDev = IGDs[i];
       
        		Node.getLog().log(Level.FINER, "Found device " + GatewayDev.getIGDRootDevice().getModelName() 
        		 								+ "\nNAT table size is " + GatewayDev.getNatTableSize() 
        		 								+ "\nNAT mapping count is " + GatewayDev.getNatMappingsCount() );
          
          
        		// now let's open the port
       
        		boolean mapped = GatewayDev.addPortMapping( "Seeds mapping" + this.MapNumber++, 
                                                   null, port, port,
                                                   localHostIP, 0, "TCP" );
        		if ( mapped ) {

        			Node.getLog().log	(Level.FINER, "Port " + port + " mapped to " + localHostIP );
        			//Node.getLog().log(Level.FINER, "Current mappings count is " + this.GatewayDev.getNatMappingsCount() );
        
        			// checking on the device
        			ActionResponse resp = GatewayDev.getSpecificPortMappingEntry( null, port, "TCP" );
        			if ( resp != null ) {
        				Node.getLog().log(Level.FINER, "Port "+ port+" mapping confirmation received from device" );
        			}
        			ans = true;
        		}else{
        			Node.getLog().log(Level.FINER, "Port " + port + " WAS NOT MAPPED TO " + localHostIP + "!" );
        		}
        	}
        
        } else {
        	Node.getLog().log(Level.FINER, "Unable to find IGD on your network" );
        }
        return ans;*/
    }
	/**
	 * The function figures out if the node is running inside a NAT. Updated 
	 * June 09, the private address is determine based on standards RFC1918 and RFC 3330
	 **/
	public boolean isInsideaNATNetwork() throws UnknownHostException, SocketException {
		boolean ans = false;
		/**
		 * The user should set debug mode if he is testing a small set of processes on his own computer.
		 * Otherwise, the program will evaluate for WAN access and other Internet-related features.
		 * At the moment, I need a debug mode myself
		 */
		
		Enumeration<NetworkInterface> netInterfaces=NetworkInterface.getNetworkInterfaces();
		InetAddress ip = null;
		Enumeration<NetworkInterface> list= NetworkInterface.getNetworkInterfaces();
		int wan = 0;
		int lan = 0;
		ans = true;
		while( list.hasMoreElements()){
			NetworkInterface inter = list.nextElement();
			Enumeration<InetAddress> addrrs = inter.getInetAddresses();
			while( addrrs.hasMoreElements()){
				ip = addrrs.nextElement();	
				String str[] = ip.getHostAddress().split("\\.");
				//for(int i = 0; i < str.length; i++){
				//	System.out.println("" + str[i]);
				//}
				if( str.length == 1){
					Node.getLog().log(Level.WARNING, "IPv6 host " + ip.getHostAddress());
					continue;
				}
				try{
					int[] b = {Integer.parseInt(str[0]), Integer.parseInt(str[1]), Integer.parseInt(str[2]), Integer.parseInt(str[3]) };
					
					/*the if is the rule to determine a loopback address */
					if( b[0] == 127 && b[1] == 0 && b[2] == 0 && (b[3] >= 0 && b[3] <= 255) ){
						Node.getLog().log(Level.FINER, " Skiping loopback address ");
					}else /*the next rules are to determine a private network address.*/ 
					if(  		( 	
									b[0] == 10 &&
									(b[1] >= 0 && b[1] <= 255) &&
									(b[2] >= 0 && b[2] <= 255) &&
									(b[3] >= 0 && b[3] <= 255) 
								)
								||
								( 	
									b[0] == 172 &&
									(b[1] >= 16 && b[1] <= 31) &&
									(b[2] >= 0 && b[2] <= 255) &&
									(b[3] >= 0 && b[3] <= 255)
								)
								||
								(
								   b[0] == 192 &&
								   b[1] == 168 &&
								  ( b[2] >= 0 && b[2] <= 255) &&
								  ( b[3] >= 0 && b[3] <= 255)
								)
							){
						lan ++;
						LANAddress = ip.getHostAddress();
					}else /*all other are internet addresses*/{
						wan ++;
						WANAddress = ip.getHostAddress();	
						Node.getLog().log(Level.FINEST, " wan " + wan + " val " +  WANAddress);
					}
				}catch(NumberFormatException e){
					Node.getLog().log(Level.WARNING, "IPv6 host " + ip.getHostAddress());
					 
				}
			
			Node.getLog().log(Level.FINER,"device: " +inter.getDisplayName()+ " ip: " + ip.getHostAddress() + " wan: " + wan + " lan: " + lan );
			}
		}
		if( wan < 1){
			//this is a NAT network
			ans = true;
			//this.NetworkType = Types.WanOrNat.NAT;
		}else{
			//it is a WAN
			ans = false;
			//this.NetworkType = Types.WanOrNat.WAN;
		}
		
		return ans;
	}
	
	/**
	 * It will contact the website www.yougetsignal.com/tools/open-ports/.  It then parses the html page to get the WAN 
	 * addres for this host
	 * @return
	 * @throws IOException 
	 */
	public  String findOutWANIP() throws IOException{
	    	String wan_ip = null;
	    	BufferedReader reader;
			URL yahoo;
			/**
			 * get the page with the ip inforamtion
			 */
			
			yahoo = new URL("http://www.yougetsignal.com/tools/open-ports/");
		
	        URLConnection yc = yahoo.openConnection();
	        reader = new BufferedReader(
	                                new InputStreamReader(
	                                yc.getInputStream()));
	        
	        /**
	         * 
	         * check each line to look for the ip address.
	         * 
	         */
		
			String c = null;
		
			while ((c = reader.readLine()) != null) {
				
				// <p style="font-size: 1.4em;">97.81.206.85</p>
				/**
				 * 
				 * split based on < character
				 */
				String list[] = c.split("<");
				if( list.length > 1){
					/**
					 * 
					 * split based on > character
					 */
					String list2[] = list[1].split(">");
					if( list2.length > 1){
						String ip = list2[1];
						/**
						 * make sure there are 4 text string sparated by 
						 * dots
						 * 
						 */
						
						String num[] = ip.split("\\.");
						
						boolean is_ip = true;
						
						if( num.length == 4){
							for( int m = 0; m < num.length ; m++){
								/**
								 * make sure the strings are numbers
								 */
								try{
									Integer.parseInt(num[m]);
								}catch( NumberFormatException e ){
									is_ip = false;
								}
							}
							if( is_ip ){
								Node.getLog().log(Level.FINER, ip );
								wan_ip = ip;
							}
						}
						
					}
				}
			}
			this.WANAddress = wan_ip;
			return wan_ip;
	}
	/**
	 * Return LANAddres.  It is not intuitive, but calling isInsideNATNetwork and 
	 * getWANIP() will create the values for LAN address and WAN.
	 * @return
	 */
	public String getLANAddress() {
		return LANAddress;
	}
	/**
	 * Stes the LAN address.
	 * @param address
	 */
	public void setLANAddress(String address) {
		LANAddress = address;
	}
	/**
	 * Returns the WAN address.  That is the address that YouGetSignal.org reports when
	 * the site is contacted.
	 * @return
	 */
	public String getWANAddress() {
		return WANAddress;
	}
	/**
	 * Sets the WAN address.
	 * @param address
	 */
	public void setWANAddress(String address) {
		WANAddress = address;
	}
	    
}
