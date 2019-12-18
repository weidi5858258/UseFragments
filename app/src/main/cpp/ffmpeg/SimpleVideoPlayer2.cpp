//
// Created by root on 19-8-8.
//

#include "SimpleVideoPlayer2.h"

// 需要引入native绘制的头文件
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {// 不能少
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

};// extern "C" end

#define LOG "alexander"

/***
 https://www.cnblogs.com/azraelly/archive/2013/01/01/2841269.html
 图文详解YUV420数据格式
 https://blog.csdn.net/cgwang_1580/article/details/79595958
 常用视频像素格式NV12、NV2、I420、YV12、YUYV

 int av_image_get_buffer_size(enum AVPixelFormat pix_fmt, int width, int height, int align);
 函数的作用是通过指定像素格式、图像宽、图像高来计算所需的内存大小
 重点说明一个参数align:此参数是设定内存对齐的对齐数,也就是按多大的字节进行内存对齐.
 比如设置为1,表示按1字节对齐,那么得到的结果就是与实际的内存大小一样.
 再比如设置为4,表示按4字节对齐.也就是内存的起始地址必须是4的整倍数.

 int av_image_alloc(
     uint8_t *pointers[4],
     int linesizes[4],
     int w,
     int h,
     enum AVPixelFormat pix_fmt,
     int align);
 pointers[4]：保存图像通道的地址.
 如果是RGB,则前三个指针分别指向R,G,B的内存地址,第四个指针保留不用.
 linesizes[4]：保存图像每个通道的内存对齐的步长,即一行的对齐内存的宽度,此值大小等于图像宽度.
 w:       要申请内存的图像宽度.
 h:       要申请内存的图像高度.
 pix_fmt: 要申请内存的图像的像素格式.
 align:   用于内存对齐的值.
 返回值：所申请的内存空间的总大小.如果是负值,表示申请失败.

 int av_image_fill_arrays(
     uint8_t *dst_data[4],
     int dst_linesize[4],
     const uint8_t *src,
     enum AVPixelFormat pix_fmt,
     int width,
     int height,
     int align);
 av_image_fill_arrays()函数自身不具备内存申请的功能,
 此函数类似于格式化已经申请的内存,即通过av_image_alloc()函数申请的内存空间.
 dst_data[4]:     [out]对申请的内存格式化为三个通道后,分别保存其地址.
 dst_linesize[4]: [out]格式化的内存的步长(即内存对齐后的宽度).
 *src:            [in]av_image_alloc()函数申请的内存地址.
 pix_fmt:         [in]申请 src内存时的像素格式.
 width:           [in]申请src内存时指定的宽度.
 height:          [in]申请scr内存时指定的高度.
 align:           [in]申请src内存时指定的对齐字节数.

// 在jni层或者其他cpp文件中创建线程是不行的
pthread_t audioReadDataThread, audioHandleDataThread;
// 创建线程
pthread_create(&audioReadDataThread, NULL, readData, audioWrapper->father);
pthread_create(&audioHandleDataThread, NULL, handleAudioData, NULL);
// 等待线程执行完
pthread_join(audioReadDataThread, NULL);
pthread_join(audioHandleDataThread, NULL);
// 取消线程
//pthread_cancel(audioReadDataThread);
//pthread_cancel(audioHandleDataThread);

pthread_t videoReadDataThread, videoHandleDataThread;
// 创建线程
pthread_create(&videoReadDataThread, NULL, readData, videoWrapper->father);
pthread_create(&videoHandleDataThread, NULL, handleVideoData, NULL);
// 等待线程执行完
pthread_join(videoReadDataThread, NULL);
pthread_join(videoHandleDataThread, NULL);
// 取消线程
//pthread_cancel(videoReadDataThread);
//pthread_cancel(videoHandleDataThread);
 */
namespace alexander {

    // 1 second of 48khz 32bit audio
#define MAX_AUDIO_FRAME_SIZE 192000

#define TYPE_UNKNOW -1
#define TYPE_AUDIO 1
#define TYPE_VIDEO 2

#define NEXT_READ_UNKNOW -1
#define NEXT_READ_QUEUE1 1
#define NEXT_READ_QUEUE2 2

#define NEXT_HANDLE_UNKNOW -1
#define NEXT_HANDLE_QUEUE1 1
#define NEXT_HANDLE_QUEUE2 2

#define MAX_AVPACKET_COUNT_AUDIO_HTTP 3000
#define MAX_AVPACKET_COUNT_VIDEO_HTTP 3000

#define MAX_AVPACKET_COUNT_AUDIO_LOCAL 100
#define MAX_AVPACKET_COUNT_VIDEO_LOCAL 100

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
        // 有些东西需要通过它去得到
        AVCodecParameters *avCodecParameters = NULL;
        // 解码器
        AVCodec *decoderAVCodec = NULL;
        // 编码器(没用到,因为是播放,所以不需要编码)
        AVCodec *encoderAVCodec = NULL;

        unsigned char *outBuffer1 = NULL;
        unsigned char *outBuffer2 = NULL;
        unsigned char *outBuffer3 = NULL;
        size_t outBufferSize = 0;

        int streamIndex = -1;
        // 总共读取了多少个AVPacket
        int readFramesCount = 0;
        // 总共处理了多少个AVPacket
        int handleFramesCount = 0;

        // C++中也可以使用list来做
        struct AVPacketQueue *queue1 = NULL;
        struct AVPacketQueue *queue2 = NULL;
        bool isReadQueue1Full = false;
        bool isReadQueue2Full = false;
        int nextRead = NEXT_READ_UNKNOW;
        int nextHandle = NEXT_HANDLE_UNKNOW;
        // 队列中最多保存多少个AVPacket
        int maxAVPacketsCount = 0;

        bool isStarted = false;
        bool isReading = false;
        bool isHandling = false;
        // 因为user所以pause
        bool isPausedForUser = false;
        // 因为cache所以pause
        bool isPausedForCache = false;
        // seek的初始化条件有没有完成,true表示完成
        bool seekToInit = false;

        // 单位: 秒
        int64_t duration = 0;
        // 单位: 秒
        int64_t timestamp = 0;
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

    struct AudioWrapper {
        struct Wrapper *father = NULL;
        SwrContext *swrContext = NULL;
        // 存储非压缩数据(视频对应RGB/YUV像素数据,音频对应PCM采样数据)
        AVFrame *decodedAVFrame = NULL;
        // 从音频源或视频源中得到
        // 采样率
        int srcSampleRate = 0;
        int dstSampleRate = 0;
        // 声道数
        int srcNbChannels = 0;
        // 由dstChannelLayout去获到
        int dstNbChannels = 0;
        int srcNbSamples = 0;
        int dstNbSamples = 0;
        // 由srcNbChannels能得到srcChannelLayout,也能由srcChannelLayout得到srcNbChannels
        int srcChannelLayout = 0;
        // 双声道输出
        int dstChannelLayout = 0;
        // 从音频源或视频源中得到(采样格式)
        enum AVSampleFormat srcAVSampleFormat = AV_SAMPLE_FMT_NONE;
        // 输出的采样格式16bit PCM
        enum AVSampleFormat dstAVSampleFormat = AV_SAMPLE_FMT_S16;
    };

    struct VideoWrapper {
        struct Wrapper *father = NULL;
        SwsContext *swsContext = NULL;
        // 从视频源中得到
        enum AVPixelFormat srcAVPixelFormat = AV_PIX_FMT_NONE;
        // 从原来的像素格式转换为想要的视频格式(可能应用于不需要播放视频的场景)
        // 播放时dstAVPixelFormat必须跟srcAVPixelFormat的值一样,不然画面有问题
        enum AVPixelFormat dstAVPixelFormat = AV_PIX_FMT_RGBA;
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
        int srcLineSize[4] = {NULL}, dstLineSize[4] = {NULL};
    };

    static struct AudioWrapper *audioWrapper = NULL;
    static struct VideoWrapper *videoWrapper = NULL;
    static bool isLocal = false;

    static char inFilePath[2048];
    static ANativeWindow *pANativeWindow = NULL;

    int getAVPacketFromQueue(struct AVPacketQueue *packet_queue, AVPacket *avpacket);

    char *getStrAVPixelFormat(AVPixelFormat format);

    // 已经不需要调用了
    void initAV() {
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avcodec_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        avformat_network_init();
        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
    }

    void initAudio() {
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            av_free(audioWrapper->father);
            audioWrapper->father = NULL;
        }
        if (audioWrapper != NULL) {
            av_free(audioWrapper);
            audioWrapper = NULL;
        }
        // 这里是先有儿子,再有父亲了.其实应该先构造父亲,再把父亲信息传给儿子.
        audioWrapper = (struct AudioWrapper *) av_mallocz(sizeof(struct AudioWrapper));
        memset(audioWrapper, 0, sizeof(struct AudioWrapper));
        audioWrapper->father = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(audioWrapper->father, 0, sizeof(struct Wrapper));

        audioWrapper->father->type = TYPE_AUDIO;
        audioWrapper->father->nextRead = NEXT_READ_QUEUE1;
        audioWrapper->father->nextHandle = NEXT_HANDLE_QUEUE1;
        if (isLocal) {
            audioWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_AUDIO_LOCAL;
        } else {
            audioWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_AUDIO_HTTP;
        }
        LOGD("initAudio() maxAVPacketsCount: %d\n", audioWrapper->father->maxAVPacketsCount);
        audioWrapper->father->streamIndex = -1;
        audioWrapper->father->readFramesCount = 0;
        audioWrapper->father->handleFramesCount = 0;
        audioWrapper->father->isStarted = false;
        audioWrapper->father->isReading = false;
        audioWrapper->father->isHandling = false;
        audioWrapper->father->isPausedForUser = false;
        audioWrapper->father->isPausedForCache = false;
        audioWrapper->father->seekToInit = false;
        audioWrapper->father->isReadQueue1Full = false;
        audioWrapper->father->isReadQueue2Full = false;
        audioWrapper->father->duration = -1;
        audioWrapper->father->timestamp = -1;
        audioWrapper->father->queue1 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        audioWrapper->father->queue2 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        memset(audioWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
        memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
        audioWrapper->father->queue1->allAVPacketsCount = 0;
        audioWrapper->father->queue1->allAVPacketsSize = 0;
        audioWrapper->father->queue2->allAVPacketsCount = 0;
        audioWrapper->father->queue2->allAVPacketsSize = 0;
        audioWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        audioWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;

        audioWrapper->dstSampleRate = 44100;
        audioWrapper->dstAVSampleFormat = AV_SAMPLE_FMT_S16;
        audioWrapper->dstChannelLayout = AV_CH_LAYOUT_STEREO;
    }

    void initVideo() {
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            av_free(videoWrapper->father);
            videoWrapper->father = NULL;
        }
        if (videoWrapper != NULL) {
            av_free(videoWrapper);
            videoWrapper = NULL;
        }

        videoWrapper = (struct VideoWrapper *) av_mallocz(sizeof(struct VideoWrapper));
        memset(videoWrapper, 0, sizeof(struct VideoWrapper));
        videoWrapper->father = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(videoWrapper->father, 0, sizeof(struct Wrapper));

