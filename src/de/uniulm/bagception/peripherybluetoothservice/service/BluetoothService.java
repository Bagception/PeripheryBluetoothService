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


	
}
