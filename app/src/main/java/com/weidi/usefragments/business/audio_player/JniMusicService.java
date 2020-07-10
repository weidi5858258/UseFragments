package com.weidi.usefragments.business.audio_player;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.business.video_player.FFMPEG;
import com.weidi.usefragments.receiver.MediaButtonReceiver;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.MLog;

/**
 * Created by root on 19-7-1.
 */
public class JniMusicService extends Service {


    @Override
    public IBinder onBind(Intent intent) {
        MLog.d(TAG, "onBind() ---> RemoteService");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MLog.d(TAG, "onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        MLog.d(TAG, "onCreate()");
        super.onCreate();
        internalCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MLog.d(TAG, "onStartCommand()");
        internalStartCommand(intent, flags, startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        MLog.d(TAG, "onDestroy()");
        internalDestroy();
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////

    private static final String TAG =
            JniMusicService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private PlaybackQueue mPlaybackQueue;

    private void internalCreate() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                uiHandleMessage(msg);
            }
        };

        mContext = JniMusicService.this;
        EventBusUtils.register(this);
        registerHeadsetPlugReceiver();

        //mFFMPEGPlayer.setCallback(mFFMPEGPlayer.mCallback);

        mThreadHandler.removeMessages(MSG_CREATE_PLAYBACKQUEUE);
        mThreadHandler.sendEmptyMessage(MSG_CREATE_PLAYBACKQUEUE);
    }

    private void internalStartCommand(Intent intent, int flags, int startId) {
        //JobHandlerService.startForeground(this, "Alexander", "MusicService");
        mThreadHandler.removeMessages(MSG_THREAD_START_COMMAND);
        mThreadHandler.sendEmptyMessage(MSG_THREAD_START_COMMAND);
    }

