package com.weidi.usefragments.business.video_player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.contents.Contents;
import com.weidi.usefragments.test_view.BubblePopupWindow;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.MLog;
import com.weidi.utils.MyToast;

import java.io.File;
import java.util.HashMap;

import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

public class PlayerWrapper {

    private static final String TAG = "player_alexander";

    public static final int PLAYBACK_PROGRESS_UPDATED = 200;
    private static final int MSG_ON_PROGRESS_UPDATED = 10;
    private static final int MSG_START_PLAYBACK = 11;
    private static final int MSG_SEEK_TO_ADD = 12;
    private static final int MSG_SEEK_TO_SUBTRACT = 13;
    private static final int MSG_DOWNLOAD = 14;

    public static final String PLAYBACK_ADDRESS = "playback_address";
    public static final String PLAYBACK_POSITION = "playback_position";
    public static final String PLAYBACK_ISLIVE = "playback_islive";
    // 是否正常结束
    public static final String PLAYBACK_NORMAL_FINISH = "playback_normal_finish";
    public static final String PLAYBACK_MEDIA_TYPE = "playback_media_type";

    private HashMap<String, Long> mPathTimeMap = new HashMap<>();

    private SharedPreferences mSP;
    private PowerManager.WakeLock mPowerWakeLock;
    private SurfaceHolder mSurfaceHolder;
    private FFMPEG mFFMPEGPlayer;
    private String mPath;
    // 有些mp3文件含有video,因此播放失败
    private String mType;
    private long mProgress;
    private long mPresentationTime;
    private int mDownloadProgress = -1;
    private long contentLength = -1;
    private boolean mNeedToSyncProgressBar = true;
    private boolean mIsScreenPress = false;
    private boolean mHasError = false;
    private boolean mIsSeparatedAudioVideo = false;
    private long mMediaDuration;
    private boolean mIsLocal = true;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mRootView;

    private SurfaceView mSurfaceView;
    private LinearLayout mControllerPanelLayout;
    private ProgressBar mLoadingView;
    private SeekBar mProgressBar;
    private TextView mFileNameTV;
    private TextView mProgressTimeTV;
    private TextView mDurationTimeTV;
    private ImageButton mPreviousIB;
    private ImageButton mPlayIB;
    private ImageButton mPauseIB;
    private ImageButton mNextIB;
    private ImageButton mPreviousIB2;
    // 声音
    private ImageButton mVolumeNormal;
    private ImageButton mVolumeMute;
    // 下载
    private ImageButton mDownloadIB;//download_btn
    private boolean mIsDownloading = false;

    // 跟气泡相关
    private LayoutInflater mLayoutInflater;
    private View mBubbleView;
    private TextView mShowTimeTV;// 气泡上显示时间
    private BubblePopupWindow mBubblePopupWindow;

    private Handler mUiHandler;
    // 屏幕的宽高
    // 竖屏:width = 1080 height = 2244
    // 横屏:width = 2244 height = 1080
    private int mScreenWidth;
    private int mScreenHeight;
    // 视频源的宽高
    private int mVideoWidth;
    private int mVideoHeight;
    // 想要的宽高
    private int mNeedVideoWidth;
    private int mNeedVideoHeight;
    // 控制面板的高度
    private int mControllerPanelLayoutHeight;

    private Context mContext;
    private JniPlayerActivity mActivity;
    private PlayerService mService;

    private boolean mIsPhoneDevice;
    private boolean mIsPortraitScreen;

