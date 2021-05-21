package com.weidi.usefragments.business.video_player;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCrypto;
import android.media.MediaDataSource;
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

import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.Callback;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import androidx.annotation.RequiresApi;

/***
 Created by weidi on 2020/08/17.
 */
public class SimplePlayer {

    private static final String TAG = "player_alexander";
    //SimpleVideoPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final float VOLUME_NORMAL = 1.0f;
    public static final float VOLUME_MUTE = 0.0f;

    public static final int MAX_CACHE_AUDIO_COUNT = 210;// 5000
    public static final int MAX_CACHE_VIDEO_COUNT = 60;

    private static final int START_CACHE_COUNT_HTTP = 30;// 3000
    private static final int START_CACHE_COUNT_LOCAL = 30;

    // 只是设置为默认值
    // 音频一帧的大小不能超过这个值,不然出错(如果设成1024 * 1024会有杂音,不能过大,调查了好久才发现跟这个有关)
    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;// 102400
    // 视频一帧的大小不能超过这个值,不然出错
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024 * 5;// 5242880

    public static final int TYPE_VIDEO = 0x0001;
    public static final int TYPE_AUDIO = 0x0002;

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

    private Context mContext = null;
    private String mPath = "/storage/37C8-3904/myfiles/video/哪吒之魔童降世.mp4";
    private boolean mIsLocal = false;

    private LocalMediaDataSource mediaDataSource;
    private MediaExtractor mExtractor = null;
    private Surface mSurface = null;

    private Handler mUiHandler = null;
    private Handler mThreadHandler = null;
    private HandlerThread mHandlerThread = null;

    private Callback mCallback = null;

    private boolean isEOF = false;
    private boolean isReading = false;
    private Object readDataLock = new Object();

    public AudioWrapper mAudioWrapper;
    public VideoWrapper mVideoWrapper;

    public static class SimpleWrapper {
        public String mime = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        public MediaCodec.CryptoInfo cryptoInfo = null;
        public int flags = 0;
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
                    CACHE_START = START_CACHE_COUNT_HTTP;
                    frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
                    break;
                case TYPE_VIDEO:
                    this.type = TYPE_VIDEO;
                    CACHE = MAX_CACHE_VIDEO_COUNT;
                    CACHE_START = START_CACHE_COUNT_HTTP;
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
            flags = 0;
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

    public static class VideoWrapper extends SimpleWrapper {
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
            mSurface = null;
            width = 0;
            height = 0;
            super.clear();
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
            MediaUtils.releaseMediaCodec(decoderMediaCodec);
            MediaUtils.releaseAudioTrack(mAudioTrack);
            mAudioTrack = null;
            super.clear();
        }
    }