    public static final int GET_OBJECT_OF_MUSICSERVICE = 0x0001;

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case GET_OBJECT_OF_MUSICSERVICE:
                if (objArray != null
                        && objArray.length > 0
                        && objArray[0] instanceof Callback) {
                    mCallback = (Callback) objArray[0];
                }
                result = JniMusicService.this;
                break;
            default:
                break;
        }
        return result;
    }

    // 音频大概有7分钟时间
    // 464375873 07:44
    // 426645333 07:06
    // 271952108 04:31
    private static final int MAX_CACHE_AUDIO_COUNT = 20000;
    private static final int START_CACHE_COUNT_HTTP = 100;
    private static final int START_CACHE_COUNT_LOCAL = 50;
    // 音频一帧的大小不能超过这个值,不然出错(如果设成1024 * 1024会有杂音,不能过大,调查了好久才发现跟这个有关)
    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;
    private static final int TIME_OUT = 10000;

    /*private static final int PREPARE = 0x0001;
    private static final int PLAY = 0x0002;
    private static final int PAUSE = 0x0003;
    private static final int STOP = 0x0004;
    private static final int RELEASE = 0x0005;
    private static final int PREV = 0x0006;
    private static final int NEXT = 0x0007;*/
    // Internal messages
    private static final int MSG_PREPARE = 0;
    private static final int MSG_SET_PLAY_WHEN_READY = 1;
    private static final int MSG_DO_SOME_WORK = 2;
    private static final int MSG_SEEK_TO = 3;
    private static final int MSG_SET_PLAYBACK_PARAMETERS = 4;
    private static final int MSG_SET_SEEK_PARAMETERS = 5;
    private static final int MSG_PLAY = 20;
    private static final int MSG_PAUSE = 21;
    private static final int MSG_STOP = 6;
    private static final int MSG_RELEASE = 7;
    private static final int MSG_DESTROY = 8;
    private static final int MSG_PREV = 9;
    private static final int MSG_NEXT = 10;
    private static final int MSG_REFRESH_SOURCE_INFO = 8;
    private static final int MSG_PERIOD_PREPARED = 9;
    private static final int MSG_SOURCE_CONTINUE_LOADING_REQUESTED = 10;
    private static final int MSG_TRACK_SELECTION_INVALIDATED = 11;
    private static final int MSG_SET_REPEAT_MODE = 12;
    private static final int MSG_SET_SHUFFLE_ENABLED = 13;
    private static final int MSG_SET_FOREGROUND_MODE = 14;
    private static final int MSG_SEND_MESSAGE = 15;
    private static final int MSG_SEND_MESSAGE_TO_TARGET_THREAD = 16;
    private static final int MSG_PLAYBACK_PARAMETERS_CHANGED_INTERNAL = 17;
    private static final int MSG_HANDLE_DATA_LOCK_WAIT = 18;
    private static final int MSG_SEEK_TO_NOTIFY = 19;
    private static final int MSG_GAME_OVER = 22;
    private static final int MSG_CREATE_PLAYBACKQUEUE = 23;
    private static final int MSG_UI_READ_DATA = 24;
    private static final int MSG_UI_HANDLE_DATA = 25;
    private static final int MSG_THREAD_START_COMMAND = 26;

    private static final int STATUS_READ_DATA1_STARTED = 0x0001;
    private static final int STATUS_READ_DATA2_STARTED = 0x0002;
    private static final int STATUS_READ_DATA_PAUSED = 0x0003;
    private static final int STATUS_READ_FINISHED = 0x0004;
    private static final int STATUS_READ_ERROR = 0x0005;

    // 为了注册广播
    private Context mContext = null;
    private String mPath = null;
    private float mVolume = 1.0f;
    private boolean mIsLocal = false;
    // 为false时,就是时长小于7分钟的音频,一次性加载完成
    private boolean mIsLongTime = false;

    private Handler mUiHandler = null;
    private Handler mThreadHandler = null;
    private HandlerThread mHandlerThread = null;
    private Callback mCallback = null;
    private SharedPreferences mSP = null;

    public static final String PATH = "media_path";
    private FFMPEG mFFMPEGPlayer = FFMPEG.getDefault();;

    public String getName() {
        if (!TextUtils.isEmpty(mPath)) {
            return mPath.substring(mPath.lastIndexOf("/") + 1, mPath.length());
        }
        return "@@@@@@@@@@@@";
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        // mPath = Contents.getUri();
        mPath = path;
        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
    }

    public void setHandler(Handler handler) {
        mFFMPEGPlayer.setHandler(handler);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    // 95160000
    public void setProgress(long progress) {
        MLog.i(TAG, "----------------------------------------------------------");
        String elapsedTime = DateUtils.formatElapsedTime(progress);
        MLog.i(TAG, "player_alexander setProgress() progress: " + progress + " " + elapsedTime);
        MLog.i(TAG, "----------------------------------------------------------");
        if (progress < 0) {
            return;
        }
        //mFFMPEGPlayer.seekTo(progress);
    }

    public long getDuration() {
        return 0;//mFFMPEGPlayer.getDuration();
    }

    public void play() {
        if (!isRunning()) {
            mThreadHandler.removeMessages(MSG_PREPARE);
            mThreadHandler.sendEmptyMessage(MSG_PREPARE);
        } else {
            mThreadHandler.removeMessages(MSG_PLAY);
            mThreadHandler.sendEmptyMessage(MSG_PLAY);
        }
    }

    public void pause() {
        mThreadHandler.removeMessages(MSG_PAUSE);
        mThreadHandler.sendEmptyMessage(MSG_PAUSE);
    }

    public void stop() {
        mThreadHandler.removeMessages(MSG_STOP);
        mThreadHandler.sendEmptyMessage(MSG_STOP);
    }

    public void prev() {
        mThreadHandler.removeMessages(MSG_PREV);
        mThreadHandler.sendEmptyMessage(MSG_PREV);
    }

    public void next() {
        mThreadHandler.removeMessages(MSG_NEXT);
        mThreadHandler.sendEmptyMessage(MSG_NEXT);
    }

    public void release() {
        mThreadHandler.removeMessages(MSG_RELEASE);
        mThreadHandler.sendEmptyMessage(MSG_RELEASE);
    }

    public void destroy() {
        //mThreadHandler.removeMessages(MSG_DESTROY);
        //mThreadHandler.sendEmptyMessageDelayed(MSG_DESTROY, 1000);
    }

    public boolean isPlaying() {
        return false;//mFFMPEGPlayer.isPlaying();
    }

    public boolean isRunning() {
        return false;//mFFMPEGPlayer.isRunning();
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_PREPARE:
                if (mCallback != null) {
                    MLog.i(TAG, "onReady");
                    mCallback.onReady();
                }

                if (internalPrepare()) {
                    mUiHandler.removeMessages(MSG_UI_HANDLE_DATA);
                    mUiHandler.sendEmptyMessage(MSG_UI_HANDLE_DATA);

                    mUiHandler.removeMessages(MSG_UI_READ_DATA);
                    mUiHandler.sendEmptyMessageDelayed(MSG_UI_READ_DATA, 500);
                } else {
                    if (mCallback != null) {
                        MLog.e(TAG, "onError");
                        mCallback.onError(0, null);
                    }
                }
                break;
            case MSG_PLAY:
                internalPlay();
                break;
            case MSG_PAUSE:
                internalPause();
                break;
            case MSG_DO_SOME_WORK:
                break;
            case MSG_STOP:
                internalStop();
                break;
            case MSG_RELEASE:
                internalRelease();
                break;
            case MSG_DESTROY:
                internalDestroy();
                break;
            case MSG_PREV:
                internalPrev();
                break;
            case MSG_NEXT:
                internalNext();
                break;
            case MSG_HANDLE_DATA_LOCK_WAIT:
                MLog.e(TAG, "卧槽!卧槽!卧槽!等了10分钟了还不能播放,爷不等了");
                internalRelease();
                /*if (internalPrepare()) {
                    new Thread(mAudioReadData).start();
                    new Thread(mVideoReadData).start();
                }*/
                break;
            case MSG_SEEK_TO_NOTIFY:
                //                notifyToHandleDataLock();
                break;
            case MSG_GAME_OVER:
                if (msg.obj == null) {
                    return;
                }
                /*if (msg.obj instanceof SimpleWrapper) {
                    SimpleWrapper wrapper = (SimpleWrapper)
                            msg.obj;

                    if (wrapper instanceof AudioWrapper
                            && mAudioWrapper.mAudioTrack != null) {
                        mAudioWrapper.mAudioTrack.release();
                    }

                    if (wrapper.decoderMediaCodec != null) {
                        wrapper.decoderMediaCodec.release();
                    }

                    if (wrapper.extractor != null) {
                        wrapper.extractor.release();
                    }

                    wrapper.clear();

                    if (!mAudioWrapper.isHandling) {
                        if (mCallback != null) {
                            MLog.w(TAG, "handleData() onFinished");
                            mCallback.onFinished();
                        }
                    }
                }*/
                break;
            case MSG_CREATE_PLAYBACKQUEUE:
                mSP = PreferenceManager.getDefaultSharedPreferences(mContext);
                mPlaybackQueue = new PlaybackQueue(JniMusicService.this);
                mPath = mPlaybackQueue.next();
                break;
            case MSG_THREAD_START_COMMAND:
                mPath = mSP.getString(PATH, null);
                /*if (!mFFMPEGPlayer.isRunning()) {
                    if (!TextUtils.isEmpty(mPath)) {
                        *//*mThreadHandler.removeMessages(MSG_PREPARE);
                        mThreadHandler.sendEmptyMessage(MSG_PREPARE);*//*
                    } else {
                        mPath = mPlaybackQueue.next();
                    }
                }*/
                break;
            default:
                break;
        }
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_UI_READ_DATA:
                MLog.i(TAG, "uiHandleMessage() MSG_UI_READ_DATA");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //mFFMPEGPlayer.readData();
                    }
                }).start();
                break;
            case MSG_UI_HANDLE_DATA:
                MLog.i(TAG, "uiHandleMessage() MSG_UI_HANDLE_DATA");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //mFFMPEGPlayer.audioHandleData();
                    }
                }).start();
                break;
            default:
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            MLog.e(TAG, "internalPrepare() mPath is empty");
            return false;
        }

        mSP.edit().putString(PATH, mPath).apply();
        MLog.i(TAG, "internalPrepare() start");
        /*mFFMPEGPlayer.setMode(FFMPEG.USE_MODE_ONLY_AUDIO);
        mFFMPEGPlayer.setSurface(mPath, null);
        if (mFFMPEGPlayer.initPlayer() < 0) {
            MLog.e(TAG, "internalPrepare() end error");
            return false;
        }*/
        MLog.i(TAG, "internalPrepare() end");

        return true;
    }

    private void internalPlay() {
        /*if (mFFMPEGPlayer.isRunning()
                && !mFFMPEGPlayer.isPlaying()) {
            mFFMPEGPlayer.play();
        }*/
    }

    private void internalPause() {
        /*if (mFFMPEGPlayer.isRunning()
                && mFFMPEGPlayer.isPlaying()) {
            mFFMPEGPlayer.pause();
        }*/
    }

    private void internalStop() {
        MLog.d(TAG, "internalStop() start");
        //mFFMPEGPlayer.stop();
        MLog.d(TAG, "internalStop() end");
    }

    private void internalRelease() {
        MLog.d(TAG, "internalRelease() start");
        internalStop();
        MLog.d(TAG, "internalRelease() end");
    }

    private void internalDestroy() {
        MLog.d(TAG, "internalDestroy() start");
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }

        mFFMPEGPlayer.releaseAll();
        unregisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
        MLog.d(TAG, "internalDestroy() end");
    }

    private void internalPrev() {
        release();
        mPath = mPlaybackQueue.prev();
        mThreadHandler.removeMessages(MSG_PREPARE);
        mThreadHandler.sendEmptyMessageDelayed(MSG_PREPARE, 500);
    }

    private void internalNext() {
        release();
        mPath = mPlaybackQueue.next();
        mThreadHandler.removeMessages(MSG_PREPARE);
        mThreadHandler.sendEmptyMessageDelayed(MSG_PREPARE, 500);
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
        if (mContext == null) {
            return;
        }

        mHeadsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.setPriority(2147483647);
        mContext.registerReceiver(mHeadsetPlugReceiver, filter);

        mAudioManager =
                (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mMediaButtonReceiver = new ComponentName(
                mContext.getPackageName(), MediaButtonReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiver);
    }

    private void unregisterHeadsetPlugReceiver() {
        if (mContext == null || mHeadsetPlugReceiver == null) {
            return;
        }

        mContext.unregisterReceiver(mHeadsetPlugReceiver);
        mAudioManager.unregisterMediaButtonEventReceiver(mMediaButtonReceiver);
    }

    private class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                switch (intent.getIntExtra("state", 0)) {
                    case 0:
                        MLog.d(TAG, "HeadsetPlugReceiver headset not connected");
                        //pause();
                        break;
                    case 1:
                        MLog.d(TAG, "HeadsetPlugReceiver headset has connected");
                        //play();
                        break;
                    default:
                }
            }
        }
    }

}
