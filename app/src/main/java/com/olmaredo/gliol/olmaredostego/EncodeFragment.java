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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
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

/*
    TODO allow something more then mere ASCII chars, è in example
 */

public class EncodeFragment extends Fragment implements TaskManager {
    private final String TAG = "EncodeFragment";
    //Random numbers to match requests
    private static final int CAMERA_REQUEST_CODE = 4444;
    private static final int PERMISSION_CODE = 14;
    private static final int DEFAULT_EMBEDDING_POWER = 20;
    //Strings used to index data in the save instance object
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";
    private static final String bundleEmbedPow = "bEP";
    private static final String bundleNameText = "bNT";
    private static final String bundleTaskProgress = "bTP";
    private static final String bundleWasTaskRunning = "bWTR";
    private static final String bundleTaskType = "bTT";
    private static final double SCALE = 100.0f;


    private Button encode;  //Encode button
    private Button copySignature; //Put in the clipboard the signature used
    private ImageView preview;  //Preview the image selected
    private EditText inputText; //Box to type the hidden text manually
    private TextView percentageText; //Shows the embedding power strength
    private EditText keyField; // Enter encoding key

    private String fileNameOriginal;    //Name of the original photo file
    private String fileNameText;    //Hold the path of the input text file
    private String inputString = "not from file";   //Input text, also used in logic flux
    private Uri outputFileUri = null; //Camera output file path, stupid URI thing
    private int embeddingPower = DEFAULT_EMBEDDING_POWER; //Default embedding power

    //Used to pass a reference to the asyncTask, because in the button handler "this" doesn't work as they should
    private EncodeFragment thisthis;

    //All it needs to manage destruction and creation of the ProgressDialog
    private ProgressDialog progressDialog;
    private int taskProgress;
    private String taskType;
    private boolean wasTaskRunning = false;

