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
import android.util.Log;
import android.view.Surface;

import java.util.Objects;

/***

 */
public class VideoEncoder extends BaseEncoder {

    private static final String TAG = VideoEncoder.class.getSimpleName();
    private static final boolean DEBUG = true;

    public VideoEncoder(VideoEncodeConfig config) {
        super(config);
    }

    @Override
    protected void onConfigured(MediaCodec encoder) {
        mSurface = encoder.createInputSurface();
        if (DEBUG)
            Log.i(TAG, "VideoEncoder create input surfaceJavaObject: " + mSurface);
    }

    public Surface getSurface() {
        return Objects.requireNonNull(mSurface, "doesn't prepare()");
    }

    @Override
    public void release() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        super.release();
    }


}
