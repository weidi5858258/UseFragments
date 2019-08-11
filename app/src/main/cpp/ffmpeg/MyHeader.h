//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_MYHEADER_H
#define USEFRAGMENTS_MYHEADER_H

// 必须得有
#include "jni.h"
#include "android/log.h"

#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <string>

// 定义了一些常用类型的最小值,最大值
#include <limits.h>
#include <unistd.h>
#include <errno.h>
#include <setjmp.h>
#include <libgen.h>
#include <inttypes.h>
#include <math.h>
//下面三个头文件使用open函数时用到
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mount.h>
#include <wchar.h>
#include <time.h>
#include <pthread.h>

#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/un.h>

extern "C" {// 不能少
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavformat/avio.h>
#include "libswresample/swresample.h"
// libswscale是一个主要用于处理图片像素数据的类库.可以完成图片像素格式的转换,图片的拉伸等工作.
#include <libswscale/swscale.h>
#include <libavfilter/avfilter.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/channel_layout.h>
#include <libavutil/mathematics.h>
#include <libavutil/samplefmt.h>
// 这里是做分片时候重采样编码音频用的
#include <libavutil/audio_fifo.h>
#include <libavutil/imgutils.h>
#include <libavutil/avutil.h>
#include <libavutil/avassert.h>
#include <libavutil/avstring.h>
#include <libavutil/frame.h>
#include <libavutil/hwcontext.h>
#include <libavutil/parseutils.h>
#include <libavutil/pixdesc.h>
#include <libavutil/pixfmt.h>
#include <libavutil/fifo.h>
#include <libavutil/log.h>
#include <libavutil/opt.h>
#include <libavutil/mem.h>
#include <libavutil/error.h>
#include <libavutil/time.h>

//    #include <jconfig.h>
//    #include <jerror.h>
//    #include <jmorecfg.h>
//    #include <jpeglib.h>
//    #include <turbojpeg.h>

};// extern "C" end

#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG,__VA_ARGS__)  // 定义LOGI类型
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG,__VA_ARGS__)  // 定义LOGW类型
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG,__VA_ARGS__) // 定义LOGF类型

// 1 second of 48khz 32bit audio
#define MAX_AUDIO_FRAME_SIZE 192000

#define TYPE_UNKNOW -1
#define TYPE_AUDIO 1
#define TYPE_VIDEO 2

#define MAX_AVPACKET_COUNT_AUDIO 2000
#define MAX_AVPACKET_COUNT_VIDEO 5000

typedef struct AVPacketQueue {
    AVPacketList *firstAVPacketList = NULL;
    AVPacketList *lastAVPacketList = NULL;
    // 有多少个AVPacketList
    int allAVPacketsCount = 0;
    // 所有AVPacket占用的空间大小
    int64_t allAVPacketsSize = 0;
};

// 子类都要用到的部分
struct Wrapper {
    int type = TYPE_UNKNOW;
    AVFormatContext *avFormatContext = NULL;
    AVCodecContext *avCodecContext = NULL;
    // 解码器
    AVCodec *decoderAVCodec = NULL;
    // 编码器
    AVCodec *encoderAVCodec = NULL;
    // 存储压缩数据(视频对应H.264等码流数据,音频对应AAC/MP3等码流数据)
    // AVPacket *avPacket = NULL;
    // 存储非压缩数据(视频对应RGB/YUV像素数据,音频对应PCM采样数据)
    AVFrame *srcAVFrame = NULL;
    // 用于格式转换(音频用不到)
    AVFrame *dstAVFrame = NULL;
    // 有些东西需要通过它去得到
    AVCodecParameters *avCodecParameters = NULL;
    int streamIndex = -1;
    // 读取了多少个AVPacket
    int readFramesCount = 0;
    // 处理了多少个AVPacket
    int handleFramesCount = 0;
    // 存储原始数据
    unsigned char *outBuffer1 = NULL;
    unsigned char *outBuffer2 = NULL;
    unsigned char *outBuffer3 = NULL;
    size_t outBufferSize = 0;
    // 视频使用到sws_scale函数时需要定义这些变量,音频也要用到
    unsigned char *srcData[4] = {NULL}, dstData[4] = {NULL};

    struct AVPacketQueue *queue1 = NULL;
    struct AVPacketQueue *queue2 = NULL;
    bool isHandlingForQueue1 = false;
    bool isHandlingForQueue2 = false;

    int maxAVPacketsCount = 1000;

    bool isReading = false;
    bool isHandling = false;

    // 因为user所以pause
    bool isPausedForUser = false;
    // 因为cache所以pause
    bool isPausedForCache = false;

    pthread_mutex_t readLockMutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t readLockCondition = PTHREAD_COND_INITIALIZER;

    pthread_mutex_t handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t handleLockCondition = PTHREAD_COND_INITIALIZER;
};

struct AudioWrapper {
    struct Wrapper father;
    SwrContext *swrContext = NULL;
    // 从音频源或视频源中得到
    // 采样率
    int srcSampleRate = 0;
    int dstSampleRate = 44100;
    // 声道数
    int srcNbChannels = 0;
    // 由dstChannelLayout去获到
    int dstNbChannels = 0;
    int srcNbSamples = 0;
    int dstNbSamples = 0;
    // 由srcNbChannels能得到srcChannelLayout,也能由srcChannelLayout得到srcNbChannels
    int srcChannelLayout = 0;
    int dstChannelLayout = 0;
    // 双声道输出
    // int dstChannelLayout = AV_CH_LAYOUT_STEREO;
    // 从音频源或视频源中得到(采样格式)
    enum AVSampleFormat srcAVSampleFormat = AV_SAMPLE_FMT_NONE;
    // 输出的采样格式16bit PCM
    enum AVSampleFormat dstAVSampleFormat = AV_SAMPLE_FMT_S16;

    // 要播放的数据存在于playBuffer中
    DECLARE_ALIGNED(16, unsigned char, playBuffer)[MAX_AUDIO_FRAME_SIZE * 4];

    //解码一次得到的数据量
    unsigned int decodedDataSize = 0;
    //用于标记已处理过的数据位置(针对audio_decoded_data_size的位置)
    unsigned int decodedDataSizeIndex = 0;
};

struct VideoWrapper {
    struct Wrapper father;
    SwsContext *swsContext = NULL;
    // 从视频源中得到
    enum AVPixelFormat srcAVPixelFormat = AV_PIX_FMT_NONE;
    // 从原来的像素格式转换为想要的视频格式(可能应用于不需要播放视频的场景)
    // 播放时dstAVPixelFormat必须跟srcAVPixelFormat的值一样,不然画面有问题
    enum AVPixelFormat dstAVPixelFormat = AV_PIX_FMT_RGB24;
    // 从视频源中得到的宽高
    int srcWidth = 0, srcHeight = 0;
    size_t srcArea = 0;
    // 想要播放的窗口大小,可以直接使用srcWidth和srcHeight
    int dstWidth = 720, dstHeight = 360;
    size_t dstArea = 0;
    // 使用到sws_scale函数时需要定义这些变量
    int srcLineSize[4] = {0}, dstLineSize[4] = {0};
};



#endif //USEFRAGMENTS_MYHEADER_H
