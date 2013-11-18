package de.uniulm.bagception.peripherybluetoothservice.service;

import android.os.Message;
import de.philipphock.android.lib.logging.LOG;
import de.philipphock.android.lib.services.messenger.MessengerService;

public class BluetoothService extends MessengerService{

	
	
	@Override
	protected void handleMessage(Message m) {
		for(String keys:m.getData().keySet()){
			LOG.out(this, keys+": "+m.getData().getString(keys));
		}
				
	}

	@Override
	protected void onFirstInit() {
		LOG.out(this, "BT CLIENT SERVICE");
		
	}

	
	
	

	
}
