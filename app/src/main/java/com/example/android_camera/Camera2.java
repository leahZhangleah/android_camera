package com.example.android_camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import com.example.android_camera.base.AspectRatio;
import com.example.android_camera.base.CameraViewInterface;
import com.example.android_camera.base.Constants;
import com.example.android_camera.base.Constants.*;
import com.example.android_camera.base.PreviewInterface;
import com.example.android_camera.base.SizeMap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;

import static com.example.android_camera.base.Constants.*;

@TargetApi(21)
public class Camera2 extends CameraViewInterface {
    private static final String TAG = "Camera2";
    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();

    static {
        INTERNAL_FACINGS.put(FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
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
    private AspectRatio mAspectRatio = DEFAULT_ASPECT_RATIO;
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
        collectCameraInfo(); //define preview and picture sizes
        prepareImageReader();
        startOpeningCamera(); //open camera
        return true;
    }

    private void prepareImageReader() {
        if(mImageReader!=null){
            mImageReader.close();
        }

        com.example.android_camera.base.Size largest = mPictureSizes.getSizes(mAspectRatio).last(); //get largest picture size
        mImageReader = ImageReader.newInstance(largest.getWidth(),largest.getHeight(),ImageFormat.JPEG,2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,null);
    }

    /**
     * <p>Starts opening a camera device.</p>
     * <p>The result will be processed in {@link #mCameraDeviceStateCallback}.</p>
     */
    private void startOpeningCamera() {
        try{
            cameraManager.openCamera(mCameraId,mCameraDeviceStateCallback,null);
        }catch (CameraAccessException e){
            throw new RuntimeException("Failed to open camera: " + mCameraId, e);
        }catch (SecurityException e){
            Log.d(TAG,"user doesn't give permission");
        }
    }

    private boolean chooseCameraIdByFacing() {
        try{
            int internalFacing = INTERNAL_FACINGS.get(mFacing);
            final String[] ids = cameraManager.getCameraIdList();
            if(ids.length==0){//no camera
                throw new RuntimeException("No camera available.");
            }
            for(String id:ids){
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if(level==null||level==CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
                    continue;
                }
                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(internal==null){
                    throw new NullPointerException("Unexpected state: LENS_FACING null");
                }
                if(internal==internalFacing){
                    mCameraId=id;
                    mCameraCharacteristics=characteristics;
                    return true;
                }
            }
            //not found
            mCameraId=ids[0];
            mCameraCharacteristics = cameraManager.getCameraCharacteristics(mCameraId);
            Integer level = mCameraCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null ||
                    level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                return false;
            }
            Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (internal == null) {
                throw new NullPointerException("Unexpected state: LENS_FACING null");
            }
            for(int i=0,count=INTERNAL_FACINGS.size();i<count;i++){
                if(INTERNAL_FACINGS.valueAt(i)==internal){
                    mFacing = INTERNAL_FACINGS.keyAt(i);
                    return true;
                }
            }
            // The operation can reach here when the only camera device is an external one.
            // We treat it as facing back.
            mFacing= FACING_BACK;
            return true;
        }catch (CameraAccessException e){
            throw new RuntimeException("Failed to get a list of camera devices", e);
        }
    }

