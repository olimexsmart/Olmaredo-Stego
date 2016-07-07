package com.olmaredo.gliol.olmaredostego;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

public class MessageDecoding extends AsyncTask<Bitmap, Integer, String> {
    private static final String TAG = "MessageDecoding";

    Context context;
    TaskManager callerFragment;
    double[] signature;
    byte N;

    private MessageDecoding() {}

    public MessageDecoding(Context c, double[] s, TaskManager result)
    {
        context = c;
        signature = s;
        callerFragment = result;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        //SaveSignature(signature);
        callerFragment.onTaskStarted("Decoding message in grey scale");
    }

    @Override
    protected String doInBackground(Bitmap... params) {

        N = (byte) Math.round(Math.sqrt(signature.length));
        char[][] Y = GetXMatrix(params[0], N);
        publishProgress(50);
        String result = "";
        char[] buffer = new char[Y.length]; //Used to copy one column
        char c = 0;
        Log.v(TAG, "Created Y matrix.");

        for(int i = 0; i < Y[0].length; i++)
        {   //This should be avoided creating Y transposed
            for(int k = 0; k < Y.length; k++)
                buffer[k] = Y[k][i];
            //Every eight cycles save the char in the result
            if(i % 8 == 0 && i != 0)
            {
                result += c;
                c = 0;
            }
            //Here assembly each char, bit by bit
            if(GetSign(signature, buffer)) //If true set the bit to one
                c |= (1 << (i % 8));

            publishProgress((int)((i / (double)Y[0].length) * 50) + 50);
        }

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
    private boolean GetSign (double[]signature, char[]block)
    {
        double buffer = 0;
        for (int i = 0; i < signature.length; i++ )
        {
            buffer += signature[i] * block[i];
        }

        return buffer > 0;

    }

    private char[][] GetXMatrix(Bitmap image, byte N)
    {//Think about checking if the image is in grayscale
        int H = image.getHeight();
        int W = image.getWidth();

        char[][] X = new char[N * N][(H * W) / (N * N)];

        for (int h = 0; h < H; h += N)  //Loop on image's rows (going down)
        {
            for (int w = 0; w < W; w += N)  //Loop on image's columns (from left to right for each row)
            {
                for (int a = 0; a < N; a++) //Loop on block's rows
                {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        X[(a * N) + b][(W / N) * (h / N) + (w / N)] = (char) Color.green(image.getPixel(w + b, h + a));
                    }
                }
            }
        }

        /* Index explanation:
            Note: h and w are incremented by the block dimension, not by one!

            X
            [From 0 to N^2-1, the position is determined by the amount of block's pixels
            read unitl now: rows (a) * bloc dimension + pixel read on current row]
            [From 0 to P, the column is determined by how many blocks were read until now.
            W/N gives us the amount of block per row, times how many rows we did, plus how
            many block were read in this row]

            getPixel
            (w as a horizontal offset in pixels, b selects the pixel in the block's row)
            (h as the vertical offset, a selects the row)
         */
        return X;
    }
}