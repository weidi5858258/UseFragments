package com.weidi.usefragments.business.video_player;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;
import com.weidi.usefragments.tool.AACPlayer;
import com.weidi.usefragments.tool.MLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.weidi.usefragments.business.video_player.FFMPEG.USE_MODE_ONLY_VIDEO;
import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_IS_MUTE;
import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

/***
 Created by weidi on 2019/8/6.

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
 6.prev or nextHandle
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

public class SimpleVideoPlayer {

    private static final String TAG =
            "player_alexander";
    //SimpleVideoPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final int MAX_CACHE_AUDIO_COUNT = 600;// 5000
    public static final int MAX_CACHE_VIDEO_COUNT = 60;

    private static final int START_CACHE_COUNT_HTTP = 30;// 3000
    private static final int START_CACHE_COUNT_LOCAL = 30;

    // 只是设置为默认值
    // 音频一帧的大小不能超过这个值,不然出错(如果设成1024 * 1024会有杂音,不能过大,调查了好久才发现跟这个有关)
    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;// 1024 * 100
    // 视频一帧的大小不能超过这个值,不然出错
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024;

    public static final int TYPE_AUDIO = 0x0001;
    public static final int TYPE_VIDEO = 0x0002;
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
    private Context mContext = null;
    private String mPath = null;
    //private float mVolume = 1.0f;
    private boolean mIsLocal = false;
    private MediaExtractor extractor = null;

    private Handler mUiHandler = null;
    private Handler mThreadHandler = null;
    private HandlerThread mHandlerThread = null;

    private com.weidi.usefragments.tool.Callback mCallback = null;
    private Surface mSurface = null;

    private boolean isReading = false;
    private Object readDataLock = new Object();
    //private Object handleDataLock = new Object();
    public AudioWrapper mAudioWrapper;
    public VideoWrapper mVideoWrapper;
    private int use_mode = FFMPEG.USE_MODE_MEDIA;

    private static class SimpleWrapper {
        public String mime = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        // 是否需要渲染图像(播放音频为false,播放视频为true)
        public boolean render = false;
        public int trackIndex = -1;
        // 使用于while条件判断
        public boolean isHandling = false;
        // readData用于缓存数据,handleData用于处理数据
        public ArrayList<AVPacket> readData = null;

        public int CACHE;
        public int CACHE_START;
        // 用于标识音频还是视频
        public int type;
        // 总时长
        public long durationUs = 0;
        // 播放的时长(下面两个参数一起做的事是每隔一秒发一次回调函数)
        public long sampleTime = 0;
        public long startTimeUs = 0;

        // 使用于时间戳
        public long presentationTimeUs = 0;

        //        // 下面4个变量在没有seek过的情况下统计是正确的,seek过后就不正确了
        //        // "块"的概念,一"块"有N个字节.
        //        // 编码时接收的数据就是一"块"的大小,少一个字节或者多一个字节都会出现异常
        //        public long readFrameCounts = 0;
        //        // 处理了多少"块"
        //        public long handleFrameCounts = 0;
        //        // 使用readSampleData(...)方法总共读了多少字节
        //        public long readFrameLengthTotal = 0;
        //        // 处理了多少字节
        //        public long handleFrameLengthTotal = 0;


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
        public int size = 0;
        // 放一帧音频或者视频的容器
        public byte[] data = null;

        // seekTo
        public long progressUs = -1;
        public boolean needToSeek = false;

        private SimpleWrapper() {
        }

        public SimpleWrapper(int type) {
            switch (type) {
                case TYPE_AUDIO:
                    this.type = TYPE_AUDIO;
                    CACHE = MAX_CACHE_AUDIO_COUNT;
                    frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
                    break;
                case TYPE_VIDEO:
                    this.type = TYPE_VIDEO;
                    CACHE = MAX_CACHE_VIDEO_COUNT;
                    frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
                    break;
                default:
                    break;
            }
        }

        public void clear() {
            mime = null;
            decoderMediaCodec = null;
            decoderMediaFormat = null;
            render = false;
            trackIndex = -1;
            //isReading = false;
            isStarted = false;
            isHandling = false;
            isPausedForUser = false;
            isPausedForCache = false;
            isPausedForSeek = false;
            //readData.clear();
            //handleData.clear();
            durationUs = 0;
            sampleTime = 0;
            startTimeUs = 0;
            presentationTimeUs = 0;
            //frameMaxLength = 0;
            size = 0;

            /*progressUs = -1;
            needToSeek = false;*/
        }
    }

    private static class AudioWrapper extends SimpleWrapper {
        public AudioTrack mAudioTrack = null;

        private AudioWrapper() {
        }

        public AudioWrapper(int type) {
            super(type);
        }

        public void clear() {
            MediaUtils.releaseAudioTrack(mAudioTrack);
            MediaUtils.releaseMediaCodec(decoderMediaCodec);
            super.clear();
        }
    }

    private static class VideoWrapper extends SimpleWrapper {
        public Surface mSurface = null;
        public int width = 0;
        public int height = 0;

        private VideoWrapper() {
        }

        public VideoWrapper(int type) {
            super(type);
        }

        public void clear() {
            MediaUtils.releaseMediaCodec(decoderMediaCodec);
            super.clear();
        }
    }

    public SimpleVideoPlayer() {
        // init();
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setDataSource(String path) {
        mPath = path;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setCallback(com.weidi.usefragments.tool.Callback callback) {
        mCallback = callback;
    }

    public void setVolume(float volume) {
        if (mAudioWrapper.mAudioTrack == null
                || mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return;
        }
        if (volume < 0 || volume > 1.0f) {
            volume = FFMPEG.VOLUME_NORMAL;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioWrapper.mAudioTrack.setVolume(volume);
        } else {
            mAudioWrapper.mAudioTrack.setStereoVolume(volume, volume);
        }
    }

    public long mProgressUs = -1;

    // 返回"微秒"
    public long getCurrentPosition() {
        return mAudioWrapper.presentationTimeUs;
    }

    // 95160000
    public void setProgressUs(long progressUs) {
        MLog.i(TAG, "setProgressUs() start");
        MLog.i(TAG, "----------------------------------------------------------");
        String elapsedTime = DateUtils.formatElapsedTime(progressUs / 1000 / 1000);
        MLog.i(TAG, "setProgressUs() progressUs: " + progressUs + " " + elapsedTime);
        MLog.i(TAG, "----------------------------------------------------------");
        if (progressUs < 0
                || progressUs > mAudioWrapper.durationUs
                || progressUs > mVideoWrapper.durationUs) {
            MLog.i(TAG, "setProgressUs() return");
            return;
        }

        mAudioWrapper.isPausedForSeek = true;
        mVideoWrapper.isPausedForSeek = true;
        notifyToHandle(mAudioWrapper);
        notifyToHandle(mVideoWrapper);

        mIsSeeked = true;
        mProgressUs = progressUs;
        mAudioWrapper.progressUs = progressUs;
        mVideoWrapper.progressUs = progressUs;
        notifyToRead();
        notifyToRead(mAudioWrapper);
        notifyToRead(mVideoWrapper);
        MLog.i(TAG, "setProgressUs() end");
    }

    private void seekTo() {
        MLog.i(TAG, "seekTo()");
        if (mAudioWrapper.isStarted && mVideoWrapper.isStarted) {
            while (!mAudioWrapper.needToSeek || !mVideoWrapper.needToSeek) {
                SystemClock.sleep(1);
            }
        }
        MLog.i(TAG, "seekTo() start");
        /*int index = -1;
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
                wrapper.handleData.addAll(wrapper.readData.subList(index, size - 1));
                wrapper.readData.clear();
                notifyToHandle(wrapper);
            }
        } else {
            wrapper.readData.clear();
            // SEEK_TO_PREVIOUS_SYNC SEEK_TO_CLOSEST_SYNC SEEK_TO_NEXT_SYNC
            wrapper.extractor.seekTo(
                    wrapper.progressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        }*/

        synchronized (mAudioWrapper.readDataLock) {
            mAudioWrapper.readData.clear();
        }
        synchronized (mVideoWrapper.readDataLock) {
            mVideoWrapper.readData.clear();
        }
        // SEEK_TO_PREVIOUS_SYNC SEEK_TO_CLOSEST_SYNC SEEK_TO_NEXT_SYNC
        extractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        mAudioWrapper.needToSeek = false;
        mVideoWrapper.needToSeek = false;
        mAudioWrapper.isPausedForSeek = false;
        mVideoWrapper.isPausedForSeek = false;
        mAudioWrapper.presentationTimeUs = 0;
        mVideoWrapper.presentationTimeUs = 0;
        preElapsedTime = null;

        MLog.i(TAG, "seekTo() end");
    }

    public long getDurationUs() {
        return mAudioWrapper.durationUs;
    }

    public void play() {
        internalPlay();
        /*if (!isRunning()) {
            mThreadHandler.removeMessages(MSG_PREPARE);
            mThreadHandler.sendEmptyMessage(MSG_PREPARE);
        } else {
            mThreadHandler.removeMessages(MSG_PLAY);
            mThreadHandler.sendEmptyMessage(MSG_PLAY);
        }*/
    }

    public void pause() {
        internalPause();
        //mThreadHandler.removeMessages(MSG_PAUSE);
        //mThreadHandler.sendEmptyMessage(MSG_PAUSE);
    }

    public void release() {
        internalRelease();
        //mThreadHandler.removeMessages(MSG_RELEASE);
        //mThreadHandler.sendEmptyMessage(MSG_RELEASE);
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
        return mAudioWrapper.isHandling
                || mVideoWrapper.isHandling;
    }

    public boolean initPlayer() {
        return internalPrepare() && prepareAudio() && prepareVideo();
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

                if (internalPrepare()) {
                    if (prepareAudio() && prepareVideo()) {
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                handleData(mAudioWrapper);
                            }
                        });
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                handleData(mVideoWrapper);
                            }
                        });
                        SystemClock.sleep(500);
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                readData();
                            }
                        });
                    } else {
                        if (mCallback != null) {
                            MLog.e(TAG, "onError prepareAudio() || prepareVideo()");
                            mCallback.onError(0, null);
                        }
                    }
                } else {
                    if (mCallback != null) {
                        MLog.e(TAG, "onError internalPrepare()");
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
            case MSG_RELEASE:
                internalRelease();
                //internalDestroy();
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
                    SimpleWrapper wrapper = (SimpleWrapper) msg.obj;

                    if (wrapper instanceof AudioWrapper
                            && mAudioWrapper.mAudioTrack != null) {
                        mAudioWrapper.mAudioTrack.release();
                    }

                    if (wrapper.decoderMediaCodec != null) {
                        wrapper.decoderMediaCodec.release();
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
                    if (mProgressUs == -1) {
                        step = (int) ((mVideoWrapper.presentationTimeUs
                                / (mVideoWrapper.durationUs * 1.00)) * 3840.00);
                        Log.d(TAG, "onKeuiHandleMessageyDown() step: " + step);
                    }
                    step += 100;
                    long progress = (long) (((step / 3840.00) * mVideoWrapper.durationUs));
                    setProgressUs(progress);
                } else {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 1");
                    if (!mAudioWrapper.isPausedForCache
                            && !mVideoWrapper.isPausedForCache) {
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

    // 读到的都是压缩数据
    public void readData() {
        if (mAudioWrapper.trackIndex != -1 && mVideoWrapper.trackIndex != -1) {
            use_mode = FFMPEG.USE_MODE_MEDIA;
            mAudioWrapper.readData = new ArrayList<AVPacket>();
            mVideoWrapper.readData = new ArrayList<AVPacket>(mVideoWrapper.CACHE);
            mVideoWrapper.width =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            mVideoWrapper.height =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            if (mCallback != null) {
                mCallback.onChangeWindow(mVideoWrapper.width, mVideoWrapper.height);
            }
        } else if (mAudioWrapper.trackIndex != -1 && mVideoWrapper.trackIndex == -1) {
            use_mode = FFMPEG.USE_MODE_ONLY_AUDIO;
            mAudioWrapper.readData = new ArrayList<AVPacket>(mAudioWrapper.CACHE);
            if (mCallback != null) {
                mCallback.onChangeWindow(0, 0);
            }
        } else if (mAudioWrapper.trackIndex == -1 && mVideoWrapper.trackIndex != -1) {
            use_mode = FFMPEG.USE_MODE_ONLY_VIDEO;
            mVideoWrapper.readData = new ArrayList<AVPacket>(mVideoWrapper.CACHE);
            mVideoWrapper.width =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            mVideoWrapper.height =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            if (mCallback != null) {
                mCallback.onChangeWindow(mVideoWrapper.width, mVideoWrapper.height);
            }
        }
        MLog.i(TAG, "readData()" +
                " mVideoWrapper.trackIndex: " + mVideoWrapper.trackIndex +
                " mAudioWrapper.trackIndex: " + mAudioWrapper.trackIndex);

        MLog.i(TAG, "readData() start");
        //setProgressUs(721057837);
        int readSize = -1;
        int trackIndex = -1;
        ByteBuffer room = ByteBuffer.allocate(mVideoWrapper.frameMaxLength);
        //byte[] audioBuffer = new byte[mAudioWrapper.frameMaxLength];
        //byte[] videoBuffer = new byte[mVideoWrapper.frameMaxLength];

        boolean audioStarted = false;
        boolean videoStarted = false;
        int audioSize = 0;
        int videoSize = 0;
        AVPacket data = null;
        while (isReading) {
            try {
                // seekTo
                if (mAudioWrapper.isPausedForSeek
                        || mVideoWrapper.isPausedForSeek) {
                    seekTo();
                    audioStarted = false;
                    videoStarted = false;
                }

                room.clear();
                readSize = extractor.readSampleData(room, 0);
                trackIndex = extractor.getSampleTrackIndex();

                // region readSize < 0

                if (readSize < 0) {
                    // 没有数据可读了,结束
                    // 也有可能是底层异常引起
                    // MediaHTTPConnection:
                    // readAt 1033700796 / 32768 => java.net.ProtocolException:
                    // unexpected end of stream
                    MLog.e(TAG, "readData() end      readSize: " + readSize);
                    MLog.e(TAG, "readData() end audioDataSize: " + mAudioWrapper.readData.size());
                    MLog.e(TAG, "readData() end videoDataSize: " + mVideoWrapper.readData.size());

                    isReading = false;
                    // 开启任务处理数据(如果还没有开启任务过的话)
                    // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                    if (!mAudioWrapper.isPausedForUser && !mVideoWrapper.isPausedForUser) {
                        notifyToHandle(mAudioWrapper);
                        notifyToHandle(mVideoWrapper);
                    }

                    MLog.e(TAG, "readData() notifyToReadWait start");
                    // 需要seek,不能退出
                    notifyToReadWait();
                    MLog.e(TAG, "readData() notifyToReadWait end");
                    if (mAudioWrapper.isPausedForSeek
                            || mVideoWrapper.isPausedForSeek) {
                        isReading = true;
                        continue;
                    } else {
                        break;
                    }
                }

                // endregion

                data = new AVPacket(readSize, extractor.getSampleTime());
                room.get(data.data, 0, readSize);

                switch (use_mode) {
                    case FFMPEG.USE_MODE_MEDIA:
                        if (trackIndex == mAudioWrapper.trackIndex) {
                            synchronized (mAudioWrapper.readDataLock) {
                                mAudioWrapper.readData.add(data);
                                audioSize = mAudioWrapper.readData.size();
                            }

                            if (!audioStarted && audioSize == mAudioWrapper.CACHE_START) {
                                MLog.i(TAG, "readData() audio notifyToHandle");
                                audioStarted = true;
                                notifyToHandle(mAudioWrapper);
                            }
                        } else if (trackIndex == mVideoWrapper.trackIndex) {
                            synchronized (mVideoWrapper.readDataLock) {
                                mVideoWrapper.readData.add(data);
                                videoSize = mVideoWrapper.readData.size();
                            }

                            if (videoStarted && videoSize >= mVideoWrapper.CACHE) {
                                //MLog.i(TAG, "readData() notifyToReadWait start");
                                System.gc();
                                notifyToReadWait(mVideoWrapper);
                                if (mAudioWrapper.isPausedForSeek
                                        || mVideoWrapper.isPausedForSeek) {
                                    MLog.i(TAG, "readData() notifyToReadWait end for Seek");
                                    continue;
                                }
                                //MLog.i(TAG, "readData() notifyToReadWait end");
                            } else if (!videoStarted && videoSize == mVideoWrapper.CACHE_START) {
                                MLog.i(TAG, "readData() video notifyToHandle");
                                videoStarted = true;
                                notifyToHandle(mVideoWrapper);
                            }
                        }
                        break;
                    case FFMPEG.USE_MODE_ONLY_AUDIO:
                        if (trackIndex == mAudioWrapper.trackIndex) {
                            synchronized (mAudioWrapper.readDataLock) {
                                mAudioWrapper.readData.add(data);
                                audioSize = mAudioWrapper.readData.size();
                            }

                            if (audioStarted && audioSize >= mAudioWrapper.CACHE) {
                                System.gc();
                                notifyToReadWait(mAudioWrapper);
                                if (mAudioWrapper.isPausedForSeek) {
                                    continue;
                                }
                            } else if (!audioStarted && audioSize == mAudioWrapper.CACHE_START) {
                                MLog.i(TAG, "readData() audio notifyToHandle");
                                audioStarted = true;
                                notifyToHandle(mAudioWrapper);
                            }
                        }
                        break;
                    case FFMPEG.USE_MODE_ONLY_VIDEO:
                        if (trackIndex == mVideoWrapper.trackIndex) {
                            synchronized (mVideoWrapper.readDataLock) {
                                mVideoWrapper.readData.add(data);
                                videoSize = mVideoWrapper.readData.size();
                            }

                            if (videoStarted && videoSize >= mVideoWrapper.CACHE) {
                                System.gc();
                                notifyToReadWait(mVideoWrapper);
                                if (mVideoWrapper.isPausedForSeek) {
                                    continue;
                                }
                            } else if (!videoStarted && videoSize == mVideoWrapper.CACHE_START) {
                                MLog.i(TAG, "readData() video notifyToHandle");
                                videoStarted = true;
                                notifyToHandle(mVideoWrapper);
                            }
                        }
                        break;
                    default:
                        break;
                }


                // Test
                /*if (trackIndex == mAudioWrapper.trackIndex) {
                    mAudioWrapper.data = data.data;
                    mAudioWrapper.size = data.size;
                    mAudioWrapper.sampleTime = data.sampleTime;
                    if (!feedInputBufferAndDrainOutputBuffer(mAudioWrapper)) {
                        mAudioWrapper.isHandling = false;
                        break;
                    }
                } else if (trackIndex == mVideoWrapper.trackIndex) {
                    mVideoWrapper.data = data.data;
                    mVideoWrapper.size = data.size;
                    mVideoWrapper.sampleTime = data.sampleTime;
                    if (!feedInputBufferAndDrainOutputBuffer(mVideoWrapper)) {
                        mVideoWrapper.isHandling = false;
                        break;
                    }
                }*/

                // 跳到下一帧
                extractor.advance();
            } catch (Exception e) {
                e.printStackTrace();
                isReading = false;
                break;
            }
        }// while(...) end

        if (extractor != null) {
            extractor.release();
            extractor = null;
        }

        MLog.i(TAG, "readData() end");
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void handleData(SimpleWrapper wrapper) {
        switch (use_mode) {
            case FFMPEG.USE_MODE_MEDIA:
                if (wrapper.type == TYPE_AUDIO) {
                    mAudioWrapper.isHandling = true;
                } else {
                    mVideoWrapper.isHandling = true;
                }
                break;
            case FFMPEG.USE_MODE_ONLY_AUDIO:
                if (wrapper.type == TYPE_AUDIO) {
                    mAudioWrapper.isHandling = true;
                } else {
                    mVideoWrapper.isHandling = false;
                }
                break;
            case FFMPEG.USE_MODE_ONLY_VIDEO:
                if (wrapper.type == TYPE_AUDIO) {
                    mAudioWrapper.isHandling = false;
                } else {
                    mVideoWrapper.isHandling = true;
                }
                break;
            default:
                break;
        }

        if (wrapper.isHandling) {
            if (wrapper.type == TYPE_AUDIO) {
                MLog.d(TAG, "handleData() audio notifyToHandleWait start");
            } else {
                MLog.w(TAG, "handleData() video notifyToHandleWait start");
            }
            notifyToHandleWait(wrapper);
            if (wrapper.type == TYPE_AUDIO) {
                MLog.d(TAG, "handleData() audio notifyToHandleWait end");
            } else {
                MLog.w(TAG, "handleData() video notifyToHandleWait end");
            }
            if (!wrapper.isHandling) {
                return;
            }
        }

        AVPacket data = null;
        int readDataSize = 0;
        if (wrapper.type == TYPE_AUDIO) {
            MLog.d(TAG, "handleData() audio start");
        } else {
            MLog.w(TAG, "handleData() video start");
        }
        while (wrapper.isHandling) {
            // region 暂停装置

            if (wrapper.isPausedForUser
                    || wrapper.isPausedForCache
                    || wrapper.isPausedForSeek) {
                boolean isPausedForUser = wrapper.isPausedForUser;
                boolean isPausedForSeek = wrapper.isPausedForSeek;

                if (isPausedForSeek) {
                    wrapper.needToSeek = true;
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() audio Seek notifyToHandleWait start");
                    } else {
                        MLog.w(TAG, "handleData() video Seek notifyToHandleWait start");
                    }
                } else if (isPausedForUser) {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() audio User notifyToHandleWait start");
                    } else {
                        MLog.w(TAG, "handleData() video User notifyToHandleWait start");
                    }
                } else {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() audio Cache notifyToHandleWait start");
                    } else {
                        MLog.w(TAG, "handleData() video Cache notifyToHandleWait start");
                    }
                }

                notifyToHandleWait(wrapper);

                if (wrapper.isPausedForSeek || wrapper.isPausedForUser) {
                    continue;
                }

                if (isPausedForSeek) {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() audio Seek notifyToHandleWait end");
                        if (mCallback != null) {
                            mCallback.onPlayed();
                        }
                    } else {
                        MLog.w(TAG, "handleData() video Seek notifyToHandleWait end");
                    }
                } else if (isPausedForUser) {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() audio User notifyToHandleWait end");
                    } else {
                        MLog.w(TAG, "handleData() video User notifyToHandleWait end");
                    }
                } else {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() audio Cache notifyToHandleWait end");
                    } else {
                        MLog.w(TAG, "handleData() video Cache notifyToHandleWait end");
                    }
                }
            }

            // endregion

            // 取得一个AVPacket
            synchronized (wrapper.readDataLock) {
                if (!wrapper.readData.isEmpty()) {
                    data = wrapper.readData.get(0);
                    wrapper.readData.remove(0);
                }
                readDataSize = wrapper.readData.size();
            }

            if (wrapper.type == TYPE_AUDIO) {
                //MLog.d(TAG, "handleData() audio readDataSize: " + readDataSize);
                if (use_mode == FFMPEG.USE_MODE_ONLY_AUDIO) {
                    if ((mIsLocal && (readDataSize % 30 == 0))
                            || (!mIsLocal && readDataSize < mAudioWrapper.CACHE)) {
                        notifyToRead(wrapper);
                    }
                }
            } else {
                //MLog.w(TAG, "handleData() video readDataSize: " + readDataSize);
                if (use_mode == FFMPEG.USE_MODE_MEDIA || use_mode == USE_MODE_ONLY_VIDEO) {
                    if ((mIsLocal && (readDataSize % 30 == 0))
                            || (!mIsLocal && readDataSize < mVideoWrapper.CACHE)) {
                        notifyToRead(wrapper);
                    }
                }
            }

            if (readDataSize > 0) {
                wrapper.data = data.data;
                wrapper.size = data.size;
                wrapper.sampleTime = data.sampleTime;

                // 向MediaCodec喂数据
                if (!feedInputBufferAndDrainOutputBuffer(wrapper)) {
                    wrapper.isHandling = false;
                    break;
                }

                data.clear();

                if (wrapper.type == TYPE_AUDIO) {
                    curElapsedTime = DateUtils.formatElapsedTime(
                            (wrapper.sampleTime / 1000) / 1000);
                    if (mCallback != null
                            // 防止重复更新
                            && !TextUtils.equals(curElapsedTime, preElapsedTime)) {
                        mCallback.onProgressUpdated(wrapper.sampleTime / 1000000);
                        preElapsedTime = curElapsedTime;
                        //MLog.d(TAG, "handleData() curElapsedTime: " + curElapsedTime);
                    }
                } else {
                }
            } else {
                if (isReading) {
                    if (mCallback != null) {
                        if (wrapper.type == TYPE_AUDIO) {
                            MLog.d(TAG, "handleData() audio onPaused");
                        } else {
                            MLog.w(TAG, "handleData() video onPaused");
                        }
                        mCallback.onPaused();
                    }

                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.e(TAG, "卧槽!卧槽!卧槽! audio太不给力了");
                    } else {
                        MLog.e(TAG, "卧槽!卧槽!卧槽! video太不给力了");
                    }
                    mAudioWrapper.isPausedForCache = true;
                    mVideoWrapper.isPausedForCache = true;

                    notifyToHandleWait(wrapper);

                    mAudioWrapper.isPausedForCache = false;
                    mVideoWrapper.isPausedForCache = false;

                    if (wrapper.isPausedForSeek) {
                        continue;
                    }

                    if (wrapper.type == TYPE_AUDIO) {
                        notifyToHandle(mVideoWrapper);
                        MLog.d(TAG, "handleData() audio onPlayed");
                    } else {
                        notifyToHandle(mAudioWrapper);
                        MLog.w(TAG, "handleData() video onPlayed");
                    }
                    if (mCallback != null) {
                        mCallback.onPlayed();
                    }
                } else {
                    wrapper.isHandling = false;
                    break;
                }
            }
        }// while(true) end
        if (wrapper.type == TYPE_AUDIO) {
            MLog.d(TAG, "handleData() audio end");
        } else {
            MLog.w(TAG, "handleData() video end");
        }

        if (wrapper != null) {
            wrapper.clear();
            if (wrapper.type == TYPE_AUDIO) {
                mAudioWrapper = null;
            } else {
                mVideoWrapper = null;
            }
            wrapper = null;
        }

        if (mAudioWrapper == null
                && mVideoWrapper == null) {
            //SystemClock.sleep(500);
            internalDestroy();
            if (mCallback != null) {
                MLog.w(TAG, "handleData() onFinished");
                mCallback.onFinished();
            }

            System.gc();
            MLog.i(TAG, "handleData() audio and video are finished");
        }
    }

    private void init() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleVideoPlayer.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleVideoPlayer.this.uiHandleMessage(msg);
            }
        };
    }

    private boolean prepareAudio() {
        // Audio
        MLog.d(TAG, "prepareAudio() start");
        MLog.d(TAG, "prepareAudio() audio decoderMediaFormat: " +
                mAudioWrapper.decoderMediaFormat.toString());

        /***
         解码前
         {
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2],
         mime=audio/mp4a-latm, aac-profile=2, channel-count=2, track-id=2, bitrate=96000,
         max-input-size=444, durationUs=10871488000,
         sample-rate=48000, max-bitrate=96000
         }
         解码后
         {pcm-encoding=2, mime=audio/raw, channel-count=2, sample-rate=48000}
         */
        // durationUs
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mAudioWrapper.durationUs =
                    mAudioWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mAudioWrapper.durationUs / 1000) / 1000);
            MLog.d(TAG, "prepareAudio()          audioDurationUs: " +
                    mAudioWrapper.durationUs + " " + durationTime);
            if (mAudioWrapper.durationUs > 0 || mVideoWrapper.durationUs > 0) {
                mAudioWrapper.durationUs =
                        (mAudioWrapper.durationUs > mVideoWrapper.durationUs)
                                ?
                                mAudioWrapper.durationUs
                                :
                                mVideoWrapper.durationUs;
                mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            } else {
                mVideoWrapper.durationUs = mAudioWrapper.durationUs = -1;
            }
        }
        // max-input-size
        mAudioWrapper.frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mAudioWrapper.frameMaxLength =
                    mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            if (mAudioWrapper.frameMaxLength <= 0) {
                mAudioWrapper.frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
            }
        }
        MLog.d(TAG, "prepareAudio() audio     frameMaxLength: " +
                mAudioWrapper.frameMaxLength);
        // 创建音频解码器
        try {
            extractor.selectTrack(mAudioWrapper.trackIndex);
            MLog.d(TAG, "prepareAudio() audio mAudioWrapper.mime: " + mAudioWrapper.mime);
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
                        MLog.d(TAG, "prepareAudio() audio          codecName: " + codecName);
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

            mAudioWrapper.render = false;
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
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
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
            MLog.d(TAG, "prepareAudio() audio              csd-0: " + sb.toString());
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
            MLog.d(TAG, "prepareAudio() audio              csd-1: " + sb.toString());
        }
        MLog.d(TAG, "prepareAudio() end");

        return true;
    }

    private boolean prepareVideo() {
        // Video
        MLog.w(TAG, "prepareVideo() start");
        if (mSurface != null) {
            mVideoWrapper.mSurface = mSurface;
        } else {
            MLog.e(TAG, "prepareVideo() mSurface is null");
            return false;
        }
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
        MLog.w(TAG, "prepareVideo() video decoderMediaFormat: " +
                mVideoWrapper.decoderMediaFormat.toString());

        /***
         解码前
         {
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=28 cap=28],
         csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9],
         mime=video/avc, frame-rate=24, track-id=1, profile=8,
         width=1280, height=720, max-input-size=243905, durationUs=10871402208,
         bitrate-mode=0, level=512
         }
         解码后
         {crop-top=0, crop-right=1279, color-format=19, height=720,
         color-standard=1, crop-left=0, color-transfer=3, stride=1280,
         mime=video/raw, slice-height=720, width=1280, color-range=2, crop-bottom=719}
         */
        // durationUs
        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mVideoWrapper.durationUs =
                    mVideoWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mVideoWrapper.durationUs / 1000) / 1000);
            MLog.w(TAG, "prepareVideo()          videoDurationUs: " +
                    mVideoWrapper.durationUs + " " + durationTime);
            if (mAudioWrapper.durationUs > 0 || mVideoWrapper.durationUs > 0) {
                mAudioWrapper.durationUs =
                        (mAudioWrapper.durationUs > mVideoWrapper.durationUs)
                                ?
                                mAudioWrapper.durationUs
                                :
                                mVideoWrapper.durationUs;
                mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            } else {
                mVideoWrapper.durationUs = mAudioWrapper.durationUs = -1;
            }
        }
        // max-input-size
        mVideoWrapper.frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mVideoWrapper.frameMaxLength =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            if (mVideoWrapper.frameMaxLength <= 0) {
                mVideoWrapper.frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
            }
        }
        MLog.w(TAG, "prepareVideo() video     frameMaxLength: " +
                mVideoWrapper.frameMaxLength);
        // 创建视频解码器
        try {
            extractor.selectTrack(mVideoWrapper.trackIndex);
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
                mVideoWrapper.decoderMediaCodec = null;
            }
        }
        if (mVideoWrapper.decoderMediaCodec == null) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
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
            MLog.w(TAG, "prepareVideo() video              csd-0: " + sb.toString());
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
            MLog.w(TAG, "prepareVideo() video              csd-1: " + sb.toString());
        }
        MLog.w(TAG, "prepareVideo() end");

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            MLog.e(TAG, "internalPrepare() mPath is empty");
            return false;
        }
        MLog.i(TAG, "internalPrepare() start");

        if (mCallback != null) {
            mCallback.onReady();
        }

        mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
        mVideoWrapper = new VideoWrapper(TYPE_VIDEO);
        mAudioWrapper.clear();
        mVideoWrapper.clear();
        preElapsedTime = null;
        curElapsedTime = null;
        runCounts = 0;
        isReading = true;
        mAudioWrapper.isHandling = true;
        mVideoWrapper.isHandling = true;

        if (mPath.startsWith("/storage/")) {
            File file = new File(mPath);
            if (!file.canRead()
                    || file.isDirectory()) {
                MLog.e(TAG, "internalPrepare() 不能读取此文件: " + mPath);
                internalRelease();
                return false;
            }
            long fileSize = file.length();
            MLog.i(TAG, "internalPrepare() fileSize: " + fileSize);
            mIsLocal = true;
            mAudioWrapper.CACHE_START = START_CACHE_COUNT_LOCAL;
            mVideoWrapper.CACHE_START = START_CACHE_COUNT_LOCAL;
        } else {
            mIsLocal = false;
            mAudioWrapper.CACHE_START = START_CACHE_COUNT_HTTP;
            mVideoWrapper.CACHE_START = START_CACHE_COUNT_HTTP;
        }

        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
            return false;
        }

        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mAudioWrapper.mime = mime;
                mAudioWrapper.trackIndex = i;
                mAudioWrapper.decoderMediaFormat = format;
            } else if (mime.startsWith("video/")) {
                mVideoWrapper.mime = mime;
                mVideoWrapper.trackIndex = i;
                mVideoWrapper.decoderMediaFormat = format;
            }
        }
        if ((TextUtils.isEmpty(mAudioWrapper.mime)
                && TextUtils.isEmpty(mVideoWrapper.mime))
                || (mAudioWrapper.trackIndex == -1
                && mVideoWrapper.trackIndex == -1)
                || (mAudioWrapper.decoderMediaFormat == null
                && mVideoWrapper.decoderMediaFormat == null)) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            internalRelease();
            return false;
        }

        MLog.i(TAG, "internalPrepare() end");

        return true;
    }

    private void printInfo() {
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
            MLog.i(TAG, "internalPrepare()      audioDurationUs: " + audioDurationUs);
            MLog.i(TAG, "internalPrepare()      videoDurationUs: " + videoDurationUs);
            MLog.i(TAG, "internalPrepare()           durationUs: " + mVideoWrapper.durationUs);
        }

        mThreadHandler.removeMessages(MSG_HANDLE_DATA_LOCK_WAIT);
    }

    private void internalPlay() {
        if (!mAudioWrapper.isPausedForCache
                && !mVideoWrapper.isPausedForCache) {
            mAudioWrapper.isPausedForUser = false;
            mVideoWrapper.isPausedForUser = false;
            notifyToHandle(mAudioWrapper);
            notifyToHandle(mVideoWrapper);
        }
    }

    private void internalPause() {
        if (!mAudioWrapper.isPausedForCache
                && !mVideoWrapper.isPausedForCache) {
            mAudioWrapper.isPausedForUser = true;
            mVideoWrapper.isPausedForUser = true;
        }
    }

    private void internalRelease() {
        MLog.i(TAG, "internalRelease() start");
        //notifyAudioEndOfStream();
        //notifyVideoEndOfStream();
        isReading = false;
        mAudioWrapper.isPausedForSeek = false;
        mVideoWrapper.isPausedForSeek = false;
        notifyToRead();

        mAudioWrapper.isHandling = false;
        mAudioWrapper.isPausedForUser = false;
        mAudioWrapper.isPausedForCache = false;

        mVideoWrapper.isHandling = false;
        mVideoWrapper.isPausedForUser = false;
        mVideoWrapper.isPausedForCache = false;

        notifyToRead(mAudioWrapper);
        notifyToRead(mVideoWrapper);
        notifyToHandle(mAudioWrapper);
        notifyToHandle(mVideoWrapper);
        MLog.i(TAG, "internalRelease() end");
    }

    private void internalDestroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    private String preElapsedTime = null;
    private String curElapsedTime = null;
    //private boolean onlyOneStart = true;

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
                /*if (wrapper.type == TYPE_AUDIO) {
                    MLog.d(TAG, "drainOutputBuffer() Audio roomIndex: " + roomIndex);
                } else {
                    MLog.w(TAG, "drainOutputBuffer() Video roomIndex: " + roomIndex);
                }*/
                switch (roomIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        // 像音频,第一个输出日志
                        /*if (wrapper.type == TYPE_AUDIO) {
                            MLog.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_TRY_AGAIN_LATER");
                        } else {
                            MLog.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_TRY_AGAIN_LATER");
                        }*/
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        // 像音频,第三个输出日志
                        // 一般一个视频各自调用一次
                        if (wrapper.type == TYPE_AUDIO) {
                            MLog.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleAudioOutputFormat();
                        } else {
                            MLog.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleVideoOutputFormat();
                        }
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        // 像音频,第二个输出日志.视频好像没有这个输出日志
                        if (wrapper.type == TYPE_AUDIO) {
                            MLog.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        } else {
                            MLog.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        }
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
                // 不能根据room是否为null来判断是audio还是video(我的三星Note2手机上是可以的)
                if (room != null) {
                    // audio
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    if (wrapper.type == TYPE_AUDIO) {
                        handleAudioOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    } else {
                        handleVideoOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    }
                } else {
                    // video
                    handleVideoOutputBuffer(roomIndex, null, roomInfo, roomSize);
                }

                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    } else {
                        MLog.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    }
                    wrapper.decoderMediaCodec.releaseOutputBuffer(roomIndex, wrapper.render);
                    return false;
                }
                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    } else {
                        MLog.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    }
                }

                wrapper.decoderMediaCodec.releaseOutputBuffer(roomIndex, wrapper.render);
            } catch (IllegalStateException
                    | IllegalArgumentException
                    | NullPointerException e) {
                if (wrapper.type == TYPE_AUDIO) {
                    MLog.e(TAG, "drainOutputBuffer() Audio Output occur exception: " + e);
                } else {
                    MLog.e(TAG, "drainOutputBuffer() Video Output occur exception: " + e);
                }
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
        if (wrapper.needToSeek) {
            return true;
        }

        /*// 互相等待,一起start
        if (wrapper.type == TYPE_AUDIO) {
            // 如果是音频先走到这里,那么等待视频的开始
            while (!mVideoWrapper.isStarted) {
                if (!wrapper.isHandling) {
                    return false;
                }
                SystemClock.sleep(10);
            }
        } else {
            wrapper.isStarted = true;
            // 如果是视频先走到这里,那么等待音频的开始
            while (!mAudioWrapper.isStarted) {
                if (!wrapper.isHandling) {
                    return false;
                }
                SystemClock.sleep(10);
            }
        }*/

        boolean feedInputBufferResult = false;
        boolean drainOutputBufferResult = false;
        try {
            // Input Data
            feedInputBufferResult = feedInputBuffer(
                    wrapper.decoderMediaCodec,
                    wrapper.data,
                    0,
                    wrapper.size,
                    wrapper.sampleTime);

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
        try {
            MediaFormat newMediaFormat = mAudioWrapper.decoderMediaCodec.getOutputFormat();
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
            MLog.d(TAG, "handleAudioOutputFormat() newMediaFormat: " +
                    mAudioWrapper.decoderMediaFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mAudioWrapper.mAudioTrack != null) {
            mAudioWrapper.mAudioTrack.release();
        }

        // 创建AudioTrack
        // 1.
        int sampleRateInHz =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        // 2.
        int channelCount =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        // 3.
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            // 关键参数(需要解码后才能知道)
            audioFormat =
                    mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
        }

        // sampleRateInHz: 48000 channelCount: 2 audioFormat: 2
        MLog.d(TAG, "handleAudioOutputFormat()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);

        // create AudioTrack
        mAudioWrapper.mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioWrapper.mAudioTrack != null) {
            if (mContext != null) {
                SharedPreferences sp =
                        mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean isMute = sp.getBoolean(PLAYBACK_IS_MUTE, false);
                if (!isMute) {
                    setVolume(FFMPEG.VOLUME_NORMAL);
                } else {
                    setVolume(FFMPEG.VOLUME_MUTE);
                }
            } else {
                setVolume(FFMPEG.VOLUME_NORMAL);
            }
            mAudioWrapper.mAudioTrack.play();
            mAudioWrapper.isStarted = true;
            /*while (!mVideoWrapper.isStarted) {
                if (!mAudioWrapper.isHandling) {
                    return;
                }
                SystemClock.sleep(10);
            }*/
            MLog.d(TAG, "handleAudioOutputFormat() 声音马上输出......");
        } else {
            MLog.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
            mAudioWrapper.isHandling = false;
            notifyToRead(mAudioWrapper);
            notifyToHandle(mAudioWrapper);
        }

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

        // 音视频都已经开始的话,就可以播放了
        if (mCallback != null) {
            MLog.i(TAG, "onPlayed");
            mCallback.onPlayed();
        }
    }

    private void handleVideoOutputFormat() {
        /***
         解码前
         {csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9],
         mime=video/avc, frame-rate=24, track-id=1, profile=8,
         width=1280, height=720, max-input-size=243905, durationUs=10871402208,
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=28 cap=28],
         bitrate-mode=0, level=512}
         解码后
         {crop-top=0, crop-right=1279, color-format=19, height=720,
         color-standard=1, crop-left=0, color-transfer=3, stride=1280,
         mime=video/raw, slice-height=720, width=1280, color-range=2, crop-bottom=719}
         */
        try {
            MediaFormat newMediaFormat = mVideoWrapper.decoderMediaCodec.getOutputFormat();
            Class clazz = Class.forName("android.media.MediaFormat");
            Method method = clazz.getDeclaredMethod("getMap");
            method.setAccessible(true);
            Object newObject = method.invoke(newMediaFormat);
            Object oldObject = method.invoke(mVideoWrapper.decoderMediaFormat);
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
            MLog.w(TAG, "handleVideoOutputFormat() newMediaFormat: " +
                    mVideoWrapper.decoderMediaFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mVideoWrapper.isStarted = true;
        // 如果是视频先走到这里,那么等待音频的开始
        /*while (!mAudioWrapper.isStarted) {
            if (!mVideoWrapper.isHandling) {
                return;
            }
            SystemClock.sleep(10);
        }*/

        MLog.w(TAG, "handleVideoOutputFormat() 画面马上输出......");
    }

    private int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                        MediaCodec.BufferInfo roomInfo, int roomSize) {
        mAudioWrapper.presentationTimeUs = roomInfo.presentationTimeUs;
        // 输出音频
        if (mAudioWrapper.isHandling
                && mAudioWrapper.mAudioTrack != null
                && mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            byte[] audioData = new byte[roomSize];
            room.get(audioData, 0, audioData.length);
            mAudioWrapper.mAudioTrack.write(audioData, 0, audioData.length);
        }

        return 0;
    }

    private boolean mIsSeeked = false;
    private int mCountWithSeek = 0;
    private long prePresentationTimeUs;
    private static final int RUN_COUNTS = 100;
    private int[] timeDiff = new int[RUN_COUNTS];
    private int TIME_DIFFERENCE = 150;
    private int runCounts = 0;

    // 最最最关键的一步
    private int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                        MediaCodec.BufferInfo roomInfo, int roomSize) {
        mVideoWrapper.presentationTimeUs = roomInfo.presentationTimeUs;

        if (mVideoWrapper.presentationTimeUs > 0 && mAudioWrapper.presentationTimeUs > 0) {
            int diffTime =
                    (int) ((mVideoWrapper.presentationTimeUs - mAudioWrapper.presentationTimeUs) / 1000);
            if (runCounts < RUN_COUNTS) {
                if (diffTime > 0) {
                    timeDiff[runCounts++] = diffTime;
                }
            } else if (runCounts == RUN_COUNTS) {
                runCounts++;
                int totleTimeDiff = 0;
                for (int i = 0; i < RUN_COUNTS; i++) {
                    totleTimeDiff += timeDiff[i];
                }
                int averageTimeDiff = totleTimeDiff / RUN_COUNTS;
                // 希望得到一个好的TIME_DIFFERENCE值
                hope_to_get_a_good_result(averageTimeDiff);

            }
            if (diffTime < 0) {
                return 0;
            }
            while (diffTime > TIME_DIFFERENCE) {
                if (!mVideoWrapper.isHandling
                        || !mAudioWrapper.isHandling
                        || mVideoWrapper.isPausedForUser
                        || mAudioWrapper.isPausedForUser
                        || mVideoWrapper.isPausedForCache
                        || mAudioWrapper.isPausedForCache
                        || mVideoWrapper.isPausedForSeek
                        || mAudioWrapper.isPausedForSeek) {
                    return 0;
                }

                SystemClock.sleep(1);
                diffTime =
                        (int) ((mVideoWrapper.presentationTimeUs - mAudioWrapper.presentationTimeUs) / 1000);
            }
        }

        SystemClock.sleep(11);

        return 0;
    }

    private void hope_to_get_a_good_result(int averageTimeDiff) {
        MLog.w(TAG, "handleVideoOutputBuffer() averageTimeDiff: " + averageTimeDiff);
        if (averageTimeDiff > 1000) {
            TIME_DIFFERENCE = 1000;
        } else if (averageTimeDiff > 900 && averageTimeDiff <= 1000) {
            TIME_DIFFERENCE = 950;
        } else if (averageTimeDiff > 800 && averageTimeDiff <= 900) {
            TIME_DIFFERENCE = 850;
        } else if (averageTimeDiff > 700 && averageTimeDiff <= 800) {
            TIME_DIFFERENCE = 750;
        } else if (averageTimeDiff > 600 && averageTimeDiff <= 700) {
            TIME_DIFFERENCE = 650;
        } else if (averageTimeDiff > 500 && averageTimeDiff <= 600) {
            TIME_DIFFERENCE = 550;
        } else if (averageTimeDiff > 400 && averageTimeDiff <= 500) {
            TIME_DIFFERENCE = 450;
        } else if (averageTimeDiff > 300 && averageTimeDiff <= 400) {
            TIME_DIFFERENCE = 350;
        } else if (averageTimeDiff > 200 && averageTimeDiff <= 300) {
            TIME_DIFFERENCE = 250;
        } else if (averageTimeDiff > 100 && averageTimeDiff <= 200) {
            TIME_DIFFERENCE = averageTimeDiff;
        } else if (averageTimeDiff <= 100) {
            TIME_DIFFERENCE = 150;
        }
        MLog.w(TAG, "handleVideoOutputBuffer() TIME_DIFFERENCE: " + TIME_DIFFERENCE);
    }

    private void notifyAudioEndOfStream() {
        MLog.d(TAG, "notifyAudioEndOfStream() start");
        // audio end notify
        int inputAudioBufferIndex = mAudioWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        while (inputAudioBufferIndex < 0) {
            inputAudioBufferIndex = mAudioWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mAudioWrapper.decoderMediaCodec.queueInputBuffer(
                inputAudioBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        MLog.d(TAG, "notifyAudioEndOfStream() end");
    }

    private void notifyVideoEndOfStream() {
        MLog.w(TAG, "notifyVideoEndOfStream() start");
        // video end notify
        int inputBufferIndex = mVideoWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        while (inputBufferIndex < 0) {
            inputBufferIndex = mVideoWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mVideoWrapper.decoderMediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        MLog.w(TAG, "notifyVideoEndOfStream() end");
    }

    private void notifyToRead() {
        synchronized (readDataLock) {
            readDataLock.notify();
        }
    }

    private void notifyToReadWait() {
        try {
            synchronized (readDataLock) {
                readDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void notifyToRead(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        synchronized (wrapper.readDataLock) {
            wrapper.readDataLock.notify();
        }
    }

    private void notifyToReadWait(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        try {
            synchronized (wrapper.readDataLock) {
                wrapper.readDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void notifyToHandle(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        synchronized (wrapper.handleDataLock) {
            wrapper.handleDataLock.notify();
        }
    }

    private void notifyToHandleWait(SimpleWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        try {
            synchronized (wrapper.handleDataLock) {
                wrapper.handleDataLock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
