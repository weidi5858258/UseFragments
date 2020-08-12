package com.weidi.usefragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.weidi.usefragments.business.media.Camera2Fragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.tool.AutoFitTextureView;
import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;
import androidx.legacy.app.FragmentCompat;

/***
 2020/08/11
 */
public class Camera2Activity extends BaseActivity {

    private static final String TAG =
            Camera2Activity.class.getSimpleName();

    private static final boolean DEBUG = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);

        internalCreate();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (DEBUG)
            MLog.d(TAG, "onRestart(): " + printThis());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            MLog.d(TAG, "onStart(): " + printThis());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            MLog.d(TAG, "onResume(): " + printThis());

        internalResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            MLog.d(TAG, "onPause(): " + printThis());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            MLog.d(TAG, "onStop(): " + printThis());

        internalStop();
    }

    @Override
    public void onDestroy() {
        internalDestroy();

        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + printThis());
    }

    /*@Override
    public void onBackPressed() {
        if (DEBUG)
            Log.d(TAG, "onBackPressed()");
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            Log.d(TAG, "onActivityResult() requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            Log.d(TAG, "onSaveInstanceState() outState: " + outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onRestoreInstanceState() savedInstanceState: " + savedInstanceState);
    }

    /***
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w640dp h336dp 320dpi nrml long land
        // finger -keyb/v/h -nav/h s.264}
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w360dp h616dp 320dpi nrml long port
        // finger -keyb/v/h -nav/h s.265}
        if (DEBUG)
            Log.d(TAG, "onConfigurationChanged() newConfig: " + newConfig);
    }

    ///////////////////////////////////////////////////////////////////////////////

    private void setFullscreen(boolean isShowStatusBar, boolean isShowNavigationBar) {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (!isShowStatusBar) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if (!isShowNavigationBar) {
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
    }

    private int curOrientation = 0;
    private int preOrientation = -1;
    private OrientationEventListener mOrientationListener;

    private void internalCreate() {
        setFullscreen(false, false);

        setContentView(R.layout.camera_layout);
        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);

        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                // 手机平放时,检测不到有效的角度
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }

                if (orientation > 350 || orientation < 10) { //0度
                    // 手机竖屏,摄像头在上边
                    curOrientation = 0;
                } else if (orientation > 80 && orientation < 100) { //90度
                    // 手机横屏,摄像头在右边
                    curOrientation = 90;
                } else if (orientation > 170 && orientation < 190) { //180度
                    // 手机竖屏,摄像头在下边
                    curOrientation = 180;
                } else if (orientation > 260 && orientation < 280) { //270度
                    // 手机横屏,摄像头在左边
                    curOrientation = 270;
                } else {
                    return;
                }

                if (preOrientation != curOrientation) {
                    preOrientation = curOrientation;
                    switch (curOrientation) {
                        case 0:
                        case 180:
                            configureTransform(720, 1280);
                            break;
                        case 90:
                        case 270:
                            configureTransform(1280, 720);
                            break;
                        default:
                            break;
                    }
                }
            }
        };

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surfaceJavaObject
        // is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    private void internalResume() {
        startBackgroundThread();
        if (mOrientationListener != null) {
            if (mOrientationListener.canDetectOrientation()) {
                mOrientationListener.enable();
            } else {
                mOrientationListener.disable();
            }
        }
    }

    private void internalStop() {
        closeCamera();
        stopBackgroundThread();
        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }
    }

    private void internalDestroy() {
        setFullscreen(true, true);
    }

    /***
     我们都知道Camera预览采集的图像流通常为NV21或YV12，
     那么编码器需要指定相应的颜色格式，否则编码得到的数据可能会出现花屏、叠影、颜色失真等现象。
     MediaCodecInfo.CodecCapabilities.存储了编码器所有支持的颜色格式，常见颜色格式映射如下：
     原始数据 编码器
     NV12(YUV420sp) ———> COLOR_FormatYUV420PackedSemiPlanar
     NV21 ———-> COLOR_FormatYUV420SemiPlanar
     YV12(I420) ———-> COLOR_FormatYUV420Planar
     */

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    /***
     Surface.ROTATION_0
     表示的是手机竖屏方向向上(也就是竖屏时的值),
     后面几个以此为基准依次以顺时针90度递增.

     int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
     这个rotation就是
     Surface.ROTATION_0,
     Surface.ROTATION_90,
     Surface.ROTATION_180,
     Surface.ROTATION_270
     其中的一个值,
     根据这个值然后返回后面的值.

     三星Note2手机.
     手机竖屏时,rotation = 0
     从手机竖屏逆时针旋转90度,rotation = 1
     从手机竖屏逆时针旋转180度,rotation = 2
     从手机竖屏逆时针旋转270度,rotation = 3
     从手机竖屏顺时针旋转90度,相当于逆时针旋转270度,rotation = 3
     从手机竖屏顺时针旋转180度,相当于逆时针旋转180度,rotation = 2
     从手机竖屏顺时针旋转270度,相当于逆时针旋转90度,rotation = 1

     总结:
     首先得认识这样一个事实,
     就是在这里有一个默认值,这个值就是Surface.ROTATION_0(为0),
     即手机竖屏时(传感器在最上面的状态,也就是我们一般竖屏拿手机的状态).
     然后得到的rotation的值就是屏幕旋转后的值,
     这个值是相对于这个默认值来说的.
     这个值可以理解为屏幕方向的值(也可以说是固定的值)
     这些值就是(手机一般为这样,可能跟平板得到的值不一样)
     自然竖屏 0 表示我们一般拿手机的姿势
     自然横屏 1 表示我们横向拿手机的姿势,摄像头在左手边
     反向竖屏 2 表示我们把手机拿反的姿势
     反向横屏 3 表示我们横向拿手机的姿势,摄像头在右手边

     上面说了一大堆,就是在说rotation为我们放置手机时得到的一个状态值.
     手机摄像头的传感器的默认方向为我们横向拿手机,摄像头在左手边时的一个方向.
     此时的rotation为1.
     */
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /***
     4320P(8K) 7680*4320
     2160P(4K) 3840*2160
     1440P(HD)
     1080P(HD) 1920*1080
     720P (HD) 1280*720
     480P
     360P
     240P
     144P
     */

    @InjectOnClick({R.id.texture})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.texture: {
                //takePicture();
                break;
            }
            default:
                break;
        }
    }

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mBackCameraId;
    private String mFrontCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     * <p>
     * ImageReader的回调函数, 其中的onImageAvailable会以一定频率（由EXECUTION_FREQUENCY和相机帧率决定）
     * 识别从预览中传回的图像，并在透明的SurfaceView中画框
     * 这个回调频率和预览刷新的帧率是一样的，帧率太快这里可能会造成crash
     * <p>
     * 回调函数中能得到的是Image对象，由于用于物体识别的函数参数需要的是cv::Mat的对象，
     * 所以我必须将YUV_420_888格式的图像转为cv::Mat，
     * 这部分没有学过，但运气很好找到了GitHub上的一个开源算法，
     * 很好用：GitHub-quickbirdstudios / yuvToMat
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            MLog.d(TAG, "onImageAvailable()");
            // 在子线程执行,防止预览界面卡顿
            // 当图片可得到的时候获取图片并保存
            /*mBackgroundHandler.post(
                    new ImageSaver(reader, mFile));*/
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /***
     A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     预览
     拍照
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                public void onCaptureStarted(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        long timestamp,
                        long frameNumber) {
                    // default empty implementation
                }

                @Override
                public void onCaptureProgressed(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        CaptureResult partialResult) {
                    process(partialResult);
                }

                @Override
                public void onCaptureCompleted(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        TotalCaptureResult result) {
                    process(result);
                }

                public void onCaptureFailed(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        CaptureFailure failure) {
                    // default empty implementation
                }

                private void process(CaptureResult result) {
                    switch (mState) {
                        case STATE_PREVIEW: {
                            // We have nothing to do when the camera preview is working normally.
                            break;
                        }
                        /***
                         等待对焦
                         */
                        case STATE_WAITING_LOCK: {
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == null) {
                                if (DEBUG)
                                    MLog.d(TAG, "mCaptureCallback STATE_WAITING_LOCK" +
                                            " process() afState is null");
                                captureStillPicture();
                            } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                                // CONTROL_AE_STATE can be null on some devices
                                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                                if (DEBUG)
                                    MLog.d(TAG, "mCaptureCallback STATE_WAITING_LOCK" +
                                            " process() afState: " + aeState);
                                if (aeState == null ||
                                        aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                    mState = STATE_PICTURE_TAKEN;
                                    // 对焦完成
                                    captureStillPicture();
                                } else {
                                    runPrecaptureSequence();
                                }
                            }
                            break;
                        }
                        case STATE_WAITING_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null ||
                                    aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                    aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                                mState = STATE_WAITING_NON_PRECAPTURE;
                            }
                            break;
                        }
                        case STATE_WAITING_NON_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null ||
                                    aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                                if (DEBUG)
                                    MLog.d(TAG, "mCaptureCallback STATE_WAITING_NON_PRECAPTURE" +
                                            " process() afState: " + aeState);

                                mState = STATE_PICTURE_TAKEN;
                                captureStillPicture();
                            }
                            break;
                        }
                    }
                }

            };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {

    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(
            Size[] choices, int textureViewWidth,
            int textureViewHeight, int maxWidth, int maxHeight,
            Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setupCameraOutputs(int width, int height) {
        MLog.d(TAG, "setupCameraOutputs()" +
                " width: " + width +
                " height: " + height);
        int rotatedPreviewWidth = width;
        int rotatedPreviewHeight = height;

        CameraManager manager =
                (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        int screenWidth = displaySize.x;
        int screenHeight = displaySize.y;
        // screenWidth: 720 screenHeight: 1280
        MLog.d(TAG, "setupCameraOutputs()" +
                " screenWidth: " + screenWidth +
                " screenHeight: " + screenHeight);

        try {
            for (String cameraId : manager.getCameraIdList()) {
                MLog.d(TAG, "setupCameraOutputs()" +
                        " cameraId: " + cameraId);
                if (TextUtils.isEmpty(cameraId)) {
                    continue;
                }

                // 获取某个相机(摄像头特性)
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // 检查支持
                int deviceLevel = characteristics.get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {

                }

                // 获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }


                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) {
                    continue;
                }
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mFrontCameraId = cameraId;
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mBackCameraId = cameraId;
                    Range<Integer>[] fpsRanges = characteristics.get(
                            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                    Log.d(TAG, "setupCameraOutputs() fpsRanges: " + Arrays.toString(fpsRanges));
                    // 设置预览画面的帧率 视实际情况而定选择一个帧率范围
                    /*mPreviewRequestBuilder.set(
                            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[0]);*/
                }

                // 拍照时使用最大的宽高
                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        //Arrays.asList(map.getOutputSizes(TextureView.class)),// 不能这样使用
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                // ImageFormat.JPEG        largest.getWidth(): 3264 largest.getHeight(): 2448
                // ImageFormat.YV12        largest.getWidth(): 960  largest.getHeight(): 720
                // ImageFormat.YUV_420_888 largest.getWidth(): 960  largest.getHeight(): 720
                MLog.d(TAG, "setupCameraOutputs() " + printThis() +
                        " largest.getWidth(): " + largest.getWidth() +
                        " largest.getHeight(): " + largest.getHeight());

                /***
                 * 实时帧数据获取类
                 * 由于获取实时帧所以选用YV12或者YUV_420_888两个格式，暂时不采用JPEG格式
                 * 在真机显示的过程中,不同的数据格式所设置的width和height需要注意，否侧视频会很卡顿
                 * YV12:width 720， height 960
                 * YUV_420_888：width 720， height 960
                 * JPEG:获取帧数据不能用 ImageFormat.JPEG 格式，否则你会发现预览非常卡的，
                 * 因为渲染 JPEG 数据量过大，导致掉帧，所以预览帧请使用其他编码格式
                 *
                 * 输入相机的尺寸必须是相机支持的尺寸，这样画面才能不失真，TextureView输入相机的尺寸也是这个
                 */
                /*mImageReader = ImageReader.newInstance(
                        largest.getWidth(),
                        largest.getHeight(),
                        ImageFormat.YUV_420_888,
                        *//*maxImages*//*5);// ImageFormat.JPEG, 2
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener,
                        mBackgroundHandler);*/

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                // noinspection ConstantConditions
                mSensorOrientation = characteristics.get(
                        CameraCharacteristics.SENSOR_ORIENTATION);
                MLog.d(TAG, "setupCameraOutputs() " + printThis() +
                        " rotation: " + rotation +
                        " mSensorOrientation: " + mSensorOrientation);
                boolean swappedDimensions = false;
                // note2 displayRotation: 0 mSensorOrientation: 90
                switch (rotation) {
                    // 竖屏
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    // 横屏
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + rotation);
                        break;
                }

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    screenWidth = displaySize.y;
                    screenHeight = displaySize.x;
                }

                if (screenWidth > MAX_PREVIEW_WIDTH) {
                    screenWidth = MAX_PREVIEW_WIDTH;
                }

                if (screenHeight > MAX_PREVIEW_HEIGHT) {
                    screenHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(
                        map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth,
                        rotatedPreviewHeight,
                        screenWidth,
                        screenHeight,
                        largest);
                // mPreviewSize.getWidth(): 960 mPreviewSize.getHeight(): 720
                MLog.d(TAG, "setupCameraOutputs()" +
                        " mPreviewSize.getWidth(): " + mPreviewSize.getWidth() +
                        " mPreviewSize.getHeight(): " + mPreviewSize.getHeight());

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                /*int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // 横屏
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    // 竖屏
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }*/

                // Check if the flash is supported.
                Boolean available = characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                // return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            /*ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);*/
        }
    }

    /***
     * 如果视频显示需要角度旋转 用该函数进行角度转正(结果就是人看到的东西是正的)
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setupCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     *
     * 总结:
     * int rotation = getWindowManager().getDefaultDisplay().getRotation();
     * rotation(0) ---> rotation(1) 从0能到1
     * rotation(0) ---> rotation(3) 从0能到3
     * rotation(1) ---> rotation(0) 从1能到0
     * rotation(3) ---> rotation(0) 从3能到0
     * rotation(1) !--> rotation(3) 从1不能到3
     * rotation(3) !--> rotation(1) 从3不能到1
     * "0"的状态是手机竖屏,摄像头在上边.
     * "2"的状态是手机竖屏,摄像头在下边.
     * "1"的状态是手机横屏,摄像头在左边.
     * "3"的状态是手机横屏,摄像头在右边.
     * 现在遇到的问题:
     * "1"状态时,逆时针旋转90度(应该是"2"的状态),状态不发生变化,再次逆时针旋转90度(应该是"3"的状态),状态还是不发生变化.
     * "3"状态时,顺时针旋转90度(应该是"2"的状态),状态不发生变化,再次顺时针旋转90度(应该是"1"的状态),状态还是不发生变化.
     *
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Log.i(TAG, "configureTransform()" +
                " viewWidth: " + viewWidth +
                " viewHeight: " + viewHeight);

        if (null == mTextureView || null == mPreviewSize) {
            return;
        }

        //int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int rotation = 0;
        switch (curOrientation) {
            case 0:
                rotation = 0;
                break;
            case 90:
                rotation = 3;
                break;
            case 180:
                rotation = 2;
                break;
            case 270:
                rotation = 1;
                break;
            default:
                rotation = -1;
                break;
        }
        Log.i(TAG, "configureTransform() rotation: " + rotation);
        if (rotation < 0) {
            return;
        }

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(
                0, 0, viewWidth, viewHeight);
        // mPreviewSize.getWidth(): 960 mPreviewSize.getHeight(): 720
        RectF bufferRect = new RectF(
                0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        // TextureView的中心点坐标
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        float bufferCenterX = bufferRect.centerX();
        float bufferCenterY = bufferRect.centerY();
        Log.i(TAG, "configureTransform()" +
                " centerX: " + centerX +
                " centerY: " + centerY +
                " bufferCenterX: " + bufferCenterX +
                " bufferCenterY: " + bufferCenterY);
        switch (rotation) {
            case Surface.ROTATION_90: // 1
            case Surface.ROTATION_270:// 3
                bufferRect.offset(centerX - bufferCenterX, centerY - bufferCenterY);
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / mPreviewSize.getHeight(),
                        (float) viewWidth / mPreviewSize.getWidth());
                Log.i(TAG, "configureTransform()" +
                        " scale: " + scale);
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
                break;
            case Surface.ROTATION_0:  // 0
                matrix.postRotate(0, centerX, centerY);
                break;
            case Surface.ROTATION_180:// 2
                bufferRect.offset(bufferCenterX - centerX, bufferCenterY - centerY);
                matrix.setRectToRect(bufferRect, viewRect, Matrix.ScaleToFit.FILL);
                scale = Math.max(
                        (float) mPreviewSize.getHeight() / viewHeight,
                        (float) mPreviewSize.getWidth() / viewWidth);
                Log.i(TAG, "configureTransform()" +
                        " scale: " + scale);
                matrix.postScale(scale, scale, bufferCenterX, bufferCenterY);
                matrix.postRotate(90, bufferCenterX, bufferCenterY);
                break;
            default:
                break;
        }

        mTextureView.setTransform(matrix);
    }

    /**
     * Opens the camera specified by {@link}.
     */
    private void openCamera(int width, int height) {
        MLog.d(TAG, "openCamera()" +
                " width: " + width +
                " height: " + height);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //requestCameraPermission();
            return;
        }

        setupCameraOutputs(width, height);
        configureTransform(width, height);

        CameraManager manager =
                (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mBackCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            // assert texture != null;
            if (texture == null || !mTextureView.isAvailable()) {
                return;
            }

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(
                    mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            /***
             TEMPLATE_PREVIEW        创建一个适合于相机预览窗口的请求
             TEMPLATE_STILL_CAPTURE  创建适用于静态图像捕获的请求
             TEMPLATE_RECORD         创建适合录像的请求
             TEMPLATE_VIDEO_SNAPSHOT 在录制视频时创建适合静态图像捕获的请求
             */
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            //设置实时帧数据接收
            //mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(
                    //Arrays.asList(surface, mImageReader.getSurface()),
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(
                                CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;

                            try {
                                // 自动对焦
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(
                                        mPreviewRequest,
                                        // 如果想要拍照,那么绝不能设置为null
                                        // 如果单纯预览,那么可以设置为null
                                        mCaptureCallback,
                                        mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    },
                    mBackgroundHandler);
            // null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     * 将焦点锁定为静态图像捕获的第一步(对焦)
     */
    private void lockFocus() {
        if (DEBUG)
            MLog.d(TAG, "lockFocus(): " + printThis());
        try {
            // This is how to tell the camera to lock focus.
            // 请求等待焦点
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);

            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(
                    mPreviewRequestBuilder.build(),
                    // 这里好像可以设置为null
                    mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        if (DEBUG)
            MLog.d(TAG, "runPrecaptureSequence(): " + printThis());
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(
                    mPreviewRequestBuilder.build(),
                    mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        if (DEBUG)
            MLog.d(TAG, "captureStillPicture(): " + printThis());
        try {
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // 自动对焦
            captureBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            /***
             拍出来的照片需要旋转多少角度(由getOrientation(rotation)得到)后,
             才能达到照片的图像信息跟预览时的图像信息保持一致.
             */
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // CaptureRequest.JPEG_ORIENTATION的值是针对顺时针旋转而言的
            captureBuilder.set(
                    CaptureRequest.JPEG_ORIENTATION,
                    getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback =
                    new CameraCaptureSession.CaptureCallback() {

                        @Override
                        public void onCaptureCompleted(
                                CameraCaptureSession session,
                                CaptureRequest request,
                                TotalCaptureResult result) {
                            showToast("Saved: " + mFile);
                            unlockFocus();
                        }
                    };

            // 停止连续取景
            mCaptureSession.stopRepeating();
            // 捕获图片
            mCaptureSession.capture(
                    captureBuilder.build(),
                    CaptureCallback,
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     * 返回值的意思是拍出来的照片如果要跟预览画面一致,
     * 那么在拍照的时候需要旋转0, 90, 270, or 360这样的角度才能达到想要的效果.
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        int orientation = (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
        if (DEBUG)
            MLog.d(TAG, "getOrientation() " + printThis() +
                    " rotation: " + rotation +
                    " mSensorOrientation: " + mSensorOrientation +
                    " orientation: " + orientation);
        /***
         rotation的值一般在手机系统中定义好的,手机怎么拿就有不同的值.

         rotation: 0 mSensorOrientation: 90 orientation: 90(竖屏)
         意思就是说手机竖屏拍照时,
         需要一个90度的结果才能使得拍出来的照片的方向跟预览时的方向保持一致
         rotation: 1 mSensorOrientation: 90 orientation: 0 (横屏)
         意思就是说手机横屏拍照时,
         需要一个0度的结果才能使得拍出来的照片的方向跟预览时的方向保持一致
         rotation: 2 mSensorOrientation: 90 orientation: 270
         rotation: 3 mSensorOrientation: 90 orientation: 180

         mSensorOrientation: 90
         含义:
         "手机的自然方向"
         "摄像头的自然方向"
         90就是这两个"自然方向"的差值

         我们一般使用手机是用左手拿手机,手机的姿势是竖屏,摄像头在上面.
         这个时候的状态为"手机的自然方向"或者"屏幕的自然方向".
         但是摄像头的传感器方向("摄像头的自然方向":横屏,摄像头在左边)跟"屏幕的自然方向"
         不一定一致,得到的90就是说它们在角度上相差了90度(为逆时针的差值).
         也就是说"屏幕的自然方向"按逆时针旋转90度后它们才保持一致,
         这时拍出来的照片在视觉上是正常的.
         当它们两个的角度不一致(mSensorOrientation为0说明一致,不为0说明不一致)时进行拍照,
         那么需要把"摄像头的自然方向"(横屏,摄像头在左边)开始顺时针旋转一定的角度与"手机的自然方向"
         达到重合后,这样拍摄出来的照片在视觉上才是正常的.
         orientation的值就是这个需要"旋转一定的角度".
         */
        return orientation;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        if (DEBUG)
            MLog.d(TAG, "unlockFocus(): " + printThis());
        try {
            // Reset the auto-focus trigger
            // 重置自动对焦
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(
                    mPreviewRequestBuilder.build(),
                    // 这里好像可以设置为null
                    mCaptureCallback,
                    mBackgroundHandler);

            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            // 打开连续取景模式
            mCaptureSession.setRepeatingRequest(
                    mPreviewRequest,
                    // 如果想要拍照,那么绝不能设置为null
                    // 如果单纯预览,那么可以设置为null
                    mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /***
     开始预览时
     开始拍照时
     重新预览时
     */
    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final ImageReader mReader;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(ImageReader reader, File file) {
            mReader = reader;
            mFile = file;
            MLog.d(TAG, "ImageSaver(): " + mFile.getAbsolutePath());
        }

        @Override
        public void run() {
            if (mReader == null) {
                return;
            }
            // 获取最近一帧图像
            Image image = mReader.acquireLatestImage();
            if (image == null) {
                return;
            }
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            // buffer.capacity()
            // buffer.remaining()
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            image.close();
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static Camera2Fragment.ErrorDialog newInstance(String message) {
            Camera2Fragment.ErrorDialog dialog = new Camera2Fragment.ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    // 获取camera最佳预览尺寸
    //从底层拿camera支持的previewsize，完了和屏幕分辨率做差，diff最小的就是最佳预览分辨率
    private void getPreviewSize(String mCameraId) {
        CameraManager mCameraManager = null;
        try {
            int diffs = Integer.MAX_VALUE;
            WindowManager windowManager =
                    (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point screenResolution = new Point(display.getWidth(), display.getHeight());

            CameraCharacteristics props =
                    mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap configurationMap =
                    props.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] availablePreviewSizes =
                    configurationMap.getOutputSizes(SurfaceTexture.class);

            int bestPreviewWidth = 0;
            int bestPreviewHeight = 0;
            for (Size previewSize : availablePreviewSizes) {
                Log.v(TAG, " PreviewSizes = " + previewSize);
                int mCameraPreviewWidth = previewSize.getWidth();
                int mCameraPreviewHeight = previewSize.getHeight();
                int newDiffs =
                        Math.abs(mCameraPreviewWidth - screenResolution.x) +
                                Math.abs(mCameraPreviewHeight - screenResolution.y);
                Log.v(TAG, "newDiffs = " + newDiffs);

                if (newDiffs == 0) {
                    bestPreviewWidth = mCameraPreviewWidth;
                    bestPreviewHeight = mCameraPreviewHeight;
                    break;
                }
                if (diffs > newDiffs) {
                    bestPreviewWidth = mCameraPreviewWidth;
                    bestPreviewHeight = mCameraPreviewHeight;
                    diffs = newDiffs;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /***
     * {@link TextureView.SurfaceTextureListener}
     * handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        SurfaceTexture texture, int width, int height) {
                    MLog.d(TAG, "onSurfaceTextureAvailable()" +
                            " width: " + width +
                            " height: " + height);
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        SurfaceTexture texture, int width, int height) {
                    // 手机横竖屏切换时会回调
                    MLog.d(TAG, "onSurfaceTextureSizeChanged()" +
                            " width: " + width +
                            " height: " + height);
                    //configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(
                        SurfaceTexture texture) {
                    MLog.d(TAG, "onSurfaceTextureDestroyed()");
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(
                        SurfaceTexture texture) {
                }
            };

    /***
     * {@link CameraDevice.StateCallback} is called when
     * {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    MLog.i(TAG, "onOpened()");
                    // 打开摄像头
                    // This method is called when the camera is opened.
                    // We start camera preview here.
                    mCameraOpenCloseLock.release();
                    mCameraDevice = cameraDevice;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(CameraDevice cameraDevice) {
                    MLog.i(TAG, "onDisconnected()");
                    mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(CameraDevice cameraDevice, int error) {
                    MLog.i(TAG, "onError() error: " + error);
                    mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    mCameraDevice = null;
                    finish();
                }
            };

}
