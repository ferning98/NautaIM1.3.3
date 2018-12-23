package com.fernapps.nautaim;

import android.content.Context;

import com.iceteck.silicompressorr.SiliCompressor;

/**
 * Created by FeRN@NDeZ on 07/04/2017.
 */

public class CompressImageHelper {
    Context context;

    public CompressImageHelper(Context c) {
        context = c;
    }

    public String compressImage(String filePath) {
        SiliCompressor compressor = new SiliCompressor(context);
        return compressor.compress(filePath);
    }
}
