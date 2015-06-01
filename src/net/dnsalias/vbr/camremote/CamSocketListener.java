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
	private boolean running;	

	public CamSocketListener( URI uri, Context myContext) { // URI.create("ws://yourserver.com")
		Log.d(TAG, "CamSocketListener - constr");
		_Context=(CamActivity)myContext;
		this.uri = uri;		

		running = true;
		
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
			running = false;
			// maybe we should close half way ?
			dIn.close();
			dOs.close();
			camsocket.close();
		} catch (IOException e) {
			Log.d(TAG, "error closing socket");
			e.printStackTrace();
		}
		_Context.setConnected(false);
	}

	public void sendIdentity() {	
		// try to get a name ?
		//String manufacturer = Build.MANUFACTURER;
		//String model = Build.MODEL;
		String deviceName = Build.MANUFACTURER + "," + Build.MODEL;
		//TODO: better name ?
		/* 
		 BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();

	    String deviceName = myDevice.getName();


		Log.d(TAG, "Connected!");
		if (deviceName != null)  {
			camsocket.send("hello: "+deviceName);
		} else {
		 */
		sendString('H',deviceName);

		_Context.setConnected(true);

	}


	private void sendString(char type,String data)
	{
		if (data != null ) {
			// refuse to send null data !
			try {

				dOs.writeChar(type);
				byte[] msg = data.getBytes("UTF-8");

				dOs.writeInt(msg.length);
				dOs.write(msg);
				dOs.flush();
			} catch (IOException e) {		
				e.printStackTrace();
			}
		}
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
		try {
			dOs.writeChar('S');
			dOs.writeInt(data.length);
			dOs.write(data);
			dOs.flush();
		} catch (IOException e) {			
			e.printStackTrace();
		}		

		Log.d(TAG, "Send Shot : out ");
	}

	public void sendBalanceMode(String modelist) {		
		sendString('B',modelist);
	}

	public void sendFocusMode(String modelist) {		
		sendString('F',modelist);
	}

	public void sendExposureMode(String modelist) {
		sendString('E',modelist);		
	}

	public void sendPreview(byte[] data) {
		Log.d(TAG, "Send Preview : data are " + data.length);
		//camsocket.send("Preview");
		//camsocket.send(data);	
		Log.d(TAG, "Send Preview : out ");
	}

	public void sendPreviewSize(int w, int h) {
		sendString('T',(w+","+h));

	}

	private void parseMessage(char type) {		
		switch (type) {
		case 'S': // takeShot
			// this should run on main thread !
			_Context.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					_Context.takePicture();
				}
			});
			break;
		case 'Q' : // quit !
			break;
		default:
			System.out.println("parseMessage of type :" + type );
		}
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
		sendIdentity();

		Log.d(TAG, "CamSocketListener - socket thread up");
		
		// background job here ...
		try {	
			while (running) {								
				char type =  dIn.readChar();
				int length = dIn.readInt();                    // read length of incoming message
				Log.d(TAG,"will read type "+ type + " of len : "+length);
				if(length>0) {
					byte[] message = new byte[length];
					dIn.readFully(message, 0, message.length); // read the message
					// do something with it !
					Log.d(TAG,"read " + length + " bytes");
					//parseMessage(type,length,message);
				} else {				
					parseMessage(type);
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
