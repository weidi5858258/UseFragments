package com.weidi.usefragments.tool;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by weidi on 2019/6/30.
 */

public class SampleAudioPlayer {

    private static final String TAG =
            SampleAudioPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int TIME_OUT = 10000;
    private static final int PLAY = 0x0001;
    private static final int PAUSE = 0x0002;
    private static final int STOP = 0x0003;
    private static final int RELEASE = 0x0004;
    private static final int PREV = 0x0005;
    private static final int NEXT = 0x0006;

    // 为了注册广播
    private Context mContext;
    private String mPath;
    private MediaExtractor mMediaExtractor;
    private MediaCodec mAudioDncoderMediaCodec;
    private MediaFormat mAudioDncoderMediaFormat;
    private AudioTrack mAudioTrack;
    private int mAudioTrackIndex = -1;
    private long mCurFileSize;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;
    private volatile static ExecutorService singleService;

    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private Object mPauseLock = new Object();
    private Object mStopLock = new Object();

    private Callback mCallback;

    public interface Callback {
        void onPlaybackReady();

        void onPlaybackPaused();

        void onPlaybackStarted();

        void onPlaybackFinished();

        void onProgressUpdated(int progress);

        void onPlaybackError();

        void onPlaybackInfo(String info);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public SampleAudioPlayer(String path) {
        mPath = path;
        init();
    }

    public SampleAudioPlayer() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
        registerHeadsetPlugReceiver();
    }

    public void setPath(String path) {
        mPath = path;
        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
    }

    public void play() {
        mUiHandler.removeMessages(PLAY);
        mUiHandler.sendEmptyMessage(PLAY);
    }

    public void pause() {
        mThreadHandler.removeMessages(PAUSE);
        mThreadHandler.sendEmptyMessage(PAUSE);
    }

    public void stop() {
        mThreadHandler.removeMessages(STOP);
        mThreadHandler.sendEmptyMessage(STOP);
    }

    public void prev() {
        mThreadHandler.removeMessages(PREV);
        mThreadHandler.sendEmptyMessageDelayed(PREV, 500);
    }

    public void next() {
        mThreadHandler.removeMessages(NEXT);
        mThreadHandler.sendEmptyMessageDelayed(NEXT, 500);
    }

    public void release() {
        mThreadHandler.removeMessages(RELEASE);
        mThreadHandler.sendEmptyMessage(RELEASE);
    }

    public void destroy() {
        if (mHandlerThread != null
                && mAudioDncoderMediaCodec == null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        unregisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
    }

    public boolean isRunning() {
        return mIsRunning;
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

    private static ExecutorService getSingleThreadPool() {
        if (singleService == null) {
            synchronized (SampleAudioPlayer.class) {
                if (singleService == null) {
                    singleService = Executors.newSingleThreadExecutor();
                }
            }
        }
        return singleService;
    }

    private void init() {
        EventBusUtils.register(this);
        getSingleThreadPool();

        mHandlerThread = new HandlerThread("OtherHandlerThread");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleAudioPlayer.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleAudioPlayer.this.uiHandleMessage(msg);
            }
        };
    }

