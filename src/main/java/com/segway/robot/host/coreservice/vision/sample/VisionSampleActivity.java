package com.segway.robot.host.coreservice.vision.sample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.stream.StreamType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The Sample Activity demonstrate the main function of Segway Robot VisionService.
 *
 * @author jacob
 */
public class VisionSampleActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "VisionSampleActivity";
    private static final int TIME_PERIOD = 5 * 1000;

    private Vision mVision;

    private Switch mPreviewSwitch;
    private Switch mTransferSwitch;
    private Switch mSaveColorSwitch;
    private Switch mSaveDepthSwitch;

    private SurfaceView mColorSurfaceView;
    private SurfaceView mDepthSurfaceView;

    private ImageView mColorImageView;
    private ImageView mDepthImageView;

    private boolean mIsSaveColor;
    private boolean mIsSaveDepth;

    private PreviewPresenter mPreviewPresenter;
    private TransferPresenter mTransferPresenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vision_sample);

        // init content view
        mPreviewSwitch = (Switch) findViewById(R.id.preview);
        mTransferSwitch = (Switch) findViewById(R.id.transfer);

        mPreviewSwitch.setOnCheckedChangeListener(this);
        mTransferSwitch.setOnCheckedChangeListener(this);


        mColorSurfaceView = (SurfaceView) findViewById(R.id.colorSurface);
        mDepthSurfaceView = (SurfaceView) findViewById(R.id.depthSurface);


        mSaveColorSwitch = (Switch) findViewById(R.id.saveColor);
        mSaveDepthSwitch = (Switch) findViewById(R.id.saveDepth);

        mSaveColorSwitch.setOnCheckedChangeListener(this);
        mSaveDepthSwitch.setOnCheckedChangeListener(this);


        mColorImageView = (ImageView) findViewById(R.id.colorImage);
        mDepthImageView = (ImageView) findViewById(R.id.depthImage);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mVision.bindService(this, mBindStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreviewSwitch.setChecked(false);
        mTransferSwitch.setChecked(false);
        mSaveColorSwitch.setChecked(false);
        mSaveDepthSwitch.setChecked(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVision.unbindService();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    /**
     * @param buttonView
     * @param isChecked
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.preview:
                if (isChecked) {
                    if (mPreviewPresenter == null) {
                        mPreviewPresenter = new PreviewPresenter(mVision, mColorSurfaceView, mDepthSurfaceView);
                    }
                    mPreviewPresenter.start();
                } else {
                    mPreviewPresenter.stop();
                }
                break;
            case R.id.transfer:
                if (isChecked) {
                    if (mTransferPresenter == null) {
                        mTransferPresenter = new TransferPresenter(mVision, mIImageState);
                    }
                    mTransferPresenter.start();
                    mSaveColorSwitch.setEnabled(true);
                    mSaveDepthSwitch.setEnabled(true);
                } else {
                    mTransferPresenter.stop();
                    mSaveColorSwitch.setEnabled(false);
                    mSaveDepthSwitch.setEnabled(false);
                }
                mSaveColorSwitch.setChecked(false);
                mSaveDepthSwitch.setChecked(false);
                break;
            case R.id.saveColor:
                if (isChecked) {
                    mIsSaveColor = true;
                } else {
                    mIsSaveColor = false;
                }
                break;
            case R.id.saveDepth:
                if (isChecked) {
                    mIsSaveDepth = true;
                } else {
                    mIsSaveDepth = false;
                }
                break;
        }
    }

    ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind() called");
            mPreviewSwitch.setEnabled(true);
            mTransferSwitch.setEnabled(true);
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "onUnbind() called with: reason = [" + reason + "]");
        }
    };

    private IImageState mIImageState = new IImageState() {

        Runnable mRunnable;

        @Override
        public void updateImage(int type, final Bitmap bitmap) {
            switch (type) {
                case StreamType.COLOR:
                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mColorImageView.setImageBitmap(bitmap);
                        }
                    };
                    if (mIsSaveColor) {
                        saveColorToFile(bitmap);
                    }
                    break;
                case StreamType.DEPTH:
                    mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mDepthImageView.setImageBitmap(bitmap);
                        }
                    };
                    if (mIsSaveDepth) {
                        saveDepthToFile(bitmap);
                    }
                    break;
            }

            if (mRunnable != null) {
                runOnUiThread(mRunnable);
            }
        }
    };

    private long startTimeColor = System.currentTimeMillis();

    private void saveColorToFile(final Bitmap bitmap) {

        if (System.currentTimeMillis() - startTimeColor < TIME_PERIOD) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                startTimeColor = System.currentTimeMillis();
                File f = new File(getExternalFilesDir(null).getAbsolutePath() + "/C" + System.currentTimeMillis() + ".png");
                Log.d(TAG, "saveBitmapToFile(): " + f.getAbsolutePath());
                try {
                    FileOutputStream fOut = new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private long startTimeDepth = System.currentTimeMillis();

    private void saveDepthToFile(final Bitmap bitmap) {

        if (System.currentTimeMillis() - startTimeDepth < TIME_PERIOD) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                startTimeDepth = System.currentTimeMillis();
                File f = new File(getExternalFilesDir(null).getAbsolutePath() + "/D" + System.currentTimeMillis() + ".png");
                Log.d(TAG, "saveBitmapToFile(): " + f.getAbsolutePath());
                try {
                    FileOutputStream fOut = new FileOutputStream(f);
                    Bitmap greyBitmap = depth2Grey(bitmap);
                    greyBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    /**
     * Convert depth image into grey image
     *
     * @param img the depth image in RGB_565 format
     * @return the GREY image in RGB_565 format
     */
    private Bitmap depth2Grey(Bitmap img) {
        int width = img.getWidth();
        int height = img.getHeight();

        int[] pixels = new int[width * height];

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                //grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                grey = (red * 38 + green * 75 + blue * 15) >> 7;
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }
}
