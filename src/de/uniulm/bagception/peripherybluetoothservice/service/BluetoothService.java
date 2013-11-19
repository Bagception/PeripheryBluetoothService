package de.uniulm.bagception.peripherybluetoothservice.service;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import de.philipphock.android.lib.logging.LOG;
import de.uniulm.bagception.bluetooth.BagceptionBTServiceInterface;
import de.uniulm.bagception.bluetooth.BagceptionBluetoothUtil;
import de.uniulm.bagception.bluetooth.CheckReachableCallback;
import de.uniulm.bagception.bluetoothservermessengercommunication.MessengerConstants;
import de.uniulm.bagception.bluetoothservermessengercommunication.service.BundleMessengerService;

public class BluetoothService extends BundleMessengerService implements
		CheckReachableCallback,ResponseSystem.Interaction {

	//TODO broadcast recv to resend all responses
	
	public enum ResponseMode{

		/**
		 * the service does most things automatically, 
		 * if he is not sure, the ResponseSystem is asked to handle that
		 * note that an actual responder can also be software, not only a user interaction
		 * 
		 * with this design, we can implement smart services that handles interactions implicitly
		 */
		MINIMAL, //implementation progress: 0 //TODO
		
	
		/**
		 * the service performs operations in background but halts on every decision, 
		 * even if it is the only option(like connection to only one device)
		 * this mode is intended to be used with your primary smartphone
		 */
		MAXIMAL  //implementation progress: 0 //TODO
	}
	
	

	//TODO later, init this with config values
	private ResponseMode responseMode = ResponseMode.MAXIMAL;
	private ResponseSystem responseSystem;
	
	private final BluetoothAdapter btAdapter = BluetoothAdapter
			.getDefaultAdapter();
	

	// counter that indicates if there is a pending isReachable request to
	// determine when the test is done (due to async operatopn)
	private volatile int pendingDeviceFeedbacks = 0;
	private final ArrayList<BluetoothDevice> bagceptionDevicesInRange = new ArrayList<BluetoothDevice>();

	
	private BluetoothDevice tmp_bt_device_confirm;
	
	@Override
	protected void handleMessage(Message m) {
		if (m.what == MessengerConstants.MESSAGE_BUNDLE_MESSAGE){
			handleMessageBundle(m.getData());
		}else if (m.what == MessengerConstants.MESSAGE_BUNDLE_RESPONSE){
			responseSystem.handleInteraction(m.getData());
		}else if (m.what == MessengerConstants.MESSAGE_BUNDLE_STATUS){
			//not implemented yet
		}
		
		

	}

	protected void handleMessageBundle(Bundle b){
		LOG.out(this, "handleMessage");
		for (String keys : b.keySet()) {
			LOG.out(this, keys + ": " + b.getString(keys));

		}

		if (b.getString("cmd").equals("PING")) {
			Bundle pongBundle = new Bundle();
			pongBundle.putString("CMD", "PONG");
			sendMessageBundle(pongBundle);
		}
	}
	
	@Override
	protected void onFirstInit() {
		responseSystem = new ResponseSystem(this);
	}

	// internal routines

	// states

	protected void handleNotConnectedState() {
		// TODO implement
	}

	protected void handleConnectedState() {
		// TODO implement
	}

	// connect

	protected void connectToAvailableContainer(BluetoothDevice device) {
		// TODO implement
	}


	// data transmission

	protected void sendBluetoothData(Bundle b) {
		// TODO implement
	}

	protected void onBluetoothDataRecv(Bundle b) {
		// TODO implement
	}

	// disconnection

	protected void disconnect() {
		// TODO implement
	}

	protected void onDisconnected() {
		// TODO implement
	}

	// reachability check

	protected void getPairedBagceptionDevicesInRangeAsyncDone() {
		if (bagceptionDevicesInRange.size() == 0) {
			// nothing in range.. pause? //TODO
		} else if (bagceptionDevicesInRange.size() == 1) {
			//here we have only one device in range
			
			final BluetoothDevice device = bagceptionDevicesInRange.get(0);
			
				
			switch (responseMode){
			
				case MAXIMAL:
					tmp_bt_device_confirm = device;
					responseSystem.makeResponse_confirmEstablishingConnection(device);
					
					break;
					
				case MINIMAL:
					//connect to container
					connectToAvailableContainer(device);
					break;
				
			}
				
			
		} else {
			responseSystem.makeResponse_askForSpecificDevice(bagceptionDevicesInRange);
		}
	}

	protected synchronized void getPairedBagceptionDevicesInRangeAsync() {
		bagceptionDevicesInRange.clear();
		Set<BluetoothDevice> bonded = btAdapter.getBondedDevices();
		final UUID serviceUUID = UUID
				.fromString(BagceptionBTServiceInterface.BT_UUID);
		for (BluetoothDevice device : bonded) {
			if (BagceptionBluetoothUtil.isBagceptionServer(device)) {
				pendingDeviceFeedbacks++;
				BagceptionBluetoothUtil.checkReachable(device, serviceUUID,
						this);
			}
		}

	}

	@Override
	public synchronized void isReachable(BluetoothDevice device,
			boolean reachable) {
		pendingDeviceFeedbacks--;
		if (reachable) {
			bagceptionDevicesInRange.add(device);
		}
		if (pendingDeviceFeedbacks <= 0) {
			// all bond devices checked for reachability
			getPairedBagceptionDevicesInRangeAsyncDone();
		}

	}



	
	//ResponseSystem.Interaction

	@Override
	public void interactionFor_askForSpecificDevice(BluetoothDevice d) {
		connectToAvailableContainer(d);
		
	}

	@Override
	public void interactionFor_confirmEstablishingConnection(boolean connect) {
		if (connect){
			connectToAvailableContainer(tmp_bt_device_confirm);
		}else{
			//TODO todo?
		}
		
		
	}
}
