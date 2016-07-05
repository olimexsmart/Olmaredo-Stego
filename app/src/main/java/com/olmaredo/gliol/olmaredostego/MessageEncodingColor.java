package com.olmaredo.gliol.olmaredostego;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/*

 */
public class MessageEncodingColor extends AsyncTask<Bitmap, Integer, Bitmap> {
    private final String TAG = "MessageEncodingColor";

    Context context;
    String message;
    byte N = 8; //Block size
    int finHeight = 480; //This could be useful to add in a constructor, DONE
    int strength = 1;
    TaskManager callerFragment;
    double[] signatureR;
    double[] signatureG;
    double[] signatureB;

    //We don't wont this to be called without a message specified.
    private MessageEncodingColor() {
    }

    public MessageEncodingColor(TaskManager result, Context c, String message, byte blockSize, int cropSize, int strength) {
        context = c;
        this.message = message;
        this.N = blockSize;
        this.strength = strength;
        callerFragment = result;
        finHeight = cropSize;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        callerFragment.onTaskStarted("Embedding message in RGB");

        Log.v(TAG, "PreExecute terminated");
    }


    @Override
    protected Bitmap doInBackground(Bitmap... params) {

        params[0] = ResizeNCrop(params[0], N, finHeight);
        Log.v(TAG, "Image resized: " + params[0].getHeight() + " " + params[0].getWidth());
        //Checking how much information can contain the image

        int maxLenght = (params[0].getHeight() * params[0].getWidth() * 3) / (N * N * 8);
        if (message.length() >= maxLenght) {
            message = message.substring(0, maxLenght - 1);
            publishProgress(maxLenght + 1000); //To be sure is greater than 100
        }


        //Getting X matrices
        int H = params[0].getHeight();
        int W = params[0].getWidth();

        int Nsqr = N * N;
        byte[] xr = new byte[Nsqr];
        byte[] xg = new byte[Nsqr];
        byte[] xb = new byte[Nsqr];
        double[][] autocorrelationR = new double[Nsqr][Nsqr];
        double[][] autocorrelationG = new double[Nsqr][Nsqr];
        double[][] autocorrelationB = new double[Nsqr][Nsqr];

        for (int h = 0; h < H; h += N)  //Loop on image's rows (going down)
        {
            for (int w = 0; w < W; w += N)  //Loop on image's columns (from left to right for each row)
            {
                for (int a = 0; a < N; a++) //Loop on block's rows
                {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        xr[a * N + b] = (byte) Color.red(params[0].getPixel(w + b, h + a));
                        xg[a * N + b] = (byte) Color.green(params[0].getPixel(w + b, h + a));
                        xb[a * N + b] = (byte) Color.blue(params[0].getPixel(w + b, h + a));
                    }
                }
                for (int j = 0; j < Nsqr; j++) {
                    for (int k = 0; k < Nsqr; k++) {
                        autocorrelationR[j][k] += xr[k] * xr[j];
                        autocorrelationG[j][k] += xg[k] * xg[j];
                        autocorrelationB[j][k] += xb[k] * xb[j];
                    }
                }
            }
            publishProgress((int) ((h / (double) H) * 65));
        }

        signatureR = GetSignatureVector(autocorrelationR);
        publishProgress(70);
        signatureG = GetSignatureVector(autocorrelationG);
        publishProgress(75);
        signatureB = GetSignatureVector(autocorrelationB);
        publishProgress(80);
        //Giving a push to the garbage collector
        autocorrelationR = null;
        autocorrelationG = null;
        autocorrelationB = null;
        xr = null;
        xg = null;
        xb = null;
        Log.v(TAG, "Created autocorrelation matrices and signature vectors.");

        /*
        Encoding message here, the logic is to use all planes consequently following RGB order.
        So the first three bits are coded into the same block on different colors, the second block
        on the three colors contains the next three bits and so on
         */
        int P = (H * W * 3) / Nsqr; //All the same dimensions, but this time we have three times the information
        //Padding the message
        char[] c = new char[P / 8 + 1]; //One bit per block: one byte every eight blocks times three colors, adding one for non perfect division
        for (int k = 0; k < message.length(); k++) {
            c[k] = message.charAt(k);
        }
        for (int k = message.length(); k < c.length; k++) {
            c[k] = '\0';
        }

