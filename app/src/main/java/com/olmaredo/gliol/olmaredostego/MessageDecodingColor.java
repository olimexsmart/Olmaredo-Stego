package com.olmaredo.gliol.olmaredostego;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*
	This activity manges the decoding tab.
*/
public class MessageDecodingColor extends AsyncTask<Bitmap, Integer, String> {
    private static final String TAG = "MessageDecodingColor";
    private static final double SCALE = 1000000.0f;

    Context context;
    private TaskManager callerFragment;
    private char[] key;
    private byte N;

    private MessageDecodingColor() {
    }

    public MessageDecodingColor(TaskManager result, Context c, char[] key, byte blockSize) {
        context = c;
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
            publishProgress((int)((h / (double)H) * 70));
        }

        Log.v(TAG, "Created Y matrices.");
        String result = "";
        char[] buffer = new char[Xr.length]; //Used to copy one column
        char c = 0;
        int I = Xr[0].length * 3;
        byte[] signatureR = HashKey(key, "4444".getBytes(), 10000, N * N * 8);
        byte[] signatureG = HashKey(key, "7777".getBytes(), 10000, N * N * 8);
        byte[] signatureB = HashKey(key, "9999".getBytes(), 10000, N * N * 8);
        //Log.v(TAG, "Signature length: " + signature.length);
        //Log.v(TAG, "Signature values: " + signature[0] + " " + signature[32] + " " + signature[63]);

        for (int i = 0; i < I; i++) {
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
            publishProgress((int)((i / (double)I) * 30) + 70);
        }

        publishProgress(100);
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
    private boolean GetSign(byte[] signature, char[] block) {
        double buffer = 0;
        for (int i = 0; i < signature.length; i++) {
            buffer += (double)signature[i] * block[i] / SCALE;
        }

        return buffer > 0;
    }

    // Hash key, from string to array of bytes
    private byte[] HashKey(final char[] password, final byte[] salt, final int iterations, final int keyLength) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKey key = skf.generateSecret(spec);
            return key.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
