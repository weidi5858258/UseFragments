package com.weidi.usefragments.business.video_player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.contents.Contents;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.JniObject;
import com.weidi.usefragments.tool.MLog;
import com.weidi.utils.MyToast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_audioHandleData;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_download;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_getDuration;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_init;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_initPlayer;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_isPlaying;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_isRunning;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_pause;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_play;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_readData;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_seekTo;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_setMode;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_setSurface;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_stepAdd;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrame;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrameForFinish;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_frameByFrameForReady;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_stepSubtract;
import static com.weidi.usefragments.business.video_player.FFMPEG.DO_SOMETHING_CODE_videoHandleData;
import static com.weidi.usefragments.business.video_player.FFMPEG.USE_MODE_AAC_H264;
import static com.weidi.usefragments.business.video_player.FFMPEG.USE_MODE_AUDIO_VIDEO;
import static com.weidi.usefragments.business.video_player.FFMPEG.VOLUME_MUTE;
import static com.weidi.usefragments.business.video_player.FFMPEG.VOLUME_NORMAL;
import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

public class PlayerWrapper {

    private static final String TAG = "player_alexander";

    public static final boolean IS_HIKEY970 = false;

    public static final int PLAYBACK_PROGRESS_UPDATED = 200;
    private static final int MSG_ON_PROGRESS_UPDATED = 10;
    private static final int MSG_START_PLAYBACK = 11;
    private static final int MSG_SEEK_TO_ADD = 12;
    private static final int MSG_SEEK_TO_SUBTRACT = 13;
    private static final int MSG_DOWNLOAD = 14;
    private static final int MSG_LOAD_CONTENTS = 15;

    public static final String PLAYBACK_ADDRESS = "playback_address";
    public static final String PLAYBACK_POSITION = "playback_position";
    public static final String PLAYBACK_ISLIVE = "playback_islive";
    // 是否正常结束
    public static final String PLAYBACK_NORMAL_FINISH = "playback_normal_finish";
    public static final String PLAYBACK_MEDIA_TYPE = "playback_media_type";
    // true表示静音
    public static final String PLAYBACK_IS_MUTE = "playback_is_mute";
    // 保存窗口位置
    public static final String PLAYBACK_WINDOW_POSITION = "playback_window_position";
    public static final String PLAYBACK_WINDOW_POSITION_TAG = "@@@@@@@@@@";
    // 显示控制面板(true表示显示,false表示隐藏)
    public static final String PLAYBACK_SHOW_CONTROLLERPANELLAYOUT =
            "playback_show_controllerpanellayout";
    // 使用"use_exoplayer"和"use_ffmpeg"两个字符串.默认为"use_exoplayer"
    // 当值为use_exoplayer时,表示在FfmpegUseMediaCodecDecode类中,
    // MediaFormat的值由exoplayer去得到的.
    // 另一个的意思是由底层传给java层一些参数然后创建MediaFormat
    public static final String PLAYBACK_USE_EXOPLAYER_OR_FFMPEG =
            "playback_use_exoplayer_or_ffmpeg";

    private HashMap<String, Long> mPathTimeMap = new HashMap<>();
    private ArrayList<String> mCouldPlaybackPathList = new ArrayList<>();

    private SharedPreferences mSP;
    private PowerManager.WakeLock mPowerWakeLock;
    private SurfaceHolder mSurfaceHolder;
    private FFMPEG mFFMPEGPlayer;
    //private SimpleVideoPlayer mSimpleVideoPlayer;
    private SimplePlayer mSimpleVideoPlayer;
    private FfmpegUseMediaCodecDecode mFfmpegUseMediaCodecDecode;
    private GetMediaFormat mGetMediaFormat;
    private String mPath;
    private String md5Path;
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
    private boolean mIsH264 = false;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mRootView;

    private AudioManager mAudioManager;
    private AudioFocusRequest mAudioFocusRequest;

    private SurfaceView mSurfaceView;
    private LinearLayout mControllerPanelLayout;
    private ProgressBar mLoadingView;
    private SeekBar mProgressBar;
    private TextView mFileNameTV;
    private TextView mProgressTimeTV;
    private TextView mSeekTimeTV;
    private TextView mDurationTimeTV;
    private ImageButton mPreviousIB;
    private ImageButton mPlayIB;
    private ImageButton mPauseIB;
    private ImageButton mNextIB;
    private ImageButton mExitIB;
    // 声音
    private ImageButton mVolumeNormal;
    private ImageButton mVolumeMute;
    // 下载
    private TextView mDownloadTV;
    private boolean mIsDownloading = false;
    // 1(停止下载) 2(下载音视频) 3(只下载,不播放)
    private int mDownloadClickCounts = 0;
    // 跟视频有关的提示信息
    private ScrollView textInfoScrollView;
    private TextView textInfoTV;

    private Handler mUiHandler;
    private Handler mThreadHandler;
    private HandlerThread mHandlerThread;
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
    // 音视频的加载进度
    private LinearLayout mProgressBarLayout;
    private ProgressBar mVideoProgressBar;
    private ProgressBar mAudioProgressBar;
    private int mProgressBarLayoutHeight;

    private Context mContext;
    private JniPlayerActivity mActivity;
    private PlayerService mService;

    private boolean mIsPhoneDevice;
    // 是否是竖屏 true为竖屏
    private boolean mIsPortraitScreen;

    public static final String PLAYBACK_USE_PLAYER = "playback_use_player";
    public static final String PLAYER_FFMPEG = "player_ffmpeg";
    public static final String PLAYER_MEDIACODEC = "player_mediacodec";
    public static final String PLAYER_FFMPEG_MEDIACODEC = "player_ffmpeg_mediacodec";
    private String whatPlayer = PLAYER_FFMPEG_MEDIACODEC;

    // 第一个存储视频地址,第二个存储标题
    public static final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();

    // 必须首先被调用
    public void setService(Service service) {
        mService = null;
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
        mSeekTimeTV = mRootView.findViewById(R.id.seek_time_tv);
        mDurationTimeTV = mRootView.findViewById(R.id.duration_time_tv);
        mPreviousIB = mRootView.findViewById(R.id.button_prev);
        mPlayIB = mRootView.findViewById(R.id.button_play);
        mPauseIB = mRootView.findViewById(R.id.button_pause);
        mNextIB = mRootView.findViewById(R.id.button_next);

        mExitIB = mRootView.findViewById(R.id.button_exit);
        mExitIB.setVisibility(View.VISIBLE);
        mDownloadTV = mRootView.findViewById(R.id.download_tv);
        mVolumeNormal = mRootView.findViewById(R.id.volume_normal);
        mVolumeMute = mRootView.findViewById(R.id.volume_mute);

        mProgressBarLayout = mRootView.findViewById(R.id.progress_bar_layout);
        mVideoProgressBar = mRootView.findViewById(R.id.video_progress_bar);
        mAudioProgressBar = mRootView.findViewById(R.id.audio_progress_bar);

        textInfoScrollView = mRootView.findViewById(R.id.text_scrollview);
        textInfoTV = mRootView.findViewById(R.id.text_info_tv);

        if (mSP == null) {
            mSP = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        mSurfaceView.setOnClickListener(mOnClickListener);
        mPreviousIB.setOnClickListener(mOnClickListener);
        mPlayIB.setOnClickListener(mOnClickListener);
        mPauseIB.setOnClickListener(mOnClickListener);
        mNextIB.setOnClickListener(mOnClickListener);
        mExitIB.setOnClickListener(mOnClickListener);
        mDownloadTV.setOnClickListener(mOnClickListener);
        mVolumeNormal.setOnClickListener(mOnClickListener);
        mVolumeMute.setOnClickListener(mOnClickListener);
    }

    public void setService(Activity activity, Service service) {
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
            mSeekTimeTV = mRootView.findViewById(R.id.seek_time_tv);
            mDurationTimeTV = mRootView.findViewById(R.id.duration_time_tv);
            mPreviousIB = mRootView.findViewById(R.id.button_prev);
            mPlayIB = mRootView.findViewById(R.id.button_play);
            mPauseIB = mRootView.findViewById(R.id.button_pause);
            mNextIB = mRootView.findViewById(R.id.button_next);

            mExitIB = mRootView.findViewById(R.id.button_exit);
            mExitIB.setVisibility(View.VISIBLE);
            mDownloadTV = mRootView.findViewById(R.id.download_tv);
            mVolumeNormal = mRootView.findViewById(R.id.volume_normal);
            mVolumeMute = mRootView.findViewById(R.id.volume_mute);

            mProgressBarLayout = mRootView.findViewById(R.id.progress_bar_layout);
            mVideoProgressBar = mRootView.findViewById(R.id.video_progress_bar);
            mAudioProgressBar = mRootView.findViewById(R.id.audio_progress_bar);

            if (mSP == null) {
                mSP = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            }

            mSurfaceView.setOnClickListener(mOnClickListener);
            mPreviousIB.setOnClickListener(mOnClickListener);
            mPlayIB.setOnClickListener(mOnClickListener);
            mPauseIB.setOnClickListener(mOnClickListener);
            mNextIB.setOnClickListener(mOnClickListener);
            mExitIB.setOnClickListener(mOnClickListener);
            mDownloadTV.setOnClickListener(mOnClickListener);
            mVolumeNormal.setOnClickListener(mOnClickListener);
            mVolumeMute.setOnClickListener(mOnClickListener);
        }
    }

