/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdlib.h>

#define  LOG_TAG    "libplasma"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static u_int32_t  buildAgbr(unsigned char r, unsigned char g, unsigned char b)
{
    return (u_int32_t)( 0xff000000 |
                       ((b  << 16)  & 0xff0000) |
                       ((g  << 8) & 0x00ff00) |
                       ((r  << 0) & 0x0000ff) );
}

JNIEXPORT void JNICALL Java_com_example_plasma_PlasmaView_renderPlasma(JNIEnv * env, jobject  obj, jobject bitmap,  jlong  nativeRgbArray)
{
    AndroidBitmapInfo  info;
    u_int32_t*          pixels;
    int                ret;

    unsigned char* rgbArray = nativeRgbArray;

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

    //--ABGR로 저장되 있음
    //unsigned char* current = rgbArray;
    int stride = info.width * 3;
    int size = stride * info.height;
    int idx = 0;
    //unsigned char* image = rgbArray;
    unsigned char* image = (unsigned char*)malloc(size);

    memset(image, 0xff, size);

    for (int i = 0; i < info.width / 2; i++) {
        image[stride * 799 + i * 3] = 0xff;
        image[stride * 799 + i * 3 + 1] = 0x00;
        image[stride * 799 + i * 3 + 2] = 0x00;
    }

    for (int i = 0; i < info.width * 800; i++) {
        pixels[i] = buildAgbr(image[idx], image[idx+1], image[idx+2]);
        idx+=3;
    }

    free(image);

    AndroidBitmap_unlockPixels(env, bitmap);

}
