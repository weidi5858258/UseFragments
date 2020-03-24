package com.weidi.usefragments.business.video_player;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import com.weidi.usefragments.R;
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

    private SharedPreferences mSP;
    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsAddedView = false;
    private View mRootView;

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

    private void internalCreate() {
        EventBusUtils.register(this);

        mSP = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerService.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                PlayerService.this.uiHandleMessage(msg);
            }
        };

        initPlayerWindow();
    }

    public static final String COMMAND_ACTION =
            "com.weidi.usefragments.business.video_player.PlayerService";
    public static final String COMMAND_NAME = "HandlePlayerService";
    private static final int COMMAND_SHOW_WINDOW = 1;
    private static final int COMMAND_HIDE_WINDOW = 2;
    private static final int COMMAND_STOP_SERVICE = 3;

    private void internalStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        MLog.d(TAG, "internalStartCommand() action: " + action);
        if (!TextUtils.equals(COMMAND_ACTION, action)) {
            return;
        }
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
            default:
                break;
        }
    }

    private void internalDestroy() {
        removeView();
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
            default:
                break;
        }
    }

    private void initPlayerWindow() {
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
        mRootView = inflater.inflate(R.layout.activity_player, null);
        mRootView.setOnTouchListener(new PlayerOnTouchListener());

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

        mSurfaceView = mRootView.findViewById(R.id.surfaceView);
        mControllerPanelLayout = mRootView.findViewById(R.id.controller_panel_layout);
        mProgressBar = mRootView.findViewById(R.id.loading_view);

        mIsAddedView = false;
    }

    private void addView() {
        if (mSurfaceHolder == null) {
            // 没有图像出来,就是由于没有设置PixelFormat.RGBA_8888
            // 这里要写
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.setFormat(PixelFormat.RGBA_8888);
            mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(
                        SurfaceHolder holder) {
                    MLog.d(TAG, "surfaceCreated()");
                            /*if (mFFMPEGPlayer == null) {
                                return;
                            }*/

                    //startPlayback(holder);
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
                    mSurfaceHolder = null;
                            /*if (mFFMPEGPlayer != null) {
                                mFFMPEGPlayer.releaseAll();
                            }*/
                }
            });
        }

        mWindowManager.addView(mRootView, mLayoutParams);
        mIsAddedView = true;
    }

    private void removeView() {
        if (mIsAddedView) {
            mWindowManager.removeView(mRootView);
            mIsAddedView = false;
        }
    }

    private class PlayerOnTouchListener implements View.OnTouchListener {
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowY = (int) event.getRawY();
                    int movedY = nowY - y;
                    y = nowY;
                    mLayoutParams.y = mLayoutParams.y + movedY;

                    // 更新悬浮窗控件布局
                    mWindowManager.updateViewLayout(view, mLayoutParams);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

}
