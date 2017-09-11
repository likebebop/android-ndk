package com.linecorp.android.common.jpegturbo;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * @author Han SangChul ( hanburn@linecorp.com )
 */
public class JpegTurbo {

    protected static volatile boolean isLoaded = false; // 로딩성공하면 native(libjpegturbo.so)에서 true로 설정한다.

    static {
        loadLibrary();
    }
static final String TAG = "likebebop";
    @SuppressWarnings("deprecation")
    public static void loadLibrary() {
        if (isLoaded) {
            return;
        }

        if (isLoaded == false) {
            try {
                System.loadLibrary("jpegturbo");
            } catch (Throwable e) {
                //CustomToastHelper.showWarn(R.string.failed_to_load_library);
                isLoaded = false;
                Log.w(TAG, e);
            } finally {
                if (isLoaded) {
                    Log.d(TAG, "load jpegturbo.so CPU_ABI:" + Build.CPU_ABI);
                } else {
                    Log.d(TAG, "load fail jpegturbo.so CPU_ABI:" + Build.CPU_ABI);
                }
            }
        }
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Compress
     * Bitmap -> File
     */
    static public void compressSafely(Bitmap bm, int quality, String outPath) throws Exception {
        checkBitmap(bm);
        checkQuality(quality);
        checkOutPath(outPath);

        if (!isLoaded()) {
            compressWithAndroid(bm, quality, outPath);
            return;
        }

        // native call
        nativeCompressFromBitmapToFile(bm, quality, outPath);
    }

    static private void compressWithAndroid(Bitmap bm, int quality, String outPath) throws IOException {
        File file = new File(outPath);
        FileOutputStream fos = new FileOutputStream(file);
        bm.compress(Bitmap.CompressFormat.JPEG, quality, fos);
        if (fos != null) {
            fos.close();
        }
    }

    static public Bitmap decodeByteArraySafely(final byte[] data, int length) throws Exception {

        int inSamplingSize = 1; // 원본 크기 그대로 읽는다.
        Rect bounds = JpegTurbo.nativeDecompressBoundsFromByteArray(data, length);
        bounds.right /= inSamplingSize;
        bounds.bottom /= inSamplingSize;
        int width = bounds.width();
        int height = bounds.height();

        return nativeDecompressFromByteArray(data, length, width, height);
    }

    private static void checkOutPath(String outPath) {

        try {
            File file = new File(outPath);
            FileOutputStream fos = new FileOutputStream(file);
            fos.close();
            file.deleteOnExit();
        } catch (Exception e) {
            throw new IllegalArgumentException("outPath is not writable:" + outPath);
        }
    }

    private static void checkBitmap(Bitmap bm) {
        if (bm == null || bm.isRecycled()) {
            throw new IllegalArgumentException("Invalid argument in Source bitmap");
        }
    }

    private static void checkQuality(int quality) {
        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException("Invalid argument in JPEG quality");
        }
    }

    /**
     * nativeDecompress 에서 호출된다.
     */
    static protected Bitmap createBitmap(int width, int height) {
        try {
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError error) {

        } catch (Exception e) {
        }
        return null;
    }


    ///////////////////////////////////////////////////////////////////////
    // native declaration

    // jpeg 인코딩 결과 버퍼의 충분한 사이즈를 계산한다. 즉 dst버퍼는 이 크기를 가져야 한다.
    //public native static int nativeBufSize(int width, int height, int jpegSubsamp) throws Exception;
    //private native static int nativeCompressFromByteToByte(byte[] srcBuf, int width, int height, int jpegQual, byte[] dstbuf) throws Exception;
    //private native static int nativeCompressFromByteToFile(byte[] srcBuf, int width, int height, int jpegQual, String outPath) throws Exception;

    private native static int nativeCompressFromBitmapToFile(Bitmap bm, int jpegQual, String outPath) throws Exception;

    // jpeg의 header 정보를 추출한다.
    private native static Rect nativeDecompressBoundsFromByteArray(byte[] srcBuf, int size) throws Exception;

    //private native static void nativeDecompressFromByteToByte(byte[] srcBuf, int size, byte[] dstBuf, int desiredWidth, int desiredHeight) throws Exception;

    private native static Bitmap nativeDecompressFromByteArray(byte[] srcBuf, int size, int desiredWidth,
                                                               int desiredHeight) throws Exception;

    // jpeg의 header 정보를 추출한다.
    private native static Rect nativeDecompressBoundsFromFile(String inputPath) throws Exception;

    private native static Bitmap nativeDecompressFromFile(String inputPath, int desiredWidth, int desiredHeight)
            throws Exception;

    /**
     * @param data                 : jpeg raw data
     * @param scaleRatioOneToEight : scale factor ( 1, 2, 4, 8 )
     * @return : native Bitmap data handle
     */
    public native static long nativeDecodeB612(byte[] data, int scaleRatioOneToEight);
}
