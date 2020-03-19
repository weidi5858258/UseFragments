package com.weidi.usefragments.business.video_player;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
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
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;
import com.weidi.usefragments.tool.MLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/***

 */

public class OnlyVideoPlayer {

    private static final String TAG =
            OnlyVideoPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final int MAX_CACHE_VIDEO_COUNT = 500;
    private static final int START_CACHE_COUNT_HTTP = 3000;
    private static final int START_CACHE_COUNT_LOCAL = 100;

    // 只是设置为默认值
    // 音频一帧的大小不能超过这个值,不然出错(如果设成1024 * 1024会有杂音,不能过大,调查了好久才发现跟这个有关)
    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;
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
    private boolean mIsLocal = false;
    private MediaExtractor extractor = null;

    private Handler mUiHandler = null;
    private Handler mThreadHandler = null;
    private HandlerThread mHandlerThread = null;
    private com.weidi.usefragments.tool.Callback mCallback = null;
    private VideoWrapper mVideoWrapper = new VideoWrapper(TYPE_VIDEO);

    private static class SimpleWrapper {
        public String mime = null;
        public MediaExtractor extractor = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        // 是否需要渲染图像(播放音频为false,播放视频为true)
        public boolean render = false;
        public int trackIndex = -1;
        // 使用于while条件判断
        public boolean isReading = false;
        // 使用于while条件判断
        public boolean isHandling = false;
        // readData用于缓存数据,handleData用于处理数据
        public ArrayList<AVPacket> readData = null;
        public ArrayList<AVPacket> handleData = null;
        public boolean handleDataFull = false;

        public int CACHE;
        public int CACHE_START;
        // 用于标识音频还是视频
        public int type;
        // 总时长
        public long durationUs = 0;
        // 播放的时长(下面两个参数一起做的事是每隔一秒发一次回调函数)
        public long presentationTimeUs1 = 0;
        public long startTimeUs = 0;

        // 使用于时间戳
        public long presentationTimeUs2 = 0;

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

        private SimpleWrapper() {
        }

        public SimpleWrapper(int type) {
            switch (type) {
                case TYPE_VIDEO:
                    this.type = TYPE_VIDEO;
                    CACHE = MAX_CACHE_VIDEO_COUNT;
                    frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
                    readData = new ArrayList<AVPacket>(CACHE);
                    handleData = new ArrayList<AVPacket>(CACHE);
                    break;
                default:
                    break;
            }
        }

        public void clear() {
            mime = null;
            extractor = null;
            decoderMediaCodec = null;
            decoderMediaFormat = null;
            render = false;
            trackIndex = -1;
            isReading = false;
            isHandling = false;
            isStarted = false;
            isPausedForUser = false;
            isPausedForCache = false;
            isPausedForSeek = false;
            readData.clear();
            handleData.clear();
            durationUs = 0;
            presentationTimeUs1 = 0;
            startTimeUs = 0;
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
            frameMaxLength = 0;
            frameDataLength = 0;
        }
    }

    private static class VideoWrapper extends SimpleWrapper {
        public Surface mSurface = null;

        private VideoWrapper() {
        }

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

    public OnlyVideoPlayer() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
        registerHeadsetPlugReceiver();
    }

    public void setPath(String path) {
        // mPath = Contents.getUri();
        mPath = path;
        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
    }

    public void setSurface(Surface surface) {
        mVideoWrapper.mSurface = surface;
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
                || progressUs > mVideoWrapper.durationUs) {
            return;
        }

        mVideoWrapper.isPausedForSeek = true;
        notifyToHandle(mVideoWrapper);

