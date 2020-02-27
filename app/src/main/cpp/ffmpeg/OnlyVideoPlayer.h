//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_SIMPLEVIDEOPLAYER_H
#define USEFRAGMENTS_SIMPLEVIDEOPLAYER_H

// 需要引入native绘制的头文件
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <list>
#include <iostream>
#include <iomanip>

#ifdef __cplusplus
extern "C" {
#endif

//extern "C" {// 不能少
// ffmpeg使用MediaCodec进行硬解码(需要编译出支持硬解码的so库)
#include <libavcodec/jni.h>
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

// 使用libyuv,将YUV转换RGB
#include <libyuv/basic_types.h>
#include <libyuv/compare.h>
#include <libyuv/convert.h>
#include <libyuv/convert_argb.h>
#include <libyuv/convert_from.h>
#include <libyuv/convert_from_argb.h>
#include <libyuv/cpu_id.h>
#include <libyuv/mjpeg_decoder.h>
#include <libyuv/planar_functions.h>
#include <libyuv/rotate.h>
#include <libyuv/rotate_argb.h>
#include <libyuv/row.h>
#include <libyuv/scale.h>
#include <libyuv/scale_argb.h>
#include <libyuv/scale_row.h>
#include <libyuv/version.h>
#include <libyuv/video_common.h>

//    #include <jconfig.h>
//    #include <jerror.h>
//    #include <jmorecfg.h>
//    #include <jpeglib.h>
//    #include <turbojpeg.h>

//};// extern "C" end

#ifdef __cplusplus
}
#endif

#include <time.h>

//#include "MediaWrapper.h"
#include "ffmpeg.h"
//#include "MyHeader.h"
#include "../include/Log.h"

namespace alexander2 {

    // 1 second of 48khz 32bit audio
#define MAX_AUDIO_FRAME_SIZE 192000

#define TYPE_UNKNOW -1
#define TYPE_AUDIO 1
#define TYPE_VIDEO 2

    // 不能无限制读取数据进行保存,这样要出错的(能播放,但不是想要的结果)
#define MAX_AVPACKET_COUNT         10000

//#define MAX_AVPACKET_COUNT_AUDIO_HTTP 3000
#define MAX_AVPACKET_COUNT_VIDEO_HTTP 3000

//#define MAX_AVPACKET_COUNT_AUDIO_LOCAL 100
#define MAX_AVPACKET_COUNT_VIDEO_LOCAL 100

#define CACHE_COUNT 100

    // 子类都要用到的部分
    struct Wrapper {
        int type = TYPE_UNKNOW;
        AVCodecContext *avCodecContext = NULL;
        // 有些东西需要通过它去得到
        AVCodecParameters *avCodecParameters = NULL;
        // 解码器
        AVCodec *decoderAVCodec = NULL;
        // 编码器(没用到,因为是播放,所以不需要编码)
        // AVCodec *encoderAVCodec = NULL;

        unsigned char *outBuffer1 = NULL;
        unsigned char *outBuffer2 = NULL;
        unsigned char *outBuffer3 = NULL;
        //size_t outBufferSize = 0;

        int streamIndex = -1;

        // 不能这样定义
        // std::list<AVPacket*> *list1 = NULL;
        std::list<AVPacket> *list1 = NULL;
        std::list<AVPacket> *list2 = NULL;
        // 队列中最多保存多少个AVPacket
        int list1LimitCounts = 0;
        int list2LimitCounts = 0;
        bool isHandleList1Full = false;

        bool isStarted = false;
        bool isReading = false;
        bool isHandling = false;
        // 因为user所以pause
        bool isPausedForUser = false;
        // 因为cache所以pause
        bool isPausedForCache = false;
        // 因为seek所以pause
        bool isPausedForSeek = false;
        // seek的初始化条件有没有完成,true表示完成
        bool needToSeek = false;

        // 单位: 秒
        int64_t duration = -1;
        // 单位: 秒 seekTo到某个时间点
        int64_t timestamp = -1;
        // 跟线程有关
        pthread_mutex_t readLockMutex;
        pthread_cond_t readLockCondition;
        pthread_mutex_t handleLockMutex;
        pthread_cond_t handleLockCondition;

        // 存储压缩数据(视频对应H.264等码流数据,音频对应AAC/MP3等码流数据)
        // AVPacket *avPacket = NULL;
        // 视频使用到sws_scale函数时需要定义这些变量,音频也要用到
        // unsigned char *srcData[4] = {NULL}, dstData[4] = {NULL};
    };

    struct VideoWrapper {
        struct Wrapper *father = NULL;
        SwsContext *swsContext = NULL;
        // 从视频源中得到
        enum AVPixelFormat srcAVPixelFormat = AV_PIX_FMT_NONE;
        // 从原来的像素格式转换为想要的视频格式(可能应用于不需要播放视频的场景)
        // 播放时dstAVPixelFormat必须跟srcAVPixelFormat的值一样,不然画面有问题
        enum AVPixelFormat dstAVPixelFormat = AV_PIX_FMT_NONE;
        // 一个视频没有解码之前读出的数据是压缩数据,把压缩数据解码后就是原始数据
        // 解码后的原始数据(像素格式可能不是我们想要的,如果是想要的,那么没必要再调用sws_scale函数了)
        AVFrame *decodedAVFrame = NULL;
        // 解码后的原始数据(像素格式是我们想要的)
        AVFrame *rgbAVFrame = NULL;
        // 从视频源中得到的宽高
        int srcWidth = 0, srcHeight = 0;
        size_t srcArea = 0;
        // 想要播放的窗口大小,可以直接使用srcWidth和srcHeight
        int dstWidth = 720, dstHeight = 360;
        size_t dstArea = 0;
        // 使用到sws_scale函数时需要定义这些变量
        int srcLineSize[4] = {0}, dstLineSize[4] = {0};
    };

    void *readData(void *opaque);

    void *handleData(void *opaque);

    void initAV();

    void initVideo();

    int openAndFindAVFormatContext();

    int findStreamIndex();

    int findAndOpenAVCodecForVideo();

    int createSwsContext();

    void closeVideo();

    int initPlayer();

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject);

    int play();

    int pause();

    int stop();

    int release();

    bool isRunning();

    bool isPlaying();

    int seekTo(int64_t timestamp);

    int64_t getDuration();

    void stepAdd();

    void stepSubtract();

    /*class SimpleVideoPlayer {

    private:
        // char *inFilePath = "/storage/2430-1702/BaiduNetdisk/music/谭咏麟 - 水中花.mp3";
        char *inFilePath = NULL;

        ANativeWindow *pANativeWindow = NULL;

    public:
    };*/

}


#endif //USEFRAGMENTS_SIMPLEVIDEOPLAYER_H
