package com.example.gliol.olmaredostego;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by gliol on 18/06/2016.
 */
public class SettingsFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Button bt = (Button)view.findViewById(R.id.btSettings);
        EditText etBlock = (EditText) view.findViewById(R.id.etBlock);
        EditText etCropped = (EditText) view.findViewById(R.id.etCropped);

        //Default values
        etBlock.setText(String.valueOf(8));
        etCropped.setText(String.valueOf(480));

        //Button
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });

    }


}
