package de.uniulm.bagception.peripherybluetoothservice.service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import de.uniulm.bagception.protocol.bundle.constants.Response;
import de.uniulm.bagception.protocol.bundle.constants.ResponseAnswer;

public class ResponseSystem {

	private final BluetoothService service;	
	
	
	private final ConcurrentHashMap<Integer, Bundle> pendingUserInteractions = new ConcurrentHashMap<Integer, Bundle>();
	

	
	
	public ResponseSystem(BluetoothService c) {
		service = c;
	}
	
	public int getPendingUserInteractionsSize(){
		return pendingUserInteractions.size();
	}
	
	public void resendAll(){
		for (Map.Entry<Integer, Bundle> set:pendingUserInteractions.entrySet()){
			service.sendResponseBundle(set.getValue());
		}

	}
	
	/*
	 * ################################################# 
	 * ###############   handle response ->#############
	 * #################################################
	 */
	
		
	public void makeResponse_askForSpecificDevice(ArrayList<BluetoothDevice> devices){
		
		
		Bundle b = Response.getResponseBundle(Response.Ask_For_Specific_Device);
		b.putParcelableArrayList(Response.EXTRA_KEYS.PAYLOAD, devices);
		
		pendingUserInteractions.remove(ResponseAnswer.Confirm_Established_Connection.getResponseAnswerCode());
		
		service.sendResponseBundle(b);
		pendingUserInteractions.put(Response.Ask_For_Specific_Device.getResponseCode(), b);

	}
	
	public void clearResponseRequestFor_askForSpecificDevice(){
		pendingUserInteractions.remove(Response.Ask_For_Specific_Device);
	}
	
	public void makeResponse_confirmEstablishingConnection(BluetoothDevice device){
		
		Bundle b = Response.getResponseBundle(Response.Confirm_Established_Connection);
		b.putParcelable(Response.EXTRA_KEYS.PAYLOAD, device);
		
		pendingUserInteractions.remove(ResponseAnswer.Ask_For_Specific_Device.getResponseAnswerCode());
		
		service.sendResponseBundle(b);
		pendingUserInteractions.put(Response.Confirm_Established_Connection.getResponseCode(), b);
		
		
	}
	public void clearResponseRequestFor_confirmEstablishingConnection(BluetoothDevice device){
		pendingUserInteractions.remove(Response.Confirm_Established_Connection);
	}
	
	
	/*
	 * ################################################# 
	 * ###########   handle responseAnswer <-###########
	 * #################################################
	 */

	
	/**
	 * handles answers
	 * @param b
	 */
	public void handleInteraction(Bundle b){
		
		ResponseAnswer r = ResponseAnswer.getResponseAnswer(b);
		boolean isAck = ResponseAnswer.isACK(b);
		
		if (isAck){
			pendingUserInteractions.remove(r.getResponseAnswerCode());
			
			return;
		}
		
		switch (r){
		
		case Ask_For_Specific_Device:
			BluetoothDevice device = b.getParcelable(ResponseAnswer.EXTRA_KEYS.PAYLOAD);
			service.interactionFor_askForSpecificDevice(device);
			break;
			
		case Confirm_Established_Connection:
			Boolean boo = b.getBoolean(ResponseAnswer.EXTRA_KEYS.PAYLOAD);
			service.interactionFor_confirmEstablishingConnection(boo);
			break;
		
		}
		
	}
	
	public interface Interaction{
		public void interactionFor_askForSpecificDevice(BluetoothDevice d);
		public void interactionFor_confirmEstablishingConnection(boolean connect);
		
	}
	
}

