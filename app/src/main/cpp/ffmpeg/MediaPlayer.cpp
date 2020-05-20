//
// Created by root on 19-8-8.
//








/***
usleep(1000*40);//等待40毫秒
av_gettime_relative() 单位:微秒

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

#include <string>
#include "MediaPlayer.h"

#define LOG "player_alexander"

static char inFilePath[2048];
AVFormatContext *avFormatContext = NULL;
struct AudioWrapper *audioWrapper = NULL;
struct VideoWrapper *videoWrapper = NULL;
static pthread_mutex_t readLockMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t readLockCondition = PTHREAD_COND_INITIALIZER;
bool isLocal = false;
bool isReading = false;
bool isVideoHandling = false;
bool isInterrupted = false;
bool runOneTime = true;
// seek时间
int64_t timeStamp = -1;
static long curProgress = 0;
static long preProgress = 0;
// 视频播放时每帧之间的暂停时间,单位为ms
static int videoSleepTime = 11;

double TIME_DIFFERENCE = 1.000000;// 0.180000
// 当前音频时间戳
static double audioPts = 0;
static double preAudioPts = 0;
// 当前视频时间戳
static double videoPts = 0;
static double preVideoPts = 0;
// 上一个时间戳
static double videoPtsPre = 0;

ANativeWindow *pANativeWindow = NULL;
// 绘制时的缓冲区
static ANativeWindow_Buffer mANativeWindow_Buffer;

extern int use_mode;

namespace alexander_media {

#define RUN_COUNTS 88
    int runCounts = 0;
    double averageTimeDiff = 0;
    double timeDiff[RUN_COUNTS];

    long mediaDuration = -1;
    // 读线程超时变量
    int64_t startReadTime = -1;
    int64_t endReadTime = -1;

    int64_t startVideoLockedTime = -1;
    int64_t endVideoLockedTime = -1;

    static int frameRate = 0;
    bool isVideoLocked = false;

    ///////////////////////////////////////////////////////

    // true表示只下载,不播放
    static bool onlyDownloadNotPlayback = false;
    static bool needToDownload = false;
    static bool isInitSuccess = false;
    // 下载时音视频为一个文件
    static char outFilePath[2048];
    static AVFormatContext *avFormatContextOutput = NULL;
    static int64_t *dts_start_from = NULL;
    static int64_t *pts_start_from = NULL;
    // 下载时音视频分开
    static char videoOutFilePath[2048];// 2048
    static char audioOutFilePath[2048];// 2048
    static AVFormatContext *avFormatContextVideoOutput = NULL;
    static AVFormatContext *avFormatContextAudioOutput = NULL;
    static AVStream *video_out_stream = NULL;
    static AVStream *audio_out_stream = NULL;

    FILE *videoFile = NULL;
    FILE *audioFile = NULL;

    ///////////////////////////////////////////////////////

    char *getStrAVPixelFormat(AVPixelFormat format);

    void closeDownload();

    void closeOther();

    /***
     在Android logcat中打印FFmpeg调试信息
     https://zhuanlan.zhihu.com/p/48384062
     FFmpeg日志输出到adb logcat
     https://blog.csdn.net/matrix_laboratory/article/details/57080891
     */
    static void log_callback_null(void *ptr, int level, const char *fmt, va_list vl) {
        static int print_prefix = 1;
        static int count;
        static char prev[1024];
        char line[1024];
        static int is_atty;

        av_log_format_line(ptr, level, fmt, vl, line, sizeof(line), &print_prefix);

        strcpy(prev, line);
        //sanitize((uint8_t *)line);

        if (level <= AV_LOG_WARNING) {
            LOGE("%s", line);
        } else {
            LOGI("%s", line);
        }
    }

    void notifyToRead() {
        pthread_mutex_lock(&readLockMutex);
        pthread_cond_signal(&readLockCondition);
        pthread_mutex_unlock(&readLockMutex);
    }

    void notifyToReadWait() {
        pthread_mutex_lock(&readLockMutex);
        pthread_cond_wait(&readLockCondition, &readLockMutex);
        pthread_mutex_unlock(&readLockMutex);
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

    int read_thread_interrupt_cb(void *opaque) {
        // 必须通过传参方式进行判断,不能用全局变量判断
        AudioWrapper *audioWrapper = (AudioWrapper *) opaque;
        endReadTime = av_gettime_relative();
        if (!audioWrapper->father->isReading) {
            LOGE("read_thread_interrupt_cb() 退出\n");
            return 1;
        } else if ((audioWrapper->father->isPausedForCache || !audioWrapper->father->isStarted)
                   && startReadTime > 0
                   && (endReadTime - startReadTime) > MAX_RELATIVE_TIME) {
            if (audioWrapper->father->list1->size() < audioWrapper->father->list1LimitCounts
                && videoWrapper->father->list1->size() < videoWrapper->father->list1LimitCounts
                && audioWrapper->father->list2->size() < audioWrapper->father->list1LimitCounts
                && videoWrapper->father->list2->size() < videoWrapper->father->list1LimitCounts) {
                LOGE("read_thread_interrupt_cb() 读取数据超时\n");
                isInterrupted = true;
                onError(0x101, "读取数据超时");
                return 1;
            }
        }
        return 0;
    }

    // 已经不需要调用了
    void initAV() {
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avcodec_register_all();

        avdevice_register_all();

        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        avformat_network_init();

        // 打印ffmpeg里面的日志
        //av_log_set_callback(log_callback_null);

        LOGW("ffmpeg [av_version_info()] version: %s\n", av_version_info());

        /*AVCodec *codecName = av_codec_next(NULL);
        while (codecName) {
            switch (codecName->type) {
                case AVMEDIA_TYPE_VIDEO: {
                    LOGI("ffmpeg AVMEDIA_TYPE_VIDEO: %s\n", codecName->name);
                    break;
                }
                case AVMEDIA_TYPE_AUDIO: {
                    LOGI("ffmpeg AVMEDIA_TYPE_AUDIO: %s\n", codecName->name);
                    break;
                }
                default: {
                    LOGI("ffmpeg other: %s\n", codecName->name);
                    break;
                }
            }
            codecName = codecName->next;
        }*/

        readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        readLockCondition = PTHREAD_COND_INITIALIZER;
        TIME_DIFFERENCE = 1.000000;
        videoSleepTime = 11;
        preProgress = 0;
        audioPts = 0.0;
        videoPts = 0.0;
        preAudioPts = 0.0;
        preVideoPts = 0.0;
        videoPtsPre = 0;
        runOneTime = true;
        runCounts = 0;
        averageTimeDiff = 0.0;
        memset(timeDiff, '0', sizeof(timeDiff));
        isInterrupted = false;
        startReadTime = -1;
        endReadTime = -1;
        mediaDuration = -1;
        isVideoLocked = false;
        startVideoLockedTime = -1;
        endVideoLockedTime = -1;
        isReading = false;
        onlyDownloadNotPlayback = false;
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
        if (isLocal) {
            audioWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_AUDIO_LOCAL;
        } else {
            audioWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_AUDIO_HTTP;
        }
        audioWrapper->father->list2LimitCounts = MAX_AVPACKET_COUNT;
        LOGD("initAudio() list1LimitCounts: %d\n", audioWrapper->father->list1LimitCounts);
        LOGD("initAudio() list2LimitCounts: %d\n", audioWrapper->father->list2LimitCounts);
        audioWrapper->father->streamIndex = -1;
        audioWrapper->father->readFramesCount = 0;
        audioWrapper->father->handleFramesCount = 0;
        audioWrapper->father->isStarted = false;
        audioWrapper->father->isReading = true;
        audioWrapper->father->isHandling = true;
        audioWrapper->father->isPausedForUser = false;
        audioWrapper->father->isPausedForCache = false;
        audioWrapper->father->isPausedForSeek = false;
        audioWrapper->father->needToSeek = false;
        audioWrapper->father->allowDecode = false;
        audioWrapper->father->isHandleList1Full = false;
//        audioWrapper->father->isReadList2Full = false;
        audioWrapper->father->list1 = new std::list<AVPacket>();
        audioWrapper->father->list2 = new std::list<AVPacket>();

        audioWrapper->father->duration = -1;
        audioWrapper->father->timestamp = -1;
        audioWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        audioWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        audioWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;
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
        if (isLocal) {
            videoWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_VIDEO_LOCAL;
        } else {
            videoWrapper->father->list1LimitCounts = MAX_AVPACKET_COUNT_VIDEO_HTTP;
        }
        videoWrapper->father->list2LimitCounts = MAX_AVPACKET_COUNT;
        LOGW("initVideo() list1LimitCounts: %d\n", videoWrapper->father->list1LimitCounts);
        LOGW("initVideo() list2LimitCounts: %d\n", videoWrapper->father->list2LimitCounts);
        videoWrapper->father->streamIndex = -1;
        videoWrapper->father->readFramesCount = 0;
        videoWrapper->father->handleFramesCount = 0;
        videoWrapper->father->isStarted = false;
        videoWrapper->father->isReading = true;
        videoWrapper->father->isHandling = true;
        videoWrapper->father->isPausedForUser = false;
        videoWrapper->father->isPausedForCache = false;
        videoWrapper->father->isPausedForSeek = false;
        videoWrapper->father->needToSeek = false;
        videoWrapper->father->allowDecode = false;
        videoWrapper->father->isHandleList1Full = false;
//        videoWrapper->father->isReadList2Full = false;
        videoWrapper->father->list1 = new std::list<AVPacket>();
        videoWrapper->father->list2 = new std::list<AVPacket>();

        videoWrapper->father->duration = -1;
        videoWrapper->father->timestamp = -1;
        videoWrapper->father->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->readLockCondition = PTHREAD_COND_INITIALIZER;
        videoWrapper->father->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoWrapper->father->handleLockCondition = PTHREAD_COND_INITIALIZER;
    }

    int initDownload() {
        //avformat_alloc_output_context2(&avFormatContextOutput, NULL, NULL, outFilePath);
        //AVOutputFormat *out_fmt = avFormatContextOutput->oformat;
        AVOutputFormat *out_fmt = av_guess_format(NULL, outFilePath, NULL);
        if (!out_fmt) {
            LOGE("initDownload() out_fmt is NULL.\n");
            return -1;
        }

        if (avFormatContextOutput != NULL) {
            //avformat_close_input(&avFormatContextOutput);
            avformat_free_context(avFormatContextOutput);
            avFormatContextOutput = NULL;
        }
        avFormatContextOutput = avformat_alloc_context();
        avFormatContextOutput->oformat = out_fmt;

        int nb_streams = avFormatContext->nb_streams;
        /*for (int i = 0; i < nb_streams; i++) {
            AVStream *in_stream = avFormatContext->streams[i];
            AVStream *out_stream = avformat_new_stream(avFormatContextOutput, NULL);
            if (!out_stream) {
                LOGE("initDownload() out_stream is NULL.\n");
                return -1;
            }
            avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
            out_stream->codecpar->codec_tag = 0;
        }*/

        audio_out_stream = avformat_new_stream(avFormatContextOutput, NULL);
        video_out_stream = avformat_new_stream(avFormatContextOutput, NULL);
        if (!audio_out_stream) {
            LOGE("initDownload() audio_out_stream is NULL.\n");
            return -1;
        }
        if (!video_out_stream) {
            LOGE("initDownload() video_out_stream is NULL.\n");
            return -1;
        }
        AVCodecParameters *audio_codecpar = audioWrapper->father->avStream->codecpar;
        AVCodecParameters *video_codecpar = videoWrapper->father->avStream->codecpar;
        int ret = avcodec_parameters_copy(audio_out_stream->codecpar, audio_codecpar);
        if (ret < 0) {
            LOGE("initDownload() audio avcodec_parameters_copy occurs error.\n");
            return -1;
        }
        ret = avcodec_parameters_copy(video_out_stream->codecpar, video_codecpar);
        if (ret < 0) {
            LOGE("initDownload() video avcodec_parameters_copy occurs error.\n");
            return -1;
        }
        audio_out_stream->codecpar->codec_tag = 0;
        video_out_stream->codecpar->codec_tag = 0;

        ret = avio_open(&avFormatContextOutput->pb, outFilePath, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("initDownload() avio_open occurs error.\n");
            return -1;
        }

        // 写头部信息
        ret = avformat_write_header(avFormatContextOutput, NULL);
        if (ret < 0) {
            LOGE("initDownload() avformat_write_header occurs error.\n");
            return -1;
        }

        // 根据流数量申请空间，并全部初始化为0
        if (dts_start_from != NULL) {
            free(dts_start_from);
            dts_start_from = NULL;
        }
        if (pts_start_from != NULL) {
            free(pts_start_from);
            pts_start_from = NULL;
        }
        size_t size = sizeof(int64_t) * nb_streams;
        dts_start_from = (int64_t *) malloc(size);
        pts_start_from = (int64_t *) malloc(size);
        if (!dts_start_from) {
            LOGE("initDownload() dts_start_from is NULL.\n");
            return -1;
        }
        if (!pts_start_from) {
            LOGE("initDownload() pts_start_from is NULL.\n");
            return -1;
        }
        memset(dts_start_from, 0, size);
        memset(pts_start_from, 0, size);

        return 0;
    }

    int initDownload2() {
        int ret = -1;
        AVOutputFormat *video_out_fmt = av_guess_format(NULL, videoOutFilePath, NULL);
        if (!video_out_fmt) {
            LOGE("initDownload() video_out_fmt is NULL.\n");
            return -1;
        }
        AVOutputFormat *audio_out_fmt = av_guess_format(NULL, audioOutFilePath, NULL);
        if (!audio_out_fmt) {
            LOGE("initDownload() audio_out_fmt is NULL.\n");
            return -1;
        }

        if (avFormatContextVideoOutput != NULL) {
            //avformat_close_input(&avFormatContextVideoOutput);
            avformat_free_context(avFormatContextVideoOutput);
            avFormatContextVideoOutput = NULL;
        }
        avFormatContextVideoOutput = avformat_alloc_context();
        avFormatContextVideoOutput->oformat = video_out_fmt;
        video_out_stream = avformat_new_stream(avFormatContextVideoOutput, NULL);
        if (!video_out_stream) {
            LOGE("initDownload() video_out_stream is NULL.\n");
            return -1;
        }
        if (avFormatContextAudioOutput != NULL) {
            //avformat_close_input(&avFormatContextAudioOutput);
            avformat_free_context(avFormatContextAudioOutput);
            avFormatContextAudioOutput = NULL;
        }
        avFormatContextAudioOutput = avformat_alloc_context();
        avFormatContextAudioOutput->oformat = audio_out_fmt;
        audio_out_stream = avformat_new_stream(avFormatContextAudioOutput, NULL);
        if (!audio_out_stream) {
            LOGE("initDownload() audio_out_stream is NULL.\n");
            return -1;
        }

        AVStream *videoAVStream = videoWrapper->father->avStream;
        AVCodecParameters *video_codecpar = videoAVStream->codecpar;
        AVStream *audioAVStream = audioWrapper->father->avStream;
        AVCodecParameters *audio_codecpar = audioAVStream->codecpar;

        ret = avcodec_parameters_copy(video_out_stream->codecpar, video_codecpar);
        //ret = avcodec_copy_context(video_out_stream->codec, videoAVStream->codec);
        if (ret < 0) {
            LOGE("initDownload() video avcodec_parameters_copy occurs error.\n");
            return -1;
        }
        ret = avcodec_parameters_copy(audio_out_stream->codecpar, audio_codecpar);
        if (ret < 0) {
            LOGE("initDownload() audio avcodec_parameters_copy occurs error.\n");
            return -1;
        }

        video_out_stream->codecpar->codec_tag = 0;
        audio_out_stream->codecpar->codec_tag = 0;
        ret = avio_open(&avFormatContextVideoOutput->pb, videoOutFilePath, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("initDownload() video avio_open occurs error.\n");
            return -1;
        }
        ret = avio_open(&avFormatContextAudioOutput->pb, audioOutFilePath, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("initDownload() audio avio_open occurs error.\n");
            return -1;
        }

        // 写头部信息
        ret = avformat_write_header(avFormatContextVideoOutput, NULL);
        if (ret < 0) {
            LOGE("initDownload() video avformat_write_header occurs error.\n");
            return -1;
        }
        ret = avformat_write_header(avFormatContextAudioOutput, NULL);
        if (ret < 0) {
            LOGE("initDownload() audio avformat_write_header occurs error.\n");
            return -1;
        }

        return 0;
    }

    int initDownload3() {
        audioFile = fopen(audioOutFilePath, "wb");
        videoFile = fopen(videoOutFilePath, "wb");
        if (!audioFile || !videoFile) {
            return -1;
        }

        return 0;
    }

    int openAndFindAVFormatContext() {
        LOGI("openAndFindAVFormatContext() start\n");
        if (avFormatContext != NULL) {
            LOGI("openAndFindAVFormatContext() videoAVFormatContext isn't NULL\n");
            avformat_free_context(avFormatContext);
            avFormatContext = NULL;
        }
        avFormatContext = avformat_alloc_context();
        if (avFormatContext == NULL) {
            LOGE("videoAVFormatContext is NULL.\n");
            return -1;
        }
        if (!isLocal) {
            avFormatContext->interrupt_callback.callback = read_thread_interrupt_cb;
            avFormatContext->interrupt_callback.opaque = audioWrapper;
            /*AVDictionary *options = NULL;
            av_dict_set(&options, "stimeout", "10000000", 0);*/
            int64_t startTime = av_gettime_relative();
            startReadTime = startTime;
            /***
             -104(Connection reset by peer)
             -875574520(Server returned 404 Not Found)
             -1094995529(Invalid data found when processing input)
             -1330794744(Protocol not found)
             */
            int ret = avformat_open_input(&avFormatContext,
                                          inFilePath,
                                          NULL, NULL);
            if (ret) {
                char buf[1024];
                av_strerror(ret, buf, 1024);
                LOGE("Couldn't open file, because this: [ %d(%s) ]", ret, buf);
                // 这里就是某些视频初始化失败的地方
                LOGE("Couldn't open input stream.\n");
                return -1;
            }
            int64_t endTime = av_gettime_relative();
            LOGI("openAndFindAVFormatContext() avformat_open_input: %ld\n",
                 (long) ((endTime - startTime) / 1000));
        } else {
            if (avformat_open_input(&avFormatContext,
                                    inFilePath,
                                    NULL, NULL) != 0) {
                LOGE("Couldn't open input stream.\n");
                return -1;
            }
        }
        if (avformat_find_stream_info(avFormatContext, NULL) != 0) {
            LOGE("Couldn't find stream information.\n");
            return -1;
        }
        LOGI("openAndFindAVFormatContext() end\n");
        return 0;
    }

    int findStreamIndex() {
        if (avFormatContext == NULL) {
            return -1;
        }
        LOGI("findStreamIndex() start\n");
        // stream counts
        int streams = avFormatContext->nb_streams;
        LOGI("Stream counts   : %d\n", streams);
        for (int i = 0; i < streams; i++) {
            // 得到AVCodecParameters
            AVCodecParameters *avCodecParameters = avFormatContext->streams[i]->codecpar;
            if (avCodecParameters != NULL) {
                AVMediaType mediaType = avCodecParameters->codec_type;
                switch (mediaType) {
                    case AVMEDIA_TYPE_AUDIO: {
                        audioWrapper->father->streamIndex = i;
                        audioWrapper->father->avCodecParameters = avCodecParameters;
                        LOGD("audioStreamIndex: %d\n", audioWrapper->father->streamIndex);
                        break;
                    }
                    case AVMEDIA_TYPE_VIDEO: {
                        videoWrapper->father->streamIndex = i;
                        videoWrapper->father->avCodecParameters = avCodecParameters;
                        LOGW("videoStreamIndex: %d\n", videoWrapper->father->streamIndex);
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        if (audioWrapper->father->streamIndex != -1
            && videoWrapper->father->streamIndex != -1) {
            use_mode = USE_MODE_MEDIA;
            videoWrapper->father->avStream = avFormatContext->streams[videoWrapper->father->streamIndex];
            audioWrapper->father->avStream = avFormatContext->streams[audioWrapper->father->streamIndex];
            LOGI("findStreamIndex() USE_MODE_MEDIA\n");
        } else if (audioWrapper->father->streamIndex == -1
                   && videoWrapper->father->streamIndex != -1) {
            use_mode = USE_MODE_ONLY_VIDEO;
            videoWrapper->father->avStream = avFormatContext->streams[videoWrapper->father->streamIndex];
            LOGI("findStreamIndex() USE_MODE_ONLY_VIDEO\n");
        } else if (audioWrapper->father->streamIndex != -1
                   && videoWrapper->father->streamIndex == -1) {
            use_mode = USE_MODE_ONLY_AUDIO;
            audioWrapper->father->avStream = avFormatContext->streams[audioWrapper->father->streamIndex];
            LOGI("findStreamIndex() USE_MODE_ONLY_AUDIO\n");
        } else {
            LOGE("Didn't find audio or video stream.\n");
            return -1;
        }

        LOGI("findStreamIndex() end\n");
        return 0;
    }

    int findAndOpenAVCodecForAudio() {
        if (use_mode == USE_MODE_ONLY_VIDEO) {
            return 0;
        }
        LOGI("findAndOpenAVCodecForAudio() start\n");
        // audio
        if (audioWrapper->father->streamIndex != -1) {
            // 获取音频解码器
            // 先通过AVCodecParameters找到AVCodec
            AVCodecID codecID = audioWrapper->father->avCodecParameters->codec_id;
            LOGD("findAndOpenAVCodecForAudio() codecID: %d\n", codecID);
            // audio是没有下面这些东西的
            switch (codecID) {
                case AV_CODEC_ID_FLAC: {
                    LOGD("findAndOpenAVCodecForAudio() AV_CODEC_ID_FLAC\n");
                    // 硬解码264
                    /*audioWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name(
                            "h264_mediacodec");*/
                    break;
                }
                case AV_CODEC_ID_AAC: {
                    LOGD("findAndOpenAVCodecForAudio() AV_CODEC_ID_AAC\n");
                    break;
                }
                case AV_CODEC_ID_AAC_LATM: {
                    LOGD("findAndOpenAVCodecForAudio() AV_CODEC_ID_AAC_LATM\n");
                    break;
                }
                default: {
                    // 软解
                    // audioWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
                    break;
                }
            }
            audioWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
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
            } else {
                LOGE("findAndOpenAVCodecForAudio() audioWrapper->father->decoderAVCodec is NULL\n");
                audioWrapper->father->avCodecContext =
                        avFormatContext->streams[audioWrapper->father->streamIndex]->codec;
                if (audioWrapper->father->avCodecContext != NULL) {
                    audioWrapper->father->decoderAVCodec =
                            avcodec_find_decoder(audioWrapper->father->avCodecContext->codec_id);
                    if (audioWrapper->father->decoderAVCodec != NULL) {
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
        }
        if (audioWrapper->father->avCodecContext == NULL) {
            LOGE("findAndOpenAVCodecForAudio() audioWrapper->father->avCodecContext is NULL\n");
            return -1;
        }

        LOGI("findAndOpenAVCodecForAudio() end\n");
        return 0;
    }

    int findAndOpenAVCodecForVideo() {
        if (use_mode == USE_MODE_ONLY_AUDIO) {
            return 0;
        }
        LOGI("findAndOpenAVCodecForVideo() start\n");
        // video
        if (videoWrapper->father->streamIndex != -1) {
            videoWrapper->father->decoderAVCodec = NULL;
            // avcodec_find_decoder_by_name
            AVCodecID codecID = videoWrapper->father->avCodecParameters->codec_id;
            switch (codecID) {
                case AV_CODEC_ID_HEVC: {
                    LOGW("findAndOpenAVCodecForVideo() hevc_mediacodec\n");
                    // 硬解码265
                    videoWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name(
                            "hevc_mediacodec");
                    break;
                }
                case AV_CODEC_ID_H264: {
                    LOGW("findAndOpenAVCodecForVideo() h264_mediacodec\n");
                    // 硬解码264
                    videoWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name(
                            "h264_mediacodec");
                    break;
                }
                case AV_CODEC_ID_MPEG4: {
                    LOGW("findAndOpenAVCodecForVideo() mpeg4_mediacodec\n");
                    // 硬解码mpeg4
                    videoWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name(
                            "mpeg4_mediacodec");
                    break;
                }
                default: {
                    LOGW("findAndOpenAVCodecForVideo() codecID\n");
                    // 软解
                    // videoWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
                    break;
                }
            }
            /*if (!videoWrapper->father->decoderAVCodec) {
                // 有相应的so库时这句就不要执行了
                videoWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
            }*/
            videoWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
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

        LOGI("findAndOpenAVCodecForVideo() end\n");
        return 0;
    }

    int createSwrContent() {
        if (use_mode == USE_MODE_ONLY_VIDEO) {
            return 0;
        }
        LOGI("createSwrContent() start\n");
        audioWrapper->dstSampleRate = 44100;
        audioWrapper->dstAVSampleFormat = AV_SAMPLE_FMT_S16;
        audioWrapper->dstChannelLayout = AV_CH_LAYOUT_STEREO;

        // src
        audioWrapper->srcSampleRate = audioWrapper->father->avCodecContext->sample_rate;
        audioWrapper->srcNbChannels = audioWrapper->father->avCodecContext->channels;
        audioWrapper->srcAVSampleFormat = audioWrapper->father->avCodecContext->sample_fmt;
        audioWrapper->srcNbSamples = audioWrapper->father->avCodecContext->frame_size;
        audioWrapper->srcChannelLayout = audioWrapper->father->avCodecContext->channel_layout;
        LOGD("---------------------------------\n");
        LOGD("srcSampleRate       : %d\n", audioWrapper->srcSampleRate);// 48000 48000 48000
        LOGD("srcNbChannels       : %d\n", audioWrapper->srcNbChannels);// 2 6 0
        LOGD("srcAVSampleFormat   : %d\n", audioWrapper->srcAVSampleFormat);// 8 -1 -1
        LOGD("srcNbSamples        : %d\n", audioWrapper->srcNbSamples);// 1024 0 0
        LOGD("srcChannelLayout1   : %d\n", audioWrapper->srcChannelLayout);// 3 0 0
        // 有些视频从源视频中得到的channel_layout与使用函数得到的channel_layout结果是一样的
        // 但是还是要使用函数得到的channel_layout为好
        audioWrapper->srcChannelLayout = av_get_default_channel_layout(audioWrapper->srcNbChannels);
        LOGD("srcChannelLayout2   : %d\n", audioWrapper->srcChannelLayout);// 3 63 0
        LOGD("---------------------------------\n");

        // dst
        // Android中跟音频有关的参数: dstSampleRate dstNbChannels 位宽
        // dstSampleRate, dstAVSampleFormat和dstChannelLayout指定
        // 然后通过下面处理后在Java端就能创建AudioTrack对象了
        // 不然像有些5声道,6声道就创建不了,因此没有声音
        audioWrapper->dstSampleRate = audioWrapper->srcSampleRate;
        audioWrapper->dstNbSamples = audioWrapper->srcNbSamples;
        audioWrapper->dstNbChannels = av_get_channel_layout_nb_channels(
                audioWrapper->dstChannelLayout);

        LOGD("dstSampleRate       : %d\n", audioWrapper->dstSampleRate);// 48000 48000
        LOGD("dstNbChannels       : %d\n", audioWrapper->dstNbChannels);// 2 2
        LOGD("dstAVSampleFormat   : %d\n", audioWrapper->dstAVSampleFormat);// 1 1
        LOGD("dstNbSamples        : %d\n", audioWrapper->dstNbSamples);// 1024 0
        LOGD("---------------------------------\n");
        /*if (audioWrapper->dstNbSamples == 0) {
            audioWrapper->dstNbSamples = 1024;
        }*/

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

        LOGI("createSwrContent() end\n");
        return 0;
    }

    int createSwsContext() {
        if (use_mode == USE_MODE_ONLY_AUDIO) {
            return 0;
        }
        LOGI("createSwsContext() start\n");
        // Android支持的目标像素格式
        // AV_PIX_FMT_RGB32
        // AV_PIX_FMT_RGBA
        videoWrapper->dstAVPixelFormat = AV_PIX_FMT_RGBA;

        videoWrapper->srcWidth = videoWrapper->father->avCodecContext->width;
        videoWrapper->srcHeight = videoWrapper->father->avCodecContext->height;
        videoWrapper->srcAVPixelFormat = videoWrapper->father->avCodecContext->pix_fmt;

        int64_t bit_rate = videoWrapper->father->avCodecContext->bit_rate;
        int bit_rate_tolerance = videoWrapper->father->avCodecContext->bit_rate_tolerance;
        int bits_per_coded_sample = videoWrapper->father->avCodecContext->bits_per_coded_sample;
        int bits_per_raw_sample = videoWrapper->father->avCodecContext->bits_per_raw_sample;
        int delay = videoWrapper->father->avCodecContext->delay;
        int frame_number = videoWrapper->father->avCodecContext->frame_number;
        int frame_size = videoWrapper->father->avCodecContext->frame_size;
        int level = videoWrapper->father->avCodecContext->level;
        LOGW("---------------------------------\n");
        LOGW("bit_rate            : %ld\n", (long) bit_rate);
        LOGW("bit_rate_tolerance  : %d\n", bit_rate_tolerance);
        LOGW("bits_per_coded_sample: %d\n", bits_per_coded_sample);
        LOGW("bits_per_raw_sample : %d\n", bits_per_raw_sample);
        LOGW("delay               : %d\n", delay);
        LOGW("level               : %d\n", level);
        LOGW("frame_size          : %d\n", frame_size);
        LOGW("frame_number        : %d\n", frame_number);

        AVStream *stream = avFormatContext->streams[videoWrapper->father->streamIndex];
        // 帧数
        int64_t videoFrames = stream->nb_frames;
        /***
         帧率:fps每秒显示帧数(frames) Frame per Second(fps)/Hz
         码率:bps(比特率,数据率)确定整体视频/音频质量的参数，秒为单位处理的字节数，码率和视频质量成正比
         肉眼想看到连续移动图像至少需要15帧。
         值得一提的是手机视频拍摄中，无论是720P还是1080P基本都是30帧每秒。
         目前受到硬件迭代更新的影响，拍摄帧率有达到60fps/120fps，但基本还是30的倍数。
         电影放映的标准是每秒放映24帧，中国的电视、广告、动画播放每秒25帧，
         这个帧率属于PAL制式，在亚洲和欧洲电视台较为常用，而美国加拿大一般都是NTSC制式每秒29.97帧。

         看起来好像比较正常
         30fps 1480kbps
         30fps 6704kbps

         30fps 12897kbps快
         30fps 14739kbps快
         30fps 11697kbps快
         30fps 1948kbps 快
         29fps 1119kbps 快
         25fps 895kbps  快
         29fps 622kbps  快
         23fps 1212kbps 快
         29fps 38572kbps慢
         */
        if (stream->avg_frame_rate.den) {
            frameRate = stream->avg_frame_rate.num / stream->avg_frame_rate.den;
        }
        int bitRate = avFormatContext->bit_rate / 1000;
        LOGW("videoFrames         : %d\n", (long) videoFrames);
        LOGW("frameRate           : %d fps/Hz\n", frameRate);
        LOGW("bitRate             : %d kbps\n", bitRate);
        LOGW("srcWidth            : %d\n", videoWrapper->srcWidth);
        LOGW("srcHeight           : %d\n", videoWrapper->srcHeight);
        LOGW("srcAVPixelFormat    : %d %s\n",
             videoWrapper->srcAVPixelFormat, getStrAVPixelFormat(videoWrapper->srcAVPixelFormat));
        LOGW("dstAVPixelFormat    : %d %s\n",// 在initVideo()中初始化设置
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
        videoWrapper->father->outBuffer1 =
                (unsigned char *) av_malloc(videoWrapper->father->outBufferSize);
        int imageFillArrays = av_image_fill_arrays(videoWrapper->rgbAVFrame->data,
                                                   videoWrapper->rgbAVFrame->linesize,
                                                   videoWrapper->father->outBuffer1,
                                                   videoWrapper->dstAVPixelFormat,
                                                   videoWrapper->srcWidth,
                                                   videoWrapper->srcHeight,
                                                   1);
        LOGW("imageGetBufferSize1 : %d\n", imageGetBufferSize);
        LOGW("imageGetBufferSize2 : %d\n", videoWrapper->father->outBufferSize);
        LOGW("imageFillArrays     : %d\n", imageFillArrays);
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

        if (frameRate <= 23) {
            TIME_DIFFERENCE = 0.000600;
        } else if (frameRate == 24) {
            TIME_DIFFERENCE = 0.500000;
        }
        LOGI("createSwsContext()    TIME_DIFFERENCE    : %lf\n", TIME_DIFFERENCE);

        LOGI("createSwsContext() end\n");
        return 0;
    }

    int seekToImpl() {
        // seekTo
        LOGI("seekToImpl() sleep start\n");
        while (!audioWrapper->father->needToSeek
               || !videoWrapper->father->needToSeek) {
            if (!audioWrapper->father->isHandling || !videoWrapper->father->isHandling) {
                return 0;
            }
            av_usleep(1000);
        }
        LOGI("seekToImpl() sleep end\n");
        LOGD("seekToImpl() audio list2 size: %d\n", audioWrapper->father->list2->size());
        LOGD("seekToImpl() video list2 size: %d\n", videoWrapper->father->list2->size());
        if (audioWrapper->father->list2->size() != 0) {
            std::list<AVPacket>::iterator iter;
            for (iter = audioWrapper->father->list2->begin();
                 iter != audioWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
            audioWrapper->father->list2->clear();
        }
        if (videoWrapper->father->list2->size() != 0) {
            std::list<AVPacket>::iterator iter;
            for (iter = videoWrapper->father->list2->begin();
                 iter != videoWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
            videoWrapper->father->list2->clear();
        }
        LOGI("seekToImpl() av_seek_frame start\n");
        LOGI("seekToImpl() timestamp: %ld\n", (long) timeStamp);
        //LOGI("seekToImpl() timestamp: %"PRIu64"\n", timestamp);
        av_seek_frame(avFormatContext, -1,
                      timeStamp * AV_TIME_BASE,
                      AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
        // 清空解码器的缓存
        avcodec_flush_buffers(audioWrapper->father->avCodecContext);
        avcodec_flush_buffers(videoWrapper->father->avCodecContext);
        timeStamp = -1;
        preProgress = 0;
        videoPtsPre = 0;
        audioWrapper->father->isPausedForSeek = false;
        videoWrapper->father->isPausedForSeek = false;
        audioWrapper->father->isStarted = false;
        videoWrapper->father->isStarted = false;
        LOGI("seekToImpl() av_seek_frame end\n");
        LOGI("==================================================================\n");
        return 0;
    }

    int downloadImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);

        AVStream *in_stream, *out_stream;
        in_stream = avFormatContext->streams[copyAVPacket->stream_index];
        out_stream = avFormatContextOutput->streams[copyAVPacket->stream_index];

        // 将截取后的每个流的起始dts,pts保存下来,作为开始时间,用来做后面的时间基转换
        if (dts_start_from[copyAVPacket->stream_index] == 0) {
            dts_start_from[copyAVPacket->stream_index] = copyAVPacket->dts;
        }
        if (pts_start_from[copyAVPacket->stream_index] == 0) {
            pts_start_from[copyAVPacket->stream_index] = copyAVPacket->pts;
        }

        // 时间基转换
        copyAVPacket->pts = av_rescale_q_rnd(
                copyAVPacket->pts - pts_start_from[copyAVPacket->stream_index],
                in_stream->time_base,
                out_stream->time_base,
                (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        copyAVPacket->dts = av_rescale_q_rnd(
                copyAVPacket->dts - dts_start_from[copyAVPacket->stream_index],
                in_stream->time_base,
                out_stream->time_base,
                (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));

        if (copyAVPacket->pts < 0) {
            copyAVPacket->pts = 0;
        }
        if (copyAVPacket->dts < 0) {
            copyAVPacket->dts = 0;
        }

        copyAVPacket->duration = (int) av_rescale_q(
                (int64_t) copyAVPacket->duration,
                in_stream->time_base,
                out_stream->time_base);
        copyAVPacket->pos = -1;

        // 一帧视频播放时间必须在解码时间点之后,当出现pkt.pts < pkt.dts时会导致程序异常,
        // 所以我们丢掉有问题的帧,不会有太大影响
        if (copyAVPacket->pts < copyAVPacket->dts) {
            return 0;
        }

        av_interleaved_write_frame(avFormatContextOutput, copyAVPacket);

        /*AVRational time_base;
        if (wrapper->type == TYPE_AUDIO) {
            time_base = audio_out_stream->time_base;
        } else {
            time_base = video_out_stream->time_base;
        }
        copyAVPacket->pts = av_rescale_q_rnd(copyAVPacket->pts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->dts = av_rescale_q_rnd(copyAVPacket->dts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->duration = av_rescale_q(copyAVPacket->duration,
                                              wrapper->avStream->time_base,
                                              time_base);
        copyAVPacket->pos = -1;
        //将包写到输出媒体文件
        av_interleaved_write_frame(avFormatContextOutput, copyAVPacket);
        copyAVPacket->stream_index = 0;*/

        av_packet_unref(copyAVPacket);
    }

    // 音视频分开保存时
    int downloadImpl2(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);

        AVRational time_base;
        if (wrapper->type == TYPE_AUDIO) {
            time_base = audio_out_stream->time_base;
        } else {
            time_base = video_out_stream->time_base;
        }
        copyAVPacket->pts = av_rescale_q_rnd(copyAVPacket->pts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->dts = av_rescale_q_rnd(copyAVPacket->dts,
                                             wrapper->avStream->time_base,
                                             time_base,
                                             (AVRounding) (AV_ROUND_NEAR_INF |
                                                           AV_ROUND_PASS_MINMAX));
        copyAVPacket->duration = av_rescale_q(copyAVPacket->duration,
                                              wrapper->avStream->time_base,
                                              time_base);
        copyAVPacket->pos = -1;
        copyAVPacket->stream_index = 0;
        //将包写到输出媒体文件
        if (wrapper->type == TYPE_AUDIO) {
            //LOGD("downloadImpl2() audio copyAVPacket->pts: %d\n", (long) copyAVPacket->pts);
            av_interleaved_write_frame(avFormatContextAudioOutput, copyAVPacket);
        } else {
            //LOGW("downloadImpl2() video copyAVPacket->pts: %d\n", (long) copyAVPacket->pts);
            av_interleaved_write_frame(avFormatContextVideoOutput, copyAVPacket);
        }

        av_packet_unref(copyAVPacket);
    }

    int downloadImpl3(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);

        if (wrapper->type == TYPE_AUDIO) {
            fwrite(copyAVPacket->data, copyAVPacket->size, 1, audioFile);
        } else {
            fwrite(copyAVPacket->data, copyAVPacket->size, 1, videoFile);
        }

        av_packet_unref(copyAVPacket);
    }

    int readDataImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        wrapper->readFramesCount++;
        // 复制数据
        av_packet_ref(copyAVPacket, srcAVPacket);
        av_packet_unref(srcAVPacket);

        // 保存数据
        pthread_mutex_lock(&wrapper->readLockMutex);
        wrapper->list2->push_back(*copyAVPacket);
        size_t list2Size = wrapper->list2->size();
        pthread_mutex_unlock(&wrapper->readLockMutex);

        if (!isLocal) {
            if (wrapper->type == TYPE_AUDIO) {
                if (list2Size % 10 == 0) {
                    onLoadProgressUpdated(0x1003, list2Size);
                }
                /*if (list2Size % 500 == 0) {
                    LOGD("readDataImpl() audio list2Size: %d\n", list2Size);
                }*/
            } else {
                if (list2Size % 10 == 0) {
                    onLoadProgressUpdated(0x1000, list2Size);
                }
                /*if (list2Size % 500 == 0) {
                    LOGW("readDataImpl() video list2Size: %d\n", list2Size);
                }*/
            }
        }

        if (!wrapper->isHandleList1Full
            && list2Size == wrapper->list1LimitCounts) {
            /***
             什么时候走这里?
             1.开始播放缓冲好的时候
             2.因为缓冲原因
             3.seek后
             */
            // 下面两个都不行
            // std::move(wrapper->list2->begin(), wrapper->list2->end(), std::back_inserter(wrapper->list1));
            // wrapper->list1->swap((std::list<AVPacket> &) wrapper->list2);
            // 把list2中的内容全部复制给list1
            pthread_mutex_lock(&wrapper->readLockMutex);
            wrapper->list1->clear();
            wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
            wrapper->list2->clear();
            wrapper->isHandleList1Full = true;
            pthread_mutex_unlock(&wrapper->readLockMutex);
            notifyToHandle(wrapper);

            if (wrapper->type == TYPE_AUDIO) {
                onLoadProgressUpdated(0x1003, 0);
                onLoadProgressUpdated(0x1004, wrapper->list1->size());
                LOGD("readDataImpl() audio 填满数据了\n");
            } else {
                onLoadProgressUpdated(0x1001, 0);
                onLoadProgressUpdated(0x1002, wrapper->list1->size());
                LOGW("readDataImpl() video 填满数据了\n");
            }
        } else if (wrapper->type == TYPE_VIDEO
                   && list2Size >= wrapper->list2LimitCounts) {
            LOGI("readDataImpl() audio list1: %d\n", audioWrapper->father->list1->size());
            LOGI("readDataImpl() video list1: %d\n", videoWrapper->father->list1->size());
            LOGI("readDataImpl() audio list2: %d\n", audioWrapper->father->list2->size());
            LOGI("readDataImpl() video list2: %d\n", videoWrapper->father->list2->size());
            if (audioWrapper->father->list2->size() > audioWrapper->father->list1LimitCounts) {
                LOGD("readDataImpl() notifyToReadWait start\n");
                notifyToReadWait(videoWrapper->father);
                LOGD("readDataImpl() notifyToReadWait end\n");
            }
        }
        return 0;
    }

    void *readData(void *opaque) {
        LOGI("%s\n", "readData() start");

        if (audioWrapper == NULL
            || audioWrapper->father == NULL
            || videoWrapper == NULL
            || videoWrapper->father == NULL) {
            return NULL;
        } else if (!audioWrapper->father->isReading
                   || !audioWrapper->father->isHandling
                   || !videoWrapper->father->isReading
                   || !videoWrapper->father->isHandling) {
            closeAudio();
            closeVideo();
            closeOther();
            onFinished();
            LOGF("%s\n", "readData() finish");
            return NULL;
        }

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();
        av_init_packet(srcAVPacket);
        srcAVPacket->data = NULL;
        srcAVPacket->size = 0;
        av_init_packet(copyAVPacket);
        copyAVPacket->data = NULL;
        copyAVPacket->size = 0;

        // seekTo
        if (timeStamp > 0) {
            LOGI("readData() timeStamp: %ld\n", (long) timeStamp);
            audioWrapper->father->needToSeek = true;
            videoWrapper->father->needToSeek = true;
            audioWrapper->father->isPausedForSeek = true;
            videoWrapper->father->isPausedForSeek = true;
        }

        /*if (needToDownload) {
            if (initDownload2() < 0) {
                isInitSuccess = false;
            } else {
                isInitSuccess = true;
            }
            LOGI("readData() isInitSuccess: %d\n", isInitSuccess);
        }*/

        isReading = true;
        /***
         有几种情况:
         1.list1中先存满n个,然后list2多次存取
         2.list1中先存满n个,然后list2一次性存满
         3.list1中还没满n个文件就读完了
         */
        for (;;) {
            // exit
            if (!audioWrapper->father->isReading
                && !videoWrapper->father->isReading) {
                // for (;;) end
                break;
            }

            // seekTo
            if ((audioWrapper->father->isPausedForSeek || videoWrapper->father->isPausedForSeek)
                && timeStamp >= 0) {
                seekToImpl();
            }

            startReadTime = av_gettime_relative();
            int readFrame = av_read_frame(avFormatContext, srcAVPacket);
            endReadTime = av_gettime_relative();

            if (isInterrupted) {
                stop();
                break;
            }

            // 0 if OK, < 0 on error or end of file
            if (readFrame < 0) {
                if (readFrame != -12 && readFrame != AVERROR_EOF) {
                    LOGE("readData() readFrame  : %d\n", readFrame);
                    continue;
                }
                // readData() AVERROR_EOF readFrame: -12 (Cannot allocate memory)
                // readData() AVERROR_EOF readFrame: -1094995529
                // readData() AVERROR_EOF readFrame: -1414092869 超时
                // readData() AVERROR_EOF readFrame: -541478725 文件已经读完了
                LOGF("readData() AVERROR_EOF: %d\n", AVERROR_EOF);
                LOGF("readData() readFrame  : %d\n", readFrame);
                LOGF("readData() audio list2: %d\n", audioWrapper->father->list2->size());
                LOGF("readData() video list2: %d\n", videoWrapper->father->list2->size());

                // 读到文件末尾了
                audioWrapper->father->isReading = false;
                videoWrapper->father->isReading = false;
                audioWrapper->father->isHandleList1Full = true;
                videoWrapper->father->isHandleList1Full = true;
                // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                notifyToHandle(audioWrapper->father);
                notifyToHandle(videoWrapper->father);

                /*pthread_mutex_lock(&readLockMutex);
                if (needToDownload && isInitSuccess) {
                    LOGI("readData() 文件读完,已经停止下载\n");
                    needToDownload = false;
                    isInitSuccess = false;
                    closeDownload();
                }
                pthread_mutex_unlock(&readLockMutex);*/

                if (onlyDownloadNotPlayback) {
                    onInfo("文件已读完");
                }

                // 不退出线程
                LOGI("readData() notifyToReadWait start\n");
                notifyToReadWait();
                LOGI("readData() notifyToReadWait end\n");
                if (audioWrapper->father->isPausedForSeek
                    || videoWrapper->father->isPausedForSeek) {
                    LOGF("readData() start seek\n");
                    audioWrapper->father->isReading = true;
                    videoWrapper->father->isReading = true;
                    continue;
                } else {
                    // for (;;) end
                    break;
                }
            }// 文件已读完

            // 关键帧的判断
            /*if (srcAVPacket->flags & AV_PKT_FLAG_KEY) {
                LOGI("read a key frame");
            }*/

            if (srcAVPacket->stream_index == audioWrapper->father->streamIndex) {
                if (audioWrapper->father->isReading) {
                    if (needToDownload && isInitSuccess) {
                        downloadImpl2(audioWrapper->father, srcAVPacket, copyAVPacket);
                    }
                    if (!onlyDownloadNotPlayback) {
                        readDataImpl(audioWrapper->father, srcAVPacket, copyAVPacket);
                    }
                }
            } else if (srcAVPacket->stream_index == videoWrapper->father->streamIndex) {
                if (videoWrapper->father->isReading) {
                    if (needToDownload && isInitSuccess) {
                        downloadImpl2(videoWrapper->father, srcAVPacket, copyAVPacket);
                    }
                    if (!onlyDownloadNotPlayback) {
                        readDataImpl(videoWrapper->father, srcAVPacket, copyAVPacket);
                    }
                }
            }
            if (onlyDownloadNotPlayback) {
                av_packet_unref(srcAVPacket);
            }
        }// for(;;) end

        pthread_mutex_lock(&readLockMutex);
        if (needToDownload && isInitSuccess) {
            LOGI("readData() 读线程退出,停止下载\n");
            needToDownload = false;
            isInitSuccess = false;
            closeDownload();
        }
        pthread_mutex_unlock(&readLockMutex);

        if (srcAVPacket != NULL) {
            av_packet_unref(srcAVPacket);
            srcAVPacket = NULL;
        }

        isReading = false;

        LOGF("%s\n", "readData() end");
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
        if (ret >= 0) {
            audioWrapper->father->isStarted = true;
            while (!videoWrapper->father->isStarted) {
                if (audioWrapper->father->isPausedForSeek
                    || !videoWrapper->father->isHandling
                    || !audioWrapper->father->isHandling) {
                    return 0;
                }
                av_usleep(1000);
            }
            if (runOneTime
                && audioWrapper->father->isStarted
                && videoWrapper->father->isStarted) {
                LOGD("handleAudioDataImpl() 音视频都已经准备好,开始播放!!!\n");
                runOneTime = false;
                // 回调(通知到java层)
                onPlayed();
            }

            ////////////////////////////////////////////////////////////////////

            audioPts = decodedAVFrame->pts * av_q2d(stream->time_base);
            if (mediaDuration < 0 && preAudioPts > 0 && preAudioPts > audioPts) {
                return 0;
            }
            preAudioPts = audioPts;
            //LOGD("handleVideoDataImpl() audioPts: %lf\n", audioPts);
            endVideoLockedTime = av_gettime_relative();
            /*if (!isLocal
                && mediaDuration < 0
                && startVideoLockedTime > 0
                && endVideoLockedTime > 0
                && (endVideoLockedTime - startVideoLockedTime) > 10000000) {
                // 说明video已经在ANativeWindow中被block住了
                LOGE("handleAudioDataImpl() video已经在ANativeWindow中被锁住了\n");
                //isVideoLocked = true;
                //stop();
            }*/

            // 有时长时才更新时间进度
            if (mediaDuration > 0) {
                curProgress = (long) audioPts;// 秒
                if (curProgress > preProgress
                    && curProgress <= mediaDuration) {
                    preProgress = curProgress;
                    onProgressUpdated(curProgress);
                }
            }

            ////////////////////////////////////////////////////////////////////

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
        } else {
            LOGE("audio 转换时出错 %d", ret);
        }
        return ret;
    }


    int handleVideoDataImpl(AVStream *stream, AVFrame *decodedAVFrame) {
        videoWrapper->father->isStarted = true;
        while (!audioWrapper->father->isStarted) {
            if (videoWrapper->father->isPausedForSeek
                || !audioWrapper->father->isHandling
                || !videoWrapper->father->isHandling) {
                LOGI("handleVideoDataImpl() videoWrapper->father->isStarted return\n");
                return 0;
            }
            av_usleep(1000);
        }

        /***
         以音频为基准,同步视频到音频
         1.视频慢了则加快播放或丢掉部分视频帧
         2.视频快了则延迟播放,继续渲染上一帧
         音频需要正常播放才是好的体验
         */
        videoPts = decodedAVFrame->pts * av_q2d(stream->time_base);
        if (mediaDuration < 0 && preVideoPts > 0 && preVideoPts > videoPts) {
            return 0;
        }
        preVideoPts = videoPts;
        //LOGW("handleVideoDataImpl() videoPts: %lf\n", videoPts);

        if (videoPts > 0 && audioPts > 0) {
            double tempTimeDifference = videoPts - audioPts;
            if (runCounts < RUN_COUNTS) {
                if (tempTimeDifference > 0) {
                    timeDiff[runCounts++] = tempTimeDifference;
                    //LOGI("handleVideoDataImpl() video - audio      : %lf\n", tempTimeDifference);
                }
            } else if (runCounts == RUN_COUNTS) {
                runCounts++;
                double totleTimeDiff = 0;
                for (int i = 0; i < RUN_COUNTS; i++) {
                    if (videoWrapper->father->isPausedForSeek
                        || !audioWrapper->father->isHandling
                        || !videoWrapper->father->isHandling) {
                        LOGI("handleVideoDataImpl() RUN_COUNTS return\n");
                        return 0;
                    }
                    totleTimeDiff += timeDiff[i];
                }
                averageTimeDiff = totleTimeDiff / RUN_COUNTS;
                LOGI("handleVideoDataImpl() frameRate: %d averageTimeDiff: %lf inFilePath: %s\n",
                     frameRate, averageTimeDiff, inFilePath);
                if (frameRate >= 24) {
                    if (averageTimeDiff > 0.300000 && averageTimeDiff <= 0.400000) {
                        TIME_DIFFERENCE = 0.200000;
                    } /*else if (averageTimeDiff > 0.400000 && averageTimeDiff <= 0.500000) {
                        TIME_DIFFERENCE = 0.300000;
                    } else if (averageTimeDiff > 0.500000 && averageTimeDiff <= 0.700000) {
                        TIME_DIFFERENCE = 0.400000;
                    } else if (averageTimeDiff > 0.700000) {
                        TIME_DIFFERENCE = 0.300000;
                    }*/ else if (averageTimeDiff > 0.400000) {
                        TIME_DIFFERENCE = 0.300000;
                    }
                    LOGI("handleVideoDataImpl() TIME_DIFFERENCE: %lf\n", TIME_DIFFERENCE);
                }
            }
            if (tempTimeDifference < 0) {
                // 正常情况下videoTimeDifference比audioTimeDifference大一些
                // 如果发现小了,说明视频播放慢了,应丢弃这些帧
                // break后videoTimeDifference增长的速度会加快
                // videoPts = audioPts + averageTimeDiff;
                return 0;
            }

            if (tempTimeDifference > 2.000000) {
                // 不好的现象.为什么会出现这种情况还不知道?
                //LOGE("handleVideoDataImpl() audioTimeDifference: %lf\n", audioPts);
                //LOGE("handleVideoDataImpl() videoTimeDifference: %lf\n", videoPts);
                //LOGE("handleVideoDataImpl() video - audio      : %lf\n", tempTimeDifference);
                videoPts = audioPts + averageTimeDiff;
                //LOGE("handleVideoDataImpl() videoTimeDifference: %lf\n", videoPts);
            }
            // 如果videoTimeDifference比audioTimeDifference大出了一定的范围
            // 那么说明视频播放快了,应等待音频
            while (videoPts - audioPts > TIME_DIFFERENCE) {
                if (videoWrapper->father->isPausedForSeek
                    || !audioWrapper->father->isHandling
                    || !videoWrapper->father->isHandling) {
                    LOGI("handleVideoDataImpl() TIME_DIFFERENCE return\n");
                    return 0;
                }
                av_usleep(1000);
            }
        }

        // 渲染画面
        if (videoWrapper->father->isHandling) {
            startVideoLockedTime = av_gettime_relative();
            // 3.lock锁定下一个即将要绘制的Surface
            //LOGW("handleVideoDataImpl() ANativeWindow_lock 1\n");
            ANativeWindow_lock(pANativeWindow, &mANativeWindow_Buffer, NULL);
            //LOGW("handleVideoDataImpl() ANativeWindow_lock 2\n");

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
            uint8_t *dst = (uint8_t *) mANativeWindow_Buffer.bits;
            int dstStride = mANativeWindow_Buffer.stride * 4;
            // 由于window的stride和帧的stride不同,因此需要逐行复制
            for (int h = 0; h < videoWrapper->srcHeight; h++) {
                /*if (videoWrapper->father->isPausedForSeek
                    || !videoWrapper->father->isHandling) {
                    LOGI("handleVideoDataImpl() memcpy return\n");
                    ANativeWindow_unlockAndPost(pANativeWindow);
                    return 0;
                }*/
                memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
            }

            ////////////////////////////////////////////////////////

            // timeDifference = 0.040000
            // 单位: 毫秒
            int tempSleep = ((int) ((videoPts - videoPtsPre) * 1000)) - 30;
            if (videoSleepTime != tempSleep) {
                videoSleepTime = tempSleep;
                //LOGW("handleVideoDataImpl() videoSleepTime     : %d\n", videoSleepTime);
            }
            if (videoSleepTime < 12 && videoSleepTime > 0) {
                videoSleep(videoSleepTime);
            } else {
                if (videoSleepTime > 0) {
                    // 好像是个比较合理的值
                    videoSleep(11);
                }
                // videoSleepTime <= 0时不需要sleep
            }
            videoPtsPre = videoPts;

            ////////////////////////////////////////////////////////

            // 6.unlock绘制
            //LOGW("handleVideoDataImpl() ANativeWindow_unlockAndPost 1\n");
            ANativeWindow_unlockAndPost(pANativeWindow);
            //LOGW("handleVideoDataImpl() ANativeWindow_unlockAndPost 2\n");
        }
    }

    int handleDataClose(Wrapper *wrapper) {
        // 让"读线程"退出
        notifyToRead();

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() for (;;) audio end\n");
            while (isReading) {
                av_usleep(1000);
            }
            LOGF("%s\n", "handleData() audio end");

            LOGD("handleData() audio                   list1: %d\n",
                 audioWrapper->father->list1->size());
            LOGD("handleData() audio                   list2: %d\n",
                 audioWrapper->father->list2->size());
            LOGW("handleData() video                   list1: %d\n",
                 wrapper->list1->size());
            LOGW("handleData() video                   list2: %d\n",
                 wrapper->list2->size());

            int64_t startTime = av_gettime_relative();
            int64_t endTime = -1;
            while (isVideoHandling) {
                endTime = av_gettime_relative();
                if ((endTime - startTime) >= 10000000) {
                    LOGE("%s\n", "Exception Exit");
                    break;
                }
                av_usleep(1000);
            }
            avcodec_flush_buffers(audioWrapper->father->avCodecContext);
            avcodec_flush_buffers(videoWrapper->father->avCodecContext);
            closeAudio();
            closeVideo();
            closeOther();
            // 必须保证每次退出都要执行到
            onFinished();
            if (isVideoLocked) {
                // throw exception
                audioWrapper->father->isStarted = false;
            }
            LOGF("%s\n", "Safe Exit");
        } else {
            LOGW("handleData() for (;;) video end\n");
            isVideoHandling = false;
            LOGF("%s\n", "handleData() video end");
        }
    }

    void *handleData(void *opaque) {
        if (!opaque) {
            return NULL;
        }
        Wrapper *wrapper = NULL;
        int *type = (int *) opaque;
        if (*type == TYPE_AUDIO) {
            wrapper = audioWrapper->father;
        } else {
            wrapper = videoWrapper->father;
        }
        if (!wrapper) {
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

        if (!wrapper->isHandling) {
            handleDataClose(wrapper);
            return NULL;
        }

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() for (;;) audio start\n");
        } else {
            LOGW("handleData() ANativeWindow_setBuffersGeometry() start\n");
            // 2.设置缓冲区的属性（宽、高、像素格式）,像素格式要和SurfaceView的像素格式一直
            ANativeWindow_setBuffersGeometry(pANativeWindow,
                                             videoWrapper->srcWidth,
                                             videoWrapper->srcHeight,
                                             WINDOW_FORMAT_RGBA_8888);
            LOGW("handleData() ANativeWindow_setBuffersGeometry() end\n");
            LOGW("handleData() for (;;) video start\n");
            isVideoHandling = true;
        }

        AVStream *stream = avFormatContext->streams[wrapper->streamIndex];
        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = NULL;
        if (wrapper->type == TYPE_AUDIO) {
            decodedAVFrame = audioWrapper->decodedAVFrame;
        } else {
            decodedAVFrame = videoWrapper->decodedAVFrame;
        }

        int ret = 0;
        bool maybeHasException = false;
        // test
        //long count = 0;
        for (;;) {
            //count++;
            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            // region 暂停装置

            if (wrapper->isPausedForUser
                || wrapper->isPausedForCache
                || wrapper->isPausedForSeek) {
                bool isPausedForUser = wrapper->isPausedForUser;
                bool isPausedForSeek = wrapper->isPausedForSeek;
                if (isPausedForSeek) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() Seek  audio start\n");
                        LOGD("handleData() audio list1 size: %d\n", wrapper->list1->size());
                    } else {
                        LOGW("handleData() wait() Seek  video start\n");
                        LOGD("handleData() video list1 size: %d\n", wrapper->list1->size());
                    }
                    if (wrapper->list1->size() != 0) {
                        std::list<AVPacket>::iterator iter;
                        for (iter = wrapper->list1->begin();
                             iter != wrapper->list1->end();
                             iter++) {
                            AVPacket avPacket = *iter;
                            av_packet_unref(&avPacket);
                        }
                        wrapper->list1->clear();
                    }
                    wrapper->isHandleList1Full = false;
                    wrapper->needToSeek = true;
                } else if (isPausedForUser) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() User  audio start\n");
                    } else {
                        LOGW("handleData() wait() User  video start\n");
                    }
                } else {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() wait() Cache audio start 被动暂停\n");
                    } else {
                        LOGE("handleData() wait() Cache video start 被动暂停\n");
                    }
                }
                notifyToHandleWait(wrapper);
                if (wrapper->isPausedForUser
                    || wrapper->isPausedForCache
                    || wrapper->isPausedForSeek) {
                    continue;
                }
                if (isPausedForSeek) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() Seek  audio end\n");
                    } else {
                        LOGW("handleData() wait() Seek  video end\n");
                    }
                } else if (isPausedForUser) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() User  audio end\n");
                    } else {
                        LOGW("handleData() wait() User  video end\n");
                    }
                } else {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() wait() Cache audio end   被动暂停\n");
                    } else {
                        LOGE("handleData() wait() Cache video end   被动暂停\n");
                    }
                }
                startVideoLockedTime = av_gettime_relative();
            }// 暂停装置 end

            // endregion

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (!isLocal) {
                if (wrapper->list1->size() >= wrapper->list1LimitCounts) {
                    wrapper->startHandleTime = av_gettime_relative();
                    maybeHasException = true;
                }
            }

            // region 从队列中取出一个AVPacket

            wrapper->allowDecode = false;
            if (wrapper->list1->size() > 0) {
                srcAVPacket = &wrapper->list1->front();
                // 内容copy
                av_packet_ref(copyAVPacket, srcAVPacket);
                av_packet_unref(srcAVPacket);
                wrapper->list1->pop_front();
                wrapper->handleFramesCount++;
                wrapper->allowDecode = true;
            }

            if (!isLocal) {
                size_t list1Size = wrapper->list1->size();
                if (wrapper->type == TYPE_AUDIO) {
                    if (list1Size % 10 == 0) {
                        onLoadProgressUpdated(0x1004, list1Size);
                    }
                    /*if (list1Size % 1000 == 0) {
                        LOGD("handleData()   audio list1Size: %d\n", list1Size);
                    }*/
                } else {
                    if (list1Size % 10 == 0) {
                        onLoadProgressUpdated(0x1002, list1Size);
                    }
                    /*if (list1Size % 1000 == 0) {
                        LOGW("handleData()   video list1Size: %d\n", list1Size);
                    }*/
                }
            }

            // endregion

            // region 播放异常处理

            if (!isLocal) {
                if (maybeHasException && wrapper->list1->size() == 0) {
                    wrapper->endHandleTime = av_gettime_relative();
                    /*if (count >= 500) {
                        wrapper->endHandleTime = wrapper->startHandleTime;
                    }*/
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData()  audio handleTime: %ld\n",
                             (long) (wrapper->endHandleTime - wrapper->startHandleTime));
                    } else {
                        LOGE("handleData()  video handleTime: %ld\n",
                             (long) (wrapper->endHandleTime - wrapper->startHandleTime));
                    }
                    /***
                     830377 243952 227061 251820 243842
                     */
                    // 如果不是本地视频,从一千个左右的数据到0个数据的时间不超过30秒,那么就有问题了.
                    if ((wrapper->endHandleTime - wrapper->startHandleTime) < 251820) {
                        LOGE("handleData() maybeHasException\n");
                        // 257
                        onError(0x101, "播放时发生异常");
                        // 258(见鬼了,"0x101"这个值正常,"0x102"这个值不正常)
                        //onError(0x102, "播放时发生异常");
                        stop();
                    } else {
                        maybeHasException = false;
                    }
                }
            }

            // endregion

            // region 复制数据

            if (wrapper->isReading) {
                if (wrapper->list1->size() == 0) {
                    wrapper->isHandleList1Full = false;
                    if (wrapper->list2->size() > 0) {
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        wrapper->isHandleList1Full = true;
                        pthread_mutex_unlock(&wrapper->readLockMutex);

                        LOGI("===================================================\n");
                        if (wrapper->type == TYPE_AUDIO) {
                            onLoadProgressUpdated(0x1003, 0);
                            onLoadProgressUpdated(0x1004, wrapper->list1->size());
                            LOGD("handleData() audio 接下去要处理的数据有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGW("handleData() video 接下去要处理的数据有 list1: %d\n",
                                 videoWrapper->father->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 videoWrapper->father->list2->size());
                        } else {
                            onLoadProgressUpdated(0x1000, 0);
                            onLoadProgressUpdated(0x1002, wrapper->list1->size());
                            LOGW("handleData() video 接下去要处理的数据有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGD("handleData() audio 接下去要处理的数据有 list1: %d\n",
                                 audioWrapper->father->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 audioWrapper->father->list2->size());
                        }
                        LOGI("===================================================\n");
                    }
                    notifyToRead(videoWrapper->father);
                }
            } else {
                if (wrapper->list1->size() > 0) {
                    // 还有数据,先用完再说
                } else {
                    if (wrapper->list2->size() > 0) {
                        // 把剩余的数据全部复制过来
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        pthread_mutex_unlock(&wrapper->readLockMutex);

                        LOGI("===================================================\n");
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("handleData() audio 最后要处理的数据还有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGW("handleData() video                   list1: %d\n",
                                 videoWrapper->father->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 videoWrapper->father->list2->size());
                        } else {
                            LOGW("handleData() video 最后要处理的数据还有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGD("handleData() audio                   list1: %d\n",
                                 audioWrapper->father->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 audioWrapper->father->list2->size());
                        }
                        LOGI("===================================================\n");
                    } else {
                        wrapper->isHandling = false;
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("handleData() audio wrapper->isHandling is false\n");
                        } else {
                            LOGW("handleData() video wrapper->isHandling is false\n");
                        }
                    }
                }
            }

            // endregion

            // region 缓冲处理

            if (wrapper->isReading
                && wrapper->isHandling
                && !wrapper->isHandleList1Full
                && wrapper->list1->size() == 0
                && wrapper->list2->size() == 0) {

                LOGE("---------------------------------------------------\n");
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("handleData() audio                   list1: %d\n",
                         wrapper->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         wrapper->list2->size());
                    LOGW("handleData() video                   list1: %d\n",
                         videoWrapper->father->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         videoWrapper->father->list2->size());
                } else {
                    LOGW("handleData() video                   list1: %d\n",
                         wrapper->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         wrapper->list2->size());
                    LOGD("handleData() audio                   list1: %d\n",
                         audioWrapper->father->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         audioWrapper->father->list2->size());
                }
                LOGE("---------------------------------------------------\n");

                // 开始暂停
                onPaused();

                // 通知"读"
                notifyToRead(videoWrapper->father);
                if (wrapper->type == TYPE_AUDIO) {
                    // 音频Cache引起的暂停
                    audioWrapper->father->isPausedForCache = true;

                    // 让视频也同时暂停
                    videoWrapper->father->isPausedForCache = true;
                    // 音频自身暂停
                    LOGE("handleData() wait() Cache audio start 主动暂停\n");
                    notifyToHandleWait(audioWrapper->father);
                    if (wrapper->isPausedForSeek) {
                        audioWrapper->father->isPausedForCache = false;
                        videoWrapper->father->isPausedForCache = false;
                        continue;
                    }
                    LOGE("handleData() wait() Cache audio end   主动暂停\n");

                    audioWrapper->father->isPausedForCache = false;
                    // 通知视频结束暂停
                    videoWrapper->father->isPausedForCache = false;
                    notifyToHandle(videoWrapper->father);
                } else {
                    // 视频Cache引起的暂停
                    videoWrapper->father->isPausedForCache = true;

                    // 让音频也同时暂停
                    audioWrapper->father->isPausedForCache = true;
                    // 视频自身暂停
                    LOGE("handleData() wait() Cache video start 主动暂停\n");
                    notifyToHandleWait(videoWrapper->father);
                    if (wrapper->isPausedForSeek) {
                        audioWrapper->father->isPausedForCache = false;
                        videoWrapper->father->isPausedForCache = false;
                        continue;
                    }
                    LOGE("handleData() wait() Cache video end   主动暂停\n");

                    videoWrapper->father->isPausedForCache = false;
                    // 通知音频结束暂停
                    audioWrapper->father->isPausedForCache = false;
                    notifyToHandle(audioWrapper->father);
                }

                // 开始播放
                onPlayed();

                LOGI("***************************************************\n");
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("handleData() audio                   list1: %d\n",
                         wrapper->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         wrapper->list2->size());
                    LOGW("handleData() video                   list1: %d\n",
                         videoWrapper->father->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         videoWrapper->father->list2->size());
                } else {
                    LOGW("handleData() video                   list1: %d\n",
                         wrapper->list1->size());
                    LOGW("handleData() video                   list2: %d\n",
                         wrapper->list2->size());
                    LOGD("handleData() audio                   list1: %d\n",
                         audioWrapper->father->list1->size());
                    LOGD("handleData() audio                   list2: %d\n",
                         audioWrapper->father->list2->size());
                }
                LOGI("***************************************************\n");
            }

            // endregion

            // region 解码过程

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (!wrapper->allowDecode) {
                continue;
            }

            ret = avcodec_send_packet(wrapper->avCodecContext, copyAVPacket);
            av_packet_unref(copyAVPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() audio avcodec_send_packet   ret: %d\n", ret);// -11
                    } else {
                        LOGE("handleData() video avcodec_send_packet   ret: %d\n", ret);
                    }
                    break;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("audio 发送数据包到解码器时出错 %d", ret);// -22
                    } else {
                        LOGE("video 发送数据包到解码器时出错 %d", ret);
                    }
                    //wrapper->isHandling = false;
                    break;
                case 0:
                    break;
                default:
                    if (wrapper->type == TYPE_AUDIO) {
                        // audio 发送数据包时出现异常 -50531338
                        LOGE("audio 发送数据包时出现异常 %d", ret);// -1094995529
                    } else {
                        LOGE("video 发送数据包时出现异常 %d", ret);
                    }
                    break;
            }// switch (ret) end

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (ret != 0) {
                continue;
            }

            ret = avcodec_receive_frame(wrapper->avCodecContext, decodedAVFrame);
            switch (ret) {
                // 输出是不可用的,必须发送新的输入
                case AVERROR(EAGAIN):
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() audio avcodec_receive_frame ret: %d\n", ret);
                    } else {
                        LOGE("handleData() video avcodec_receive_frame ret: %d\n", ret);
                    }
                    break;
                case AVERROR(EINVAL):
                    // codec打不开,或者是一个encoder
                case AVERROR_EOF:
                    // 已经完全刷新,不会再有输出帧了
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("audio 从解码器接收解码帧时出错 %d", ret);
                    } else {
                        LOGE("video 从解码器接收解码帧时出错 %d", ret);
                    }
                    //wrapper->isHandling = false;
                    break;
                case 0: {
                    // 解码成功,返回一个输出帧
                    break;
                }
                default:
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("audio 接收解码帧时出现异常 %d", ret);
                    } else {
                        LOGE("video 接收解码帧时出现异常 %d", ret);
                    }
                    break;
            }// switch (ret) end

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (ret != 0) {
                continue;
            }

            // endregion

            ///////////////////////////////////////////////////////////////////

            // 播放声音和渲染画面
            if (wrapper->type == TYPE_AUDIO) {
                handleAudioDataImpl(stream, decodedAVFrame);
            } else {
                handleVideoDataImpl(stream, decodedAVFrame);
            }

            ///////////////////////////////////////////////////////////////////

            // 设置结束标志
            if (!wrapper->isReading
                && wrapper->list1->size() == 0
                && wrapper->list2->size() == 0) {
                wrapper->isHandling = false;
                if (wrapper->type == TYPE_AUDIO) {
                    LOGD("handleData() audio wrapper->isHandling is false 2\n");
                } else {
                    LOGW("handleData() video wrapper->isHandling is false 2\n");
                }
            }
        }//for(;;) end

        if (srcAVPacket != NULL) {
            av_packet_unref(srcAVPacket);
            // app crash 上面的copyAVPacket调用却没事,why
            // av_packet_free(&srcAVPacket);
            srcAVPacket = NULL;
        }
        if (copyAVPacket != NULL) {
            av_packet_free(&copyAVPacket);
            copyAVPacket = NULL;
        }

        handleDataClose(wrapper);

        return NULL;
    }

    void closeAudio() {
        // audio
        if (audioWrapper == NULL
            || audioWrapper->father == NULL) {
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
        if (audioWrapper->father->avCodecContext != NULL) {
            avcodec_close(audioWrapper->father->avCodecContext);
            av_free(audioWrapper->father->avCodecContext);
            audioWrapper->father->avCodecContext = NULL;
        }
        pthread_mutex_destroy(&audioWrapper->father->readLockMutex);
        pthread_cond_destroy(&audioWrapper->father->readLockCondition);
        pthread_mutex_destroy(&audioWrapper->father->handleLockMutex);
        pthread_cond_destroy(&audioWrapper->father->handleLockCondition);

        if (audioWrapper->father->list1->size() != 0) {
            LOGD("closeAudio() list1 is not empty, %d\n", audioWrapper->father->list1->size());
            std::list<AVPacket>::iterator iter;
            for (iter = audioWrapper->father->list1->begin();
                 iter != audioWrapper->father->list1->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
        }
        if (audioWrapper->father->list2->size() != 0) {
            LOGD("closeAudio() list2 is not empty, %d\n", audioWrapper->father->list2->size());
            std::list<AVPacket>::iterator iter;
            for (iter = audioWrapper->father->list2->begin();
                 iter != audioWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
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
        if (pANativeWindow != NULL) {
            // 7.释放资源
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = NULL;
        }
        pthread_mutex_destroy(&readLockMutex);
        pthread_cond_destroy(&readLockCondition);
        // video
        if (videoWrapper == NULL
            || videoWrapper->father == NULL) {
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
        if (videoWrapper->father->avCodecContext != NULL) {
            avcodec_close(videoWrapper->father->avCodecContext);
            av_free(videoWrapper->father->avCodecContext);
            videoWrapper->father->avCodecContext = NULL;
        }

        pthread_mutex_destroy(&videoWrapper->father->readLockMutex);
        pthread_cond_destroy(&videoWrapper->father->readLockCondition);
        pthread_mutex_destroy(&videoWrapper->father->handleLockMutex);
        pthread_cond_destroy(&videoWrapper->father->handleLockCondition);

        if (videoWrapper->father->list1->size() != 0) {
            LOGW("closeVideo() list1 is not empty, %d\n", videoWrapper->father->list1->size());
            std::list<AVPacket>::iterator iter;
            for (iter = videoWrapper->father->list1->begin();
                 iter != videoWrapper->father->list1->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
        }
        if (videoWrapper->father->list2->size() != 0) {
            LOGW("closeVideo() list2 is not empty, %d\n", videoWrapper->father->list2->size());
            std::list<AVPacket>::iterator iter;
            for (iter = videoWrapper->father->list2->begin();
                 iter != videoWrapper->father->list2->end();
                 iter++) {
                AVPacket avPacket = *iter;
                av_packet_unref(&avPacket);
            }
        }
        delete (videoWrapper->father->list1);
        delete (videoWrapper->father->list2);
        videoWrapper->father->list1 = NULL;
        videoWrapper->father->list2 = NULL;

        av_free(videoWrapper->father);
        videoWrapper->father = NULL;
        av_free(videoWrapper);
        videoWrapper = NULL;

        /*if (videoAVFormatContext != NULL) {
            avformat_close_input(&videoAVFormatContext);
            videoAVFormatContext = NULL;
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

    void closeDownload() {
        LOGI("%s\n", "closeDownload() start");
        if (avFormatContextOutput != NULL) {
            // 写尾部信息
            av_write_trailer(avFormatContextOutput);
            avio_close(avFormatContextOutput->pb);

            //avformat_close_input(&avFormatContextOutput);
            avformat_free_context(avFormatContextOutput);
            avFormatContextOutput = NULL;
        }
        if (avFormatContextVideoOutput != NULL) {
            av_write_trailer(avFormatContextVideoOutput);
            avio_close(avFormatContextVideoOutput->pb);

            //avformat_close_input(&avFormatContextVideoOutput);
            avformat_free_context(avFormatContextVideoOutput);
            avFormatContextVideoOutput = NULL;
        }
        if (avFormatContextAudioOutput != NULL) {
            av_write_trailer(avFormatContextAudioOutput);
            avio_close(avFormatContextAudioOutput->pb);

            //avformat_close_input(&avFormatContextAudioOutput);
            avformat_free_context(avFormatContextAudioOutput);
            avFormatContextAudioOutput = NULL;
        }
        if (audio_out_stream != NULL) {
        }
        if (video_out_stream != NULL) {
        }
        if (dts_start_from != NULL) {
            free(dts_start_from);
            dts_start_from = NULL;
        }
        if (pts_start_from != NULL) {
            free(pts_start_from);
            pts_start_from = NULL;
        }
        if (videoFile) {
            fclose(videoFile);
            videoFile = NULL;
        }
        if (audioFile) {
            fclose(audioFile);
            audioFile = NULL;
        }
        LOGI("%s\n", "closeDownload() end");
    }

    void closeOther() {
        if (avFormatContext != NULL) {
            LOGI("%s\n", "closeOther() start");
            //ffmpeg 4.0版本不能调用
            //avformat_close_input(&avFormatContext);
            avformat_free_context(avFormatContext);
            avFormatContext = NULL;
            LOGI("%s\n", "closeOther() end");
        }
        closeDownload();
    }

    int initPlayer() {
        LOGW("%s\n", "initPlayer() start");

        onReady();

        initAV();
        initAudio();
        initVideo();
        if (openAndFindAVFormatContext() < 0) {
            LOGE("openAndFindAVFormatContext() failed\n");
            closeAudio();
            closeVideo();
            closeOther();
            if (isInterrupted) {
                onFinished();
            } else {
                onError(0x100, "openAndFindAVFormatContext() failed");
            }
            return -1;
        }
        if (findStreamIndex() < 0) {
            LOGE("findStreamIndex() failed\n");
            closeAudio();
            closeVideo();
            closeOther();
            onError(0x100, "findStreamIndex() failed");
            return -1;
        }
        if (findAndOpenAVCodecForAudio() < 0) {
            LOGE("findAndOpenAVCodecForAudio() failed\n");
            closeAudio();
            closeVideo();
            closeOther();
            onError(0x100, "findAndOpenAVCodecForAudio() failed");
            return -1;
        }
        if (findAndOpenAVCodecForVideo() < 0) {
            LOGE("findAndOpenAVCodecForVideo() failed\n");
            closeAudio();
            closeVideo();
            closeOther();
            onError(0x100, "findAndOpenAVCodecForVideo() failed");
            return -1;
        }
        if (createSwrContent() < 0) {
            LOGE("createSwrContent() failed\n");
            closeAudio();
            closeVideo();
            closeOther();
            onError(0x100, "createSwrContent() failed");
            return -1;
        }
        if (createSwsContext() < 0) {
            LOGE("createSwsContext() failed\n");
            closeAudio();
            closeVideo();
            closeOther();
            onError(0x100, "createSwsContext() failed");
            return -1;
        }

        if (!audioWrapper->father->isReading
            || !audioWrapper->father->isHandling
            || !videoWrapper->father->isReading
            || !videoWrapper->father->isHandling) {
            closeAudio();
            closeVideo();
            closeOther();
            onFinished();
            LOGW("%s\n", "initPlayer() finish");
            return -1;
        }

        mediaDuration = (long) (avFormatContext->duration / AV_TIME_BASE);
        LOGI("initPlayer() mediaDuration: %ld\n", mediaDuration);
        if (avFormatContext->duration != AV_NOPTS_VALUE) {
            // 得到的是秒数
            mediaDuration = (long) ((avFormatContext->duration + 5000) / AV_TIME_BASE);
            long hours, mins, seconds;
            seconds = mediaDuration;
            mins = seconds / 60;
            seconds %= 60;
            hours = mins / 60;
            mins %= 60;
            // 00:54:16
            // 单位: 秒
            LOGI("initPlayer() media seconds: %ld\n", mediaDuration);
            LOGI("initPlayer() media          %02d:%02d:%02d\n", hours, mins, seconds);
        }
        switch (use_mode) {
            case USE_MODE_MEDIA: {
                audioWrapper->father->duration =
                videoWrapper->father->duration = mediaDuration;
                onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);
                break;
            }
            case USE_MODE_ONLY_VIDEO: {
                videoWrapper->father->duration = mediaDuration;
                closeAudio();
                onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);
                break;
            }
            case USE_MODE_ONLY_AUDIO: {
                audioWrapper->father->duration = mediaDuration;
                closeVideo();
                onChangeWindow(0, 0);
                break;
            }
            default:
                break;
        }

        LOGW("%s\n", "initPlayer() end");

        return 0;
    }

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject) {
//        const char *src = "/storage/emulated/0/Movies/权力的游戏第三季05.mp4";
//        const char *src = "http://192.168.0.112:8080/tomcat_video/game_of_thrones/game_of_thrones_season_1/01.mp4";
//        av_strlcpy(inVideoFilePath, src, sizeof(inVideoFilePath));

        memset(inFilePath, '\0', sizeof(inFilePath));
        av_strlcpy(inFilePath, filePath, sizeof(inFilePath));
        LOGI("setJniParameters() filePath  : %s", inFilePath);

        isLocal = false;
        char *result = strstr(inFilePath, "http://");
        if (result == NULL) {
            result = strstr(inFilePath, "https://");
            if (result == NULL) {
                result = strstr(inFilePath, "rtmp://");
                if (result == NULL) {
                    result = strstr(inFilePath, "rtsp://");
                    if (result == NULL) {
                        isLocal = true;
                    }
                }
            }
        }
        LOGI("setJniParameters() isLocal   : %d", isLocal);

        if (pANativeWindow != NULL) {
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = NULL;
        }
        // 1.获取一个关联Surface的NativeWindow窗体
        pANativeWindow = ANativeWindow_fromSurface(env, surfaceJavaObject);
    }

    int play() {
        if (onlyDownloadNotPlayback) {
            return 0;
        }
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = false;
            if (!audioWrapper->father->isPausedForCache) {
                notifyToHandle(audioWrapper->father);
            }
        }
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = false;
            if (!videoWrapper->father->isPausedForCache) {
                notifyToHandle(videoWrapper->father);
            }
        }
        return 0;
    }

    int pause() {
        LOGI("pause() start\n");
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = true;
        }
        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = true;
        }
        LOGI("pause() end\n");
        return 0;
    }

    int stop() {
        LOGI("stop() start\n");
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
            LOGI("stop() audio\n");
            // audio
            audioWrapper->father->isStarted = false;
            audioWrapper->father->isReading = false;
            audioWrapper->father->isHandling = false;
            audioWrapper->father->isPausedForUser = false;
            audioWrapper->father->isPausedForCache = false;
            audioWrapper->father->isPausedForSeek = false;
            audioWrapper->father->isHandleList1Full = false;
            notifyToRead(audioWrapper->father);
            notifyToHandle(audioWrapper->father);
        }

        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            LOGI("stop() video\n");
            // video
            videoWrapper->father->isStarted = false;
            videoWrapper->father->isReading = false;
            videoWrapper->father->isHandling = false;
            videoWrapper->father->isPausedForUser = false;
            videoWrapper->father->isPausedForCache = false;
            videoWrapper->father->isPausedForSeek = false;
            videoWrapper->father->isHandleList1Full = false;
            notifyToRead();
            notifyToRead(videoWrapper->father);
            notifyToHandle(videoWrapper->father);
        }
        LOGI("stop() end\n");

        return 0;
    }

    int release() {
        stop();
        return 0;
    }

    // 有没有在运行,即使暂停状态也是运行状态
    bool isRunning() {
        bool audioRunning = false;
        bool videoRunning = false;
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
            audioRunning = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling;
        }
        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            videoRunning = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling;
        }
        return audioRunning && videoRunning;
    }

    // 有没有在播放,暂停状态不算播放状态
    bool isPlaying() {
        bool audioPlaying = false;
        bool videoPlaying = false;
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
            audioPlaying = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling
                           && !audioWrapper->father->isPausedForUser
                           && !audioWrapper->father->isPausedForCache;
        }
        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            videoPlaying = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling
                           && !videoWrapper->father->isPausedForUser
                           && !videoWrapper->father->isPausedForCache;
        }
        return audioPlaying && videoPlaying;
    }

    bool isPausedForUser() {
        bool audioPlaying = false;
        bool videoPlaying = false;
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
            audioPlaying = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling
                           && audioWrapper->father->isPausedForUser
                           && videoWrapper->father->isPausedForCache;
        }
        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            videoPlaying = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling
                           && videoWrapper->father->isPausedForUser
                           && videoWrapper->father->isPausedForCache;
        }
        return audioPlaying && videoPlaying;
    }

    /***
     1.暂停状态下seek
     2.播放状态下seek

     bug
     ---------->onClick() mProgress: 0 00:00
     ==================================================================
     seekTo() timestamp: 1402537096
     ---------->onClick() mProgress: 0 00:00
     ==================================================================
     seekTo() timestamp: 1402537096
     */
    int seekTo(int64_t timestamp) {// 单位秒.比如seek到100秒,就传100
        LOGI("==================================================================\n");
        LOGI("seekTo() timeStamp: %ld\n", (long) timestamp);

        if ((long) timestamp > 0
            && (audioWrapper == NULL
                || videoWrapper == NULL)) {
            timeStamp = timestamp;
            return 0;
        }

        if ((long) timestamp < 0
            || audioWrapper == NULL
            || audioWrapper->father == NULL
            || audioWrapper->father->isPausedForSeek
            || videoWrapper == NULL
            || videoWrapper->father == NULL
            || videoWrapper->father->isPausedForSeek
            || mediaDuration < 0
            || ((long) timestamp) > mediaDuration) {
            return -1;
        }

        LOGD("seekTo() signal() to Read and Handle\n");
        timeStamp = timestamp;
        if (!isLocal && (long) timestamp == 0) {
            timeStamp = 5;
        }
        audioWrapper->father->isPausedForSeek = true;
        videoWrapper->father->isPausedForSeek = true;
        audioWrapper->father->needToSeek = false;
        videoWrapper->father->needToSeek = false;
        notifyToHandle(audioWrapper->father);
        notifyToHandle(videoWrapper->father);
        notifyToRead(audioWrapper->father);
        notifyToRead(videoWrapper->father);
        notifyToRead();

        return 0;
    }

    // 返回值单位是秒
    long getDuration() {
        return mediaDuration;
    }

    void stepAdd(int64_t addStep) {
        /*++videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepAdd()      videoSleepTime: %d\n", videoSleepTime);*/

        if (mediaDuration > 0) {
            LOGI("stepAdd() addStep: %ld\n", (long) addStep);
            seekTo(curProgress + addStep);
        }
    }

    void stepSubtract(int64_t subtractStep) {
        /*--videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepSubtract() videoSleepTime: %d\n", videoSleepTime);*/

        if (mediaDuration > 0) {
            LOGI("stepAdd() subtractStep: %ld\n", (long) subtractStep);
            seekTo(curProgress - subtractStep);
        }
    }

    int download(int flag, const char *filePath, const char *fileName) {
        switch (flag) {
            case 0: // 下载音视频
            case 4: // 只下载,不播放
            case 5: // 只提取音视频,不播放
            {
                /*if (!audioWrapper->father->isReading
                    || !videoWrapper->father->isReading) {
                    return 0;
                }*/

                /*std::string mediaPath;
                mediaPath.append(filePath);
                mediaPath.append(fileName);
                mediaPath.append(".mp4");// .h264 error
                memset(outFilePath, '\0', sizeof(outFilePath));
                av_strlcpy(outFilePath, mediaPath.c_str(), sizeof(outFilePath));
                LOGI("download() outFilePath: %s\n", outFilePath);*/

                std::string videoPath;
                std::string audioPath;
                videoPath.append(filePath);
                videoPath.append(fileName);
                videoPath.append(".h264");// ppm
                audioPath.append(filePath);
                audioPath.append(fileName);
                audioPath.append(".aac");

                memset(videoOutFilePath, '\0', sizeof(videoOutFilePath));
                av_strlcpy(videoOutFilePath, videoPath.c_str(), sizeof(videoOutFilePath));
                memset(audioOutFilePath, '\0', sizeof(audioOutFilePath));
                av_strlcpy(audioOutFilePath, audioPath.c_str(), sizeof(audioOutFilePath));

                LOGI("download() videoOutFilePath: %s\n", videoOutFilePath);
                LOGI("download() audioOutFilePath: %s\n", audioOutFilePath);

                onlyDownloadNotPlayback = false;
                if (initDownload2() < 0) {
                    needToDownload = false;
                    isInitSuccess = false;
                } else {
                    if (flag == 4 || flag == 5) {
                        onlyDownloadNotPlayback = true;
                        if (flag == 5) {
                            seekTo(0);
                        }
                        pause();
                    }
                    needToDownload = true;
                    isInitSuccess = true;
                }
                LOGI("download() isInitSuccess: %d\n", isInitSuccess);

                break;
            }
            case 1: // 停止下载
            {
                pthread_mutex_lock(&readLockMutex);
                if (needToDownload && isInitSuccess) {
                    LOGI("download() 停止下载\n");
                    needToDownload = false;
                    isInitSuccess = false;
                    //av_usleep(1000 * 1000 * 3);// 3秒
                    closeDownload();
                }
                pthread_mutex_unlock(&readLockMutex);
                break;
            }
            case 2: // 只下载音频
            {
                break;
            }
            case 3: // 只下载视频
            {
                break;
            }
            default:
                break;
        }

        return 0;
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

    int GetH264Stream() {
        int ret;
        //AVFormatContext *avFormatContext = NULL;
        //AVFormatContext *avFormatContextVideoOutput=NULL;

        uint8_t sps[100];
        uint8_t pps[100];
        int spsLength = 0;
        int ppsLength = 0;
        uint8_t startcode[4] = {00, 00, 00, 01};
        FILE *fp;

        fp = fopen("123.h264", "wb+");

        //char *InputFileName = "11111.mp4";

        if ((ret = avformat_open_input(&avFormatContext, inFilePath, NULL, NULL)) < 0) {
            return ret;
        }

        if ((ret = avformat_find_stream_info(avFormatContext, NULL)) < 0) {
            avformat_close_input(&avFormatContext);
            return ret;
        }

        spsLength =
                avFormatContext->streams[0]->codec->extradata[6] * 0xFF +
                avFormatContext->streams[0]->codec->extradata[7];

        ppsLength = avFormatContext->streams[0]->codec->extradata[8 + spsLength + 1] * 0xFF +
                    avFormatContext->streams[0]->codec->extradata[8 + spsLength + 2];

        for (int i = 0; i < spsLength; i++) {
            sps[i] = avFormatContext->streams[0]->codec->extradata[i + 8];
        }

        for (int i = 0; i < ppsLength; i++) {
            pps[i] = avFormatContext->streams[0]->codec->extradata[i + 8 + 2 + 1 + spsLength];
        }


        for (int i = 0; i < avFormatContext->nb_streams; i++) {
            if (avFormatContext->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
                int videoindex = i;
            } else if (avFormatContext->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
                int audioindex = i;
            }
        }

        AVOutputFormat *ofmt = NULL;
        AVPacket pkt;

        avformat_alloc_output_context2(&avFormatContextVideoOutput, NULL, NULL, videoOutFilePath);

        if (!avFormatContextVideoOutput) {
            printf("Could not create output context\n");
            ret = AVERROR_UNKNOWN;
        }
        ofmt = avFormatContextVideoOutput->oformat;
        int i;

        for (i = 0; i < avFormatContext->nb_streams; i++) {
            AVStream *in_stream = avFormatContext->streams[i];
            AVStream *out_stream = avformat_new_stream(avFormatContextVideoOutput,
                                                       in_stream->codec->codec);

            if (!out_stream) {
                printf("Failed allocating output stream\n");
                ret = AVERROR_UNKNOWN;
            }
            ret = avcodec_copy_context(out_stream->codec, in_stream->codec);
            if (ret < 0) {
                printf("Failed to copy context from input to output stream codec context\n");
            }
            out_stream->codec->codec_tag = 0;
            if (avFormatContextVideoOutput->oformat->flags & AVFMT_GLOBALHEADER) {
                //out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
            }
        }

        if (!(ofmt->flags & AVFMT_NOFILE)) {
            ret = avio_open(&avFormatContextVideoOutput->pb, videoOutFilePath, AVIO_FLAG_WRITE);
            if (ret < 0) {
                printf("Could not open output file '%s'", videoOutFilePath);

            }
        }
        ret = avformat_write_header(avFormatContextVideoOutput, NULL);

        int frame_index = 0;
        int flag = 1;

        av_init_packet(&pkt);
        pkt.data = NULL;
        pkt.size = 0;

        while (1) {
            AVStream *in_stream, *out_stream;

            ret = av_read_frame(avFormatContext, &pkt);

            if (ret < 0)
                break;
            in_stream = avFormatContext->streams[pkt.stream_index];
            out_stream = avFormatContextVideoOutput->streams[pkt.stream_index];

            AVPacket tmppkt;
            if (in_stream->codec->codec_type == AVMEDIA_TYPE_VIDEO) {

                if (flag) {
                    fwrite(startcode, 4, 1, fp);
                    fwrite(sps, spsLength, 1, fp);
                    fwrite(startcode, 4, 1, fp);
                    fwrite(pps, ppsLength, 1, fp);

                    pkt.data[0] = 0x00;
                    pkt.data[1] = 0x00;
                    pkt.data[2] = 0x00;
                    pkt.data[3] = 0x01;
                    fwrite(pkt.data, pkt.size, 1, fp);

                    flag = 0;
                } else {
                    pkt.data[0] = 0x00;
                    pkt.data[1] = 0x00;
                    pkt.data[2] = 0x00;
                    pkt.data[3] = 0x01;
                    fwrite(pkt.data, pkt.size, 1, fp);
                }

                pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base,
                                           (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
                pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base,
                                           (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
                pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base,
                                            out_stream->time_base);
                pkt.pos = -1;

                pkt.stream_index = 0;
                ret = av_interleaved_write_frame(avFormatContextVideoOutput, &pkt);
            }

            av_free_packet(&pkt);
        }

        fclose(fp);
        fp = NULL;

        av_write_trailer(avFormatContextVideoOutput);
        return 0;
    }

}
