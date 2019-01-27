package com.olmaredo.gliol.olmaredostego;

import android.os.Environment;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.google.android.material.tabs.TabLayout;

import java.io.File;

/*
    TODO WHEN EVERYTHING IS COMPLETED: clean up this file from useless code
 */

public class StartActivity extends AppCompatActivity implements SettingsFragment.OnSettingsUpdated{
    private static final String TAG = "StartActivity";

    private static final String bundleCropSize = "bCS";
    private static final String bundleBlockSize = "bBS";
    private static final String bundleInColor = "bIC";

	//Objects that manage the Tab GUI
    TabLayout tabLayout;
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;

	//This data is used to communicate between tabs
    public int BlockSize = SettingsFragment.DEFAULT_BLOCK_SIZE;
    public int CropSize = SettingsFragment.DEFAULT_CROP_SIZE;
    public boolean inColor = SettingsFragment.DEFAULT_ON_COLOR;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        getSupportActionBar().hide(); //Hide the bar with the title, take up space and it's useless
		
		//Create and connects the Java objects to the XML design
        tabLayout = (TabLayout) findViewById(R.id.tablayout); 
        final TabLayout.Tab encode = tabLayout.newTab();
        final TabLayout.Tab decode = tabLayout.newTab();
        final TabLayout.Tab info = tabLayout.newTab();
        final TabLayout.Tab settings = tabLayout.newTab();
        encode.setText("Encode");
        decode.setText("Decode");
        info.setText("Info");
        settings.setText("Setup");
		//Position of tabs
        tabLayout.addTab(info, 0);
        tabLayout.addTab(encode, 1);
        tabLayout.addTab(decode, 2);
        tabLayout.addTab(settings, 3);
		//No idea, just woks
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(viewPagerAdapter);
		//Colors are important
        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.drawable.tab_selector));
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.indicator));
		//Click on the tab and change view
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        //Create the app directory
        File dir = new File(Environment.getExternalStorageDirectory() + "/PicturesTest/");
        dir.mkdir();
		//As always activity information needs to be saved and restored
        if (savedInstanceState != null) {
            BlockSize = savedInstanceState.getInt(bundleBlockSize);
            CropSize = savedInstanceState.getInt(bundleCropSize);
            inColor = savedInstanceState.getBoolean(bundleInColor);
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
        outState.putBoolean(bundleInColor, inColor);
        super.onSaveInstanceState(outState);
    }
    
	@Override //Simply nicer than directly reference class objects, TODO maybe some kind of consistency check would be nice
    public void UpdateSettings(int blockSize, int cropSize, boolean color) {
        BlockSize = blockSize;
        CropSize = cropSize;
        inColor = color;
    }
}
