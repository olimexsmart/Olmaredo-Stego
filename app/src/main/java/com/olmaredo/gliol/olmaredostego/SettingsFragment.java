package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

/*
    TODO manage fragment restore
    TODO empty editext when selected, so isn't necessary to erase the content every time
	TODO get rid of that save settings button
	TODO enhance hints and suggestions on how to use setting
 */

public class SettingsFragment extends Fragment {
    private final String TAG = "SettingFragment";
	//These should be written in the startActivity maybe
    static int DEFAULT_BLOCK_SIZE = 8;
    static int DEFAULT_CROP_SIZE = 480;

    EditText etCropped;
    OnSettingsUpdated callback;
    Switch onColor;
    RadioGroup groupRadio;
    int blockSize = DEFAULT_BLOCK_SIZE;

    public interface OnSettingsUpdated {
        void UpdateSettings(int blockSize, int cropSize, boolean color);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        //Interface link to XML
        Button bt = (Button) view.findViewById(R.id.btSettings);
        groupRadio = (RadioGroup) view.findViewById(R.id.rgSignatureSource);
        etCropped = (EditText) view.findViewById(R.id.etCropped);

        //Default values
        etCropped.setText(String.valueOf(DEFAULT_CROP_SIZE));


        //Button
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.UpdateSettings(blockSize, Integer.parseInt(etCropped.getText().toString()), onColor.isChecked());
                Toast.makeText(getContext(), "Settings updated!", Toast.LENGTH_SHORT).show();
            }
        });

        groupRadio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.block4px) {
                    blockSize = 4;
                } else if (checkedId == R.id.block8px) {
                    blockSize = 8;
                } else {
                    blockSize = 16;
                }
            }
        });
    }

    @Override //It's deprecated but it works, STFU
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
