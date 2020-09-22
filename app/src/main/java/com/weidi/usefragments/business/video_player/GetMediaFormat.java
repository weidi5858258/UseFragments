/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weidi.usefragments.business.video_player;

import android.content.Context;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.android.exoplayer2.ObjectHelper;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.weidi.usefragments.MyApplication;
import com.weidi.usefragments.tool.MLog;

/***
 
 */
public class GetMediaFormat {

    private static final String TAG = "player_alexander";

    //private volatile static GetMediaFormat sGetMediaFormat = null;
    private SimpleExoPlayer player;
    private Handler mUiHandler;
    private Context mContext;
    private PlayerWrapper mPlayerWrapper;
    private int mStreamCount = 0;
    public static MediaFormat sVideoMediaFormat = null;
    public static MediaFormat sAudioMediaFormat = null;

    /*public static GetMediaFormat getDefault() {
        if (sGetMediaFormat == null) {
            synchronized (GetMediaFormat.class) {
                if (sGetMediaFormat == null) {
                    sGetMediaFormat = new GetMediaFormat();
                }
            }
        }
        return sGetMediaFormat;
    }*/

    public GetMediaFormat() {
        mUiHandler = new Handler(Looper.getMainLooper());
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setPlayerWrapper(PlayerWrapper playerWrapper) {
        mPlayerWrapper = playerWrapper;
    }

    public synchronized void start(String path) {
        if (TextUtils.isEmpty(path)
                || player != null) {
            release(false);
            return;
        }
        MLog.i(TAG, "start() begin");
        mStreamCount = 0;
        sVideoMediaFormat = null;
        sAudioMediaFormat = null;
        ObjectHelper.getDefault().setMediaFormatCallback(mCallback);
        Uri uri = null;

        /*uri = Uri.parse("/storage/2430-1702/BaiduNetdisk/video/痞子英雄2-黎明升起.mp4");
        uri = Uri.parse("/storage/2430-1702/BaiduNetdisk/video/AQUAMAN_Trailer_3840_2160_4K.webm");
        uri = Uri.parse("/storage/1532-48AD/Videos/Movies/AQUAMAN_Trailer_3840_2160_4K.webm");
        uri = Uri.parse("/storage/37C8-3904/myfiles/video/4K_hevc_3840x2160.mp4");
        uri = Uri.parse("https://zb3.qhqsnedu.com/live/chingyinglam/playlist.m3u8");
        uri = Uri.parse("https://zb3.qhqsnedu.com/live/chingyinglam/playlist.m3u8");
        uri = Uri.parse("http://ivi.bupt.edu.cn/hls/cctv6hd.m3u8");
        uri = Uri.parse("/storage/37C8-3904/myfiles/video/AQUAMAN_Trailer_3840_2160_4K.webm");
        uri = Uri.parse("/storage/37C8-3904/sample_kingsman.mp4");*/

        uri = Uri.parse(path);
        MediaSource mediaSources =
                ((MyApplication) mContext.getApplicationContext())
                        .createLeafMediaSource(uri, null, null);
        MediaSource mediaSource = new MergingMediaSource(mediaSources);
        RenderersFactory renderersFactory =
                ((MyApplication) mContext.getApplicationContext())
                        .buildRenderersFactory(false);
        player = new SimpleExoPlayer.Builder(mContext, renderersFactory).build();
        player.prepare(mediaSource, true, false);
        MLog.i(TAG, "start() end");
    }

    public synchronized void release(boolean needToPlayback) {
        if (player != null) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (player != null) {
                        player.release();
                        player = null;
                        if (mPlayerWrapper != null && needToPlayback) {
                            //mPlayerWrapper.startPlayback();
                        }
                    }
                }
            });
        }
    }

    private ObjectHelper.MediaFormatCallback mCallback =
            new ObjectHelper.MediaFormatCallback() {
                private int mSCount = 0;

                @Override
                public void setStreamCount(int count) {
                    mSCount = 0;
                    mStreamCount = count;
                    MLog.i(TAG, "MediaFormatCallback mStreamCount: " + mStreamCount);
                }

                @Override
                public void onCreated(MediaFormat mediaFormat) {
                    if (mediaFormat != null) {
                        MLog.i(TAG, "MediaFormatCallback mediaFormat: \n" + mediaFormat);
                    } else {
                        MLog.e(TAG, "MediaFormatCallback mediaFormat is null");
                    }
                    if (player == null) {
                        return;
                    }
                    if (mediaFormat == null || mStreamCount <= 0) {
                        release(true);
                        return;
                    }
                    String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                    if (TextUtils.isEmpty(mime)) {
                        release(true);
                        return;
                    }
                    if (mime.startsWith("video/")) {
                        sVideoMediaFormat = mediaFormat;
                    } else if (mime.startsWith("audio/")) {
                        sAudioMediaFormat = mediaFormat;
                    }
                    if ((++mSCount) == mStreamCount) {
                        release(true);
                    }
                }
            };

}
