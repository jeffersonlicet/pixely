package net.sparkly.projectx.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
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
import android.widget.RelativeLayout;

import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.Orientation;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import net.sparkly.projectx.R;
import net.sparkly.projectx.adapters.ModeListAdapter;
import net.sparkly.projectx.models.SingleModeItem;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CameraActivity extends AppCompatActivity
{
    private static final int BACK_CAMERA     = 0;
    private static final int FRONTAL_CAMERA  = 1;
    private static final String TAG          = CameraActivity.class.getSimpleName();

    private boolean canTakePicture;
    private boolean isFilteringEnabled;

    private ModeListAdapter adapter;
    private List<SingleModeItem> modes = new ArrayList<>();
    private List<Integer> selectedCameraModes = new ArrayList<>();

    private Activity mActivity;
    private GestureDetectorCompat mDetector;
    private FocusMarkerLayout focusMarkerLayout;

    @BindView(R.id.surfaceView)
    CameraView surfaceView;

    @BindView(R.id.relativeWrapper)
    RelativeLayout relativeWrapper;

    @BindView(R.id.modeSelector)
    DiscreteScrollView modeSelector;

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
        modeSelector.setAdapter((adapter));
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

        modeSelector.addOnItemChangedListener(new DiscreteScrollView.OnItemChangedListener<RecyclerView.ViewHolder>()
        {
            @Override
            public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int adapterPosition)
            {
                if (viewHolder != null)
                {
                    viewHolder.itemView.setSelected(true);

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
                    if (isFilteringEnabled)
                    {
                        /*surfaceView.takePicture(new CameraSurfaceView.TakePictureCallback()
                        {
                            @Override
                            public void takePictureOK(Bitmap bmp)
                            {File mediaStorageDir;
                                String state = Environment.getExternalStorageState();

                                if (Environment.MEDIA_MOUNTED.equals(state)) {
                                    mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_PICTURES),
                                            "" + "ProjectX"
                                    );
                                } else {
                                    mediaStorageDir = new File(
                                            Environment.getExternalStoragePublicDirectory(
                                                    Environment.DIRECTORY_PICTURES),
                                            "" + "ProjectX"
                                    );
                                }

                                if (!mediaStorageDir.exists()) {
                                    if (!mediaStorageDir.mkdirs()) {
                                        return;
                                    }
                                }

                                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                                File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

                                try
                                {
                                    FileOutputStream fos = new FileOutputStream(mediaFile);
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                    fos.write(out.toByteArray());
                                    fos.close();
                                } catch (FileNotFoundException e)
                                {
                                    e.printStackTrace();
                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }, null, "@curve G(0, 35)(255, 255)B(0, 133)(255, 255) @adjust brightness 0.03 @adjust contrast 1.35 @curve G(0, 13)(255, 255)B(88, 0)(255, 255) @adjust brightness -0.04 @adjust contrast 0.8 @pixblend multiply 250 223 182 255 100", 0.5f, false);
                    */}
                }
            }
        });

        //surfaceView.requestPortrait();

    }

    private void performSwipeRight()
    {

    }

    private void performSwipeLeft()
    {
        surfaceView.setFilterWithConfig("@curve G(0, 35)(255, 255)B(0, 133)(255, 255) @adjust brightness 0.03 @adjust contrast 1.35 @curve G(0, 13)(255, 255)B(88, 0)(255, 255) @adjust brightness -0.04 @adjust contrast 0.8 @pixblend multiply 250 223 182 255 100");
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
    }

    @SuppressLint("ClickableViewAccessibility")
    private void buildCamera()
    {
        surfaceView.setCameraFacing(BACK_CAMERA);

        surfaceView.setOnCreateCallback(new CameraView.OnCreateCallback()
        {
            @Override
            public void createOver(boolean success)
            {
                isFilteringEnabled = true;
            }
        });


        /*surfaceView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                //Toast.makeText(getApplicationContext(), " simple touch", Toast.LENGTH_SHORT).show();

            }
        });*/

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
}