    // 必须首先被调用
    public void setActivity(Activity activity, Service service) {
        mActivity = null;
        mService = null;
        if (activity != null) {
            if (!(activity instanceof JniPlayerActivity)) {
                return;
            }
            JniPlayerActivity jniPlayerActivity = (JniPlayerActivity) activity;
            mActivity = jniPlayerActivity;
            mContext = jniPlayerActivity.getApplicationContext();
            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

            mSurfaceView = jniPlayerActivity.mSurfaceView;
            mControllerPanelLayout = jniPlayerActivity.mControllerPanelLayout;
            mLoadingView = jniPlayerActivity.mLoadingView;
            mProgressBar = jniPlayerActivity.mProgressBar;
            mFileNameTV = jniPlayerActivity.mFileNameTV;
            mProgressTimeTV = jniPlayerActivity.mProgressTimeTV;
            mDurationTimeTV = jniPlayerActivity.mDurationTimeTV;
            mPreviousIB = jniPlayerActivity.mPreviousIB;
            mPlayIB = jniPlayerActivity.mPlayIB;
            mPauseIB = jniPlayerActivity.mPauseIB;
            mNextIB = jniPlayerActivity.mNextIB;
        } else if (service != null) {
            if (!(service instanceof PlayerService)) {
                return;
            }
            PlayerService playerService = (PlayerService) service;
            mService = playerService;
            mContext = playerService.getApplicationContext();

            mWindowManager = playerService.mWindowManager;
            mLayoutParams = playerService.mLayoutParams;
            mRootView = playerService.mRootView;
            mRootView.setOnTouchListener(new PlayerOnTouchListener());

            mSurfaceView = mRootView.findViewById(R.id.surfaceView);
            mControllerPanelLayout = mRootView.findViewById(R.id.controller_panel_layout);
            mLoadingView = mRootView.findViewById(R.id.loading_view);
            mProgressBar = mRootView.findViewById(R.id.progress_bar);
            mFileNameTV = mRootView.findViewById(R.id.file_name_tv);
            mProgressTimeTV = mRootView.findViewById(R.id.progress_time_tv);
            mDurationTimeTV = mRootView.findViewById(R.id.duration_time_tv);
            mPreviousIB = mRootView.findViewById(R.id.button_prev);
            mPlayIB = mRootView.findViewById(R.id.button_play);
            mPauseIB = mRootView.findViewById(R.id.button_pause);
            mNextIB = mRootView.findViewById(R.id.button_next);

            mPreviousIB2 = mRootView.findViewById(R.id.button_prev2);
            mDownloadIB = mRootView.findViewById(R.id.download_btn);
            mVolumeNormal = mRootView.findViewById(R.id.volume_normal);
            mVolumeMute = mRootView.findViewById(R.id.volume_mute);

            mPreviousIB2.setVisibility(View.VISIBLE);
            mVolumeNormal.setVisibility(View.VISIBLE);
            mVolumeMute.setVisibility(View.GONE);

            mSurfaceView.setOnClickListener(mOnClickListener);
            mPreviousIB.setOnClickListener(mOnClickListener);
            mPlayIB.setOnClickListener(mOnClickListener);
            mPauseIB.setOnClickListener(mOnClickListener);
            mNextIB.setOnClickListener(mOnClickListener);
            mPreviousIB2.setOnClickListener(mOnClickListener);
            mDownloadIB.setOnClickListener(mOnClickListener);
            mVolumeNormal.setOnClickListener(mOnClickListener);
            mVolumeMute.setOnClickListener(mOnClickListener);
        }
    }

    public void setDataSource(String path) {
        mPath = path;
        mIsLocal = true;
        if (!TextUtils.isEmpty(mPath)) {
            String newPath = mPath.toLowerCase();
            if (newPath.startsWith("http://") || newPath.startsWith("https://")) {
                mIsLocal = false;
            }
        }
    }

    public void setType(String type) {
        mType = type;
    }

    public Handler getUiHandler() {
        return mUiHandler;
    }