    public SimplePlayer() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setHandler(Handler handler) {
        mUiHandler = handler;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    // 调用setSurface()和setDataSource()就可以播放了
    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setDataSource(String localPath) {
        if (TextUtils.isEmpty(localPath)) {
            return;
        }

        if (mCallback != null) {
            Log.i(TAG, "onReady");
            mCallback.onReady();
        }
        mPath = localPath;
        mThreadHandler.obtainMessage();
        mThreadHandler.removeMessages(MSG_DO_SOME_WORK);
        mThreadHandler.sendEmptyMessage(MSG_DO_SOME_WORK);
    }

    public void setVolume(float volume) {
        if (mAudioWrapper.mAudioTrack == null
                || mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return;
        }
        if (volume < 0 || volume > 1.0f) {
            volume = VOLUME_NORMAL;
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
        Log.i(TAG, "setProgressUs() start");
        Log.i(TAG, "----------------------------------------------------------");
        String elapsedTime = DateUtils.formatElapsedTime(progressUs / 1000000);
        Log.i(TAG, "setProgressUs() progressUs: " + progressUs + " " + elapsedTime);
        Log.i(TAG, "----------------------------------------------------------");
        if (progressUs < 0
                || progressUs > mAudioWrapper.durationUs
                || progressUs > mVideoWrapper.durationUs
                || mAudioWrapper.isPausedForSeek
                || mVideoWrapper.isPausedForSeek) {
            Log.i(TAG, "setProgressUs() return");
            return;
        }
        mAudioWrapper.isPausedForSeek = true;
        mVideoWrapper.isPausedForSeek = true;
        notifyToHandle(mAudioWrapper);
        notifyToHandle(mVideoWrapper);
        mProgressUs = progressUs;
        mAudioWrapper.progressUs = progressUs;
        mVideoWrapper.progressUs = progressUs;
        notifyToRead();
        notifyToRead(mAudioWrapper);
        notifyToRead(mVideoWrapper);
        Log.i(TAG, "setProgressUs() end");
    }

    private void seekTo() {
        Log.i(TAG, "seekTo() start");
        if (mAudioWrapper.isStarted && mVideoWrapper.isStarted) {
            while (!mAudioWrapper.needToSeek || !mVideoWrapper.needToSeek) {
                SystemClock.sleep(1);
            }
        }
        synchronized (mAudioWrapper.readDataLock) {
            mAudioWrapper.readData.clear();
        }
        synchronized (mVideoWrapper.readDataLock) {
            mVideoWrapper.readData.clear();
        }
        Log.i(TAG, "seekTo()");
        // SEEK_TO_PREVIOUS_SYNC SEEK_TO_CLOSEST_SYNC SEEK_TO_NEXT_SYNC
        mExtractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        mAudioWrapper.decoderMediaCodec.flush();
        mVideoWrapper.decoderMediaCodec.flush();
        mAudioWrapper.needToSeek = false;
        mVideoWrapper.needToSeek = false;
        mAudioWrapper.isPausedForSeek = false;
        mVideoWrapper.isPausedForSeek = false;
        mAudioWrapper.presentationTimeUs = 0;
        mVideoWrapper.presentationTimeUs = 0;
        preElapsedTime = null;
        Log.i(TAG, "seekTo() end");
    }

    public long getDurationUs() {
        return mAudioWrapper.durationUs;
    }

    public void play() {
        //internalPlay();
        mThreadHandler.removeMessages(MSG_PLAY);
        mThreadHandler.sendEmptyMessage(MSG_PLAY);
    }

    public void pause() {
        //internalPause();
        mThreadHandler.removeMessages(MSG_PAUSE);
        mThreadHandler.sendEmptyMessage(MSG_PAUSE);
    }

    public void release() {
        //internalRelease();
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
        return mAudioWrapper.isHandling
                || mVideoWrapper.isHandling;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void doSomeWork() {
        mediaDataSource = new LocalMediaDataSource();
        mediaDataSource.setDataSource(mPath);
        mediaDataSource.setCallback(new LocalMediaDataSource.Callback() {
            @Override
            public void onStart() {
                Log.i(TAG, "LocalMediaDataSource.Callback onStart()");
                mExtractor = new MediaExtractor();
                try {
                    isEOF = false;
                    mediaDataSource.hasSetDataSource = false;
                    Log.i(TAG, "setDataSource() start");
                    mExtractor.setDataSource(mediaDataSource);
                    Log.i(TAG, "setDataSource() end");
                    mediaDataSource.hasSetDataSource = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mExtractor != null) {
                        mExtractor.release();
                    }
                    mExtractor = null;
                    mediaDataSource.safeExit();
                    mediaDataSource = null;
                    System.gc();
                    if (mCallback != null) {
                        Log.w(TAG, "onStart() onFinished");
                        mCallback.onFinished();
                    }
                    return;
                }

                if (mExtractor == null
                        || mSurface == null) {
                    Log.e(TAG, "onStart()" +
                            " mExtractor or mSurface is null");
                    mediaDataSource.safeExit();
                    mediaDataSource = null;
                    System.gc();
                    if (mCallback != null) {
                        Log.w(TAG, "onStart() onFinished");
                        mCallback.onFinished();
                    }
                    return;
                }

                // LocalMediaDataSource中的readAt(...)函数没有写好
                int trackCount = mExtractor.getTrackCount();
                if (trackCount <= 0) {
                    Log.e(TAG, "onStart()" +
                            " trackCount <= 0");
                    mediaDataSource.safeExit();
                    mediaDataSource = null;
                    System.gc();
                    if (mCallback != null) {
                        Log.w(TAG, "onStart() onFinished");
                        mCallback.onFinished();
                    }
                    return;
                }

                mThreadHandler.removeMessages(MSG_PREPARE);
                mThreadHandler.sendEmptyMessageDelayed(MSG_PREPARE, 1000);
            }

            @Override
            public void onEnd() {
                Log.i(TAG, "LocalMediaDataSource.Callback onEnd()");
                isEOF = true;
            }

            @Override
            public void onError(String msg) {
                Log.e(TAG, "LocalMediaDataSource.Callback onError() msg: " + msg);
                isEOF = true;
            }
        });

        // 开始运行
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                mediaDataSource.readLocalData();
            }
        });
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_DO_SOME_WORK:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    doSomeWork();
                }
                break;
            case MSG_PREPARE:
                if (internalPrepare()) {
                    if (prepareVideo() && prepareAudio()) {
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                handleData(mVideoWrapper);
                            }
                        });
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                handleData(mAudioWrapper);
                            }
                        });
                        SystemClock.sleep(500);
                        ThreadPool.getFixedThreadPool().execute(new Runnable() {
                            @Override
                            public void run() {
                                readData();
                            }
                        });
                    }
                } else {
                    if (mCallback != null) {
                        Log.e(TAG, "onError internalPrepare()");
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
            case MSG_RELEASE:
                internalRelease();
                break;
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
                if (DEBUG)
                    Log.d(TAG, "onKeyDown() 2");
                if (mProgressUs == -1) {
                    step = (int) ((mVideoWrapper.presentationTimeUs
                            / (mVideoWrapper.durationUs * 1.00)) * 3840.00);
                    Log.d(TAG, "onKeuiHandleMessageyDown() step: " + step);
                }
                step += 100;
                long progress = (long) (((step / 3840.00) * mVideoWrapper.durationUs));
                setProgressUs(progress);
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
    private void readData() {
        mAudioWrapper.readData = new ArrayList<AVPacket>();
        mVideoWrapper.readData = new ArrayList<AVPacket>(mVideoWrapper.CACHE);
        mVideoWrapper.width =
                mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        mVideoWrapper.height =
                mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        if (mCallback != null) {
            mCallback.onChangeWindow(mVideoWrapper.width, mVideoWrapper.height);
        }

        Log.i(TAG, "readData() start");
        //setProgressUs(721057837);
        int readSize = -1;
        int trackIndex = -1;
        ByteBuffer room = null;
        room = ByteBuffer.allocate(mVideoWrapper.frameMaxLength);

        boolean audioStarted = false;
        boolean videoStarted = false;
        AVPacket data = null;
        isReading = true;
        for (; ; ) {
            if (!isReading) {
                break;
            }
            try {
                // seekTo
                if (mAudioWrapper.isPausedForSeek
                        || mVideoWrapper.isPausedForSeek) {
                    seekTo();
                    audioStarted = false;
                    videoStarted = false;
                }

                room.clear();
                readSize = mExtractor.readSampleData(room, 0);
                trackIndex = mExtractor.getSampleTrackIndex();

                // region readSize < 0

                if (readSize < 0) {
                    Log.e(TAG, "readData() end      readSize: " + readSize + " isEOF: " + isEOF);
                    Log.e(TAG,
                            "readData() end audioDataSize: " + mAudioWrapper.readData.size());
                    Log.e(TAG,
                            "readData() end videoDataSize: " + mVideoWrapper.readData.size());
                    if (isEOF) {
                        // 没有数据可读了,结束
                        // 也有可能是底层异常引起
                        // MediaHTTPConnection:
                        // readAt 1033700796 / 32768 => java.net.ProtocolException:
                        // unexpected end of stream

                        isReading = false;
                        // 开启任务处理数据(如果还没有开启任务过的话)
                        // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                        if (!mAudioWrapper.isPausedForUser
                                && !mVideoWrapper.isPausedForUser) {
                            notifyToHandle(mAudioWrapper);
                            notifyToHandle(mVideoWrapper);
                        }

                        /*Log.e(TAG, "readData() notifyToReadWait start");
                        // 需要seek,不能退出
                        notifyToReadWait();
                        Log.e(TAG, "readData() notifyToReadWait end");
                        if (mAudioWrapper.isPausedForSeek
                                || mVideoWrapper.isPausedForSeek) {
                            isReading = true;
                            continue;
                        } else {
                            break;
                        }*/

                        break;
                    }

                    SystemClock.sleep(1000);
                    //mExtractor.advance();
                    continue;
                }

                // endregion

                data = new AVPacket(readSize, mExtractor.getSampleTime());
                data.cryptoInfo = null;
                data.flags = 0;
                room.get(data.data, 0, readSize);

                if (trackIndex == mVideoWrapper.trackIndex
                        && mVideoWrapper.isHandling) {
                    synchronized (mVideoWrapper.readDataLock) {
                        mVideoWrapper.readData.add(data);
                    }

                    if (videoStarted
                            && mVideoWrapper.readData.size() >= mVideoWrapper.CACHE
                            && mAudioWrapper.readData.size() >= mAudioWrapper.CACHE) {
                        /*Log.i(TAG, "readData() notifyToReadWait start");
                        Log.i(TAG,
                                "readData() video size: " + mVideoWrapper.readData.size());
                        Log.i(TAG,
                                "readData() audio size: " + mAudioWrapper.readData.size());*/
                        //System.gc();
                        notifyToReadWait(mVideoWrapper);
                        if (mAudioWrapper.isPausedForSeek
                                || mVideoWrapper.isPausedForSeek) {
                            Log.i(TAG, "readData() notifyToReadWait end for Seek");
                            continue;
                        }
                        //Log.i(TAG, "readData() notifyToReadWait end");
                    } else if ((!videoStarted || mVideoWrapper.isPausedForCache)
                            && mVideoWrapper.readData.size() >= mVideoWrapper.CACHE_START) {
                        Log.w(TAG, "readData() video notifyToHandle");
                        videoStarted = true;
                        notifyToHandle(mVideoWrapper);
                    }
                } else if (trackIndex == mAudioWrapper.trackIndex
                        && mAudioWrapper.isHandling) {
                    synchronized (mAudioWrapper.readDataLock) {
                        mAudioWrapper.readData.add(data);
                    }

                    if ((!audioStarted || mAudioWrapper.isPausedForCache)
                            && mAudioWrapper.readData.size() >= mAudioWrapper.CACHE_START) {
                        Log.d(TAG, "readData() audio notifyToHandle");
                        audioStarted = true;
                        notifyToHandle(mAudioWrapper);
                    }
                }

                // Test
                /*if (trackIndex == mVideoWrapper.trackIndex) {
                    mVideoWrapper.data = data.data;
                    mVideoWrapper.size = data.size;
                    mVideoWrapper.sampleTime = data.sampleTime;
                    if (!feedInputBufferAndDrainOutputBuffer(mVideoWrapper)) {
                        mVideoWrapper.isHandling = false;
                        break;
                    }
                } else if (trackIndex == mAudioWrapper.trackIndex) {
                    mAudioWrapper.data = data.data;
                    mAudioWrapper.size = data.size;
                    mAudioWrapper.sampleTime = data.sampleTime;
                    if (!feedInputBufferAndDrainOutputBuffer(mAudioWrapper)) {
                        mAudioWrapper.isHandling = false;
                        break;
                    }
                }*/

                // 跳到下一帧
                mExtractor.advance();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }// for (; ; ) end
        isReading = false;

        if (mExtractor != null) {
            mExtractor.release();
        }
        mExtractor = null;

        Log.i(TAG, "readData() end");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void handleData(SimpleWrapper wrapper) {
        if (wrapper.type == TYPE_AUDIO) {
            mAudioWrapper.isHandling = true;
        } else {
            mVideoWrapper.isHandling = true;
        }
        if (wrapper.type == TYPE_AUDIO) {
            Log.d(TAG, "handleData() audio notifyToHandleWait start");
        } else {
            Log.w(TAG, "handleData() video notifyToHandleWait start");
        }
        notifyToHandleWait(wrapper);
        if (wrapper.type == TYPE_AUDIO) {
            Log.d(TAG, "handleData() audio notifyToHandleWait end");
        } else {
            Log.w(TAG, "handleData() video notifyToHandleWait end");
        }

        AVPacket data = null;
        //int readDataSize = 0;
        if (wrapper.type == TYPE_AUDIO) {
            Log.d(TAG, "handleData() audio start");
        } else {
            Log.w(TAG, "handleData() video start");
        }
        for (; ; ) {
            if (!wrapper.isHandling) {
                break;
            }

            // region 暂停装置

            if (wrapper.isPausedForUser
                    || wrapper.isPausedForCache
                    || wrapper.isPausedForSeek) {
                boolean isPausedForUser = wrapper.isPausedForUser;
                boolean isPausedForSeek = wrapper.isPausedForSeek;

                if (isPausedForSeek) {
                    wrapper.needToSeek = true;
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Seek notifyToHandleWait start");
                    } else {
                        Log.w(TAG, "handleData() video Seek notifyToHandleWait start");
                    }
                } else if (isPausedForUser) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio User notifyToHandleWait start");
                    } else {
                        Log.w(TAG, "handleData() video User notifyToHandleWait start");
                    }
                } else {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Cache notifyToHandleWait start");
                    } else {
                        Log.w(TAG, "handleData() video Cache notifyToHandleWait start");
                    }
                }

                notifyToHandleWait(wrapper);

                if (wrapper.isPausedForSeek || wrapper.isPausedForUser) {
                    continue;
                }

                if (isPausedForSeek) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Seek notifyToHandleWait end");
                        if (mCallback != null) {
                            mCallback.onPlayed();
                        }
                    } else {
                        Log.w(TAG, "handleData() video Seek notifyToHandleWait end");
                    }
                } else if (isPausedForUser) {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio User notifyToHandleWait end");
                    } else {
                        Log.w(TAG, "handleData() video User notifyToHandleWait end");
                    }
                } else {
                    if (wrapper.type == TYPE_AUDIO) {
                        Log.d(TAG, "handleData() audio Cache notifyToHandleWait end");
                    } else {
                        Log.w(TAG, "handleData() video Cache notifyToHandleWait end");
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
                //readDataSize = wrapper.readData.size();
                /*if (wrapper.type == TYPE_AUDIO) {
                    Log.d(TAG, "handleData() audio readDataSize: " + readDataSize);
                } else {
                    Log.w(TAG, "handleData() video readDataSize: " + readDataSize);
                }*/
            }

            //Log.w(TAG, "handleData() video readDataSize: " + readDataSize);
            if (isReading &&
                    ((mVideoWrapper.isHandling && mVideoWrapper.readData.size() < mVideoWrapper.CACHE)
                            ||
                            (mAudioWrapper.isHandling && mAudioWrapper.readData.size() < mAudioWrapper.CACHE))) {
                //Log.d(TAG, "handleData() audio readDataSize: " + mAudioWrapper.readData.size());
                //Log.w(TAG, "handleData() video readDataSize: " + mVideoWrapper.readData.size());
                notifyToRead(mVideoWrapper);
            }

            if (data != null) {
                wrapper.data = data.data;
                wrapper.size = data.size;
                wrapper.sampleTime = data.presentationTimeUs;
                wrapper.cryptoInfo = data.cryptoInfo;
                wrapper.flags = data.flags;

                // 向MediaCodec喂数据
                if (!feedInputBufferAndDrainOutputBuffer(wrapper)) {
                    wrapper.isHandling = false;
                    /*if (wrapper.type == TYPE_AUDIO) {
                        mVideoWrapper.isHandling = false;
                    } else {
                        mAudioWrapper.isHandling = false;
                    }*/
                    break;
                }
            }

            if (wrapper.readData.size() > 0) {
                if (wrapper.type == TYPE_AUDIO) {
                    curElapsedTime = DateUtils.formatElapsedTime(
                            (wrapper.sampleTime / 1000) / 1000);
                    if (mCallback != null
                            // 防止重复更新
                            && !TextUtils.equals(curElapsedTime, preElapsedTime)) {
                        mCallback.onProgressUpdated(wrapper.sampleTime / 1000000);
                        preElapsedTime = curElapsedTime;
                        //Log.d(TAG, "handleData() curElapsedTime: " + curElapsedTime);
                    }
                }
            } else {
                if (isReading) {
                    notifyToRead(mVideoWrapper);
                    if (mCallback != null) {
                        if (wrapper.type == TYPE_AUDIO) {
                            Log.d(TAG, "handleData() audio onPaused");
                        } else {
                            Log.w(TAG, "handleData() video onPaused");
                        }
                        mCallback.onPaused();
                    }

                    if (wrapper.type == TYPE_AUDIO) {
                        /*if (mIsLocal && mUiHandler != null) {
                            mUiHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setProgressUs(mAudioWrapper.presentationTimeUs);
                                }
                            }, 200);
                        }*/
                        Log.e(TAG, "卧槽!卧槽!卧槽! audio太不给力了");
                    } else {
                        Log.e(TAG, "卧槽!卧槽!卧槽! video太不给力了");
                    }
                    mVideoWrapper.isPausedForCache = true;
                    mAudioWrapper.isPausedForCache = true;

                    notifyToHandleWait(wrapper);

                    mVideoWrapper.isPausedForCache = false;
                    mAudioWrapper.isPausedForCache = false;

                    if (wrapper.isPausedForSeek) {
                        continue;
                    }

                    if (wrapper.type == TYPE_AUDIO) {
                        //Log.d(TAG, "handleData() audio onPlayed");
                        notifyToHandle(mVideoWrapper);
                    } else {
                        //Log.w(TAG, "handleData() video onPlayed");
                        notifyToHandle(mAudioWrapper);
                    }
                    if (mCallback != null) {
                        mCallback.onPlayed();
                    }
                } else {
                    wrapper.isHandling = false;
                    break;
                }
            }
        }// for (; ; ) end
        if (wrapper.type == TYPE_AUDIO) {
            Log.d(TAG, "handleData() audio end");
        } else {
            Log.w(TAG, "handleData() video end");
        }

        isReading = false;
        mVideoWrapper.isPausedForSeek = false;
        mAudioWrapper.isPausedForSeek = false;
        notifyToRead();

        if (wrapper != null) {
            if (wrapper.type == TYPE_AUDIO) {
                mAudioWrapper.clear();
                //mAudioWrapper = null;
            } else {
                mVideoWrapper.clear();
                //mVideoWrapper = null;
            }
        }
        if (mVideoWrapper.trackIndex == -1
                && mAudioWrapper.trackIndex == -1) {
            mediaDataSource.safeExit();
            mediaDataSource = null;
            System.gc();
            //SystemClock.sleep(500);
            //internalDestroy();
            if (mCallback != null) {
                Log.w(TAG, "handleData() onFinished");
                mCallback.onFinished();
            }

            Log.i(TAG, "handleData() audio and video are finished");
            mVideoWrapper = null;
            mAudioWrapper = null;
        }
    }

    private void init() {
        /*mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimplePlayer.this.uiHandleMessage(msg);
            }
        };*/
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimplePlayer.this.threadHandleMessage(msg);
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        Log.i(TAG, "internalPrepare() start");

        /*if (mCallback != null) {
            Log.i(TAG, "onReady");
            mCallback.onReady();
        }*/

        mVideoWrapper = new VideoWrapper(TYPE_VIDEO);
        mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
        mVideoWrapper.clear();
        mAudioWrapper.clear();
        preElapsedTime = null;
        curElapsedTime = null;
        TIME_DIFFERENCE = 888;
        averageTimeDiff = 0;
        runCounts = 0;
        Arrays.fill(timeDiff, 0);
        mIsLocal = false;
        isReading = false;
        mVideoWrapper.isHandling = false;
        mAudioWrapper.isHandling = false;
        Log.i(TAG, "internalPrepare() video       CACHE: " + mVideoWrapper.CACHE);
        Log.i(TAG, "internalPrepare() audio       CACHE: " + mAudioWrapper.CACHE);
        Log.i(TAG, "internalPrepare() video CACHE_START: " + mVideoWrapper.CACHE_START);
        Log.i(TAG, "internalPrepare() audio CACHE_START: " + mAudioWrapper.CACHE_START);

        int trackCount = mExtractor.getTrackCount();
        Log.i(TAG, "internalPrepare() trackCount: " + trackCount);
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (TextUtils.isEmpty(mime)) {
                continue;
            }
            if (mime.startsWith("video/")) {
                mExtractor.selectTrack(i);
                mVideoWrapper.mime = mime;
                mVideoWrapper.trackIndex = i;
                mVideoWrapper.decoderMediaFormat = format;
            } else if (mime.startsWith("audio/")) {
                mExtractor.selectTrack(i);
                mAudioWrapper.mime = mime;
                mAudioWrapper.trackIndex = i;
                mAudioWrapper.decoderMediaFormat = format;
            } else if (mime.startsWith("text/")) {

            }
        }

        Log.i(TAG, "internalPrepare()" +
                " mVideoWrapper.trackIndex: " + mVideoWrapper.trackIndex +
                " mAudioWrapper.trackIndex: " + mAudioWrapper.trackIndex);
        if (mVideoWrapper.trackIndex == -1
                || mAudioWrapper.trackIndex == -1) {
            return false;
        }

        Log.w(TAG, "internalPrepare() video decoderMediaFormat:\n" +
                mVideoWrapper.decoderMediaFormat);
        Log.d(TAG, "internalPrepare() audio decoderMediaFormat:\n" +
                mAudioWrapper.decoderMediaFormat);

        int width = mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        if (mCallback != null) {
            mCallback.onChangeWindow(width, height);
        }

        Log.i(TAG, "internalPrepare() end");
        return true;
    }

    private boolean prepareVideo() {
        Log.w(TAG, "prepareVideo() start");

        // 可以设置
        /*mVideoWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);*/
        // 不能设置,不然画面出不来
        /*mVideoWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);*/

        // durationUs
        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mVideoWrapper.durationUs =
                    mVideoWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mVideoWrapper.durationUs / 1000) / 1000);
            Log.w(TAG, "prepareVideo()          videoDurationUs: " +
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
        if (!mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            // 4K 3110400
            mVideoWrapper.decoderMediaFormat.setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE, VIDEO_FRAME_MAX_LENGTH);
        }
        mVideoWrapper.frameMaxLength =
                mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        Log.w(TAG, "prepareVideo() video     frameMaxLength: " +
                mVideoWrapper.frameMaxLength);

        // OMX.MTK.VIDEO.DECODER.MPEG2.secure
        // OMX.google.raw.decoder
        String errorInfo = null;
        MediaCodecInfo[] mediaCodecInfos = MediaUtils.findAllDecodersByMime(mVideoWrapper.mime);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            try {
                mExtractor.selectTrack(mVideoWrapper.trackIndex);
                mVideoWrapper.decoderMediaCodec =
                        MediaCodec.createByCodecName(mediaCodecInfo.getName());
                mVideoWrapper.decoderMediaCodec.configure(
                        mVideoWrapper.decoderMediaFormat, mSurface, null, 0);
                mVideoWrapper.decoderMediaCodec.start();
                mVideoWrapper.render = true;
                Log.i(TAG, "prepareVideo() CodecName: " + mediaCodecInfo.getName());
                break;
            } catch (NullPointerException
                    | IllegalArgumentException
                    | MediaCodec.CodecException
                    | IOException e) {
                e.printStackTrace();
                errorInfo = e.toString();
                MediaUtils.releaseMediaCodec(mVideoWrapper.decoderMediaCodec);
                mVideoWrapper.decoderMediaCodec = null;
                continue;
            }
        }
        if (mVideoWrapper.decoderMediaCodec == null) {
            Log.e(TAG, "prepareVideo() create Video MediaCodec failure");
            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
            internalRelease();
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, errorInfo);
            }
            return false;
        }

        Log.w(TAG, "prepareVideo() end");
        return true;
    }

    private boolean prepareAudio() {
        Log.d(TAG, "prepareAudio() start");

        // durationUs
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mAudioWrapper.durationUs =
                    mAudioWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mAudioWrapper.durationUs / 1000) / 1000);
            Log.d(TAG, "prepareAudio()          audioDurationUs: " +
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
        if (!mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mAudioWrapper.decoderMediaFormat.setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_FRAME_MAX_LENGTH);
        }
        mAudioWrapper.frameMaxLength =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        Log.d(TAG, "prepareAudio() audio     frameMaxLength: " +
                mAudioWrapper.frameMaxLength);
        String errorInfo = null;
        // 创建音频解码器
        try {
            mExtractor.selectTrack(mAudioWrapper.trackIndex);
            // audio/vorbis
            Log.d(TAG, "prepareAudio() audio mAudioWrapper.mime: " + mAudioWrapper.mime);
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
                        Log.d(TAG, "prepareAudio() audio          codecName: " + codecName);
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
            errorInfo = e.toString();
            if (mAudioWrapper.decoderMediaCodec != null) {
                mAudioWrapper.decoderMediaCodec.release();
                mAudioWrapper.decoderMediaCodec = null;
            }
        }
        if (mAudioWrapper.decoderMediaCodec == null) {
            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
            internalRelease();
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, errorInfo);
            }
            return false;
        }

        Log.d(TAG, "prepareAudio() end");
        return true;
    }

    private void internalPlay() {
        if (mAudioWrapper != null
                && mVideoWrapper != null
                && !mAudioWrapper.isPausedForCache
                && !mVideoWrapper.isPausedForCache) {
            mAudioWrapper.isPausedForUser = false;
            mVideoWrapper.isPausedForUser = false;
            notifyToHandle(mAudioWrapper);
            notifyToHandle(mVideoWrapper);
        }
    }

    private void internalPause() {
        if (mAudioWrapper != null
                && mVideoWrapper != null
                && !mAudioWrapper.isPausedForCache
                && !mVideoWrapper.isPausedForCache) {
            mAudioWrapper.isPausedForUser = true;
            mVideoWrapper.isPausedForUser = true;
        }
    }

    private void internalRelease() {
        Log.i(TAG, "internalRelease() start");
        //notifyAudioEndOfStream();
        //notifyVideoEndOfStream();
        if (mediaDataSource != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaDataSource.safeExit();
            }
        }
        if (mVideoWrapper != null && mAudioWrapper != null) {
            isEOF = true;
            isReading = false;
            mVideoWrapper.isPausedForSeek = false;
            mAudioWrapper.isPausedForSeek = false;
            notifyToRead();
            mVideoWrapper.isHandling = false;
            mVideoWrapper.isPausedForUser = false;
            mVideoWrapper.isPausedForCache = false;
            mAudioWrapper.isHandling = false;
            mAudioWrapper.isPausedForUser = false;
            mAudioWrapper.isPausedForCache = false;
            notifyToRead(mVideoWrapper);
            notifyToRead(mAudioWrapper);
            notifyToHandle(mVideoWrapper);
            notifyToHandle(mAudioWrapper);
        }
        Log.i(TAG, "internalRelease() end");
    }

    private void internalDestroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    private boolean feedInputBufferAndDrainOutputBuffer(SimpleWrapper wrapper) {
        if (!wrapper.isHandling) {
            return false;
        }

        // seek的时候就不要往下走了
        if (wrapper.needToSeek) {
            return true;
        }

        EDMediaCodec.TYPE type;

        if (wrapper.type == TYPE_VIDEO) {
            type = EDMediaCodec.TYPE.TYPE_VIDEO;
        } else if (wrapper.type == TYPE_AUDIO) {
            type = EDMediaCodec.TYPE.TYPE_AUDIO;
        } else {
            return true;
        }

        return EDMediaCodec.feedInputBufferAndDrainOutputBuffer(
                mCodecCallback,
                type,
                wrapper.decoderMediaCodec,
                wrapper.cryptoInfo,
                wrapper.data,
                0,
                wrapper.size,
                wrapper.sampleTime,
                wrapper.flags,
                wrapper.render,
                true);
    }

    private String preElapsedTime = null;
    private String curElapsedTime = null;
    //private boolean onlyOneStart = true;
    //private boolean mIsSeeked = false;
    //private int mCountWithSeek = 0;
    private long prePresentationTimeUs;
    private static final int RUN_COUNTS = 100;
    private int[] timeDiff = new int[RUN_COUNTS];
    private int TIME_DIFFERENCE = 888;// 150
    private int runCounts = 0;
    private int averageTimeDiff = 0;

    private void hope_to_get_a_good_result() {
        Log.i(TAG, "hope_to_get_a_good_result() averageTimeDiff: " + averageTimeDiff);
        if (averageTimeDiff > 1000) {
            /***
             1179 1212 1228 1251 1275 1440
             */
            TIME_DIFFERENCE = 600;
        } else if (averageTimeDiff > 900 && averageTimeDiff <= 1000) {
            /***
             932
             */
            TIME_DIFFERENCE = 500;
        } else if (averageTimeDiff > 800 && averageTimeDiff <= 900) {
            /***
             842 880
             */
            TIME_DIFFERENCE = 400;
        } else if (averageTimeDiff > 700 && averageTimeDiff <= 800) {
            TIME_DIFFERENCE = 400;
        } else if (averageTimeDiff > 600 && averageTimeDiff <= 700) {
            /***
             668
             */
            TIME_DIFFERENCE = 400;
        } else if (averageTimeDiff > 500 && averageTimeDiff <= 600) {
            TIME_DIFFERENCE = 300;
        } else if (averageTimeDiff > 400 && averageTimeDiff <= 500) {
            /***
             490
             */
            TIME_DIFFERENCE = 200;
        } else if (averageTimeDiff > 300 && averageTimeDiff <= 400) {
            /***
             342
             */
            TIME_DIFFERENCE = 100;
        } else if (averageTimeDiff > 200 && averageTimeDiff <= 300) {
            /***
             216 220
             */
            TIME_DIFFERENCE = 200;
        } else if (averageTimeDiff > 100 && averageTimeDiff <= 200) {
            /***
             106 129 136 145 178
             */
            TIME_DIFFERENCE = 100;
        } else if (averageTimeDiff <= 100) {
            /***
             16 28 100
             */
            TIME_DIFFERENCE = 50;
        }
        Log.i(TAG, "hope_to_get_a_good_result() TIME_DIFFERENCE: " + TIME_DIFFERENCE);

        /*if (mCallback != null) {
            mCallback.onInfo(String.valueOf(TIME_DIFFERENCE));
        }*/
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

    private EDMediaCodec.Callback mCodecCallback = new EDMediaCodec.Callback() {
        @Override
        public boolean isVideoFinished() {
            return !mVideoWrapper.isHandling;
        }

        @Override
        public boolean isAudioFinished() {
            return !mAudioWrapper.isHandling;
        }

        @Override
        public void handleVideoOutputFormat(MediaFormat mediaFormat) {
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
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                Log.d(TAG, "handleVideoOutputFormat() newMediaFormat:\n" +
                        mVideoWrapper.decoderMediaFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mVideoWrapper.isStarted = true;
            Log.w(TAG, "handleVideoOutputFormat() 画面马上输出......");
        }

        @Override
        public void handleAudioOutputFormat(MediaFormat mediaFormat) {
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
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                Log.d(TAG, "handleAudioOutputFormat() newMediaFormat:\n" +
                        mAudioWrapper.decoderMediaFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mAudioWrapper.mAudioTrack != null) {
                mAudioWrapper.mAudioTrack.release();
            }

            /***
             {sample-rate=48000, track-id=2, mime=audio/raw, profile=2, bitrate=70288,
             aac-profile=2, is-dual-mono=0, pcm-encoding=2, durationUs=0, is-adts=1,
             channel-count=1, mime-old=audio/mp4a-latm, max-input-size=102400, group=1024,
             csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2]}

             {sample-rate=48000, track-id=2, mime=audio/raw, profile=2, bitrate=223376,
             aac-profile=2, is-dual-mono=0, pcm-encoding=2, durationUs=0, is-adts=1,
             channel-count=2, mime-old=audio/mp4a-latm, max-input-size=102400, group=1024,
             csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2]}
             */

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
            Log.d(TAG, "handleAudioOutputFormat()" +
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
                    boolean isMute = false;
                    if (!isMute) {
                        setVolume(VOLUME_NORMAL);
                    } else {
                        setVolume(VOLUME_MUTE);
                    }
                } else {
                    setVolume(VOLUME_NORMAL);
                }
                mAudioWrapper.mAudioTrack.play();
                mAudioWrapper.isStarted = true;
                Log.d(TAG, "handleAudioOutputFormat() 声音马上输出......");
            } else {
                Log.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
                mAudioWrapper.isHandling = false;
                notifyToRead(mAudioWrapper);
                notifyToHandle(mAudioWrapper);
            }

            mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);

            // 音视频都已经开始的话,就可以播放了
            if (mCallback != null) {
                Log.i(TAG, "onPlayed");
                mCallback.onPlayed();
            }
        }

        @Override
        public int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {
            if (room == null || roomInfo == null) {
                return -1;
            }

            mVideoWrapper.presentationTimeUs = roomInfo.presentationTimeUs;

            if (mVideoWrapper.presentationTimeUs > 0
                    && mAudioWrapper.presentationTimeUs > 0) {
                int diffTime =
                        (int) ((mVideoWrapper.presentationTimeUs - mAudioWrapper
                                .presentationTimeUs) / 1000);
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
                    averageTimeDiff = totleTimeDiff / RUN_COUNTS;
                    // 希望得到一个好的TIME_DIFFERENCE值
                    hope_to_get_a_good_result();

                }
                if (diffTime < 0) {
                    return 0;
                }
                if (diffTime > averageTimeDiff) {
                    mVideoWrapper.presentationTimeUs =
                            mAudioWrapper.presentationTimeUs + averageTimeDiff * 1000;
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
                            (int) ((mVideoWrapper.presentationTimeUs - mAudioWrapper
                                    .presentationTimeUs) / 1000);
                }
            }

            SystemClock.sleep(11);

            return 0;
        }

        @Override
        public int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {
            if (room == null || roomInfo == null) {
                return -1;
            }

            mAudioWrapper.presentationTimeUs = roomInfo.presentationTimeUs;
            if (mVideoWrapper.isHandling
                    && mVideoWrapper.presentationTimeUs > 0
                    && mAudioWrapper.presentationTimeUs > 0) {
                int diffTime =
                        (int) ((mAudioWrapper.presentationTimeUs - mVideoWrapper
                                .presentationTimeUs) / 1000);
                while (diffTime > 0) {
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
                            (int) ((mAudioWrapper.presentationTimeUs - mVideoWrapper
                                    .presentationTimeUs) / 1000);
                }
            }

            // 输出音频
            if (mAudioWrapper.isHandling
                    && mAudioWrapper.mAudioTrack != null
                    && mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                byte[] audioData = new byte[roomSize];
                room.get(audioData, 0, audioData.length);
                mAudioWrapper.mAudioTrack.write(audioData, roomInfo.offset, audioData.length);
            }

            return 0;
        }
    };

}
