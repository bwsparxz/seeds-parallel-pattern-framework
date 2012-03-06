package edu.uncc.grid.seeds.otemplate.pattern.networkscout;

import java.io.Serializable;

import edu.uncc.grid.pgaf.interfaces.NonBlockReceiver;

public class MessageReceiver implements NonBlockReceiver {
	int Id;
	NetworkScoutTemplate Parent;
	public MessageReceiver( int id, NetworkScoutTemplate parent){
		Id =id;
		Parent = parent;
	}
	@Override
	public void Update(Serializable dat) throws InterruptedException {
		Byte b = (Byte) dat;
		long time = System.currentTimeMillis();
		Parent.RttStopTime[Id] = time;
		//this won't affect testing because synchronization happens
		//after we get the time from the receive.
		Parent.ReceiveWaits[Id] = false;
	}
}
