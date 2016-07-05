package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Character.isLetter;

/*

 */
public class DecodeFragment extends Fragment implements TaskManager {
    private final int REQ_CODE = 2222;
    private final String TAG = "DecodeFragment";
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";
    private static final String bundleTaskProgress = "bTP";
    private static final String bundleWasTaskRunning = "bWTR";
    private static final String bundleTaskType = "bTT";
    private static final String bundleResultText = "bRT";


    Button photo;
    ImageView preview;
    EditText keySignature;
    Button decode;
    TextView result;
    Button toClipboard;
    Button pasteKey;
    DecodeFragment thisthis;
    String fileNameOriginal;
    String resultText = "";
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
        keySignature = (EditText) view.findViewById(R.id.etCustom);
        decode = (Button) view.findViewById(R.id.btDecode);
        result = (TextView) view.findViewById(R.id.twShowResult);
        toClipboard = (Button) view.findViewById(R.id.btClipboardText);
        pasteKey = (Button) view.findViewById(R.id.btPaste);


        if (savedInstanceState != null) {
            fileNameOriginal = savedInstanceState.getString(bundleNameOriginal);
            wasTaskRunning = savedInstanceState.getBoolean(bundleWasTaskRunning);
            if (savedInstanceState.containsKey(bundleUri)) {
                outputFileUri = Uri.parse(savedInstanceState.getString(bundleUri));
                Bitmap im = ReadImageScaled();
                if (im != null) {
                    preview.setImageBitmap(im);
                    decode.setEnabled(true);
                }
            }
            if (savedInstanceState.containsKey(bundleTaskProgress))
                taskProgress = savedInstanceState.getInt(bundleTaskProgress);
            if(savedInstanceState.containsKey(bundleTaskType))
                taskType = savedInstanceState.getString(bundleTaskType);
            if(savedInstanceState.containsKey(bundleResultText))
            {
                resultText = savedInstanceState.getString(bundleResultText);
                result.setText(resultText);
                toClipboard.setEnabled(true);
            }
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


        decode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //First thing to do is check if the file is valid
                if (new File(fileNameOriginal).exists()) {
                    StartActivity activity = (StartActivity) getActivity();
                    toClipboard.setEnabled(true);
                    //Renamed to lighten up code readability
                    int blockSize = activity.BlockSize;
                    String customKey = keySignature.getText().toString();

                    if (activity.inColor) {
                        double[] signR;
                        double[] signG;
                        double[] signB;
                        //If there is written something and is coherent with the necessary signature, keep in mind that every number takes 5 characters
                        //But since we are in color mode, we are expecting the same thin three times
                        if (customKey.length() == blockSize * blockSize * 15) { //960 on standard blocksize, quite a lot
                            Toast.makeText(getContext(), "Key is valid", Toast.LENGTH_SHORT).show();
                            //Dividing the string into the respective signature
                            signR = StringToSignature(customKey.substring(0, blockSize * blockSize * 5));
                            signG = StringToSignature(customKey.substring(blockSize * blockSize * 5, blockSize * blockSize * 10));
                            signB = StringToSignature(customKey.substring(blockSize * blockSize * 10, blockSize * blockSize * 15));

                            MessageDecodingColor messageDecodingColor = new MessageDecodingColor(thisthis, getContext(), signR, signG, signB);
                            messageDecodingColor.execute(ReadImage());
                        } else { //Ask for another
                            Toast.makeText(getContext(), "Invalid key!", Toast.LENGTH_LONG).show();
                        }

                    } else { //In black and white
                        //If there is written something and is coherent with the necessary signature, keep in mind that every number takes 5 characters
                        double[] signatureBW;
                        if (customKey.length() == blockSize * blockSize * 5) {
                            Toast.makeText(getContext(), "Key is valid", Toast.LENGTH_SHORT).show();

                            signatureBW = StringToSignature(customKey);
                            MessageDecoding messageDecoding = new MessageDecoding(getContext(), signatureBW, thisthis);
                            messageDecoding.execute(ReadImage());
                        } else { //Get the default one
                            Toast.makeText(getContext(), "Invalid key!", Toast.LENGTH_LONG).show();
                        }
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

        pasteKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);

                if (clipboard.hasPrimaryClip() || clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

                    // Gets the clipboard as text.
                    CharSequence pasteData = item.getText();

                    // If the string contains data, then the paste operation is done
                    if (pasteData != null) {
                        keySignature.setText(pasteData);
                        decode.setEnabled(true);
                    } else {
                        Toast.makeText(getContext(), "Nothing to paste from the clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(getContext(), "Nothing to paste from the clipboard", Toast.LENGTH_SHORT).show();
                }
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
        if(resultText.length() > 0)
            outState.putString(bundleResultText, resultText);

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
                decode.setEnabled(true);
                Log.v(TAG, "Choosen photo.");
            } else {
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
        resultText = message;

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


}