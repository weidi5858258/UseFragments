package com.weidi.usefragments.business.video_player;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;
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
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.service.DownloadFileService;
import com.weidi.usefragments.test_view.BubblePopupWindow;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.business.contents.Contents;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.JniObject;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;

/***

 */
public class PlayerActivity extends BaseActivity {

    private static final String TAG =
            PlayerActivity.class.getSimpleName();
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
        if (mSampleVideoPlayer != null) {
            mSampleVideoPlayer.release();
        }
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

    private static final int PLAYBACK_INFO = 0x001;
    private static final int PLAYBACK_PROGRESS_UPDATED = 0x002;
    private static final int PLAYBACK_PROGRESS_CHANGED = 0x003;
    private static final int MSG_ON_READY = 0x004;
    private static final int MSG_LOADING_SHOW = 0x005;
    private static final int MSG_LOADING_HIDE = 0x006;
    private static final int MSG_ON_PROGRESS_UPDATED = 0x007;

    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private PowerManager.WakeLock mPowerWakeLock;
    // private SimpleVideoPlayer mSampleVideoPlayer;
    // private SimpleVideoPlayer10 mSampleVideoPlayer;
    private SimpleVideoPlayer2 mSampleVideoPlayer;
    // private SimpleVideoPlayer8 mSampleVideoPlayer;
    // private OnlyVideoPlayer mSampleVideoPlayer;
    private String mPath;
    private long mProgressUs;
    private long mPresentationTimeUs;
    private int mDownloadProgress = -1;
    private long contentLength = -1;
    private boolean mNeedToSyncProgressBar = true;
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
        // startService(new Intent(this, MediaDataService.class));
        // Volume change should always affect media volume_normal
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerActivity.this.uiHandleMessage(msg);
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
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(
                    SurfaceHolder holder) {
                MLog.d(TAG, "surfaceCreated()");
                mSurface = holder.getSurface();

                /*mSampleVideoPlayer.setSurface(mSurface);
                mSampleVideoPlayer.play();*/
                //nextHandle();

                /*SimpleVideoPlayer mSampleVideoPlayer = new SimpleVideoPlayer();
                mSampleVideoPlayer.setContext(getContext());
                mSampleVideoPlayer.setPath(mVideoPath);
                mSampleVideoPlayer.setSurface(mSurface);
                mSampleVideoPlayer.play();*/

                mSampleVideoPlayer.setContext(getContext());
                mSampleVideoPlayer.setPath(mPath);
                mSampleVideoPlayer.setSurface(mSurface);
                mSampleVideoPlayer.setCallback(mCallback);
                //mSampleVideoPlayer.setProgressUs(300160000L);
                mSampleVideoPlayer.play();
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(
                    SurfaceHolder holder) {
                MLog.d(TAG, "surfaceDestroyed()");
            }
        });

        // mSampleVideoPlayer = new SimpleVideoPlayer();
        // mSampleVideoPlayer = new SimpleVideoPlayer10();
        mSampleVideoPlayer = new SimpleVideoPlayer2();
        // mSampleVideoPlayer = new SimpleVideoPlayer8();
        // mSampleVideoPlayer = new OnlyVideoPlayer();

        int duration = (int) mSampleVideoPlayer.getDurationUs() / 1000;
        int currentPosition = (int) mPresentationTimeUs / 1000;
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
                    long tempProgress =
                            (long) ((progress / 3840.00) * mSampleVideoPlayer.getDurationUs());
                    mProgressUs = tempProgress;
                    String elapsedTime =
                            DateUtils.formatElapsedTime(tempProgress / 1000 / 1000);
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
                null);*/
        EventBusUtils.post(
                DownloadFileService.class,
                DownloadFileService.MSG_SET_CALLBACK,
                new Object[]{mDownloadCallback});
    }

    private void internalStop() {
        if (mPowerWakeLock != null) {
            mPowerWakeLock.release();
            mPowerWakeLock = null;
        }
    }

    private void internalDestroy() {

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
            case PLAYBACK_PROGRESS_UPDATED:
                String curElapsedTime = DateUtils.formatElapsedTime(
                        (mPresentationTimeUs / 1000) / 1000);
                mProgressTimeTV.setText(curElapsedTime);

                if (mNeedToSyncProgressBar) {
                    int duration = (int) (mSampleVideoPlayer.getDurationUs() / 1000);
                    int currentPosition = (int) (mPresentationTimeUs / 1000);
                    float pos = (float) currentPosition / duration;
                    int target = Math.round(pos * mProgressBar.getMax());
                    mProgressBar.setProgress(target);
                }
                break;
            case PLAYBACK_PROGRESS_CHANGED:
                long progressUs = (long) ((mProgressUs / 3840.00) * mSampleVideoPlayer
                        .getDurationUs());
                mSampleVideoPlayer.setProgressUs(progressUs);
                break;
            case MSG_ON_READY:
                String durationTime = DateUtils.formatElapsedTime(
                        (mSampleVideoPlayer.getDurationUs() / 1000) / 1000);
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
            case MSG_LOADING_SHOW:
                mLoadingView.setVisibility(View.VISIBLE);
                break;
            case MSG_LOADING_HIDE:
                mLoadingView.setVisibility(View.GONE);
                break;
            case MSG_ON_PROGRESS_UPDATED:
                mProgressBar.setSecondaryProgress(mDownloadProgress);
                break;
            default:
                break;
        }
    }

    private Callback mCallback = new Callback() {
        private boolean onlyOne = true;

        @Override
        public int onTransact(int code, Parcel data, Parcel reply) {
            return 0;
        }

        @Override
        public int onTransact(int code, JniObject jniObject) {
            return 0;
        }

        @Override
        public void onReady() {
            mUiHandler.removeMessages(MSG_ON_TRANSACT_READY);
            mUiHandler.sendEmptyMessage(MSG_ON_TRANSACT_READY);
        }

        @Override
        public void onChangeWindow(int width, int height) {

        }

        @Override
        public void onPaused() {
            mUiHandler.removeMessages(MSG_LOADING_SHOW);
            mUiHandler.sendEmptyMessage(MSG_LOADING_SHOW);
        }

        @Override
        public void onPlayed() {
            mUiHandler.removeMessages(MSG_LOADING_HIDE);
            mUiHandler.sendEmptyMessage(MSG_LOADING_HIDE);
            if (onlyOne) {
                onlyOne = false;
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        String durationTime = DateUtils.formatElapsedTime(
                                (mSampleVideoPlayer.getDurationUs() / 1000) / 1000);
                        mDurationTimeTV.setText(durationTime);
                    }
                });
            }
        }

        @Override
        public void onFinished() {
            //mSampleVideoPlayer.play();
            finish();
        }

        @Override
        public void onProgressUpdated(long presentationTimeUs) {
            mPresentationTimeUs = presentationTimeUs;
            mUiHandler.removeMessages(PLAYBACK_PROGRESS_UPDATED);
            mUiHandler.sendEmptyMessage(PLAYBACK_PROGRESS_UPDATED);
        }

        @Override
        public void onError(int error, String errorInfo) {

        }

        @Override
        public void onInfo(String info) {

        }
    };

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

    private static final int STEP = 1000;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_prev:
                    // 声音太快执行这里
                    MediaUtils.variableValues -= STEP;
                    MLog.i(TAG, "MediaUtils.variableValues: " + MediaUtils.variableValues);
                    break;
                case R.id.button_play:
                    if (mSampleVideoPlayer.isRunning()) {
                        if (mSampleVideoPlayer.isPlaying()) {
                            mPlayIB.setVisibility(View.GONE);
                            mPauseIB.setVisibility(View.VISIBLE);
                            mSampleVideoPlayer.pause();
                        }
                    }
                    break;
                case R.id.button_pause:
                    if (mSampleVideoPlayer.isRunning()) {
                        if (!mSampleVideoPlayer.isPlaying()) {
                            mPlayIB.setVisibility(View.VISIBLE);
                            mPauseIB.setVisibility(View.GONE);
                            mSampleVideoPlayer.play();
                        }
                    }
                    break;
                case R.id.button_next:
                    // 声音太慢执行这里
                    MediaUtils.variableValues += STEP;
                    MLog.i(TAG, "MediaUtils.variableValues: " + MediaUtils.variableValues);
                    break;
                case R.id.surfaceView:
                    if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
                        mNeedToSyncProgressBar = true;
                        mControllerPanelLayout.setVisibility(View.INVISIBLE);
                    } else {
                        mControllerPanelLayout.setVisibility(View.VISIBLE);
                    }
                    break;
                case R.id.content_tv:
                    mNeedToSyncProgressBar = true;
                    mSampleVideoPlayer.setProgressUs(mProgressUs);
                    mBubblePopupWindow.dismiss();
                    break;
                default:
                    break;
            }
        }
    };

}