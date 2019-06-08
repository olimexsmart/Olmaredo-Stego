package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static androidx.core.app.ActivityCompat.requestPermissions;

class OlmaredoUtil {
    private static final String TAG = "OlmaredoUtil";


    // Static utility class
    private OlmaredoUtil() {}

    /*
        Gets a image and the size of the blocks
        Returns the image cropped in a way that every dimension
        is a multiple of the block size.
     */
    static Bitmap ResizeNCrop(Bitmap original, int N, int finalDimension) {

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
    static byte[] HashKey(final char[] password, final byte[] salt, final int iterations, final int keyLength) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKey key = skf.generateSecret(spec);
            return key.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    // Returns an array of random numbers with no repetition, upper-bounded
    static int[] RandomArrayNoRepetitions(int numbersNeeded, int max, byte[] signature) {

        long seed = fromBytesToLong(signature);

        if (max < numbersNeeded) {
            throw new IllegalArgumentException("Can't ask for more numbers (" + numbersNeeded + ") than are available (" + max + ")");
        }

        Random rng = new Random(seed);
        // Note: use LinkedHashSet to maintain insertion order
        Set<Integer> generated = new LinkedHashSet<>();
        while (generated.size() < numbersNeeded) {
            Integer next = rng.nextInt(max);
            // As we're adding to a set, this will automatically do a containment check
            generated.add(next);
        }

        // Conversion to int array, not ideal but it simplifies not using iterators at the upper level
        // Possibly stupid and wasteful
        int[] retArray = new int[generated.size()];
        int i = 0;
        for (Integer aGenerated : generated) {
            retArray[i] = aGenerated;
            i++;
        }
        return retArray;
    }

    private static long fromBytesToLong(final byte[] b){
        long value = 0;
        for (int i = 0; i < b.length; i++) {
            value += ((long) b[i] & 0xffL) << (8 * i);
        }
        return value;
    }

    static double[] gaussianNoise(int howMuch, double variance, double mean, byte[] signature) {

        long seed = fromBytesToLong(signature);
        Random caos = new Random(seed);

        double[] noise = new double[howMuch];

        for (int i = 0; i < noise.length; i++) {
            noise[i] = caos.nextGaussian() * Math.sqrt(variance) + mean;
        }
        return noise;
    }

    //http://stackoverflow.com/questions/3331527/android-resize-a-large-bitmap-file-to-scaled-output-file
    //Useful to save RAM for a GUI preview
    static Bitmap ReadImageScaled(Activity activity, String fileNameOriginal, Uri outputFileUri) {
        InputStream in;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = Objects.requireNonNull(activity).getContentResolver().openInputStream(outputFileUri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            Objects.requireNonNull(in).close();

            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b;
            in = activity.getContentResolver().openInputStream(outputFileUri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = Objects.requireNonNull(b).getHeight();
                int width = b.getWidth();
                Log.d(TAG, "1th scale operation dimensions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            Objects.requireNonNull(in).close();

            Log.d(TAG, "bitmap size - width: " + b.getWidth() + ", height: " +
                    b.getHeight());
            return ExifUtil.rotateBitmap(fileNameOriginal, b);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    //Read a full size image, like for processing it
    static Bitmap ReadImage(Activity activity, String fileNameOriginal, Uri outputFileUri) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1; //Set as you want but bigger than one
        options.inJustDecodeBounds = false;

        try {
            InputStream imageStream = Objects.requireNonNull(activity).getContentResolver().openInputStream(outputFileUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            Log.v(TAG, outputFileUri.toString());
            return ExifUtil.rotateBitmap(fileNameOriginal, bitmap);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "File not found.");
        }
        return null;
    }

    // Necessary to Android 6.0 and above for run time permissions
    static boolean CheckPermissions(Fragment fragment, Context contextCompat, int PERMISSION_CODE) {
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(contextCompat), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(contextCompat, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

            fragment.requestPermissions(permissions, PERMISSION_CODE);

            return false;
        } else
            return true;
    }

    //Takes an absolute path of a txt file and gives back its contents, this simple
    static String ReadTextFile(Context context, String path) {
        StringBuilder text = new StringBuilder();
        BufferedReader br;
        String line;
        File f = new File(path);

        try {
            br = new BufferedReader(new FileReader(f));
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            Toast.makeText(context, f.getName() + " not found", Toast.LENGTH_SHORT).show();
        }

        return text.toString();
    }


}
