package com.example.android_camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MediaActivity extends AppCompatActivity {
    ImageView captureImg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        captureImg = findViewById(R.id.captured_image);

        Intent intent = getIntent();
        String filePath = intent.getStringExtra("imageByte");

        Bitmap bmp = BitmapFactory.decodeFile(filePath);
        captureImg.setImageBitmap(bmp);
    }
}
