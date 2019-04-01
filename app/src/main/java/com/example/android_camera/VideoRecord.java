package com.example.android_camera;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class VideoRecord {
    private static final String TAG = "VideoRecord";
    Camera camera;
    MediaRecorder mediaRecorder;
    File videoOutputFile;
    CameraPreview mPreview;

    public VideoRecord(Camera camera, File videoOutputFile, CameraPreview mPreview) {
        this.camera = camera;
        this.videoOutputFile = videoOutputFile;
        this.mPreview = mPreview;
    }

    private boolean prepareVideoRecorder(){
        // Step 1:set camera to MediaRecorder
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mediaRecorder.setOutputFile(videoOutputFile.getPath());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try{
            mediaRecorder.prepare();
        }catch (IOException e){
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder(){
        if(mediaRecorder!=null){
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder=null;
        }
    }

    public void stopRecording(){
        mediaRecorder.stop();
        releaseMediaRecorder();
    }

    public void startRecording(){
        if(prepareVideoRecorder()){
            mediaRecorder.start();
        }else{
            // prepare didn't work, release the camera
            releaseMediaRecorder();
        }
    }
}
