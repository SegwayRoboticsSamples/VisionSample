package com.segway.robot.host.coreservice.vision.sample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
/**
 * The Sample Activity demonstrate the main function of Segway Robot VisionService.
 */
public class VisionSampleActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
    private boolean mBind;
    private Vision mVision;

    private Switch mBindSwitch;
    private Switch mPreviewSwitch;
    private Switch mTransferSwitch;
    private Switch mSaveColorSwitch;
    private Switch mSaveDepthSwitch;

    private SurfaceView mColorSurfaceView;
    private SurfaceView mDepthSurfaceView;

    private ImageView mColorImageView;
    private ImageView mDepthImageView;

    private StreamInfo colorInfo;
    private StreamInfo depthInfo;

    private Boolean mIsSaveColor;
    private Boolean mIsSaveDepth;

    ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            mBind = true;
        }

        @Override
        public void onUnbind(String reason) {
            mBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vision_sample);

        // init content view
        mBindSwitch = (Switch) findViewById(R.id.bind);
        mPreviewSwitch = (Switch) findViewById(R.id.preview);
        mTransferSwitch = (Switch) findViewById(R.id.transfer);
        mSaveColorSwitch = (Switch) findViewById(R.id.saveColor);
        mSaveDepthSwitch = (Switch) findViewById(R.id.saveDepth);

        mBindSwitch.setOnCheckedChangeListener(this);
        mPreviewSwitch.setOnCheckedChangeListener(this);
        mTransferSwitch.setOnCheckedChangeListener(this);
        mSaveColorSwitch.setOnCheckedChangeListener(this);
        mSaveDepthSwitch.setOnCheckedChangeListener(this);

        mColorSurfaceView = (SurfaceView) findViewById(R.id.colorSurface);
        mDepthSurfaceView = (SurfaceView) findViewById(R.id.depthSurface);

        mColorImageView = (ImageView) findViewById(R.id.colorImage);
        mDepthImageView = (ImageView) findViewById(R.id.depthImage);

        // get Vision SDK instance
        mVision = Vision.getInstance();
        mIsSaveColor = mSaveColorSwitch.isChecked();
        mIsSaveDepth = mSaveDepthSwitch.isChecked();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mBindSwitch.setChecked(false);
        mPreviewSwitch.setChecked(false);
        mTransferSwitch.setChecked(false);
        mSaveColorSwitch.setChecked(false);
        mSaveDepthSwitch.setChecked(false);
    }

    /**
     * Start preview color and depth image
     */
    private synchronized void startPreview() {
        // 1. Get activated stream info from Vision Service.
        //    Streams are pre-config.
        StreamInfo[] infos = mVision.getActivatedStreamInfo();
        for(StreamInfo info : infos) {
            // Adjust image ratio for display
            float ratio = (float)info.getWidth()/info.getHeight();
            ViewGroup.LayoutParams layout;
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    // Adjust color surface view
                    mColorSurfaceView.getHolder().setFixedSize(info.getWidth(), info.getHeight());
                    layout = mColorSurfaceView.getLayoutParams();
                    layout.width = (int) (mColorSurfaceView.getHeight() * ratio);
                    mColorSurfaceView.setLayoutParams(layout);

                    // preview color stream
                    mVision.startPreview(StreamType.COLOR, mColorSurfaceView.getHolder().getSurface());
                    break;
                case StreamType.DEPTH:
                    // Adjust depth surface view
                    mDepthSurfaceView.getHolder().setFixedSize(info.getWidth(), info.getHeight());
                    layout = mDepthSurfaceView.getLayoutParams();
                    layout.width = (int) (mDepthSurfaceView.getHeight() * ratio);
                    mDepthSurfaceView.setLayoutParams(layout);

                    // preview depth stream
                    mVision.startPreview(StreamType.DEPTH, mDepthSurfaceView.getHolder().getSurface());
                    break;
            }
        }
    }

    /**
     * Stop preview
     */
    private synchronized void stopPreview() {
        StreamInfo[] infos = mVision.getActivatedStreamInfo();
        for(StreamInfo info : infos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    // Stop color preview
                    mVision.stopPreview(StreamType.COLOR);
                    break;
                case StreamType.DEPTH:
                    // Stop depth preview
                    mVision.stopPreview(StreamType.DEPTH);
                    break;
            }
        }
    }

    /**
     * FrameListener instance for get raw image data form vision service
     */
    Vision.FrameListener mFrameListener = new Vision.FrameListener() {
        @Override
        public void onNewFrame(int streamType, Frame frame) {
            final Bitmap mColorBitmap = Bitmap.createBitmap(colorInfo.getWidth(), colorInfo.getHeight(), Bitmap.Config.ARGB_8888);
            final Bitmap mDepthBitmap = Bitmap.createBitmap(depthInfo.getWidth(), depthInfo.getHeight(), Bitmap.Config.RGB_565);
            Runnable runnable = null;
            switch (streamType) {
                case StreamType.COLOR:
                    // draw color image to bitmap and display
                    mColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            mColorImageView.setImageBitmap(mColorBitmap);
                        }
                    };

                    // save image in a new thread
                    if(mIsSaveColor) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                File f = new File(VisionSampleActivity.this.getExternalFilesDir(null).getAbsolutePath() + "/C" + System.currentTimeMillis() + ".png");
                                try {
                                    FileOutputStream fOut = new FileOutputStream(f);
                                    mColorBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                                    fOut.flush();
                                    fOut.close();
                                } catch(IOException e) {
                                    Log.e("VisionSample", "File not found!", e);
                                }
                            }
                        }).start();
                    }
                    break;
                case StreamType.DEPTH:
                    // draw depth image to bitmap and display
                    mDepthBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            mDepthImageView.setImageBitmap(mDepthBitmap);
                        }
                    };

                    // save image in a new thread
                    if(mIsSaveDepth) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                File f = new File(VisionSampleActivity.this.getExternalFilesDir(null).getAbsolutePath() + "/D" + System.currentTimeMillis() + ".png");
                                try {
                                    FileOutputStream fOut = new FileOutputStream(f);
                                    Bitmap mDepthGreyBitmap = depth2Grey(mDepthBitmap);
                                    mDepthGreyBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                                    fOut.flush();
                                    fOut.close();
                                } catch(IOException e) {
                                    Log.e("VisionSample", "File not found!", e);
                                }
                            }
                        }).start();
                    }
                    break;
            }

            if (runnable != null) {
                runOnUiThread(runnable);
            }
        }
    };

    /**
     * Convert depth image into grey image
     * @param img the depth image in RGB_565 format
     * @return    the GREY image in RGB_565 format
     */
    private Bitmap depth2Grey(Bitmap img) {
        int width = img.getWidth();
        int height = img.getHeight();

        int []pixels = new int[width * height];

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for(int i = 0; i < height; i++)  {
            for(int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey  & 0x00FF0000 ) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                //grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                grey  = (red*38 + green*75 + blue*15) >> 7;
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    /**
     * Start transfer raw image data form VisionService to giving FrameListener
     */
    private synchronized void startImageTransfer() {
        StreamInfo[] infos = mVision.getActivatedStreamInfo();
        for(StreamInfo info : infos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    colorInfo = info;
                    mVision.startListenFrame(StreamType.COLOR, mFrameListener);
                    break;
                case StreamType.DEPTH:
                    depthInfo = info;
                    mVision.startListenFrame(StreamType.DEPTH, mFrameListener);
                    break;
            }
        }

    }

    /**
     * Stop transfer raw image data
     */
    private synchronized void stopImageTransfer() {
        mVision.stopListenFrame(StreamType.COLOR);
        mVision.stopListenFrame(StreamType.DEPTH);
    }

    /**
     * Buttons
     * @param buttonView
     * @param isChecked
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.bind:
                if (isChecked) {
                    if(!mVision.bindService(this, mBindStateListener)) {
                        mBindSwitch.setChecked(false);
                        Toast.makeText(this, "Bind service failed", Toast.LENGTH_SHORT).show();
                    }
                    Toast.makeText(this, "Bind service success", Toast.LENGTH_SHORT).show();
                } else {
                    mPreviewSwitch.setChecked(false);
                    mTransferSwitch.setChecked(false);
                    mSaveColorSwitch.setChecked(false);
                    mSaveDepthSwitch.setChecked(false);
                    mVision.unbindService();
                    mBind = false;
                    Toast.makeText(this, "Unbind service", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.preview:
                if (isChecked) {
                    if (!mBind) {
                        mPreviewSwitch.setChecked(false);
                        Toast.makeText(this, "Need to bind service first", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    startPreview();
                } else {
                    if (mBind) {
                        stopPreview();
                    }
                }
                break;
            case R.id.transfer:
                if (isChecked) {
                    if (!mBind) {
                        mTransferSwitch.setChecked(false);
                        Toast.makeText(this, "Need to bind service first", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    startImageTransfer();
                } else {
                    if (mBind) {
                        stopImageTransfer();
                    }
                }
                break;
            case R.id.saveColor:
                if (isChecked) {
                    if (!mBind) {
                        mSaveColorSwitch.setChecked(false);
                        Toast.makeText(this, "Need to bind service first", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    mIsSaveColor = true;
                } else {
                    if (mBind) {
                        mIsSaveColor = false;
                    }
                }
                break;
            case R.id.saveDepth:
                if (isChecked) {
                    if (!mBind) {
                        mSaveDepthSwitch.setChecked(false);
                        Toast.makeText(this, "Need to bind service first", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    mIsSaveDepth = true;
                } else {
                    if (mBind) {
                        mIsSaveDepth = false;
                    }
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBind){
            mVision.unbindService();
            StreamInfo[] infos = mVision.getActivatedStreamInfo();
            for(StreamInfo info : infos) {
                switch (info.getStreamType()) {
                    case StreamType.COLOR:
                        mVision.stopListenFrame(StreamType.COLOR);
                        break;
                    case StreamType.DEPTH:
                        mVision.stopListenFrame(StreamType.DEPTH);
                        break;
                }
            }
        }
    }
}
