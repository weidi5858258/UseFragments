package com.weidi.usefragments.business.video_player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
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
import android.view.Surface;
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
import com.weidi.usefragments.BaseActivity;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.contents.Contents;
import com.weidi.usefragments.test_view.BubblePopupWindow;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.MLog;
import com.weidi.utils.MyToast;

import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

public class PlayerWrapper {

    private static final String TAG = "player_alexander";

    public static final String CONTENT_PATH = "content_path";

    public static final int PLAYBACK_PROGRESS_UPDATED = 200;
    private static final int MSG_ON_PROGRESS_UPDATED = 10;
    private static final int MSG_START_PLAYBACK = 11;

    public static final String PLAYBACK_ADDRESS = "playback_address";
    public static final String PLAYBACK_POSITION = "playback_position";
    public static final String PLAYBACK_ISLIVE = "playback_islive";
    private SharedPreferences mSP;

    private SurfaceHolder mSurfaceHolder;
    private PowerManager.WakeLock mPowerWakeLock;
    private FFMPEG mFFMPEGPlayer;
    private String mPath;
    private long mProgress;
    private long mPresentationTime;
    private int mDownloadProgress = -1;
    private long contentLength = -1;
    private boolean mNeedToSyncProgressBar = true;
    private boolean mIsScreenPress = false;
    private boolean mHasError = false;

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
    // 气泡上显示时间
    private TextView mShowTimeTV;
    private BubblePopupWindow mBubblePopupWindow;

    private Handler mUiHandler;
    private int videoWidth;
    private int videoHeight;

    private Context mContext;
    private Activity mActivity;

