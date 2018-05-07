package com.segway.robot.host.coreservice.vision.sample;

import android.graphics.Bitmap;

/**
 *
 * @author jacob
 * @date 5/7/18
 */

public interface IImageState {
    void updateImage(int Type,Bitmap bitmap);
}
