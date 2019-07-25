package com.weidi.usefragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;
import com.weidi.usefragments.tool.SampleVideoPlayer;
import com.weidi.usefragments.tool.SampleVideoPlayer7;

/***

 */
public class PlayerActivity extends BaseActivity {

    private static final String TAG =
            PlayerActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    private static final int PLAYBACK_INFO = 0x001;
    private static final int PLAYBACK_PROGRESS_UPDATED = 0x002;
    private static final int PLAYBACK_PROGRESS_CHANGED = 0x003;

    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private PowerManager.WakeLock mPowerWakeLock;
    //    private SampleVideoPlayer mSampleVideoPlayer;
    private SampleVideoPlayer7 mSampleVideoPlayer;
    private int mProgress;
    private long mPresentationTimeUs;
    private LinearLayout mControllerPanelLayout;
    private TextView mFileNameTV;
    private TextView mProgressTimeTV;
    private TextView mDurationTimeTV;
    private SeekBar mProgressBar;
    private ImageButton mPreviousIB;
    private ImageButton mPlayPauseIB;
    private ImageButton mNextIB;

    private Handler mUiHandler;

    private void initData() {
        // Volume change should always affect media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerActivity.this.uiHandleMessage(msg);
            }
        };

        mControllerPanelLayout = findViewById(R.id.controller_panel_layout);
        mFileNameTV = findViewById(R.id.file_name_tv);
        mProgressTimeTV = findViewById(R.id.progress_time_tv);
        mDurationTimeTV = findViewById(R.id.duration_time_tv);
        mProgressBar = findViewById(R.id.progress_bar);
        mPreviousIB = findViewById(R.id.button_prev);
        mPlayPauseIB = findViewById(R.id.button_play_pause);
        mNextIB = findViewById(R.id.button_next);
        mSurfaceView = findViewById(R.id.surfaceView);
        mPreviousIB.setOnClickListener(mOnClickListener);
        mPlayPauseIB.setOnClickListener(mOnClickListener);
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
                //next();

                /*SampleVideoPlayer mSampleVideoPlayer = new SampleVideoPlayer();
                mSampleVideoPlayer.setContext(getContext());
                mSampleVideoPlayer.setPath(mVideoPath);
                mSampleVideoPlayer.setSurface(mSurface);
                mSampleVideoPlayer.play();*/

                mSampleVideoPlayer.setContext(getContext());
                mSampleVideoPlayer.setPath(null);
                mSampleVideoPlayer.setSurface(mSurface);
                mSampleVideoPlayer.setCallback(mCallback);
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

        //        mSampleVideoPlayer = new SampleVideoPlayer();
        mSampleVideoPlayer = new SampleVideoPlayer7();

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
                    mProgress = progress;
                    mUiHandler.removeMessages(PLAYBACK_PROGRESS_CHANGED);
                    mUiHandler.sendEmptyMessageDelayed(PLAYBACK_PROGRESS_CHANGED, 50);
                }
            }

            // Tracking end
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void internalStart() {
        // When player view started,wake the lock.
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mPowerWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        mPowerWakeLock.acquire();
    }

    private void internalStop() {
        if (mPowerWakeLock != null) {
            mPowerWakeLock.release();
            mPowerWakeLock = null;
        }
    }

    private void internalDestroy() {
        if (mSampleVideoPlayer != null) {
            mSampleVideoPlayer.release();
        }
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

                int duration = (int) (mSampleVideoPlayer.getDurationUs() / 1000);
                int currentPosition = (int) (mPresentationTimeUs / 1000);
                float pos = (float) currentPosition / duration;
                int target = Math.round(pos * mProgressBar.getMax());
                mProgressBar.setProgress(target);
                break;
            case PLAYBACK_PROGRESS_CHANGED:
                long process = (long) ((mProgress / 3840.00) * mSampleVideoPlayer.getDurationUs());
                mSampleVideoPlayer.setProgressUs(process);
                MLog.d(TAG, "uiHandleMessage() process: " + process +
                        " " + DateUtils.formatElapsedTime(process / 1000 / 1000));
                break;
            default:
                break;
        }
    }

    private Callback mCallback = new Callback() {
        @Override
        public void onReady() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    String durationTime = DateUtils.formatElapsedTime(
                            (mSampleVideoPlayer.getDurationUs() / 1000) / 1000);
                    mDurationTimeTV.setText(durationTime);
                    if (durationTime.length() > 5) {
                        mProgressTimeTV.setText("00:00:00");
                    } else {
                        mProgressTimeTV.setText("00:00");
                    }
                    mProgressBar.setProgress(0);
                }
            });
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
        public void onProgressUpdated(long presentationTimeUs) {
            mPresentationTimeUs = presentationTimeUs;
            mUiHandler.removeMessages(PLAYBACK_PROGRESS_UPDATED);
            mUiHandler.sendEmptyMessage(PLAYBACK_PROGRESS_UPDATED);
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
                    break;
                case R.id.button_play_pause:
                    break;
                case R.id.button_next:
                    break;
                case R.id.surfaceView:
                    if (mControllerPanelLayout.getVisibility() == View.VISIBLE) {
                        mControllerPanelLayout.setVisibility(View.INVISIBLE);
                    } else {
                        mControllerPanelLayout.setVisibility(View.VISIBLE);
                    }
                    break;
                default:
                    break;
            }
        }
    };

}