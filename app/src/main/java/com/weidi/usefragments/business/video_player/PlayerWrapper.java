package com.weidi.usefragments.business.video_player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
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

import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

public class PlayerWrapper {

    private static final String TAG = "player_alexander";

    public static final int PLAYBACK_PROGRESS_UPDATED = 200;
    private static final int MSG_ON_PROGRESS_UPDATED = 10;
    private static final int MSG_START_PLAYBACK = 11;

    public static final String PLAYBACK_ADDRESS = "playback_address";
    public static final String PLAYBACK_POSITION = "playback_position";
    public static final String PLAYBACK_ISLIVE = "playback_islive";

    private SharedPreferences mSP;
    private PowerManager.WakeLock mPowerWakeLock;
    private SurfaceHolder mSurfaceHolder;
    private FFMPEG mFFMPEGPlayer;
    private String mPath;
    private long mProgress;
    private long mPresentationTime;
    private int mDownloadProgress = -1;
    private long contentLength = -1;
    private boolean mNeedToSyncProgressBar = true;
    private boolean mIsScreenPress = false;
    private boolean mHasError = false;

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
    // 想要的高度
    private int mNeedVideoHeight;
    // 控制面板的高度
    private int mControllerPanelLayoutHeight;

    private Context mContext;
    private JniPlayerActivity mActivity;
    private PlayerService mService;

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

