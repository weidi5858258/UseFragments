/*
 * Copyright (c) 2014 Yrom Wang <http://www.yrom.net>
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

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.weidi.usefragments.media.encoder.AudioEncodeConfig;
import com.weidi.usefragments.media.encoder.BaseEncoder;
import com.weidi.usefragments.media.encoder.VideoEncodeConfig;
import com.weidi.usefragments.media.encoder.VideoEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

/***

 */
public class ScreenRecorder {

    private static final String TAG = ScreenRecorder.class.getSimpleName();
    private static final boolean VERBOSE = false;
    private static final int INVALID_INDEX = -1;
    static final String VIDEO_AVC = MIMETYPE_VIDEO_AVC; // H.264 Advanced Video Coding
    static final String AUDIO_AAC = MIMETYPE_AUDIO_AAC; // H.264 Advanced Audio Coding
    private int mWidth;
    private int mHeight;
    private int mDpi;
    private String mDstPath;
    private MediaProjection mMediaProjection;
    private VideoEncoder mVideoEncoder;
    private MicRecorder mAudioEncoder;

    private MediaFormat mVideoOutputFormat = null, mAudioOutputFormat = null;
    private int mVideoTrackIndex = INVALID_INDEX, mAudioTrackIndex = INVALID_INDEX;
    private MediaMuxer mMediaMuxer;
    private boolean mMuxerStarted = false;

    private AtomicBoolean mForceQuit = new AtomicBoolean(false);
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private VirtualDisplay mVirtualDisplay;

    private HandlerThread mHandlerThread;
    private ThreadHandler mThreadHandler;

    private Callback mCallback;
    private LinkedList<Integer> mPendingVideoEncoderBufferIndices = new LinkedList<>();
    private LinkedList<Integer> mPendingAudioEncoderBufferIndices = new LinkedList<>();
    private LinkedList<MediaCodec.BufferInfo> mPendingAudioEncoderBufferInfos = new LinkedList<>();
    private LinkedList<MediaCodec.BufferInfo> mPendingVideoEncoderBufferInfos = new LinkedList<>();

