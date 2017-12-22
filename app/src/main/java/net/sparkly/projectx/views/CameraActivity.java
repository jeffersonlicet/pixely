package net.sparkly.projectx.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.Orientation;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import net.sparkly.projectx.R;
import net.sparkly.projectx.adapters.FilterListAdapter;
import net.sparkly.projectx.adapters.ModeListAdapter;
import net.sparkly.projectx.models.FilterItem;
import net.sparkly.projectx.models.SingleModeItem;
import net.sparkly.projectx.utils.StorageManager;
import net.sparkly.projectx.views.widgets.CameraSurfaceView;
import net.sparkly.projectx.views.widgets.CameraView;
import net.sparkly.projectx.views.widgets.FocusMarkerLayout;

import org.wysaid.camera.CameraInstance;
import org.wysaid.nativePort.CGENativeLibrary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity
{
    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final int BACK_CAMERA = 0;
    private static final int FRONTAL_CAMERA = 1;
    private static final int FLASH_OFF = 0;
    private static final int FLASH_ON = 1;
    private static final int FLASH_AUTO = 2;

    private int actualFlash;
    private int actualCamera;

    private boolean canTakePicture;
    private boolean isFilteringEnabled;
    private boolean pendingToggleFlash;
    private boolean pendingToggleCamera;

    private ModeListAdapter adapter;
    private FilterListAdapter filtersAdapter;

    private List<SingleModeItem> modes = new ArrayList<>();
    private List<FilterItem> filters = new ArrayList<>();
    private List<Integer> selectedCameraModes = new ArrayList<>();
    private List<Integer> selectedFilter;

    private Activity mActivity;
    private GestureDetectorCompat mDetector;
    private FocusMarkerLayout focusMarkerLayout;
    private StorageManager storageManager;

    @BindView(R.id.surfaceView)
    CameraView surfaceView;

    @BindView(R.id.relativeWrapper)
    RelativeLayout relativeWrapper;

    @BindView(R.id.modeSelector)
    DiscreteScrollView modeSelector;

    @BindView(R.id.filterSelector)
    DiscreteScrollView filterSelector;

    @BindView(R.id.toggleFlash)
    ImageButton toggleFlashIcon;

    @BindView(R.id.toggleCamera)
    ImageButton toggleCameraIcon;

    @BindView(R.id.cameraShutter)
    RelativeLayout cameraShutter;

    @BindView(R.id.photoThumbnail)
    ImageView photoThumbnail;

    @BindView(R.id.photoThumbnailBorder)
    View photoThumbnailBorder;

    @OnClick({R.id.toggleFlash, R.id.toggleCamera, R.id.cameraShutter})
    public void clickManager(View view)
    {
        switch (view.getId())
        {
            case R.id.toggleFlash:
                toggleFlash();
                break;
            case R.id.toggleCamera:
                toggleCamera();
                break;

            case R.id.cameraShutter:
                //takePicture();
                break;
        }
    }

    private void takePicture()
    {
        if (!canTakePicture) return;

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                surfaceView.takePicture(new CameraSurfaceView.TakePictureCallback()
                {
                    @Override
                    public void takePictureOK(Bitmap bmp)
                    {
                        surfaceView.takeShot(new CameraSurfaceView.TakePictureCallback()
                        {
                            @Override
                            public void takePictureOK(final Bitmap bmp)
                            {
                                mActivity.runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bmp);
                                        roundedBitmapDrawable.setCircular(true);
                                        roundedBitmapDrawable.setAntiAlias(true);
                                        photoThumbnail.setImageDrawable(roundedBitmapDrawable);
                                        animateThumbnail();
                                        Log.d(TAG, bmp.getHeight() + " height and " + bmp.getWidth() + " width");
                                    }
                                });

                            }
                        }, true);

                        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        String name = File.separator + "IMG_" + timeStamp + ".jpg";
                        storageManager.createFile(name, bmp);

                    }

                }, null, null, 1, false);
            }
        }).start();
    }

    private void toggleFlash()
    {
        if (pendingToggleFlash)
            return;

        pendingToggleFlash = true;
        int flashBackup = actualFlash;
        int drawable = 0;

        switch (actualFlash)
        {
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

        boolean response = surfaceView.setFlashLightMode(actualFlash);

        if (response)
            toggleFlashIcon.setImageDrawable(getResources().getDrawable(drawable));


        else
            actualFlash = flashBackup;

        pendingToggleFlash = false;
    }

    private void toggleCamera()
    {
        if (pendingToggleCamera)
            return;

        pendingToggleCamera = true;

        try
        {
            actualCamera = actualCamera == BACK_CAMERA ? FRONTAL_CAMERA : BACK_CAMERA;

            toggleCameraIcon.setImageDrawable(getResources().getDrawable(actualCamera == BACK_CAMERA ? R.drawable.ic_camera_front_white_24dp : R.drawable.ic_camera_rear_white_24dp));
            toggleFlashIcon.setVisibility(actualCamera == BACK_CAMERA ? View.VISIBLE : View.GONE);

            surfaceView.setCameraFacing(actualCamera);
            surfaceView.onPause();
            surfaceView.onResume();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        pendingToggleCamera = false;
    }

    public CGENativeLibrary.LoadImageCallback mLoadImageCallback = new CGENativeLibrary.LoadImageCallback()
    {
        @Override
        public Bitmap loadImage(String name, Object arg)
        {
            AssetManager am = getAssets();
            InputStream is;
            try
            {
                is = am.open(name);
            } catch (IOException e)
            {
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
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        focusMarkerLayout = new FocusMarkerLayout(this);
        focusMarkerLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        relativeWrapper.addView(focusMarkerLayout);
        relativeWrapper.setClickable(false);

        mActivity = this;

        try
        {
            storageManager = new StorageManager(this);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        try
        {
            mDetector = new GestureDetectorCompat(this, new CameraGestureListener());
            CGENativeLibrary.setLoadImageCallback(mLoadImageCallback, null);
            buildCamera();

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        modes.add(new SingleModeItem(0, "Gif"));
        modes.add(new SingleModeItem(1, "Portrait"));
        modes.add(new SingleModeItem(2, "Video"));

        adapter = new ModeListAdapter(this, modes, modeSelector, selectedCameraModes);
        modeSelector.setOrientation(Orientation.HORIZONTAL);
        modeSelector.setAdapter(adapter);
        modeSelector.setItemTransitionTimeMillis(30);
        modeSelector.setItemTransformer(new ScaleTransformer.Builder()
                .setMinScale(0.7f)
                .setMaxScale(0.8f)
                .build());

        modeSelector.post(new Runnable()
        {
            @Override
            public void run()
            {
                modeSelector.smoothScrollToPosition(1);
            }
        });

        modeSelector.addOnItemChangedListener(new onModeChangedListener());

        filters.add(new FilterItem(0, "", 0, R.drawable.thumbnail_border));
        filters.add(new FilterItem(1, getString(R.string.filter0), getResources().getInteger(R.integer.intensityFilter0), R.drawable.thumbnail_border));
        filters.add(new FilterItem(2, "", 0, R.drawable.thumbnail_border));

        filtersAdapter = new FilterListAdapter(this, filters, filterSelector, selectedFilter, new FilterListAdapter.FilterItemClickListener()
        {
            @Override
            public void onClickSelectedItemListener(int selected)
            {
                takePicture();
            }
        });

        filterSelector.setOrientation(Orientation.HORIZONTAL);
        filterSelector.setAdapter(filtersAdapter);
        filterSelector.setSlideOnFling(true);
        filterSelector.setSlideOnFlingThreshold(1800);
        filterSelector.setItemTransitionTimeMillis(30);

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
    }

    @Override
    public void onPause()
    {
        super.onPause();
        CameraInstance.getInstance().stopCamera();
        surfaceView.release(null);
        surfaceView.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        surfaceView.onResume();
        modeSelector.post(new Runnable()
        {
            @Override
            public void run()
            {
                modeSelector.smoothScrollToPosition(1);
            }
        });
    }

    private void performSwipeRight()
    {
        int previous = filterSelector.getCurrentItem() - 1;
        if(previous >= 0)
            filterSelector.smoothScrollToPosition(previous);
    }

    private void performSwipeLeft()
    {
        int next = filterSelector.getCurrentItem() + 1;
        if(next < filtersAdapter.getItemCount())
            filterSelector.smoothScrollToPosition(next);
        //surfaceView.setFilterWithConfig("@curve G(0, 35)(255, 255)B(0, 133)(255, 255) @adjust brightness 0.03 @adjust contrast 1.35 @curve G(0, 13)(255, 255)B(88, 0)(255, 255) @adjust brightness -0.04 @adjust contrast 0.8 @pixblend multiply 250 223 182 255 100");
    }

    private void performSwipeTop()
    {

    }

    private void performSwipeBottom()
    {

    }

    private void performFocus(MotionEvent event)
    {
        final float x = event.getX();
        final float y = event.getY();
        final float focusX = x / surfaceView.getWidth();
        final float focusY = y / surfaceView.getHeight();

        focusMarkerLayout.focus(focusX, focusY);

        surfaceView.focusAtPoint(focusX, focusY, new Camera.AutoFocusCallback()
        {
            @Override
            public void onAutoFocus(boolean success, Camera camera)
            {
                if (success)
                {
                    Log.e(TAG, String.format("Focus OK, pos: %g, %g", focusX, focusY));

                } else
                {
                    Log.e(TAG, String.format("Focus failed, pos: %g, %g", focusX, focusY));
                }
            }
        });
    }

    private void animateThumbnail()
    {
        photoThumbnailBorder.setVisibility(View.VISIBLE);
        photoThumbnail.animate().setListener(null).cancel();
        photoThumbnail.setScaleX(2f);
        photoThumbnail.setScaleY(2f);
        photoThumbnail.setAlpha(0.3f);
        photoThumbnail.animate().alpha(1f).scaleX(1).scaleY(1).setStartDelay(0).setDuration(330)
                .setListener(new AnimatorListenerAdapter()
                {
                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        super.onAnimationEnd(animation);
                    }
                }).start();

    }

    @SuppressLint("ClickableViewAccessibility")
    private void buildCamera()
    {
        surfaceView.setStorageManager(storageManager);
        surfaceView.setCameraFacing(BACK_CAMERA);

        surfaceView.setOnCreateCallback(new CameraView.OnCreateCallback()
        {
            @Override
            public void createOver(boolean success)
            {
                isFilteringEnabled = true;
                canTakePicture = true;
            }
        });

        surfaceView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                mDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
    }

    private class CameraGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onDown(MotionEvent event)
        {
            return true;
        }

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onSingleTapUp(MotionEvent event)
        {
            performFocus(event);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            try
            {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY))
                {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD)
                    {
                        if (diffX > 0)
                        {
                            performSwipeRight();

                        } else
                        {
                            performSwipeLeft();
                        }
                    }
                } else
                {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD)
                    {
                        if (diffY > 0)
                        {
                            performSwipeBottom();
                        } else
                        {
                            performSwipeTop();
                        }
                    }
                }
            } catch (Exception exception)
            {
                exception.printStackTrace();
            }
            return true;
        }
    }

    private class onModeChangedListener implements DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder>
    {
        @Override
        public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition)
        {
            if (viewHolder != null)
            {
                for (int i : selectedCameraModes)
                {
                    try
                    {
                        modeSelector.getViewHolder(i).itemView.setSelected(false);

                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }

                selectedCameraModes.clear();
                selectedCameraModes.add(adapterPosition);
                viewHolder.itemView.setSelected(true);
            }
        }
    }
}
