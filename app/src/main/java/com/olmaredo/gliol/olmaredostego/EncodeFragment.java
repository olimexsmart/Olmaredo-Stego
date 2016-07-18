package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/*
    TODO allow something more then mere ASCII chars, è in example
 */

public class EncodeFragment extends Fragment implements TaskManager {
    private final String TAG = "EncodeFragment";
    private final int CAMERA_REQUEST_CODE = 4444;
    private final int PERMISSION_CODE = 14;
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";
    private static final String bundleSignBW = "bSBW";
    private static final String bundleSignR = "bSR";
    private static final String bundleSignG = "bSG";
    private static final String bundleSignB = "bSB";
    private static final String bundleEmbedPow = "bEP";
    private static final String bundleNameText = "bNT";
    private static final String bundleTaskProgress = "bTP";
    private static final String bundleWasTaskRunning = "bWTR";
    private static final String bundleTaskType = "bTT";
    private static final String bundleKeySignature = "bKS";


    Button photo;
    Button encode;
    Button pickFile;
    Button copySignature;
    ImageView preview;
    TextView signaturePreview;
    EditText inputText;
    SeekBar seekPower;
    String fileNameOriginal;
    String fileNameText;
    String inputString = "not from file";
    String keySignature = "";
    Uri outputFileUri = null;
    int embeddingPower = 20;
    //MessageEncoding messageEmbedding;
    TextView percentageText;
    int blockSizeSaved;
    int cropSizeSaved;
    EncodeFragment thisthis;
    double[] signBlackWhite;
    double[] signR;
    double[] signG;
    double[] signB;
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
        return inflater.inflate(R.layout.encoding, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisthis = this;

        photo = (Button) view.findViewById(R.id.btPhotoEncode);
        encode = (Button) view.findViewById(R.id.btEncode);
        preview = (ImageView) view.findViewById(R.id.imPreview);
        pickFile = (Button) view.findViewById(R.id.btPickFile);
        inputText = (EditText) view.findViewById(R.id.etMessage);
        seekPower = (SeekBar) view.findViewById(R.id.sbEmbeddingPower);
        percentageText = (TextView) view.findViewById(R.id.tvSeekBar);
        copySignature = (Button) view.findViewById(R.id.copySignature);
        signaturePreview = (TextView) view.findViewById(R.id.twSignature);

        if (savedInstanceState != null) {
            boolean readyToEncode = false; //Determine when restoring the activity if there is all the necessary to start encoding
            fileNameOriginal = savedInstanceState.getString(bundleNameOriginal);
            embeddingPower = savedInstanceState.getInt(bundleEmbedPow);
            fileNameText = savedInstanceState.getString(bundleNameText);
            wasTaskRunning = savedInstanceState.getBoolean(bundleWasTaskRunning);

            if (savedInstanceState.containsKey(bundleUri)) {
                outputFileUri = Uri.parse(savedInstanceState.getString(bundleUri));
                Bitmap im = ReadImageScaled();
                if (im != null) {
                    preview.setImageBitmap(im);
                    readyToEncode = true;
                }
            }

            if (new File(fileNameText).exists()) {
                inputString = ReadTextFile(fileNameText);
                inputText.setHint(new File(fileNameText).getName() + " correctly opened.");

                if (readyToEncode)
                    encode.setEnabled(true);
            }

            if (savedInstanceState.containsKey(bundleSignBW))
                signBlackWhite = savedInstanceState.getDoubleArray(bundleSignBW);
            if (savedInstanceState.containsKey(bundleSignR))
                signR = savedInstanceState.getDoubleArray(bundleSignR);
            if (savedInstanceState.containsKey(bundleSignG))
                signG = savedInstanceState.getDoubleArray(bundleSignG);
            if (savedInstanceState.containsKey(bundleSignB))
                signB = savedInstanceState.getDoubleArray(bundleSignB);

            if (savedInstanceState.containsKey(bundleTaskProgress))
                taskProgress = savedInstanceState.getInt(bundleTaskProgress);
            if (savedInstanceState.containsKey(bundleTaskType))
                taskType = savedInstanceState.getString(bundleTaskType);

            if (savedInstanceState.containsKey(bundleKeySignature)) {
                keySignature = savedInstanceState.getString(bundleKeySignature);
                signaturePreview.setText(keySignature);
                copySignature.setEnabled(true);
            }
            Log.v(TAG, "Activity restored.");
        } else {
            fileNameOriginal = "nothing here";
            fileNameText = "nothing here";
            Log.v(TAG, "Activity NOT restored.");
        }


        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckPermissions()) {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
                    Log.v(TAG, timeStamp);

                    fileNameOriginal = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-original.jpg";
                    openImageIntent();
                } else {
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Opens a dialog that selects a txt file and loads it
        pickFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckPermissions()) {
                    FileChooser fileChooser = new FileChooser(getActivity());
                    fileChooser.setExtension(".txt");
                    fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
                        @Override
                        public void fileSelected(final File file) {
                            //Read text from file

                            fileNameText = file.getAbsolutePath();
                            Log.v(TAG, "Opened text file: " + fileNameText);

                            inputString = ReadTextFile(fileNameText);
                            inputText.setText("");
                            inputText.setHint(file.getName() + " correctly opened.");
                            encode.setEnabled(true);
                        }
                    });
                    fileChooser.showDialog();
                } else {
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }
            }
        });


        //This makes sure that is impossible to choose a file once a single character is written in the field
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (inputText.getText().length() > 0) {
                    encode.setEnabled(true);
                } else if (inputString.length() > 0) {
                    encode.setEnabled(true);
                } else
                    encode.setEnabled(false);
            }
        });


        seekPower.setProgress(embeddingPower);
        percentageText.setText("" + embeddingPower + "%");
        seekPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar bar) {
                embeddingPower = bar.getProgress(); // the value of the seekBar progress
            }

            public void onStartTrackingTouch(SeekBar bar) {
            }

            public void onProgressChanged(SeekBar bar, int paramInt, boolean paramBoolean) {
                percentageText.setText("" + paramInt + "%"); // here in textView the percent will be shown
            }
        });

        //Starts the Async task that encodes the message
        encode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (new File(fileNameOriginal).exists()) {
                    StartActivity activity = (StartActivity) getActivity();
                    blockSizeSaved = activity.BlockSize;
                    cropSizeSaved = activity.CropSize;

                    if (inputText.getText().length() > 0)
                        inputString = inputText.getText().toString();
                    //Strip all non ASCII characters
                    inputString = inputString.replaceAll("[^\\x20-\\x7e]", "");
                    Log.v(TAG, "Starting encoding: " + blockSizeSaved + " " + cropSizeSaved);
                    //copySignature.setEnabled(true);

                    if (activity.InColor) {
                        MessageEncodingColor messageEncodingColor = new MessageEncodingColor(thisthis, getContext(), inputString, (byte) blockSizeSaved, cropSizeSaved, embeddingPower, activity.PatternReduction);
                        messageEncodingColor.execute(ReadImage());
                    } else {
                        MessageEncoding messageEncoding = new MessageEncoding(thisthis, getContext(), inputString, (byte) blockSizeSaved, cropSizeSaved, embeddingPower, activity.PatternReduction);
                        messageEncoding.execute(ReadImage());
                    }
                } else {
                    Toast.makeText(getContext(), "Open a valid image!", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Transform the signature into a string and copy it to the clipboard
        copySignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("nothing", signaturePreview.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Signature copied in the clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        Log.v(TAG, "OnView ended");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString(bundleNameOriginal, fileNameOriginal);
        outState.putInt(bundleEmbedPow, embeddingPower);
        outState.putString(bundleNameText, fileNameText);
        outState.putBoolean(bundleWasTaskRunning, wasTaskRunning);

        if (outputFileUri != null)
            outState.putString(bundleUri, outputFileUri.toString());
        if (signBlackWhite != null)
            outState.putDoubleArray(bundleSignBW, signBlackWhite);
        if (signR != null)
            outState.putDoubleArray(bundleSignR, signR);
        if (signG != null)
            outState.putDoubleArray(bundleSignG, signG);
        if (signB != null)
            outState.putDoubleArray(bundleSignB, signB);
        if (wasTaskRunning) {
            outState.putInt(bundleTaskProgress, taskProgress);
            outState.putString(bundleTaskType, taskType);
        }
        if (keySignature.length() > 0)
            outState.putString(bundleKeySignature, keySignature);

        super.onSaveInstanceState(outState);
    }

    private boolean CheckPermissions() {
        if(ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            String[] permissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, PERMISSION_CODE);

            return false;
        }
        else
            return true;
    }


    private String ReadTextFile(String path) {

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
            Toast.makeText(getContext(), f.getName() + " not found", Toast.LENGTH_SHORT).show();
        }

        return text.toString();
    }

    private String SignatureToString(double[] signature) {
        double z;
        String key = "";
        Random caos = new Random();
        for (double ev : signature) {
            z = Math.floor(ev * 10000); //Save only first 4 digits
            if (z > 0) //If positive, insert a number
                key += caos.nextInt(10);
            else //Else negative, insert an Uppercase letter
                key += (char) (caos.nextInt(91 - 65) + 65); //Produces only numbers between 65 and 90, ASCII for uppercase letters

            z = Math.abs(z);
            //This is a positional encoding
            key += (int) z % 10;
            key += ((int) z / 10) % 10;
            key += ((int) z / 100) % 10;
            key += ((int) z / 1000) % 10;
        }

        return key;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.v(TAG, "Entered activity result");
            Log.v(TAG, fileNameOriginal);

            final boolean isCamera;
            if (data == null) {
                isCamera = true;
            } else {
                final String action = data.getAction();
                if (action == null) {
                    isCamera = false;
                } else {
                    isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                }
            }

            if (!isCamera) { //Else the correct path is already stored in fileNameOriginal
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
            } else //Force media update if we added a new photo
                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, outputFileUri));

            Bitmap im = ReadImageScaled();
            if (im != null) {
                preview.setImageBitmap(im);
                Log.v(TAG, "Choosen photo.");
            } else {
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

    private void openImageIntent() {

        // Determine Uri of camera image to save.
        File fromCamera = new File(fileNameOriginal);
        outputFileUri = Uri.fromFile(fromCamera);

        // Camera.
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getActivity().getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Filesystem.
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(pickIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

        startActivityForResult(chooserIntent, CAMERA_REQUEST_CODE);
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
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            wasTaskRunning = false;
            setRetainInstance(false);

        if (CheckPermissions()) {
            copySignature.setEnabled(true);

            signBlackWhite = signature;

            String key = "";
            key += SignatureToString(signBlackWhite);
            keySignature = key;
            signaturePreview.setText(key);

            FileOutputStream out = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
            String fileNameResult = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-result-blackwhite.png";
            try {
                out = new FileOutputStream(fileNameResult);
                bm.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                File f = new File(fileNameResult);
                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (FileNotFoundException e) {
                Log.v(TAG, "Invalid saving path.");
            } catch (NullPointerException e) {
                Log.v(TAG, "The embedding result is null.");
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onTaskCompleted(Bitmap bm, double[] signatureR, double[] signatureG, double[] signatureB) {

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        wasTaskRunning = false;
        setRetainInstance(false);

        if (CheckPermissions()) {
            copySignature.setEnabled(true);

            signR = signatureR;
            signG = signatureG;
            signB = signatureB;

            String key = "";
            key += SignatureToString(signR);
            key += SignatureToString(signG);
            key += SignatureToString(signB);
            keySignature = key;
            signaturePreview.setText(key);

            FileOutputStream out = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
            String fileNameResult = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-result-color.png";
            try {
                out = new FileOutputStream(fileNameResult);
                bm.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                File f = new File(fileNameResult);
                getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (FileNotFoundException e) {
                Log.v(TAG, "Invalid saving path.");
            } catch (NullPointerException e) {
                Log.v(TAG, "The embedding result is null.");
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onTaskCompleted(String message) {
        //Nothing to do here
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
