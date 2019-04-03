package com.example.android_camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "LiveCameraView";
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private Activity activity;
    private int currentCameraId;
    private List<Camera.Size> supportedPreviewSizes;
    private Camera.Size mPreviewSize;

    public CameraPreview(Context context, Activity activity, Camera camera,int cameraId) {
        super(context);
        mCamera = camera;
        this.activity = activity;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        currentCameraId = cameraId;
    }

    private void log(String methodName){
        Log.i(TAG,methodName + "is called");
    }

    private void setUpCamera(){
        if(mCamera!=null){
            supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            //If there's something related to size change - requestLayout() should be called,
            // if there's only a visual change without a size changed - you should call invalidate(),  It will result in onDraw being called eventually
            //If you don't tell your View that its size has changed (with a requestLayout method call),
            // then the View will assume it didn't, and the onMeasure and onLayout won't be called.
            requestLayout();

            //todo setup camera parameters
            Camera.Parameters parameters = mCamera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            }

            List<String> flashModes = parameters.getSupportedFlashModes();
            if(flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)){
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            }

            mCamera.setParameters(parameters);
        }
    }


    //horizontal space requirements as imposed by the parent
    //vertical space requirements as imposed by the parent
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //get optimal screen size
        final int width = resolveSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        setMeasuredDimension(width,height);

        if(supportedPreviewSizes!=null){
            mPreviewSize = getOptimalPreviewSize(supportedPreviewSizes,width,height);
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width/height;
        if(sizes==null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;
        // Try to find an size match aspect ratio and size
        for(Camera.Size size: sizes){
            double ratio = size.width / size.height;
            if(Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue; //continue means skip to next iteration directly
            if(Math.abs(size.height - targetHeight) < minDiff){
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if(optimalSize==null){
            minDiff = Double.MAX_VALUE;
            for(Camera.Size size: sizes){
                if(Math.abs(size.height - targetHeight) < minDiff){
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    /*Called from layout when this view should assign a size and position to each of its children.
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(changed){
            final int width = right - left;
            final int height = bottom - top;

            int previewWidth = width;
            int previewHeight = height;
            if(mPreviewSize!=null){
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }
        }
    }*/

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        log("surfaceCreated");

        try{
            if(mCamera!=null){
                mCamera.setPreviewDisplay(surfaceHolder);
                setCameraDisplayOrientation(activity,currentCameraId,mCamera);
                mCamera.startPreview();
            }
        }catch (IOException e){
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    /*
holder	SurfaceHolder: The SurfaceHolder whose surface has changed.
format	int: The new PixelFormat of the surface.
width	int: The new width of the surface.
height	int: The new height of the surface.
This method is always called at least once, after surfaceCreated(SurfaceHolder).
     */

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        log("surfaceChanged");
        if(mSurfaceHolder.getSurface() == null) return;
        // stop preview before making changes
        try{
            if(mCamera!=null){
                mCamera.stopPreview();
            }
        }catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }
        // start preview with new settings

        //todo:new setting for preview, i.e. device rotation, calculate the previewsize etc.
        //mCamera.getParameters().set

        //todo:initial set for camera, i.e. previewSize,
        List<Camera.Size> previewSizeList = mCamera.getParameters().getSupportedPreviewSizes();
        printSupportedSize(previewSizeList,"previewSize");
        List<Camera.Size> pictureSizeList = mCamera.getParameters().getSupportedPictureSizes();
        printSupportedSize(pictureSizeList,"pictureSize");
        List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
        printSupportedFormats(previewFormats,"previewFormats");
        List<Integer> pictureFormats = mCamera.getParameters().getSupportedPictureFormats();
        printSupportedFormats(pictureFormats,"pictureFormats");

        try{
            mCamera.setPreviewDisplay(mSurfaceHolder);
            setCameraDisplayOrientation(activity,currentCameraId,mCamera);
            mCamera.startPreview();
        }catch (IOException e){
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }


    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera){
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId,info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation){
            case Surface.ROTATION_0:degrees = 0; break;
            case Surface.ROTATION_90:degrees = 90; break;
            case Surface.ROTATION_180:degrees = 180; break;
            case Surface.ROTATION_270:degrees=270;break;
        }

        int result;
        if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        }else{
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        log("surfaceDestroyed");
        //mCamera.stopPreview();
        //mCamera.release();
        //todo:empty. Take care of releasing the Camera preview in your activity.
        if(mCamera!=null){
            mCamera.stopPreview();
        }
    }

    private <T> void printSupportedSize(List<T> sizes,String type){
        Camera.Size result = null;
        for (int i=0;i<sizes.size();i++){
            result = (Camera.Size) sizes.get(i);
            Log.i(type, "Supported Size. Width: " + result.width + "height : " + result.height);
        }
    }

    private <T> void printSupportedFormats(List<T> formats,String type){
        Integer result = null;
        for (int i=0;i<formats.size();i++){
            result = (Integer) formats.get(i);
            Log.i(type, result.toString());
        }
    }

}