    public void setActivity(Activity activity, Service service) {
        mActivity = activity;
        if (activity != null) {
            if (!(activity instanceof JniPlayerActivity)) {
                return;
            }
            JniPlayerActivity jniPlayerActivity = (JniPlayerActivity) activity;
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

    public void setContext(Context context) {
        mContext = context;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public void onCreate() {
        EventBusUtils.register(this);
        // Volume change should always affect media volume
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);
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

        //        mLoadingView = findViewById(R.id.loading_view);
        //        mControllerPanelLayout = findViewById(R.id.controller_panel_layout);
        //        mFileNameTV = findViewById(R.id.file_name_tv);
        //        mProgressTimeTV = findViewById(R.id.progress_time_tv);
        //        mDurationTimeTV = findViewById(R.id.duration_time_tv);
        //        mProgressBar = findViewById(R.id.progress_bar);
        //        mPreviousIB = findViewById(R.id.button_prev);
        //        mPlayIB = findViewById(R.id.button_play);
        //        mPauseIB = findViewById(R.id.button_pause);
        //        mNextIB = findViewById(R.id.button_next);
        //        mSurfaceView = findViewById(R.id.surfaceView);

        mShowTimeTV.setOnClickListener(mOnClickListener);
        mPreviousIB.setOnClickListener(mOnClickListener);
        mPlayIB.setOnClickListener(mOnClickListener);
        mPauseIB.setOnClickListener(mOnClickListener);
        mNextIB.setOnClickListener(mOnClickListener);
        mSurfaceView.setOnClickListener(mOnClickListener);

        mFFMPEGPlayer = FFMPEG.getDefault();

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
                        mNeedToSyncProgressBar = false;
                        showBubbleView("", mProgressBar);
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

    public void onResume() {
        if (mSurfaceHolder == null) {
            // 没有图像出来,就是由于没有设置PixelFormat.RGBA_8888
            // 这里要写
            mSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(
                        SurfaceHolder holder) {
                    MLog.d(TAG, "surfaceCreated()");
                    if (mFFMPEGPlayer == null) {
                        return;
                    }

                    mSurfaceHolder = holder;
                    startPlayback(holder);
                }

                @Override
                public void surfaceChanged(
                        SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(
                        SurfaceHolder holder) {
                    MLog.d(TAG, "surfaceDestroyed()");
                    if (mFFMPEGPlayer != null) {
                        mFFMPEGPlayer.releaseAll();
                    }
                }
            });
        }

        if (mFFMPEGPlayer != null) {
            if (mFFMPEGPlayer.isRunning()) {
                if (!mFFMPEGPlayer.isPlaying()) {
                    mPlayIB.setVisibility(View.VISIBLE);
                    mPauseIB.setVisibility(View.GONE);
                    mControllerPanelLayout.setVisibility(View.GONE);
                    mFFMPEGPlayer.play();
                }
            }
        }
    }

    public void onPause() {

    }

    public void onStop() {

    }

    public void onDestroy() {
        if (mPowerWakeLock != null && mPowerWakeLock.isHeld()) {
            mPowerWakeLock.release();
            mPowerWakeLock = null;
        }
        EventBusUtils.unregister(this);
        if (mFFMPEGPlayer != null) {
            mFFMPEGPlayer.releaseAll();
        }
        mFFMPEGPlayer = null;
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
                videoWidth = msg.arg1;
                videoHeight = msg.arg2;
                MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW videoWidth: " +
                        videoWidth + " videoHeight: " + videoHeight);
                if (mContext.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE) {
                    //handleLandscapeScreen();
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
                        // 不需要重新播放
                        if (mActivity != null && mActivity instanceof BaseActivity) {
                            BaseActivity activity = (BaseActivity) mActivity;
                            activity.finish();
                            activity.exitActivity();
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
                    startPlayback(mSurfaceHolder);
                } else {
                    if (mActivity != null && mActivity instanceof BaseActivity) {
                        BaseActivity activity = (BaseActivity) mActivity;
                        activity.finish();
                        activity.exitActivity();
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

                    if (mActivity != null) {
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
                        mNeedToSyncProgressBar = true;
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
                                /*mPlayIB.setVisibility(View.GONE);
                                mPauseIB.setVisibility(View.VISIBLE);
                                mControllerPanelLayout.setVisibility(View.VISIBLE);*/
                                mFFMPEGPlayer.pause();
                            } else {
                                /*mPlayIB.setVisibility(View.VISIBLE);
                                mPauseIB.setVisibility(View.GONE);
                                mControllerPanelLayout.setVisibility(View.GONE);*/
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
                break;
            case MSG_START_PLAYBACK:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mFFMPEGPlayer.audioHandleData();
                    }
                }).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mFFMPEGPlayer.videoHandleData();
                    }
                }).start();
                break;
            case PLAYBACK_PROGRESS_UPDATED:
                mProgressBar.setSecondaryProgress(mDownloadProgress);
                break;
            default:
                break;
        }
    }

    private void showBubbleView(String time, View view) {
        mShowTimeTV.setText(time);
        mBubblePopupWindow.show(view);
    }

    private void startPlayback(SurfaceHolder holder) {
        if (holder == null) {
            return;
        }

        Surface surface = holder.getSurface();
        // 这里也要写
        holder.setFormat(PixelFormat.RGBA_8888);
        mFFMPEGPlayer.setMode(FFMPEG.USE_MODE_MEDIA);
        mFFMPEGPlayer.setCallback(mFFMPEGPlayer.mCallback);
        mFFMPEGPlayer.setHandler(mUiHandler);
        mFFMPEGPlayer.setSurface(mPath, surface);

        new Thread(new Runnable() {
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
        }).start();
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

    // 处理横屏
    @SuppressLint("SourceLockedOrientationActivity")
    private void handleLandscapeScreen() {
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

        // 屏幕宽高
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW screenWidth: " +
                screenWidth + " screenHeight: " + screenHeight);
        // 控制面板高度
        int controllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW controllerPanelLayoutHeight: " +
                controllerPanelLayoutHeight);
        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        relativeParams.width = screenWidth;
        relativeParams.height = screenHeight;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);
        mSurfaceView.setLayoutParams(relativeParams);
        // 改变ControllerPanelLayout高度
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        frameParams.setMargins(0, (screenHeight - controllerPanelLayoutHeight), 0, 0);
        frameParams.width = screenWidth;
        frameParams.height = controllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);
    }

    // 处理竖屏
    private void handlePortraitScreen() {
        mControllerPanelLayout.setVisibility(View.VISIBLE);
        // 屏幕宽高
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;// 1080
        int screenHeight = displayMetrics.heightPixels;// 2244
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW screenWidth: " +
                screenWidth + " screenHeight: " + screenHeight);
        // 控制面板高度
        int controllerPanelLayoutHeight = mControllerPanelLayout.getHeight();
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW controllerPanelLayoutHeight: " +
                controllerPanelLayoutHeight);
        // 改变SurfaceView高度
        RelativeLayout.LayoutParams relativeParams =
                (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        relativeParams.setMargins(0, 0, 0, 0);
        int tempHeight = (screenWidth * videoHeight) / videoWidth;
        if (tempHeight > screenHeight) {
            tempHeight = screenHeight;
        }
        relativeParams.width = screenWidth;
        relativeParams.height = tempHeight;
        MLog.d(TAG, "Callback.MSG_ON_CHANGE_WINDOW relativeParams.width: " +
                relativeParams.width + " relativeParams.height: " + relativeParams.height);
        mSurfaceView.setLayoutParams(relativeParams);
        // 改变ControllerPanelLayout高度
        if (tempHeight > (int) (screenHeight * 2 / 3)) {
            tempHeight = (int) (screenHeight / 2);
        }
        FrameLayout.LayoutParams frameParams =
                (FrameLayout.LayoutParams) mControllerPanelLayout.getLayoutParams();
        frameParams.setMargins(0, tempHeight, 0, 0);
        frameParams.width = screenWidth;
        frameParams.height = controllerPanelLayoutHeight;
        mControllerPanelLayout.setLayoutParams(frameParams);
    }

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
