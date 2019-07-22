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
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/***
 Created by weidi on 2019/7/10.

 现在有的问题:
 视频:
 1.花屏
 2.播放速度过快

 */

public class SampleVideoPlayer6 {

    private static final String TAG =
            SampleVideoPlayer6.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int AUDIO_CACHE = 1024 * 1024 * 1;
    private static final int VIDEO_CACHE = 1024 * 1024 * 8;
    // 音频一帧的大小不能超过这个值,不然出错(如果设成1024 * 1024会有杂音,不能过大,调查了好久才发现跟这个有关)
    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;
    // 视频一帧的大小不能超过这个值,不然出错
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024;

    private static final int TYPE_AUDIO = 0x0001;
    private static final int TYPE_VIDEO = 0x0002;

    private static final int TIME_OUT = 10000;
    private static final int PREPARE = 0x0001;
    private static final int PLAY = 0x0002;
    private static final int PAUSE = 0x0003;
    private static final int STOP = 0x0004;
    private static final int RELEASE = 0x0005;
    private static final int PREV = 0x0006;
    private static final int NEXT = 0x0007;

    private static final int READ_UNKNOW = 0x0001;
    private static final int READ_STARTED = 0x0002;
    private static final int READ_PAUSED = 0x0003;
    private static final int READ_FINISHED = 0x0004;
    private static final int READ_ERROR = 0x0005;

    // 为了注册广播
    private Context mContext;
    private String mPath;
    private float mVolume = 1.0f;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private SimpleWrapper mAudioWrapper = new SimpleWrapper(TYPE_AUDIO);
    private SimpleWrapper mVideoWrapper = new SimpleWrapper(TYPE_VIDEO);

    private static final int HEADER_FLAG_LENGTH = 4;
    private static final byte[] HEADER_FLAG1 = new byte[]{88, 88, 88, 88};
    private static final byte[] HEADER_FLAG2 = new byte[]{-88, -88, -88, -88};

    private static class SimpleWrapper {
        public String mime = null;
        public MediaExtractor mExtractor = null;
        public MediaCodec mDecoderMediaCodec = null;
        public MediaFormat mDecoderMediaFormat = null;
        public AudioTrack mAudioTrack = null;
        public Surface mSurface = null;
        public boolean render = false;
        public int mTrackIndex = -1;
        // while
        public boolean mIsReading = false;
        // while
        public boolean mIsHandling = false;
        public byte[] mData1 = null;
        public byte[] mData2 = null;
        public ArrayMap<Long, Long> mTime1 = new ArrayMap<Long, Long>();
        public ArrayMap<Long, Long> mTime2 = new ArrayMap<Long, Long>();
        // mData1中保存的实际数据大小(包含了标志位长度)
        public int readDataSize = 0;
        public int frameMaxLength = 0;
        public int mReadStatus = READ_UNKNOW;
        // "块"的概念,一"块"有N个字节.
        // 编码时接收的数据就是一"块"的大小,少一个字节或者多一个字节都会出现异常
        public long mReadFrameCounts = 0;
        // 处理了多少"块"
        public long mHandleFrameCounts = 0;
        // 使用readSampleData(...)方法总共读了多少字节
        public long mReadFrameLengthTotal = 0;
        // 处理了多少字节
        public long mHandleFrameLengthTotal = 0;
        public int CACHE;
        public int mType;
        public MediaUtils.Callback callback;

        public Object mReadDataLock = new Object();
        public Object mHandleDataLock = new Object();

        public SimpleWrapper(int type) {
            switch (type) {
                case TYPE_AUDIO:
                    CACHE = AUDIO_CACHE;
                    mType = TYPE_AUDIO;
                    mData1 = new byte[AUDIO_CACHE];
                    mData2 = new byte[AUDIO_CACHE];
                    frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
                    break;
                case TYPE_VIDEO:
                    CACHE = VIDEO_CACHE;
                    mType = TYPE_VIDEO;
                    mData1 = new byte[VIDEO_CACHE];
                    mData2 = new byte[VIDEO_CACHE];
                    frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
                    break;
                default:
                    break;
            }
        }

        public void clear() {
            mime = null;
            mExtractor = null;
            mDecoderMediaCodec = null;
            mDecoderMediaFormat = null;
            mAudioTrack = null;
            render = false;
            mTrackIndex = -1;
            mIsReading = false;
            mIsHandling = false;
            Arrays.fill(mData1, (byte) 0);
            Arrays.fill(mData2, (byte) 0);
            mTime1.clear();
            mTime2.clear();
            readDataSize = 0;
            mReadStatus = READ_UNKNOW;
            mReadFrameCounts = 0;
            mHandleFrameCounts = 0;
            mReadFrameLengthTotal = 0;
            mHandleFrameLengthTotal = 0;
        }
    }