    /*if (newPath.startsWith("http://")
            || newPath.startsWith("https://")
            || newPath.startsWith("rtmp://")
            || newPath.startsWith("rtsp://")
            || newPath.startsWith("ftp://")) {
        mIsLocal = false;
    }*/
    public void setDataSource(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mPath = path;
        md5Path = md5(path);
        String newPath = mPath.toLowerCase();
        if (newPath.startsWith("/storage/")) {
            mIsLocal = true;
        } else {
            mIsLocal = false;
        }
        if (newPath.endsWith(".h264")) {
            mIsH264 = true;
        } else {
            mIsH264 = false;
        }
        MLog.i(TAG, "setDataSource() mIsLocal: " + mIsLocal);
        MLog.i(TAG, "setDataSource()  mIsH264: " + mIsH264);
    }

    public void setType(String type) {
        mType = type;
    }

    public Handler getUiHandler() {
        return mUiHandler;
    }

    public void onCreate() {
        EventBusUtils.register(this);
        if (mSP == null) {
            mSP = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }

        mPathTimeMap.clear();
        mContentsMap.clear();
        mCouldPlaybackPathList.clear();

        mIsPhoneDevice = isPhoneDevice();
        if (mFFMPEGPlayer == null) {
            mFFMPEGPlayer = FFMPEG.getDefault();
        }
        if (mFfmpegUseMediaCodecDecode == null) {
            mFfmpegUseMediaCodecDecode = new FfmpegUseMediaCodecDecode();
        }
        mFFMPEGPlayer.setContext(mContext);
        mFFMPEGPlayer.setFfmpegUseMediaCodecDecode(mFfmpegUseMediaCodecDecode);
        mFfmpegUseMediaCodecDecode.setContext(mContext);

        /*if (mGetMediaFormat == null) {
            mGetMediaFormat = new GetMediaFormat();
        }
        mGetMediaFormat.setContext(mContext);
        mGetMediaFormat.setPlayerWrapper(this);*/
        //mFfmpegUseMediaCodecDecode.setGetMediaFormat(mGetMediaFormat);

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerWrapper.this.uiHandleMessage(msg);
            }
        };

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerWrapper.this.threadHandleMessage(msg);
            }
        };

        sendMessageForLoadContents();

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
                if (fromTouch && !isFrameByFrameMode) {
                    // 得到的是秒
                    long tempProgress =
                            (long) ((progress / 3840.00) * mMediaDuration);
                    mProgress = tempProgress;
                    if (!mIsH264) {
                        String elapsedTime =
                                DateUtils.formatElapsedTime(tempProgress);
                        mSeekTimeTV.setText(elapsedTime);
                    } else {
                        mSeekTimeTV.setText(String.valueOf(tempProgress));
                    }
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
                        if (isFrameByFrameMode) {
                            return false;
                        }
                        mNeedToSyncProgressBar = false;
                        mSeekTimeTV.setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isFrameByFrameMode) {
                            return false;
                        }
                        mNeedToSyncProgressBar = true;
                        mSeekTimeTV.setVisibility(View.GONE);
                        if (!mIsH264) {
                            MLog.d(TAG, "MotionEvent.ACTION_UP mProgress: " + mProgress +
                                    " " + DateUtils.formatElapsedTime(mProgress));
                        } else {
                            MLog.d(TAG, "MotionEvent.ACTION_UP mProgress: " + mProgress);
                        }
                        if (mProgress >= 0 && mProgress <= mMediaDuration) {
                            if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                                mSimpleVideoPlayer.setProgressUs(mProgress * 1000000);
                            } else {
                                mFFMPEGPlayer.onTransact(
                                        DO_SOMETHING_CODE_seekTo,
                                        JniObject.obtain().writeLong(mProgress));
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        // Test
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    saveLog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SystemClock.sleep(5000);
                    readLog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();*/
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
        if (mHandlerThread != null) {
            mHandlerThread.quit();
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
            case Callback.MSG_ON_TRANSACT_VIDEO_PRODUCER:// 生产者
                mVideoProgressBar.setSecondaryProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_VIDEO_CONSUMER:// 消费者
                mVideoProgressBar.setProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_AUDIO_PRODUCER:// 生产者
                mAudioProgressBar.setSecondaryProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_AUDIO_CONSUMER:// 消费者
                mAudioProgressBar.setProgress(((JniObject) msg.obj).valueInt);
                break;
            case Callback.MSG_ON_TRANSACT_INIT_VIDEO_MEDIACODEC:
                /*if (mFfmpegUseMediaCodecDecode != null) {
                    mFfmpegUseMediaCodecDecode.initVideoMediaCodec(msg);
                }*/
                break;
            case Callback.MSG_ON_TRANSACT_READY:
                MLog.d(TAG, "Callback.MSG_ON_TRANSACT_READY");
                onReady();
                break;
            case Callback.MSG_ON_TRANSACT_CHANGE_WINDOW:
                MLog.d(TAG, "Callback.MSG_ON_TRANSACT_CHANGE_WINDOW");
                onChangeWindow(msg);
                break;
            case Callback.MSG_ON_TRANSACT_PLAYED:
                MLog.d(TAG, "Callback.MSG_ON_TRANSACT_PLAYED");
                mPlayIB.setVisibility(View.VISIBLE);
                mPauseIB.setVisibility(View.GONE);
                mLoadingView.setVisibility(View.GONE);
                break;
            case Callback.MSG_ON_TRANSACT_PAUSED:
                MLog.d(TAG, "Callback.MSG_ON_TRANSACT_PAUSED");
                mPlayIB.setVisibility(View.GONE);
                mPauseIB.setVisibility(View.VISIBLE);
                if (!mIsLocal) {
                    mLoadingView.setVisibility(View.VISIBLE);
                }
                break;
            case Callback.MSG_ON_TRANSACT_FINISHED:
                MLog.d(TAG, "Callback.MSG_ON_TRANSACT_FINISHED");
                onFinished();
                break;
            case Callback.MSG_ON_TRANSACT_INFO:
                if (msg.obj != null && msg.obj instanceof String) {
                    String toastInfo = ((String) msg.obj).trim();
                    MLog.d(TAG, "Callback.MSG_ON_TRANSACT_INFO\n" + toastInfo);
                    //MyToast.show(toastInfo);
                    if (toastInfo.contains("[")
                            && toastInfo.contains("]")) {
                        textInfoTV.setText(toastInfo);
                    }
                }
                break;
            case Callback.MSG_ON_TRANSACT_ERROR:
                MLog.d(TAG, "Callback.MSG_ON_TRANSACT_ERROR");
                // 如果有错误,底层先发送error,然后再finish
                onError(msg);
                break;
            case Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED:
                onUpdated(msg);
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:// 79 单击事件
                if (clickCounts > NEED_CLICK_COUNTS) {
                    clickCounts = NEED_CLICK_COUNTS;
                }

                switch (clickCounts) {
                    case 1:
                        MLog.d(TAG, "clickOne()");
                        clickOne();
                        break;
                    case 2:
                        MLog.d(TAG, "clickTwo()");
                        clickTwo();
                        break;
                    case 3:
                        MLog.d(TAG, "clickThree()");
                        clickThree();
                        break;
                    case 4:
                        MLog.d(TAG, "clickFour()");
                        clickFour();
                        break;
                    case 5:
                        MLog.d(TAG, "clickFive()");
                        clickFive();
                        break;
                    case 6:
                        MLog.d(TAG, "clickSix()");
                        clickSix();
                        break;
                    case 7:
                        MLog.d(TAG, "clickSeven()");
                        clickSeven();
                        break;
                    case 8:
                        break;
                    case 9:
                        MLog.d(TAG, "clickNine()");
                        clickNine();
                        break;
                    case 10:
                        MLog.d(TAG, "clickTen()");
                        clickTen();
                        break;
                    default:
                        break;
                }

                clickCounts = 0;
                break;
            case MSG_START_PLAYBACK:
                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    /*ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            mSimpleVideoPlayer.handleData(mSimpleVideoPlayer.mAudioWrapper);
                        }
                    });
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            mSimpleVideoPlayer.handleData(mSimpleVideoPlayer.mVideoWrapper);
                        }
                    });*/
                } else {
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            sendEmptyMessage(DO_SOMETHING_CODE_audioHandleData);
                        }
                    });
                    ThreadPool.getFixedThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            sendEmptyMessage(DO_SOMETHING_CODE_videoHandleData);
                        }
                    });

                    if (mIsSeparatedAudioVideo) {
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                SystemClock.sleep(500);
                                sendEmptyMessage(DO_SOMETHING_CODE_readData);
                            }
                        });
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                SystemClock.sleep(1000);
                                sendEmptyMessage(DO_SOMETHING_CODE_readData);
                            }
                        });
                    }
                }
                break;
            case MSG_SEEK_TO_ADD:
                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    mSimpleVideoPlayer.setProgressUs(
                            mSimpleVideoPlayer.getCurrentPosition() + addStep * 1000000);
                } else {
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.onTransact(
                                DO_SOMETHING_CODE_stepAdd,
                                JniObject.obtain().writeLong(addStep));
                    }
                }
                addStep = 0;
                break;
            case MSG_SEEK_TO_SUBTRACT:
                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    mSimpleVideoPlayer.setProgressUs(
                            mSimpleVideoPlayer.getCurrentPosition() - subtractStep * 1000000);
                } else {
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.onTransact(
                                DO_SOMETHING_CODE_stepSubtract,
                                JniObject.obtain().writeLong(subtractStep));
                    }
                }
                subtractStep = 0;
                break;
            case PLAYBACK_PROGRESS_UPDATED:
                mProgressBar.setSecondaryProgress(mDownloadProgress);
                break;
            default:
                break;
        }
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_DOWNLOAD:
                if (mDownloadClickCounts > 4) {
                    mDownloadClickCounts = 4;
                }
                MLog.d(TAG, "threadHandleMessage() mDownloadClickCounts: " +
                        mDownloadClickCounts);

                // 点击次数
                switch (mDownloadClickCounts) {
                    case 1:
                        if (!mIsDownloading) {
                            break;
                        }
                        mIsDownloading = false;
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // 这个操作在ThreadHandler中没有发生问题
                                mDownloadTV.setText("1");
                            }
                        });
                        // 停止下载
                        mFFMPEGPlayer.onTransact(
                                DO_SOMETHING_CODE_download,
                                JniObject.obtain()
                                        .writeInt(1).writeStringArray(new String[]{"", ""}));
                        break;
                    case 2:
                    case 3:
                    case 4:
                        if (mIsDownloading) {
                            break;
                        }
                        mIsDownloading = true;
                        String path =
                                "/storage/1532-48AD/Android/data/" +
                                        "com.weidi.usefragments/files/Movies/";
                        String title;
                        if (mIsLocal) {
                            title = mPath.substring(
                                    mPath.lastIndexOf("/") + 1, mPath.lastIndexOf("."));
                        } else {
                            title = mContentsMap.get(mPath);
                        }
                        StringBuilder sb = new StringBuilder();
                        if (TextUtils.isEmpty(title)) {
                            sb.append("media-");
                        } else {
                            sb.append(title);
                            sb.append("-");
                        }
                        sb.append(mSimpleDateFormat.format(new Date()));
                        // 保存路径 文件名
                        if (mDownloadClickCounts == 2) {
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDownloadTV.setText("2");
                                }
                            });
                            // 开始下载,边播放边下
                            mFFMPEGPlayer.onTransact(
                                    DO_SOMETHING_CODE_download,
                                    JniObject.obtain()
                                            .writeInt(0)
                                            .writeStringArray(new String[]{path, sb.toString()}));
                        } else {
                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mDownloadClickCounts == 3) {
                                        mDownloadTV.setText("3");
                                    } else {
                                        mDownloadTV.setText("4");
                                    }
                                    if (TextUtils.isEmpty(mType)
                                            || mType.startsWith("video/")) {
                                        mVideoWidth = 0;
                                        mVideoHeight = 0;
                                        handlePortraitScreen();
                                    }
                                }
                            });
                            if (mDownloadClickCounts == 3) {
                                // 只下载,不播放.不调用seekTo
                                mFFMPEGPlayer.onTransact(
                                        DO_SOMETHING_CODE_download,
                                        JniObject.obtain()
                                                .writeInt(4)
                                                .writeStringArray(
                                                        new String[]{path, sb.toString()}));
                            } else {
                                // 只提取音视频,不播放.调用seekTo到0
                                mFFMPEGPlayer.onTransact(
                                        DO_SOMETHING_CODE_download,
                                        JniObject.obtain()
                                                .writeInt(5)
                                                .writeStringArray(
                                                        new String[]{path, sb.toString()}));
                            }
                        }
                        break;
                    default:
                        break;
                }

                mDownloadClickCounts = 0;
                break;
            case MSG_LOAD_CONTENTS:
                loadContents();
                break;
            default:
                break;
        }
    }

    // @@@
    public void startForGetMediaFormat() {
        whatPlayer = mSP.getString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC);
        /*if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)
                && !mPath.endsWith(".m4s")
                && !mPath.endsWith(".h264")
                && !mPath.endsWith(".aac")
                //&& mIsLocal
                && (TextUtils.isEmpty(mType)
                || mType.startsWith("video/"))) {
            onReady();
            mGetMediaFormat.start(mPath);
            return;
        }*/

        startPlayback();
    }

    public void startPlayback() {
        MLog.d(TAG, "startPlayback() start");
        if (!mService.mIsAddedView || !mRootView.isShown()) {
            MLog.e(TAG, "startPlayback() The condition is not satisfied");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            /***
             AUDIOFOCUS_LOSS：当本程序正在播放音频时有另一播放器请求获得音频焦点播放音频，那么就会回调该方法并传入此参数
             AUDIOFOCUS_LOSS_TRANSIENT：当另一个播放器请求“短暂”获得音频焦点，传入此参数
             AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK：当另一播放器请求“短暂”获得音频焦点且不用完全暂停可以让对方降低音量时传入此参数
             AUDIOFOCUS_GAIN：当其他播放器正在播放音频时，本程序请求获得音频焦点播放音频，传入此参数
             AUDIOFOCUS_GAIN_TRANSIENT：本程序请求“短暂”获取音频焦点时传入此参数
             AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK：本程序请求“短暂”音频焦点且可能会将低对方音量时传入此参数
             */
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
            mAudioFocusRequest =
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            // 有短暂的音频（如短信）来了，这时音乐的声音就会降低（但不会暂停）以此凸显出短信提示音，这是由系统自己自动处理的
                            .setWillPauseWhenDucked(false)
                            .setAcceptsDelayedFocusGain(true)// ---> AUDIOFOCUS_REQUEST_DELAYED
                            .setAudioAttributes(audioAttributes)
                            .setOnAudioFocusChangeListener(AudioFocusChangeListener)
                            .build();
            int focusRequest = mAudioManager.requestAudioFocus(mAudioFocusRequest);
            switch (focusRequest) {
                case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                    // 不允许播放
                    MLog.i(TAG, "startPlayback() AudioManager.AUDIOFOCUS_REQUEST_FAILED");
                    break;
                case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                    // 开始播放
                    MLog.i(TAG, "startPlayback() AudioManager.AUDIOFOCUS_REQUEST_GRANTED");
                    break;
                case AudioManager.AUDIOFOCUS_REQUEST_DELAYED:
                    MLog.i(TAG, "startPlayback() AudioManager.AUDIOFOCUS_REQUEST_DELAYED");
                    break;
                default:
                    break;
            }
        }

        mSurfaceHolder = mSurfaceView.getHolder();
        // 这里也要写
        mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);

        // 底层有关参数的设置
        mFFMPEGPlayer.setHandler(mUiHandler);
        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
            //mSimpleVideoPlayer = new SimpleVideoPlayer();
            mSimpleVideoPlayer = new SimplePlayer();
            mSimpleVideoPlayer.setContext(mContext);
            mSimpleVideoPlayer.setHandler(mUiHandler);
            mSimpleVideoPlayer.setCallback(mFFMPEGPlayer.mCallback);
        }

        // 开启线程初始化ffmpeg
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                MLog.d(TAG, "startPlayback()                  mPath: " + mPath);

                // region 判断是什么样的文件(一般用于本地文件)

                String tempPath = "";
                mIsSeparatedAudioVideo = false;
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
                } else if (mPath.endsWith(".h264") || mPath.endsWith(".aac")) {
                    tempPath = mPath.substring(0, mPath.lastIndexOf("/"));
                    String fileName = mPath.substring(
                            mPath.lastIndexOf("/") + 1, mPath.lastIndexOf("."));
                    MLog.d(TAG, "startPlayback()               fileName: " + fileName);
                    StringBuilder sb = new StringBuilder(tempPath);
                    sb.append("/");
                    sb.append(fileName);
                    sb.append(".aac");
                    File audioFile = new File(sb.toString());
                    sb = new StringBuilder(tempPath);
                    sb.append("/");
                    sb.append(fileName);
                    sb.append(".h264");
                    File videoFile = new File(sb.toString());
                    MLog.d(TAG,
                            "startPlayback()                  audio: " + audioFile.getAbsolutePath());
                    MLog.d(TAG,
                            "startPlayback()                  video: " + videoFile.getAbsolutePath());
                    if (audioFile.exists() && videoFile.exists()) {
                        mIsSeparatedAudioVideo = true;
                    }
                }
                MLog.d(TAG, "startPlayback() mIsSeparatedAudioVideo: " + mIsSeparatedAudioVideo);

                // endregion

                if (mIsSeparatedAudioVideo
                        && TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                    MyToast.show(PLAYER_MEDIACODEC + "模式下不能播放");
                    if (mService != null) {
                        mService.removeView();
                        if (mSurfaceHolder != null) {
                            mSurfaceHolder.removeCallback(mSurfaceCallback);
                            mSurfaceHolder = null;
                        }
                    }
                    return;
                }

                if (!mIsSeparatedAudioVideo) {
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        mSimpleVideoPlayer.setSurface(mSurfaceHolder.getSurface());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mSimpleVideoPlayer.setDataSource(mPath);
                        }
                        /*if (!mSimpleVideoPlayer.initPlayer()) {
                            return;
                        }*/
                    } else {
                        sendEmptyMessage(DO_SOMETHING_CODE_init);
                        if (TextUtils.isEmpty(mType)
                                || mType.startsWith("video/")) {
                            if (!mPath.endsWith(".h264")) {
                                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                        JniObject.obtain().writeInt(
                                                FFMPEG.USE_MODE_MEDIA_MEDIACODEC));
                            } else {
                                mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                        JniObject.obtain().writeInt(
                                                FFMPEG.USE_MODE_ONLY_VIDEO));
                            }
                        } else if (mType.startsWith("audio/")) {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                    JniObject.obtain().writeInt(FFMPEG.USE_MODE_ONLY_AUDIO));
                        }
                    }

                    if (mPathTimeMap.containsKey(md5Path)) {
                        long position = mPathTimeMap.get(md5Path);
                        MLog.d(TAG, "startPlayback()               position: " + position);

                        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                            //mSimpleVideoPlayer.setProgressUs(position * 1000000);
                        } else {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_seekTo,
                                    JniObject.obtain().writeLong(position));
                        }
                    }
                } else {
                    sendEmptyMessage(DO_SOMETHING_CODE_init);
                    // [.m4s] or [.h264 and .aac](达不到同步效果)
                    if (mPath.endsWith(".m4s")) {
                        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                JniObject.obtain().writeInt(USE_MODE_AUDIO_VIDEO));
                    } else {
                        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setMode,
                                JniObject.obtain().writeInt(USE_MODE_AAC_H264));
                    }
                }

                if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                } else {
                    mFfmpegUseMediaCodecDecode.setSurface(mSurfaceHolder.getSurface());
                    mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_setSurface,
                            JniObject.obtain()
                                    .writeString(mPath)
                                    .writeObject(mSurfaceHolder.getSurface()));

                    if (Integer.parseInt(mFFMPEGPlayer.onTransact(
                            DO_SOMETHING_CODE_initPlayer, null)) != 0) {
                        // 不在这里做事了.遇到error会从底层回调到java端的
                        //MyToast.show("音视频初始化失败");
                        //mUiHandler.removeMessages(Callback.MSG_ON_ERROR);
                        //mUiHandler.sendEmptyMessage(Callback.MSG_ON_ERROR);
                        return;
                    }
                }

                mUiHandler.removeMessages(MSG_START_PLAYBACK);
                mUiHandler.sendEmptyMessage(MSG_START_PLAYBACK);
                if (!mIsSeparatedAudioVideo) {
                    SystemClock.sleep(600);
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        //mSimpleVideoPlayer.readData();
                    } else {
                        sendEmptyMessage(DO_SOMETHING_CODE_readData);
                    }
                }

                MLog.d(TAG, "startPlayback() end");
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

    // 状态栏高度
    private int getStatusBarHeight() {
        int height = 0;
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android");
        if (resourceId > 0) {
            height = resources.getDimensionPixelSize(resourceId);
        }
        // getStatusBarHeight() height: 48 95
        MLog.d(TAG, "getStatusBarHeight() height: " + height);
        return height;
    }

    // 虚拟导航栏高度
    private int getNavigationBarHeight() {
        int height = 0;
        Resources resources = mContext.getResources();
        int resourceId = resources.getIdentifier(
                "navigation_bar_height",
                "dimen",
                "android");
        if (resourceId > 0) {
            height = resources.getDimensionPixelSize(resourceId);
        }
        MLog.d(TAG, "getNavigationBarHeight() height: " + height);
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
        if (mVideoWidth == 0 || mVideoHeight == 0) {
            return;
        }

        mIsPortraitScreen = false;

        mRootView.setBackgroundColor(
                mContext.getResources().getColor(R.color.black));
        mControllerPanelLayout.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

        // 暂停按钮高度
        getPauseRlHeight();

        if (statusBarHeight != 0) {
            statusBarHeight = getStatusBarHeight();
        }
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              statusBarHeight: " +
                statusBarHeight);

        // mScreenWidth: 2149 mScreenHeight: 1080
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        if (mVideoWidth <= mScreenWidth && mVideoHeight <= mScreenHeight) {
            mNeedVideoWidth = (mScreenHeight * mVideoWidth) / mVideoHeight;
            mNeedVideoHeight = mScreenHeight;
        } else if (mVideoWidth > mScreenWidth && mVideoHeight <= mScreenHeight) {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
        } else if (mVideoWidth <= mScreenWidth && mVideoHeight > mScreenHeight) {
            mNeedVideoWidth = (mScreenHeight * mVideoWidth) / mVideoHeight;
            mNeedVideoHeight = mScreenHeight;
        } else if (mVideoWidth > mScreenWidth && mVideoHeight > mScreenHeight) {
            mNeedVideoWidth = (mScreenHeight * mVideoWidth) / mVideoHeight;
            mNeedVideoHeight = mScreenHeight;
        }
        if (mNeedVideoWidth > mScreenWidth) {
            mNeedVideoWidth = mScreenWidth;
        }
        if (mNeedVideoHeight > mScreenHeight) {
            mNeedVideoHeight = mScreenHeight;
        }
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        // 生产,消耗进度条高度
        mProgressBarLayoutHeight = mProgressBarLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW     mProgressBarLayoutHeight: " +
                mProgressBarLayoutHeight);
        if (mProgressBarLayoutHeight > 0) {
            RelativeLayout.LayoutParams relativeParams =
                    (RelativeLayout.LayoutParams) mProgressBarLayout.getLayoutParams();
            relativeParams.setMargins(
                    (mScreenWidth - mNeedVideoWidth) / 2, (mScreenHeight - mNeedVideoHeight) / 2,
                    0, 0);
            relativeParams.width = mNeedVideoWidth;
            relativeParams.height = mProgressBarLayoutHeight;
            mProgressBarLayout.setLayoutParams(relativeParams);
        }

        // 改变SurfaceView宽高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        //relativeParams.setMargins(0, 0, 0, 0);
        relativeParams.setMargins(
                (mScreenWidth - mNeedVideoWidth) / 2, (mScreenHeight - mNeedVideoHeight) / 2,
                0, 0);
        relativeParams.width = mNeedVideoWidth;
        if (statusBarHeight != 0) {
            relativeParams.height = mNeedVideoHeight - statusBarHeight;
        } else {
            relativeParams.height = mNeedVideoHeight;
        }
        mSurfaceView.setLayoutParams(relativeParams);
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW         relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(
                (mScreenWidth - mNeedVideoWidth) / 2, 6, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if ((mScreenWidth / 3) < mNeedVideoWidth) {// top = 120
            frameParams.setMargins((mScreenWidth - mNeedVideoWidth) / 2, 40, 0, 0);
            frameParams.width = mNeedVideoWidth;
        } else {
            frameParams.setMargins((mScreenWidth - mScreenHeight) / 2, 40, 0, 0);
            frameParams.width = mScreenHeight;
        }
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

        mIsPortraitScreen = true;
        handleLandscapeScreenFlag = false;

        mRootView.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

        int x = 0;
        int y = 0;
        String position = mSP.getString(PLAYBACK_WINDOW_POSITION, null);
        if (position != null && position.contains(PLAYBACK_WINDOW_POSITION_TAG)) {
            String[] positions = position.split(PLAYBACK_WINDOW_POSITION_TAG);
            y = Integer.parseInt(positions[1]);
        }
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW x: " + x + " y: " + y);

        // 控制面板高度
        mControllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mControllerPanelLayoutHeight: " +
                mControllerPanelLayoutHeight);

        // 暂停按钮高度
        int pauseRlHeight = getPauseRlHeight();

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
                mScreenWidth + " mScreenHeight: " + mScreenHeight);

        // 生产,消耗进度条高度
        mProgressBarLayoutHeight = mProgressBarLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW     mProgressBarLayoutHeight: " +
                mProgressBarLayoutHeight);
        if (mProgressBarLayoutHeight > 0) {
            RelativeLayout.LayoutParams relativeParams =
                    (RelativeLayout.LayoutParams) mProgressBarLayout.getLayoutParams();
            relativeParams.setMargins(0, 0, 0, 0);
            relativeParams.width = mScreenWidth;
            relativeParams.height = mProgressBarLayoutHeight;
            mProgressBarLayout.setLayoutParams(relativeParams);
        }

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
        }
        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        mSurfaceView.setLayoutParams(relativeParams);
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(0, 6, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
            if (mNeedVideoHeight < mScreenHeight) {
                frameParams.setMargins(
                        0, mNeedVideoHeight - mControllerPanelLayoutHeight - 10, 0, 0);
            } else {
                frameParams.setMargins(
                        0, getStatusBarHeight(), 0, 0);
            }
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            frameParams.setMargins(
                    0, mNeedVideoHeight, 0, 0);
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                    updateRootViewLayout(
                            mScreenWidth,
                            mNeedVideoHeight, x, y);
                } else {
                    if (mMediaDuration <= 0) {
                        // 直播节目
                        updateRootViewLayout(
                                mScreenWidth,
                                mNeedVideoHeight + pauseRlHeight, x, y);
                    } else {
                        updateRootViewLayout(
                                mScreenWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight, x, y);
                    }
                }
            } else {
                if (TextUtils.isEmpty(mType)
                        || mType.startsWith("video/")) {
                    if (mMediaDuration <= 0) {
                        // 是视频并且只下载不播放的情况下
                        updateRootViewLayout(mScreenWidth, pauseRlHeight, x, y);
                        return;
                    }
                }
                if (mMediaDuration > 0) {
                    // 音乐 或者 mMediaDuration > 0
                    updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1, x, y);
                } else {
                    updateRootViewLayout(mScreenWidth, pauseRlHeight + 1, x, y);
                }
            }
        }
    }

    // 电视机专用
    public void handlePortraitScreenWithTV() {
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreenWithTV");

        mIsPortraitScreen = true;

        mRootView.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.transparent));

        int x = 0;
        int y = 0;
        String position = mSP.getString(PLAYBACK_WINDOW_POSITION, null);
        if (position != null && position.contains(PLAYBACK_WINDOW_POSITION_TAG)) {
            String[] positions = position.split(PLAYBACK_WINDOW_POSITION_TAG);
            x = Integer.parseInt(positions[0]);
            y = Integer.parseInt(positions[1]);
        }
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW x: " + x + " y: " + y);

        int pauseRlHeight = getPauseRlHeight();

        // mScreenWidth: 1080 mScreenHeight: 2244
        // 屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                 mScreenWidth: " +
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
        }
        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        mSurfaceView.setLayoutParams(relativeParams);
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW              mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(0, 4, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
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
                    updateRootViewLayout(mScreenWidth, mNeedVideoHeight, x, y);
                } else {
                    if (mMediaDuration <= 0) {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + pauseRlHeight, x, y);
                    } else {
                        updateRootViewLayout(mScreenWidth,
                                mNeedVideoHeight + mControllerPanelLayoutHeight, x, y);
                    }
                }
            } else {
                updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1, x, y);
            }
        }
    }

    // Hikey970开发板专用
    @SuppressLint("SourceLockedOrientationActivity")
    public void handleLandscapeScreenWithHikey970() {
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handleLandscapeScreenWithHikey970");

        getPauseRlHeight();

        // 状态样高度
        int statusBarHeight = getStatusBarHeight();
        // 系统控制面板高度
        int navigationBarHeight = getNavigationBarHeight();

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
        relativeParams.height = mScreenHeight - statusBarHeight - navigationBarHeight;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);
        mSurfaceView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        frameParams.setMargins(
                0, getStatusBarHeight(), 0, 0);
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            updateRootViewLayout(mScreenWidth, relativeParams.height);
        }
    }

    // Hikey970开发板专用
    public void handlePortraitScreenWithHikey970() {
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW handlePortraitScreenWithHikey970");

        int pauseRlHeight = getPauseRlHeight();

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
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
        } else {
            mNeedVideoWidth = mScreenWidth;
            mNeedVideoHeight = 1;
            mControllerPanelLayout.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lightgray));
        }
        relativeParams.width = mNeedVideoWidth;
        relativeParams.height = mNeedVideoHeight;
        mSurfaceView.setLayoutParams(relativeParams);
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW mNeedVideoWidth: " +
                mNeedVideoWidth + " mNeedVideoHeight: " + mNeedVideoHeight);

        relativeParams =
                (RelativeLayout.LayoutParams) textInfoScrollView.getLayoutParams();
        relativeParams.setMargins(0, 4, 0, 0);
        textInfoScrollView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                frameParams.setMargins(0, getStatusBarHeight(), 0, 0);
            } else {
                frameParams.setMargins(0, 0, 0, 0);
            }
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                updateRootViewLayout(mScreenWidth, mNeedVideoHeight + pauseRlHeight);
            } else {
                updateRootViewLayout(mScreenWidth, mControllerPanelLayoutHeight + 1);
            }
        }
    }

    public void sendMessageForLoadContents() {
        mThreadHandler.removeMessages(MSG_LOAD_CONTENTS);
        mThreadHandler.sendEmptyMessage(MSG_LOAD_CONTENTS);
    }

    private boolean mIsTesting = false;

    public void startTest() {
        mIsTesting = true;
        mCouldPlaybackPathList.clear();
        mCouldPlaybackPathList.add("");
    }

    private void onReady() {
        // 是否显示控制面板
        if (TextUtils.isEmpty(mType)
                || mType.startsWith("video/")) {
            if (!mIsLocal) {
                mLoadingView.setVisibility(View.VISIBLE);
                mControllerPanelLayout.setVisibility(View.VISIBLE);
            } else {
                boolean show = mSP.getBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, false);
                if (show) {
                    mControllerPanelLayout.setVisibility(View.VISIBLE);
                } else {
                    mControllerPanelLayout.setVisibility(View.INVISIBLE);
                }
            }
        } else if (mType.startsWith("audio/")) {
            mControllerPanelLayout.setVisibility(View.VISIBLE);
        }
        mProgressTimeTV.setText("");
        mDurationTimeTV.setText("");
        mProgressBar.setProgress(0);
        mVideoProgressBar.setProgress(0);
        mVideoProgressBar.setSecondaryProgress(0);
        mAudioProgressBar.setProgress(0);
        mAudioProgressBar.setSecondaryProgress(0);
        mProgressBarLayout.setVisibility(View.GONE);
        mPlayIB.setVisibility(View.GONE);
        mPauseIB.setVisibility(View.VISIBLE);
        textInfoScrollView.setVisibility(View.GONE);
        textInfoTV.setText("");
        if (mService != null) {
            mDownloadTV.setText("");
            // R.color.lightgray
            mDownloadTV.setBackgroundColor(
                    mContext.getResources().getColor(android.R.color.transparent));
            boolean isMute = mSP.getBoolean(PLAYBACK_IS_MUTE, false);
            if (!isMute) {
                mVolumeNormal.setVisibility(View.VISIBLE);
                mVolumeMute.setVisibility(View.GONE);
            } else {
                mVolumeNormal.setVisibility(View.GONE);
                mVolumeMute.setVisibility(View.VISIBLE);
            }
        }
        String title;
        if (mIsLocal) {
            title = mPath.substring(mPath.lastIndexOf("/") + 1);
        } else {
            title = mContentsMap.get(mPath);
        }
        if (!TextUtils.isEmpty(title)) {
            mFileNameTV.setText(title);
        } else {
            mFileNameTV.setText("");
        }
    }

    private void onChangeWindow(Message msg) {
        // 视频宽高
        mVideoWidth = msg.arg1;
        mVideoHeight = msg.arg2;
        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
            mMediaDuration = mSimpleVideoPlayer.getDurationUs() / 1000000;
        } else {
            mMediaDuration = Long.parseLong(
                    mFFMPEGPlayer.onTransact(
                            DO_SOMETHING_CODE_getDuration, null));
        }
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW               mMediaDuration: " +
                mMediaDuration);
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW                   videoWidth: " +
                mVideoWidth + " videoHeight: " + mVideoHeight);
        if (!mIsH264) {
            String durationTime = DateUtils.formatElapsedTime(mMediaDuration);
            mDurationTimeTV.setText(durationTime);
        } else {
            mDurationTimeTV.setText(String.valueOf(mMediaDuration));
        }

        if (TextUtils.isEmpty(mType)
                || mType.startsWith("video/")) {
            /*if ((mMediaDuration > 0 && mMediaDuration <= 300) || mIsH264) {
                mControllerPanelLayout.setVisibility(View.VISIBLE);
            } else {
                //mControllerPanelLayout.setVisibility(View.INVISIBLE);
            }*/
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                boolean show = mSP.getBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, false);
                if (!mIsLocal) {
                    mProgressBarLayout.setVisibility(View.VISIBLE);
                    if (show) {
                        mControllerPanelLayout.setVisibility(View.VISIBLE);
                    } else {
                        mControllerPanelLayout.setVisibility(View.INVISIBLE);
                    }
                }
                if (show) {
                    textInfoScrollView.setVisibility(View.VISIBLE);
                } else {
                    textInfoScrollView.setVisibility(View.GONE);
                }
            }

            if (mVideoWidth == 0 && mVideoHeight == 0) {
                mType = "audio/";
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                textInfoScrollView.setVisibility(View.GONE);
            }

            if (!mCouldPlaybackPathList.contains(mPath)) {
                mCouldPlaybackPathList.add(mPath);
            }
            SharedPreferences.Editor edit = mSP.edit();
            // 保存播放地址
            edit.putString(PLAYBACK_ADDRESS, mPath);
            // 开始播放设置为false,表示初始化状态
            edit.putBoolean(PLAYBACK_NORMAL_FINISH, false);
            edit.putString(PLAYBACK_MEDIA_TYPE, mType);
            edit.commit();
        }

        if (mIsPhoneDevice) {
            MLog.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW 手机");
            if (mContext.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                // 横屏
                if (!IS_HIKEY970) {
                    handleLandscapeScreen(0);
                } else {
                    // handlePortraitScreenWithHikey970();
                    handlePortraitScreenWithTV();
                }
            } else {
                // 竖屏
                handlePortraitScreen();
            }
        } else {
            MLog.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW 电视机");
            handlePortraitScreenWithTV();
        }
    }

    private void onFinished() {
        mFfmpegUseMediaCodecDecode.releaseMediaCodec();

        if (mHasError) {
            mHasError = false;
            MLog.d(TAG, "onFinished() restart playback");
            // 重新开始播放
            // startPlayback();
            startForGetMediaFormat();
        } else {
            MyToast.show("Safe Exit");
            if (mIsTesting) {

                return;
            }
            // 播放结束
            if (mService != null) {
                if (!mService.needToPlaybackOtherVideo()) {
                    mService.removeView();
                    if (mSurfaceHolder != null) {
                        mSurfaceHolder.removeCallback(mSurfaceCallback);
                        mSurfaceHolder = null;
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
                    }
                    System.gc();
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
    }

    private void onError(Message msg) {
        mHasError = false;
        String errorInfo = null;
        if (msg.obj != null) {
            errorInfo = (String) msg.obj;
        }
        int error = msg.arg1;
        switch (error) {
            case Callback.ERROR_TIME_OUT:
                //case Callback.ERROR_DATA_EXCEPTION:
                MLog.e(TAG, "PlayerWrapper Callback.ERROR_TIME_OUT errorInfo: " + errorInfo);
                // 需要重新播放
                mHasError = true;
                break;
            case Callback.ERROR_FFMPEG_INIT:
                MLog.e(TAG, "PlayerWrapper Callback.ERROR_FFMPEG_INIT errorInfo: " + errorInfo);
                if (TextUtils.isEmpty(mType)
                        || mType.startsWith("video/")) {
                    if (mCouldPlaybackPathList.contains(mPath)
                            && !mPath.startsWith("http://cache.m.iqiyi.com/")) {
                        // startPlayback();
                        startForGetMediaFormat();
                        break;
                    } else {
                        String path = mSP.getString(PLAYBACK_ADDRESS, null);
                        if (TextUtils.equals(path, mPath)
                                && !mPath.startsWith("http://cache.m.iqiyi.com/")) {
                            // startPlayback();
                            startForGetMediaFormat();
                            break;
                        }
                    }
                }

                MyToast.show("音视频初始化失败");
                // 不需要重新播放
                if (mService != null) {
                    /*MLog.i(TAG, "PlayerWrapper Callback.ERROR_FFMPEG_INIT " +
                            "mService.removeView()");*/
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
    }

    private void onUpdated(Message msg) {
        if (msg.obj == null) {
            return;
        }

        // 秒
        mPresentationTime = (Long) msg.obj;
        if (!mIsH264) {
            String curElapsedTime = DateUtils.formatElapsedTime(mPresentationTime);
            mProgressTimeTV.setText(curElapsedTime);
        } else {
            mProgressTimeTV.setText(String.valueOf(mPresentationTime));
        }

        if (mNeedToSyncProgressBar && mMediaDuration > 0) {
            int currentPosition = (int) (mPresentationTime);
            float pos = (float) currentPosition / mMediaDuration;
            int target = Math.round(pos * mProgressBar.getMax());
            mProgressBar.setProgress(target);
        }

        if (mMediaDuration > 0) {
            if (mPresentationTime < (mMediaDuration - 5)) {
                mPathTimeMap.put(md5Path, mPresentationTime);
                if (mIsH264 && (mMediaDuration - mPresentationTime <= 1000000)) {
                    mPathTimeMap.remove(md5Path);
                }
            } else {
                if (mPathTimeMap.containsKey(md5Path)) {
                    mPathTimeMap.remove(md5Path);
                }
            }
        }
    }

    private void updateRootViewLayout(int width, int height) {
        updateRootViewLayout(width, height, 0, 0);
    }

    private void updateRootViewLayout(int width, int height, int x, int y) {
        if (mService != null && mService.mIsAddedView) {
            // mLayoutParams.gravity = Gravity.DISPLAY_CLIP_VERTICAL;
            mLayoutParams.width = width;
            mLayoutParams.height = height;
            mLayoutParams.x = x;
            mLayoutParams.y = y;
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        }
    }

    private int getPauseRlHeight() {
        int pauseRlHeight = 0;
        if (mService != null) {
            RelativeLayout pause_rl = mRootView.findViewById(R.id.pause_rl);
            pauseRlHeight = pause_rl.getHeight();
            SeekBar progress_bar = mRootView.findViewById(R.id.progress_bar);
            RelativeLayout show_time_rl = mRootView.findViewById(R.id.show_time_rl);
            ImageButton button_prev = mRootView.findViewById(R.id.button_prev);
            ImageButton button_next = mRootView.findViewById(R.id.button_next);
            if (mMediaDuration <= 0 && !mIsH264) {
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
        return pauseRlHeight;
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(
                SurfaceHolder holder) {
            MLog.d(TAG, "surfaceCreated()");

            // startPlayback();
            startForGetMediaFormat();
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
            if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                mSimpleVideoPlayer.release();
            } else {
                if (mGetMediaFormat != null) {
                    mGetMediaFormat.release(false);
                }
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
                    if (!isFrameByFrameMode) {
                        if (mFFMPEGPlayer != null) {
                            if (!mIsH264) {
                                if (mMediaDuration > 300) {
                                    subtractStep += 30;
                                } else {
                                    subtractStep += 10;
                                }
                            } else {
                                if (mMediaDuration > 52428800) {// 50MB
                                    subtractStep += 1048576;// 1MB
                                } else {
                                    subtractStep += 524288;// 514KB
                                }
                            }
                            MLog.d(TAG, "onClick() subtractStep: " + subtractStep);
                            mUiHandler.removeMessages(MSG_SEEK_TO_SUBTRACT);
                            mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_SUBTRACT, 500);
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrame, null);
                        }
                    }
                    break;
                case R.id.button_play:
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        if (mSimpleVideoPlayer.isRunning()) {
                            if (mSimpleVideoPlayer.isPlaying()) {
                                mPlayIB.setVisibility(View.GONE);
                                mPauseIB.setVisibility(View.VISIBLE);
                                mSimpleVideoPlayer.pause();
                            }
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                                    DO_SOMETHING_CODE_isRunning, null))) {
                                if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                                        DO_SOMETHING_CODE_isPlaying, null))) {
                                    mPlayIB.setVisibility(View.GONE);
                                    mPauseIB.setVisibility(View.VISIBLE);
                                    sendEmptyMessage(DO_SOMETHING_CODE_pause);
                                }
                            }
                        }
                    }
                    break;
                case R.id.button_pause:
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        if (mSimpleVideoPlayer.isRunning()) {
                            if (!mSimpleVideoPlayer.isPlaying()) {
                                mPlayIB.setVisibility(View.VISIBLE);
                                mPauseIB.setVisibility(View.GONE);
                                mSimpleVideoPlayer.play();
                            }
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                                    DO_SOMETHING_CODE_isPlaying, null))
                                    || isFrameByFrameMode) {
                                if (isFrameByFrameMode) {
                                    isFrameByFrameMode = false;
                                    mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrameForFinish, null);
                                    mVolumeNormal.setVisibility(View.VISIBLE);
                                    mVolumeMute.setVisibility(View.GONE);
                                    mFFMPEGPlayer.setVolume(VOLUME_NORMAL);
                                    mFfmpegUseMediaCodecDecode.setVolume(VOLUME_NORMAL);
                                    mSP.edit().putBoolean(PLAYBACK_IS_MUTE, false).commit();
                                    MyToast.show("帧模式已关闭");
                                }
                                mPlayIB.setVisibility(View.VISIBLE);
                                mPauseIB.setVisibility(View.GONE);
                                mLoadingView.setVisibility(View.GONE);
                                sendEmptyMessage(DO_SOMETHING_CODE_play);
                            }
                        }
                    }
                    break;
                case R.id.button_next:
                    if (!isFrameByFrameMode) {
                        if (mFFMPEGPlayer != null) {
                            if (!mIsH264) {
                                if (mMediaDuration > 300) {
                                    addStep += 30;
                                } else {
                                    addStep += 10;
                                }
                            } else {
                                if (mMediaDuration > 52428800) {// 50MB
                                    addStep += 1048576;// 1MB
                                } else {
                                    addStep += 524288;// 514KB
                                }
                            }
                            MLog.d(TAG, "onClick() addStep: " + addStep);
                            mUiHandler.removeMessages(MSG_SEEK_TO_ADD);
                            mUiHandler.sendEmptyMessageDelayed(MSG_SEEK_TO_ADD, 500);
                        }
                    } else {
                        if (mFFMPEGPlayer != null) {
                            mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrame, null);
                        }
                    }
                    break;
                case R.id.surfaceView:
                    mIsScreenPress = true;
                    onEvent(KeyEvent.KEYCODE_HEADSETHOOK, null);
                    break;
                case R.id.button_exit:
                    mDownloadClickCounts = 0;
                    mIsDownloading = false;
                    isFrameByFrameMode = false;
                    if (mService != null) {
                        mService.removeView();
                    }
                    break;
                case R.id.volume_normal:
                    mVolumeNormal.setVisibility(View.GONE);
                    mVolumeMute.setVisibility(View.VISIBLE);
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        mSimpleVideoPlayer.setVolume(VOLUME_MUTE);
                    } else {
                        mFFMPEGPlayer.setVolume(VOLUME_MUTE);
                        mFfmpegUseMediaCodecDecode.setVolume(VOLUME_MUTE);
                    }
                    mSP.edit().putBoolean(PLAYBACK_IS_MUTE, true).commit();
                    break;
                case R.id.volume_mute:
                    if (isFrameByFrameMode) {
                        return;
                    }
                    mVolumeNormal.setVisibility(View.VISIBLE);
                    mVolumeMute.setVisibility(View.GONE);
                    if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
                        mSimpleVideoPlayer.setVolume(VOLUME_NORMAL);
                    } else {
                        mFFMPEGPlayer.setVolume(VOLUME_NORMAL);
                        mFfmpegUseMediaCodecDecode.setVolume(VOLUME_NORMAL);
                    }
                    mSP.edit().putBoolean(PLAYBACK_IS_MUTE, false).commit();
                    break;
                case R.id.download_tv:
                    if (TextUtils.isEmpty(mDownloadTV.getText())) {
                        mDownloadTV.setText("1");
                        mDownloadTV.setBackgroundColor(
                                mContext.getResources().getColor(R.color.burlywood));
                        return;
                    }
                    mDownloadClickCounts++;
                    mThreadHandler.removeMessages(MSG_DOWNLOAD);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD, 500);
                    break;
                default:
                    break;
            }
        }
    };

    private void clickOne() {
        if (mFFMPEGPlayer != null) {
            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                || mContext.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    mControllerPanelLayout.setVisibility(View.GONE);
                    textInfoScrollView.setVisibility(View.GONE);
                    mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, false).commit();
                }
            } else {
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                textInfoScrollView.setVisibility(View.VISIBLE);
                mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, true).commit();
            }
        }
    }

    private void clickTwo() {
        if (mFFMPEGPlayer != null) {
            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                return;
            }
        }

        // 播放与暂停
        if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                DO_SOMETHING_CODE_isPlaying, null))) {
            mPlayIB.setVisibility(View.GONE);
            mPauseIB.setVisibility(View.VISIBLE);
            sendEmptyMessage(DO_SOMETHING_CODE_pause);
        } else {
            mPlayIB.setVisibility(View.VISIBLE);
            mPauseIB.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.GONE);
            sendEmptyMessage(DO_SOMETHING_CODE_play);
        }
    }

    private void clickThree() {
        if (mFFMPEGPlayer != null) {
            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                if (TextUtils.isEmpty(mType)
                        || mType.startsWith("video/")) {
                    mType = "video/";
                } else if (mType.startsWith("audio/")) {
                    mType = "audio/";
                }
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_SHOW_WINDOW,
                        new Object[]{mPath, mType});
                return;
            }
        }

        /***
         缓冲过程中,按三下进行暂停,继续缓冲.
         */
        mPlayIB.setVisibility(View.GONE);
        mPauseIB.setVisibility(View.VISIBLE);
        if (!mIsLocal) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
        sendEmptyMessage(DO_SOMETHING_CODE_pause);
        MyToast.show("pause");
    }

    private boolean handleLandscapeScreenFlag = false;

    @SuppressLint("SourceLockedOrientationActivity")
    private void clickFour() {
        if (mFFMPEGPlayer != null) {
            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                return;
            }
        }

        if (mIsPhoneDevice) {
            if (mService != null) {
                // 如果当前是横屏
                if (mContext.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE) {
                    MLog.d(TAG, "onKeyDown() 4 横屏");
                    /*if (JniPlayerActivity.isAliveJniPlayerActivity) {
                        handleLandscapeScreen(0);
                    } else {
                    }*/
                    if (!IS_HIKEY970) {
                        if (handleLandscapeScreenFlag) {
                            handleLandscapeScreenFlag = false;
                            handleLandscapeScreen(1);
                        } else {
                            handleLandscapeScreenFlag = true;
                            handleLandscapeScreen(0);
                        }
                    } else {
                        if (handleLandscapeScreenFlag) {
                            handleLandscapeScreenFlag = false;
                            // handleLandscapeScreenWithHikey970();
                            handleLandscapeScreen(0);
                        } else {
                            handleLandscapeScreenFlag = true;
                            //handlePortraitScreenWithHikey970();
                            handlePortraitScreenWithTV();
                        }
                    }
                } else if (mContext.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_PORTRAIT) {
                    MLog.d(TAG, "onKeyDown() 4 竖屏");
                    handlePortraitScreen();
                }
            } /*else if (mActivity != null) {
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
            }*/
        } else {
            // 电视机
            if (handleLandscapeScreenFlag) {
                handleLandscapeScreenFlag = false;
                handleLandscapeScreen(0);
            } else {
                handleLandscapeScreenFlag = true;
                handlePortraitScreenWithTV();
            }
        }
    }

    private void clickFive() {
        if (mFFMPEGPlayer != null) {
            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                return;
            }
        }

        isFrameByFrameMode = Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                DO_SOMETHING_CODE_frameByFrameForReady, null));
        if (isFrameByFrameMode) {
            // 静音
            mVolumeNormal.setVisibility(View.GONE);
            mVolumeMute.setVisibility(View.VISIBLE);
            mFFMPEGPlayer.setVolume(VOLUME_MUTE);
            mFfmpegUseMediaCodecDecode.setVolume(VOLUME_MUTE);
            mSP.edit().putBoolean(PLAYBACK_IS_MUTE, true).commit();
            // 显示控制面板
            mControllerPanelLayout.setVisibility(View.VISIBLE);
            textInfoScrollView.setVisibility(View.VISIBLE);
            mSP.edit().putBoolean(PLAYBACK_SHOW_CONTROLLERPANELLAYOUT, true).commit();
            // 显示"暂停"按钮
            mPlayIB.setVisibility(View.GONE);
            mPauseIB.setVisibility(View.VISIBLE);
            MyToast.show("帧模式已开启");
        }
    }

    private void clickSix() {
        String whatPlayer = mSP.getString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC);
        if (TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
            MyToast.show(PLAYER_FFMPEG);
            mSP.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG).commit();
        } else {
            MyToast.show(PLAYER_FFMPEG_MEDIACODEC);
            mSP.edit().putString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC).commit();
        }
    }

    private void clickSeven() {
        if (mFfmpegUseMediaCodecDecode != null) {
            if (mFfmpegUseMediaCodecDecode.mUseMediaCodecForAudio) {
                mFfmpegUseMediaCodecDecode.mUseMediaCodecForAudio = false;
                MyToast.show("不使用音频硬解码");
            } else {
                mFfmpegUseMediaCodecDecode.mUseMediaCodecForAudio = true;
                MyToast.show("使用音频硬解码");
            }
        }
    }

    private void clickNine() {
        if (mFFMPEGPlayer != null) {
            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                return;
            }
        }

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(JniPlayerActivity.COMMAND_NO_FINISH, true);
        intent.setClass(mContext, JniPlayerActivity.class);
        mContext.startActivity(intent);
    }

    private void clickTen() {
        if (mFFMPEGPlayer != null) {
            if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                return;
            }
        }

        if (TextUtils.equals(whatPlayer, PLAYER_MEDIACODEC)) {
            mSimpleVideoPlayer.release();
        } else {
            if (mGetMediaFormat != null) {
                mGetMediaFormat.release(false);
            }
            mFFMPEGPlayer.releaseAll();
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

    private void sendEmptyMessage(int code) {
        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.onTransact(code, null);
        }
    }

    private void loadContents() {
        MLog.i(TAG, "loadContents() start");
        mContentsMap.clear();
        File[] files = mContext.getExternalFilesDirs(Environment.MEDIA_SHARED);
        if (files == null) {
            MLog.e(TAG, "loadContents() files is null");
            return;
        }
        File file = null;
        for (File f : files) {
            if (f == null) {
                continue;
            }
            MLog.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
            file = f;
        }
        if (file != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(file.getAbsolutePath());
            sb.append("/");
            sb.append("contents.txt");
            file = new File(sb.toString());
            if (file.exists()) {
                readContents(file);
                MLog.i(TAG, "loadContents() end");
                return;
            }
        }

        for (Map.Entry<String, String> tempMap : Contents.movieMap.entrySet()) {
            if (!mContentsMap.containsKey(tempMap.getValue())) {
                mContentsMap.put(tempMap.getValue(), tempMap.getKey());
            }
        }
        MLog.i(TAG, "loadContents() end");
    }

    private void readContents(File file) {
        final String TAG = "@@@@@@@@@@";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String aLineContent = null;
            String[] contents = null;
            String key = null;
            String value = null;
            StringBuilder sb = new StringBuilder();
            int i = 0;
            //一次读一行，读入null时文件结束
            while ((aLineContent = reader.readLine()) != null) {
                if (aLineContent.length() == 0) {
                    continue;
                }

                aLineContent = aLineContent.trim();

                if (aLineContent.contains(TAG) && !aLineContent.startsWith("#")) {
                    ++i;
                    contents = aLineContent.split(TAG);
                    key = contents[0];
                    value = contents[1];

                    sb.append(i);
                    if (i < 10) {
                        sb.append("____");
                    } else if (i >= 10 && i < 100) {
                        sb.append("___");
                    } else if (i >= 100 && i < 1000) {
                        sb.append("__");
                    } else if (i >= 1000 && i < 10000) {
                        sb.append("_");
                    }
                    sb.append("_");
                    sb.append(value);

                    if (contents.length > 1) {
                        if (!mContentsMap.containsKey(key)) {
                            mContentsMap.put(key, sb.toString());
                        } else {
                            --i;
                        }
                    }

                    /*MLog.i("player_alexander", "readContents() sb.toString(): " +
                            sb.toString());*/
                    sb.delete(0, sb.length());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    private static int length(String s) {
        if (s == null) {
            return 0;
        }
        char[] c = s.toCharArray();
        int len = 0;
        for (int i = 0; i < c.length; i++) {
            len++;
            if (!isLetter(c[i])) {
                len++;
            }
        }
        return len;
    }

    private static boolean isLetter(char c) {
        int k = 0x80;
        return c / k == 0 ? true : false;
    }

    private boolean isFrameByFrameMode = false;
    private static final int NEED_CLICK_COUNTS = 10;
    private int clickCounts = 0;

    public Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            // 有线耳机发出的事件
            case KeyEvent.KEYCODE_HEADSETHOOK:// 79
                if (!isFrameByFrameMode) {
                    ++clickCounts;
                    MyToast.show(String.valueOf(clickCounts));

                    // 单位时间内按1次,2次,3次分别实现单击,双击,三击
                    mUiHandler.removeMessages(KeyEvent.KEYCODE_HEADSETHOOK);
                    mUiHandler.sendEmptyMessageDelayed(KeyEvent.KEYCODE_HEADSETHOOK, 1000);
                } else {
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.onTransact(DO_SOMETHING_CODE_frameByFrame, null);
                    }
                }
                break;
            // 85 86 87 88 126 127 这些都是蓝牙耳机发出的事件
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:// 85
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:// 86
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:// 87
                // 双击
                MLog.d(TAG, "clickTwo()");
                clickTwo();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:// 88
                // 三击
                MLog.d(TAG, "clickThree()");
                clickThree();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:// 126
            case KeyEvent.KEYCODE_MEDIA_PAUSE:// 127
                // 单击
                //MLog.d(TAG, "clickOne()");
                //clickOne();
                break;
            default:
                break;
        }
        return result;
    }

    public void playPlayerWithTelephonyCall() {
        if (mFFMPEGPlayer != null) {
            if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                boolean isMute = mSP.getBoolean(PLAYBACK_IS_MUTE, false);
                if (!isMute) {
                    mFFMPEGPlayer.setVolume(VOLUME_NORMAL);
                    mVolumeNormal.setVisibility(View.VISIBLE);
                    mVolumeMute.setVisibility(View.GONE);
                } else {
                    mFFMPEGPlayer.setVolume(VOLUME_MUTE);
                    mVolumeNormal.setVisibility(View.GONE);
                    mVolumeMute.setVisibility(View.VISIBLE);
                }

                if (!Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                        DO_SOMETHING_CODE_isPlaying, null))) {
                    mPlayIB.setVisibility(View.VISIBLE);
                    mPauseIB.setVisibility(View.GONE);
                    sendEmptyMessage(DO_SOMETHING_CODE_play);
                }
            }
        }
    }

    public void pausePlayerWithTelephonyCall() {
        if (mFFMPEGPlayer != null) {
            if (Boolean.parseBoolean(mFFMPEGPlayer.onTransact(
                    DO_SOMETHING_CODE_isRunning, null))) {
                mPlayIB.setVisibility(View.GONE);
                mPauseIB.setVisibility(View.VISIBLE);
                sendEmptyMessage(DO_SOMETHING_CODE_pause);
            }
        }
    }

    AudioManager.OnAudioFocusChangeListener AudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // Resume playback or Raise it back to normal
                            MLog.i(TAG, "onAudioFocusChange() AudioManager.AUDIOFOCUS_GAIN");
                            playPlayerWithTelephonyCall();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                            //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                            //am.abandonAudioFocus(afChangeListener);
                            // Stop playback
                            MLog.i(TAG, "onAudioFocusChange() AudioManager.AUDIOFOCUS_LOSS");
                            pausePlayerWithTelephonyCall();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Pause playback
                            MLog.i(TAG, "onAudioFocusChange()" +
                                    " AudioManager.AUDIOFOCUS_LOSS_TRANSIENT");
                            pausePlayerWithTelephonyCall();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Pause playback
                            // Lower the volume
                            MLog.i(TAG, "onAudioFocusChange()" +
                                    " AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                            pausePlayerWithTelephonyCall();
                            break;
                        default:
                            break;
                    }
                }
            };

    private class PlayerOnTouchListener implements View.OnTouchListener {
        private StringBuffer sb = new StringBuffer();
        private int x;
        private int y;

        private int tempX;
        private int tempY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (!mIsPortraitScreen) {
                return true;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    tempX = x;
                    tempY = y;
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

                    tempX = mLayoutParams.x;
                    tempY = mLayoutParams.y;

                    // 更新悬浮窗控件布局
                    mWindowManager.updateViewLayout(view, mLayoutParams);
                    break;
                case MotionEvent.ACTION_UP:
                    sb.delete(0, sb.length());
                    sb.append(tempX);
                    sb.append(PLAYBACK_WINDOW_POSITION_TAG);
                    sb.append(tempY);
                    mSP.edit().putString(PLAYBACK_WINDOW_POSITION, sb.toString()).commit();
                    // MLog.i(TAG, "Callback.MSG_ON_CHANGE_WINDOW sb.toString(): " + sb.toString());
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private boolean mIsReading = false;

    private void saveLog() throws Exception {
        File[] files = mContext.getExternalFilesDirs(Environment.MEDIA_SHARED);
        File file = null;
        for (File f : files) {
            MLog.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
            file = f;
        }
        if (file == null) {
            return;
        }
        file = new File(file.getAbsolutePath(), "/Log.txt");
        if (file.exists()) {
            file.delete();
        }

        // adb logcat -G 20m &&
        String[] running = new String[]{"logcat", "-s", "adb logcat *:V"};
        Process exec = Runtime.getRuntime().exec(running);
        InputStream inputStream = exec.getInputStream();

        FileOutputStream os = new FileOutputStream(file, false);
        int readLength = 0;
        byte[] buffer = new byte[1024];
        MLog.i(TAG, "saveLog() start");
        while (-1 != (readLength = inputStream.read(buffer))) {
            os.write(buffer, 0, readLength);
            os.flush();
        }
        MLog.i(TAG, "saveLog() end");
    }

    private void readLog() throws Exception {
        File[] files = mContext.getExternalFilesDirs(Environment.MEDIA_SHARED);
        File file = null;
        for (File f : files) {
            MLog.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
            file = f;
        }
        if (file == null) {
            return;
        }

        file = new File(file.getAbsolutePath(), "/Log.txt");
        BufferedReader reader = new BufferedReader(new FileReader(file));

        mIsReading = true;
        String aLineLog = null;
        MLog.i(TAG, "readLog() start");
        while (mIsReading) {
            aLineLog = reader.readLine();
            if (TextUtils.isEmpty(aLineLog)) {
                SystemClock.sleep(10);
                continue;
            }

            if (aLineLog.contains("DmrPlayerService")) {
                MLog.d(TAG, "readLog(): \n" + aLineLog);
            }
        }
        MLog.i(TAG, "readLog() end");
    }

    public static String md5(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] digest = md.digest(str.getBytes());
            return bytes2hex02(digest);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String bytes2hex02(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        String tmp = null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & b);
            // 每个字节8为，转为16进制标志，2个16进制位
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }

}
