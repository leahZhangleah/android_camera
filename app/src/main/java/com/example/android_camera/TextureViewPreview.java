package com.example.android_camera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.example.android_camera.base.PreviewInterface;

public class TextureViewPreview extends PreviewInterface {
    private final TextureView mTextureView;
    private int mDisplayOrientation;

    public TextureViewPreview(Context context, ViewGroup parent) {
        final View view = View.inflate(context,R.layout.texture_view,parent);
        mTextureView = view.findViewById(R.id.texture_view);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setSize(width,height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                setSize(width, height);
                configureTransform();
                dispatchSurfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                setSize(0,0);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }



    @Override
    public Surface getSurface() {
        return new Surface(mTextureView.getSurfaceTexture());
    }

    @Override
    public View getView() {
        return mTextureView;
    }

    @Override
    public Class getOutputClass() {
        return SurfaceTexture.class;
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        configureTransform();
    }

    @Override
    public boolean isReady() {
        return mTextureView.getSurfaceTexture()!=null;
    }

    // This method is called only from Camera2.
    @Override
    public void setBufferSize(int width, int height) {
        mTextureView.getSurfaceTexture().setDefaultBufferSize(width, height);
    }

    private void configureTransform(){
        Matrix matrix = new Matrix();
        if(mDisplayOrientation%180 ==90){
            final int width = getWidth();
            final int height = getHeight();
            matrix.setPolyToPoly(
                    new float[]{
                            0f,0f,//top left
                            width,0f,//top right
                            0f,height,//bottom left
                            width,height//bottom right
                    },
                    0,
                    mDisplayOrientation==90?
                            //clockwise rotate 90 degrees
                            new float[]{
                                    0f,height,//top left
                                    0f,0f,//top right
                                    width,height,//bottom left
                                    width,0f//bottom right
                            }://mdisplayorientation = 270
                            //counter-clockwise
                            new float[]{
                                    width,0f,//top left
                                    width,height,//top right
                                    0f,0f,//bottom left
                                    0f, height,//bottom right
                            },
                    0,
                    4
                    );
        }else if(mDisplayOrientation == 180){
            matrix.postRotate(180,getWidth()/2,getHeight()/2);
        }
        mTextureView.setTransform(matrix);
    }
}
