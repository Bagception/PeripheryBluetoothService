package de.uniulm.bagception.peripherybluetoothservice.service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import de.uniulm.bagception.peripherybluetoothservice.response.Response;

public class ResponseSystem {

	private final BluetoothService service;	
	private final ConcurrentHashMap<String, Bundle> pendingUserInteractions = new ConcurrentHashMap<String, Bundle>();
	
	private final String askForSpecificDevice = "askForSpecificDevice";
	private final String confirmEstablishingConnection = "confirmEstablishingConnection";
	

	
	
	public ResponseSystem(BluetoothService c) {
		service = c;
	}
	
	public int getPendingUserInteractionsSize(){
		return pendingUserInteractions.size();
	}
	
	public void resendAll(){
		for (Map.Entry<String, Bundle> set:pendingUserInteractions.entrySet()){
			service.sendResponseBundle(set.getValue());
		}
	}
	
	public void makeResponse_askForSpecificDevice(ArrayList<BluetoothDevice> devices){
		Bundle b = new Bundle();
		
		b.putString(Response.RESPONSE_TYPE_KEY, Response.RESPONSE_TYPE_VALUE_Ask_ForSpecificDevice);
		b.putParcelableArrayList(Response.RESPONSE_PAYLOAD_CONNECTABLE_DEVICES, devices);
		service.sendResponseBundle(b);
		pendingUserInteractions.put(askForSpecificDevice, b);

	}
	
	public void clearResponseRequestFor_askForSpecificDevice(){
		pendingUserInteractions.remove(askForSpecificDevice);
	}
	
	public void makeResponse_confirmEstablishingConnection(BluetoothDevice device){
		Bundle b = new Bundle();
		
		b.putString(Response.RESPONSE_TYPE_KEY, Response.RESPONSE_TYPE_VALUE_confirmEstablishingConnection);
		b.putParcelable(Response.RESPONSE_PAYLOAD_CONNECTABLE_DEVICE, device);
		service.sendResponseBundle(b);
		pendingUserInteractions.put(confirmEstablishingConnection, b);
	}
	public void clearResponseRequestFor_confirmEstablishingConnection(BluetoothDevice device){
		pendingUserInteractions.remove(confirmEstablishingConnection);
	}
	
	public void handleInteraction(Bundle b){
		if (b.getString(Response.RESPONSE_TYPE_KEY).equals(Response.RESPONSE_TYPE_VALUE_Ask_ForSpecificDevice)){
			BluetoothDevice device=b.getParcelable(Response.RESPONSE_PAYLOAD_CONNECTABLE_DEVICE_replyDevice);
			service.interactionFor_askForSpecificDevice(device);
			
		}else if (b.getString(Response.RESPONSE_TYPE_KEY).equals(Response.RESPONSE_TYPE_VALUE_confirmEstablishingConnection)){
			boolean connect=b.getBoolean(Response.RESPONSE_TYPE_VALUE_confirmEstablishingConnection_replyYES_NO);
			service.interactionFor_confirmEstablishingConnection(connect);
			
		}
		
		//TODO continue
	}
	
	public interface Interaction{
		public void interactionFor_askForSpecificDevice(BluetoothDevice d);
		public void interactionFor_confirmEstablishingConnection(boolean connect);
		
	}
	
}

