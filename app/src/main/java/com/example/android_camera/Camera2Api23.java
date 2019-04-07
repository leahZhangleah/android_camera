package com.example.android_camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.params.StreamConfigurationMap;
import com.example.android_camera.base.Size;

import com.example.android_camera.base.PreviewInterface;
import com.example.android_camera.base.SizeMap;
@TargetApi(23)
public class Camera2Api23 extends Camera2 {
    public Camera2Api23(Callback mCallback, PreviewInterface mPreview, Context context) {
        super(mCallback, mPreview, context);
    }

    @Override
    protected void collectPictureSizes(SizeMap sizes, StreamConfigurationMap map) {
        // Try to get hi-res output sizes
        android.util.Size[] outputSizes = map.getHighResolutionOutputSizes(ImageFormat.JPEG);
        if (outputSizes != null) {
            for (android.util.Size size : map.getHighResolutionOutputSizes(ImageFormat.JPEG)) {
                sizes.add(new Size(size.getWidth(), size.getHeight()));
            }
        }

        if (sizes.isEmpty()) {
            super.collectPictureSizes(sizes, map);
        }
    }

}
