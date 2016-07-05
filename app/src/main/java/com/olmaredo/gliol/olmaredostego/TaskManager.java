package com.olmaredo.gliol.olmaredostego;

import android.graphics.Bitmap;

/*
    TODO finish moving all progress dialog ref in fragments, managin all the support variable on rotation
 */
public interface TaskManager {
    void onTaskStarted(String type);

    void onTaskProgress(int progress);

    void onTaskCompleted(Bitmap bm, double[] signature);
    void onTaskCompleted(Bitmap bm, double[] signatureR, double[] signatureG, double[] signatureB);
    void onTaskCompleted(String message);
}
