/*
 * Copyright (c) 2017 Yrom Wang <http://www.yrom.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.weidi.usefragments.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseLongArray;

import com.weidi.usefragments.media.encoder.AudioEncodeConfig;
import com.weidi.usefragments.media.encoder.BaseEncoder;
import com.weidi.usefragments.tool.MLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;
import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;

/***

 */
public class CustomAudioRecord {

    private static final String TAG =
            CustomAudioRecord.class.getSimpleName();
    private static final boolean DEBUG = true;

    private MediaCodec mAudioEncoder;

    CustomAudioRecord(AudioEncodeConfig config) {
        mSampleRate = config.mSampleRate;
        mChannelsSampleRate = mSampleRate * config.mChannelCount;
        if (DEBUG) Log.i(TAG, "in bitrate " + mChannelsSampleRate * 16 /* PCM_16BIT*/);
        //        mCallbackDelegate = new CallbackDelegate(myLooper, mCallback);

        mRecordThread = new HandlerThread(TAG);
        mRecordThread.start();
        mRecordHandler = new RecordHandler(mRecordThread.getLooper());
    }

    private HandlerThread mRecordThread;
    private RecordHandler mRecordHandler;
    private AudioRecord mAudioRecord;
    private int mSampleRate;
    private int mFormat = AudioFormat.ENCODING_PCM_16BIT;

    private AtomicBoolean mForceStopFlag = new AtomicBoolean(false);
    //    private CallbackDelegate mCallbackDelegate;
    private int mChannelsSampleRate;

    public void prepare() throws IOException {
        if (mRecordHandler != null) {
            mRecordHandler.removeMessages(MSG_THREAD_PREPARE);
            mRecordHandler.sendEmptyMessage(MSG_THREAD_PREPARE);
        }
    }

    public void start() {
        mForceStopFlag.set(false);
        if (mRecordHandler != null) {
            mRecordHandler.removeMessages(MSG_THREAD_START);
            mRecordHandler.sendEmptyMessage(MSG_THREAD_START);
            mRecordHandler.sendEmptyMessage(MSG_THREAD_FEED_INPUT);
        }
    }

    public void stop() {
        mForceStopFlag.set(true);
        if (mRecordHandler != null) {
            mRecordHandler.removeMessages(MSG_THREAD_STOP);
            mRecordHandler.sendEmptyMessage(MSG_THREAD_STOP);
        }
    }

    public void release() {
        if (mRecordHandler != null) {
            mRecordHandler.removeMessages(MSG_THREAD_RELEASE);
            mRecordHandler.sendEmptyMessage(MSG_THREAD_RELEASE);
            mRecordThread.quitSafely();
            mRecordHandler = null;
        }
    }

    void releaseOutputBuffer(int index) {
        if (DEBUG) Log.d(TAG, "audio encoder released output buffer index=" + index);
        Message.obtain(mRecordHandler, MSG_THREAD_RELEASE_OUTPUT, index, 0).sendToTarget();
    }

    private static final int MSG_THREAD_PREPARE = 0;
    private static final int MSG_THREAD_FEED_INPUT = 1;
    private static final int MSG_THREAD_DRAIN_OUTPUT = 2;
    private static final int MSG_THREAD_RELEASE_OUTPUT = 3;
    private static final int MSG_THREAD_START = 4;
    private static final int MSG_THREAD_STOP = 5;
    private static final int MSG_THREAD_RELEASE = 6;

    private class RecordHandler extends Handler {

        private LinkedList<MediaCodec.BufferInfo> mCachedInfos = new LinkedList<>();
        private LinkedList<Integer> mMuxingOutputBufferIndices = new LinkedList<>();
        private int mPollRate = 2048_000 / mSampleRate; // poll per 2048 samples

        RecordHandler(Looper looper) {
            super(looper);
            if (DEBUG)
                MLog.d(TAG, "RecordHandler() mPollRate: " + mPollRate);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_THREAD_PREPARE:
                    mAudioRecord = MediaUtils.createAudioRecord(
                            MediaRecorder.AudioSource.MIC, mSampleRate, 2, mFormat);
                    break;
                case MSG_THREAD_FEED_INPUT:
                    putRecordDataPassToMediaCodec();
                    break;
                case MSG_THREAD_DRAIN_OUTPUT:
                    offerOutput();
                    pollInputIfNeed();
                    break;
                case MSG_THREAD_RELEASE_OUTPUT:
                    //mAudioEncoder.releaseOutputBuffer(msg.arg1);
                    // Nobody care what it exactly is.
                    mMuxingOutputBufferIndices.poll();
                    if (DEBUG) Log.d(TAG, "audio encoder released output buffer index="
                            + msg.arg1 + ", remaining=" + mMuxingOutputBufferIndices.size());
                    pollInputIfNeed();
                    break;
                case MSG_THREAD_START:
                    if (CustomAudioRecord.this.mAudioRecord != null) {
                        mAudioRecord.startRecording();
                    }
                    break;
                case MSG_THREAD_STOP:
                    if (CustomAudioRecord.this.mAudioRecord != null) {
                        CustomAudioRecord.this.mAudioRecord.stop();
                    }
                    break;
                case MSG_THREAD_RELEASE:
                    if (CustomAudioRecord.this.mAudioRecord != null) {
                        CustomAudioRecord.this.mAudioRecord.release();
                        CustomAudioRecord.this.mAudioRecord = null;
                    }
                    break;
            }
        }

