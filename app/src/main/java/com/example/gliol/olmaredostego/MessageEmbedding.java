package com.example.gliol.olmaredostego;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.renderscript.ScriptGroup;
import android.util.Log;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * Created by gliol on 12/05/2016.
 */
public class MessageEmbedding extends AsyncTask<Bitmap, Integer, Bitmap> {
    private static final String TAG = "MessageEmbedding";

    ProgressDialog progressDialog;
    Context context;
    String message;
    int blockSize = 8;
    int finHeight = 480;

    //We don't wont this to be called without a message specified.
    private MessageEmbedding(){}

    public MessageEmbedding(Context c, String message)
    {
        context = c;
        this.message = message;
    }

    public MessageEmbedding(Context c, String message, int blockSize)
    {
        context = c;
        this.message = message;
        this.blockSize = blockSize;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Embedding message.");
        progressDialog.setMessage("Resizing...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(true);
        progressDialog.setIndeterminate(false);

        progressDialog.show();

        Log.v(TAG, "PreExecute terminated");
    }

    @Override
    protected Bitmap doInBackground(Bitmap... params) {

        Bitmap buffer = ResizeNCrop(params[0], blockSize, finHeight);
        Log.v(TAG, "Image resized: " + buffer.getHeight() + " " + buffer.getWidth());

        //progressDialog.setMessage("Gray scaling...");
        publishProgress(10);
        buffer = ToGrayscale(buffer);
        Log.v(TAG, "Gray-scaled.");

        //progressDialog.setMessage("Creating X matrix...");
        publishProgress(25);
        byte[][] X = GetXMatrix(buffer, blockSize);
        buffer.recycle(); //Free-up memory
        Log.v(TAG, "Created X matrix.");

        //progressDialog.setMessage("Calculatig autocorrelation...");
        publishProgress(50);
        double[][] autocorrelation = GetAutocorrelation(X);
        X = null; //Hoping that the GC will act fast
        Log.v(TAG, "Created autocorrelation matrix.");

        //progressDialog.setMessage("Calculating signature vector...");
        publishProgress(75);
        double[] s = GetSignatureVector(autocorrelation);
        autocorrelation = null;
        Log.v(TAG, "Got signature vector.");

        //Message embedding here
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        //Setting progress percentage
        progressDialog.setProgress(values[0]);
    }

    //################################ PROCESSING FUNCTIONS #############################################

    /*
        Gets a image and the size of the blocks
        Returns the image cropped in a way that every dimension
        is a multiple of the block size.
     */
    private Bitmap ResizeNCrop(Bitmap original, int N, int finalHeight){

        double ratio = (double) original.getHeight() / finalHeight;
        int finalWidth = original.getWidth() / (int)ratio;

        Bitmap resized = original.createScaledBitmap(original, finalWidth, finalHeight, false);

        return Bitmap.createBitmap(resized, 0, 0, finalWidth - (finalWidth % N), finalHeight - (finalHeight % N));
    }

    /*
        Gets a Bitmap
        Returns the same Bitmap in gray scale, setting all RGB components to the same value
     */
    private Bitmap ToGrayscale(Bitmap bmpOriginal){
        Bitmap bmpGrayscale = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        int pixel;
        int r, g, b;
        int a;

        for(int x = 0; x < bmpOriginal.getWidth(); ++x) {
            for(int y = 0; y < bmpOriginal.getHeight(); ++y) {
                // get one pixel color
                pixel = bmpOriginal.getPixel(x, y);
                // retrieve color of all channels
                a = Color.alpha(pixel);
                r = Color.red(pixel);
                g = Color.green(pixel);
                b = Color.blue(pixel);
                // Y = 0.2126 R + 0.7152 G + 0.0722 B  as in Rec 709 (Wiki)
                r = g = b = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);
                // set new pixel color to output bitmap
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
    private byte[][] GetXMatrix(Bitmap image, int N)
    {//Think about checking if the image is in grayscale
        int H = image.getHeight();
        int W = image.getWidth();

        byte[][] X = new byte[N * N][(H * W) / N * N];

        for (int h = 0; h < H; h += N)  //Loop on image's rows (going down)
        {
            for (int w = 0; w < W; w += N)  //Loop on image's columns (from left to right for each row)
            {
                for (int a = 0; a < N; a++) //Loop on block's rows
                {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        X[(a * N) + b][(W / N) * (h / N) + (w / N)] = (byte)Color.green(image.getPixel(w + b, h + a));
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
    private double[][] GetAutocorrelation (byte[][] x)
    {
        int P = x[0].length;
        int Nsqr = x.length;
        double[][] buffer = new double[Nsqr][Nsqr];

        for (int p = 0; p < P; p++)
        {
            for (int j = 0; j < Nsqr; j++)
            {
                for (int k = 0; k < Nsqr; k++ )
                {
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
    private double[] GetSignatureVector(double[][] matrix)
    {   //It's a N x N matrix
        int N = matrix.length;

        Matrix A = new Matrix(matrix);
        A = A.transpose().times(A);

        // compute the spectral decomposition
        EigenvalueDecomposition e = A.eig();
        Matrix V = e.getV();    //<-- Eigenvalues
        Matrix D = e.getD();    //<-- Eigenvectors

        //Select the smallest eigenvalue and return the corresponding eigenvector
        int smallestI = 0;
        double smallest = V.get(0, 0);
        for (int k = 1; k < N; k++)
        {
            if(V.get(k, k) < smallest)
            {
                smallest = V.get(k, k);
                smallestI = k;
            }
        }

        //Copy into a one dimensional array
        double[][] temp = D.getArray();
        double[] result = new double[N];
        for(int n = 0; n < N; n++)
            result[n] = temp[n][smallestI];

        return result;
    }

    /*
        Embedding the message into the image, combining
        the X matrix, the signature vector and the image
        following the formula: Y = A * b * S + X
        Where A is a coefficient meaning the embedding power
        and b is the corresponding bit of the image.
     */
    private byte[][] EmbedMessage(String message, double A, double[] sign, byte[][] X)
    {
        int P = X[0].length;
        int N = (int) Math.round(Math.sqrt(X.length));
        byte[][] Y = new byte[N][P];

        for(int p = 0; p < P; p++)
        {
            //TODO
        }

        return Y;
    }

    /*
        Once the message is finally embedded, the representation
        needs to go back to a Bitmap to be then saved.
        Comments are similar to those on getting X
     */
    private Bitmap GetImageFromY(byte[][] y, int finHeight)
    {
        int finWidht = y[0].length / finHeight;
        int N = (int) Math.round(Math.sqrt(y.length));
        Bitmap result = Bitmap.createBitmap(finWidht, finHeight, Bitmap.Config.ARGB_8888);
        int rgb;

        for (int h = 0; h < finHeight; h += N)
        {
            for (int w = 0; w < finWidht; w += N)
            {
                for(int a = 0; a < N; a++)
                {
                    for (int b = 0; b < N; b++)
                    {
                        rgb = y[(a * 8) + b][(h / N) + (w / N)];
                        result.setPixel(w + b, h + a, Color.argb(255, rgb, rgb, rgb));
                    }
                }
            }
        }

        return result;
    }

}
