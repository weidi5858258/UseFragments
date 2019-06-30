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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by weidi on 2019/6/30.
 */

public class SampleAudioPlayer {

    private static final String TAG =
            SampleAudioPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int PREPARE = 0x0001;
    private static final int PLAY = 0x0002;
    private static final int PAUSE = 0x0003;
    private static final int STOP = 0x0004;
    private static final int RELEASE = 0x0005;

    private String mPath;
    private MediaExtractor mMediaExtractor;
    private MediaCodec mAudioDncoderMediaCodec;
    private MediaFormat mAudioDncoderMediaFormat;
    private AudioTrack mAudioTrack;
    private int mAudioTrackIndex = -1;
    private int bufferSizeInBytes;

    private HandlerThread mPlayHandlerThread;
    private Handler mPlayThreadHandler;
    private HandlerThread mOtherHandlerThread;
    private Handler mOtherThreadHandler;
    private Handler mUiHandler;

    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private Object mPauseLock = new Object();
    private Object mStopLock = new Object();

    public SampleAudioPlayer(String path) {
        mPath = path;
        init();
    }

    public SampleAudioPlayer() {
        init();
    }

    public void setPath(String path) {
        mPath = path;
    }

    public void play() {
        mPlayThreadHandler.removeMessages(PLAY);
        mPlayThreadHandler.sendEmptyMessage(PLAY);
    }

    public void pause() {
        mOtherThreadHandler.removeMessages(PAUSE);
        mOtherThreadHandler.sendEmptyMessage(PAUSE);
    }

    public void stop() {
        mOtherThreadHandler.removeMessages(STOP);
        mOtherThreadHandler.sendEmptyMessage(STOP);
    }

    public void release() {

    }

    public void next() {
        if (!mIsRunning) {
            return;
        }
    }

    public void prev() {
        if (!mIsRunning) {
            return;
        }
    }

    public void destroy() {
        if (mPlayHandlerThread != null) {
            mPlayHandlerThread.quit();
            mPlayHandlerThread = null;
        }
        if (mOtherHandlerThread != null) {
            mOtherHandlerThread.quit();
            mOtherHandlerThread = null;
        }
    }

    private void init() {
        mPlayHandlerThread = new HandlerThread(TAG);
        mPlayHandlerThread.start();
        mPlayThreadHandler = new Handler(mPlayHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleAudioPlayer.this.playThreadHandleMessage(msg);
            }
        };
        mOtherHandlerThread = new HandlerThread(TAG);
        mOtherHandlerThread.start();
        mOtherThreadHandler = new Handler(mOtherThreadHandler.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SampleAudioPlayer.this.otherThreadHandleMessage(msg);
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

        // state
        mIsRunning = true;
        mIsPaused = false;

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
        bufferSizeInBytes = MediaUtils.getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat);
        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
    }

    private void internalStart() {
        if (DEBUG)
            MLog.w(TAG, "internalStart() internalStart");

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
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Input
            roomIndex = mAudioDncoderMediaCodec.dequeueInputBuffer(-1);
            if (roomIndex >= 0) {
                room = mAudioDncoderMediaCodec.getInputBuffer(roomIndex);
                room.clear();
                readSize = mMediaExtractor.readSampleData(room, 0);
                long presentationTimeUs = System.nanoTime() / 1000;
                if (readSize < 0) {
                    mAudioDncoderMediaCodec.queueInputBuffer(
                            roomIndex,
                            0,
                            0,
                            presentationTimeUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                    if (DEBUG)
                        MLog.w(TAG, "internalStart() read end");
                    mIsRunning = false;
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
                } catch (IllegalStateException e) {
                    MLog.e(TAG, "internalStart() Output occur exception: " + e);
                    e.printStackTrace();
                }
            }
            //
            if (!mIsRunning) {
                synchronized (mStopLock) {
                    mStopLock.notify();
                }
            }
        }
        if (DEBUG)
            MLog.w(TAG, "internalStart() end");
    }

    private void internalPlay() {
        if (!mIsRunning) {
            internalPrepare();
            internalStart();
        }

        mIsPaused = false;
        synchronized (mPauseLock) {
            mPauseLock.notify();
        }
    }

    private void internalPause() {
        if (!mIsRunning) {
            return;
        }

        mIsPaused = true;
    }

    private void internalStop() {
        if (!mIsRunning) {
            return;
        }

        mIsRunning = false;
        mIsPaused = false;
        synchronized (mPauseLock) {
            mPauseLock.notify();
        }
        synchronized (mStopLock) {
            try {
                mStopLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        MediaUtils.stopAudioTrack(mAudioTrack);
        MediaUtils.stopMediaCodec(mAudioDncoderMediaCodec);
    }

    private void internalRelease() {
        internalStop();
        MediaUtils.releaseAudioTrack(mAudioTrack);
        MediaUtils.releaseMediaCodec(mAudioDncoderMediaCodec);
    }

    private void internalNext() {

    }

    private void internalPrev() {

    }

    private void playThreadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case PREPARE:
                internalPrepare();
                break;
            case PLAY:
                internalPlay();
                break;
            default:
                break;
        }
    }

    private void otherThreadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case PAUSE:
                internalPause();
                break;
            case STOP:
                internalStop();
                break;
            case RELEASE:
                internalRelease();
                break;
            default:
                break;
        }
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }
    }

}
