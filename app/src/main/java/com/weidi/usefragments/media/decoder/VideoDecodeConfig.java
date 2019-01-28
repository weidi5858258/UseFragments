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

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.util.Objects;

/***

 */
public class VideoDecodeConfig {

    private final String mCodecName;
    private final String mMimeType;
    private final int mWidth;
    private final int mHeight;
    private final int mBitRate;
    private final int mFrameRate;
    private final int mIFrameInterval;
    private final MediaCodecInfo.CodecProfileLevel mCodecProfileLevel;

    /**
     * @param codecName         selected codec name, maybe null
     * @param mimeType          video MIME type, cannot be null
     * @param codecProfileLevel profile level for video encoder nullable
     */

    /***
     *
     * @param codecName maybe null
     * @param mimeType cannot be null
     * @param width
     * @param height
     * @param bitRate
     * @param frameRate
     * @param iFrameInterval
     * @param codecProfileLevel profile level for video encoder nullable
     */
    public VideoDecodeConfig(String codecName, String mimeType,
                             int width, int height, int bitRate,
                             int frameRate, int iFrameInterval,
                             MediaCodecInfo.CodecProfileLevel codecProfileLevel) {
        this.mCodecName = codecName;
        this.mMimeType = Objects.requireNonNull(mimeType);
        this.mWidth = width;
        this.mHeight = height;
        this.mBitRate = bitRate;
        this.mFrameRate = frameRate;
        this.mIFrameInterval = iFrameInterval;
        this.mCodecProfileLevel = codecProfileLevel;
    }

    MediaFormat getMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(mMimeType, mWidth, mHeight);
        // 必须设置为COLOR_FormatSurface，因为是用Surface作为输入源
        format.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(
                MediaFormat.KEY_BIT_RATE,
                mBitRate);
        // 设置帧率
        format.setInteger(
                MediaFormat.KEY_FRAME_RATE,
                mFrameRate);
        // 设置抽取关键帧的间隔，以s为单位，负数或者0会不抽取关键帧
        format.setInteger(
                MediaFormat.KEY_I_FRAME_INTERVAL,
                mIFrameInterval);
        if (mCodecProfileLevel != null
                && mCodecProfileLevel.profile != 0
                && mCodecProfileLevel.level != 0) {
            format.setInteger(
                    MediaFormat.KEY_PROFILE,
                    mCodecProfileLevel.profile);
            format.setInteger(
                    MediaFormat.KEY_LEVEL,
                    mCodecProfileLevel.level);
        }
        // maybe useful
        // format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 10_000_000);
        return format;
    }


}
