package com.example.android_camera.base;

import android.view.View;

import java.util.Set;

public abstract class CameraViewInterface {
    public interface Callback{
        void onCameraOpened();
        void onCameraClosed();
        void onPictureTaken(byte[] data);
    }

    protected final Callback mCallback;

    protected final PreviewInterface mPreview;

    public CameraViewInterface(Callback mCallback, PreviewInterface mPreview) {
        this.mCallback = mCallback;
        this.mPreview = mPreview;
    }

    View getView(){return mPreview.getView();}

    public abstract boolean start();
    public abstract void stop();
    public abstract boolean isCameraOpened();
    public abstract void setFacing(int facing);
    public abstract int getFacing();
    public abstract Set<AspectRatio> getSupportedAspectRatios();
    public abstract boolean setAspectRatio(AspectRatio ratio);
    public abstract AspectRatio getAspectRatio();
    public abstract void setAutoFocus(boolean autoFocus);
    public abstract boolean getAutoFocus();
    public abstract void setFlash(int flash);
    public abstract int getFlash();
    public abstract void takePicture();
    public abstract void setDisplayOrientation(int displayOrientation);
}
