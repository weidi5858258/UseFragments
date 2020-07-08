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
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseLongArray;

import com.weidi.usefragments.media.encoder.AudioEncodeConfig;
import com.weidi.usefragments.media.encoder.BaseEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;
import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
import static android.os.Build.VERSION_CODES.N;

/***

 */
class MicRecorder {

    private static final String TAG = "MicRecorder";
    private static final boolean VERBOSE = false;

    private final HandlerThread mRecordThread;
    private RecordHandler mRecordHandler;
    private AudioRecord mAudioRecord; // access in mRecordThread only!
    private int mSampleRate;
    private int mChannelConfig;
    private int mFormat = AudioFormat.ENCODING_PCM_16BIT;

    private AtomicBoolean mForceStop = new AtomicBoolean(false);
    //    private CallbackDelegate mCallbackDelegate;
    private int mChannelsSampleRate;

    MicRecorder(AudioEncodeConfig config) {
        mSampleRate = config.mSampleRate;
        mChannelsSampleRate = mSampleRate * config.mChannelCount;
        if (VERBOSE) Log.i(TAG, "in bitrate " + mChannelsSampleRate * 16 /* PCM_16BIT*/);
        mChannelConfig = config.mChannelCount == 2
                ?
                AudioFormat.CHANNEL_IN_STEREO
                :
                AudioFormat.CHANNEL_IN_MONO;
        //        mCallbackDelegate = new CallbackDelegate(myLooper, mCallback);

        mRecordThread = new HandlerThread(TAG);
        mRecordThread.start();
        mRecordHandler = new RecordHandler(mRecordThread.getLooper());
    }


    public void prepare() throws IOException {
        Looper myLooper = Objects.requireNonNull(Looper.myLooper(), "Should prepare in " +
                "HandlerThread");
        // run callback in caller thread

        mRecordHandler.sendEmptyMessage(MSG_PREPARE);
    }

    public void stop() {
        // clear callback queue
        mCallbackDelegate.removeCallbacksAndMessages(null);
        mForceStop.set(true);
        if (mRecordHandler != null) mRecordHandler.sendEmptyMessage(MSG_STOP);
    }

    public void release() {
        if (mRecordHandler != null) mRecordHandler.sendEmptyMessage(MSG_RELEASE);
        mRecordThread.quitSafely();
    }

    void releaseOutputBuffer(int index) {
        if (VERBOSE) Log.d(TAG, "audio encoder released output buffer index=" + index);
        Message.obtain(mRecordHandler, MSG_RELEASE_OUTPUT, index, 0).sendToTarget();
    }


    ByteBuffer getOutputBuffer(int index) {
        return mEncoder.getOutputBuffer(index);
    }


    private static class CallbackDelegate extends Handler {
        private BaseEncoder.Callback mCallback;

        CallbackDelegate(Looper l, BaseEncoder.Callback callback) {
            super(l);
            this.mCallback = callback;
        }


        void onError(Encoder encoder, Exception exception) {
            Message.obtain(this, () -> {
                if (mCallback != null) {
                    mCallback.onError(encoder, exception);
                }
            }).sendToTarget();
        }

        void onOutputFormatChanged(BaseEncoder encoder, MediaFormat format) {
            Message.obtain(this, () -> {
                if (mCallback != null) {
                    mCallback.onOutputFormatChanged(encoder, format);
                }
            }).sendToTarget();
        }

        void onOutputBufferAvailable(BaseEncoder encoder, int index, MediaCodec.BufferInfo info) {
            Message.obtain(this, () -> {
                if (mCallback != null) {
                    mCallback.onOutputBufferAvailable(encoder, index, info);
                }
            }).sendToTarget();
        }

    }

    private static final int MSG_PREPARE = 0;
    private static final int MSG_FEED_INPUT = 1;
    private static final int MSG_DRAIN_OUTPUT = 2;
    private static final int MSG_RELEASE_OUTPUT = 3;
    private static final int MSG_STOP = 4;
    private static final int MSG_RELEASE = 5;

    private class RecordHandler extends Handler {

        private LinkedList<MediaCodec.BufferInfo> mCachedInfos = new LinkedList<>();
        private LinkedList<Integer> mMuxingOutputBufferIndices = new LinkedList<>();
        private int mPollRate = 2048_000 / mSampleRate; // poll per 2048 samples

        RecordHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PREPARE:
                    mAudioRecord = MediaUtils.createAudioRecord(
                            MediaRecorder.AudioSource.MIC, mSampleRate, 2, mFormat);
                    if (mAudioRecord == null) {
                        Log.e(TAG, "create audio record failure");
                        //mCallbackDelegate.onError(MicRecorder.this, new
                        // IllegalArgumentException());
                        break;
                    }

                    mAudioRecord.startRecording();
                    try {
                        //mEncoder.prepare();
                    } catch (Exception e) {
                        //mCallbackDelegate.onError(MicRecorder.this, e);
                    }
                    break;
                case MSG_FEED_INPUT:
                    if (!mForceStop.get()) {
                        int index = pollInput();
                        if (VERBOSE)
                            Log.d(TAG, "audio encoder returned input buffer index=" + index);
                        if (index >= 0) {
                            feedAudioEncoder(index);
                            // tell encoder to eat the fresh meat!
                            if (!mForceStop.get()) sendEmptyMessage(MSG_DRAIN_OUTPUT);
                        } else {
                            // try later...
                            if (VERBOSE) Log.i(TAG, "try later to poll input buffer");
                            sendEmptyMessageDelayed(MSG_FEED_INPUT, mPollRate);
                        }
                    }
                    break;
                case MSG_DRAIN_OUTPUT:
                    offerOutput();
                    pollInputIfNeed();
                    break;
                case MSG_RELEASE_OUTPUT:
                    //mEncoder.releaseOutputBuffer(msg.arg1);
                    mMuxingOutputBufferIndices.poll(); // Nobody care what it exactly is.
                    if (VERBOSE) Log.d(TAG, "audio encoder released output buffer index="
                            + msg.arg1 + ", remaining=" + mMuxingOutputBufferIndices.size());
                    pollInputIfNeed();
                    break;
                case MSG_STOP:
                    if (MicRecorder.this.mAudioRecord != null) {
                        MicRecorder.this.mAudioRecord.stop();
                    }
                    //mEncoder.stop();
                    break;
                case MSG_RELEASE:
                    if (MicRecorder.this.mAudioRecord != null) {
                        MicRecorder.this.mAudioRecord.release();
                        MicRecorder.this.mAudioRecord = null;
                    }
                    //mEncoder.release();
                    break;
            }
        }

        private void offerOutput() {
            while (!mForceStop.get()) {
                MediaCodec.BufferInfo info = mCachedInfos.poll();
                if (info == null) {
                    info = new MediaCodec.BufferInfo();
                }
                int index = mEncoder.getEncoder().dequeueOutputBuffer(info, 1);
                if (VERBOSE) Log.d(TAG, "audio encoder returned output buffer index=" + index);
                if (index == INFO_OUTPUT_FORMAT_CHANGED) {
                    mCallbackDelegate.onOutputFormatChanged(mEncoder, mEncoder.getEncoder()
                            .getOutputFormat());
                }
                if (index < 0) {
                    info.set(0, 0, 0, 0);
                    mCachedInfos.offer(info);
                    break;
                }
                mMuxingOutputBufferIndices.offer(index);
                mCallbackDelegate.onOutputBufferAvailable(mEncoder, index, info);

            }
        }

        private int pollInput() {
            return mEncoder.getEncoder().dequeueInputBuffer(0);
        }

        private void pollInputIfNeed() {
            if (mMuxingOutputBufferIndices.size() <= 1 && !mForceStop.get()) {
                // need fresh data, right now!
                removeMessages(MSG_FEED_INPUT);
                sendEmptyMessageDelayed(MSG_FEED_INPUT, 0);
            }
        }
    }

    /**
     * NOTE: Should waiting all output buffer disappear queue input buffer
     */
    private void feedAudioEncoder(int index) {
        if (index < 0 || mForceStop.get()) return;
        final AudioRecord r = Objects.requireNonNull(mAudioRecord, "maybe release");
        final boolean eos = r.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED;
        final ByteBuffer frame = mEncoder.getInputBuffer(index);
        int offset = frame.position();
        int limit = frame.limit();
        int read = 0;
        if (!eos) {
            read = r.read(frame, limit);
            if (VERBOSE) Log.d(TAG, "Read frame data size " + read + " for index "
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
        if (VERBOSE) Log.d(TAG, "Feed codec index=" + index + ", presentationTimeUs="
                + pstTs + ", flags=" + flags);
        mEncoder.queueInputBuffer(index, offset, read, pstTs, flags);
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
        if (VERBOSE)
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
