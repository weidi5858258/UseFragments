package com.weidi.usefragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.service.DownloadFileService;
import com.weidi.usefragments.test_view.BubblePopupWindow;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.Contents;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.FFMPEG;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;
import com.weidi.usefragments.tool.SimpleVideoPlayer7;

/***

 */
public class JniPlayerActivity2 extends BaseActivity {

    private static final String TAG =
            JniPlayerActivity2.class.getSimpleName();
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
        internalStart();
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
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            MLog.d(TAG, "onPause(): " + printThis());
        internalStop();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            MLog.d(TAG, "onStop(): " + printThis());
    }

    @Override
    public void onDestroy() {
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

    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private PowerManager.WakeLock mPowerWakeLock;

    private void initData() {
        // Volume change should always affect media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mSurfaceView = findViewById(R.id.surfaceView);
        // 没有图像出来,就是由于没有设置PixelFormat.RGBA_8888
        // 这里要写
        mSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(
                    SurfaceHolder holder) {
                MLog.d(TAG, "surfaceCreated()");
                mSurface = holder.getSurface();
                // 这里也要写
                holder.setFormat(PixelFormat.RGBA_8888);

                // Test
                FFMPEG ffmpeg = new FFMPEG();
                ffmpeg.setSurface(mSurface);
                ffmpeg.play();
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

}