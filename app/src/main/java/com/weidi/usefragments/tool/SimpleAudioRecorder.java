package com.weidi.usefragments.tool;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.weidi.usefragments.media.MediaUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 Created by root on 19-7-10.
 1.消除回声
 2.抑制噪音
 */

public class SimpleAudioRecorder {

    private static final String TAG =
            SimpleAudioRecorder.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final boolean mNeedToSavePcmData = true;
    private static final String PATH =
            "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Music";
    private static final int TIME_OUT = 10000;
    private static final int PLAY_RECORD = 0x0001;
    private static final int PAUSE_RECORD = 0x0002;
    private static final int STOP_RECORD = 0x0003;

    private Context mContext;
    private MediaCodec mAudioEncoderMediaCodec;
    private MediaFormat mAudioEncoderMediaFormat;
    private AudioRecord mAudioRecord;
    private int bufferSizeInBytes;
    private byte[] mPcmData;
    private boolean mIsRecording = false;
    private boolean mIsPaused = false;
    private Callback mCallback;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;
    private volatile static ExecutorService singleService;

    private Object mPauseLock = new Object();
    private Object mStopLock = new Object();

    // 回声消除器
    private AcousticEchoCanceler mAcousticEchoCanceler;
    // 噪音抑制器
    private NoiseSuppressor mNoiseSuppressor;

