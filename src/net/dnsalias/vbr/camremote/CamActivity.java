package net.dnsalias.vbr.camremote;

//import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.Log;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class CamActivity extends Activity {
	private static final String TAG = "CamActivity";
	private Camera mCamera;
	private CameraPreview mPreview;
	private PictureCallback mPicture;
	private Button capture, switchCamera;
	private Context myContext;
	private LinearLayout cameraPreview;
	private boolean cameraFront = false;

	// client socket part ?
	//private Socket socket;
	//private static final int SERVERPORT = 5000;
	//private static final String SERVER_IP = "10.0.2.2";
	private URI uri;
	private CamSocketListener camsocket = null;
	private boolean connected;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cam);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		myContext = this;
		initialize();    

		
		
		//TODO: new Thread(new ClientTask()).start();
		Log.d(TAG, "onCreate'd");
	}

	private int findBackFacingCamera() {
		int cameraId = -1;
		//Search for the back facing camera
		//get the number of cameras
		int numberOfCameras = Camera.getNumberOfCameras();
		Log.d(TAG, "findBackFacingCamera - num:  " + numberOfCameras);

		//for every camera check
		for (int i = 0; i < numberOfCameras; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			Log.d(TAG, "findBackFacingCamera - num:  " + i + " face: " + info.facing);
			if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
				Log.d(TAG, "findBackFacingCamera - num:  " + i + " is backfacing ");
				cameraId = i;
				cameraFront = false;
				break;
			}
		}
		return cameraId;
	}

	public void onResume() {
		super.onResume();
		Log.d(TAG,"activity camera onResume.");

		if (!checkCameraHardware(myContext)) {
			Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
			Log.d(TAG, "onResume - Sorry, your phone does not have a camera!");
			toast.show();
			finish();
		}
		if (mCamera == null) {
			Log.d(TAG,"acquireing camera onResume.");
			//if the front facing camera does not exist

			//TODO: 
			//switchCamera.setVisibility(View.GONE);
			int id = findBackFacingCamera();
			Log.d(TAG,"onResume: find camera id : " + id);
			mCamera = getCameraInstance(); // Camera.open(findBackFacingCamera());
			if (mCamera != null) {
				mPicture = getPictureCallback();
				mPreview.refreshCamera(mCamera);
			} else {
				Log.e(TAG, "onResume - no camera found !!!! find : " + Camera.getNumberOfCameras());
			}
		}
	}

	public void initialize() {
		cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
		mPreview = new CameraPreview(myContext, mCamera);
		cameraPreview.addView(mPreview);

		capture = (Button) findViewById(R.id.button_capture);
		capture.setOnClickListener(captureListener);

		switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
		switchCamera.setOnClickListener(connectCameraListener);
	}

	/*
	 * TODO: better idea ...
	 */
	OnClickListener connectCameraListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.d(TAG, "connectCameraListener - connecting...!");
			if (!connected) {
			// TODO:
			SharedPreferences prefs = PreferenceManager
				    .getDefaultSharedPreferences(CamActivity.this);
			String port = prefs.getString("prefServerport", "5000");
			String server = prefs.getString("prefServername", "127.0.0.1");
			Log.d(TAG, "connectCameraListener URI : ws://" + server + ":" + port + "/remotecam");
			//uri = URI.create("ws://10.24.244.99:5000/remotecam");
			uri = URI.create("ws://"+server+":"+port+"/remotecam");
			camsocket = new CamSocketListener(uri,myContext);
			
			//camsocket.send();
			switchCamera.setText("disconnect");
			} else {
				camsocket.close();
				//TODO: ?delete 
			}
		}
	};

	// TODO: better
	public void chooseCamera() {
		//if the camera preview is the front
		Log.d(TAG, "chooseCamera - check : " + cameraFront);
		if (cameraFront) {
			Log.d(TAG, "chooseCamera -  use front camera ");
			int cameraId = 0; // findFrontFacingCamera();
			if (cameraId >= 0) {
				//open the backFacingCamera
				//set a picture callback
				//refresh the preview

				mCamera = Camera.open(cameraId);
				mPicture = getPictureCallback();
				mPreview.refreshCamera(mCamera);
			}
		} else {
			Log.d(TAG, "chooseCamera -  use back camera ");
			int cameraId = findBackFacingCamera();
			if (cameraId >= 0) {
				//open the backFacingCamera
				//set a picture callback
				//refresh the preview

				mCamera = Camera.open(cameraId);
				mPicture = getPictureCallback();
				mPreview.refreshCamera(mCamera);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG,"release camera onPause.");
		//when on Pause, release camera in order to be used from other applications
		releaseCamera();
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){
		Camera c = null;
		try {			
			c = Camera.open(); // attempt to get a Camera instance
		}
		catch (Exception e){
			// Camera is not available (in use or does not exist)
			Log.d(TAG, "getCameraInstance: Error getting camera : " + e.getMessage());
		}
		return c; // returns null if camera is unavailable
	}

	private boolean checkCameraHardware(Context context) {
		//check if the device has camera
		/*
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            Log.d(TAG, "hasCamera -  no camera ?");
            //return false;
            return true;
        }
		 */
		/* for now ? */
		int numCameras = Camera.getNumberOfCameras();
		if (numCameras > 0) {
			return true;
		} else
			return false;
	}

	private PictureCallback getPictureCallback() {
		PictureCallback picture = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				Log.d(TAG, "onPictureTaken - in");

				if (camsocket != null) 
					camsocket.sendPicture(data);
				else {
					//make a new picture file
					FileOutputStream fos;

					File pictureFile = getOutputMediaFile();

					if (pictureFile == null) {
						Log.d(TAG, "onPictureTaken - no file");

						//refresh camera to continue preview
						mPreview.refreshCamera(mCamera);
						capture.setEnabled(true);
						return ;
					} 
					try {
						//write the file
						fos = new FileOutputStream(pictureFile);

						fos.write(data);
						fos.close();

					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

					// use async for job
					//new SaveImageTask().execute(data);
					Log.d(TAG, "onPictureTaken - jpeg wrote bytes: " + data.length + " to " + pictureFile.getAbsolutePath());
					//Log.d(TAG, "onPictureTaken - jpeg wrote bytes: " + data.length);
					//Log.d(TAG, "onPictureTaken - jpeg");
				}

				//refresh camera to continue preview
				mPreview.refreshCamera(mCamera);
				capture.setEnabled(true);
				
			}
		};
		return picture;
	}

	// on main thread !
	public void takePicture() {
		Log.d(TAG, "takePicture - in");
		//mCamera.stopPreview();
		
		 //-- Must add the following callback to allow the camera to autofocus.
		/* TODO:
	    mCamera.autoFocus(new Camera.AutoFocusCallback(){
	        @Override
	        public void onAutoFocus(boolean success, Camera camera) {
	            Log.d(TAG, "take onAutoFocus: isAutofocus " + Boolean.toString(success));
	        }
	    } );
	    */

		capture.setEnabled(false);
		//setCameraParameters(); // here ?

		//mCamera.startPreview();
		// TODO: bug > 2.3
		mCamera.setPreviewCallback(null);
		mCamera.takePicture(null, null, mPicture);
	}

	OnClickListener captureListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.d(TAG, "onClick Camera");			
			takePicture();
		}
	};

	/*
	 * async mode ?
	 */

	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File pictureFile = getOutputMediaFile();

				outStream = new FileOutputStream(pictureFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + pictureFile.getAbsolutePath());

				//refreshGallery(outFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return null;
		}

	}

	//make picture and save to a folder
	private static File getOutputMediaFile() {
		Log.d(TAG, "getOutputMediaFile");

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Log.d(TAG, "getOutputMediaFile - sdcard mounted");

			//make a new file directory inside the "sdcard" folder
			//File sdCard = Environment.getExternalStorageDirectory();
			File storageDir = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_PICTURES);
			//Log.d(TAG, "getOutputMediaFile - storage is " + getFilesDir());
			File mediaStorageDir = new File (storageDir.getAbsolutePath() + File.separator + "Camremote");
			//File mediaStorageDir = Environment.getExternalStorageDirectory();


			//if this "JCGCamera folder does not exist
			if (!mediaStorageDir.exists()) {
				//if you cannot make this folder return
				if (!mediaStorageDir.mkdirs()) {
					Log.d(TAG, "getOutputMediaFile - cannot create dirs");
					return null;
				}
			}

		//take the current timeStamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		//and make a media file:
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

		return mediaFile;
		} else {
			Log.d(TAG, "getOutputMediaFile - sdcard unmounted");
			return null;
		}
	}

	private void releaseCamera() {
		// stop and release camera
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (action == KeyEvent.ACTION_DOWN) {
				//TODO
				Log.d(TAG, "vol up");
				takePicture();
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (action == KeyEvent.ACTION_DOWN) {
				//TODO
				Log.d(TAG, "vol down");
			}
			return true;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	/*
	 * other callbacks ...
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.cam, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		switch (item.getItemId()) {
		case  R.id.focus :
				return true;
				//break;
		case R.id.btnPrefs:
			Intent intent = new Intent(CamActivity.this, CamremoteSetting.class);
			startActivity(intent);
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	public void setConnected(boolean b) {
		connected = b;
		if (b & (camsocket != null))  {
			Camera.Parameters mParameters = mCamera.getParameters();

			String mode = mParameters.get("whitebalance-values");			
			camsocket.sendBalanceMode(mode);
			mode = mParameters.get("exposure-mode-values");
			camsocket.sendExposureMode(mode);
			
			int w = mParameters.getPreviewSize().width;
			int h = mParameters.getPreviewSize().height;
			camsocket.sendPreviewSize(w,h);
			
			// default is 17 (NV21)
			//Log.d(TAG, "prev format is : " + mParameters.getPreviewFormat());
			
			//TODO:
			/* 
			mPreview.setPreviewSource(this);
			*/
			
		     
			new Thread(new Runnable() {
			    public void run() {
			    	while (connected) {
			    		Log.d(TAG, "poll frame");
			    		try {
							Thread.sleep(120);
							byte[] data = mPreview.getImageBuffer();
				    		sendPreviewFrame(data);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			    		
			    		
			    	}
			    }
			}).start();
			
		}
	}

	public void sendPreviewFrame(byte[] data) {
		if (camsocket != null) 
			camsocket.sendPreview(data);		
	}

	/*
	 * client socket handling
	 */
	/*
	public class ClientTask implements Runnable {

		@Override
		public void run() {
			try {
				InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
				socket = new Socket(serverAddr, SERVERPORT);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}
	*/
}