        videoWrapper->father->type = TYPE_VIDEO;
        videoWrapper->father->nextRead = NEXT_READ_QUEUE1;
        videoWrapper->father->nextHandle = NEXT_HANDLE_QUEUE1;
        if (isLocal) {
            videoWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_VIDEO_LOCAL;
        } else {
            videoWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_VIDEO_HTTP;
        }
        LOGW("initVideo() maxAVPacketsCount: %d\n", videoWrapper->father->maxAVPacketsCount);
        videoWrapper->father->streamIndex = -1;
        videoWrapper->father->readFramesCount = 0;
        videoWrapper->father->handleFramesCount = 0;
        videoWrapper->father->isStarted = false;
        videoWrapper->father->isReading = false;
        videoWrapper->father->isHandling = false;
        videoWrapper->father->isPausedForUser = false;
        videoWrapper->father->isPausedForCache = false;
        videoWrapper->father->seekToInit = false;
        videoWrapper->father->isReadQueue1Full = false;
        videoWrapper->father->isReadQueue2Full = false;
        videoWrapper->father->duration = -1;
        videoWrapper->father->timestamp = -1;
        videoWrapper->father->queue1 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        videoWrapper->father->queue2 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        memset(videoWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
        memset(videoWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
        videoWrapper->father->queue1->allAVPacketsCount = 0;
        videoWrapper->father->queue1->allAVPacketsSize = 0;
        videoWrapper->father->queue2->allAVPacketsCount = 0;
        videoWrapper->father->queue2->allAVPacketsSize = 0;
        videoWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        videoWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;

        // Android支持的目标像素格式
        // AV_PIX_FMT_RGB32
        // AV_PIX_FMT_RGBA
        videoWrapper->dstAVPixelFormat = AV_PIX_FMT_RGBA;
    }

    int openAndFindAVFormatContextForAudio() {
        // AVFormatContext初始化,里面设置结构体的一些默认信息
        // 相当于Java中创建对象
        /***
        播放流媒体命令:
        ffplay -rtsp_transport tcp -max_delay 5000000 rtsp://mms.cnr.cn/cnr003?MzE5MTg0IzEjIzI5NjgwOQ==
        相应代码为:
        AVDictionary *avdic=NULL;
        char option_key[]="rtsp_transport";
        char option_value[]="tcp";
        av_dict_set(&avdic,option_key,option_value,0);
        char option_key2[]="max_delay";
        char option_value2[]="5000000";
        av_dict_set(&avdic,option_key2,option_value2,0);
        char url[]="rtsp://mms.cnr.cn/cnr003?MzE5MTg0IzEjIzI5NjgwOQ==";
        avformat_open_input(&pFormatCtx,url,NULL,&avdic);
         
        AVDictionary* options = NULL;
        //设置缓存大小,1080p可将值调大
        av_dict_set(&options, "buffer_size", "102400", 0);
        //以udp方式打开,如果以tcp方式打开将udp替换为tcp
        av_dict_set(&options, "rtsp_transport", "tcp", 0);
        //设置超时断开连接时间,单位微秒
        av_dict_set(&options, "stimeout", "2000000", 0);
        //设置最大时延
        av_dict_set(&options, "max_delay", "500000", 0);
         */
        audioWrapper->father->avFormatContext = avformat_alloc_context();
        if (audioWrapper->father->avFormatContext == NULL) {
            LOGE("audioWrapper->father->avFormatContext is NULL.\n");
            return -1;
        }
        // 获取基本的文件信息
        if (avformat_open_input(&audioWrapper->father->avFormatContext,
                                inFilePath, NULL, NULL) != 0) {
            LOGE("Couldn't open audio input stream.\n");
            return -1;
        }
        // 文件中的流信息
        if (avformat_find_stream_info(audioWrapper->father->avFormatContext, NULL) != 0) {
            LOGE("Couldn't find stream information.\n");
            return -1;
        }

        return 0;
    }

    int openAndFindAVFormatContextForVideo() {
        videoWrapper->father->avFormatContext = avformat_alloc_context();
        if (videoWrapper->father->avFormatContext == NULL) {
            LOGE("videoWrapper->father->avFormatContext is NULL.\n");
            return -1;
        }
        if (avformat_open_input(&videoWrapper->father->avFormatContext,
                                inFilePath, NULL, NULL) != 0) {
            LOGE("Couldn't open video input stream.\n");
            return -1;
        }
        if (avformat_find_stream_info(videoWrapper->father->avFormatContext, NULL) != 0) {
            LOGE("Couldn't find stream information.\n");
            return -1;
        }

        return 0;
    }

    /***
     找音视频流轨道
     */
    int findStreamIndexForAudio() {
        if (audioWrapper->father->avFormatContext == NULL) {
            return -1;
        }
        // audio stream index
        int streams = audioWrapper->father->avFormatContext->nb_streams;
        for (int i = 0; i < streams; i++) {
            // 得到AVCodecParameters
            audioWrapper->father->avCodecParameters =
                    audioWrapper->father->avFormatContext->streams[i]->codecpar;
            if (audioWrapper->father->avCodecParameters != NULL) {
                AVMediaType mediaType = audioWrapper->father->avCodecParameters->codec_type;
                if (mediaType == AVMEDIA_TYPE_AUDIO) {
                    audioWrapper->father->streamIndex = i;
                    break;
                }
            }
        }
        if (audioWrapper->father->streamIndex == -1) {
            LOGE("Didn't find audio stream.\n");
            return -1;
        } else {
            LOGI("audioStreamIndex: %d\n", audioWrapper->father->streamIndex);
            return 0;
        }
    }

    int findStreamIndexForVideo() {
        if (videoWrapper->father->avFormatContext == NULL) {
            return -1;
        }
        // video stream index
        int streams = videoWrapper->father->avFormatContext->nb_streams;
        for (int i = 0; i < streams; i++) {
            // 得到AVCodecParameters
            videoWrapper->father->avCodecParameters =
                    videoWrapper->father->avFormatContext->streams[i]->codecpar;
            if (videoWrapper->father->avCodecParameters != NULL) {
                AVMediaType mediaType = videoWrapper->father->avCodecParameters->codec_type;
                if (mediaType == AVMEDIA_TYPE_VIDEO) {
                    videoWrapper->father->streamIndex = i;
                    break;
                }
            }
        }

        if (videoWrapper->father->streamIndex == -1) {
            LOGE("Didn't find video stream.\n");
            return -1;
        } else {
            LOGI("videoStreamIndex: %d\n", videoWrapper->father->streamIndex);
            return 0;
        }
    }

    int findAndOpenAVCodecForAudio() {
        if (audioWrapper->father->avCodecParameters == NULL
            || audioWrapper->father->streamIndex == -1) {
            return -1;
        }
        // audio
        if (audioWrapper->father->streamIndex != -1) {
            // 获取音频解码器
            // 先通过AVCodecParameters找到AVCodec
            audioWrapper->father->decoderAVCodec = avcodec_find_decoder(
                    audioWrapper->father->avCodecParameters->codec_id);
            if (audioWrapper->father->decoderAVCodec != NULL) {
                // 获取解码器上下文
                // 再通过AVCodec得到AVCodecContext
                audioWrapper->father->avCodecContext = avcodec_alloc_context3(
                        audioWrapper->father->decoderAVCodec);
                if (audioWrapper->father->avCodecContext != NULL) {
                    // 关联操作
                    if (avcodec_parameters_to_context(
                            audioWrapper->father->avCodecContext,
                            audioWrapper->father->avCodecParameters) < 0) {
                        return -1;
                    } else {
                        // 打开AVCodec
                        if (avcodec_open2(
                                audioWrapper->father->avCodecContext,
                                audioWrapper->father->decoderAVCodec, NULL) != 0) {
                            LOGE("Could not open audio codec.\n");
                            return -1;
                        }
                    }
                }
            }
        }

        return 0;
    }

    int findAndOpenAVCodecForVideo() {
        if (videoWrapper->father->avCodecParameters == NULL
            || videoWrapper->father->streamIndex == -1) {
            return -1;
        }
        // video
        if (videoWrapper->father->streamIndex != -1) {
            videoWrapper->father->decoderAVCodec = avcodec_find_decoder(
                    videoWrapper->father->avCodecParameters->codec_id);
            if (videoWrapper->father->decoderAVCodec != NULL) {
                videoWrapper->father->avCodecContext = avcodec_alloc_context3(
                        videoWrapper->father->decoderAVCodec);
                if (videoWrapper->father->avCodecContext != NULL) {
                    if (avcodec_parameters_to_context(
                            videoWrapper->father->avCodecContext,
                            videoWrapper->father->avCodecParameters) < 0) {
                        return -1;
                    } else {
                        if (avcodec_open2(
                                videoWrapper->father->avCodecContext,
                                videoWrapper->father->decoderAVCodec, NULL) != 0) {
                            LOGE("Could not open video codec.\n");
                            return -1;
                        }
                    }
                }
            }
        }

        return 0;
    }

    int createSwrContent() {
        // src
        audioWrapper->srcSampleRate = audioWrapper->father->avCodecContext->sample_rate;
        audioWrapper->srcNbSamples = audioWrapper->father->avCodecContext->frame_size;
        audioWrapper->srcNbChannels = audioWrapper->father->avCodecContext->channels;
        audioWrapper->srcChannelLayout = audioWrapper->father->avCodecContext->channel_layout;
        audioWrapper->srcAVSampleFormat = audioWrapper->father->avCodecContext->sample_fmt;
        LOGI("---------------------------------\n");
        LOGI("srcSampleRate       : %d\n", audioWrapper->srcSampleRate);
        LOGI("srcNbChannels       : %d\n", audioWrapper->srcNbChannels);
        LOGI("srcAVSampleFormat   : %d\n", audioWrapper->srcAVSampleFormat);
        LOGI("srcNbSamples        : %d\n", audioWrapper->srcNbSamples);
        LOGI("srcChannelLayout1   : %d\n", audioWrapper->srcChannelLayout);
        // 有些视频从源视频中得到的channel_layout与使用函数得到的channel_layout结果是一样的
        // 但是还是要使用函数得到的channel_layout为好
        audioWrapper->srcChannelLayout = av_get_default_channel_layout(audioWrapper->srcNbChannels);
        LOGI("srcChannelLayout2   : %d\n", audioWrapper->srcChannelLayout);
        LOGI("---------------------------------\n");
        if (audioWrapper->srcNbSamples <= 0) {
            audioWrapper->srcNbSamples = 1024;
        }
        // dst
        // Android中跟音频有关的参数: dstSampleRate dstNbChannels 位宽
        // dstSampleRate,dstAVSampleFormat和dstChannelLayout指定
        // 然后通过下面处理后在Java端就能创建AudioTrack对象了
        // 不然像有些5声道,6声道就创建不了,因此没有声音
        audioWrapper->dstSampleRate = audioWrapper->srcSampleRate;
        audioWrapper->dstNbSamples = audioWrapper->srcNbSamples;
        audioWrapper->dstNbChannels = av_get_channel_layout_nb_channels(
                audioWrapper->dstChannelLayout);

        LOGI("dstSampleRate       : %d\n", audioWrapper->dstSampleRate);
        LOGI("dstNbChannels       : %d\n", audioWrapper->dstNbChannels);
        LOGI("dstAVSampleFormat   : %d\n", audioWrapper->dstAVSampleFormat);
        LOGI("dstNbSamples        : %d\n", audioWrapper->dstNbSamples);
        LOGI("---------------------------------\n");

        audioWrapper->swrContext = swr_alloc();
        swr_alloc_set_opts(audioWrapper->swrContext,
                           audioWrapper->dstChannelLayout,  // out_ch_layout
                           audioWrapper->dstAVSampleFormat, // out_sample_fmt
                           audioWrapper->dstSampleRate,     // out_sample_rate
                           audioWrapper->srcChannelLayout,  // in_ch_layout
                           audioWrapper->srcAVSampleFormat, // in_sample_fmt
                           audioWrapper->srcSampleRate,     // in_sample_rate
                           0,                               // log_offset
                           NULL);                           // log_ctx
        if (audioWrapper->swrContext == NULL) {
            LOGI("%s\n", "createSwrContent() swrContext is NULL");
            return -1;
        }

        int ret = swr_init(audioWrapper->swrContext);
        if (ret != 0) {
            LOGI("%s\n", "createSwrContent() swrContext swr_init failed");
            return -1;
        } else {
            LOGI("%s\n", "createSwrContent() swrContext swr_init success");
        }

        // 这个对应关系还不知道怎么弄
        int audioFormat = 2;
        switch (audioWrapper->dstAVSampleFormat) {
            case AV_SAMPLE_FMT_NONE: {
                break;
            }
            case AV_SAMPLE_FMT_U8: {
                break;
            }
            case AV_SAMPLE_FMT_S16: {
                audioFormat = 2;
                break;
            }
            case AV_SAMPLE_FMT_S32: {
                break;
            }
            case AV_SAMPLE_FMT_S64: {
                break;
            }
            case AV_SAMPLE_FMT_FLT: {
                break;
            }
            case AV_SAMPLE_FMT_DBL: {
                break;
            }
            case AV_SAMPLE_FMT_U8P: {
                break;
            }
            case AV_SAMPLE_FMT_S16P: {
                break;
            }
            case AV_SAMPLE_FMT_S32P: {
                break;
            }
            case AV_SAMPLE_FMT_S64P: {
                break;
            }
            case AV_SAMPLE_FMT_FLTP: {
                break;
            }
            case AV_SAMPLE_FMT_DBLP: {
                break;
            }
            case AV_SAMPLE_FMT_NB: {
                break;
            }
            default:
                break;
        }

        LOGI("%s\n", "createSwrContent() createAudioTrack start");
        createAudioTrack(audioWrapper->dstSampleRate,
                         audioWrapper->dstNbChannels,
                         audioFormat);
        LOGI("%s\n", "createSwrContent() createAudioTrack end");

        // avPacket ---> decodedAVFrame ---> dstAVFrame ---> 播放声音
        audioWrapper->decodedAVFrame = av_frame_alloc();
        // 16bit 44100 PCM 数据,16bit是2个字节
        audioWrapper->father->outBuffer1 = (unsigned char *) av_malloc(MAX_AUDIO_FRAME_SIZE);
        audioWrapper->father->outBufferSize = MAX_AUDIO_FRAME_SIZE;

        if (audioWrapper->father->avFormatContext->duration != AV_NOPTS_VALUE) {
            int64_t duration = audioWrapper->father->avFormatContext->duration + 5000;
            // 得到的是秒数
            audioWrapper->father->duration = duration / AV_TIME_BASE;
        }

        return 0;
    }

    int createSwsContext() {
        videoWrapper->srcWidth = videoWrapper->father->avCodecContext->width;
        videoWrapper->srcHeight = videoWrapper->father->avCodecContext->height;
        videoWrapper->srcAVPixelFormat = videoWrapper->father->avCodecContext->pix_fmt;
        LOGI("---------------------------------\n");
        LOGI("srcWidth            : %d\n", videoWrapper->srcWidth);
        LOGI("srcHeight           : %d\n", videoWrapper->srcHeight);
        LOGI("srcAVPixelFormat    : %d %s\n",
             videoWrapper->srcAVPixelFormat, getStrAVPixelFormat(videoWrapper->srcAVPixelFormat));
        LOGI("dstAVPixelFormat    : %d %s\n",
             videoWrapper->dstAVPixelFormat, getStrAVPixelFormat(videoWrapper->dstAVPixelFormat));
        videoWrapper->dstWidth = videoWrapper->srcWidth;
        videoWrapper->dstHeight = videoWrapper->srcHeight;
        videoWrapper->srcArea = videoWrapper->srcWidth * videoWrapper->srcHeight;
        videoWrapper->dstArea = videoWrapper->srcArea;

        // decodedAVFrame为解码后的数据
        // avPacket ---> decodedAVFrame ---> rgbAVFrame ---> 渲染画面
        videoWrapper->decodedAVFrame = av_frame_alloc();
        videoWrapper->rgbAVFrame = av_frame_alloc();

        int imageGetBufferSize = av_image_get_buffer_size(
                videoWrapper->dstAVPixelFormat, videoWrapper->srcWidth, videoWrapper->srcHeight, 1);
        videoWrapper->father->outBufferSize = imageGetBufferSize * sizeof(unsigned char);
        LOGI("imageGetBufferSize1 : %d\n", imageGetBufferSize);
        LOGI("imageGetBufferSize2 : %d\n", videoWrapper->father->outBufferSize);
        videoWrapper->father->outBuffer1 =
                (unsigned char *) av_malloc(videoWrapper->father->outBufferSize);
        int imageFillArrays = av_image_fill_arrays(
                videoWrapper->rgbAVFrame->data,
                videoWrapper->rgbAVFrame->linesize,
                videoWrapper->father->outBuffer1,
                videoWrapper->dstAVPixelFormat,
                videoWrapper->srcWidth,
                videoWrapper->srcHeight,
                1);
        if (imageFillArrays < 0) {
            LOGI("imageFillArrays     : %d\n", imageFillArrays);
            return -1;
        }
        // 由于解码出来的帧格式不是RGBA,在渲染之前需要进行格式转换
        // 现在swsContext知道程序员想要得到什么样的像素格式了
        videoWrapper->swsContext = sws_getContext(
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->srcAVPixelFormat,
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->dstAVPixelFormat,
                SWS_BICUBIC,// SWS_BICUBIC SWS_BILINEAR 使用什么转换算法
                NULL, NULL, NULL);
        if (videoWrapper->swsContext == NULL) {
            LOGI("%s\n", "videoSwsContext is NULL.");
            return -1;
        }
        LOGI("---------------------------------\n");

        if (videoWrapper->father->avFormatContext->duration != AV_NOPTS_VALUE) {
            int64_t duration = videoWrapper->father->avFormatContext->duration + 5000;
            // 得到的是秒数
            videoWrapper->father->duration = duration / AV_TIME_BASE;
        }

        return 0;
    }

    int putAVPacketToQueue(struct AVPacketQueue *packet_queue,
                           AVPacket *avpacket) {
        // 需要把AVPacket类型构造成AVPacketList类型,因此先要构造一个AVPacketList指针
        AVPacketList *avpacket_list = NULL;
        avpacket_list = (AVPacketList *) av_malloc(sizeof(AVPacketList));
        if (!avpacket_list) {
            return -1;
        }
        avpacket_list->pkt = *avpacket;
        avpacket_list->next = NULL;

        // 第一次为NULL
        if (!packet_queue->lastAVPacketList) {
            packet_queue->firstAVPacketList = avpacket_list;
            /*fLOGI(stdout,
                    "packet_queue->first_pkt->pkt.pos = %ld\n",
                    packet_queue->firstAVPacketList->pkt.pos);*/
        } else {
            packet_queue->lastAVPacketList->next = avpacket_list;
        }
        packet_queue->lastAVPacketList = avpacket_list;
        packet_queue->allAVPacketsCount++;
        packet_queue->allAVPacketsSize += avpacket->size;
        //packet_queue->allAVPacketsSize += avpacket_list->pkt.size;

        return 0;
    }

    int getAVPacketFromQueue(struct AVPacketQueue *packet_queue,
                             AVPacket *avpacket) {
        AVPacketList *avpacket_list = packet_queue->firstAVPacketList;
        if (avpacket_list) {
            packet_queue->firstAVPacketList = avpacket_list->next;
            if (!packet_queue->firstAVPacketList) {
                packet_queue->lastAVPacketList = NULL;
                // LOGI("%s\n", "没有数据了");
            }
            *avpacket = avpacket_list->pkt;
            packet_queue->allAVPacketsCount--;
            packet_queue->allAVPacketsSize -= avpacket->size;
            //packet_queue->allAVPacketsSize -= avpacket_list->pkt.size;

            av_free(avpacket_list);
            return 0;
        }

        return -1;
    }

    double TIME_DIFFERENCE = 1.000000;// 0.180000
    double audioTimeDifference = 0;
    double videoTimeDifference = 0;
    double totalTimeDifference = 0;
    long totalTimeDifferenceCount = 0;
    long preProgress = 0;
    long sleep = 0;
    long step = 0;
    bool needLocalLog = false;

    void *readData(void *opaque) {
        if (opaque == NULL) {
            return NULL;
        }
        Wrapper *wrapper = NULL;
        int *type = (int *) opaque;
        if (*type == TYPE_AUDIO) {
#ifdef USE_AUDIO
            wrapper = audioWrapper->father;
#endif
        } else {
#ifdef USE_VIDEO
            wrapper = videoWrapper->father;
#endif
        }
        // Wrapper *wrapper = (Wrapper *) opaque;
        if (wrapper == NULL) {
            LOGI("%s\n", "wrapper is NULL");
            return NULL;
        }

        int hours, mins, seconds;
        // 得到的是秒数
        seconds = getDuration();
        mins = seconds / 60;
        seconds %= 60;
        hours = mins / 60;
        mins %= 60;
        // 00:54:16
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("%s\n", "readData() audio start");
            // 单位: 秒
            LOGD("readData() audio seconds: %d\n", (int) audioWrapper->father->duration);
            LOGD("readData() audio          %02d:%02d:%02d\n", hours, mins, seconds);
        } else {
            LOGW("%s\n", "readData() video start");
            LOGW("readData() video seconds: %d\n", (int) videoWrapper->father->duration);
            LOGW("readData() video          %02d:%02d:%02d\n", hours, mins, seconds);
        }

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *dstAVPacket = av_packet_alloc();

        int readFrame = -1;
        wrapper->isReading = true;
        for (;;) {
            // seekTo
            if (wrapper != NULL && wrapper->timestamp != -1) {
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("%s\n", "readData() audio seek start");
                } else {
                    LOGW("%s\n", "readData() video seek start");
                }

                if (!wrapper->seekToInit) {
                    pthread_mutex_lock(&wrapper->readLockMutex);
                    pthread_cond_wait(&wrapper->readLockCondition, &wrapper->readLockMutex);
                    pthread_mutex_unlock(&wrapper->readLockMutex);
                }
                wrapper->seekToInit = false;

                AVStream *stream =
                        wrapper->avFormatContext->streams[wrapper->streamIndex];
                av_seek_frame(
                        wrapper->avFormatContext,
                        wrapper->streamIndex,
                        //wrapper->timestamp * AV_TIME_BASE,
                        wrapper->timestamp / av_q2d(stream->time_base),
                        AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);

                // 千万不能调用
                // avcodec_flush_buffers(wrapper->avCodecContext);

                preProgress = 0;
                wrapper->timestamp = -1;
                av_packet_unref(srcAVPacket);
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("%s\n", "readData() audio seek end");
                } else {
                    LOGW("%s\n", "readData() video seek end");
                }
                if (wrapper->type == TYPE_AUDIO) {
#ifdef USE_VIDEO
                    if (videoWrapper != NULL && videoWrapper->father != NULL) {
                        while (videoWrapper->father->timestamp != -1) {
                            audioSleep(1);
                        }
                    }
#endif
                } else {
#ifdef USE_AUDIO
                    if (audioWrapper != NULL && audioWrapper->father != NULL) {
                        while (audioWrapper->father->timestamp != -1) {
                            videoSleep(1);
                        }
                    }
#endif
                }
            }// seekTo end

