package com.olmaredo.gliol.olmaredostego;

import android.graphics.Bitmap;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

class OlmaredoUtil {

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
}
