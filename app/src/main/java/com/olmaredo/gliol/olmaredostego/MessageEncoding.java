package com.olmaredo.gliol.olmaredostego;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Created by gliol on 12/05/2016.
 * <p/>
 * This task gets:
 * - Bitmap image to host the message
 * - String containg the message (assuming is text for now)
 * - Block size
 * - Embedding power
 */

/*
    TODO final for can be optimized when c = 0
 */

public class MessageEncoding extends AsyncTask<Bitmap, Integer, Bitmap> {
    private static final String TAG = "MessageEncoding";

    Context context;
    String message;
    byte N = 8;
    int finDimension = 480; //This could be useful to add in a constructor
    int strength = 1;
    TaskManager callerFragment;
    double[] signature;
    boolean patternReduction = false;

    //We don't wont this to be called without a message specified.
    private MessageEncoding() {
    }

    public MessageEncoding(TaskManager result, Context c, String message, byte blockSize, int cropSize, int strength, boolean patternRed) {
        context = c;
        this.message = message;
        this.N = blockSize;
        this.strength = strength;
        callerFragment = result;
        finDimension = cropSize;
        patternReduction = patternRed;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        callerFragment.onTaskStarted("Embedding message in gray scale");

        Log.v(TAG, "PreExecute terminated");
    }

    @Override
    protected Bitmap doInBackground(Bitmap... params) {
        //Resize the image and crop it to have multiple of N edges
        params[0] = ResizeNCrop(params[0], N, finDimension);
        Log.v(TAG, "Image resized: " + params[0].getHeight() + " " + params[0].getWidth());

        //ow much informa
        int maxLenght = (params[0].getHeight() * params[0].getWidth()) / (N * N * 8);
        if (message.length() >= maxLenght) {
            message = message.substring(0, maxLenght - 1);
            publishProgress(maxLenght + 1000); //To be sure is greater than 100
        }


        params[0] = ToGrayscale(params[0]);
        Log.v(TAG, "Gray-scaled.");
        publishProgress(10);

        int H = params[0].getHeight();
        int W = params[0].getWidth();
        int Nsqr = N * N;
        byte[] x = new byte[Nsqr];
        double[][] autocorrelation = new double[Nsqr][Nsqr];

        /*
        Index explanation:
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

        for (int h = 0; h < H; h += N)  //Loop on image's rows (going down)
        {
            for (int w = 0; w < W; w += N)  //Loop on image's columns (from left to right for each row)
            {
                for (int a = 0; a < N; a++) //Loop on block's rows
                {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        x[a * N + b] = (byte) Color.green(params[0].getPixel(w + b, h + a));
                    }
                }
                for (int j = 0; j < Nsqr; j++) {
                    for (int k = 0; k < Nsqr; k++) {
                        autocorrelation[j][k] += x[k] * x[j];
                    }
                }
            }
            publishProgress((int) ((h / (double) H) * 50) + 10);
        }

        signature = GetSignatureVector(autocorrelation);
        //SaveSignature(signature);
        publishProgress(70);

        /*
        Embedding the message into the image, combining
        the X matrix, the signature vector and the image
        following the formula: Y = A * b * S + X
        Where A is a coefficient meaning the embedding power
        and b is the corresponding bit of the image.
        As for now the block dimension are fixed so there is the necessity
        of pad the string with dummy characters in order to fill the whole image.
        The string will be padded with '\0' chars.
        */
        int P = (H * W) / Nsqr;
        char[] c = new char[P / 8 + 1]; //One bit per block: one byte every eight blocks, adding one for non perfect division
        for (int k = 0; k < message.length(); k++) {
            c[k] = message.charAt(k);
        }
        for (int k = message.length(); k < c.length; k++) {
            c[k] = '\0';
        }

        int sign;
        byte byteCounter = 0;
        double e; //temp value holding the new pixel value
        int Wmax = W / N; //Number of blocks per row
        int w = 0;
        int h = 0;
        int offset = 0; //shifts the signature vector reading, % Nsqr cycles the index
        for (int p = 0; p < P; p++) {
            if ((c[p / 8] & 1 << byteCounter) == 0) sign = -1;
            else sign = 1;

            //Applying the bit to the block
            for (int a = 0; a < N; a++) {   //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                for (int b = 0; b < N; b++) //Loop on block's columns
                {   //e is the final value of the pixel
                    e = (sign * strength * signature[((a * N) + b + offset) % Nsqr] + Color.green(params[0].getPixel((w * N) + b, (h * N) + a)));
                    if (e < 0) e = 0;
                    else if (e > 255) e = 255;

                    int rgb = (int) Math.round(e);
                    params[0].setPixel((w * N) + b, (h * N) + a, Color.argb(255, rgb, rgb, rgb));
                }
            }

            w++; //Move one block left
            if(patternReduction)
                offset++;

            if (w == Wmax) {
                w = 0;
                h++; //Move on row down
            }
            byteCounter++;
            if (byteCounter == 8) byteCounter = 0;

            publishProgress((int) ((p / (double) P) * 30) + 70);
        }


        return params[0];
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

        callerFragment.onTaskCompleted(bitmap, signature);
    }

    //################################ PROCESSING FUNCTIONS #############################################

    /*
        Gets a image and the size of the blocks
        Returns the image cropped in a way that every dimension
        is a multiple of the block size.
     */
    private Bitmap ResizeNCrop(Bitmap original, int N, int finalDimension) {

        Bitmap resized = original;

        if (original.getHeight() < original.getWidth() && original.getHeight() > finalDimension) { //Image is in landscape and needs to be resized
            double ratio = (double) finalDimension / original.getHeight();
            int finalWidth = (int) (original.getWidth() * ratio);

            resized = Bitmap.createScaledBitmap(original, finalWidth, finalDimension, false);
        } else if (original.getHeight() > original.getWidth() && original.getWidth() > finalDimension) //Image is in portrait and needs to be resized
        {
            double ratio = (double) finalDimension / original.getWidth();
            int finalHeight = (int) (original.getHeight() * ratio);

            resized = Bitmap.createScaledBitmap(original, finalDimension, finalHeight, false);
        }

        return Bitmap.createBitmap(resized, 0, 0, resized.getWidth() - (resized.getWidth() % N), resized.getHeight() - (resized.getHeight() % N));
    }

    /*
        Gets a Bitmap
        Returns the same Bitmap in gray scale, setting all RGB components to the same value
     */
    private Bitmap ToGrayscale(Bitmap bmpOriginal) {
        Bitmap bmpGrayscale = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        int pixel;
        int r, g, b;
        int a;

        for (int x = 0; x < bmpOriginal.getWidth(); ++x) {
            for (int y = 0; y < bmpOriginal.getHeight(); ++y) {
                // get one pixel original
                pixel = bmpOriginal.getPixel(x, y);
                // retrieve original of all channels
                a = Color.alpha(pixel);
                r = Color.red(pixel);
                g = Color.green(pixel);
                b = Color.blue(pixel);
                // Y = 0.2126 R + 0.7152 G + 0.0722 B  as in Rec 709 (Wiki)
                r = g = b = (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
                // set new pixel original to output bitmap
                bmpGrayscale.setPixel(x, y, Color.argb(a, r, g, b));
            }
        }
        return bmpGrayscale;
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
