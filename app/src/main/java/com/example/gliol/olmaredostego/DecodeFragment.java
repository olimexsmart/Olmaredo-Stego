package com.example.gliol.olmaredostego;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static java.lang.Character.isLetter;

/*
    TODO find a generic signature that works
 */
public class DecodeFragment extends Fragment implements GetResultDecoding {
    private final int REQ_CODE = 2222;
    private final String TAG = "DecodeFragment";
    private static final String bundleNameOriginal = "bNO";
    private static final String bundleUri = "bU";


    Button photo;
    ImageView preview;
    RadioGroup groupRadio;
    EditText etCustom;
    Button decode;
    TextView result;
    Button toClipboard;
    DecodeFragment thisthis;
    String fileNameOriginal;
    Uri outputFileUri = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.decoding, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        thisthis = this;

        //Radio button e altre amenità
        photo = (Button) view.findViewById(R.id.btBrowseDecode);
        preview = (ImageView) view.findViewById(R.id.ivPreview);
        groupRadio = (RadioGroup) view.findViewById(R.id.radioGroup);
        etCustom = (EditText) view.findViewById(R.id.etCustom);
        decode = (Button) view.findViewById(R.id.btDecode);
        result = (TextView) view.findViewById(R.id.twShowResult);
        toClipboard = (Button) view.findViewById(R.id.btClipboardText);

        if (savedInstanceState != null) {
            fileNameOriginal = savedInstanceState.getString(bundleNameOriginal);
            if (savedInstanceState.containsKey(bundleUri)) {
                outputFileUri = Uri.parse(savedInstanceState.getString(bundleUri));
                Bitmap im = ReadImageScaled();
                if (im != null) {
                    preview.setImageBitmap(im);
                }
            }
            Log.v(TAG, "Activity restored.");
        } else {
            fileNameOriginal = "nothing here";
            Log.v(TAG, "Activity NOT restored.");
        }

        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, REQ_CODE);
            }
        });

        groupRadio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                if (checkedId == R.id.rbCustom) {
                    etCustom.setVisibility(View.VISIBLE);
                } else {
                    etCustom.setVisibility(View.INVISIBLE);
                    etCustom.setText("");
                }
            }
        });

        decode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartActivity activity = (StartActivity) getActivity();
                double[] signature = new double[activity.BlockSize * activity.BlockSize];

                //If there is written something and is coherent with the necessary signature, keep in mind that every number takes 5 characters
                if (etCustom.getText().toString().length() == activity.BlockSize * activity.BlockSize * 5) {
                    Toast.makeText(getContext(), "Using custom signature", Toast.LENGTH_LONG).show();
                    char[] key = etCustom.getText().toString().toCharArray();
                    for (int i = 0; i < signature.length; i++) {   //Bit level ascii hack, from char to int and at the correct position
                        signature[i] = (key[i * 5 + 1] - 48) / 10000.0 + (key[i * 5 + 2] - 48) / 1000.0 + (key[i * 5 + 3] - 48) / 100.0 + (key[i * 5 + 4] - 48) / 10.0;
                        //Change signBlackWhite if the incipit is a letter
                        if (isLetter(key[i * 5]))
                            signature[i] *= -1;
                    }

                } else { //Get the default one
                    Random caos = new Random();
                    for (int i = 0; i < signature.length; i++) {   //Consider implementing a function of i that better mimics the pdf of the vector
                        signature[i] = caos.nextInt(2500) / 10000.0;
                        if (caos.nextBoolean())
                            signature[i] *= -1;
                    }
                    Toast.makeText(getContext(), "Using default signature", Toast.LENGTH_LONG).show();
                }

                MessageDecoding messageDecoding = new MessageDecoding(getContext(), signature, thisthis);
                messageDecoding.execute(ReadImage());
            }
        });

        toClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("nothing", result.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Signature copied in the clipboard", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putString(bundleNameOriginal, fileNameOriginal);
        if (outputFileUri != null)
            outState.putString(bundleUri, outputFileUri.toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK && null != data) {
            outputFileUri = data.getData();
            //At this point we have the image selected Uri
            //Now get the absolute path into a nice String

            // SDK < API11
            if (Build.VERSION.SDK_INT < 11)
                fileNameOriginal = RealPathUtil.getRealPathFromURI_BelowAPI11(getContext(), outputFileUri);

                // SDK >= 11 && SDK < 19
            else if (Build.VERSION.SDK_INT < 19)
                fileNameOriginal = RealPathUtil.getRealPathFromURI_API11to18(getContext(), outputFileUri);

                // SDK > 19 (Android 4.4)
            else
                fileNameOriginal = RealPathUtil.getRealPathFromURI_API19(getContext(), outputFileUri);

            Bitmap im = ReadImageScaled();
            if (im != null) {
                preview.setImageBitmap(im);
                Log.v(TAG, "Choosen photo.");
            } else {
                Log.v(TAG, "Image is null");
            }
        }
    }

    private Bitmap ReadImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1; //Set as you want but bigger than one
        options.inJustDecodeBounds = false;

        try {
            InputStream imageStream = getActivity().getContentResolver().openInputStream(outputFileUri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            Log.v(TAG, outputFileUri.toString());
            return ExifUtil.rotateBitmap(fileNameOriginal, bitmap);
        } catch (FileNotFoundException e) {
            Log.v(TAG, "File not found.");
        }
        return null;
    }

    //http://stackoverflow.com/questions/3331527/android-resize-a-large-bitmap-file-to-scaled-output-file
    private Bitmap ReadImageScaled() {
        InputStream in = null;
        try {
            final int IMAGE_MAX_SIZE = 1200000; // 1.2MP
            in = getActivity().getContentResolver().openInputStream(outputFileUri);

            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d(TAG, "scale = " + scale + ", orig-width: " + o.outWidth + ", orig-height: " + o.outHeight);

            Bitmap b = null;
            in = getActivity().getContentResolver().openInputStream(outputFileUri);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                o = new BitmapFactory.Options();
                o.inSampleSize = scale;
                b = BitmapFactory.decodeStream(in, null, o);

                // resize to desired dimensions
                int height = b.getHeight();
                int width = b.getWidth();
                Log.d(TAG, "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, (int) x,
                        (int) y, true);
                b.recycle();
                b = scaledBitmap;

                System.gc();
            } else {
                b = BitmapFactory.decodeStream(in);
            }
            in.close();

            Log.d(TAG, "bitmap size - width: " + b.getWidth() + ", height: " +
                    b.getHeight());
            return ExifUtil.rotateBitmap(fileNameOriginal, b);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }


    @Override
    public void OnResultReady(String message) {
        result.setText(message);
    }
}