package de.uniulm.bagception.peripherybluetoothservice.service;

import android.os.Bundle;
import android.os.Message;
import de.philipphock.android.lib.logging.LOG;
import de.uniulm.bagception.bluetoothservermessengercommunication.service.BundleMessengerService;

public class BluetoothService extends BundleMessengerService{

	
	
	@Override
	protected void handleMessage(Message m) {
		LOG.out(this, "handleMessage");
		for(String keys:m.getData().keySet()){
			LOG.out(this, keys+": "+m.getData().getString(keys));
			
		}
		
		if (m.getData().getString("cmd").equals("PING")){
			Bundle pongBundle = new Bundle();
			pongBundle.putString("CMD", "PONG");
			sendBundle(pongBundle);
		}
				
	}

	@Override
	protected void onFirstInit() {
		
		
	}


	//internal routines
	
	protected void handleNotConnectedState(){
		//TODO implement	
	}
	protected void handleConnectedState(){
		//TODO implement
	}
	
	
	protected void connectToAvailableContainer(){
		//TODO implement
	}
	protected void onMultipleContainerAvailable(){
		//TODO implement	
	}
	protected void sendBluetoothData(Bundle b){
		//TODO implement
	}
	protected void onBluetoothDataRecv(Bundle b){
		//TODO implement
	}
	protected void disconnect(){
		//TODO implement
	}
	protected void onDisconnected(){
		//TODO implement
	}
	
	
	
}