    public SampleVideoPlayer6() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
        //registerHeadsetPlugReceiver();
    }

    public void setPath(String path) {
        mPath = path;
        mPath = Contents.getUri();
        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
    }

    public void setSurface(Surface surface) {
        mVideoWrapper.mSurface = surface;
    }

    public void play() {
        // mThreadHandler.removeMessages(PLAY);
        // mThreadHandler.sendEmptyMessage(PLAY);

        mThreadHandler.removeMessages(PREPARE);
        mThreadHandler.sendEmptyMessage(PREPARE);
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
                && mAudioWrapper.mDecoderMediaCodec == null
                && mVideoWrapper.mDecoderMediaCodec == null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }

        unregisterHeadsetPlugReceiver();
        EventBusUtils.unregister(this);
    }

    public boolean isRunning() {
        return mAudioWrapper.mIsHandling && mVideoWrapper.mIsHandling;
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
            case PREPARE:
                if (internalPrepare()) {
                    //new Thread(mAudioReadData).start();
                    new Thread(mVideoReadData).start();
                }
                break;
            case PLAY:
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
                    MediaUtils.SLEEP_TIME++;
                    if (DEBUG)
                        Log.d(TAG, "onKeyDown() MediaUtils.SLEEP_TIME: " +
                                MediaUtils.SLEEP_TIME);
                    /*if (mCallback != null) {
                        mCallback.onPlaybackFinished();
                    }*/
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
            case PLAY:
                internalPlay();
                break;
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
                SampleVideoPlayer6.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleVideoPlayer6.this.uiHandleMessage(msg);
            }
        };
    }

    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            return false;
        }
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() start");

        if (!mPath.startsWith("http://")
                && !mPath.startsWith("HTTP://")
                && !mPath.startsWith("https://")
                && !mPath.startsWith("HTTPS://")) {
            File file = new File(mPath);
            if (!file.canRead()
                    || file.isDirectory()) {
                if (DEBUG)
                    MLog.e(TAG, "不能读取此文件: " + mPath);
                return false;
            }
            long fileSize = file.length();
            if (DEBUG)
                MLog.d(TAG, "internalPrepare() fileSize: " + fileSize);
        }

        mAudioWrapper.callback = mAudioCallback;
        mVideoWrapper.callback = mVideoCallback;

        mAudioWrapper.clear();
        mVideoWrapper.clear();

        // Audio
        mAudioWrapper.mExtractor = new MediaExtractor();
        try {
            mAudioWrapper.mExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int trackCount = mAudioWrapper.mExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mAudioWrapper.mDecoderMediaFormat = mAudioWrapper.mExtractor.getTrackFormat(i);
            String mime = mAudioWrapper.mDecoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mAudioWrapper.mime = mime;
                mAudioWrapper.mTrackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(mAudioWrapper.mime)
                || mAudioWrapper.mTrackIndex == -1) {
            return false;
        }

        if (mAudioWrapper.mDecoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mAudioWrapper.frameMaxLength =
                    mAudioWrapper.mDecoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }
        /*mAudioWrapper.mDecoderMediaFormat.setInteger(
                MediaFormat.KEY_MAX_INPUT_SIZE, mAudioWrapper.frameMaxLength);*/
        MLog.d(TAG, "internalPrepare()                mAudioWrapper.mime: " +
                mAudioWrapper.mime);
        MLog.d(TAG, "internalPrepare() mAudioWrapper.mDecoderMediaFormat: " +
                mAudioWrapper.mDecoderMediaFormat.toString());
        try {
            mAudioWrapper.mExtractor.selectTrack(mAudioWrapper.mTrackIndex);
            if (!TextUtils.equals("audio/ac4", mAudioWrapper.mime)) {
                mAudioWrapper.mDecoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec(
                        mAudioWrapper.mime, mAudioWrapper.mDecoderMediaFormat);
            } else {
                MediaCodecInfo mediaCodecInfo =
                        MediaUtils.getDecoderMediaCodecInfo(mAudioWrapper.mime);
                String codecName = null;
                if (mediaCodecInfo != null) {
                    codecName = mediaCodecInfo.getName();
                } else {
                    if (TextUtils.equals("audio/ac4", mAudioWrapper.mime)) {
                        codecName = "OMX.google.raw.decoder";
                        mAudioWrapper.mDecoderMediaFormat.setString(
                                MediaFormat.KEY_MIME, "audio/raw");
                    }
                }
                if (!TextUtils.isEmpty(codecName)) {
                    mAudioWrapper.mDecoderMediaCodec =
                            MediaCodec.createByCodecName(codecName);
                    mAudioWrapper.mDecoderMediaCodec.configure(
                            mAudioWrapper.mDecoderMediaFormat, null, null, 0);
                    mAudioWrapper.mDecoderMediaCodec.start();
                }
            }
            mAudioWrapper.render = false;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException
                | IOException e) {
            e.printStackTrace();
            if (mAudioWrapper.mDecoderMediaCodec != null) {
                mAudioWrapper.mDecoderMediaCodec.release();
            }
            mAudioWrapper.mDecoderMediaCodec = null;
        }
        if (mAudioWrapper.mDecoderMediaCodec == null) {
            return false;
        }

        if (mAudioWrapper.mDecoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mAudioWrapper.mDecoderMediaFormat.getByteBuffer("csd-0");
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
        if (mAudioWrapper.mDecoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mAudioWrapper.mDecoderMediaFormat.getByteBuffer("csd-1");
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
        mVideoWrapper.mExtractor = new MediaExtractor();
        try {
            mVideoWrapper.mExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        trackCount = mVideoWrapper.mExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mVideoWrapper.mDecoderMediaFormat = mVideoWrapper.mExtractor.getTrackFormat(i);
            String mime = mVideoWrapper.mDecoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                mVideoWrapper.mime = mime;
                mVideoWrapper.mTrackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(mVideoWrapper.mime)
                || mVideoWrapper.mTrackIndex == -1) {
            return false;
        }

        if (mVideoWrapper.mDecoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            mVideoWrapper.frameMaxLength =
                    mVideoWrapper.mDecoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }
        /*mVideoWrapper.mDecoderMediaFormat.setInteger(
                MediaFormat.KEY_MAX_INPUT_SIZE, mVideoWrapper.frameMaxLength);*/
        /***
         BITRATE_MODE_CQ:  表示完全不控制码率，尽最大可能保证图像质量
         BITRATE_MODE_CBR: 表示编码器会尽量把输出码率控制为设定值
         BITRATE_MODE_VBR: 表示编码器会根据图像内容的复杂度（实际上是帧间变化量的大小）来动态调整输出码率，
         图像复杂则码率高，图像简单则码率低
         */
        mVideoWrapper.mDecoderMediaFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        MLog.d(TAG, "internalPrepare()                        video mime: " +
                mVideoWrapper.mime);
        MLog.d(TAG, "internalPrepare() mVideoWrapper.mDecoderMediaFormat: " +
                mVideoWrapper.mDecoderMediaFormat.toString());
        try {
            mVideoWrapper.mExtractor.selectTrack(mVideoWrapper.mTrackIndex);
            mVideoWrapper.mDecoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            mVideoWrapper.mime,
                            mVideoWrapper.mDecoderMediaFormat,
                            mVideoWrapper.mSurface);
            mVideoWrapper.render = true;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException e) {
            e.printStackTrace();
            if (mVideoWrapper.mDecoderMediaCodec != null) {
                mVideoWrapper.mDecoderMediaCodec.release();
            }
            mVideoWrapper.mDecoderMediaCodec = null;
        }
        if (mVideoWrapper.mDecoderMediaCodec == null) {
            return false;
        }

        if (mVideoWrapper.mDecoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mVideoWrapper.mDecoderMediaFormat.getByteBuffer("csd-0");
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
        if (mVideoWrapper.mDecoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mVideoWrapper.mDecoderMediaFormat.getByteBuffer("csd-1");
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

        if (DEBUG)
            MLog.d(TAG, "internalPrepare() end");
        return true;
    }

    private void internalStart() {
    }

    private void internalPlay() {
    }

    private void internalPause() {
    }

    private void internalStop() {
    }

    private void internalAudioRelease() {
    }

    private void internalVideoRelease() {
    }

    private void internalPrev() {
    }

    private void internalNext() {
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

    /***
     * 寻找指定buffer中H264帧头的开始位置
     *
     * @param offset 开始的位置
     * @param data   数据
     * @param size   需要检测的最大值
     * @return
     */
    private void findH264Head(byte[] data, int offset, int size, List<Integer> list) {
        list.clear();
        int i = 0;
        for (i = offset; i < size; ) {
            // 发现帧头
            if (isH264Head(data, i, size)) {
                list.add(i);
                i += 3;
            } else {
                i++;
            }
        }
    }

    /***
     判断h264帧头
     00 00 00 01 65        (I帧)
     00 00 00 01 61 / 41   (P帧)
     */
    private boolean isH264Head(byte[] buffer, int offset, int size) {
        /*if (offset + 3 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x00
                && buffer[offset + 3] == 0x01) {
            // 00 00 00 01
            return true;
        } else if (offset + 2 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x01) {
            // 00 00 01
            return true;
        }*/

        if (offset + 3 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x00
                && buffer[3] == 0x01
                && isVideoFrameHeadType(buffer[offset + 4])) {
            // 00 00 00 01 x
            return true;
        } else if (offset + 2 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x01
                && isVideoFrameHeadType(buffer[offset + 3])) {
            // 00 00 01 x
            return true;
        }

        return false;
    }

    /***
     I帧或者P帧
     */
    private boolean isVideoFrameHeadType(byte head) {
        return head == (byte) 0x65
                || head == (byte) 0x61
                || head == (byte) 0x41;
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

    private boolean feedInputBufferAndDrainOutputBuffer(
            MediaCodec mediaCodec,
            byte[] data, int offset, int size,
            long presentationTimeUs,
            boolean render,
            boolean needToSleep,
            MediaUtils.Callback callback) {
        // Input
        boolean feedInputBufferResult = MediaUtils.feedInputBuffer(
                mediaCodec, data, offset, size,
                presentationTimeUs);
        // Output
        boolean drainOutputBufferResult = MediaUtils.drainOutputBuffer(
                mediaCodec, render, needToSleep, callback);

        return feedInputBufferResult && drainOutputBufferResult;

        /*// Input
        SystemClock.sleep(1);
        // Output
        SystemClock.sleep(1);*/
    }

    private void audioReadData() {
        readData(mAudioWrapper);
    }

    private void videoReadData() {
        readData(mVideoWrapper);
    }

    private void readData(SimpleWrapper wrapper) {
        String showInfo = null;
        if (wrapper.mType == TYPE_AUDIO) {
            showInfo = "  readData() audio start";
        } else {
            showInfo = "  readData() video start";
        }
        MLog.w(TAG, showInfo);

        if (wrapper.mType == TYPE_AUDIO) {
            showInfo = "  readData() audio wrapper.frameMaxLength: " + wrapper.frameMaxLength;
        } else {
            showInfo = "  readData() video wrapper.frameMaxLength: " + wrapper.frameMaxLength;
        }
        MLog.i(TAG, showInfo);

        /***
         数据先读到room,再从room转移到buffer
         */
        ByteBuffer room = ByteBuffer.allocate(wrapper.frameMaxLength);
        byte[] buffer = new byte[wrapper.frameMaxLength];
        int readTotalSize = 0;
        /***
         三种情况退出循环:
         1.异常
         2.要处理的数据有问题时不能继续往下走,然后通知这里结束
         3.readSize < 0
         */
        wrapper.mExtractor.selectTrack(wrapper.mTrackIndex);
        wrapper.mIsReading = true;
        // Test
        Map<Long, Integer> arrayMap = new ArrayMap<>();
        StringBuilder tempSB = new StringBuilder();
        byte preBufferLastByte = 0;
        while (wrapper.mIsReading) {
            try {
                room.clear();
                // wrapper.mExtractor ---> room
                // readSize为实际读到的大小(音视频一帧的大小),其值可能远远小于room的大小
                int readSize = wrapper.mExtractor.readSampleData(room, 0);
                // 没有数据可读了,结束
                if (readSize < 0) {
                    wrapper.mReadStatus = READ_FINISHED;
                    wrapper.mIsReading = false;
                    synchronized (wrapper.mHandleDataLock) {
                        wrapper.mHandleDataLock.notify();
                    }
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "  readData() audio     readSize: " + readSize;
                    } else {
                        showInfo = "  readData() video     readSize: " + readSize;
                    }
                    MLog.i(TAG, showInfo);
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "  readData() audio readDataSize: " + wrapper.readDataSize;
                    } else {
                        showInfo = "  readData() video readDataSize: " + wrapper.readDataSize;
                    }
                    MLog.i(TAG, showInfo);

                    // 开启任务处理数据(如果还没有开启任务过的话)
                    // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                    if (!wrapper.mIsHandling) {
                        wrapper.mIsHandling = true;
                        if (wrapper.mType == TYPE_AUDIO) {
                            new Thread(mAudioHandleData).start();
                        } else {
                            new Thread(mVideoHandleData).start();
                        }
                    }
                    break;
                }

                Arrays.fill(buffer, (byte) 0);
                // room ---> buffer
                room.get(buffer, 0, readSize);

                // Test
                if (!feedInputBufferAndDrainOutputBuffer(
                        wrapper.mDecoderMediaCodec,
                        buffer, 0, readSize,
                        0,
                        wrapper.render,
                        wrapper.mType == TYPE_AUDIO ? false : true,
                        wrapper.callback)) {
                    wrapper.mIsHandling = false;
                    break;
                }

                if (readSize > wrapper.frameMaxLength) {
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "  readData() audio 出现大体积帧       readSize: " + readSize;
                    } else {
                        showInfo = "  readData() video 出现大体积帧       readSize: " + readSize;
                    }
                    MLog.e(TAG, showInfo);
                }

                wrapper.mReadFrameLengthTotal += readSize;
                // 实际读取到的大小 + 标志位长度
                readTotalSize += readSize + HEADER_FLAG_LENGTH;
                if (readTotalSize <= wrapper.CACHE) {
                    wrapper.mReadStatus = READ_STARTED;
                    wrapper.mReadFrameCounts++;
                    if (preBufferLastByte == 0
                            || preBufferLastByte != 88) {
                        System.arraycopy(HEADER_FLAG1, 0,
                                wrapper.mData1, wrapper.readDataSize, HEADER_FLAG_LENGTH);
                    } else {
                        System.arraycopy(HEADER_FLAG2, 0,
                                wrapper.mData1, wrapper.readDataSize, HEADER_FLAG_LENGTH);
                    }
                    preBufferLastByte = buffer[readSize - 1];

                    // buffer ---> mData1
                    System.arraycopy(buffer, 0,
                            wrapper.mData1,
                            wrapper.readDataSize + HEADER_FLAG_LENGTH,
                            readSize);
                    wrapper.readDataSize += readSize + HEADER_FLAG_LENGTH;

                    // 一帧对应一个时间戳
                    wrapper.mTime1.put(
                            wrapper.mReadFrameCounts, wrapper.mExtractor.getSampleTime());
                    /*MLog.d(TAG, "Test wrapper.mReadFrameCounts: " + wrapper.mReadFrameCounts+
                    " readSize: "+readSize);*/
                    arrayMap.put(wrapper.mReadFrameCounts, readSize);
                    if (wrapper.mReadFrameCounts == 365) {
                        StringBuilder sb = new StringBuilder();
                        for (byte bt : buffer) {
                            sb.append(" ");
                            sb.append(bt);
                        }
                        showInfo = "handleData() 365 video: " + sb.toString();
                        MLog.i(TAG, showInfo);
                        showInfo = "handleData() buffer[readSize - 1]: " + buffer[readSize - 1];
                        MLog.i(TAG, showInfo);
                    } else if (wrapper.mReadFrameCounts == 366) {
                        StringBuilder sb = new StringBuilder();
                        for (byte bt : buffer) {
                            sb.append(" ");
                            sb.append(bt);
                        }
                        showInfo = "handleData() 366 video: " + sb.toString();
                        MLog.i(TAG, showInfo);
                        showInfo = "handleData() buffer[readSize - 1]: " + buffer[readSize - 1];
                        MLog.i(TAG, showInfo);
                    }
                } else {
                    /***
                     在某些帧上多读或者少读了几个字节,造成花屏
                     */
                    // Test
                    //////////////////////////////////////////////////////////////////////////
                    MLog.i(TAG, "Test start");
                    wrapper.mReadFrameCounts = 0;
                    ArrayList<Integer> offsetList = new ArrayList<Integer>();
                    int frameDataLength = 0;
                    byte[] frameData = new byte[wrapper.frameMaxLength];
                    System.arraycopy(
                            wrapper.mData1, 0,
                            wrapper.mData2, 0, wrapper.readDataSize);
                    findHead(wrapper.mData2, 0, wrapper.readDataSize, offsetList);
                    int offsetCounts = offsetList.size();
                    MLog.i(TAG, "Test offsetCounts: " + offsetCounts);
                    for (int i = 0; i < offsetCounts; i++) {
                        Arrays.fill(frameData, (byte) 0);
                        if (i + 1 < offsetCounts) {
                            wrapper.mReadFrameCounts++;
                            readSize = arrayMap.get(wrapper.mReadFrameCounts);
                            /***
                             集合中至少有两个offset才有一帧输出
                             各帧之间的offset很重要,比如有:0, 519, 1038, 1585, 2147 ...
                             知道了offset,那么就知道了要"喂"多少数据了.
                             两个offset的位置一减就是一帧的长度
                             */
                            frameDataLength = offsetList.get(i + 1)
                                    - offsetList.get(i)
                                    - HEADER_FLAG_LENGTH;
                            System.arraycopy(
                                    wrapper.mData2, offsetList.get(i) + HEADER_FLAG_LENGTH,
                                    frameData, 0, frameDataLength);
                            if (!feedInputBufferAndDrainOutputBuffer(
                                    wrapper.mDecoderMediaCodec,
                                    frameData, 0, frameDataLength,
                                    0,
                                    wrapper.render,
                                    wrapper.mType == TYPE_AUDIO ? false : true,
                                    wrapper.callback)) {
                                wrapper.mIsHandling = false;
                                break;
                            }
                            /*MLog.d(TAG, "handleData() offset: " + offsetList.get(i) +
                                    "    " + frameData[0] +
                                    " " + frameData[1] +
                                    " " + frameData[2] +
                                    " " + frameData[3] +
                                    " " + frameData[4] +
                                    " " + frameData[5] +
                                    " " + frameData[6]);
                            MLog.d(TAG, "handleData() frameDataLength: " + frameDataLength);*/
                            if (wrapper.mReadFrameCounts == 365) {
                                StringBuilder sb = new StringBuilder();
                                for (byte bt : frameData) {
                                    sb.append(" ");
                                    sb.append(bt);
                                }
                                showInfo = "handleData() 365 video: " + sb.toString();
                                MLog.i(TAG, showInfo);
                            } else if (wrapper.mReadFrameCounts == 366) {
                                StringBuilder sb = new StringBuilder();
                                for (byte bt : frameData) {
                                    sb.append(" ");
                                    sb.append(bt);
                                }
                                showInfo = "handleData() 366 video: " + sb.toString();
                                MLog.i(TAG, showInfo);
                            }
                            if(readSize!=frameDataLength){
                                MLog.e(TAG, "Test        wrapper.mReadFrameCounts: " + wrapper
                                .mReadFrameCounts);
                                MLog.e(TAG, "Test                    src readSize: " + readSize);
                                MLog.e(TAG, "Test                 frameDataLength: " +
                                frameDataLength);
                            }
                            continue;
                        }
                    }
                    if (true) {
                        MLog.i(TAG, "Test end");
                        wrapper.mIsReading = false;
                        break;
                    }
                    //////////////////////////////////////////////////////////////////////////


                    long presentationTimeUs = wrapper.mExtractor.getSampleTime();

                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "  readData() audio 吃饱了,休息一下 readDataSize: " +
                                wrapper.readDataSize;
                    } else {
                        showInfo = "  readData() video 吃饱了,休息一下 readDataSize: " +
                                wrapper.readDataSize;
                    }
                    MLog.w(TAG, showInfo);

                    wrapper.mReadStatus = READ_PAUSED;
                    synchronized (wrapper.mHandleDataLock) {
                        wrapper.mHandleDataLock.notify();
                    }
                    // 开启任务处理数据
                    // 如果任务是在这里被开启的,那么说明网络文件长度大于CACHE
                    if (!wrapper.mIsHandling) {
                        wrapper.mIsHandling = true;
                        if (wrapper.mType == TYPE_AUDIO) {
                            new Thread(mAudioHandleData).start();
                        } else {
                            new Thread(mVideoHandleData).start();
                        }
                    }

                    System.gc();

                    // wait
                    synchronized (wrapper.mReadDataLock) {
                        if (wrapper.mType == TYPE_AUDIO) {
                            showInfo = "  readData() audio mReadDataLock.wait() start";
                        } else {
                            showInfo = "  readData() video mReadDataLock.wait() start";
                        }
                        MLog.w(TAG, showInfo);
                        try {
                            wrapper.mReadDataLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (wrapper.mType == TYPE_AUDIO) {
                            showInfo = "  readData() audio mReadDataLock.wait() end";
                        } else {
                            showInfo = "  readData() video mReadDataLock.wait() end";
                        }
                        MLog.w(TAG, showInfo);
                    }

                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "  readData() audio 饿了,继续吃";
                    } else {
                        showInfo = "  readData() video 饿了,继续吃";
                    }
                    MLog.e(TAG, showInfo);

                    Arrays.fill(wrapper.mData1, (byte) 0);
                    wrapper.readDataSize = readSize + HEADER_FLAG_LENGTH;
                    readTotalSize = wrapper.readDataSize;
                    if (preBufferLastByte == 0
                            || preBufferLastByte != 88) {
                        System.arraycopy(HEADER_FLAG1, 0,
                                wrapper.mData1, 0, HEADER_FLAG_LENGTH);
                    } else {
                        System.arraycopy(HEADER_FLAG2, 0,
                                wrapper.mData1, 0, HEADER_FLAG_LENGTH);
                    }
                    preBufferLastByte = buffer[readSize - 1];
                    // 此时buffer中还有readSize个byte
                    System.arraycopy(buffer, 0,
                            wrapper.mData1, HEADER_FLAG_LENGTH, readSize);

                    wrapper.mTime1.clear();
                    wrapper.mReadFrameCounts++;
                    // 一帧对应一个时间戳
                    wrapper.mTime1.put(
                            wrapper.mReadFrameCounts, presentationTimeUs);
                }

                // 跳到下一帧
                wrapper.mExtractor.advance();
            } catch (Exception e) {
                e.printStackTrace();
                wrapper.mIsReading = false;
                wrapper.mReadStatus = READ_ERROR;
                break;
            }
        }// while(...) end

        if (wrapper.mType == TYPE_AUDIO) {
            showInfo = "  readData() audio end";
        } else {
            showInfo = "  readData() video end";
        }
        MLog.w(TAG, showInfo);
    }

    private void audioHandleData() {
        handleData(mAudioWrapper);
    }

    private void videoHandleData() {
        handleData(mVideoWrapper);
    }

    private void handleData(SimpleWrapper wrapper) {
        String showInfo = null;
        if (wrapper.mType == TYPE_AUDIO) {
            showInfo = "handleData() audio start";
        } else {
            showInfo = "handleData() video start";
        }
        MLog.w(TAG, showInfo);

        // mData2中剩下的数据大小
        ArrayList<Integer> offsetList = new ArrayList<Integer>();
        int preReadDataSize = 0;
        int lastOffsetIndex = 0;
        int restOfDataSize = 0;
        // 音频或者视频一帧的实际大小
        int frameDataLength = 0;
        // 放一帧音频或者视频的容器
        byte[] frameData = new byte[wrapper.frameMaxLength];
        // mData1 ---> mData2
        System.arraycopy(
                wrapper.mData1, 0,
                wrapper.mData2, 0, wrapper.readDataSize);
        wrapper.mTime2.clear();
        wrapper.mTime2.putAll(wrapper.mTime1);

        boolean onlyOne = true;
        while (wrapper.mIsHandling) {
            if (wrapper.mType == TYPE_AUDIO) {
                showInfo = "handleData() audio findHead start";
            } else {
                showInfo = "handleData() video findHead start";
            }
            MLog.i(TAG, showInfo);
            findHead(wrapper.mData2, 0, wrapper.readDataSize, offsetList);
            if (wrapper.mType == TYPE_AUDIO) {
                showInfo = "handleData() audio findHead end";
            } else {
                showInfo = "handleData() video findHead end";
            }
            MLog.i(TAG, showInfo);

            int offsetCounts = offsetList.size();
            if (wrapper.mType == TYPE_AUDIO) {
                showInfo = "handleData() audio findHead    offsetCounts: " + offsetCounts;
            } else {
                showInfo = "handleData() video findHead    offsetCounts: " + offsetCounts;
            }
            MLog.i(TAG, showInfo);
            if (offsetCounts > 1) {
                preReadDataSize = wrapper.readDataSize;
                lastOffsetIndex = offsetList.get(offsetCounts - 1);
                // 实际读取到的数据大小 - 最后一个offsetIndex = 剩余的数据(一帧完整的数据)
                restOfDataSize = wrapper.readDataSize - lastOffsetIndex;

                if (wrapper.mType == TYPE_AUDIO) {
                    showInfo = "handleData() audio findHead    readDataSize: " +
                            wrapper.readDataSize;
                } else {
                    showInfo = "handleData() video findHead    readDataSize: " +
                            wrapper.readDataSize;
                }
                MLog.i(TAG, showInfo);
                if (wrapper.mType == TYPE_AUDIO) {
                    showInfo = "handleData() audio findHead lastOffsetIndex: " +
                            lastOffsetIndex;
                } else {
                    showInfo = "handleData() video findHead lastOffsetIndex: " +
                            lastOffsetIndex;
                }
                MLog.i(TAG, showInfo);
                if (wrapper.mType == TYPE_AUDIO) {
                    showInfo = "handleData() audio findHead  restOfDataSize: " +
                            restOfDataSize;
                } else {
                    showInfo = "handleData() video findHead  restOfDataSize: " +
                            restOfDataSize;
                }
                MLog.i(TAG, showInfo);
            } else {
                StringBuilder sb = new StringBuilder();
                for (byte bt : wrapper.mData2) {
                    sb.append(" ");
                    sb.append(bt);
                }
                if (wrapper.mType == TYPE_AUDIO) {
                    showInfo = "handleData() audio: " + sb.toString();
                } else {
                    showInfo = "handleData() video: " + sb.toString();
                }
                MLog.i(TAG, showInfo);
                wrapper.mIsHandling = false;
                break;
            }
            if (wrapper.mIsReading) {
                // 此处发送消息后,readDataSize的大小可能会变化
                synchronized (wrapper.mReadDataLock) {
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "handleData() audio findHead mReadDataLock.notify()";
                    } else {
                        showInfo = "handleData() video findHead mReadDataLock.notify()";
                    }
                    MLog.w(TAG, showInfo);
                    wrapper.mReadDataLock.notify();
                }
            }

            for (int i = 0; i < offsetCounts; i++) {
                Arrays.fill(frameData, (byte) 0);
                if (i + 1 < offsetCounts) {
                    /***
                     集合中至少有两个offset才有一帧输出
                     各帧之间的offset很重要,比如有:0, 519, 1038, 1585, 2147 ...
                     知道了offset,那么就知道了要"喂"多少数据了.
                     两个offset的位置一减就是一帧的长度
                     */
                    frameDataLength = offsetList.get(i + 1)
                            - offsetList.get(i)
                            - HEADER_FLAG_LENGTH;
                    wrapper.mHandleFrameLengthTotal += frameDataLength;
                    wrapper.mHandleFrameCounts++;
                    System.arraycopy(
                            wrapper.mData2, offsetList.get(i) + HEADER_FLAG_LENGTH,
                            frameData, 0, frameDataLength);

                    /*if (wrapper.mType == TYPE_AUDIO) {
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

                    if (onlyOne) {
                        onlyOne = false;
                        MediaUtils.startTimeMs = System.currentTimeMillis();
                        MediaUtils.startTimeMs = System.nanoTime();
                    }
                    long presentationTimeUs = 0;
                    /*if ((Long) wrapper.mHandleFrameCounts != null
                            && wrapper.mTime2.containsKey((Long) wrapper.mHandleFrameCounts)) {
                        presentationTimeUs = wrapper.mTime2.get((Long) wrapper.mHandleFrameCounts);
                    } else {
                        presentationTimeUs = System.nanoTime();
                    }*/
                    presentationTimeUs = (System.nanoTime() - MediaUtils.startTimeMs) * 1000;
                    /*if (!feedInputBufferAndDrainOutputBuffer(
                            wrapper.mDecoderMediaCodec,
                            frameData, 0, frameDataLength,
                            presentationTimeUs,
                            wrapper.render,
                            wrapper.mType == TYPE_AUDIO ? false : true,
                            wrapper.callback)) {
                        wrapper.mIsHandling = false;
                        break;
                    }*/
                    continue;
                } else {
                    // 处理集合中最后一个offset的位置
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "handleData() audio preReadDataSize: " + preReadDataSize;
                    } else {
                        showInfo = "handleData() video preReadDataSize: " + preReadDataSize;
                    }
                    MLog.i(TAG, showInfo);
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "handleData() audio    readDataSize: " + wrapper.readDataSize;
                    } else {
                        showInfo = "handleData() video    readDataSize: " + wrapper.readDataSize;
                    }
                    MLog.i(TAG, showInfo);
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "handleData() audio lastOffsetIndex: " + lastOffsetIndex;
                    } else {
                        showInfo = "handleData() video lastOffsetIndex: " + lastOffsetIndex;
                    }
                    MLog.i(TAG, showInfo);
                    if (wrapper.mType == TYPE_AUDIO) {
                        showInfo = "handleData() audio  restOfDataSize: " + restOfDataSize;
                    } else {
                        showInfo = "handleData() video  restOfDataSize: " + restOfDataSize;
                    }
                    MLog.i(TAG, showInfo);

                    // 处理剩余的数据
                    if (restOfDataSize > 0) {
                        frameDataLength = restOfDataSize - HEADER_FLAG_LENGTH;
                        wrapper.mHandleFrameLengthTotal += frameDataLength;
                        wrapper.mHandleFrameCounts++;
                        System.arraycopy(
                                wrapper.mData2, lastOffsetIndex + HEADER_FLAG_LENGTH,
                                frameData, 0, frameDataLength);

                        long presentationTimeUs = 0;
                        /*if ((Long) wrapper.mHandleFrameCounts != null
                                && wrapper.mTime2.containsKey((Long) wrapper.mHandleFrameCounts)) {
                            presentationTimeUs = wrapper.mTime2.get((Long) wrapper
                                    .mHandleFrameCounts);
                        } else {
                            presentationTimeUs = System.nanoTime();
                        }*/
                        presentationTimeUs = (System.nanoTime() - MediaUtils.startTimeMs) / 1000;
                        /*if (!feedInputBufferAndDrainOutputBuffer(
                                wrapper.mDecoderMediaCodec,
                                frameData, 0, frameDataLength,
                                presentationTimeUs,
                                wrapper.render,
                                wrapper.mType == TYPE_AUDIO ? false : true,
                                wrapper.callback)) {
                            wrapper.mIsHandling = false;
                            break;
                        }*/
                    }

                    if (wrapper.readDataSize != wrapper.CACHE
                            && preReadDataSize == wrapper.readDataSize
                            && wrapper.mReadStatus == READ_FINISHED) {
                        wrapper.mIsHandling = false;
                        break;
                    } else {
                        if (wrapper.mReadStatus == READ_STARTED) {
                            synchronized (wrapper.mHandleDataLock) {
                                try {
                                    wrapper.mHandleDataLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        Arrays.fill(wrapper.mData2, (byte) 0);
                        System.arraycopy(
                                wrapper.mData1, 0,
                                wrapper.mData2, 0, wrapper.readDataSize);

                        wrapper.mTime2.clear();
                        wrapper.mTime2.putAll(wrapper.mTime1);
                        break;
                    }
                }
            }// for(...) end
        }// while(true) end

        if (wrapper.mType == TYPE_AUDIO) {
            showInfo = "handleData() audio        wrapper.mReadFrameCounts: " +
                    wrapper.mReadFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() audio      wrapper.mHandleFrameCounts: " +
                    wrapper.mHandleFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() audio   wrapper.mReadFrameLengthTotal: " +
                    wrapper.mReadFrameLengthTotal;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() audio wrapper.mHandleFrameLengthTotal: " +
                    wrapper.mHandleFrameLengthTotal;
            MLog.d(TAG, showInfo);
        } else {
            showInfo = "handleData() video        wrapper.mReadFrameCounts: " +
                    wrapper.mReadFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() video      wrapper.mHandleFrameCounts: " +
                    wrapper.mHandleFrameCounts;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() video   wrapper.mReadFrameLengthTotal: " +
                    wrapper.mReadFrameLengthTotal;
            MLog.d(TAG, showInfo);
            showInfo = "handleData() video wrapper.mHandleFrameLengthTotal: " +
                    wrapper.mHandleFrameLengthTotal;
            MLog.d(TAG, showInfo);
        }

        if (wrapper.mAudioTrack != null) {
            wrapper.mAudioTrack.release();
        }

        if (wrapper.mDecoderMediaCodec != null) {
            wrapper.mDecoderMediaCodec.release();
        }

        wrapper.clear();

        if (wrapper.mType == TYPE_AUDIO) {
            showInfo = "handleData() audio end";
        } else {
            showInfo = "handleData() video end";
        }
        MLog.w(TAG, showInfo);

        /*if (!mAudioWrapper.mIsHandling
                && !mVideoWrapper.mIsHandling) {
            play();
        }*/
    }

    private Runnable mAudioHandleData = new Runnable() {
        @Override
        public void run() {
            audioHandleData();
        }
    };

    private Runnable mVideoHandleData = new Runnable() {
        @Override
        public void run() {
            videoHandleData();
        }
    };

    private Runnable mAudioReadData = new Runnable() {
        @Override
        public void run() {
            audioReadData();
        }
    };

    private Runnable mVideoReadData = new Runnable() {
        @Override
        public void run() {
            videoReadData();
        }
    };

    private MediaUtils.Callback mAudioCallback = new MediaUtils.Callback() {
        @Override
        public void onFormatChanged(MediaFormat newMediaFormat) {
            mAudioWrapper.mDecoderMediaFormat = newMediaFormat;

            if (mAudioWrapper.mAudioTrack != null) {
                mAudioWrapper.mAudioTrack.release();
            }

            int sampleRateInHz =
                    mAudioWrapper.mDecoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount =
                    mAudioWrapper.mDecoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            mAudioWrapper.mDecoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mAudioWrapper.mDecoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
            if (AACPlayer.sampleRateIndexMap.containsKey(sampleRateInHz)
                    && AACPlayer.channelConfigIndexMap.containsKey(channelCount)) {
                List<byte[]> list = new ArrayList<>();
                list.add(MediaUtils.buildAacAudioSpecificConfig(
                        AACPlayer.sampleRateIndexMap.get(sampleRateInHz),
                        AACPlayer.channelConfigIndexMap.get(channelCount)));
                MediaUtils.setCsdBuffers(mAudioWrapper.mDecoderMediaFormat, list);
            }
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            if (mAudioWrapper.mDecoderMediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                audioFormat =
                        mAudioWrapper.mDecoderMediaFormat.getInteger(
                                MediaFormat.KEY_PCM_ENCODING);
            }
            mAudioWrapper.mAudioTrack = MediaUtils.createAudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRateInHz, channelCount, audioFormat,
                    AudioTrack.MODE_STREAM);
            if (mAudioWrapper.mAudioTrack != null) {
                setVolume();
                mAudioWrapper.mAudioTrack.play();
                MLog.d(TAG, "mAudioCallback mAudioWrapper.mAudioTrack.play()");
            }
        }

        @Override
        public void onInputBuffer(
                ByteBuffer room, MediaUtils.InputBufferInfo info) {

        }

        @Override
        public void onOutputBuffer(
                ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
            byte[] audioData = new byte[roomSize];
            room.get(audioData, 0, audioData.length);
            // MLog.d(TAG, "mAudioCallback audioData.length: " + audioData.length);
            if (mAudioWrapper.mAudioTrack != null) {
                mAudioWrapper.mAudioTrack.write(audioData, 0, audioData.length);
            }
        }
    };

    private MediaUtils.Callback mVideoCallback = new MediaUtils.Callback() {
        @Override
        public void onFormatChanged(MediaFormat newMediaFormat) {
            mVideoWrapper.mDecoderMediaFormat = newMediaFormat;
        }

        @Override
        public void onInputBuffer(
                ByteBuffer room, MediaUtils.InputBufferInfo info) {

        }

        @Override
        public void onOutputBuffer(
                ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {

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
