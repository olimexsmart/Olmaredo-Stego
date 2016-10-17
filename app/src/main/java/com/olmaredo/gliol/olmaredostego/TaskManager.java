package com.olmaredo.gliol.olmaredostego;

import android.graphics.Bitmap;

/*
    TODO finish moving all progress dialog ref in fragments, managin all the support variable on rotation
 */

/*
	This interface manages the asynctask needed for the image encoding and decoding.
	Tasks holds a reference to the caller activity, that implements this interface.
	This way GUI elements can be managed from the activity. This is important because the
	GUI is tied to the activity and thus all refernces to it can change impredictevly.
*/
public interface TaskManager {
    
	void onTaskStarted(String type);

    void onTaskProgress(int progress);

    void onTaskCompleted(Bitmap bm, double[] signature);
    void onTaskCompleted(Bitmap bm, double[] signatureR, double[] signatureG, double[] signatureB);
    void onTaskCompleted(String message);
}
