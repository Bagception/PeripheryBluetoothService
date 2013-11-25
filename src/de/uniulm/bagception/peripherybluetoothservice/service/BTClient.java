package de.uniulm.bagception.peripherybluetoothservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import de.uniulm.bagception.protocol.bundle.BundleProtocolCallback;

public class BTClient implements Runnable {
	private Thread recvThread;
	private volatile boolean listening = true;
	private BluetoothSocket clientSocket;
	private final BluetoothDevice device;
	private final UUID uuid;
	private InputStream clientSocketInStream;
	private OutputStream clientSocketOutStream;

	private boolean isConnected = false;
	private BTClient.ClientStatusCallback clientcallback;
	private final BundleProtocolmpl protocol;

	public BTClient(BluetoothDevice device, String uuid, BundleProtocolCallback callback, ClientStatusCallback clientCallback) throws IOException {
		this.clientcallback = clientCallback;
		this.protocol = new BundleProtocolmpl(callback);
		this.device = device;
		this.uuid = UUID.fromString(uuid);
		
	}

	// executes itself in another thread and listens
	public void startListeningForIncomingBytes() {
		recvThread = new Thread(this);
		recvThread.start();
	}

	public void run() {
		byte[] buffer = new byte[1024]; // buffer store for the stream
		BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

		try {// cannot throw here of course (the method who called start will
				// already be somewhere else because of the thread
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception.

			clientSocket = device.createRfcommSocketToServiceRecord(uuid);

			
			clientSocket.connect();
			clientSocketInStream = clientSocket.getInputStream();
			clientSocketOutStream = clientSocket.getOutputStream();
			
		} catch (IOException connectException) {
			// Unable to connect; close the socket and get out.
			connectException.printStackTrace();
			try {
				clientSocket.close();
				clientcallback.onDisconnect();
				isConnected=false;
			} catch (IOException closeException) {
				closeException.printStackTrace();
			}
			return;
		}
		clientcallback.onConnect();
		isConnected=true;
		// manage the connection
		int bytes = 0;
		while (listening) {
			try {
				// Read from the InputStream
				Log.d("bt", "before read");
				bytes = clientSocketInStream.read(buffer);
				Log.d("bt", "after read");
				if (bytes == -1) {
					// connection remotely closed
					listening = false;
					clientcallback.onDisconnect();
					isConnected=false;
					break;
				}
				
				protocol.bytesRecv(buffer,bytes);
			} catch (IOException e) {
				e.printStackTrace();
				clientcallback.onDisconnect();
				isConnected=false;
				break;
			}
		}
	}

	/**
	 * Will cancel an in-progress connection, and close the socket.
	 * 
	 * @throws IOException
	 */
	public void cancel() throws IOException {
		listening = false;
		clientSocket.close();

	}

	public boolean isConnected() {
		return isConnected;
	}
	
	public void send(Bundle b){
		try {
			clientSocketOutStream.write(protocol.getSendableBytes(b));
			clientSocketOutStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	public interface ClientStatusCallback{
		public void onConnect();
		public void onDisconnect();
		
	}

}