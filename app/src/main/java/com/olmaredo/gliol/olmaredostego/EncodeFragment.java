package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


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


    private FloatingActionButton encode;  // Encode button
    private ImageView preview;  // Preview the image selected
    private TextInputEditText inputText; // Box to type the hidden text manually
    private TextInputEditText keyField; // Enter encoding key

    private String fileNameOriginal = "";    // Name of the original photo file
    private String fileNameText = "";    // Holds the path of the input text file
    private String fileNameResult = ""; // Holds the output file name for sharing
    private Uri outputFileUri = null; // Camera output file path, stupid URI thing

    //Used to pass a reference to the asyncTask, because in the button handler "this" doesn't work as they should
    private EncodeFragment thisThis;

    //All it needs to manage destruction and creation of the ProgressDialog
    private ProgressDialog progressDialog;
    private int taskProgress;
    private String taskType; // Saving the title of the progressDialog
    private boolean wasTaskRunning = false;

    private BottomAppBar bottomAppBar;

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
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisThis = this; //this
        //Interface link to XML
        //Pick photo button handler
        encode = view.findViewById(R.id.btEncode);
        preview = view.findViewById(R.id.ivPreview);
        //To open text file from file manager
        inputText = view.findViewById(R.id.etMessage);
        //Cursor that selects the embedding power
        keyField = view.findViewById(R.id.etKey);


        bottomAppBar = view.findViewById(R.id.barEncode);
        ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(bottomAppBar);
        setHasOptionsMenu(true);


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

                    String pathToPictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();

                    fileNameResult = pathToPictureFolder + "/Olmaredo/" + timeStamp + "-Encoded.png";
                    fileNameOriginal = pathToPictureFolder + "/Olmaredo/" + timeStamp + "-Taken.jpg";
                    //Opens a dialog that let you choose if open the gallery or the camera app
                    //OlmaredoUtil.openImageIntent(thisThis, getActivity(), fileNameOriginal, outputFileUri, CAMERA_REQUEST_CODE);
                    // Determine Uri of camera image to save.
                    File fromCamera = new File(fileNameOriginal);
                    //outputFileUri = Uri.fromFile(fromCamera);
                    outputFileUri = FileProvider.getUriForFile(Objects.requireNonNull(getContext()), BuildConfig.APPLICATION_ID, fromCamera);

                    // Camera.
                    final List<Intent> cameraIntents = new ArrayList<>();
                    final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    final PackageManager packageManager = Objects.requireNonNull(getActivity()).getPackageManager();
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
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[0]));

                    startActivityForResult(chooserIntent, CAMERA_REQUEST_CODE);
                } else {
                    //If some tin-foil-hat didn't give the permissions
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Live char counter
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView space = view.findViewById(R.id.tvSpace);
                space.setText(String.format(Locale.ITALIAN, "%d", s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        //Starts the Async task that encodes the message
        encode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Checking consistency of input data
                if (!new File(fileNameOriginal).exists()) {
                    Snackbar.make(encode, "Open an image first", Snackbar.LENGTH_LONG).show();
                    // Open dialog as if user user clicked on it
                    new android.os.Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    preview.callOnClick();
                                }
                            }, 2000);

                    return;
                }
                // Reading key and trimming whitespaces
                String key = Objects.requireNonNull(keyField.getText()).toString().trim();
                if (key.length() < 4) {
                    Snackbar.make(encode, "Enter key at least 4 characters long", Snackbar.LENGTH_LONG).show();
                    keyField.requestFocus();
                    return;
                }
                if (Objects.requireNonNull(inputText.getText()).length() < 1) {
                    Snackbar.make(encode, "Enter some text to hide", Snackbar.LENGTH_LONG).show();
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

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Making sure that our bar is the one of the activity, otherwise the menu is inflated on the other one
        StartActivity activity = (StartActivity) getActivity();

        if (Objects.requireNonNull(activity).Tab != 0) {
            ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(bottomAppBar);
            activity.Tab = 0;
        }

        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_encode, menu);
        super.onCreateOptionsMenu(menu, inflater);

        // This is stupid, it should not be here, but it is convenient because this method is called on tab change
        // Update textViews with setting retrieving the encoding settings
        TextView bd = Objects.requireNonNull(getView()).findViewById(R.id.tvMiniBD);
        bd.setText(String.format(Locale.ITALIAN, "%d px", activity.BlockSize));

        TextView fh = getView().findViewById(R.id.tvMiniFH);
        fh.setText(String.format(Locale.ITALIAN, "%d px", activity.CropSize));

        TextView ep = getView().findViewById(R.id.tvMiniI);
        ep.setText(String.format(Locale.ITALIAN, "%d%%", activity.EmbeddingPower));

        if (new File(fileNameOriginal).exists()) {
            UpdateMaxSize();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_share:
                if (new File(fileNameResult).exists()) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");// You Can set source type here like video, image text, etc.
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(fileNameResult));
                    shareIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    startActivity(Intent.createChooser(shareIntent, "Share File using:"));
                } else {
                    Snackbar.make(encode, "Encode an image first", Snackbar.LENGTH_LONG).show();
                }
                Log.v(TAG, "Share menu clicked");
                return true;
            case R.id.menu_file:
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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

        // Avoiding transactionTooLargeException
        if (Objects.requireNonNull(inputText.getText()).toString().length() > 10000) {
            inputText.setText(inputText.getText().toString().substring(0, 10000));
            Log.v(TAG, "inputText too large, truncated");
        }

        Log.v(TAG, "Fragment Saved");
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

                UpdateMaxSize();

                Log.v(TAG, "Chosen photo.");
            } else {
                Log.v(TAG, "Image is null");
            }
        }
    }

    // Updates textView with image capacity
    private void UpdateMaxSize() {
        // This may look rather ugly
        // But it is
        // The goal is to quickly return to the caller as possible
        // Otherwise the GUI cannot proceed in rendering, since this is called in callbacks
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // Setting max character embeddable in image
                        StartActivity activity = (StartActivity) getActivity();
                        int N = Objects.requireNonNull(activity).BlockSize;
                        int finHeight = activity.CropSize;

                        Bitmap temp = OlmaredoUtil.ResizeNCrop(Objects.requireNonNull(OlmaredoUtil.ReadImage(getActivity(), fileNameOriginal, outputFileUri)), N, finHeight);

                        //Checking how much information can contain the image
                        int H = temp.getHeight();
                        int W = temp.getWidth();
                        int WMax = W / N; //Number of blocks per row
                        int HMax = H / N; //Number of blocks per row
                        int NBlocks = HMax * WMax;

                        int maxLength = ((NBlocks * 3) / 8) - 3; // Safe from border conditions

                        TextView ml = Objects.requireNonNull(getView()).findViewById(R.id.tvSpaceTot);
                        ml.setText(String.format(Locale.ITALIAN, "%d", maxLength));
                    }
                }, 500);
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


    // This takes the modified photo and saves it
    @Override
    public void onTaskCompleted(Bitmap bm) {

        if (OlmaredoUtil.CheckPermissions(thisThis, getContext(), PERMISSION_CODE)) {

            FileOutputStream out = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());

            String pathToPictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();

            fileNameResult = pathToPictureFolder + "/Olmaredo/" + timeStamp + "-Encoded.png";
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
        } else {
            Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
        }

        // GUI changes
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        wasTaskRunning = false;
        setRetainInstance(false);
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
