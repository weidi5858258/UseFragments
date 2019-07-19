package com.weidi.usefragments.tool;

import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

/***
 Created by root on 19-7-19.
 */

public class MP4Player {
    private static final String TAG =
            MP4Player.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int TIME_OUT = 10000;
    private static final int PREPARE = 0x0001;
    private static final int PLAY = 0x0002;
    private static final int PAUSE = 0x0003;
    private static final int STOP = 0x0004;
    private static final int RELEASE = 0x0005;
    private static final int PREV = 0x0006;
    private static final int NEXT = 0x0007;

    private String mPath;
    // 必须要有两个MediaExtractor对象,不能共用同一个
    private MediaExtractor mAudioExtractor;
    private MediaExtractor mVideoExtractor;
    private MediaCodec mAudioDncoderMediaCodec;
    private MediaCodec mVideoDncoderMediaCodec;
    private MediaFormat mAudioDncoderMediaFormat;
    private MediaFormat mVideoDncoderMediaFormat;
    private AudioTrack mAudioTrack;
    private Surface mSurface;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;


}

/***
 MP4封装格式是基于QuickTime容器格式定义，媒体描述与媒体数据分开，
 目前被广泛应用于封装h.264视频和ACC音频，是高清视频/HDV的代表。
 MP4文件中所有数据都封装在box中（对应QuickTime中的atom），
 即MP4文件是由若干个box组成，每个box有长度和类型，每个box中还可以包含另外的子box（称container box）。
 一个MP4文件首先会有且只有一个“ftyp”类型的box，作为MP4格式的标志并包含关于文件的一些信息；
 之后会有且只有一个“moov”类型的box（Movie Box），它是一种container box，子box包含了媒体的metadata信息；
 MP4文件的媒体数据包含在“mdat”类型的box（Midia Data Box）中，
 该类型的box也是container box，可以有多个，也可以没有（当媒体数据全部引用其他文件时），
 媒体数据的结构由metadata进行描述。
 MP4中box存储方式为大端模式。一般，标准的box开头会有四个字节的box size。

 track
 表示一些sample的集合，对于媒体数据来说，track表示一个视频或音频序列。
 hint track
 特殊的track，并不包含媒体数据，包含的是一些将其他数据track打包成流媒体的指示信息。
 sample
 对于非hint   track来说，video sample即为一帧视频，或一组连续视频帧，
 audio sample即为一段连续的压缩音频，它们统称sample。
 对于hint   track，sample定义一个或多个流媒体包的格式。
 sample table
 指明sampe时序和物理布局的表。
 chunk
 一个track的几个sample组成的单元。

 在H264中，SPS和PPS存在于NALU header中，而在MP4文件中，SPS和PPS存在于AVCDecoderConfigurationRecord， 首先要定位avcC.
 MP4文件，由层级的box/atom组成。box的头信息包含固定4个字节的长度信息和4个字节的类型信息。
 box也有层级结构的，这样box可以嵌套或者包含多个box。一般真正的视频数据都在“mdat”类型的box中。
 一般MP4文件中有音视频track，每个track则有很多chunk，chunk内有多个sample。从track一直到sample，都是按照层级/表进行组织的。

 一个典型的“avcC”内容如下：
 01
 42
 c0
 15
 fd
 e1 00
 17
 67 42 c0 15 92 44 0f 04 7f 58 08 80 00 00 3e 80 00 0b b5 47 8b 17 50
 01 00
 04
 68 ce 32 c8
 解析的结果(内部的数值都是十六进制的)
 version:      01
 profile:      42
 compatibilty: c0
 level:        15
 lengthSize:   2 (0xfd -> 2)
 num of SPS:   1
 SPS len:      17
 SPS:          67 42 c0 15 92 44 0f 04 7f 58 08 80 00 00 3e 80 00 0b b5 47 8b 17 50
 num of PPS:   1
 PPS len:      4
 PPS:          68 ce 32 c8

 根据里面的lengthSize就可以得到“NALU的长度”
 SPS和PPS是h264流中的元信息，在MP4文件中单独存放在“avcC”中。
 转换的时候，还需要将SPS和PPS提取出来，添加上0x00000001，放在h264视频流的开始位置。

 #define NALU_TYPE_SLICE    1
 #define NALU_TYPE_DPA      2
 #define NALU_TYPE_DPB      3
 #define NALU_TYPE_DPC      4
 #define NALU_TYPE_IDR      5
 #define NALU_TYPE_SEI      6
 #define NALU_TYPE_SPS      7
 #define NALU_TYPE_PPS      8
 #define NALU_TYPE_AUD      9
 #define NALU_TYPE_EOSEQ    10
 #define NALU_TYPE_EOSTREAM 11
 #define NALU_TYPE_FILL     12

 00 00 00 01 06
 00 00 00 01 67
 00 00 00 01 68
 00 00 00 01 65
 开头均为0x00000001四个字节，根据起始的第五个字节分析，
 段1：0x06 & 0x1f = 6，nalu为辅助增强信息 (SEI)；
 段2：0x67 & 0x1f = 7，nalu为SPS；
 段3：0x68 & 0x1f = 8，nalu为PPS；
 段4：0x65 & 0x1f = 5，nalu为I帧。








 */