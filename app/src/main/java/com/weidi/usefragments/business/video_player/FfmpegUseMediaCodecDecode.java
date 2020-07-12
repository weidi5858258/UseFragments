package com.weidi.usefragments.business.video_player;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;

import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.AACPlayer;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.JniObject;
import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.weidi.usefragments.business.video_player.FFMPEG.USE_MODE_ONLY_VIDEO;
import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_IS_MUTE;
import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

/***
 Created by weidi on 2020/07/11.


 */

public class FfmpegUseMediaCodecDecode {

    private static final String TAG =
            "player_alexander";

    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;// 102400
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024 * 5;// 5242880

    public static final int TYPE_AUDIO = 0x0001;
    public static final int TYPE_VIDEO = 0x0002;
    private static final int TIME_OUT = 10000;

    // 为了注册广播
    private Context mContext = null;
    private String mPath = null;
    private MediaExtractor extractor = null;

    private Callback mCallback = null;
    private Surface mSurface = null;

    public AudioWrapper mAudioWrapper;
    public VideoWrapper mVideoWrapper;
    private FFMPEG mFFMPEG;

    public static class SimpleWrapper {
        public String mime = null;
        public MediaCodec decoderMediaCodec = null;
        public MediaFormat decoderMediaFormat = null;
        // 是否需要渲染图像(播放音频为false,播放视频为true)
        public boolean render = false;
        public int trackIndex = -1;
        // 使用于while条件判断
        public boolean isHandling = false;

        // 用于标识音频还是视频
        public int type;
        // 总时长
        public long durationUs = 0;
        // 播放的时长(下面两个参数一起做的事是每隔一秒发一次回调函数)
        public long sampleTime = 0;
        public long startTimeUs = 0;

        // 使用于时间戳
        public long presentationTimeUs = 0;

        public boolean isStarted = false;
        // 因为user所以pause
        public boolean isPausedForUser = false;
        // 因为cache所以pause
        public boolean isPausedForCache = false;
        // 因为seek所以pause
        public boolean isPausedForSeek = false;

        // 一帧音频或者视频的最大值
        public int frameMaxLength = 0;
        // 音频或者视频一帧的实际大小
        public int size = 0;
        // 放一帧音频或者视频的容器
        public byte[] data = null;

        private SimpleWrapper() {
        }

        public SimpleWrapper(int type) {
            switch (type) {
                case TYPE_AUDIO:
                    this.type = TYPE_AUDIO;
                    frameMaxLength = AUDIO_FRAME_MAX_LENGTH;
                    break;
                case TYPE_VIDEO:
                    this.type = TYPE_VIDEO;
                    frameMaxLength = VIDEO_FRAME_MAX_LENGTH;
                    break;
                default:
                    break;
            }
        }

        public void clear() {
            mime = null;
            decoderMediaCodec = null;
            decoderMediaFormat = null;
            render = false;
            trackIndex = -1;
            isStarted = false;
            isHandling = false;
            isPausedForUser = false;
            isPausedForCache = false;
            isPausedForSeek = false;
            durationUs = 0;
            sampleTime = 0;
            startTimeUs = 0;
            presentationTimeUs = 0;
            //frameMaxLength = 0;
            size = 0;

            /*progressUs = -1;
            needToSeek = false;*/
        }
    }

    public static class AudioWrapper extends SimpleWrapper {
        public AudioTrack mAudioTrack = null;

        private AudioWrapper() {
        }

        public AudioWrapper(int type) {
            super(type);
        }

        public void clear() {
            MediaUtils.releaseAudioTrack(mAudioTrack);
            MediaUtils.releaseMediaCodec(decoderMediaCodec);
            super.clear();
        }
    }

    public static class VideoWrapper extends SimpleWrapper {
        public Surface mSurface = null;
        public int width = 0;
        public int height = 0;

        private VideoWrapper() {
        }

        public VideoWrapper(int type) {
            super(type);
        }

        public void clear() {
            MediaUtils.releaseMediaCodec(decoderMediaCodec);
            super.clear();
        }
    }

