package com.example.gliol.olmaredostego;

import android.graphics.Bitmap;

/**
 * Created by gliol on 20/05/2016.
 */
public interface GetResultEmbedding {
    void onResultsReady(Bitmap bm, double[] signature);
}