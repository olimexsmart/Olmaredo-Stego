package com.example.gliol.olmaredostego;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by gliol on 18/06/2016.
 */
public class SettingsFragment extends Fragment {
    private final String TAG = "SettingFragment";
    static int DEFAULT_BLOCK_SIZE = 8;
    static int DEFAULT_CROP_SIZE = 480;

    EditText etBlock;
    EditText etCropped;
    OnSettingsUpdated callback;

    public interface OnSettingsUpdated{
        void UpdateSettings(int blockSize, int cropSize);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Button bt = (Button)view.findViewById(R.id.btSettings);

        etBlock = (EditText) view.findViewById(R.id.etBlock);
        etCropped = (EditText) view.findViewById(R.id.etCropped);

        //Default values
        etBlock.setText(String.valueOf(DEFAULT_BLOCK_SIZE));
        etCropped.setText(String.valueOf(DEFAULT_CROP_SIZE));


        //Button
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.UpdateSettings(Integer.parseInt(etBlock.getText().toString()), Integer.parseInt(etCropped.getText().toString()));
                Toast.makeText(getContext(), "Settings updated!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            callback = (OnSettingsUpdated) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSettingsUpdated");
        }
    }


}