    public void onCreate() {
        EventBusUtils.register(this);
        mSP = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        mIsPhoneDevice = isPhoneDevice();

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerWrapper.this.uiHandleMessage(msg);
            }
        };

        mLayoutInflater = LayoutInflater.from(mContext);
        mBubbleView = mLayoutInflater.inflate(R.layout.layout_popup_view, null);
        mShowTimeTV = mBubbleView.findViewById(R.id.content_tv);
        mBubblePopupWindow = new BubblePopupWindow(mContext);
        mBubblePopupWindow.setBubbleView(mBubbleView);
        mShowTimeTV.setOnClickListener(mOnClickListener);

        if (mFFMPEGPlayer == null) {
            mFFMPEGPlayer = FFMPEG.getDefault();
        }
        int duration = (int) mMediaDuration;
        int currentPosition = (int) mPresentationTime;
        float pos = (float) currentPosition / duration;
        int target = Math.round(pos * mProgressBar.getMax());
        mProgressBar.setProgress(target);
        mProgressBar.setSecondaryProgress(0);
        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (fromTouch) {
                    // 得到的是秒
                    long tempProgress =
                            (long) ((progress / 3840.00) * mMediaDuration);
                    mProgress = tempProgress;
                    String elapsedTime =
                            DateUtils.formatElapsedTime(tempProgress);
                    mShowTimeTV.setText(elapsedTime);
                    /*mUiHandler.removeMessages(PLAYBACK_PROGRESS_CHANGED);
                    mUiHandler.sendEmptyMessageDelayed(PLAYBACK_PROGRESS_CHANGED, 50);*/
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mProgressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 滑动SeekBar时进度条不要更新,滑动结束后再更新
                        mNeedToSyncProgressBar = false;
                        mShowTimeTV.setText("");
                        mBubblePopupWindow.show(mProgressBar);
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    // 调用之前,视频路径先设置好
    @SuppressLint("InvalidWakeLockTag")
    public void onResume() {
        MLog.i(TAG, "onResume()");
        if (mPowerWakeLock == null) {
            // When player view started,wake the lock.
            PowerManager powerManager =
                    (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mPowerWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            mPowerWakeLock.acquire();
        }

        if (mSurfaceHolder == null) {
            mSurfaceHolder = mSurfaceView.getHolder();
            // 没有图像出来,就是由于没有设置PixelFormat.RGBA_8888
            // 这里要写
            mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
            mSurfaceHolder.addCallback(mSurfaceCallback);
        }
    }

    public void onPause() {
        if (mPowerWakeLock != null && mPowerWakeLock.isHeld()) {
            mPowerWakeLock.release();
            mPowerWakeLock = null;
        }
    }

    public void onStop() {

    }

    public void onDestroy() {
        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.releaseAll();
            mFFMPEGPlayer = null;
        }
        EventBusUtils.unregister(this);
    }

    public void onRelease() {
        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.releaseAll();
        }
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case Callback.MSG_ON_READY:
                mDurationTimeTV.setText("00:00:00");
                mProgressBar.setProgress(0);
                if (mService != null) {
                    mVolumeNormal.setVisibility(View.VISIBLE);
                    mVolumeMute.setVisibility(View.GONE);
                }
                String title;
                if (mIsLocal) {
                    title = mPath.substring(
                            mPath.lastIndexOf("/") + 1, mPath.lastIndexOf("."));
                } else {
                    title = Contents.getTitle(mPath);
                }
                if (!TextUtils.isEmpty(title)) {
                    mFileNameTV.setText(title);
                } else {
                    mFileNameTV.setText("");
                }
                if (!mIsLocal) {
                    mLoadingView.setVisibility(View.VISIBLE);
                }
                if (TextUtils.isEmpty(mType)
                        || mType.startsWith("video/")) {
                    mControllerPanelLayout.setVisibility(View.INVISIBLE);
                } else if (mType.startsWith("audio/")) {
                    mControllerPanelLayout.setVisibility(View.VISIBLE);
                }
                break;
            case Callback.MSG_ON_CHANGE_WINDOW:
                // 视频宽高
                mVideoWidth = msg.arg1;
                mVideoHeight = msg.arg2;
                MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW videoWidth: " +
                        mVideoWidth + " videoHeight: " + mVideoHeight);
                mMediaDuration = mFFMPEGPlayer.getDuration();
                String durationTime = DateUtils.formatElapsedTime(mMediaDuration);
                mDurationTimeTV.setText(durationTime);

                if (mIsPhoneDevice) {
                    MLog.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW 手机");
                    if (mContext.getResources().getConfiguration().orientation
                            == Configuration.ORIENTATION_LANDSCAPE) {
                        if (JniPlayerActivity.isAliveJniPlayerActivity) {
                            handleLandscapeScreen(0);
                        } else {
                            handleLandscapeScreen(1);
                        }
                    } else {
                        handlePortraitScreen();
                    }
                } else {
                    MLog.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW 电视机");
                    handlePortraitScreen2();
                }

                if (TextUtils.isEmpty(mType)
                        || mType.startsWith("video/")) {
                    SharedPreferences.Editor edit = mSP.edit();
                    // 保存播放地址
                    edit.putString(PLAYBACK_ADDRESS, mPath);
                    // 开始播放设置为false,表示初始化状态
                    edit.putBoolean(PLAYBACK_NORMAL_FINISH, false);
                    edit.putString(PLAYBACK_MEDIA_TYPE, mType);
                    edit.commit();
                }
                break;
            case Callback.MSG_ON_PLAYED:
                mPlayIB.setVisibility(View.VISIBLE);
                mPauseIB.setVisibility(View.GONE);
                if (!mIsLocal) {
                    mLoadingView.setVisibility(View.GONE);
                }

                /*if (mVideoWidth != 0 && mVideoHeight != 0) {
                    mControllerPanelLayout.setVisibility(View.GONE);
                } else {
                    mControllerPanelLayout.setVisibility(View.VISIBLE);
                }*/

                /*if (mContext.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE) {
                    mControllerPanelLayout.setVisibility(View.GONE);// INVISIBLE
                } else {
                    mControllerPanelLayout.setVisibility(View.VISIBLE);// INVISIBLE
                }*/
                break;
            case Callback.MSG_ON_PAUSED:
                mPlayIB.setVisibility(View.GONE);
                mPauseIB.setVisibility(View.VISIBLE);
                if (!mIsLocal) {
                    mLoadingView.setVisibility(View.VISIBLE);
                }
                //mControllerPanelLayout.setVisibility(View.GONE);
                break;
            case Callback.MSG_ON_FINISHED:
                if (mHasError) {
                    mHasError = false;
                    // 重新开始播放
                    startPlayback();
                } else {
                    MyToast.show("Safe Exit");
                    // 播放结束
                    if (mService != null) {
                        if (!mService.needToPlaybackOtherVideo()) {
                            mService.removeView();
                            if (mSurfaceHolder != null) {
                                mSurfaceHolder.removeCallback(mSurfaceCallback);
                                mSurfaceHolder = null;
                            }
                        }
                    } else if (mActivity != null) {
                        mActivity.finish();
                        mActivity.exitActivity();
                        if (mSurfaceHolder != null) {
                            mSurfaceHolder.removeCallback(mSurfaceCallback);
                            mSurfaceHolder = null;
                        }
                    }
                }
                if (TextUtils.isEmpty(mType)
                        || mType.startsWith("video/")) {
                    // 正常结束设置为true
                    mSP.edit().putBoolean(PLAYBACK_NORMAL_FINISH, true).commit();
                }
                break;
            case Callback.MSG_ON_INFO:
                break;
            case Callback.MSG_ON_ERROR:
                mHasError = false;
                int error = msg.arg1;
                String errorInfo = null;
                if (msg.obj != null) {
                    errorInfo = (String) msg.obj;
                }
                switch (error) {
                    case Callback.ERROR_TIME_OUT:
                    case Callback.ERROR_DATA_EXCEPTION:
                        // 需要重新播放
                        mHasError = true;
                        MLog.e(TAG, "PlayerWrapper Callback.MSG_ON_ERROR errorInfo: " + errorInfo);
                        break;
                    case Callback.ERROR_FFMPEG_INIT:
                        MLog.e(TAG,
                                "PlayerWrapper Callback.ERROR_FFMPEG_INIT errorInfo: " + errorInfo);
                        MyToast.show("音视频初始化失败");
                        // 不需要重新播放
                        if (mService != null) {
                            MLog.i(TAG,
                                    "PlayerWrapper Callback.ERROR_FFMPEG_INIT " +
                                            "mService.removeView()");
                            mService.removeView();
                            if (mSurfaceHolder != null) {
                                mSurfaceHolder.removeCallback(mSurfaceCallback);
                                mSurfaceHolder = null;
                            }
                        } else if (mActivity != null) {
                            mActivity.finish();
                            //mActivity.exitActivity();
                            if (mSurfaceHolder != null) {
                                mSurfaceHolder.removeCallback(mSurfaceCallback);
                                mSurfaceHolder = null;
                            }
                        }
                        break;
                    default:
                        break;
                }
                break;
            case Callback.MSG_ON_PROGRESS_UPDATED:
                if (msg.obj == null) {
                    return;
                }

                mPresentationTime = (Long) msg.obj;// 秒
                String curElapsedTime = DateUtils.formatElapsedTime(mPresentationTime);
                mProgressTimeTV.setText(curElapsedTime);

                if (mNeedToSyncProgressBar && mMediaDuration > 0) {
                    int currentPosition = (int) (mPresentationTime);
                    float pos = (float) currentPosition / mMediaDuration;
                    int target = Math.round(pos * mProgressBar.getMax());
                    mProgressBar.setProgress(target);
                }

                if (mMediaDuration > 0) {
                    if (mPresentationTime < mMediaDuration) {
                        mPathTimeMap.put(mPath, mPresentationTime);
                    } else {
                        mPathTimeMap.remove(mPath);
                    }
                    //MLog.d(TAG, "Callback.MSG_ON_PROGRESS_UPDATED "+mPathTimeMap.size());
                }
                //mSP.edit().putLong(PLAYBACK_POSITION, mPresentationTime).commit();
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:// 单击事件
                if (clickCounts > NEED_CLICK_COUNTS) {
                    clickCounts = NEED_CLICK_COUNTS;
                }
                switch (clickCounts) {
                    case 1:
                        //MLog.d(TAG, "onKeyDown() 1");
                        clickOne();
                        break;
                    case 2:
                        //MLog.d(TAG, "onKeyDown() 2");
                        clickTwo();
                        break;
                    case 3:
                        //MLog.d(TAG, "onKeyDown() 3");
                        clickThree();
                        break;
                    case 4:
                        //MLog.d(TAG, "onKeyDown() 4");
                        clickFour();
                        break;
                    case 5:
                        break;
                    case 6:
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    case 9:
                        break;
                    case 10:
                        break;
                    default:
                        break;
                }

                clickCounts = 0;
                mNeedToSyncProgressBar = true;
                break;
            case MSG_START_PLAYBACK:
                ThreadPool.getFixedThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        mFFMPEGPlayer.audioHandleData();
                    }
                });
                ThreadPool.getFixedThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        mFFMPEGPlayer.videoHandleData();
                    }
                });

                if (mIsSeparatedAudioVideo) {
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(500);
                            mFFMPEGPlayer.readData();
                        }
                    });
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(1000);
                            mFFMPEGPlayer.readData();
                        }
                    });
                }
                break;
            case MSG_SEEK_TO_ADD:
                if (mFFMPEGPlayer != null) {
                    mFFMPEGPlayer.stepAdd(addStep);
                }
                addStep = 0;
                break;
            case MSG_SEEK_TO_SUBTRACT:
                if (mFFMPEGPlayer != null) {
                    mFFMPEGPlayer.stepSubtract(subtractStep);
                }
                subtractStep = 0;
                break;
            case PLAYBACK_PROGRESS_UPDATED:
                mProgressBar.setSecondaryProgress(mDownloadProgress);
                break;
            case MSG_DOWNLOAD:
                if (!mIsDownloading) {
                    mIsDownloading = true;
                    mDownloadIB.setImageResource(R.drawable.download2);
                } else {
                    mIsDownloading = false;
                    mDownloadIB.setImageResource(R.drawable.download1);
                }
                break;
            default:
                break;
        }
    }

    public void startPlayback() {
        MLog.d(TAG, "startPlayback()");
        mVolumeNormal.setVisibility(View.VISIBLE);
        mVolumeMute.setVisibility(View.GONE);
        if (mSurfaceHolder == null) {
            mSurfaceHolder = mSurfaceView.getHolder();
        }
        // 这里也要写
        mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
        // 底层有关参数的设置
        mFFMPEGPlayer.setHandler(mUiHandler);
        mFFMPEGPlayer.setCallback(mFFMPEGPlayer.mCallback);
        // mFFMPEGPlayer.setSurface(mPath, mSurfaceHolder.getSurface());

        // 开启线程初始化ffmpeg
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                mIsSeparatedAudioVideo = false;
                String tempPath = "";
                MLog.d(TAG, "startPlayback()                  mPath: " + mPath);

                if (mPathTimeMap.containsKey(mPath)) {
                    long position = mPathTimeMap.get(mPath);
                    MLog.d(TAG, "startPlayback()               position: " + position);
                    mFFMPEGPlayer.seekTo(position);
                }

                if (mPath.endsWith(".m4s")) {
                    tempPath = mPath.substring(0, mPath.lastIndexOf("/"));
                    File audioFile = new File(tempPath + "/audio.m4s");
                    File videoFile = new File(tempPath + "/video.m4s");
                    MLog.d(TAG,
                            "startPlayback()                  audio: " + audioFile.getAbsolutePath());
                    MLog.d(TAG,
                            "startPlayback()                  video: " + videoFile.getAbsolutePath());
                    if (audioFile.exists() && videoFile.exists()) {
                        mIsSeparatedAudioVideo = true;
                    }
                }

                MLog.d(TAG, "startPlayback() mIsSeparatedAudioVideo: " + mIsSeparatedAudioVideo);
                if (!mIsSeparatedAudioVideo) {
                    if (TextUtils.isEmpty(mType)
                            || mType.startsWith("video/")) {
                        mFFMPEGPlayer.setMode(FFMPEG.USE_MODE_MEDIA);
                    } else if (mType.startsWith("audio/")) {
                        mFFMPEGPlayer.setMode(FFMPEG.USE_MODE_ONLY_AUDIO);
                    }
                    mFFMPEGPlayer.setSurface(mPath, mSurfaceHolder.getSurface());
                } else {
                    mFFMPEGPlayer.setMode(FFMPEG.USE_MODE_AUDIO_VIDEO);
                    mFFMPEGPlayer.setSurface(tempPath, mSurfaceHolder.getSurface());
                }

                if (mFFMPEGPlayer.initPlayer() != 0) {
                    // 不在这里做事了.遇到error会从底层回调到java端的
                    //MyToast.show("音视频初始化失败");
                    //mUiHandler.removeMessages(Callback.MSG_ON_ERROR);
                    //mUiHandler.sendEmptyMessage(Callback.MSG_ON_ERROR);
                    return;
                }

                if (TextUtils.isEmpty(mType)
                        || mType.startsWith("video/")) {
                    MyToast.show("音视频初始化成功");
                }
                mUiHandler.removeMessages(MSG_START_PLAYBACK);
                mUiHandler.sendEmptyMessage(MSG_START_PLAYBACK);
                if (!mIsSeparatedAudioVideo) {
                    SystemClock.sleep(500);
                    mFFMPEGPlayer.readData();
                }
            }
        });
    }

    // 执行全屏和取消全屏的方法
    private void setFullscreen(Activity context, boolean fullscreen) {
        Window window = context.getWindow();
        WindowManager.LayoutParams winParams = window.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (fullscreen) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        window.setAttributes(winParams);
    }

    private int getStatusBarHeight() {
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android");
        int height = resources.getDimensionPixelSize(resourceId);
        // getStatusBarHeight() height: 48 95
        MLog.d(TAG, "getStatusBarHeight() height: " + height);
        return height;
    }

    // 处理横屏
    @SuppressLint("SourceLockedOrientationActivity")
    public void handleLandscapeScreen(int statusBarHeight) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }*/

        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handleLandscapeScreen");
        if (statusBarHeight != 0) {
            statusBarHeight = getStatusBarHeight();
        }
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW statusBarHeight: " + statusBarHeight);

        // mScreenWidth: 2149 mScreenHeight: 1080
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);
        mControllerPanelLayout.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        relativeParams.width = mScreenWidth;
        if (statusBarHeight != 0) {
            relativeParams.height = mScreenHeight - statusBarHeight;
        } else {
            relativeParams.height = mScreenHeight;
        }
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);
        mSurfaceView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        //frameParams.setMargins(0, (mScreenHeight - mControllerPanelLayoutHeight - 150), 0, 0);
        frameParams.setMargins(0, 120, 0, 0);
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (statusBarHeight != 0) {
                updateRootViewLayout(mScreenWidth, mScreenHeight - statusBarHeight);
            } else {
                updateRootViewLayout(mScreenWidth, mScreenHeight);
            }
        }
    }

    // 处理竖屏
    public void handlePortraitScreen() {
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreen");

        int pauseRlHeight = 0;
        if (mService != null) {
            RelativeLayout pause_rl = mRootView.findViewById(R.id.pause_rl);
            pauseRlHeight = pause_rl.getHeight();
            SeekBar progress_bar = mRootView.findViewById(R.id.progress_bar);
            RelativeLayout show_time_rl = mRootView.findViewById(R.id.show_time_rl);
            ImageButton button_prev = mRootView.findViewById(R.id.button_prev);
            ImageButton button_next = mRootView.findViewById(R.id.button_next);
            if (mMediaDuration <= 0) {
                progress_bar.setVisibility(View.GONE);
                show_time_rl.setVisibility(View.GONE);
                button_prev.setVisibility(View.INVISIBLE);
                button_next.setVisibility(View.INVISIBLE);
            } else {
                progress_bar.setVisibility(View.VISIBLE);
                show_time_rl.setVisibility(View.VISIBLE);
                button_prev.setVisibility(View.VISIBLE);
                button_next.setVisibility(View.VISIBLE);
            }
        }
        //mControllerPanelLayout.setVisibility(View.VISIBLE);

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
            if (mNeedVideoHeight > mScreenHeight) {
                mNeedVideoHeight = mScreenHeight;
            }
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = 1;
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));
        }

        /*if (mVideoWidth >= mScreenWidth) {
            relativeParams.setMargins(0, 0, 0, 0);
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
            if (mNeedVideoHeight > mScreenHeight) {
                mNeedVideoHeight = mScreenHeight;
            }
        } else {
            relativeParams.setMargins((mScreenWidth - mVideoWidth) / 2, 0, 0, 0);
            mNeedVideoWidth = mVideoWidth;
            mNeedVideoHeight = mVideoHeight;
            if (mNeedVideoHeight > mScreenHeight) {
                mNeedVideoHeight = mScreenHeight;
            }
        }*/
        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);
        mSurfaceView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
            //frameParams.setMargins(0, (int) (mScreenHeight / 2), 0, 0);
            frameParams.setMargins(0, getStatusBarHeight(), 0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            frameParams.setMargins(0, mNeedVideoHeight, 0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));
            /*if (mMediaDuration > 0) {
                mControllerPanelLayout.setBackgroundColor(
                        mContext.getResources().getColor(R.color.lightgray));
            } else {
                mControllerPanelLayout.setBackgroundColor(
                        mContext.getResources().getColor(android.R.color.transparent));
            }*/
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                    updateRootViewLayout(mScreenWidth, mNeedVideoHeight);
                } else {
                    if (mMediaDuration < 0) {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + pauseRlHeight);
                    } else {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight);
                    }
                }
            } else {
                updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1);
            }
        }
    }

    // 电视机专用
    public void handlePortraitScreen2() {
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreen2");

        int pauseRlHeight = 0;
        if (mService != null) {
            RelativeLayout pause_rl = mRootView.findViewById(R.id.pause_rl);
            pauseRlHeight = pause_rl.getHeight();
            SeekBar progress_bar = mRootView.findViewById(R.id.progress_bar);
            RelativeLayout show_time_rl = mRootView.findViewById(R.id.show_time_rl);
            ImageButton button_prev = mRootView.findViewById(R.id.button_prev);
            ImageButton button_next = mRootView.findViewById(R.id.button_next);
            if (mMediaDuration <= 0) {
                progress_bar.setVisibility(View.GONE);
                show_time_rl.setVisibility(View.GONE);
                button_prev.setVisibility(View.INVISIBLE);
                button_next.setVisibility(View.INVISIBLE);
            } else {
                progress_bar.setVisibility(View.VISIBLE);
                show_time_rl.setVisibility(View.VISIBLE);
                button_prev.setVisibility(View.VISIBLE);
                button_next.setVisibility(View.VISIBLE);
            }
        }

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        mScreenWidth = mScreenWidth / 3;

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
            if (mNeedVideoHeight > mScreenHeight) {
                mNeedVideoHeight = mScreenHeight;
            }
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = 1;
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));
        }

        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);
        mSurfaceView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
            //frameParams.setMargins(0, (int) (mScreenHeight / 2), 0, 0);
            frameParams.setMargins(0, getStatusBarHeight(), 0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            frameParams.setMargins(0, mNeedVideoHeight, 0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                    updateRootViewLayout(mScreenWidth, mNeedVideoHeight);
                } else {
                    if (mMediaDuration < 0) {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + pauseRlHeight);
                    } else {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight);
                    }
                }
            } else {
                updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1);
            }
        }
    }

    private void updateRootViewLayout(int width, int height) {
        if (mService != null && mService.mIsAddedView) {
            mLayoutParams.width = width;
            mLayoutParams.height = height;
            mLayoutParams.x = 0;
            mLayoutParams.y = 0;
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        }
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(
                SurfaceHolder holder) {
            MLog.d(TAG, "surfaceCreated()");
            if (mFFMPEGPlayer == null) {
                return;
            }

            startPlayback();
        }

        @Override
        public void surfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            MLog.d(TAG, "surfaceChanged() width: " + width + " height: " + height);
        }

        @Override
        public void surfaceDestroyed(
                SurfaceHolder holder) {
            MLog.d(TAG, "surfaceDestroyed()");
            if (mFFMPEGPlayer != null) {
                mFFMPEGPlayer.releaseAll();
            }
        }
    };

    private long addStep = 0;
    private long subtractStep = 0;
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_prev:
                    mNeedToSyncProgressBar = true;
                    if (mFFMPEGPlayer != null) {
                        if (mMediaDuration > 300) {
                            subtractStep += 30;
                        } else {
                            subtractStep += 10;
                        }
                        MLog.d(TAG, "onClick() subtractStep: " + subtractStep);
                        mUiHandler.removeMessages(MSG_SEEK_TO_SUBTRACT);
                        mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_SUBTRACT, 500);
                    }
                    break;
                case R.id.button_play:
                    mNeedToSyncProgressBar = true;
                    if (mFFMPEGPlayer != null) {
                        if (mFFMPEGPlayer.isRunning()) {
                            if (mFFMPEGPlayer.isPlaying()) {
                                mPlayIB.setVisibility(View.GONE);
                                mPauseIB.setVisibility(View.VISIBLE);
                                mFFMPEGPlayer.pause();
                            }
                        }
                    }
                    break;
                case R.id.button_pause:
                    mNeedToSyncProgressBar = true;
                    if (mFFMPEGPlayer != null) {
                        if (!mFFMPEGPlayer.isPlaying()) {
                            mPlayIB.setVisibility(View.VISIBLE);
                            mPauseIB.setVisibility(View.GONE);
                            mFFMPEGPlayer.play();
                        }
                    }
                    break;
                case R.id.button_next:
                    mNeedToSyncProgressBar = true;
                    if (mFFMPEGPlayer != null) {
                        if (mMediaDuration > 300) {
                            addStep += 30;
                        } else {
                            addStep += 10;
                        }
                        MLog.d(TAG, "onClick() addStep: " + addStep);
                        mUiHandler.removeMessages(MSG_SEEK_TO_ADD);
                        mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_ADD, 500);
                    }
                    break;
                case R.id.surfaceView:
                    if (!mNeedToSyncProgressBar) {
                        mNeedToSyncProgressBar = true;
                        return;
                    }
                    mIsScreenPress = true;
                    onEvent(KeyEvent.KEYCODE_HEADSETHOOK, null);
                    break;
                case R.id.content_tv:
                    mNeedToSyncProgressBar = true;
                    mBubblePopupWindow.dismiss();
                    MLog.d(TAG, "onClick() mProgress: " + mProgress +
                            " " + DateUtils.formatElapsedTime(mProgress));
                    if (mProgress >= 0 && mProgress <= mMediaDuration) {
                        mFFMPEGPlayer.seekTo(mProgress);
                    }
                    break;
                case R.id.button_prev2:
                    mNeedToSyncProgressBar = true;
                    if (mService != null) {
                        mService.removeView();
                    }
                    break;
                case R.id.volume_normal:
                    mNeedToSyncProgressBar = true;
                    mVolumeNormal.setVisibility(View.GONE);
                    mVolumeMute.setVisibility(View.VISIBLE);
                    mFFMPEGPlayer.setVolume(FFMPEG.VOLUME_MUTE);
                    break;
                case R.id.volume_mute:
                    mNeedToSyncProgressBar = true;
                    mVolumeNormal.setVisibility(View.VISIBLE);
                    mVolumeMute.setVisibility(View.GONE);
                    mFFMPEGPlayer.setVolume(FFMPEG.VOLUME_NORMAL);
                    break;
                case R.id.download_btn:
                    mUiHandler.removeMessages(MSG_DOWNLOAD);
                    mUiHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD, 500);
                    break;
                default:
                    break;
            }
        }
    };

    private void clickOne() {
        if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                mControllerPanelLayout.setVisibility(View.GONE);
            }
        } else {
            mControllerPanelLayout.setVisibility(View.VISIBLE);
        }
    }

    private void clickTwo() {
        // 播放与暂停
        if (mFFMPEGPlayer != null) {
            if (mFFMPEGPlayer.isRunning()) {
                if (mFFMPEGPlayer.isPlaying()) {
                    mPlayIB.setVisibility(View.GONE);
                    mPauseIB.setVisibility(View.VISIBLE);
                    mFFMPEGPlayer.pause();
                } else {
                    mPlayIB.setVisibility(View.VISIBLE);
                    mPauseIB.setVisibility(View.GONE);
                    mFFMPEGPlayer.play();
                }
            }
        }
    }

    private void clickThree() {

    }

    private boolean handleLandscapeScreenFlag = false;

    @SuppressLint("SourceLockedOrientationActivity")
    private void clickFour() {
        if (mIsPhoneDevice) {
            if (mService != null) {
                // 如果当前是横屏
                if (mContext.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE) {
                    MLog.d(TAG, "onKeyDown() 4 横屏");
                    // 强制横屏
                    // 执行全屏操作
                    // 重新计算mScreenWidth和mScreenHeight的值

                    if (JniPlayerActivity.isAliveJniPlayerActivity) {
                        handleLandscapeScreen(0);
                    } else {
                        if (handleLandscapeScreenFlag) {
                            handleLandscapeScreenFlag = false;
                            handleLandscapeScreen(1);
                        } else {
                            handleLandscapeScreenFlag = true;
                            handleLandscapeScreen(0);
                        }
                    }
                } else if (mContext.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_PORTRAIT) {
                    MLog.d(TAG, "onKeyDown() 4 竖屏");
                    // 强制竖屏
                    // 取消全屏操作
                    // 重新计算mScreenWidth和mScreenHeight的值

                    handlePortraitScreen();
                }
            } else if (mActivity != null) {
                // 如果当前是横屏
                if (mContext.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE) {
                    // 强制竖屏
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    // 取消全屏操作
                    setFullscreen(mActivity, false);
                } else if (mContext.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_PORTRAIT) {
                    // 强制横屏
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    // 执行全屏操作
                    setFullscreen(mActivity, true);
                }
            }
        } else {
            // 电视机
            if (handleLandscapeScreenFlag) {
                handleLandscapeScreenFlag = false;
                handleLandscapeScreen(0);
            } else {
                handleLandscapeScreenFlag = true;
                handlePortraitScreen2();
            }
        }
    }

    private boolean isPhoneDevice() {
        boolean isPhoneDevice = true;
        UiModeManager uiModeManager =
                (UiModeManager) mContext.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_TELEVISION) {
            isPhoneDevice = true;
        } else {
            isPhoneDevice = false;
        }
        return isPhoneDevice;
    }

    public void pausePlayerWithTelephonyCall() {
        if (mFFMPEGPlayer != null) {
            if (mFFMPEGPlayer.isRunning()) {
                if (mFFMPEGPlayer.isPlaying()) {
                    mPlayIB.setVisibility(View.GONE);
                    mPauseIB.setVisibility(View.VISIBLE);
                    mFFMPEGPlayer.pause();
                }
            }
        }
    }

    private static final int NEED_CLICK_COUNTS = 4;
    private int clickCounts = 0;

    public Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                ++clickCounts;

                // 单位时间内按1次,2次,3次分别实现单击,双击,三击
                mUiHandler.removeMessages(KeyEvent.KEYCODE_HEADSETHOOK);
                mUiHandler.sendEmptyMessageDelayed(KeyEvent.KEYCODE_HEADSETHOOK, 300);
                break;
            default:
                break;
        }
        return result;
    }

    private class PlayerOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    mLayoutParams.x = mLayoutParams.x + movedX;
                    mLayoutParams.y = mLayoutParams.y + movedY;

                    // 更新悬浮窗控件布局
                    mWindowManager.updateViewLayout(view, mLayoutParams);
                    break;
                default:
                    break;
            }
            return true;
        }
    }

}
