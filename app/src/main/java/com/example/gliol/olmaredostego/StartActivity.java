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
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class StartActivity extends AppCompatActivity implements GetResultEmbedding, GetResultDecoding{
    private static final String TAG = "StartActivity";
    private static final String Lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi sit amet ligula vitae tortor finibus viverra ut ac nulla. Suspendisse feugiat est non interdum finibus. Aenean nisi odio, congue in velit ac, gravida lobortis sapien. Donec ut mi finibus, dapibus leo eu, eleifend tortor. Ut mattis euismod pharetra. Nam tincidunt accumsan eros vitae congue. Quisque varius blandit bibendum. Praesent pellentesque aliquet ligula eget hendrerit. Curabitur fringilla venenatis erat, ut porta mauris auctor non.";

    Button photo;
    Button getResult;
    Intent camera;
    String fileName;
    ImageView original;
    ImageView output;
    MessageEmbedding messageEmbedding;
    MessageDecoding messageDecoding;
    Context context;
    double[] signature;
    TextView resultHealth;
    Bitmap embeddedPicture;
    StartActivity thisthis;

    int camReqCode = 4444;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        photo = (Button) findViewById(R.id.button);
        getResult = (Button) findViewById(R.id.decode);
        resultHealth = (TextView) findViewById(R.id.resultHealth);
        original = (ImageView) findViewById(R.id.imageView1);
        output = (ImageView) findViewById(R.id.imageView2);
        context = this;
        fileName = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ITALIAN).format(new Date());
        thisthis = this;
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

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(camera, camReqCode);
            }
        });

        getResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageDecoding = new MessageDecoding(context, signature, thisthis);
                messageDecoding.execute(embeddedPicture);
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

            messageEmbedding = new MessageEmbedding(this , context, Lorem, (byte)8, 10.0);
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
    public void onResultsReady(Bitmap result, double[] signature) {
        //salvare la bitmap
        messageEmbedding = null;
        output.setImageBitmap(result);
        this.signature = signature;
        embeddedPicture = result;

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

    @Override
    public void OnResultReady(String message) {
        messageDecoding = null;
        //Saving result as text as debug support
        try {
            String path = Environment.getExternalStorageDirectory() + "/PicturesTest/" + fileName + "-textresult.txt";
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(path));
            outputStreamWriter.write(message);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

        //Comparing each character trying to estimate the quality of the result
        int lenght = message.length();
        if(message.length() > Lorem.length()) {
            Log.v(TAG, "Message longer than lorem ipsum sample. " + message.length() + " vs " + Lorem.length());
            lenght = Lorem.length();
        }

        int healthIndex = 0;
        for(int i = 0; i < lenght; i++)
        {
            if(Lorem.charAt(i) == message.charAt(i))
                healthIndex++;
        }

        resultHealth.setText((healthIndex / Lorem.length()) + "%");
    }
}
