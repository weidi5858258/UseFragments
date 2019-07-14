package com.weidi.usefragments.media;

import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.util.SparseArray;
import android.view.Surface;

import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.MimeTypes;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

/***
 mimeType是已知的,都是通过mimeType去得到各种对象
 */

public class MediaUtils {

    private static final String TAG =
            MediaUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    /***
     相同条件:
     mWidth = 720;
     mHeight = 1280;

     FRAME_RATE = 30;
     VIDEO_BIT_RATE = 800000;
     一分钟大概17.8MB
     FRAME_RATE = 30;
     VIDEO_BIT_RATE = 25000;
     一分钟大概12.4MB
     FRAME_RATE = 15;
     VIDEO_BIT_RATE = 800;
     一分钟大概20.8MB
     FRAME_RATE = 30;
     VIDEO_BIT_RATE = 800;
     一分钟大概15.5MB
     FRAME_RATE = 15;
     VIDEO_BIT_RATE = 800;
     一分钟大概19.1MB
     FRAME_RATE = 30;
     VIDEO_BIT_RATE = 30000;
     一分钟大概13.2MB
     FRAME_RATE = 30;
     VIDEO_BIT_RATE = 64000;
     一分钟大概15.9MB
     */
    // 想要的编码格式
    // video/avc(H264)
    public static final String VIDEO_MIME = MediaFormat.MIMETYPE_VIDEO_AVC;
    // audio/mp4a-latm(AAC)
    public static final String AUDIO_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int VIDEO_BIT_RATE = 64000;// 1200000 8000000 800000
    // audio的有关参数定义在一起了(在下面)
    // private static final int AUDIO_BIT_RATE = 64000;// 64kbps
    private static final int FRAME_RATE = 30;
    private static final int IFRAME_INTERVAL = 1;

    /***
     AC4 is not supported in AOSP and MTK added ENCODING_AC4 in AOSP as 15.
     It is supported in AOSP as API Level 28 or later.
     */
    public static final int ENCODING_AC4;

    static {
        if (Build.VERSION.SDK_INT >= 28) {
            ENCODING_AC4 = AudioFormat.ENCODING_AC4;
        } else {
            ENCODING_AC4 = AudioFormat.ENCODING_AAC_ELD;
        }
    }

