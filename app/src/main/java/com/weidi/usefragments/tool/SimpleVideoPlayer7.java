package com.weidi.usefragments.tool;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 Created by weidi on 2019/7/10.

 亲手写过这个程序后,最最最关键的点
 我认为是时间戳问题.
 如果播放本地视频,几乎不需要时间缓存,从头手播到尾,那么几乎没什么问题,音视频几乎达到同步.
 但是经过seek,pause后,音视频就会发生不同步的现象.

 现在有的问题:
 视频:
 1.花屏(原因:在某些视频帧上多读或者少读几个字节,就会造成花屏.已解决)
 2.播放速度过快(queueInputBuffer()方法中的presentationTimeUs参数设置不正确,也就是时间戳不正确.已解决)
 3.一方pause时,另一方没有pause
 4.seek
 5.pause or play
 6.prev or next
 7.sound控制
 8.屏幕明亮控制

 总体流程:
 音频视频各有2个缓存用于存数据,各有1个缓存用于处理数据

 缓存数据:
 程序开始时称填满缓存1.
 然后
 缓存1满缓存2未满则缓存2;
 缓存1未满缓存2满则缓存1;
 缓存1满缓存2满则等待,用一个标志标记哪个缓存先满.

 处理数据:
 从缓存中copy过来的数据都是一帧帧的音视频帧.
 因此只要截取每一帧然后进行解码就行了.

 */

public class SimpleVideoPlayer7 {

    private static final String TAG =
            SimpleVideoPlayer7.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int CACHE_AUDIO_LOCAL = 1024 * 512;
    private static final int CACHE_VIDEO_LOCAL = 1024 * 1024;
    private static final int CACHE_AUDIO_HTTP = 1024 * 1024 * 2;
    private static final int CACHE_VIDEO_HTTP = 1024 * 1024 * 8;
    // 音频一帧的大小不能超过这个值,不然出错(如果设成1024 * 1024会有杂音,不能过大,调查了好久才发现跟这个有关)
    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;
    // 视频一帧的大小不能超过这个值,不然出错
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024;

    public static final int FIRST_READ_DATA1 = 0x0001;
    public static final int FIRST_READ_DATA2 = 0x0002;

    private static final int TYPE_AUDIO = 0x0001;
    private static final int TYPE_VIDEO = 0x0002;

    private static final int BUFFER = 1024 * 1024 * 2;

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


    private static final int STATUS_READ_DATA1_STARTED = 0x0001;
    private static final int STATUS_READ_DATA2_STARTED = 0x0002;
    private static final int STATUS_READ_DATA_PAUSED = 0x0003;
    private static final int STATUS_READ_FINISHED = 0x0004;
    private static final int STATUS_READ_ERROR = 0x0005;

    // 为了注册广播
    private Context mContext;
    private String mPath;
    private float mVolume = 1.0f;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;
    private Callback mCallback;

    private AudioWrapper mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
    private VideoWrapper mVideoWrapper = new VideoWrapper(TYPE_VIDEO);

    private static final int HEADER_FLAG_LENGTH = 4;
    private static final byte[] HEADER_FLAG1 = new byte[]{88, 88, 88, 88};
    private static final byte[] HEADER_FLAG2 = new byte[]{-88, -88, -88, -88};

    private static class SimpleWrapper {
        public String mime = null;
        public MediaExtractor extractor = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        // 是否需要渲染图像(播放音频不需要,播放视频需要)
        public boolean render = false;
        public int trackIndex = -1;
        // 使用于while条件判断
        public boolean isReading = false;
        // 使用于while条件判断
        public boolean isHandling = false;
        // readData1和readData2用于缓存数据,handleData用于处理数据
        public byte[] readData1 = null;
        public byte[] readData2 = null;
        public byte[] handleData = null;

        // readData1是否已装满数据.false表示未装满
        // 如果readData1中的数据复制给了mHandleData,那么立刻设为false
        public boolean isReadData1Full = false;
        // readData2是否已装满数据.false表示未装满
        public boolean isReadData2Full = false;
        // readData1和readData2都装满的情况下,先读取哪一个数据
        public int first = FIRST_READ_DATA1;

        // 保存时间戳
        public ArrayMap<Long, Long> setTime1 = new ArrayMap<Long, Long>();
        public ArrayMap<Long, Long> setTime2 = new ArrayMap<Long, Long>();
        public ArrayMap<Long, Long> getTime = new ArrayMap<Long, Long>();

        public int CACHE;
        // 用于标识音频还是视频
        public int type;
        // readData1Size中保存的实际数据大小(包含了标志位长度)
        // 如果readData1中的数据复制给了handleData,那么立刻被设为0
        public int readData1Size = 0;
        public int readData2Size = 0;
        public int readDataSize = 0;
        // 一帧音频或者视频的最大值,用于创建frameData时使用
        public int frameMaxLength = 0;
        // 先往readData1中存数据
        public int readStatus = STATUS_READ_DATA1_STARTED;
        public byte preBufferLastByte = 0;
        // 总时长
        public long durationUs = 0;
        // 播放的时长(下面两个参数一起做的事是每隔一秒发一次回调函数)
        public long presentationTimeUs1 = 0;
        public long startTimeUs = 0;

        // 使用于时间戳
        public long presentationTimeUs2 = 0;

        // 下面4个变量在没有seek过的情况下统计是正确的,seek过后就不正确了
        // "块"的概念,一"块"有N个字节.
        // 编码时接收的数据就是一"块"的大小,少一个字节或者多一个字节都会出现异常
        public long readFrameCounts = 0;
        // 处理了多少"块"
        public long handleFrameCounts = 0;
        // 使用readSampleData(...)方法总共读了多少字节
        public long readFrameLengthTotal = 0;
        // 处理了多少字节
        public long handleFrameLengthTotal = 0;

        public BufferedOutputStream outputStream;
        public String savePath = null;
        public MediaUtils.Callback callback;

        public boolean isStarted = false;
        // 因为user所以pause
        public boolean isPausedForUser = false;
        // 因为cache所以pause
        public boolean isPausedForCache = false;
        public Object readDataLock = new Object();
        public Object handleDataLock = new Object();

        // 保存handleData中每个帧的帧头位置,前后两个位置一减,就是一帧的大小(在这里包含了标志位长度4个字节)
        public ArrayList<Integer> offsetList = new ArrayList<Integer>();
        // offsetList集合中总共多少个帧头位置
        public int offsetCounts = 0;
        public int preReadDataSize = 0;
        // 帧头位置
        public int offsetIndex = 0;
        // offsetList集合中最后一个帧头位置
        public int lastOffsetIndex = 0;
        // mHandleData中剩下的数据大小
        public int restOfDataSize = 0;
        // 音频或者视频一帧的实际大小
        public int frameDataLength = 0;
        // 放一帧音频或者视频的容器
        public byte[] frameData = null;

        // seekTo
        public long progressUs = -1;
        public boolean needToSeek = false;

        public SimpleWrapper(int type) {
            switch (type) {
                case TYPE_AUDIO:
                    this.type = TYPE_AUDIO;
                    CACHE = CACHE_AUDIO_HTTP;
                    frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
                    readData1 = new byte[CACHE_AUDIO_HTTP];
                    readData2 = new byte[CACHE_AUDIO_HTTP];
                    handleData = new byte[CACHE_AUDIO_HTTP];
                    break;
                case TYPE_VIDEO:
                    this.type = TYPE_VIDEO;
                    CACHE = CACHE_VIDEO_HTTP;
                    frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
                    readData1 = new byte[CACHE_VIDEO_HTTP];
                    readData2 = new byte[CACHE_VIDEO_HTTP];
                    handleData = new byte[CACHE_VIDEO_HTTP];
                    break;
                default:
                    break;
            }
        }

        public void clear() {
            mime = null;
            // extractor = null;
            decoderMediaCodec = null;
            decoderMediaFormat = null;
            render = false;
            trackIndex = -1;
            isReading = false;
            isHandling = false;
            Arrays.fill(readData1, (byte) 0);
            Arrays.fill(readData2, (byte) 0);
            Arrays.fill(handleData, (byte) 0);
            isReadData1Full = false;
            isReadData2Full = false;
            first = FIRST_READ_DATA1;
            preBufferLastByte = 0;
            durationUs = 0;
            presentationTimeUs1 = 0;
            startTimeUs = 0;
            presentationTimeUs2 = 0;
            setTime1.clear();
            setTime2.clear();
            getTime.clear();
            readData1Size = 0;
            readData2Size = 0;
            readDataSize = 0;
            readStatus = STATUS_READ_DATA1_STARTED;
            readFrameCounts = 0;
            handleFrameCounts = 0;
            readFrameLengthTotal = 0;
            handleFrameLengthTotal = 0;
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            savePath = null;
            isStarted = false;
            isPausedForUser = false;
            isPausedForCache = false;

            offsetList.clear();
            offsetCounts = 0;
            preReadDataSize = 0;
            offsetIndex = 0;
            lastOffsetIndex = 0;
            restOfDataSize = 0;
            frameDataLength = 0;

            progressUs = -1;
            needToSeek = false;
        }
    }

