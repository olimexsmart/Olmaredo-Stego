package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

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
    Tab that manages message decoding
 */
public class DecodeFragment extends Fragment implements TaskManager {
    private final String TAG = "DecodeFragment";
    //Random numbers to match requests
    private final int REQ_CODE_GALLERY = 2222;
    private final int PERMISSION_CODE = 14;
    //Strings used to index data in the save instance object
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";
    private static final String bundleTaskProgress = "bTP";
    private static final String bundleWasTaskRunning = "bWTR";
    private static final String bundleTaskType = "bTT";
    private static final String bundleResultText = "bRT";

    private ImageView preview;
    private EditText keySignature;
    private Button decode;
    private TextView result;
    private Button toClipboard;

    private DecodeFragment thisthis; //Holds reference of this fragment where "this" keyword isn't enough
    private String fileNameOriginal; //Holds the absolute path of the photo opened
    private String resultText = ""; //Result of decoding
    private Uri outputFileUri = null; //When returning from the gallery this is the result given

    //All it needs to manage ProgressDialog of an asyncTask via interface
    private ProgressDialog progressDialog;
    private int taskProgress;
    private String taskType;
    private boolean wasTaskRunning = false;

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

        thisthis = this; //this
        //Interface link to XML
        //GUI objects
        Button photo = view.findViewById(R.id.btBrowseDecode);
        preview = view.findViewById(R.id.ivPreview);
        keySignature = view.findViewById(R.id.etCustom);
        decode = view.findViewById(R.id.btDecode);
        result = view.findViewById(R.id.twShowResult);
        toClipboard = view.findViewById(R.id.btClipboardText);
        Button pasteKey = view.findViewById(R.id.btPaste);

        //All this if statement basically takes the saved instance and resumes the activity status
        //Generally after a screen rotation, but doesn't know generally
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
            if (savedInstanceState.containsKey(bundleTaskType))
                taskType = savedInstanceState.getString(bundleTaskType);
            if (savedInstanceState.containsKey(bundleResultText)) {
                resultText = savedInstanceState.getString(bundleResultText);
                result.setText(resultText);
                toClipboard.setEnabled(true);
            }
            Log.v(TAG, "Activity restored.");
        } else {
            fileNameOriginal = "nothing here";
            Log.v(TAG, "Activity NOT restored.");
        }

        //Pick photo from gallery
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckPermissions()) {
                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, REQ_CODE_GALLERY); //Actually opens te gallery
                } /*else {
                    //Com'on
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }*/
            }
        });

        //Start decoding process
        decode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO erase blank spaces and other non printable chars from the signature
                //First thing to do is check if the file is valid
                if (new File(fileNameOriginal).exists()) {
                    StartActivity activity = (StartActivity) getActivity();
                    String customKey = keySignature.getText().toString(); //Get the signature from the GUI

                    if (customKey.length() > 0) {
                        MessageDecodingColor messageDecodingColor = new MessageDecodingColor(thisthis, customKey.toCharArray(), (byte) activity.BlockSize);
                        messageDecodingColor.execute(ReadImage());

                        toClipboard.setEnabled(true); //Make possible copying the text elsewhere
                    } else { //Ask for another
                        Toast.makeText(getContext(), "Invalid key!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    //Dai belin
                    Toast.makeText(getContext(), "Choose a valid image.", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Handy button that pastes the result into the clipboard
        toClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("nothing", result.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Message copied in the clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        //Lets you paste the enormous signature
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
                } else {
                    Toast.makeText(getContext(), "Nothing to paste from the clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /*
      .--.      .-'.      .--.      .--.      .--.      .--.      .`-.      .--.
      :::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\
      `--'      `.-'      `--'      `--'      `--'      `-.'      `--'      `--'
     */

    //Converts a string into a double array
    private double[] StringToSignature(String key) {
        double[] signature = new double[key.length() / 5];
        //Quel momento in cui vorresti scrivere una bestemmia nel codice ma poi sai che c'è Martino che legge e non vuoi offenderlo
        char[] keyChar = key.toCharArray();
        for (int i = 0; i < signature.length; i++) {   //Bit level ascii hack, from char to int and at the correct position. 48 is a magic number, number zero I guess
            signature[i] = (keyChar[i * 5 + 1] - 48) / 10000.0 + (keyChar[i * 5 + 2] - 48) / 1000.0 + (keyChar[i * 5 + 3] - 48) / 100.0 + (keyChar[i * 5 + 4] - 48) / 10.0;
            //Change sign if the prefix is a letter
            if (isLetter(keyChar[i * 5]))
                signature[i] *= -1;
        }

        return signature;
    }


    @Override //Save the state of the fragment prior to destruction
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(bundleNameOriginal, fileNameOriginal);
        outState.putBoolean(bundleWasTaskRunning, wasTaskRunning);
        if (outputFileUri != null)
            outState.putString(bundleUri, outputFileUri.toString());
        if (wasTaskRunning) {
            outState.putInt(bundleTaskProgress, taskProgress);
            outState.putString(bundleTaskType, taskType);
        }
        if (resultText.length() > 0)
            outState.putString(bundleResultText, resultText);

        super.onSaveInstanceState(outState);
    }

    //Android 6.0 and above permission check
    private boolean CheckPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, PERMISSION_CODE);

            return false;
        } else
            return true;
    }

    @Override //Flow ends up here when returning from the pick photo from gallery intent
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_GALLERY && resultCode == Activity.RESULT_OK && null != data) {
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

            Bitmap im = ReadImageScaled(); //Read a smaller version of the image and load it into the GUI
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

    //Read image from absolute path
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

    /*
        Async task management
        Allow to separate the GUI management from the AsyncTask
     */
    @Override
    public void onTaskProgress(int progress) {
        progressDialog.setProgress(progress);
        taskProgress = progress;
    }


    @Override
    public void onTaskCompleted(Bitmap bm) {
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