        //Because immutable, you know
        Bitmap mutableBitmap = params[0].copy(Bitmap.Config.ARGB_8888, true);
        params[0].recycle();
        int sign;
        byte byteCounter = 0;
        double e;
        int Wmax = W / N; //Number of blocks per row
        int w = 0;
        int h = 0;
        int r, g, blu;
        for (int p = 0; p < P; p++) { //Remember that the P stands for the number of bits to embed in the image

            if ((c[p / 8] & 1 << byteCounter) == 0) sign = -1;
            else sign = 1;
            byteCounter++;
            if (byteCounter == 8)
                byteCounter = 0;

            if (p % 3 == 0) {
                //Applying the bit to the block
                for (int a = 0; a < N; a++) {   //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signatureR[(a * N) + b] + Color.red(mutableBitmap.getPixel((w * N) + b, (h * N) + a)));
                        if (e < 0) e = 0;
                        else if (e > 255) e = 255;

                        r = (int) Math.round(e);
                        g = Color.green(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        blu = Color.blue(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        mutableBitmap.setPixel((w * N) + b, (h * N) + a, Color.argb(255, r, g, blu));
                    }
                }
            } else if (p % 3 == 1) {   //Applying the bit to the block
                for (int a = 0; a < N; a++) {   //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signatureG[(a * N) + b] + Color.green(mutableBitmap.getPixel((w * N) + b, (h * N) + a)));
                        if (e < 0) e = 0;
                        else if (e > 255) e = 255;

                        r = Color.red(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        g = (int) Math.round(e);
                        blu = Color.blue(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        mutableBitmap.setPixel((w * N) + b, (h * N) + a, Color.argb(255, r, g, blu));
                    }
                }
            } else //p % 3 == 2
            {   //Applying the bit to the block
                for (int a = 0; a < N; a++) {   //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signatureB[(a * N) + b] + Color.blue(mutableBitmap.getPixel((w * N) + b, (h * N) + a)));
                        if (e < 0) e = 0;
                        else if (e > 255) e = 255;

                        r = Color.red(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        g = Color.green(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        blu = (int) Math.round(e);
                        mutableBitmap.setPixel((w * N) + b, (h * N) + a, Color.argb(255, r, g, blu));
                    }
                }

                //At the next p increment we will be again in the top if statement, we need to be in the next block
                w++; //Move one block left
                if (w == Wmax) {
                    w = 0;
                    h++; //Move on row down
                }
            }

            publishProgress((int) ((p / (double) P) * 20) + 80);
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

        callerFragment.onTaskCompleted(bitmap, signatureR, signatureG, signatureB);
    }


    /*
        Gets a image and the size of the blocks
        Returns the image cropped in a way that every dimension
        is a multiple of the block size.
     */

    private Bitmap ResizeNCrop(Bitmap original, int N, int finalDimension) {

        Bitmap resized = original;

        if (original.getHeight() < original.getWidth() && original.getHeight() > finalDimension) { //Image is in landscape and needs to be resized
            double ratio = (double) finalDimension / original.getHeight();
            int finalWidth = original.getWidth() * (int) ratio;

            resized = Bitmap.createScaledBitmap(original, finalWidth, finalDimension, false);
        } else if (original.getHeight() > original.getWidth() && original.getWidth() > finalDimension) //Image is in portrait and needs to be resized
        {
            double ratio = (double) finalDimension / original.getWidth();
            int finalHeight = original.getHeight() * (int) ratio;

            resized = Bitmap.createScaledBitmap(original, finalDimension, finalHeight, false);
        }

        return Bitmap.createBitmap(resized, 0, 0, resized.getWidth() - (resized.getWidth() % N), resized.getHeight() - (resized.getHeight() % N));
    }


    //Reference http://math.nist.gov/javanumerics/jama/
    /*
        This gets a N x N matrix, calculates eigenvalues
        and eigenvectors, selects the smallest eigenvalue
        and returns the corresponding eigenvector.
     */
    private double[] GetSignatureVector(double[][] matrix) {   //It's a N x N matrix
        int N = matrix.length;

        Matrix A = new Matrix(matrix);
        A = A.transpose().times(A);

        // compute the spectral decomposition
        EigenvalueDecomposition e = A.eig();
        Matrix V = e.getV();    //<-- Eigenvalues
        Matrix D = e.getD();    //<-- Eigenvectors

        //Select the smallest eigenvalue and return the corresponding eigenvector
        int smallestI = 0;
        double smallest = D.get(0, 0);
        for (int k = 1; k < N; k++) {
            if (D.get(k, k) < smallest) {
                smallest = D.get(k, k);
                smallestI = k;
            }
        }

        //Copy into a one dimensional array
        double[][] temp = V.getArray();
        double[] result = new double[N];
        for (int n = 0; n < N; n++)
            result[n] = temp[n][smallestI];

        return result;
    }
}
