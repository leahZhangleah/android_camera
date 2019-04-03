package com.example.android_camera.base;

import android.support.annotation.NonNull;

public class Size implements Comparable<Size> {
    private final int mWidth,mHeight;

    public Size(int mWidth, int mHeight) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }


    @Override
    public boolean equals(Object o) {
        if(o==null)return false;
        if (this == o) return true;
        if (!(o instanceof Size)) return false;
        Size size = (Size) o;
        return mWidth == size.mWidth &&
                mHeight == size.mHeight;
    }

    @Override
    public String toString() {
        return "Size{" +
                "mWidth=" + mWidth +
                ", mHeight=" + mHeight +
                '}';
    }

    @Override
    public int hashCode() {
        //|, is bitwise or
        //<<,>>> bitwise operator, left shift or unsinged right shift on binary values of the integer
        return mHeight^((mWidth<<(Integer.SIZE/2))|(mWidth>>>(Integer.SIZE/2)));
    }

    @Override
    public int compareTo(@NonNull Size o) {
        return mWidth*mHeight-o.mWidth*o.mHeight;
    }
}
