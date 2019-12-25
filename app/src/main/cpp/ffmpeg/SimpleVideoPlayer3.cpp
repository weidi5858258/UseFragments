//
// Created by root on 19-8-8.
//

#include "SimpleVideoPlayer3.h"






/***

NV21(yuv420sp)和I420(yuv420p)
NV21的排列是YYYYYYYY VUVU  -> YUV420SP
I420的排列是YYYYYYYY UU VV -> YUV420P
ffmpeg中jpeg编码输入要求是YUVJ420P格式.
1.YUV420P to YUVJ420P
2.YUVJ420P to jpeg

在FFMPEG中，图像原始数据包括两种：planar和packed。
planar就是将几个分量分开存，比如YUV420中，data[0]专门存Y，data[1]专门存U，data[2]专门存V。
而packed则是打包存，所有数据都存在data[0]中。

大多数图像处理软件在处理时是需要RGB格式的图像.
默认的视频流是压缩的YUV格式，Android下是YUV420SP.
Android提供的SurfaceView、GLSurfaceView、TextureView等控件只支持RGB格式的渲染.
RGB 转换成 YUV
Y=Y  = (0.257 * R) + (0.504 * G) + (0.098 * B) + 16
V=Cr =  (0.439 * R) - (0.368 * G) - (0.071 * B) + 128
U=Cb =  -( 0.148 * R) - (0.291 * G) + (0.439 * B) + 128
YUV 转换成 RGB
R = 1.164 (Y - 16)  +  1.596 (V  -  128)
G = 1.164 (Y - 16)  -  0.813 (V  -  128)  -  0.391 (U  -  128)
B = 1.164 (Y - 16)  +  2.018 (U  -  128)

R=Y+1.4075*(V-128)
G=Y-0.3455*(U-128) – 0.7169*(V-128)
B=Y+1.779*(U-128)

public native int[] decodeYUV420SP(byte[] buf, int width, int height);
返回的结果是一个ARGB_8888格式的颜色数组.
mBitmap = Bitmap.createBitmap(data, width, height, Config.ARGB_8888);

保存YUV420P格式的数据，用以下代码：
fwrite(pFrameYUV->data[0],(pCodecCtx->width)*(pCodecCtx->height),1,output);
fwrite(pFrameYUV->data[1],(pCodecCtx->width)*(pCodecCtx->height)/4,1,output);
fwrite(pFrameYUV->data[2],(pCodecCtx->width)*(pCodecCtx->height)/4,1,output);
保存RGB24格式的数据，用以下代码：
fwrite(pFrameYUV->data[0],(pCodecCtx->width)*(pCodecCtx->height)*3,1,output);
保存UYVY格式的数据，用以下代码：
fwrite(pFrameYUV->data[0],(pCodecCtx->width)*(pCodecCtx->height),2,output);

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

int sws_scale(struct SwsContext *c, const uint8_t *const srcSlice[],
                                          const int srcStride[], int srcSliceY, int srcSliceH,
                                          uint8_t *const dst[], const int dstStride[]);
// c参数指定使用包含图像数据格式转换信息的struct SwsContext对象
// srcSlice参数用于指定的原图像数据缓冲区地址
// srcStride参数用于指定原数据缓冲区一行数据的大小
// srcSliceY参数用于指定从原图像的第几行开始转换
// srcSliceH参数用于指定转换到原图像的第几行.
// dst参数用于指定存放生成图像数据的缓冲区地址
// dstStride参数指定存放生成图像的一行数据大小的缓冲区

typedef struct AVFrame {
    #define AV_NUM_DATA_POINTERS 8
    // 此指针数组是用于存放数据缓冲区地址，因有可能是平面的数据，所有用了多个指针变量存放不同分量的数据缓冲区
    uint8_t *data[AV_NUM_DATA_POINTERS];
    // 存放每个缓冲区的一行数据的字节数
    int linesize[AV_NUM_DATA_POINTERS];
    ...
}AVFrame

yuv422转换成420p:
1. 创建AVFrame对象
    AVFrame  *frm422 = av_frame_alloc();
    AVFrame  *frm420p = av_frame_alloc();
2. 绑定数据缓冲区
    av_image_fill_arrays(frm422->data, frm422->linesize, buf_422, AV_PIX_FMT_YUYV422, w, h, 16);
    av_image_fill_arrays(frm420p->data, frm420p->linesize, buf_420p, AV_PIX_FMT_YUV420P, w, h, 16);
3. 指定原数据格式，分辨率及目标数据格式，分辨率
    struct SwsContext *sws = sws_getContext(w, h, AV_PIX_FMT_YUYV422,
                                            w,h, AV_PIX_FMT_YUV420P,
                                            SWS_BILINEAR,
                                            NULL, NULL, NULL);
4. 转换并调整分辨率
    int ret = sws_scale(sws, frm422->data, frm422->linesize, 0, h, frm420p->data, frm420p->linesize);
5. 回收空间
    av_frame_free(&frm422);
    av_frame_free(&frm420p);
    sws_freeContext(sws);

argb转换成yuv420p:

int MyDataCovert::argb32Toyuv420p(uint8_t *buf_argb, uint8_t *buf_420p, int w, int h)
{
    AVFrame  *frmArgb = av_frame_alloc();
    AVFrame  *frm420p = av_frame_alloc();

    //绑定数据缓冲区
    avpicture_fill((AVPicture *)frmArgb, buf_argb, AV_PIX_FMT_BGRA, w, h);
    avpicture_fill((AVPicture *)frm420p, buf_420p, AV_PIX_FMT_YUV420P, w, h);

    //指定原数据格式，分辨率及目标数据格式，分辨率
    struct SwsContext *sws = sws_getContext(w, h, AV_PIX_FMT_BGRA,
                                            w,h, AV_PIX_FMT_YUV420P,
                                            SWS_BILINEAR,
                                            NULL, NULL, NULL);

    //转换
    int ret = sws_scale(sws, frmArgb->data, frmArgb->linesize, 0, h, frm420p->data, frm420p->linesize);
    av_frame_free(&frmArgb);
    av_frame_free(&frm420p);
    sws_freeContext(sws);
    return  (ret == h) ? 0 : -1;
}

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

#define LOG "Player_alexander"

namespace alexander {

    typedef struct AVPacketQueue {
        AVPacketList *firstAVPacketList = NULL;
        AVPacketList *lastAVPacketList = NULL;
        // 有多少个AVPacketList
        int allAVPacketsCount = 0;
        // 所有AVPacket占用的空间大小
        int64_t allAVPacketsSize = 0;
    };

    static struct AudioWrapper *audioWrapper = NULL;
    static struct VideoWrapper *videoWrapper = NULL;
    static bool isLocal = false;

    static char inFilePath[2048];
    static ANativeWindow *pANativeWindow = NULL;

    double TIME_DIFFERENCE = 1.000000;// 0.180000
    double audioTimeDifference = 0;
    // 当前时间戳
    double videoTimeDifference = 0;
    long videoTimeDifference_l = 0;
    // 上一个时间戳
    double videoTimeDifferencePre = 0;
    long videoTimeDifferencePre_l = 0;
    // 当前时间戳与上一个时间戳的差
    double maxVideoTimeDifference = 0;
    long maxVideoTimeDifference_l = 0;
    // 正常播放情况下,视频时间戳减去音频时间戳
    double timeDifferenceWithAV = 0;


    double totalTimeDifference = 0;
    long totalTimeDifferenceCount = 0;
    long preProgress = 0;
    long videoSleepTime = 0;
    long step = 0;
    bool needLocalLog = true;

    // 绘制时的缓冲区
    ANativeWindow_Buffer outBuffer;
    bool onlyOneVideo = true;
    int64_t curAVFramePtsVideo = 0;
    int64_t preAVFramePtsVideo = 0;

    int getAVPacketFromQueue(struct AVPacketQueue *packet_queue, AVPacket *avpacket);

    char *getStrAVPixelFormat(AVPixelFormat format);

    AudioWrapper *getAudioWrapper() {
        return audioWrapper;
    }

    VideoWrapper *getVideoWrapper() {
        return videoWrapper;
    }

    // 通知读线程开始读(其中有一个队列空的情况)
    void notifyToRead(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->readLockMutex);
        pthread_cond_signal(&wrapper->readLockCondition);
        pthread_mutex_unlock(&wrapper->readLockMutex);
    }

    // 通知读线程开始等待(队列都满的情况)
    void notifyToReadWait(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->readLockMutex);
        pthread_cond_wait(&wrapper->readLockCondition, &wrapper->readLockMutex);
        pthread_mutex_unlock(&wrapper->readLockMutex);
    }

    // 通知处理线程开始处理(几种情况需要唤醒:开始播放时,cache缓存时)
    void notifyToHandle(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->handleLockMutex);
        pthread_cond_signal(&wrapper->handleLockCondition);
        pthread_mutex_unlock(&wrapper->handleLockMutex);
    }

    // 通知处理线程开始等待
    void notifyToHandleWait(Wrapper *wrapper) {
        pthread_mutex_lock(&wrapper->handleLockMutex);
        pthread_cond_wait(&wrapper->handleLockCondition, &wrapper->handleLockMutex);
        pthread_mutex_unlock(&wrapper->handleLockMutex);
    }

    struct AVPacketNode {
        bool operator()(const AVPacket &packet_a, const AVPacket &packet_b) {
            return packet_a.pts <= packet_b.pts;
        }
    };

    struct AVFrameNode {
        bool operator()(const AVFrame &frame_a, const AVFrame &frame_b) {
            // 升序排序;若改为>,则变为降序
            return frame_a.pts <= frame_b.pts;
        }
    };

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
        audioWrapper->father->nextRead = NEXT_READ_LIST1;
        audioWrapper->father->nextHandle = NEXT_HANDLE_LIST1;
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
        audioWrapper->father->isReadList1Full = false;
        audioWrapper->father->isReadList2Full = false;
        audioWrapper->father->duration = -1;
        audioWrapper->father->timestamp = -1;
        /*audioWrapper->father->queue1 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        audioWrapper->father->queue2 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        memset(audioWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
        memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
        audioWrapper->father->queue1->allAVPacketsCount = 0;
        audioWrapper->father->queue1->allAVPacketsSize = 0;
        audioWrapper->father->queue2->allAVPacketsCount = 0;
        audioWrapper->father->queue2->allAVPacketsSize = 0;*/
        audioWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        audioWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;

        //audioWrapper->father->list1 = (vector *) av_mallocz(sizeof(vector));
        //audioWrapper->father->list2 = (vector *) av_mallocz(sizeof(vector));
        audioWrapper->father->list1 = new std::list<AVPacket>();
        audioWrapper->father->list2 = new std::list<AVPacket>();
        audioWrapper->father->tempList = new std::list<AVFrame>();

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
        videoWrapper->father->nextRead = NEXT_READ_LIST1;
        videoWrapper->father->nextHandle = NEXT_HANDLE_LIST1;
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
        videoWrapper->father->isReadList1Full = false;
        videoWrapper->father->isReadList2Full = false;
        videoWrapper->father->duration = -1;
        videoWrapper->father->timestamp = -1;
        /*videoWrapper->father->queue1 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        videoWrapper->father->queue2 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        memset(videoWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
        memset(videoWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
        videoWrapper->father->queue1->allAVPacketsCount = 0;
        videoWrapper->father->queue1->allAVPacketsSize = 0;
        videoWrapper->father->queue2->allAVPacketsCount = 0;
        videoWrapper->father->queue2->allAVPacketsSize = 0;*/
        videoWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        videoWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;

        //videoWrapper->father->list1 = (vector *) av_mallocz(sizeof(vector));
        //videoWrapper->father->list2 = (vector *) av_mallocz(sizeof(vector));
        videoWrapper->father->list1 = new std::list<AVPacket>();
        videoWrapper->father->list2 = new std::list<AVPacket>();
        videoWrapper->father->tempList = new std::list<AVFrame>();

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
            LOGD("audioStreamIndex: %d\n", audioWrapper->father->streamIndex);
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
            LOGW("videoStreamIndex: %d\n", videoWrapper->father->streamIndex);
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
            // avcodec_find_decoder_by_name
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
        LOGD("---------------------------------\n");
        LOGD("srcSampleRate       : %d\n", audioWrapper->srcSampleRate);
        LOGD("srcNbChannels       : %d\n", audioWrapper->srcNbChannels);
        LOGD("srcAVSampleFormat   : %d\n", audioWrapper->srcAVSampleFormat);
        LOGD("srcNbSamples        : %d\n", audioWrapper->srcNbSamples);
        LOGD("srcChannelLayout1   : %d\n", audioWrapper->srcChannelLayout);
        // 有些视频从源视频中得到的channel_layout与使用函数得到的channel_layout结果是一样的
        // 但是还是要使用函数得到的channel_layout为好
        audioWrapper->srcChannelLayout = av_get_default_channel_layout(audioWrapper->srcNbChannels);
        LOGD("srcChannelLayout2   : %d\n", audioWrapper->srcChannelLayout);
        LOGD("---------------------------------\n");
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

        LOGD("dstSampleRate       : %d\n", audioWrapper->dstSampleRate);
        LOGD("dstNbChannels       : %d\n", audioWrapper->dstNbChannels);
        LOGD("dstAVSampleFormat   : %d\n", audioWrapper->dstAVSampleFormat);
        LOGD("dstNbSamples        : %d\n", audioWrapper->dstNbSamples);
        LOGD("---------------------------------\n");

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
            LOGE("%s\n", "createSwrContent() swrContext is NULL");
            return -1;
        }

        int ret = swr_init(audioWrapper->swrContext);
        if (ret != 0) {
            LOGE("%s\n", "createSwrContent() swrContext swr_init failed");
            return -1;
        } else {
            LOGD("%s\n", "createSwrContent() swrContext swr_init success");
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

        LOGD("%s\n", "createSwrContent() createAudioTrack start");
        createAudioTrack(audioWrapper->dstSampleRate,
                         audioWrapper->dstNbChannels,
                         audioFormat);
        LOGD("%s\n", "createSwrContent() createAudioTrack end");

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
        LOGW("---------------------------------\n");
        LOGW("srcWidth            : %d\n", videoWrapper->srcWidth);
        LOGW("srcHeight           : %d\n", videoWrapper->srcHeight);
        LOGW("srcAVPixelFormat    : %d %s\n",
             videoWrapper->srcAVPixelFormat, getStrAVPixelFormat(videoWrapper->srcAVPixelFormat));
        LOGW("dstAVPixelFormat    : %d %s\n",
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
        LOGW("imageGetBufferSize1 : %d\n", imageGetBufferSize);
        LOGW("imageGetBufferSize2 : %d\n", videoWrapper->father->outBufferSize);
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
            LOGE("imageFillArrays     : %d\n", imageFillArrays);
            return -1;
        }
        // 由于解码出来的帧格式不是RGBA,在渲染之前需要进行格式转换
        // 现在swsContext知道程序员想要得到什么样的像素格式了
        videoWrapper->swsContext = sws_getContext(
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->srcAVPixelFormat,
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->dstAVPixelFormat,
                // SWS_BICUBIC SWS_BILINEAR 原分辨率与目标分辨率不一致时使用哪种算法来调整.
                SWS_BICUBIC,
                NULL, NULL,
                // 指定调整图像缩放的算法,可设为NULL使用默认算法.
                NULL);
        if (videoWrapper->swsContext == NULL) {
            LOGE("%s\n", "videoSwsContext is NULL.");
            return -1;
        }
        LOGW("---------------------------------\n");

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

    int readDataImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *dstAVPacket) {
        av_packet_ref(dstAVPacket, srcAVPacket);
        wrapper->readFramesCount++;

        if (wrapper->nextRead == NEXT_READ_LIST1
            && !wrapper->isReadList1Full) {
            // 保存到队列去,然后取出来进行解码播放
            wrapper->list1->push_back(*dstAVPacket);
            //LOGW("readData() video wrapper->list1->size(): %d\n", wrapper->list1->size());
            if (wrapper->list1->size() == wrapper->maxAVPacketsCount) {
                // 不能排序,不然画面很模糊
                // wrapper->list1->sort(AVPacketNode());

                wrapper->isReadList1Full = true;
                // list1满了,接着存到list2去
                wrapper->nextRead = NEXT_READ_LIST2;

                if (isLocal && needLocalLog) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("readData() audio list1 満了\n");
                        LOGD("readData() audio list2 Size : %ld\n", wrapper->list2->size());
                    } else {
                        LOGW("readData() video list1 満了\n");
                        LOGW("readData() video list2 Size : %ld\n", wrapper->list2->size());
                    }
                }

                notifyToHandle(wrapper);
            }
        } else if (wrapper->nextRead == NEXT_READ_LIST2
                   && !wrapper->isReadList2Full) {
            wrapper->list2->push_back(*dstAVPacket);
            //LOGW("readData() video wrapper->list2->size(): %d\n", wrapper->list2->size());
            if (wrapper->list2->size() == wrapper->maxAVPacketsCount) {
                // wrapper->list2->sort(AVPacketNode());

                wrapper->isReadList2Full = true;
                wrapper->nextRead = NEXT_READ_LIST1;

                if (isLocal && needLocalLog) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("readData() audio list2 満了\n");
                        LOGD("readData() audio list1 Size : %ld\n", wrapper->list1->size());
                        /*if (audioWrapper != NULL
                            && audioWrapper->father != NULL
                            && !audioWrapper->father->isPausedForCache) {
                            if (srcAVPacket->data) {
                                av_packet_unref(srcAVPacket);
                                av_frame_unref(decodedAVFrame);
                            }
                            continue;
                        }*/
                    } else {
                        LOGW("readData() video list2 満了\n");
                        LOGW("readData() video list1 Size : %ld\n", wrapper->list1->size());
                    }
                }

                notifyToHandle(wrapper);
            }
        } else if (wrapper->isReadList1Full
                   && wrapper->isReadList2Full) {
            // 两个队列都满的话,就进行等待
            if (isLocal && needLocalLog) {
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("readData() audio list1和list2都満了,好开心( ^_^ )\n");
                    LOGD("readData() audio 休息一下\n");
                } else {
                    LOGW("readData() video list1和list2都満了,好开心( ^_^ )\n");
                    LOGW("readData() video 休息一下\n");
                }
            }

            notifyToReadWait(wrapper);

            if (isLocal && needLocalLog) {
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("readData() audio 结束休息\n");
                } else {
                    LOGW("readData() video 结束休息\n");
                }
            }

            // 保存"等待前的一帧"
            if (wrapper->nextRead == NEXT_READ_LIST1
                && !wrapper->isReadList1Full) {
                if (isLocal && needLocalLog) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("readData() audio next LIST1\n");
                    } else {
                        LOGW("readData() video next LIST1\n");
                    }
                }
                wrapper->list1->push_back(*dstAVPacket);
            } else if (wrapper->nextRead == NEXT_READ_LIST2
                       && !wrapper->isReadList2Full) {
                if (isLocal && needLocalLog) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("readData() audio next LIST2\n");
                    } else {
                        LOGW("readData() video next LIST2\n");
                    }
                }
                wrapper->list2->push_back(*dstAVPacket);
            }
        }
    }

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
            // 单位: 秒
            LOGD("readData() audio seconds: %d\n", (int) wrapper->duration);
            LOGD("readData() audio          %02d:%02d:%02d\n", hours, mins, seconds);
            LOGD("%s\n", "readData() audio start");
        } else {
            LOGW("readData() video seconds: %d\n", (int) wrapper->duration);
            LOGW("readData() video          %02d:%02d:%02d\n", hours, mins, seconds);
            LOGW("%s\n", "readData() video start");
        }

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *dstAVPacket = av_packet_alloc();

        int readFrame = -1;
        wrapper->isReading = true;
        for (;;) {
            // seekTo

            // exit
            if (!wrapper->isReading) {
                // for (;;) end
                break;
            }

            // 读一帧,如果是想要的流,那么break
            while (1) {
                // exit
                if (!wrapper->isReading) {
                    // while(1) end
                    break;
                }

                // 0 if OK, < 0 on error or end of file
                readFrame = av_read_frame(wrapper->avFormatContext, srcAVPacket);
                //LOGI("readFrame           : %d\n", readFrame);
                if (readFrame < 0) {
                    if (readFrame == AVERROR_EOF) {
                        wrapper->isReading = false;
                        // readData() video AVERROR_EOF readFrame: -541478725
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGF("readData() audio AVERROR_EOF readFrame: %d\n", readFrame);
                        } else {
                            LOGF("readData() video AVERROR_EOF readFrame: %d\n", readFrame);
                        }
                        if (wrapper->list1->size() > 0) {
                            wrapper->isReadList1Full = true;
                        }
                        if (wrapper->list2->size() > 0) {
                            wrapper->isReadList2Full = true;
                        }

                        // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                        if ((wrapper->isReadList1Full
                             && !wrapper->isReadList2Full
                             && wrapper->list1->size() > 0
                             && wrapper->list2->size() == 0)
                            || wrapper->isPausedForCache) {
                            if (wrapper->type == TYPE_AUDIO) {
                                LOGD("readData() audio signal() handleLockCondition break\n");
                            } else {
                                LOGW("readData() video signal() handleLockCondition break\n");
                            }

                            notifyToHandle(wrapper);
                        }

                        // while(1) end
                        break;
                    }

                    continue;
                }

                if (srcAVPacket->stream_index != wrapper->streamIndex) {
                    // 遇到其他流时释放
                    av_packet_unref(srcAVPacket);
                    continue;
                }

                break;
            }// while(1) end 成功读到一帧数据

            // exit
            if (!wrapper->isReading) {
                // for (;;) end
                break;
            }

            readDataImpl(wrapper, srcAVPacket, dstAVPacket);

            av_packet_unref(srcAVPacket);
        }// for(;;) end

        av_packet_unref(srcAVPacket);
        srcAVPacket = NULL;

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("readData() audio readFramesCount          : %d\n", wrapper->readFramesCount);
            LOGD("%s\n", "readData() audio end");
        } else {
            LOGW("readData() video readFramesCount          : %d\n", wrapper->readFramesCount);
            LOGW("%s\n", "readData() video end");
        }
        return NULL;
    }

    int handleAudioDataImpl(AVStream *stream, AVFrame *decodedAVFrame) {
        // 转换音频
        int ret = swr_convert(
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
            //LOGD("handleData() audio audioTimeDifference: %lf\n", audioTimeDifference);
#endif
            // 显示时间进度
            long progress = (long) audioTimeDifference;
            if (progress > preProgress) {
                preProgress = progress;
                onProgressUpdated(progress);
            }

            // 获取给定音频参数所需的缓冲区大小
            int out_buffer_size = av_samples_get_buffer_size(
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
    }

    int handleVideoDataImpl(AVStream *stream, AVFrame *decodedAVFrame) {
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
            && onlyOneVideo) {
            LOGW("handleVideoData() 音视频都已经准备好,开始播放!!!\n");
            onlyOneVideo = false;
            // 回调(通知到java层)
            onPlayed();
        }
#endif

#ifdef USE_AUDIO
        /***
         以音频为基准,同步视频到音频
         1.视频慢了则加快播放或丢掉部分视频帧
         2.视频快了则延迟播放,继续渲染上一帧
         音频需要正常播放才是好的体验
         */
        if (videoTimeDifference < audioTimeDifference) {
            // 正常情况下videoTimeDifference比audioTimeDifference大一些
            // 如果发现小了,说明视频播放慢了,应丢弃这些帧
            // break后videoTimeDifference增长的速度会加快
            return 0;
        }
        double tempTimeDifference = videoTimeDifference - audioTimeDifference;
        if (tempTimeDifference > 2.000000) {
            // 不好的现象.为什么会出现这种情况还不知道?
            LOGE("handleVideoDataImpl() audioTimeDifference: %lf\n", audioTimeDifference);
            LOGE("handleVideoDataImpl() videoTimeDifference: %lf\n", videoTimeDifference);
            LOGE("handleVideoDataImpl() video - audio      : %lf\n", tempTimeDifference);
            videoTimeDifference = videoTimeDifferencePre + maxVideoTimeDifference;
            //videoTimeDifference = videoTimeDifferencePre + maxVideoTimeDifference + timeDifferenceWithAV;
            LOGE("handleVideoDataImpl() videoTimeDifference: %lf\n", videoTimeDifference);
        }
        // 如果videoTimeDifference比audioTimeDifference大出了一定的范围
        // 那么说明视频播放快了,应等待音频
        while (videoTimeDifference - audioTimeDifference > TIME_DIFFERENCE) {
            videoSleep(1);
        }
#endif

        //int64_t curAVFramePts = decodedAVFrame->pts;
        //videoTimeDifference = curAVFramePtsVideo * av_q2d(stream->time_base);
        /*double timeDifference = (curAVFramePtsVideo - preAVFramePtsVideo) * av_q2d(stream->time_base);
        preAVFramePtsVideo = curAVFramePtsVideo;*/

        if (videoWrapper->father->isHandling) {
            // 3.lock锁定下一个即将要绘制的Surface
            ANativeWindow_lock(pANativeWindow, &outBuffer, NULL);
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
            uint8_t *src = videoWrapper->rgbAVFrame->data[0];
            // 一行的长度
            int srcStride = videoWrapper->rgbAVFrame->linesize[0];
            uint8_t *dst = (uint8_t *) outBuffer.bits;
            int dstStride = outBuffer.stride * 4;
            // 由于window的stride和帧的stride不同,因此需要逐行复制
            for (int h = 0; h < videoWrapper->srcHeight; h++) {
                memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
            }

            double timeDifference = (curAVFramePtsVideo - preAVFramePtsVideo) * av_q2d(stream->time_base);
            preAVFramePtsVideo = curAVFramePtsVideo;
            // timeDifference = 0.040000
            // 单位: 毫秒
            long tempSleep = timeDifference * 1000;
            tempSleep -= 30;
            tempSleep += step;
            if (videoSleepTime != tempSleep) {
                videoSleepTime = tempSleep;
            }
            //LOGW("handleVideoDataImpl() videoSleepTime     : %ld\n", videoSleepTime);
            if (videoSleepTime < 15 && videoSleepTime > 0) {
                videoSleep(videoSleepTime);
            } else {
                if (videoSleepTime > 0) {
                    // 好像是个比较合理的值
                    videoSleep(11);
                }
                // videoSleepTime <= 0时不需要sleep
            }

            // 6.unlock绘制
            ANativeWindow_unlockAndPost(pANativeWindow);
        }
    }

    void *handleData(void *opaque) {
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
        if (wrapper == NULL) {
            LOGE("%s\n", "wrapper is NULL");
            return NULL;
        }

        // 线程等待
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() wait() audio start\n");
        } else {
            LOGW("handleData() wait() video start\n");
        }
        notifyToHandleWait(wrapper);
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() wait() audio end\n");
        } else {
            LOGW("handleData() wait() video end\n");
        }

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() for (;;) audio start\n");
        } else {
            LOGW("handleData() for (;;) video start\n");
            LOGW("handleData() ANativeWindow_setBuffersGeometry() start\n");
            // 2.设置缓冲区的属性（宽、高、像素格式）,像素格式要和SurfaceView的像素格式一直
            ANativeWindow_setBuffersGeometry(pANativeWindow,
                                             videoWrapper->srcWidth,
                                             videoWrapper->srcHeight,
                                             WINDOW_FORMAT_RGBA_8888);
            LOGW("handleData() ANativeWindow_setBuffersGeometry() end\n");
        }

        audioTimeDifference = 0.0;
        videoTimeDifference = 0.0;
        totalTimeDifference = 0.0;
        totalTimeDifferenceCount = 0.0;
        onlyOneVideo = true;
        curAVFramePtsVideo = 0;
        preAVFramePtsVideo = 0;

        bool testCode = false;
        bool decodedError = false;
        int MaxRunTimes = 0;
        wrapper->isHandling = true;
        int ret = 0, out_buffer_size = 0;
        AVStream *stream = wrapper->avFormatContext->streams[wrapper->streamIndex];

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *dstAVPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = NULL;
        if (wrapper->type == TYPE_AUDIO) {
            decodedAVFrame = audioWrapper->decodedAVFrame;
        } else {
            decodedAVFrame = videoWrapper->decodedAVFrame;
        }
        AVFrame *dstAVFrame = av_frame_alloc();

        for (;;) {
            // 暂停装置
            if (wrapper->isPausedForUser
                || wrapper->isPausedForCache) {
                bool isPausedForUser = wrapper->isPausedForUser;
                if (isPausedForUser) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() User  audio start\n");
                    } else {
                        LOGW("handleData() wait() User  video start\n");
                    }
                } else {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() Cache audio start\n");
                    } else {
                        LOGW("handleData() wait() Cache video start\n");
                    }
                }
                notifyToHandleWait(wrapper);
                if (isPausedForUser) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() User  audio end\n");
                    } else {
                        LOGW("handleData() wait() User  video end\n");
                    }
                } else {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() Cache audio end\n");
                    } else {
                        LOGW("handleData() wait() Cache video end\n");
                    }
                }
            }// 暂停装置 end

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (testCode) {
                if (wrapper->type == TYPE_AUDIO) {
                } else {
                    bool haveAVFrame = false;
                    // 先从队列里取
                    if (wrapper->tempList->size() != 0) {
                        std::list<AVFrame>::iterator iter;
                        for (iter = wrapper->tempList->begin();
                             iter != wrapper->tempList->end();
                             iter++) {
                            AVFrame avFrame = *iter;
                            curAVFramePtsVideo = avFrame.pts;
                            videoTimeDifference = curAVFramePtsVideo * av_q2d(stream->time_base);
                            videoTimeDifference_l = round(videoTimeDifference * 1000000);
                            if ((maxVideoTimeDifference_l + videoTimeDifferencePre_l) >= videoTimeDifference_l) {
                                decodedAVFrame = &avFrame;
                                videoTimeDifferencePre = videoTimeDifference;
                                videoTimeDifferencePre_l = round(videoTimeDifferencePre * 1000000);
                                haveAVFrame = true;

                                // 播放声音和渲染画面
                                if (wrapper->type == TYPE_AUDIO) {
                                    handleAudioDataImpl(stream, decodedAVFrame);
                                } else {
                                    handleVideoDataImpl(stream, decodedAVFrame);
                                }

                                wrapper->tempList->erase(iter);
                                LOGI("handleData() 找到一个: %lf\n", videoTimeDifference);

                                break;
                            }
                        }
                    }
                    if (haveAVFrame) {
                        continue;
                    }
                }
            }

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (wrapper->nextHandle == NEXT_HANDLE_LIST1
                && wrapper->isReadList1Full
                && wrapper->list1->size() > 0) {
                srcAVPacket = &(wrapper->list1->front());
                // 内容copy
                av_packet_ref(dstAVPacket, srcAVPacket);
                av_packet_unref(srcAVPacket);
                wrapper->list1->pop_front();
                wrapper->handleFramesCount++;

                if (wrapper->list1->size() == 0) {
                    wrapper->list1->clear();
                    wrapper->isReadList1Full = false;
                    wrapper->nextHandle = NEXT_HANDLE_LIST2;
                    if (isLocal && needLocalLog) {
                        LOGD("handleData() list1 用完了\n");
                        LOGD("handleData() list2 isReadList2Full : %d\n", wrapper->isReadList2Full);
                        LOGD("handleData() list2 AVFramesCount   : %d\n", wrapper->list2->size());
                    }
                    if (wrapper->isReading) {
                        if (isLocal && needLocalLog) {
                            LOGD("handleData() list1 空了,快读吧\n");
                        }
                        notifyToRead(wrapper);
                    }
                }
            } else if (wrapper->nextHandle == NEXT_HANDLE_LIST2
                       && wrapper->isReadList2Full
                       && wrapper->list2->size() > 0) {
                srcAVPacket = &(wrapper->list2->front());
                av_packet_ref(dstAVPacket, srcAVPacket);
                av_packet_unref(srcAVPacket);
                wrapper->list2->pop_front();
                wrapper->handleFramesCount++;

                if (wrapper->list2->size() == 0) {
                    wrapper->list2->clear();
                    wrapper->isReadList2Full = false;
                    wrapper->nextHandle = NEXT_HANDLE_LIST1;
                    if (isLocal && needLocalLog) {
                        LOGD("handleData() list2 用完了\n");
                        LOGD("handleData() list1 isReadList1Full : %d\n", wrapper->isReadList1Full);
                        LOGD("handleData() list1 AVFramesCount   : %d\n", wrapper->list1->size());
                    }
                    if (wrapper->isReading) {
                        if (isLocal && needLocalLog) {
                            LOGD("handleData() list2 空了,快读吧\n");
                        }
                        notifyToRead(wrapper);
                    }
                }
            } else if (wrapper->isReading
                       && !wrapper->isReadList1Full
                       && !wrapper->isReadList2Full) {
                // 开始暂停
                onPaused();

                if (wrapper->type == TYPE_AUDIO) {
                    // 音频Cache引起的暂停

                    // 让视频也同时暂停
                    if (videoWrapper != NULL && videoWrapper->father != NULL) {
                        videoWrapper->father->isPausedForCache = true;
                    }
                    LOGE("handleData() wait() audio Cache start\n");
                    notifyToHandleWait(audioWrapper->father);
                    LOGE("handleData() wait() audio Cache end\n");

                    // 通知视频结束暂停
                    notifyToHandle(videoWrapper->father);
                } else {
                    // 视频Cache引起的暂停

                    // 让音频也同时暂停
                    if (audioWrapper != NULL && audioWrapper->father != NULL) {
                        audioWrapper->father->isPausedForCache = true;
                    }
                    LOGE("handleData() wait() video Cache start\n");
                    notifyToHandleWait(videoWrapper->father);
                    LOGE("handleData() wait() video Cache end\n");

                    // 通知音频结束暂停
                    notifyToHandle(audioWrapper->father);
                }

                // 开始播放
                onPlayed();
                continue;
            } else if (!wrapper->isReading
                       && wrapper->list1->size() == 0
                       && wrapper->list2->size() == 0) {
                wrapper->isHandling = false;
                // for (;;) end
                break;
            }

            if (!dstAVPacket) {
                if (!wrapper->isReading
                    && wrapper->list1->size() == 0
                    && wrapper->list2->size() == 0) {
                    // for (;;) end
                    break;
                }
                continue;
            }

            /*if (wrapper->type == TYPE_AUDIO) {
            } else {
                int64_t pts = dstAVPacket->pts;
                double packetPts = pts * av_q2d(stream->time_base);
                LOGD("handleData() video packetPts             : %lf\n", packetPts);
            }*/

            // 解码过程
            ret = avcodec_send_packet(wrapper->avCodecContext, dstAVPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    LOGE("readData() avcodec_send_packet   ret: %d\n", ret);
                    av_packet_unref(dstAVPacket);
                    continue;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("audio 发送数据包到解码器时出错 %d", ret);
                    } else {
                        LOGE("video 发送数据包到解码器时出错 %d", ret);
                    }
                    wrapper->isReading = false;
                    break;
                case 0:
                default:
                    break;
            }// switch (ret) end

            if (!wrapper->isReading) {
                // for (;;) end
                break;
            }

            /*while (1) {
                ret = avcodec_receive_frame(wrapper->avCodecContext, decodedAVFrame);
                switch (ret) {
                    // 输出是不可用的,必须发送新的输入
                    case AVERROR(EAGAIN):
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGE("readData() audio avcodec_receive_frame ret: %d\n", ret);
                        } else {
                            LOGE("readData() video avcodec_receive_frame ret: %d\n", ret);
                            decodedError = true;
                            videoSleep(11);
                        }
                        break;
                    case AVERROR(EINVAL):
                        // codec打不开,或者是一个encoder
                    case AVERROR_EOF:
                        // 已经完全刷新,不会再有输出帧了
                        wrapper->isReading = false;
                        break;
                    case 0: {
                        // 解码成功,返回一个输出帧
                        break;
                    }
                    default:
                        // 合法的解码错误
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGE("audio 从解码器接收帧时出错 %d", ret);
                        } else {
                            LOGE("video 从解码器接收帧时出错 %d", ret);
                            decodedError = true;
                        }
                        break;
                }// switch (ret) end

                break;
            }*/// while(1) end 解码后的一帧数据

            ret = avcodec_receive_frame(wrapper->avCodecContext, decodedAVFrame);
            if (ret == AVERROR(EAGAIN)) {
                if (wrapper->type == TYPE_AUDIO) {
                    LOGE("readData() audio avcodec_receive_frame ret: %d\n", ret);
                } else {
                    LOGE("readData() video avcodec_receive_frame ret: %d\n", ret);
                    videoSleep(11);
                }
                continue;
            }
            if (ret == 0) {
                ///////////////////////////////////////////////////////////////////

                if (wrapper->type == TYPE_AUDIO) {
                } else {
                    curAVFramePtsVideo = decodedAVFrame->pts;
                    videoTimeDifference = curAVFramePtsVideo * av_q2d(stream->time_base);
                    videoTimeDifference_l = round(videoTimeDifference * 1000000);
                    if (videoTimeDifference > 0
                        && audioTimeDifference > 0
                        && videoTimeDifference > audioTimeDifference
                        && MaxRunTimes <= 10) {
                        MaxRunTimes++;
                        double tempMaxVideoTimeDifference = videoTimeDifference - videoTimeDifferencePre;
                        if (tempMaxVideoTimeDifference > maxVideoTimeDifference) {
                            maxVideoTimeDifference = tempMaxVideoTimeDifference;
                            maxVideoTimeDifference_l = round(maxVideoTimeDifference * 1000000);
                            LOGW("handleData() video maxVideoTimeDifference: %lf\n", maxVideoTimeDifference);
                        }
                        timeDifferenceWithAV = videoTimeDifference - audioTimeDifference;
                        LOGW("handleData() video timeDifferenceWithAV  : %lf\n", timeDifferenceWithAV);
                    }

                    // 0.016667
                    // 0.100000 0.116667 0.133333 0.150000 0.166667 0.183333 0.200000
                    /***
                     如果有10几帧解码失败了,然后出现下一帧的时间戳很大
                     videoTimeDifferencePre: 406.583333
                     maxVideoTimeDifference: 0.125000
                     videoTimeDifference   : 425.541667
                     */

                    if (wrapper->type == TYPE_AUDIO) {
                    } else {
                        LOGI("handleData() videoTimeDifferencePre: %lf\n", videoTimeDifferencePre);
                        LOGD("handleData() audioTimeDifference   : %lf\n", audioTimeDifference);
                        LOGW("handleData() videoTimeDifference   : %lf\n", videoTimeDifference);
                    }
                    if ((maxVideoTimeDifference_l + videoTimeDifferencePre_l) >= videoTimeDifference_l
                        || (videoTimeDifference_l - (maxVideoTimeDifference_l + videoTimeDifferencePre_l)) <= 100000) {
                        // 当前帧的时间戳正常
                    } else {
                        if (testCode) {
                            if (!decodedError) {
                                // 内容先copy
                                av_frame_ref(dstAVFrame, decodedAVFrame);
                                // 然后放进list中
                                wrapper->tempList->push_back(*dstAVFrame);
                                // 进行排序
                                wrapper->tempList->sort(AVFrameNode());
                                LOGE("handleData() video videoTimeDifferencePre: %lf\n", videoTimeDifferencePre);
                                LOGE("handleData() video maxVideoTimeDifference: %lf\n", maxVideoTimeDifference);
                                LOGE("handleData() video videoTimeDifference   : %lf\n", videoTimeDifference);
                                LOGI("handleData() --------------------------------------------\n");

                                /*std::list<AVFrame>::iterator iter;
                                for (iter = wrapper->tempList->begin();
                                     iter != wrapper->tempList->end();
                                     iter++) {
                                    AVFrame avFrame = *iter;
                                    int64_t nowPts = avFrame.pts;
                                    double videoTimeDifference = nowPts * av_q2d(stream->time_base);
                                    LOGI("handleData() video tempNowPts: %lf\n", videoTimeDifference);
                                }*/

                                //videoTimeDifferencePre = videoTimeDifference;
                                continue;
                            } else {
                                decodedError = false;
                                LOGE("handleData() video videoTimeDifferencePre: %lf\n", videoTimeDifferencePre);
                                LOGE("handleData() video maxVideoTimeDifference: %lf\n", maxVideoTimeDifference);
                                LOGE("handleData() video videoTimeDifference   : %lf\n", videoTimeDifference);
                                LOGI("handleData() ============================================\n");
                            }
                        }
                    };
                }

                // 播放声音和渲染画面
                if (wrapper->type == TYPE_AUDIO) {
                    handleAudioDataImpl(stream, decodedAVFrame);
                } else {
                    handleVideoDataImpl(stream, decodedAVFrame);
                }

                if (wrapper->type == TYPE_AUDIO) {
                } else {
                    videoTimeDifferencePre = videoTimeDifference;
                    videoTimeDifferencePre_l = round(videoTimeDifferencePre * 1000000);
                    /*if (videoTimeDifference > videoTimeDifferencePre) {
                    }*/
                }

                ///////////////////////////////////////////////////////////////////
            }

            av_packet_unref(dstAVPacket);
        }//for(;;) end
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() for (;;) audio end\n");
        } else {
            LOGW("handleData() for (;;) video end\n");
        }

        av_packet_unref(srcAVPacket);
        av_packet_free(&srcAVPacket);
        srcAVPacket = NULL;
