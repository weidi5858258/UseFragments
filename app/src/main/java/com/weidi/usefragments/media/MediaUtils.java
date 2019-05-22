package com.weidi.usefragments.media;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.weidi.usefragments.tool.MLog;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by root on 19-1-28.
 */

public class MediaUtils {

    private static final String TAG =
            MediaUtils.class.getSimpleName();
    private static final boolean DEBUG = true;

    // 想要的编码格式
    private static final String VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int BIT_RATE = 1200000;
    private static final int FRAME_RATE = 30;
    private static final int IFRAME_INTERVAL = 1;

    public static MediaCodec getMediaEncoder(int width, int height) {
        MediaCodecInfo codecInfo = selectCodec(VIDEO_MIME_TYPE);
        if (codecInfo == null) {
            throw new RuntimeException("不支持的编码格式");
        }

        MediaCodec encoder = null;
        try {
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
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
            encoder.setCallback(new MediaCodec.Callback() {

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
                        Log.e(TAG, "onError()\n" + e.getDiagnosticInfo());
                }

                @Override
                public void onOutputFormatChanged(
                        @NonNull MediaCodec codec,
                        @NonNull MediaFormat format) {
                    if (DEBUG)
                        Log.d(TAG, "onOutputFormatChanged() format: " + format);
                    // getSpsPpsByteBuffer(mediaFormat);
                }

            });

            /*encoder.configure(
                    getMediaEncoderFormat(width, height),
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();*/
        } catch (IOException e) {
            e.printStackTrace();
            encoder = null;
        }