            // exit
            if (!wrapper->isReading) {
                // for (;;) end
                break;
            }

            // 读一帧,如果是想要的流,那么break
            // 如果讲到文件尾,那么
            while (1) {
                // exit
                if (!wrapper->isReading) {
                    // while(1) end
                    break;
                }

                // 读取一帧压缩数据放到avPacket
                // 0 if OK, < 0 on error or end of file
                // 有时读一次跳出,有时读多次跳出
                //LOGD("readData() av_read_frame\n");
                readFrame = av_read_frame(wrapper->avFormatContext, srcAVPacket);
                /*try {
                } catch (...) {
                    LOGE("readData() av_read_frame error\n");
                    continue;
                }*/
                //LOGI("readFrame           : %d\n", readFrame);
                if (readFrame < 0) {
                    if (readFrame == AVERROR_EOF) {
                        wrapper->isReading = false;

                        if (wrapper->type == TYPE_AUDIO) {
                            LOGF("readData() audio AVERROR_EOF readFrame: %d\n", readFrame);
                        } else {
                            LOGF("readData() video AVERROR_EOF readFrame: %d\n", readFrame);
                        }
                        if (wrapper->queue1->allAVPacketsCount > 0) {
                            wrapper->isReadQueue1Full = true;
                        }
                        if (wrapper->queue2->allAVPacketsCount > 0) {
                            wrapper->isReadQueue2Full = true;
                        }

                        // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                        if ((wrapper->isReadQueue1Full
                             && !wrapper->isReadQueue2Full
                             && wrapper->queue1->allAVPacketsCount > 0
                             && wrapper->queue2->allAVPacketsCount == 0)
                            || wrapper->isPausedForCache) {
                            if (wrapper->type == TYPE_AUDIO) {
                                LOGD("readData() audio signal() handleLockCondition break\n");
                            } else {
                                LOGW("readData() video signal() handleLockCondition break\n");
                            }
                            // 唤醒线程
                            pthread_mutex_lock(&wrapper->handleLockMutex);
                            pthread_cond_signal(&wrapper->handleLockCondition);
                            pthread_mutex_unlock(&wrapper->handleLockMutex);
                        }

                        // while(1) end
                        break;
                    }
                    /*if (wrapper->type == TYPE_AUDIO) {
                        LOGF("readData() audio readFrame: %d\n", readFrame);
                    } else {
                        LOGF("readData() video readFrame: %d\n", readFrame);
                    }*/
                    continue;
                }

                if (srcAVPacket->stream_index != wrapper->streamIndex) {
                    // 遇到其他流时释放
                    av_packet_unref(srcAVPacket);
                    continue;
                }

                break;
            }// while(1) end

            // exit
            if (!wrapper->isReading) {
                // for (;;) end
                break;
            }

            /*if (wrapper->type == TYPE_VIDEO) {
                LOGF("readData() video data: %u, %u, %u, %u, %u\n",
                     srcAVPacket->data[0],
                     srcAVPacket->data[1],
                     srcAVPacket->data[2],
                     srcAVPacket->data[3],
                     srcAVPacket->data[4],
                     srcAVPacket->data[5]);
            }*/

            wrapper->readFramesCount++;
            // 非常非常非常重要
            // av_copy_packet(dstAVPacket, srcAVPacket);
            av_packet_ref(dstAVPacket, srcAVPacket);

            if (wrapper->nextRead == NEXT_READ_QUEUE1
                && !wrapper->isReadQueue1Full) {
                // 保存到队列去,然后取出来进行解码播放
                putAVPacketToQueue(wrapper->queue1, dstAVPacket);
                if (wrapper->queue1->allAVPacketsCount == wrapper->maxAVPacketsCount) {
                    wrapper->isReadQueue1Full = true;
                    // queue1满了,接着存到queue2去
                    wrapper->nextRead = NEXT_READ_QUEUE2;
                    if (isLocal && needLocalLog) {
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("readData() audio Queue1 満了\n");
                            LOGD("readData() audio Queue1 Size : %ld\n",
                                 wrapper->queue1->allAVPacketsSize);
                            LOGD("readData() audio signal() handleLockCondition\n");
                        } else {
                            LOGW("readData() video Queue1 満了\n");
                            LOGW("readData() video Queue1 Size : %ld\n",
                                 wrapper->queue1->allAVPacketsSize);
                            LOGW("readData() video signal() handleLockCondition\n");
                        }
                    }
                    // 唤醒"解码"线程(几种情况需要唤醒:开始播放时,cache缓存时)
                    pthread_mutex_lock(&wrapper->handleLockMutex);
                    pthread_cond_signal(&wrapper->handleLockCondition);
                    pthread_mutex_unlock(&wrapper->handleLockMutex);
                }
            } else if (wrapper->nextRead == NEXT_READ_QUEUE2
                       && !wrapper->isReadQueue2Full) {
                putAVPacketToQueue(wrapper->queue2, dstAVPacket);
                if (wrapper->queue2->allAVPacketsCount == wrapper->maxAVPacketsCount) {
                    wrapper->isReadQueue2Full = true;
                    wrapper->nextRead = NEXT_READ_QUEUE1;
                    if (wrapper->type == TYPE_AUDIO) {
                        if (isLocal && needLocalLog) {
                            LOGD("readData() audio Queue2 満了\n");
                            LOGD("readData() audio Queue2 Size : %ld\n",
                                 wrapper->queue2->allAVPacketsSize);
                        }
                        if (audioWrapper != NULL
                            && audioWrapper->father != NULL
                            && !audioWrapper->father->isPausedForCache) {
                            if (srcAVPacket->data) {
                                av_packet_unref(srcAVPacket);
                            }
                            continue;
                        }
                        if (isLocal && needLocalLog) {
                            LOGD("readData() audio signal() handleLockCondition\n");
                        }
                    } else {
                        if (isLocal && needLocalLog) {
                            LOGW("readData() video Queue2 満了\n");
                            LOGW("readData() video Queue2 Size : %ld\n",
                                 wrapper->queue2->allAVPacketsSize);
                            LOGW("readData() video signal() handleLockCondition\n");
                        }
                    }
                    // 唤醒"解码"线程
                    pthread_mutex_lock(&wrapper->handleLockMutex);
                    pthread_cond_signal(&wrapper->handleLockCondition);
                    pthread_mutex_unlock(&wrapper->handleLockMutex);
                }
            } else if (wrapper->isReadQueue1Full
                       && wrapper->isReadQueue2Full) {
                // 两个队列都满的话,就进行等待
                if (isLocal && needLocalLog) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("readData() audio Queue1和Queue2都満了,好开心( ^_^ )\n");
                        LOGD("readData() audio wait() readLockCondition start\n");
                    } else {
                        LOGW("readData() video Queue1和Queue2都満了,好开心( ^_^ )\n");
                        LOGW("readData() video wait() readLockCondition start\n");
                    }
                }
                // 读数据线程等待
                pthread_mutex_lock(&wrapper->readLockMutex);
                pthread_cond_wait(&wrapper->readLockCondition, &wrapper->readLockMutex);
                pthread_mutex_unlock(&wrapper->readLockMutex);
                if (isLocal && needLocalLog) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("readData() audio wait() readLockCondition end\n");
                    } else {
                        LOGW("readData() video wait() readLockCondition end\n");
                    }
                }

                // 保存"等待前的一帧"
                if (wrapper->nextRead == NEXT_READ_QUEUE1
                    && !wrapper->isReadQueue1Full) {
                    if (isLocal && needLocalLog) {
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("readData() audio next QUEUE1\n");
                        } else {
                            LOGW("readData() video next QUEUE1\n");
                        }
                    }
                    putAVPacketToQueue(wrapper->queue1, dstAVPacket);
                } else if (wrapper->nextRead == NEXT_READ_QUEUE2
                           && !wrapper->isReadQueue2Full) {
                    if (isLocal && needLocalLog) {
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("readData() audio next QUEUE2\n");
                        } else {
                            LOGW("readData() video next QUEUE2\n");
                        }
                    }
                    putAVPacketToQueue(wrapper->queue2, dstAVPacket);
                }
            }

            av_packet_unref(srcAVPacket);
        }// for(;;) end

        av_packet_unref(srcAVPacket);

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("readData() audio readFramesCount          : %d\n",
                 wrapper->readFramesCount);
            LOGD("%s\n", "readData() audio end");
        } else {
            LOGW("readData() video readFramesCount          : %d\n",
                 wrapper->readFramesCount);
            LOGW("%s\n", "readData() video end");
        }
        return NULL;
    }

    void *handleAudioData(void *opaque) {
        LOGD("%s\n", "handleAudioData() start");

        // 线程等待
        LOGD("handleAudioData() wait() start\n");
        pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
        pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                          &audioWrapper->father->handleLockMutex);
        pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
        LOGD("handleAudioData() wait() end\n");

        int ret = 0, out_buffer_size = 0;
        AVStream *stream =
                audioWrapper->father->avFormatContext->streams[audioWrapper->father->streamIndex];
        // 压缩数据(原始数据)
        AVPacket *avPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = audioWrapper->decodedAVFrame;
        audioWrapper->father->isHandling = true;
        LOGD("handleAudioData() for (;;) start\n");
        for (;;) {
            // 暂停装置
            if (audioWrapper->father->isPausedForUser
                || audioWrapper->father->isPausedForCache) {
                bool isPausedForUser = audioWrapper->father->isPausedForUser;
                if (isPausedForUser) {
                    LOGD("handleAudioData() wait() User  start\n");
                } else {
                    LOGD("handleAudioData() wait() Cache start\n");
                }
                pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                                  &audioWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                if (isPausedForUser) {
                    LOGD("handleAudioData() wait() User  end\n");
                } else {
                    LOGD("handleAudioData() wait() Cache end\n");
                }
            }

            if (!audioWrapper->father->isHandling) {
                // for (;;) end
                break;
            }

            memset(avPacket, 0, sizeof(*avPacket));
            if (audioWrapper->father->nextHandle == NEXT_HANDLE_QUEUE1
                && audioWrapper->father->isReadQueue1Full
                && audioWrapper->father->queue1->allAVPacketsCount > 0) {
                audioWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(audioWrapper->father->queue1, avPacket);
                if (audioWrapper->father->queue1->allAVPacketsCount == 0) {
                    memset(audioWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
                    audioWrapper->father->isReadQueue1Full = false;
                    audioWrapper->father->nextHandle = NEXT_HANDLE_QUEUE2;
                    if (isLocal && needLocalLog) {
                        LOGD("handleAudioData() Queue1 用完了\n");
                        LOGD("handleAudioData() Queue2 isReadQueue2Full : %d\n",
                             audioWrapper->father->isReadQueue2Full);
                        LOGD("handleAudioData() Queue2 allAVPacketsCount: %d\n",
                             audioWrapper->father->queue2->allAVPacketsCount);
                    }
                    if (audioWrapper->father->isReading) {
                        if (isLocal && needLocalLog) {
                            LOGD("handleAudioData() signal() readLockCondition\n");
                        }
                        pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                        pthread_cond_signal(&audioWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                    }
                }
            } else if (audioWrapper->father->nextHandle == NEXT_HANDLE_QUEUE2
                       && audioWrapper->father->isReadQueue2Full
                       && audioWrapper->father->queue2->allAVPacketsCount > 0) {
                audioWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(audioWrapper->father->queue2, avPacket);
                if (audioWrapper->father->queue2->allAVPacketsCount == 0) {
                    memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
                    audioWrapper->father->isReadQueue2Full = false;
                    audioWrapper->father->nextHandle = NEXT_HANDLE_QUEUE1;
                    if (isLocal && needLocalLog) {
                        LOGD("handleAudioData() Queue2 用完了\n");
                        LOGD("handleAudioData() Queue1 isReadQueue1Full : %d\n",
                             audioWrapper->father->isReadQueue1Full);
                        LOGD("handleAudioData() Queue1 allAVPacketsCount: %d\n",
                             audioWrapper->father->queue1->allAVPacketsCount);
                    }
                    if (audioWrapper->father->isReading) {
                        if (isLocal && needLocalLog) {
                            LOGD("handleAudioData() signal() readLockCondition\n");
                        }
                        pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                        pthread_cond_signal(&audioWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                    }
                }
            } else if (audioWrapper->father->isReading
                       && !audioWrapper->father->isReadQueue1Full
                       && !audioWrapper->father->isReadQueue2Full) {
                onPaused();
                // 音频Cache引起的暂停
#ifdef USE_VIDEO
                // 让视频也同时暂停
                if (videoWrapper != NULL && videoWrapper->father != NULL) {
                    videoWrapper->father->isPausedForCache = true;
                }
#endif
                audioWrapper->father->isPausedForCache = true;
                LOGE("handleAudioData() wait() Cache start\n");
                pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                                  &audioWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                LOGE("handleAudioData() wait() Cache end\n");
                audioWrapper->father->isPausedForCache = false;
                // 通知视频结束暂停
#ifdef USE_VIDEO
                if (videoWrapper != NULL && videoWrapper->father != NULL) {
                    videoWrapper->father->isPausedForCache = false;
                    pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                    pthread_cond_signal(&videoWrapper->father->handleLockCondition);
                    pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                }
#endif
                onPlayed();
                continue;
            } else if (!audioWrapper->father->isReading
                       && audioWrapper->father->queue1->allAVPacketsCount == 0
                       && audioWrapper->father->queue2->allAVPacketsCount == 0) {
                audioWrapper->father->isHandling = false;
                // for (;;) end
                break;
            }

            if (!avPacket) {
                if (!audioWrapper->father->isReading
                    && audioWrapper->father->queue1->allAVPacketsCount == 0
                    && audioWrapper->father->queue2->allAVPacketsCount == 0) {
                    // for (;;) end
                    break;
                }
                continue;
            }

            ret = avcodec_send_packet(audioWrapper->father->avCodecContext, avPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    continue;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    LOGE("audio 发送数据包到解码器时出错 %d", ret);
                    audioWrapper->father->isHandling = false;
                    break;
                case 0:
                default:
                    break;
            }
            if (!audioWrapper->father->isHandling) {
                // for (;;) end
                break;
            }
            while (1) {
                ret = avcodec_receive_frame(audioWrapper->father->avCodecContext, decodedAVFrame);
                switch (ret) {
                    // 输出是不可用的,必须发送新的输入
                    case AVERROR(EAGAIN):
                        break;
                        // codec打不开,或者是一个encoder
                    case AVERROR(EINVAL):
                        // 已经完全刷新,不会再有输出帧了
                    case AVERROR_EOF:
                        audioWrapper->father->isHandling = false;
                        break;
                    case 0: {
                        // 成功,返回一个输出帧
                        // 9.转换音频
                        ret = swr_convert(
                                audioWrapper->swrContext,
                                // 输出缓冲区
                                &audioWrapper->father->outBuffer1,
                                // 每通道采样的可用空间量
                                MAX_AUDIO_FRAME_SIZE,
                                // 输入缓冲区
                                (const uint8_t **) decodedAVFrame->data,
                                // 一个通道中可用的输入采样数量
                                decodedAVFrame->nb_samples);
                        if (ret < 0) {
                            LOGE("转换时出错 %d", ret);
                        } else {
                            if (!audioWrapper->father->isStarted) {
                                audioWrapper->father->isStarted = true;
                            }

#ifdef USE_VIDEO
                            while (videoWrapper != NULL
                                   && videoWrapper->father != NULL
                                   && !videoWrapper->father->isStarted) {
                                // usleep(1000);
                                audioSleep(1);
                            }
                            audioTimeDifference =
                                    decodedAVFrame->pts * av_q2d(stream->time_base);
                            //LOGD("handleAudioData() nowPts : %lf\n", audioTimeDifference);
#endif

                            // 获取给定音频参数所需的缓冲区大小
                            out_buffer_size = av_samples_get_buffer_size(
                                    NULL,
                                    // 输出的声道个数
                                    audioWrapper->dstNbChannels,
                                    // 一个通道中音频采样数量
                                    decodedAVFrame->nb_samples,
                                    // 输出采样格式16bit
                                    audioWrapper->dstAVSampleFormat,
                                    // 缓冲区大小对齐（0 = 默认值,1 = 不对齐）
                                    1);

                            write(audioWrapper->father->outBuffer1, 0, out_buffer_size);
                        }
                        break;
                    }
                    default:
                        // 合法的解码错误
                        LOGE("从解码器接收帧时出错 %d", ret);
                        break;
                }
                break;
            }//while(1) end
            av_packet_unref(avPacket);
        }//for(;;) end
        LOGD("handleAudioData() for (;;) end\n");

        av_packet_unref(avPacket);
        avPacket = NULL;

        LOGD("handleAudioData() audio handleFramesCount : %d\n",
             audioWrapper->father->handleFramesCount);
        if (audioWrapper->father->readFramesCount == audioWrapper->father->handleFramesCount) {
            LOGD("%s\n", "audioWrapper() audio正常播放完毕");
        }
        stop();
        closeAudio();

        LOGD("%s\n", "handleAudioData() end");
        return NULL;
    }

    void *handleVideoData(void *opaque) {
        LOGW("%s\n", "handleVideoData() start");

        // 线程等待
        LOGW("handleVideoData() wait() start\n");
        pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
        pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                          &videoWrapper->father->handleLockMutex);
        pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
        LOGW("handleVideoData() wait() end\n");

        audioTimeDifference = 0;
        videoTimeDifference = 0;
        totalTimeDifference = 0;
        totalTimeDifferenceCount = 0;

        sleep = 0;
        step = 0;
        long tempSleep = 0;
        preProgress = 0;
        int64_t prePts = 0;
        int64_t nowPts = 0;
        double timeDifference = 0;
        int ret = 0;
        bool onlyOne = true;

        LOGW("handleVideoData() ANativeWindow_setBuffersGeometry() start\n");
        // 2.设置缓冲区的属性（宽、高、像素格式）,像素格式要和SurfaceView的像素格式一直
        ANativeWindow_setBuffersGeometry(pANativeWindow,
                                         videoWrapper->srcWidth,
                                         videoWrapper->srcHeight,
                                         WINDOW_FORMAT_RGBA_8888);
        LOGW("handleVideoData() ANativeWindow_setBuffersGeometry() end\n");
        // 绘制时的缓冲区
        ANativeWindow_Buffer outBuffer;

        AVStream *stream =
                videoWrapper->father->avFormatContext->streams[videoWrapper->father->streamIndex];
        // 必须创建(存放压缩数据,如H264)
        AVPacket *avPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = videoWrapper->decodedAVFrame;
        videoWrapper->father->isHandling = true;
        LOGW("handleVideoData() for (;;) start\n");
        for (;;) {
            // 暂停装置
            if (videoWrapper->father->isPausedForUser
                || videoWrapper->father->isPausedForCache) {
                bool isPausedForUser = videoWrapper->father->isPausedForUser;
                if (isPausedForUser) {
                    LOGW("handleVideoData() wait() User  start\n");
                } else {
                    LOGW("handleVideoData() wait() Cache start\n");
                }
                pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                                  &videoWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                if (isPausedForUser) {
                    LOGW("handleVideoData() wait() User  end\n");
                } else {
                    LOGW("handleVideoData() wait() Cache end\n");
                }
            }

            if (!videoWrapper->father->isHandling) {
                // for (;;) end
                break;
            }

            memset(avPacket, 0, sizeof(*avPacket));
            if (videoWrapper->father->nextHandle == NEXT_HANDLE_QUEUE1
                && videoWrapper->father->isReadQueue1Full
                && videoWrapper->father->queue1->allAVPacketsCount > 0) {
                videoWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(videoWrapper->father->queue1, avPacket);
                if (videoWrapper->father->queue1->allAVPacketsCount == 0) {
                    memset(videoWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
                    videoWrapper->father->isReadQueue1Full = false;
                    videoWrapper->father->nextHandle = NEXT_HANDLE_QUEUE2;
                    if (isLocal && needLocalLog) {
                        LOGW("handleVideoData() Queue1 用完了\n");
                        LOGW("handleVideoData() Queue2 isReadQueue2Full : %d\n",
                             videoWrapper->father->isReadQueue2Full);
                        LOGW("handleVideoData() Queue2 allAVPacketsCount: %d\n",
                             videoWrapper->father->queue2->allAVPacketsCount);
                    }
                    if (videoWrapper->father->isReading) {
                        if (isLocal && needLocalLog) {
                            LOGW("handleVideoData() signal() readLockCondition\n");
                        }
                        pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                        pthread_cond_signal(&videoWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
                    }
                }
            } else if (videoWrapper->father->nextHandle == NEXT_HANDLE_QUEUE2
                       && videoWrapper->father->isReadQueue2Full
                       && videoWrapper->father->queue2->allAVPacketsCount > 0) {
                videoWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(videoWrapper->father->queue2, avPacket);
                if (videoWrapper->father->queue2->allAVPacketsCount == 0) {
                    memset(videoWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
                    videoWrapper->father->isReadQueue2Full = false;
                    videoWrapper->father->nextHandle = NEXT_HANDLE_QUEUE1;
                    if (isLocal && needLocalLog) {
                        LOGW("handleVideoData() Queue2 用完了\n");
                        LOGW("handleVideoData() Queue1 isReadQueue1Full : %d\n",
                             videoWrapper->father->isReadQueue1Full);
                        LOGW("handleVideoData() Queue1 allAVPacketsCount: %d\n",
                             videoWrapper->father->queue1->allAVPacketsCount);
                    }
                    if (videoWrapper->father->isReading) {
                        if (isLocal && needLocalLog) {
                            LOGW("handleVideoData() signal() readLockCondition\n");
                        }
                        pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                        pthread_cond_signal(&videoWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
                    }
                }
            } else if (videoWrapper->father->isReading
                       && !videoWrapper->father->isReadQueue1Full
                       && !videoWrapper->father->isReadQueue2Full) {
                onPaused();
                // 视频Cache引起的暂停
#ifdef USE_AUDIO
                // 让音频也同时暂停
                if (audioWrapper != NULL && audioWrapper->father != NULL) {
                    audioWrapper->father->isPausedForCache = true;
                }
#endif
                // 自身暂停
                videoWrapper->father->isPausedForCache = true;
                LOGE("handleVideoData() wait() Cache start\n");
                pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                                  &videoWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                LOGE("handleVideoData() wait() Cache end\n");
                videoWrapper->father->isPausedForCache = false;
#ifdef USE_AUDIO
                // 通知音频结束暂停
                if (audioWrapper != NULL && audioWrapper->father != NULL) {
                    audioWrapper->father->isPausedForCache = false;
                    pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                    pthread_cond_signal(&audioWrapper->father->handleLockCondition);
                    pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                }
#endif
                onPlayed();
                continue;
            } else if (!videoWrapper->father->isReading
                       && videoWrapper->father->queue1->allAVPacketsCount == 0
                       && videoWrapper->father->queue2->allAVPacketsCount == 0) {
                videoWrapper->father->isHandling = false;
                onProgressUpdated(videoWrapper->father->duration);
                LOGW("handleVideoData() 电影结束,散场\n");
                // for (;;) end
                break;
            }

            if (!avPacket) {
                if (!videoWrapper->father->isReading
                    && videoWrapper->father->queue1->allAVPacketsCount == 0
                    && videoWrapper->father->queue2->allAVPacketsCount == 0) {
                    // for (;;) end
                    break;
                }
                continue;
            }

            ret = avcodec_send_packet(videoWrapper->father->avCodecContext, avPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    continue;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    LOGE("video 发送数据包到解码器时出错 %d", ret);
                    videoWrapper->father->isHandling = false;
                    break;
                case 0:
                default:
                    break;
            }
            if (!videoWrapper->father->isHandling) {
                // for (;;) end
                break;
            }
            while (1) {
                ret = avcodec_receive_frame(videoWrapper->father->avCodecContext,
                                            decodedAVFrame);
                switch (ret) {
                    case AVERROR(EAGAIN):
                        break;
                    case AVERROR(EINVAL):
                    case AVERROR_EOF:
                        videoWrapper->father->isHandling = false;
                        break;
                    case 0: {
                        if (!videoWrapper->father->isStarted) {
                            videoWrapper->father->isStarted = true;
                        }
#ifdef USE_AUDIO
                        while (audioWrapper != NULL
                               && audioWrapper->father != NULL
                               && !audioWrapper->father->isStarted) {
                            videoSleep(1);
                        }
                        if (audioWrapper != NULL
                            && audioWrapper->father != NULL
                            && audioWrapper->father->isStarted
                            && videoWrapper->father->isStarted
                            && onlyOne) {
                            LOGW("handleVideoData() 音视频都已经准备好,开始播放!!!\n");
                            onlyOne = false;
                            // 回调(通知到java层)
                            onPlayed();
                        }
#endif
                        nowPts = decodedAVFrame->pts;
                        timeDifference = (nowPts - prePts) * av_q2d(stream->time_base);
                        prePts = nowPts;
#ifdef USE_AUDIO
                        // video nowPts    : 258.600000
                        // video nowPts    : 274.800000
                        // video - audio   : 16.773333
                        videoTimeDifference =
                                nowPts * av_q2d(stream->time_base);
                        //LOGD("handleVideoData() audio nowPts    : %lf\n", audioTimeDifference);
                        //LOGW("handleVideoData() video nowPts    : %lf\n", videoTimeDifference);
                        // 显示时间进度
                        long progress = (long) videoTimeDifference;
                        //LOGW("handleVideoData() progress    : %ld\n", progress);
                        //LOGW("handleVideoData() preProgress : %ld\n", preProgress);
                        if (progress > preProgress) {
                            preProgress = progress;
                            onProgressUpdated(progress);
                        }
                        if (videoTimeDifference < audioTimeDifference) {
                            // 正常情况下videoTimeDifference比audioTimeDifference大一些
                            // 如果发现小了,说明视频播放慢了,应丢弃这些帧
                            // break后videoTimeDifference增长的速度会加快
                            break;
                        }
                        // 0.177853 0.155691 0.156806 0.154362
                        double tempTimeDifference = videoTimeDifference - audioTimeDifference;
                        if (tempTimeDifference > 2.000000) {
                            // 不好的现象
                            // 为什么会出现这种情况还不知道?
                            LOGE("handleVideoData() video - audio   : %lf\n", tempTimeDifference);
                        }
                        // 如果videoTimeDifference比audioTimeDifference大出了一定的范围
                        // 那么说明视频播放快了,应等待音频
                        while (videoTimeDifference - audioTimeDifference > TIME_DIFFERENCE) {
                            videoSleep(1);
                        }
#endif
                        if (videoWrapper->father->isHandling) {
                            // 3.lock锁定下一个即将要绘制的Surface
                            ANativeWindow_lock(pANativeWindow, &outBuffer, NULL);

                            /*
                            // 第一种方式(一般情况下这种方式是可以的,但是在我的一加手机上不行,画面是花屏)
                            // 4.读取帧画面放入缓冲区,指定RGB的AVFrame的像素格式、宽高和缓冲区
                            av_image_fill_arrays(rgbAVFrame->data,
                                                 rgbAVFrame->linesize,
                                                 (const uint8_t *) outBuffer.bits,
                                                 AV_PIX_FMT_RGBA,
                                                 videoWrapper->srcWidth,
                                                 videoWrapper->srcHeight,
                                                 1);
                            // 5.将YUV420P->RGBA_8888(只有AV_PIX_FMT_YUV420P这种像素格式才可以使用下面方法)
                            libyuv::I420ToARGB(decodedAVFrame->data[0], decodedAVFrame->linesize[0],
                                               decodedAVFrame->data[2], decodedAVFrame->linesize[2],
                                               decodedAVFrame->data[1], decodedAVFrame->linesize[1],
                                               rgbAVFrame->data[0], rgbAVFrame->linesize[0],
                                               videoWrapper->srcWidth, videoWrapper->srcHeight);
                             */

                            // 第二种方式(我的一加手机就能正常显示画面了)
                            // https://blog.csdn.net/u013898698/article/details/79430202
                            // 格式转换
                            // 把decodedAVFrame的数据经过格式转换后保存到rgbAVFrame中
                            sws_scale(videoWrapper->swsContext,
                                      (uint8_t const *const *) decodedAVFrame->data,
                                      decodedAVFrame->linesize,
                                      0,
                                      videoWrapper->srcHeight,
                                      videoWrapper->rgbAVFrame->data,
                                      videoWrapper->rgbAVFrame->linesize);
                            // 这段代码非常关键,还看不懂啥意思
                            // 把rgbAVFrame里面的数据复制到outBuffer中就能渲染画面了
                            // 获取stride
                            // 一行的数据
                            uint8_t *src = videoWrapper->rgbAVFrame->data[0];
                            // 一行的长度
                            int srcStride = videoWrapper->rgbAVFrame->linesize[0];
                            uint8_t *dst = (uint8_t *) outBuffer.bits;
                            int dstStride = outBuffer.stride * 4;
                            // 由于window的stride和帧的stride不同,因此需要逐行复制
                            for (int h = 0; h < videoWrapper->srcHeight; h++) {
                                memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
                            }

                            // 单位: 毫秒
                            tempSleep = timeDifference * 1000;// 0.040000
                            tempSleep -= 30;
                            tempSleep += step;
                            if (sleep != tempSleep) {
                                sleep = tempSleep;
                                // LOGW("handleVideoData() sleep  : %ld\n", sleep);
                            }
                            if (sleep < 15 && sleep > 0) {
                                videoSleep(sleep);
                            } else {
                                if (sleep > 0) {
                                    // 好像是个比较合理的值
                                    videoSleep(11);
                                }
                                // sleep <= 0时不需要sleep
                            }

                            // 6.unlock绘制
                            ANativeWindow_unlockAndPost(pANativeWindow);
                        }
                        break;
                    }
                    default:
                        break;
                }// switch

                break;
            }// while(1) end

            av_packet_unref(avPacket);

            // 视频的话前三个有值(可能是data[0], data[1], data[2]分别存了YUV的值吧)
            /*LOGI("handleVideoData() dstAVFrame->linesize[0]: %d\n",
                 decodedAVFrame->linesize[0]);
            LOGI("handleVideoData() dstAVFrame->linesize[1]: %d\n",
                 decodedAVFrame->linesize[1]);
            LOGI("handleVideoData() dstAVFrame->linesize[2]: %d\n",
                 decodedAVFrame->linesize[2]);*/
            /*LOGI("handleVideoData() dstAVFrame->linesize[3]: %d\n",
                    videoWrapper->father->dstAVFrame->linesize[3]);
            LOGI("handleVideoData() dstAVFrame->linesize[4]: %d\n",
                    videoWrapper->father->dstAVFrame->linesize[4]);
            LOGI("handleVideoData() dstAVFrame->linesize[5]: %d\n",
                    videoWrapper->father->dstAVFrame->linesize[5]);
            LOGI("handleVideoData() dstAVFrame->linesize[6]: %d\n",
                    videoWrapper->father->dstAVFrame->linesize[6]);
            LOGI("handleVideoData() dstAVFrame->linesize[7]: %d\n",
                    videoWrapper->father->dstAVFrame->linesize[7]);*/

        }// for(;;) end
        LOGW("handleVideoData() for (;;) end\n");

        av_packet_unref(avPacket);
        avPacket = NULL;

        LOGW("handleVideoData() video handleFramesCount : %d\n",
             videoWrapper->father->handleFramesCount);
        if (videoWrapper->father->readFramesCount == videoWrapper->father->handleFramesCount) {
            LOGW("%s\n", "handleVideoData() video正常播放完毕");
        }
        stop();
        closeVideo();

        onFinished();

        LOGW("%s\n", "handleVideoData() end");
        return NULL;
    }

    void closeAudio() {
        // audio
        if (audioWrapper == NULL || audioWrapper->father == NULL) {
            return;
        }
        LOGI("%s\n", "closeAudio() start");
        if (audioWrapper->father->outBuffer1 != NULL) {
            av_free(audioWrapper->father->outBuffer1);
            audioWrapper->father->outBuffer1 = NULL;
        }
        if (audioWrapper->father->outBuffer2 != NULL) {
            av_free(audioWrapper->father->outBuffer2);
            audioWrapper->father->outBuffer2 = NULL;
        }
        if (audioWrapper->father->outBuffer3 != NULL) {
            av_free(audioWrapper->father->outBuffer3);
            audioWrapper->father->outBuffer3 = NULL;
        }
        /*if (audioWrapper->father->srcData[0] != NULL) {
            av_freep(&audioWrapper->father->srcData[0]);
            audioWrapper->father->srcData[0] = NULL;
        }
        if (audioWrapper->father->dstData[0] != NULL) {
            av_freep(&audioWrapper->father->dstData[0]);
            audioWrapper->father->dstData[0] = NULL;
        }*/
        if (audioWrapper->swrContext != NULL) {
            swr_free(&audioWrapper->swrContext);
            audioWrapper->swrContext = NULL;
        }
        if (audioWrapper->decodedAVFrame != NULL) {
            av_frame_free(&audioWrapper->decodedAVFrame);
            audioWrapper->decodedAVFrame = NULL;
        }
        if (audioWrapper->father->avCodecParameters != NULL) {
            avcodec_parameters_free(&audioWrapper->father->avCodecParameters);
            audioWrapper->father->avCodecParameters = NULL;
        }
        if (audioWrapper->father->avCodecContext != NULL) {
            avcodec_close(audioWrapper->father->avCodecContext);
            av_free(audioWrapper->father->avCodecContext);
            audioWrapper->father->avCodecContext = NULL;
        }
        pthread_mutex_destroy(&audioWrapper->father->readLockMutex);
        pthread_cond_destroy(&audioWrapper->father->readLockCondition);
        pthread_mutex_destroy(&audioWrapper->father->handleLockMutex);
        pthread_cond_destroy(&audioWrapper->father->handleLockCondition);
        int count = audioWrapper->father->queue1->allAVPacketsCount;
        if (count > 0) {
            AVPacket *avPacket = av_packet_alloc();
            for (int i = 0; i < count; i++) {
                getAVPacketFromQueue(audioWrapper->father->queue1, avPacket);
                av_packet_unref(avPacket);
            }
            av_packet_unref(avPacket);
        }
        count = audioWrapper->father->queue2->allAVPacketsCount;
        if (count > 0) {
            AVPacket *avPacket = av_packet_alloc();
            for (int i = 0; i < count; i++) {
                getAVPacketFromQueue(audioWrapper->father->queue2, avPacket);
                av_packet_unref(avPacket);
            }
            av_packet_unref(avPacket);
        }
        av_free(audioWrapper->father->queue1);
        av_free(audioWrapper->father->queue2);
        audioWrapper->father->queue1 = NULL;
        audioWrapper->father->queue2 = NULL;
        av_free(audioWrapper->father);
        audioWrapper->father = NULL;
        av_free(audioWrapper);
        audioWrapper = NULL;

        LOGI("%s\n", "closeAudio() end");
    }

    void closeVideo() {
        // video
        if (pANativeWindow != NULL) {
            // 7.释放资源
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = NULL;
        }
        if (videoWrapper == NULL || videoWrapper->father == NULL) {
            return;
        }
        LOGI("%s\n", "closeVideo() start");
        if (videoWrapper->father->outBuffer1 != NULL) {
            av_free(videoWrapper->father->outBuffer1);
            videoWrapper->father->outBuffer1 = NULL;
        }
        if (videoWrapper->father->outBuffer2 != NULL) {
            av_free(videoWrapper->father->outBuffer2);
            videoWrapper->father->outBuffer2 = NULL;
        }
        if (videoWrapper->father->outBuffer3 != NULL) {
            av_free(videoWrapper->father->outBuffer3);
            videoWrapper->father->outBuffer3 = NULL;
        }
        /*if (videoWrapper->father->srcData[0] != NULL) {
            av_freep(&videoWrapper->father->srcData[0]);
            videoWrapper->father->srcData[0] = NULL;
        }
        if (videoWrapper->father->dstData[0] != NULL) {
            av_freep(&videoWrapper->father->dstData[0]);
            videoWrapper->father->dstData[0] = NULL;
        }*/
        if (videoWrapper->swsContext != NULL) {
            sws_freeContext(videoWrapper->swsContext);
            videoWrapper->swsContext = NULL;
        }
        if (videoWrapper->decodedAVFrame != NULL) {
            av_frame_free(&videoWrapper->decodedAVFrame);
            videoWrapper->decodedAVFrame = NULL;
        }
        if (videoWrapper->rgbAVFrame != NULL) {
            av_frame_free(&videoWrapper->rgbAVFrame);
            videoWrapper->rgbAVFrame = NULL;
        }
        if (videoWrapper->father->avCodecParameters != NULL) {
            avcodec_parameters_free(&videoWrapper->father->avCodecParameters);
            videoWrapper->father->avCodecParameters = NULL;
        }
        if (videoWrapper->father->avCodecContext != NULL) {
            avcodec_close(videoWrapper->father->avCodecContext);
            av_free(videoWrapper->father->avCodecContext);
            videoWrapper->father->avCodecContext = NULL;
        }
        pthread_mutex_destroy(&videoWrapper->father->readLockMutex);
        pthread_cond_destroy(&videoWrapper->father->readLockCondition);
        pthread_mutex_destroy(&videoWrapper->father->handleLockMutex);
        pthread_cond_destroy(&videoWrapper->father->handleLockCondition);
        int count = videoWrapper->father->queue1->allAVPacketsCount;
        if (count > 0) {
            AVPacket *avPacket = av_packet_alloc();
            for (int i = 0; i < count; i++) {
                getAVPacketFromQueue(videoWrapper->father->queue1, avPacket);
                av_packet_unref(avPacket);
            }
            av_packet_unref(avPacket);
        }
        count = videoWrapper->father->queue2->allAVPacketsCount;
        if (count > 0) {
            AVPacket *avPacket = av_packet_alloc();
            for (int i = 0; i < count; i++) {
                getAVPacketFromQueue(videoWrapper->father->queue2, avPacket);
                av_packet_unref(avPacket);
            }
            av_packet_unref(avPacket);
        }
        av_free(videoWrapper->father->queue1);
        av_free(videoWrapper->father->queue2);
        videoWrapper->father->queue1 = NULL;
        videoWrapper->father->queue2 = NULL;
        av_free(videoWrapper->father);
        videoWrapper->father = NULL;
        av_free(videoWrapper);
        videoWrapper = NULL;

        /*if (avFormatContext != NULL) {
            avformat_close_input(&avFormatContext);
            avFormatContext = NULL;
        }*/

        /*if (inFile != NULL) {
            fclose(inFile);
            inFile = NULL;
        }
        if (outFile != NULL) {
            fclose(outFile);
            outFile = NULL;
        }*/
        LOGI("%s\n", "closeVideo() end");
    }

    int initAudioPlayer() {
        LOGI("%s\n", "initAudioPlayer() start");

#ifdef USE_AUDIO
        // audio
        initAudio();
        if (openAndFindAVFormatContextForAudio() < 0) {
            LOGE("openAndFindAVFormatContextForAudio() failed\n");
            closeAudio();
            return -1;
        }
        if (findStreamIndexForAudio() < 0) {
            LOGE("findStreamIndexForAudio() failed\n");
            closeAudio();
            return -1;
        }
        if (findAndOpenAVCodecForAudio() < 0) {
            LOGE("findAndOpenAVCodecForAudio() failed\n");
            closeAudio();
            return -1;
        }
        if (createSwrContent() < 0) {
            LOGE("createSwrContent() failed\n");
            closeAudio();
            return -1;
        }
#endif

        LOGI("%s\n", "initAudioPlayer() end");

        return 0;
    }


    int initVideoPlayer() {
        LOGI("%s\n", "initVideoPlayer() start");

#ifdef USE_VIDEO
        // video
        initVideo();
        if (openAndFindAVFormatContextForVideo() < 0) {
            LOGE("openAndFindAVFormatContextForVideo() failed\n");
            closeVideo();
            return -1;
        }
        if (findStreamIndexForVideo() < 0) {
            LOGE("findStreamIndexForVideo() failed\n");
            closeVideo();
            return -1;
        }
        if (findAndOpenAVCodecForVideo() < 0) {
            LOGE("findAndOpenAVCodecForVideo() failed\n");
            closeVideo();
            return -1;
        }
        if (createSwsContext() < 0) {
            LOGE("createSwsContext() failed\n");
            closeVideo();
            return -1;
        }
#endif

        LOGI("%s\n", "initVideoPlayer() end");

        return 0;
    }

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject) {
        isLocal = false;
        memset(inFilePath, '\0', sizeof(inFilePath));
//        const char *src = "/storage/emulated/0/Movies/权力的游戏第三季05.mp4";
//        const char *src = "http://192.168.0.112:8080/tomcat_video/game_of_thrones/game_of_thrones_season_1/01.mp4";
//        av_strlcpy(inFilePath, src, sizeof(inFilePath));
        av_strlcpy(inFilePath, filePath, sizeof(inFilePath));
        LOGI("setJniParameters() inFilePath: %s", inFilePath);
        char *result = strstr(inFilePath, "http://");
        if (result == NULL) {
            result = strstr(inFilePath, "https://");
            if (result == NULL) {
                result = strstr(inFilePath, "HTTP://");
                if (result == NULL) {
                    result = strstr(inFilePath, "HTTPS://");
                    if (result == NULL) {
                        isLocal = true;
                    }
                }
            }
        }
        LOGI("setJniParameters() isLocal   : %d", isLocal);

#ifdef USE_VIDEO
        // 1.获取一个关联Surface的NativeWindow窗体
        pANativeWindow = ANativeWindow_fromSurface(env, surfaceJavaObject);
        if (pANativeWindow == NULL) {
            LOGI("handleVideoData() pANativeWindow is NULL\n");
        } else {
            LOGI("handleVideoData() pANativeWindow isn't NULL\n");
        }
#endif
    }

    int play() {
#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = false;
            pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
            pthread_cond_signal(&audioWrapper->father->handleLockCondition);
            pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = false;
            pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
            pthread_cond_signal(&videoWrapper->father->handleLockCondition);
            pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
        }
#endif
        return 0;
    }

    int pause() {
#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = true;
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = true;
        }
#endif
        return 0;
    }

    int stop() {
#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            LOGI("stop() audio start\n");
            audioWrapper->father->isStarted = false;
            audioWrapper->father->isReading = false;
            audioWrapper->father->isHandling = false;
            audioWrapper->father->isPausedForUser = false;
            audioWrapper->father->isPausedForCache = false;
            audioWrapper->father->isReadQueue1Full = false;
            audioWrapper->father->isReadQueue2Full = false;
            //
            pthread_mutex_lock(&audioWrapper->father->readLockMutex);
            pthread_cond_signal(&audioWrapper->father->readLockCondition);
            pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
            //
            pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
            pthread_cond_signal(&audioWrapper->father->handleLockCondition);
            pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
            LOGI("stop() audio end\n");
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            LOGI("stop() video start\n");
            videoWrapper->father->isStarted = false;
            videoWrapper->father->isReading = false;
            videoWrapper->father->isHandling = false;
            videoWrapper->father->isPausedForUser = false;
            videoWrapper->father->isPausedForCache = false;
            videoWrapper->father->isReadQueue1Full = false;
            videoWrapper->father->isReadQueue2Full = false;
            //
            pthread_mutex_lock(&videoWrapper->father->readLockMutex);
            pthread_cond_signal(&videoWrapper->father->readLockCondition);
            pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
            //
            pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
            pthread_cond_signal(&videoWrapper->father->handleLockCondition);
            pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
            LOGI("stop() video end\n");
        }
#endif
        return 0;
    }

    int release() {
        stop();
        return 0;
    }

    bool isRunning() {
        bool audioRunning = false;
        bool videoRunning = false;
#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioRunning = audioWrapper->father->isReading
                           || audioWrapper->father->isHandling;
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoRunning = videoWrapper->father->isReading
                           || videoWrapper->father->isHandling;
        }
#endif
        return audioRunning || videoRunning;
    }

    bool isPlaying() {
        bool audioPlaying = false;
        bool videoPlaying = false;
#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioPlaying = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling
                           && !audioWrapper->father->isPausedForUser
                           && !audioWrapper->father->isPausedForCache;
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoPlaying = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling
                           && !videoWrapper->father->isPausedForUser
                           && !videoWrapper->father->isPausedForCache;
        }
