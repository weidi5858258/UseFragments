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

package com.weidi.usefragments.media.encoder;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/***

 */
public abstract class BaseEncoder implements IEncoder {

    private static final String TAG = BaseEncoder.class.getSimpleName();
    private static final boolean DEBUG = true;

    protected IConfig mIConfig;
    private MediaCodec mMediaEncoder;
    // MediaCodec对象调用configure()方法时使用
    protected MediaFormat mMediaFormat;
    protected Surface mSurface;
    protected MediaCrypto mMediaCrypto;

    private MediaCodec.Callback mCallback;

    public BaseEncoder(IConfig encodeConfig) {
        this.mIConfig = encodeConfig;

        if (mIConfig == null) {
            throw new NullPointerException("BaseEncoder() mIConfig is null");
        }
    }

    public BaseEncoder(IConfig encodeConfig,
                       MediaFormat mediaFormat,
                       Surface surface,
                       MediaCrypto mediaCrypto) {
        this.mIConfig = encodeConfig;
        this.mMediaFormat = mediaFormat;
        this.mSurface = surface;
        this.mMediaCrypto = mediaCrypto;

        if (mIConfig == null) {
            throw new NullPointerException("BaseEncoder() mIConfig is null");
        }
    }

    /***
     * 调用prepare()方法之前进行设置
     *
     * @param callback
     */
    protected void setCallback(MediaCodec.Callback callback) {
        mCallback = callback;
    }

    /***
     * 调用configure()方法后需要子类再干些什么事(还没有调用start()方法之前)
     *
     * @param encoder
     */
    protected void onConfigured(MediaCodec encoder) {
    }

    /***
     * Must call in a worker handler thread!
     */
    @Override
    public void prepare() throws IOException {
        if (Looper.myLooper() == null
                || Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("should run in a HandlerThread");
        }
        if (mMediaEncoder != null) {
            throw new IllegalStateException("prepared!");
        }

        if (mMediaFormat == null) {
            mMediaFormat = mIConfig.createMediaFormat();
        }
        if (DEBUG)
            Log.d(TAG, "prepare() mMediaFormat: " + mMediaFormat);

        mMediaEncoder = createEncoder();
        if (mMediaEncoder == null) {
            if (DEBUG)
                Log.e(TAG, "prepare() mMediaEncoder is null");
            return;
        }

        if (this.mCallback != null) {
            // NOTE: MediaCodec maybe crash on some devices due to null callback
            mMediaEncoder.setCallback(mCallback);
        }

        try {
            mMediaEncoder.configure(
                    mMediaFormat,
                    mSurface,
                    mMediaCrypto,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (MediaCodec.CodecException e) {
            Log.e(TAG, "Configure codec failure!\n  with format" + mMediaFormat, e);
            throw e;
        }

        onConfigured(mMediaEncoder);
    }

    /**
     * @see MediaCodec#start()
     */
    @Override
    public void start() {
        if (mMediaEncoder != null) {
            mMediaEncoder.start();
        }
    }

    /**
     * @see MediaCodec#stop()
     */
    @Override
    public void stop() {
        if (mMediaEncoder != null) {
            mMediaEncoder.stop();
        }
    }

    /**
     * @see MediaCodec#release()
     */
    @Override
    public void release() {
        if (mMediaEncoder != null) {
            mMediaEncoder.release();
            mMediaEncoder = null;
        }
    }

    @Override
    public MediaCodec getEncoder() {
        return Objects.requireNonNull(
                mMediaEncoder,
                "Doesn't invoke prepare() method.");
    }

    /**
     * create a new instance of MediaCodec
     */
    private MediaCodec createEncoder() throws IOException {
        MediaCodec mediaCodec = null;

        try {
            if (!TextUtils.isEmpty(this.mIConfig.getCodecName())) {
                mediaCodec = MediaCodec.createByCodecName(this.mIConfig.getCodecName());
            } else if (!TextUtils.isEmpty(this.mIConfig.getMimeType())) {
                mediaCodec = MediaCodec.createEncoderByType(this.mIConfig.getMimeType());
            }
        } catch (IOException e) {
            Log.w(TAG, "createEncoder() create '" +
                    this.mIConfig.getCodecName() + "' failure!", e);
            mediaCodec = null;
        }

        return mediaCodec;
    }

    /////////////////////////////////////////////////////////////

    /**
     * @throws NullPointerException if prepare() not call
     * @see MediaCodec#getInputBuffer(int)
     */
    public final ByteBuffer getInputBuffer(int index) {
        return getEncoder().getInputBuffer(index);
    }

    /**
     * @throws NullPointerException if prepare() not call
     * @see MediaCodec#getOutputBuffer(int)
     */
    public final ByteBuffer getOutputBuffer(int index) {
        return getEncoder().getOutputBuffer(index);
    }

    /**
     * @throws NullPointerException if prepare() not call
     * @see MediaCodec#getInputBuffer(int)
     * @see MediaCodec#queueInputBuffer(int, int, int, long, int)
     */
    public final void queueInputBuffer(
            int index,
            int offset,
            int size,
            long presentationTimeUs,
            int flags) {
        getEncoder().queueInputBuffer(index, offset, size, presentationTimeUs, flags);
    }

    /**
     * @throws NullPointerException if prepare() not call
     * @see MediaCodec#releaseOutputBuffer(int, boolean)
     */
    public final void releaseOutputBuffer(int index) {
        getEncoder().releaseOutputBuffer(index, false);
    }

}
