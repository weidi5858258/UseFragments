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
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.receiver.MediaButtonReceiver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/***
 Created by weidi on 2019/7/10.
 */

public class SampleVideoPlayer {

    private static final String TAG =
            SampleVideoPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int TIME_OUT = 10000;
    private static final int PREPARE = 0x0001;
    private static final int PLAY = 0x0002;
    private static final int PAUSE = 0x0003;
    private static final int STOP = 0x0004;
    private static final int RELEASE = 0x0005;
    private static final int PREV = 0x0006;
    private static final int NEXT = 0x0007;

    private static final int SEEKBAR_PROGRESS = 3840;

    // 为了注册广播
    private Context mContext;
    private String mPath;
    // 必须要有两个MediaExtractor对象,不能共用同一个
    private MediaExtractor mAudioExtractor;
    private MediaExtractor mVideoExtractor;
    private MediaCodec mAudioDncoderMediaCodec;
    private MediaCodec mVideoDncoderMediaCodec;
    private MediaFormat mAudioDncoderMediaFormat;
    private MediaFormat mVideoDncoderMediaFormat;
    private AudioTrack mAudioTrack;
    private Surface mSurface;
    private int mAudioTrackIndex = -1;
    private int mVideoTrackIndex = -1;
    private long mDurationUs;
    private long mCurPositionUs;
    private long mProgressUs = -1;
    private boolean mIsAudioDurationUsUsed = true;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private boolean mIsAudioRunning = false;
    private boolean mIsVideoRunning = false;
    private boolean mIsAudioPaused = false;
    private boolean mIsVideoPaused = false;
    private Object mAudioPauseLock = new Object();
    private Object mVideoPauseLock = new Object();
    private Object mAudioStopLock = new Object();
    private Object mVideoStopLock = new Object();

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

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public SampleVideoPlayer(String path) {
        mPath = path;
        init();
    }

    public SampleVideoPlayer() {
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
        mPath = "/storage/2430-1702/BaiduNetdisk/video/05.mp4";
        mPath = "/storage/2430-1702/BaiduNetdisk/video/08_mm-MP4-H264_720x400_2997_AAC" +
                "-LC_192_48.mp4";
        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void play() {
        mThreadHandler.removeMessages(PLAY);
        mThreadHandler.sendEmptyMessage(PLAY);

        // mThreadHandler.sendEmptyMessage(PREPARE);
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
        return mIsAudioRunning && mIsVideoRunning;
    }

    public long getDurationUs() {
        return mDurationUs;
    }

    public long getCurPositionUs() {
        return mCurPositionUs;
    }

    public void setProgressUs(long progressUs) {
        mProgressUs = progressUs;
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

    private void init() {
        EventBusUtils.register(this);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleVideoPlayer.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleVideoPlayer.this.uiHandleMessage(msg);
            }
        };
    }

