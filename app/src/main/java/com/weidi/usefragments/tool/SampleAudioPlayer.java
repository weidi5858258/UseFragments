package com.weidi.usefragments.tool;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.weidi.usefragments.media.MediaUtils;

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

    private static final int PLAY = 0x0001;
    private static final int PAUSE = 0x0002;
    private static final int STOP = 0x0003;
    private static final int RELEASE = 0x0004;
    private static final int PREV = 0x0005;
    private static final int NEXT = 0x0006;

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

    public void setPath(String path) {
        mPath = path;
        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
    }

    public void play() {
        mUiHandler.removeMessages(PLAY);
        mUiHandler.sendEmptyMessage(PLAY);

        /*if (!mIsRunning && !mIsPaused) {
            // 目的是解码工作在此线程中工作
            mPlayThreadHandler.removeMessages(PLAY);
            mPlayThreadHandler.sendEmptyMessage(PLAY);
        } else {
            if (mIsPaused) {
                mThreadHandler.removeMessages(PLAY);
                mThreadHandler.sendEmptyMessage(PLAY);
            }
        }*/
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
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
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

        if (DEBUG)
            MLog.d(TAG, "internalPrepare() start");

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
            String mime = mAudioDncoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                try {
                    mAudioDncoderMediaCodec = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                mAudioTrackIndex = i;
                break;
            }
        }
        if (mAudioDncoderMediaCodec == null
                || mAudioDncoderMediaFormat == null
                || mAudioTrackIndex < 0) {
            return;
        }

        mMediaExtractor.selectTrack(mAudioTrackIndex);
        int sampleRateInHz = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mCurFileSize = file.length();
        if (DEBUG)
            MLog.i(TAG, "internalPrepare() mCurFileSize: " + mCurFileSize);
        // 9561547
        // 9525288
        int channelConfig = AudioFormat.CHANNEL_IN_DEFAULT;
        // 有异常,在Android平台上录制音频时可能会设置下面的值
        /*channelConfig = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK);
        if (channelConfig <= 0) {
        }*/
        switch (channelCount) {
            case 1:
                // 如果是单声道的话还不能确定是哪个值
                channelConfig = AudioFormat.CHANNEL_IN_MONO;// 16
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;// 4
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;// 12
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;// 12
                break;
            default:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
        }
        // 还不知道怎样从一个音频中得到"数据位宽"
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);

        if (DEBUG)
            MLog.d(TAG, "internalPrepare() end");
    }

    private void internalStart() {
        if (mAudioTrack == null
                || mAudioDncoderMediaCodec == null
                || mAudioDncoderMediaFormat == null
                || mAudioTrackIndex < 0) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "internalStart() start");

        mAudioTrack.play();
        mAudioDncoderMediaCodec.configure(
                mAudioDncoderMediaFormat, null, null, 0);
        mAudioDncoderMediaCodec.start();

        long hasReadedSize = 0;
        int readSize = -1;
        // 房间编号
        int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
        // 房间
        ByteBuffer room = null;
        // 用于保存房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        while (mIsRunning) {
            // pause device
            if (mIsPaused) {
                synchronized (mPauseLock) {
                    if (DEBUG)
                        MLog.w(TAG, "internalStart() mPauseLock.wait() start");
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (DEBUG)
                        MLog.w(TAG, "internalStart() mPauseLock.wait() end");
                }
            }

            try {
                // Input
                roomIndex = mAudioDncoderMediaCodec.dequeueInputBuffer(-1);
                if (roomIndex >= 0) {
                    room = mAudioDncoderMediaCodec.getInputBuffer(roomIndex);
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
                                presentationTimeUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                        if (DEBUG)
                            MLog.i(TAG, "internalStart() read end");
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
                                presentationTimeUs,
                                0);
                        mMediaExtractor.advance();
                    }
                }
            } catch (MediaCodec.CryptoException
                    | IllegalStateException e) {
                MLog.e(TAG, "internalStart Input occur exception: " + e);
                internalStop(false);
                break;
            }

            // Output
            roomIndex = mAudioDncoderMediaCodec.dequeueOutputBuffer(
                    roomInfo, 10000);
            while (roomIndex >= 0) {
                room = mAudioDncoderMediaCodec.getOutputBuffer(roomIndex);
                // room.limit()与bufferInfo.size的大小是相同的
                // 一帧AAC数据,大小大概为500~550这个范围(每次得到的room大小是不一样的)
                int roomSize = roomInfo.size;
                byte[] pcmData = new byte[roomSize];
                room.get(pcmData, 0, pcmData.length);
                mAudioTrack.write(pcmData, 0, pcmData.length);

                try {
                    mAudioDncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                    roomIndex = mAudioDncoderMediaCodec.dequeueOutputBuffer(
                            roomInfo, 10000);
                } catch (MediaCodec.CryptoException
                        | IllegalStateException e) {
                    MLog.e(TAG, "internalStart() Output occur exception: " + e);
                    e.printStackTrace();
                    internalStop(false);
                    break;
                }
            }

            // stop device
            if (!mIsRunning) {
                synchronized (mStopLock) {
                    mStopLock.notify();
                }
                break;
            }
        }
        if (DEBUG)
            MLog.d(TAG, "internalStart() end");
    }

    private void internalPlay() {
        if (!mIsRunning && !mIsPaused) {
            // state
            mIsRunning = true;
            mIsPaused = false;
            getSingleThreadPool().execute(mPlayRunnable);
        }

        if (mIsRunning) {
            if (DEBUG)
                MLog.d(TAG, "internalPlay() start");

            mIsPaused = false;
            synchronized (mPauseLock) {
                mPauseLock.notify();
            }

            if (DEBUG)
                MLog.d(TAG, "internalPlay() end");
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

        mIsPaused = false;
        synchronized (mPauseLock) {
            mPauseLock.notify();
        }
        if (mIsRunning && needWait) {
            mIsRunning = false;
            synchronized (mStopLock) {
                try {
                    mStopLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mIsRunning = false;

        MediaUtils.stopAudioTrack(mAudioTrack);
        MediaUtils.stopMediaCodec(mAudioDncoderMediaCodec);

        if (DEBUG)
            MLog.d(TAG, "internalStop() end");
    }

    private void internalRelease() {
        if (DEBUG)
            MLog.d(TAG, "internalRelease() start");
        internalStop(true);
        MediaUtils.releaseAudioTrack(mAudioTrack);
        MediaUtils.releaseMediaCodec(mAudioDncoderMediaCodec);
        if (DEBUG)
            MLog.d(TAG, "internalRelease() end");
    }

    private void internalPrev() {
        internalRelease();
        play();
    }

    private void internalNext() {
        internalRelease();
        play();
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
            internalPrepare();
            internalStart();
        }
    };

}