        /***
         把录音数据传递到MediaCodec中去编码
         */
        private void putRecordDataPassToMediaCodec() {
            if (!mForceStopFlag.get()) {
                int index = mAudioEncoder.dequeueInputBuffer(0);
                if (DEBUG)
                    Log.d(TAG, "audio encoder returned input buffer index: " + index);

                if (index >= 0) {
                    feedAudioEncoder(index);
                    // tell encoder to eat the fresh meat!
                    if (!mForceStopFlag.get()) {
                        sendEmptyMessage(MSG_THREAD_DRAIN_OUTPUT);
                    }
                } else {
                    // try later...
                    if (DEBUG)
                        Log.d(TAG, "try later to poll input buffer");
                    sendEmptyMessageDelayed(MSG_THREAD_FEED_INPUT, mPollRate);
                }
            }
        }

        private void offerOutput() {
            while (!mForceStopFlag.get()) {
                MediaCodec.BufferInfo info = mCachedInfos.poll();
                if (info == null) {
                    info = new MediaCodec.BufferInfo();
                }
                int index = mAudioEncoder.dequeueOutputBuffer(info, 1);
                if (DEBUG)
                    Log.d(TAG, "audio encoder returned output buffer index: " + index);
                if (index == INFO_OUTPUT_FORMAT_CHANGED) {
                    /*mCallbackDelegate.onOutputFormatChanged(
                            mEncoder,
                            mEncoder.mAudioEncoder.getOutputFormat());*/
                }
                if (index < 0) {
                    info.set(0, 0, 0, 0);
                    mCachedInfos.offer(info);
                    break;
                }

                mMuxingOutputBufferIndices.offer(index);
                // mCallbackDelegate.onOutputBufferAvailable(mEncoder, index, info);
            }
        }

        private void pollInputIfNeed() {
            if (mMuxingOutputBufferIndices.size() <= 1 && !mForceStopFlag.get()) {
                // need fresh data, right now!
                removeMessages(MSG_THREAD_FEED_INPUT);
                sendEmptyMessageDelayed(MSG_THREAD_FEED_INPUT, 0);
            }
        }
    }

    /**
     * NOTE: Should waiting all output buffer disappear queue input buffer
     * 把MIC中的声音数据传到MediaCodec中去编码
     */
    private void feedAudioEncoder(int index) {
        if (index < 0
                || mForceStopFlag.get()
                || mAudioRecord == null) {
            return;
        }

        boolean eos = mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED;
        ByteBuffer frame = mAudioEncoder.getInputBuffer(index);
        int offset = frame.position();
        int limit = frame.limit();
        int read = 0;
        if (!eos) {
            read = mAudioRecord.read(frame, limit);
            if (DEBUG) Log.d(TAG, "Read frame data size " + read + " for index "
                    + index + " buffer : " + offset + ", " + limit);
            if (read < 0) {
                read = 0;
            }
        }

        long pstTs = calculateFrameTimestamp(read << 3);

        int flags = BUFFER_FLAG_KEY_FRAME;
        if (eos) {
            flags = BUFFER_FLAG_END_OF_STREAM;
        }

        // feed frame to encoder
        if (DEBUG) Log.d(TAG, "Feed codec index=" + index + ", presentationTimeUs1="
                + pstTs + ", flags=" + flags);
        mAudioEncoder.queueInputBuffer(index, offset, read, pstTs, flags);
    }


    private static final int LAST_FRAME_ID = -1;
    private SparseLongArray mFramesUsCache = new SparseLongArray(2);

    /**
     * Gets presentation time (us) of polled frame.
     * 1 sample = 16 bit
     */
    private long calculateFrameTimestamp(int totalBits) {
        int samples = totalBits >> 4;
        long frameUs = mFramesUsCache.get(samples, -1);
        if (frameUs == -1) {
            frameUs = samples * 1000_000 / mChannelsSampleRate;
            mFramesUsCache.put(samples, frameUs);
        }
        long timeUs = SystemClock.elapsedRealtimeNanos() / 1000;
        // accounts the delay of polling the audio sample data
        timeUs -= frameUs;
        long currentUs;
        long lastFrameUs = mFramesUsCache.get(LAST_FRAME_ID, -1);
        if (lastFrameUs == -1) { // it's the first frame
            currentUs = timeUs;
        } else {
            currentUs = lastFrameUs;
        }
        if (DEBUG)
            Log.i(TAG, "count samples pts: " + currentUs + ", time pts: " + timeUs + ", samples: " +
                    "" + samples);
        // maybe too late to acquire sample data
        if (timeUs - currentUs >= (frameUs << 1)) {
            // reset
            currentUs = timeUs;
        }
        mFramesUsCache.put(LAST_FRAME_ID, currentUs + frameUs);
        return currentUs;
    }

}
