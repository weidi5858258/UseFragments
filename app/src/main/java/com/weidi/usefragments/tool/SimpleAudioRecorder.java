package com.weidi.usefragments.tool;

import android.content.Context;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.weidi.usefragments.media.MediaUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 Created by root on 19-7-10.
 */

public class SimpleAudioRecorder {

    private static final String TAG =
            SimpleAudioRecorder.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int PREPARE = 0x0001;
    private static final int STOP_RECORD = 0x0002;
    private static final int PCM_TO_WAV = 0x0003;

    private Context mContext;
    private AudioRecord mAudioRecord;
    private int bufferSizeInBytes;
    private byte[] mPcmData;
    private boolean mIsRecording = false;
    private boolean mIsPaused = false;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;
    private volatile static ExecutorService singleService;

    private Object mPauseLock = new Object();
    private Object mStopLock = new Object();

    public SimpleAudioRecorder() {
        init();
    }

    public void setContext(Context context) {
        mContext = context;
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

        mThreadHandler.sendEmptyMessage(PREPARE);
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

    private void internalPrepare() {
        mAudioRecord = MediaUtils.createAudioRecord();
        Assertions.checkNotNull(mAudioRecord != null);
        bufferSizeInBytes = MediaUtils.getMinBufferSize() * 2;
        mPcmData = new byte[bufferSizeInBytes];
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case PREPARE:
                internalPrepare();
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
            default:
                break;
        }
    }

    private Runnable mRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            int readSize = -1;
            while (true) {
                if (!mIsRecording) {
                    break;
                }

                if (mIsPaused) {
                    synchronized (mPauseLock) {
                        try {
                            mPauseLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                mAudioRecord.read(mPcmData, 0, bufferSizeInBytes);
                if (readSize < 0) {
                    mIsRecording = false;
                    break;
                }
            }
        }
    };

}
