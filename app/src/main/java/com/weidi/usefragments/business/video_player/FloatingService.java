package com.weidi.usefragments.business.video_player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.R;
import com.weidi.usefragments.tool.MLog;

/***
 Created by root on 19-8-5.
 */

public class FloatingService extends Service {

    private static final String TAG =
            FloatingService.class.getSimpleName();
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

    public static String PATH =
            "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";
    public static final String PREFERENCES_NAME = "alexander_preferences";

    private SharedPreferences mSP;
    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private void internalCreate() {
        EventBusUtils.register(this);

        mSP = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                FloatingService.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                FloatingService.this.uiHandleMessage(msg);
            }
        };
    }


    private void internalDestroy() {
        EventBusUtils.unregister(this);

        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {

            default:
                break;
        }
        return result;
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {

            default:
                break;
        }
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {

            default:
                break;
        }
    }

    private void showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                // 获取WindowManager服务
                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

                LayoutInflater inflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View rootView = inflater.inflate(R.layout.activity_player, null);

                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                }
                layoutParams.format = PixelFormat.RGBA_8888;
                layoutParams.width = 500;
                layoutParams.height = 100;
                layoutParams.x = 300;
                layoutParams.y = 300;

                windowManager.addView(rootView, layoutParams);
            }
        }


    }
}
