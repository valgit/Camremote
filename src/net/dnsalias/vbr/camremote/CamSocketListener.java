package net.dnsalias.vbr.camremote;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.codebutler.android_websockets.WebSocketClient;

public class CamSocketListener implements WebSocketClient.Listener {
	protected static final String TAG = "CamSocketListener";

	WebSocketClient camsocket;
	private CamActivity _Context; // TODO: Activity ?
	
	List<BasicNameValuePair> extraHeaders = Arrays.asList(
		    new BasicNameValuePair("Cookie", "camremote=1.0")
		);

	
	public CamSocketListener(URI uri, Context myContext) { // URI.create("ws://yourserver.com")
		Log.d(TAG, "CamSocketListener - constr");
		_Context=(CamActivity)myContext;
		
		camsocket = new WebSocketClient(uri, this , extraHeaders);
		try {
		camsocket.connect();
		} catch (Exception e){
			// Camera is not available (in use or does not exist)
			Log.d(TAG, "CamSocketListener: Error getting socket : " + e.getMessage());
		}
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
		camsocket.disconnect();
		
	}

	@Override
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
			{
			camsocket.send("hello:"+ manufacturer + "," + model);
			_Context.setConnected(true);
		}
	}

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

	@Override
	public void onMessage(byte[] data) {
		// TODO Auto-generated method stub
		//Log.d(TAG, String.format("Got binary message! %s", toHexString(data)));
		Log.d(TAG, String.format("Got binary message"));
	}

	@Override
	public void onDisconnect(int code, String reason) {
		// TODO Auto-generated method stub
		Log.d(TAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
		_Context.setConnected(false);
	}

	@Override
	public void onError(Exception error) {
		// TODO Auto-generated method stub
		Log.e(TAG, "Error!", error);
	}

	public void sendPicture(byte[] data) {
		Log.d(TAG, "Send Shot : data are " + data.length);
		camsocket.send("Shot");
		camsocket.send(data);	
		Log.d(TAG, "Send Shot : out ");
	}

	public void sendBalanceMode(String modelist) {
		camsocket.send("Balance:"+modelist);
	}
	
	public void sendFocusMode(String modelist) {
		camsocket.send("Focus:"+modelist);
	}
	
	public void sendExposureMode(String modelist) {
		camsocket.send("Exposure:"+modelist);
	}

	public void sendPreview(byte[] data) {
		Log.d(TAG, "Send Preview : data are " + data.length);
		camsocket.send("Preview");
		camsocket.send(data);	
		Log.d(TAG, "Send Preview : out ");
	}

	public void sendPreviewSize(int w, int h) {
		camsocket.send("PSize:"+w+","+h);
		
	}
}