#endif
        return audioPlaying && videoPlaying;
    }

    int seekTo(int64_t timestamp) {
        LOGI("==================================================================\n");
        LOGI("seekTo() timestamp: %ld\n", timestamp);
#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->timestamp = timestamp;
            pthread_mutex_lock(&audioWrapper->father->readLockMutex);
            pthread_cond_signal(&audioWrapper->father->readLockCondition);
            pthread_mutex_unlock(&audioWrapper->father->readLockMutex);

            audioWrapper->father->nextRead = NEXT_READ_QUEUE1;
            audioWrapper->father->nextHandle = NEXT_HANDLE_QUEUE1;
            audioWrapper->father->isReadQueue1Full = false;
            audioWrapper->father->isReadQueue2Full = false;

            // 如果audio处理线程由于Cache原因而暂停,那么不用处理,继续让它暂停好了
            // 如果audio处理线程由于User原因而暂停,那么需要通知它,使它变成由于Cache原因而暂停
            if (audioWrapper->father->isPausedForUser) {
                audioWrapper->father->isPausedForUser = false;
                pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                pthread_cond_signal(&audioWrapper->father->handleLockCondition);
                pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
            }

            // 把Queue1和Queue2队列清空
            int count = audioWrapper->father->queue1->allAVPacketsCount;
            if (count > 0) {
                AVPacket *avPacket = av_packet_alloc();
                for (int i = 0; i < count; i++) {
                    getAVPacketFromQueue(audioWrapper->father->queue1, avPacket);
                    av_packet_unref(avPacket);
                }
                av_packet_unref(avPacket);
            }
            audioWrapper->father->queue1->firstAVPacketList = NULL;
            audioWrapper->father->queue1->lastAVPacketList = NULL;
            audioWrapper->father->queue1->allAVPacketsCount = 0;
            audioWrapper->father->queue1->allAVPacketsSize = 0;
            memset(audioWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
            count = audioWrapper->father->queue2->allAVPacketsCount;
            if (count > 0) {
                AVPacket *avPacket = av_packet_alloc();
                for (int i = 0; i < count; i++) {
                    getAVPacketFromQueue(audioWrapper->father->queue2, avPacket);
                    av_packet_unref(avPacket);
                }
                av_packet_unref(avPacket);
            }
            audioWrapper->father->queue2->firstAVPacketList = NULL;
            audioWrapper->father->queue2->lastAVPacketList = NULL;
            audioWrapper->father->queue2->allAVPacketsCount = 0;
            audioWrapper->father->queue2->allAVPacketsSize = 0;
            memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));

            // 通知开始seek
            LOGD("seekTo() audio signal() handleLockCondition\n");
            pthread_mutex_lock(&audioWrapper->father->readLockMutex);
            pthread_cond_signal(&audioWrapper->father->readLockCondition);
            pthread_mutex_unlock(&audioWrapper->father->readLockMutex);

            audioWrapper->father->seekToInit = true;
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->timestamp = timestamp;
            pthread_mutex_lock(&videoWrapper->father->readLockMutex);
            pthread_cond_signal(&videoWrapper->father->readLockCondition);
            pthread_mutex_unlock(&videoWrapper->father->readLockMutex);

            videoWrapper->father->nextRead = NEXT_READ_QUEUE1;
            videoWrapper->father->nextHandle = NEXT_HANDLE_QUEUE1;
            videoWrapper->father->isReadQueue1Full = false;
            videoWrapper->father->isReadQueue2Full = false;

            // 如果video处理线程由于Cache原因而暂停,那么不用处理,继续让它暂停好了
            // 如果video处理线程由于User原因而暂停,那么需要通知它,使它变成由于Cache原因而暂停
            if (videoWrapper->father->isPausedForUser) {
                videoWrapper->father->isPausedForUser = false;
                pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                pthread_cond_signal(&videoWrapper->father->handleLockCondition);
                pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
            }

            // 把Queue1和Queue2队列清空
            int count = videoWrapper->father->queue1->allAVPacketsCount;
            if (count > 0) {
                AVPacket *avPacket = av_packet_alloc();
                for (int i = 0; i < count; i++) {
                    getAVPacketFromQueue(videoWrapper->father->queue1, avPacket);
                    av_packet_unref(avPacket);
                }
                av_packet_unref(avPacket);
            }
            videoWrapper->father->queue1->firstAVPacketList = NULL;
            videoWrapper->father->queue1->lastAVPacketList = NULL;
            videoWrapper->father->queue1->allAVPacketsCount = 0;
            videoWrapper->father->queue1->allAVPacketsSize = 0;
            memset(videoWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
            count = videoWrapper->father->queue2->allAVPacketsCount;
            if (count > 0) {
                AVPacket *avPacket = av_packet_alloc();
                for (int i = 0; i < count; i++) {
                    getAVPacketFromQueue(videoWrapper->father->queue2, avPacket);
                    av_packet_unref(avPacket);
                }
                av_packet_unref(avPacket);
            }
            videoWrapper->father->queue2->firstAVPacketList = NULL;
            videoWrapper->father->queue2->lastAVPacketList = NULL;
            videoWrapper->father->queue2->allAVPacketsCount = 0;
            videoWrapper->father->queue2->allAVPacketsSize = 0;
            memset(videoWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));

            // 通知开始seek
            LOGW("seekTo() video signal() handleLockCondition\n");
            pthread_mutex_lock(&videoWrapper->father->readLockMutex);
            pthread_cond_signal(&videoWrapper->father->readLockCondition);
            pthread_mutex_unlock(&videoWrapper->father->readLockMutex);

            videoWrapper->father->seekToInit = true;
        }
