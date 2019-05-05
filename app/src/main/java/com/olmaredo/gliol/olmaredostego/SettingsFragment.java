package com.olmaredo.gliol.olmaredostego;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

/*
	TODO enhance hints and suggestions on how to use setting
	TODO solve warnings
	TODO consistency checks
 */

public class SettingsFragment extends Fragment {
    private final String TAG = "SettingFragment";
    //These should be written in the startActivity maybe
    static int DEFAULT_BLOCK_SIZE = 8;
    static int DEFAULT_CROP_SIZE = 480;
    static int DEFAULT_EMBEDDING_POWER = 20;

    private static final String bundleEmbedPow = "bEP";
    private static final String bundleCropSize = "bCS";
    private static final String bundleBlockSize = "bBS";


    private OnSettingsUpdated callback;

    private int blockSize = DEFAULT_BLOCK_SIZE;
    private int embeddingPower = DEFAULT_EMBEDDING_POWER; //Default embedding power
    private int cropSize = DEFAULT_CROP_SIZE;


    private TextView percentageText; //Shows the embedding power strength


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        //Interface link to XML
        RadioGroup groupRadio = view.findViewById(R.id.rgSignatureSource);
        EditText etCropped = view.findViewById(R.id.etCropped);
        percentageText = view.findViewById(R.id.tvSeekBar);

        if (savedInstanceState != null) {
            embeddingPower = savedInstanceState.getInt(bundleEmbedPow);
            cropSize = savedInstanceState.getInt(bundleCropSize);
            blockSize = savedInstanceState.getInt(bundleBlockSize);
        }

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

        // Restoring seekBar
        SeekBar seekPower = view.findViewById(R.id.sbEmbeddingPower);
        seekPower.setProgress(embeddingPower);
        percentageText.setText(String.format(Locale.ITALIAN, "%d%%", embeddingPower));

        // Listener for cropping value
        etCropped.setText(String.valueOf(cropSize));
        etCropped.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cropSize = Integer.parseInt(s.toString());
                callback.UpdateSettings(blockSize, cropSize, embeddingPower);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Listener for block size
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
                callback.UpdateSettings(blockSize, cropSize, embeddingPower);
            }
        });

        // Updates the embedding power value
        seekPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar bar) {
                embeddingPower = bar.getProgress(); // the value of the seekBar progress
                callback.UpdateSettings(blockSize, cropSize, embeddingPower);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(bundleEmbedPow, embeddingPower);
        outState.putInt(bundleBlockSize, blockSize);
        outState.putInt(bundleCropSize, cropSize);

        super.onSaveInstanceState(outState);
    }
}