    /**
     * <p>Collects some information from {@link #mCameraCharacteristics}.</p>
     * <p>This rewrites {@link #mPreviewSizes}, {@link #mPictureSizes}, and optionally,
     * {@link #mAspectRatio}.</p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(map==null){
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }
        mPreviewSizes.clear();
        for(Size size:map.getOutputSizes(mPreview.getOutputClass())){
            int width=size.getWidth();
            int height=size.getHeight();
            if(width<=MAX_PREVIEW_WIDTH&&height<=MAX_PREVIEW_HEIGHT){
                mPreviewSizes.add(new com.example.android_camera.base.Size(width,height));
            }
        }
        mPictureSizes.clear();
        collectPictureSizes(mPictureSizes,map);
        for(AspectRatio ratio:mPreviewSizes.getRatios()){
            if(!mPictureSizes.getRatios().contains(ratio)){
                mPreviewSizes.remove(ratio);
            }
        }
        if(!mPreviewSizes.getRatios().contains(mAspectRatio)){
            mAspectRatio = mPreviewSizes.getRatios().iterator().next();
        }
    }

    protected void collectPictureSizes(SizeMap mPictureSizes, StreamConfigurationMap map) {
        for(Size size:map.getOutputSizes(ImageFormat.JPEG)){
            mPictureSizes.add(new com.example.android_camera.base.Size(size.getWidth(),size.getHeight()));
        }
    }

    /**
     * <p>Starts a capture session for camera preview.</p>
     * <p>This rewrites {@link #mPreviewRequestBuilder}.</p>
     * <p>The result will be continuously processed in {@link #mSessionCallback}.</p>
     */
    private void startCaptureSession() {
        if(!isCameraOpened()||!mPreview.isReady()||mImageReader==null)return;
        com.example.android_camera.base.Size previewSize = chooseOptimalSize();
        mPreview.setBufferSize(previewSize.getWidth(),previewSize.getHeight());
        Surface surface = mPreview.getSurface();
        try {
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    mSessionCallback, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to start camera session");
        }
    }

    private com.example.android_camera.base.Size chooseOptimalSize() {
        int surfaceLonger, surfaceShorter;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }
        SortedSet<com.example.android_camera.base.Size> candidates = mPreviewSizes.getSizes(mAspectRatio);

        // Pick the smallest of those big enough
        for (com.example.android_camera.base.Size size : candidates) {
            if (size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
        return candidates.last();
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
        //reset capture session
        if(mCaptureSession!=null){
            mCaptureSession.close();
            mCaptureSession=null;
            startCaptureSession();
        }
        return true;
    }

    @Override
    public AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    public void setAutoFocus(boolean autoFocus) {
        if(mAutoFocus==autoFocus)return;
        mAutoFocus=autoFocus;
        if(mPreviewRequestBuilder!=null){
            updateAutoFocus();
            if(mCaptureSession!=null){
                try{
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),mCaptureCallback,null);
                }catch (CameraAccessException e){
                    mAutoFocus=!mAutoFocus; //revert
                }
            }
        }
    }

    @Override
    public boolean getAutoFocus() {
        return mAutoFocus;
    }

    private void updateAutoFocus() {
        if (mAutoFocus) {
            int[] modes = mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus is not supported
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        } else {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    @Override
    public void setFlash(int flash) {
        if(mFlash==flash) return;
        int saved = mFlash;
        mFlash = flash;
        if(mPreviewRequestBuilder!=null){
            try{
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),mCaptureCallback,null);
            }catch (CameraAccessException e){
                mFlash = saved; //revert
            }
        }
    }

    @Override
    public int getFlash() {
        return mFlash;
    }

    private void updateFlash() {
        switch (mFlash) {
            case FLASH_OFF:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case FLASH_ON:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case FLASH_TORCH:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                break;
            case FLASH_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case FLASH_RED_EYE:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
        }
    }

    @Override
    public void takePicture() {
        if(mAutoFocus) lockFocus();
        captureStillPicture();
    }

    private void captureStillPicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            switch (mFlash) {
                case FLASH_OFF:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                    break;
                case FLASH_ON:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    break;
                case FLASH_TORCH:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);
                    break;
                case FLASH_AUTO:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case FLASH_RED_EYE:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
            // Calculate JPEG orientation.
            @SuppressWarnings("ConstantConditions")
            int sensorOrientation = mCameraCharacteristics.get(
                    CameraCharacteristics.SENSOR_ORIENTATION);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    (sensorOrientation +
                            mDisplayOrientation * (mFacing == FACING_FRONT ? 1 : -1) +
                            360) % 360);
            // Stop preview and capture a still picture.
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            unlockFocus();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot capture a still picture.", e);
        }
    }


    private void lockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mCaptureCallback.setState(PictureCaptureCallback.STATE_LOCKING);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to lock focus.", e);
        }
    }

    private void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            updateAutoFocus();
            updateFlash();
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    null);
            mCaptureCallback.setState(PictureCaptureCallback.STATE_PREVIEW);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to restart camera preview.", e);
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        mPreview.setDisplayOrientation(mDisplayOrientation);
    }
}
