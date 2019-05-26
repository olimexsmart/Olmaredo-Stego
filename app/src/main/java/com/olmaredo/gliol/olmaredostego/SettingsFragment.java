package com.olmaredo.gliol.olmaredostego;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;
import java.util.Objects;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingFragment";
    private static final String TAG_SETTINGS = "SettingApp";
    //These should be written in the startActivity maybe
    static int DEFAULT_BLOCK_SIZE = 8;
    static int DEFAULT_CROP_SIZE = 1600;
    static int DEFAULT_EMBEDDING_POWER = 20;

    private static final String bundleEmbedPow = "bEP";
    private static final String bundleCropSize = "bCS";
    private static final String bundleBlockSize = "bBS";


    private OnSettingsUpdated callback;

    private int cropSize = DEFAULT_CROP_SIZE;
    private int blockSize = DEFAULT_BLOCK_SIZE;
    private int embeddingPower = DEFAULT_EMBEDDING_POWER; //Default embedding power


    private TextView percentageText; //Shows the embedding power strength
    private EditText etCropped;
    private RadioGroup groupRadio;
    private SeekBar seekPower;

    private RadioGroup.OnCheckedChangeListener oCCL;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        //Interface link to XML
        groupRadio = view.findViewById(R.id.rgSignatureSource);
        etCropped = view.findViewById(R.id.etCropped);
        percentageText = view.findViewById(R.id.tvSeekBar);
        Button restore = view.findViewById(R.id.btRestore);

        if (savedInstanceState != null) {
            embeddingPower = savedInstanceState.getInt(bundleEmbedPow);
            cropSize = savedInstanceState.getInt(bundleCropSize);
            blockSize = savedInstanceState.getInt(bundleBlockSize);
        } else
            restoreSavedSettings(); // Restoring settings from memory

        restoreBlockSize();


        // Restoring seekBar
        seekPower = view.findViewById(R.id.sbEmbeddingPower);
        seekPower.setProgress(embeddingPower);
        percentageText.setText(String.format(Locale.ITALIAN, "%d%%", embeddingPower));

        // Restore settings handler, no idea why the auto indentation is so messed up
        restore.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           cropSize = DEFAULT_CROP_SIZE;
                                           etCropped.setText(String.format(Locale.ITALIAN, "%d", cropSize));

                                           blockSize = DEFAULT_BLOCK_SIZE;
                                           restoreBlockSize();

                                           embeddingPower = DEFAULT_EMBEDDING_POWER;
                                           seekPower.setProgress(embeddingPower);

                                           callback.UpdateSettings(blockSize, cropSize, embeddingPower);
                                           saveSettings();
                                       }
                                   }
        );


        // Listener for cropping value
        etCropped.setText(String.valueOf(cropSize));
        etCropped.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not confirm setting if empty or less than 240 (decided at random)
                if (etCropped.getText().toString().trim().equalsIgnoreCase("")) {
                    etCropped.setError("Cannot be empty");
                    return;
                } else if (Integer.parseInt(etCropped.getText().toString()) < 240) {
                    etCropped.setError("Needs a number greater than 240");
                    return;
                }

                cropSize = Integer.parseInt(s.toString());
                callback.UpdateSettings(blockSize, cropSize, embeddingPower);
                saveSettings();

                Log.v(TAG, "Updated cropping setting");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        // Listener for block size
        oCCL = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.block4px) {
                    blockSize = 4;
                } else if (checkedId == R.id.block8px) {
                    blockSize = 8;
                } else {
                    blockSize = 16;
                }
                callback.UpdateSettings(blockSize, cropSize, embeddingPower);
                saveSettings();

                Log.v(TAG, "Updated block size setting");
            }
        };
        groupRadio.setOnCheckedChangeListener(oCCL);

        // Updates the embedding power value
        seekPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar bar) {
                embeddingPower = bar.getProgress(); // the value of the seekBar progress
                callback.UpdateSettings(blockSize, cropSize, embeddingPower);
                saveSettings();

                Log.v(TAG, "Updated power setting");
            }

            public void onStartTrackingTouch(SeekBar bar) {
                //Man...
            }

            public void onProgressChanged(SeekBar bar, int paramInt, boolean paramBoolean) {
                // Updating textView next to the bar
                percentageText.setText(String.format(Locale.ITALIAN, "%d%%", paramInt));
            }
        });
    }

    private void restoreSavedSettings() {
        SharedPreferences sp = Objects.requireNonNull(getActivity()).getSharedPreferences(TAG_SETTINGS, 0);

        cropSize = sp.getInt(bundleCropSize, DEFAULT_CROP_SIZE);
        blockSize = sp.getInt(bundleBlockSize, DEFAULT_BLOCK_SIZE);
        embeddingPower = sp.getInt(bundleEmbedPow, DEFAULT_EMBEDDING_POWER);
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = Objects.requireNonNull(getActivity()).getSharedPreferences(TAG_SETTINGS, 0).edit();

        editor.putInt(bundleCropSize, cropSize);
        editor.putInt(bundleBlockSize, blockSize);
        editor.putInt(bundleEmbedPow, embeddingPower);
        editor.apply();
    }

    private void restoreBlockSize() {
        // workaround https://stackoverflow.com/questions/4519103/error-in-androids-clearcheck-for-radiogroup/30450418
        groupRadio.setOnCheckedChangeListener(null);
        groupRadio.clearCheck();
        groupRadio.setOnCheckedChangeListener(oCCL);

        switch (blockSize) {
            case 4:
                groupRadio.check(R.id.block4px);
                break;
            case 8:
                groupRadio.check(R.id.block8px);
                break;
            case 16:
                groupRadio.check(R.id.block16px);
                break;
            default: // Avoided if using consistency checks
                groupRadio.check(R.id.block8px);
        }
    }

    @Override // Checking that classes as the method implemented
    public void onAttach(Context activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            callback = (OnSettingsUpdated) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSettingsUpdated");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(bundleEmbedPow, embeddingPower);
        outState.putInt(bundleBlockSize, blockSize);
        outState.putInt(bundleCropSize, cropSize);

        super.onSaveInstanceState(outState);
    }
}
