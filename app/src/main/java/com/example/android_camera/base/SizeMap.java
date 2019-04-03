package com.example.android_camera.base;

import android.support.v4.util.ArrayMap;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SizeMap {
    private final ArrayMap<AspectRatio, SortedSet<Size>> mRatios = new ArrayMap<>();

    public boolean add(Size size){
        for(AspectRatio ratio: mRatios.keySet()){
            if(ratio.matches(size)){
                final SortedSet<Size> sizes = mRatios.get(ratio);
                if(sizes.contains(size)){
                    return false;
                }else{
                    sizes.add(size);
                    return true;
                }
            }
        }

        // None of the existing ratio matches the provided size; add a new key
        SortedSet<Size> sizes = new TreeSet<>();
        sizes.add(size);
        mRatios.put(AspectRatio.of(size.getWidth(),size.getHeight()),sizes);
        return true;
    }

    public void remove(AspectRatio ratio){
        mRatios.remove(ratio);
    }

    public Set<AspectRatio> getRatios() {
        return mRatios.keySet();
    }

    public SortedSet<Size> getSizes(AspectRatio ratio){
        return mRatios.get(ratio);
    }

    void clear(){
        mRatios.clear();
    }

    boolean isEmpty(){
        return mRatios.isEmpty();
    }
}
