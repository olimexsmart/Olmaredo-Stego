package com.example.gliol.olmaredostego;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class StartActivity extends AppCompatActivity implements GetResult{
    private static final String TAG = "StartActivity";

    Button b;
    Intent camera;
    String fileName;
    ImageView original;
    ImageView output;
    MessageEmbedding messageEmbedding;
    Context context;
    //GetResult res;

    int camReqCode = 4444;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
/*
        res = new GetResult() {
            @Override
            public void onResultsReady(Bitmap bm) {
                ManageResult(bm);
            }
        };
*/
        b = (Button) findViewById(R.id.button);
        original = (ImageView) findViewById(R.id.imageView1);
        output = (ImageView) findViewById(R.id.imageView2);
        context = this;
        messageEmbedding = new MessageEmbedding(this , context, "null", 8, 10.0);
        fileName = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
        Log.v(TAG, "Created instances");

        camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);



        File dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/");
        dir.mkdir();

        dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/" + fileName + "-original.jpg");

        try {
            dir.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(dir));

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(camera, camReqCode);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == camReqCode && resultCode == RESULT_OK) {
            Log.v(TAG, "Entered activity result");
            /*
                Here the image could be modified or converted into something else
             */
            String path = Environment.getExternalStorageDirectory() + "/PicturesTest/" + fileName + "-original.jpg";
            //File image = new File(path);

            Bitmap im = ReadImage(path);
            original.setImageBitmap(im);
            messageEmbedding.execute(im);
            Log.v(TAG, "Taken photo and launched task.");
        }
    }

    private Bitmap ReadImage(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1; //Set as you want but bigger than one
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path);
    }

    @Override
    public void onResultsReady(Bitmap result) {
        //salvare la bitmap
        output.setImageBitmap(result);

        FileOutputStream out = null;
        try {
            String path = Environment.getExternalStorageDirectory() + "/PicturesTest/" + fileName + "-result.jpg";
            out = new FileOutputStream(path);
            result.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
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
