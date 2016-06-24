package com.example.gliol.olmaredostego;

import android.graphics.Bitmap;

/**
 * Created by gliol on 24/06/2016.
 */
public interface GetResultEncodingColor {
    void onResultsReady(Bitmap bm, double[] signatureR, double[] signatureG, double[] signatureB);
}
