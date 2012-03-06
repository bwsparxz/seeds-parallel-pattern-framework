package edu.uncc.grid.seeds.otemplate.pattern.networkscout;

import java.io.Serializable;

import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;

public class BandwidthMessageReceiver implements NonBlockReceiver {
	int Id;
	NetworkScoutTemplate Parent;
	public BandwidthMessageReceiver( int id, NetworkScoutTemplate parent){
		Id =id;
		Parent = parent;
	}
	@Override
	public void Update(Serializable dat) throws InterruptedException {
		byte[] b = (byte[]) dat;
		long time = System.currentTimeMillis();
		
		//this won't affect testing because synchronization happens
		//after we get the time from the receive.
		Parent.RttStopTime[Id] = time;
		Parent.ReceiveWaits[Id] = false;
		
	}
}
