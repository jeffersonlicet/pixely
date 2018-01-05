package net.sparkly.pixely.processors;


import android.content.Context;

import net.sparkly.pixely.widgets.CameraView;

public class CameraProcessor extends FilterProcessor {

    private CameraView surface;

    public CameraProcessor(Context context, CameraView surface) {
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
