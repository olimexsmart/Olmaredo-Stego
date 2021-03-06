package com.olmaredo.gliol.olmaredostego;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


/*

 */
public class MessageEncodingColor extends AsyncTask<Bitmap, Integer, Bitmap> {
    private final String TAG = "MessageEncodingColor";
    private static final int ITERATIONS = 1000;
    private static final float VARIANCE = 10.0f;


    @SuppressLint("StaticFieldLeak")
    private Context context;
    private String message;
    private char[] key;
    private byte N; //Block size
    private int finHeight;
    private double strength;
    private TaskManager callerFragment;

    MessageEncodingColor(TaskManager result, Context c, String message, char[] key, byte blockSize, int cropSize, double strength) {
        context = c;
        this.message = message;
        this.key = key;
        this.N = blockSize;
        this.strength = strength;
        callerFragment = result;
        finHeight = cropSize;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        callerFragment.onTaskStarted("Encoding message");

        Log.v(TAG, "PreExecute terminated");
    }


    @Override
    protected Bitmap doInBackground(Bitmap... params) {

        params[0] = OlmaredoUtil.ResizeNCrop(params[0], N, finHeight);
        Log.v(TAG, "Image resized: " + params[0].getHeight() + " " + params[0].getWidth());

        //Checking how much information can contain the image
        int H = params[0].getHeight();
        int W = params[0].getWidth();
        int NSqr = N * N;
        int WMax = W / N; //Number of blocks per row
        int HMax = H / N; //Number of blocks per row
        int NBlocks = HMax * WMax;

        int maxLength = ((NBlocks * 3) / 8) - 3; // Safe from border conditions
        if (message.length() >= maxLength) {
            message = message.substring(0, maxLength - 5);
            publishProgress(maxLength + 1000); //To be sure is greater than 100
        }

        message += "\0\0\0\0\0"; // Hopefully one of these will make it through

        byte[] signatureRf = OlmaredoUtil.HashKey(key, "4444".getBytes(), ITERATIONS, NSqr * 8);
        byte[] signatureGf = OlmaredoUtil.HashKey(key, "7777".getBytes(), ITERATIONS, NSqr * 8);
        byte[] signatureBf = OlmaredoUtil.HashKey(key, "9999".getBytes(), ITERATIONS, NSqr * 8);

        // Generating gaussian-noise signatures, which seems to have better performance
        double[] signatureR = OlmaredoUtil.gaussianNoise(NSqr, VARIANCE, 0, signatureRf);
        double[] signatureG = OlmaredoUtil.gaussianNoise(NSqr, VARIANCE, 0, signatureGf);
        double[] signatureB = OlmaredoUtil.gaussianNoise(NSqr, VARIANCE, 0, signatureBf);
        //Log.v(TAG, "Encoding Gaussian: " + signatureR[0] + signatureG[12] + signatureB[20]);

        int ML = message.length();
        int NBlocksNeeded = ML * 8;
        int NBlocksNeededRGB = NBlocksNeeded / 3; // N blocks needed, RGB planes counting as one
        NBlocksNeededRGB += (NBlocksNeeded % 3) + 1; // Need to make it divisible by 3 because of how the next for loop is structured

        // Preparing scattering indexes - bits are shuffled on the image
        int[] posR = OlmaredoUtil.RandomArrayNoRepetitions(NBlocksNeededRGB, NBlocks, signatureRf);
        int[] posG = OlmaredoUtil.RandomArrayNoRepetitions(NBlocksNeededRGB, NBlocks, signatureGf);
        int[] posB = OlmaredoUtil.RandomArrayNoRepetitions(NBlocksNeededRGB, NBlocks, signatureBf);
        int posInd = 0;
        int wR = posR[0] % WMax;
        int wG = posG[0] % WMax;
        int wB = posB[0] % WMax;
        int hR = posR[0] / WMax;
        int hG = posG[0] / WMax;
        int hB = posB[0] / WMax;


        //Because immutable, you know
        Bitmap mutableBitmap = params[0].copy(Bitmap.Config.ARGB_8888, true);
        params[0].recycle();
        int sign;
        byte bitCounter = 0;
        double e;
        int r, g, blu;

        /*
            Encoding message here, the logic is to use all planes consequently following RGB order.
            So the first three bits are coded into the same block on different colors, the second block
            on the three colors contains the next three bits and so on
        */

        for (int p = 0; p < NBlocksNeeded; p++) { //Remember that the P stands for the number of bits to embed in the image

            if ((message.charAt(p / 8) & 1 << bitCounter) == 0) sign = -1;
            else sign = 1;
            bitCounter++;
            bitCounter %= 8;

            if (p % 3 == 0) {
                //Applying the bit to the block
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signatureR[(a * N) + b] + Color.red(mutableBitmap.getPixel((wR * N) + b, (hR * N) + a)));

                        //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                        if (e < 0) e = 0;
                        else if (e > 255) e = 255;

                        r = (int) Math.round(e);
                        g = Color.green(mutableBitmap.getPixel((wR * N) + b, (hR * N) + a));
                        blu = Color.blue(mutableBitmap.getPixel((wR * N) + b, (hR * N) + a));
                        mutableBitmap.setPixel((wR * N) + b, (hR * N) + a, Color.argb(255, r, g, blu));
                    }
                }
            } else if (p % 3 == 1) {   //Applying the bit to the block
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signatureG[(a * N) + b] + Color.green(mutableBitmap.getPixel((wG * N) + b, (hG * N) + a)));
                        if (e < 0) e = 0;
                        else if (e > 255) e = 255;

                        r = Color.red(mutableBitmap.getPixel((wG * N) + b, (hG * N) + a));
                        g = (int) Math.round(e);
                        blu = Color.blue(mutableBitmap.getPixel((wG * N) + b, (hG * N) + a));
                        mutableBitmap.setPixel((wG * N) + b, (hG * N) + a, Color.argb(255, r, g, blu));
                    }
                }
            } else //p % 3 == 2
            {   //Applying the bit to the block
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signatureB[(a * N) + b] + Color.blue(mutableBitmap.getPixel((wB * N) + b, (hB * N) + a)));
                        if (e < 0) e = 0;
                        else if (e > 255) e = 255;

                        r = Color.red(mutableBitmap.getPixel((wB * N) + b, (hB * N) + a));
                        g = Color.green(mutableBitmap.getPixel((wB * N) + b, (hB * N) + a));
                        blu = (int) Math.round(e);
                        mutableBitmap.setPixel((wB * N) + b, (hB * N) + a, Color.argb(255, r, g, blu));
                    }
                }

                //At the next p increment we will be again in the first if statement, we need to be in the next block
                posInd++;
                wR = posR[posInd] % WMax;
                wG = posG[posInd] % WMax;
                wB = posB[posInd] % WMax;

                hR = posR[posInd] / WMax;
                hG = posG[posInd] / WMax;
                hB = posB[posInd] / WMax;
            }

            publishProgress((int) ((p / (double) NBlocksNeeded) * 100));
        }

        return mutableBitmap;
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        //Setting progress percentage
        if (values[0] > 100) {
            Toast.makeText(context, "Input text too long, trimming it at: " + (values[0] - 1000), Toast.LENGTH_LONG).show();
        } else {
            callerFragment.onTaskProgress(values[0]);
        }
    }


    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);

        callerFragment.onTaskCompleted(bitmap);
    }


}
