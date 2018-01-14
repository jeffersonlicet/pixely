package net.sparkly.pixely.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.Orientation;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import net.sparkly.pixely.processors.CameraProcessor;
import net.sparkly.pixely.GlideApp;
import net.sparkly.pixely.R;
import net.sparkly.pixely.adapters.FilterListAdapter;
import net.sparkly.pixely.adapters.ModeListAdapter;
import net.sparkly.pixely.adapters.PhotoFeedAdapter;
import net.sparkly.pixely.models.FilterItem;
import net.sparkly.pixely.models.PhotoItem;
import net.sparkly.pixely.models.SingleModeItem;
import net.sparkly.pixely.utils.StorageManager;
import net.sparkly.pixely.widgets.CameraSurfaceView;
import net.sparkly.pixely.widgets.CameraView;
import net.sparkly.pixely.widgets.FocusMarkerLayout;

import org.wysaid.camera.CameraInstance;
import org.wysaid.nativePort.CGENativeLibrary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

public class CameraActivity extends BaseActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final int BACK_CAMERA = 0;
    private static final int FRONTAL_CAMERA = 1;
    private static final int FLASH_OFF = 0;
    private static final int FLASH_ON = 1;
    private static final int FLASH_AUTO = 2;

    private int actualFlash;
    private int actualCamera;
    private final int seekBarWait = 5000;
    private int frontalPhotoQuality;
    private int rearPhotoQuality;
    private float filterIntensity = 1;
    private String filterConfig = "";
    private int selectedIndex;
    private int nFocusIntents;

    private boolean canTakePicture;
    private boolean isFilteringEnabled;
    private boolean isModeSelectorEnabled;
    private boolean pendingToggleFlash;
    private boolean pendingToggleCamera;
    private boolean wasFocused;
    private boolean isPhotoFeedOpened;

    private ModeListAdapter adapter;
    private FilterListAdapter filtersAdapter;
    private PhotoFeedAdapter photoFeedAdapter;
    private Handler hideSeekBarHandler = new Handler();
    private CameraProcessor cameraProcessor;

    private List<SingleModeItem> modes = new ArrayList<>();
    private List<FilterItem> filters = new ArrayList<>();
    private List<Integer> selectedCameraModes = new ArrayList<>();
    private List<PhotoItem> previewPhotos = new ArrayList<>();
    private List<Integer> selectedFilter;

    private Activity mActivity;
    private GestureDetectorCompat mDetector;
    private FocusMarkerLayout focusMarkerLayout;
    private StorageManager storageManager;
    private StorageManager privateStorageManager;

    @BindView(R.id.focusFailedContainer)
    FrameLayout focusFailedContainer;

    @BindView(R.id.surfaceView)
    CameraView surfaceView;

    @BindView(R.id.relativeWrapper)
    RelativeLayout relativeWrapper;

    @BindView(R.id.modeSelector)
    DiscreteScrollView modeSelector;

    @BindView(R.id.filterSelector)
    DiscreteScrollView filterSelector;

    @BindView(R.id.photoFeed)
    DiscreteScrollView photoFeed;

    @BindView(R.id.toggleFlash)
    ImageButton toggleFlashIcon;

    @BindView(R.id.toggleCamera)
    ImageButton toggleCameraIcon;

    @BindView(R.id.goSettings)
    ImageButton goSettings;

    @BindView(R.id.cameraShutter)
    RelativeLayout cameraShutter;

    @BindView(R.id.photoThumbnail)
    ImageView photoThumbnail;

    @BindView(R.id.photoThumbnailBorder)
    ImageView photoThumbnailBorder;

    @BindView(R.id.filterIndicator)
    TextView filterIndicator;

    @BindView(R.id.intensityIndicator)
    AppCompatSeekBar intensityIndicator;

    @BindView(R.id.camera_shutter_outside)
    ImageView camera_shutter_outside;

    @BindView(R.id.photoFeedWrapper)
    RelativeLayout photoFeedWrapper;

    @OnClick({R.id.toggleFlash, R.id.toggleCamera, R.id.photoThumbnail, R.id.goSettings})
    public void clickManager(View view) {
        switch (view.getId()) {
            case R.id.toggleFlash:
                toggleFlash();
                break;
            case R.id.toggleCamera:
                toggleCamera();
                break;

            case R.id.photoThumbnail:
                openPhotoFeed();
                break;

            case R.id.goSettings:
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mActivity = this;

        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        Crashlytics.log("Testing crashlytics");

        focusMarkerLayout = new FocusMarkerLayout(this);
        focusMarkerLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));

        relativeWrapper.addView(focusMarkerLayout);
        relativeWrapper.setClickable(false);

        try {
            storageManager = new StorageManager(this, true);
            privateStorageManager = new StorageManager(this, false);
            cameraProcessor = new CameraProcessor(this, surfaceView);
            cameraProcessor.initLibrary();
            filters = cameraProcessor.getFilters();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            mDetector = new GestureDetectorCompat(this, new CameraGestureListener());
            CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);
            buildCamera();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        modes.add(new SingleModeItem(0, "GALLERY"));
        modes.add(new SingleModeItem(1, "PORTRAIT"));

        adapter = new ModeListAdapter(this, modes, modeSelector, selectedCameraModes);

        modeSelector.setOrientation(Orientation.HORIZONTAL);
        modeSelector.setAdapter(adapter);
        modeSelector.setItemTransitionTimeMillis(30);
        modeSelector.setItemTransformer(new ScaleTransformer.Builder()
            .setMinScale(0.7f)
            .setMaxScale(0.8f)
            .build());

        modeSelector.addOnItemChangedListener(new onModeChangedListener());

        modeSelector.post(new Runnable() {
            @Override
            public void run() {
                modeSelector.scrollToPosition(1);
                isModeSelectorEnabled = true;
            }
        });

        filtersAdapter = new FilterListAdapter(this, filters, filterSelector, selectedFilter, new FilterListAdapter.FilterItemClickListener() {
            @Override
            public void onClickSelectedItemListener(int selected) {
                try {
                    if (!canTakePicture) return;

                    takePicture();

                    camera_shutter_outside.setImageDrawable(getResources().getDrawable(R.drawable.camera_shutter_outside_dashed));
                    RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotate.setDuration(3000);
                    rotate.setInterpolator(new LinearInterpolator());
                    rotate.setRepeatCount(Animation.INFINITE);
                    camera_shutter_outside.startAnimation(rotate);

                } catch (Exception ex) {
                    Crashlytics.logException(ex);
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

        filterSelector.post(new Runnable() {
            @Override
            public void run() {
                filterSelector.smoothScrollToPosition(0);
            }
        });
        filterSelector.addOnItemChangedListener(new onFilterChangedListener());


        intensityIndicator.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if (!isFilteringEnabled)
                    return;

                filterIntensity = i / 100f;

                if (!filterConfig.isEmpty()) {
                    cameraProcessor.setIntensity(filterIntensity, null);
                }

                restartHideSeekBar();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                restartHideSeekBar();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                restartHideSeekBar();
            }
        });

        photoFeedAdapter = new PhotoFeedAdapter(this, previewPhotos, privateStorageManager, storageManager);

        photoFeed.setOrientation(Orientation.HORIZONTAL);
        photoFeed.setAdapter(photoFeedAdapter);
        photoFeed.setItemTransitionTimeMillis(30);
        photoFeed.setItemTransformer(new ScaleTransformer.Builder()
            .setMinScale(1f)
            .setMaxScale(1f)
            .build());

    }

    private void openPhotoFeed() {
        if (isPhotoFeedOpened) return;

        try {
            photoFeed.post(new Runnable() {
                @Override
                public void run() {
                    photoFeed.smoothScrollToPosition(0);
                    photoFeedWrapper.setVisibility(View.VISIBLE);
                }
            });
            isPhotoFeedOpened = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Runnable runHideSeek = new Runnable() {
        @Override
        public void run() {
            intensityIndicator.animate().setStartDelay(0).alpha(0).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    intensityIndicator.setVisibility(View.GONE);
                }
            }).start();
        }
    };

    private void takePicture() {
        if (!canTakePicture) return;

        if (!wasFocused) {
            Log.d(TAG, "Trying to focus auto");
            try {
                surfaceView.cameraInstance().getCameraDevice().cancelAutoFocus();
                surfaceView.setFocusMode(FOCUS_MODE_AUTO);
                surfaceView.cameraInstance().getCameraDevice().autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean successful, Camera camera) {
                        if (successful) {
                            Log.d(TAG, "Succefull");

                            wasFocused = true;
                            internalTakePicture();
                        } else {
                            Log.d(TAG, "Error focusing");
                            if (nFocusIntents < 2) {
                                nFocusIntents++;
                                takePicture();
                            } else {
                                nFocusIntents = 0;

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        camera_shutter_outside.clearAnimation();
                                        camera_shutter_outside.setImageDrawable(getResources().getDrawable(R.drawable.camera_shutter_outside));
                                        camera_shutter_outside.setRotation(0);

                                        focusFailedContainer.setVisibility(View.VISIBLE);

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                focusFailedContainer.setVisibility(View.GONE);
                                            }
                                        }, 1000);

                                    }
                                });

                            }

                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Crashlytics.logException(ex);
            }
        } else internalTakePicture();

    }

    private void internalTakePicture() {

        final PhotoItem currentPhoto = new PhotoItem();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Bundle params = new Bundle();
                params.putInt("filterId", selectedIndex);
                params.putFloat("filterIntensity", filterIntensity);
                FirebaseAnalytics.getInstance(mActivity).logEvent("pictureTaken", params);

                final long startTime2 = System.currentTimeMillis();

                //Take real picture with high resolution
                surfaceView.takePicture(new CameraSurfaceView.TakePictureCallback() {
                    @Override
                    public void takePictureOK(Bitmap bmp) {
                        @SuppressLint("SimpleDateFormat") String timeStamp =
                            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        final String name = File.separator + "IMG_" + timeStamp + ".jpg";
                        storageManager.createFile(name, bmp);
                        bmp.recycle();

                        MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[]{storageManager.getFile(name).getPath()}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i(TAG, "Scanned " + path);
                                }
                            });

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                int positionOfScroll = photoFeed.getCurrentItem();
                                currentPhoto.setBigPath(storageManager.getFile(name).getAbsolutePath());
                                setString("lastPhotoOriginal", name);

                                photoFeedAdapter.notifyDataSetChanged();
                                photoFeed.scrollToPosition(positionOfScroll);

                                Log.d(TAG, "Done with big");
                                Log.d(TAG, (System.currentTimeMillis() - startTime2) + "ms taking and saving big");

                            }
                        });

                    }

                }, null, filterConfig.isEmpty() ? null : filterConfig, filterConfig.isEmpty() ? 1 : filterIntensity, actualCamera == FRONTAL_CAMERA);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();

                surfaceView.takeShot(new CameraSurfaceView.TakePictureCallback() {
                    @Override
                    public void takePictureOK(Bitmap bmp) {
                        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        final String name = File.separator + "IMG_" + timeStamp + ".jpg";
                        privateStorageManager.createFile(name, bmp);
                        bmp.recycle();

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Log.d(TAG, " Done with thumbnail");

                                    GlideApp
                                        .with(getApplicationContext())
                                        .load(privateStorageManager.getFile(name))
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(photoThumbnail);

                                    animateThumbnail();
                                    setString("lastPhotoThumbnail", name);
                                    currentPhoto.setThumbPath(privateStorageManager.getFile(name).getAbsolutePath());
                                    previewPhotos.add(0, currentPhoto);
                                    photoFeedAdapter.notifyItemInserted(0);

                                    camera_shutter_outside.clearAnimation();
                                    camera_shutter_outside.setImageDrawable(getResources().getDrawable(R.drawable.camera_shutter_outside));
                                    camera_shutter_outside.setRotation(0);

                                    photoFeed.setAdapter(photoFeedAdapter);

                                    Log.d(TAG, (System.currentTimeMillis() - startTime) + "ms taking and saving snap");
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                    }
                }, true);

            }
        }).start();
    }

    private void toggleFlash() {
        if (pendingToggleFlash)
            return;

        pendingToggleFlash = true;
        int flashBackup = actualFlash;
        int drawable = 0;

        setBoolean("flashChanged", true);

        switch (actualFlash) {
            case FLASH_OFF:
                actualFlash = FLASH_ON;
                drawable = R.drawable.ic_flash_on_white_24dp;
                break;

            case FLASH_ON:
                actualFlash = FLASH_AUTO;
                drawable = R.drawable.ic_flash_auto_white_24dp;
                break;
            case FLASH_AUTO:
                actualFlash = FLASH_OFF;
                drawable = R.drawable.ic_flash_off_white_24dp;
                break;
        }

        setInteger("flashMode", actualFlash);

        boolean response = surfaceView.setFlashLightMode(actualFlash);

        if (response)
            toggleFlashIcon.setImageDrawable(getResources().getDrawable(drawable));


        else
            actualFlash = flashBackup;

        pendingToggleFlash = false;
    }

    private void toggleCamera() {
        if (pendingToggleCamera)
            return;

        pendingToggleCamera = true;

        try {
            actualCamera = actualCamera == BACK_CAMERA ? FRONTAL_CAMERA : BACK_CAMERA;

            setBoolean("cameraChanged", true);
            setInteger("cameraFacing", actualCamera);

            toggleCameraIcon.setImageDrawable(getResources().getDrawable(actualCamera == BACK_CAMERA ? R.drawable.ic_camera_front_white_24dp : R.drawable.ic_camera_rear_white_24dp));
            toggleFlashIcon.setVisibility(actualCamera == BACK_CAMERA ? View.VISIBLE : View.GONE);

            surfaceView.setCameraFacing(actualCamera);
            surfaceView.onPause();
            surfaceView.onResume();

            if (filterConfig != null) {
                surfaceView.setFilterWithConfig(filterConfig);
                surfaceView.setFilterIntensity(filterIntensity);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        pendingToggleCamera = false;
    }

    private CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback() {
        @Override
        public Bitmap loadImage(String name, Object arg) {
            Log.i(TAG, "Loading file: " + name);

            AssetManager am = getAssets();
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


    @Override
    public void onBackPressed() {
        if (isPhotoFeedOpened) {
            isPhotoFeedOpened = false;
            photoFeedWrapper.setVisibility(View.GONE);
        } else if (selectedIndex != 0) {
            filterSelector.smoothScrollToPosition(0);
        } else super.onBackPressed();
    }

    @Override
    public void onPause() {
        super.onPause();
        CameraInstance.getInstance().stopCamera();
        surfaceView.release(null);
        surfaceView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        surfaceView.onResume();
    }

    private void performSwipeRight() {
        int previous = filterSelector.getCurrentItem() - 1;
        if (previous >= 0)
            filterSelector.smoothScrollToPosition(previous);
    }

    private void performSwipeLeft() {
        int next = filterSelector.getCurrentItem() + 1;

        if (next < filtersAdapter.getItemCount())
            filterSelector.smoothScrollToPosition(next);
    }

    private void performSwipeTop() {

    }

    private void performSwipeBottom() {

    }

    private void performFocus(MotionEvent event) {


        if (!filterConfig.isEmpty()) {
            intensityIndicator.setVisibility(View.VISIBLE);
            intensityIndicator.animate().alpha(1).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    restartHideSeekBar();
                    super.onAnimationEnd(animation);
                }


            }).start();

        }

        if (actualCamera != BACK_CAMERA)
            return;

        final float x = event.getX();
        final float y = event.getY();
        final float focusX = x / surfaceView.getWidth();
        final float focusY = y / surfaceView.getHeight();

        focusMarkerLayout.focus(focusX, focusY);

        surfaceView.focusAtPoint(focusX, focusY, new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    wasFocused = true;
                    Log.e(TAG, String.format("Focus OK, pos: %g, %g", focusX, focusY));

                } else {
                    wasFocused = false;
                    Log.e(TAG, String.format("Focus failed, pos: %g, %g", focusX, focusY));
                }
            }
        });
    }

    private void restartHideSeekBar() {
        hideSeekBarHandler.removeCallbacks(runHideSeek);
        hideSeekBarHandler.postDelayed(runHideSeek, seekBarWait);
    }

    private void animateThumbnail() {
        photoThumbnail.animate().setListener(null).cancel();
        photoThumbnail.setScaleX(6f);
        photoThumbnail.setScaleY(6f);
        photoThumbnail.setAlpha(0f);
        photoThumbnail.setVisibility(View.VISIBLE);
        photoThumbnail.animate().setStartDelay(0).alpha(1f).scaleX(1f).scaleY(1f).setDuration(500)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                }
            }).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void buildCamera() {
        if (getBoolean("frontalQualityChanged"))
            frontalPhotoQuality = getInteger("frontalImageQuality");
        else frontalPhotoQuality = 2;

        if (getBoolean("rearQualityChanged"))
            rearPhotoQuality = getInteger("rearImageQuality");
        else rearPhotoQuality = 1;

        surfaceView.setFrontalPhotoQuality(frontalPhotoQuality);
        surfaceView.setRearPhotoQuality(rearPhotoQuality);

        surfaceView.setStorageManager(storageManager);

        if (getBoolean("cameraChanged")) {
            actualCamera = getInteger("cameraFacing");

            try {

                toggleCameraIcon.setImageDrawable(getResources().getDrawable(actualCamera == BACK_CAMERA ? R.drawable.ic_camera_front_white_24dp : R.drawable.ic_camera_rear_white_24dp));
                toggleFlashIcon.setVisibility(actualCamera == BACK_CAMERA ? View.VISIBLE : View.GONE);

                surfaceView.setCameraFacing(actualCamera);
                surfaceView.onPause();
                surfaceView.onResume();

                if (filterConfig != null) {
                    surfaceView.setFilterWithConfig(filterConfig);
                    surfaceView.setFilterIntensity(filterIntensity);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        surfaceView.setOnCreateCallback(new CameraView.OnCreateCallback() {
            @Override
            public void createOver(boolean success) {
                if (actualCamera == BACK_CAMERA)
                    surfaceView.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);

                isFilteringEnabled = true;
                canTakePicture = true;

                // Flash and thumbnail
                buildPreferences();

                Log.i(TAG, "Surface Created");
            }
        });

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
    }

    private void buildPreferences() {
        if (getBoolean("flashChanged")) {
            int flashMode = getInteger("flashMode");
            int drawable = 0;

            switch (flashMode) {
                case FLASH_OFF:
                    drawable = R.drawable.ic_flash_off_white_24dp;
                    break;
                case FLASH_ON:
                    drawable = R.drawable.ic_flash_on_white_24dp;
                    break;
                case FLASH_AUTO:
                    drawable = R.drawable.ic_flash_auto_white_24dp;
                    break;
            }

            final boolean response = surfaceView.setFlashLightMode(flashMode);

            final int finalDrawable = drawable;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (response)
                        toggleFlashIcon.setImageDrawable(getResources().getDrawable(finalDrawable));
                }
            });

            actualFlash = flashMode;
        }


        final String lastTakenThumb = getString("lastPhotoThumbnail");
        final String lastTakenOriginal = getString("lastPhotoOriginal");

        final PhotoItem currentPhoto = new PhotoItem();

        if (!lastTakenThumb.isEmpty() && !lastTakenOriginal.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GlideApp
                        .with(getApplicationContext())
                        .load(privateStorageManager.getFile(lastTakenThumb))
                        .apply(RequestOptions.circleCropTransform())
                        .into(photoThumbnail);

                    photoThumbnail.setScaleX(1f);
                    photoThumbnail.setScaleY(1f);
                    photoThumbnail.setAlpha(1f);
                    photoThumbnail.setVisibility(View.VISIBLE);

                    currentPhoto.setThumbPath(privateStorageManager.getFile(lastTakenThumb).getAbsolutePath());
                    currentPhoto.setBigPath(privateStorageManager.getFile(lastTakenOriginal).getAbsolutePath());
                    previewPhotos.add(0, currentPhoto);
                    photoFeedAdapter.notifyItemInserted(0);
                    photoFeed.setAdapter(photoFeedAdapter);

                }
            });
        }
    }

    private class CameraGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            performFocus(event);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            performSwipeRight();

                        } else {
                            performSwipeLeft();
                        }
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            performSwipeBottom();
                        } else {
                            performSwipeTop();
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return true;
        }
    }

    private class onModeChangedListener implements DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder> {
        @Override
        public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition) {
            if (viewHolder != null) {
                for (int i : selectedCameraModes) {
                    try {
                        modeSelector.getViewHolder(i).itemView.setSelected(false);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                selectedCameraModes.clear();
                selectedCameraModes.add(adapterPosition);
                viewHolder.itemView.setSelected(true);

                // TODO CHANGE WITH ID OF GALLERY
                if (adapterPosition == 0 && isModeSelectorEnabled) {
                    startActivity(new Intent(getApplicationContext(), GalleryActivity.class));
                    finish();
                }

                Log.d(TAG, adapterPosition + "selected item");
                Log.d(TAG, isModeSelectorEnabled + " selector enabled");

            }
        }
    }

    private class onFilterChangedListener implements DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder> {
        @Override
        public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition) {
            FilterItem filter = filters.get(adapterPosition);
            filterIndicator.setText(filter.getName());

            cameraProcessor.setFilter(adapterPosition, null);

            intensityIndicator.setProgress((int) (filter.getIntensity() * 100f));
            Log.d(TAG, " New intensity:" + (int) (filter.getIntensity() * 100f));

            filterConfig = filter.getParams();
            filterIntensity = filter.getIntensity();

            filterIndicator.setAlpha(0f);
            filterIndicator.setScaleX(0f);
            filterIndicator.setScaleY(0f);

            filterIndicator.setVisibility(View.VISIBLE);
            filterIndicator.animate().setListener(null).cancel();
            filterIndicator.animate().setStartDelay(0).setDuration(350).alpha(1).scaleX(1).scaleY(1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    filterIndicator.animate().setStartDelay(1500).alpha(0f).scaleX(0f).scaleY(0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
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