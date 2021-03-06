package com.olmaredo.gliol.olmaredostego;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.Objects;

import static com.olmaredo.gliol.olmaredostego.SettingsFragment.DEFAULT_BLOCK_SIZE;
import static com.olmaredo.gliol.olmaredostego.SettingsFragment.DEFAULT_CROP_SIZE;
import static com.olmaredo.gliol.olmaredostego.SettingsFragment.DEFAULT_EMBEDDING_POWER;


public class StartActivity extends AppCompatActivity implements OnSettingsUpdated {
    private static final String TAG = "StartActivity";
    private static final String TAG_SETTINGS = "SettingApp";

    private static final String bundleEmbedPow = "bEP";
    private static final String bundleCropSize = "bCS";
    private static final String bundleBlockSize = "bBS";

    //Objects that manage the Tab GUI
    TabLayout tabLayout;
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;

    public int Tab = -1;

	//This data is used to communicate between tabs
    public int BlockSize = DEFAULT_BLOCK_SIZE;
    public int CropSize = DEFAULT_CROP_SIZE;
    public int EmbeddingPower = DEFAULT_EMBEDDING_POWER;


    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        restoreSavedSettings();

        //Create and connects the Java objects to the XML design
        tabLayout = findViewById(R.id.tablayout);
        final TabLayout.Tab encode = tabLayout.newTab();
        final TabLayout.Tab decode = tabLayout.newTab();
        final TabLayout.Tab settings = tabLayout.newTab();
        encode.setText("Encode");
        decode.setText("Decode");
        settings.setText("Settings");

        //Position of tabs
        tabLayout.addTab(encode, 0);
        tabLayout.addTab(decode, 1);
        tabLayout.addTab(settings, 2);

        //No idea, just woks
        viewPager = findViewById(R.id.viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(viewPagerAdapter);

        //Colors are important
        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.drawable.tab_selector));
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.indicator));

        //Click on the tab and change view
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        //Create the app directory
        String pathToPictureFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        File dir = new File(pathToPictureFolder + "/Olmaredo/");
        if (!dir.mkdir()) {
            Snackbar.make(tabLayout, "Could not create folder!", Snackbar.LENGTH_INDEFINITE);
        }

        Log.v(TAG, "OnCreate completed.");
    }


    @Override
    //Simply nicer than directly reference class objects
    public void UpdateSettings(int blockSize, int cropSize, int embeddingPower) {
        BlockSize = blockSize;
        CropSize = cropSize;
        EmbeddingPower = embeddingPower;
    }

    private void restoreSavedSettings() {
        SharedPreferences sp = Objects.requireNonNull(getSharedPreferences(TAG_SETTINGS, 0));

        CropSize = sp.getInt(bundleCropSize, DEFAULT_CROP_SIZE);
        BlockSize = sp.getInt(bundleBlockSize, DEFAULT_BLOCK_SIZE);
        EmbeddingPower = sp.getInt(bundleEmbedPow, DEFAULT_EMBEDDING_POWER);
    }
}