            mSurfaceView = playerService.mSurfaceView;
            mControllerPanelLayout = playerService.mControllerPanelLayout;
            mLoadingView = playerService.mLoadingView;
            mProgressBar = playerService.mProgressBar;
            mFileNameTV = playerService.mFileNameTV;
            mProgressTimeTV = playerService.mProgressTimeTV;
            mDurationTimeTV = playerService.mDurationTimeTV;
            mPreviousIB = playerService.mPreviousIB;
            mPlayIB = playerService.mPlayIB;
            mPauseIB = playerService.mPauseIB;
            mNextIB = playerService.mNextIB;
        }
    }

    public void setPath(String path) {
        mPath = path;
    }

    public void onCreate() {
        EventBusUtils.register(this);
        mSP = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

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
        mSurfaceView.setOnClickListener(mOnClickListener);
        mPreviousIB.setOnClickListener(mOnClickListener);
        mPlayIB.setOnClickListener(mOnClickListener);
        mPauseIB.setOnClickListener(mOnClickListener);
        mNextIB.setOnClickListener(mOnClickListener);

        if (mFFMPEGPlayer == null) {
            mFFMPEGPlayer = FFMPEG.getDefault();
        }
        int duration = (int) mFFMPEGPlayer.getDuration();
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
                            (long) ((progress / 3840.00) * mFFMPEGPlayer.getDuration());
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

    @SuppressLint("SourceLockedOrientationActivity")
    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case Callback.MSG_ON_READY:
                String durationTime = DateUtils.formatElapsedTime(mFFMPEGPlayer.getDuration());
                mDurationTimeTV.setText(durationTime);
                if (durationTime.length() > 5) {
                    mProgressTimeTV.setText("00:00:00");
                } else {
                    mProgressTimeTV.setText("00:00");
                }
                mProgressBar.setProgress(0);
                mFileNameTV.setText(Contents.getTitle());
                mLoadingView.setVisibility(View.VISIBLE);
                break;
            case Callback.MSG_ON_CHANGE_WINDOW:
                // 视频宽高
                mVideoWidth = msg.arg1;
                mVideoHeight = msg.arg2;
                MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW videoWidth: " +
                        mVideoWidth + " videoHeight: " + mVideoHeight);

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
                break;
            case Callback.MSG_ON_PLAYED:
                durationTime = DateUtils.formatElapsedTime(mFFMPEGPlayer.getDuration());
                mDurationTimeTV.setText(durationTime);
                mLoadingView.setVisibility(View.GONE);
                mPlayIB.setVisibility(View.VISIBLE);
                mPauseIB.setVisibility(View.GONE);
                if (mContext.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE) {
                    mControllerPanelLayout.setVisibility(View.GONE);// INVISIBLE
                } else {
                    mControllerPanelLayout.setVisibility(View.VISIBLE);// INVISIBLE
                }

                SharedPreferences.Editor edit = mSP.edit();
                edit.putString(PLAYBACK_ADDRESS, mPath);
                if (mFFMPEGPlayer.getDuration() < 0) {
                    edit.putBoolean(PLAYBACK_ISLIVE, true);
                } else {
                    edit.putBoolean(PLAYBACK_ISLIVE, false);
                }
                edit.commit();
                break;
            case Callback.MSG_ON_PAUSED:
                mPlayIB.setVisibility(View.GONE);
                mPauseIB.setVisibility(View.VISIBLE);
                mLoadingView.setVisibility(View.VISIBLE);
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                break;
            case Callback.MSG_ON_ERROR:
                if (msg.obj == null) {
                    return;
                }
                mHasError = false;
                int error = msg.arg1;
                String errorInfo = (String) msg.obj;
                switch (error) {
                    case Callback.ERROR_TIME_OUT:
                    case Callback.ERROR_DATA_EXCEPTION:
                        // 需要重新播放
                        mHasError = true;
                        MLog.e(TAG, "Callback.MSG_ON_ERROR " + errorInfo);
                        break;
                    case Callback.ERROR_FFMPEG_INIT:
                        MLog.e(TAG, "Callback.ERROR_FFMPEG_INIT " + errorInfo);
                        // 不需要重新播放
                        if (mService != null) {
                            mService.removeView();
                            mSurfaceHolder.removeCallback(mSurfaceCallback);
                            mSurfaceHolder = null;
                        } else if (mActivity != null) {
                            mActivity.finish();
                            mActivity.exitActivity();
                            mSurfaceHolder.removeCallback(mSurfaceCallback);
                            mSurfaceHolder = null;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case Callback.MSG_ON_FINISHED:
                if (mHasError) {
                    mHasError = false;
                    // 重新开始播放
                    startPlayback();
                } else {
                    // 播放结束
                    if (mService != null) {
                        if (!mService.needToPlaybackOtherVideo()) {
                            MyToast.show("Safe Exit");
                            mService.removeView();
                            mSurfaceHolder.removeCallback(mSurfaceCallback);
                            mSurfaceHolder = null;
                        }
                    } else if (mActivity != null) {
                        mActivity.finish();
                        mActivity.exitActivity();
                        mSurfaceHolder.removeCallback(mSurfaceCallback);
                        mSurfaceHolder = null;
                    }
                }

                break;
            case Callback.MSG_ON_PROGRESS_UPDATED:
                if (msg.obj == null) {
                    return;
                }

                mPresentationTime = (Long) msg.obj;// 秒
                String curElapsedTime = DateUtils.formatElapsedTime(mPresentationTime);
                mProgressTimeTV.setText(curElapsedTime);

                int duration = (int) (mFFMPEGPlayer.getDuration());
                if (mNeedToSyncProgressBar && duration > 0) {
                    int currentPosition = (int) (mPresentationTime);
                    float pos = (float) currentPosition / duration;
                    int target = Math.round(pos * mProgressBar.getMax());
                    mProgressBar.setProgress(target);
                }

                mSP.edit().putLong(PLAYBACK_POSITION, mPresentationTime).commit();
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:// 单击事件
                if (firstFlag && secondFlag && threeFlag && fourFlag) {
                    /*Log.d(TAG, "onKeyDown() 4");*/

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
                                handleLandscapeScreen(1);
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
                } else if (firstFlag && secondFlag && threeFlag) {
                    /*Log.d(TAG, "onKeyDown() 3");*/

                    if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
                        mControllerPanelLayout.setVisibility(View.GONE);
                    } else {
                        mControllerPanelLayout.setVisibility(View.VISIBLE);
                    }
                } else if (firstFlag && secondFlag) {
                    /*Log.d(TAG, "onKeyDown() 2");*/

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
                } else {
                    /*Log.d(TAG, "onKeyDown() 1");*/
                }
                firstFlag = false;
                secondFlag = false;
                threeFlag = false;
                fourFlag = false;
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
                break;
            case PLAYBACK_PROGRESS_UPDATED:
                mProgressBar.setSecondaryProgress(mDownloadProgress);
                break;
            default:
                break;
        }
    }

    public void startPlayback() {
        if (mSurfaceHolder == null) {
            mSurfaceHolder = mSurfaceView.getHolder();
        }
        // 这里也要写
        mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
        // 底层有关参数的设置
        mFFMPEGPlayer.setMode(FFMPEG.USE_MODE_MEDIA);
        mFFMPEGPlayer.setCallback(mFFMPEGPlayer.mCallback);
        mFFMPEGPlayer.setHandler(mUiHandler);
        mFFMPEGPlayer.setSurface(mPath, mSurfaceHolder.getSurface());

        // 开启线程初始化ffmpeg
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (mFFMPEGPlayer.initPlayer() != 0) {
                    MyToast.show("音视频初始化失败");
                    mUiHandler.removeMessages(Callback.MSG_ON_ERROR);
                    mUiHandler.sendEmptyMessage(Callback.MSG_ON_ERROR);
                    return;
                }

                mUiHandler.removeMessages(MSG_START_PLAYBACK);
                mUiHandler.sendEmptyMessage(MSG_START_PLAYBACK);
                MyToast.show("音视频初始化成功");
                SystemClock.sleep(500);
                mFFMPEGPlayer.readData();
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

        /*if (mScreenWidth > mScreenHeight) {
            return;
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

        mControllerPanelLayout.setVisibility(View.VISIBLE);

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
        mNeedVideoHeight = (mScreenWidth * mVideoHeight) / mVideoWidth;
        if (mNeedVideoHeight > mScreenHeight) {
            mNeedVideoHeight = mScreenHeight;
        }
        relativeParams.width = mScreenWidth;
        relativeParams.height = mNeedVideoHeight;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);
        mSurfaceView.setLayoutParams(relativeParams);

        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
            //frameParams.setMargins(0, (int) (mScreenHeight / 2), 0, 0);
            frameParams.setMargins(0, getStatusBarHeight(), 0, 0);
        } else {
            frameParams.setMargins(0, mNeedVideoHeight, 0, 0);
        }
        frameParams.width = mScreenWidth;
        frameParams.height = mControllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);

        if (mService != null) {
            if (mNeedVideoHeight > (int) (mScreenHeight * 2 / 3)) {
                updateRootViewLayout(mScreenWidth, mNeedVideoHeight);
            } else {
                updateRootViewLayout(mScreenWidth, mNeedVideoHeight + mControllerPanelLayoutHeight);
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

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_prev:
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.stepSubtract();
                    }
                    break;
                case R.id.button_play:
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
                    if (mFFMPEGPlayer != null) {
                        if (!mFFMPEGPlayer.isPlaying()) {
                            mPlayIB.setVisibility(View.VISIBLE);
                            mPauseIB.setVisibility(View.GONE);
                            mFFMPEGPlayer.play();
                        }
                    }
                    break;
                case R.id.button_next:
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.stepAdd();
                    }
                    break;
                case R.id.surfaceView:
                    mIsScreenPress = true;
                    onEvent(KeyEvent.KEYCODE_HEADSETHOOK, null);
                    break;
                case R.id.content_tv:
                    mNeedToSyncProgressBar = true;
                    mBubblePopupWindow.dismiss();
                    MLog.d(TAG, "onClick() mProgress: " + mProgress +
                            " " + DateUtils.formatElapsedTime(mProgress));
                    if (mProgress >= 0 && mProgress <= mFFMPEGPlayer.getDuration()) {
                        mFFMPEGPlayer.seekTo(mProgress);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private boolean firstFlag = false;
    private boolean secondFlag = false;
    private boolean threeFlag = false;
    private boolean fourFlag = false;

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (!firstFlag) {
                    firstFlag = true;
                } else if (firstFlag && !secondFlag) {
                    secondFlag = true;
                } else if (firstFlag && secondFlag && !threeFlag) {
                    threeFlag = true;
                } else if (firstFlag && secondFlag && threeFlag && !fourFlag) {
                    fourFlag = true;
                }
                // 单位时间内按1次,2次,3次分别实现单击,双击,三击
                mUiHandler.removeMessages(KeyEvent.KEYCODE_HEADSETHOOK);
                mUiHandler.sendEmptyMessageDelayed(KeyEvent.KEYCODE_HEADSETHOOK, 300);
                break;
            default:
                break;
        }
        return result;
    }

}
