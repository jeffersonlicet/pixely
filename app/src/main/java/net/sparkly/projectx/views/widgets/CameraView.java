package net.sparkly.projectx.views.widgets;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.media.ExifInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import net.sparkly.projectx.utils.ClearColor;

import org.wysaid.camera.CameraInstance;
import org.wysaid.common.Common;
import org.wysaid.common.FrameBufferObject;
import org.wysaid.nativePort.CGEFrameRecorder;
import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.texUtils.TextureRenderer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener
{
    public static final String TAG = CameraView.class.getSimpleName();
    private static final int BACK_CAMERA = 0;

    private ClearColor mClearColor;
    private SurfaceTexture mSurfaceTexture;
    private CGEFrameRecorder mFrameRecorder;
    private TextureRenderer.Viewport mDrawViewport = new TextureRenderer.Viewport();

    private int mViewWidth;
    private int mViewHeight;
    private int mRecordWidth;
    private int mRecordHeight;
    private int mTextureID;
    private int mMaxTextureSize;

    private boolean mIsUsingMask;
    private boolean mFitFullView = true;
    private boolean mIsCameraBackForward = true;
    private boolean mIsTransformMatrixSet;

    private float mMaskAspectRatio = 1.0f;
    private float[] _transformMatrix = new float[16];

    protected OnCreateCallback mOnCreateCallback;

    public CameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        setEGLContextClientVersion(2);

        //EGL Depth
        //setEGLConfigChooser(8, 8, 8, 8, 8, 0);

        getHolder().setFormat(PixelFormat.RGBA_8888);

        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        mClearColor = new ClearColor();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig)
    {
        if (!cameraInstance().isCameraOpened())
        {
            int facing = mIsCameraBackForward ? Camera.CameraInfo.CAMERA_FACING_BACK :
                    Camera.CameraInfo.CAMERA_FACING_FRONT;

            if (!cameraInstance().tryOpenCamera(null, facing))
            {
                Log.e(TAG, "Error opening camera");
                //TODO ABORT ALL
            }

            Camera.Size bestPreviewSize = getBestPreviewSize(cameraInstance().getParams());

            Log.d(TAG, bestPreviewSize.width + " best preview width");
            Log.d(TAG, bestPreviewSize.height + " best preview  height");

            cameraInstance().setPreferPreviewSize(bestPreviewSize.height, bestPreviewSize.width);

            mRecordWidth = bestPreviewSize.height;
            mRecordHeight = bestPreviewSize.width;

            Camera.Size bestPictureSize = getBestPictureSize(cameraInstance().getParams());
            cameraInstance().setPictureSize(bestPictureSize.width, bestPictureSize.height, true);

            Log.d(TAG, bestPictureSize.width + " best picture width");
            Log.d(TAG, bestPictureSize.height + " best picture  height");
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int texSize[] = new int[1];

        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, texSize, 0);
        mMaxTextureSize = texSize[0];

        Log.d(TAG, "Max textureSize" + mMaxTextureSize);

        mTextureID = Common.genSurfaceTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mFrameRecorder = new CGEFrameRecorder();
        mIsTransformMatrixSet = false;

        if (!mFrameRecorder.init(mRecordWidth, mRecordHeight, mRecordWidth, mRecordHeight))
        {
            Log.e(TAG, "Frame Recorder init failed!");
        }

        mFrameRecorder.setSrcRotation((float) (Math.PI / 2.0));
        mFrameRecorder.setSrcFlipScale(1.0f, -1.0f);
        mFrameRecorder.setRenderFlipScale(1.0f, -1.0f);

        requestRender();

        if (mOnCreateCallback != null)
        {
            mOnCreateCallback.createOver(cameraInstance().getCameraDevice() != null);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height)
    {
        GLES20.glClearColor(mClearColor.r, mClearColor.g, mClearColor.b, mClearColor.a);

        if (!cameraInstance().isPreviewing())
        {
            //Set the view dimensions
            mViewWidth = width;
            mViewHeight = height;

            //Adapt the view port
            adaptViewPort();

            //Request camera
            cameraInstance().startPreview(mSurfaceTexture);
            mFrameRecorder.srcResize(cameraInstance().previewHeight(), cameraInstance().previewWidth());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        super.surfaceDestroyed(holder);
        cameraInstance().stopCamera();
    }

    @Override
    public void onDrawFrame(GL10 gl10)
    {
        if (mSurfaceTexture == null || !cameraInstance().isPreviewing())
        {
            Log.d(TAG, "onDrawFrame() mSurfaceTexture == null || !cameraInstance().isPreviewing()");
            return;
        }

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(_transformMatrix);

        mFrameRecorder.update(mTextureID, _transformMatrix);
        mFrameRecorder.runProc();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);

        mFrameRecorder.render(mDrawViewport.x, mDrawViewport.y, mDrawViewport.width, mDrawViewport.height);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    @Override
    public void onPause()
    {
        cameraInstance().stopCamera();
        super.onPause();
    }

    public synchronized void setFilterWithConfig(final String config)
    {
        queueEvent(new Runnable()
        {
            @Override
            public void run()
            {

                if (mFrameRecorder != null)
                {
                    mFrameRecorder.setFilterWidthConfig(config);
                } else
                {
                    Log.e(TAG, "setFilterWithConfig after release!!");
                }
            }
        });
    }

    public void setFilterIntensity(final float intensity)
    {
        queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                if (mFrameRecorder != null)
                {
                    mFrameRecorder.setFilterIntensity(intensity);
                } else
                {
                    Log.e(TAG, "setFilterIntensity after release!!");
                }
            }
        });
    }

    public void setOnCreateCallback(final OnCreateCallback callback)
    {

        assert callback != null : "Invalid Operation!";

        if (mFrameRecorder == null)
        {
            mOnCreateCallback = callback;
        } else
        {
            queueEvent(new Runnable()
            {
                @Override
                public void run()
                {
                    callback.createOver(cameraInstance().getCameraDevice() != null);
                }
            });
        }
    }

    public void setCameraFacing(int facing)
    {
        if (BACK_CAMERA == facing)
        {
            mIsCameraBackForward = true;
        } else mIsCameraBackForward = false;
    }

    public void focusAtPoint(float x, float y, Camera.AutoFocusCallback focusCallback)
    {
        cameraInstance().focusAtPoint(y, 1.0f - x, focusCallback);
    }

    public CameraInstance cameraInstance()
    {
        return CameraInstance.getInstance();
    }

    public interface ReleaseOKCallback
    {
        void releaseOK();
    }

    public synchronized void release(final ReleaseOKCallback callback)
    {
        if (mFrameRecorder != null)
        {
            queueEvent(new Runnable()
            {
                @Override
                public void run()
                {
                    if (mFrameRecorder != null)
                    {
                        mFrameRecorder.release();
                        mFrameRecorder = null;

                        GLES20.glDeleteTextures(1, new int[]{mTextureID}, 0);

                        mTextureID = 0;
                        mSurfaceTexture.release();
                        mSurfaceTexture = null;

                        Log.i(TAG, "GLSurfaceview release...");
                        if (callback != null)
                            callback.releaseOK();
                    }

                }
            });
        }
    }

    public interface OnCreateCallback
    {
        void createOver(boolean success);
    }

    private void adaptViewPort()
    {
        float scaling;
        scaling = mRecordWidth / (float) mRecordHeight;


        float viewRatio = mViewWidth / (float) mViewHeight;
        float s = scaling / viewRatio;

        int w, h;

        if (s > 1.0)
        {
            w = (int) (mViewHeight * scaling);
            h = mViewHeight;
        } else
        {
            w = mViewWidth;
            h = (int) (mViewWidth / scaling);
        }

        mDrawViewport.width = w;
        mDrawViewport.height = h;
        mDrawViewport.x = (mViewWidth - mDrawViewport.width) / 2;
        mDrawViewport.y = (mViewHeight - mDrawViewport.height) / 2;

    }

    private Camera.Size getBestPictureSize(Camera.Parameters parameters)
    {
        int max = 0;
        int index = 0;

        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();

        for (int i = 0; i < sizes.size(); i++)
        {
            Camera.Size s = sizes.get(i);

            int size = s.height * s.width;
            if (size > max)
            {
                index = i;
                max = size;
            }
        }

        return sizes.get(index);
    }

    private Camera.Size getBestPreviewSize(Camera.Parameters parameters)
    {
        int max = 0;
        int index = 0;

        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        for (int i = 0; i < sizes.size(); i++)
        {
            Camera.Size s = sizes.get(i);

            int size = s.height * s.width;
            if (size > max)
            {
                index = i;
                max = size;
            }
        }

        return sizes.get(index);
    }
}
