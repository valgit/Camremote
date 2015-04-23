package net.dnsalias.vbr.camremote;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // create the surface and start camera preview
            if (mCamera == null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.d(VIEW_LOG_TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void refreshCamera(Camera camera,int w,int h) {
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

        Camera.Parameters parameters = mCamera.getParameters();

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        int i = 0;
        for (Camera.Size cs : sizes) {
            Log.d(TAG, "Camera - preview supports:(" + (i++) + ") " + cs.width + "x" + cs.height);
        }
        Size optimalSize = getOptimalPreviewSize(sizes, h, w);
        Log.d(TAG, "INFO: optimal size : " + optimalSize.height + "," + optimalSize.width);

        Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        //Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 90; break;
            case Surface.ROTATION_90: degrees = 180; break;
            case Surface.ROTATION_180: degrees = 270; break;
            case Surface.ROTATION_270: degrees = 0; break;
        }

        mCamera.setDisplayOrientation(degrees);
        Log.d(TAG, "INFO: orient " + degrees);

        //parameters.setJpegQuality(100);//a value between 1 and 100

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        //parameters.set("jpeg-quality", 100);
        //parameters.setPreviewSize(w, h);

        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        mCamera.setParameters(parameters);
        //camera.startPreview();

        //-- Must add the following callback to allow the camera to autofocus.
        mCamera.autoFocus(new Camera.AutoFocusCallback(){
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Log.d(TAG, "onAutoFocus: isAutofocus " + Boolean.toString(success));
            }
        } );

        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        refreshCamera(mCamera,w,h);
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

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
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
}

