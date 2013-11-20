package de.uniulm.bagception.peripherybluetoothservice.service;

import android.os.Bundle;
import de.uniulm.bagception.protocol.bundle.BundleProtocol;
import de.uniulm.bagception.protocol.bundle.BundleProtocolCallback;
import de.uniulm.bagception.protocol.message.PayloadContentLengthProtocol;
import de.uniulm.bagception.protocol.message.PayloadContentLengthProtocolCallback;


public class BundleProtocolmpl implements  PayloadContentLengthProtocolCallback{


	private final BundleProtocol bundleProtocol;
	private final PayloadContentLengthProtocol plcp;
	
	public BundleProtocolmpl(BundleProtocolCallback callback) {
		this.bundleProtocol = new BundleProtocol(callback);
		this.plcp = new PayloadContentLengthProtocol(this);
	}

	@Override
	public void onMessageRecv(String msg) {
		bundleProtocol.in(msg);
		
	}

	public void bytesRecv(byte[] b,int bytes){
		plcp.in(new String(b,0,bytes));
	}
	
	public byte[] getSendableBytes(Bundle b){
		return plcp.out(bundleProtocol.out(b)).getBytes();
	}
}
