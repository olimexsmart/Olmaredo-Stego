package com.example.gliol.olmaredostego;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by gliol on 18/06/2016.
 */
public class EncodeFragment extends Fragment {
    private final String TAG = "EncodeFragment";
    private final int CAMERA_REQUEST_CODE = 4444;

    ImageButton photo;
    String fileNameOriginal;
    String fileNameResult;
    String timeStamp;
    Intent camera;
    Uri outputFileUri;
    Uri selectedImageUri;
    MessageEmbedding messageEmbedding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.encoding, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photo = (ImageButton) view.findViewById(R.id.chooseImage);
        photo.setImageResource(R.drawable.ic_action_name);

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
                Log.v(TAG, timeStamp);

                fileNameOriginal = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-original.jpg";
                fileNameResult = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-result.jpg";
/*
                File fileOriginal = new File(fileNameOriginal);
                try {
                    fileOriginal.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileOriginal));

                startActivityForResult(camera, CAMERA_REQUEST_CODE);
*/
                openImageIntent();
            }
        });
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

            if (isCamera) {
                selectedImageUri = outputFileUri;
            } else {
                selectedImageUri = data == null ? null : data.getData();
            }

            Bitmap im = ReadImage();
            if(im != null) {
                photo.setImageBitmap(im);
/*
                messageEmbedding = new MessageEmbedding(this, context, Lorem, (byte) 8, 10.0);
                messageEmbedding.execute(im);
*/
                Log.v(TAG, "Choosen photo.");
            }
            else{
                Log.v(TAG, "Image is null");
            }
        }
    }

    private Bitmap ReadImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1; //Set as you want but bigger than one
        options.inJustDecodeBounds = false;

        Cursor cursor = null;
        String result;
        Context c =
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = .getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return BitmapFactory.decodeFile(result);
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
        for(ResolveInfo res : listCam) {
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
}
