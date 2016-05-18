package com.example.gliol.olmaredostego;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

/**
 * Created by gliol on 16/05/2016.
 */
public class MessageDecoding extends AsyncTask<Bitmap, Integer, String> {
    private static final String TAG = "MessageDecoding";

    ProgressDialog progressDialog;
    Context context;

    private MessageDecoding() {}

    public MessageDecoding(Context c)
    {
        context = c;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Decoding message.");
        //progressDialog.setMessage("Resizing...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100); //Set total number of blocks here
        progressDialog.setCancelable(true);
        progressDialog.setIndeterminate(false);

        progressDialog.show();
    }

    @Override
    protected String doInBackground(Bitmap... params) {


        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        progressDialog.setProgress(values[0]);
    }

    private boolean GetSign (double[]signature, byte[]block)
    {
        double buffer = 0;
        for (int i = 0; i < signature.length; i++ )
        {
            buffer += signature[i] * block[i];
        }

        if (buffer < 0)
            return false;
        else
            return true;

    }
}
