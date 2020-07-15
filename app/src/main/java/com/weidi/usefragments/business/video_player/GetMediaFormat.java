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

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.C.ContentType;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ObjectHelper;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.weidi.usefragments.MyApplication;
import com.weidi.usefragments.tool.MLog;

import java.io.File;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class GetMediaFormat {

    private static final String TAG = "player_alexander";

    //private volatile static GetMediaFormat sGetMediaFormat = null;
    private DataSource.Factory dataSourceFactory;
    private SimpleExoPlayer player;
    private MediaSource mediaSource;

    private Handler mUiHandler;
    private Context mContext;
    private String mPath;

    private PlayerWrapper mPlayerWrapper;

    private boolean runOneTime = true;

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
        runOneTime = true;
        mVideoMediaFormat = null;
        mAudioMediaFormat = null;
        mUiHandler = new Handler(Looper.getMainLooper());
        ObjectHelper.getDefault().setMediaFormatCallback(mCallback);
        Uri uri = null;
        /*uri = Uri.parse("/storage/2430-1702/BaiduNetdisk/video/痞子英雄2-黎明升起.mp4");
        uri = Uri.parse("/storage/2430-1702/BaiduNetdisk/video/AQUAMAN_Trailer_3840_2160_4K.webm");
        uri = Uri.parse("/storage/1532-48AD/Videos/Movies/AQUAMAN_Trailer_3840_2160_4K.webm");
        uri = Uri.parse("/storage/37C8-3904/myfiles/video/kingsman.mp4");
        uri = Uri.parse("/storage/37C8-3904/myfiles/video/4K_hevc_3840x2160.mp4");
        uri = Uri.parse("https://zb3.qhqsnedu.com/live/chingyinglam/playlist.m3u8");
        uri = Uri.parse("https://zb3.qhqsnedu.com/live/chingyinglam/playlist.m3u8");
        uri = Uri.parse("/storage/37C8-3904/myfiles/video/AQUAMAN_Trailer_3840_2160_4K.webm");
        uri = Uri.parse("http://ivi.bupt.edu.cn/hls/cctv6hd.m3u8");*/

        uri = Uri.parse(mPath);
        MediaSource mediaSources = createLeafMediaSource(uri, null, null);
        mediaSource = new MergingMediaSource(mediaSources);
        RenderersFactory renderersFactory = buildRenderersFactory(false);
        player = new SimpleExoPlayer.Builder(/* context= */ mContext, renderersFactory).build();
        player.prepare(mediaSource, true, false);
        MLog.i(TAG, "start() end");
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
            mediaSource = null;
        }
    }

    private boolean useExtensionRenderers() {
        //return "withExtensions".equals(BuildConfig.FLAVOR);
        return true;
    }

    private RenderersFactory buildRenderersFactory(boolean preferExtensionRenderer) {
        @DefaultRenderersFactory.ExtensionRendererMode
        int extensionRendererMode =
                useExtensionRenderers()
                        ? (preferExtensionRenderer
                        ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        return new DefaultRenderersFactory(/* context= */ mContext)
                .setExtensionRendererMode(extensionRendererMode);
    }

    private MediaSource createLeafMediaSource(
            Uri uri, String extension, DrmSessionManager<?> drmSessionManager) {
        dataSourceFactory = buildDataSourceFactory();
        @ContentType int type = Util.inferContentType(uri, extension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private DataSource.Factory buildDataSourceFactory() {
        return ((MyApplication) mContext.getApplicationContext()).buildDataSourceFactory();
    }

    private ObjectHelper.MediaFormatCallback mCallback =
            new ObjectHelper.MediaFormatCallback() {
                @Override
                public void onCreated(MediaFormat mediaFormat) {
                    if (mediaFormat == null) {
                        return;
                    }
                    if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                        mVideoMediaFormat = mediaFormat;
                    } else if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                        mAudioMediaFormat = mediaFormat;
                    }
                    MLog.i(TAG, "onCreated() mediaFormat: \n" + mediaFormat);
                    synchronized (GetMediaFormat.class) {
                        if (runOneTime) {
                            runOneTime = false;
                            if (mPlayerWrapper != null) {
                                mPlayerWrapper.startPlayback();
                            }
                            mUiHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    release();
                                }
                            }, 500);
                        }
                    }
                }
            };

}
