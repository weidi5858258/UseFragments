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
import com.google.android.exoplayer2.upstream.DataSource;
import com.weidi.usefragments.MyApplication;
import com.weidi.usefragments.tool.MLog;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class GetMediaFormat {

    private static final String TAG = "player_alexander";

    //private volatile static GetMediaFormat sGetMediaFormat = null;
    private SimpleExoPlayer player;
    private MediaSource mediaSource;

    private Handler mUiHandler;
    private Context mContext;
    private String mPath;

    private PlayerWrapper mPlayerWrapper;

    private int mStreamCount = 0;

    public MediaFormat mVideoMediaFormat = null;
    public MediaFormat mAudioMediaFormat = null;

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

    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setDataSource(String path) {
        mPath = path;
    }

    public void setPlayerWrapper(PlayerWrapper playerWrapper) {
        mPlayerWrapper = playerWrapper;
    }

    public void start() {
        if (TextUtils.isEmpty(mPath)
                || player != null) {
            return;
        }
        MLog.i(TAG, "start() begin");
        mStreamCount = 0;
        mVideoMediaFormat = null;
        mAudioMediaFormat = null;
        mUiHandler = new Handler(Looper.getMainLooper());
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

        uri = Uri.parse(mPath);
        MediaSource mediaSources =
                ((MyApplication) mContext.getApplicationContext())
                        .createLeafMediaSource(uri, null, null);
        mediaSource = new MergingMediaSource(mediaSources);
        RenderersFactory renderersFactory =
                ((MyApplication) mContext.getApplicationContext())
                        .buildRenderersFactory(false);
        player = new SimpleExoPlayer.Builder(/* context= */ mContext, renderersFactory).build();
        player.prepare(mediaSource, true, false);
        MLog.i(TAG, "start() end");
    }

    public void release(boolean needToPlayback) {
        if (player != null) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (player != null) {
                        player.release();
                        player = null;
                        mediaSource = null;
                        if (mPlayerWrapper != null && needToPlayback) {
                            mPlayerWrapper.startPlayback();
                        }
                    }
                }
            });
            /*synchronized (GetMediaFormat.class) {
                if (player != null) {
                }
            }*/
        }
    }

    private ObjectHelper.MediaFormatCallback mCallback =
            new ObjectHelper.MediaFormatCallback() {
                private int mSCount = 0;

                @Override
                public void setStreamCount(int count) {
                    mSCount = 0;
                    mStreamCount = count;
                    MLog.i(TAG, "setStreamCount() mStreamCount: " + mStreamCount);
                }

                @Override
                public void onCreated(MediaFormat mediaFormat) {
                    MLog.i(TAG, "onCreated()       mediaFormat: \n" + mediaFormat);
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
                        mVideoMediaFormat = mediaFormat;
                    } else if (mime.startsWith("audio/")) {
                        mAudioMediaFormat = mediaFormat;
                    }
                    if ((++mSCount) == mStreamCount) {
                        release(true);
                    }
                }
            };

}
