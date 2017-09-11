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
package com.example.plasma;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.linecorp.android.common.jpegturbo.JpegTurbo;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class Plasma extends Activity
{

    public Bitmap decodeFromAsset(String path) {
        AssetManager assetManager = getApplicationContext().getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(path);
            return BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            return null;
        } finally {
            try {
                istr.close();
            } catch (Exception e) {
            }
        }
    }




    public byte[] toBytes(String path) {
        AssetManager assetManager = getApplicationContext().getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(path);
            return IOUtils.toByteArray(istr);
        } catch (IOException e) {
            return null;
        } finally {
            try {
                istr.close();
            } catch (Exception e) {
            }
        }
    }

    private static native long buildBytes();

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        DisplayMetrics display = getResources().getDisplayMetrics();

        byte[] bytes = toBytes("B612_20170911_114134.jpg");

        //long nativeRgbArray = JpegTurbo.nativeDecodeB612(bytes, 1);
        //setContentView(new PlasmaView(this, display.widthPixels, display.heightPixels, nativeRgbArray));
        setContentView(new PlasmaView(this, display.widthPixels, display.heightPixels, buildBytes()));
    }

    // load our native library
    static {
        System.loadLibrary("plasma");
    }
}

// Custom view for rendering plasma.
//
// Note: suppressing lint wrarning for ViewConstructor since it is
//       manually set from the activity and not used in any layout.
@SuppressLint("ViewConstructor")
class PlasmaView extends View {
    private Bitmap mBitmap;
    private long nativeRgbArray;

    // implementend by libplasma.so
    private static native void renderPlasma(Bitmap  bitmap, long nativeRgbArray);

    public PlasmaView(Context context, int width, int height, long nativeRgbArray) {
        super(context);
        this.nativeRgbArray = nativeRgbArray;
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    @Override protected void onDraw(Canvas canvas) {
        renderPlasma(mBitmap, nativeRgbArray);
        canvas.drawBitmap(mBitmap, 0, 0, null);
        // force a redraw, with a different time-based pattern.
        invalidate();
    }
}
