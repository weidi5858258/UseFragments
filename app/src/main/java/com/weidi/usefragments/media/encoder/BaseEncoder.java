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

    private IEncodeConfig mIEncodeConfig;
    private MediaCodec mMediaEncoder;
    // MediaCodec对象调用configure()方法时使用
    protected MediaFormat mMediaFormat;
    protected Surface mSurface;
    protected MediaCrypto mMediaCrypto;
    protected int mConfigureFlag = MediaCodec.CONFIGURE_FLAG_ENCODE;

    private MediaCodec.Callback mCallback;

    public BaseEncoder(IEncodeConfig encodeConfig) {
        this.mIEncodeConfig = encodeConfig;
        if (mIEncodeConfig == null) {
            throw new NullPointerException("BaseEncoder() mIEncodeConfig is null");
        }
    }

    public BaseEncoder(IEncodeConfig encodeConfig,
                       MediaFormat mediaFormat,
                       Surface surface,
                       MediaCrypto mediaCrypto) {
        this.mIEncodeConfig = encodeConfig;
        this.mMediaFormat = mediaFormat;
        this.mSurface = surface;
        this.mMediaCrypto = mediaCrypto;
        if (mIEncodeConfig == null) {
            throw new NullPointerException("BaseEncoder() mIEncodeConfig is null");
        }
    }

    public BaseEncoder(IEncodeConfig encodeConfig,
                       MediaFormat mediaFormat,
                       Surface surface,
                       MediaCrypto mediaCrypto,
                       int configureFlag) {
        this.mIEncodeConfig = encodeConfig;
        this.mMediaFormat = mediaFormat;
        this.mSurface = surface;
        this.mMediaCrypto = mediaCrypto;
        this.mConfigureFlag = configureFlag;
        if (mIEncodeConfig == null) {
            throw new NullPointerException("BaseEncoder() mIEncodeConfig is null");
        }
    }

    protected void setCallback(MediaCodec.Callback callback) {
        mCallback = callback;
    }

    /***
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
            mMediaFormat = mIEncodeConfig.createMediaFormat();
        }
        if (DEBUG)
            Log.d(TAG, "prepare() mMediaFormat: " + mMediaFormat);

        mMediaEncoder = createEncoder(mIEncodeConfig.getMimeType());
        if (mMediaEncoder == null) {
            if (DEBUG)
                Log.e(TAG, "prepare() mMediaEncoder is null");
            return;
        }

        try {
            if (this.mCallback != null) {
                // NOTE: MediaCodec maybe crash on some devices due to null callback
                mMediaEncoder.setCallback(mCallback);
            }

            mMediaEncoder.configure(mMediaFormat, mSurface, mMediaCrypto, mConfigureFlag);
            // encoder.start();
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

    /**
     * create a new instance of MediaCodec
     */
    private MediaCodec createEncoder(String type) throws IOException {
        try {
            // use codec name first
            if (this.mIEncodeConfig.getCodecName() != null) {
                return MediaCodec.createByCodecName(this.mIEncodeConfig.getCodecName());
            }
        } catch (IOException e) {
            Log.w(TAG, "createEncoder() create '" +
                    this.mIEncodeConfig.getCodecName() + "' failure!", e);
        }
        return MediaCodec.createEncoderByType(type);
    }


    protected final MediaCodec getEncoder() {
        return Objects.requireNonNull(mMediaEncoder, "doesn't prepare()");
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
     */
    public final ByteBuffer getInputBuffer(int index) {
        return getEncoder().getInputBuffer(index);
    }

    /**
     * @throws NullPointerException if prepare() not call
     * @see MediaCodec#queueInputBuffer(int, int, int, long, int)
     * @see MediaCodec#getInputBuffer(int)
     */
    public final void queueInputBuffer(int index, int offset, int size, long pstTs, int flags) {
        getEncoder().queueInputBuffer(index, offset, size, pstTs, flags);
    }

    /**
     * @throws NullPointerException if prepare() not call
     * @see MediaCodec#releaseOutputBuffer(int, boolean)
     */
    public final void releaseOutputBuffer(int index) {
        getEncoder().releaseOutputBuffer(index, false);
    }

}
