package net.dnsalias.vbr.camremote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private byte[] mLastFrame = null;
    //private LinkedList<byte[]> mQueue = new LinkedList<byte[]>();
  //Creating shared object
    private BlockingQueue<byte[]> mQueue = new LinkedBlockingQueue<byte[]>();
    private int width;
	private int height;
	
	private CamActivity datasource;
	
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        // for drawing ?
        //setWillNotDraw(false);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // create the surface and start camera preview
            if (mCamera == null) {
            	mCamera.setPreviewCallback(mPreviewCallback);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                
               
            }
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void refreshCamera(Camera camera) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        setCamera(camera);

        try {
        	mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.3; // from stackoverflow ? 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Log.d(TAG, "INFO: getOptimalPreviewSize fitting size : " + h + "," + w );
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            Log.d(TAG, "INFO: getOptimalPreviewSize test size : " + size.height + "," + size.width);
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            Log.d(TAG, "INFO: getOptimalPreviewSize optimal not found");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                Log.d(TAG, "INFO: getOptimalPreviewSize try size : " + size.height + "," + size.width);
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
 
/*
 * real workhorse of picture
 */
    private int getDisplayOrientation() {
    	Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        return degrees;
    }
    
public void setCameraParameters(int w,int h) {
	Log.d(TAG, "setCameraParameters - in");
	Camera.Parameters mParameters = mCamera.getParameters();

	// preview info
    List<Size> sizes = mParameters.getSupportedPreviewSizes();
    int i = 0;
    for (Camera.Size cs : sizes) {
        Log.d(TAG, "Camera - preview supports:(" + (i++) + ") " + cs.width + "x" + cs.height);
    }
    //TODO: 
    Size optimalSize = getOptimalPreviewSize(sizes, w, h);
    Log.d(TAG, "INFO: optimal size : " + optimalSize.height + "," + optimalSize.width);

    
    int degrees = getDisplayOrientation();

    mCamera.setDisplayOrientation(degrees);
    Log.d(TAG, "INFO: orient " + degrees);

    //parameters.setJpegQuality(100);//a value between 1 and 100

    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO );
    mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
    // TODO: setup preview size
    this.width = optimalSize.width;
    this.height = optimalSize.height;
    
   /* was autofocus here */

    
	// get the best picture size ...
	sizes = mParameters.getSupportedPictureSizes();
	i = 0;
	//int dim = 0;
	for (Camera.Size cs : sizes) {
		//if(cs.width  >= 1024 && cs.height >= 768) dim = i;
		Log.d(TAG, "setCameraParameters - supports:(" + (i++) + ") " + cs.width + "x" + cs.height);
	}
	Size size = sizes.get(0); // TODO : better
	mParameters.setPictureSize(size.width, size.height);

	Size cs = mParameters.getPictureSize();
	Log.d(TAG, "setCameraParameters - current size : " + cs.width + " x " + cs.height );
	Log.d(TAG, "setCameraParameters - current focus : " + mParameters.getFocusMode());
	Log.d(TAG, "setCameraParameters - current expo  : " + mParameters.getExposureCompensation());
	Log.d(TAG, "setCameraParameters - current zoom  : " + mParameters.getZoom());

	Log.d(TAG, "Supported Exposure Modes:" + mParameters.get("exposure-mode-values"));    
	Log.d(TAG, "Supported White Balance Modes:" + mParameters.get("whitebalance-values"));
	Log.d(TAG, "Exposure setting = " + mParameters.get("exposure"));
	Log.d(TAG, "White Balance setting = " + mParameters.get("whitebalance")); 
	
	mParameters.setJpegQuality(100);//a value between 1 and 100
	mParameters.setPictureFormat(PixelFormat.JPEG);
	mCamera.setParameters(mParameters);
	
	 /* test */
    mCamera.autoFocus(new Camera.AutoFocusCallback(){
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            Log.d(TAG, "onAutoFocus: isAutofocus " + Boolean.toString(success));
        }
    } );
	
}

    public void setupCamera(Camera camera,int w,int h) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        setCamera(camera);
        
        setCameraParameters(w,h);

        try {
        	mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        setupCamera(mCamera,w,h);
        
       
        
    }

    public void setCamera(Camera camera) {
        //method to set a camera instance
        mCamera = camera;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        // mCamera.release();

    }

    @Override 
    public void onDraw(Canvas canvas) { 

        //drawGuidance
    	Log.d(TAG, "onDraw");

    } 
    
    public void resetBuff() {

        synchronized (mQueue) {
            mQueue.clear();
            //mLastFrame = null;
        }
    }
    
    public byte[] getImageBuffer() {
    	/*
        synchronized (mQueue) {
			if (mQueue.size() > 0) {
				Log.d(TAG, " Q size " + mQueue.size());
				mLastFrame = mQueue.poll();
			}
    	}*/
    	try {
			mLastFrame = mQueue.take();
		} catch (InterruptedException e) {			
			e.printStackTrace();			
		}        
        return mLastFrame;
    }
    
    /*
     * TODO: maybo use 
     * setPreviewCallbackWithBuffer(Camera.PreviewCallback)
     * and  addCallbackBuffer(byte[])
     * 
     */
    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
    	private long timestamp=0;
    	
    	@Override
    	public void onPreviewFrame(byte[] data, Camera camera) {            
    		//Log.d(TAG, "onPreviewFrame");
    		//TODO: better way, too slow...
    		/*
    		Log.d("onPreviewFrame","Time Gap = "+(System.currentTimeMillis()-timestamp));
    		timestamp=System.currentTimeMillis();
*/
    		/*
            if (datasource!=null) 
            	datasource.sendPreviewFrame(data);
    		 */
    		if (width != 0) {
    			ByteArrayOutputStream out = new ByteArrayOutputStream();
    			YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
    			yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
    			byte[] imageBytes = out.toByteArray();
    			/*
            synchronized (mQueue) {            	
                if (mQueue.size() == MAX_BUFFER) {
                    //mQueue.poll();
                	mQueue.clear();
                }
                mQueue.add(imageBytes);
            } *//*
    			try {
    				mQueue.put(imageBytes);
    			} catch (InterruptedException e) {				
    				e.printStackTrace();
    			}*/

    		}
    	}
    };

	

    // set the source output for frame data
	public void setPreviewSource(CamActivity camActivity) {
		datasource = camActivity;		
	}
    
/* 
 * TODO for preview :
 
 http://www.codepool.biz/tech-frontier/android/making-android-smart-phone-a-remote-ip-camera.html
 https://github.com/DynamsoftRD/Android-IP-Camera
	
	*/
}

