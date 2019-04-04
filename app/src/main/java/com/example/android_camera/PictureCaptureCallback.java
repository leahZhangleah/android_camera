package com.example.android_camera;

import android.graphics.Paint;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

/**
 * A {@link CameraCaptureSession.CaptureCallback} for capturing a still picture.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class PictureCaptureCallback extends CameraCaptureSession.CaptureCallback {
    static final int STATE_PREVIEW = 0;
    static final int STATE_LOCKING = 1;
    static final int STATE_LOCKED = 2;
    static final int STATE_PRECAPTURE = 3;
    static final int STATE_WAITING = 4;
    static final int STATE_CAPTURING = 5;

    private int mState;

    public PictureCaptureCallback() {
    }

    public void setState(int state) {
        this.mState = state;
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session,@NonNull CaptureRequest request,  @NonNull CaptureResult partialResult) {
        process(partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        process(result);
    }

    private void process(@NonNull CaptureResult result){
        switch (mState){
            case STATE_LOCKING:
                Integer af= result.get(CaptureResult.CONTROL_AF_STATE);
                if(af==null)break;
                if(af==CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED||
                    af==CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if(ae==null||ae==CaptureResult.CONTROL_AE_STATE_CONVERGED){
                        setState(STATE_CAPTURING);
                        onReady();
                    }else{
                        setState(STATE_LOCKED);
                        onPrecaptureRequired();
                    }
                }
                break;
            case STATE_PRECAPTURE:
                Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                if(ae==null||ae==CaptureResult.CONTROL_AE_STATE_PRECAPTURE||
                ae== CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED||
                ae==CaptureResult.CONTROL_AE_STATE_CONVERGED){
                    setState(STATE_WAITING);
                }
                break;
            case STATE_WAITING:
                Integer auto_exp = result.get(CaptureResult.CONTROL_AE_STATE);
                if(auto_exp==null||auto_exp!=CaptureResult.CONTROL_AE_STATE_PRECAPTURE){
                    setState(STATE_CAPTURING);
                    onReady();
                }
                break;


        }
    }

    public abstract void onReady();
    public abstract void onPrecaptureRequired();
}