    private static class AudioWrapper extends SimpleWrapper {
        public AudioTrack mAudioTrack = null;

        public AudioWrapper(int type) {
            super(type);
        }

        public void clear() {
            if (mAudioTrack != null) {
                mAudioTrack.release();
            }
            if (decoderMediaCodec != null) {
                decoderMediaCodec.release();
            }
            super.clear();
        }
    }

    private static class VideoWrapper extends SimpleWrapper {
        public Surface mSurface = null;

        public VideoWrapper(int type) {
            super(type);
        }

        public void clear() {
            if (decoderMediaCodec != null) {
                decoderMediaCodec.release();
            }
            super.clear();
        }
    }

    public SimpleVideoPlayer7() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
        registerHeadsetPlugReceiver();
    }

    public void setPath(String path) {
        mPath = Contents.getUri();
        mPath = path;
        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
    }

    public void setSurface(Surface surface) {
        mVideoWrapper.mSurface = surface;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public long mProgressUs = -1;

    // 95160000
    public void setProgressUs(long progressUs) {
        MLog.d(TAG, "setProgressUs() progressUs: " + progressUs);
        setProgressUs(mAudioWrapper, progressUs);
        setProgressUs(mVideoWrapper, progressUs);
    }

    private void setProgressUs(SimpleWrapper wrapper, long progressUs) {
        if (progressUs < 0) {
            return;
        }

        mProgressUs = progressUs;
        wrapper.progressUs = progressUs;
        wrapper.needToSeek = true;
        MediaUtils.progressTimeMs = progressUs / 1000;

        String showInfo = null;
        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "setProgressUs() audio MediaUtils.progressTimeMs: " +
                    MediaUtils.progressTimeMs;
        } else {
            showInfo = "setProgressUs() video MediaUtils.progressTimeMs: " +
                    MediaUtils.progressTimeMs;
        }
        MLog.i(TAG, showInfo);

        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "setProgressUs() audio readDataLock.notify()";
        } else {
            showInfo = "setProgressUs() video readDataLock.notify()";
        }
        MLog.i(TAG, showInfo);

        // 两个缓存都满了,正在等待中,因此需要发送notify
        synchronized (wrapper.readDataLock) {
            wrapper.readDataLock.notify();
        }
    }

    public long getDurationUs() {
        return mVideoWrapper.durationUs;
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
        /*mThreadHandler.removeMessages(STOP);
        mThreadHandler.sendEmptyMessage(STOP);*/
    }

    public void prev() {
        /*mThreadHandler.removeMessages(PREV);
        mThreadHandler.sendEmptyMessageDelayed(PREV, 500);*/
    }

    public void next() {
        /*mThreadHandler.removeMessages(NEXT);
        mThreadHandler.sendEmptyMessageDelayed(NEXT, 500);*/
    }

    public void release() {
        mThreadHandler.removeMessages(MSG_RELEASE);
        mThreadHandler.sendEmptyMessage(MSG_RELEASE);
    }

    public boolean isPlaying() {
        return mAudioWrapper.isStarted
                && !mAudioWrapper.isPausedForUser
                && !mAudioWrapper.isPausedForCache
                && mVideoWrapper.isStarted
                && !mVideoWrapper.isPausedForUser
                && !mVideoWrapper.isPausedForCache;
    }

    public boolean isRunning() {
        return mAudioWrapper.isReading
                || mAudioWrapper.isHandling
                || mVideoWrapper.isReading
                || mVideoWrapper.isHandling;
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
                if (internalPrepare()) {
                    new Thread(mAudioReadData).start();
                    new Thread(mVideoReadData).start();
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
            case MSG_RELEASE:
                internalRelease();
                internalDestroy();
                break;
            case MSG_HANDLE_DATA_LOCK_WAIT:
                MLog.e(TAG, "卧槽!卧槽!卧槽!等了10分钟了还不能播放,爷不等了");
                internalRelease();
                if (internalPrepare()) {
                    new Thread(mAudioReadData).start();
                    new Thread(mVideoReadData).start();
                }
                break;
            case MSG_SEEK_TO_NOTIFY:
                notifyToHandleDataLock();
                break;
            case MSG_GAME_OVER:
                if (msg.obj == null) {
                    return;
                }
                if (msg.obj instanceof SimpleWrapper) {
                    SimpleWrapper wrapper = (SimpleWrapper) msg.obj;

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

                    if (!mAudioWrapper.isHandling
                            && !mVideoWrapper.isHandling) {
                        if (mCallback != null) {
                            MLog.w(TAG, "handleData() onFinished");
                            mCallback.onFinished();
                        }
                    }
                }
                break;
            /*case PLAY:
                internalPlay();
                break;
            case PAUSE:
                internalPause();
                break;
            case STOP:
            case RELEASE:
                internalStop();
                break;
            case PREV:
                internalPrev();
                break;
            case NEXT:
                internalNext();
                break;*/
            default:
                break;
        }
    }

    // Test
    //long step = 300000000;
    long step = 0;

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
                    step += 200;
                    long progress = (long) (((step / 3840.00) * mVideoWrapper.durationUs));
                    setProgressUs(progress);
                } else {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 1");
                    /*if (mIsAudioRunning) {
                        if (mIsAudioPaused) {
                            internalPlay();
                        } else {
                            internalPause();
                        }
                    }*/
                }
                firstFlag = false;
                secondFlag = false;
                threeFlag = false;
                break;
            case MSG_RELEASE:
                internalRelease();
                break;
            /*case PLAY:
                internalPlay();
                break;*/
            default:
                break;
        }
    }

    private void init() {
        EventBusUtils.register(this);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleVideoPlayer7.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleVideoPlayer7.this.uiHandleMessage(msg);
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            return false;
        }
        MLog.w(TAG, "internalPrepare() start");

        mAudioWrapper.clear();
        mVideoWrapper.clear();
        mAudioWrapper.isReading = true;
        mVideoWrapper.isReading = true;
        mAudioWrapper.callback = mAudioCallback;
        mVideoWrapper.callback = mVideoCallback;

        if (!mPath.startsWith("http://")
                && !mPath.startsWith("HTTP://")
                && !mPath.startsWith("https://")
                && !mPath.startsWith("HTTPS://")) {
            File file = new File(mPath);
            if (!file.canRead()
                    || file.isDirectory()) {
                if (DEBUG)
                    MLog.e(TAG, "不能读取此文件: " + mPath);
                mAudioWrapper.isReading = false;
                mVideoWrapper.isReading = false;
                return false;
            }
            long fileSize = file.length();
            if (DEBUG)
                MLog.d(TAG, "internalPrepare() fileSize: " + fileSize);

            // 如果是本地文件,重新初始化空间大小
            mAudioWrapper.CACHE = CACHE_AUDIO_LOCAL;
            mAudioWrapper.readData1 = new byte[CACHE_AUDIO_LOCAL];
            mAudioWrapper.readData2 = new byte[CACHE_AUDIO_LOCAL];
            mAudioWrapper.handleData = new byte[CACHE_AUDIO_LOCAL];

            mVideoWrapper.CACHE = CACHE_VIDEO_LOCAL;
            mVideoWrapper.readData1 = new byte[CACHE_VIDEO_LOCAL];
            mVideoWrapper.readData2 = new byte[CACHE_VIDEO_LOCAL];
            mVideoWrapper.handleData = new byte[CACHE_VIDEO_LOCAL];
        }

        String PATH = null;
        PATH = "/data/data/com.weidi.usefragments/files/Movies/";
        PATH = "/storage/37C8-3904/Android/data/com.weidi.usefragments/files/Movies";
        PATH = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";
        mAudioWrapper.savePath = PATH + "audio.aac";
        mVideoWrapper.savePath = PATH + "video.h264";

        try {
            mAudioWrapper.outputStream = new BufferedOutputStream(
                    new FileOutputStream(mAudioWrapper.savePath), BUFFER);
            mVideoWrapper.outputStream = new BufferedOutputStream(
                    new FileOutputStream(mVideoWrapper.savePath), BUFFER);
        } catch (Exception e) {
            e.printStackTrace();
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
            return false;
        }

        // 如果等待了10分钟后还没有数据,那么可以做一些事
        mThreadHandler.removeMessages(MSG_HANDLE_DATA_LOCK_WAIT);
        mThreadHandler.sendEmptyMessageDelayed(
                MSG_HANDLE_DATA_LOCK_WAIT, 1000 * 60 * 10);

        // Audio
        mAudioWrapper.extractor = new MediaExtractor();
        try {
            mAudioWrapper.extractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
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
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
            return false;
        }

        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mAudioWrapper.frameMaxLength =
                    mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }
        mAudioWrapper.frameData = new byte[mAudioWrapper.frameMaxLength];
        MLog.d(TAG, "internalPrepare()               audio mime: " +
                mAudioWrapper.mime);
        MLog.d(TAG, "internalPrepare() audio decoderMediaFormat: " +
                mAudioWrapper.decoderMediaFormat.toString());
        // 创建音频解码器
        try {
            mAudioWrapper.extractor.selectTrack(mAudioWrapper.trackIndex);
            if (!TextUtils.equals("audio/ac4", mAudioWrapper.mime)) {
                mAudioWrapper.decoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec(
                        mAudioWrapper.mime, mAudioWrapper.decoderMediaFormat);
            } else {
                MediaCodecInfo mediaCodecInfo =
                        MediaUtils.getDecoderMediaCodecInfo(mAudioWrapper.mime);
                String codecName = null;
                if (mediaCodecInfo != null) {
                    codecName = mediaCodecInfo.getName();
                } else {
                    if (TextUtils.equals("audio/ac4", mAudioWrapper.mime)) {
                        codecName = "OMX.google.raw.decoder";
                        mAudioWrapper.decoderMediaFormat.setString(
                                MediaFormat.KEY_MIME, "audio/raw");
                    }
                }
                if (!TextUtils.isEmpty(codecName)) {
                    mAudioWrapper.decoderMediaCodec =
                            MediaCodec.createByCodecName(codecName);
                    mAudioWrapper.decoderMediaCodec.configure(
                            mAudioWrapper.decoderMediaFormat, null, null, 0);
                    mAudioWrapper.decoderMediaCodec.start();
                }
            }
            mAudioWrapper.render = false;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException
                | IOException e) {
            e.printStackTrace();
            if (mAudioWrapper.decoderMediaCodec != null) {
                mAudioWrapper.decoderMediaCodec.release();
            }
            mAudioWrapper.decoderMediaCodec = null;
        }
        if (mAudioWrapper.decoderMediaCodec == null) {
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
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
            MLog.d(TAG, "internalPrepare() audio csd-0: " + sb.toString());
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
            MLog.d(TAG, "internalPrepare() audio csd-1: " + sb.toString());
        }

        // Video
        mVideoWrapper.extractor = new MediaExtractor();
        try {
            mVideoWrapper.extractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
            return false;
        }
        trackCount = mVideoWrapper.extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mVideoWrapper.decoderMediaFormat = mVideoWrapper.extractor.getTrackFormat(i);
            String mime = mVideoWrapper.decoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                mVideoWrapper.mime = mime;
                mVideoWrapper.trackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(mVideoWrapper.mime)
                || mVideoWrapper.trackIndex == -1) {
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
            return false;
        }

        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mVideoWrapper.frameMaxLength =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }
        mVideoWrapper.frameData = new byte[mVideoWrapper.frameMaxLength];
        /***
         BITRATE_MODE_CQ:  表示完全不控制码率，尽最大可能保证图像质量
         BITRATE_MODE_CBR: 表示编码器会尽量把输出码率控制为设定值
         BITRATE_MODE_VBR: 表示编码器会根据图像内容的复杂度（实际上是帧间变化量的大小）来动态调整输出码率，
         图像复杂则码率高，图像简单则码率低
         */
        mVideoWrapper.decoderMediaFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        // 使用于编码器
        /*mVideoWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);*/
        MLog.d(TAG, "internalPrepare()               video mime: " +
                mVideoWrapper.mime);
        MLog.d(TAG, "internalPrepare() video decoderMediaFormat: " +
                mVideoWrapper.decoderMediaFormat.toString());
        // 创建视频解码器
        try {
            mVideoWrapper.extractor.selectTrack(mVideoWrapper.trackIndex);
            mVideoWrapper.decoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            mVideoWrapper.mime,
                            mVideoWrapper.decoderMediaFormat,
                            mVideoWrapper.mSurface);
            mVideoWrapper.render = true;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException e) {
            e.printStackTrace();
            if (mVideoWrapper.decoderMediaCodec != null) {
                mVideoWrapper.decoderMediaCodec.release();
            }
            mVideoWrapper.decoderMediaCodec = null;
        }
        if (mVideoWrapper.decoderMediaCodec == null) {
            mAudioWrapper.isReading = false;
            mVideoWrapper.isReading = false;
            return false;
        }

        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-0");
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
            MLog.d(TAG, "internalPrepare() video csd-0: " + sb.toString());
        }
        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-1");
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
            MLog.d(TAG, "internalPrepare() video csd-1: " + sb.toString());
        }

        // 保存最小的那个时间
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)
                && mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            long audioDurationUs =
                    mAudioWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            long videoDurationUs =
                    mVideoWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            mAudioWrapper.durationUs =
                    audioDurationUs > videoDurationUs ? videoDurationUs : audioDurationUs;
            mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            MLog.d(TAG, "internalPrepare()           durationUs: " + mVideoWrapper.durationUs);
        }

        mThreadHandler.removeMessages(MSG_HANDLE_DATA_LOCK_WAIT);

        String showInfo = null;
        showInfo = "internalPrepare() audio frameMaxLength: " + mAudioWrapper.frameMaxLength;
        MLog.d(TAG, showInfo);
        showInfo = "internalPrepare() video frameMaxLength: " + mVideoWrapper.frameMaxLength;
        MLog.d(TAG, showInfo);

        MLog.w(TAG, "internalPrepare() end");
        return true;
    }

    private void internalStart() {

    }

    private void internalPlay() {
        if (!mAudioWrapper.isPausedForCache
                && !mVideoWrapper.isPausedForCache) {
            mAudioWrapper.isPausedForUser = false;
            mVideoWrapper.isPausedForUser = false;
            notifyToHandleDataLock();
        }
    }

    private void internalPause() {
        if (!mAudioWrapper.isPausedForCache
                && !mVideoWrapper.isPausedForCache) {
            mAudioWrapper.isPausedForUser = true;
            mVideoWrapper.isPausedForUser = true;
        }
    }

    private void internalStop() {

    }

    private void internalRelease() {
        MLog.d(TAG, "internalRelease() start");
        mAudioWrapper.isReading = false;
        mAudioWrapper.isHandling = false;
        mAudioWrapper.isPausedForUser = false;
        mAudioWrapper.isPausedForCache = false;

        mVideoWrapper.isReading = false;
        mVideoWrapper.isHandling = false;
        mVideoWrapper.isPausedForUser = false;
        mVideoWrapper.isPausedForCache = false;
        notifyToReadDataLock();
        notifyToHandleDataLock();
        MLog.d(TAG, "internalRelease() end");
    }

    private void internalDestroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }

        unregisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
    }

    private void internalPrev() {

    }

    private void internalNext() {

    }

    private void notifyToReadDataLock() {
        synchronized (mAudioWrapper.readDataLock) {
            MLog.i(TAG, "notifyToReadDataLock()                audio readDataLock.notify()");
            mAudioWrapper.readDataLock.notify();
        }
        synchronized (mVideoWrapper.readDataLock) {
            MLog.i(TAG, "notifyToReadDataLock()                video readDataLock.notify()");
            mVideoWrapper.readDataLock.notify();
        }
    }

    private void notifyToHandleDataLock() {
        synchronized (mAudioWrapper.handleDataLock) {
            MLog.i(TAG, "notifyToHandleDataLock()              audio handleDataLock.notify()");
            mAudioWrapper.handleDataLock.notify();
        }
        synchronized (mVideoWrapper.handleDataLock) {
            MLog.i(TAG, "notifyToHandleDataLock()              video handleDataLock.notify()");
            mVideoWrapper.handleDataLock.notify();
        }
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

    private void findHead(byte[] data, int offset, int size, List<Integer> list) {
        list.clear();
        for (int i = offset; i < size; ) {
            // 发现帧头
            if (isHead(data, i, size)) {
                list.add(i);
                i += 4;
            } else {
                i++;
            }
        }
    }

    private boolean isHead(byte[] data, int offset, int size) {
        if (offset + 3 >= size) {
            return false;
        }
        if (data[offset] == 88
                && data[offset + 1] == 88
                && data[offset + 2] == 88
                && data[offset + 3] == 88) {
            return true;
        } else if (data[offset] == -88
                && data[offset + 1] == -88
                && data[offset + 2] == -88
                && data[offset + 3] == -88) {
            return true;
        }
        return false;
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
    private long audioTimeDifference = 0;
    private long videoTimeDifference = 0;
    private boolean onlyOneStart = true;

    private boolean feedInputBufferAndDrainOutputBuffer(SimpleWrapper wrapper) {
        if (!wrapper.isHandling) {
            return false;
        }

        // seek的时候就不要往下走了
        if (wrapper.needToSeek) {
            return true;
        }

        // 如果是视频,那么等待音频的开始
        if (wrapper.type == TYPE_VIDEO) {
            while (!mAudioWrapper.isStarted) {
                SystemClock.sleep(10);
            }
            wrapper.isStarted = true;
        }

        // 如果是音频,自己已经开始了,那么等待视频的开始
        if (wrapper.type == TYPE_AUDIO) {
            while (wrapper.isStarted
                    && !mVideoWrapper.isStarted) {
                SystemClock.sleep(10);
            }
        }

        // 音视频都已经开始的话,就可以播放了
        if (mAudioWrapper.isHandling
                && mVideoWrapper.isHandling
                && mAudioWrapper.isStarted
                && mVideoWrapper.isStarted
                && onlyOneStart) {
            onlyOneStart = false;
            if (mCallback != null) {
                MLog.d(TAG, "feedInputBufferAndDrainOutputBuffer() onPlayed");
                mCallback.onPlayed();
            }
        }

        // pause装置
        if (wrapper.isPausedForCache
                || wrapper.isPausedForUser) {
            boolean isPausedForCache = wrapper.isPausedForCache;
            boolean isPausedForUser = wrapper.isPausedForUser;

            String showInfo = null;
            if (wrapper.type == TYPE_AUDIO) {
                showInfo = "feedInputBufferAndDrainOutputBuffer() audio handleDataLock.wait()";
            } else {
                showInfo = "feedInputBufferAndDrainOutputBuffer() video handleDataLock.wait()";
            }
            MLog.d(TAG, showInfo);

            // 如果是缓存pause,那么  需要显示加载框
            // 如果是人为pause,那么不需要显示加载框
            /*if (isPausedForCache) {
                if (mCallback != null) {
                    mCallback.onPaused();
                }
            }*/

            long startTimeMs = System.currentTimeMillis();
            synchronized (wrapper.handleDataLock) {
                try {
                    wrapper.handleDataLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long endTimeMs = System.currentTimeMillis();

            /***
             pause后,在视频播放的时间戳上需要用上这个时间差
             但是pause时间太长的话,现在会出错.
             */
            if (wrapper.type == TYPE_AUDIO) {
                MLog.d(TAG, "audio endTimeMs - startTimeMs: " + (endTimeMs - startTimeMs));
            } else {
                MLog.d(TAG, "video endTimeMs - startTimeMs: " + (endTimeMs - startTimeMs));
            }
            if (isPausedForCache) {
                MediaUtils.paustTimeMs += (endTimeMs - startTimeMs);
            } else if (isPausedForUser) {
                // 使用差值最大的那个
                if (wrapper.type == TYPE_AUDIO) {
                    audioTimeDifference = (endTimeMs - startTimeMs);
                } else {
                    videoTimeDifference = (endTimeMs - startTimeMs);
                }
                if (audioTimeDifference != 0
                        && videoTimeDifference != 0) {
                    MediaUtils.paustTimeMs +=
                            audioTimeDifference >= videoTimeDifference
                                    ? audioTimeDifference : videoTimeDifference;
                    audioTimeDifference = 0;
                    videoTimeDifference = 0;
                }
            }
            if (wrapper.type == TYPE_AUDIO) {
                MLog.d(TAG, "audio  MediaUtils.paustTimeMs: " + MediaUtils.paustTimeMs);
            } else {
                MLog.d(TAG, "video  MediaUtils.paustTimeMs: " + MediaUtils.paustTimeMs);
            }

            /*if (isPausedForCache) {
                if (mCallback != null) {
                    mCallback.onPlayed();
                }
            }*/
        }// pause end

        ++wrapper.handleFrameCounts;
        wrapper.handleFrameLengthTotal += wrapper.frameDataLength;

        // 得到wrapper.presentationTimeUs时间戳
        if ((Long) wrapper.handleFrameCounts != null
                && wrapper.getTime.containsKey((Long) wrapper.handleFrameCounts)) {
            wrapper.presentationTimeUs1 =
                    wrapper.getTime.get((Long) wrapper.handleFrameCounts);
        } else {
            if (wrapper.type == TYPE_AUDIO) {
                MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer() audio handleFrameCounts: " +
                        wrapper.handleFrameCounts + " 没有拿到时间戳");
            } else {
                MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer() video handleFrameCounts: " +
                        wrapper.handleFrameCounts + " 没有拿到时间戳");
            }
        }

        /*String elapsedTime = DateUtils.formatElapsedTime(
                (wrapper.presentationTimeUs1 / 1000) / 1000);
        if (wrapper.type == TYPE_AUDIO) {
            MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer audio   handleFrameCounts: " +
                    wrapper.handleFrameCounts);
            MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer audio  presentationTimeUs1: " +
                    wrapper.presentationTimeUs1 / 1000);
            MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer audio         elapsedTime: " +
                    elapsedTime);
        } else {
            MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer video   handleFrameCounts: " +
                    wrapper.handleFrameCounts);
            MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer video  presentationTimeUs1: " +
                    wrapper.presentationTimeUs1 / 1000);
            MLog.e(TAG, "feedInputBufferAndDrainOutputBuffer video         elapsedTime: " +
                    elapsedTime);
        }*/

        // 过一秒才更新
        if (wrapper instanceof VideoWrapper
                && wrapper.presentationTimeUs1 != -1
                && wrapper.presentationTimeUs1 - wrapper.startTimeUs >= 1000000) {
            // 如果退出,那么seekTo到这个位置就行了
            wrapper.startTimeUs = wrapper.presentationTimeUs1;
            curElapsedTime = DateUtils.formatElapsedTime(
                    (wrapper.presentationTimeUs1 / 1000) / 1000);
            if (mCallback != null
                    // 防止重复更新
                    && !TextUtils.equals(curElapsedTime, preElapsedTime)) {
                preElapsedTime = curElapsedTime;
                mCallback.onProgressUpdated(wrapper.presentationTimeUs1);
            }
        }

        boolean feedInputBufferResult = false;
        boolean drainOutputBufferResult = false;
        try {
            // Input
            feedInputBufferResult = MediaUtils.feedInputBuffer(
                    wrapper.decoderMediaCodec,
                    wrapper.frameData,
                    0,
                    wrapper.frameDataLength,
                    wrapper.presentationTimeUs1);
            // Output
            drainOutputBufferResult = MediaUtils.drainOutputBuffer(
                    wrapper.decoderMediaCodec,
                    wrapper.render,
                    wrapper.type == TYPE_AUDIO ? false : true,
                    wrapper.callback);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return feedInputBufferResult && drainOutputBufferResult;
    }

    private void handleAudioOutputBuffer(
            int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
        // 被使用到video那里进行"睡眠"多少时间的判断
        mAudioWrapper.presentationTimeUs2 = roomInfo.presentationTimeUs;
        while (mAudioWrapper.isHandling
                && mVideoWrapper.isHandling
                && (mAudioWrapper.presentationTimeUs2 / 1000
                > mVideoWrapper.presentationTimeUs2 / 1000)) {
            SystemClock.sleep(1);
        }

        // 输出音频
        if (mAudioWrapper.isHandling
                && mAudioWrapper.mAudioTrack != null
                && mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            room.position(roomInfo.offset);
            room.limit(roomInfo.offset + roomSize);
            byte[] audioData = new byte[roomSize];
            room.get(audioData, 0, audioData.length);
            mAudioWrapper.mAudioTrack.write(audioData, 0, audioData.length);

            /*String elapsedTime = DateUtils.formatElapsedTime(
                    (roomInfo.presentationTimeUs / 1000) / 1000);
            MLog.e(TAG, "mAudioCallback             presentationTimeUs: " +
                    roomInfo.presentationTimeUs / 1000);
            MLog.e(TAG, "mAudioCallback                    elapsedTime: " +
                    elapsedTime);*/
        }
    }

    // 最最最关键的一步
    private void handleVideoOutputBuffer(
            int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
        mVideoWrapper.presentationTimeUs2 = roomInfo.presentationTimeUs;

        /***
         pause,seek等问题都在这里
         */
        if (mProgressUs != -1) {
            if (MediaUtils.COUNT >= 1) {
                MediaUtils.COUNT--;
                MLog.d(TAG, "handleVideoOutputBuffer()               COUNT: " +
                        MediaUtils.COUNT);
            } else {
                mProgressUs = -1;
                MediaUtils.COUNT = 10;
                MediaUtils.progressTimeMs = roomInfo.presentationTimeUs / 1000
                        - System.currentTimeMillis()
                        + MediaUtils.startTimeMs
                        + MediaUtils.paustTimeMs
                        + MediaUtils.variableValues
                        - 100;
                long videoToAudioTimeDifference =
                        roomInfo.presentationTimeUs / 1000
                                - mAudioWrapper.presentationTimeUs2 / 1000;
                MLog.d(TAG, "handleVideoOutputBuffer()      timeDifference: " +
                        videoToAudioTimeDifference);
                MLog.d(TAG, "handleVideoOutputBuffer()      progressTimeMs: " +
                        MediaUtils.progressTimeMs);
            }
            if (MediaUtils.COUNT < 10) {
                return;
            }
        }

        /*String elapsedTime = DateUtils.formatElapsedTime(
                (roomInfo.presentationTimeUs / 1000) / 1000);
        // roomInfo.presentationTimeUs / 1000(ms)
        MLog.d(TAG, "handleVideoOutputBuffer() presentationTimeUs1: " +
                roomInfo.presentationTimeUs / 1000);
        MLog.d(TAG, "handleVideoOutputBuffer()        elapsedTime1: " +
                elapsedTime);
        // timeDifference(ms)
        long timeDifference = System.currentTimeMillis()
                - MediaUtils.startTimeMs
                - MediaUtils.paustTimeMs
                + MediaUtils.progressTimeMs
                - MediaUtils.variableValues;
        elapsedTime = DateUtils.formatElapsedTime(
                timeDifference / 1000);
        MLog.i(TAG, "handleVideoOutputBuffer() presentationTimeUs2: " +
                timeDifference);
        MLog.i(TAG, "handleVideoOutputBuffer()        elapsedTime2: " +
                elapsedTime);*/

        // 不能执行,不然很卡顿
        /*if (roomInfo.presentationTimeUs / 1000
                - mAudioWrapper.presentationTimeUs2 / 1000 < 0) {
            return;
        }*/

        while (mAudioWrapper.isHandling
                && mVideoWrapper.isHandling
                && (mVideoWrapper.presentationTimeUs2 / 1000
                - mAudioWrapper.presentationTimeUs2 / 1000 >= 650)) {
            SystemClock.sleep(1);// 10
        }

        while (roomInfo.presentationTimeUs / 1000
                > System.currentTimeMillis()
                - MediaUtils.startTimeMs
                - MediaUtils.paustTimeMs
                + MediaUtils.progressTimeMs
                - MediaUtils.variableValues) {
            SystemClock.sleep(1);
        }
    }

    private void readData(SimpleWrapper wrapper) {
        if (mCallback != null) {
            MLog.d(TAG, "readData() onReady");
            mCallback.onReady();
        }

        String showInfo = null;
        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "  readData() audio start";
        } else {
            showInfo = "  readData() video start";
        }
        MLog.w(TAG, showInfo);

        preElapsedTime = null;
        curElapsedTime = null;
        /***
         数据先读到room,再从room复制到buffer
         */
        ByteBuffer room = ByteBuffer.allocate(wrapper.frameMaxLength);
        byte[] buffer = new byte[wrapper.frameMaxLength];
        int readData1TotalSize = 0;
        int readData2TotalSize = 0;
        /***
         三种情况退出循环:
         1.异常
         2.要处理的数据有问题时不能继续往下走,然后通知这里结束
         3.readSize < 0
         */
        int readSize = -1;
        boolean needToRead = true;
        /*if (wrapper instanceof VideoWrapper) {
            wrapper.CACHE = 1024 * 1024 * 2;
            wrapper.readData1 = new byte[wrapper.CACHE];
        }*/
        wrapper.extractor.selectTrack(wrapper.trackIndex);
        wrapper.isReading = true;
        while (wrapper.isReading) {
            try {
                // seekTo
                if (wrapper.progressUs != -1) {
                    wrapper.first = FIRST_READ_DATA1;
                    wrapper.readStatus = STATUS_READ_DATA1_STARTED;
                    wrapper.isReadData1Full = false;
                    wrapper.isReadData2Full = false;
                    wrapper.preBufferLastByte = 0;
                    wrapper.readData1Size = 0;
                    wrapper.readData2Size = 0;
                    wrapper.readFrameCounts = 0;
                    wrapper.handleFrameCounts = 0;
                    wrapper.readFrameLengthTotal = 0;
                    wrapper.handleFrameLengthTotal = 0;
                    wrapper.startTimeUs = 0;
                    wrapper.setTime1.clear();
                    wrapper.setTime2.clear();
                    Arrays.fill(wrapper.readData1, (byte) 0);
                    Arrays.fill(wrapper.readData2, (byte) 0);
                    readData1TotalSize = 0;
                    readData2TotalSize = 0;
                    needToRead = true;

                    if (wrapper.progressUs <= wrapper.durationUs) {
                        if (wrapper.type == TYPE_AUDIO) {
                            showInfo = "  readData() audio seekTo: " + wrapper.progressUs;
                        } else {
                            showInfo = "  readData() video seekTo: " + wrapper.progressUs;
                        }
                        MLog.d(TAG, showInfo);
                        wrapper.extractor.seekTo(
                                wrapper.progressUs, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    }
                    wrapper.progressUs = -1;
                }

                if (needToRead) {
                    room.clear();
                    // wrapper.extractor ---> room
                    // readSize为实际读到的大小(音视频一帧的大小),其值可能远远小于room的大小
                    readSize = wrapper.extractor.readSampleData(room, 0);
                    if (readSize < 0) {
                        // 没有数据可读了,结束
                        // 也有可能是底层异常引起
                        // MediaHTTPConnection:
                        // readAt 1033700796 / 32768 => java.net.ProtocolException:
                        // unexpected end of stream
                        if (wrapper.type == TYPE_AUDIO) {
                            showInfo = "  readData() audio readSize: " + readSize;
                        } else {
                            showInfo = "  readData() video readSize: " + readSize;
                        }
                        MLog.e(TAG, showInfo);

                        if (wrapper.readStatus == STATUS_READ_DATA1_STARTED) {
                            wrapper.isReadData1Full = true;
                            if (!wrapper.isReadData2Full) {
                                wrapper.first = FIRST_READ_DATA1;
                            } else {
                                wrapper.first = FIRST_READ_DATA2;
                            }

                            if (wrapper.type == TYPE_AUDIO) {
                                showInfo = "  readData() audio readData1还有 " +
                                        wrapper.readData1Size + " 字节";
                            } else {
                                showInfo = "  readData() video readData1还有 " +
                                        wrapper.readData1Size + " 字节";
                            }
                            MLog.i(TAG, showInfo);
                        } else if (wrapper.readStatus == STATUS_READ_DATA2_STARTED) {
                            wrapper.isReadData2Full = true;
                            if (!wrapper.isReadData1Full) {
                                wrapper.first = FIRST_READ_DATA2;
                            } else {
                                wrapper.first = FIRST_READ_DATA1;
                            }

                            if (wrapper.type == TYPE_AUDIO) {
                                showInfo = "  readData() audio readData2还有 " +
                                        wrapper.readData2Size + " 字节";
                            } else {
                                showInfo = "  readData() video readData2还有 " +
                                        wrapper.readData2Size + " 字节";
                            }
                            MLog.i(TAG, showInfo);
                        }
                        wrapper.isReading = false;
                        wrapper.readStatus = STATUS_READ_FINISHED;

                        wrapper.outputStream.flush();
                        wrapper.outputStream.close();

                        // 开启任务处理数据(如果还没有开启任务过的话)
                        // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                        if (!wrapper.isHandling) {
                            wrapper.isHandling = true;
                            if (wrapper.type == TYPE_AUDIO) {
                                new Thread(mAudioHandleData).start();
                            } else {
                                new Thread(mVideoHandleData).start();
                            }
                        } else {
                            synchronized (wrapper.handleDataLock) {
                                wrapper.handleDataLock.notify();
                            }
                        }
                        break;
                    }// readSize < 0

                    Arrays.fill(buffer, (byte) 0);
                    // room ---> buffer
                    room.get(buffer, 0, readSize);
                    wrapper.readFrameLengthTotal += readSize;

                    if (readSize > wrapper.frameMaxLength) {
                        if (wrapper.type == TYPE_AUDIO) {
                            showInfo = "  readData() audio 出现大体积帧       readSize: " + readSize;
                        } else {
                            showInfo = "  readData() video 出现大体积帧       readSize: " + readSize;
                        }
                        MLog.e(TAG, showInfo);
                    }
                }// needToRead

                switch (wrapper.readStatus) {
                    case STATUS_READ_DATA1_STARTED:
                        /***
                         readData1未满,readData2未满
                         readData1未满,readData2满
                         */
                        needToRead = true;
                        // 实际读取到的大小 + 标志位长度
                        readData1TotalSize += readSize + HEADER_FLAG_LENGTH;
                        if (readData1TotalSize <= wrapper.CACHE) {
                            copyBufferToReadData1(
                                    wrapper,
                                    buffer,
                                    readSize,
                                    wrapper.extractor.getSampleTime());
                        } else {
                            // wrapper.readData1满了
                            wrapper.isReadData1Full = true;
                            if (!wrapper.isHandling) {
                                wrapper.isHandling = true;
                                wrapper.needToSeek = false;
                                if (wrapper.type == TYPE_AUDIO) {
                                    showInfo = "  readData() audio start()";
                                    MLog.i(TAG, showInfo);
                                    new Thread(mAudioHandleData).start();
                                } else {
                                    showInfo = "  readData() video start()";
                                    MLog.i(TAG, showInfo);
                                    new Thread(mVideoHandleData).start();
                                }
                            } else {
                                if (wrapper.progressUs == -1) {
                                    if (wrapper.type == TYPE_AUDIO) {
                                        showInfo = "  readData() audio handleDataLock.notify()";
                                        MLog.i(TAG, showInfo);
                                    } else {
                                        showInfo = "  readData() video handleDataLock.notify()";
                                        MLog.i(TAG, showInfo);
                                    }
                                    synchronized (wrapper.handleDataLock) {
                                        wrapper.handleDataLock.notify();
                                    }
                                }
                            }
                            if (!wrapper.isReadData2Full) {
                                wrapper.first = FIRST_READ_DATA1;
                                wrapper.readStatus = STATUS_READ_DATA2_STARTED;
                            } else {
                                wrapper.first = FIRST_READ_DATA2;
                                wrapper.readStatus = STATUS_READ_DATA_PAUSED;
                            }
                            needToRead = false;
                            readData1TotalSize = 0;
                            /*if (wrapper instanceof VideoWrapper) {
                                wrapper.CACHE = CACHE_VIDEO_HTTP;
                            }*/

                            if (wrapper.type == TYPE_AUDIO) {
                                showInfo = "  readData() audio readData1满了";
                                MLog.i(TAG, showInfo);
                                showInfo = "  readData() audio readData1Size: " +
                                        wrapper.readData1Size;
                                MLog.i(TAG, showInfo);
                            } else {
                                showInfo = "  readData() video readData1满了";
                                MLog.i(TAG, showInfo);
                                showInfo = "  readData() video readData1Size: " +
                                        wrapper.readData1Size;
                                MLog.i(TAG, showInfo);
                            }
                            continue;
                        }
                        break;
                    case STATUS_READ_DATA2_STARTED:
                        /***
                         readData1满,readData2未满
                         */
                        needToRead = true;
                        readData2TotalSize += readSize + HEADER_FLAG_LENGTH;
                        if (readData2TotalSize <= wrapper.CACHE) {
                            copyBufferToReadData2(
                                    wrapper,
                                    buffer,
                                    readSize,
                                    wrapper.extractor.getSampleTime());
                        } else {
                            // wrapper.readData2满了
                            wrapper.isReadData2Full = true;
                            if (wrapper.progressUs == -1) {
                                if (wrapper.type == TYPE_AUDIO) {
                                    showInfo = "  readData() audio handleDataLock.notify()";
                                    MLog.i(TAG, showInfo);
                                } else {
                                    showInfo = "  readData() video handleDataLock.notify()";
                                    MLog.i(TAG, showInfo);
                                }
                                synchronized (wrapper.handleDataLock) {
                                    wrapper.handleDataLock.notify();
                                }
                            }
                            if (!wrapper.isReadData1Full) {
                                wrapper.first = FIRST_READ_DATA2;
                                wrapper.readStatus = STATUS_READ_DATA1_STARTED;
                            } else {
                                wrapper.first = FIRST_READ_DATA1;
                                wrapper.readStatus = STATUS_READ_DATA_PAUSED;
                            }
                            needToRead = false;
                            readData2TotalSize = 0;

                            if (wrapper.type == TYPE_AUDIO) {
                                showInfo = "  readData() audio readData2满了";
                                MLog.i(TAG, showInfo);
                                showInfo = "  readData() audio readData2Size: " +
                                        wrapper.readData2Size;
                                MLog.i(TAG, showInfo);
                            } else {
                                showInfo = "  readData() video readData2满了";
                                MLog.i(TAG, showInfo);
                                showInfo = "  readData() video readData2Size: " +
                                        wrapper.readData2Size;
                                MLog.i(TAG, showInfo);
                            }
                            continue;
                        }
                        break;
                    case STATUS_READ_DATA_PAUSED:
                        /***
                         readData1满,readData2满
                         */
                        needToRead = true;
                        long presentationTimeUs = wrapper.extractor.getSampleTime();
                        if (wrapper.type == TYPE_AUDIO) {
                            showInfo =
                                    "  readData() audio readData1和readData2都满了,好开心( ^_^ )";
                            MLog.i(TAG, showInfo);
                            showInfo = "  readData() audio handleDataLock.notify()";
                            MLog.i(TAG, showInfo);
                        } else {
                            showInfo =
                                    "  readData() video readData1和readData2都满了,好开心( ^_^ )";
                            MLog.i(TAG, showInfo);
                            showInfo = "  readData() video handleDataLock.notify()";
                            MLog.i(TAG, showInfo);
                        }
                        synchronized (wrapper.handleDataLock) {
                            wrapper.handleDataLock.notify();
                        }

                        wrapper.outputStream.flush();

                        System.gc();

                        if (wrapper.progressUs == -1) {
                            // wait
                            synchronized (wrapper.readDataLock) {
                                if (wrapper.type == TYPE_AUDIO) {
                                    showInfo = "  readData() audio readDataLock.wait() start";
                                } else {
                                    showInfo = "  readData() video readDataLock.wait() start";
                                }
                                MLog.w(TAG, showInfo);
                                try {
                                    wrapper.readDataLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (wrapper.type == TYPE_AUDIO) {
                                    showInfo = "  readData() audio readDataLock.wait() end";
                                } else {
                                    showInfo = "  readData() video readDataLock.wait() end";
                                }
                                MLog.w(TAG, showInfo);
                            }
                        }

                        if (wrapper.progressUs == -1) {
                            if (!wrapper.isReadData1Full) {
                                wrapper.setTime1.clear();
                                copyBufferToReadData1(
                                        wrapper, buffer, readSize, presentationTimeUs);

                                readData1TotalSize = wrapper.readData1Size;
                            } else if (!wrapper.isReadData2Full) {
                                wrapper.setTime2.clear();
                                copyBufferToReadData2(
                                        wrapper, buffer, readSize, presentationTimeUs);

                                readData2TotalSize = wrapper.readData2Size;
                            }
                        }
                        break;
                    default:
                        break;
                }// switch end

                if (wrapper.isReading) {
                    // 跳到下一帧
                    wrapper.extractor.advance();
                }
            } catch (Exception e) {
                e.printStackTrace();
                wrapper.isReading = false;
                wrapper.readStatus = STATUS_READ_ERROR;
                break;
            }
        }// while(...) end

        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "  readData() audio end";
        } else {
            showInfo = "  readData() video end";
        }
        MLog.w(TAG, showInfo);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void handleData(SimpleWrapper wrapper) {
        String showInfo = null;
        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "handleData() audio start";
        } else {
            showInfo = "handleData() video start";
        }
        MLog.w(TAG, showInfo);

        copyReadData1ToHandleData(wrapper);
        /*if (wrapper instanceof VideoWrapper) {
            wrapper.readData1 = new byte[CACHE_VIDEO_HTTP];
        }*/

        onlyOneStart = true;
        MediaUtils.startTimeMs = System.currentTimeMillis();
        MediaUtils.paustTimeMs = 0;
        MediaUtils.progressTimeMs = 0;
        MediaUtils.variableValues = 0;
        while (wrapper.isHandling) {
            // 从wrapper.handleData数组中挑选出音视频帧
            findHead(wrapper.handleData, 0, wrapper.readDataSize, wrapper.offsetList);
            wrapper.offsetCounts = wrapper.offsetList.size();
            if (wrapper.type == TYPE_AUDIO) {
                showInfo = "handleData() audio findHead    offsetCounts: " + wrapper.offsetCounts;
            } else {
                showInfo = "handleData() video findHead    offsetCounts: " + wrapper.offsetCounts;
            }
            MLog.i(TAG, showInfo);

            if (wrapper.offsetCounts > 1) {
                wrapper.preReadDataSize = wrapper.readDataSize;
                wrapper.lastOffsetIndex = wrapper.offsetList.get(wrapper.offsetCounts - 1);
                // 实际读取到的数据大小 - 最后一个offsetIndex = 剩余的数据(一帧完整的数据)
                wrapper.restOfDataSize = wrapper.readDataSize - wrapper.lastOffsetIndex;

                /*if (wrapper.type == TYPE_AUDIO) {
                    showInfo = "handleData() audio findHead    readDataSize: " +
                            wrapper.readDataSize;
                } else {
                    showInfo = "handleData() video findHead    readDataSize: " +
                            wrapper.readDataSize;
                }
                MLog.i(TAG, showInfo);
                if (wrapper.type == TYPE_AUDIO) {
                    showInfo = "handleData() audio findHead lastOffsetIndex: " +
                            wrapper.lastOffsetIndex;
                } else {
                    showInfo = "handleData() video findHead lastOffsetIndex: " +
                            wrapper.lastOffsetIndex;
                }
                MLog.i(TAG, showInfo);
                if (wrapper.type == TYPE_AUDIO) {
                    showInfo = "handleData() audio findHead  restOfDataSize: " +
                            wrapper.restOfDataSize;
                } else {
                    showInfo = "handleData() video findHead  restOfDataSize: " +
                            wrapper.restOfDataSize;
                }
                MLog.i(TAG, showInfo);*/
            } else {
                // exit
                wrapper.isHandling = false;

                if (wrapper.type == TYPE_AUDIO) {
                    showInfo = "handleData() audio exit";
                } else {
                    showInfo = "handleData() video exit";
                }
                MLog.e(TAG, showInfo);

                StringBuilder sb = new StringBuilder();
                for (byte bt : wrapper.handleData) {
                    sb.append(" ");
                    sb.append(bt);
                }
                if (wrapper.type == TYPE_AUDIO) {
                    showInfo = "handleData() audio: " + sb.toString();
                } else {
                    showInfo = "handleData() video: " + sb.toString();
                }
                MLog.i(TAG, showInfo);
                break;
            }

            // 发送消息通知读取数据
            if (wrapper.isReading) {
                // 此处发送消息后,readDataSize的大小可能会变化
                synchronized (wrapper.readDataLock) {
                    if (wrapper.type == TYPE_AUDIO) {
                        showInfo = "handleData() audio readDataLock.notify()";
                    } else {
                        showInfo = "handleData() video readDataLock.notify()";
                    }
                    MLog.w(TAG, showInfo);
                    wrapper.readDataLock.notify();
                }
            }

            // 向MediaCodec喂数据
            for (int i = 0; i < wrapper.offsetCounts; i++) {
                if (!wrapper.isHandling) {
                    break;
                }

                Arrays.fill(wrapper.frameData, (byte) 0);
                if (i + 1 < wrapper.offsetCounts
                        && !wrapper.needToSeek) {
                    /***
                     集合中至少有两个offset才有一帧输出
                     各帧之间的offset很重要,比如有:0, 519, 1038, 1585, 2147 ...
                     知道了offset,那么就知道了要"喂"多少数据了.
                     两个offset的位置一减就是一帧的长度
                     因为保存时加入标志位,所以要减去HEADER_FLAG_LENGTH
                     */
                    wrapper.frameDataLength = wrapper.offsetList.get(i + 1)
                            - wrapper.offsetList.get(i)
                            - HEADER_FLAG_LENGTH;
                    System.arraycopy(
                            wrapper.handleData, wrapper.offsetList.get(i) + HEADER_FLAG_LENGTH,
                            wrapper.frameData, 0, wrapper.frameDataLength);

                    if (!feedInputBufferAndDrainOutputBuffer(wrapper)) {
                        wrapper.isHandling = false;
                        break;// for(...) end
                    }

                    // 这个日志是需要的
                    /*if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() offset: " + offsetList.get(i) +
                                "    " + frameData[0] +
                                " " + frameData[1] +
                                " " + frameData[2] +
                                " " + frameData[3] +
                                " " + frameData[4] +
                                " " + frameData[5] +
                                " " + frameData[6]);
                        MLog.d(TAG, "handleData() frameDataLength: " + frameDataLength);
                    }*/
                    continue;
                } else {
                    // 处理集合中最后一个offset的位置
                    // 处理剩余的数据(集合中的最后一帧)
                    if (wrapper.restOfDataSize > 0
                            && !wrapper.needToSeek) {
                        wrapper.frameDataLength = wrapper.restOfDataSize - HEADER_FLAG_LENGTH;
                        System.arraycopy(
                                wrapper.handleData, wrapper.lastOffsetIndex + HEADER_FLAG_LENGTH,
                                wrapper.frameData, 0, wrapper.frameDataLength);

                        if (!feedInputBufferAndDrainOutputBuffer(wrapper)) {
                            wrapper.isHandling = false;
                            break;// for(...) end
                        }
                    }// 处理剩余数据 end

                    if (wrapper.readDataSize != wrapper.CACHE
                            && wrapper.preReadDataSize == wrapper.readDataSize
                            && wrapper.readStatus == STATUS_READ_FINISHED
                            && wrapper.readData1Size == 0
                            && wrapper.readData2Size == 0) {
                        // 没有内容了
                        wrapper.isHandling = false;
                        break;
                    } else {
                        // 两个缓存都未满,那么等待
                        if (!wrapper.isReadData1Full
                                && !wrapper.isReadData2Full
                                || wrapper.needToSeek) {
                            if (!wrapper.needToSeek) {
                                // 自身状态的设置
                                wrapper.isPausedForCache = true;

                                // 如果当前是视频,那么通知音频pause
                                // 如果当前是音频,那么通知视频pause
                                if (wrapper.type == TYPE_AUDIO) {
                                    mVideoWrapper.isPausedForCache = true;
                                } else {
                                    mAudioWrapper.isPausedForCache = true;
                                }
                            }

                            if (!wrapper.needToSeek) {
                                if (wrapper.type == TYPE_AUDIO) {
                                    MLog.e(TAG, "                   卧槽!卧槽!卧槽!audio太不给力了");
                                } else {
                                    MLog.e(TAG, "                   卧槽!卧槽!卧槽!video太不给力了");
                                }
                            } else {
                                if (wrapper.type == TYPE_AUDIO) {
                                    MLog.e(TAG, "             audio handleDataLock.wait()");
                                } else {
                                    MLog.e(TAG, "             video handleDataLock.wait()");
                                }
                            }

                            if (mCallback != null
                                    && mAudioWrapper.isHandling
                                    && mAudioWrapper.isStarted
                                    && mVideoWrapper.isHandling
                                    && mVideoWrapper.isStarted) {
                                if (wrapper.type == TYPE_AUDIO) {
                                    showInfo = "handleData() audio onPaused";
                                    MLog.d(TAG, showInfo);
                                } else {
                                    showInfo = "handleData() video onPaused";
                                    MLog.d(TAG, showInfo);
                                }
                                mCallback.onPaused();
                            }

                            // 如果等待了10分钟后还没有数据,那么可以做一些事
                            mThreadHandler.removeMessages(MSG_HANDLE_DATA_LOCK_WAIT);
                            mThreadHandler.sendEmptyMessageDelayed(
                                    MSG_HANDLE_DATA_LOCK_WAIT, 1000 * 60 * 10);

                            synchronized (wrapper.handleDataLock) {
                                try {
                                    wrapper.handleDataLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            mThreadHandler.removeMessages(MSG_HANDLE_DATA_LOCK_WAIT);

                            if (!wrapper.needToSeek) {
                                wrapper.isPausedForCache = false;

                                // 如果当前是视频,那么通知音频play
                                // 如果当前是音频,那么通知视频play
                                if (wrapper.type == TYPE_AUDIO) {
                                    MLog.e(TAG, "             video handleDataLock.notify()");
                                    mVideoWrapper.isPausedForCache = false;
                                    synchronized (mVideoWrapper.handleDataLock) {
                                        mVideoWrapper.handleDataLock.notify();
                                    }
                                } else {
                                    MLog.e(TAG, "             audio handleDataLock.notify()");
                                    mAudioWrapper.isPausedForCache = false;
                                    synchronized (mAudioWrapper.handleDataLock) {
                                        mAudioWrapper.handleDataLock.notify();
                                    }
                                }
                            }

                            if (mCallback != null
                                    && mAudioWrapper.isHandling
                                    && mAudioWrapper.isStarted
                                    && mVideoWrapper.isHandling
                                    && mVideoWrapper.isStarted) {
                                if (wrapper.type == TYPE_AUDIO) {
                                    showInfo = "handleData() audio onPlayed";
                                    MLog.d(TAG, showInfo);
                                } else {
                                    showInfo = "handleData() video onPlayed";
                                    MLog.d(TAG, showInfo);
                                }
                                mCallback.onPlayed();
                            }
                        }// wait end

                        if (wrapper.needToSeek) {
                            wrapper.needToSeek = false;
                            if (wrapper.type == TYPE_AUDIO) {
                                MLog.e(TAG, "             audio seekTo end handleData开始取数据");
                            } else {
                                MLog.e(TAG, "             video seekTo end handleData开始取数据");
                            }
                        }

                        // 向readData1或者readData2缓存中取数据
                        Arrays.fill(wrapper.handleData, (byte) 0);
                        if (wrapper.isReadData1Full
                                && !wrapper.isReadData2Full) {
                            /***
                             readData1满,readData2未满
                             */
                            copyReadData1ToHandleData(wrapper);
                        } else if (!wrapper.isReadData1Full
                                && wrapper.isReadData2Full) {
                            /***
                             readData1未满,readData2满
                             */
                            copyReadData2ToHandleData(wrapper);
                        } else if (wrapper.isReadData1Full
                                && wrapper.isReadData2Full) {
                            /***
                             readData1满,readData2满
                             */
                            if (wrapper.first == FIRST_READ_DATA1) {
                                copyReadData1ToHandleData(wrapper);
                            } else {
                                copyReadData2ToHandleData(wrapper);
                            }
                        }
                        break;// for(...) end
                    }// 最后一帧处理完后,复制数据 end
                }// 处理最后一帧 end
            }// for(...) end
        }// while(true) end

        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "handleData() audio        wrapper.readFrameCounts: " +
                    wrapper.readFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() audio      wrapper.handleFrameCounts: " +
                    wrapper.handleFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() audio   wrapper.readFrameLengthTotal: " +
                    wrapper.readFrameLengthTotal;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() audio wrapper.handleFrameLengthTotal: " +
                    wrapper.handleFrameLengthTotal;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() audio end";
            MLog.w(TAG, showInfo);
        } else {
            showInfo = "handleData() video        wrapper.readFrameCounts: " +
                    wrapper.readFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() video      wrapper.handleFrameCounts: " +
                    wrapper.handleFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() video   wrapper.readFrameLengthTotal: " +
                    wrapper.readFrameLengthTotal;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() video wrapper.handleFrameLengthTotal: " +
                    wrapper.handleFrameLengthTotal;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() video end";
            MLog.w(TAG, showInfo);
        }

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

        if (!mAudioWrapper.isHandling
                && !mVideoWrapper.isHandling) {
            mAudioWrapper.clear();
            mVideoWrapper.clear();
            if (mCallback != null) {
                MLog.w(TAG, "handleData() onFinished");
                mCallback.onFinished();
            }
        }

        /*mThreadHandler.removeMessages(MSG_GAME_OVER);
        Message msg = mThreadHandler.obtainMessage();
        msg.what = MSG_GAME_OVER;
        msg.obj = wrapper;
        mThreadHandler.sendMessageDelayed(msg, 500);*/
    }

    private void copyBufferToReadData1(
            SimpleWrapper wrapper, byte[] buffer, int size, long presentationTimeUs)
            throws IOException {
        wrapper.readStatus = STATUS_READ_DATA1_STARTED;
        // 先加标志位
        if (wrapper.preBufferLastByte == 0
                || wrapper.preBufferLastByte != 88) {
            System.arraycopy(HEADER_FLAG1, 0,
                    wrapper.readData1, wrapper.readData1Size, HEADER_FLAG_LENGTH);
            wrapper.outputStream.write(
                    HEADER_FLAG1, 0, HEADER_FLAG_LENGTH);
        } else {
            System.arraycopy(HEADER_FLAG2, 0,
                    wrapper.readData1, wrapper.readData1Size, HEADER_FLAG_LENGTH);
            wrapper.outputStream.write(
                    HEADER_FLAG2, 0, HEADER_FLAG_LENGTH);
        }
        wrapper.outputStream.write(buffer, 0, size);
        wrapper.preBufferLastByte = buffer[size - 1];

        // buffer ---> readData1
        System.arraycopy(buffer, 0,
                wrapper.readData1, wrapper.readData1Size + HEADER_FLAG_LENGTH, size);
        wrapper.readData1Size += size + HEADER_FLAG_LENGTH;
        ++wrapper.readFrameCounts;

        // 一帧对应一个时间戳
        wrapper.setTime1.put(
                wrapper.readFrameCounts, presentationTimeUs);

        /*String elapsedTime = DateUtils.formatElapsedTime(
                (presentationTimeUs1 / 1000) / 1000);
        MLog.i(TAG, "mVideoCallback      readData1Size1: " + wrapper.readData1Size);
        MLog.i(TAG, "mVideoCallback    readFrameCounts1: " + wrapper.readFrameCounts);
        MLog.i(TAG, "mVideoCallback presentationTimeUs1: " + presentationTimeUs1 / 1000);
        MLog.i(TAG, "mVideoCallback        elapsedTime1: " + elapsedTime);*/
    }

    private void copyBufferToReadData2(
            SimpleWrapper wrapper, byte[] buffer, int size, long presentationTimeUs)
            throws IOException {
        wrapper.readStatus = STATUS_READ_DATA2_STARTED;
        // 先加标志位
        if (wrapper.preBufferLastByte == 0
                || wrapper.preBufferLastByte != 88) {
            System.arraycopy(HEADER_FLAG1, 0,
                    wrapper.readData2, wrapper.readData2Size, HEADER_FLAG_LENGTH);
            wrapper.outputStream.write(
                    HEADER_FLAG1, 0, HEADER_FLAG_LENGTH);
        } else {
            System.arraycopy(HEADER_FLAG2, 0,
                    wrapper.readData2, wrapper.readData2Size, HEADER_FLAG_LENGTH);
            wrapper.outputStream.write(
                    HEADER_FLAG2, 0, HEADER_FLAG_LENGTH);
        }
        wrapper.outputStream.write(buffer, 0, size);
        wrapper.preBufferLastByte = buffer[size - 1];

        System.arraycopy(buffer, 0,
                wrapper.readData2, wrapper.readData2Size + HEADER_FLAG_LENGTH, size);
        wrapper.readData2Size += size + HEADER_FLAG_LENGTH;
        ++wrapper.readFrameCounts;

        // 一帧对应一个时间戳
        wrapper.setTime2.put(
                wrapper.readFrameCounts, presentationTimeUs);

        /*String elapsedTime = DateUtils.formatElapsedTime(
                (presentationTimeUs1 / 1000) / 1000);
        MLog.i(TAG, "mVideoCallback      readData2Size2: " + wrapper.readData2Size);
        MLog.i(TAG, "mVideoCallback    readFrameCounts2: " + wrapper.readFrameCounts);
        MLog.i(TAG, "mVideoCallback presentationTimeUs2: " + presentationTimeUs1 / 1000);
        MLog.i(TAG, "mVideoCallback        elapsedTime2: " + elapsedTime);*/
    }

    private void copyReadData1ToHandleData(SimpleWrapper wrapper) {
        wrapper.readDataSize = wrapper.readData1Size;
        System.arraycopy(
                wrapper.readData1, 0,
                wrapper.handleData, 0, wrapper.readDataSize);
        Arrays.fill(wrapper.readData1, (byte) 0);
        // 必须要设置为0
        wrapper.readData1Size = 0;
        wrapper.isReadData1Full = false;

        wrapper.getTime.clear();
        wrapper.getTime.putAll(wrapper.setTime1);

        String showInfo = null;
        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "handleData() audio readData1空了";
            MLog.i(TAG, showInfo);
            showInfo = "handleData() audio 正在处理readData1数据...";
            MLog.i(TAG, showInfo);
        } else {
            showInfo = "handleData() video readData1空了";
            MLog.i(TAG, showInfo);
            showInfo = "handleData() video 正在处理readData1数据...";
            MLog.i(TAG, showInfo);
        }
    }

    private void copyReadData2ToHandleData(SimpleWrapper wrapper) {
        wrapper.readDataSize = wrapper.readData2Size;
        System.arraycopy(
                wrapper.readData2, 0,
                wrapper.handleData, 0, wrapper.readDataSize);
        Arrays.fill(wrapper.readData2, (byte) 0);
        wrapper.readData2Size = 0;
        wrapper.isReadData2Full = false;

        wrapper.getTime.clear();
        wrapper.getTime.putAll(wrapper.setTime2);

        String showInfo = null;
        if (wrapper.type == TYPE_AUDIO) {
            showInfo = "handleData() audio readData2空了";
            MLog.i(TAG, showInfo);
            showInfo = "handleData() audio 正在处理readData2数据...";
            MLog.i(TAG, showInfo);
        } else {
            showInfo = "handleData() video readData2空了";
            MLog.i(TAG, showInfo);
            showInfo = "handleData() video 正在处理readData2数据...";
            MLog.i(TAG, showInfo);
        }
    }

    private Runnable mAudioHandleData = new Runnable() {
        @Override
        public void run() {
            handleData(mAudioWrapper);
        }
    };

    private Runnable mVideoHandleData = new Runnable() {
        @Override
        public void run() {
            handleData(mVideoWrapper);
        }
    };

    private Runnable mAudioReadData = new Runnable() {
        @Override
        public void run() {
            readData(mAudioWrapper);
        }
    };

    private Runnable mVideoReadData = new Runnable() {
        @Override
        public void run() {
            readData(mVideoWrapper);
        }
    };

    private MediaUtils.Callback mAudioCallback = new MediaUtils.Callback() {
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onFormatChanged(MediaFormat newMediaFormat) {
            mAudioWrapper.decoderMediaFormat = newMediaFormat;
            MLog.d(TAG, "mAudioCallback decoderMediaFormat: " + newMediaFormat);

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
                audioFormat =
                        mAudioWrapper.decoderMediaFormat.getInteger(
                                MediaFormat.KEY_PCM_ENCODING);
            }
            mAudioWrapper.mAudioTrack = MediaUtils.createAudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRateInHz, channelCount, audioFormat,
                    AudioTrack.MODE_STREAM);
            if (mAudioWrapper.mAudioTrack != null) {
                setVolume();
                mAudioWrapper.mAudioTrack.play();
                mAudioWrapper.isStarted = true;
                MLog.d(TAG, "mAudioCallback mAudioWrapper.mAudioTrack.play() 声音马上输出");
            }
        }

        @Override
        public void onInputBuffer(
                int roomIndex, ByteBuffer room, MediaUtils.InputBufferInfo info) {
            // 不在这里输入
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onOutputBuffer(
                int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
            handleAudioOutputBuffer(roomIndex, room, roomInfo, roomSize);
        }
    };

    private MediaUtils.Callback mVideoCallback = new MediaUtils.Callback() {
        @Override
        public void onFormatChanged(MediaFormat newMediaFormat) {
            mVideoWrapper.decoderMediaFormat = newMediaFormat;
            MLog.d(TAG, "mVideoCallback decoderMediaFormat: " + newMediaFormat);
        }

        @Override
        public void onInputBuffer(
                int roomIndex, ByteBuffer room, MediaUtils.InputBufferInfo info) {
            // 不在这里输入
        }

        @Override
        public void onOutputBuffer(
                int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
            // 最最最关键的一步
            handleVideoOutputBuffer(roomIndex, room, roomInfo, roomSize);

            // 不要删除
            /*boolean sync = (roomInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
            MLog.d(TAG, "mVideoCallback sync: " + sync);
            if (!sync) {
                byte[] decodedData = new byte[roomSize];
                // 同步帧,填充着sps和pps参数
                // 0 0 0 1 103 66 -128 40 -23 1 104 20 50 0 0 0 1 104 -18 6 -14
                StringBuilder sb = new StringBuilder();
                for (byte bt : decodedData) {
                    sb.append(" ");
                    sb.append(bt);
                }
                MLog.i(TAG, "mVideoCallback video: " + sb.toString());
            }*/
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
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset not connected");
                        //pause();
                        break;
                    case 1:
                        if (DEBUG)
                            MLog.d(TAG, "HeadsetPlugReceiver headset has connected");
                        //play();
                        break;
                    default:
                }
            }
        }
    }

}
