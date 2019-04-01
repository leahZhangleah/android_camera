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


    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    private void log(String methodName){
        Log.i(TAG,methodName + "is called");
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        log("surfaceCreated");

        try{
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
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
            mCamera.stopPreview();
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
            setCameraDisplayOrientation(activity,);
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
