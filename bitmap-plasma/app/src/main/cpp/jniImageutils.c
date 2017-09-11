/**
 *
 * @author likebebop
 *
 */

#include <jni.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdlib.h>

//-- com.linecorp.kuru.utils
#define  LOG_TAG    "libimageutils"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


JNIEXPORT jlong JNICALL Java_com_linecorp_kuru_utils_NativeImageUtils_malloc(JNIEnv* env, jclass clazz, jint size)
{
    return (jlong)malloc(size);
}

JNIEXPORT void JNICALL Java_com_linecorp_kuru_utils_NativeImageUtils_free(JNIEnv* env, jclass clazz, jlong addr)
{
    free((void *)addr);
}



static u_int32_t  buildAbgr(unsigned char r, unsigned char g, unsigned char b)
{
    return (u_int32_t)( 0xff000000 |
                        ((b  << 16)  & 0xff0000) |
                        ((g  << 8) & 0x00ff00) |
                        ((r  << 0) & 0x0000ff) );
}

const int RGB_COMPONENT_SIZE = 3;


JNIEXPORT void JNICALL
Java_com_linecorp_kuru_utils_NativeImageUtils_renderToBitmap(JNIEnv *env, jclass type,
                                                             jobject bitmap, jlong nativeRgbArray) {

    AndroidBitmapInfo  info;
    u_int32_t*          pixels;
    int                ret;

    unsigned char* rgbArray = (unsigned char*)nativeRgbArray;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    unsigned char* image = nativeRgbArray;
    int idx = 0;
    for (int i = 0; i < info.width * info.height; i++) {
        pixels[i] = buildAbgr(image[idx], image[idx+1], image[idx+2]);
        idx += RGB_COMPONENT_SIZE;
    }

    AndroidBitmap_unlockPixels(env, bitmap);
   // LOGI("renderToBitmap");
}


int isYuvPoint(int i, int j, int width) {
    if (j % 2 != 0) {
        return 1;
    }

    if (j == 0) {
        return i % 2 == 0;
    } else {
        if (width % 2 == 0) {
            return i % 2 == 0;
        } else {
            return (i + 1) % 2 == 0;
        }
    }
}



void encodeYUV420SP(unsigned char * yuv420sp, unsigned char * rgb, int width, int height) {
    int frameSize = width * height;

    int yIndex = 0;
    int uvIndex = frameSize;

    int R, G, B, Y, U, V;
    int index = 0;
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            R = (rgb[index* RGB_COMPONENT_SIZE] & 0xff);
            G = (rgb[index* RGB_COMPONENT_SIZE + 1] & 0xff);
            B = (rgb[index* RGB_COMPONENT_SIZE + 2] & 0xff);

            // well known RGB to YUV algorithm
            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
            U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
            V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

            // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
            //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
            //    pixel AND every other scanline.
            yuv420sp[yIndex++] = (unsigned char) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
            if (isYuvPoint(i, j, width)) {
                yuv420sp[uvIndex++] = (unsigned char) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                yuv420sp[uvIndex++] = (unsigned char) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
            }

            index++;
        }
    }
}

int buildArraySize(int width, int height) {
    int frameSize = width * height;
    if (width % 2 == 0 && height % 2 == 0) {
        return frameSize * 3 / 2;
    }

    int newHeight = width;
    if (width % 2 != 0) {
        newHeight = (height + 1) / 2 * 2;
    }
    frameSize = width * newHeight;
    int count1 = frameSize / 4 * 2; // 4개씩 묶은 픽셀은 2픽셀 할당
    int count2 = (frameSize % 4 + 1) / 2 * 2; // 1,2개 픽셀에는 2개, 3개 픽셀에는 4개 할당
    return frameSize + count1 + count2;
}


JNIEXPORT jbyteArray JNICALL
Java_com_linecorp_kuru_utils_NativeImageUtils_rgbArrayToNv21(JNIEnv *env, jclass type,
                                                             jlong nativeRgbArray, jint width, jint height) {

    int arraySize = buildArraySize(width, height);
    unsigned char *nv21Array = malloc(arraySize);
    encodeYUV420SP(nv21Array, nativeRgbArray, width, height);

    jbyteArray array =(*env)->NewByteArray(env, arraySize);
    (*env)->SetByteArrayRegion(env, array, 0, arraySize, nv21Array);
    free(nv21Array);
    return array;
}

JNIEXPORT void JNICALL
Java_com_linecorp_kuru_utils_NativeImageUtils_fillTestRgb(JNIEnv *env, jclass type,
                                                       jlong nativeRgbArray, jint width,
                                                       jint height) {

    int stride = width * RGB_COMPONENT_SIZE;
    unsigned char* image = nativeRgbArray;
    for (int i = 0; i < width; i++) {
        image[stride * height/2 + i * 3] = 0xff;
        image[stride * height/2 + i * 3 + 1] = 0x00;
        image[stride * height/2 + i * 3 + 2] = 0x00;
    }

}