    private void internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            return;
        }

        if (mCallback != null) {
            mCallback.onPlaybackReady();
        }
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() start");

        File file = new File(mPath);
        if (!file.canRead()
                || file.isDirectory()) {
            if (DEBUG)
                MLog.e(TAG, "不能读取此文件: " + mPath);
            return;
        }
        long fileSize = file.length();
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() fileSize: " + fileSize);

        mAudioExtractor = null;
        mVideoExtractor = null;
        mAudioDncoderMediaCodec = null;
        mVideoDncoderMediaCodec = null;
        mAudioDncoderMediaFormat = null;
        mVideoDncoderMediaFormat = null;
        mAudioTrackIndex = -1;
        mVideoTrackIndex = -1;

        // Audio
        mAudioExtractor = new MediaExtractor();
        try {
            mAudioExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            mIsAudioRunning = false;
            return;
        }
        int trackCount = mAudioExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mAudioDncoderMediaFormat = mAudioExtractor.getTrackFormat(i);
            String mime = mAudioDncoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                MLog.d(TAG, "internalPrepare() audio mime: " + mime);
                MLog.d(TAG, "internalPrepare() mAudioDncoderMediaFormat: " +
                        mAudioDncoderMediaFormat.toString());
                boolean hasException = false;
                try {
                    mAudioExtractor.selectTrack(i);
                    mAudioDncoderMediaCodec = MediaCodec.createDecoderByType(mime);
                    mAudioDncoderMediaCodec.configure(
                            mAudioDncoderMediaFormat, null, null, 0);
                    mAudioDncoderMediaCodec.start();
                    mAudioTrackIndex = i;
                } catch (MediaCodec.CryptoException
                        | IllegalStateException
                        | IllegalArgumentException
                        | IOException e) {
                    e.printStackTrace();
                    hasException = true;
                }
                if (hasException) {
                    hasException = false;
                    mAudioDncoderMediaCodec = null;
                    try {
                        mAudioExtractor.selectTrack(i);
                        MediaCodecInfo mediaCodecInfo = MediaUtils.getDecoderMediaCodecInfo(mime);
                        String codecName = null;
                        if (mediaCodecInfo != null) {
                            codecName = mediaCodecInfo.getName();
                        } else {
                            if (TextUtils.equals("audio/ac4", mime)) {
                                codecName = "OMX.google.raw.decoder";
                                mAudioDncoderMediaFormat.setString(
                                        MediaFormat.KEY_MIME, "audio/raw");
                            }
                        }
                        if (!TextUtils.isEmpty(codecName)) {
                            mAudioDncoderMediaCodec =
                                    MediaCodec.createByCodecName(codecName);
                            mAudioDncoderMediaCodec.configure(
                                    mAudioDncoderMediaFormat, null, null, 0);
                            mAudioDncoderMediaCodec.start();
                            mAudioTrackIndex = i;
                        }
                    } catch (MediaCodec.CryptoException
                            | IllegalStateException
                            | IllegalArgumentException
                            | IOException e) {
                        e.printStackTrace();
                        hasException = true;
                    }
                    if (hasException) {
                        if (mAudioDncoderMediaCodec != null) {
                            mAudioDncoderMediaCodec.release();
                        }
                        mAudioDncoderMediaCodec = null;
                        mAudioDncoderMediaFormat = null;
                        mAudioTrackIndex = -1;
                    }
                }
                break;
            }
        }

        // Video
        mVideoExtractor = new MediaExtractor();
        try {
            mVideoExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            mIsVideoRunning = false;
            return;
        }
        trackCount = mVideoExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mVideoDncoderMediaFormat = mVideoExtractor.getTrackFormat(i);
            String mime = mVideoDncoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                MLog.d(TAG, "internalPrepare() video mime: " + mime);
                MLog.d(TAG, "internalPrepare() mVideoDncoderMediaFormat: " +
                        mVideoDncoderMediaFormat.toString());
                boolean hasException = false;
                try {
                    mVideoExtractor.selectTrack(i);
                    mVideoDncoderMediaCodec = MediaCodec.createDecoderByType(mime);
                    mVideoDncoderMediaCodec.configure(
                            mVideoDncoderMediaFormat, mSurface, null, 0);
                    mVideoDncoderMediaCodec.start();
                    mVideoTrackIndex = i;
                } catch (MediaCodec.CryptoException
                        | IllegalStateException
                        | IllegalArgumentException
                        | IOException e) {
                    e.printStackTrace();
                    hasException = true;
                }
                if (hasException) {
                    if (mVideoDncoderMediaCodec != null) {
                        mVideoDncoderMediaCodec.release();
                    }
                    mVideoDncoderMediaCodec = null;
                    mVideoDncoderMediaFormat = null;
                    mVideoTrackIndex = -1;
                }
                break;
            }
        }

        if (mAudioDncoderMediaCodec == null
                || mVideoDncoderMediaCodec == null
                || mAudioDncoderMediaFormat == null
                || mVideoDncoderMediaFormat == null
                || mAudioTrackIndex < 0
                || mVideoTrackIndex < 0) {
            mIsAudioRunning = false;
            mIsVideoRunning = false;
            /*if (mCallback != null) {
                mCallback.onPlaybackFinished();
            }*/
            return;
        }

        long audioDurationUs = mAudioDncoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() audioDurationUs: " + audioDurationUs);
        long videoDurationUs = mVideoDncoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() videoDurationUs: " + videoDurationUs);
        if (audioDurationUs > 0 && videoDurationUs > 0) {
            mDurationUs = Math.min(audioDurationUs, videoDurationUs);
            if (mDurationUs == audioDurationUs) {
                mIsAudioDurationUsUsed = true;
            } else {
                mIsAudioDurationUsUsed = false;
            }
            if (DEBUG)
                MLog.d(TAG, "internalPrepare() mDurationUs:     " + mDurationUs);
        }

        int sampleRateInHz = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioTrack != null) {
            mAudioTrack.play();
        } /*else {
            mIsAudioRunning = false;
            *//*if (mCallback != null) {
                mCallback.onPlaybackFinished();
            }*//*
            return;
        }*/

        if (DEBUG)
            MLog.d(TAG, "internalPrepare() end");
    }

    private void internalStart() {
        if (!mIsAudioRunning
                || !mIsVideoRunning) {
            return;
        }

        if (mCallback != null) {
            mCallback.onPlaybackStarted();
        }
        if (DEBUG)
            MLog.d(TAG, "internalStart() start");

        new Thread(mAudioPlayRunnable).start();
        new Thread(mVideoPlayRunnable).start();

        if (DEBUG)
            MLog.d(TAG, "internalStart() end");
    }

    private void internalPlay() {
        if (DEBUG)
            MLog.d(TAG, "internalPlay() start");

        MLog.d(TAG, "internalPlay() mIsAudioRunning: " + mIsAudioRunning +
                " mIsVideoRunning: " + mIsVideoRunning);
        if (!mIsAudioRunning
                && !mIsVideoRunning
                && !mIsAudioPaused
                && !mIsVideoPaused) {
            if (DEBUG)
                MLog.d(TAG, "internalPlay() execute AudioRunnable and VideoRunnable");

            // state
            mIsAudioRunning = true;
            mIsVideoRunning = true;
            mIsAudioPaused = false;
            mIsVideoPaused = false;
            internalPrepare();
            internalStart();
        }

        if (mIsAudioRunning
                && mIsVideoRunning) {
            mIsAudioPaused = false;
            synchronized (mAudioPauseLock) {
                mAudioPauseLock.notify();
            }

            mIsVideoPaused = false;
            synchronized (mVideoPauseLock) {
                mVideoPauseLock.notify();
            }
        }

        if (DEBUG)
            MLog.d(TAG, "internalPlay() end");
    }

    private void internalPause() {
        if (!mIsAudioRunning
                || !mIsVideoRunning) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "internalPause() start");

        mIsAudioPaused = true;
        mIsVideoPaused = true;

        if (DEBUG)
            MLog.d(TAG, "internalPause() end");
    }

    private void internalStop() {
        if (!mIsAudioRunning
                || !mIsVideoRunning) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "internalStop() start");

        mIsAudioPaused = false;
        synchronized (mAudioPauseLock) {
            mAudioPauseLock.notify();
        }

        mIsVideoPaused = false;
        synchronized (mVideoPauseLock) {
            mVideoPauseLock.notify();
        }

        notifyAudioEndOfStream();
        notifyVideoEndOfStream();

        if (DEBUG)
            MLog.d(TAG, "internalStop() end");
    }

    private void internalAudioRelease() {
        if (DEBUG)
            MLog.d(TAG, "internalAudioRelease() start");
        MediaUtils.stopAudioTrack(mAudioTrack);
        MediaUtils.releaseAudioTrack(mAudioTrack);
        MediaUtils.stopMediaCodec(mAudioDncoderMediaCodec);
        MediaUtils.releaseMediaCodec(mAudioDncoderMediaCodec);
        if (DEBUG)
            MLog.d(TAG, "internalAudioRelease() end");
    }

    private void internalVideoRelease() {
        if (DEBUG)
            MLog.d(TAG, "internalVideoRelease() start");
        MediaUtils.stopMediaCodec(mVideoDncoderMediaCodec);
        MediaUtils.releaseMediaCodec(mVideoDncoderMediaCodec);
        if (DEBUG)
            MLog.d(TAG, "internalVideoRelease() end");
    }

    private void internalPrev() {
        internalStop();
        if (mIsAudioRunning) {
            synchronized (mAudioStopLock) {
                try {
                    mAudioStopLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        play();
    }

    private void internalNext() {
        internalStop();
        if (mIsAudioRunning) {
            synchronized (mAudioStopLock) {
                if (DEBUG)
                    MLog.d(TAG, "internalNext() mAudioStopLock.wait() start");
                try {
                    mAudioStopLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (DEBUG)
                    MLog.d(TAG, "internalNext() mAudioStopLock.wait() end");
            }
        }
        if (mIsVideoRunning) {
            synchronized (mVideoStopLock) {
                if (DEBUG)
                    MLog.d(TAG, "internalNext() mVideoStopLock.wait() start");
                try {
                    mVideoStopLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (DEBUG)
                    MLog.d(TAG, "internalNext() mVideoStopLock.wait() end");
            }
        }
        mIsAudioRunning = false;
        mIsVideoRunning = false;
        mThreadHandler.removeMessages(PLAY);
        mThreadHandler.sendEmptyMessageDelayed(PLAY, 1000);
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

    /***
     相当于发送一个消息
     */
    private void notifyAudioEndOfStream() {
        if (DEBUG)
            MLog.d(TAG, "notifyAudioEndOfStream() start");

        if (mAudioDncoderMediaCodec != null) {
            long startTime = SystemClock.uptimeMillis();
            try {
                for (; ; ) {
                    int roomIndex = mAudioDncoderMediaCodec.dequeueInputBuffer(0);
                    if (roomIndex >= 0) {
                        mAudioDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        break;
                    }
                    if (SystemClock.uptimeMillis() - startTime >= 5000) {
                        mIsAudioRunning = false;
                        return;
                    }
                }
            } catch (MediaCodec.CryptoException
                    | IllegalStateException e) {
                e.printStackTrace();
                mIsAudioRunning = false;
                return;
            }
        } else {
            mIsAudioRunning = false;
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "notifyAudioEndOfStream() end");
    }

    private void notifyVideoEndOfStream() {
        if (DEBUG)
            MLog.d(TAG, "notifyVideoEndOfStream() start");

        if (mVideoDncoderMediaCodec != null) {
            long startTime = SystemClock.uptimeMillis();
            try {
                for (; ; ) {
                    int roomIndex = mVideoDncoderMediaCodec.dequeueInputBuffer(0);
                    if (roomIndex >= 0) {
                        mVideoDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        break;
                    }
                    if (SystemClock.uptimeMillis() - startTime >= 5000) {
                        mIsVideoRunning = false;
                        return;
                    }
                }
            } catch (MediaCodec.CryptoException
                    | IllegalStateException e) {
                e.printStackTrace();
                mIsVideoRunning = false;
                return;
            }
        } else {
            mIsVideoRunning = false;
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "notifyVideoEndOfStream() end");
    }

    /***
     * 从socket读byte数组
     *
     * @param in
     * @param length
     * @return
     */
    private static byte[] readBytes(InputStream in, long length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        while (read < length) {
            int cur = 0;
            try {
                cur = in.read(buffer, 0, (int) Math.min(1024, length - read));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (cur < 0) {
                break;
            }
            read += cur;
            baos.write(buffer, 0, cur);
        }
        return baos.toByteArray();
    }

    private int bytesToInt(byte[] bytes) {
        int i = 0;
        i = (int) ((bytes[0] & 0xff)
                | ((bytes[1] & 0xff) << 8)
                | ((bytes[2] & 0xff) << 16)
                | ((bytes[3] & 0xff) << 24));
        return i;
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case PREPARE:
                internalPrepare();
                mUiHandler.sendEmptyMessage(PLAY);
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
                    if (mIsAudioRunning) {
                        if (mIsAudioPaused) {
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

    private Runnable mAudioPlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (DEBUG)
                MLog.d(TAG, "mAudioPlayRunnable start");

            int frameDataLength = 1024 * 100;
            byte[] frameData = new byte[frameDataLength];
            // long presentationTimeUs = System.nanoTime() / 1000;
            long startTimeUs = 0;
            String prevElapsedTime = null;
            String curElapsedTime = null;
            boolean hasPlaybackFinished = false;
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
            mAudioExtractor.selectTrack(mAudioTrackIndex);
            /***
             总体思想:
             播放完毕,播放异常,停止播放
             都设置
             mIsAudioRunning = false;
             然后退出while循环,
             最后release.
             */
            while (true) {
                // stop device
                if (!mIsAudioRunning || !mIsVideoRunning) {
                    mIsAudioRunning = false;
                    break;
                }

                // pause device
                if (mIsAudioPaused) {
                    synchronized (mAudioPauseLock) {
                        if (DEBUG)
                            MLog.w(TAG, "mAudioPlayRunnable mAudioPauseLock.wait() start");
                        try {
                            mAudioPauseLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (DEBUG)
                            MLog.w(TAG, "mAudioPlayRunnable mAudioPauseLock.wait() end");
                    }
                }

                // seekTo
                if (mProgressUs != -1) {
                    mAudioExtractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    mProgressUs = -1;
                    startTimeUs = 0;
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
                        if (room != null) {
                            room.clear();
                            readSize = mAudioExtractor.readSampleData(room, 0);
                            if (readSize < 0) {
                                mIsAudioRunning = false;
                                break;
                            }
                            room.position(0);
                            room.limit(readSize);
                            room.get(frameData, 0, readSize);
                            MLog.d(TAG, "mAudioPlayRunnable " +
                                    "    " + frameData[0] +
                                    " " + frameData[1] +
                                    " " + frameData[2] +
                                    " " + frameData[3] +
                                    " " + frameData[4] +
                                    " " + frameData[5] +
                                    " " + frameData[6]);
                            MLog.d(TAG, "mAudioPlayRunnable frameLength: " + readSize);
                        }
                        int flags = 0;
                        /***
                         第一次为0
                         然后大概26ms读一次数据(26122us)
                         结束为-1
                         */
                        long presentationTimeUs = mAudioExtractor.getSampleTime();
                        if (mIsAudioDurationUsUsed) {
                            mCurPositionUs = presentationTimeUs;
                        }
                        if (mIsAudioDurationUsUsed
                                && presentationTimeUs != -1
                                // 过一秒才更新
                                && presentationTimeUs - startTimeUs >= 1000000) {
                            startTimeUs = presentationTimeUs;

                            curElapsedTime = DateUtils.formatElapsedTime(
                                    (presentationTimeUs / 1000) / 1000);
                            if (mCallback != null
                                    // 防止重复更新
                                    && !TextUtils.equals(curElapsedTime, prevElapsedTime)) {
                                prevElapsedTime = curElapsedTime;
                                mCallback.onProgressUpdated(presentationTimeUs);
                            }
                        } else if (presentationTimeUs == -1) {// game over
                            if (!hasPlaybackFinished) {
                                hasPlaybackFinished = true;
                            } else {
                                // 起保护作用
                                mIsAudioRunning = false;
                                break;
                            }
                            readSize = 0;
                            presentationTimeUs = 0;
                            /***
                             发送此flags(MediaCodec.BUFFER_FLAG_END_OF_STREAM)的目的是,
                             在Output阶段,根据
                             (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                             这个条件判断已经到结尾了,然后退出.这是优雅的退出方式.
                             */
                            flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;

                            if (mCallback != null) {
                                mCallback.onProgressUpdated(mDurationUs);
                            }
                            if (DEBUG)
                                MLog.i(TAG, "mAudioPlayRunnable read end");
                        }

                        mAudioDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                readSize,
                                presentationTimeUs,
                                flags);

                        if (!hasPlaybackFinished) {
                            mAudioExtractor.advance();
                        }
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException
                        | NullPointerException e) {
                    MLog.e(TAG, "mAudioPlayRunnable Input occur exception: " + e);

                    mIsAudioRunning = false;
                    break;
                }


                // Output
                for (; ; ) {
                    // stop device
                    if (!mIsAudioRunning || !mIsVideoRunning) {
                        mIsAudioRunning = false;
                        break;
                    }
                    try {
                        roomIndex = mAudioDncoderMediaCodec.dequeueOutputBuffer(
                                roomInfo, TIME_OUT);
                        switch (roomIndex) {
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                mAudioDncoderMediaFormat =
                                        mAudioDncoderMediaCodec.getOutputFormat();
                                MLog.d(TAG, "mAudioPlayRunnable " +
                                        "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                                MLog.d(TAG, "mAudioPlayRunnable " + mAudioDncoderMediaFormat);
                                if (mAudioTrack != null) {
                                    break;
                                }
                                int sampleRateInHz =
                                        mAudioDncoderMediaFormat.getInteger(
                                                MediaFormat.KEY_SAMPLE_RATE);
                                int channelCount =
                                        mAudioDncoderMediaFormat.getInteger(
                                                MediaFormat.KEY_CHANNEL_COUNT);
                                int audioFormat =
                                        mAudioDncoderMediaFormat.getInteger(
                                                MediaFormat.KEY_PCM_ENCODING);
                                mAudioTrack = MediaUtils.createAudioTrack(
                                        AudioManager.STREAM_MUSIC,
                                        sampleRateInHz, channelCount, audioFormat,
                                        AudioTrack.MODE_STREAM);
                                if (mAudioTrack != null) {
                                    mAudioTrack.play();
                                } else {
                                    // mIsAudioRunning = false;
                                    MLog.d(TAG, "mAudioPlayRunnable " +
                                            "mAudioTrack is null");
                                }
                                break;
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                MLog.d(TAG, "mAudioPlayRunnable " +
                                        "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                    outputBuffers = mAudioDncoderMediaCodec.getOutputBuffers();
                                }
                                break;
                            default:
                                break;
                        }
                        if (roomIndex < 0) {
                            break;
                        }

                        if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            MLog.d(TAG, "mAudioPlayRunnable " +
                                    "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                            mAudioDncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                            continue;
                        }
                        // 非常重要
                        if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            MLog.d(TAG, "mAudioPlayRunnable " +
                                    "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");

                            mIsAudioRunning = false;
                            break;
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            room = mAudioDncoderMediaCodec.getOutputBuffer(roomIndex);
                        } else {
                            room = outputBuffers[roomIndex];
                        }
                        if (room != null) {
                            // room.limit()与bufferInfo.size的大小是相同的
                            // 一帧AAC数据,大小大概为500~550这个范围(每次得到的room大小是不一样的)
                            roomSize = roomInfo.size;
                            room.position(roomInfo.offset);
                            room.limit(roomInfo.offset + roomSize);
                            byte[] pcmData = new byte[roomSize];
                            room.get(pcmData, 0, pcmData.length);
                            if (mAudioTrack != null) {
                                mAudioTrack.write(pcmData, 0, pcmData.length);
                            }
                        }

                        mAudioDncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                    } catch (IllegalStateException
                            | IllegalArgumentException
                            | NullPointerException e) {
                        MLog.e(TAG, "mAudioPlayRunnable Output occur exception: " + e);

                        mIsAudioRunning = false;
                        break;
                    }
                }// for(;;) end
            }// while(true) end

            internalAudioRelease();

            synchronized (mAudioStopLock) {
                if (DEBUG)
                    MLog.d(TAG, "mAudioPlayRunnable mAudioStopLock.notify()");
                mAudioStopLock.notify();
            }
            synchronized (mVideoStopLock) {
                if (DEBUG)
                    MLog.d(TAG, "mAudioPlayRunnable mVideoStopLock.notify()");
                mVideoStopLock.notify();
            }

            if (hasPlaybackFinished) {
                if (mCallback != null) {
                    mCallback.onPlaybackFinished();
                }
            }

            MLog.d(TAG, "mAudioPlayRunnable mIsAudioRunning: " + mIsAudioRunning);

            if (DEBUG)
                MLog.d(TAG, "mAudioPlayRunnable end");
        }
    };

    private Runnable mVideoPlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (DEBUG)
                MLog.d(TAG, "mVideoPlayRunnable start");

            // long presentationTimeUs = System.nanoTime() / 1000;
            long startTimeMs = System.currentTimeMillis();
            long startTimeUs = 0;
            String prevElapsedTime = null;
            String curElapsedTime = null;
            boolean hasPlaybackFinished = false;
            int readSize = -1;
            // 房间号
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            // 房间
            ByteBuffer room = null;
            // 房间大小
            int roomSize = 0;
            // 房间信息
            MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = mVideoDncoderMediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mVideoDncoderMediaCodec.getOutputBuffers();
            mVideoExtractor.selectTrack(mVideoTrackIndex);
            // 保持纵横比
            // 此方法必须在configure和start之后执行才有效
            mVideoDncoderMediaCodec.setVideoScalingMode(
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            /***
             总体思想:
             播放完毕,播放异常,停止播放
             都设置
             mIsVideoRunning = false;
             然后退出while循环,
             最后release.
             */
            while (true) {
                // stop device
                if (!mIsAudioRunning || !mIsVideoRunning) {
                    mIsVideoRunning = false;
                    break;
                }

                // pause device
                if (mIsVideoPaused) {
                    synchronized (mVideoPauseLock) {
                        if (DEBUG)
                            MLog.w(TAG, "mVideoPlayRunnable mVideoPauseLock.wait() start");
                        try {
                            mVideoPauseLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (DEBUG)
                            MLog.w(TAG, "mVideoPlayRunnable mVideoPauseLock.wait() end");
                    }
                }

                // seekTo
                if (mProgressUs != -1) {
                    mVideoExtractor.seekTo(mProgressUs, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    mProgressUs = -1;
                    startTimeUs = 0;
                }

                try {
                    // Input
                    // 设置解码等待时间，0为不等待，-1为一直等待，其余为时间单位
                    roomIndex = mVideoDncoderMediaCodec.dequeueInputBuffer(-1);
                    if (roomIndex >= 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            room = mVideoDncoderMediaCodec.getInputBuffer(roomIndex);
                        } else {
                            room = inputBuffers[roomIndex];
                        }
                        if (room != null) {
                            room.clear();
                            readSize = mVideoExtractor.readSampleData(room, 0);
                        }
                        int flags = 0;
                        /***
                         第一次为0
                         然后大概26ms读一次数据(26122us)
                         结束为-1
                         */
                        long presentationTimeUs = mVideoExtractor.getSampleTime();
                        if (!mIsAudioDurationUsUsed) {
                            mCurPositionUs = presentationTimeUs;
                        }
                        if (!mIsAudioDurationUsUsed
                                && presentationTimeUs != -1
                                // 过一秒才更新
                                && presentationTimeUs - startTimeUs >= 1000000) {
                            startTimeUs = presentationTimeUs;

                            curElapsedTime = DateUtils.formatElapsedTime(
                                    (presentationTimeUs / 1000) / 1000);
                            if (mCallback != null
                                    // 防止重复更新
                                    && !TextUtils.equals(curElapsedTime, prevElapsedTime)) {
                                prevElapsedTime = curElapsedTime;
                                mCallback.onProgressUpdated(presentationTimeUs);
                            }
                        } else if (presentationTimeUs == -1) {// game over
                            if (!hasPlaybackFinished) {
                                hasPlaybackFinished = true;
                            } else {
                                // 起保护作用
                                mIsAudioRunning = false;
                                break;
                            }
                            readSize = 0;
                            presentationTimeUs = 0;
                            /***
                             发送此flags(MediaCodec.BUFFER_FLAG_END_OF_STREAM)的目的是,
                             在Output阶段,根据
                             (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                             这个条件判断已经到结尾了,然后退出.这是优雅的退出方式.
                             */
                            flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;

                            /*if (mCallback != null) {
                                mCallback.onProgressUpdated(mDurationUs);
                            }*/
                            if (DEBUG)
                                MLog.i(TAG, "mVideoPlayRunnable read end");
                        }

                        mVideoDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                readSize,
                                presentationTimeUs,
                                flags);

                        if (!hasPlaybackFinished) {
                            mVideoExtractor.advance();
                        }
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException
                        | NullPointerException e) {
                    MLog.e(TAG, "mVideoPlayRunnable Input occur exception: " + e);

                    mIsVideoRunning = false;
                    break;
                }


                // Output
                for (; ; ) {
                    // stop device
                    if (!mIsAudioRunning || !mIsVideoRunning) {
                        mIsVideoRunning = false;
                        break;
                    }
                    try {
                        roomIndex = mVideoDncoderMediaCodec.dequeueOutputBuffer(
                                roomInfo, TIME_OUT);
                        switch (roomIndex) {
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                mVideoDncoderMediaFormat =
                                        mVideoDncoderMediaCodec.getOutputFormat();
                                MLog.d(TAG, "mVideoPlayRunnable " +
                                        "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                                MLog.d(TAG, "mVideoPlayRunnable " + mVideoDncoderMediaFormat);
                                break;
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                MLog.d(TAG, "mVideoPlayRunnable " +
                                        "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                    outputBuffers = mVideoDncoderMediaCodec.getOutputBuffers();
                                }
                                break;
                            default:
                                break;
                        }
                        if (roomIndex < 0) {
                            break;
                        }

                        if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            MLog.d(TAG, "mVideoPlayRunnable " +
                                    "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                            mVideoDncoderMediaCodec.releaseOutputBuffer(roomIndex, true);
                            continue;
                        }
                        // 非常重要
                        if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            MLog.d(TAG, "mVideoPlayRunnable " +
                                    "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");

                            mIsVideoRunning = false;
                            break;
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            room = mVideoDncoderMediaCodec.getOutputBuffer(roomIndex);
                        } else {
                            room = outputBuffers[roomIndex];
                        }
                        /***
                         roomInfo.presentationTimeUs        ---> 微妙
                         roomInfo.presentationTimeUs / 1000 ---> 毫秒
                         roomInfo.presentationTimeUs / 1000 某帧将要显示的时间点
                         System.currentTimeMillis()         当前的时间点
                         这样去理解:
                         某个产品只需要在明天的某个时间点完成就行了,
                         但是实际是这个产品在今天的某个时间点就完成了,
                         这就说明了工作麻利,提前完成任务了,也就是"快"的意思.
                         类比现在的情况:
                         "某帧将要显示的时间点"比"当前的时间点"大,
                         说明显示过早了,应该等一等再显示.
                         */
                        while (roomInfo.presentationTimeUs / 1000
                                > System.currentTimeMillis() - startTimeMs) {
                            SystemClock.sleep(10);
                        }

                        mVideoDncoderMediaCodec.releaseOutputBuffer(roomIndex, true);
                    } catch (IllegalStateException
                            | IllegalArgumentException
                            | NullPointerException e) {
                        MLog.e(TAG, "mVideoPlayRunnable Output occur exception: " + e);

                        mIsVideoRunning = false;
                        break;
                    }
                }// for(;;) end
            }// while(true) end

            internalVideoRelease();

            synchronized (mAudioStopLock) {
                if (DEBUG)
                    MLog.d(TAG, "mVideoPlayRunnable mAudioStopLock.notify()");
                mAudioStopLock.notify();
            }
            synchronized (mVideoStopLock) {
                if (DEBUG)
                    MLog.d(TAG, "mVideoPlayRunnable mVideoStopLock.notify()");
                mVideoStopLock.notify();
            }

            if (hasPlaybackFinished) {
                if (mCallback != null) {
                    mCallback.onPlaybackFinished();
                }
            }

            MLog.d(TAG, "mVideoPlayRunnable mIsVideoRunning: " + mIsVideoRunning);

            if (DEBUG)
                MLog.d(TAG, "mVideoPlayRunnable end");
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
