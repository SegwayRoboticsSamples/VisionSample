package com.segway.robot.host.coreservice.vision.sample;

import android.graphics.Bitmap;
import android.util.Log;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.frame.Frame;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

/**
 * @author jacob
 * @date 5/7/18
 */

public class TransferPresenter {

    private static final String TAG = "TransferPresenter";

    private Vision mVision;
    private IImageState mIImageState;

    private StreamInfo mColorInfo;
    private StreamInfo mDepthInfo;

    public TransferPresenter(Vision mVision, IImageState mIImageState) {
        this.mVision = mVision;
        this.mIImageState = mIImageState;
    }

    public synchronized void start() {
        Log.d(TAG, "start() called");
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            switch (info.getStreamType()) {
                case StreamType.COLOR:
                    mColorInfo = info;
                    mVision.startListenFrame(StreamType.COLOR, mFrameListener);
                    break;
                case StreamType.DEPTH:
                    mDepthInfo = info;
                    mVision.startListenFrame(StreamType.DEPTH, mFrameListener);
                    break;
            }
        }
    }

    public synchronized void stop() {
        Log.d(TAG, "stop() called");
        mVision.stopListenFrame(StreamType.COLOR);
        mVision.stopListenFrame(StreamType.DEPTH);
    }

    /**
     * FrameListener instance for get raw image data form vision service
     */
    Vision.FrameListener mFrameListener = new Vision.FrameListener() {

        @Override
        public void onNewFrame(int streamType, Frame frame) {
            Bitmap mColorBitmap = Bitmap.createBitmap(mColorInfo.getWidth(), mColorInfo.getHeight(), Bitmap.Config.ARGB_8888);
            Bitmap mDepthBitmap = Bitmap.createBitmap(mDepthInfo.getWidth(), mDepthInfo.getHeight(), Bitmap.Config.RGB_565);

            switch (streamType) {
                case StreamType.COLOR:
                    // draw color image to bitmap and display
                    mColorBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    mIImageState.updateImage(StreamType.COLOR,mColorBitmap);
                    break;
                case StreamType.DEPTH:
                    // draw depth image to bitmap and display
                    mDepthBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
                    mIImageState.updateImage(StreamType.DEPTH,mDepthBitmap);
                    break;
            }
        }
    };
}
