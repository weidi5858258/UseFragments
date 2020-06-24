package com.weidi.usefragments.business.video_player;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.MainActivity1;
import com.weidi.usefragments.R;
import com.weidi.usefragments.receiver.MediaButtonReceiver;
import com.weidi.usefragments.tool.MLog;

import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

/***
 Created by root on 19-8-5.
 */

public class PlayerService extends Service {

    /*private static final String TAG =
            FloatingService.class.getSimpleName();*/
    private static final String TAG = "player_alexander";
    private static final boolean DEBUG = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        MLog.i(TAG, "onBind() intent: " + intent);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MLog.i(TAG, "onUnbind() intent: " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MLog.i(TAG, "onCreate()");
        internalCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MLog.i(TAG, "onStartCommand() intent: " + intent);
        internalStartCommand(intent, flags, startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        MLog.i(TAG, "onDestroy()");
        internalDestroy();
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////

    public static final int MSG_DOWNLOAD_START = 1;
    public static final int MSG_DOWNLOAD_PAUSE_OR_START = 2;
    public static final int MSG_DOWNLOAD_STOP = 3;
    public static final int MSG_IS_DOWNLOADING = 4;
    public static final int MSG_SET_CALLBACK = 5;
    public static final int MSG_GET_CONTENT_LENGTH = 6;
    public static final int MSG_TEST = 1000;
    private static final int BUFFER = 1024 * 1024 * 2;

    private Handler mUiHandler;
    private PlayerWrapper mPlayerWrapper;
    public boolean mIsAddedView = false;
    private String mPrePath = null;
    private String mCurPath = null;

    public WindowManager mWindowManager;
    public WindowManager.LayoutParams mLayoutParams;
    public View mRootView;
    private View mTestView;

    private void internalCreate() {
        if (mPlayerWrapper == null) {
            mPlayerWrapper = new PlayerWrapper();
        }

        EventBusUtils.register(this);
        registerHeadsetPlugReceiver();
        registerTelephonyReceiver();

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerService.this.uiHandleMessage(msg);
            }
        };

        initPlayerWindow();
    }

    /***
     播放视频
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 1 \
     --es HandlePlayerServicePath "video/" \
     --es HandlePlayerServicePath "http://wttv-lh.akamaihd
     .net:80/i/WTTVBreaking_1@333494/index_3000_av-b.m3u8"
     停止播放
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 2
     停止服务
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 3
     重新加载内容
     adb shell am startservice \
     -n com.weidi.usefragments/com.weidi.usefragments.business.video_player.PlayerService \
     -a com.weidi.usefragments.business.video_player.PlayerService \
     --ei HandlePlayerService 4
     */

    public static final String COMMAND_ACTION =
            "com.weidi.usefragments.business.video_player.PlayerService";
    public static final String COMMAND_NAME = "HandlePlayerService";
    public static final String COMMAND_PATH = "HandlePlayerServicePath";
    public static final String COMMAND_TYPE = "HandlePlayerServiceType";
    public static final int COMMAND_SHOW_WINDOW = 1;
    public static final int COMMAND_HIDE_WINDOW = 2;
    public static final int COMMAND_STOP_SERVICE = 3;
    public static final int COMMAND_RESTART_LOAD_CONTENTS = 4;
    public static final int COMMAND_HANDLE_LANDSCAPE_SCREEN = 5;
    public static final int COMMAND_HANDLE_PORTRAIT_SCREEN = 6;
    public static final int COMMAND_TEST = 7;

