package com.example.android_camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseIntArray;

import com.example.android_camera.base.AspectRatio;
import com.example.android_camera.base.CameraViewInterface;
import com.example.android_camera.base.Constants;
import com.example.android_camera.base.PreviewInterface;
import com.example.android_camera.base.SizeMap;

import java.nio.ByteBuffer;
import java.util.Set;

@TargetApi(21)
public class Camera2 extends CameraViewInterface {
    private static final String TAG = "Camera2";
    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();

    static {
        INTERNAL_FACINGS.put(Constants.FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(Constants.FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    private final CameraManager cameraManager;
    private String mCameraId;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraDevice mCamera;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private ImageReader mImageReader;
    private final SizeMap mPreviewSizes = new SizeMap();
    private final SizeMap mPictureSizes = new SizeMap();
    private int mFacing;
    private AspectRatio mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
    private boolean mAutoFocus;
    private int mFlash;
    private int mDisplayOrientation;
    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            mCallback.onCameraOpened();
            startCaptureSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            super.onClosed(camera);
            mCallback.onCameraClosed();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera=null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera=null;
        }
    };

    private final CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if(mCamera==null)return;
            mCaptureSession=session;
            updateAutoFocus();
            updateFlash();
            try{
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        mCaptureCallback,null);
            }catch (CameraAccessException e){
                Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
            }catch (IllegalStateException e){
                Log.e(TAG, "Failed to start camera preview.", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure capture session.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            
        }
    };

    PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {
        @Override
        public void onReady() {
            captureStillPicture();
        }

        @Override
        public void onPrecaptureRequired() {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            setState(STATE_PRECAPTURE);
            try{
                mCaptureSession.capture(mPreviewRequestBuilder.build(),this,null);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
            }catch (CameraAccessException e){
                Log.e(TAG, "Failed to run precapture sequence.", e);
            }
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try(Image image = reader.acquireNextImage()){
                Image.Plane[] planes = image.getPlanes();
                if(planes.length>0){
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    mCallback.onPictureTaken(data);
                }
            }
        }
    };

    public Camera2(Callback mCallback, PreviewInterface mPreview, Context context) {
        super(mCallback, mPreview);
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mPreview.setCallback(new PreviewInterface.Callback() {
            @Override
            public void onSurfaceChanged() {
                startCaptureSession();
            }
        });
    }

    @Override
    public boolean start() {
        if(!chooseCameraIdByFacing()){
            return false;
        }
        collectCameraInfo();
        prepareImageReader();
        startOpeningCamera();
        return true;
    }

    @Override
    public void stop() {
        if(mCaptureSession!=null){
            mCaptureSession.close();
            mCaptureSession=null;
        }
        if(mCamera!=null){
            mCamera.close();
            mCamera=null;
        }
        if(mImageReader!=null){
            mImageReader.close();
            mImageReader=null;
        }
    }

    @Override
    public boolean isCameraOpened() {
        return mCamera!=null;
    }

    @Override
    public void setFacing(int facing) {
        if(mFacing==facing)return;
        mFacing=facing;
        if(isCameraOpened()){
            stop();
            start();
        }
    }

    @Override
    public int getFacing() {
        return mFacing;
    }

    @Override
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mPreviewSizes.getRatios();
    }

    @Override
    public boolean setAspectRatio(AspectRatio ratio) {
        if(ratio==null||ratio.equals(mAspectRatio)||!mPreviewSizes.getRatios().contains(ratio)) return false;
        mAspectRatio=ratio;
        prepareImageReader();
    }

    @Override
    public AspectRatio getAspectRatio() {
        return null;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {

    }

    @Override
    public boolean getAutoFocus() {
        return false;
    }

    @Override
    public void setFlash(int flash) {

    }

    @Override
    public int getFlash() {
        return 0;
    }

    @Override
    public void takePicture() {

    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {

    }
}