    public SimpleAudioRecorder() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public AudioRecord getAudioRecord() {
        return mAudioRecord;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public boolean isPaused() {
        return mIsPaused;
    }

    public void play() {
        if (!mIsRecording) {
            mIsRecording = true;
            mUiHandler.removeMessages(PLAY_RECORD);
            mUiHandler.sendEmptyMessage(PLAY_RECORD);
        } else {
            mThreadHandler.removeMessages(PLAY_RECORD);
            mThreadHandler.sendEmptyMessage(PLAY_RECORD);
        }
    }

    public void pause() {
        mThreadHandler.removeMessages(PAUSE_RECORD);
        mThreadHandler.sendEmptyMessage(PAUSE_RECORD);
    }

    public void stop() {
        mThreadHandler.removeMessages(STOP_RECORD);
        mThreadHandler.sendEmptyMessage(STOP_RECORD);
    }

    private void init() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleAudioRecorder.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleAudioRecorder.this.uiHandleMessage(msg);
            }
        };
    }

    private static ExecutorService getSingleThreadPool() {
        if (singleService == null) {
            synchronized (SimpleAudioPlayer.class) {
                if (singleService == null) {
                    singleService = Executors.newSingleThreadExecutor();
                }
            }
        }
        return singleService;
    }

    private void internalPrepare() {
        if (DEBUG)
            MLog.w(TAG, "internalPrepare() start");

        if (mCallback != null) {
            mCallback.onReady();
        }
        mAudioEncoderMediaCodec = MediaUtils.getAudioEncoderMediaCodec();
        mAudioEncoderMediaFormat = MediaUtils.getAudioEncoderMediaFormat();
        if (mAudioRecord == null) {
            mAudioRecord = MediaUtils.createAudioRecord();
            Assertions.checkNotNull(mAudioRecord != null);
            mAudioRecord.startRecording();
            bufferSizeInBytes = MediaUtils.getMinBufferSize() * 2;
            mPcmData = new byte[bufferSizeInBytes];
        }

        if (mAudioEncoderMediaCodec == null
                || mAudioEncoderMediaFormat == null
                || mAudioRecord == null) {
            mIsRecording = false;
            return;
        }

        // 我的手机不支持
        if (AcousticEchoCanceler.isAvailable()) {
            if (mAcousticEchoCanceler == null
                    && mAudioRecord != null) {
                int audioSession = mAudioRecord.getAudioSessionId();
                mAcousticEchoCanceler = AcousticEchoCanceler.create(audioSession);
                if (mAcousticEchoCanceler != null) {
                    mAcousticEchoCanceler.setEnabled(true);
                    if (DEBUG)
                        MLog.d(TAG, "internalPrepare(): 此手机支持回声消除功能");
                }
            }
        }

        if (DEBUG)
            MLog.w(TAG, "internalPrepare() end");
    }

    private void internalStart() {
        if (!mIsRecording) {
            return;
        }
        if (DEBUG)
            MLog.w(TAG, "internalStart() start");

        File pcmFile = new File(PATH, "test1.pcm");
        File aacFile = new File(PATH, "test1.aac");
        if (pcmFile.exists()) {
            try {
                pcmFile.delete();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        if (aacFile.exists()) {
            try {
                aacFile.delete();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        try {
            pcmFile.createNewFile();
            aacFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            mIsRecording = false;
            return;
        }
        BufferedOutputStream pcmOS = null;
        try {
            pcmOS = new BufferedOutputStream(
                    new FileOutputStream(pcmFile), bufferSizeInBytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mIsRecording = false;
            return;
        }
        BufferedOutputStream aacOS = null;
        try {
            aacOS = new BufferedOutputStream(
                    new FileOutputStream(aacFile), 1024);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mIsRecording = false;
            return;
        }

        int readSize = -1;
        // 房间编号
        int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
        // 房间
        ByteBuffer room = null;
        // 用于保存房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = null;
        ByteBuffer[] outputBuffers = null;
        try {
            inputBuffers = mAudioEncoderMediaCodec.getInputBuffers();
            outputBuffers = mAudioEncoderMediaCodec.getOutputBuffers();
        } catch (IllegalStateException
                | NullPointerException e) {
            e.printStackTrace();
            mIsRecording = false;
            return;
        }
        long startTime = SystemClock.uptimeMillis();
        if (mCallback != null) {
            mCallback.onPlayed();
        }
        while (true) {
            if (mIsPaused) {
                synchronized (mPauseLock) {
                    if (DEBUG)
                        MLog.i(TAG, "internalStart() mPauseLock.wait() start");
                    if (mCallback != null) {
                        mCallback.onPaused();
                    }
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    startTime = SystemClock.uptimeMillis();
                    if (mCallback != null) {
                        mCallback.onPlayed();
                    }
                    if (DEBUG)
                        MLog.i(TAG, "internalStart() mPauseLock.wait() end");
                }
            }

            if (!mIsRecording) {
                if (DEBUG)
                    MLog.w(TAG, "internalStart() while(true) break 1");
                if (mCallback != null) {
                    mCallback.onFinished();
                }
                break;
            }

            if (mAudioRecord != null) {
                Arrays.fill(mPcmData, (byte) 0);
                readSize = mAudioRecord.read(mPcmData, 0, bufferSizeInBytes);
                // MLog.w(TAG, "internalStart() readSize: " + readSize);
            }
            if (readSize < 0) {
                if (DEBUG)
                    MLog.w(TAG, "internalStart() readSize < 0");
                mIsRecording = false;
                if (mCallback != null) {
                    mCallback.onFinished();
                }
                break;
            }

            if (!mIsRecording) {
                if (DEBUG)
                    MLog.w(TAG, "internalStart() while(true) break 2");
                if (mCallback != null) {
                    mCallback.onFinished();
                }
                break;
            }

            // save mPcmData to SD card
            try {
                if (mNeedToSavePcmData) {
                    // pcm裸流
                    pcmOS.write(mPcmData);
                    pcmOS.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                mIsRecording = false;
                break;
            }

            try {
                roomIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(TIME_OUT);
                if (roomIndex >= 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        room = mAudioEncoderMediaCodec.getInputBuffer(roomIndex);
                    } else {
                        room = inputBuffers[roomIndex];
                    }
                    if (room != null) {
                        long presentationTimeUs = System.nanoTime() / 1000;
                        room.clear();
                        room.put(mPcmData);
                        mAudioEncoderMediaCodec.queueInputBuffer(
                                roomIndex, 0, readSize, presentationTimeUs, 0);
                    }
                }
            } catch (IllegalStateException
                    | MediaCodec.CryptoException
                    | NullPointerException e) {
                e.printStackTrace();
                mIsRecording = false;
                break;
            }

            for (; ; ) {
                // 一般循环4次
                try {
                    roomIndex = mAudioEncoderMediaCodec.dequeueOutputBuffer(roomInfo, TIME_OUT);
                    switch (roomIndex) {
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            /*MLog.d(TAG, "mRecordingRunnable " +
                                    "Output MediaCodec.INFO_TRY_AGAIN_LATER");*/
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            MLog.d(TAG, "mRecordingRunnable " +
                                    "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            mAudioEncoderMediaFormat =
                                    mAudioEncoderMediaCodec.getOutputFormat();
                            break;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            MLog.d(TAG, "mRecordingRunnable " +
                                    "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = mAudioEncoderMediaCodec.getOutputBuffers();
                            break;
                        default:
                            break;
                    }
                    if (roomIndex < 0) {
                        break;
                    }

                    if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        MLog.d(TAG, "mRecordingRunnable " +
                                "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                        mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                        continue;
                    } else if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        MLog.d(TAG, "mRecordingRunnable " +
                                "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                        mIsRecording = false;
                        break;
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        room = mAudioEncoderMediaCodec.getOutputBuffer(roomIndex);
                    } else {
                        room = outputBuffers[roomIndex];
                    }

                    //////////////////////////AAC编码操作//////////////////////////
                    if (room != null) {
                        /***
                         room中存放的是aac裸流
                         直接保存aac裸流的话,其他播放器是不知道怎么播放的.
                         必须为每一个aac裸流添加7个字节的头信息后,
                         其他播放器才知道怎么播放.
                         */
                        int roomSize = roomInfo.size;
                        /***
                         roomInfo.offset一直为0
                         置bufferInfo.offset这个位置,
                         意思就是:从room的第bufferInfo.offset个位置开始读数据
                         */
                        room.position(roomInfo.offset);
                        room.limit(roomInfo.offset + roomSize);
                        // 一帧AAC数据和ADTS头的大小
                        int frameSize = roomSize + 7;
                        // MLog.w(TAG, "internalStart() frameSize: " + frameSize);
                        // 空间只能不断地new
                        byte[] aacData = new byte[frameSize];
                        // 先写7个字节的头信息
                        MediaUtils.addADTStoFrame(aacData, frameSize);
                        // 0~6的位置已经有数据了,因此从第7个位置开始写
                        room.get(aacData, 7, roomSize);
                        // save aacData to SD card
                        aacOS.write(aacData, 0, frameSize);
                        aacOS.flush();

                        long endTime = SystemClock.uptimeMillis();
                        if (endTime - startTime >= 1000) {
                            startTime = endTime;
                            if (mCallback != null) {
                                mCallback.onProgressUpdated(endTime * 1000);
                            }
                        }
                        // test 下面这样是不行的
                        /*MLog.w(TAG, "internalStart() roomSize: " + roomSize);
                        byte[] aacData = new byte[roomSize];
                        aacOS.write(aacData, 0, roomSize);
                        aacOS.flush();*/
                    }
                    /////////////////////////////////////////////////////////////

                    mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                } catch (IllegalStateException
                        | MediaCodec.CryptoException
                        | IOException
                        | NullPointerException e) {
                    e.printStackTrace();
                    mIsRecording = false;
                    break;
                }
            }// for(;;) end
        }// while(true) end

        try {
            if (pcmOS != null) {
                pcmOS.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pcmOS != null) {
                try {
                    pcmOS.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    pcmOS = null;
                }
            }
        }

        try {
            if (aacOS != null) {
                aacOS.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (aacOS != null) {
                try {
                    aacOS.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    aacOS = null;
                }
            }
        }

        inputBuffers = null;
        outputBuffers = null;
        MediaUtils.stopAudioRecord(mAudioRecord);
        MediaUtils.stopMediaCodec(mAudioEncoderMediaCodec);
        MediaUtils.releaseMediaCodec(mAudioEncoderMediaCodec);

        if (DEBUG)
            MLog.w(TAG, "internalStart() end");
    }

    private void internalPlay() {
        if (!mIsRecording) {
            return;
        }

        if (DEBUG)
            MLog.w(TAG, "internalPlay() start");

        mIsPaused = false;
        synchronized (mPauseLock) {
            mPauseLock.notify();
        }

        if (DEBUG)
            MLog.w(TAG, "internalPlay() end");
    }

    private void internalPause() {
        if (!mIsRecording) {
            return;
        }

        if (DEBUG)
            MLog.w(TAG, "internalPause() start");

        mIsPaused = true;

        if (DEBUG)
            MLog.w(TAG, "internalPause() end");
    }

    private void internalStop() {
        if (!mIsRecording) {
            return;
        }

        if (DEBUG)
            MLog.w(TAG, "internalStop() start");

        mIsPaused = false;
        mIsRecording = false;
        synchronized (mPauseLock) {
            mPauseLock.notify();
        }

        if (DEBUG)
            MLog.w(TAG, "internalStop() end");
    }

    private void internalRelease() {

    }

    private void releaseAcousticEchoCanceler() {
        if (mAcousticEchoCanceler == null) {
            return;
        }
        mAcousticEchoCanceler.setEnabled(false);
        mAcousticEchoCanceler.release();
        mAcousticEchoCanceler = null;
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case PLAY_RECORD:
                internalPlay();
                break;
            case PAUSE_RECORD:
                internalPause();
                break;
            case STOP_RECORD:
                internalStop();
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
            case PLAY_RECORD:
                getSingleThreadPool().execute(mRecordingRunnable);
                break;
            default:
                break;
        }
    }

    private Runnable mRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            mThreadHandler.removeMessages(STOP_RECORD);
            mThreadHandler.sendEmptyMessageDelayed(STOP_RECORD, 61 * 1000);
            internalPrepare();
            internalStart();
        }
    };

}
