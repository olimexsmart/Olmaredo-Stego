package com.olmaredo.gliol.olmaredostego;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

/*
	This activity manges the decoding tab.
*/
public class MessageDecodingColor extends AsyncTask<Bitmap, Integer, String> {
    private static final String TAG = "MessageDecodingColor";
    private static final double SCALE = 1000000.0f;

    @SuppressLint("StaticFieldLeak")
    private TaskManager callerFragment;
    private char[] key;
    private byte N;


    MessageDecodingColor(TaskManager result, char[] key, byte blockSize) {
        callerFragment = result;
        this.key = key;
        this.N = blockSize;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        callerFragment.onTaskStarted("Decoding message in RGB");
    }

    @Override
    protected String doInBackground(Bitmap... params) {
        //Getting X matrices
        int H = params[0].getHeight();
        int W = params[0].getWidth();
        int Wmax = W / N; //Number of blocks per row
        int Hmax = H / N; //Number of blocks per row
        int NBlocks = Hmax * Wmax;
        char[][] Xr = new char[NBlocks][N * N]; // char because we don't want a signed number
        char[][] Xg = new char[NBlocks][N * N];
        char[][] Xb = new char[NBlocks][N * N];

        byte[] signatureR = OlmaredoUtil.HashKey(key, "4444".getBytes(), 10000, N * N * 8);
        byte[] signatureG = OlmaredoUtil.HashKey(key, "7777".getBytes(), 10000, N * N * 8);
        byte[] signatureB = OlmaredoUtil.HashKey(key, "9999".getBytes(), 10000, N * N * 8);

        int pos[] = OlmaredoUtil.RandomArrayNoRepetitions(NBlocks, NBlocks, signatureR);
        int w;
        int h;

        for (int posInd = 0; posInd < NBlocks; posInd++) {
            w = pos[posInd] % Wmax;
            h = pos[posInd] / Wmax;

            for (int a = 0; a < N; a++) //Loop on block's rows
            {
                for (int b = 0; b < N; b++) //Loop on block's columns
                {
                    Xr[posInd][(a * N) + b] = (char) Color.red(params[0].getPixel((w * N) + b, (h * N) + a));
                    Xg[posInd][(a * N) + b] = (char) Color.green(params[0].getPixel((w * N) + b, (h * N) + a));
                    Xb[posInd][(a * N) + b] = (char) Color.blue(params[0].getPixel((w * N) + b, (h * N) + a));
                }
            }
            publishProgress((int) ((posInd / (double) pos.length) * 70));
        }

        Log.v(TAG, "Created Y matrices.");

        int NBlocksRGB = NBlocks * 3;
        char c = 0;
        StringBuilder result = new StringBuilder(NBlocksRGB / 8);

        for (int p = 0; p < NBlocksRGB; p++) {
            if (p % 8 == 0 && p != 0) { //Every eight cycles save the char in the result
                result.append(c);
                c = 0;
            }
            //Same logic as in the encoding
            if (p % 3 == 0) {
                if (GetSign(signatureR, Xr[p / 3])) //If true set the bit to one
                    c |= (1 << (p % 8));
            } else if (p % 3 == 1) {
                if (GetSign(signatureG, Xg[p / 3])) //If true set the bit to one
                    c |= (1 << (p % 8));
            } else {
                if (GetSign(signatureB, Xb[p / 3])) //If true set the bit to one
                    c |= (1 << (p % 8));
            }
            publishProgress((int) ((p / (double) NBlocksRGB) * 30) + 70);
        }

        publishProgress(100);
        Log.v(TAG, "Giving back the result string");
        return result.toString();

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
    private boolean GetSign(byte[] signature, char[] block) {
        double buffer = 0;
        for (int i = 0; i < signature.length; i++) {
            buffer += (double) signature[i] * block[i] / SCALE;
        }

        return buffer > 0;
    }
}