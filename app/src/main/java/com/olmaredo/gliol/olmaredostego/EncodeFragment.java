package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class EncodeFragment extends Fragment implements TaskManager {
    private final String TAG = "EncodeFragment";
    //Random numbers to match requests
    private static final int CAMERA_REQUEST_CODE = 4444;
    private static final int PERMISSION_CODE = 14;
    //Strings used to index data in the save instance object
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";
    private static final String bundleNameText = "bNT";
    private static final String bundleTaskProgress = "bTP";
    private static final String bundleWasTaskRunning = "bWTR";
    private static final String bundleTaskType = "bTT";


    private FloatingActionButton encode;  //Encode button
    private ImageView preview;  //Preview the image selected
    private TextInputEditText inputText; //Box to type the hidden text manually
    private TextInputEditText keyField; // Enter encoding key

    private String fileNameOriginal;    //Name of the original photo file
    private String fileNameText;    //Hold the path of the input text file
    private Uri outputFileUri = null; //Camera output file path, stupid URI thing

    //Used to pass a reference to the asyncTask, because in the button handler "this" doesn't work as they should
    private EncodeFragment thisThis;

    //All it needs to manage destruction and creation of the ProgressDialog
    private ProgressDialog progressDialog;
    private int taskProgress;
    private String taskType; // Saving the title of the progressDialog
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.encoding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisThis = this; //this
        //Interface link to XML
        //Pick photo button handler
        encode = view.findViewById(R.id.btEncode);
        preview = view.findViewById(R.id.imPreview);
        //To open text file from file manager
        Button pickFile = view.findViewById(R.id.btPickFile);
        inputText = view.findViewById(R.id.etMessage);
        //Cursor that selects the embedding power
        keyField = view.findViewById(R.id.etKey);


        //All this if statement basically takes the saved instance and resumes the activity status
        //Generally after a screen rotation, but it is not known generally
        if (savedInstanceState != null) {
            fileNameOriginal = savedInstanceState.getString(bundleNameOriginal);
            fileNameText = savedInstanceState.getString(bundleNameText);
            wasTaskRunning = savedInstanceState.getBoolean(bundleWasTaskRunning);

            // Restoring image preview
            if (savedInstanceState.containsKey(bundleUri)) {
                outputFileUri = Uri.parse(savedInstanceState.getString(bundleUri));
                Bitmap im = OlmaredoUtil.ReadImageScaled(getActivity(), fileNameOriginal, outputFileUri);
                if (im != null) {
                    preview.setImageBitmap(im);
                }
            }

            // Reading the file again because saving it in memory could be a problem for large files
            if (new File(fileNameText).exists()) {
                inputText.setText(OlmaredoUtil.ReadTextFile(getContext(), fileNameText));
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
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OlmaredoUtil.CheckPermissions(thisThis, getContext(), PERMISSION_CODE)) {
                    //Timestamp to avoid an already existing file name
                    String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
                    Log.v(TAG, timeStamp);
                    fileNameOriginal = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-original.jpg";
                    //Opens a dialog that let you choose if open the gallery or the camera app
                    OlmaredoUtil.openImageIntent(thisThis, getActivity(), fileNameOriginal, outputFileUri, CAMERA_REQUEST_CODE);
                } else {
                    //If some tin-foil-hat didn't give the permissions
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }
            }
        });

        //Opens a dialog that selects a txt file and loads it
        pickFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OlmaredoUtil.CheckPermissions(thisThis, getContext(), PERMISSION_CODE)) {
                    FileChooser fileChooser = new FileChooser(getActivity());
                    fileChooser.setExtension(".txt");  //Only plain text files for now

                    fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
                        @Override
                        public void fileSelected(final File file) {
                            //Read text from file
                            fileNameText = file.getAbsolutePath();
                            Log.v(TAG, "Opened text file: " + fileNameText);

                            inputText.setText(OlmaredoUtil.ReadTextFile(getContext(), fileNameText));
                        }
                    });
                    fileChooser.showDialog();
                } else {
                    //I hate those smart ass motherfuckers
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }
            }
        });




        //Starts the Async task that encodes the message
        encode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Checking consistency of input data
                if (!new File(fileNameOriginal).exists()) {
                    Snackbar.make(encode, "Open a valid image!", Snackbar.LENGTH_LONG).show();
                    // Open dialog as if user user clicked on it
                    preview.callOnClick();
                    return;
                }
                // Reading key and trimming whitespaces
                String key = Objects.requireNonNull(keyField.getText()).toString().trim();
                if (key.length() < 4) {
                    Snackbar.make(encode, "Enter key at least 4 characters long!", Snackbar.LENGTH_LONG).show();
                    keyField.requestFocus();
                    return;
                }
                if (Objects.requireNonNull(inputText.getText()).length() < 1) {
                    Snackbar.make(encode, "Enter some text to hide!", Snackbar.LENGTH_LONG).show();
                    inputText.requestFocus();
                    return;
                }

                //Retrieving the encoding settings
                StartActivity activity = (StartActivity) getActivity();
                int blockSizeSaved = Objects.requireNonNull(activity).BlockSize;
                int cropSizeSaved = activity.CropSize;
                int embeddingPower = activity.EmbeddingPower;
                String inputString = inputText.getText().toString();

                // Basically compressing the charset into 8 bits
                inputString = new String(inputString.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);

                Log.v(TAG, "Entered key: " + key);
                Log.v(TAG, "Starting encoding: " + blockSizeSaved + " " + cropSizeSaved);

                // Starting the background task
                MessageEncodingColor messageEncodingColor = new MessageEncodingColor(thisThis, getContext(), inputString, key.toCharArray(), (byte) blockSizeSaved, cropSizeSaved, (double) embeddingPower);
                messageEncodingColor.execute(OlmaredoUtil.ReadImage(getActivity(), fileNameOriginal, outputFileUri));
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
    public void onSaveInstanceState(@NonNull Bundle outState) {

        outState.putString(bundleNameOriginal, fileNameOriginal);
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
                fileNameOriginal = RealPathUtil.getRealPathFromURI_API19(getContext(), outputFileUri);
            } else //Force media update if we added a new photo
                Objects.requireNonNull(getActivity()).sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, outputFileUri));

            //Loading a scaled image in the GUI, saving RAM
            Bitmap im = OlmaredoUtil.ReadImageScaled(getActivity(), fileNameOriginal, outputFileUri);
            if (im != null) {
                preview.setImageBitmap(im);
                keyField.requestFocus();

                Log.v(TAG, "Chosen photo.");
            } else {
                Log.v(TAG, "Image is null");
            }
        }
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

        if (OlmaredoUtil.CheckPermissions(thisThis, getContext(), PERMISSION_CODE)) {

            FileOutputStream out = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());

            String pathToPictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();

            String fileNameResult = pathToPictureFolder + "/Olmaredo/" + timeStamp + "-Encoded.png";
            Log.v(TAG, "Writing image file: " + fileNameResult);

            try {
                // Writing encoded image on file
                out = new FileOutputStream(fileNameResult);
                bm.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance

                // Updating the gallery with new photo
                File f = new File(fileNameResult);
                Objects.requireNonNull(getActivity()).sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)));
                // PNG is a loss less format, the compression factor (100) is ignored
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
        Log.v(TAG, "In on task completed but nothing to do here");
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

        Log.v(TAG, "In onDetached and already detached");
    }
}