        return encoder;
    }

    public static MediaCodec getMediaDecoder(int width, int height) {
        MediaCodecInfo codecInfo = selectCodec(VIDEO_MIME_TYPE);
        if (codecInfo == null) {
            throw new RuntimeException("不支持的编码格式");
        }

        MediaCodec decoder = null;
        try {
            decoder = MediaCodec.createByCodecName(codecInfo.getName());
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
            decoder.setCallback(new MediaCodec.Callback() {

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
            });

            // 最后一个参数是flag,用来标记是否是编码,传入0表示作为解码.
            /*decoder.configure(
                    getMediaDecoderFormat(width, height),
                    surface,
                    null,
                    0);
            decoder.start();*/
        } catch (IOException e) {
            e.printStackTrace();
        }
        return decoder;
    }

    public static MediaFormat getMediaEncoderFormat(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
        // 必须设置为COLOR_FormatSurface，因为是用surface作为输入源
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        // 设置帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // 设置抽取关键帧的间隔，以s为单位，负数或者0会不抽取关键帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (DEBUG)
            Log.d(TAG, "getMediaEncoderFormat() created video format: " + format);

        return format;
    }

    public static MediaFormat getMediaDecoderFormat(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
        // 设置帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        if (DEBUG)
            Log.d(TAG, "getMediaDecoderFormat() created video format: " + format);

        return format;
    }

    /***
     * Find an encoder supported specified MIME type
     * 查找支持"video/avc"这种MIME type的CodecName,
     * 然后就可以通过MediaCodec.createByCodecName(CodecName)
     * 这个方法得到一个可以MediaCodec对象.
     *
     * @param mimeType 如MediaFormat.MIMETYPE_VIDEO_AVC
     * @return Returns empty array if not found any encoder supported specified MIME type
     *
     * 在我的手机上找到
     * Video的CodecName(根据"video/avc"):
     * "OMX.MTK.VIDEO.ENCODER.AVC"
     * "OMX.google.h264.encoder"
     *
     * Audio的CodecName(根据"audio/mp4a-latm"):
     * "OMX.google.aac.encoder"
     *
     */
    public static MediaCodecInfo[] findEncodersByType(String mimeType) {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        List<MediaCodecInfo> mediaCodecInfos = new ArrayList<MediaCodecInfo>();
        for (MediaCodecInfo mediaCodecInfo : codecList.getCodecInfos()) {
            if (!mediaCodecInfo.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities codecCapabilities =
                        mediaCodecInfo.getCapabilitiesForType(mimeType);
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
     * @return
     */

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
     44100Hz(在所有设备上都能正常工作)
     @param channelCount
     声道数
     @param audioFormat
     AudioFormat.ENCODING_PCM_16BIT(比较常用)
     AudioFormat.ENCODING_PCM_8BIT
     AudioFormat.ENCODING_PCM_FLOAT
     @return
     */
    public static AudioRecord createAudioRecord(
            int audioSource, int sampleRateInHz,
            int channelCount, int audioFormat) {
        /***
         channelConfig
         AudioFormat.CHANNEL_IN_STEREO
         AudioFormat.CHANNEL_IN_MONO(在所有设备上都能正常工作)
         */
        int channelConfig = channelCount == 2
                ?
                AudioFormat.CHANNEL_IN_STEREO
                :
                AudioFormat.CHANNEL_IN_MONO;
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat) * 2;
        if (bufferSizeInBytes <= 0) {
            if (DEBUG)
                Log.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }
        if (DEBUG)
            MLog.d(TAG, "createAudioRecord() bufferSizeInBytes: " + (bufferSizeInBytes / 2));

        AudioRecord audioRecord = new AudioRecord(
                audioSource,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                bufferSizeInBytes);
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            if (DEBUG)
                Log.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioRecord(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        return audioRecord;
    }

    /***
     一般使用这个就可以了(调用这个方法前必须先动态取得相应权限)
     */
    public static AudioRecord createAudioRecord() {
        if (DEBUG)
            Log.d(TAG, "createAudioRecord() start");
        int audioSource = MediaRecorder.AudioSource.MIC;
        // 兼容所有Android设备
        int sampleRateInHz = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        // 兼容所有Android设备
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                sampleRateInHz,
                channelConfig,
                audioFormat);
        if (DEBUG)
            MLog.d(TAG, "createAudioRecord() bufferSizeInBytes: " + bufferSizeInBytes);
        if (bufferSizeInBytes <= 0) {
            //if (bufferSizeInBytes <= AudioRecord.ERROR_BAD_VALUE) {
            if (DEBUG)
                Log.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        bufferSizeInBytes *= 2;
        AudioRecord audioRecord = new AudioRecord(
                audioSource,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                bufferSizeInBytes);
        // 此判断很关键
        if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            audioRecord.release();
            if (DEBUG)
                Log.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioRecord(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        if (DEBUG)
            Log.d(TAG, "createAudioRecord() end");
        return audioRecord;
    }

    /***
     *
     * @param sampleRateInHz 44100
     * @param channelCount 声道数
     * @param audioFormat AudioFormat.ENCODING_PCM_16BIT
     * @param mode AudioTrack.MODE_STREAM
     * @param sessionId AudioManager.AUDIO_SESSION_ID_GENERATE
     * @return
     */
    public static AudioTrack createAudioTrack(
            int sampleRateInHz, int channelCount,
            int audioFormat, int mode, int sessionId) {
        int channelConfig = channelCount == 2
                ?
                AudioFormat.CHANNEL_IN_STEREO
                :
                AudioFormat.CHANNEL_IN_MONO;
        int minBufferSize = AudioRecord.getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat) * 2;
        if (minBufferSize <= 0) {
            if (DEBUG)
                Log.e(TAG, String.format(Locale.US,
                        "Bad arguments: getMinBufferSize(%d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat));
            return null;
        }

        if (DEBUG)
            MLog.d(TAG, "createAudioTrack() minBufferSize: " + (minBufferSize / 2));
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(sampleRateInHz)
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .build();
        AudioTrack audioTrack = new AudioTrack(
                attributes,
                format,
                minBufferSize,
                mode,
                sessionId);
        if (audioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            if (DEBUG)
                Log.e(TAG, String.format(Locale.US,
                        "Bad arguments to new AudioTrack(%d, %d, %d, %d, %d)",
                        sampleRateInHz, channelConfig, audioFormat, mode, sessionId));
            return null;
        }

        return audioTrack;
    }

    public static void printMediaCodecInfos(MediaCodecInfo[] mediaCodecInfos, String mimeType) {
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
                if (VIDEO_MIME_TYPE.equals(mimeType)) {
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
            Log.i(TAG, builder.toString());
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
        } else {
            bitRate = 10000000;
        }
        return bitRate;
    }

    /***
     *
     * @param mimeType "video/avc"
     * @return
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return null;
        }
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodecInfo[] infos = list.getCodecInfos();
        if (infos == null) {
            return null;
        }
        for (MediaCodecInfo info : infos) {
            if (info == null || !info.isEncoder()) {
                continue;
            }
            for (String type : info.getSupportedTypes()) {
                if (TextUtils.isEmpty(type)) {
                    continue;
                }
                if (type.equalsIgnoreCase(mimeType)) {
                    if (DEBUG)
                        Log.d(TAG, "selectCodec() the selected encoder is : " + info.getName());
                    return info;
                }
            }
        }
        return null;
    }

}
