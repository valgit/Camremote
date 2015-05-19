package net.dnsalias.vbr.camremote;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class CamSocketListener extends  WebSocketClient {
	protected static final String TAG = "CamSocketListener";

	//private WebSocketClient camsocket;
	private CamActivity _Context; // TODO: Activity ?

	List<BasicNameValuePair> extraHeaders = Arrays.asList(
			new BasicNameValuePair("Cookie", "camremote=1.0")
			);


	public CamSocketListener(URI uri) {
		super(uri);
		Log.d(TAG, "CamSocketListener - constr");		
	}

	public CamSocketListener(URI uri, Context myContext) { 
		super(uri);

		Log.d(TAG, "CamSocketListener - constr");
		_Context=(CamActivity)myContext;

		//camsocket = new WebSocketClient(uri, this , extraHeaders);		
		//camsocket = new WebSocketClient(uri)

	}

	@Override
	public void onOpen( ServerHandshake handshake) {	
		Log.d(TAG, String.format("onOpen via draft : %s", getDraft()));
		// try to get a name ?
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;

		//TODO: better name ?	
		{
			send("hello:"+ manufacturer + "," + model);
			_Context.setConnected(true);
		}

	}

	@Override
	public void onMessage(String message) {		
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

	/*
			@Override
			public void onMessage(byte[] data) {
				//Log.d(TAG, String.format("Got binary message! %s", toHexString(data)));
				Log.d(TAG, String.format("Got binary message"));
			}
	 */
	@Override
	public void onClose( int code, String reason, boolean remote ) {		
		Log.d(TAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
		_Context.setConnected(false);
	}

	@Override
	public void onError(Exception error) {		
		Log.e(TAG, "Error!", error);
		error.printStackTrace();
	}


	/*
		// open the webscoket !
		Log.d(TAG, "CamSocketListener - socket created");
		try {
			camsocket.connect();
		} catch (Exception e){
			// Camera is not available (in use or does not exist)
			Log.d(TAG, "CamSocketListener: Error getting socket : " + e.getMessage());
		}
		Log.d(TAG, "CamSocketListener - out constr");

	}
	 */

	/*
	 * add-on methods
	 */
	public void sendPicture(byte[] data) {
		Log.d(TAG, "Send Shot : data are " + data.length);
		send("Shot");
		send(data);	
		Log.d(TAG, "Send Shot : out ");
	}

	public void sendBalanceMode(String modelist) {
		send("Balance:"+modelist);
	}

	public void sendFocusMode(String modelist) {
		send("Focus:"+modelist);
	}

	public void sendExposureMode(String modelist) {
		send("Exposure:"+modelist);
	}

	public void sendPreview(byte[] data) {
		Log.d(TAG, "Send Preview : data are " + data.length);
		send("Preview");
		send(data);	
		Log.d(TAG, "Send Preview : out ");
	}

	public void sendPreviewSize(int w, int h) {
		send("PSize:"+w+","+h);

	}
}
