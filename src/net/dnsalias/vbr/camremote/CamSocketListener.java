package net.dnsalias.vbr.camremote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class CamSocketListener implements Runnable {
	protected static final String TAG = "CamSocketListener";

	private Socket camsocket;
	private DataInputStream dIn;
	private DataOutputStream dOs;
	private URI uri;

	private CamActivity _Context; // TODO: Activity ?

	public CamSocketListener(URI uri, Context myContext) { // URI.create("ws://yourserver.com")
		Log.d(TAG, "CamSocketListener - constr");
		_Context=(CamActivity)myContext;
		this.uri = uri;		

		Log.d(TAG, "CamSocketListener - out constr");
	}

	/*
	public void send() {
		// Later… 
		camsocket.send("hello!");
		camsocket.send(new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF });		
	}
	 */

	public void close() {
		try {
			dIn.close();
			dOs.close();
			camsocket.close();
		} catch (IOException e) {
			Log.d(TAG, "error closing socket");
			e.printStackTrace();
		}
		_Context.setConnected(false);
	}

	public void onConnect() {	
		// try to get a name ?
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;

		//TODO: better name ?
		/* 
		 BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();

	    String deviceName = myDevice.getName();


		Log.d(TAG, "Connected!");
		if (deviceName != null)  {
			camsocket.send("hello: "+deviceName);
		} else {
		 */
		try {
			// should be TLV !!
			//os.write("takeShot".getBytes());
			//dOs.writeChar(0x0053); // "S"

			dOs.writeChar('H');
			byte[] msg = (manufacturer + "," + model).getBytes("UTF-8");

			dOs.writeInt(msg.length);
			dOs.write(msg);

		} catch (IOException e) {		
			e.printStackTrace();
		}			

		_Context.setConnected(true);

	}

	/*
	@Override
	public void onMessage(String message) {
		// TODO Auto-generated method stub
		Log.d(TAG, String.format("Got string message! %s", message));
		if (message.equals("takeShot")) {
			// this should run on main thread !
			_Context.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					_Context.takePicture();
				}
			});
		}
	}

	 */

	/*
	@Override
	public void onError(Exception error) {
		// TODO Auto-generated method stub
		Log.e(TAG, "Error!", error);
		_Context.setConnected(false);
	}
	 */

	public void sendPicture(byte[] data) {
		Log.d(TAG, "Send Shot : data are " + data.length);
		//camsocket.send("Shot");
		//camsocket.send(data);	
		Log.d(TAG, "Send Shot : out ");
	}

	public void sendBalanceMode(String modelist) {
		//camsocket.send("Balance:"+modelist);
	}

	public void sendFocusMode(String modelist) {
		//camsocket.send("Focus:"+modelist);
	}

	public void sendExposureMode(String modelist) {
		//camsocket.send("Exposure:"+modelist);
	}

	public void sendPreview(byte[] data) {
		Log.d(TAG, "Send Preview : data are " + data.length);
		//camsocket.send("Preview");
		//camsocket.send(data);	
		Log.d(TAG, "Send Preview : out ");
	}

	public void sendPreviewSize(int w, int h) {
		//camsocket.send("PSize:"+w+","+h);

	}

	@Override
	public void run() {
		// open socket
		try {
			camsocket = new Socket(uri.getHost(),uri.getPort());

			dIn = new DataInputStream(camsocket.getInputStream());
			dOs = new DataOutputStream(camsocket.getOutputStream());

		} catch (UnknownHostException e) {
			Log.d(TAG, "bad hostname" + uri.getHost());
		} catch (IOException e) {
			Log.d(TAG, "Error getting socket : " + e.getMessage());
			//e.printStackTrace();
		}

		// async call ?
		onConnect();

		// background job here ...
		try {	
			while (true) {								
				char type = (char) dIn.readByte();
				int length = dIn.readInt();                    // read length of incoming message
				Log.d(TAG,"will read type "+ type + " of len : "+length);
				if(length>0) {
					byte[] message = new byte[length];
					dIn.readFully(message, 0, message.length); // read the message
					// do something with it !
					Log.d(TAG,"read " + length + " bytes");
				} else {				
					Log.d(TAG,"nothing read ?");
				}
			} // while
		} catch (SocketException e) {
			Log.e(TAG,"socket : "+ e.getMessage());
		}
		catch (IOException e) {
			//report exception somewhere.
			e.printStackTrace();
		}

	}
}
