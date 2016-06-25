package com.example.gliol.olmaredostego;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by gliol on 24/06/2016.
 */
public class MessageDecodingColor extends AsyncTask<Bitmap, Integer, String> {
    private static final String TAG = "MessageDecodingColor";

    ProgressDialog progressDialog;
    Context context;
    GetResultDecoding returnResult;
    double[] signatureR;
    double[] signatureG;
    double[] signatureB;
    byte N;

    private MessageDecodingColor() {
    }

    public MessageDecodingColor(GetResultDecoding result, Context c, double[] sR, double[] sG, double[] sB) {
        context = c;
        signatureR = sR;
        signatureG = sG;
        signatureB = sB;
        returnResult = result;
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
        }

        publishProgress(75);
        Log.v(TAG, "Created Y matrices.");
        String result = "";
        char[] buffer = new char[Xr.length]; //Used to copy one column
        char c = 0;

        for (int i = 0; i < Xr[0].length * 3; i++) {
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
        }

        publishProgress(100);
        Log.v(TAG, "Giving back the result string");
        return result;

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressDialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        progressDialog.hide();
        progressDialog.dismiss();
        this.returnResult.OnResultReady(s);
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
}