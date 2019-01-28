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
import android.media.MediaFormat;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/***

 */
public abstract class BaseEncoder implements IEncoder {

    private static final String TAG = BaseEncoder.class.getSimpleName();
    private static final boolean DEBUG = true;

    private String mCodecName;
    private MediaCodec mMediaEncoder;

    public BaseEncoder() {
    }

    public BaseEncoder(String codecName) {
        this.mCodecName = codecName;
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

        MediaFormat format = createMediaFormat();
        Log.d("Encoder", "Create media format: " + format);

        String mimeType = format.getString(MediaFormat.KEY_MIME);
        final MediaCodec encoder = createEncoder(mimeType);
        try {
            /*if (this.mCallback != null) {
                // NOTE: MediaCodec maybe crash on some devices due to null callback
                encoder.setCallback(mCodecCallback);
            }*/
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            onConfigured(encoder);
            // encoder.start();
        } catch (MediaCodec.CodecException e) {
            Log.e("Encoder", "Configure codec failure!\n  with format" + format, e);
            throw e;
        }

        mMediaEncoder = encoder;
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
     * call immediately after {@link #getEncoder() MediaCodec}
     * configure with {@link #createMediaFormat() MediaFormat} success
     *
     * @param encoder
     */
    protected void onConfigured(MediaCodec encoder) {
    }

    /**
     * create a new instance of MediaCodec
     */
    private MediaCodec createEncoder(String type) throws IOException {
        try {
            // use codec name first
            if (this.mCodecName != null) {
                return MediaCodec.createByCodecName(mCodecName);
            }
        } catch (IOException e) {
            Log.w("@@", "Create MediaCodec by name '" + mCodecName + "' failure!", e);
        }
        return MediaCodec.createEncoderByType(type);
    }

    /**
     * create {@link MediaFormat} for {@link MediaCodec}
     */
    protected abstract MediaFormat createMediaFormat();

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
