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
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 Created by weidi on 2019/7/10.

 现在有的问题:
 视频:
 1.花屏
 2.播放速度过快
 音频:

 */

public class SampleVideoPlayer2 {

    private static final String TAG =
            SampleVideoPlayer2.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int AUDIO_CACHE = 1024 * 1024 * 1;
    private static final int VIDEO_CACHE = 1024 * 1024 * 8;
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

    // 为了注册广播
    private Context mContext;
    private String mPath;
    private float mVolume = 1.0f;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private SimpleWrapper mAudioWrapper = new SimpleWrapper(TYPE_AUDIO);
    private SimpleWrapper mVideoWrapper = new SimpleWrapper(TYPE_VIDEO);

    private Callback mCallback;

    public interface Callback {
        void onPlaybackReady();

        void onPlaybackPaused();

        void onPlaybackStarted();

        void onPlaybackFinished();

        void onProgressUpdated(long presentationTimeUs);

        void onPlaybackError();

        void onPlaybackInfo(String info);
    }

    private static class SimpleWrapper {
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
        public boolean mData1HasData = false;
        public byte[] mData1 = null;
        public byte[] mData2 = null;
        public int readDataSize = -1;
        public int lastOffsetIndex = -1;
        public int mReadStatus = READ_UNKNOW;
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
                    break;
                case TYPE_VIDEO:
                    CACHE = VIDEO_CACHE;
                    mType = TYPE_VIDEO;
                    mData1 = new byte[VIDEO_CACHE];
                    mData2 = new byte[VIDEO_CACHE];
                    break;
                default:
                    break;
            }
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public SampleVideoPlayer2() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
        registerHeadsetPlugReceiver();
    }

    public void setPath(String path) {
        mPath = path;
        mPath = "/storage/37C8-3904/myfiles/video/Silent_Movie_321_AC4_H265_MP4_50fps.mp4";
        mPath = "/storage/2430-1702/BaiduNetdisk/video/流浪的地球.mp4";
        mPath = "http://192.168.0.112:8080/tomcat_video/test.mp4";
        mPath = "http://192.168.0.112:8080/tomcat_video/game_of_thrones_5_01.mp4";
        mPath = "http://download.xunleizuida.com/1904/" +
                "%E5%BF%8D%E8%80%85%E5%88%BA%E5%AE%A2.HD1280" +
                "%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4";
        mPath = "http://xunlei.jingpin88.com/20171028/6WQ5SFS2/mp4/6WQ5SFS2.mp4";
        mPath = "/storage/2430-1702/BaiduNetdisk/video/05.mp4";
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
                    new Thread(mAudioReadData).start();
                    //new Thread(mVideoReadData).start();
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
                SampleVideoPlayer2.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleVideoPlayer2.this.uiHandleMessage(msg);
            }
        };
    }

    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            return false;
        }
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() start");

        /*File file = new File(mPath);
        if (!file.canRead()
                || file.isDirectory()) {
            if (DEBUG)
                MLog.e(TAG, "不能读取此文件: " + mPath);
            return false;
        }
        long fileSize = file.length();
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() fileSize: " + fileSize);*/

        mAudioWrapper.callback = mAudioCallback;
        mVideoWrapper.callback = mVideoCallback;

        String audioMime = null;
        String videoMime = null;

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
                audioMime = mime;
                mAudioWrapper.mTrackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(audioMime)
                || mAudioWrapper.mTrackIndex == -1) {
            return false;
        }

        MLog.d(TAG, "internalPrepare() audio mime: " + audioMime);
        MLog.d(TAG, "internalPrepare() mAudioWrapper.decoderMediaFormat: " +
                mAudioWrapper.mDecoderMediaFormat.toString());
        try {
            mAudioWrapper.mExtractor.selectTrack(mAudioWrapper.mTrackIndex);
            if (!TextUtils.equals("audio/ac4", audioMime)) {
                mAudioWrapper.mDecoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec(
                        audioMime, mAudioWrapper.mDecoderMediaFormat);
            } else {
                MediaCodecInfo mediaCodecInfo = MediaUtils.getDecoderMediaCodecInfo(audioMime);
                String codecName = null;
                if (mediaCodecInfo != null) {
                    codecName = mediaCodecInfo.getName();
                } else {
                    if (TextUtils.equals("audio/ac4", audioMime)) {
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
                videoMime = mime;
                mVideoWrapper.mTrackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(videoMime)
                || mVideoWrapper.mTrackIndex == -1) {
            return false;
        }

        mVideoWrapper.mDecoderMediaFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        MLog.d(TAG, "internalPrepare() video mime: " + videoMime);
        MLog.d(TAG, "internalPrepare() mVideoWrapper.decoderMediaFormat: " +
                mVideoWrapper.mDecoderMediaFormat.toString());
        try {
            mVideoWrapper.mExtractor.selectTrack(mVideoWrapper.mTrackIndex);
            mVideoWrapper.mDecoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            videoMime, mVideoWrapper.mDecoderMediaFormat, mVideoWrapper.mSurface);
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
            MLog.d(TAG, "internalPrepare() csd-0: " + sb.toString());
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
            MLog.d(TAG, "internalPrepare() csd-1: " + sb.toString());
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

    private void findAudioHead(byte[] data, int offset, int size, List<Integer> list) {
        list.clear();
        int i = 0;
        for (i = offset; i < size; ) {
            // 发现帧头
            if (isAudioHead(data, i, size)) {
                list.add(i);
                i += 1;
            } else {
                i++;
            }
        }
    }

    private boolean isAudioHead(byte[] data, int offset, int size) {
        if (offset + 1 >= size) {
            return false;
        }
        switch (data[offset]) {
            case 33:
                switch (data[offset + 1]) {
                    case 0:
                    case 11:
                    case 17:
                    case 25:
                    case 26:
                    case 28:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 76:
                    case 77:
                    case 78:
                    case 107:
                    case 121:
                    case 122:
                    case 124:
                    case -126:
                        return true;
                    default:
                        break;
                }
            case 32:
                // -123 ~ -81
                // data[offset + 1] >= -123 && data[offset + 1] <= -81
                switch (data[offset + 1]) {
                    case 116:
                    case 120:
                    case 121:

                    case -81:
                    case -82:
                    case -83:
                    case -84:
                    case -85:
                    case -86:
                    case -87:
                    case -88:
                    case -89:
                    case -90:
                    case -91:
                    case -92:
                    case -93:
                    case -94:
                    case -95:
                    case -96:
                    case -97:
                    case -98:
                    case -99:
                    case -100:
                    case -101:
                    case -102:
                    case -103:
                    case -104:
                    case -105:
                    case -106:
                    case -107:
                    case -108:
                    case -109:
                    case -110:
                    case -111:
                    case -112:
                    case -113:
                    case -114:
                    case -115:
                    case -116:
                    case -117:
                    case -118:
                    case -119:
                    case -120:
                    case -121:
                    case -122:
                    case -123:
                        return true;
                    default:
                        break;
                }
            case -34:
                switch (data[offset + 1]) {
                    case 2:
                        return true;
                    default:
                        break;
                }
            default:
                break;
        }
        return false;
    }

    /***
     * 寻找指定buffer中AAC帧头的开始位置
     *
     * @param offset 开始的位置
     * @param data   数据
     * @param size   需要检测的最大值
     * @return
     */
    private void findAacHead(byte[] data, int offset, int size, List<Integer> list) {
        list.clear();
        int i = 0;
        for (i = offset; i < size; ) {
            // 发现帧头
            if (isAacHead(data, i, size)) {
                list.add(i);
                i += 1;
            } else {
                i++;
            }
        }
    }

    /***
     判断aac帧头
     */
    private boolean isAacHead(byte[] data, int offset, int size) {
        if (offset + 1 < size
                && data[offset] == 33) {
            switch (data[offset + 1]) {
                case 26:
                case 28:
                    return true;
            }
        }

        /*if (offset + 6 < size
                && data[offset] == (byte) 0xFF
                && data[offset + 1] == (byte) 0xF1
                && (data[offset + 3] == (byte) 0x80 || data[offset + 3] == 64)
                && data[offset + 6] == (byte) 0xFC) {
            // -1 -15 80 -128 63 -97 -4
            // -1 -15 84   64 3  -65 -4
            return true;
        }*/
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

    private void feedInputBufferAndDrainOutputBuffer(
            MediaCodec mediaCodec,
            byte[] data, int offset, int size, long startTimeUs,
            boolean render,
            boolean needToSleep,
            MediaUtils.Callback callback) {
        long presentationTimeUs =
                (System.nanoTime() - startTimeUs) / 1000;
        // Input
        MediaUtils.feedInputBuffer(
                mediaCodec, data, offset, size,
                presentationTimeUs);
        // Output
        MediaUtils.drainOutputBuffer(
                mediaCodec, render, needToSleep, callback);

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
        if (DEBUG)
            MLog.w(TAG, "readData() start");

        wrapper.mReadStatus = READ_UNKNOW;
        // 测试了一下,好像最大只能读取8192个byte
        int bufferLength = 1024 * 1024;
        byte[] buffer = new byte[bufferLength];
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferLength);
        // mData1中保存的实际数据大小
        wrapper.readDataSize = 0;
        int readTotalSize = 0;
        Arrays.fill(wrapper.mData1, (byte) 0);
        /***
         三种情况退出循环:
         1.异常
         2.要处理的数据有问题时不能继续往下走,然后通知这里结束
         3.readSize < 0
         */
        wrapper.mExtractor.selectTrack(wrapper.mTrackIndex);
        wrapper.mIsReading = true;
        while (wrapper.mIsReading) {
            try {
                Arrays.fill(buffer, (byte) 0);
                byteBuffer.clear();
                // httpAccessor ---> readData1
                int readSize = wrapper.mExtractor.readSampleData(byteBuffer, 0);
                if (readSize < 0) {
                    wrapper.mReadStatus = READ_FINISHED;
                    synchronized (wrapper.mHandleDataLock) {
                        wrapper.mHandleDataLock.notify();
                    }
                    MLog.i(TAG, "readData()     readSize: " + readSize);
                    MLog.i(TAG, "readData() readDataSize: " + wrapper.readDataSize);
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
                    wrapper.mIsReading = false;
                    break;
                }
                byteBuffer.get(buffer, 0, readSize);
                readTotalSize += readSize;
                if (readTotalSize <= wrapper.CACHE) {
                    wrapper.mReadStatus = READ_STARTED;
                    // buffer ---> readData1
                    System.arraycopy(buffer, 0,
                            wrapper.mData1, wrapper.readDataSize, readSize);
                    wrapper.readDataSize += readSize;
                } else {
                    MLog.w(TAG, "readData() 吃饱了,休息一下 readDataSize: " +
                            wrapper.readDataSize);
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
                    // wait
                    synchronized (wrapper.mReadDataLock) {
                        MLog.i(TAG, "readData() readDataLock.wait() start");
                        try {
                            wrapper.mReadDataLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        MLog.i(TAG, "readData() readDataLock.wait() end");
                    }
                    MLog.d(TAG, "readData() 饿了,继续吃 mData1HasData: " + wrapper
                            .mData1HasData);
                    if (!wrapper.mData1HasData) {
                        Arrays.fill(wrapper.mData1, (byte) 0);
                        wrapper.readDataSize = readSize;
                        readTotalSize = readSize;
                        // 此时buffer中还有readSize个byte
                        System.arraycopy(buffer, 0,
                                wrapper.mData1, 0, readSize);
                    } else {
                        wrapper.mData1HasData = false;
                        wrapper.readDataSize = readSize + (wrapper.CACHE - wrapper.lastOffsetIndex);
                        readTotalSize = wrapper.readDataSize;
                        // 此时buffer中还有readSize个byte
                        System.arraycopy(buffer, 0,
                                wrapper.mData1, wrapper.CACHE - wrapper.lastOffsetIndex, readSize);
                    }
                }
                wrapper.mExtractor.advance();
            } catch (NullPointerException e) {
                e.printStackTrace();
                wrapper.mIsReading = false;
                wrapper.mReadStatus = READ_UNKNOW;
                break;
            }
        }// while(...) end

        if (DEBUG)
            MLog.w(TAG, "readData() end");
    }

    private void audioHandleData() {
        handleData(mAudioWrapper);
    }

    private void videoHandleData() {
        handleData(mVideoWrapper);
    }

    private void handleData(SimpleWrapper wrapper) {
        if (DEBUG)
            MLog.w(TAG, "handleData() start");

        // mData2中剩下的数据大小
        ArrayList<Integer> offsetList = new ArrayList<Integer>();
        long frameTotal = 0;
        int preReadDataSize = 0;
        int restOfDataSize = 0;
        int frameDataLength = 1024 * 100;
        byte[] frameData = new byte[frameDataLength];
        // readData1 ---> handleData
        System.arraycopy(
                wrapper.mData1, 0,
                wrapper.mData2, 0, wrapper.readDataSize);

        long startDecodeTime = System.nanoTime();
        //startTime = System.currentTimeMillis();
        boolean isHandlingData = true;
        while (isHandlingData) {
            MLog.i(TAG, "handleData() findHead start");
            if (wrapper.readDataSize == wrapper.CACHE
                    || wrapper.readDataSize == wrapper.lastOffsetIndex) {
                if (wrapper.mType == TYPE_AUDIO) {
                    findAacHead(wrapper.mData2, 0, wrapper.CACHE, offsetList);
                } else {
                    findH264Head(wrapper.mData2, 0, wrapper.CACHE, offsetList);
                }
            } else {
                if (wrapper.mType == TYPE_AUDIO) {
                    findAacHead(wrapper.mData2, 0, wrapper.readDataSize, offsetList);
                } else {
                    findH264Head(wrapper.mData2, 0, wrapper.readDataSize, offsetList);
                }
            }
            MLog.i(TAG, "handleData() findHead end");
            int offsetCounts = offsetList.size();
            MLog.i(TAG, "handleData() findHead    offsetCounts: " + offsetCounts);
            if (offsetCounts > 1) {
                preReadDataSize = wrapper.readDataSize;
                wrapper.lastOffsetIndex = offsetList.get(offsetCounts - 1);
                restOfDataSize = wrapper.readDataSize - wrapper.lastOffsetIndex;
                MLog.i(TAG, "handleData() findHead    readDataSize: " +
                        wrapper.readDataSize);
                MLog.i(TAG, "handleData() findHead lastOffsetIndex: " +
                        wrapper.lastOffsetIndex);
                MLog.i(TAG, "handleData() findHead  restOfDataSize: " +
                        restOfDataSize);
            } else {
                StringBuilder sb = new StringBuilder();
                for (byte bt : wrapper.mData2) {
                    sb.append(" ");
                    sb.append(bt);
                }
                MLog.d(TAG, "handleData() " + sb.toString());

                break;
            }
            if (wrapper.mIsReading) {
                // 此处发送消息后,readDataSize的大小可能会变化
                synchronized (wrapper.mReadDataLock) {
                    MLog.i(TAG, "handleData() findHead readDataLock.notify()");
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
                    int frameLength = offsetList.get(i + 1) - offsetList.get(i);
                    frameTotal += frameLength;
                    if (frameLength > frameDataLength) {
                        MLog.d(TAG, "handleData() 出现大体积帧 frameLength: " + frameLength);
                        frameDataLength = frameLength;
                        frameData = new byte[frameLength];
                    }
                    // frameData保存着一帧的数据(包括ADTS头和AAC ES(AAC音频数据))
                    System.arraycopy(
                            wrapper.mData2, offsetList.get(i),
                            frameData, 0, frameLength);
                    /*if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "handleData() offset: " + offsetList.get(i) +
                                "    " + frameData[0] +
                                " " + frameData[1] +
                                " " + frameData[2] +
                                " " + frameData[3] +
                                " " + frameData[4] +
                                " " + frameData[5] +
                                " " + frameData[6]);
                        MLog.d(TAG, "handleData() frameLength: " + frameLength);
                    }*/

                    feedInputBufferAndDrainOutputBuffer(
                            wrapper.mDecoderMediaCodec,
                            frameData, 0, frameLength, startDecodeTime,
                            wrapper.render, wrapper.mType == TYPE_AUDIO ? false : true,
                            wrapper.callback);
                } else {
                    // 处理集合中最后一个offset的位置
                    MLog.i(TAG, "handleData() preReadDataSize: " + preReadDataSize);
                    MLog.i(TAG, "handleData()    readDataSize: " + wrapper.readDataSize);
                    MLog.i(TAG, "handleData() lastOffsetIndex: " + wrapper.lastOffsetIndex);
                    MLog.i(TAG, "handleData()  restOfDataSize: " + restOfDataSize);
                    if (restOfDataSize > 0) {
                        System.arraycopy(
                                wrapper.mData2, wrapper.lastOffsetIndex,
                                frameData, 0, restOfDataSize);
                    }

                    // mData1中还有数据
                    if (wrapper.readDataSize != wrapper.CACHE
                            && preReadDataSize == wrapper.readDataSize
                            && wrapper.mReadStatus == READ_FINISHED) {
                        // 最后一帧
                        if (wrapper.CACHE >= 1024 * 1024) {
                            int frameLength = restOfDataSize;
                            MLog.d(TAG, "handleData() last     offset: " + 0 +
                                    "    " + frameData[0] +
                                    " " + frameData[1] +
                                    " " + frameData[2] +
                                    " " + frameData[3] +
                                    " " + frameData[4] +
                                    " " + frameData[5] +
                                    " " + frameData[6]);
                            MLog.d(TAG, "handleData()     frameLength: " + frameLength);
                            frameTotal += frameLength;

                            feedInputBufferAndDrainOutputBuffer(
                                    wrapper.mDecoderMediaCodec,
                                    frameData, 0, frameLength, startDecodeTime,
                                    wrapper.render, wrapper.mType == TYPE_AUDIO ? false : true,
                                    wrapper.callback);

                            isHandlingData = false;
                        }
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

                        // 把mData2中剩下的数据移动到mData2的开头
                        Arrays.fill(wrapper.mData2, (byte) 0);
                        System.arraycopy(
                                frameData, 0,
                                wrapper.mData2, 0, restOfDataSize);
                        // readData1 ---> handleData
                        if (wrapper.readDataSize + restOfDataSize <= wrapper.CACHE) {
                            System.arraycopy(wrapper.mData1, 0,
                                    wrapper.mData2, restOfDataSize, wrapper.readDataSize);
                        } else {
                            wrapper.mData1HasData = true;
                            // mData2已填满数据
                            System.arraycopy(wrapper.mData1, 0,
                                    wrapper.mData2, restOfDataSize, wrapper.CACHE - restOfDataSize);
                            // mData1还剩下(CACHE - lastOffsetIndex)个字节
                            byte[] buffer = new byte[wrapper.CACHE - wrapper.lastOffsetIndex];
                            System.arraycopy(wrapper.mData1, wrapper.lastOffsetIndex,
                                    buffer, 0, wrapper.CACHE - wrapper.lastOffsetIndex);
                            Arrays.fill(wrapper.mData1, (byte) 0);
                            System.arraycopy(buffer, 0,
                                    wrapper.mData1, 0, wrapper.CACHE - wrapper.lastOffsetIndex);
                        }
                        break;
                    }
                }
            }// for(...) end
        }// while(true) end

        if (wrapper.mIsReading) {
            synchronized (wrapper.mReadDataLock) {
                wrapper.mReadDataLock.notify();
            }
        }
        wrapper.mIsReading = false;

        if (wrapper.mAudioTrack != null) {
            wrapper.mAudioTrack.release();
        }

        if (wrapper.mDecoderMediaCodec != null) {
            wrapper.mDecoderMediaCodec.release();
        }

        MLog.w(TAG, "handleData()      frameTotal: " + frameTotal);

        if (DEBUG)
            MLog.w(TAG, "handleData() end");
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
        public void onInputBuffer(ByteBuffer room, MediaUtils.InputBufferInfo info) {

        }

        @Override
        public void onOutputBuffer(ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
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
        public void onInputBuffer(ByteBuffer room, MediaUtils.InputBufferInfo info) {

        }

        @Override
        public void onOutputBuffer(ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
            /*byte[] videoData = new byte[roomSize];
            room.get(videoData, 0, videoData.length);
            MLog.d(TAG, "mVideoCallback videoData.length: " + videoData.length);*/
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
