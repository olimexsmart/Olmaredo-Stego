package com.example.gliol.olmaredostego;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.design.widget.TabLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class StartActivity extends AppCompatActivity implements GetResultDecoding, SettingsFragment.OnSettingsUpdated{
    private static final String TAG = "StartActivity";
    //Used as test
    private static final String Lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi sit amet ligula vitae tortor finibus viverra ut ac nulla. Suspendisse feugiat est non interdum finibus. Aenean nisi odio, congue in velit ac, gravida lobortis sapien. Donec ut mi finibus, dapibus leo eu, eleifend tortor. Ut mattis euismod pharetra. Nam tincidunt accumsan eros vitae congue. Quisque varius blandit bibendum. Praesent pellentesque aliquet ligula eget hendrerit. Curabitur fringilla venenatis erat, ut porta mauris auctor non.";

    //Used to retrieve data when activity is reloaded

    //private static final String bundleSignature = "bS";
    private static final String bundleCropSize = "bCS";
    private static final String bundleBlockSize = "bBS";


    Button getResult;
    Intent camera;


    ImageView output;

    MessageDecoding messageDecoding;
    Context context;
    double[] signature;
    TextView resultHealth;
    StartActivity thisthis;

    TabLayout tabLayout;
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;

    String stringBlock;

    public int BlockSize = SettingsFragment.DEFAULT_BLOCK_SIZE;
    public int CropSize = SettingsFragment.DEFAULT_CROP_SIZE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        getSupportActionBar().hide();

        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        final TabLayout.Tab encode = tabLayout.newTab();
        final TabLayout.Tab decode = tabLayout.newTab();
        final TabLayout.Tab info = tabLayout.newTab();
        final TabLayout.Tab settings = tabLayout.newTab();

        encode.setText("Encode");
        decode.setText("Decode");
        info.setText("Info");
        settings.setText("Setup");

        tabLayout.addTab(info, 0);
        tabLayout.addTab(encode, 1);
        tabLayout.addTab(decode, 2);
        tabLayout.addTab(settings, 3);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(viewPagerAdapter);

        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.drawable.tab_selector));
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.indicator));

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

/*

        getResult = (Button) findViewById(R.id.decode);
        resultHealth = (TextView) findViewById(R.id.resultHealth);
        original = (ImageView) findViewById(R.id.imageView1);
        output = (ImageView) findViewById(R.id.imageView2);
        */
        context = this;
        thisthis = this;
        camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Create the app directory
        File dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/");
        dir.mkdir();


        if (savedInstanceState != null) {
            BlockSize = savedInstanceState.getInt(bundleBlockSize);
            CropSize = savedInstanceState.getInt(bundleCropSize);

            Log.v(TAG, "Activity restored.");
        } else {
            Log.v(TAG, "Activity NOT restored.");
        }

        Log.v(TAG, "Created instances");

/*


        getResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, fileNameResult);

            }
        });
        */


    }

    //Save data in case activity is killed or restarted
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(bundleBlockSize, BlockSize);
        outState.putInt(bundleCropSize, CropSize);
        super.onSaveInstanceState(outState);
    }
/*

    private Bitmap ReadImageThumb(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 8; //Set as you want but bigger than one
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path);
    }

*/
  /*  //Returning data from MessageEmbedding
    @Override
    public void onResultsReady(Bitmap result, double[] signature) {
        //salvare la bitmap
        messageEmbedding = null;
        output.setImageBitmap(result);
        this.signature = signature;


    }
*/

    //Returning data from MessageDecoding
    @Override
    public void OnResultReady(String message) {
       /* messageDecoding = null;
        //Saving result as text as debug support
        try {
            String path = Environment.getExternalStorageDirectory() + "/PicturesTest/" + timeStamp + "-decoded.txt";
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

        int offset;
        for(offset = 0; offset < lenght; offset++)
        {
            if(message.charAt(offset) == 'L') break;
        }
        int healthIndex = 0;

        for(int i = 0; i < lenght; i++)
        {
            if(Lorem.charAt(i) == message.charAt(i + offset))
                healthIndex++;
        }
        Log.v(TAG, "Healthindex: " + healthIndex);

        resultHealth.setText(Math.round((healthIndex / (double)Lorem.length()) * 100) + "%");
        */
    }


    @Override
    public void UpdateSettings(int blockSize, int cropSize) {
        BlockSize = blockSize;
        CropSize = cropSize;
    }
}
