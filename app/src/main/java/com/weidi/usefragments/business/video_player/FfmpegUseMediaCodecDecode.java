package com.weidi.usefragments.business.video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;

import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.AACPlayer;
import com.weidi.usefragments.tool.JniObject;
import com.weidi.usefragments.tool.MLog;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_IS_MUTE;
import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_USE_EXOPLAYER_OR_FFMPEG;
import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_USE_PLAYER;
import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYER_FFMPEG_MEDIACODEC;
import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

/***
 Created by weidi on 2020/07/11.


 */

public class FfmpegUseMediaCodecDecode {

    private static final String TAG =
            "player_alexander";

    private static final int AUDIO_FRAME_MAX_LENGTH = 1024 * 100;// 102400
    private static final int VIDEO_FRAME_MAX_LENGTH = 1024 * 1024 * 5;// 5242880

    // 不要修改值,如果要修改,MediaPlayerForMediaCodec.cpp中也要修改
    public static final int TYPE_AUDIO = 0x0001;
    public static final int TYPE_VIDEO = 0x0002;

    // 为了注册广播
    private Context mContext = null;
    private Surface mSurface = null;

    public AudioWrapper mAudioWrapper = null;
    public VideoWrapper mVideoWrapper = null;
    private FFMPEG mFFMPEG = null;
    private GetMediaFormat mGetMediaFormat = null;

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
        if (mFFMPEG == null) {
            mFFMPEG = FFMPEG.getDefault();
        }
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setVolume(float volume) {
        if (mAudioWrapper == null
                || mAudioWrapper.mAudioTrack == null
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
        if (mVideoWrapper != null) {
            mVideoWrapper.isHandling = false;
            mVideoWrapper.isPausedForUser = false;
            mVideoWrapper.isPausedForCache = false;
            mVideoWrapper.isPausedForSeek = false;
        }

        if (mAudioWrapper != null) {
            mAudioWrapper.isHandling = false;
            mAudioWrapper.isPausedForUser = false;
            mAudioWrapper.isPausedForCache = false;
            mAudioWrapper.isPausedForSeek = false;
        }
        MLog.i(TAG, "release() end");
    }

    public void setGetMediaFormat(GetMediaFormat getMediaFormat) {
        mGetMediaFormat = getMediaFormat;
    }

    // video
    private static final int AV_CODEC_ID_HEVC = 173;
    private static final int AV_CODEC_ID_H264 = 27;
    private static final int AV_CODEC_ID_MPEG4 = 12;
    private static final int AV_CODEC_ID_VP8 = 139;
    private static final int AV_CODEC_ID_VP9 = 167;
    private static final int AV_CODEC_ID_MPEG2VIDEO = 2;

    // audio
    private static final int AV_CODEC_ID_MP2 = 86016;
    private static final int AV_CODEC_ID_MP3 = 86017;
    private static final int AV_CODEC_ID_AAC = 86018;
    private static final int AV_CODEC_ID_AC3 = 86019;
    private static final int AV_CODEC_ID_VORBIS = 86021;
    private static final int AV_CODEC_ID_FLAC = 86028;
    private static final int AV_CODEC_ID_QCELP = 86040;
    private static final int AV_CODEC_ID_EAC3 = 86056;
    private static final int AV_CODEC_ID_AAC_LATM = 86065;
    private static final int AV_CODEC_ID_OPUS = 86076;

    /***
     video
     只有csd-0
     MediaFormat.MIMETYPE_VIDEO_HEVC
     video/hevc
     {track-id=1, level=65536, mime=video/hevc, profile=1, language=und, display-width=3840,
     durationUs=20000400, display-height=2160, width=3840, max-input-size=1086672, frame-rate=60,
     height=2160, csd-0=java.nio.HeapByteBuffer[pos=0 lim=88 cap=88]}
     csd-0: {0, 0, 0, 1, 64, 1, 12, 1, -1, -1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0,
     -103, -107, -104, 9, 0, 0, 0, 1, 66, 1, 1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0,
     -103, -96, 1, -32, 32, 2, 28, 89, 101, 102, -110, 76, -82, 106, 4, 36, 4, 8, 0, 0, 31, 64,
     0, 7, 83, 0, 64, 0, 0, 0, 1, 68, 1, -63, 114, -76, 98, 64, 0}

     MediaFormat.MIMETYPE_VIDEO_MPEG4
     video/mp4v-es
     {max-bitrate=800000, track-id=1, level=4, mime=video/mp4v-es, profile=1, language=und,
     display-width=640, durationUs=69200000, display-height=480, width=640, max-input-size=25046,
     frame-rate=25, height=480, csd-0=java.nio.HeapByteBuffer[pos=0 lim=45 cap=45]}
     csd-0: {0, 0, 1, -80, 1, 0, 0, 1, -75, -119, 19, 0, 0, 1, 0, 0, 0, 1, 32, 0, -60, -115,
     -120, 0, -51, 20, 4, 60, 20, 99, 0, 0, 1, -78, 76, 97, 118, 99, 53, 50, 46, 49, 53, 46, 48}

     MediaFormat.MIMETYPE_VIDEO_MPEG2
     video/mpeg2
     {track-id=1, mime=video/mpeg2, width=1920, height=1080, csd-0=java.nio.HeapByteBuffer[pos=0
     lim=22 cap=22]}
     csd-0: {0, 0, 1, -77, 120, 4, 56, 53, -1, -1, -32, 24, 0, 0, 1, -75, 20, 74, 0, 1, 0, 0}

     有csd-0, csd-1
     MediaFormat.MIMETYPE_VIDEO_AVC
     video/avc
     {track-id=1, level=2048, mime=video/avc, profile=8, language=und, display-width=1920,
     csd-1=java.nio.HeapByteBuffer[pos=0 lim=8 cap=8], durationUs=585966666, display-height=1080,
     width=1920, max-input-size=173638, frame-rate=30, height=1080, csd-0=java.nio
     .HeapByteBuffer[pos=0 lim=34 cap=34]}
     csd-0: {0, 0, 0, 1, 103, 100, 0, 40, -84, -47, 0, 120, 2, 39, -27, -64, 90, -128, -128,
     -125, 32, 0, 0, 3, 0, 32, 0, 0, 7, -127, -29, 6, 34, 64}
     csd-1: {0, 0, 0, 1, 104, -21, -113, 44}

     没有csd-0, csd-1
     MediaFormat.MIMETYPE_VIDEO_VP8
     video/x-vnd.on2.vp8
     {track-id=1, durationUs=52069000, display-height=1080, mime=video/x-vnd.on2.vp8, width=1920,
     frame-rate=30, height=1080, display-width=1920}

     MediaFormat.MIMETYPE_VIDEO_VP9
     video/x-vnd.on2.vp9
     {color-transfer=3, track-id=1, durationUs=328983000, display-height=2160, mime=video/x-vnd
     .on2.vp9, width=3840, color-range=2, color-standard=1, frame-rate=24, height=2160,
     display-width=3840}

     audio
     MediaFormat.MIMETYPE_AUDIO_MPEG
     audio/mpeg
     {encoder-delay=576, sample-rate=44100, pcm-encoding=2, track-id=1, durationUs=207856326,
     mime=audio/raw, channel-count=2, bitrate=320000, mime-old=audio/mpeg, encoder-padding=698,
     max-input-size=102400}

     MediaFormat.MIMETYPE_AUDIO_RAW
     MediaFormat.MIMETYPE_AUDIO_FLAC
     audio/raw
     {sample-rate=96000, pcm-encoding=2, track-id=1, durationUs=170533333, mime=audio/raw,
     channel-count=2, mime-old=audio/raw, max-input-size=102400}

     MediaFormat.MIMETYPE_AUDIO_EAC3
     audio/eac3
     {sample-rate=48000, track-id=2, durationUs=38112000, mime=audio/eac3, channel-count=2,
     language=eng, max-input-size=788}

     MediaFormat.MIMETYPE_AUDIO_AAC
     audio/mp4a-latm
     {sample-rate=44100, pcm-encoding=2, track-id=1, durationUs=5534069040, mime=audio/raw,
     profile=2, channel-count=2, mime-old=audio/mp4a-latm, max-input-size=102400,
     csd-0=java.nio.HeapByteBuffer[pos=2 lim=2 cap=2]}
     {max-bitrate=82376, sample-rate=16000, track-id=1, mime=audio/raw, profile=2, language=und,
     aac-profile=2, bitrate=64000, encoder-delay=2112, pcm-encoding=2, durationUs=788096000,
     channel-count=2, mime-old=audio/mp4a-latm, encoder-padding=491, max-input-size=1257,
     csd-0=java.nio.HeapByteBuffer[pos=2 lim=2 cap=2]}
     csd-0: {18, 16}
     csd-0: {19, 16, 86, -27, -99, 72, 0}
     csd-0: {20, 16}
     csd-0: {17, -112}
     csd-0: {17, -112, 86, -27, 0}

     MediaFormat.MIMETYPE_AUDIO_VORBIS
     audio/vorbis
     {csd-1=java.nio.HeapByteBuffer[pos=3861 lim=3861 cap=3861], sample-rate=48000, pcm-encoding=2,
     track-id=1, durationUs=1070812, mime=audio/raw, channel-count=2, mime-old=audio/vorbis,
     max-input-size=102400, csd-0=java.nio.HeapByteBuffer[pos=30 lim=30 cap=30]}
     csd-0: {...}
     csd-1: {...}

     MediaFormat.MIMETYPE_AUDIO_AC3
     audio/ac3
     {sample-rate=48000, pcm-encoding=2, track-id=2, durationUs=128160000, mime=audio/raw,
     channel-count=2, language=und, mime-old=audio/ac3, max-input-size=1812}
     csd-0: {0, 0, 0, 1, 103, 100, 0, 41, -84, 27, 26, 80, 30, 1, 19, -9, -128, -75, 1, 1, 1, 64,
     0, 0, -6, 64, 0, 58, -104, 56, -104, 0, 1, 48, -33, 0, 0, 28, -100, 62, 49, 46, 49, 48, 0, 2,
     97, -66, 0, 0, 57, 56, 124, 98, 92, 62, 56, 97, 75}
     csd-1: {0, 0, 0, 1, 104, -6, -116, -14, 60}
     {sample-rate=48000, pcm-encoding=2, track-id=2, durationUs=5617728000, mime=audio/raw,
     channel-count=2, language=new, mime-old=audio/ac3, max-input-size=1556}
     */
    public boolean initAudioMediaCodec(JniObject jniObject) {
        if (mContext != null) {
            SharedPreferences sp =
                    mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            String whatPlayer = sp.getString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC);
            if (!TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
                return false;
            }
        }
        MLog.d(TAG, "initAudioMediaCodec() start");
        MediaFormat mediaFormat = null;
        String audioMime = null;
        if (mGetMediaFormat != null
                && mGetMediaFormat.mAudioMediaFormat != null) {
            mediaFormat = mGetMediaFormat.mAudioMediaFormat;
            audioMime = mediaFormat.getString(MediaFormat.KEY_MIME);
            MLog.d(TAG, "initAudioMediaCodec() audio GetMediaFormat");
        } else {
            // region

            if (jniObject == null
                    || jniObject.valueObjectArray == null
                    || jniObject.valueObjectArray.length < 3) {
                MLog.e(TAG, "initAudioMediaCodec() jniObject failure");
                return false;
            }

            int audioMimeType = jniObject.valueInt;
            Object[] valueObjectArray = jniObject.valueObjectArray;

            MLog.d(TAG, "initAudioMediaCodec()    mimeType: " + audioMimeType);
            switch (audioMimeType) {
                /*case AV_CODEC_ID_MP2: {// 86016
                    audioMime = MediaFormat.MIMETYPE_AUDIO_AAC;
                    break;
                }*/
                case AV_CODEC_ID_MP3: {// 86017
                    audioMime = MediaFormat.MIMETYPE_AUDIO_MPEG;
                    break;
                }
                case AV_CODEC_ID_AAC: {// 86018
                    audioMime = MediaFormat.MIMETYPE_AUDIO_AAC;
                    break;
                }
                case AV_CODEC_ID_AC3: {// 86019
                    audioMime = MediaFormat.MIMETYPE_AUDIO_AC3;
                    break;
                }
                case AV_CODEC_ID_VORBIS: {// 86021
                    audioMime = MediaFormat.MIMETYPE_AUDIO_VORBIS;
                    break;
                }
                case AV_CODEC_ID_FLAC: {// 86028
                    audioMime = MediaFormat.MIMETYPE_AUDIO_FLAC;
                    audioMime = MediaFormat.MIMETYPE_AUDIO_RAW;
                    break;
                }
                case AV_CODEC_ID_QCELP: {// 86040
                    audioMime = MediaFormat.MIMETYPE_AUDIO_QCELP;
                    break;
                }
                case AV_CODEC_ID_EAC3: {// 86056
                    audioMime = MediaFormat.MIMETYPE_AUDIO_EAC3;
                    break;
                }
                case AV_CODEC_ID_AAC_LATM: {// 86065
                    audioMime = MediaFormat.MIMETYPE_AUDIO_AAC;
                    break;
                }
                case AV_CODEC_ID_OPUS: {// 86076
                    audioMime = MediaFormat.MIMETYPE_AUDIO_OPUS;
                    break;
                }
                default: {
                    break;
                }
            }
            if (TextUtils.isEmpty(audioMime)) {
                MLog.e(TAG, "initAudioMediaCodec() TextUtils.isEmpty(audioMime)");
                return false;
            }
            MLog.d(TAG, "initAudioMediaCodec() audio  mime: " + audioMime);

            long[] parameters = (long[]) valueObjectArray[0];
            int sampleRateInHz = (int) parameters[0];
            int channelCount = (int) parameters[1];
            int audioFormat = (int) parameters[2];
            // 单位: 秒
            int duration = (int) parameters[3];

            MediaUtils.sampleRateInHz = sampleRateInHz;
            MediaUtils.channelCount = channelCount;
            MediaUtils.audioFormat = audioFormat;
            mediaFormat = MediaUtils.getAudioDecoderMediaFormat();

            Object object1 = valueObjectArray[1];
            Object object2 = valueObjectArray[2];
            byte[] csd0 = null;
            byte[] csd1 = null;

            if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_MPEG)
                    || TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_RAW)
                    || TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_FLAC)
                    || TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_EAC3)) {// 无csd-0
                // csd-1

            } else if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_AAC)) {// csd-0
                if (object1 == null && object2 == null) {
                    csd0 = new byte[]{17, -112, 86, -27, 0};
                } else if (object1 != null && object2 == null) {
                    csd0 = (byte[]) object1;
                }
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
            } else if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_VORBIS)) {// csd-0
                // csd-1
                if (object1 != null && object2 != null) {
                    csd0 = (byte[]) object1;
                    csd1 = (byte[]) object2;
                } else if (object1 == null && object2 == null) {
                    /*csd0 = new byte[]{1, 118, 111, 114, 98, 105, 115, 0, 0, 0, 0, 2, 68, -84, 0,
                            0, -1,
                            -1, -1, -1, 0, 113, 2, 0, -1, -1, -1, -1, -72, 1};
                    csd1 = new byte[]{};*/
                }
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
                mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));
            } else if (TextUtils.equals(audioMime, MediaFormat.MIMETYPE_AUDIO_AC3)) {// csd-0 csd-1
                if (object1 != null && object2 != null) {
                    csd0 = (byte[]) object1;
                    csd1 = (byte[]) object2;
                } else if (object1 == null && object2 == null) {
                    csd0 = new byte[]
                            {0, 0, 0, 1, 103, 100, 0, 41, -84, 27, 26, 80, 30, 1, 19, -9, -128, -75,
                                    1, 1, 1, 64, 0, 0, -6, 64, 0, 58, -104, 56, -104, 0, 1, 48, -33,
                                    0, 0, 28, -100, 62, 49, 46, 49, 48, 0, 2, 97, -66, 0, 0, 57, 56,
                                    124, 98, 92, 62, 56, 97, 75};
                    csd1 = new byte[]{0, 0, 0, 1, 104, -6, -116, -14, 60};
                }
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
                mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));
            }

            mediaFormat.setString(MediaFormat.KEY_MIME, audioMime);
            if (duration > 0) {
                mediaFormat.setLong(MediaFormat.KEY_DURATION, duration * 1000000L);
            } else {
                // 随便设置了一个数
                mediaFormat.setLong(MediaFormat.KEY_DURATION, -9223372036854L);
            }

            // endregion
        }

        if (mAudioWrapper != null && mAudioWrapper.decoderMediaCodec != null) {
            // mAudioWrapper.decoderMediaCodec.flush();
            MLog.d(TAG, "initAudioMediaCodec() audio clear");
            mAudioWrapper.clear();
            mAudioWrapper = null;
        }

        mAudioWrapper = new AudioWrapper(TYPE_AUDIO);
        mAudioWrapper.isHandling = true;
        mAudioWrapper.render = false;
        mAudioWrapper.mime = audioMime;
        mAudioWrapper.decoderMediaFormat = mediaFormat;
        MLog.d(TAG, "initAudioMediaCodec() create MediaCodec start");
        mAudioWrapper.decoderMediaCodec =
                MediaUtils.getAudioDecoderMediaCodec(
                        mAudioWrapper.mime,
                        mAudioWrapper.decoderMediaFormat);
        MLog.d(TAG, "initAudioMediaCodec() create MediaCodec end");
        if (mAudioWrapper.decoderMediaCodec == null) {
            return false;
        }

        // mAudioWrapper.decoderMediaCodec.flush();
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
            MLog.d(TAG, "initAudioMediaCodec() audio csd-0: " + sb.toString());
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
            MLog.d(TAG, "initAudioMediaCodec() audio csd-1: " + sb.toString());
        }

        MLog.d(TAG, "initAudioMediaCodec() end");
        return true;
    }

    public boolean initVideoMediaCodec(JniObject jniObject) {
        boolean useExoPlayerForMediaFormat = true;
        if (mContext != null) {
            SharedPreferences sp =
                    mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            String whatPlayer = sp.getString(PLAYBACK_USE_PLAYER, PLAYER_FFMPEG_MEDIACODEC);
            if (!TextUtils.equals(whatPlayer, PLAYER_FFMPEG_MEDIACODEC)) {
                return false;
            }

            String use_mode = sp.getString(PLAYBACK_USE_EXOPLAYER_OR_FFMPEG, "use_exoplayer");
            if (TextUtils.equals(use_mode, "use_ffmpeg")) {
                useExoPlayerForMediaFormat = false;
            }
        }
        MLog.w(TAG, "initVideoMediaCodec() start");
        MediaFormat mediaFormat = null;
        String videoMime = null;
        if (mGetMediaFormat != null
                && mGetMediaFormat.mVideoMediaFormat != null) {
            // mime=video/hevc
            videoMime = mGetMediaFormat.mVideoMediaFormat.getString(MediaFormat.KEY_MIME);
            // http://112.17.40.12/PLTV/88888888/224/3221226758/1.m3u8
            if (TextUtils.equals(videoMime, "video/hevc")
                    || TextUtils.equals(videoMime, "video/avc")) {
                useExoPlayerForMediaFormat = false;
            }
        }
        if (useExoPlayerForMediaFormat
                && mGetMediaFormat != null
                && mGetMediaFormat.mVideoMediaFormat != null) {
            mediaFormat = mGetMediaFormat.mVideoMediaFormat;
            videoMime = mediaFormat.getString(MediaFormat.KEY_MIME);
            MLog.w(TAG, "initVideoMediaCodec() video GetMediaFormat");
            if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                int max_input_size = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                MLog.w(TAG, "initVideoMediaCodec() video max_input_size: " + max_input_size);
            }
        } else {
            // region

            if (jniObject == null
                    || jniObject.valueObjectArray == null
                    || jniObject.valueObjectArray.length < 3) {
                MLog.e(TAG, "initVideoMediaCodec() jniObject failure");
                return false;
            }

            int videoMimeType = jniObject.valueInt;
            Object[] valueObjectArray = jniObject.valueObjectArray;

            MLog.w(TAG, "initVideoMediaCodec()    mimeType: " + videoMimeType);
            switch (videoMimeType) {
                case AV_CODEC_ID_HEVC:
                    videoMime = MediaFormat.MIMETYPE_VIDEO_HEVC;
                    break;
                case AV_CODEC_ID_H264:
                    videoMime = MediaFormat.MIMETYPE_VIDEO_AVC;
                    break;
                case AV_CODEC_ID_MPEG4:
                    videoMime = MediaFormat.MIMETYPE_VIDEO_MPEG4;
                    break;
                case AV_CODEC_ID_VP8:
                    videoMime = MediaFormat.MIMETYPE_VIDEO_VP8;
                    break;
                case AV_CODEC_ID_VP9:
                    videoMime = MediaFormat.MIMETYPE_VIDEO_VP9;
                    break;
                case AV_CODEC_ID_MPEG2VIDEO:
                    videoMime = MediaFormat.MIMETYPE_VIDEO_MPEG2;
                    break;
                default:
                    break;
            }
            if (TextUtils.isEmpty(videoMime) || mSurface == null) {
                MLog.e(TAG, "initVideoMediaCodec() " +
                        "TextUtils.isEmpty(videoMime) || mSurface == null");
                return false;
            }

            long[] parameters = (long[]) valueObjectArray[0];
            int width = (int) parameters[0];
            int height = (int) parameters[1];
            // 单位: 秒
            int duration = (int) parameters[2];
            int frameRate = (int) parameters[3];
            long bit_rate = parameters[4];
            mediaFormat = MediaUtils.getVideoDecoderMediaFormat(width, height);

            Object object1 = valueObjectArray[1];
            Object object2 = valueObjectArray[2];
            byte[] csd0 = null;
            byte[] csd1 = null;

            if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_HEVC)) {// 1
                if (object1 != null && object2 == null) {
                    csd0 = (byte[]) object1;
                } else {
                    csd0 = new byte[]{0, 0, 0, 1, 64, 1, 12, 1,
                            -1, -1, 1, 96, 0, 0, 3, 0, -112,
                            0, 0, 3, 0, 0, 3, 0,
                            -103, -107, -104, 9, 0, 0, 0, 1, 66, 1, 1, 1, 96, 0, 0, 3, 0, -112, 0
                            , 0,
                            3, 0, 0, 3, 0,
                            -103, -96, 1, -32, 32, 2, 28, 89, 101, 102, -110, 76, -82, 106, 4, 36
                            , 4,
                            8, 0, 0, 31, 64,
                            0, 7, 83, 0, 64, 0, 0, 0, 1, 68, 1, -63, 114, -76, 98, 64, 0};
                }
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
            } else if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_AVC)) {// 2
                if (object1 != null && object2 != null) {
                    csd0 = (byte[]) object1;
                    csd1 = (byte[]) object2;
                    if (csd0.length == 0) {
                        csd0 = new byte[]{0, 0, 0, 1, 103, 100, 0, 40, -84,
                                -47, 0, 120, 2, 39, -27,
                                -64, 90, -128, -128, -125, 32, 0, 0,
                                3, 0, 32, 0, 0, 7, -127, -29, 6, 34, 64};
                    }
                    if (csd1.length == 0) {
                        csd1 = new byte[]{0, 0, 0, 1, 104, -21, -113, 44};
                    }
                    mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
                    mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));
                }
            } else if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_MPEG4)) {// 1
                if (object1 != null && object2 == null) {
                    csd0 = (byte[]) object1;
                } else {
                    csd0 = new byte[]{0, 0, 1, -80, 1, 0, 0, 1, -75, -119, 19, 0, 0, 1, 0, 0, 0, 1,
                            32, 0, -60, -115,
                            -120, 0, -51, 20, 4, 60, 20, 99, 0, 0, 1, -78, 76, 97, 118, 99, 53, 50,
                            46, 49, 53, 46, 48};
                }
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
            } else if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_VP8)) {// 0

            } else if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_VP9)) {// 0

            } else if (TextUtils.equals(videoMime, MediaFormat.MIMETYPE_VIDEO_MPEG2)) {// 1
                if (object1 != null && object2 == null) {
                    csd0 = (byte[]) object1;
                } else {
                    csd0 = new byte[]{0, 0, 1, -77, 120, 4, 56, 53, -1, -1,
                            -32, 24, 0, 0, 1, -75, 20, 74, 0, 1, 0, 0};
                }
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
            }

            mediaFormat.setString(MediaFormat.KEY_MIME, videoMime);
            if (duration > 0) {
                mediaFormat.setLong(MediaFormat.KEY_DURATION, duration * 1000000L);
            } else {
                // 随便设置了一个数
                mediaFormat.setLong(MediaFormat.KEY_DURATION, -9223372036854L);
            }
            // 设置帧率
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            // 设置比特率
            mediaFormat.setLong(MediaFormat.KEY_BIT_RATE, bit_rate);
            mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

            // endregion
        }

        MLog.w(TAG, "initVideoMediaCodec() video  mime: " + videoMime);
        if (mVideoWrapper != null && mVideoWrapper.decoderMediaCodec != null) {
            // mVideoWrapper.decoderMediaCodec.flush();
            MLog.w(TAG, "initVideoMediaCodec() video clear");
            mVideoWrapper.clear();
            mVideoWrapper = null;
        }

        mVideoWrapper = new VideoWrapper(TYPE_VIDEO);
        mVideoWrapper.isHandling = true;
        mVideoWrapper.render = true;
        mVideoWrapper.mime = videoMime;
        mVideoWrapper.decoderMediaFormat = mediaFormat;
        mVideoWrapper.mSurface = mSurface;
        MLog.w(TAG, "initVideoMediaCodec() create MediaCodec start");
        mVideoWrapper.decoderMediaCodec =
                MediaUtils.getVideoDecoderMediaCodec(
                        mVideoWrapper.mime,
                        mVideoWrapper.decoderMediaFormat,
                        mVideoWrapper.mSurface);
        MLog.w(TAG, "initVideoMediaCodec() create MediaCodec end");
        if (mVideoWrapper.decoderMediaCodec == null) {
            return false;
        }

        // mVideoWrapper.decoderMediaCodec.flush();
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
            MLog.w(TAG, "initVideoMediaCodec() video csd-0: " + sb.toString());
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
            MLog.w(TAG, "initVideoMediaCodec() video csd-1: " + sb.toString());
        }

        MLog.w(TAG, "initVideoMediaCodec() end");
        return true;
    }

    public boolean feedInputBufferAndDrainOutputBuffer(SimpleWrapper wrapper) {
        return EDMediaCodec.feedInputBufferAndDrainOutputBuffer(
                mCallback,
                (wrapper.type == TYPE_AUDIO
                        ? EDMediaCodec.TYPE.TYPE_AUDIO
                        : EDMediaCodec.TYPE.TYPE_VIDEO),
                wrapper.decoderMediaCodec,
                wrapper.data,
                0,
                wrapper.size,
                wrapper.sampleTime,
                wrapper.render,
                true);
    }

    // video
    private JniObject mVideoJniObject = new JniObject();
    private int[] videoValueIntArray = new int[2];
    private Object[] videoValueObjectArray = new Object[2];
    // audio
    private JniObject mAudioJniObject = new JniObject();
    private int[] audioValueIntArray = new int[2];
    private Object[] audioValueObjectArray = new Object[2];

    private EDMediaCodec.Callback mCallback = new EDMediaCodec.Callback() {
        @Override
        public boolean isVideoFinished() {
            return !mVideoWrapper.isHandling;
        }

        @Override
        public boolean isAudioFinished() {
            return !mAudioWrapper.isHandling;
        }

        @Override
        public void handleVideoOutputFormat(MediaFormat mediaFormat) {
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

             durationUs: 从Us到秒, 1 s = 1000 * 1000 Us
             */
            try {
                MediaFormat newMediaFormat = mediaFormat;
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
                    if (oldMap.containsKey("mime-old")) {
                        return;
                    }
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                MLog.w(TAG, "handleVideoOutputFormat() newMediaFormat: \n" +
                        mVideoWrapper.decoderMediaFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleAudioOutputFormat(MediaFormat mediaFormat) {
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
                MediaFormat newMediaFormat = mediaFormat;
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
                    if (oldMap.containsKey("mime-old")) {
                        return;
                    }
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                MLog.d(TAG, "handleAudioOutputFormat() newMediaFormat: \n" +
                        mAudioWrapper.decoderMediaFormat);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MediaUtils.releaseAudioTrack(mAudioWrapper.mAudioTrack);

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
            } else {
                MLog.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
                mAudioWrapper.isHandling = false;
                handleAudioOutputBuffer(-1, null, null, -1);
                return;
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
        }

        @Override
        public int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {
            if (mFFMPEG != null) {
                videoValueIntArray[0] = roomIndex;
                videoValueIntArray[1] = roomSize;
                videoValueObjectArray[0] = room;
                videoValueObjectArray[1] = roomInfo;
                mVideoJniObject.valueIntArray = videoValueIntArray;
                mVideoJniObject.valueObjectArray = videoValueObjectArray;
                return Integer.parseInt(
                        mFFMPEG.onTransact(
                                FFMPEG.DO_SOMETHING_CODE_handleVideoOutputBuffer, mVideoJniObject));
            }

            return 0;
        }

        @Override
        public int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {
            if (mFFMPEG != null) {
                audioValueIntArray[0] = roomIndex;
                audioValueIntArray[1] = roomSize;
                audioValueObjectArray[0] = room;
                audioValueObjectArray[1] = roomInfo;
                mAudioJniObject.valueIntArray = audioValueIntArray;
                mAudioJniObject.valueObjectArray = audioValueObjectArray;
                Integer.parseInt(
                        mFFMPEG.onTransact(
                                FFMPEG.DO_SOMETHING_CODE_handleAudioOutputBuffer, mAudioJniObject));
            }
            if (mAudioWrapper.isHandling
                    && mAudioWrapper.mAudioTrack != null
                    && mAudioWrapper.mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED
                    && roomSize > 0) {
                byte[] audioData = new byte[roomSize];
                room.get(audioData, 0, audioData.length);
                mAudioWrapper.mAudioTrack.write(audioData, roomInfo.offset, audioData.length);
            }

            return 0;
        }
    };

}