    private void internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            return;
        }

        File file = new File(mPath);
        if (!file.canRead()) {
            if (DEBUG)
                MLog.e(TAG, "不能读取此文件: " + mPath);
            return;
        }
        // 9561547
        // 9525288
        mCurFileSize = file.length();
        if (DEBUG)
            MLog.i(TAG, "internalPrepare() mCurFileSize: " + mCurFileSize);

        if (DEBUG)
            MLog.d(TAG, "internalPrepare() start");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalPrepare() start");
        }

        mMediaExtractor = null;
        mAudioDncoderMediaCodec = null;
        mAudioDncoderMediaFormat = null;
        mAudioTrackIndex = -1;

        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int trackCount = mMediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mAudioDncoderMediaFormat = mMediaExtractor.getTrackFormat(i);
            if (DEBUG)
                MLog.d(TAG, "internalPrepare() mAudioDncoderMediaFormat: " +
                        mAudioDncoderMediaFormat.toString());
            if (mCallback != null) {
                mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                        " mIsPaused: " + mIsPaused);
                mCallback.onPlaybackInfo("internalPrepare() " +
                        mAudioDncoderMediaFormat.toString());
            }
            String mime = mAudioDncoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                boolean hasException = false;
                try {
                    mMediaExtractor.selectTrack(i);
                    mAudioDncoderMediaCodec = MediaCodec.createDecoderByType(mime);
                    mAudioDncoderMediaCodec.configure(
                            mAudioDncoderMediaFormat, null, null, 0);
                    mAudioDncoderMediaCodec.start();
                    mAudioTrackIndex = i;
                } catch (MediaCodec.CodecException e) {
                    e.printStackTrace();
                    hasException = true;
                } catch (MediaCodec.CryptoException e) {
                    e.printStackTrace();
                    hasException = true;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    hasException = true;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    hasException = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    hasException = true;
                }
                if (hasException) {
                    if (mAudioDncoderMediaCodec != null) {
                        mAudioDncoderMediaCodec.release();
                        mAudioDncoderMediaCodec = null;
                    }
                    mAudioDncoderMediaFormat = null;
                    return;
                }
                break;
            }
        }
        if (mAudioDncoderMediaFormat == null) {
            mIsRunning = false;
            if (mCallback != null) {
                mCallback.onPlaybackInfo("internalStart() mAudioDncoderMediaFormat is null");
                mCallback.onPlaybackFinished();
            }
            return;
        }

        int sampleRateInHz = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        // 还不知道怎样从一个音频中得到"数据位宽"
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        // 爱你没有停止的一天 泰国最流行爱情歌.mp3
        // 32000 1
        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioTrack != null) {
            mAudioTrack.play();
        }

        if (DEBUG)
            MLog.d(TAG, "internalPrepare() end");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalPrepare() end");
        }
    }

    private void internalStart() {
        if (mAudioTrack == null) {
            mIsRunning = false;
            if (mCallback != null) {
                mCallback.onPlaybackInfo("internalStart() mAudioTrack is null");
                mCallback.onPlaybackFinished();
            }
            return;
        } else if (mAudioDncoderMediaCodec == null) {
            mIsRunning = false;
            if (mCallback != null) {
                mCallback.onPlaybackInfo("internalStart() mAudioDncoderMediaCodec is null");
                mCallback.onPlaybackFinished();
            }
            return;
        } else if (mAudioTrackIndex < 0) {
            mIsRunning = false;
            if (mCallback != null) {
                mCallback.onPlaybackInfo("internalStart() mAudioTrackIndex < 0");
                mCallback.onPlaybackFinished();
            }
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "internalStart() start");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalStart() start");
        }

        long hasReadedSize = 0;
        int readSize = -1;
        // 房间号
        int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
        // 房间
        ByteBuffer room = null;
        // 房间大小
        int roomSize = 0;
        // 房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = mAudioDncoderMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mAudioDncoderMediaCodec.getOutputBuffers();
        while (mIsRunning) {
            // stop device
            if (!mIsRunning) {
                synchronized (mStopLock) {
                    mStopLock.notify();
                }
                break;
            }

            // pause device
            if (mIsPaused) {
                synchronized (mPauseLock) {
                    if (DEBUG)
                        MLog.w(TAG, "internalStart() mPauseLock.wait() start");
                    onPlaybackInfo(
                            "mIsRunning: " + mIsRunning + " mIsPaused: " + mIsPaused);
                    onPlaybackInfo(
                            "internalStart() mPauseLock.wait() start");
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (DEBUG)
                        MLog.w(TAG, "internalStart() mPauseLock.wait() end");
                    onPlaybackInfo(
                            "mIsRunning: " + mIsRunning + " mIsPaused: " + mIsPaused);
                    onPlaybackInfo(
                            "internalStart() mPauseLock.wait() end");
                }
            }

            try {
                // Input
                roomIndex = mAudioDncoderMediaCodec.dequeueInputBuffer(-1);
                if (roomIndex >= 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        room = mAudioDncoderMediaCodec.getInputBuffer(roomIndex);
                    } else {
                        room = inputBuffers[roomIndex];
                    }
                    room.clear();
                    readSize = mMediaExtractor.readSampleData(room, 0);
                    hasReadedSize += readSize;
                    /*if (DEBUG)
                        MLog.i(TAG, "internalStart() hasReadedSize: " + hasReadedSize +
                                " readSize: " + readSize);*/
                    long presentationTimeUs = System.nanoTime() / 1000;
                    if (readSize < 0) {
                        mAudioDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                0,
                                0,
                                /***
                                 发送此flags的目的是,在Output阶段,根据
                                 (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                                 这个条件判断已经到结尾了,然后退出.这是优雅的退出方式.
                                 */
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                        if (DEBUG)
                            MLog.i(TAG, "internalStart() read end");
                        onPlaybackInfo(
                                "mIsRunning: " + mIsRunning + " mIsPaused: " + mIsPaused);
                        onPlaybackInfo(
                                "internalStart() read end");
                        mIsRunning = false;
                        internalStop(false);
                        if (mCallback != null) {
                            mCallback.onPlaybackFinished();
                        }
                        break;
                    } else {
                        mAudioDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                readSize,
                                // presentationTimeUs,
                                mMediaExtractor.getSampleTime(),
                                0);
                        mMediaExtractor.advance();
                    }
                }
            } catch (MediaCodec.CryptoException e) {
                MLog.e(TAG, "internalStart Input occur exception: " + e);
                onPlaybackInfo(
                        "mIsRunning: " + mIsRunning + " mIsPaused: " + mIsPaused);
                onPlaybackInfo(
                        "internalStart() Input occur exception: ");
                onPlaybackInfo(
                        e.toString());
                // internalStop(false);

                mIsRunning = false;
                internalStop(false);
                break;
            } catch (MediaCodec.CodecException e) {
                MLog.e(TAG, "internalStart Input occur exception: " + e);
                if (mCallback != null) {
                    mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                            " mIsPaused: " + mIsPaused);
                    mCallback.onPlaybackInfo("internalStart() Input occur exception: ");
                    mCallback.onPlaybackInfo(e.toString());
                }
                // internalStop(false);

                mIsRunning = false;
                internalStop(false);
                break;
            } catch (IllegalStateException e) {
                MLog.e(TAG, "internalStart Input occur exception: " + e);
                if (mCallback != null) {
                    mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                            " mIsPaused: " + mIsPaused);
                    mCallback.onPlaybackInfo("internalStart() Input occur exception: ");
                    mCallback.onPlaybackInfo(e.toString());
                }
                // internalStop(false);

                mIsRunning = false;
                internalStop(false);
                break;
            }


            // Output
            for (; ; ) {
                // stop device
                if (!mIsRunning) {
                    synchronized (mStopLock) {
                        mStopLock.notify();
                    }
                    return;
                }
                try {
                    roomIndex = mAudioDncoderMediaCodec.dequeueOutputBuffer(
                            roomInfo, TIME_OUT);
                    if (roomIndex < 0) {
                        break;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        room = mAudioDncoderMediaCodec.getOutputBuffer(roomIndex);
                    } else {
                        room = outputBuffers[roomIndex];
                    }
                    // room.limit()与bufferInfo.size的大小是相同的
                    // 一帧AAC数据,大小大概为500~550这个范围(每次得到的room大小是不一样的)
                    roomSize = roomInfo.size;
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    byte[] pcmData = new byte[roomSize];
                    room.get(pcmData, 0, pcmData.length);
                    mAudioTrack.write(pcmData, 0, pcmData.length);
                    mAudioDncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                } catch (MediaCodec.CodecException e) {
                    MLog.e(TAG, "internalStart() Output occur exception: " + e);
                    if (mCallback != null) {
                        mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                                " mIsPaused: " + mIsPaused);
                        mCallback.onPlaybackInfo("internalStart() Output occur exception: ");
                        mCallback.onPlaybackInfo(e.toString());
                    }
                    // internalStop(false);

                    mIsRunning = false;
                    internalStop(false);
                    return;
                } catch (IllegalStateException e) {
                    MLog.e(TAG, "internalStart() Output occur exception: " + e);
                    if (mCallback != null) {
                        mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                                " mIsPaused: " + mIsPaused);
                        mCallback.onPlaybackInfo("internalStart() Output occur exception: ");
                        mCallback.onPlaybackInfo(e.toString());
                    }
                    // internalStop(false);

                    mIsRunning = false;
                    internalStop(false);
                    return;
                } catch (IllegalArgumentException e) {
                    MLog.e(TAG, "internalStart() Output occur exception: " + e);
                    if (mCallback != null) {
                        mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                                " mIsPaused: " + mIsPaused);
                        mCallback.onPlaybackInfo("internalStart() Output occur exception: ");
                        mCallback.onPlaybackInfo(e.toString());
                    }
                    // internalStop(false);

                    mIsRunning = false;
                    internalStop(false);
                    return;
                }
            }
        }

        if (DEBUG)
            MLog.d(TAG, "internalStart() end");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalStart() end");
        }
    }

    private void internalPlay() {
        if (!mIsRunning && !mIsPaused) {
            if (mCallback != null) {
                mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                        " mIsPaused: " + mIsPaused);
                mCallback.onPlaybackInfo("internalPlay() execute Runnable");
            }
            // state
            mIsRunning = true;
            mIsPaused = false;
            getSingleThreadPool().execute(mPlayRunnable);
        }

        if (mIsRunning) {
            if (DEBUG)
                MLog.d(TAG, "internalPlay() start");
            if (mCallback != null) {
                mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                        " mIsPaused: " + mIsPaused);
                mCallback.onPlaybackInfo("internalPlay() start");
            }

            mIsPaused = false;
            synchronized (mPauseLock) {
                mPauseLock.notify();
            }

            if (DEBUG)
                MLog.d(TAG, "internalPlay() end");
            if (mCallback != null) {
                mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                        " mIsPaused: " + mIsPaused);
                mCallback.onPlaybackInfo("internalPlay() end");
            }
        }
    }

    private void internalPause() {
        if (!mIsRunning) {
            return;
        }

        mIsPaused = true;
    }

    private void internalStop(boolean needWait) {
        if (!mIsRunning) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "internalStop() start");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalStop() start");
        }

        mIsPaused = false;
        synchronized (mPauseLock) {
            mPauseLock.notify();
        }

        if (mIsRunning && needWait) {
            mIsRunning = false;
            synchronized (mStopLock) {
                if (mCallback != null) {
                    mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                            " mIsPaused: " + mIsPaused);
                    mCallback.onPlaybackInfo("internalStop mStopLock.wait() start");
                }
                try {
                    mStopLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mCallback != null) {
                    mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                            " mIsPaused: " + mIsPaused);
                    mCallback.onPlaybackInfo("internalStop mStopLock.wait() end");
                }
            }
        }
        mIsRunning = false;

        MediaUtils.stopAudioTrack(mAudioTrack);
        MediaUtils.stopMediaCodec(mAudioDncoderMediaCodec);

        if (DEBUG)
            MLog.d(TAG, "internalStop() end");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalStop() end");
        }
    }

    private void internalRelease() {
        if (DEBUG)
            MLog.d(TAG, "internalRelease() start");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalRelease() start");
        }
        internalStop(false);
        MediaUtils.releaseAudioTrack(mAudioTrack);
        MediaUtils.releaseMediaCodec(mAudioDncoderMediaCodec);
        if (DEBUG)
            MLog.d(TAG, "internalRelease() end");
        if (mCallback != null) {
            mCallback.onPlaybackInfo("mIsRunning: " + mIsRunning +
                    " mIsPaused: " + mIsPaused);
            mCallback.onPlaybackInfo("internalRelease() end");
        }
    }

    private void internalPrev() {
        internalRelease();
        play();
    }

    private void internalNext() {
        internalRelease();
        play();
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                onKeyDown(KeyEvent.KEYCODE_HEADSETHOOK, null);
                break;
            default:
                break;
        }
        return result;
    }

    private void onPlaybackInfo(String info) {
        if (mCallback != null) {
            mCallback.onPlaybackInfo(info);
        }
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case PAUSE:
                internalPause();
                break;
            case STOP:
                internalStop(true);
                break;
            case RELEASE:
                internalRelease();
                break;
            case PREV:
                internalPrev();
                break;
            case NEXT:
                internalNext();
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
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (firstFlag && secondFlag && threeFlag) {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 3");
                    if (mCallback != null) {
                        mCallback.onPlaybackFinished();
                    }
                } else if (firstFlag && secondFlag) {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 2");
                    if (mCallback != null) {
                        mCallback.onPlaybackFinished();
                    }
                } else {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 1");
                    if (mIsRunning) {
                        if (mIsPaused) {
                            internalPlay();
                        } else {
                            internalPause();
                        }
                    }
                }
                firstFlag = false;
                secondFlag = false;
                threeFlag = false;
                break;
            case PLAY:
                internalPlay();
                break;
            default:
                break;
        }
    }

    private Runnable mPlayRunnable = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            internalPrepare();
            internalStart();
        }
    };

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
        if (mContext == null) {
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
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset not connected");
                        pause();
                        break;
                    case 1:
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset has connected");
                        play();
                        break;
                    default:
                }
            }
        }
    }

}