    /***
     MediaCodecList ---> MediaCodecInfo --->
     CodecCapabilities,AudioCapabilities,VideoCapabilities

     看我吧
     */
    public static void lookAtMe() {
        // 第一步
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        // 第二步
        MediaCodecInfo[] mediaCodecInfos = mediaCodecList.getCodecInfos();
        if (mediaCodecInfos == null) {
            return;
        }
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            if (DEBUG)
                MLog.d(TAG,
                        "lookAtMe()===================================================");
            if (DEBUG) {
                MLog.d(TAG,
                        "lookAtMe() mediaCodecInfo.name:      " + mediaCodecInfo.getName());
                MLog.d(TAG,
                        "lookAtMe() mediaCodecInfo.isEncoder: " + mediaCodecInfo.isEncoder());
            }
            String[] types = mediaCodecInfo.getSupportedTypes();
            if (types == null) {
                continue;
            }
            // MediaCodecInfo.CodecCapabilities需要通过type得到
            for (String type : types) {
                if (TextUtils.isEmpty(type)) {
                    continue;
                }
                if (DEBUG)
                    MLog.d(TAG,
                            "lookAtMe() type: " + type);
                MediaCodecInfo.CodecCapabilities codecCapabilities =
                        mediaCodecInfo.getCapabilitiesForType(type);
                if (codecCapabilities == null) {
                    continue;
                }
                // 好像是用在TV上,对4K 60FPS的视频进行播放的一种模式
                // 支持的话才能启用相关的代码.如AudioAttributes.FLAG_HW_AV_SYNC这个flag的设置
                boolean isTunneling = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && codecCapabilities.isFeatureSupported(
                        MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback);
                if (DEBUG)
                    MLog.d(TAG,
                            "lookAtMe() isTunneling: " + isTunneling);

                MediaCodecInfo.AudioCapabilities audioCapabilities =
                        codecCapabilities.getAudioCapabilities();
                MediaCodecInfo.VideoCapabilities videoCapabilities =
                        codecCapabilities.getVideoCapabilities();
                // 然后可以通过audioCapabilities和videoCapabilities得到各自的有关信息
                if (audioCapabilities != null) {
                    if (DEBUG)
                        MLog.d(TAG,
                                "lookAtMe() 音频比特率范围: " +
                                        audioCapabilities.getBitrateRange().toString());
                    Range<Integer>[] sampleRateRanges =
                            audioCapabilities.getSupportedSampleRateRanges();
                    int sampleRateRangesCount = sampleRateRanges.length;
                    StringBuilder stringBuilder = new StringBuilder("[");
                    for (int i = 0; i < sampleRateRangesCount; i++) {
                        stringBuilder.append(sampleRateRanges[i].getLower());
                        if (i != sampleRateRangesCount - 1) {
                            stringBuilder.append(", ");
                        } else {
                            stringBuilder.append("]");
                        }
                    }
                    if (DEBUG)
                        MLog.d(TAG,
                                "lookAtMe() 音频采样率范围: " +
                                        stringBuilder.toString());
                    if (DEBUG)
                        MLog.d(TAG,
                                "lookAtMe() 音频最大输入通道数: " +
                                        audioCapabilities.getMaxInputChannelCount());
                }
                if (videoCapabilities != null) {
                    if (DEBUG)
                        MLog.d(TAG,
                                "lookAtMe() 视频比特率范围:" +
                                        videoCapabilities.getBitrateRange().toString());
                    if (DEBUG)
                        MLog.d(TAG,
                                "lookAtMe() 视频支持的宽度范围: " +
                                        videoCapabilities.getSupportedWidths().toString());
                    if (DEBUG)
                        MLog.d(TAG,
                                "lookAtMe() 视频支持的高度范围: " +
                                        videoCapabilities.getSupportedHeights().toString());
                    if (DEBUG)
                        MLog.d(TAG,
                                "lookAtMe() 视频帧率范围: " +
                                        videoCapabilities.getSupportedFrameRates().toString());
                }
            }
        }
    }

    /***
     * 最好是通过这个方法找到多个MediaCodecInfo对象,
     * 如果一个创建或者初始化失败,可以再使用下一个,这样能
     * 全部循环.如果使用getEncoderMediaCodecInfo(...)
     * 只能找到一个MediaCodecInfo对象.
     *
     *
     * Find an encoder supported specified MIME type
     * 查找支持"video/avc"这种MIME type的CodecName,
     * 然后就可以通过MediaCodec.createByCodecName(CodecName)
     * 这个方法得到一个可以MediaCodec对象.
     *
     * @param mime 如MediaFormat.MIMETYPE_VIDEO_AVC
     * @return Returns empty array if not found any encoder supported specified MIME type
     *
     * 在我的手机上找到
     * Video的CodecName(根据"video/avc"):
     * "OMX.google.h264.encoder"
     * "OMX.SEC.AVC.Encoder"
     *
     * Audio的CodecName(根据"audio/mp4a-latm"):
     * "OMX.google.aac.encoder"
     */
    public static @NonNull
    MediaCodecInfo[] findAllEncodersByMime(String mime) {
        // MediaCodecList.REGULAR_CODECS
        // MediaCodecList.ALL_CODECS
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        List<MediaCodecInfo> mediaCodecInfos = new ArrayList<MediaCodecInfo>();
        for (MediaCodecInfo mediaCodecInfo : codecList.getCodecInfos()) {
            // 过滤掉非编码器
            if (!mediaCodecInfo.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities codecCapabilities =
                        mediaCodecInfo.getCapabilitiesForType(mime);
                if (codecCapabilities == null) {
                    continue;
                }
            } catch (IllegalArgumentException e) {
                // unsupported
                continue;
            }
            mediaCodecInfos.add(mediaCodecInfo);
        }

        return mediaCodecInfos.toArray(new MediaCodecInfo[mediaCodecInfos.size()]);
    }

    public static @NonNull
    MediaCodecInfo[] findAllDecodersByMime(String mime) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        List<MediaCodecInfo> mediaCodecInfos = new ArrayList<MediaCodecInfo>();
        for (MediaCodecInfo mediaCodecInfo : codecList.getCodecInfos()) {
            // 过滤掉编码器
            if (mediaCodecInfo.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities codecCapabilities =
                        mediaCodecInfo.getCapabilitiesForType(mime);
                if (codecCapabilities == null) {
                    continue;
                }
            } catch (IllegalArgumentException e) {
                // unsupported
                continue;
            }
            mediaCodecInfos.add(mediaCodecInfo);
        }

        return mediaCodecInfos.toArray(new MediaCodecInfo[mediaCodecInfos.size()]);
    }

    /***
     *
     * @param mime "video/avc" "audio/mp4a-latm"
     * @return
     */
    public static MediaCodecInfo getEncoderMediaCodecInfo(String mime) {
        if (TextUtils.isEmpty(mime)) {
            return null;
        }
        // 找到的都是编码器
        MediaCodecInfo[] infos = findAllEncodersByMime(mime);
        if (infos == null) {
            return null;
        }
        for (MediaCodecInfo mediaCodecInfo : infos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            for (String type : mediaCodecInfo.getSupportedTypes()) {
                if (TextUtils.isEmpty(type)) {
                    continue;
                }
                if (type.equalsIgnoreCase(mime)) {
                    if (DEBUG)
                        MLog.d(TAG,
                                "getEncoderMediaCodecInfo() the selected encoder is : " +
                                        mediaCodecInfo.getName());
                    return mediaCodecInfo;
                }
            }
        }
        return null;
    }

    public static MediaCodecInfo getDecoderMediaCodecInfo(String mime) {
        if (TextUtils.isEmpty(mime)) {
            return null;
        }
        // 找到的都是解码器
        MediaCodecInfo[] infos = findAllDecodersByMime(mime);
        if (infos == null) {
            return null;
        }
        for (MediaCodecInfo mediaCodecInfo : infos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            for (String type : mediaCodecInfo.getSupportedTypes()) {
                if (TextUtils.isEmpty(type)) {
                    continue;
                }
                if (type.equalsIgnoreCase(mime)) {
                    if (DEBUG)
                        MLog.d(TAG,
                                "getEncoderMediaCodecInfo() the selected encoder is : " +
                                        mediaCodecInfo.getName());
                    return mediaCodecInfo;
                }
            }
        }
        return null;
    }

    /***
     得到用于编码的MediaCodec对象的步骤:
     1.视频部分想要编码到什么,比如这里是想要编码成“video/avc”(mime)
     然后根据这个mime去找MediaCodecInfo对象,
     如果找到的话,说明当前手机支持此mime.
     2.然后从MediaCodecInfo对象中得到name,
     再根据MediaCodec.createByCodecName(name)
     方法就能得到用于编码的MediaCodec对象.
     * @return
     */
    public static MediaCodec getVideoEncoderMediaCodec(Surface surface) {
        MediaCodec encoder = null;
        MediaCodecInfo[] mediaCodecInfos = findAllEncodersByMime(VIDEO_MIME);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            try {
                encoder = MediaCodec.createByCodecName(mediaCodecInfo.getName());
                encoder.configure(
                        getAudioEncoderMediaFormat(),
                        surface, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoder.start();
                if (DEBUG)
                    MLog.d(TAG, "getVideoEncoderMediaCodec() MediaCodec create success");
                break;
            } catch (NullPointerException
                    | IllegalArgumentException
                    | IOException e) {
                e.printStackTrace();
                encoder = null;
                continue;
            }
        }

        /***
         注意:
         编码器使用了surface作为输入源，则会选择surface模式，
         不会回调onInputBufferAvailable，
         我们同样不能手动输入数据给编码器，否则会发生错误。

         系统会首先回调onOutputFormatChanged方法，
         这个方法就是关于h264的sps和pps信息，
         这两个数据在解析h264的时候非常重要，不能丢失。
         sps和pps会存放在MediaForamt中，可以通过getByteBuffer来获取，
         他们的key分别是”csd-0”和”csd-1”，注意顺序不能取反。

         h264流屏幕在变化很小的时候产生的数据很小，
         变化很大的时候产生的数据较大，
         这些数据都是通过onOutputBufferAvailable函数回调回来。

         所有的数据都存入一个线程安全的阻塞队列(LinkedBlockingQueue)中，
         启动另一个线程不断的从这个队列中取出数据传递给jni包装的jrtplib，
         发送包装好的rtp数据给接收端。
         */
        // 使用同步方式的话,下面的代码不能执行
        /*encoder.setCallback(new MediaCodec.Callback() {

            @Override
            public void onInputBufferAvailable(
                    @NonNull MediaCodec codec,
                    int index) {
                // 这个方法在使用surface模式的时候不会回调
            }

            @Override
            public void onOutputBufferAvailable(
                    @NonNull MediaCodec codec,
                    int index,
                    @NonNull MediaCodec.BufferInfo info) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                // 处理数据

                // 释放空间
                codec.releaseOutputBuffer(index, false);
            }

            @Override
            public void onError(
                    @NonNull MediaCodec codec,
                    @NonNull MediaCodec.CodecException e) {
                if (DEBUG)
                    MLog.e(TAG, "onError()\n" + e.getDiagnosticInfo());
            }

            @Override
            public void onOutputFormatChanged(
                    @NonNull MediaCodec codec,
                    @NonNull MediaFormat format) {
                if (DEBUG)
                    MLog.d(TAG, "onOutputFormatChanged() format: " + format);
                // getSpsPpsByteBuffer(mediaFormat);
            }
        });*/

        return encoder;
    }

    public static MediaCodec getVideoEncoderMediaCodec() {
        MediaCodec encoder = null;
        MediaCodecInfo[] mediaCodecInfos = findAllEncodersByMime(VIDEO_MIME);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            try {
                encoder = MediaCodec.createByCodecName(mediaCodecInfo.getName());
                encoder.configure(
                        getAudioEncoderMediaFormat(),
                        null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoder.start();
                if (DEBUG)
                    MLog.d(TAG, "getVideoEncoderMediaCodec() MediaCodec create success");
                break;
            } catch (NullPointerException
                    | IllegalArgumentException
                    | IOException e) {
                e.printStackTrace();
                encoder = null;
                continue;
            }
        }

        return encoder;
    }

    /***
     与下面的getAudioEncoderMediaFormat()对应,因为用的mime都是AUDIO_MIME_TYPE
     @return
     */
    public static MediaCodec getAudioEncoderMediaCodec() {
        MediaCodec encoder = null;
        MediaCodecInfo[] mediaCodecInfos = findAllEncodersByMime(AUDIO_MIME);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            try {
                encoder = MediaCodec.createByCodecName(mediaCodecInfo.getName());
                encoder.configure(
                        getAudioEncoderMediaFormat(),
                        null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoder.start();
                if (DEBUG)
                    MLog.d(TAG, "getAudioEncoderMediaCodec() MediaCodec create success");
                break;
            } catch (NullPointerException
                    | IllegalArgumentException
                    | IOException e) {
                e.printStackTrace();
                encoder = null;
                continue;
            }
        }

        return encoder;
    }

    /***
     根据实际视频使用，因为mime不一定是“video/avc”
     * @return
     */
    public static MediaCodec getVideoDecoderMediaCodec() {
        MediaCodecInfo codecInfo = getEncoderMediaCodecInfo(VIDEO_MIME);
        if (codecInfo == null) {
            throw new RuntimeException(
                    "根据 \"" + VIDEO_MIME + "\" 这个mime找不到对应的MediaCodecInfo对象");
        }

        MediaCodec decoder = null;
        try {
            // decoder = MediaCodec.createByCodecName(codecInfo.getName());
            // decoder = MediaCodec.createByCodecName("OMX.google.h264.encoder");
            decoder = MediaCodec.createDecoderByType(VIDEO_MIME);
            if (DEBUG)
                MLog.d(TAG, "getVideoDecoderMediaCodec() MediaCodec create success");
            // 最后一个参数是flag,用来标记是否是编码,传入0表示作为解码.
            /*decoder.configure(
                    getMediaDecoderFormat(width, height),
                    surface,
                    null,
                    0);
            decoder.start();*/
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        /***
         onInputBufferAvailable会一直回调来让我们不断的从队列中取出数据提交给解码器。
         此处一定要注意，假如用户通过dequeuinputbuffer方法获取了缓冲的索引，
         必须调用queueinputbuffer方法来释放缓冲区，
         将缓冲区的所有权交给mediacodec，
         否则后续将不会回调onInputBufferAvailable方法。

         队列中没有数据的时候可以提交空数据给解码器

         mc.queueInputBuffer(inputBufferId, 0,0, 0, 0);
         队列中有数据的时候首先要区分一下是不是sps或者pps，
         是的话就必须当做配置信息提交给解码器，
         不是的话就直接提交一帧完整的数据给解码器。
         */
        /*decoder.setCallback(new MediaCodec.Callback() {

            @Override
            public void onInputBufferAvailable(
                    @NonNull MediaCodec codec, int index) {
                // 处理数据
            }

            @Override
            public void onOutputBufferAvailable(
                    @NonNull MediaCodec codec,
                    int index,
                    @NonNull MediaCodec.BufferInfo info) {
                // 直接释放即可
                codec.releaseOutputBuffer(index, true);
            }

            @Override
            public void onError(
                    @NonNull MediaCodec codec,
                    @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(
                    @NonNull MediaCodec codec,
                    @NonNull MediaFormat format) {

            }
        });*/

        return decoder;
    }

    public static MediaCodec getAudioDecoderMediaCodec() {
        MediaCodec decoder = null;
        MediaCodecInfo[] mediaCodecInfos = findAllDecodersByMime(AUDIO_MIME);
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            if (mediaCodecInfo == null) {
                continue;
            }
            try {
                decoder = MediaCodec.createByCodecName(mediaCodecInfo.getName());
                /*decoder.configure(
                        getAudioDecoderMediaFormat(),
                        null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                decoder.start();*/
                if (DEBUG)
                    MLog.d(TAG, "getAudioDecoderMediaCodec() MediaCodec create success");
                break;
            } catch (NullPointerException
                    | IllegalArgumentException
                    | IOException e) {
                e.printStackTrace();
                decoder = null;
                continue;
            }
        }

        return decoder;
    }

    /***
     Encoder时可以自己作主指定mime得到MediaFormat对象
     这个方法是给录屏设置的参数
     * @param width
     * @param height
     * @return
     */
    public static MediaFormat getVideoEncoderMediaFormat(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME, width, height);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
        // 必须设置为COLOR_FormatSurface，因为是用surface作为输入源
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        // 设置比特率
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
        // 设置帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // 设置抽取关键帧的间隔，以s为单位，负数或者0表示不抽取关键帧
        // i-frame iinterval
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (DEBUG)
            MLog.d(TAG, "getVideoEncoderMediaFormat() created video format: " + format);

        return format;
    }

    /***
     初始化AAC编码器
     与上面的getAudioEncoderMediaCodec()对应,因为用的mime都是AUDIO_MIME_TYPE
     channelCount为2,表示双声道,也就是
     channelConfig = AudioFormat.CHANNEL_IN_STEREO.
     @return
     */
    public static MediaFormat getAudioEncoderMediaFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(
                AUDIO_MIME, sampleRateInHz, channelCount);
        format.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(
                MediaFormat.KEY_BIT_RATE,
                AUDIO_BIT_RATE);
        format.setInteger(
                MediaFormat.KEY_CHANNEL_MASK,
                channelConfig);
        format.setInteger(
                MediaFormat.KEY_MAX_INPUT_SIZE,
                getMinBufferSize() * 2);
        if (DEBUG)
            MLog.d(TAG, "getAudioEncoderMediaFormat() created audio format: " + format);
        return format;
    }

    /***
     Decoder时一般不能自己作主指定mime得到MediaFormat对象,
     应该根据实际视频得到mime,然后得到MediaFormat对象.
     * @param width
     * @param height
     * @return
     */
    public static MediaFormat getVideoDecoderMediaFormat(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME, width, height);
        // 设置帧率
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
        // 设置比特率
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
        // 设置帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // 设置抽取关键帧的间隔，以s为单位，负数或者0表示不抽取关键帧
        // i-frame iinterval
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (DEBUG)
            MLog.d(TAG, "getVideoDecoderMediaFormat() created video format: " + format);

        return format;
    }

    public static MediaFormat getAudioDecoderMediaFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(
                AUDIO_MIME, sampleRateInHz, channelCount);
        format.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        // AAC-HE
        format.setInteger(
                MediaFormat.KEY_BIT_RATE,
                AUDIO_BIT_RATE);
        format.setInteger(
                MediaFormat.KEY_CHANNEL_MASK,
                channelConfig);
        format.setInteger(
                MediaFormat.KEY_MAX_INPUT_SIZE,
                getMinBufferSize() * 2);
        if (DEBUG)
            MLog.d(TAG, "getAudioDecoderMediaFormat() created Audio format: " + format);

        return format;
    }

    /***
     public static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
     本来AudioRecord是使用AudioFormat.CHANNEL_IN_MONO的,
     但是AudioFormat.CHANNEL_IN_MONO不能使用于AudioTrack.
     我的手机AudioTrack可以使用AudioFormat.CHANNEL_IN_STEREO.
     只有创建AudioRecord对象和AudioTrack对象的三个参数
     (sampleRateInHz,channelConfig和audioFormat)一样时,
     录制是什么声音,播放才是什么声音.
     因此选择AudioFormat.CHANNEL_IN_STEREO(双声道)作为创建
     AudioRecord对象和AudioTrack对象的参数.
     */
    // 下面的参数为了得到默认的AudioRecord对象和AudioTrack对象而定义的
    // 兼容所有Android设备
    public static final int sampleRateInHz = 44100;
    // 下面两个是对应关系,只是方法所需要的参数不一样而已
    public static final int channelCount = 2;
    // 立体声(AudioFormat.CHANNEL_IN_STEREO = AudioFormat.CHANNEL_OUT_STEREO)
    private static final int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    // 数据位宽(兼容所有Android设备)
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    // AudioRecord(录音)
    private static final int audioSource = MediaRecorder.AudioSource.MIC;
    // AudioTrack(播放)
    private static final int streamType = AudioManager.STREAM_MUSIC;
    // private static final int mode = AudioTrack.MODE_STATIC
    private static final int mode = AudioTrack.MODE_STREAM;
    public static final int sessionId = AudioManager.AUDIO_SESSION_ID_GENERATE;
    // 比特率(audioFormat = 16 / 8,所以AUDIO_BIT_RATE的值不需要再除以8)
    // 没有除以8之前的单位是bit,除以8之后的单位是byte
    public static final int AUDIO_BIT_RATE = sampleRateInHz * audioFormat * channelCount;

    /***
     AudioTrack
     int sampleRateInHz = 44100;
     int channelCount = 1;
     int channelConfig = AudioFormat.CHANNEL_IN_MONO;// 16
     int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
     int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
     java.lang.IllegalArgumentException: Unsupported channel configuration.

     int sampleRateInHz = 44100;
     int channelCount = 1;
     int channelConfig = AudioFormat.CHANNEL_OUT_MONO;// 4
     int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
     int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
     getMinBufferSize(): Invalid channel configuration.
     */

    /***
     bufferSizeInBytes
     使用了默认值
     AudioRecord内部的音频缓冲区的大小,该缓冲区的值不能低于一帧“音频帧”（Frame）的大小.
     一帧音频帧的大小计算如下:
     int size = 采样率 x 位宽 x 采样时间 x 通道数
     采样时间一般取2.5ms~120ms之间,由厂商或者具体的应用决定.

     在Android中,一帧的大小可以根据下面的公式得到,
     但是音频缓冲区的大小则必须是一帧大小的2～N倍.
     */
    public static int getMinBufferSize(int sampleRateInHz,
                                       int channelConfig,
                                       int audioFormat) {
        return AudioRecord.getMinBufferSize(
                sampleRateInHz,
                channelConfig,
                audioFormat);
    }

    public static int getMinBufferSize() {
        return AudioRecord.getMinBufferSize(
                sampleRateInHz,
                channelConfig,
                audioFormat);
    }

    /***
     @param audioSource
     MediaRecorder.AudioSource.DEFAULT = 0
     MediaRecorder.AudioSource.MIC(比较常用)
     MediaRecorder.AudioSource.VOICE_UPLINK
     MediaRecorder.AudioSource.VOICE_DOWNLINK
     MediaRecorder.AudioSource.VOICE_CALL
     MediaRecorder.AudioSource.CAMCORDER
     MediaRecorder.AudioSource.VOICE_RECOGNITION
     MediaRecorder.AudioSource.VOICE_COMMUNICATION
     MediaRecorder.AudioSource.REMOTE_SUBMIX
     MediaRecorder.AudioSource.UNPROCESSED = 9
     @param sampleRateInHz
     8000
     11025
     16000
     22050
     32000
     44100(在所有设备上都能正常工作)
     48000
     一般蓝牙耳机无法达到44100Hz的采样率,
     所有在使用蓝牙耳机录音的时候,
     设置为8000Hz或者16000Hz.
     @param channelCount
     声道数
     @param audioFormat
     AudioFormat.ENCODING_PCM_16BIT(比较常用)
     AudioFormat.ENCODING_PCM_8BIT
     AudioFormat.ENCODING_PCM_FLOAT
     */
    public static AudioRecord createAudioRecord(
            int audioSource, int sampleRateInHz,
            int channelCount, int audioFormat) {
        /***
         channelConfig
         AudioFormat.CHANNEL_IN_STEREO
         AudioFormat.CHANNEL_IN_MONO(在所有设备上都能正常工作)
         */
        int channelConfig = AudioFormat.CHANNEL_IN_DEFAULT;
        switch (channelCount) {
            case 1:
                // 如果是单声道的话还不能确定是哪个值
                channelConfig = AudioFormat.CHANNEL_IN_MONO;// 16
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;// 4
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;// 12
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;// 12
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;// 12
                break;
            default:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
        }
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat);
        if (DEBUG)
            MLog.d(TAG, "createAudioRecord() bufferSizeInBytes: " + bufferSizeInBytes);
        if (bufferSizeInBytes <= 0) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        bufferSizeInBytes *= 2;
        AudioRecord audioRecord = null;
        try {
            audioRecord = new AudioRecord(
                    audioSource,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    bufferSizeInBytes);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioRecord(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        return audioRecord;
    }

    /***
     一般使用这个就可以了(调用这个方法前必须先动态取得相应权限).
     <uses-permission android:name="android.permission.RECORD_AUDIO" />

     // 使用时需要开启子线程
     byte[] buffer = new byte[bufferSizeInBytes];
     // 录制的声音存放于buffer中
     int ret = audioRecord.read(buffer, 0, bufferSizeInBytes);
     if (ret == AudioRecord.ERROR_INVALID_OPERATION) {
     if (DEBUG)
     MLog.e(TAG, "AudioRecord.ERROR_INVALID_OPERATION");
     } else if (ret == AudioRecord.ERROR_BAD_VALUE) {
     if (DEBUG)
     MLog.e(TAG, "AudioRecord.ERROR_BAD_VALUE");
     } else {
     // do something
     }
     */
    public static AudioRecord createAudioRecord() {
        if (DEBUG)
            MLog.d(TAG, "createAudioRecord() start");
        int bufferSizeInBytes = getMinBufferSize();
        if (DEBUG)
            MLog.d(TAG, "createAudioRecord() bufferSizeInBytes: " + bufferSizeInBytes);
        if (bufferSizeInBytes <= 0) {
            //if (bufferSizeInBytes <= AudioRecord.ERROR_BAD_VALUE) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        bufferSizeInBytes *= 2;
        AudioRecord audioRecord = null;
        try {
            audioRecord = new AudioRecord(
                    audioSource,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    bufferSizeInBytes);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        // 此判断很关键
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioRecord(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            try {
                audioRecord.release();
            } finally {
                audioRecord = null;
            }
            return null;
        }

        if (DEBUG)
            MLog.d(TAG, "createAudioRecord() end");
        return audioRecord;
    }

    public static void stopMediaCodec(MediaCodec mediaCodec) {
        if (mediaCodec == null) {
            return;
        }
        try {
            mediaCodec.stop();
        } catch (Exception e) {
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    public static void releaseMediaCodec(MediaCodec mediaCodec) {
        stopMediaCodec(mediaCodec);
        if (mediaCodec == null) {
            return;
        }
        try {
            mediaCodec.release();
            mediaCodec = null;
        } catch (Exception e) {
            mediaCodec = null;
        }
    }

    public static void stopAudioRecord(AudioRecord audioRecord) {
        if (audioRecord == null) {
            return;
        }
        int recordingState = audioRecord.getRecordingState();
        if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                audioRecord.stop();
            } catch (Exception e) {
                audioRecord.release();
                audioRecord = null;
            }
        }
    }

    public static void releaseAudioRecord(AudioRecord audioRecord) {
        stopAudioRecord(audioRecord);
        if (audioRecord == null) {
            return;
        }
        try {
            audioRecord.release();
            audioRecord = null;
        } catch (Exception e) {
            audioRecord = null;
        }
    }

    /***
     使用AudioTrack播放的音频必须是解码后的PCM数据
     @param streamType AudioManager.STREAM_MUSIC
     @param sampleRateInHz 44100
     @param channelCount 声道数
     @param audioFormat AudioFormat.ENCODING_PCM_16BIT
     @param mode AudioTrack.MODE_STREAM or AudioTrack.MODE_STATIC
     @param sessionId AudioManager.AUDIO_SESSION_ID_GENERATE
     @return
     */
    public static AudioTrack createAudioTrack(
            int streamType,
            int sampleRateInHz, int channelCount,
            int audioFormat, int mode, int sessionId) {
        boolean isInputPcm = isEncodingLinearPcm(audioFormat);
        int channelConfig = getChannelConfig(channelCount, isInputPcm);
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat);
        if (bufferSizeInBytes <= 0) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() bufferSizeInBytes: " + bufferSizeInBytes);
        bufferSizeInBytes *= 2;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(sampleRateInHz)
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .build();
        AudioTrack audioTrack = null;
        try {
            audioTrack = new AudioTrack(
                    attributes,
                    format,
                    bufferSizeInBytes,
                    mode,
                    sessionId);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            audioTrack.release();
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioTrack(%d, %d, %d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat, mode, sessionId));
            return null;
        }

        return audioTrack;
    }

    public static AudioTrack createAudioTrack(
            int streamType,
            int sampleRateInHz, int channelCount,
            int audioFormat, int mode) {
        if (DEBUG)
            MLog.d(TAG, "createAudioTrack(...) start");
        // 在我的手机上使用AudioFormat.CHANNEL_OUT_MONO创建不了AudioTrack
        boolean isInputPcm = isEncodingLinearPcm(audioFormat);
        int channelConfig = getChannelConfig(channelCount, isInputPcm);
        int bufferSizeInBytes = getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat);
        if (DEBUG)
            MLog.d(TAG, "createAudioTrack(...) bufferSizeInBytes: " + bufferSizeInBytes);
        if (bufferSizeInBytes <= 0) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        bufferSizeInBytes *= 2;
        AudioTrack audioTrack = null;
        // java.lang.IllegalArgumentException: Unsupported channel configuration.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setSampleRate(sampleRateInHz)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build();
                audioTrack = new AudioTrack(
                        attributes,
                        format,
                        bufferSizeInBytes,
                        mode,
                        sessionId);
            } else {
                audioTrack = new AudioTrack(
                        streamType,
                        sampleRateInHz,
                        channelConfig,
                        audioFormat,
                        bufferSizeInBytes,
                        mode);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioTrack(%d, %d, %d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat, mode, sessionId));
            try {
                audioTrack.release();
            } finally {
                audioTrack = null;
            }
            return null;
        }

        if (DEBUG)
            MLog.d(TAG, "createAudioTrack(...) end");
        return audioTrack;
    }

    public static AudioTrack createAudioTrack(int sessionId) {
        if (sessionId < 0) {
            return null;
        }
        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() start");
        int bufferSizeInBytes = getMinBufferSize();
        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() bufferSizeInBytes: " + bufferSizeInBytes);
        if (bufferSizeInBytes <= 0) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        bufferSizeInBytes *= 2;
        AudioTrack audioTrack = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        // 一使用这个flag就出错
                        // .setFlags(AudioAttributes.FLAG_HW_AV_SYNC)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setSampleRate(sampleRateInHz)
                        // 很关键的一个参数
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build();
                audioTrack = new AudioTrack(
                        attributes,
                        format,
                        bufferSizeInBytes,
                        mode,
                        sessionId);
            } else {
                audioTrack = new AudioTrack(
                        streamType,
                        sampleRateInHz,
                        channelConfig,
                        audioFormat,
                        bufferSizeInBytes,
                        mode);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioTrack(%d, %d, %d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat, mode, sessionId));
            try {
                audioTrack.release();
            } finally {
                audioTrack = null;
            }
            return null;
        }

        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() end");
        return audioTrack;
    }

    public static AudioTrack createAudioTrack() {
        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() start");
        int bufferSizeInBytes = getMinBufferSize();
        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() bufferSizeInBytes: " + bufferSizeInBytes);
        if (bufferSizeInBytes <= 0) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        bufferSizeInBytes *= 2;
        AudioTrack audioTrack = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setSampleRate(sampleRateInHz)
                        // 很关键的一个参数
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build();
                audioTrack = new AudioTrack(
                        attributes,
                        format,
                        bufferSizeInBytes,
                        mode,
                        sessionId);
            } else {
                audioTrack = new AudioTrack(
                        streamType,
                        sampleRateInHz,
                        channelConfig,
                        audioFormat,
                        bufferSizeInBytes,
                        mode);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            if (DEBUG)
                MLog.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioTrack(%d, %d, %d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat, mode, sessionId));
            try {
                audioTrack.release();
            } finally {
                audioTrack = null;
            }
            return null;
        }

        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() end");
        return audioTrack;
    }

    public static void pauseAudioTrack(AudioTrack audioTrack) {
        if (audioTrack == null) {
            return;
        }
        int playState = audioTrack.getPlayState();
        if (playState == AudioTrack.PLAYSTATE_PLAYING) {
            try {
                audioTrack.pause();
            } catch (Exception e) {
                audioTrack.release();
                audioTrack = null;
            }
        }
    }

    public static void stopAudioTrack(AudioTrack audioTrack) {
        pauseAudioTrack(audioTrack);
        if (audioTrack == null) {
            return;
        }
        int playState = audioTrack.getPlayState();
        if (playState == AudioTrack.PLAYSTATE_PAUSED) {
            try {
                audioTrack.stop();
            } catch (Exception e) {
                audioTrack.release();
                audioTrack = null;
            }
        }
    }

    public static void releaseAudioTrack(AudioTrack audioTrack) {
        stopAudioTrack(audioTrack);
        if (audioTrack == null) {
            return;
        }
        try {
            audioTrack.release();
            audioTrack = null;
        } catch (Exception e) {
            audioTrack = null;
        }
    }

    private static boolean isEncodingLinearPcm(int encoding) {
        return encoding == AudioFormat.ENCODING_PCM_8BIT
                || encoding == AudioFormat.ENCODING_PCM_16BIT
                || encoding == 0x80000000
                || encoding == 0x40000000
                || encoding == AudioFormat.ENCODING_PCM_FLOAT;
    }

    private static int getChannelConfig(int channelCount, boolean isInputPcm) {
        if (Build.VERSION.SDK_INT <= 28 && !isInputPcm) {
            // In passthrough mode the channel count used to configure the audio track doesn't
            // affect how
            // the stream is handled, except that some devices do overly-strict channel
            // configuration
            // checks. Therefore we override the channel count so that a known-working channel
            // configuration is chosen in all cases. See [Internal: b/29116190].
            if (channelCount == 7) {
                channelCount = 8;
            } else if (channelCount == 3 || channelCount == 4 || channelCount == 5) {
                channelCount = 6;
            }
        }

        // Workaround for Nexus Player not reporting support for mono passthrough.
        // (See [Internal: b/34268671].)
        if (Build.VERSION.SDK_INT <= 26
                && "fugu".equals(Build.DEVICE)
                && !isInputPcm
                && channelCount == 1) {
            channelCount = 2;
        }

        return getAudioTrackChannelConfig(channelCount);
    }

    private static int getAudioTrackChannelConfig(int channelCount) {
        switch (channelCount) {
            case 1:
                return AudioFormat.CHANNEL_OUT_MONO;
            case 2:
                return AudioFormat.CHANNEL_OUT_STEREO;
            case 3:
                return AudioFormat.CHANNEL_OUT_STEREO
                        | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
            case 4:
                return AudioFormat.CHANNEL_OUT_QUAD;
            case 5:
                return AudioFormat.CHANNEL_OUT_QUAD
                        | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
            case 6:
                return AudioFormat.CHANNEL_OUT_5POINT1;
            case 7:
                return AudioFormat.CHANNEL_OUT_5POINT1
                        | AudioFormat.CHANNEL_OUT_BACK_CENTER;
            case 8:
                if (Build.VERSION.SDK_INT >= 23) {
                    return AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;
                } else if (Build.VERSION.SDK_INT >= 21) {
                    // Equal to AudioFormat.CHANNEL_OUT_7POINT1_SURROUND,
                    // which is hidden before Android M.
                    return AudioFormat.CHANNEL_OUT_5POINT1
                            | AudioFormat.CHANNEL_OUT_SIDE_LEFT
                            | AudioFormat.CHANNEL_OUT_SIDE_RIGHT;
                } else {
                    // 8 ch output is not supported before Android L.
                    return AudioFormat.CHANNEL_INVALID;
                }
            default:
                return AudioFormat.CHANNEL_INVALID;
        }
    }

    private static int decideChannelConfig(
            final int channelCount, final boolean passthrough, String mimeType) {
        int channelConfig;
        switch (channelCount) {
            case 1:
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            case 3:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO | AudioFormat
                        .CHANNEL_OUT_FRONT_CENTER;
                break;
            case 4:
                channelConfig = AudioFormat.CHANNEL_OUT_QUAD;
                break;
            case 5:
                if (mimeType.equals(MimeTypes.AUDIO_AC3)
                        || mimeType.equals(MimeTypes.AUDIO_E_AC3)
                        || mimeType.equals(MimeTypes.AUDIO_AC4)
                        || mimeType.equals(MimeTypes.AUDIO_E_AC3_JOC)
                        || mimeType.equals(MimeTypes.AUDIO_AC4_JOC)) {
                    channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                } else {
                    channelConfig = AudioFormat.CHANNEL_OUT_QUAD
                            | AudioFormat.CHANNEL_OUT_FRONT_CENTER;
                }
                break;
            case 6:
                channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                break;
            case 7:
                channelConfig = AudioFormat.CHANNEL_OUT_5POINT1
                        | AudioFormat.CHANNEL_OUT_BACK_CENTER;
                break;
            case 8:
                channelConfig = Build.VERSION.SDK_INT < 23
                        ? AudioFormat.CHANNEL_OUT_7POINT1
                        : AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;
                break;
            default:
                throw new IllegalArgumentException("Unsupported channel count: " + channelCount);
        }
        // Workaround for overly strict channel configuration checks on nVidia Shield.
        if (Build.VERSION.SDK_INT <= 23
                && "foster".equals(Build.DEVICE)
                && "NVIDIA".equals(Build.MANUFACTURER)) {
            switch (channelCount) {
                case 7:
                    channelConfig = Build.VERSION.SDK_INT < 23
                            ? AudioFormat.CHANNEL_OUT_7POINT1
                            : AudioFormat.CHANNEL_OUT_7POINT1_SURROUND;
                    break;
                case 3:
                case 5:
                    channelConfig = AudioFormat.CHANNEL_OUT_5POINT1;
                    break;
                default:
                    break;
            }
        }

        // Workaround for Nexus Player not reporting support for mono passthrough.
        // (See [Internal: b/34268671].)
        if (Build.VERSION.SDK_INT <= 25
                && "fugu".equals(Build.DEVICE)
                && passthrough
                && channelCount == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        }
        return channelConfig;
    }

    public static void printMediaCodecInfos(MediaCodecInfo[] mediaCodecInfos,
                                            String mimeType) {
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            MediaCodecInfo.CodecCapabilities codecCapabilities =
                    mediaCodecInfo.getCapabilitiesForType(mimeType);
            MediaCodecInfo.VideoCapabilities videoCapabilities =
                    codecCapabilities.getVideoCapabilities();
            MediaCodecInfo.AudioCapabilities audioCapabilities =
                    codecCapabilities.getAudioCapabilities();
            /***
             videoCapabilities不为null时,audioCapabilities为null
             videoCapabilities为null时,audioCapabilities不为null
             mediaCodecInfo.getName(): OMX.MTK.VIDEO.ENCODER.AVC
             mediaCodecInfo.getSupportedTypes(): [video/avc]
             videoCapabilities.getSupportedWidths(): [128, 1920]
             videoCapabilities.getSupportedHeights(): [96, 1072]
             videoCapabilities.getSupportedFrameRates(): [0, 960]
             videoCapabilities.getBitrateRange(): [1, 14000000]
             videoCapabilities.profileLevels:
             codecCapabilities.colorFormats:

             audioCapabilities.getSupportedSampleRates():
             [8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000]
             audioCapabilities.getBitrateRange(): [8000, 510000]
             audioCapabilities.getMaxInputChannelCount(): 6(声道)
             */
            StringBuilder builder = new StringBuilder(512);
            builder.append("Encoder '").append(mediaCodecInfo.getName()).append('\'')
                    .append("\n  supported : ")
                    .append(Arrays.toString(mediaCodecInfo.getSupportedTypes()));
            if (videoCapabilities != null) {
                builder.append("\n  Video capabilities:")
                        .append("\n  Widths: ").append(videoCapabilities.getSupportedWidths())
                        .append("\n  Heights: ").append(videoCapabilities.getSupportedHeights())
                        .append("\n  Frame Rates: ").append(videoCapabilities
                        .getSupportedFrameRates())
                        .append("\n  Bitrate: ").append(videoCapabilities.getBitrateRange());
                if (VIDEO_MIME.equals(mimeType)) {
                    MediaCodecInfo.CodecProfileLevel[] levels = codecCapabilities.profileLevels;

                    builder.append("\n  Profile-levels: ");
                    /*for (MediaCodecInfo.CodecProfileLevel level : levels) {
                        builder.append("\n  ").append(Utils.avcProfileLevelToString(level));
                    }*/
                }
                builder.append("\n  Color-formats: ");
                for (int c : codecCapabilities.colorFormats) {
                    builder.append("\n  ").append(printColorFormat(c));
                }
            }

            if (audioCapabilities != null) {
                builder.append("\n Audio capabilities:")
                        .append("\n Sample Rates: ").append(Arrays.toString(audioCapabilities
                        .getSupportedSampleRates()))
                        .append("\n Bit Rates: ").append(audioCapabilities.getBitrateRange())
                        .append("\n Max channels: ").append(audioCapabilities
                        .getMaxInputChannelCount());
            }
            MLog.i(TAG, builder.toString());
        }
    }

    /***
     *
     * @param colorFormat codecCapabilities.colorFormats
     * @return 0x7f000200
    COLOR_FormatSurface
    0x7f000200
    COLOR_FormatYUV420Flexible
    COLOR_FormatYUV420Planar
    COLOR_Format16bitRGB565
    COLOR_Format24bitRGB888
    COLOR_Format32bitARGB8888
    0x7f000300
    COLOR_Format32bitBGRA8888
     */
    public static String printColorFormat(int colorFormat) {
        SparseArray<String> colorFormats = new SparseArray<>();
        colorFormats.clear();
        initColorFormatFields(colorFormats);

        int index = colorFormats.indexOfKey(colorFormat);
        if (index >= 0) {
            return colorFormats.valueAt(index);
        }
        return "0x" + Integer.toHexString(colorFormat);
    }

    private static void initColorFormatFields(SparseArray<String> colorFormats) {
        // COLOR_
        Field[] fields = MediaCodecInfo.CodecCapabilities.class.getFields();
        for (Field field : fields) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == 0) {
                continue;
            }
            String name = field.getName();
            if (!TextUtils.isEmpty(name) && name.startsWith("COLOR_")) {
                try {
                    int value = field.getInt(null);
                    colorFormats.put(value, name);
                } catch (IllegalAccessException e) {
                    // ignored
                }
            }
        }

    }

    public static final String VIDEO_SIZE_360 = "Size360";
    public static final String VIDEO_SIZE_720 = "Size720";
    public static final String VIDEO_SIZE_1080 = "Size1080";
    public static final String VIDEO_QUALITY_HIGH = "High";
    public static final String VIDEO_QUALITY_MIDDLE = "Middle";
    public static final String VIDEO_QUALITY_LOW = "Low";

    public static int getBitRate(String defaultVideoSize) {
        int bitRate = 10 * 1000 * 1000;
        String keyVideoSize = null;
        String quality = null;
        if (TextUtils.equals(keyVideoSize, VIDEO_SIZE_1080)) {
            if (TextUtils.equals(quality, VIDEO_QUALITY_HIGH)) {
                bitRate = 10000000;
            } else if (TextUtils.equals(quality, VIDEO_QUALITY_MIDDLE)) {
                bitRate = 3300000;
            } else if (TextUtils.equals(quality, VIDEO_QUALITY_LOW)) {
                bitRate = 1250000;
            }
        } else if (TextUtils.equals(keyVideoSize, VIDEO_SIZE_720)) {
            if (TextUtils.equals(quality, VIDEO_QUALITY_HIGH)) {
                bitRate = 8000000;
            } else if (TextUtils.equals(quality, VIDEO_QUALITY_MIDDLE)) {
                bitRate = 2700000;
            } else if (TextUtils.equals(quality, VIDEO_QUALITY_LOW)) {
                bitRate = 1000000;
            }
        } else if (TextUtils.equals(keyVideoSize, VIDEO_SIZE_360)) {
            if (TextUtils.equals(quality, VIDEO_QUALITY_HIGH)) {
                bitRate = 1200000;
            } else if (TextUtils.equals(quality, VIDEO_QUALITY_MIDDLE)) {
                bitRate = 400000;
            } else if (TextUtils.equals(quality, VIDEO_QUALITY_LOW)) {
                bitRate = 150000;
            }
        }
        return bitRate;
    }

    /***
     添加ADTS头到一帧的音频数据上
     freqIdx和chanCfg需要根据实际的采样率和声道数决定

     AAC的数据如果需要直接播放,
     则需要添加ADT头部,
     在每一帧AAC数据的前面添加ADTS,
     如果要将AAC合入到MP4中则不需要添加ADT头.
     下面的值在PCM编码成AAC,添加ADTS头时需要参照.
     int profile = 2;  // AAC LC
     int freqIdx = 11; // 8KHz
     int chanCfg = 1;  // CPE
     意思: AAC的格式对应2,8K的采样率对应参数是11,单声道是1
     采样率freqIdx参数：
     0: 96000 Hz
     1: 88200 Hz
     2: 64000 Hz
     3: 48000 Hz
     4: 44100 Hz
     5: 32000 Hz
     6: 24000 Hz
     7: 22050 Hz
     8: 16000 Hz
     9: 12000 Hz
     10: 11025 Hz
     11: 8000 Hz
     12: 7350 Hz
     13: Reserved
     14: Reserved
     15: frequency is written explictly
     声道数chanCfg参数： 
     0: Defined in AOT Specifc Config
     1: 1 channel: front-center
     2: 2 channels: front-left, front-right
     3: 3 channels: front-center, front-left, front-right
     4: 4 channels: front-center, front-left, front-right, back-center
     5: 5 channels: front-center, front-left, front-right, back-left, back-right
     6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
     7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left,
     back-right, LFE-channel
     8-15: Reserved

     @param packet        byte数组,长度为(7 + 编码后的aac数据长度)
     @param packetLength (7 + 编码后的aac数据长度)
     */
    public static void addADTStoFrame(byte[] packet, int packetLength) {
        int audioObjectType = 2; // AAC LC
        // 如果上面的sampleRateInHz,channelConfig改变的话,下面的值也要相应的改变
        int sampleRateIndex = 4; // 44.1KHz
        int channelConfig = 2; // CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        // 解决ios不能播放问题
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((audioObjectType - 1) << 6) + (sampleRateIndex << 2) + (channelConfig >> 2));
        packet[3] = (byte) (((channelConfig & 3) << 6) + (packetLength >> 11));
        packet[4] = (byte) ((packetLength & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLength & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;

        /***
         不变的量: packet[0] packet[1] packet[2] packet[3] packet[6]
         packet[0]: -1 packet[1]: -15 packet[2]: 80 packet[3]: -128 packet[4]: 64 packet[5]: 31 packet[6]: -4
         packet[0]: -1 packet[1]: -15 packet[2]: 80 packet[3]: -128 packet[4]: 64 packet[5]: 95 packet[6]: -4
         packet[0]: -1 packet[1]: -15 packet[2]: 80 packet[3]: -128 packet[4]: 63 packet[5]: -97 packet[6]: -4
         packet[0]: -1 packet[1]: -15 packet[2]: 80 packet[3]: -128 packet[4]: 66 packet[5]: -33 packet[6]: -4
         */
        /*MLog.d(TAG, "addADTStoFrame() " +
                " packet[0]: " + packet[0] +
                " packet[1]: " + packet[1] +
                " packet[2]: " + packet[2] +
                " packet[3]: " + packet[3] +
                " packet[4]: " + packet[4] +
                " packet[5]: " + packet[5] +
                " packet[6]: " + packet[6]);*/
    }

    public static byte[] buildAacAudioSpecificConfig() {
        // 对应addADTStoFrame方法中的相同参数
        int audioObjectType = 2;
        int sampleRateIndex = 4;
        int channelConfig = 2;

        byte[] specificConfig = new byte[2];
        specificConfig[0] = (byte) (((audioObjectType << 3) & 0xF8) | ((sampleRateIndex >> 1) & 0x07));
        specificConfig[1] = (byte) (((sampleRateIndex << 7) & 0x80) | ((channelConfig << 3) & 0x78));
        return specificConfig;
    }

    public static void setCsdBuffers(MediaFormat format, List<byte[]> csdBuffers) {
        for (int i = 0; i < csdBuffers.size(); i++) {
            format.setByteBuffer("csd-" + i, ByteBuffer.wrap(csdBuffers.get(i)));
        }
    }

    /***
     加入wav文件头
     */
    public static void addWaveHeaderToPcmFile(FileOutputStream wavOS,
                                              long pcmSize,
                                              long sampleRateInHz,
                                              int channelCount,
                                              long bitRate) throws IOException {
        long wavSize = pcmSize + 36;
        byte[] header = new byte[44];
        header[0] = 'R';// RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (wavSize & 0xff);
        header[5] = (byte) ((wavSize >> 8) & 0xff);
        header[6] = (byte) ((wavSize >> 16) & 0xff);
        header[7] = (byte) ((wavSize >> 24) & 0xff);
        header[8] = 'W';// WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';// 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;// 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;// format = 1
        header[21] = 0;
        header[22] = (byte) channelCount;
        header[23] = 0;
        header[24] = (byte) (sampleRateInHz & 0xff);
        header[25] = (byte) ((sampleRateInHz >> 8) & 0xff);
        header[26] = (byte) ((sampleRateInHz >> 16) & 0xff);
        header[27] = (byte) ((sampleRateInHz >> 24) & 0xff);
        header[28] = (byte) (bitRate & 0xff);
        header[29] = (byte) ((bitRate >> 8) & 0xff);
        header[30] = (byte) ((bitRate >> 16) & 0xff);
        header[31] = (byte) ((bitRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16;// bits per sample
        header[35] = 0;
        header[36] = 'd';// data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (pcmSize & 0xff);
        header[41] = (byte) ((pcmSize >> 8) & 0xff);
        header[42] = (byte) ((pcmSize >> 16) & 0xff);
        header[43] = (byte) ((pcmSize >> 24) & 0xff);
        wavOS.write(header, 0, 44);
    }

    /***
     使用MediaMetadataRetriever获取视频的第一帧作为缩略图
     //获取第一帧原尺寸图片
     mmrc.getFrameAtTime();
     //获取指定位置的原尺寸图片 注意这里传的timeUs是微秒
     mmrc.getFrameAtTime(timeUs, option);
     //获取指定位置指定宽高的缩略图
     mmrc.getScaledFrameAtTime(
     timeUs, MediaMetadataRetrieverCompat.OPTION_CLOSEST, width, height);
     //获取指定位置指定宽高并且旋转的缩略图
     mmrc.getScaledFrameAtTime(
     timeUs, MediaMetadataRetrieverCompat.OPTION_CLOSEST, width, height, rotate);
     OPTION_CLOSEST    在给定的时间，检索最近一个帧,这个帧不一定是关键帧。
     OPTION_CLOSEST_SYNC    在给定的时间，检索最近一个同步与数据源相关联的的帧（关键帧）。
     OPTION_NEXT_SYNC  在给定时间之后检索一个同步与数据源相关联的关键帧。
     OPTION_PREVIOUS_SYNC   顾名思义，同上
     */
    private static Bitmap getVideoThumb(String path) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(path);
        return media.getFrameAtTime();
    }

    /***
     * 使用ThumbnailUtils获取视频的第一帧作为缩略图
     * 获取视频的缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     * @param videoPath 视频的路径
     * @param width 指定输出视频缩略图的宽度
     * @param height 指定输出视频缩略图的高度度
     * @param kind 参照MediaStore.Images(Video).Thumbnails类中的常量MINI_KIND和MICRO_KIND。
     *            其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
     * @return 指定大小的视频缩略图
     */
    private static Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        //調用ThumbnailUtils類的靜態方法createVideoThumbnail獲取視頻的截圖；
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        if (bitmap != null) {
            //調用ThumbnailUtils類的靜態方法extractThumbnail將原圖片（即上方截取的圖片）轉化為指定大小；
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        return bitmap;
    }

    /***
     android硬编出来的第一帧数据就是sps和pps
     SPS(Sequence Parameter Set：序列参数集)
     PPS(Picture  Parameter Set：图像参数集)
     sps: 0, 0, 0, 1, 39, 66, -32, 30, -115, 104, 10, 3, -38, 108, -128,
     0, 0, 3, 0, -128, 0, 0, 12, -121, -118, 17, 80
     pps: 0, 0, 0, 1, 40, -50, 4, 73, 32
     0, 0, 0, 1是startcode
     获取到了sps和pps存起来碰到关键帧给他加上去就好了
     */
    private void onGetYuvFrame(MediaCodec encoder, byte[] yuvData) {
        // feed the encoder with yuv frame, got the encoded 264 es stream.
        ByteBuffer room = null;
        byte[] mediaHead = null;
        byte[] streamData = null;

        int roomIndex = encoder.dequeueInputBuffer(-1);
        if (roomIndex >= 0) {
            room = encoder.getInputBuffer(roomIndex);
            room.clear();
            room.put(yuvData, 0, yuvData.length);
            long presentationTimeUs = System.nanoTime() / 1000;
            encoder.queueInputBuffer(
                    roomIndex, 0, yuvData.length, presentationTimeUs, 0);
        }

        for (; ; ) {
            MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
            roomIndex = encoder.dequeueOutputBuffer(roomInfo, 0);
            if (roomIndex >= 0) {
                room = encoder.getOutputBuffer(roomIndex);
                int roomSize = roomInfo.size;
                byte[] encodedData = new byte[roomSize];
                room.get(encodedData);

                if (mediaHead == null) {
                    streamData = new byte[roomSize];
                } else {
                    streamData = new byte[roomSize + mediaHead.length];
                }
                if (mediaHead == null) {
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(encodedData);
                    if (spsPpsBuffer.getInt() == 0x00000001) {
                        mediaHead = new byte[roomSize];
                        System.arraycopy(
                                encodedData, 0, mediaHead, 0, roomSize);
                    } else {
                        Log.e(TAG, "not found media head.");
                    }
                }

                if (mediaHead != null
                        // 判断该帧是否是关键帧(关键帧的数组长度比一般帧要长很多)
                        && ((encodedData[4] == 0x65) || (encodedData[4] == 0x25))) { // key
                    // frame
                    // 编码器生成关键帧时只有
                    // 00 00 00
                    // 01 65
                    // 没有pps sps 要加上
                    Log.i("Ifarme", "关键帧长度=" + encodedData.length + "*****"
                            + encodedData[4] + "**outByte长度**" + streamData.length
                            + "****" + mediaHead.length);
                    System.arraycopy(
                            mediaHead, 0, streamData, 0, mediaHead.length);
                    System.arraycopy(
                            encodedData, 0, streamData, mediaHead.length, encodedData.length);
                    if (streamData.length > 0) {
                        /*int writeStreamVideoData =
                                myNative.peekInstance().WriteStreamVideoData(
                                        streamData, streamData.length, 1);*/
                    }
                } else {
                    streamData = encodedData;
                    if (streamData.length > 0) {
                        /*int writeStreamVideoData =
                                myNative.peekInstance().WriteStreamVideoData(
                                        streamData, streamData.length, 0);*/
                    }
                }
                encoder.releaseOutputBuffer(roomIndex, false);
            } else {
                break;
            }
        }
    }

    /***
     * 从socket读byte数组
     *
     * @param in
     * @param length
     * @return
     */
    public static byte[] readBytes(InputStream in, long length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        while (read < length) {
            int cur = 0;
            try {
                cur = in.read(buffer, 0, (int) Math.min(1024, length - read));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (cur < 0) {
                break;
            }
            read += cur;
            baos.write(buffer, 0, cur);
        }
        return baos.toByteArray();
    }

    private void prepareEncoder() throws IOException {

        //MediaFormat这个类是用来定义视频格式相关信息的
        //video/avc,这里的avc是高级视频编码Advanced Video Coding
        //mWidth和mHeight是视频的尺寸，这个尺寸不能超过视频采集时采集到的尺寸，否则会直接crash
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME, 1, 1);
        //COLOR_FormatSurface这里表明数据将是一个graphicbuffer元数据
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //设置码率，通常码率越高，视频越清晰，但是对应的视频也越大，这个值我默认设置成了2000000，也就是通常所说的2M
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
        //设置帧率，通常这个值越高，视频会显得越流畅，一般默认我设置成30，你最低可以设置成24，不要低于这个值，低于24会明显卡顿
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        //IFRAME_INTERVAL是指的帧间隔，这是个很有意思的值，它指的是，关键帧的间隔时间。通常情况下，你设置成多少问题都不大。
        //比如你设置成10，那就是10秒一个关键帧。但是，如果你有需求要做视频的预览，那你最好设置成1
        //因为如果你设置成10，那你会发现，10秒内的预览都是一个截图
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        Log.d(TAG, "created video format: " + format);
        //创建一个MediaCodec的实例
        //mEncoder = MediaCodec.createEncoderByType(VIDEO_MIME);
        //定义这个实例的格式，也就是上面我们定义的format，其他参数不用过于关注
        //mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //这一步非常关键，它设置的，是MediaCodec的编码源，也就是说，我要告诉mEncoder，你给我解码哪些流。
        //很出乎大家的意料，MediaCodec并没有要求我们传一个流文件进去，而是要求我们指定一个surface
        //而这个surface，其实就是我们在上一讲MediaProjection中用来展示屏幕采集数据的surface
        //mSurface = mEncoder.createInputSurface();
    }

}

/***
 MediaCodec 有两种方式触发输出关键帧，
 一是由配置时设置的 KEY_FRAME_RATE和KEY_I_FRAME_INTERVAL参数自动触发，
 二是运行过程中通过 setParameters 手动触发输出关键帧。
 自动触发实际是按照帧数触发的，
 例如设置帧率为 20 fps，关键帧间隔为 1s ，
 那就会每 20桢输出一个关键帧，一旦实际帧率低于配置帧率，
 那就会导致关键帧间隔时间变长。
 由于 MediaCodec 启动后就不能修改配置帧率/关键帧间隔了，
 所以如果希望改变关键帧间隔帧数，就必须重启编码器。
 手动触发输出关键帧：
 if (System.currentTimeMillis() - timeStamp >= 1000) {//1000毫秒后，设置参数
 timeStamp = System.currentTimeMillis();
 if (Build.VERSION.SDK_INT >= 23) {
 Bundle params = new Bundle();
 params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
 mMediaCodec.setParameters(params);
 }
 }
 关键帧踩坑
 有时候你会发现自动触发关键帧方式失效了
 经排查发现真正的原因是在于视频的输入源，如果是通过Camera的PreviewCallback的方式来获取视频数据再喂给MediaCodec的方式是无法控制输出关键帧的数量的。
 发现当选择支持颜色格式为yuv420p的编码器时，KEY_I_FRAME_INTERVAL 设置无效；
 选择支持yuv420sp的编码器时，KEY_I_FRAME_INTERVAL 设置有效；

 想要控制输出输出关键帧数量就必须通过调用MediaCodec.createInputSurface()
 方法获取输入Surface，再通过Opengl渲染后喂给MediaCodec才能真正控制关键帧的数量。
 //判断输出数据是否为关键帧的方法：
 boolean keyFrame = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
 部分机型MediaCodec.dequeueOutputBuffer一直报IllegalStateException
 部分机型会一直卡在MediaCodec.INFO_TRY_AGAIN_LATER中，有的原因也是因为这个
 该机型硬解码最大配置分辨率低于当前视频流的分辨率
 关于 level、profile设置
 由于视频编码后显示的数据质量偏低，所以需要调整质量。这个时候需要在这个设置level、profile
 Profile是对视频压缩特性的描述（CABAC呀、颜色采样数等等）。
 Level是对视频本身特性的描述（码率、分辨率、fps）。
 简单来说，Profile越高，就说明采用了越高级的压缩特性。
 Level越高，视频的码率、分辨率、fps越高
 // 不支持设置Profile和Level，而应该采用默认设置
 mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
 mediaFormat.setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCLevel41); // Level 4.1

 DRM是英文Digital rights management的缩写,可以理解为版权保护
 public static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
 //获取sessionId
 MediaDrm mediaDrm = new MediaDrm();
 String sessionId = mediaDrm.openSession();
 //创建MediaCrypto
 sessionId 是串联MediaDrm和MediaCrypto的关键
 MediaCrypto ctypto = new MediaCrypto(WIDEVINE_UUID, sessionId)
 //用cypto对象来进行解密
 MediaCodec codec = new MediaCodec("xxxx")
 codec.configure(..,...,ctypto)
 注意的是license并不需要在configure之前获取，可以稍后再进行
 //网络连接
 byte[] license = HttpUrlConnection.connect().......
 mediaDrm.provideKeyResponse(xxx,license);
 所有工作结束，视频可以正常播放了

 Android Tunnel Mode
 https://blog.csdn.net/yingmuliuchuan/article/details/81807512
 https://developer.amazon.com/zh/docs/fire-tv/4k-tunnel-mode-playback.html

 Audio Object Types
 MPEG-4 Audio Object Types:

 0: Null
 1: AAC Main
 2: AAC LC (Low Complexity)
 3: AAC SSR (Scalable Sample Rate)
 4: AAC LTP (Long Term Prediction)
 5: SBR (Spectral Band Replication)
 6: AAC Scalable
 7: TwinVQ
 8: CELP (Code Excited Linear Prediction)
 9: HXVC (Harmonic Vector eXcitation Coding)
 10: Reserved
 11: Reserved
 12: TTSI (Text-To-Speech Interface)
 13: Main Synthesis
 14: Wavetable Synthesis
 15: General MIDI
 16: Algorithmic Synthesis and Audio Effects
 17: ER (Error Resilient) AAC LC
 18: Reserved
 19: ER AAC LTP
 20: ER AAC Scalable
 21: ER TwinVQ
 22: ER BSAC (Bit-Sliced Arithmetic Coding)
 23: ER AAC LD (Low Delay)
 24: ER CELP
 25: ER HVXC
 26: ER HILN (Harmonic and Individual Lines plus Noise)
 27: ER Parametric
 28: SSC (SinuSoidal Coding)
 29: PS (Parametric Stereo)
 30: MPEG Surround
 31: (Escape value)
 32: Layer-1
 33: Layer-2
 34: Layer-3
 35: DST (Direct Stream Transfer)
 36: ALS (Audio Lossless)
 37: SLS (Scalable LosslesS)
 38: SLS non-core
 39: ER AAC ELD (Enhanced Low Delay)
 40: SMR (Symbolic Music Representation) Simple
 41: SMR Main
 42: USAC (Unified Speech and Audio Coding) (no SBR)
 43: SAOC (Spatial Audio Object Coding)
 44: LD MPEG Surround
 45: USAC

 Sampling Frequencies
 There are 13 supported frequencies:

 0: 96000 Hz
 1: 88200 Hz
 2: 64000 Hz
 3: 48000 Hz
 4: 44100 Hz
 5: 32000 Hz
 6: 24000 Hz
 7: 22050 Hz
 8: 16000 Hz
 9: 12000 Hz
 10: 11025 Hz
 11: 8000 Hz
 12: 7350 Hz
 13: Reserved
 14: Reserved
 15: frequency is written explictly

 Channel Configurations
 These are the channel configurations:

 0: Defined in AOT Specifc Config
 1: 1 channel: front-center
 2: 2 channels: front-left, front-right
 3: 3 channels: front-center, front-left, front-right
 4: 4 channels: front-center, front-left, front-right, back-center
 5: 5 channels: front-center, front-left, front-right, back-left, back-right
 6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel
 7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel
 8-15: Reserved

 */