package com.example.gliol.olmaredostego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Character.isLetter;

/*
    TODO add paste button https://developer.android.com/guide/topics/text/copy-paste.html
    TODO remove all references to the generic signature
 */
public class DecodeFragment extends Fragment implements TaskManager {
    private final int REQ_CODE = 2222;
    private final String TAG = "DecodeFragment";
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";
    private static final String bundleTaskProgress = "bTP";
    private static final String bundleWasTaskRunning = "bWTR";
    private static final String bundleTaskType = "bTT";


    Button photo;
    ImageView preview;
    RadioGroup groupRadio;
    EditText etCustom;
    Button decode;
    TextView result;
    Button toClipboard;
    RadioButton rbOriginal;
    RadioButton rbGeneric;
    DecodeFragment thisthis;
    String fileNameOriginal;
    Uri outputFileUri = null;
    ProgressDialog progressDialog;
    int taskProgress;
    String taskType;
    boolean wasTaskRunning = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If we are returning here from a screen orientation
        // and the AsyncTask is still working, re-create and display the
        // progress dialog.
        if (wasTaskRunning) {
            onTaskStarted(taskType);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.decoding, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        thisthis = this;

        //Radio button e altre amenità
        photo = (Button) view.findViewById(R.id.btBrowseDecode);
        preview = (ImageView) view.findViewById(R.id.ivPreview);
        groupRadio = (RadioGroup) view.findViewById(R.id.rgSignatureSource);
        etCustom = (EditText) view.findViewById(R.id.etCustom);
        decode = (Button) view.findViewById(R.id.btDecode);
        result = (TextView) view.findViewById(R.id.twShowResult);
        toClipboard = (Button) view.findViewById(R.id.btClipboardText);
        rbOriginal = (RadioButton) view.findViewById(R.id.rbOriginal);
        rbGeneric = (RadioButton) view.findViewById(R.id.rbGeneric);

        if (savedInstanceState != null) {
            fileNameOriginal = savedInstanceState.getString(bundleNameOriginal);
            wasTaskRunning = savedInstanceState.getBoolean(bundleWasTaskRunning);
            if (savedInstanceState.containsKey(bundleUri)) {
                outputFileUri = Uri.parse(savedInstanceState.getString(bundleUri));
                Bitmap im = ReadImageScaled();
                if (im != null) {
                    preview.setImageBitmap(im);
                }
            }
            if (savedInstanceState.containsKey(bundleTaskProgress))
                taskProgress = savedInstanceState.getInt(bundleTaskProgress);
            if(savedInstanceState.containsKey(bundleTaskType))
                taskType = savedInstanceState.getString(bundleTaskType);
            Log.v(TAG, "Activity restored.");
        } else {
            fileNameOriginal = "nothing here";
            Log.v(TAG, "Activity NOT restored.");
        }


        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQ_CODE);
            }
        });

        groupRadio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.rbOriginal) {
                    etCustom.setVisibility(View.VISIBLE);
                } else {
                    etCustom.setVisibility(View.INVISIBLE);
                    etCustom.setText("");
                }
            }
        });

        decode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //First thing to do is check if the file is valid
                if (new File(fileNameOriginal).exists()) {
                    StartActivity activity = (StartActivity) getActivity();
                    toClipboard.setEnabled(true);
                    //Renamed to lighten up code readability
                    int blockSize = activity.BlockSize;
                    String customKey = etCustom.getText().toString();

                    if (activity.inColor) {
                        double[] signR;
                        double[] signG;
                        double[] signB;
                        //If there is written something and is coherent with the necessary signature, keep in mind that every number takes 5 characters
                        //But since we are in color mode, we are expecting the same thin three times
                        if (customKey.length() == blockSize * blockSize * 15) { //960 on standard blocksize, quite a lot
                            Toast.makeText(getContext(), "Using original signature", Toast.LENGTH_SHORT).show();
                            //Dividing the string into the respective signature
                            signR = StringToSignature(customKey.substring(0, blockSize * blockSize * 5));
                            signG = StringToSignature(customKey.substring(blockSize * blockSize * 5, blockSize * blockSize * 10));
                            signB = StringToSignature(customKey.substring(blockSize * blockSize * 10, blockSize * blockSize * 15));
                        } else { //Get the default one
                            signR = GetDefaultSignature(blockSize);
                            signG = GetDefaultSignature(blockSize);
                            signB = GetDefaultSignature(blockSize);
                            Toast.makeText(getContext(), "Using generic signature", Toast.LENGTH_SHORT).show();
                        }

                        MessageDecodingColor messageDecodingColor = new MessageDecodingColor(thisthis, getContext(), signR, signG, signB);
                        messageDecodingColor.execute(ReadImage());
                    } else { //In black and white
                        //If there is written something and is coherent with the necessary signature, keep in mind that every number takes 5 characters
                        double[] signatureBW;
                        if (customKey.length() == blockSize * blockSize * 5) {
                            signatureBW = StringToSignature(customKey);
                            Toast.makeText(getContext(), "Using original signature", Toast.LENGTH_SHORT).show();
                        } else { //Get the default one
                            signatureBW = GetDefaultSignature(blockSize);
                            Toast.makeText(getContext(), "Using generic signature", Toast.LENGTH_SHORT).show();
                        }
                        MessageDecoding messageDecoding = new MessageDecoding(getContext(), signatureBW, thisthis);
                        messageDecoding.execute(ReadImage());
                    }
                }
                else
                {
                    Toast.makeText(getContext(), "Choose a valid image.", Toast.LENGTH_LONG).show();
                }
            }
        });

        toClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("nothing", result.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Message copied in the clipboard", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private double[] StringToSignature(String key)
    {
        double[] signature = new double[key.length() / 5];
        //Quel momento in cui vorresti scrivere una bestemmia nel codice ma poi sai che c'è Martino che legge e non vuoi offenderlo
        char[] keyChar = key.toCharArray();
        for (int i = 0; i < signature.length; i++) {   //Bit level ascii hack, from char to int and at the correct position
            signature[i] = (keyChar[i * 5 + 1] - 48) / 10000.0 + (keyChar[i * 5 + 2] - 48) / 1000.0 + (keyChar[i * 5 + 3] - 48) / 100.0 + (keyChar[i * 5 + 4] - 48) / 10.0;
            //Change sign if the prefix is a letter
            if (isLetter(keyChar[i * 5]))
                signature[i] *= -1;
        }

        return signature;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString(bundleNameOriginal, fileNameOriginal);
        outState.putBoolean(bundleWasTaskRunning, wasTaskRunning);
        if (outputFileUri != null)
            outState.putString(bundleUri, outputFileUri.toString());
        if(wasTaskRunning) {
            outState.putInt(bundleTaskProgress, taskProgress);
            outState.putString(bundleTaskType, taskType);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK && null != data) {
            outputFileUri = data.getData();
            //At this point we have the image selected Uri
            //Now get the absolute path into a nice String

            // SDK < API11
            if (Build.VERSION.SDK_INT < 11)
                fileNameOriginal = RealPathUtil.getRealPathFromURI_BelowAPI11(getContext(), outputFileUri);

                // SDK >= 11 && SDK < 19
            else if (Build.VERSION.SDK_INT < 19)
                fileNameOriginal = RealPathUtil.getRealPathFromURI_API11to18(getContext(), outputFileUri);

                // SDK > 19 (Android 4.4)
            else
                fileNameOriginal = RealPathUtil.getRealPathFromURI_API19(getContext(), outputFileUri);

            Bitmap im = ReadImageScaled();
            if (im != null) {
                preview.setImageBitmap(im);
                rbGeneric.setEnabled(true);
                rbOriginal.setEnabled(true);
                decode.setEnabled(true);
                Log.v(TAG, "Choosen photo.");
            } else {
                rbGeneric.setEnabled(false);
                rbOriginal.setEnabled(false);
                decode.setEnabled(false);
                Log.v(TAG, "Image is null");
            }
        }
    }

    private Bitmap ReadImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1; //Set as you want but bigger than one
        options.inJustDecodeBounds = false;

        try {
            InputStream imageStream = getActivity().getContentResolver().openInputStream(outputFileUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            Log.v(TAG, outputFileUri.toString());
            return ExifUtil.rotateBitmap(fileNameOriginal, bitmap);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "File not found.");
        }
        return null;
    }

    //http://stackoverflow.com/questions/3331527/android-resize-a-large-bitmap-file-to-scaled-output-file
    private Bitmap ReadImageScaled() {
        InputStream in = null;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = getActivity().getContentResolver().openInputStream(outputFileUri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b = null;
            in = getActivity().getContentResolver().openInputStream(outputFileUri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                Log.d(TAG, "1th scale operation dimenions - width: " + width + ", height: " + height);

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
            in.close();

            Log.d(TAG, "bitmap size - width: " + b.getWidth() + ", height: " +
                    b.getHeight());
            return ExifUtil.rotateBitmap(fileNameOriginal, b);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }


    @Override
    public void onTaskStarted(String type) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle(type);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();

        wasTaskRunning = true;
        taskType = type;
        setRetainInstance(true);
    }

    @Override
    public void onTaskProgress(int progress) {
        progressDialog.setProgress(progress);
        taskProgress = progress;
    }

    @Override
    public void onTaskCompleted(Bitmap bm, double[] signature) {
        //Nothing to do here
    }

    @Override
    public void onTaskCompleted(Bitmap bm, double[] signatureR, double[] signatureG, double[] signatureB) {
        //Nothing to do here
    }

    @Override
    public void onTaskCompleted(String message) {
        result.setText(message);
        toClipboard.setEnabled(true);

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        wasTaskRunning = false;
        setRetainInstance(false);
    }

    //https://androidresearch.wordpress.com/2013/05/10/dealing-with-asynctask-and-screen-orientation/
    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDetach();
    }

    private double[] GetDefaultSignature(int blockSize)
    {
        /* Random caos = new Random();
        double [] signature = new double[blockSize * blockSize];
        for (int i = 0; i < signature.length; i++) {   //Consider implementing a function of i that better mimics the pdf of the vector
            signature[i] = caos.nextInt(2500) / 10000.0;
            if (caos.nextBoolean())
                signature[i] *= -1;
        } */

        if(blockSize == 4)
        {
            double[] signature = { -0.068540633,
                    0.121830424,
                    0.03919574,
                    0.042899606,
                    0.105406926,
                    -0.200084455,
                    -0.129697921,
                    -0.072501737,
                    -0.1163317,
                    0.239463267,
                    0.111641873,
                    0.09835339,
                    0.083611903,
                    -0.176333331,
                    -0.018520831,
                    -0.063327289 };

            return signature;

        } else if(blockSize == 8)
        {
            double[] signature = { 0.013550799,
                    -0.034843473,
                    -0.0043959,
                    0.048517695,
                    -0.002238234,
                    0.059255482,
                    -0.078559989,
                    0.014244391,
                    0.014213722,
                    -0.015722614,
                    0.04840409,
                    -0.035529448,
                    0.008763869,
                    -0.072943419,
                    0.011466028,
                    0.027400204,
                    0.020102668,
                    0.009929106,
                    -0.083584044,
                    0.02262789,
                    -0.01439796,
                    0.066309548,
                    0.002827306,
                    -0.018821991,
                    -0.021130294,
                    -0.035588109,
                    0.066046005,
                    -0.007292681,
                    0.060796618,
                    -0.147714819,
                    0.122044845,
                    -0.05869794,
                    0.038249964,
                    0.015826903,
                    -0.057179628,
                    0.041005991,
                    -0.075666201,
                    0.188009009,
                    -0.138501923,
                    0.081154206,
                    -0.033524155,
                    -0.006670449,
                    0.013594852,
                    0.01036181,
                    -0.044380213,
                    -0.041699048,
                    -0.005159213,
                    0.005260393,
                    0.016821012,
                    0.048322546,
                    -0.049773114,
                    0.010679569,
                    -0.033221734,
                    0.030250527,
                    0.033002373,
                    -0.000434596,
                    0.02085235,
                    -0.061169812,
                    0.037657237,
                    -0.021991542,
                    0.049414188,
                    -0.039358332,
                    0.020101502,
                    -0.036807504};

            return signature;

        } else if (blockSize==16)
        {
            double[] signature = {
                    -0.009529035,
                    0.010431936,
                    0.023213675,
                    -0.015983185,
                    0.046889032,
                    -0.036110641,
                    -0.011784772,
                    0.003591379,
                    -0.001660228,
                    -0.008836464,
                    0.025865287,
                    -0.010714925,
                    -0.003962822,
                    0.010806413,
                    -0.002418591,
                    0.013975269,
                    -0.012610961,
                    -0.013765788,
                    0.029387502,
                    -0.044606179,
                    -0.016054597,
                    0.011391592,
                    0.010795003,
                    0.074938032,
                    -0.003583655,
                    -0.039853597,
                    0.002034122,
                    0.014434159,
                    0.046694342,
                    -0.016137955,
                    -0.016941738,
                    -0.030068744,
                    -0.002907361,
                    0.015609275,
                    0.003125393,
                    -0.003561968,
                    0.004712717,
                    0.009243541,
                    0.011408505,
                    -0.042455353,
                    -0.023420702,
                    -0.019323577,
                    0.048501979,
                    -0.022272255,
                    -0.019215322,
                    -0.02076081,
                    0.056444172,
                    -0.024498976,
                    0.016702019,
                    -0.039023644,
                    -0.002648204,
                    0.008288538,
                    0.025028595,
                    -0.024927293,
                    -0.057010404,
                    0.075247357,
                    -0.010462531,
                    0.035544606,
                    -0.046154031,
                    -0.013482714,
                    -0.010760907,
                    0.043984241,
                    0.002637241,
                    -0.002584549,
                    -0.000815998,
                    0.03716009,
                    0.001321672,
                    -0.052239774,
                    -0.015765025,
                    0.05366541,
                    0.00810626,
                    -0.085978181,
                    0.008927499,
                    0.001007659,
                    0.02268843,
                    0.002279618,
                    -0.009885639,
                    -0.02498098,
                    -0.02393891,
                    0.033270219,
                    0.011980047,
                    -0.030193455,
                    -0.025273356,
                    0.026375985,
                    0.02463467,
                    -0.02164717,
                    0.0250256,
                    0.01812787,
                    0.015396435,
                    0.009291049,
                    -0.055245657,
                    0.058690637,
                    -0.045691007,
                    -0.000577767,
                    0.046419746,
                    -0.014115091,
                    -0.015239015,
                    0.011005538,
                    0.009101344,
                    0.041140477,
                    0.001088668,
                    0.018490718,
                    -0.035328577,
                    0.030563472,
                    -0.039716703,
                    0.00750596,
                    0.000687826,
                    0.03594012,
                    -0.017950953,
                    0.044000769,
                    -0.035693918,
                    -0.038196908,
                    -0.012920034,
                    -0.008503107,
                    -0.019418137,
                    -0.051614098,
                    -0.004685128,
                    0.003121728,
                    0.016755731,
                    -0.039426658,
                    0.034454015,
                    0.005099273,
                    -0.016667772,
                    -0.051507048,
                    0.036122737,
                    -0.01379325,
                    0.012217565,
                    0.047835235,
                    0.026275524,
                    0.046788703,
                    0.032434796,
                    0.053770385,
                    -0.058808627,
                    0.045322175,
                    -0.036854858,
                    0.009394169,
                    0.022396831,
                    0.022694439,
                    -0.036015375,
                    0.060599689,
                    -0.016480945,
                    -0.020947962,
                    -0.016216212,
                    0.016734575,
                    -0.022639928,
                    -0.033434954,
                    -0.047335476,
                    -0.041149122,
                    0.047901382,
                    -0.079011815,
                    0.037310153,
                    -0.000769007,
                    -0.003072288,
                    0.015704052,
                    -0.025428118,
                    -0.012601145,
                    -0.015126764,
                    0.02845968,
                    -0.020127457,
                    0.005185131,
                    0.013748712,
                    0.0213051,
                    0.058759443,
                    0.008552138,
                    -0.022800499,
                    0.052020889,
                    0.004375218,
                    -0.032353836,
                    -0.010813649,
                    -0.001154243,
                    -0.000919455,
                    0.000691146,
                    0.03256573,
                    -0.008711157,
                    0.003117966,
                    -0.001045578,
                    -0.039518431,
                    0.033994807,
                    -0.031604613,
                    0.032729095,
                    -0.01974616,
                    0.001021991,
                    -0.013381643,
                    0.043223055,
                    -0.03840998,
                    0.004158352,
                    -0.03000004,
                    -0.010971116,
                    -0.022041364,
                    0.001573312,
                    0.009568617,
                    -0.016212065,
                    0.015563384,
                    -0.032275487,
                    0.053544908,
                    -0.035387023,
                    -0.020638518,
                    -0.012253589,
                    0.050941067,
                    -0.004185101,
                    0.031395128,
                    0.025028899,
                    0.030429049,
                    -0.007981029,
                    0.041799185,
                    0.002073908,
                    -0.014109227,
                    0.04283406,
                    -0.01151731,
                    -0.002847278,
                    -0.026964826,
                    -0.001976965,
                    0.025384871,
                    -0.043618083,
                    -0.010411725,
                    0.013201403,
                    -0.007784869,
                    -0.023405462,
                    -0.040177341,
                    0.000603565,
                    -0.027300653,
                    -0.015388243,
                    -0.006929794,
                    -0.001168801,
                    -0.009723083,
                    0.012127144,
                    -0.002332186,
                    0.01791031,
                    0.001705129,
                    0.032648689,
                    0.019340438,
                    -0.009208587,
                    0.013389752,
                    0.018398983,
                    0.027267288,
                    -0.023012732,
                    0.02002381,
                    0.039199495,
                    -0.024643951,
                    -0.015600604,
                    0.037263203,
                    -0.042785968,
                    -0.004247194,
                    -0.000715155,
                    0.023854075,
                    -0.007698244,
                    -0.006466873,
                    -0.025223931,
                    0.013941737,
                    -0.026682984,
                    -0.021380614,
                    0.022778104,
                    -0.010761095,
                    -0.036239833,
                    0.027485765,
                    -0.002503282
            };

            return signature;
        }

        return null; //non ci va mai (speriamo...)
    }
}