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

package com.weidi.usefragments.media.decoder;

import android.media.MediaFormat;

import java.util.Objects;

/***

 */
public class AudioDecodeConfig {

    private final String mCodecName;
    private final String mMimeType;
    private final int mBitRate;
    private final int mSampleRate;
    private final int mChannelCount;
    private final int mProfile;

    /***
     *
     * @param codecName
     * @param mimeType
     * @param bitRate
     * @param sampleRate
     * @param channelCount
     * @param profile
     */
    public AudioDecodeConfig(String codecName, String mimeType,
                             int bitRate, int sampleRate, int channelCount, int profile) {
        this.mCodecName = codecName;
        this.mMimeType = Objects.requireNonNull(mimeType);
        this.mBitRate = bitRate;
        this.mSampleRate = sampleRate;
        this.mChannelCount = channelCount;
        this.mProfile = profile;
    }

    MediaFormat getMediaFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(mMimeType, mSampleRate, mChannelCount);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, mProfile);
        //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096 * 4);
        return format;
    }


}
