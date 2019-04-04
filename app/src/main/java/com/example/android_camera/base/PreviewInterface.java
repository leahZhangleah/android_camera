package com.example.android_camera.base;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

/**
 * Encapsulates all the operations related to camera preview in a backward-compatible manner.
 */
public abstract class PreviewInterface {
    public interface Callback{
        void onSurfaceChanged();
    }

    private Callback callback;
    private int mWidth, mHeight;

    public void setCallback(Callback callback){
        this.callback = callback;
    }

    public abstract Surface getSurface();
    public abstract View getView();
    public abstract Class getOutputClass();
    public abstract void setDisplayOrientation(int displayOrientation);
    public abstract boolean isReady();
    protected void dispatchSurfaceChanged(){
        callback.onSurfaceChanged();
    }

    SurfaceHolder getSurfaceHolder(){return null;}

    public Object getSurfaceTexture(){
        return null;
    }

    public abstract void setBufferSize(int width, int height);

    protected void setSize(int width, int height){
        mWidth = width;
        mHeight=height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
