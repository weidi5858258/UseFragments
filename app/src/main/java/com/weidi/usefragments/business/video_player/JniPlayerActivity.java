package com.weidi.usefragments.business.video_player;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.BaseActivity;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.contents.Contents;
import com.weidi.usefragments.receiver.MediaButtonReceiver;
import com.weidi.usefragments.test_view.BubblePopupWindow;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;
import com.weidi.utils.MyToast;

/***

 */
public class JniPlayerActivity extends BaseActivity {

    /*private static final String TAG =
            JniPlayerActivity.class.getSimpleName();*/
    private static final String TAG = "player_alexander";
    private static final boolean DEBUG = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);
        initData();
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
        internalStart();
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
        exitActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            Log.d(TAG, "onActivityResult(): " + printThis() +
                    " requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            Log.d(TAG, "onSaveInstanceState(): " + printThis() +
                    " outState: " + outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onRestoreInstanceState(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        PermissionsUtils.onRequestPermissionsResult(
                this,
                permissions,
                grantResults);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (DEBUG)
            MLog.d(TAG, "onBackPressed(): " + printThis());
        finish();
        exitActivity();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (DEBUG)
            MLog.d(TAG, "onWindowFocusChanged(): " + printThis() +
                    " hasFocus: " + hasFocus);
    }

    ///////////////////////////////////////////////////////////////////////

    public static final String CONTENT_PATH = "content_path";

    private static final int PLAYBACK_INFO = 1;// 0x001
    public static final int PLAYBACK_PROGRESS_UPDATED = 200;
    private static final int PLAYBACK_PROGRESS_CHANGED = 3;
    private static final int MSG_LOADING_SHOW = 8;
    private static final int MSG_LOADING_HIDE = 9;
    private static final int MSG_ON_PROGRESS_UPDATED = 10;
    private static final int MSG_START_PLAYBACK = 11;

    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private PowerManager.WakeLock mPowerWakeLock;
    private FFMPEG mFFMPEGPlayer;
    private String mPath;
    private long mProgress;
    private long mPresentationTime;
    private int mDownloadProgress = -1;
    private long contentLength = -1;
    private boolean mNeedToSyncProgressBar = true;
    private boolean mIsScreenPress = false;
    private ProgressBar mLoadingView;
    private LinearLayout mControllerPanelLayout;
    private TextView mFileNameTV;
    private TextView mProgressTimeTV;
    private TextView mDurationTimeTV;
    private SeekBar mProgressBar;
    private ImageButton mPreviousIB;
    private ImageButton mPlayIB;
    private ImageButton mPauseIB;
    private ImageButton mNextIB;
    // 跟气泡相关
    LayoutInflater mLayoutInflater;
    View mBubbleView;
    // 气泡上显示时间
    TextView mShowTimeTV;
    BubblePopupWindow mBubblePopupWindow;

    private Handler mUiHandler;

    private void initData() {
        EventBusUtils.register(this);
        registerHeadsetPlugReceiver();
        // Volume change should always affect media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                JniPlayerActivity.this.uiHandleMessage(msg);
            }
        };

        mLayoutInflater = LayoutInflater.from(this);
        mBubbleView = mLayoutInflater.inflate(R.layout.layout_popup_view, null);
        mShowTimeTV = mBubbleView.findViewById(R.id.content_tv);
        mBubblePopupWindow = new BubblePopupWindow(this);
        mBubblePopupWindow.setBubbleView(mBubbleView);
        mShowTimeTV.setOnClickListener(mOnClickListener);

        mPath = getIntent().getStringExtra(CONTENT_PATH);

        mLoadingView = findViewById(R.id.loading_view);
        mControllerPanelLayout = findViewById(R.id.controller_panel_layout);
        mFileNameTV = findViewById(R.id.file_name_tv);
        mProgressTimeTV = findViewById(R.id.progress_time_tv);
        mDurationTimeTV = findViewById(R.id.duration_time_tv);
        mProgressBar = findViewById(R.id.progress_bar);
        mPreviousIB = findViewById(R.id.button_prev);
        mPlayIB = findViewById(R.id.button_play);
        mPauseIB = findViewById(R.id.button_pause);
        mNextIB = findViewById(R.id.button_next);
        mSurfaceView = findViewById(R.id.surfaceView);
        mPreviousIB.setOnClickListener(mOnClickListener);
        mPlayIB.setOnClickListener(mOnClickListener);
        mPauseIB.setOnClickListener(mOnClickListener);
        mNextIB.setOnClickListener(mOnClickListener);
        mSurfaceView.setOnClickListener(mOnClickListener);

        mFFMPEGPlayer = new FFMPEG();

        int duration = (int) mFFMPEGPlayer.getDuration();
        int currentPosition = (int) mPresentationTime;
        float pos = (float) currentPosition / duration;
        int target = Math.round(pos * mProgressBar.getMax());
        mProgressBar.setProgress(target);
        mProgressBar.setSecondaryProgress(0);
        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // Tracking start
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            // Tracking
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

            // Tracking end
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

    @SuppressLint("InvalidWakeLockTag")
    private void internalStart() {
        // When player view started,wake the lock.
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mPowerWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        mPowerWakeLock.acquire();
    }

    private void internalResume() {
        /*contentLength = (Long) EventBusUtils.post(
                DownloadFileService.class,
                DownloadFileService.MSG_GET_CONTENT_LENGTH,
                null);
        EventBusUtils.post(
                DownloadFileService.class,
                DownloadFileService.MSG_SET_CALLBACK,
                new Object[]{mDownloadCallback});*/

        if (mSurface == null) {
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

                    mSurface = holder.getSurface();
                    // 这里也要写
                    holder.setFormat(PixelFormat.RGBA_8888);

                    mFFMPEGPlayer.setCallback(mFFMPEGPlayer.mCallback);
                    mFFMPEGPlayer.setHandler(mUiHandler);
                    mFFMPEGPlayer.setSurface(mPath, mSurface);

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
                        mFFMPEGPlayer = null;
                    }
                }
            });
        }

        if (mFFMPEGPlayer != null) {
            if (mFFMPEGPlayer.isRunning()) {
                if (!mFFMPEGPlayer.isPlaying()) {
                    mPlayIB.setVisibility(View.VISIBLE);
                    mPauseIB.setVisibility(View.GONE);
                    mControllerPanelLayout.setVisibility(View.INVISIBLE);
                    mFFMPEGPlayer.play();
                }
            }
        }
    }

    private void internalStop() {
        if (mFFMPEGPlayer != null) {
            if (mFFMPEGPlayer.isRunning()) {
                if (mFFMPEGPlayer.isPlaying()) {
                    mPlayIB.setVisibility(View.GONE);
                    mPauseIB.setVisibility(View.VISIBLE);
                    mControllerPanelLayout.setVisibility(View.VISIBLE);
                    mFFMPEGPlayer.pause();
                }
            }
        }
    }

    private void internalDestroy() {
        if (mPowerWakeLock != null && mPowerWakeLock.isHeld()) {
            mPowerWakeLock.release();
            mPowerWakeLock = null;
        }
        unregisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
    }

    private void showBubbleView(String time, View view) {
        mShowTimeTV.setText(time);
        mBubblePopupWindow.show(view);
    }

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
            case Callback.MSG_ON_PLAYED:
                durationTime = DateUtils.formatElapsedTime(mFFMPEGPlayer.getDuration());
                mDurationTimeTV.setText(durationTime);
                mLoadingView.setVisibility(View.GONE);
                mPlayIB.setVisibility(View.VISIBLE);
                mPauseIB.setVisibility(View.GONE);
                mControllerPanelLayout.setVisibility(View.VISIBLE);// INVISIBLE
                break;
            case Callback.MSG_ON_PAUSED:
                mPlayIB.setVisibility(View.GONE);
                mPauseIB.setVisibility(View.VISIBLE);
                mLoadingView.setVisibility(View.VISIBLE);
                mControllerPanelLayout.setVisibility(View.VISIBLE);
                break;
            case Callback.MSG_ON_FINISHED:
            case Callback.MSG_ON_ERROR:
                finish();
                exitActivity();
                break;
            case Callback.MSG_ON_PROGRESS_UPDATED:
                if (msg.obj == null) {
                    return;
                }
                mPresentationTime = (Long) msg.obj;
                String curElapsedTime = DateUtils.formatElapsedTime(mPresentationTime);
                mProgressTimeTV.setText(curElapsedTime);

                if (mNeedToSyncProgressBar) {
                    int duration = (int) (mFFMPEGPlayer.getDuration());
                    int currentPosition = (int) (mPresentationTime);
                    float pos = (float) currentPosition / duration;
                    int target = Math.round(pos * mProgressBar.getMax());
                    mProgressBar.setProgress(target);
                }
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:// 单击事件
                if (firstFlag && secondFlag && threeFlag) {
                    /*if (DEBUG)
                        Log.d(TAG, "onKeyDown() 3");*/

                    if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
                        mNeedToSyncProgressBar = true;
                        mControllerPanelLayout.setVisibility(View.INVISIBLE);
                    } else {
                        mControllerPanelLayout.setVisibility(View.VISIBLE);
                    }
                } else if (firstFlag && secondFlag) {
                    /*if (DEBUG)
                        Log.d(TAG, "onKeyDown() 2");*/

                    if (mFFMPEGPlayer != null) {
                        Log.d(TAG, "onKeyDown() mPresentationTime: " + mPresentationTime);
                        mFFMPEGPlayer.seekTo(mPresentationTime + 30);
                    }
                } else {
                    /*if (DEBUG)
                        Log.d(TAG, "onKeyDown() 1");*/

                    // 播放与暂停
                    if (mFFMPEGPlayer != null) {
                        if (mFFMPEGPlayer.isRunning()) {
                            if (mFFMPEGPlayer.isPlaying()) {
                                mPlayIB.setVisibility(View.GONE);
                                mPauseIB.setVisibility(View.VISIBLE);
                                mControllerPanelLayout.setVisibility(View.VISIBLE);
                                mFFMPEGPlayer.pause();
                            } else {
                                mPlayIB.setVisibility(View.VISIBLE);
                                mPauseIB.setVisibility(View.GONE);
                                mControllerPanelLayout.setVisibility(View.INVISIBLE);
                                mFFMPEGPlayer.play();
                            }
                        }
                    }
                    /*if (!mIsScreenPress) {
                    }
                    mIsScreenPress = false;*/
                }
                firstFlag = false;
                secondFlag = false;
                threeFlag = false;
                break;
            case MSG_START_PLAYBACK:
                if (!isDestroyed()) {
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
                }
                break;
            case PLAYBACK_PROGRESS_UPDATED:
                mProgressBar.setSecondaryProgress(mDownloadProgress);
                break;
            default:
                break;
        }
    }

    private DownloadCallback mDownloadCallback = new DownloadCallback() {
        @Override
        public void onReady() {

        }

        @Override
        public void onPaused() {

        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onFinished() {

        }

        @Override
        public void onProgressUpdated(long readDataSize) {
            if (contentLength == -1) {
                contentLength = readDataSize;
                return;
            }

            int progress = (int) ((readDataSize / (contentLength * 1.00)) * 3840);
            if (progress > mDownloadProgress || progress == 3840) {
                mDownloadProgress = progress;
                // MLog.i(TAG, "onProgressUpdated() progress: " + progress);
                mUiHandler.removeMessages(MSG_ON_PROGRESS_UPDATED);
                mUiHandler.sendEmptyMessage(MSG_ON_PROGRESS_UPDATED);
            }
        }

        @Override
        public void onError() {

        }

        @Override
        public void onInfo(String info) {

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
                    /*if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
                        mNeedToSyncProgressBar = true;
                        mControllerPanelLayout.setVisibility(View.INVISIBLE);
                    } else {
                        mControllerPanelLayout.setVisibility(View.VISIBLE);
                    }*/

                    mIsScreenPress = true;
                    EventBusUtils.post(
                            JniPlayerActivity.this, KeyEvent.KEYCODE_HEADSETHOOK, null);
                    break;
                case R.id.content_tv:
                    mNeedToSyncProgressBar = true;
                    mBubblePopupWindow.dismiss();
                    MLog.d("player_alexander", "onClick() mProgress: " + mProgress +
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

    /////////////////////////////////////////////////////////////////

    /***
     下面是耳机操作
     */

    // Android监听耳机的插拔事件(只能动态注册,经过测试可行)
    private HeadsetPlugReceiver mHeadsetPlugReceiver;
    private AudioManager mAudioManager;
    private ComponentName mMediaButtonReceiver;

    private void registerHeadsetPlugReceiver() {
        mHeadsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.setPriority(2147483647);
        registerReceiver(mHeadsetPlugReceiver, filter);

        mAudioManager =
                (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiver = new ComponentName(
                getPackageName(), MediaButtonReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiver);
    }

    private void unregisterHeadsetPlugReceiver() {
        if (mHeadsetPlugReceiver == null) {
            return;
        }

        unregisterReceiver(mHeadsetPlugReceiver);
        mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiver);
    }

    private class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                switch (intent.getIntExtra("state", 0)) {
                    case 0:
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset not connected");
                        //pause();
                        break;
                    case 1:
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset has connected");
                        //play();
                        break;
                    default:
                }
            }
        }
    }

}