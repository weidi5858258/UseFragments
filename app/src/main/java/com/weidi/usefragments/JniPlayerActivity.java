package com.weidi.usefragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.weidi.usefragments.tool.FFMPEG;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;

/***

 */
public class JniPlayerActivity extends BaseActivity {

    private static final String TAG =
            JniPlayerActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_jni);
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

                FFMPEG.simpleAudioPlayer(mSurface);
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

}