    private MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            quit();
        }
    };

    private static final int MSG_THREAD_START = 0;
    private static final int MSG_THREAD_STOP = 1;
    private static final int MSG_THREAD_ERROR = 2;
    private static final int STOP_WITH_EOS = 1;

    private class ThreadHandler extends Handler {
        ThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_THREAD_START:
                    try {
                        startScreenRecord();
                        if (mCallback != null) {
                            mCallback.onStart();
                        }
                    } catch (Exception e) {
                        msg.obj = e;
                    }
                    break;
                case MSG_THREAD_STOP:
                case MSG_THREAD_ERROR:
                    stopEncoders();
                    if (msg.arg1 != STOP_WITH_EOS) {
                        signalEndOfStream();
                    }
                    if (mCallback != null) {
                        mCallback.onStop((Throwable) msg.obj);
                    }
                    release();
                    break;
            }
        }
    }

    /***
     * @param dpi for {@link VirtualDisplay}
     */
    public ScreenRecorder(VideoEncodeConfig video,
                          AudioEncodeConfig audio,
                          int dpi,
                          MediaProjection mediaProjection,
                          String dstPath) {
        mWidth = video.mWidth;
        mHeight = video.mHeight;
        mDpi = dpi;
        mMediaProjection = mediaProjection;
        mDstPath = dstPath;
        mVideoEncoder = new VideoEncoder(video);
        mAudioEncoder = audio == null ? null : new MicRecorder(audio);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new ThreadHandler(mHandlerThread.getLooper());
    }

    /**
     * stop task
     */
    public final void quit() {
        mForceQuit.set(true);
        if (!mIsRunning.get()) {
            release();
        } else {
            signalStop(false);
        }

    }

    public void start() {
        mThreadHandler.sendEmptyMessage(MSG_THREAD_START);
    }

    public void pause() {

    }

    public void stop() {

    }

    public String getSavedPath() {
        return mDstPath;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    interface Callback {
        void onStop(Throwable error);

        void onStart();

        void onRecording(long presentationTimeUs);
    }


    private void signalEndOfStream() {
        MediaCodec.BufferInfo eos = new MediaCodec.BufferInfo();
        ByteBuffer buffer = ByteBuffer.allocate(0);
        eos.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        if (VERBOSE) Log.i(TAG, "Signal EOS to muxer ");
        if (mVideoTrackIndex != INVALID_INDEX) {
            writeSampleData(mVideoTrackIndex, eos, buffer);
        }
        if (mAudioTrackIndex != INVALID_INDEX) {
            writeSampleData(mAudioTrackIndex, eos, buffer);
        }
        mVideoTrackIndex = INVALID_INDEX;
        mAudioTrackIndex = INVALID_INDEX;
    }

    private void startScreenRecord() {
        if (mIsRunning.get() || mForceQuit.get()) {
            throw new IllegalStateException();
        }
        if (mMediaProjection == null) {
            throw new IllegalStateException("maybe release");
        }
        mIsRunning.set(true);

        mMediaProjection.registerCallback(mProjectionCallback, mThreadHandler);

        try {
            mMediaMuxer = new MediaMuxer(
                    mDstPath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            prepareVideoEncoder();
            prepareAudioEncoder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                TAG + "-display",
                mWidth,
                mHeight,
                mDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mVideoEncoder.getSurface(),
                null,
                null);

        if (VERBOSE)
            Log.d(TAG, "created virtual display: " + mVirtualDisplay.getDisplay());
    }

    private void muxVideo(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            Log.w(TAG, "muxVideo: Already stopped!");
            return;
        }
        if (!mMuxerStarted || mVideoTrackIndex == INVALID_INDEX) {
            mPendingVideoEncoderBufferIndices.add(index);
            mPendingVideoEncoderBufferInfos.add(buffer);
            return;
        }
        ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(index);
        writeSampleData(mVideoTrackIndex, buffer, encodedData);
        mVideoEncoder.releaseOutputBuffer(index);
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE)
                Log.d(TAG, "Stop encoder and muxer, since the buffer has been marked with EOS");
            // send release msg
            mVideoTrackIndex = INVALID_INDEX;
            signalStop(true);
        }
    }


    private void muxAudio(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            Log.w(TAG, "muxAudio: Already stopped!");
            return;
        }
        if (!mMuxerStarted || mAudioTrackIndex == INVALID_INDEX) {
            mPendingAudioEncoderBufferIndices.add(index);
            mPendingAudioEncoderBufferInfos.add(buffer);
            return;

        }
        ByteBuffer encodedData = mAudioEncoder.getOutputBuffer(index);
        writeSampleData(mAudioTrackIndex, buffer, encodedData);
        mAudioEncoder.releaseOutputBuffer(index);
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE)
                Log.d(TAG, "Stop encoder and muxer, since the buffer has been marked with EOS");
            mAudioTrackIndex = INVALID_INDEX;
            signalStop(true);
        }
    }

    private void writeSampleData(int track, MediaCodec.BufferInfo buffer, ByteBuffer encodedData) {
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            if (VERBOSE) Log.d(TAG, "Ignoring BUFFER_FLAG_CODEC_CONFIG");
            buffer.size = 0;
        }
        boolean eos = (buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if (buffer.size == 0 && !eos) {
            if (VERBOSE) Log.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        } else {
            if (buffer.presentationTimeUs != 0) { // maybe 0 if eos
                if (track == mVideoTrackIndex) {
                    resetVideoPts(buffer);
                } else if (track == mAudioTrackIndex) {
                    resetAudioPts(buffer);
                }
            }
            if (VERBOSE)
                Log.d(TAG, "[" + Thread.currentThread().getId() + "] Got buffer, track=" + track
                        + ", info: size=" + buffer.size
                        + ", presentationTimeUs=" + buffer.presentationTimeUs);
            if (!eos && mCallback != null) {
                mCallback.onRecording(buffer.presentationTimeUs);
            }
        }
        if (encodedData != null) {
            encodedData.position(buffer.offset);
            encodedData.limit(buffer.offset + buffer.size);
            mMediaMuxer.writeSampleData(track, encodedData, buffer);
            if (VERBOSE)
                Log.i(TAG, "Sent " + buffer.size + " bytes to MediaMuxer on track " + track);
        }
    }

    private long mVideoPtsOffset, mAudioPtsOffset;

    private void resetAudioPts(MediaCodec.BufferInfo buffer) {
        if (mAudioPtsOffset == 0) {
            mAudioPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mAudioPtsOffset;
        }
    }

    private void resetVideoPts(MediaCodec.BufferInfo buffer) {
        if (mVideoPtsOffset == 0) {
            mVideoPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mVideoPtsOffset;
        }
    }

    private void resetVideoOutputFormat(MediaFormat newFormat) {
        // should happen before receiving buffers, and should only happen once
        if (mVideoTrackIndex >= 0 || mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        if (VERBOSE)
            Log.i(TAG, "Video output format changed.\n New format: " + newFormat.toString());
        mVideoOutputFormat = newFormat;
    }

    private void resetAudioOutputFormat(MediaFormat newFormat) {
        // should happen before receiving buffers, and should only happen once
        if (mAudioTrackIndex >= 0 || mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        if (VERBOSE)
            Log.i(TAG, "Audio output format changed.\n New format: " + newFormat.toString());
        mAudioOutputFormat = newFormat;
    }

    private void startMuxerIfReady() {
        if (mMuxerStarted || mVideoOutputFormat == null
                || (mAudioEncoder != null && mAudioOutputFormat == null)) {
            return;
        }

        mVideoTrackIndex = mMediaMuxer.addTrack(mVideoOutputFormat);
        mAudioTrackIndex = mAudioEncoder == null ? INVALID_INDEX : mMediaMuxer.addTrack
                (mAudioOutputFormat);
        mMediaMuxer.start();
        mMuxerStarted = true;
        if (VERBOSE) Log.i(TAG, "Started media muxer, videoIndex=" + mVideoTrackIndex);
        if (mPendingVideoEncoderBufferIndices.isEmpty() && mPendingAudioEncoderBufferIndices
                .isEmpty()) {
            return;
        }
        if (VERBOSE) Log.i(TAG, "Mux pending video output buffers...");
        MediaCodec.BufferInfo info;
        while ((info = mPendingVideoEncoderBufferInfos.poll()) != null) {
            int index = mPendingVideoEncoderBufferIndices.poll();
            muxVideo(index, info);
        }
        if (mAudioEncoder != null) {
            while ((info = mPendingAudioEncoderBufferInfos.poll()) != null) {
                int index = mPendingAudioEncoderBufferIndices.poll();
                muxAudio(index, info);
            }
        }
        if (VERBOSE) Log.i(TAG, "Mux pending video output buffers done.");
    }

    private void prepareVideoEncoder() throws IOException {
        MediaCodec.Callback callback = new MediaCodec.Callback() {
            boolean ranIntoError = false;

            @Override
            public void onInputBufferAvailable(
                    @NonNull MediaCodec codec,
                    int index) {

            }

            @Override
            public void onOutputBufferAvailable(
                    @NonNull MediaCodec codec,
                    int index,
                    @NonNull MediaCodec.BufferInfo info) {
                if (VERBOSE) Log.i(TAG, "VideoEncoder output buffer available: index=" + index);
                try {
                    muxVideo(index, info);
                } catch (Exception e) {
                    Log.e(TAG, "Muxer encountered an error! ", e);
                    Message.obtain(mThreadHandler, MSG_THREAD_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onError(
                    @NonNull MediaCodec codec,
                    @NonNull MediaCodec.CodecException e) {
                ranIntoError = true;
                Log.e(TAG, "VideoEncoder ran into an error! ", e);
                Message.obtain(mThreadHandler, MSG_THREAD_ERROR, e).sendToTarget();
            }

            @Override
            public void onOutputFormatChanged(
                    @NonNull MediaCodec codec,
                    @NonNull MediaFormat format) {
                resetVideoOutputFormat(format);
                startMuxerIfReady();
            }


        };
        mVideoEncoder.setCallback(callback);
        mVideoEncoder.prepare();
    }

    private void prepareAudioEncoder() throws IOException {
        final MicRecorder micRecorder = mAudioEncoder;
        if (micRecorder == null) return;
        MediaCodec.Callback callback = new MediaCodec.Callback() {
            boolean ranIntoError = false;
            @Override
            public void onInputBufferAvailable(
                    @NonNull MediaCodec codec,
                    int index) {

            }

            @Override
            public void onOutputBufferAvailable(
                    @NonNull MediaCodec codec,
                    int index,
                    @NonNull MediaCodec.BufferInfo info) {
                if (VERBOSE)
                    Log.i(TAG, "[" + Thread.currentThread().getId() + "] AudioEncoder output " +
                            "buffer available: index=" + index);
                try {
                    muxAudio(index, info);
                } catch (Exception e) {
                    Log.e(TAG, "Muxer encountered an error! ", e);
                    Message.obtain(mThreadHandler, MSG_THREAD_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onError(
                    @NonNull MediaCodec codec,
                    @NonNull MediaCodec.CodecException e) {
                ranIntoError = true;
                Log.e(TAG, "MicRecorder ran into an error! ", e);
                Message.obtain(mThreadHandler, MSG_THREAD_ERROR, e).sendToTarget();
            }

            @Override
            public void onOutputFormatChanged(
                    @NonNull MediaCodec codec,
                    @NonNull MediaFormat format) {
                if (VERBOSE)
                    Log.d(TAG, "[" + Thread.currentThread().getId() + "] AudioEncoder returned " +
                            "new format " + format);
                resetAudioOutputFormat(format);
                startMuxerIfReady();
            }
        };
        micRecorder.setCallback(callback);
        micRecorder.prepare();
    }

    private void signalStop(boolean stopWithEOS) {
        Message msg = Message.obtain(mThreadHandler, MSG_THREAD_STOP, stopWithEOS ? STOP_WITH_EOS
                : 0, 0);
        mThreadHandler.sendMessageAtFrontOfQueue(msg);
    }

    private void stopEncoders() {
        mIsRunning.set(false);
        mPendingAudioEncoderBufferInfos.clear();
        mPendingAudioEncoderBufferIndices.clear();
        mPendingVideoEncoderBufferInfos.clear();
        mPendingVideoEncoderBufferIndices.clear();
        // maybe called on an error has been occurred
        try {
            if (mVideoEncoder != null) mVideoEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }
        try {
            if (mAudioEncoder != null) mAudioEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }

    }

    private void release() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mProjectionCallback);
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        mVideoOutputFormat = mAudioOutputFormat = null;
        mVideoTrackIndex = mAudioTrackIndex = INVALID_INDEX;
        mMuxerStarted = false;

        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mMediaMuxer != null) {
            try {
                mMediaMuxer.stop();
                mMediaMuxer.release();
            } catch (Exception e) {
                // ignored
            }
            mMediaMuxer = null;
        }
        mThreadHandler = null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mMediaProjection != null) {
            Log.e(TAG, "release() not called!");
            release();
        }
    }

}
