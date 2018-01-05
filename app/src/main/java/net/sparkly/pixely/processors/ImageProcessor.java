package net.sparkly.pixely.processors;


import android.content.Context;

import org.wysaid.view.ImageGLSurfaceView;

public class ImageProcessor extends FilterProcessor {

    private ImageGLSurfaceView surface;

    public ImageProcessor(Context context, ImageGLSurfaceView surface) {
        super(context);
        this.surface = surface;
    }

    public void setIntensity(float value, IntensityCallback callback) {
        super.setIntensity(surface, value, callback);
    }

    public void setFilter(final int index, final FilteringCallback callback) {
        super.setFilter(surface, index, callback);
    }

    public void setFilter(final int index, final float intensity, final FilteringCallback callback) {
        super.setFilter(surface, index, intensity, callback);
    }
}
