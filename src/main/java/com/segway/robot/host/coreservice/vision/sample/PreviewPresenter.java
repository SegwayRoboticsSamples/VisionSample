package com.segway.robot.host.coreservice.vision.sample;

import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.segway.robot.sdk.vision.Vision;
import com.segway.robot.sdk.vision.stream.StreamInfo;
import com.segway.robot.sdk.vision.stream.StreamType;

/**
 *
 * @author jacob
 * @date 5/7/18
 */

public class PreviewPresenter{
    private static final String TAG = "PreviewPresenter";

    private Vision mVision;
    private SurfaceView mColorSurfaceView;
    private SurfaceView mDepthSurfaceView;


    public PreviewPresenter(Vision mVision, SurfaceView mColorSurfaceView, SurfaceView mDepthSurfaceView) {
        this.mVision = mVision;
        this.mColorSurfaceView = mColorSurfaceView;
        this.mDepthSurfaceView = mDepthSurfaceView;
    }

    public synchronized void start() {
        Log.d(TAG, "start() called");

        // Get activated stream info from Vision Service. Streams are pre-config.
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();
        for (StreamInfo info : streamInfos) {
            // Adjust image ratio for display
            float ratio = (float) info.getWidth() / info.getHeight();
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

    public synchronized void stop() {
        Log.d(TAG, "stop() called");
        StreamInfo[] streamInfos = mVision.getActivatedStreamInfo();

        for (StreamInfo info : streamInfos) {
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

}
