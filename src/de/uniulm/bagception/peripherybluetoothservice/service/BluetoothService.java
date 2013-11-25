package de.uniulm.bagception.peripherybluetoothservice.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.webkit.WebView.FindListener;
import de.philipphock.android.lib.logging.LOG;
import de.uniulm.bagception.bluetooth.BagceptionBTServiceInterface;
import de.uniulm.bagception.bluetooth.BagceptionBluetoothUtil;
import de.uniulm.bagception.bluetooth.CheckReachableCallback;
import de.uniulm.bagception.bluetoothservermessengercommunication.MessengerConstants;
import de.uniulm.bagception.bluetoothservermessengercommunication.service.BundleMessengerService;
import de.uniulm.bagception.protocol.bundle.BundleProtocolCallback;
import de.uniulm.bagception.protocol.bundle.constants.Command;
import de.uniulm.bagception.protocol.bundle.constants.StatusCode;

public class BluetoothService extends BundleMessengerService implements
		CheckReachableCallback, ResponseSystem.Interaction,
		BundleProtocolCallback, BTClient.ClientStatusCallback {

	private BTClient btclient;

	// TODO broadcast recv to resend all responses

	public enum ResponseMode {

		/**
		 * the service does most things automatically, if he is not sure, the
		 * ResponseSystem is asked to handle that note that an actual responder
		 * can also be software, not only a user interaction
		 * 
		 * with this design, we can implement smart services that handles
		 * interactions implicitly
		 */
		MINIMAL, // implementation progress: 0 //TODO

		/**
		 * the service performs operations in background but halts on every
		 * decision, even if it is the only option(like connection to only one
		 * device) this mode is intended to be used with your primary smartphone
		 */
		MAXIMAL // implementation progress: 0 //TODO
	}

	// TODO later, init this with config values
	private ResponseMode responseMode = ResponseMode.MAXIMAL;
	private ResponseSystem responseSystem;

	private final BluetoothAdapter btAdapter = BluetoothAdapter
			.getDefaultAdapter();

	// counter that indicates if there is a pending isReachable request to
	// determine when the test is done (due to async operatopn)
	private volatile int pendingDeviceFeedbacks = 0;
	private final ArrayList<BluetoothDevice> bagceptionDevicesInRange = new ArrayList<BluetoothDevice>();

	private BluetoothDevice tmp_bt_device_confirm;

	/**
	 * called by IBinder an activity sends messages, this is where they arrive
	 * 
	 * @param b
	 */
	@Override
	protected void handleMessage(Message m) {
		if (m.what == MessengerConstants.MESSAGE_BUNDLE_MESSAGE) {
			handleMessageBundle(m.getData());
		} else if (m.what == MessengerConstants.MESSAGE_BUNDLE_RESPONSE) {
			responseSystem.handleInteraction(m.getData());
		} else if (m.what == MessengerConstants.MESSAGE_BUNDLE_COMMAND) {
			Command command = Command.getCommand(m.getData());
			handleCommand(command);

		}

	}

	protected void handleMessageBundle(Bundle b) {
		for (String keys : b.keySet()) {
			LOG.out(this, keys + ": " + b.get(keys));

		}

		btclient.send(b);

	}

	protected void handleCommand(Command command) {
		LOG.out(this, "command recv " + command.getCommandCode());
		switch (command) {
		case TRIGGER_SCAN_DEVICES:
			getPairedBagceptionDevicesInRangeAsync();
			break;
		case PING:
			sendCommandBundle(Command.getCommandBundle(Command.PONG));
		case PONG:
			// nothing to do here, Pong is only on client side
		case POLL_ALL_RESPONSES:
			LOG.out(this, "POLL");
			responseSystem.resendAll();
			break;

		case RESEND_STATUS:
			LOG.out(this, "RESEND");
			if (btclient == null) {
				onDisconnect();
				return;
			}
			if (btclient.isConnected()) {
				onConnect();
			} else {
				onDisconnect();
			}
		case DISCONNECT:
			try {
				btclient.cancel();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}

	}

	protected void sendStatus(StatusCode code) {
		sendStatusBundle(StatusCode.getStatusBundle(code));
	}

	@Override
	protected void onFirstInit() {
		responseSystem = new ResponseSystem(this);
	}

	// internal routines

	// states

	protected void handleNotConnectedState() {
		sendStatusBundle(StatusCode.getStatusBundle(StatusCode.DISCONNECTED));
	}

	protected void handleConnectedState() {
		sendStatusBundle(StatusCode.getStatusBundle(StatusCode.CONNECTED));

	}

	// connect

	protected void connectToAvailableContainer(BluetoothDevice device) {
		try {
			btclient = new BTClient(device,
					BagceptionBTServiceInterface.BT_UUID, this, this);
			btclient.startListeningForIncomingBytes();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// data transmission

	protected void onBluetoothDataRecv(Bundle b) {
		sendMessageBundle(b);
	}

	/*
	 * ############################################### ###############
	 * reachability check ##############
	 * #################################################
	 */

	protected void getPairedBagceptionDevicesInRangeAsyncDone() {
		LOG.out(this, "reachable scan done");
		LOG.out(this, bagceptionDevicesInRange.size() + " devices in range");
		if (bagceptionDevicesInRange.size() == 0) {
			// nothing in range.. pause? //TODO
		} else if (bagceptionDevicesInRange.size() == 1) {
			// here we have only one device in range

			final BluetoothDevice device = bagceptionDevicesInRange.get(0);

			switch (responseMode) {

			case MAXIMAL:
				tmp_bt_device_confirm = device;
				responseSystem
						.makeResponse_confirmEstablishingConnection(device);

				break;

			case MINIMAL:
				// connect to container
				connectToAvailableContainer(device);
				break;

			}

		} else {
			responseSystem
					.makeResponse_askForSpecificDevice(bagceptionDevicesInRange);
		}
	}

	protected synchronized void getPairedBagceptionDevicesInRangeAsync() {
		LOG.out(this, "begin scanning..");
		bagceptionDevicesInRange.clear();
		Set<BluetoothDevice> bonded = btAdapter.getBondedDevices();
		final UUID serviceUUID = UUID
				.fromString(BagceptionBTServiceInterface.BT_UUID);
		for (BluetoothDevice device : bonded) {
			LOG.out(this, "bond device: " + device.getName());
			if (BagceptionBluetoothUtil.isBagceptionServer(device)) {
				LOG.out(this, "bagception device: " + device.getName());
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
		LOG.out(this, "DEVICE: " + device.getName() + " reachable: "
				+ reachable);
		if (reachable) {
			bagceptionDevicesInRange.add(device);
		}
		if (pendingDeviceFeedbacks <= 0) {
			// all bond devices checked for reachability
			getPairedBagceptionDevicesInRangeAsyncDone();

		}

	}

	/*
	 * ############################################### //########
	 * ResponseSystem.Interaction ###########
	 * ################################################
	 */

	@Override
	public void interactionFor_askForSpecificDevice(BluetoothDevice d) {
		connectToAvailableContainer(d);

	}

	@Override
	public void interactionFor_confirmEstablishingConnection(boolean connect) {
		if (connect) {
			connectToAvailableContainer(tmp_bt_device_confirm);
		} else {
			// TODO todo?
		}

	}

	/*
	 * ############################################### ###########
	 * BundleProtocolCallback ###########
	 * ################################################
	 */

	@Override
	public void onBundleRecv(Bundle bundle) {
		sendMessageBundle(bundle);

	}

	/*
	 * ############################################### //###########
	 * BTClient.ClientStatusCallbac ###########
	 * ################################################
	 */

	@Override
	public void onConnect() {
		handleConnectedState();

	}

	@Override
	public void onDisconnect() {
		handleNotConnectedState();
	}

}
