package com.example.gliol.olmaredostego;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MessageDecodingColor extends AsyncTask<Bitmap, Integer, String> {
    private static final String TAG = "MessageDecodingColor";

    Context context;
    TaskManager callerFragment;
    double[] signatureR;
    double[] signatureG;
    double[] signatureB;
    byte N;

    private MessageDecodingColor() {
    }

    public MessageDecodingColor(TaskManager result, Context c, double[] sR, double[] sG, double[] sB) {
        context = c;
        signatureR = sR;
        signatureG = sG;
        signatureB = sB;
        callerFragment = result;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
/*
        SaveSignature(signatureR, "red");
        SaveSignature(signatureG, "green");
        SaveSignature(signatureB, "blue");
*/
        callerFragment.onTaskStarted("Decoding message in RGB");
    }

    @Override
    protected String doInBackground(Bitmap... params) {
        N = (byte) Math.round(Math.sqrt(signatureR.length));
        //Getting X matrices
        int H = params[0].getHeight();
        int W = params[0].getWidth();
        char[][] Xr = new char[N * N][(H * W) / (N * N)];
        char[][] Xg = new char[N * N][(H * W) / (N * N)];
        char[][] Xb = new char[N * N][(H * W) / (N * N)];

        for (int h = 0; h < H; h += N)  //Loop on image's rows (going down)
        {
            for (int w = 0; w < W; w += N)  //Loop on image's columns (from left to right for each row)
            {
                for (int a = 0; a < N; a++) //Loop on block's rows
                {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        Xr[(a * N) + b][(W / N) * (h / N) + (w / N)] = (char) Color.red(params[0].getPixel(w + b, h + a));
                        Xg[(a * N) + b][(W / N) * (h / N) + (w / N)] = (char) Color.green(params[0].getPixel(w + b, h + a));
                        Xb[(a * N) + b][(W / N) * (h / N) + (w / N)] = (char) Color.blue(params[0].getPixel(w + b, h + a));
                    }
                }
            }
            publishProgress((int)((h / (double)H) * 70));
        }

        Log.v(TAG, "Created Y matrices.");
        String result = "";
        char[] buffer = new char[Xr.length]; //Used to copy one column
        char c = 0;
        int I = Xr[0].length * 3;

        for (int i = 0; i < I; i++) {
            if (i % 8 == 0 && i != 0) //Every eight cycles save the char in the result
            {
                result += c;
                c = 0;
            }
            //Same logic as in the encoding
            if (i % 3 == 0) {
                //This should be avoided creating Y transposed
                for (int k = 0; k < Xr.length; k++)
                    buffer[k] = Xr[k][i / 3];

                //Here assembly each char, bit by bit
                if (GetSign(signatureR, buffer)) //If true set the bit to one
                    c |= (1 << (i % 8));

            } else if (i % 3 == 1) {
                //This should be avoided creating Y transposed
                for (int k = 0; k < Xg.length; k++)
                    buffer[k] = Xg[k][i / 3];

                //Here assembly each char, bit by bit
                if (GetSign(signatureG, buffer)) //If true set the bit to one
                    c |= (1 << (i % 8));

            } else {
                //This should be avoided creating Y transposed
                for (int k = 0; k < Xb.length; k++)
                    buffer[k] = Xb[k][i / 3];

                //Here assembly each char, bit by bit
                if (GetSign(signatureB, buffer)) //If true set the bit to one
                    c |= (1 << (i % 8));

            }
            publishProgress((int)((i / (double)I) * 30) + 70);
        }

        publishProgress(100);
        Log.v(TAG, "Giving back the result string");
        return result;

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        callerFragment.onTaskProgress(values[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        callerFragment.onTaskCompleted(s);
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    /*
        Gets the key vector and a envelopped block
        Returns the value of the bit assigned to the
        block.
     */
    private boolean GetSign(double[] signature, char[] block) {
        double buffer = 0;
        for (int i = 0; i < signature.length; i++) {
            buffer += signature[i] * block[i];
        }

        return buffer > 0;
    }


    private void SaveSignature(double [] signature, String type)
    {

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
            String path = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-DECODING-signature-" + type + ".txt";
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(path));
            for (double aSignature : signature) {
                outputStreamWriter.write(aSignature + "\n");
            }
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }
}