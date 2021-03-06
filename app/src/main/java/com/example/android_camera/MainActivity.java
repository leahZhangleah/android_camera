package com.example.android_camera;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Camera camera;
    private CameraPreview mPreview;
    private VideoRecord videoRecord;
    private boolean isRecording;
    FrameLayout frameLayout;
    int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        frameLayout = findViewById(R.id.camera_preview);

        Button captureBtn = findViewById(R.id.button_capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("captureBtn");
                if(camera!=null){
                    camera.takePicture(null,null,pictureCallback);
                }
            }
        });

        final Button videoBtn = findViewById(R.id.button_video);
        videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("videoBtn");
                if(videoRecord!=null){
                    if(isRecording){
                        videoRecord.stopRecording();

                        isRecording= false;
                    }else{
                        videoRecord.startRecording();
                        videoBtn.setText("Stop");
                        isRecording=true;
                    }
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
        if(camera==null){
            //访问相机资源
            camera = getCameraInstance();
            //创建预览界面
            mPreview = new CameraPreview(this,this,camera,switchCamera());
            //将预览界面添加到布局页面中
            frameLayout.addView(mPreview);


            File videoOutputFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
            videoRecord = new VideoRecord(camera,videoOutputFile,mPreview);
        }

    }

    private void cameraInstantiation(){
        if(camera==null){
            //访问相机资源
            camera = getCameraInstance();
            //创建预览界面
            mPreview = new CameraPreview(this,this,camera,switchCamera());
            //将预览界面添加到布局页面中
            frameLayout.addView(mPreview);

            File videoOutputFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
            videoRecord = new VideoRecord(camera,videoOutputFile,mPreview);
        }

    }


    //拍照监听
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            log("onPictureTaken");
            //get a file for the taken pic
            File picFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if(picFile==null){
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }
            //将拍摄的照片保存到文件中,并在下一个ACTIVITY中显示
            try{
                FileOutputStream fos = new FileOutputStream(picFile);
                fos.write(data);
                fos.close();
                consumeImgByte(picFile.getAbsolutePath());
            }catch (FileNotFoundException e){
                Log.d(TAG, "File not found: " + e.getMessage());
            }catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            //todo
        }
    };

    //不能直接将byte[]传给下一个activity,因为size太大
    private void consumeImgByte(String filePath) {
        Intent intent = new Intent(this,MediaActivity.class);
        intent.putExtra("imageByte",filePath);
        startActivity(intent);
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        log("getoutputMediaFile");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyCameraApp");
        if(!mediaStorageDir.exists()){
            if(!mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if(type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath()+File.separator+"IMG_"+timeStamp+".jpg");
        }else if(type == MEDIA_TYPE_VIDEO){
            mediaFile = new File(mediaStorageDir.getPath()+File.separator+"VID_"+timeStamp+".mp4");
        }else{
            return null;
        }
        return mediaFile;
    }

    //调换镜头
    private int switchCamera(){
        if(Camera.getNumberOfCameras()>1){
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId,info);
            if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            }else if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            }

        };
       return cameraId;
    }


    //If your application does not specifically require a camera using a manifest declaration,
    // you should check to see if a camera is available at runtime.
    //todo since we have specified in manifest, we don't need to check here
    private boolean checkCameraHardware(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) return true;
        return false;
    }

    //get an instance of camera
    private Camera getCameraInstance(){
        log("getCameraInstance");
        Camera camera = null;
        try{
            camera = Camera.open(cameraId);
            //get it cameraId as parameter to specify which camera to open 0=rear. 1=front, maybe more
        }catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return camera; // returns null if camera is unavailable
    }

    @Override
    protected void onPause() {
        log("onpause");
        releaseCamera();
        super.onPause();
    }

    private void releaseCamera(){
        if(camera!=null){
            camera.release();
            camera=null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop");
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        log("onRestart");
    }

    private void log(String methodName){
        Log.i(TAG,methodName + "is called");
    }
}