    public FfmpegUseMediaCodecDecode() {

    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setDataSource(String path) {
        mPath = path;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public boolean initPlayer() {
        return internalPrepare() && prepareAudio() && prepareVideo();
    }

    public void setVolume(float volume) {
        if (mAudioWrapper.mAudioTrack == null
                || mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return;
        }
        if (volume < 0 || volume > 1.0f) {
            volume = FFMPEG.VOLUME_NORMAL;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioWrapper.mAudioTrack.setVolume(volume);
        } else {
            mAudioWrapper.mAudioTrack.setStereoVolume(volume, volume);
        }
    }

    public void release() {
        MLog.i(TAG, "release() start");
        //notifyVideoEndOfStream();
        //notifyAudioEndOfStream();
        mVideoWrapper.isHandling = false;
        mVideoWrapper.isPausedForUser = false;
        mVideoWrapper.isPausedForCache = false;
        mVideoWrapper.isPausedForSeek = false;

        mAudioWrapper.isHandling = false;
        mAudioWrapper.isPausedForUser = false;
        mAudioWrapper.isPausedForCache = false;
        mAudioWrapper.isPausedForSeek = false;
        MLog.i(TAG, "release() end");
    }

    private boolean prepareAudio() {
        if (TextUtils.isEmpty(mAudioWrapper.mime)
                && mAudioWrapper.trackIndex == -1
                && mAudioWrapper.decoderMediaFormat == null) {
            MLog.d(TAG, "prepareAudio() return");
            return true;
        }
        // Audio
        MLog.d(TAG, "prepareAudio() start");
        MLog.d(TAG, "prepareAudio() audio decoderMediaFormat: " +
                mAudioWrapper.decoderMediaFormat.toString());

        /***
         解码前
         {
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2],
         mime=audio/mp4a-latm, aac-profile=2, channel-count=2, track-id=2, bitrate=96000,
         max-input-size=444, durationUs=10871488000,
         sample-rate=48000, max-bitrate=96000
         }
         解码后
         {pcm-encoding=2, mime=audio/raw, channel-count=2, sample-rate=48000}
         */
        // durationUs
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mAudioWrapper.durationUs =
                    mAudioWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mAudioWrapper.durationUs / 1000) / 1000);
            MLog.d(TAG, "prepareAudio()          audioDurationUs: " +
                    mAudioWrapper.durationUs + " " + durationTime);
            if (mAudioWrapper.durationUs > 0 || mVideoWrapper.durationUs > 0) {
                mAudioWrapper.durationUs =
                        (mAudioWrapper.durationUs > mVideoWrapper.durationUs)
                                ?
                                mAudioWrapper.durationUs
                                :
                                mVideoWrapper.durationUs;
                mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            } else {
                mVideoWrapper.durationUs = mAudioWrapper.durationUs = -1;
            }
        }
        // max-input-size
        if (!mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            // 4K 8192
            mAudioWrapper.decoderMediaFormat.setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_FRAME_MAX_LENGTH);
        }
        mAudioWrapper.frameMaxLength =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        MLog.d(TAG, "prepareAudio() audio     frameMaxLength: " +
                mAudioWrapper.frameMaxLength);
        String errorInfo = null;
        // 创建音频解码器
        try {
            extractor.selectTrack(mAudioWrapper.trackIndex);
            // audio/vorbis
            MLog.d(TAG, "prepareAudio() audio mAudioWrapper.mime: " + mAudioWrapper.mime);
            switch (mAudioWrapper.mime) {
                case "audio/ac4":
                    MediaCodecInfo mediaCodecInfo =
                            MediaUtils.getDecoderMediaCodecInfo(mAudioWrapper.mime);
                    String codecName = null;
                    if (mediaCodecInfo != null) {
                        codecName = mediaCodecInfo.getName();
                    } else {
                        codecName = "OMX.google.raw.decoder";
                        mAudioWrapper.decoderMediaFormat.setString(
                                MediaFormat.KEY_MIME, "audio/raw");
                    }
                    if (!TextUtils.isEmpty(codecName)) {
                        MLog.d(TAG, "prepareAudio() audio          codecName: " + codecName);
                        mAudioWrapper.decoderMediaCodec = MediaCodec.createByCodecName(codecName);
                        mAudioWrapper.decoderMediaCodec.configure(
                                mAudioWrapper.decoderMediaFormat, null, null, 0);
                        mAudioWrapper.decoderMediaCodec.start();
                    }
                    break;
                default:
                    mAudioWrapper.decoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec(
                            mAudioWrapper.mime, mAudioWrapper.decoderMediaFormat);
                    break;
            }

            mAudioWrapper.render = false;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException
                | IOException e) {
            e.printStackTrace();
            errorInfo = e.toString();
            if (mAudioWrapper.decoderMediaCodec != null) {
                mAudioWrapper.decoderMediaCodec.release();
                mAudioWrapper.decoderMediaCodec = null;
            }
        }
        if (mAudioWrapper.decoderMediaCodec == null) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, errorInfo);
            }
            return false;
        }

        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "prepareAudio() audio              csd-0: " + sb.toString());
        }
        if (mAudioWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mAudioWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "prepareAudio() audio              csd-1: " + sb.toString());
        }
        MLog.d(TAG, "prepareAudio() end");

        return true;
    }

    private boolean prepareVideo() {
        if (TextUtils.isEmpty(mVideoWrapper.mime)
                && mVideoWrapper.trackIndex == -1
                && mVideoWrapper.decoderMediaFormat == null) {
            MLog.d(TAG, "prepareVideo() return");
            return true;
        }

        // Video
        MLog.w(TAG, "prepareVideo() start");
        if (mSurface != null) {
            mVideoWrapper.mSurface = mSurface;
        } else {
            MLog.e(TAG, "prepareVideo() mSurface is null");
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, "Surface is null");
            }
            return false;
        }
        MLog.w(TAG, "prepareVideo() video decoderMediaFormat: " +
                mVideoWrapper.decoderMediaFormat.toString());

        /***
         BITRATE_MODE_CQ:  表示完全不控制码率，尽最大可能保证图像质量
         BITRATE_MODE_CBR: 表示编码器会尽量把输出码率控制为设定值
         BITRATE_MODE_VBR: 表示编码器会根据图像内容的复杂度（实际上是帧间变化量的大小）来动态调整输出码率，
         图像复杂则码率高，图像简单则码率低
         */
        mVideoWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        // https://www.polarxiong.com/archives/Android-MediaCodec%E8%A7%86%E9%A2%91%E6%96%87%E4
        // %BB%B6%E7%A1%AC%E4%BB%B6%E8%A7%A3%E7%A0%81-%E9%AB%98%E6%95%88%E7%8E%87%E5%BE%97%E5%88
        // %B0YUV%E6%A0%BC%E5%BC%8F%E5%B8%A7-%E5%BF%AB%E9%80%9F%E4%BF%9D%E5%AD%98JPEG%E5%9B%BE%E7
        // %89%87-%E4%B8%8D%E4%BD%BF%E7%94%A8OpenGL.html
        // 指定解码后的帧格式(解码器将编码的帧解码为这种指定的格式)
        // COLOR_FormatYUV420Flexible是几乎所有解码器都支持的一种帧格式
        // 经过这种格式的指定,解码视频后的帧格式可以很方便的转换成YUV420SemiPlanar(NV21)或YUV420Planar(I420)格式
        mVideoWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

        /***
         解码前
         {
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=28 cap=28],
         csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9],
         mime=video/avc, frame-rate=24, track-id=1, profile=8,
         width=1280, height=720, max-input-size=243905, durationUs=10871402208,
         bitrate-mode=0, level=512
         }
         解码后
         {crop-top=0, crop-right=1279, color-format=19, height=720,
         color-standard=1, crop-left=0, color-transfer=3, stride=1280,
         mime=video/raw, slice-height=720, width=1280, color-range=2, crop-bottom=719}
         */
        // durationUs
        if (mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
            mVideoWrapper.durationUs =
                    mVideoWrapper.decoderMediaFormat.getLong(MediaFormat.KEY_DURATION);
            String durationTime = DateUtils.formatElapsedTime(
                    (mVideoWrapper.durationUs / 1000) / 1000);
            MLog.w(TAG, "prepareVideo()          videoDurationUs: " +
                    mVideoWrapper.durationUs + " " + durationTime);
            if (mAudioWrapper.durationUs > 0 || mVideoWrapper.durationUs > 0) {
                mAudioWrapper.durationUs =
                        (mAudioWrapper.durationUs > mVideoWrapper.durationUs)
                                ?
                                mAudioWrapper.durationUs
                                :
                                mVideoWrapper.durationUs;
                mVideoWrapper.durationUs = mAudioWrapper.durationUs;
            } else {
                mVideoWrapper.durationUs = mAudioWrapper.durationUs = -1;
            }
        }
        // max-input-size
        if (!mVideoWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            // 4K 3110400
            mVideoWrapper.decoderMediaFormat.setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE, VIDEO_FRAME_MAX_LENGTH);
        }
        mVideoWrapper.frameMaxLength =
                mVideoWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        MLog.w(TAG, "prepareVideo() video     frameMaxLength: " +
                mVideoWrapper.frameMaxLength);
        String errorInfo = null;
        // 创建视频解码器
        try {
            extractor.selectTrack(mVideoWrapper.trackIndex);
            // video/x-vnd.on2.vp9
            MLog.w(TAG, "prepareVideo() video mVideoWrapper.mime: " + mVideoWrapper.mime);
            mVideoWrapper.decoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            mVideoWrapper.mime,
                            mVideoWrapper.decoderMediaFormat,
                            mVideoWrapper.mSurface);
            mVideoWrapper.render = true;
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException e) {
            e.printStackTrace();
            errorInfo = e.toString();
            if (mVideoWrapper.decoderMediaCodec != null) {
                mVideoWrapper.decoderMediaCodec.release();
                mVideoWrapper.decoderMediaCodec = null;
            }
        }
        if (mVideoWrapper.decoderMediaCodec == null) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            if (mCallback != null) {
                mCallback.onError(Callback.ERROR_FFMPEG_INIT, errorInfo);
            }
            return false;
        }

        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.w(TAG, "prepareVideo() video              csd-0: " + sb.toString());
        }
        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.w(TAG, "prepareVideo() video              csd-1: " + sb.toString());
        }
        MLog.w(TAG, "prepareVideo() end");

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            MLog.e(TAG, "internalPrepare() mPath is empty");
            return false;
        }
        MLog.i(TAG, "internalPrepare() start");

        if (mCallback != null) {
            mCallback.onReady();
        }

        if (mFFMPEG == null) {
            mFFMPEG = FFMPEG.getDefault();
        }
        mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
        mVideoWrapper = new VideoWrapper(TYPE_VIDEO);
        mAudioWrapper.clear();
        mVideoWrapper.clear();
        mAudioWrapper.isHandling = true;
        mVideoWrapper.isHandling = true;

        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            if (mCallback != null) {
                mCallback.onError(
                        Callback.ERROR_FFMPEG_INIT, "setDataSource() occurs error");
            }
            return false;
        }

        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mAudioWrapper.mime = mime;
                mAudioWrapper.trackIndex = i;
                mAudioWrapper.decoderMediaFormat = format;
            } else if (mime.startsWith("video/")) {
                mVideoWrapper.mime = mime;
                mVideoWrapper.trackIndex = i;
                mVideoWrapper.decoderMediaFormat = format;
            }
        }
        if ((TextUtils.isEmpty(mAudioWrapper.mime)
                && TextUtils.isEmpty(mVideoWrapper.mime))
                || (mAudioWrapper.trackIndex == -1
                && mVideoWrapper.trackIndex == -1)
                || (mAudioWrapper.decoderMediaFormat == null
                && mVideoWrapper.decoderMediaFormat == null)) {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            if (mCallback != null) {
                mCallback.onError(
                        Callback.ERROR_FFMPEG_INIT, "audio and video aren't find");
            }
            return false;
        }

        MLog.i(TAG, "internalPrepare() end");

        return true;
    }

    private static final int AV_CODEC_ID_HEVC = 173;
    private static final int AV_CODEC_ID_H264 = 27;
    private static final int AV_CODEC_ID_MPEG4 = 12;
    private static final int AV_CODEC_ID_VP8 = 139;
    private static final int AV_CODEC_ID_VP9 = 167;
    private static final int AV_CODEC_ID_MPEG2VIDEO = 2;

    /***
     MediaFormat.MIMETYPE_VIDEO_HEVC
     video/hevc
     {track-id=1, level=65536, mime=video/hevc, profile=1, language=und, display-width=3840,
     durationUs=20000400, display-height=2160, width=3840, max-input-size=1086672, frame-rate=60,
     height=2160, csd-0=java.nio.HeapByteBuffer[pos=0 lim=88 cap=88]}
     csd-0: {0, 0, 0, 1, 64, 1, 12, 1, -1, -1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0,
     -103, -107, -104, 9, 0, 0, 0, 1, 66, 1, 1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0,
     -103, -96, 1, -32, 32, 2, 28, 89, 101, 102, -110, 76, -82, 106, 4, 36, 4, 8, 0, 0, 31, 64,
     0, 7, 83, 0, 64, 0, 0, 0, 1, 68, 1, -63, 114, -76, 98, 64, 0}

     MediaFormat.MIMETYPE_VIDEO_AVC
     video/avc
     {track-id=1, level=2048, mime=video/avc, profile=8, language=und, display-width=1920,
     csd-1=java.nio.HeapByteBuffer[pos=0 lim=8 cap=8], durationUs=585966666, display-height=1080,
     width=1920, max-input-size=173638, frame-rate=30, height=1080, csd-0=java.nio
     .HeapByteBuffer[pos=0 lim=34 cap=34]}
     csd-0: {0, 0, 0, 1, 103, 100, 0, 40, -84, -47, 0, 120, 2, 39, -27, -64, 90, -128, -128,
     -125, 32, 0, 0, 3, 0, 32, 0, 0, 7, -127, -29, 6, 34, 64}
     csd-1: {0, 0, 0, 1, 104, -21, -113, 44}

     MediaFormat.MIMETYPE_VIDEO_MPEG4
     video/mp4v-es
     {max-bitrate=800000, track-id=1, level=4, mime=video/mp4v-es, profile=1, language=und,
     display-width=640, durationUs=69200000, display-height=480, width=640, max-input-size=25046,
     frame-rate=25, height=480, csd-0=java.nio.HeapByteBuffer[pos=0 lim=45 cap=45]}
     csd-0: {0, 0, 1, -80, 1, 0, 0, 1, -75, -119, 19, 0, 0, 1, 0, 0, 0, 1, 32, 0, -60, -115,
     -120, 0, -51, 20, 4, 60, 20, 99, 0, 0, 1, -78, 76, 97, 118, 99, 53, 50, 46, 49, 53, 46, 48}

     MediaFormat.MIMETYPE_VIDEO_VP8
     video/x-vnd.on2.vp8
     {track-id=1, durationUs=52069000, display-height=1080, mime=video/x-vnd.on2.vp8, width=1920,
     frame-rate=30, height=1080, display-width=1920}

     MediaFormat.MIMETYPE_VIDEO_VP9
     video/x-vnd.on2.vp9
     {color-transfer=3, track-id=1, durationUs=328983000, display-height=2160, mime=video/x-vnd
     .on2.vp9, width=3840, color-range=2, color-standard=1, frame-rate=24, height=2160,
     display-width=3840}

     MediaFormat.MIMETYPE_VIDEO_MPEG2
     video/mpeg2
     {track-id=1, mime=video/mpeg2, width=1920, height=1080, csd-0=java.nio.HeapByteBuffer[pos=0
     lim=22 cap=22]}
     csd-0: {0, 0, 1, -77, 120, 4, 56, 53, -1, -1, -32, 24, 0, 0, 1, -75, 20, 74, 0, 1, 0, 0}



     */
    public boolean initMediaCodec(Message msg) {
        JniObject jniObject = (JniObject) msg.obj;
        int mimeType = jniObject.valueInt;
        Object[] valueObjectArray = jniObject.valueObjectArray;
        if (valueObjectArray.length < 3) {
            return false;
        }

        MLog.w(TAG, "initMediaCodec() start");
        MLog.w(TAG, "initMediaCodec()    mimeType: " + mimeType);
        String mime = null;
        switch (mimeType) {
            case AV_CODEC_ID_HEVC:
                mime = MediaFormat.MIMETYPE_VIDEO_HEVC;
                break;
            case AV_CODEC_ID_H264:
                mime = MediaFormat.MIMETYPE_VIDEO_AVC;
                break;
            case AV_CODEC_ID_MPEG4:
                mime = MediaFormat.MIMETYPE_VIDEO_MPEG4;
                break;
            case AV_CODEC_ID_VP8:
                mime = MediaFormat.MIMETYPE_VIDEO_VP8;
                break;
            case AV_CODEC_ID_VP9:
                mime = MediaFormat.MIMETYPE_VIDEO_VP9;
                break;
            case AV_CODEC_ID_MPEG2VIDEO:
                mime = MediaFormat.MIMETYPE_VIDEO_MPEG2;
                break;
            default:
                break;
        }
        if (TextUtils.isEmpty(mime) || mSurface == null) {
            MLog.e(TAG, "initMediaCodec() TextUtils.isEmpty(mime) || mSurface == null");
            return false;
        }
        MLog.w(TAG, "initMediaCodec() video  mime: " + mime);

        if (mVideoWrapper != null) {
            MediaUtils.releaseMediaCodec(mVideoWrapper.decoderMediaCodec);
        }

        if (mFFMPEG == null) {
            mFFMPEG = FFMPEG.getDefault();
        }
        mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
        mVideoWrapper = new VideoWrapper(TYPE_VIDEO);
        mAudioWrapper.clear();
        mVideoWrapper.clear();
        mAudioWrapper.isHandling = true;
        mVideoWrapper.isHandling = true;

        int[] parameters = (int[]) valueObjectArray[0];
        mVideoWrapper.width = parameters[0];
        mVideoWrapper.height = parameters[1];
        MediaFormat mediaFormat = MediaUtils.getVideoDecoderMediaFormat(
                parameters[0], parameters[1]);

        Object object1 = valueObjectArray[1];
        Object object2 = valueObjectArray[2];
        byte[] csd0 = null;
        byte[] csd1 = null;
        if (object1 != null && object2 != null) {
            csd0 = (byte[]) object1;
            csd1 = (byte[]) object2;
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));
        } else if (object1 != null && object2 == null) {
            csd0 = (byte[]) object1;
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
        } else if (object1 == null && object2 == null) {

        }

        mediaFormat.setString(MediaFormat.KEY_MIME, mime);
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        // 设置帧率
        // mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, argus[2]);
        // 设置比特率
        // mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, argus[3]);
        mVideoWrapper.mime = mime;
        mVideoWrapper.decoderMediaFormat = mediaFormat;
        mVideoWrapper.mSurface = mSurface;

        mVideoWrapper.decoderMediaCodec =
                MediaUtils.getVideoDecoderMediaCodec(
                        mVideoWrapper.mime,
                        mVideoWrapper.decoderMediaFormat,
                        mVideoWrapper.mSurface);
        mVideoWrapper.render = true;
        if (mVideoWrapper.decoderMediaCodec == null) {
            MLog.e(TAG, "initMediaCodec() mVideoWrapper.decoderMediaCodec is null");
        }

        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.w(TAG, "initMediaCodec() video csd-0: " + sb.toString());
        }
        if (mVideoWrapper.decoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mVideoWrapper.decoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.w(TAG, "initMediaCodec() video csd-1: " + sb.toString());
        }

        MLog.w(TAG, "initMediaCodec() end");
        return true;
    }

    public boolean feedInputBufferAndDrainOutputBuffer(SimpleWrapper wrapper) {
        if (!wrapper.isHandling) {
            handleVideoOutputBuffer(-1, null, null, 0);
            return false;
        }

        return feedInputBuffer(wrapper.decoderMediaCodec,
                wrapper.data,
                0,
                wrapper.size,
                wrapper.sampleTime)
                &&
                drainOutputBuffer(wrapper);
    }

    /***
     * 填充数据送到底层进行编解码
     * @param codec
     * @param data
     * @param offset
     * @param size
     * @param presentationTimeUs
     * @return
     */
    private boolean feedInputBuffer(
            MediaCodec codec,
            byte[] data,
            int offset,
            int size,
            long presentationTimeUs) {
        try {
            // 拿到房间号
            int roomIndex = codec.dequeueInputBuffer(TIME_OUT);
            if (roomIndex >= 0) {
                ByteBuffer room = null;
                // 根据房间号找到房间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = codec.getInputBuffer(roomIndex);
                } else {
                    room = codec.getInputBuffers()[roomIndex];
                }
                if (room == null) {
                    return false;
                }
                // 入住之前打扫一下房间
                room.clear();
                // 入住
                room.put(data, offset, size);
                int flags = 0;
                if (size <= 0) {
                    presentationTimeUs = 0L;
                    flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                }
                // 通知已经"入住"了,可以进行"编解码"的操作了
                codec.queueInputBuffer(
                        roomIndex,
                        offset,
                        size,
                        presentationTimeUs,
                        flags);
                // reset
                roomIndex = -1;
                room = null;
            }
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | NullPointerException e) {
            e.printStackTrace();
            MLog.e(TAG, "feedInputBuffer() Input occur exception: " + e);
            handleVideoOutputBuffer(-1, null, null, 0);
            MediaUtils.releaseMediaCodec(codec);
            return false;
        }

        return true;
    }

    /***
     * 拿出数据(在底层已经经过编解码了)进行处理(如视频数据进行渲染,音频数据进行播放)
     * @param wrapper
     * @return
     */
    private boolean drainOutputBuffer(SimpleWrapper wrapper) {
        // 房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        ByteBuffer room = null;
        for (; ; ) {
            if (!wrapper.isHandling) {
                handleVideoOutputBuffer(-1, null, null, 0);
                return false;
            }

            try {
                // 房间号
                int roomIndex = wrapper.decoderMediaCodec.dequeueOutputBuffer(roomInfo, TIME_OUT);
                /*if (wrapper.type == TYPE_AUDIO) {
                    MLog.d(TAG, "drainOutputBuffer() Audio roomIndex: " + roomIndex);
                } else {
                    MLog.w(TAG, "drainOutputBuffer() Video roomIndex: " + roomIndex);
                }*/
                switch (roomIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        // 像音频,第一个输出日志
                        /*if (wrapper.type == TYPE_AUDIO) {
                            MLog.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_TRY_AGAIN_LATER");
                        } else {
                            MLog.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_TRY_AGAIN_LATER");
                        }*/
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        // 像音频,第三个输出日志
                        // 一般一个视频各自调用一次
                        if (wrapper.type == TYPE_AUDIO) {
                            MLog.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleAudioOutputFormat();
                        } else {
                            MLog.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleVideoOutputFormat();
                        }
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        // 像音频,第二个输出日志.视频好像没有这个输出日志
                        if (wrapper.type == TYPE_AUDIO) {
                            MLog.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        } else {
                            MLog.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        }
                        break;
                    default:
                        break;
                }
                if (roomIndex < 0) {
                    break;
                }

                // 根据房间号找到房间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    room = wrapper.decoderMediaCodec.getOutputBuffer(roomIndex);
                } else {
                    room = wrapper.decoderMediaCodec.getOutputBuffers()[roomIndex];
                }
                // 房间大小
                int roomSize = roomInfo.size;
                // 不能根据room是否为null来判断是audio还是video(但我的三星Note2手机上是可以的)
                if (room != null) {
                    // audio
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    if (wrapper.type == TYPE_AUDIO) {
                        handleAudioOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    } else {
                        handleVideoOutputBuffer(roomIndex, room, roomInfo, roomSize);
                    }
                    room.clear();
                } else {
                    // video
                    handleVideoOutputBuffer(roomIndex, null, roomInfo, roomSize);
                }

                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    } else {
                        MLog.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                    }
                    // 结束
                    return false;
                }
                if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (wrapper.type == TYPE_AUDIO) {
                        MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    } else {
                        MLog.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    }
                }

                wrapper.decoderMediaCodec.releaseOutputBuffer(roomIndex, wrapper.render);
            } catch (IllegalStateException
                    | IllegalArgumentException
                    | NullPointerException e) {
                e.printStackTrace();
                if (wrapper.type == TYPE_AUDIO) {
                    MLog.e(TAG, "drainOutputBuffer() Audio Output occur exception: " + e);
                } else {
                    MLog.e(TAG, "drainOutputBuffer() Video Output occur exception: " + e);
                    handleVideoOutputBuffer(-1, room, roomInfo, 0);
                    MediaUtils.releaseMediaCodec(wrapper.decoderMediaCodec);
                }
                return false;
            }
        }// for(;;) end

        return true;
    }

    private void handleAudioOutputFormat() {
        /***
         解码前
         {mime=audio/mp4a-latm, aac-profile=2, channel-count=2, track-id=2, bitrate=96000,
         max-input-size=444, durationUs=10871488000,
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=2 cap=2],
         sample-rate=48000, max-bitrate=96000}
         解码后
         {pcm-encoding=2, mime=audio/raw, channel-count=2, sample-rate=48000}
         */
        try {
            MediaFormat newMediaFormat = mAudioWrapper.decoderMediaCodec.getOutputFormat();
            Class clazz = Class.forName("android.media.MediaFormat");
            Method method = clazz.getDeclaredMethod("getMap");
            method.setAccessible(true);
            Object newObject = method.invoke(newMediaFormat);
            Object oldObject = method.invoke(mAudioWrapper.decoderMediaFormat);
            if (newObject != null
                    && newObject instanceof Map
                    && oldObject != null
                    && oldObject instanceof Map) {
                Map<String, Object> newMap = (Map) newObject;
                Map<String, Object> oldMap = (Map) oldObject;
                for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                    oldMap.put(entry.getKey(), entry.getValue());
                }
            }
            MLog.d(TAG, "handleAudioOutputFormat() newMediaFormat: " +
                    mAudioWrapper.decoderMediaFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mAudioWrapper.mAudioTrack != null) {
            mAudioWrapper.mAudioTrack.release();
        }

        // 创建AudioTrack
        // 1.
        int sampleRateInHz =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        // 2.
        int channelCount =
                mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        // 3.
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (mAudioWrapper.decoderMediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            // 关键参数(需要解码后才能知道)
            audioFormat =
                    mAudioWrapper.decoderMediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
        }

        // sampleRateInHz: 48000 channelCount: 2 audioFormat: 2
        MLog.d(TAG, "handleAudioOutputFormat()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);

        // create AudioTrack
        mAudioWrapper.mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioWrapper.mAudioTrack != null) {
            if (mContext != null) {
                SharedPreferences sp =
                        mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean isMute = sp.getBoolean(PLAYBACK_IS_MUTE, false);
                if (!isMute) {
                    setVolume(FFMPEG.VOLUME_NORMAL);
                } else {
                    setVolume(FFMPEG.VOLUME_MUTE);
                }
            } else {
                setVolume(FFMPEG.VOLUME_NORMAL);
            }
            mAudioWrapper.mAudioTrack.play();
            mAudioWrapper.isStarted = true;
            /*while (!mVideoWrapper.isStarted) {
                if (!mAudioWrapper.isHandling) {
                    return;
                }
                SystemClock.sleep(10);
            }*/
            MLog.d(TAG, "handleAudioOutputFormat() 声音马上输出......");
        } else {
            MLog.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
            mAudioWrapper.isHandling = false;
        }

        mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        mAudioWrapper.decoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
        if (AACPlayer.sampleRateIndexMap.containsKey(sampleRateInHz)
                && AACPlayer.channelConfigIndexMap.containsKey(channelCount)) {
            List<byte[]> list = new ArrayList<>();
            list.add(MediaUtils.buildAacAudioSpecificConfig(
                    AACPlayer.sampleRateIndexMap.get(sampleRateInHz),
                    AACPlayer.channelConfigIndexMap.get(channelCount)));
            MediaUtils.setCsdBuffers(mAudioWrapper.decoderMediaFormat, list);
        }

        // 音视频都已经开始的话,就可以播放了
        if (mCallback != null) {
            MLog.i(TAG, "onPlayed");
            mCallback.onPlayed();
        }
    }

    private void handleVideoOutputFormat() {
        /***
         解码前
         {csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9],
         mime=video/avc, frame-rate=24, track-id=1, profile=8,
         width=1280, height=720, max-input-size=243905, durationUs=10871402208,
         csd-0=java.nio.HeapByteBuffer[pos=0 lim=28 cap=28],
         bitrate-mode=0, level=512}
         解码后
         {crop-top=0, crop-right=1279, color-format=19, height=720,
         color-standard=1, crop-left=0, color-transfer=3, stride=1280,
         mime=video/raw, slice-height=720, width=1280, color-range=2, crop-bottom=719}
         */
        try {
            MediaFormat newMediaFormat = mVideoWrapper.decoderMediaCodec.getOutputFormat();
            Class clazz = Class.forName("android.media.MediaFormat");
            Method method = clazz.getDeclaredMethod("getMap");
            method.setAccessible(true);
            Object newObject = method.invoke(newMediaFormat);
            Object oldObject = method.invoke(mVideoWrapper.decoderMediaFormat);
            if (newObject != null
                    && newObject instanceof Map
                    && oldObject != null
                    && oldObject instanceof Map) {
                Map<String, Object> newMap = (Map) newObject;
                Map<String, Object> oldMap = (Map) oldObject;
                for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                    oldMap.put(entry.getKey(), entry.getValue());
                }
            }
            MLog.w(TAG, "handleVideoOutputFormat() newMediaFormat: " +
                    mVideoWrapper.decoderMediaFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mVideoWrapper.isStarted = true;
        // 如果是视频先走到这里,那么等待音频的开始
        /*while (!mAudioWrapper.isStarted) {
            if (!mVideoWrapper.isHandling) {
                return;
            }
            SystemClock.sleep(10);
        }*/

        MLog.w(TAG, "handleVideoOutputFormat() 画面马上输出......");
    }

    private int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                        MediaCodec.BufferInfo roomInfo, int roomSize) {
        return 0;
    }

    private JniObject mJniObject = new JniObject();
    //private int[] valueIntArray = new int[2];
    //private Object[] valueObjectArray = new Object[2];

    // 最最最关键的一步
    private int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                        MediaCodec.BufferInfo roomInfo, int roomSize) {
        if (mFFMPEG != null) {
            return Integer.parseInt(
                    mFFMPEG.onTransact(
                            FFMPEG.DO_SOMETHING_CODE_handleVideoOutputBuffer, mJniObject));

            /*valueIntArray[0] = roomIndex;
            valueIntArray[1] = roomSize;
            valueObjectArray[0] = room;
            valueObjectArray[1] = roomInfo;
            mJniObject.valueIntArray = valueIntArray;
            mJniObject.valueObjectArray = valueObjectArray;
            return Integer.parseInt(
                    mFFMPEG.onTransact(
                            FFMPEG.DO_SOMETHING_CODE_handleVideoOutputBuffer, mJniObject));*/
        }
        return 0;
    }

    private void notifyAudioEndOfStream() {
        MLog.d(TAG, "notifyAudioEndOfStream() start");
        // audio end notify
        int inputAudioBufferIndex = mAudioWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        while (inputAudioBufferIndex < 0) {
            inputAudioBufferIndex = mAudioWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mAudioWrapper.decoderMediaCodec.queueInputBuffer(
                inputAudioBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        MLog.d(TAG, "notifyAudioEndOfStream() end");
    }

    private void notifyVideoEndOfStream() {
        MLog.w(TAG, "notifyVideoEndOfStream() start");
        // video end notify
        int inputBufferIndex = mVideoWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        while (inputBufferIndex < 0) {
            inputBufferIndex = mVideoWrapper.decoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mVideoWrapper.decoderMediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        MLog.w(TAG, "notifyVideoEndOfStream() end");
    }

}
