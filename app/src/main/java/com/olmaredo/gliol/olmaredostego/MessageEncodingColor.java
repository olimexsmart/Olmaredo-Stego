package com.olmaredo.gliol.olmaredostego;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/*

 */
public class MessageEncodingColor extends AsyncTask<Bitmap, Integer, Bitmap> {
    private final String TAG = "MessageEncodingColor";

    Context context;
    private String message;
    private char[] key;
    private byte N; //Block size
    private int finHeight;
    private double strength;
    private TaskManager callerFragment;

    //We don't wont this to be called without a message specified.
    private MessageEncodingColor() {
    }

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


        int H = params[0].getHeight();
        int W = params[0].getWidth();
        int Nsqr = N * N;

        byte[] signature = HashKey(key, "4444".getBytes(), 10000, Nsqr * 8);
        Log.v(TAG, "Signature length: " + signature.length);
        Log.v(TAG, "Signature values: " + signature[0] + " " + signature[32] + " " + signature[63]);

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
        // TODO remove this for and understand consequences
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
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signature[(a * N) + b] + Color.red(mutableBitmap.getPixel((w * N) + b, (h * N) + a)));

                        //Clipping to avoid over/under flow, good idea could be reducing the dynamic range instead.
                        if (e < 0) e = 0;
                        else if (e > 255) e = 255;

                        r = (int) Math.round(e);
                        g = Color.green(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        blu = Color.blue(mutableBitmap.getPixel((w * N) + b, (h * N) + a));
                        mutableBitmap.setPixel((w * N) + b, (h * N) + a, Color.argb(255, r, g, blu));
                    }
                }
            } else if (p % 3 == 1) {   //Applying the bit to the block
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signature[(a * N) + b] + Color.green(mutableBitmap.getPixel((w * N) + b, (h * N) + a)));
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
                for (int a = 0; a < N; a++) {
                    for (int b = 0; b < N; b++) //Loop on block's columns
                    {
                        e = (sign * strength * signature[(a * N) + b] + Color.blue(mutableBitmap.getPixel((w * N) + b, (h * N) + a)));
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

            publishProgress((int) ((p / (double) P) * 100));
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


    /*
        Gets a image and the size of the blocks
        Returns the image cropped in a way that every dimension
        is a multiple of the block size.
     */
    //TODO put these into a static encoding utility class
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
