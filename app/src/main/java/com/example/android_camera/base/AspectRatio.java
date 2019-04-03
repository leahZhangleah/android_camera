package com.example.android_camera.base;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;

public class AspectRatio implements Comparable<AspectRatio>, Parcelable {
    private final static SparseArrayCompat<SparseArrayCompat<AspectRatio>> sCache = new SparseArrayCompat<>(16);
    private int mX,mY;

    public static AspectRatio of(int x, int y){
        int gcd = gcd(x,y);
        x/=gcd;
        y/=gcd;
        SparseArrayCompat<AspectRatio> arrayX = sCache.get(x);
        if(arrayX==null){
            AspectRatio ratio = new AspectRatio(x,y);
            arrayX=new SparseArrayCompat<>();
            arrayX.put(y,ratio);
            sCache.put(x,arrayX);
            return ratio;
        }else{
            AspectRatio ratio = arrayX.get(y);
            if(ratio==null){
                ratio = new AspectRatio(x,y);
                arrayX.put(y,ratio);
            }
            return ratio;
        }
    }

    private AspectRatio(int x, int y){
        mX = x;
        mY = y;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public boolean matches(Size size){
        int gcd = gcd(size.getWidth(),size.getHeight());
        int x = (size.getWidth())/gcd;
        int y = (size.getHeight())/gcd;
        return mX==x&&mY==y;
    }

    public static AspectRatio parse(String s){
        int position = s.indexOf(":");
        if(position==-1){
            throw new IllegalArgumentException("Malformed aspect ratio: " + s);
        }
        try{
            int x=Integer.parseInt(s.substring(0,position));
            int y = Integer.parseInt(s.substring(position+1));
            return new AspectRatio(x,y);
        }catch (NumberFormatException e){
            throw new IllegalArgumentException("Malformed aspect ratio: " + s, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if(o==null) return false;
        if (this == o) return true;
        if (!(o instanceof AspectRatio)) return false;
        AspectRatio ratio = (AspectRatio) o;
        return mX == ratio.mX &&
                mY == ratio.mY;
    }

    @Override
    public int hashCode() {
        return mY^((mX<<(Integer.SIZE/2))|(mX>>>(Integer.SIZE/2)));
    }

    @Override
    public String toString() {
        return "AspectRatio{" +
                "mX=" + mX +
                ", mY=" + mY +
                '}';
    }

    public float toFloat(){
        return (float) mX/mY;
    }

    @Override
    public int compareTo(@NonNull AspectRatio o) {
        if(equals(o)){
            return 0;
        }else if(toFloat()-o.toFloat()>0){
            return 1;
        }
        return -1;
    }

    public AspectRatio inverse(){
        return AspectRatio.of(mY,mX);
    }

    //gcd: greatest common divisor 最大公约数
    private static int gcd(int a, int b){
        while(b!=0){
            int c = b;
            b = a % b;
            a = c;
        }
        return a;
    }


    /*****parceble methods***/
    protected AspectRatio(Parcel in) {
    }

    public static final Creator<AspectRatio> CREATOR = new Creator<AspectRatio>() {
        @Override
        public AspectRatio createFromParcel(Parcel in) {
            int x=in.readInt();
            int y=in.readInt();
            return AspectRatio.of(x,y);
        }

        @Override
        public AspectRatio[] newArray(int size) {
            return new AspectRatio[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mX);
        dest.writeInt(mY);
    }

}