        mIsSeeked = true;
        mProgressUs = progressUs;
        mVideoWrapper.durationUs = progressUs;
        notifyToRead(mVideoWrapper);
    }

    private void seekTo() {
        while (!mVideoWrapper.needToSeek) {
            SystemClock.sleep(1);
        }
        MLog.i(TAG, "seekTo() start");
        mVideoWrapper.needToSeek = false;
        mVideoWrapper.isPausedForSeek = false;
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

        synchronized (mVideoWrapper.readData) {
            mVideoWrapper.readData.clear();
        }
        // SEEK_TO_PREVIOUS_SYNC SEEK_TO_CLOSEST_SYNC SEEK_TO_NEXT_SYNC
        extractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        MLog.i(TAG, "seekTo() end");
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
        internalRelease();
        mThreadHandler.removeMessages(MSG_RELEASE);
        mThreadHandler.sendEmptyMessage(MSG_RELEASE);
    }

    public boolean isPlaying() {
        return mVideoWrapper.isStarted
                && !mVideoWrapper.isPausedForUser
                && !mVideoWrapper.isPausedForCache;
    }

    public boolean isRunning() {
        return mVideoWrapper.isStarted && mVideoWrapper.isHandling;
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
                    if (prepareVideo()) {
                        new Thread(mVideoHandleData).start();
                        SystemClock.sleep(500);
                        new Thread(mReadData).start();
                    } else {
                        if (mCallback != null) {
                            MLog.e(TAG, "onError prepareVideo()");
                            mCallback.onError(0,null);
                        }
                    }
                } else {
                    if (mCallback != null) {
                        MLog.e(TAG, "onError internalPrepare()");
                        mCallback.onError(0,null);
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

                    if (wrapper.decoderMediaCodec != null) {
                        wrapper.decoderMediaCodec.release();
                    }

                    if (wrapper.extractor != null) {
                        wrapper.extractor.release();
                    }

                    wrapper.clear();

                    if (!mVideoWrapper.isHandling) {
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
                        step = (int) ((mVideoWrapper.presentationTimeUs2
                                / (mVideoWrapper.durationUs * 1.00)) * 3840.00);
                        Log.d(TAG, "onKeuiHandleMessageyDown() step: " + step);
                    }
                    step += 100;
                    long progress = (long) (((step / 3840.00) * mVideoWrapper.durationUs));
                    setProgressUs(progress);
                } else {
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() 1");
                    if (!mVideoWrapper.isPausedForCache) {
                        if (!mVideoWrapper.isPausedForUser
                                && !mVideoWrapper.isPausedForUser) {
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
    private void readData() {
        MLog.i(TAG, "readData() start");

        int readSize = -1;
        int trackIndex = -1;
        ByteBuffer room = ByteBuffer.allocate(mVideoWrapper.frameMaxLength);
        byte[] buffer = new byte[mVideoWrapper.frameMaxLength];

        while (mVideoWrapper.isReading) {
            try {
                // seekTo
                if (mVideoWrapper.isPausedForSeek) {
                    seekTo();
                }

                // wrapper.extractor ---> room
                // readSize为实际读到的大小(音视频一帧的大小),其值可能远远小于room的大小
                room.clear();
                readSize = extractor.readSampleData(room, 0);
                trackIndex = extractor.getSampleTrackIndex();

                if (readSize < 0) {
                    // 没有数据可读了,结束
                    // 也有可能是底层异常引起
                    // MediaHTTPConnection:
                    // readAt 1033700796 / 32768 => java.net.ProtocolException:
                    // unexpected end of stream
                    MLog.e(TAG, "readData() end      readSize: " + readSize);
                    MLog.e(TAG, "readData() end videoDataSize: " + mVideoWrapper.readData.size());

                    mVideoWrapper.isReading = false;
                    // 开启任务处理数据(如果还没有开启任务过的话)
                    // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                    if (!mVideoWrapper.isPausedForUser) {
                        notifyToHandle(mVideoWrapper);
                    }

                    MLog.e(TAG, "readData() notifyToReadWait start");
                    // 需要seek,不能退出
                    notifyToReadWait(mVideoWrapper);
                    MLog.e(TAG, "readData() notifyToReadWait end");
                    if (mVideoWrapper.isPausedForSeek) {
                        mVideoWrapper.isReading = true;
                        continue;
                    } else {
                        break;
                    }
                }// readSize < 0

                Arrays.fill(buffer, (byte) 0);
                // room ---> buffer
                room.get(buffer, 0, readSize);
                // 作为解码时的时间戳传进去
                AVPacket data = new AVPacket(readSize, extractor.getSampleTime());
                System.arraycopy(buffer, 0, data.data, 0, readSize);

                if (trackIndex == mVideoWrapper.trackIndex) {
                    synchronized (mVideoWrapper.readData) {
                        mVideoWrapper.readData.add(data);
                    }
                    int size = mVideoWrapper.readData.size();
                    // 只有当video达到一定数量级时才暂停
                    if (size == mVideoWrapper.CACHE) {
                        MLog.i(TAG, "readData() videoDataSize: " + mVideoWrapper.readData.size());
                        MLog.i(TAG, "readData() notifyToReadWait start");
                        System.gc();
                        notifyToReadWait(mVideoWrapper);
                        if (mVideoWrapper.isPausedForSeek) {
                            MLog.i(TAG, "readData() notifyToReadWait end for Seek");
                            continue;
                        }
                        MLog.i(TAG, "readData() notifyToReadWait end");
                    } else if (!mVideoWrapper.handleDataFull
                            && size == mVideoWrapper.CACHE_START) {
                        // 通知开始播放
                        synchronized (mVideoWrapper.readData) {
                            mVideoWrapper.handleData.clear();
                            mVideoWrapper.handleData.addAll(mVideoWrapper.readData);
                            mVideoWrapper.readData.clear();
                            mVideoWrapper.handleDataFull = true;
                        }
                        MLog.i(TAG, "readData() video notifyToHandle");
                        notifyToHandle(mVideoWrapper);
                    }
                }

                if (mVideoWrapper.isReading) {
                    // 跳到下一帧
                    extractor.advance();
                }
            } catch (Exception e) {
                e.printStackTrace();
                mVideoWrapper.isReading = false;
                break;
            }
        }// while(...) end

        MLog.i(TAG, "readData() end");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void handleData(SimpleWrapper wrapper) {
        MLog.w(TAG, "handleData() video notifyToHandleWait start");
        notifyToHandleWait(wrapper);
        MLog.w(TAG, "handleData() video notifyToHandleWait end");
        if (!wrapper.isHandling) {
            return;
        }

        MLog.w(TAG, "handleData() video start");
        while (wrapper.isHandling) {
            // pause装置
            if (wrapper.isPausedForUser
                    || wrapper.isPausedForSeek) {
                boolean isPausedForSeek = wrapper.isPausedForSeek;

                if (isPausedForSeek) {
                    wrapper.handleDataFull = false;
                    wrapper.handleData.clear();
                    wrapper.needToSeek = true;
                    MLog.w(TAG, "handleData() video Seek notifyToHandleWait start");
                } else {
                    MLog.w(TAG, "handleData() video User notifyToHandleWait start");
                }

                notifyToHandleWait(wrapper);

                if (wrapper.isPausedForSeek || wrapper.isPausedForUser) {
                    continue;
                }

                if (isPausedForSeek) {
                    MLog.w(TAG, "handleData() video Seek notifyToHandleWait end");
                } else {
                    MLog.w(TAG, "handleData() video User notifyToHandleWait end");
                }
            }// pause end

            if (wrapper.isHandling && !wrapper.handleData.isEmpty()) {
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

                if (wrapper.type == TYPE_AUDIO) {
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
                    notifyToRead(wrapper);
                } else {
                    if (mVideoWrapper.isReading) {
                        if (mCallback != null) {
                            MLog.w(TAG, "handleData() video onPaused");
                            mCallback.onPaused();
                        }

                        MLog.e(TAG, "卧槽!卧槽!卧槽! video太不给力了");
                        mVideoWrapper.isPausedForCache = true;
                        notifyToHandleWait(wrapper);
                        mVideoWrapper.isPausedForCache = false;

                        if (wrapper.isPausedForSeek) {
                            continue;
                        }

                        if (mCallback != null) {
                            MLog.w(TAG, "handleData() video onPlayed");
                            mCallback.onPlayed();
                        }
                    } else {
                        wrapper.isHandling = false;
                        break;
                    }
                }
            }// empty的情况
        }// while(true) end
        MLog.w(TAG, "handleData() video end");

        if (wrapper.decoderMediaCodec != null) {
            wrapper.decoderMediaCodec.release();
            wrapper.decoderMediaCodec = null;
        }

        if (wrapper.extractor != null) {
            wrapper.extractor.release();
            wrapper.extractor = null;
        }

        if (!mVideoWrapper.isHandling) {
            // 让"读线程"结束
            notifyToRead(mVideoWrapper);

            mVideoWrapper.clear();
            if (mCallback != null) {
                MLog.w(TAG, "handleData() onFinished");
                mCallback.onFinished();
            }

            mVideoWrapper = null;
            internalDestroy();
            System.gc();
        }
    }

    private void init() {
        EventBusUtils.register(this);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                OnlyVideoPlayer.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                OnlyVideoPlayer.this.uiHandleMessage(msg);
            }
        };
    }

    private boolean prepareVideo() {
        // Video
        MLog.w(TAG, "prepareVideo() start");
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
        MLog.w(TAG, "internalPrepare() video decoderMediaFormat: " +
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
        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mVideoWrapper.durationUs =
                    mVideoWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mVideoWrapper.durationUs / 1000) / 1000);
            MLog.w(TAG, "internalPrepare()          videoDurationUs: " +
                    mVideoWrapper.durationUs + " " + durationTime);
        }
        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mVideoWrapper.frameMaxLength =
                    mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }
        MLog.w(TAG, "internalPrepare() video     frameMaxLength: " +
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
            MLog.w(TAG, "internalPrepare() video              csd-0: " + sb.toString());
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
            MLog.w(TAG, "internalPrepare() video              csd-1: " + sb.toString());
        }
        MLog.w(TAG, "prepareVideo() end");

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            return false;
        }
        MLog.i(TAG, "internalPrepare() start");

        /*mContext.bindService(new Intent(mContext, MediaDataService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE);*/

        //onlyOneStart = true;
        preElapsedTime = null;
        curElapsedTime = null;
        mVideoWrapper.clear();
        mVideoWrapper.isReading = true;
        mVideoWrapper.isHandling = true;

        mIsLocal = false;
        mVideoWrapper.CACHE_START = START_CACHE_COUNT_HTTP;
        if (!mPath.startsWith("http://")
                && !mPath.startsWith("HTTP://")
                && !mPath.startsWith("https://")
                && !mPath.startsWith("HTTPS://")) {
            File file = new File(mPath);
            if (!file.canRead()
                    || file.isDirectory()) {
                if (DEBUG)
                    MLog.e(TAG, "不能读取此文件: " + mPath);
                internalRelease();
                return false;
            }
            long fileSize = file.length();
            if (DEBUG)
                MLog.i(TAG, "internalPrepare() fileSize: " + fileSize);
            mVideoWrapper.CACHE_START = START_CACHE_COUNT_LOCAL;
            mIsLocal = true;
        }

        String PATH = null;
        PATH = "/data/data/com.weidi.usefragments/files/Movies/";
        PATH = "/storage/37C8-3904/Android/data/com.weidi.usefragments/files/Movies";
        PATH = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";
        mVideoWrapper.savePath = PATH + "video.h264";

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
            if (mime.startsWith("video/")) {
                mVideoWrapper.mime = mime;
                mVideoWrapper.trackIndex = i;
                mVideoWrapper.decoderMediaFormat = format;
                break;
            }
        }
        if (TextUtils.isEmpty(mVideoWrapper.mime)
                || mVideoWrapper.trackIndex == -1
                || mVideoWrapper.decoderMediaFormat == null) {
            internalRelease();
            return false;
        }

        MLog.i(TAG, "internalPrepare() end");

        return true;
    }

    private void printInfo() {
        mThreadHandler.removeMessages(MSG_HANDLE_DATA_LOCK_WAIT);
    }

    private void internalStart() {

    }

    private void internalPlay() {
        if (!mVideoWrapper.isPausedForCache) {
            mVideoWrapper.isPausedForUser = false;
            notifyToHandle(mVideoWrapper);
        }
    }

    private void internalPause() {
        if (!mVideoWrapper.isPausedForCache) {
            mVideoWrapper.isPausedForUser = true;
        }
    }

    private void internalStop() {

    }

    private void internalRelease() {
        MLog.d(TAG, "internalRelease() start");
        mVideoWrapper.isReading = false;
        mVideoWrapper.isHandling = false;
        mVideoWrapper.isPausedForUser = false;
        mVideoWrapper.isPausedForCache = false;
        mVideoWrapper.isPausedForSeek = false;
        notifyToRead(mVideoWrapper);
        notifyToHandle(mVideoWrapper);
        MLog.d(TAG, "internalRelease() end");
    }

    private void internalDestroy() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }

        //mContext.unbindService(serviceConnection);
        unregisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
    }

    private void internalPrev() {

    }

    private void internalNext() {

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
                if (room != null) {
                    // audio
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

        // 互相等待,一起start
        if (wrapper.type == TYPE_AUDIO) {
            // 如果是音频先走到这里,那么等待视频的开始
            while (wrapper.isStarted && !mVideoWrapper.isStarted) {
                if (!wrapper.isHandling) {
                    return false;
                }
                SystemClock.sleep(10);
            }
        } else {
            // 如果是视频先走到这里,那么等待音频的开始
            /*while (!mAudioWrapper.isStarted) {
                if (!wrapper.isHandling) {
                    return false;
                }
                SystemClock.sleep(10);
            }*/
            wrapper.isStarted = true;
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
        MediaFormat newMediaFormat = mVideoWrapper.decoderMediaCodec.getOutputFormat();
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        MLog.w(TAG, "handleVideoOutputFormat() newMediaFormat: " +
                mVideoWrapper.decoderMediaFormat);
        MLog.w(TAG, "handleVideoOutputFormat() 画面马上输出......");

        // 音视频都已经开始的话,就可以播放了
        if (mCallback != null) {
            MLog.i(TAG, "onPlayed");
            mCallback.onPlayed();
        }
    }

    private boolean mIsSeeked = false;
    private int mCountWithSeek = 0;
    private long prePresentationTimeUs;

    // 最最最关键的一步
    private int handleVideoOutputBuffer(
            int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
        mVideoWrapper.presentationTimeUs2 = roomInfo.presentationTimeUs;
        /*MLog.w(TAG, "handleVideoOutputBuffer() 时间差: " +
                (mVideoWrapper.presentationTimeUs2-prePresentationTimeUs));*/
        /*int diffTime = (int)((mVideoWrapper.presentationTimeUs2 - prePresentationTimeUs)/1000);
        SystemClock.sleep(diffTime);
        prePresentationTimeUs = mVideoWrapper.presentationTimeUs2;*/

        // test

        // 不能执行,不然很卡顿
        /*if (roomInfo.presentationTimeUs / 1000
                - mAudioWrapper.presentationTimeUs2 / 1000 < 0) {
            return;
        }*/

        /*long timeUs = mVideoWrapper.presentationTimeUs2 / 1000
                - mAudioWrapper.presentationTimeUs2 / 1000;
        MLog.w(TAG, "handleVideoOutputBuffer() videoUs - audioUs: " + timeUs);
        if (mIsSeeked && (timeUs > 1000 || timeUs < 0)) {
            // MLog.e(TAG, "handleVideoOutputBuffer() videoUs - audioUs: " + timeUs);
            if (++mCountWithSeek <= 10) {
                mVideoWrapper.presentationTimeUs2 = mAudioWrapper.presentationTimeUs2;
            } else {
                mIsSeeked = false;
                mCountWithSeek = 0;
            }
            return -1;
        }*/

        /*while (mAudioWrapper.isHandling
                && mVideoWrapper.isHandling
                && (mVideoWrapper.presentationTimeUs2 / 1000
                - mAudioWrapper.presentationTimeUs2 / 1000 >= 650)) {
            if (mVideoWrapper.isPausedForSeek) {
                return 0;
            }
            SystemClock.sleep(1);
        }*/

        return 0;
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

    private Runnable mVideoHandleData = new Runnable() {
        @Override
        public void run() {
            handleData(mVideoWrapper);
        }
    };

    private Runnable mReadData = new Runnable() {
        @Override
        public void run() {
            readData();
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
