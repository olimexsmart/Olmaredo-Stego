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

/*
    TODO WHEN EVERYTHING IS COMPLETED: clean up this file from useless code
 */

public class StartActivity extends AppCompatActivity implements SettingsFragment.OnSettingsUpdated{
    private static final String TAG = "StartActivity";
    //Used as test
    public static final String Lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi sit amet ligula vitae tortor finibus viverra ut ac nulla. Suspendisse feugiat est non interdum finibus. Aenean nisi odio, congue in velit ac, gravida lobortis sapien. Donec ut mi finibus, dapibus leo eu, eleifend tortor. Ut mattis euismod pharetra. Nam tincidunt accumsan eros vitae congue. Quisque varius blandit bibendum. Praesent pellentesque aliquet ligula eget hendrerit. Curabitur fringilla venenatis erat, ut porta mauris auctor non.";

    //Used to retrieve data when activity is reloaded

    //private static final String bundleSignature = "bS";
    private static final String bundleCropSize = "bCS";
    private static final String bundleBlockSize = "bBS";
    private static final String bundleInColor = "bIC";

    Intent camera;
    Context context;
    StartActivity thisthis;
    TabLayout tabLayout;
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;

    public int BlockSize = SettingsFragment.DEFAULT_BLOCK_SIZE;
    public int CropSize = SettingsFragment.DEFAULT_CROP_SIZE;
    public boolean inColor = false;


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
            inColor = savedInstanceState.getBoolean(bundleInColor);
            Log.v(TAG, "Start Activity restored.");
        } else {
            Log.v(TAG, "Start Activity NOT restored.");
        }

        Log.v(TAG, "Created instances");

    }

    //Save data in case activity is killed or restarted
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(bundleBlockSize, BlockSize);
        outState.putInt(bundleCropSize, CropSize);
        outState.putBoolean(bundleInColor, inColor);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void UpdateSettings(int blockSize, int cropSize, boolean color) {
        BlockSize = blockSize;
        CropSize = cropSize;
        inColor = color;
    }
}
