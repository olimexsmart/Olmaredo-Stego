package com.example.gliol.olmaredostego;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    TODO manage pictures on portrait, the resasing is done wrong
 */

public class MessageEncoding extends AsyncTask<Bitmap, Integer, Bitmap> {
    private static final String TAG = "MessageEncoding";

    ProgressDialog progressDialog;
    Context context;
    String message;
    byte N = 8;
    int finHeight = 480; //This could be useful to add in a constructor
    int strength = 1;
    GetResultEmbedding returnResult;
    double[] signature;

    //We don't wont this to be called without a message specified.
    private MessageEncoding() {
    }

    public MessageEncoding(GetResultEmbedding result, Context c, String message) {
        context = c;
        this.message = message;
        this.returnResult = result;
    }

    public MessageEncoding(GetResultEmbedding result, Context c, String message, byte blockSize, int cropSize, int strength) {
        context = c;
        this.message = message;
        this.N = blockSize;
        this.strength = strength;
        returnResult = result;
        finHeight = cropSize;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Embedding message in gray scale");
        //progressDialog.setMessage("Resizing...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(true);
        progressDialog.setIndeterminate(false);

        progressDialog.show();

        Log.v(TAG, "PreExecute terminated");
    }

    @Override
    protected Bitmap doInBackground(Bitmap... params) {

        params[0] = ResizeNCrop(params[0], N, finHeight);
        Log.v(TAG, "Image resized: " + params[0].getHeight() + " " + params[0].getWidth());

        int maxLenght = (params[0].getHeight() * params[0].getWidth()) / (N * N * 8);
        if(message.length() >= maxLenght)
        {
            message = message.substring(0, maxLenght - 1);
            publishProgress(maxLenght + 1000); //To be sure is greater than 100
        }


        params[0] = ToGrayscale(params[0]);
        Log.v(TAG, "Gray-scaled.");
        publishProgress(10);
/*
        publishProgress(25);
        char[][] X = GetXMatrix(params[0], N);
        Log.v(TAG, "Created X matrix.");

        publishProgress(75);
        signature = GetSignatureVector(GetAutocorrelation(X));
        Log.v(TAG, "Got signature vector.");
        SaveSignature(signature); //Saving signature to get a general one

        publishProgress(85);
        X = EmbedMessage(message, strength, signature, X);
        Log.v(TAG, "Embedded message.");

        publishProgress(100);
        Log.v(TAG, "Recreating image and returning.");
        return GetImageFromY(params[0], X, finHeight);
        */

        int H = params[0].getHeight();
        int W = params[0].getWidth();

        //char[][] X = new char[N * N][(H * W) / (N * N)];
        int Nsqr = N * N;
        byte[] x = new byte[Nsqr];
        double[][] autocorrelation = new double[Nsqr][Nsqr];

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
            publishProgress((int)((h / (double)H) * 50) + 10);
        }

        SaveSignature(signature);
        signature = GetSignatureVector(autocorrelation);
        publishProgress(70);

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
        double e;
        int Wmax = W / N; //Number of blocks per row
        int w = 0;
        int h = 0;
        for (int p = 0; p < P; p++) {
            if ((c[p / 8] & 1 << byteCounter) == 0) sign = -1;
            else sign = 1;

            //Applying the bit to the block
            for (int a = 0; a < N; a++) {   //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                for (int b = 0; b < N; b++) //Loop on block's columns
                {
                    e = (sign * strength * signature[(a * N) + b] + Color.green(params[0].getPixel((w * N) + b, (h * N) + a)));
                    if (e < 0) e = 0;
                    else if (e > 255) e = 255;

                    int rgb = (int) Math.round(e);
                    params[0].setPixel((w * N) + b, (h * N) + a, Color.argb(255, rgb, rgb, rgb));
                }
            }

            w++; //Move one block left
            if(w == Wmax)
            {
                w = 0;
                h++; //Move on row down
            }
            byteCounter++;
            if (byteCounter == 8) byteCounter = 0;

            publishProgress((int)((p / (double)P) * 30) + 70);
        }


        return params[0];
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        //Setting progress percentage
        if(values[0] > 100)
        {
            Toast.makeText(context, "Input text too long, trimming it at: " + (values[0] - 1000), Toast.LENGTH_LONG).show();
        }
        else {
            progressDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        progressDialog.hide();
        //if (progressDialog.isShowing())
        progressDialog.dismiss();
        this.returnResult.onResultsReady(bitmap, signature);
    }

    //################################ PROCESSING FUNCTIONS #############################################

    /*
        Gets a image and the size of the blocks
        Returns the image cropped in a way that every dimension
        is a multiple of the block size.
     */

