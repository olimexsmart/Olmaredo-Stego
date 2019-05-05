package com.olmaredo.gliol.olmaredostego;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import android.util.Log;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = "ViewPagerBlaBla";

    private int numberOfTabs;

    ViewPagerAdapter(FragmentManager fm, int NumberOfTabs) {
        super(fm);
        numberOfTabs = NumberOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        Log.v(TAG, "Tab number " + position + " selected");

        switch (position) {
            case 0:
                return new EncodeFragment();
            case 1:
                return new DecodeFragment();
            case 2:
                return new SettingsFragment();
            default:
                return null;
        }
    }


    @Override
    public int getCount() {
        return numberOfTabs;           // As there are only 3 Tabs
    }

}