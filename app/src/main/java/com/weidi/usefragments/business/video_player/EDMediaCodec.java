package com.weidi.usefragments.business.video_player;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.MLog;

import java.nio.ByteBuffer;

public class EDMediaCodec {

    private static final String TAG =
            "player_alexander";

    private static final int TIME_OUT = 10000;

    enum TYPE {
        TYPE_VIDEO,
        TYPE_AUDIO,
        //TYPE_SUBTITLE
    }

    public static boolean feedInputBufferAndDrainOutputBuffer(
            TYPE type,
            MediaCodec codec,
            byte[] data,
            int offset,
            int size,
            long presentationTimeUs,
            boolean render) {
        /*if (!wrapper.isHandling) {
            if (wrapper.type == TYPE_AUDIO) {
                handleAudioOutputBuffer(-1, null, null, -1);
            } else {
                handleVideoOutputBuffer(-1, null, null, -1);
            }
            MediaUtils.releaseMediaCodec(wrapper.decoderMediaCodec);
            return false;
        }*/

        return feedInputBuffer(type,
                codec,
                data,
                offset,
                size,
                presentationTimeUs)
                &&
                drainOutputBuffer(type, codec, render);
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
    private static boolean feedInputBuffer(
            TYPE type,
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
            if (type == TYPE.TYPE_AUDIO) {
                MLog.e(TAG, "feedInputBuffer() Audio Output occur exception: " + e);
                handleAudioOutputBuffer(-1, null, null, -1);
            } else {
                MLog.e(TAG, "feedInputBuffer() Video Output occur exception: " + e);
                handleVideoOutputBuffer(-1, null, null, -1);
            }
            MediaUtils.releaseMediaCodec(codec);
            return false;
        }

        return true;
    }

    /***
     * 拿出数据(在底层已经经过编解码了)进行处理(如视频数据进行渲染,音频数据进行播放)
     * @return
     */
    private static boolean drainOutputBuffer(TYPE type, MediaCodec codec, boolean render) {
        // 房间信息
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        ByteBuffer room = null;
        for (; ; ) {
            /*if (!wrapper.isHandling) {
                if (wrapper.type == TYPE_AUDIO) {
                    handleAudioOutputBuffer(-1, null, null, -1);
                } else {
                    handleVideoOutputBuffer(-1, null, null, -1);
                }
                MediaUtils.releaseMediaCodec(wrapper.decoderMediaCodec);
                return false;
            }*/

            try {
                // 房间号
                int roomIndex = codec.dequeueOutputBuffer(roomInfo, TIME_OUT);
                /*if (type == TYPE.TYPE_AUDIO) {
                    MLog.d(TAG, "drainOutputBuffer() Audio roomIndex: " + roomIndex);
                } else {
                    MLog.w(TAG, "drainOutputBuffer() Video roomIndex: " + roomIndex);
                }*/
                switch (roomIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        // 像音频,第一个输出日志
                        /*if (type == TYPE.TYPE_AUDIO) {
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
                        if (type == TYPE.TYPE_AUDIO) {
                            MLog.d(TAG, "drainOutputBuffer() " +
                                    "Audio Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleAudioOutputFormat(codec.getOutputFormat());
                        } else {
                            MLog.w(TAG, "drainOutputBuffer() " +
                                    "Video Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            handleVideoOutputFormat(codec.getOutputFormat());
                        }
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        // 像音频,第二个输出日志.视频好像没有这个输出日志
                        if (type == TYPE.TYPE_AUDIO) {
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
                    room = codec.getOutputBuffer(roomIndex);
                } else {
                    room = codec.getOutputBuffers()[roomIndex];
                }
                // 房间大小
                int roomSize = roomInfo.size;
                // 不能根据room是否为null来判断是audio还是video(但我的三星Note2手机上是可以的)
                if (room != null) {
                    // audio
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    if (type == TYPE.TYPE_AUDIO) {
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
                    if (type == TYPE.TYPE_AUDIO) {
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
                    if (type == TYPE.TYPE_AUDIO) {
                        MLog.d(TAG, "drainOutputBuffer() " +
                                "Audio Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    } else {
                        MLog.w(TAG, "drainOutputBuffer() " +
                                "Video Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                    }
                }

                codec.releaseOutputBuffer(roomIndex, render);
            } catch (IllegalStateException
                    | IllegalArgumentException
                    | NullPointerException e) {
                e.printStackTrace();
                if (type == TYPE.TYPE_AUDIO) {
                    MLog.e(TAG, "drainOutputBuffer() Audio Output occur exception: " + e);
                    handleAudioOutputBuffer(-1, room, roomInfo, -1);
                } else {
                    MLog.e(TAG, "drainOutputBuffer() Video Output occur exception: " + e);
                    handleVideoOutputBuffer(-1, room, roomInfo, -1);
                }
                MediaUtils.releaseMediaCodec(codec);
                return false;
            }
        }// for(;;) end

        return true;
    }

    private static void handleAudioOutputFormat(MediaFormat mediaFormat) {

    }

    private static void handleVideoOutputFormat(MediaFormat mediaFormat) {

    }

    private static int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                               MediaCodec.BufferInfo roomInfo, int roomSize) {

        return 0;
    }

    private static int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                               MediaCodec.BufferInfo roomInfo, int roomSize) {

        return 0;
    }

}
