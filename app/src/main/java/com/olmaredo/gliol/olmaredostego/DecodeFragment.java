package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.Locale;
import java.util.Objects;


public class DecodeFragment extends Fragment implements TaskManager {
    private final String TAG = "DecodeFragment";
    //Random numbers to match requests
    private final int REQ_CODE_GALLERY = 2222;
    //Strings used to index data in the save instance object
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";
    private static final String bundleTaskProgress = "bTP";
    private static final String bundleWasTaskRunning = "bWTR";
    private static final String bundleTaskType = "bTT";
    private static final String bundleResultText = "bRT";

    private ImageView preview;
    private TextInputEditText keySignature;
    private FloatingActionButton decode;
    private TextView result;

    private DecodeFragment thisThis; //Holds reference of this fragment where "this" keyword isn't enough
    private String fileNameOriginal; //Holds the absolute path of the photo opened
    private String resultText = ""; //Result of decoding
    private Uri outputFileUri = null; //When returning from the gallery this is the result given

    //All it needs to manage ProgressDialog of an asyncTask via interface
    private ProgressDialog progressDialog;
    private int taskProgress;
    private String taskType;
    private boolean wasTaskRunning = false;

    private BottomAppBar bottomAppBar;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.decoding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisThis = this; //this
        //Interface link to XML
        //GUI objects
        preview = view.findViewById(R.id.ivPreview);
        keySignature = view.findViewById(R.id.etKey);
        decode = view.findViewById(R.id.btDecode);
        result = view.findViewById(R.id.twShowResult);


        bottomAppBar = view.findViewById(R.id.barDecode);
        ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(bottomAppBar);

        setHasOptionsMenu(true);

        //All this if statement basically takes the saved instance and resumes the activity status
        //Generally after a screen rotation, but doesn't know generally
        if (savedInstanceState != null) {
            fileNameOriginal = savedInstanceState.getString(bundleNameOriginal);
            wasTaskRunning = savedInstanceState.getBoolean(bundleWasTaskRunning);
            if (savedInstanceState.containsKey(bundleUri)) {
                outputFileUri = Uri.parse(savedInstanceState.getString(bundleUri));
                Bitmap im = OlmaredoUtil.ReadImageScaled(getActivity(), fileNameOriginal, outputFileUri);
                if (im != null) {
                    preview.setImageBitmap(im);
                }
            }
            if (savedInstanceState.containsKey(bundleTaskProgress))
                taskProgress = savedInstanceState.getInt(bundleTaskProgress);
            if (savedInstanceState.containsKey(bundleTaskType))
                taskType = savedInstanceState.getString(bundleTaskType);
            if (savedInstanceState.containsKey(bundleResultText)) {
                resultText = savedInstanceState.getString(bundleResultText);
                result.setText(resultText);
            }
            Log.v(TAG, "Activity restored.");
        } else {
            fileNameOriginal = "nothing here";
            Log.v(TAG, "Activity NOT restored.");
        }

        //Pick photo from gallery
        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (OlmaredoUtil.CheckPermissions(thisThis, getContext(), REQ_CODE_GALLERY)) {
                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, REQ_CODE_GALLERY); //Actually opens the gallery
                } else {
                    //Com'on
                    Toast.makeText(getContext(), "This app doesn't have permission to do what it has to do.", Toast.LENGTH_LONG).show();
                }
            }
        });

        keySignature.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    Log.v(TAG, "Starting decoding from keyboard");
                    decode.callOnClick();
                    return true;
                }
                return false;
            }
        });

        //Start decoding process
        decode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Checking consistency of input data
                if (!new File(fileNameOriginal).exists()) {
                    Snackbar.make(decode, "Open an image first", Snackbar.LENGTH_LONG).show();
                    new android.os.Handler().postDelayed(
                            new Runnable() {
                                public void run() {
                                    preview.callOnClick();
                                }
                            }, 2000);
                    return;
                }
                // Reading key and trimming whitespaces
                String key = Objects.requireNonNull(keySignature.getText()).toString().trim(); //Get the signature from the GUI
                if (key.length() < 4) {
                    Snackbar.make(decode, "Enter key at least 4 characters long", Snackbar.LENGTH_LONG).show();
                    keySignature.requestFocus();
                    return;
                }

                // Retrieving app settings and
                StartActivity activity = (StartActivity) getActivity();
                MessageDecodingColor messageDecodingColor = new MessageDecodingColor(thisThis, key.toCharArray(), (byte) Objects.requireNonNull(activity).BlockSize);
                messageDecodingColor.execute(OlmaredoUtil.ReadImage(getActivity(), fileNameOriginal, outputFileUri));
            }
        });
    }


    /*
      .--.      .-'.      .--.      .--.      .--.      .--.      .`-.      .--.
      :::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\::::::::.\
      `--'      `.-'      `--'      `--'      `--'      `-.'      `--'      `--'
     */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.

        StartActivity activity = (StartActivity) getActivity();
        // Making sure that our bar is the one of the activity, otherwise the menu is inflated on the other one
        if (Objects.requireNonNull(activity).Tab != 1) {
            ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(bottomAppBar);
            activity.Tab = 1;
        }

        inflater.inflate(R.menu.menu_decode, menu);
        super.onCreateOptionsMenu(menu, inflater);

        // This is stupid, it should not be here, but it is convenient because this method is called on tab change
        // Update textViews with setting retrieving the encoding settings
        TextView bd = Objects.requireNonNull(getView()).findViewById(R.id.tvMiniBD);
        bd.setText(String.format(Locale.ITALIAN, "%d px", activity.BlockSize));

        TextView fh = getView().findViewById(R.id.tvMiniFH);
        fh.setText(String.format(Locale.ITALIAN, "%d px", activity.CropSize));

        TextView ep = getView().findViewById(R.id.tvMiniI);
        ep.setText(String.format(Locale.ITALIAN, "%d%%", activity.EmbeddingPower));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.menu_copy) {
            if (result.getText().toString().length() > 0) {
                ClipboardManager clipboard = (ClipboardManager) Objects.requireNonNull(getActivity()).getSystemService(Activity.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("nothing", result.getText().toString());
                clipboard.setPrimaryClip(clip);
                Snackbar.make(decode, "Message copied in the clipboard", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(decode, "Nothing to copy", Snackbar.LENGTH_LONG).show();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override //Save the state of the fragment prior to destruction
    public void onSaveInstanceState(@NonNull Bundle outState) {
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

    @Override //Flow ends up here when returning from the pick photo from gallery intent
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_GALLERY && resultCode == Activity.RESULT_OK && null != data) {
            outputFileUri = data.getData();
            //At this point we have the image selected Uri
            //Now get the absolute path into a nice String

            fileNameOriginal = RealPathUtil.getRealPathFromURI_API19(getContext(), outputFileUri);

            Bitmap im = OlmaredoUtil.ReadImageScaled(getActivity(), fileNameOriginal, outputFileUri); //Read a smaller version of the image and load it into the GUI
            if (im != null) {
                preview.setImageBitmap(im);
                decode.setEnabled(true);
                keySignature.requestFocus();

                Log.v(TAG, "Chosen photo.");
            } else {
                decode.setEnabled(false);
                Log.v(TAG, "Image is null");
            }
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
        Log.v(TAG, "In on task completed but nothing to do here");
    }

    @Override
    public void onTaskCompleted(String message) {
        result.setText(message);
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

        Log.v(TAG, "In onDetached and already detached");
    }


}