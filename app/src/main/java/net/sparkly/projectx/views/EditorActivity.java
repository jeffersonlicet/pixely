package net.sparkly.projectx.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.Orientation;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import net.sparkly.projectx.R;
import net.sparkly.projectx.adapters.FilterListAdapter;
import net.sparkly.projectx.models.FilterItem;
import net.sparkly.projectx.utils.StorageManager;

import org.wysaid.nativePort.CGEImageHandler;
import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.view.ImageGLSurfaceView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EditorActivity extends AppCompatActivity
{
    private static final String TAG = EditorActivity.class.getSimpleName();
    private FilterListAdapter filtersAdapter;
    private String photoUri;
    private int seekBarWait = 5000;
    private float filterIntensity = 1;
    private String filterConfig = "";
    private int selectedIndex;
    private List<FilterItem> filters = new ArrayList<>();
    private StorageManager storageManager;
    private List<Integer> selectedFilter;
    private Handler hideSeekBarHandler = new Handler();
    private boolean isFilteringEnabled;

    @BindView(R.id.surfaceView)
    ImageGLSurfaceView surfaceView;

    @BindView(R.id.filterSelector)
    DiscreteScrollView filterSelector;

    @BindView(R.id.filterIndicator)
    TextView filterIndicator;

    @BindView(R.id.intensityIndicator)
    AppCompatSeekBar intensityIndicator;

    @BindView(R.id.progressWorkingIndicator)
    ProgressBar progressWorkingIndicator;

    private Runnable runHideSeek = new Runnable()
    {
        @Override
        public void run()
        {
            intensityIndicator.animate().setStartDelay(0).alpha(0).setDuration(500).setListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    super.onAnimationEnd(animation);
                    intensityIndicator.setVisibility(View.GONE);
                }
            }).start();
        }
    };

    private CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback()
    {
        @Override
        public Bitmap loadImage(String name, Object arg)
        {
            Log.i(TAG, "Loading file: " + name);

            AssetManager am = getAssets();
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
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        try
        {
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        setContentView(R.layout.activity_editor);
        ButterKnife.bind(this);

        try
        {
            storageManager = new StorageManager(this, true);
            CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        buildFilters();

        filtersAdapter = new FilterListAdapter(this, filters, filterSelector, selectedFilter, new FilterListAdapter.FilterItemClickListener()
        {
            @Override
            public void onClickSelectedItemListener(int selected)
            {
                try
                {
                    //When click selected

                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        filterSelector.setOrientation(Orientation.HORIZONTAL);
        filterSelector.setAdapter(filtersAdapter);
        filterSelector.setSlideOnFling(true);
        filterSelector.setSlideOnFlingThreshold(1000);
        filterSelector.setItemTransitionTimeMillis(80);

        filterSelector.setItemTransformer(new ScaleTransformer.Builder()
                .setMaxScale(1.00f)
                .setMinScale(0.8f)
                .build());

        filterSelector.post(new Runnable()
        {
            @Override
            public void run()
            {
                filterSelector.smoothScrollToPosition(0);
            }
        });
        filterSelector.addOnItemChangedListener(new onFilterChangedListener());

        intensityIndicator.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                if (!isFilteringEnabled) return;

                final float value = i / 100f;
                filterIntensity = value;

                if (!filterConfig.isEmpty())
                {
                    surfaceView.queueEvent(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            CGEImageHandler handler = surfaceView.getImageHandler();
                            handler.setFilterIntensity(value);
                            handler.processFilters();
                            surfaceView.requestRender();
                        }
                    });
                }

                restartHideSeekBar();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                restartHideSeekBar();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                restartHideSeekBar();
            }
        });


        try {
            photoUri = getIntent().getStringExtra("photoUri");

            final FutureTarget<Bitmap> futureTarget =
                    Glide.with(this)
                            .asBitmap()
                            .load(new File(photoUri))
                            .submit();

            surfaceView.setDisplayMode(ImageGLSurfaceView.DisplayMode.DISPLAY_SCALE_TO_FILL);

            surfaceView.setSurfaceCreatedCallback(new ImageGLSurfaceView.OnSurfaceCreatedCallback() {
                @Override
                public void surfaceCreated() {

                    try
                    {
                        surfaceView.setImageBitmap(futureTarget.get());
                        surfaceView.setFilterWithConfig("");
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressWorkingIndicator.setVisibility(View.GONE);

                            }
                        });
                        isFilteringEnabled = true;

                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    } catch (ExecutionException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void restartHideSeekBar()
    {
        hideSeekBarHandler.removeCallbacks(runHideSeek);
        hideSeekBarHandler.postDelayed(runHideSeek, seekBarWait);
    }

    private void buildFilters()
    {
        try
        {
            int nFilters = getResources().getInteger(R.integer.nFilters);
            Log.d(TAG, "Shutter inside id: " + R.drawable.camera_shutter_inside);
            filters.add(new FilterItem(0, getString(R.string.nameFilter0), "", 1, R.drawable.camera_shutter_inside));


            for (int i = 1; i < nFilters; i++)
            {
                Log.d(TAG, "Creating filter");
                String name = getString(getResources().getIdentifier("nameFilter" + i, "string", getPackageName()));
                String params = getString(getResources().getIdentifier("configFilter" + i, "string", getPackageName()));
                float intensity = Float.parseFloat(getString(getResources().getIdentifier("intensityFilter" + i, "string", getPackageName())));
                String thumbName = getString(getResources().getIdentifier("thumbFilter" + i, "string", getPackageName()));
                filters.add(new FilterItem(i, name, params, intensity, getResources().getIdentifier(thumbName, "drawable", getPackageName())));
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private class onFilterChangedListener implements DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder>
    {
        @Override
        public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition)
        {
            if(!isFilteringEnabled) return;

            final FilterItem filter = filters.get(adapterPosition);
            filterIndicator.setText(filter.getName());
            progressWorkingIndicator.setVisibility(View.VISIBLE);
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    surfaceView.queueEvent(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            CGEImageHandler handler = surfaceView.getImageHandler();
                            handler.setFilterIntensity(filter.getIntensity());
                            handler.setFilterWithConfig(filter.getParams());

                            //handler.revertImage();
                            handler.processFilters();

                            //surfaceView.requestRender();
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    progressWorkingIndicator.setVisibility(View.GONE);
                                }
                            });
                        }
                    });

                }
            }).start();

            intensityIndicator.setProgress((int) (filter.getIntensity() * 100f));
            Log.d(TAG, " New intensity:" + (int) (filter.getIntensity() * 100f));

            filterConfig = filter.getParams();
            filterIntensity = filter.getIntensity();

            filterIndicator.setAlpha(0f);
            filterIndicator.setScaleX(0f);
            filterIndicator.setScaleY(0f);

            filterIndicator.setVisibility(View.VISIBLE);
            filterIndicator.animate().setListener(null).cancel();
            filterIndicator.animate().setStartDelay(0).setDuration(350).alpha(1).scaleX(1).scaleY(1).setListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    super.onAnimationEnd(animation);
                    filterIndicator.animate().setStartDelay(1500).alpha(0f).scaleX(0f).scaleY(0f).setDuration(200).setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            super.onAnimationEnd(animation);
                            filterIndicator.setVisibility(View.GONE);
                        }
                    }).start();
                }
            }).start();

            filtersAdapter.notifyChangedItem(adapterPosition);
            selectedIndex = adapterPosition;
        }
    }
}
