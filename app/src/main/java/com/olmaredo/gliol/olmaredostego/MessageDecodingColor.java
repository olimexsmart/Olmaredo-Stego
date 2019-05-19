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
    private static final int ITERATIONS = 1000;
    private static final float VARIANCE = 1.0f;

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

        callerFragment.onTaskStarted("Decoding message");
    }

    @Override
    protected String doInBackground(Bitmap... params) {
        //Getting X matrices
        int H = params[0].getHeight();
        int W = params[0].getWidth();
        int wMax = W / N; //Number of blocks per row
        int hMax = H / N; //Number of blocks per row
        int NBlocks = hMax * wMax;
        int NSqr = N * N;
        char[] Xr = new char[NSqr]; // char because we don't want a signed number
        char[] Xg = new char[NSqr];
        char[] Xb = new char[NSqr];

        byte[] signatureRf = OlmaredoUtil.HashKey(key, "4444".getBytes(), ITERATIONS, NSqr * 8);
        byte[] signatureGf = OlmaredoUtil.HashKey(key, "7777".getBytes(), ITERATIONS, NSqr * 8);
        byte[] signatureBf = OlmaredoUtil.HashKey(key, "9999".getBytes(), ITERATIONS, NSqr * 8);

        double[] signatureR = OlmaredoUtil.gaussianNoise(NSqr, VARIANCE, 0, signatureRf);
        double[] signatureG = OlmaredoUtil.gaussianNoise(NSqr, VARIANCE, 0, signatureGf);
        double[] signatureB = OlmaredoUtil.gaussianNoise(NSqr, VARIANCE, 0, signatureBf);
        //Log.v(TAG, "Decoding Gaussian: " + signatureR[0] + signatureG[12] + signatureB[20]);


        int[] posR = OlmaredoUtil.RandomArrayNoRepetitions(NBlocks, NBlocks, signatureRf);
        int[] posG = OlmaredoUtil.RandomArrayNoRepetitions(NBlocks, NBlocks, signatureGf);
        int[] posB = OlmaredoUtil.RandomArrayNoRepetitions(NBlocks, NBlocks, signatureBf);
        int wR, wG, wB;
        int hR, hG, hB;


        wR = posR[0] % wMax;
        wG = posG[0] % wMax;
        wB = posB[0] % wMax;
        hR = posR[0] / wMax;
        hG = posG[0] / wMax;
        hB = posB[0] / wMax;

        for (int a = 0; a < N; a++) { //Loop on block's rows
            for (int b = 0; b < N; b++) { //Loop on block's columns
                Xr[(a * N) + b] = (char) Color.red(params[0].getPixel((wR * N) + b, (hR * N) + a));
                Xg[(a * N) + b] = (char) Color.green(params[0].getPixel((wG * N) + b, (hG * N) + a));
                Xb[(a * N) + b] = (char) Color.blue(params[0].getPixel((wB * N) + b, (hB * N) + a));
            }
        }

        Log.v(TAG, "Created Y matrices.");

        int NBlocksRGB = NBlocks * 3;
        char c = 0;
        char terminators = 0;
        int posInd = 0;
        StringBuilder result = new StringBuilder(NBlocksRGB / 8);

        for (int p = 0; p < NBlocksRGB; p++) {
            if (p % 8 == 0 && p != 0) { //Every eight cycles save the char in the result
                // Detecting end of string
                if (c == '\0') terminators++;
                if (terminators == 3) break;
                // Appending decoded character
                result.append(c);
                c = 0;
            }
            //Same logic as in the encoding
            if (p % 3 == 0) {
                if (GetSign(signatureR, Xr)) //If true set the bit to one
                    c |= (1 << (p % 8));
            } else if (p % 3 == 1) {
                if (GetSign(signatureG, Xg)) //If true set the bit to one
                    c |= (1 << (p % 8));
            } else {
                if (GetSign(signatureB, Xb)) //If true set the bit to one
                    c |= (1 << (p % 8));

                posInd++;
                wR = posR[posInd] % wMax;
                wG = posG[posInd] % wMax;
                wB = posB[posInd] % wMax;
                hR = posR[posInd] / wMax;
                hG = posG[posInd] / wMax;
                hB = posB[posInd] / wMax;

                for (int a = 0; a < N; a++) { //Loop on block's rows
                    for (int b = 0; b < N; b++) { //Loop on block's columns
                        Xr[(a * N) + b] = (char) Color.red(params[0].getPixel((wR * N) + b, (hR * N) + a));
                        Xg[(a * N) + b] = (char) Color.green(params[0].getPixel((wG * N) + b, (hG * N) + a));
                        Xb[(a * N) + b] = (char) Color.blue(params[0].getPixel((wB * N) + b, (hB * N) + a));
                    }
                }
            }
            publishProgress((int) ((p / (double) NBlocksRGB) * 100));
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
        Gets the key vector and a enveloped block
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