    //In order to have smoothest transition possible, immediately create the new Dialog
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If we are returning here from a screen orientation change
        // and the AsyncTask is still working, re-create and display the
        // progress dialog.
        if (wasTaskRunning) {
            onTaskStarted(taskType);
        }
    }

    //Tab layout manager
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.encoding, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisthis = this; //this
        //Interface link to XML
        //Pick photo button handler
        Button photo = view.findViewById(R.id.btPhotoEncode);
        encode = view.findViewById(R.id.btEncode);
        preview = view.findViewById(R.id.imPreview);
        //To open text file from file manager
        Button pickFile = view.findViewById(R.id.btPickFile);
        inputText = view.findViewById(R.id.etMessage);
        //Cursor that selects the embedding power
        SeekBar seekPower = view.findViewById(R.id.sbEmbeddingPower);
        percentageText = view.findViewById(R.id.tvSeekBar);
        copySignature = view.findViewById(R.id.copySignature);
        keyField = view.findViewById(R.id.etKey);
        //Some GUI changes
        seekPower.setProgress(embeddingPower);
        percentageText.setText("" + embeddingPower + "%");

        //All this if statement basically takes the saved instance and resumes the activity status
        //Generally after a screen rotation, but it is not known generally
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

            // TODO simply save the instance of the file and the name file instead of reading it again
            if (new File(fileNameText).exists()) {
                inputString = ReadTextFile(fileNameText);
                inputText.setHint(new File(fileNameText).getName() + " correctly opened.");

                if (readyToEncode)
                    encode.setEnabled(true);
            }


            if (savedInstanceState.containsKey(bundleTaskProgress))
                taskProgress = savedInstanceState.getInt(bundleTaskProgress);
            if (savedInstanceState.containsKey(bundleTaskType))
                taskType = savedInstanceState.getString(bundleTaskType);

            Log.v(TAG, "Activity restored.");
        } else {
            fileNameOriginal = "nothing here";
            fileNameText = "nothing here";
            Log.v(TAG, "Activity NOT restored.");
        }

        //Choose photo from gallery or take a photo
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckPermissions()) {
                    //Timestamp to avoid an already existing file name
                    String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
                    Log.v(TAG, timeStamp);
                    fileNameOriginal = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-original.jpg";
                    //Opens a dialog that let you choose if open the gallery or the camera app
                    openImageIntent();
                } /*else {
                    //If some tin-foil-hat didn't give the permissions
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }*/
            }
        });

        //TODO complitely rethink flow of input file and edit text string
        // TODO Use system file picker instead
        //Opens a dialog that selects a txt file and loads it
        pickFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckPermissions()) {
                    FileChooser fileChooser = new FileChooser(getActivity());
                    fileChooser.setExtension(".txt");  //Only plain text files for now

                    fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
                        @Override
                        public void fileSelected(final File file) {
                            //Read text from file
                            fileNameText = file.getAbsolutePath();
                            Log.v(TAG, "Opened text file: " + fileNameText);

                            inputString = ReadTextFile(fileNameText);
                            inputText.setText("");
                            inputText.setHint(file.getName() + " correctly opened."); //Give a confirmation of the operation
                            encode.setEnabled(true);
                        }
                    });
                    fileChooser.showDialog();
                } /*else {
                    //I hate those smart ass motherfuckers
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }*/
            }
        });


        //This makes sure that is impossible to choose a file once a single character is written in the field
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //Bah...
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Meh...
            }

            @Override
            public void afterTextChanged(Editable s) {
                //Basically if there if some text somewhere, from GUI or from file
                if (inputText.getText().length() > 0 || inputString.length() > 0) {
                    encode.setEnabled(true);
                } else
                    encode.setEnabled(false);
            }
        });

        //Updates the embedding power value
        seekPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar bar) {
                embeddingPower = bar.getProgress(); // the value of the seekBar progress
            }

            public void onStartTrackingTouch(SeekBar bar) {
                //Man...
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
                    //Retrieving the encoding settings
                    StartActivity activity = (StartActivity) getActivity();
                    int blockSizeSaved = activity.BlockSize;
                    int cropSizeSaved = activity.CropSize;
                    // Checking consistency of input data
                    if (inputText.getText().length() > 0)
                        inputString = inputText.getText().toString();
                    else if (!(inputString.length() > 0)) {
                        Toast.makeText(getContext(), "Enter some text to hide", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (keyField.getText().length() < 4) {
                        Toast.makeText(getContext(), "Enter key at least 4 characters long", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.v(TAG, "Entered key: " + keyField.getText());
                    //Strip all non ASCII characters
                    inputString = inputString.replaceAll("[^\\x20-\\x7e]", "");
                    Log.v(TAG, "Starting encoding: " + blockSizeSaved + " " + cropSizeSaved);
                    //copySignature.setEnabled(true); debug purposes, I keep it here for a remainder

                    MessageEncodingColor messageEncodingColor = new MessageEncodingColor(thisthis, getContext(), inputString, keyField.getText().toString().toCharArray(), (byte) blockSizeSaved, cropSizeSaved, (double)embeddingPower / SCALE);
                    messageEncodingColor.execute(ReadImage());

                } else {
                    //Geeez
                    Toast.makeText(getContext(), "Open a valid image!", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Transform the signature into a string and copy it to the clipboard
        copySignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                //ClipData clip = ClipData.newPlainText("nothing", signaturePreview.getText().toString());
                //clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Signature copied in the clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        Log.v(TAG, "OnView ended");
    }

    /*
      .--.      .-'.      .--.      .--.      .--.      .--.      .`-.      .--.
      :::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\
      `--'      `.-'      `--'      `--'      `--'      `-.'      `--'      `--'
     */

    //Saves the state of the activity before is destroyed, orientation change and such
    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString(bundleNameOriginal, fileNameOriginal);
        outState.putInt(bundleEmbedPow, embeddingPower);
        outState.putString(bundleNameText, fileNameText);
        outState.putBoolean(bundleWasTaskRunning, wasTaskRunning);

        if (outputFileUri != null)
            outState.putString(bundleUri, outputFileUri.toString());
        if (wasTaskRunning) {
            outState.putInt(bundleTaskProgress, taskProgress);
            outState.putString(bundleTaskType, taskType);
        }

        super.onSaveInstanceState(outState);
    }

    //Necessary to Android 6.0 and above for run time permissions
    private boolean CheckPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, PERMISSION_CODE);

            return false;
        } else
            return true;
    }

    //Returning from the camera app or the gallery
    //Save, resize, load in GUI
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.v(TAG, "Entered activity result");
            Log.v(TAG, fileNameOriginal);
            //From where are we coming from, it's a super-safe triple check
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

            //Loading a scaled image in the GUI, saving RAM
            Bitmap im = ReadImageScaled();
            if (im != null) {
                preview.setImageBitmap(im);
                Log.v(TAG, "Choosen photo.");
            } else {
                Log.v(TAG, "Image is null");
            }
        }
    }

    //Read a full size image, like for processing it
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
    //Useful to save RAM for a GUI preview
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

    //Takes an absolute path of a txt file and gives back its contents, this simple
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

    //Display a dialog box that let you choose between an already existing image o taking a new one
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

    //From now on there is all the methods from the TaskManager, managing asyncTasks
    @Override
    public void onTaskStarted(String type) { //Creates the progressDialog
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
    public void onTaskProgress(int progress) { //Updates the Dialog
        progressDialog.setProgress(progress);
        taskProgress = progress;
    }


    //This takes the modified photo and saves it
    @Override //COLOR
    public void onTaskCompleted(Bitmap bm) {

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        wasTaskRunning = false;
        setRetainInstance(false);

        if (CheckPermissions()) {
            copySignature.setEnabled(true);

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
        } /*else {
            Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
        }*/
    }

    @Override
    public void onTaskCompleted(String message) {
        //Nothing to do here
    }

    //https://androidresearch.wordpress.com/2013/05/10/dealing-with-asynctask-and-screen-orientation/
    //Basically the problem was that during activity lifecycle the AT continued going on but the PD retained
    //always the original references to the activity, crashing when it was time to destroying it.
    //This way the PD is managed directly from the activity
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
