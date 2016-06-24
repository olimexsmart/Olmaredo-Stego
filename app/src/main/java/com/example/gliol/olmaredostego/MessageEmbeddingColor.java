package com.example.gliol.olmaredostego;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/*
    TODO manage pictures on portrait, the resasing is done wrong
 */
public class MessageEmbeddingColor extends AsyncTask<Bitmap, Integer, Bitmap> {
    private final String TAG = "MessageEmbeddingColor";

    ProgressDialog progressDialog;
    Context context;
    String message;
    byte N = 8; //Block size
    int finHeight = 480; //This could be useful to add in a constructor, DONE
    int strength = 1;
    GetResultEncodingColor returnResult;
    double[] signatureR;
    double[] signatureG;
    double[] signatureB;

    //We don't wont this to be called without a message specified.
    private MessageEmbeddingColor() {
    }

    public MessageEmbeddingColor(GetResultEncodingColor result, Context c, String message) {
        context = c;
        this.message = message;
        this.returnResult = result;
    }

    public MessageEmbeddingColor(GetResultEncodingColor result, Context c, String message, byte blockSize, int cropSize, int strength) {
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
        progressDialog.setTitle("Embedding message.");
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
        publishProgress(10);

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

        publishProgress(50);
        signatureR = GetSignatureVector(GetAutocorrelation(Xr));
        publishProgress(60);
        signatureG = GetSignatureVector(GetAutocorrelation(Xg));
        publishProgress(70);
        signatureB = GetSignatureVector(GetAutocorrelation(Xb));
        publishProgress(80);
        //X = null; //Hoping that the GC will act fast // sadly there is the need of it
        Log.v(TAG, "Created autocorrelation matrices and signature vectors.");

        /*
        Encoding message here, the logic is to use all planes consequently following RGB order.
        So the first three bits are coded into the same block on different colors, the second block
        on the three colors contains the next three bits and so on
         */
        int P = Xr[0].length * 3; //All the same dimensions, but this time we have three times the information
        //byte N = (byte) Math.round(Math.sqrt(X.length));
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
        //TODO This loop needs to be optimized with P not multiplied by 3 and without the if else statement inside
        for (int p = 0; p < P; p++) { //Remeber that the P stands for the number of bits to embed in the image
            b = (char) (c[p / 8] & 1 << byteCounter);
            if (b == 0) sign = -1;
            else sign = 1;

            //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
            if (P % 3 == 0) { //On red plane
                for (int n = 0; n < N * N; n++) {
                    e = (sign * strength * signatureR[n] + Xr[n][p / 3]);
                    if (e < 0) e = 0;
                    else if (e > 255) e = 255;

                    Xr[n][p / 3] = (char) Math.round(e);
                }
            } else if (P % 3 == 1) { //On green plane
                for (int n = 0; n < N * N; n++) {
                    e = (sign * strength * signatureG[n] + Xg[n][p / 3]);
                    if (e < 0) e = 0;
                    else if (e > 255) e = 255;

                    Xg[n][p / 3] = (char) Math.round(e);
                }
            } else { //On blu plane if P % 3 == 2
                for (int n = 0; n < N * N; n++) {
                    e = (sign * strength * signatureB[n] + Xb[n][p / 3]);
                    if (e < 0) e = 0;
                    else if (e > 255) e = 255;

                    Xb[n][p / 3] = (char) Math.round(e);
                }
            }
        }
        byteCounter++;
        if (byteCounter == 8) byteCounter = 0;

        publishProgress(90);
        Log.v(TAG, "Embedded message.");

        int finWidht = params[0].getWidth();
        int r;
        int g;
        int blu; //Otherwise redefinition error

        for (int h = 0; h < finHeight; h += N) {
            for (int w = 0; w < finWidht; w += N) {
                for (int a = 0; a < N; a++) {
                    for (int z = 0; z < N; z++) {
                        r = Xr[(a * N) + z][(finWidht / N) * (h / N) + (w / N)];
                        g = Xg[(a * N) + z][(finWidht / N) * (h / N) + (w / N)];
                        blu = Xb[(a * N) + z][(finWidht / N) * (h / N) + (w / N)];
                        params[0].setPixel(w + z, h + a, Color.argb(255, r, g, blu));
                    }
                }
            }
        }

        return params[0];

    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        //Setting progress percentage
        progressDialog.setProgress(values[0]);
    }


    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        progressDialog.hide();
        progressDialog.dismiss();
        this.returnResult.onResultsReady(bitmap, signatureR, signatureG, signatureB);
    }


    /*
        Gets a image and the size of the blocks
        Returns the image cropped in a way that every dimension
        is a multiple of the block size.
     */

    private Bitmap ResizeNCrop(Bitmap original, int N, int finalHeight) {

        double ratio = (double) original.getHeight() / finalHeight;
        int finalWidth = original.getWidth() / (int) ratio;

        Bitmap resized = original.createScaledBitmap(original, finalWidth, finalHeight, false);

        return Bitmap.createBitmap(resized, 0, 0, finalWidth - (finalWidth % N), finalHeight - (finalHeight % N));
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

}
