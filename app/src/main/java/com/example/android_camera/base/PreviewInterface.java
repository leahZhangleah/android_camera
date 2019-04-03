package com.example.android_camera.base;

import android.view.Surface;

/**
 * Encapsulates all the operations related to camera preview in a backward-compatible manner.
 */
public abstract class PreviewInterface {
    interface Callback{
        void onSurfaceChanged();
    }

    private Callback callback;
    private int mWidth, mHeight;

    void setCallback(Callback callback){
        this.callback = callback;
    }

    abstract Surface getSurface();
}
