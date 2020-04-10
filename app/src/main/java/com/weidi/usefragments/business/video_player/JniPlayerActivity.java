package com.weidi.usefragments.business.video_player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.BaseActivity;
import com.weidi.usefragments.R;
import com.weidi.usefragments.receiver.MediaButtonReceiver;
import com.weidi.usefragments.test_view.BubblePopupWindow;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;

import java.io.File;

import static com.weidi.usefragments.MainActivity1.isRunService;

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
        internalPause();
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
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + printThis());
        internalDestroy();
        super.onDestroy();
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
        if (DEBUG)
            Log.d(TAG, "onConfigurationChanged(): " + printThis() +
                    " newConfig: " + newConfig.toString());

        if (noFinish) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 横屏的时候隐藏刘海屏的刘海部分
                if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
                    getWindow().setAttributes(lp);
                } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // 竖屏的时候展示刘海屏的刘海部分
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    getWindow().setAttributes(lp);
                }
            }

            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                /*if (mPlayerWrapper != null) {
                    mPlayerWrapper.handleLandscapeScreen(0);
                }*/
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{0});
            } else {
                /*if (mPlayerWrapper != null) {
                    mPlayerWrapper.handlePortraitScreen();
                }*/
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
        }
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

        if (hasFocus) {
            if (getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                // 需要无状态栏的横屏
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{0});
            } else {
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////

    public static final String CONTENT_PATH = "content_path";
    public static final String COMMAND_NO_FINISH = "command_no_finish";

    public static boolean isAliveJniPlayerActivity = false;

    private PlayerWrapper mPlayerWrapper;
    private String mPath;

    public SurfaceView mSurfaceView;
    public LinearLayout mControllerPanelLayout;
    public ProgressBar mLoadingView;
    public SeekBar mProgressBar;
    public TextView mFileNameTV;
    public TextView mProgressTimeTV;
    public TextView mDurationTimeTV;
    public ImageButton mPreviousIB;
    public ImageButton mPlayIB;
    public ImageButton mPauseIB;
    public ImageButton mNextIB;

    // 跟气泡相关
    private LayoutInflater mLayoutInflater;
    private View mBubbleView;
    // 气泡上显示时间
    private TextView mShowTimeTV;
    private BubblePopupWindow mBubblePopupWindow;
    private boolean noFinish;


    private void internalCreate() {
        Intent intent = getIntent();
        // 为flase时表示从外部打开一个视频进行播放.为true时只是使用Activity的全屏特性(在本应用打开).
        noFinish = intent.getBooleanExtra(COMMAND_NO_FINISH, false);

        if (noFinish) {
            isAliveJniPlayerActivity = true;
            if (getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
        setContentView(R.layout.transparent_player);

        if (noFinish) {
            return;
        }

        // 在本应用开启当前Activity时能得到这个路径,从其他应用打开时为null
        mPath = intent.getStringExtra(CONTENT_PATH);
        if (TextUtils.isEmpty(mPath)) {
            MLog.d(TAG, "internalCreate() mPath is null");
            /***
             1.
             uri : content://media/external/video/media/272775
             path: /external/video/media/272775
             2.
             uri : content://com.huawei.hidisk.fileprovider
             /root/storage/1532-48AD/Videos/download/25068919/1/32/audio.m4s
             path: /root/storage/1532-48AD/Videos/download/25068919/1/32/audio.m4s
             */
            Uri uri = intent.getData();
            if (uri != null) {
                MLog.d(TAG, "internalCreate()    uri: " + uri.toString());
                mPath = uri.getPath();
                if (!mPath.substring(mPath.lastIndexOf("/")).contains(".")) {
                    // 如: /external/video/media/272775
                    String[] proj = {MediaStore.Images.Media.DATA};
                    Cursor actualimagecursor = this.managedQuery(
                            uri, proj, null, null, null);
                    int actual_image_column_index =
                            actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    actualimagecursor.moveToFirst();
                    mPath = actualimagecursor.getString(actual_image_column_index);
                }
                // 这个路径是不对的
                // /root/storage/1532-48AD/Videos/download/25068919/1/32/audio.m4s
                MLog.d(TAG, "internalCreate() mPath1: " + mPath);
                if (mPath.contains("/root")) {
                    mPath = mPath.substring(5);
                }
                MLog.d(TAG, "internalCreate() mPath2: " + mPath);
                if (TextUtils.isEmpty(mPath)) {
                    finish();
                    return;
                }
            }
            // video/mp4
            // audio/mpeg audio/quicktime(flac) audio/x-ms-wma(wma) audio/x-wav(wav)
            // audio/amr(amr) audio/mp3
            String type = intent.getType();
            MLog.d(TAG, "internalCreate()  type: " + type);
            if (TextUtils.isEmpty(type)
                    || (!type.startsWith("video/")
                    && !type.startsWith("audio/"))) {
                finish();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isRunService(this,
                    "com.weidi.usefragments.business.video_player.PlayerService")) {
                MLog.d(TAG, "internalCreate() PlayerService is not alive");
                if (!Settings.canDrawOverlays(this)) {
                    MLog.d(TAG, "internalCreate() startActivityForResult");
                    intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 0);
                } else {
                    MLog.d(TAG, "internalCreate() start PlayerService");
                    intent = new Intent();
                    intent.setClass(this, PlayerService.class);
                    intent.setAction(PlayerService.COMMAND_ACTION);
                    intent.putExtra(PlayerService.COMMAND_PATH, mPath);
                    intent.putExtra(PlayerService.COMMAND_TYPE, intent.getType());
                    intent.putExtra(PlayerService.COMMAND_NAME, PlayerService.COMMAND_SHOW_WINDOW);
                    startService(intent);
                }
            } else {
                MLog.d(TAG, "internalCreate() PlayerService is alive");
                //FFMPEG.getDefault().setMode(FFMPEG.USE_MODE_MEDIA);
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_SHOW_WINDOW,
                        new Object[]{mPath, intent.getType()});
            }
        }

        finish();

        // Volume change should always affect media volume_normal
        /*setVolumeControlStream(AudioManager.STREAM_MUSIC);*/

        /*mSurfaceView = findViewById(R.id.surfaceView);
        mControllerPanelLayout = findViewById(R.id.controller_panel_layout);
        mLoadingView = findViewById(R.id.loading_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mFileNameTV = findViewById(R.id.file_name_tv);
        mProgressTimeTV = findViewById(R.id.progress_time_tv);
        mDurationTimeTV = findViewById(R.id.duration_time_tv);
        mPreviousIB = findViewById(R.id.button_prev);
        mPlayIB = findViewById(R.id.button_play);
        mPauseIB = findViewById(R.id.button_pause);
        mNextIB = findViewById(R.id.button_next);

        mPlayerWrapper = new PlayerWrapper();
        mPlayerWrapper.setActivity(this, null);
        mPlayerWrapper.setPath(mPath);
        mPlayerWrapper.onCreate();*/
    }

    private void internalStart() {

    }

    private void internalResume() {
        if (mPlayerWrapper != null) {
            mPlayerWrapper.onResume();
        }
    }

    private void internalPause() {
        if (mPlayerWrapper != null) {
            mPlayerWrapper.onPause();
            finish();
        }
    }

    private void internalStop() {
        if (mPlayerWrapper != null) {
            mPlayerWrapper.onStop();
        }
    }

    private void internalDestroy() {
        if (noFinish) {
            isAliveJniPlayerActivity = false;
            if (getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_LANDSCAPE) {
                // 需要有状态栏的横屏
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_LANDSCAPE_SCREEN,
                        new Object[]{1});
            } else {
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_HANDLE_PORTRAIT_SCREEN,
                        null);
            }
        }

        if (mPlayerWrapper != null) {
            mPlayerWrapper.onDestroy();
        }
    }

}