//        av_frame_unref(decodedAVFrame);
//        av_frame_free(&decodedAVFrame);
//        decodedAVFrame = NULL;
        av_frame_unref(dstAVFrame);
        av_frame_free(&dstAVFrame);
        dstAVFrame = NULL;

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() audio handleFramesCount : %d\n", wrapper->handleFramesCount);
        } else {
            LOGW("handleData() video handleFramesCount : %d\n", wrapper->handleFramesCount);
        }
        if (wrapper->readFramesCount == wrapper->handleFramesCount) {
            if (wrapper->type == TYPE_AUDIO) {
                LOGD("%s\n", "handleData() audio正常播放完毕");
            } else {
                LOGW("%s\n", "handleData() video正常播放完毕");
            }
        }
        stop();
        if (wrapper->type == TYPE_AUDIO) {
            closeAudio();
        } else {
            closeVideo();
        }

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("%s\n", "handleData() audio end");
        } else {
            LOGW("%s\n", "handleData() video end");
        }
        return NULL;

    }

    void closeAudio() {
        // audio
        if (audioWrapper == NULL || audioWrapper->father == NULL) {
            return;
        }
        LOGD("%s\n", "closeAudio() start");
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
        /*int count = audioWrapper->father->queue1->allAVPacketsCount;
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
        audioWrapper->father->queue2 = NULL;*/

        //av_free(audioWrapper->father->list1);
        //av_free(audioWrapper->father->list2);
        if (audioWrapper->father->list1->size() != 0) {
            LOGD("%s\n", "closeAudio() list1 is not empty, %d\n", audioWrapper->father->list1->size());
        }
        if (audioWrapper->father->list2->size() != 0) {
            LOGD("%s\n", "closeAudio() list1 is not empty, %d\n", audioWrapper->father->list2->size());
        }
        delete (audioWrapper->father->list1);
        delete (audioWrapper->father->list2);
        audioWrapper->father->list1 = NULL;
        audioWrapper->father->list2 = NULL;

        av_free(audioWrapper->father);
        audioWrapper->father = NULL;
        av_free(audioWrapper);
        audioWrapper = NULL;

        LOGD("%s\n", "closeAudio() end");
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
        LOGW("%s\n", "closeVideo() start");
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
        /*int count = videoWrapper->father->queue1->allAVPacketsCount;
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
        videoWrapper->father->queue2 = NULL;*/

        //av_free(videoWrapper->father->list1);
        //av_free(videoWrapper->father->list2);
        if (videoWrapper->father->list1->size() != 0) {
            LOGW("%s\n", "closeVideo() list1 is not empty, %d\n", videoWrapper->father->list1->size());
        }
        if (videoWrapper->father->list2->size() != 0) {
            LOGW("%s\n", "closeVideo() list1 is not empty, %d\n", videoWrapper->father->list2->size());
        }
        delete (videoWrapper->father->list1);
        delete (videoWrapper->father->list2);
        videoWrapper->father->list1 = NULL;
        videoWrapper->father->list2 = NULL;

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
        LOGW("%s\n", "closeVideo() end");
    }

    int initAudioPlayer() {
        LOGD("%s\n", "initAudioPlayer() start");

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

        LOGD("%s\n", "initAudioPlayer() end");

        return 0;
    }


    int initVideoPlayer() {
        LOGW("%s\n", "initVideoPlayer() start");

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

        LOGW("%s\n", "initVideoPlayer() end");

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
        if (pANativeWindow != NULL) {
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = 0;
        }
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
            audioWrapper->father->isReadList1Full = false;
            audioWrapper->father->isReadList2Full = false;
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
            videoWrapper->father->isReadList1Full = false;
            videoWrapper->father->isReadList2Full = false;
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

            audioWrapper->father->nextRead = NEXT_READ_LIST1;
            audioWrapper->father->nextHandle = NEXT_HANDLE_LIST1;
            audioWrapper->father->isReadList1Full = false;
            audioWrapper->father->isReadList2Full = false;

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

            videoWrapper->father->nextRead = NEXT_READ_LIST1;
            videoWrapper->father->nextHandle = NEXT_HANDLE_LIST1;
            videoWrapper->father->isReadList1Full = false;
            videoWrapper->father->isReadList2Full = false;

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
        sprintf(dest, "videoSleepTime: %ld, step: %ld\n", videoSleepTime, step);
        onInfo(dest);
        LOGI("stepAdd()      videoSleepTime: %ld, step: %ld\n", videoSleepTime, step);
    }

    void stepSubtract() {
        step--;
        char dest[50];
        sprintf(dest, "videoSleepTime: %ld, step: %ld\n", videoSleepTime, step);
        onInfo(dest);
        LOGI("stepSubtract() videoSleepTime: %ld, step: %ld\n", videoSleepTime, step);
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
