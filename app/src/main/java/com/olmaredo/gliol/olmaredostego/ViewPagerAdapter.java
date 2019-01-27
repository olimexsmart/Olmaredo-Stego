package com.olmaredo.gliol.olmaredostego;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.Log;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private int numberOfTabs = 0;
    public ViewPagerAdapter(FragmentManager fm, int NumberOfTabs) {
        super(fm);
        numberOfTabs = NumberOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        Log.v("Viewpagerblabla", position + "selected");

        switch (position) {
            case 1:
                return new EncodeFragment();
            case 2:
                return new DecodeFragment();
            case 0:
                return new InfoFragment();
            case 3:
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