#endif
        return 0;
    }

    // 返回值单位是秒
    int64_t getDuration() {
        int64_t audioDuration = 0;
        int64_t videoDuration = 0;
        int64_t duration = 0;

#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioDuration = audioWrapper->father->duration;
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoDuration = videoWrapper->father->duration;
        }
#endif
        if (audioDuration != 0 && videoDuration != 0) {
            duration = audioDuration > videoDuration ? videoDuration : audioDuration;
        } else if (audioDuration != 0 && videoDuration == 0) {
            duration = audioDuration;
        } else if (audioDuration == 0 && videoDuration != 0) {
            duration = videoDuration;
        } else {
            duration = 0;
        }

        return duration;
    }

    void stepAdd() {
        step++;
        char dest[50];
        sprintf(dest, "sleep: %ld, step: %ld\n", sleep, step);
        onInfo(dest);
        LOGI("stepAdd()      sleep: %ld, step: %ld\n", sleep, step);
    }

    void stepSubtract() {
        step--;
        char dest[50];
        sprintf(dest, "sleep: %ld, step: %ld\n", sleep, step);
        onInfo(dest);
        LOGI("stepSubtract() sleep: %ld, step: %ld\n", sleep, step);
    }

    /***
     char src[40];
     char dest[100];
     memset(dest, '\0', sizeof(dest));
     strcpy(src, "This is runoob.com");
     strcpy(dest, src);

     原型：int strlen ( const char *str )
     功能：返回字符串的实际长度,不含 '\0'.
     strlen之所以不包含'\0',是因为它在计数的途中遇到'\0'结束.
     char buf[100] = "hello";
     printf("%d\n", strlen(buf));// 5
     printf("%d\n", sizeof(buf));// 100
     */

    char *getStrAVPixelFormat(AVPixelFormat format) {
        char info[25] = {0};
        switch (format) {
            case AV_PIX_FMT_NONE:
                strncpy(info, "AV_PIX_FMT_NONE", strlen("AV_PIX_FMT_NONE"));
                break;
            case AV_PIX_FMT_YUV420P:// 0
                strncpy(info, "AV_PIX_FMT_YUV420P", strlen("AV_PIX_FMT_YUV420P"));
                break;
            case AV_PIX_FMT_YUYV422:
                strncpy(info, "AV_PIX_FMT_YUYV422", strlen("AV_PIX_FMT_YUYV422"));
                break;
            case AV_PIX_FMT_RGB24:
                strncpy(info, "AV_PIX_FMT_RGB24", strlen("AV_PIX_FMT_RGB24"));
                break;
            case AV_PIX_FMT_BGR24:
                strncpy(info, "AV_PIX_FMT_BGR24", strlen("AV_PIX_FMT_BGR24"));
                break;
            case AV_PIX_FMT_YUV422P:
                strncpy(info, "AV_PIX_FMT_YUV422P", strlen("AV_PIX_FMT_YUV422P"));
                break;
            case AV_PIX_FMT_YUV444P:
                strncpy(info, "AV_PIX_FMT_YUV444P", strlen("AV_PIX_FMT_YUV444P"));
                break;
            case AV_PIX_FMT_YUV410P:
                strncpy(info, "AV_PIX_FMT_YUV410P", strlen("AV_PIX_FMT_YUV410P"));
                break;
            case AV_PIX_FMT_YUV411P:
                strncpy(info, "AV_PIX_FMT_YUV411P", strlen("AV_PIX_FMT_YUV411P"));
                break;
            case AV_PIX_FMT_GRAY8:
                strncpy(info, "AV_PIX_FMT_GRAY8", strlen("AV_PIX_FMT_GRAY8"));
                break;
            case AV_PIX_FMT_MONOWHITE:
                strncpy(info, "AV_PIX_FMT_MONOWHITE", strlen("AV_PIX_FMT_MONOWHITE"));
                break;
            case AV_PIX_FMT_MONOBLACK:
                strncpy(info, "AV_PIX_FMT_MONOBLACK", strlen("AV_PIX_FMT_MONOBLACK"));
                break;
            case AV_PIX_FMT_PAL8:
                strncpy(info, "AV_PIX_FMT_PAL8", strlen("AV_PIX_FMT_PAL8"));
                break;
            case AV_PIX_FMT_YUVJ420P:
                strncpy(info, "AV_PIX_FMT_YUVJ420P", strlen("AV_PIX_FMT_YUVJ420P"));
                break;
            case AV_PIX_FMT_YUVJ422P:
                strncpy(info, "AV_PIX_FMT_YUVJ422P", strlen("AV_PIX_FMT_YUVJ422P"));
                break;
            case AV_PIX_FMT_YUVJ444P:
                strncpy(info, "AV_PIX_FMT_YUVJ444P", strlen("AV_PIX_FMT_YUVJ444P"));
                break;
            case AV_PIX_FMT_UYVY422:
                strncpy(info, "AV_PIX_FMT_UYVY422", strlen("AV_PIX_FMT_UYVY422"));
                break;
            case AV_PIX_FMT_UYYVYY411:
                strncpy(info, "AV_PIX_FMT_UYYVYY411", strlen("AV_PIX_FMT_UYYVYY411"));
                break;
            case AV_PIX_FMT_BGR8:
                strncpy(info, "AV_PIX_FMT_BGR8", strlen("AV_PIX_FMT_BGR8"));
                break;
            case AV_PIX_FMT_BGR4:
                strncpy(info, "AV_PIX_FMT_BGR4", strlen("AV_PIX_FMT_BGR4"));
                break;
            case AV_PIX_FMT_BGR4_BYTE:
                strncpy(info, "AV_PIX_FMT_BGR4_BYTE", strlen("AV_PIX_FMT_BGR4_BYTE"));
                break;
            case AV_PIX_FMT_RGB8:
                strncpy(info, "AV_PIX_FMT_RGB8", strlen("AV_PIX_FMT_RGB8"));
                break;
            case AV_PIX_FMT_RGB4:
                strncpy(info, "AV_PIX_FMT_RGB4", strlen("AV_PIX_FMT_RGB4"));
                break;
            case AV_PIX_FMT_RGB4_BYTE:
                strncpy(info, "AV_PIX_FMT_RGB4_BYTE", strlen("AV_PIX_FMT_RGB4_BYTE"));
                break;
            case AV_PIX_FMT_NV12:
                strncpy(info, "AV_PIX_FMT_NV12", strlen("AV_PIX_FMT_NV12"));
                break;
            case AV_PIX_FMT_NV21:
                strncpy(info, "AV_PIX_FMT_NV21", strlen("AV_PIX_FMT_NV21"));
                break;
            case AV_PIX_FMT_ARGB:
                strncpy(info, "AV_PIX_FMT_ARGB", strlen("AV_PIX_FMT_ARGB"));
                break;
            case AV_PIX_FMT_RGBA:
                strncpy(info, "AV_PIX_FMT_RGBA", strlen("AV_PIX_FMT_RGBA"));
                break;
            case AV_PIX_FMT_ABGR:
                strncpy(info, "AV_PIX_FMT_ABGR", strlen("AV_PIX_FMT_ABGR"));
                break;
            case AV_PIX_FMT_BGRA:
                strncpy(info, "AV_PIX_FMT_BGRA", strlen("AV_PIX_FMT_BGRA"));
                break;
            case AV_PIX_FMT_GRAY16BE:
                strncpy(info, "AV_PIX_FMT_GRAY16BE", strlen("AV_PIX_FMT_GRAY16BE"));
                break;
            case AV_PIX_FMT_GRAY16LE:
                strncpy(info, "AV_PIX_FMT_GRAY16LE", strlen("AV_PIX_FMT_GRAY16LE"));
                break;
            case AV_PIX_FMT_YUV440P:
                strncpy(info, "AV_PIX_FMT_YUV440P", strlen("AV_PIX_FMT_YUV440P"));
                break;
            case AV_PIX_FMT_YUVJ440P:
                strncpy(info, "AV_PIX_FMT_YUVJ440P", strlen("AV_PIX_FMT_YUVJ440P"));
                break;
            case AV_PIX_FMT_YUVA420P:
                strncpy(info, "AV_PIX_FMT_YUVA420P", strlen("AV_PIX_FMT_YUVA420P"));
                break;
            case AV_PIX_FMT_RGB48BE:
                strncpy(info, "AV_PIX_FMT_RGB48BE", strlen("AV_PIX_FMT_RGB48BE"));
                break;
            case AV_PIX_FMT_RGB48LE:
                strncpy(info, "AV_PIX_FMT_RGB48LE", strlen("AV_PIX_FMT_RGB48LE"));
                break;
            case AV_PIX_FMT_RGB565BE:
                strncpy(info, "AV_PIX_FMT_RGB565BE", strlen("AV_PIX_FMT_RGB565BE"));
                break;
            case AV_PIX_FMT_RGB565LE:
                strncpy(info, "AV_PIX_FMT_RGB565LE", strlen("AV_PIX_FMT_RGB565LE"));
                break;
            case AV_PIX_FMT_RGB555BE:
                strncpy(info, "AV_PIX_FMT_RGB555BE", strlen("AV_PIX_FMT_RGB555BE"));
                break;
            case AV_PIX_FMT_RGB555LE:
                strncpy(info, "AV_PIX_FMT_RGB555LE", strlen("AV_PIX_FMT_RGB555LE"));
                break;
            case AV_PIX_FMT_BGR565BE:
                strncpy(info, "AV_PIX_FMT_BGR565BE", strlen("AV_PIX_FMT_BGR565BE"));
                break;
            case AV_PIX_FMT_BGR565LE:
                strncpy(info, "AV_PIX_FMT_BGR565LE", strlen("AV_PIX_FMT_BGR565LE"));
                break;
            case AV_PIX_FMT_BGR555BE:
                strncpy(info, "AV_PIX_FMT_BGR555BE", strlen("AV_PIX_FMT_BGR555BE"));
                break;
            case AV_PIX_FMT_BGR555LE:
                strncpy(info, "AV_PIX_FMT_BGR555LE", strlen("AV_PIX_FMT_BGR555LE"));
                break;
            default:
                strncpy(info, "AV_PIX_FMT_NONE", strlen("AV_PIX_FMT_NONE"));
                break;
        }

        return info;
    }

}
