package net.sparkly.projectx;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;

import net.sparkly.projectx.models.FilterItem;

import org.wysaid.nativePort.CGEImageHandler;
import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.view.ImageGLSurfaceView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FilterManager {

    private Context context;
    private List<FilterItem> filters = new ArrayList<>();
    private static final String TAG = FilterManager.class.getSimpleName();

    public FilterManager(Context context)
    {
        this.context = context;
    }

    public void updateIntensity(final ImageGLSurfaceView frame, final float value, final IntensityCallback callback) {

        frame.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                CGEImageHandler handler = frame.getImageHandler();
                handler.setFilterIntensity(value);
                frame.requestRender();
                callback.onIntensityChanged();
            }
        });
    }

    public void updateFilter(final ImageGLSurfaceView frame, final String params, final float value, final FilteringCallback callback)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                frame.queueEvent(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        CGEImageHandler handler = frame.getImageHandler();
                        handler.setFilterWithConfig(params, true, false);
                        handler.setFilterIntensity(value , true);

                        handler.revertImage();
                        handler.processFilters();

                        frame.requestRender();
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

    public List<FilterItem> getFilters()
    {
        return filters;
    }

    public interface FilteringCallback
    {
        void filterChanged();
    }

    public interface IntensityCallback
    {
        void onIntensityChanged();
    }

    private void buildFilters()
    {
        try
        {
            int nFilters = context.getResources().getInteger(R.integer.nFilters);
            Log.d(TAG, "Shutter inside id: " + R.drawable.camera_shutter_inside);
            filters.add(new FilterItem(0, context.getString(R.string.nameFilter0), "", 1, R.drawable.camera_shutter_inside));


            for (int i = 1; i < nFilters; i++)
            {
                Log.d(TAG, "Creating filter");
                String name = context.getString(context.getResources().getIdentifier("nameFilter" + i, "string", context.getPackageName()));
                String params = context.getString(context.getResources().getIdentifier("configFilter" + i, "string", context.getPackageName()));
                float intensity = Float.parseFloat(context.getString(context.getResources().getIdentifier("intensityFilter" + i, "string", context.getPackageName())));
                String thumbName = context.getString(context.getResources().getIdentifier("thumbFilter" + i, "string", context.getPackageName()));
                filters.add(new FilterItem(i, name, params, intensity, context.getResources().getIdentifier(thumbName, "drawable", context.getPackageName())));
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    private CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback()
    {
        @Override
        public Bitmap loadImage(String name, Object arg)
        {
            Log.i(TAG, "Loading file: " + name);

            AssetManager am = context.getAssets();
            InputStream is;
            try
            {
                is = am.open(name);
            } catch (IOException e)
            {
                Log.e(TAG, "Can not open file " + name);
                e.printStackTrace();
                return null;
            }

            return BitmapFactory.decodeStream(is);
        }

        @Override
        public void loadImageOK(Bitmap bmp, Object arg)
        {
            bmp.recycle();
        }
    };
}
