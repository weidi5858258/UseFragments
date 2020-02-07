package com.weidi.usefragments.business.audio_player;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.MainActivity1;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.keeplive.IGuardAidl;
import com.weidi.usefragments.business.keeplive.JobHandlerService;
import com.weidi.usefragments.business.keeplive.KeepLive;
import com.weidi.usefragments.business.keeplive.NotificationClickReceiver;
import com.weidi.usefragments.business.keeplive.OnePixelActivity;
import com.weidi.usefragments.business.keeplive.OnePixelReceiver;
import com.weidi.usefragments.business.keeplive.RemoteService;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;
import com.weidi.usefragments.tool.AACPlayer;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.MLog;
import com.weidi.utils.MyToast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by root on 19-7-1.
 */

public class MusicService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        MLog.d(TAG, "onBind() ---> RemoteService");
        return iGuardAidlStub;
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
            MusicService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private OnePixelReceiver mOnePixelReceiver;
    private ScreenStateReceiver mScreenStateReceiver;
    private boolean mIsBoundLocalService;
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

        mContext = MusicService.this;
        EventBusUtils.register(this);
        registerHeadsetPlugReceiver();

        /*// 像素保活
        mOnePixelReceiver = new OnePixelReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        registerReceiver(mOnePixelReceiver, intentFilter);

        // 屏幕点亮状态监听，用于单独控制音乐播放
        mScreenStateReceiver = new ScreenStateReceiver();
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("_ACTION_SCREEN_OFF");
        intentFilter2.addAction("_ACTION_SCREEN_ON");
        registerReceiver(mScreenStateReceiver, intentFilter2);*/

        mThreadHandler.sendEmptyMessage(MSG_CREATE_PLAYBACKQUEUE);
    }

    /***
     Notification(channel=null pri=0 contentView=null
     vibrate=null sound=default defaults=0x1 flags=0x40
     color=0x00000000 vis=PRIVATE)
     */
    private void internalStartCommand(Intent intent, int flags, int startId) {
        /*JobHandlerService.startForeground(this, "Alexander", "MusicService");

        // 绑定守护进程
        try {
            intent = new Intent(MusicService.this, RemoteService.class);
            //startService(intent);
            mIsBoundLocalService = bindService(
                    intent,
                    mServiceConnection,
                    Context.BIND_ABOVE_CLIENT);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        // 启用前台服务，提升优先级
        /*Notification notification = null;
        intent = new Intent(this, NotificationClickReceiver.class);
        intent.setAction(NotificationClickReceiver.CLICK_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(
                    getPackageName(), getPackageName(),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{0});
            notificationChannel.setSound(null, null);
            notificationManager.createNotificationChannel(notificationChannel);

            // PendingIntent.FLAG_UPDATE_CURRENT这个类型才能传值
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder =
                    new Notification.Builder(this, getPackageName())
                            .setContentTitle("Alexander")
                            .setContentText("MusicService")
                            .setSmallIcon(R.drawable.a2)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);
            notification = builder.build();
        } else {
            // PendingIntent.FLAG_UPDATE_CURRENT这个类型才能传值
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, getPackageName())
                            .setContentTitle("Alexander")
                            .setContentText("MusicService")
                            .setSmallIcon(R.drawable.a2)
                            .setAutoCancel(true)
                            .setVibrate(new long[]{0})
                            .setContentIntent(pendingIntent);
            notification = builder.build();
        }
        startForeground(13691, notification);*/

        // 创建前台服务
        /*String CHANNEL_ID = "com.weidi.usefragments.business.audio_player.MusicService";
        String CHANNEL_NAME = "Alexander";
        NotificationChannel notificationChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        intent = new Intent(this, MainActivity1.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).
                setContentTitle("@^@").
                setContentText("我是前台服务").
                setWhen(System.currentTimeMillis()).
                setSmallIcon(R.drawable.a9).
                setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.a1)).
                setContentIntent(pendingIntent).build();
        startForeground(110, notification);*/
    }

    public static final int GET_OBJECT_OF_MUSICSERVICE = 0x0001;

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                onKeyDown(KeyEvent.KEYCODE_HEADSETHOOK, null);
                break;
            case GET_OBJECT_OF_MUSICSERVICE:
                if (objArray != null && objArray.length > 0 && objArray[0] instanceof Callback) {
                    mCallback = (Callback) objArray[0];
                }
                result = MusicService.this;
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
    private com.weidi.usefragments.tool.Callback mCallback = null;

    private AudioWrapper mAudioWrapper = new AudioWrapper();

    private static class SimpleWrapper {
        public String mime = null;
        public MediaExtractor extractor = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        public int trackIndex = -1;
        // 使用于while条件判断
        public boolean isReading = false;
        // 使用于while条件判断
        public boolean isHandling = false;
        // readData用于缓存数据,handleData用于处理数据
        public ArrayList<AVPacket> readData = null;
        public ArrayList<AVPacket> handleData = null;
        public boolean readDataFull = false;
        public boolean handleDataFull = false;

        public int CACHE;
        public int CACHE_START;
        // 总时长
        public long durationUs = 0;
        // 播放的时长(下面两个参数一起做的事是每隔一秒发一次回调函数)
        public long presentationTimeUs1 = 0;
        // 使用于时间戳
        public long presentationTimeUs2 = 0;

        public BufferedOutputStream outputStream;
        public String savePath = null;

        public boolean isStarted = false;
        // 因为user所以pause
        public boolean isPausedForUser = false;
        // 因为cache所以pause
        public boolean isPausedForCache = false;
        // 因为seek所以pause
        public boolean isPausedForSeek = false;
        public Object readDataLock = new Object();
        public Object handleDataLock = new Object();

        // 一帧音频或者视频的最大值
        public int frameMaxLength = 0;
        // 音频或者视频一帧的实际大小
        public int frameDataLength = 0;
        // 放一帧音频或者视频的容器
        public byte[] frameData = null;

        // seekTo
        public long progressUs = -1;
        public boolean needToSeek = false;

        public SimpleWrapper() {
            CACHE = MAX_CACHE_AUDIO_COUNT;
            CACHE_START = START_CACHE_COUNT_HTTP;
            frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
            readData = new ArrayList<AVPacket>(CACHE);
            handleData = new ArrayList<AVPacket>(CACHE);
        }

        public void clear() {
            mime = null;
            extractor = null;
            decoderMediaCodec = null;
            decoderMediaFormat = null;
            trackIndex = -1;
            isReading = false;
            isHandling = false;
            isStarted = false;
            isPausedForUser = false;
            isPausedForCache = false;
            isPausedForSeek = false;
            readDataFull = false;
            handleDataFull = false;
            readData.clear();
            handleData.clear();
            durationUs = 0;
            presentationTimeUs1 = 0;
            presentationTimeUs2 = 0;
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                    outputStream = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            savePath = null;
            //frameMaxLength = 0;
            frameDataLength = 0;
        }
    }

    private static class AudioWrapper extends SimpleWrapper {
        public AudioTrack mAudioTrack = null;

        public AudioWrapper() {
            super();
        }

        public void clear() {
            MLog.d(TAG, "AudioWrapper clear()");
            if (mAudioTrack != null) {
                mAudioTrack.release();
            }
            if (decoderMediaCodec != null) {
                decoderMediaCodec.release();
            }
            super.clear();
        }
    }

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

    public void setCallback(com.weidi.usefragments.tool.Callback callback) {
        mCallback = callback;
    }

    public long mProgressUs = -1;

    // 95160000
    public void setProgressUs(long progressUs) {
        MLog.i(TAG, "----------------------------------------------------------");
        String elapsedTime = DateUtils.formatElapsedTime(progressUs / 1000 / 1000);
        MLog.i(TAG, "setProgressUs() progressUs: " + progressUs + " " + elapsedTime);
        MLog.i(TAG, "----------------------------------------------------------");
        if (progressUs < 0
                || progressUs > mAudioWrapper.durationUs
                || mAudioWrapper.isPausedForSeek) {
            return;
        }
        mProgressUs = progressUs;
        mAudioWrapper.isPausedForSeek = true;
        mAudioWrapper.progressUs = progressUs;

        notifyToHandle(mAudioWrapper);
    }

    private void seekTo(SimpleWrapper wrapper) {
        while (!wrapper.needToSeek) {
            SystemClock.sleep(1);
        }
        wrapper.needToSeek = false;
        MLog.d(TAG, "seekTo() audio start");

        int index = -1;
        int size = wrapper.readData.size();
        if (size >= 2) {
            if (wrapper.readData.get(size - 1).sampleTime >= wrapper.progressUs
                    && wrapper.readData.get(0).sampleTime <= wrapper.progressUs) {
                for (int i = 0; i + 1 < size; i++) {
                    AVPacket data1 = wrapper.readData.get(i);
                    AVPacket data2 = wrapper.readData.get(i + 1);
                    if (data1.sampleTime <= wrapper.progressUs
                            && data2.sampleTime >= wrapper.progressUs) {
                        index = i;
                        break;
                    }
                }
            }
        }
        if (index != -1) {
            synchronized (wrapper.readData) {
                AVPacket data = wrapper.readData.get(index);
                MLog.d(TAG, "seekTo() audio index: " + index +
                        " sampleTime: " + data.sampleTime +
                        " " + DateUtils.formatElapsedTime(data.sampleTime / 1000 / 1000));
                wrapper.handleData.addAll(wrapper.readData.subList(index, size - 1));
                wrapper.handleDataFull = true;
                // 需要在notifyToHandle(...)调用之前
                //wrapper.isPausedForSeek = false;
                wrapper.readData.clear();
            }
            //notifyToHandle(wrapper);
        } else {
            synchronized (wrapper.readData) {
                wrapper.readData.clear();
            }
            // SEEK_TO_PREVIOUS_SYNC SEEK_TO_CLOSEST_SYNC SEEK_TO_NEXT_SYNC
            wrapper.extractor.seekTo(
                    wrapper.progressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            //wrapper.isPausedForSeek = false;
        }

        MLog.d(TAG, "seekTo() audio end");
    }

    private void seekTo2(SimpleWrapper wrapper) {
        while (!wrapper.needToSeek) {
            SystemClock.sleep(1);
        }
        wrapper.needToSeek = false;
        MLog.d(TAG, "seekTo2() audio start");

        int index = -1;
        int size = wrapper.readData.size();
        for (int i = 0; i + 1 < size; i++) {
            AVPacket data1 = wrapper.readData.get(i);
            AVPacket data2 = wrapper.readData.get(i + 1);
            if (data1.sampleTime <= wrapper.progressUs
                    && data2.sampleTime >= wrapper.progressUs) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            synchronized (wrapper.readData) {
                AVPacket data = wrapper.readData.get(index);
                MLog.d(TAG, "seekTo2() audio index: " + index +
                        " sampleTime: " + data.sampleTime +
                        " " + DateUtils.formatElapsedTime(data.sampleTime / 1000 / 1000));
                wrapper.handleData.addAll(wrapper.readData.subList(index, size - 1));
                wrapper.handleDataFull = true;
                //wrapper.isPausedForSeek = false;
            }
        } else {
            wrapper.isReading = false;
            wrapper.isHandling = false;
            //wrapper.isPausedForSeek = false;
            notifyToRead(wrapper);
        }
        MLog.d(TAG, "seekTo2() audio end");
    }

    public long getDurationUs() {
        return mAudioWrapper.durationUs;
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
        mThreadHandler.removeMessages(MSG_DESTROY);
        mThreadHandler.sendEmptyMessageDelayed(MSG_DESTROY, 1000);
    }

    public boolean isPlaying() {
        return mAudioWrapper.isStarted
                && !mAudioWrapper.isPausedForUser
                && !mAudioWrapper.isPausedForCache;
    }

    public boolean isRunning() {
        return mAudioWrapper.isReading
                || mAudioWrapper.isHandling;
    }

    private boolean firstFlag = false;
    private boolean secondFlag = false;
    private boolean threeFlag = false;

    /***
     action=ACTION_DOWN, keyCode=KEYCODE_HEADSETHOOK,
     scanCode=226, metaState=0, flags=0x8, repeatCount=0,
     eventTime=301864016, downTime=301864016, deviceId=9, source=0x101
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (DEBUG)
            Log.d(TAG, "onKeyDown() event: " + event);*/
        switch (keyCode) {
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
                return true;
            default:
                break;
        }

        return false;
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

                internalPrepare();
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
                if (msg.obj instanceof SimpleWrapper) {
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
                }
                break;
            case MSG_CREATE_PLAYBACKQUEUE:
                mPlaybackQueue = new PlaybackQueue(MusicService.this);
                mPath = mPlaybackQueue.next();
                break;
            default:
                break;
        }
    }

    private long step = 0;

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (firstFlag && secondFlag && threeFlag) {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 3");
                    MediaUtils.SLEEP_TIME--;
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() MediaUtils.SLEEP_TIME: " +
                                MediaUtils.SLEEP_TIME);
                    /*if (mCallback != null) {
                        mCallback.onPlaybackFinished();
                    }*/
                } else if (firstFlag && secondFlag) {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 2");
                    /*MediaUtils.SLEEP_TIME++;
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() MediaUtils.SLEEP_TIME: " +
                                MediaUtils.SLEEP_TIME);*/
                    /*if (mCallback != null) {
                        mCallback.onPlaybackFinished();
                    }*/
                    if (mProgressUs == -1) {
                        step = (int) ((mAudioWrapper.presentationTimeUs2
                                / (mAudioWrapper.durationUs * 1.00)) * 3840.00);
                        Log.d(TAG, "onKeuiHandleMessageyDown() step: " + step);
                    }
                    step += 100;
                    long progress = (long) (((step / 3840.00) * mAudioWrapper.durationUs));
                    setProgressUs(progress);
                } else {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 1");
                    if (!mAudioWrapper.isPausedForCache) {
                        if (!mAudioWrapper.isPausedForUser
                                && !mAudioWrapper.isPausedForUser) {
                            internalPause();
                        } else {
                            internalPlay();
                        }
                    }
                }
                firstFlag = false;
                secondFlag = false;
                threeFlag = false;
                break;
            default:
                break;
        }
    }

    // 读到的都是压缩数据
    private void readData(SimpleWrapper wrapper) {
        MLog.d(TAG, "readData() audio start");

        /***
         三种情况退出循环:
         1.异常
         2.要处理的数据有问题时不能继续往下走,然后通知这里结束
         3.readSize < 0

         数据先读到room,再从room复制到buffer
         */
        int readSize = -1;
        ByteBuffer room = ByteBuffer.allocate(wrapper.frameMaxLength);
        byte[] buffer = new byte[wrapper.frameMaxLength];
        wrapper.extractor.selectTrack(wrapper.trackIndex);
        while (wrapper.isReading) {
            try {
                // seekTo
                if (wrapper.isPausedForSeek) {
                    seekTo(wrapper);
                }

                room.clear();
                // wrapper.extractor ---> room
                // readSize为实际读到的大小(音视频一帧的大小),其值可能远远小于room的大小
                readSize = wrapper.extractor.readSampleData(room, 0);

                if (readSize < 0) {
                    wrapper.isReading = false;
                    // 没有数据可读了,结束
                    // 也有可能是底层异常引起
                    // MediaHTTPConnection:
                    // readAt 1033700796 / 32768 => java.net.ProtocolException:
                    // unexpected end of stream
                    MLog.e(TAG, "readData() end audio readSize: " + readSize);

                    // 开启任务处理数据(如果还没有开启任务过的话)
                    // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                    notifyToHandle(wrapper);

                    /*if (wrapper.outputStream != null) {
                        try {
                            wrapper.outputStream.flush();
                            wrapper.outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }*/

                    MLog.e(TAG, "readData() audio notifyToReadWait start");
                    // 需要seek,不能退出
                    wrapper.isPausedForSeek = false;
                    SystemClock.sleep(100);
                    notifyToHandle(wrapper);
                    notifyToReadWait(wrapper);
                    MLog.e(TAG, "readData() audio notifyToReadWait end");
                    if (wrapper.isPausedForSeek) {
                        wrapper.isReading = true;
                        continue;
                    } else {
                        break;
                    }
                }// readSize < 0

                Arrays.fill(buffer, (byte) 0);
                // room ---> buffer
                room.get(buffer, 0, readSize);
                // 作为解码时的时间戳传进去
                AVPacket data = new AVPacket(readSize, wrapper.extractor.getSampleTime());
                System.arraycopy(buffer, 0, data.data, 0, readSize);
                synchronized (wrapper.readData) {
                    wrapper.readData.add(data);
                }

                int size = wrapper.readData.size();
                if (size == wrapper.CACHE) {
                    MLog.d(TAG, "readData() audio notifyToReadWait start");
                    System.gc();
                    notifyToReadWait(wrapper);
                    if (wrapper.isPausedForSeek) {
                        continue;
                    }
                    MLog.d(TAG, "readData() audio notifyToReadWait end");
                } else if (!wrapper.handleDataFull && size == wrapper.CACHE_START) {
                    synchronized (wrapper.readData) {
                        wrapper.handleData.clear();
                        wrapper.handleData.addAll(wrapper.readData);
                        wrapper.readData.clear();
                        wrapper.handleDataFull = true;
                        wrapper.isPausedForSeek = false;
                    }
                    MLog.d(TAG, "readData() audio notifyToHandle");
                    notifyToHandle(wrapper);
                }

                if (wrapper.isReading) {
                    // 跳到下一帧
                    wrapper.extractor.advance();
                }
            } catch (Exception e) {
                e.printStackTrace();
                wrapper.isReading = false;
                break;
            }
        }// while(...) end

        MLog.d(TAG, "readData() audio end");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void handleData(SimpleWrapper wrapper) {
        MLog.d(TAG, "handleData() audio notifyToHandleWait start");
        notifyToHandleWait(wrapper);
        MLog.d(TAG, "handleData() audio notifyToHandleWait end");
        if (!wrapper.isHandling) {
            return;
        }

        MLog.d(TAG, "handleData() audio start");
        boolean isFinished = false;
        while (wrapper.isHandling) {
            // pause装置
            if (wrapper.isPausedForUser
                    || wrapper.isPausedForCache
                    || wrapper.isPausedForSeek) {
                boolean isPausedForUser = wrapper.isPausedForUser;
                boolean isPausedForSeek = wrapper.isPausedForSeek;

                if (isPausedForSeek) {
                    wrapper.handleDataFull = false;
                    wrapper.handleData.clear();
                    wrapper.needToSeek = true;
                    notifyToRead(wrapper);
                    MLog.d(TAG, "handleData() audio Seek notifyToHandleWait start");
                } else if (isPausedForUser) {
                    MLog.d(TAG, "handleData() audio User notifyToHandleWait start");
                } else {
                    MLog.d(TAG, "handleData() audio Cache notifyToHandleWait start");
                }

                notifyToHandleWait(wrapper);

                if (wrapper.isPausedForSeek || wrapper.isPausedForUser) {
                    continue;
                }

                if (isPausedForSeek) {
                    MLog.d(TAG, "handleData() audio Seek notifyToHandleWait end");
                } else if (isPausedForUser) {
                    MLog.d(TAG, "handleData() audio User notifyToHandleWait end");
                } else {
                    MLog.d(TAG, "handleData() audio Cache notifyToHandleWait end");
                }
            }// pause end

            if (!wrapper.handleData.isEmpty()) {
                AVPacket data = wrapper.handleData.get(0);
                wrapper.frameData = data.data;
                wrapper.frameDataLength = data.size;
                wrapper.presentationTimeUs1 = data.sampleTime;

                // 向MediaCodec喂数据
                if (!feedInputBufferAndDrainOutputBuffer(wrapper)) {
                    wrapper.isHandling = false;
                    // while(...) end
                    break;
                }

                data = null;
                wrapper.handleData.remove(0);

                // 过一秒才更新
                // 如果退出,那么seekTo到这个位置就行了
                curElapsedTime = DateUtils.formatElapsedTime(
                        (wrapper.presentationTimeUs1 / 1000) / 1000);
                if (mCallback != null
                        // 防止重复更新
                        && !TextUtils.equals(curElapsedTime, preElapsedTime)) {
                    mCallback.onProgressUpdated(wrapper.presentationTimeUs1);
                    preElapsedTime = curElapsedTime;
                }
            }

            if (wrapper.handleData.isEmpty()) {
                synchronized (wrapper.readData) {
                    wrapper.handleDataFull = false;
                }
                if (!wrapper.readData.isEmpty()) {
                    synchronized (wrapper.readData) {
                        wrapper.handleDataFull = true;
                        wrapper.handleData.clear();
                        wrapper.handleData.addAll(wrapper.readData);
                        wrapper.readData.clear();
                    }
                    if (wrapper.isReading) {
                        notifyToRead(wrapper);
                    }
                } else {
                    if (wrapper.isReading) {
                        if (mCallback != null) {
                            MLog.d(TAG, "handleData() audio onPaused");
                            mCallback.onPaused();
                        }

                        MLog.e(TAG, "卧槽!卧槽!卧槽! audio太不给力了");
                        mAudioWrapper.isPausedForCache = true;
                        notifyToHandleWait(wrapper);
                        mAudioWrapper.isPausedForCache = false;

                        if (wrapper.isPausedForSeek) {
                            continue;
                        }

                        if (mCallback != null) {
                            MLog.d(TAG, "handleData() audio onPlayed");
                            mCallback.onPlayed();
                        }
                    } else {
                        wrapper.isHandling = false;
                        isFinished = true;
                        break;
                    }
                }
            }// empty的情况
        }// while(true) end
        MLog.d(TAG, "handleData() audio end");

        // 让"读线程"结束
        wrapper.isReading = false;
        wrapper.isPausedForSeek = false;
        notifyToRead(wrapper);

        if (mAudioWrapper.mAudioTrack != null) {
            mAudioWrapper.mAudioTrack.release();
            mAudioWrapper.mAudioTrack = null;
        }

        if (wrapper.decoderMediaCodec != null) {
            wrapper.decoderMediaCodec.release();
            wrapper.decoderMediaCodec = null;
        }

        if (wrapper.extractor != null) {
            wrapper.extractor.release();
            wrapper.extractor = null;
        }

        if (mCallback != null && isFinished) {
            MLog.d(TAG, "handleData() onFinished");
            mCallback.onFinished();
        }

        mAudioWrapper.clear();
        System.gc();
    }

    // 读到的都是压缩数据
    private void readData2(SimpleWrapper wrapper) {
        MLog.d(TAG, "readData2() audio start");

        int readSize = -1;
        ByteBuffer room = ByteBuffer.allocate(wrapper.frameMaxLength);
        byte[] buffer = new byte[wrapper.frameMaxLength];
        wrapper.extractor.selectTrack(wrapper.trackIndex);
        /***
         主要思想就是一股脑儿先读完,然后等待
         */
        while (wrapper.isReading) {
            try {
                if (!wrapper.readDataFull) {
                    room.clear();
                    readSize = wrapper.extractor.readSampleData(room, 0);
                    if (readSize < 0) {
                        MLog.e(TAG, "readData2() end audio readSize : " + readSize);
                        MLog.e(TAG, "readData2() end audio readData2: " + wrapper.readData.size());
                        MLog.e(TAG, "readData2() end audio notifyToHandle");

                        synchronized (wrapper.readData) {
                            wrapper.handleData.clear();
                            wrapper.handleData.addAll(wrapper.readData);
                            wrapper.readDataFull = true;
                            wrapper.handleDataFull = true;
                        }

                        notifyToHandle(wrapper);
                        continue;
                    }

                    Arrays.fill(buffer, (byte) 0);
                    room.get(buffer, 0, readSize);
                    AVPacket data = new AVPacket(readSize, wrapper.extractor.getSampleTime());
                    System.arraycopy(buffer, 0, data.data, 0, readSize);
                    synchronized (wrapper.readData) {
                        wrapper.readData.add(data);
                    }

                    wrapper.extractor.advance();
                } else {
                    // 为了做seek的事
                    MLog.e(TAG, "readData2() audio notifyToReadWait start");
                    wrapper.isPausedForSeek = false;
                    SystemClock.sleep(100);
                    notifyToHandle(wrapper);
                    notifyToReadWait(wrapper);
                    MLog.e(TAG, "readData2() audio notifyToReadWait end");

                    if (wrapper.isPausedForSeek) {
                        seekTo2(wrapper);
                    } else {
                        wrapper.isReading = false;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                wrapper.isReading = false;
                MLog.e(TAG, "readData2() occur error");
                break;
            }
        }// while(...) end

        MLog.d(TAG, "readData2() audio end");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void handleData2(SimpleWrapper wrapper) {
        MLog.d(TAG, "handleData2() audio notifyToHandleWait start");
        notifyToHandleWait(wrapper);
        MLog.d(TAG, "handleData2() audio notifyToHandleWait end");
        if (!wrapper.isHandling) {
            return;
        }

        MLog.d(TAG, "handleData2() audio start");
        boolean isFinished = false;
        while (wrapper.isHandling) {
            // pause装置
            if (wrapper.isPausedForUser
                    || wrapper.isPausedForSeek) {
                boolean isPausedForSeek = wrapper.isPausedForSeek;

                if (isPausedForSeek) {
                    wrapper.handleDataFull = false;
                    wrapper.handleData.clear();
                    wrapper.needToSeek = true;
                    notifyToRead(wrapper);
                    MLog.d(TAG, "handleData2() audio Seek notifyToHandleWait start");
                } else {
                    MLog.d(TAG, "handleData2() audio User notifyToHandleWait start");
                }

                notifyToHandleWait(wrapper);

                if (wrapper.isPausedForSeek || wrapper.isPausedForUser) {
                    continue;
                }

                if (isPausedForSeek) {
                    MLog.d(TAG, "handleData2() audio Seek notifyToHandleWait end");
                } else {
                    MLog.d(TAG, "handleData2() audio User notifyToHandleWait end");
                }
            }// pause end

            if (!wrapper.handleData.isEmpty()) {
                AVPacket data = wrapper.handleData.get(0);
                wrapper.frameData = data.data;
                wrapper.frameDataLength = data.size;
                wrapper.presentationTimeUs1 = data.sampleTime;
                data = null;
                wrapper.handleData.remove(0);

                // 向MediaCodec喂数据
                if (!feedInputBufferAndDrainOutputBuffer(wrapper)) {
                    wrapper.isHandling = false;
                    // while(...) end
                    break;
                }

                if (!wrapper.isPausedForSeek) {
                    // 过一秒才更新
                    // 如果退出,那么seekTo到这个位置就行了
                    curElapsedTime = DateUtils.formatElapsedTime(
                            (wrapper.presentationTimeUs1 / 1000) / 1000);
                    if (mCallback != null
                            // 防止重复更新
                            && !TextUtils.equals(curElapsedTime, preElapsedTime)) {
                        mCallback.onProgressUpdated(wrapper.presentationTimeUs1);
                        preElapsedTime = curElapsedTime;
                    }
                }
            } else {
                wrapper.isHandling = false;
                isFinished = true;
                break;
            }
        }// while(true) end
        MLog.d(TAG, "handleData2() audio end");

        // 让"读线程"结束
        wrapper.isReading = false;
        wrapper.isPausedForSeek = false;
        notifyToRead(wrapper);

        if (mAudioWrapper.mAudioTrack != null) {
            mAudioWrapper.mAudioTrack.release();
            mAudioWrapper.mAudioTrack = null;
        }

        if (wrapper.decoderMediaCodec != null) {
            wrapper.decoderMediaCodec.release();
            wrapper.decoderMediaCodec = null;
        }

        if (wrapper.extractor != null) {
            wrapper.extractor.release();
            wrapper.extractor = null;
        }

        if (mCallback != null && isFinished) {
            MLog.d(TAG, "handleData2() onFinished");
            mCallback.onFinished();
        }

        mAudioWrapper.clear();
        System.gc();
    }

    private boolean prepareAudio() {
        MLog.d(TAG, "prepareAudio() start");
        mAudioWrapper.extractor = new MediaExtractor();
        try {
            mAudioWrapper.extractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            if (mAudioWrapper.extractor != null) {
                mAudioWrapper.extractor.release();
                mAudioWrapper.extractor = null;
            }
            internalRelease();
            MLog.e(TAG, "prepareAudio() setDataSource errrr");
            return false;
        }
        int trackCount = mAudioWrapper.extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mAudioWrapper.decoderMediaFormat = mAudioWrapper.extractor.getTrackFormat(i);
            String mime = mAudioWrapper.decoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mAudioWrapper.mime = mime;
                mAudioWrapper.trackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(mAudioWrapper.mime)
                || mAudioWrapper.trackIndex == -1) {
            internalRelease();
            MLog.e(TAG, "prepareAudio() mime: " + mAudioWrapper.mime +
                    " trackIndex: " + mAudioWrapper.trackIndex);
            return false;
        }

        MLog.d(TAG, "internalPrepare() audio decoderMediaFormat: " +
                mAudioWrapper.decoderMediaFormat.toString());

        /***
         解码前
         audio
         {
         encoder-delay=576, sample-rate=44100,
         track-id=1, durationUs=202657959,
         mime=audio/mpeg, channel-count=2,
         bitrate=320000, encoder-padding=804
         }
         video
         {
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2],
         mime=audio/mp4a-latm, aac-profile=2, channel-count=2, track-id=2, bitrate=96000,
         max-input-size=444, durationUs=10871488000,
         sample-rate=48000, max-bitrate=96000
         }
         解码后
         {pcm-encoding=2, mime=audio/raw, channel-count=2, sample-rate=48000}
         */
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mAudioWrapper.durationUs =
                    mAudioWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            if (mAudioWrapper.durationUs >= 464375873) {
                mIsLongTime = true;
            }
            String durationTime = DateUtils.formatElapsedTime(
                    (mAudioWrapper.durationUs / 1000) / 1000);
            MLog.d(TAG, "internalPrepare()          audioDurationUs: " +
                    mAudioWrapper.durationUs + " " + durationTime);
        }
        // 音乐文件没有这个属性
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mAudioWrapper.frameMaxLength =
                    mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }
        MLog.d(TAG, "internalPrepare() audio     frameMaxLength: " +
                mAudioWrapper.frameMaxLength);
        // 创建音频解码器
        try {
            mAudioWrapper.extractor.selectTrack(mAudioWrapper.trackIndex);
            switch (mAudioWrapper.mime) {
                case "audio/ac4":
                    MediaCodecInfo mediaCodecInfo =
                            MediaUtils.getDecoderMediaCodecInfo(mAudioWrapper.mime);
                    String codecName = null;
                    if (mediaCodecInfo != null) {
                        codecName = mediaCodecInfo.getName();
                    } else {
                        codecName = "OMX.google.raw.decoder";
                        mAudioWrapper.decoderMediaFormat.setString(
                                MediaFormat.KEY_MIME, "audio/raw");
                    }
                    if (!TextUtils.isEmpty(codecName)) {
                        MLog.d(TAG, "internalPrepare() audio          codecName: " + codecName);
                        mAudioWrapper.decoderMediaCodec = MediaCodec.createByCodecName(codecName);
                        mAudioWrapper.decoderMediaCodec.configure(
                                mAudioWrapper.decoderMediaFormat, null, null, 0);
                        mAudioWrapper.decoderMediaCodec.start();
                    }
                    break;
                default:
                    mAudioWrapper.decoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec(
                            mAudioWrapper.mime, mAudioWrapper.decoderMediaFormat);
                    break;
            }
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException
                | IOException e) {
            e.printStackTrace();
            if (mAudioWrapper.decoderMediaCodec != null) {
                mAudioWrapper.decoderMediaCodec.release();
                mAudioWrapper.decoderMediaCodec = null;
            }
        }
        if (mAudioWrapper.decoderMediaCodec == null) {
            internalRelease();
            MLog.e(TAG, "prepareAudio() decoderMediaCodec is null");
            return false;
        }

        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "internalPrepare() audio              csd-0: " + sb.toString());
        }
        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "internalPrepare() audio              csd-1: " + sb.toString());
        }
        MLog.d(TAG, "prepareAudio() end");

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            MLog.e(TAG, "internalPrepare() mPath is empty");
            return false;
        }
        MLog.i(TAG, "internalPrepare() start");

        onlyOneStart = true;
        preElapsedTime = null;
        curElapsedTime = null;
        mAudioWrapper.clear();
        mAudioWrapper.isReading = true;
        mAudioWrapper.isHandling = true;

        mIsLocal = false;
        if (!mPath.startsWith("http://")
                && !mPath.startsWith("HTTP://")
                && !mPath.startsWith("https://")
                && !mPath.startsWith("HTTPS://")) {
            File file = new File(mPath);
            if (!file.canRead()
                    || file.isDirectory()) {
                MLog.e(TAG, "不能读取此文件: " + mPath);
                internalRelease();
                return false;
            }
            long fileSize = file.length();
            MLog.i(TAG, "internalPrepare() fileSize: " + fileSize);
            mAudioWrapper.CACHE_START = START_CACHE_COUNT_LOCAL;
            mIsLocal = true;
        }

        String PATH = null;
        PATH = "/data/data/com.weidi.usefragments/files/Movies/";
        PATH = "/storage/37C8-3904/Android/data/com.weidi.usefragments/files/Movies";
        PATH = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";
        mAudioWrapper.savePath = PATH + "audio.aac";

        /*try {
            mAudioWrapper.outputStream = new BufferedOutputStream(
                    new FileOutputStream(mAudioWrapper.savePath), BUFFER);
            mVideoWrapper.outputStream = new BufferedOutputStream(
                    new FileOutputStream(mVideoWrapper.savePath), BUFFER);
        } catch (Exception e) {
            e.printStackTrace();
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
            mAudioWrapper.outputStream = null;
            mVideoWrapper.outputStream = null;
            return false;
        }*/

        if (!prepareAudio()) {
            MLog.e(TAG, "internalPrepare() error");
            return false;
        }
        MLog.i(TAG, "internalPrepare() end");

        if (mIsLongTime) {
            new Thread(mAudioHandleData).start();
            SystemClock.sleep(500);
            new Thread(mAudioReadData).start();
        } else {
            new Thread(mAudioHandleData2).start();
            SystemClock.sleep(500);
            new Thread(mAudioReadData2).start();
        }
        mIsLongTime = false;

        return true;
    }

    private void internalPlay() {
        if (mAudioWrapper.isPausedForUser
                && !mAudioWrapper.isPausedForCache
                && !mAudioWrapper.isPausedForSeek) {
            mAudioWrapper.isPausedForUser = false;
            notifyToHandle(mAudioWrapper);
        }
    }

    private void internalPause() {
        if (!mAudioWrapper.isPausedForUser
                && !mAudioWrapper.isPausedForCache
                && !mAudioWrapper.isPausedForSeek) {
            mAudioWrapper.isPausedForUser = true;
        }
    }

    private void internalStop() {
        internalRelease();
    }

    private void internalRelease() {
        MLog.d(TAG, "internalRelease() start");
        mAudioWrapper.isReading = false;
        mAudioWrapper.isHandling = false;
        mAudioWrapper.isPausedForUser = false;
        mAudioWrapper.isPausedForCache = false;
        mAudioWrapper.isPausedForSeek = false;

        notifyToRead(mAudioWrapper);
        notifyToHandle(mAudioWrapper);
        MLog.d(TAG, "internalRelease() end");
    }

    private void internalDestroy() {
        MLog.d(TAG, "internalDestroy() start");
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }

        /*if (mIsBoundRemoteService) {
            try {
                unbindService(mServiceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            unregisterReceiver(mOnePixelReceiver);
            unregisterReceiver(mScreenStateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        // 停止前台服务--参数：表示是否移除之前的通知
        // stopForeground(true);
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

    private void setVolume() {
        if (mAudioWrapper.mAudioTrack == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioWrapper.mAudioTrack.setVolume(mVolume);
        } else {
            mAudioWrapper.mAudioTrack.setStereoVolume(mVolume, mVolume);
        }
    }

    private String preElapsedTime = null;
    private String curElapsedTime = null;
    private boolean onlyOneStart = true;

    /***
     * 填充数据送到底层进行编解码
     * @param codec
     * @param data
     * @param offset
     * @param size
     * @param presentationTimeUs
     * @return
     */
    private static boolean feedInputBuffer(
            MediaCodec codec,
            byte[] data,
            int offset,
            int size,
            long presentationTimeUs) {
        try {
            // 拿到房间号
            int roomIndex = codec.dequeueInputBuffer(TIME_OUT);
            if (roomIndex >= 0) {
                ByteBuffer room = null;
                // 根据房间号找到房间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getInputBuffer(roomIndex);
                } else {
                    room = codec.getInputBuffers()[roomIndex];
                }
                if (room != null) {
                    // 入住之前打扫一下房间
                    room.clear();
                    // 入住
                    room.put(data, offset, size);
                }
                int flags = 0;
                if (size == 0) {
                    presentationTimeUs = 0L;
                    flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                }
                // 通知已经"入住"了,可以进行"编解码"的操作了
                codec.queueInputBuffer(
                        roomIndex,
                        offset,
                        size,
                        presentationTimeUs,
                        flags);
            }
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | NullPointerException e) {
            MLog.e(TAG, "feedInputBuffer() Input occur exception: " + e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /***
     * 拿出数据(在底层已经经过编解码了)进行处理(如视频数据进行渲染,音频数据进行播放)
     * @param wrapper
     * @return
     */
    private boolean drainOutputBuffer(SimpleWrapper wrapper) {
        // 房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        for (; ; ) {
            if (!wrapper.isHandling) {
                return false;
            }
            try {
                // 房间号
                int roomIndex = wrapper.decoderMediaCodec.dequeueOutputBuffer(roomInfo, TIME_OUT);
                switch (roomIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        // 像音频,第一个输出日志
                        /*MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.INFO_TRY_AGAIN_LATER");*/
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        // 像音频,第三个输出日志
                        MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                        handleAudioOutputFormat();
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        // 像音频,第二个输出日志.视频好像没有这个输出日志
                        MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    default:
                        break;
                }
                if (roomIndex < 0) {
                    break;
                }

                ByteBuffer room = null;
                // 根据房间号找到房间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = wrapper.decoderMediaCodec.getOutputBuffer(roomIndex);
                } else {
                    room = wrapper.decoderMediaCodec.getOutputBuffers()[roomIndex];
                }
                // 房间大小
                int roomSize = roomInfo.size;
                if (room != null) {
                    // audio
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    handleAudioOutputBuffer(roomIndex, room, roomInfo, roomSize);
                }

                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    MLog.d(TAG, "drainOutputBuffer() " +
                            "Audio Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    return false;
                }
                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    MLog.d(TAG, "drainOutputBuffer() " +
                            "Audio Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                }

                wrapper.decoderMediaCodec.releaseOutputBuffer(roomIndex, false);
            } catch (IllegalStateException
                    | IllegalArgumentException
                    | NullPointerException e) {
                MLog.e(TAG, "drainOutputBuffer() Audio Output occur exception: " + e);
                e.printStackTrace();
                return false;
            }
        }// for(;;) end

        return true;
    }

    private boolean feedInputBufferAndDrainOutputBuffer(SimpleWrapper wrapper) {
        if (!wrapper.isHandling) {
            return false;
        }

        // seek的时候就不要往下走了
        if (wrapper.isPausedForSeek) {
            return true;
        }

        // 音视频都已经开始的话,就可以播放了
        if (mAudioWrapper.isStarted
                && mAudioWrapper.isHandling
                && onlyOneStart) {
            onlyOneStart = false;
            if (mCallback != null) {
                MLog.i(TAG, "onPlayed");
                mCallback.onPlayed();
            }
        }

        boolean feedInputBufferResult = false;
        boolean drainOutputBufferResult = false;
        try {
            // Input Data
            feedInputBufferResult = feedInputBuffer(
                    wrapper.decoderMediaCodec,
                    wrapper.frameData,
                    0,
                    wrapper.frameDataLength,
                    wrapper.presentationTimeUs1);

            // Output Data
            drainOutputBufferResult = drainOutputBuffer(wrapper);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return feedInputBufferResult && drainOutputBufferResult;
    }

    private void handleAudioOutputFormat() {
        /***
         解码前
         {mime=audio/mp4a-latm, aac-profile=2, channel-count=2, track-id=2, bitrate=96000,
         max-input-size=444, durationUs=10871488000,
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2],
         sample-rate=48000, max-bitrate=96000}
         解码后
         {pcm-encoding=2, mime=audio/raw, channel-count=2, sample-rate=48000}
         */
        MediaFormat newMediaFormat = mAudioWrapper.decoderMediaCodec.getOutputFormat();
        try {
            Class clazz = Class.forName("android.media.MediaFormat");
            Method method = clazz.getDeclaredMethod("getMap");
            method.setAccessible(true);
            Object newObject = method.invoke(newMediaFormat);
            Object oldObject = method.invoke(mAudioWrapper.decoderMediaFormat);
            if (newObject != null
                    && newObject instanceof Map
                    && oldObject != null
                    && oldObject instanceof Map) {
                Map<String, Object> newMap = (Map) newObject;
                Map<String, Object> oldMap = (Map) oldObject;
                for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                    oldMap.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mAudioWrapper.decoderMediaFormat = newMediaFormat;
        }
        MLog.d(TAG, "handleAudioOutputFormat() newMediaFormat: " +
                mAudioWrapper.decoderMediaFormat);

        if (mAudioWrapper.mAudioTrack != null) {
            mAudioWrapper.mAudioTrack.release();
        }

        // 创建AudioTrack
        int sampleRateInHz =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
        if (AACPlayer.sampleRateIndexMap.containsKey(sampleRateInHz)
                && AACPlayer.channelConfigIndexMap.containsKey(channelCount)) {
            List<byte[]> list = new ArrayList<>();
            list.add(MediaUtils.buildAacAudioSpecificConfig(
                    AACPlayer.sampleRateIndexMap.get(sampleRateInHz),
                    AACPlayer.channelConfigIndexMap.get(channelCount)));
            MediaUtils.setCsdBuffers(mAudioWrapper.decoderMediaFormat, list);
        }
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            // 关键参数(需要解码后才能知道)
            audioFormat =
                    mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
        }

        // create AudioTrack
        mAudioWrapper.mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioWrapper.mAudioTrack != null) {
            setVolume();
            mAudioWrapper.mAudioTrack.play();
            mAudioWrapper.isStarted = true;
            MLog.d(TAG, "handleAudioOutputFormat() 声音马上输出......");
        } else {
            MLog.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
            mAudioWrapper.isReading = false;
            mAudioWrapper.isHandling = false;
            notifyToRead(mAudioWrapper);
            notifyToHandle(mAudioWrapper);
        }
    }

    private int handleAudioOutputBuffer(
            int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
        // 被使用到video那里进行"睡眠"多少时间的判断
        mAudioWrapper.presentationTimeUs2 = roomInfo.presentationTimeUs;

        // 输出音频
        if (mAudioWrapper.isHandling
                && mAudioWrapper.mAudioTrack != null
                && mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            room.position(roomInfo.offset);
            room.limit(roomInfo.offset + roomSize);
            byte[] audioData = new byte[roomSize];
            room.get(audioData, 0, audioData.length);
            mAudioWrapper.mAudioTrack.write(audioData, 0, audioData.length);
        }

        return 0;
    }

    private void notifyToRead(SimpleWrapper wrapper) {
        /*if (wrapper == null) {
            return;
        }*/
        synchronized (wrapper.readDataLock) {
            wrapper.readDataLock.notify();
        }
    }

    private void notifyToReadWait(SimpleWrapper wrapper) {
        /*if (wrapper == null) {
            return;
        }*/
        try {
            synchronized (wrapper.readDataLock) {
                wrapper.readDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void notifyToHandle(SimpleWrapper wrapper) {
        /*if (wrapper == null) {
            return;
        }*/
        synchronized (wrapper.handleDataLock) {
            wrapper.handleDataLock.notify();
        }
    }

    private void notifyToHandleWait(SimpleWrapper wrapper) {
        /*if (wrapper == null) {
            return;
        }*/
        try {
            synchronized (wrapper.handleDataLock) {
                wrapper.handleDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Runnable mAudioHandleData = new Runnable() {
        @Override
        public void run() {
            handleData(mAudioWrapper);
        }
    };

    private Runnable mAudioReadData = new Runnable() {
        @Override
        public void run() {
            readData(mAudioWrapper);
        }
    };

    private Runnable mAudioHandleData2 = new Runnable() {
        @Override
        public void run() {
            handleData2(mAudioWrapper);
        }
    };

    private Runnable mAudioReadData2 = new Runnable() {
        @Override
        public void run() {
            readData2(mAudioWrapper);
        }
    };

    /////////////////////////////////////////////////////////////////

    private final IGuardAidl.Stub iGuardAidlStub = new IGuardAidl.Stub() {
        @Override
        public void wakeUp(String title, String discription, int iconRes) throws RemoteException {
            MLog.d(TAG, "wakeUp() title: " + title + " discription: " + discription);
        }
    };
    private boolean mIsBoundRemoteService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MLog.d(TAG, "onServiceConnected()");
            try {
                // Rrmote
                IGuardAidl guardAidl = IGuardAidl.Stub.asInterface(service);
                guardAidl.wakeUp("@^@", "888", R.drawable.a1);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MLog.d(TAG, "onServiceDisconnected()");
            if (KeepLive.isServiceRunning(
                    getApplicationContext(),
                    "com.weidi.usefragments.business.audio_player.MusicService")) {
                MLog.d(TAG, "onServiceDisconnected() MusicService is alive");
            }
            JobHandlerService.startForeground(MusicService.this, "Alexander", "MusicService");

            Intent intent2 = new Intent(MusicService.this, OnePixelActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    MusicService.this, 0, intent2, 0);
            try {
                pendingIntent.send();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(MusicService.this, RemoteService.class);
            startService(intent);
            mIsBoundRemoteService = bindService(
                    intent, mServiceConnection, Context.BIND_ABOVE_CLIENT);
            PowerManager pm =
                    (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();
            if (isScreenOn) {
                sendBroadcast(new Intent("_ACTION_SCREEN_ON"));
            } else {
                sendBroadcast(new Intent("_ACTION_SCREEN_OFF"));
            }
        }
    };

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (intent.getAction().equals("_ACTION_SCREEN_OFF")) {

            } else if (intent.getAction().equals("_ACTION_SCREEN_ON")) {

            }
        }
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
