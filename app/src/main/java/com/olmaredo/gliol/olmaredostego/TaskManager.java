package com.olmaredo.gliol.olmaredostego;

import android.graphics.Bitmap;

/*
	This interface manages the asyncTask needed for the image encoding and decoding.
	Tasks holds a reference to the caller activity, that implements this interface.
	This way GUI elements can be managed from the activity. This is important because the
	GUI is tied to the activity and thus all references to it can change unpredictably.
*/
public interface TaskManager {
    
	void onTaskStarted(String type);

    void onTaskProgress(int progress);

    void onTaskCompleted(Bitmap bm);
    void onTaskCompleted(String message);
}
