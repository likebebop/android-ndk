package com.linecorp.kuru.utils;

import android.graphics.Bitmap;

/**
 * Created by likebebop on 2017. 9. 11..
 */

public class NativeImageUtils {
    static {
        System.loadLibrary("imageutils");
    }

    public native static long malloc(int size);
    public native static void free(long addr);
    public native static void renderToBitmap(Bitmap bitmap, long nativeRgbArray);

    public native static void fillTestRgb(long nativeRgbArray, int width, int height);

    public native static byte[] rgbArrayToNv21(long nativeRgbArray, int width, int height);
    public native static void yuvToRgb(long nativeYuvArray, long nativeRgbArray, int width, int height);
}
