package com.example.gliol.olmaredostego;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
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
    TODO implement message encoding task
    TODO setEnabled managed correctly for the Encode button
    TODO manage signature
    TODO manage max input lenght, but in asynctask
 */
public class EncodeFragment extends Fragment implements GetResultEmbedding {
    private final String TAG = "EncodeFragment";
    private final int CAMERA_REQUEST_CODE = 4444;
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleNameResult = "bNR";
    private static final String bundleTimeStamp = "bTS";
    private static final String bundleUri = "bU";

    Button photo;
    Button encode;
    Button pickFile;
    ImageView preview;
    EditText inputText;
    SeekBar seekPower;
    String fileNameOriginal;
    String fileNameResult;
    String timeStamp;
    String inputFile = "not from file";
    Uri outputFileUri = null;
    int embeddingPower;
    //MessageEmbedding messageEmbedding;
    TextView percentageText;
    int blockSizeSaved;
    int cropSizeSaved;
    EncodeFragment thisthis;
    int check;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.encoding, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        thisthis = this;
        File dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/");
        dir.mkdir();

        photo = (Button) view.findViewById(R.id.btPhotoEncode);
        encode = (Button) view.findViewById(R.id.btEncode);
        preview = (ImageView) view.findViewById(R.id.imPreview);
        pickFile = (Button) view.findViewById(R.id.btPickFile);
        inputText = (EditText) view.findViewById(R.id.etMessage);
        seekPower = (SeekBar) view.findViewById(R.id.sbEmbeddingPower);
        percentageText = (TextView) view.findViewById(R.id.tvSeekBar);

        if (savedInstanceState != null) {

            fileNameOriginal = savedInstanceState.getString(bundleNameOriginal);
            fileNameResult = savedInstanceState.getString(bundleNameResult);
            timeStamp = savedInstanceState.getString(bundleTimeStamp);
            if (savedInstanceState.containsKey(bundleUri)) {
                outputFileUri = Uri.parse(savedInstanceState.getString(bundleUri));
                Bitmap im = ReadImageScaled();
                if (im != null)
                {
                    preview.setImageBitmap(im);
                }

            }
            Log.v(TAG, "Activity restored.");
        } else {
            fileNameOriginal = "nothing here";
            fileNameResult = "nothing here";
            timeStamp = "nothing here";
            Log.v(TAG, "Activity NOT restored.");
        }



        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
                Log.v(TAG, timeStamp);

                fileNameOriginal = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-original.jpg";
                fileNameResult = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-result.png";
                inputText.setEnabled(true);
                pickFile.setEnabled(true);
                openImageIntent();
            }
        });



        //Opens a dialog that selects a txt file and loads it
        pickFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileChooser fileChooser = new FileChooser(getActivity());
                fileChooser.setExtension(".txt");
                fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
                    @Override
                    public void fileSelected(final File file) {
                        //Read text from file
                        StringBuilder text = new StringBuilder();
                        BufferedReader br;
                        try {
                            br = new BufferedReader(new FileReader(file));
                            String line;

                            while ((line = br.readLine()) != null) {
                                text.append(line);
                                text.append('\n');
                            }
                            br.close();
                            inputFile = text.toString();
                            inputText.setEnabled(false);
                            inputText.setHint("File correctly opened.");
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "File not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                fileChooser.showDialog();
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
                    pickFile.setEnabled(false);
                    inputFile = "not from file";
                    encode.setEnabled(true);
                }
                else
                {
                    pickFile.setEnabled(true);
                    encode.setEnabled(false);
                }

            }
        });


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

        encode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartActivity activity = (StartActivity) getActivity();
                blockSizeSaved = activity.BlockSize;
                cropSizeSaved = activity.CropSize;

                if(inputFile == "not from file")
                    inputFile = inputText.getText().toString();

                if (new File(fileNameOriginal).exists()) {
                    Log.v(TAG, "Starting encoding: " + blockSizeSaved + " " + cropSizeSaved);
                    MessageEmbedding messageEmbedding = new MessageEmbedding(thisthis, getContext(), inputFile, (byte)blockSizeSaved, cropSizeSaved, embeddingPower);
                    messageEmbedding.execute(ReadImage());
                }
            }
        });


        Log.v(TAG, "OnView ended");
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString(bundleNameOriginal, fileNameOriginal);
        outState.putString(bundleNameResult, fileNameResult);
        outState.putString(bundleTimeStamp, timeStamp);
        if (outputFileUri != null)
            outState.putString(bundleUri, outputFileUri.toString());

        super.onSaveInstanceState(outState);
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
            }
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
    public void onResultsReady(Bitmap bm, double[] signature) {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fileNameResult);
            bm.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
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
    }
}
