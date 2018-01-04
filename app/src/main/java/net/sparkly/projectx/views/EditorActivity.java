package net.sparkly.projectx.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.Orientation;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import net.sparkly.projectx.FilterManager;
import net.sparkly.projectx.R;
import net.sparkly.projectx.adapters.FilterListAdapter;
import net.sparkly.projectx.models.FilterItem;
import net.sparkly.projectx.utils.StorageManager;

import org.wysaid.view.ImageGLSurfaceView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EditorActivity extends AppActivity
{
    private static final String TAG = EditorActivity.class.getSimpleName();

    private int seekBarWait = 5000;
    private int selectedIndex;
    private float filterIntensity = 1;
    private boolean isFilteringEnabled;

    private String photoUri;
    private String filterConfig = "";

    private FilterManager filterManager;

    private FilterListAdapter filtersAdapter;
    private List<FilterItem> filters = new ArrayList<>();
    private List<FilterItem> filtersApplied = new ArrayList<>();

    private StorageManager storageManager;
    private List<Integer> selectedFilter;
    private Handler hideSeekBarHandler = new Handler();
    private boolean saving;

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

    @BindView(R.id.buttonsContainer)
    LinearLayout buttonsContainer;

    @BindView(R.id.camera_shutter_outside)
    ImageView camera_shutter_outside;

    @OnClick({R.id.actionApply, R.id.actionCancel})
    public void onClickManager(View view)
    {
        switch (view.getId())
        {
            case R.id.actionApply:
                performSave();
                break;
            case R.id.actionCancel:
                finish();
                break;
        }
    }

    private void performSave() {
        if(saving) return;

        saving = true;

        filterSelector.setVisibility(View.GONE);
        buttonsContainer.setVisibility(View.GONE);

        camera_shutter_outside.setImageDrawable(getResources().getDrawable(R.drawable.camera_shutter_outside_dashed));
        RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(3000);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setRepeatCount(Animation.INFINITE);
        camera_shutter_outside.startAnimation(rotate);

        //progressWorkingIndicator.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                int quality = 1;

                if (getBoolean("rearQualityChanged"))
                    quality = getInteger("rearImageQuality");

                int size = 0;

                switch (quality)
                {
                    case 0:
                        size = 600;
                        break;
                    case 1:
                        size = 1024;
                        break;
                    case 2:
                        size = 2048;
                        break;
                }

                final FutureTarget<Bitmap> futureTarget =
                        Glide.with(getApplication())
                                .asBitmap()
                                .load(new File(photoUri))
                                .submit(size, size);

                 try {

                            surfaceView.setImageBitmap(futureTarget.get());

                            if(selectedIndex != 0)
                            {
                                final FilterItem filter = filters.get(selectedIndex);

                                filterManager.updateFilter(surfaceView, filter.getParams(), filter.getIntensity(), new FilterManager.FilteringCallback() {
                                    @Override
                                    public void filterChanged() {
                                        getAndSaveBitmap();
                                    }
                                });
                            } else getAndSaveBitmap();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }


            }
        }).start();


    }

    private void getAndSaveBitmap()
    {
        surfaceView.getResultBitmap(new ImageGLSurfaceView.QueryResultBitmapCallback() {
            @Override
            public void get(final Bitmap bmp) {

                @SuppressLint("SimpleDateFormat") String timeStamp =
                        new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                final String name = File.separator + "IMG_" + timeStamp + ".jpg";
                storageManager.createFile(name, bmp);
                bmp.recycle();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        MediaScannerConnection.scanFile(getApplicationContext(),
                                new String[]{storageManager.getFile(name).getPath()}, null,
                                new MediaScannerConnection.OnScanCompletedListener()
                                {
                                    @Override
                                    public void onScanCompleted(String path, Uri uri)
                                    {
                                        Log.i(TAG, "Scanned " + path);
                                    }
                                });

                        Intent newActivity = new Intent(getApplicationContext(), PhotoActivity.class);
                        newActivity.putExtra("photoUri", storageManager.getFile(name).getAbsolutePath());
                        startActivity(newActivity);
                        finish();
                    }
                });
            }
        });
    }

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
            filterManager = new FilterManager(this);
            filterManager.initLibrary();
            filters = filterManager.getFilters();

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        photoUri = getIntent().getStringExtra("photoUri");

        filtersAdapter = new FilterListAdapter(this, filterManager.getFilters(), filterSelector, selectedFilter, new FilterListAdapter.FilterItemClickListener()
        {
            @Override
            public void onClickSelectedItemListener(int selected)
            {
                /*Log.d(TAG, "Clicked selected");
                progressWorkingIndicator.setVisibility(View.VISIBLE);

                FilterItem back = filters.get(selected);
                filtersApplied.add(new FilterItem(back.getId(), back.getName(), back.getParams(), filterIntensity, back.getThumbnail()));
                surfaceView.getResultBitmap(new ImageGLSurfaceView.QueryResultBitmapCallback() {
                    @Override
                    public void get(final Bitmap bmp) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressWorkingIndicator.setVisibility(View.GONE);
                                surfaceView.setImageBitmap(bmp);
                                surfaceView.setFilterWithConfig("");

                            }
                        });
                    }
                });*/
            }
        });

        filterSelector.setOrientation(Orientation.HORIZONTAL);
        filterSelector.setAdapter(filtersAdapter);
        filterSelector.setSlideOnFling(true);
        filterSelector.setSlideOnFlingThreshold(1000);
        filterSelector.setItemTransitionTimeMillis(80);

        filterSelector.setItemTransformer(new ScaleTransformer.Builder()
                .setMaxScale(1.00f)
                .setMinScale(0.80f)
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
                if (!isFilteringEnabled) return;

                final float value = seekBar.getProgress() / 100f;

                filterIntensity = value;
                progressWorkingIndicator.setVisibility(View.VISIBLE);

                if (!filterConfig.isEmpty())
                {
                    filterManager.updateIntensity(surfaceView, value, new FilterManager.IntensityCallback() {
                        @Override
                        public void onIntensityChanged() {

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

                restartHideSeekBar();
            }
        });

        try
        {

            final FutureTarget<Bitmap> futureTarget =
                    Glide.with(this)
                            .asBitmap()
                            .load(new File(photoUri))
                            .submit(500, 500);

            surfaceView.setDisplayMode(ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FIT);
            surfaceView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if(saving) return;

                    if (!filterConfig.isEmpty())
                    {
                        intensityIndicator.setVisibility(View.VISIBLE);
                        intensityIndicator.animate().alpha(1).setDuration(300).setListener(new AnimatorListenerAdapter()
                        {
                            @Override
                            public void onAnimationEnd(Animator animation)
                            {
                                restartHideSeekBar();
                                super.onAnimationEnd(animation);
                            }


                        }).start();

                    }
                }
            });

            surfaceView.setSurfaceCreatedCallback(new ImageGLSurfaceView.OnSurfaceCreatedCallback()
            {
                @Override
                public void surfaceCreated()
                {
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
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void restartHideSeekBar()
    {
        hideSeekBarHandler.removeCallbacks(runHideSeek);
        hideSeekBarHandler.postDelayed(runHideSeek, seekBarWait);
    }

    private class onFilterChangedListener implements DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder>
    {
        @Override
        public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition)
        {
            if (!isFilteringEnabled) return;

            final FilterItem filter = filters.get(adapterPosition);
            filterIndicator.setText(filter.getName());
            progressWorkingIndicator.setVisibility(View.VISIBLE);
            filterManager.updateFilter(surfaceView, filter.getParams(), filter.getIntensity(), new FilterManager.FilteringCallback() {
                @Override
                public void filterChanged() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressWorkingIndicator.setVisibility(View.GONE);
                        }
                    });
                }
            });

             intensityIndicator.setProgress((int) (filter.getIntensity() * 100f));

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