    private Bitmap ResizeNCrop(Bitmap original, int N, int finalHeight) {

        if(original.getHeight() > finalHeight) {
            double ratio = (double) original.getHeight() / finalHeight;
            int finalWidth = original.getWidth() / (int) ratio;

            Bitmap resized = original.createScaledBitmap(original, finalWidth, finalHeight, false);

            return Bitmap.createBitmap(resized, 0, 0, finalWidth - (finalWidth % N), finalHeight - (finalHeight % N));
        }

        return original;
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

    /*
        Gets a Bitmap and re-arranges it into a two-dimensional byte
        array having dimensions depending on the block size (N) and
        the whole image dimension.
        Every block is unfolded into a column, starting from the
        first line. Because of this, knowing that blocks are NxN
        squares, the vertical dimension of the result matrix is N^2.
        The horizontal dimension (width) is directly dependent to the number of
        blocks in the image. Let's say H is the height and W the width,
        the final P width of the result is: (H * W / N^2)
     */
    private char[][] GetXMatrix(Bitmap image, byte N) {//Think about checking if the image is in grayscale
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


    /*
        This gets a N^2 x P matrix, where N is the block size
        and P is the number of blocks.
        The return value is the autocorrelation matrix.
     */
    private double[][] GetAutocorrelation(char[][] x) {
        int P = x[0].length;
        int Nsqr = x.length;
        double[][] buffer = new double[Nsqr][Nsqr];

        for (int p = 0; p < P; p++) {
            for (int j = 0; j < Nsqr; j++) {
                for (int k = 0; k < Nsqr; k++) {
                    buffer[j][k] += x[k][p] * x[j][p];
                }
            }
        }

        return buffer;
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


    /*
        Test on reducing image dynamics trying to avoid saturation
        when applying the message
     */
    private char[][] ReduceDynamic(double[] sign, double factor, char[][] X) {
        int P = X[0].length;
        byte N = (byte) Math.round(Math.sqrt(X.length));
        int maxAbs = 0;
        for (double aSign : sign) {
            int temp = (int) Math.abs(Math.round(aSign * factor));
            if (temp > maxAbs) maxAbs = temp;
        }

        if (maxAbs < 0) maxAbs = 0;
        else if (maxAbs > 255) maxAbs = 255;

        for (int p = 0; p < P; p++) {
            for (int n = 0; n < N; n++) {
                X[n][p] = map(X[n][p], (char) 0, (char) 255, (char) (maxAbs), (char) (255 - maxAbs));
            }
        }

        return X;
    }

    private char map(char x, char in_min, char in_max, char out_min, char out_max) {
        return (char) ((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }

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
    private char[][] EmbedMessage(String message, double A, double[] signature, char[][] X) {
        int P = X[0].length;
        byte N = (byte) Math.round(Math.sqrt(X.length));
        //byte[][] Y = new byte[N][P]; the modification can be applied to X itself
        //Padding the message
        char[] c = new char[P / 8 + 1]; //One bit per block: one byte every eight blocks, adding one for non perfect division
        for (int k = 0; k < message.length(); k++) {
            c[k] = message.charAt(k);
        }
        for (int k = message.length(); k < P / 8; k++) {
            c[k] = '\0';
        }

        int sign;
        byte byteCounter = 0;
        char b;
        double e;
        for (int p = 0; p < P; p++) {
            b = (char) (c[p / 8] & 1 << byteCounter);
            if (b == 0) sign = -1;
            else sign = 1;

            for (int n = 0; n < N * N; n++) {   //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                e = (sign * A * signature[n] + X[n][p]);
                if (e < 0) e = 0;
                else if (e > 255) e = 255;

                X[n][p] = (char) Math.round(e);
            }

            byteCounter++;
            if (byteCounter == 8) byteCounter = 0;
        }

        return X;
    }

    /*
        Once the message is finally embedded, the representation
        needs to go back to a Bitmap to be then saved.
        Comments are similar to those on getting X
     */
    private Bitmap GetImageFromY(Bitmap result, char[][] y, int finHeight) {
        int N = (int) Math.round(Math.sqrt(y.length));
        int finWidht = (y[0].length / (finHeight / N)) * N;

        int rgb;

        for (int h = 0; h < finHeight; h += N) {
            for (int w = 0; w < finWidht; w += N) {
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) {
                        rgb = y[(a * N) + b][(finWidht / N) * (h / N) + (w / N)];
                        result.setPixel(w + b, h + a, Color.argb(255, rgb, rgb, rgb));
                    }
                }
            }
        }

        return result;
    }


    private void SaveSignature(double [] signature)
    {

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
            String path = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-signature.txt";
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
