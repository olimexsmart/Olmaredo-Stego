package com.example.gliol.olmaredostego;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
import java.io.IOException;
import java.net.URI;

public class StartActivity extends AppCompatActivity {
    private static final String TAG = "StartActivity";

    Button b;
    Intent camera;
    String fileName = "bomber.jpg";
    ImageView color;
    ImageView blackWhite;
    MessageEmbedding messageEmbedding;
    Context context;

    int camReqCode = 4444;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        b = (Button) findViewById(R.id.button);
        color = (ImageView) findViewById(R.id.imageView1);
        blackWhite = (ImageView) findViewById(R.id.imageView2);
        context = this;
        messageEmbedding = new MessageEmbedding(context, "null", 8);
        Log.v(TAG, "Created instances");

        camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/");
        dir.mkdir();

        dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/" + fileName);

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
            /*
                Here the image could be modified or converted into something else
             */
            String path = Environment.getExternalStorageDirectory() + "/PicturesTest/" + fileName;
            //File image = new File(path);

            Bitmap im = ReadImage(path);
            Log.v(TAG, "Taken photo and launched task.");
            messageEmbedding.execute(im);

        }
    }

    private Bitmap ReadImage(String path){
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1; //Set as you want but bigger than one
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path);
    }


    /*
    Found at:
    http://stackoverflow.com/questions/3373860/convert-a-bitmap-to-grayscale-in-android

    private Bitmap toGrayscale1(Bitmap bmpOriginal)
    {   //Creating a new bitmap image
        Bitmap bmpGrayscale = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);

        //Canvas let you modify Bitmaps through drawBitmap that takes a Paint object as parameter
        Canvas c = new Canvas(bmpGrayscale);

        //Create a new Paint object and setColorFilter through a ColorMatrixColorFilter
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);    //This is the whole point of this: we want the saturation to be zero
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);

        //Apply the filter and return
        c.drawBitmap(bmpOriginal, 0, 0, paint);

        return bmpGrayscale;
    }
 */

    /*
    Found at:
    http://stackoverflow.com/questions/8381514/android-converting-color-image-to-grayscale
     */

}

