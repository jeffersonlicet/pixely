package net.sparkly.pixely.processors;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import net.sparkly.pixely.R;
import net.sparkly.pixely.models.FilterItem;
import net.sparkly.pixely.widgets.CameraView;

import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.view.ImageGLSurfaceView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FilterProcessor {

    private Context context;
    private List<FilterItem> filters = new ArrayList<>();
    private static final String TAG = FilterProcessor.class.getSimpleName();

    public FilterProcessor(Context context) {
        this.context = context;
    }

    void setIntensity(final ImageGLSurfaceView surface, final float value, final IntensityCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                surface.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        surface.getImageHandler().setFilterIntensity(value);
                        surface.requestRender();

                        if(callback != null)
                            callback.onIntensityChanged();
                    }
                });
            }
        }).start();
    }

    void setFilter(final ImageGLSurfaceView surface,final int index,final FilteringCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                surface.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        surface.getImageHandler().setFilterWithConfig(getFilters().get(index).getParams(), true, false);
                        surface.getImageHandler().setFilterIntensity(getFilters().get(index).getIntensity(), true);
                        surface.getImageHandler().revertImage();
                        surface.getImageHandler().processFilters();
                        surface.requestRender();

                        if(callback != null)
                            callback.filterChanged();
                    }
                });
            }
        }).start();
    }

    void setFilter(final ImageGLSurfaceView surface, final int index, final float intensity, final FilteringCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                surface.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        surface.getImageHandler().setFilterWithConfig(getFilters().get(index).getParams(), true, false);
                        surface.getImageHandler().setFilterIntensity(intensity, true);
                        surface.getImageHandler().revertImage();
                        surface.getImageHandler().processFilters();
                        surface.requestRender();

                        if(callback != null)
                            callback.filterChanged();
                    }
                });
            }
        }).start();
    }

    void setIntensity(final CameraView surface, final float value, final IntensityCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                surface.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        surface.setFilterIntensity(value);
                        surface.requestRender();

                        if(callback != null)
                            callback.onIntensityChanged();
                    }
                });
            }
        }).start();
    }

    void setFilter(final CameraView surface, final int index, final FilteringCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                surface.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        surface.setFilterWithConfig(getFilters().get(index).getParams());
                        surface.setFilterIntensity(getFilters().get(index).getIntensity());

                        if(callback != null)
                            callback.filterChanged();
                    }
                });
            }
        }).start();
    }

    void setFilter(final CameraView surface, final int index, final float intensity, final FilteringCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                surface.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        surface.setFilterWithConfig(getFilters().get(index).getParams());
                        surface.setFilterIntensity(intensity);

                        if(callback != null)
                            callback.filterChanged();
                    }
                });
            }
        }).start();
    }

    public void initLibrary() {
        CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);
        buildFilters();
    }

    public List<FilterItem> getFilters() {
        return filters;
    }


    public interface FilteringCallback {
        void filterChanged();
    }

    public interface IntensityCallback {
        void onIntensityChanged();
    }

    private void buildFilters() {
        try {
            int nFilters = context.getResources().getInteger(R.integer.nFilters);
            Log.d(TAG, "Shutter inside id: " + R.drawable.camera_shutter_inside);
            filters.add(new FilterItem(0, context.getString(R.string.nameFilter0), "", 1, R.drawable.camera_shutter_inside));


            for (int i = 1; i < nFilters; i++) {
                Log.d(TAG, "Creating filter");
                String name = context.getString(context.getResources().getIdentifier("nameFilter" + i, "string", context.getPackageName()));
                String params = context.getString(context.getResources().getIdentifier("configFilter" + i, "string", context.getPackageName()));
                float intensity = Float.parseFloat(context.getString(context.getResources().getIdentifier("intensityFilter" + i, "string", context.getPackageName())));
                String thumbName = context.getString(context.getResources().getIdentifier("thumbFilter" + i, "string", context.getPackageName()));
                filters.add(new FilterItem(i, name, params, intensity, context.getResources().getIdentifier(thumbName, "drawable", context.getPackageName())));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback() {
        @Override
        public Bitmap loadImage(String name, Object arg) {
            Log.i(TAG, "Loading file: " + name);

            AssetManager am = context.getAssets();
            InputStream is;
            try {
                is = am.open(name);
            } catch (IOException e) {
                Log.e(TAG, "Can not open file " + name);
                e.printStackTrace();
                return null;
            }

            return BitmapFactory.decodeStream(is);
        }

        @Override
        public void loadImageOK(Bitmap bmp, Object arg) {
            bmp.recycle();
        }
    };

}
