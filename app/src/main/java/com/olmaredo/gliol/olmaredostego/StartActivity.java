package com.olmaredo.gliol.olmaredostego;

import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.support.design.widget.TabLayout;

import java.io.File;

/*
    TODO WHEN EVERYTHING IS COMPLETED: clean up this file from useless code
 */

public class StartActivity extends AppCompatActivity implements SettingsFragment.OnSettingsUpdated{
    private static final String TAG = "StartActivity";
    //Used as test
    public static final String Lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi sit amet ligula vitae tortor finibus viverra ut ac nulla. Suspendisse feugiat est non interdum finibus. Aenean nisi odio, congue in velit ac, gravida lobortis sapien. Donec ut mi finibus, dapibus leo eu, eleifend tortor. Ut mattis euismod pharetra. Nam tincidunt accumsan eros vitae congue. Quisque varius blandit bibendum. Praesent pellentesque aliquet ligula eget hendrerit. Curabitur fringilla venenatis erat, ut porta mauris auctor non.";

    //private static final String bundleSignature = "bS";
    private static final String bundleCropSize = "bCS";
    private static final String bundleBlockSize = "bBS";
    private static final String bundleInColor = "bIC";
    private static final String bundlePatternReduction = "bPR";

    TabLayout tabLayout;
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;

    public int BlockSize = SettingsFragment.DEFAULT_BLOCK_SIZE;
    public int CropSize = SettingsFragment.DEFAULT_CROP_SIZE;
    public boolean InColor = false;
    public boolean PatternReduction = false;


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

        //Create the app directory
        File dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/");
        dir.mkdir();

        if (savedInstanceState != null) {
            BlockSize = savedInstanceState.getInt(bundleBlockSize);
            CropSize = savedInstanceState.getInt(bundleCropSize);
            InColor = savedInstanceState.getBoolean(bundleInColor);
            PatternReduction = savedInstanceState.getBoolean(bundlePatternReduction);
            Log.v(TAG, "Start Activity restored.");
        } else {
            Log.v(TAG, "Start Activity NOT restored.");
        }

        Log.v(TAG, "OnCreate completed.");
    }

    //Save data in case activity is killed or restarted
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(bundleBlockSize, BlockSize);
        outState.putInt(bundleCropSize, CropSize);
        outState.putBoolean(bundleInColor, InColor);
        outState.putBoolean(bundlePatternReduction, PatternReduction);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void UpdateSettings(int blockSize, int cropSize, boolean color, boolean patternReduction) {
        BlockSize = blockSize;
        CropSize = cropSize;
        InColor = color;
        PatternReduction = patternReduction;
    }
}