    // 测试时使用
    private void internalStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // app crash后的操作
            SharedPreferences sp = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            boolean isNormalFinish = sp.getBoolean(PlayerWrapper.PLAYBACK_NORMAL_FINISH, true);
            mCurPath = sp.getString(PlayerWrapper.PLAYBACK_ADDRESS, null);
            if (!isNormalFinish && !TextUtils.isEmpty(mCurPath)) {
                String type = sp.getString(PlayerWrapper.PLAYBACK_MEDIA_TYPE, null);
                if (TextUtils.isEmpty(type)
                        || type.startsWith("video/")) {
                    intent = new Intent();
                    intent.setClass(getApplicationContext(), MainActivity1.class);
                    startActivity(intent);

                    mPlayerWrapper.setType(type);
                    mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                    mUiHandler.sendEmptyMessageDelayed(COMMAND_SHOW_WINDOW, 3000);
                }
            }
            return;
        }

        String action = intent.getAction();
        MLog.d(TAG, "internalStartCommand()   action: " + action);
        if (!TextUtils.equals(COMMAND_ACTION, action)) {
            return;
        }
        Uri uri = intent.getData();
        if (uri != null) {
            mCurPath = uri.getPath();
        } else {
            mCurPath = intent.getStringExtra(COMMAND_PATH);
        }
        mPlayerWrapper.setType(intent.getStringExtra(COMMAND_TYPE));
        MLog.d(TAG, "internalStartCommand() mCurPath: " + mCurPath);
        int commandName = intent.getIntExtra(COMMAND_NAME, 0);
        switch (commandName) {
            case COMMAND_SHOW_WINDOW:
                mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_SHOW_WINDOW);
                break;
            case COMMAND_HIDE_WINDOW:
                mUiHandler.removeMessages(COMMAND_HIDE_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_HIDE_WINDOW);
                break;
            case COMMAND_STOP_SERVICE:
                mUiHandler.removeMessages(COMMAND_STOP_SERVICE);
                mUiHandler.sendEmptyMessage(COMMAND_STOP_SERVICE);
                break;
            case COMMAND_RESTART_LOAD_CONTENTS:
                mPlayerWrapper.sendMessageForLoadContents();
                break;
            default:
                break;
        }
    }

    private void internalDestroy() {
        mPlayerWrapper.onDestroy();
        mWindowManager.removeView(mTestView);
        removeView();
        unregisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case COMMAND_SHOW_WINDOW:
                if (objArray != null && objArray.length >= 2) {
                    mCurPath = (String) objArray[0];
                    mPlayerWrapper.setDataSource(mCurPath);
                    mPlayerWrapper.setType((String) objArray[1]);
                }
                mUiHandler.removeMessages(COMMAND_SHOW_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_SHOW_WINDOW);
                break;
            case COMMAND_HIDE_WINDOW:
                mUiHandler.removeMessages(COMMAND_HIDE_WINDOW);
                mUiHandler.sendEmptyMessage(COMMAND_HIDE_WINDOW);
                break;
            case COMMAND_STOP_SERVICE:
                mUiHandler.removeMessages(COMMAND_STOP_SERVICE);
                mUiHandler.sendEmptyMessage(COMMAND_STOP_SERVICE);
                break;
            case COMMAND_HANDLE_LANDSCAPE_SCREEN:
                if (objArray != null && objArray.length > 0) {
                    int statusBarHeight = (Integer) objArray[0];
                    Message msg = mUiHandler.obtainMessage();
                    msg.what = COMMAND_HANDLE_LANDSCAPE_SCREEN;
                    msg.arg1 = statusBarHeight;
                    mUiHandler.removeMessages(COMMAND_HANDLE_LANDSCAPE_SCREEN);
                    mUiHandler.sendMessage(msg);
                }
                break;
            case COMMAND_HANDLE_PORTRAIT_SCREEN:
                mUiHandler.removeMessages(COMMAND_HANDLE_PORTRAIT_SCREEN);
                mUiHandler.sendEmptyMessage(COMMAND_HANDLE_PORTRAIT_SCREEN);
                break;
            case KeyEvent.KEYCODE_HEADSETHOOK:// 79
                mPlayerWrapper.onEvent(KeyEvent.KEYCODE_HEADSETHOOK, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:// 85
                mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:// 86
                mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_STOP, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:// 88
                // 三击
                mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:// 87
                // 双击
                mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_NEXT, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:// 126
                // 单击
                mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PLAY, null);
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:// 127
                // 单击
                mPlayerWrapper.onEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, null);
                break;
            default:
                break;
        }
        return result;
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case COMMAND_SHOW_WINDOW:
                addView();
                break;
            case COMMAND_HIDE_WINDOW:
                removeView();
                break;
            case COMMAND_STOP_SERVICE:
                removeView();
                stopSelf();
                break;
            case COMMAND_HANDLE_LANDSCAPE_SCREEN:
                if (msg.arg1 == 0) {
                    mPlayerWrapper.handleLandscapeScreen(0);
                } else {
                    mPlayerWrapper.handleLandscapeScreen(1);
                }
                break;
            case COMMAND_HANDLE_PORTRAIT_SCREEN:
                mPlayerWrapper.handlePortraitScreen();
                break;
            default:
                break;
        }
    }

    private void initPlayerWindow() {
        mIsAddedView = false;

        // 屏幕宽高(竖屏时)
        mWindowManager =
                (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;// 1080
        int screenHeight = displayMetrics.heightPixels;// 2244
        MLog.d(TAG, "initPlayerWindow() screenWidth: " +
                screenWidth + " screenHeight: " + screenHeight);

        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTestView = inflater.inflate(R.layout.transparent_player, null);
        mLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.TOP + Gravity.LEFT;
        mLayoutParams.width = 1;// 3
        mLayoutParams.height = 1;// 5
        mLayoutParams.x = 0;
        mLayoutParams.y = 0;
        mWindowManager.addView(mTestView, mLayoutParams);
        mTestView.setBackgroundColor(getResources().getColor(R.color.darkorchid));

        mRootView = inflater.inflate(R.layout.activity_player, null);
        mLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.TOP + Gravity.LEFT;
        //mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.width = screenWidth;
        mLayoutParams.height = 400;
        mLayoutParams.x = 0;
        mLayoutParams.y = 0;

        mPlayerWrapper.setService(this);
        mPlayerWrapper.setDataSource(mCurPath);
        mPlayerWrapper.onCreate();
    }

    @SuppressLint("InvalidWakeLockTag")
    private void addView() {
        if (TextUtils.isEmpty(mCurPath)) {
            MLog.e(TAG, "addView() mCurPath is empty");
            return;
        }
        mPlayerWrapper.setDataSource(mCurPath);
        MLog.i(TAG, "addView()   mIsAddedView: " + mIsAddedView);
        if (!mIsAddedView) {
            mPlayerWrapper.onResume();
            mWindowManager.addView(mRootView, mLayoutParams);
            mPrePath = mCurPath.substring(0);
            //MLog.i(TAG, "addView() mPrePath: " + mPrePath);
            mIsAddedView = true;
        } else if (!TextUtils.equals(mCurPath, mPrePath)) {
            MLog.i(TAG, "addView() onRelease");
            // 有一个视频已经在播放了,在不关闭浮动窗口的情况下播放另一个视频
            mPlayerWrapper.onRelease();
        }
    }

    public void removeView() {
        MLog.i(TAG, "removeView() mIsAddedView: " + mIsAddedView);
        if (mIsAddedView) {
            mPlayerWrapper.onPause();
            mWindowManager.removeView(mRootView);
            mCurPath = null;
            mPrePath = null;
            mIsAddedView = false;
        }
    }

    public boolean needToPlaybackOtherVideo() {
        if (!TextUtils.equals(mCurPath, mPrePath)) {
            MLog.i(TAG, "needToPlaybackOtherVideo() mPrePath: " + mPrePath);
            MLog.i(TAG, "needToPlaybackOtherVideo() mCurPath: " + mCurPath);
            mPlayerWrapper.startPlayback();
            mPrePath = mCurPath.substring(0);
            return true;
        }

        // 不需要播放另一个视频
        return false;
    }

    /////////////////////////////////////////////////////////////////

    private TelephonyManager mTelephonyManager;

    private void registerTelephonyReceiver() {
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    // 挂断或启动监听时
                    MLog.i(TAG, "onCallStateChanged() TelephonyManager.CALL_STATE_IDLE");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    // 来电
                    MLog.i(TAG, "onCallStateChanged() TelephonyManager.CALL_STATE_RINGING");
                    mPlayerWrapper.pausePlayerWithTelephonyCall();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // 接听
                    MLog.i(TAG, "onCallStateChanged() TelephonyManager.CALL_STATE_OFFHOOK");
                    break;
                default:
                    break;
            }
        }
    };

    /////////////////////////////////////////////////////////////////

    /***
     耳机操作
     */

    // Android监听耳机的插拔事件(只能动态注册,经过测试可行)
    private HeadsetPlugReceiver mHeadsetPlugReceiver;
    private AudioManager mAudioManager;
    private ComponentName mMediaButtonReceiver;

    private void registerHeadsetPlugReceiver() {
        if (mHeadsetPlugReceiver == null) {
            mHeadsetPlugReceiver = new HeadsetPlugReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.HEADSET_PLUG");
            filter.setPriority(2147483647);
            registerReceiver(mHeadsetPlugReceiver, filter);
        }

        if (mMediaButtonReceiver == null) {
            mAudioManager =
                    (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mMediaButtonReceiver = new ComponentName(
                    getPackageName(), MediaButtonReceiver.class.getName());
            mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiver);
        }
    }

    private void unregisterHeadsetPlugReceiver() {
        if (mHeadsetPlugReceiver != null) {
            unregisterReceiver(mHeadsetPlugReceiver);
        }

        if (mMediaButtonReceiver != null) {
            mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiver);
        }
    }

    private class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (intent.hasExtra("state")) {
                switch (intent.getIntExtra("state", 0)) {
                    case 0:
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset not connected");
                        break;
                    case 1:
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset has connected");
                        break;
                    default:
                }
            }
        }
    }

}
