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
import net.sparkly.projectx.utils.StorageManager;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.hardware.Camera.Parameters.FLASH_MODE_AUTO;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_ON;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;

public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener
{
    public static final String TAG = CameraView.class.getSimpleName();
    private static final int BACK_CAMERA = 0;

    private ClearColor mClearColor;
    private SurfaceTexture mSurfaceTexture;
    private CGEFrameRecorder mFrameRecorder;
    private TextureRenderer.Viewport mDrawViewport = new TextureRenderer.Viewport();
    private StorageManager storageManager;

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

        //cameraInstance().stopCamera();
        //resumePreview();
    }

    public synchronized void resumePreview()
    {

        if (mFrameRecorder == null)
        {
            Log.e(TAG, "resumePreview after release!!");
            return;
        }

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


        requestRender();
    }

    public void focusAtPoint(float x, float y, Camera.AutoFocusCallback focusCallback)
    {
        cameraInstance().focusAtPoint(y, 1.0f - x, focusCallback);
    }

    public void setFocusMode(String focusMode)
    {
        /*if(cameraInstance().getParams().getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            return;

        cameraInstance().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);*/

        Camera.Parameters mParams = cameraInstance().getParams();
        List<String> focusModes = mParams.getSupportedFocusModes();

        for(String focus : focusModes)
        {
            Log.d(TAG, focus +  "Focus mode");
        }

        if(focusModes.contains(focusMode)){
            mParams.setFocusMode(focusMode);
        }

        cameraInstance().setParams(mParams);
    }

    public CameraInstance cameraInstance()
    {
        return CameraInstance.getInstance();
    }

    public void setStorageManager(StorageManager storageManager)
    {
        this.storageManager = storageManager;
    }

    public interface ReleaseOKCallback
    {
        void releaseOK();
    }

    public interface TakePictureCallback
    {
        void takePictureOK(Bitmap bmp);
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

    // mode value should be:
    //    Camera.Parameters.FLASH_MODE_AUTO;
    //    Camera.Parameters.FLASH_MODE_OFF;
    //    Camera.Parameters.FLASH_MODE_ON;
    //    Camera.Parameters.FLASH_MODE_RED_EYE
    //    Camera.Parameters.FLASH_MODE_TORCH 等
    public synchronized boolean setFlashLightMode(int mode)
    {
        String flashRequest = null;

        switch (mode)
        {
            case 0:
                flashRequest = FLASH_MODE_OFF;
                break;
            case 1:
                flashRequest = FLASH_MODE_ON;
                break;
            case 2:
                flashRequest = FLASH_MODE_AUTO;
                break;
        }

        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
        {
            Log.e(TAG, "No flash light is supported by current device!");
            return false;
        }

        if (!mIsCameraBackForward)
        {
            return false;
        }

        Camera.Parameters parameters = cameraInstance().getParams();

        if (parameters == null)
            return false;

        try
        {

            if (!parameters.getSupportedFlashModes().contains(flashRequest))
            {
                Log.e(TAG, "Invalid Flash Light Mode!!!");
                return false;
            }

            parameters.setFlashMode(flashRequest);
            cameraInstance().setParams(parameters);
        } catch (Exception e)
        {
            Log.e(TAG, "Switch flash light failed, check if you're using front camera.");
            return false;
        }

        return true;
    }

    public synchronized void takePicture(final CameraSurfaceView.TakePictureCallback photoCallback, Camera.ShutterCallback shutterCallback, final String config, final float intensity, final boolean isFrontMirror)
    {

        Camera.Parameters params = cameraInstance().getParams();

        if (photoCallback == null || params == null)
        {
            Log.e(TAG, "takePicture after release!");
            if (photoCallback != null)
            {
                photoCallback.takePictureOK(null);
            }
            return;
        }

        try
        {
            params.setRotation(90);
            cameraInstance().setParams(params);
        } catch (Exception e)
        {
            Log.e(TAG, "Error when takePicture: " + e.toString());
            photoCallback.takePictureOK(null);
            return;
        }

        cameraInstance().getCameraDevice().takePicture(shutterCallback, null, new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(final byte[] data, final Camera camera)
            {
                cameraInstance().getCameraDevice().startPreview();

                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Camera.Parameters params = camera.getParameters();
                        Camera.Size sz = params.getPictureSize();
                        Log.e(TAG, "Width taken: " + sz.width + " height taken:" + sz.height);

                        boolean shouldRotate;

                        Bitmap bmp;
                        int width, height;

                        if (sz.width != sz.height)
                        {
                            //默认数据格式已经设置为 JPEG
                            bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                            width = bmp.getWidth();
                            height = bmp.getHeight();
                            shouldRotate = (sz.width > sz.height && width > height) || (sz.width < sz.height && width < height);
                        } else
                        {
                            Log.i(TAG, "Cache image to get exif.");

                            try
                            {
                                String tmpFilename = getContext().getExternalCacheDir() + "/picture_cache000.jpg";
                                FileOutputStream fileout = new FileOutputStream(tmpFilename);
                                BufferedOutputStream bufferOutStream = new BufferedOutputStream(fileout);
                                bufferOutStream.write(data);
                                bufferOutStream.flush();
                                bufferOutStream.close();

                                ExifInterface exifInterface = new ExifInterface(tmpFilename);
                                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                                switch (orientation)
                                {
                                    case ExifInterface.ORIENTATION_ROTATE_90:
                                        shouldRotate = true;
                                        break;
                                    default:
                                        shouldRotate = false;
                                        break;
                                }

                                bmp = BitmapFactory.decodeFile(tmpFilename);
                                width = bmp.getWidth();
                                height = bmp.getHeight();

                            } catch (IOException e)
                            {
                                Log.e(TAG, "Err when saving bitmap...");
                                e.printStackTrace();
                                return;
                            }
                        }


                        if (width > mMaxTextureSize || height > mMaxTextureSize)
                        {
                            float scaling = Math.max(width / (float) mMaxTextureSize, height / (float) mMaxTextureSize);
                            Log.i(TAG, String.format("目标尺寸(%d x %d)超过当前设备OpenGL 能够处理的最大范围(%d x %d)， 现在将图片压缩至合理大小!", width, height, mMaxTextureSize, mMaxTextureSize));

                            bmp = Bitmap.createScaledBitmap(bmp, (int) (width / scaling), (int) (height / scaling), false);

                            width = bmp.getWidth();
                            height = bmp.getHeight();
                        }

                        Bitmap bmp2;

                        if (shouldRotate)
                        {
                            bmp2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                            Canvas canvas = new Canvas(bmp2);

                            if (cameraInstance().getFacing() == Camera.CameraInfo.CAMERA_FACING_BACK)
                            {
                                Matrix mat = new Matrix();
                                int halfLen = Math.min(width, height) / 2;
                                mat.setRotate(90, halfLen, halfLen);
                                canvas.drawBitmap(bmp, mat, null);
                            } else
                            {
                                Matrix mat = new Matrix();

                                if (isFrontMirror)
                                {
                                    mat.postTranslate(-width / 2, -height / 2);
                                    mat.postScale(-1.0f, 1.0f);
                                    mat.postTranslate(width / 2, height / 2);
                                    int halfLen = Math.min(width, height) / 2;
                                    mat.postRotate(90, halfLen, halfLen);
                                } else
                                {
                                    int halfLen = Math.max(width, height) / 2;
                                    mat.postRotate(-90, halfLen, halfLen);
                                }

                                canvas.drawBitmap(bmp, mat, null);
                            }

                            bmp.recycle();
                        } else
                        {
                            if (cameraInstance().getFacing() == Camera.CameraInfo.CAMERA_FACING_BACK)
                            {
                                bmp2 = bmp;
                            } else
                            {

                                bmp2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(bmp2);
                                Matrix mat = new Matrix();
                                if (isFrontMirror)
                                {
                                    mat.postTranslate(-width / 2, -height / 2);
                                    mat.postScale(1.0f, -1.0f);
                                    mat.postTranslate(width / 2, height / 2);
                                } else
                                {
                                    mat.postTranslate(-width / 2, -height / 2);
                                    mat.postScale(-1.0f, -1.0f);
                                    mat.postTranslate(width / 2, height / 2);
                                }

                                canvas.drawBitmap(bmp, mat, null);
                            }

                        }

                        if (config != null)
                        {
                            //CGENativeLibrary.filterImage_MultipleEffectsWriteBack(bmp2, config, intensity);
                        }

                        photoCallback.takePictureOK(bmp2);
                    }
                }).start();

            }
        });
    }

    public synchronized void takeShot(final CameraSurfaceView.TakePictureCallback callback, final boolean noMask)
    {
        assert callback != null : "callback must not be null!";

        if (mFrameRecorder == null)
        {
            Log.e(TAG, "Recorder not initialized!");
            callback.takePictureOK(null);
            return;
        }

        queueEvent(new Runnable()
        {
            @Override
            public void run()
            {

                FrameBufferObject frameBufferObject = new FrameBufferObject();
                int bufferTexID;
                IntBuffer buffer;
                Bitmap bmp;

                if (noMask || !mIsUsingMask)
                {

                    bufferTexID = Common.genBlankTextureID(mRecordWidth, mRecordHeight);
                    frameBufferObject.bindTexture(bufferTexID);
                    GLES20.glViewport(0, 0, mRecordWidth, mRecordHeight);
                    mFrameRecorder.drawCache();
                    buffer = IntBuffer.allocate(mRecordWidth * mRecordHeight);
                    GLES20.glReadPixels(0, 0, mRecordWidth, mRecordHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                    bmp = Bitmap.createBitmap(mRecordWidth, mRecordHeight, Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(buffer);
                    Log.i(TAG, String.format("w: %d, h: %d", mRecordWidth, mRecordHeight));

                } else
                {

                    bufferTexID = Common.genBlankTextureID(mDrawViewport.width, mDrawViewport.height);
                    frameBufferObject.bindTexture(bufferTexID);

                    int w = Math.min(mDrawViewport.width, mViewWidth);
                    int h = Math.min(mDrawViewport.height, mViewHeight);

                    mFrameRecorder.setRenderFlipScale(1.0f, 1.0f);
                    mFrameRecorder.setMaskTextureRatio(mMaskAspectRatio);
                    mFrameRecorder.render(0, 0, w, h);
                    mFrameRecorder.setRenderFlipScale(1.0f, -1.0f);
                    mFrameRecorder.setMaskTextureRatio(mMaskAspectRatio);

                    Log.i(TAG, String.format("w: %d, h: %d", w, h));
                    buffer = IntBuffer.allocate(w * h);
                    GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                    bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(buffer);
                }

                frameBufferObject.release();
                GLES20.glDeleteTextures(1, new int[]{bufferTexID}, 0);

                callback.takePictureOK(bmp);
            }
        });

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

        Collections.sort(sizes, new Comparator<Camera.Size>()
        {
            @Override
            public int compare(Camera.Size size1, Camera.Size size2)
            {
                return size1.height*size1.width - size2.height*size2.width;
            }
        });

        /*for (int i = 0; i < sizes.size(); i++)
        {
            Camera.Size s = sizes.get(i);

            int size = s.height * s.width;
            if (size > max)
            {
                index = i;
                max = size;
            }
        }*/

        return sizes.get((int) Math.ceil(sizes.size()/2));
    }
}
