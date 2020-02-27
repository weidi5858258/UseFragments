//
// Created by root on 19-8-8.
//








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

#include "SimpleVideoPlayer3.h"

#define LOG "player_alexander"

namespace alexander3 {

    // 不要各自拥有一个指针,音视频共用一个就行了
    static AVFormatContext *avFormatContext = NULL;

    static struct AudioWrapper *audioWrapper = NULL;
    static struct VideoWrapper *videoWrapper = NULL;
    static pthread_mutex_t readLockMutex = PTHREAD_MUTEX_INITIALIZER;
    static pthread_cond_t readLockCondition = PTHREAD_COND_INITIALIZER;
    static bool isLocal = false;
    // 是否需要输出声音
    static bool needOutputAudio = true;
    // seek时间
    static int64_t timeStamp = -1;
    static long preProgress = 0;
    // 视频播放时每帧之间的暂停时间,单位为ms
    static int videoSleepTime = 11;
    static bool needLocalLog = true;
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


    // 绘制时的缓冲区
    ANativeWindow_Buffer mANativeWindow_Buffer;
    bool onlyOneVideo = true;
    int64_t curAVFramePtsVideo = 0;
    int64_t preAVFramePtsVideo = 0;

    char *getStrAVPixelFormat(AVPixelFormat format);

    /////////////////////////////////////////////////////

    double
    //上一帧的播放时间
            pre_play_time = 0,
    //当前帧的播放时间
            cur_play_time = 0,
    // 上一次播放视频的两帧视频间隔时间
            last_delay = 0,
    //两帧视频间隔时间
            delay = 0,
    //音频轨道 实际播放时间
            audio_clock = 0,
            video_clock = 0,
    //音频帧与视频帧相差时间
            diff = 0,
            sync_threshold = 0,
    //从第一帧开始的绝对时间
            start_time = 0,
            pts = 0,
    //真正需要延迟时间
            actual_delay = 0;

    /////////////////////////////////////////////////////

    AudioWrapper *getAudioWrapper() {
        return audioWrapper;
    }

    VideoWrapper *getVideoWrapper() {
        return videoWrapper;
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

    double synchronize(AVFrame *frame, double play) {
        //clock是当前播放的时间位置
        if (play != 0) {
            video_clock = play;
        } else {
            //pst为0 则先把pts设为上一帧时间
            play = video_clock;
        }
        //可能有pts为0 则主动增加clock
        //frame->repeat_pict = 当解码时，这张图片需要要延迟多少
        //需要求出扩展延时：
        //extra_delay = repeat_pict / (2*fps) 显示这样图片需要延迟这么久来显示
        double repeat_pict = frame->repeat_pict;
        //使用AvCodecContext的而不是stream的
        double frame_delay = av_q2d(videoWrapper->father->avCodecContext->time_base);
        //如果time_base是1,25 把1s分成25份，则fps为25
        //fps = 1/(1/25)
        double fps = 1 / frame_delay;
        //pts 加上 这个延迟 是显示时间
        double extra_delay = repeat_pict / (2 * fps);
        double delay = extra_delay + frame_delay;
        //    LOGI("extra_delay:%f",extra_delay);
        video_clock += delay;
        return play;
    }

    // 已经不需要调用了
    void initAV() {
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avcodec_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        avformat_network_init();
        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
        LOGW("initAV() version: %s\n", av_version_info());

        readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        videoSleepTime = 11;
        timeStamp = -1;
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

        // Android支持的目标像素格式
        // AV_PIX_FMT_RGB32
        // AV_PIX_FMT_RGBA
        videoWrapper->dstAVPixelFormat = AV_PIX_FMT_RGBA;
    }

    int openAndFindAVFormatContext() {
        LOGI("openAndFindAVFormatContext() start\n");
        if (avFormatContext != NULL) {
            LOGI("openAndFindAVFormatContext() avFormatContext isn't NULL\n");
            avformat_free_context(avFormatContext);
            avFormatContext = NULL;
        }
        avFormatContext = avformat_alloc_context();
        if (avFormatContext == NULL) {
            LOGE("avFormatContext is NULL.\n");
            return -1;
        }
        if (avformat_open_input(&avFormatContext,
                                inFilePath,
                                NULL, NULL) != 0) {
            LOGE("Couldn't open input stream.\n");
            return -1;
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
        if (videoWrapper->father->streamIndex == -1
            || audioWrapper->father->streamIndex == -1) {
            LOGE("Didn't find audio or video stream.\n");
            return -1;
        }

        return 0;
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
            AVCodecID codecID = audioWrapper->father->avCodecParameters->codec_id;
            // audio是没有下面这些东西的
            switch (codecID) {
                case AV_CODEC_ID_HEVC: {
                    LOGD("findAndOpenAVCodecForAudio() hevc_mediacodec\n");
                    // 硬解码265
                    audioWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name("hevc_mediacodec");
                    break;
                }
                case AV_CODEC_ID_H264: {
                    LOGD("findAndOpenAVCodecForAudio() h264_mediacodec\n");
                    // 硬解码264
                    audioWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name("h264_mediacodec");
                    break;
                }
                case AV_CODEC_ID_MPEG4: {
                    LOGD("findAndOpenAVCodecForAudio() mpeg4_mediacodec\n");
                    // 硬解码mpeg4
                    audioWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name("mpeg4_mediacodec");
                    break;
                }
                default: {
                    LOGD("findAndOpenAVCodecForAudio() codecID\n");
                    // 软解
                    // audioWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
                    break;
                }
            }
            audioWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
            if (audioWrapper->father->decoderAVCodec != NULL) {
                // 获取解码器上下文
                // 再通过AVCodec得到AVCodecContext
                audioWrapper->father->avCodecContext = avcodec_alloc_context3(audioWrapper->father->decoderAVCodec);
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
            AVCodecID codecID = videoWrapper->father->avCodecParameters->codec_id;
            switch (codecID) {
                case AV_CODEC_ID_HEVC: {
                    LOGW("findAndOpenAVCodecForVideo() hevc_mediacodec\n");
                    // 硬解码265
                    videoWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name("hevc_mediacodec");
                    break;
                }
                case AV_CODEC_ID_H264: {
                    LOGW("findAndOpenAVCodecForVideo() h264_mediacodec\n");
                    // 硬解码264
                    videoWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name("h264_mediacodec");
                    break;
                }
                case AV_CODEC_ID_MPEG4: {
                    LOGW("findAndOpenAVCodecForVideo() mpeg4_mediacodec\n");
                    // 硬解码mpeg4
                    videoWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name("mpeg4_mediacodec");
                    break;
                }
                default: {
                    LOGW("findAndOpenAVCodecForVideo() codecID\n");
                    // 软解
                    // videoWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
                    break;
                }
            }
            // 有相应的so库时这句就不要执行了
            videoWrapper->father->decoderAVCodec = avcodec_find_decoder(codecID);
            if (videoWrapper->father->decoderAVCodec != NULL) {
                videoWrapper->father->avCodecContext = avcodec_alloc_context3(videoWrapper->father->decoderAVCodec);
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
        // dstSampleRate, dstAVSampleFormat和dstChannelLayout指定
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

        if (avFormatContext->duration != AV_NOPTS_VALUE) {
            int64_t duration = avFormatContext->duration + 5000;
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

        if (avFormatContext->duration != AV_NOPTS_VALUE) {
            int64_t duration = avFormatContext->duration + 5000;
            // 得到的是秒数
            videoWrapper->father->duration = duration / AV_TIME_BASE;
        }

        return 0;
    }

    int seekToImpl() {
        // seekTo
        if (timeStamp != -1) {
            // videoSleep(1000);
            LOGI("seekToImpl() av_seek_frame start\n");
            while (!audioWrapper->father->needToSeek
                   || !videoWrapper->father->needToSeek) {
                videoSleep(1);
            }
            av_seek_frame(
                    avFormatContext,
                    -1,
                    timeStamp * AV_TIME_BASE,
                    AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
            timeStamp = -1;
            preProgress = 0;
            audioWrapper->father->isPausedForSeek = false;
            videoWrapper->father->isPausedForSeek = false;
            LOGI("seekToImpl() av_seek_frame end\n");
        }
    }

    int readDataImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        wrapper->readFramesCount++;
        av_packet_ref(copyAVPacket, srcAVPacket);
        av_packet_unref(srcAVPacket);

        pthread_mutex_lock(&readLockMutex);
        // 存数据
        wrapper->list2->push_back(*copyAVPacket);
        size_t list2Size = wrapper->list2->size();
        pthread_mutex_unlock(&readLockMutex);

        // 如果发现是因为Cache问题而暂停了,那么发送一个通知
        if (wrapper->isPausedForCache
            && list2Size == CACHE_COUNT) {
            notifyToHandle(wrapper);
        }

        if (wrapper->type == TYPE_VIDEO
            && list2Size >= wrapper->list2LimitCounts) {
            LOGD("readDataImpl() audio list1: %d\n", audioWrapper->father->list1->size());
            LOGW("readDataImpl() video list1: %d\n", videoWrapper->father->list1->size());
            LOGD("readDataImpl() audio list2: %d\n", audioWrapper->father->list2->size());
            LOGW("readDataImpl() video list2: %d\n", videoWrapper->father->list2->size());
            LOGI("readDataImpl() notifyToReadWait start\n");
            notifyToReadWait(videoWrapper->father);
            LOGI("readDataImpl() notifyToReadWait end\n");
        }

        if (!wrapper->isHandleList1Full
            && list2Size == wrapper->list1LimitCounts) {
            // 下面两个都不行
            // std::move(wrapper->list2->begin(), wrapper->list2->end(), std::back_inserter(wrapper->list1));
            // wrapper->list1->swap((std::list<AVPacket> &) wrapper->list2);
            // 把list2中的内容全部复制给list1
            wrapper->list1->clear();
            wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
            wrapper->list2->clear();

            if (wrapper->type == TYPE_AUDIO) {
                LOGD("readDataImpl() audio 填满数据了\n");
            } else {
                LOGW("readDataImpl() video 填满数据了\n");
            }

            wrapper->isHandleList1Full = true;
            notifyToHandle(wrapper);
        }

        return 0;
    }

    void *readData(void *opaque) {
        int hours, mins, seconds;
        // 得到的是秒数
        seconds = getDuration();
        mins = seconds / 60;
        seconds %= 60;
        hours = mins / 60;
        mins %= 60;
        // 00:54:16
        // 单位: 秒
        LOGD("readData() audio seconds: %d\n", (int) audioWrapper->father->duration);
        LOGD("readData() audio          %02d:%02d:%02d\n", hours, mins, seconds);
        LOGD("%s\n", "readData() start");

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();

        int count_12 = 0;
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

            if (audioWrapper->father->isPausedForSeek
                || videoWrapper->father->isPausedForSeek) {
                pthread_mutex_lock(&readLockMutex);
                LOGI("readData() audio and video list2 clear\n");
                audioWrapper->father->list2->clear();
                videoWrapper->father->list2->clear();
                pthread_mutex_unlock(&readLockMutex);
            }

            // seekTo
            seekToImpl();

            // exit
            if (!audioWrapper->father->isReading
                && !videoWrapper->father->isReading) {
                // for (;;) end
                break;
            }

            // 0 if OK, < 0 on error or end of file
            int readFrame = av_read_frame(avFormatContext, srcAVPacket);
            //LOGI("readFrame           : %d\n", readFrame);
            if (readFrame < 0) {
                // 有些视频一直返回-12
                // LOGF("readData() readFrame            : %d\n", readFrame);
                if (readFrame != AVERROR_EOF) {
                    if (readFrame == -12) {
                        ++count_12;
                    }
                    if (count_12 <= 500) {
                        continue;
                    }
                }

                // readData() video AVERROR_EOF readFrame: -541478725
                LOGF("readData() AVERROR_EOF readFrame: %d\n", readFrame);
                LOGF("readData() 文件已经读完了\n");
                LOGD("readData() audio list2: %d\n", audioWrapper->father->list2->size());
                LOGW("readData() video list2: %d\n", videoWrapper->father->list2->size());

                // 读到文件末尾了
                audioWrapper->father->isReading = false;
                videoWrapper->father->isReading = false;
                audioWrapper->father->isHandleList1Full = true;
                videoWrapper->father->isHandleList1Full = true;
                // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                notifyToHandle(audioWrapper->father);
                notifyToHandle(videoWrapper->father);

                // 不退出线程
                notifyToReadWait();
                if (audioWrapper->father->isPausedForSeek
                    || videoWrapper->father->isPausedForSeek) {
                    audioWrapper->father->isReading = true;
                    videoWrapper->father->isReading = true;
                    continue;
                }
            }// 文件已读完

            // exit
            if (!audioWrapper->father->isReading
                && !videoWrapper->father->isReading) {
                // for (;;) end
                break;
            }

            if (srcAVPacket->stream_index == audioWrapper->father->streamIndex) {
                if (audioWrapper->father->isReading) {
                    readDataImpl(audioWrapper->father, srcAVPacket, copyAVPacket);
                }
            } else if (srcAVPacket->stream_index == videoWrapper->father->streamIndex) {
                if (videoWrapper->father->isReading) {
                    readDataImpl(videoWrapper->father, srcAVPacket, copyAVPacket);
                }
            }
        }// for(;;) end

        if (avFormatContext != NULL) {
            avformat_free_context(avFormatContext);
            avFormatContext = NULL;
        }

        if (srcAVPacket != NULL) {
            av_packet_unref(srcAVPacket);
            srcAVPacket = NULL;
        }

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
        if (ret < 0) {
            LOGE("audio 转换时出错 %d", ret);
//            return ret;
        } else {
            if (!audioWrapper->father->isStarted) {
                audioWrapper->father->isStarted = true;
            }
            while (videoWrapper != NULL
                   && videoWrapper->father != NULL
                   && !videoWrapper->father->isStarted) {
                // usleep(1000);
                audioSleep(1);
            }

            audioTimeDifference =
                    decodedAVFrame->pts * av_q2d(stream->time_base);
            //LOGD("handleData() audio audioTimeDifference: %lf\n", audioTimeDifference);

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

            if (needOutputAudio) {
                write(audioWrapper->father->outBuffer1, 0, out_buffer_size);
            }
        }

        return ret;
    }

    int handleVideoDataImpl(AVStream *stream, AVFrame *decodedAVFrame) {
        if (!videoWrapper->father->isStarted) {
            videoWrapper->father->isStarted = true;
        }
        while (audioWrapper != NULL
               && audioWrapper->father != NULL
               && !audioWrapper->father->isStarted) {
            if (videoWrapper->father->isPausedForSeek) {
                return 0;
            }
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

        /***
         以音频为基准,同步视频到音频
         1.视频慢了则加快播放或丢掉部分视频帧
         2.视频快了则延迟播放,继续渲染上一帧
         音频需要正常播放才是好的体验
         */
        videoTimeDifference = decodedAVFrame->pts * av_q2d(stream->time_base);
        if (videoTimeDifference < audioTimeDifference) {
            // 正常情况下videoTimeDifference比audioTimeDifference大一些
            // 如果发现小了,说明视频播放慢了,应丢弃这些帧
            // break后videoTimeDifference增长的速度会加快
            return 0;
        }
        //if (videoTimeDifference - audioTimeDifference > 2.000000) {
        if (videoTimeDifference - audioTimeDifference > TIME_DIFFERENCE) {
            // 不好的现象.为什么会出现这种情况还不知道?
            LOGE("handleVideoDataImpl() audioTimeDifference: %lf\n", audioTimeDifference);
            LOGE("handleVideoDataImpl() videoTimeDifference: %lf\n", videoTimeDifference);
            LOGE("handleVideoDataImpl() video - audio      : %lf\n", (videoTimeDifference - audioTimeDifference));
            // videoTimeDifference = audioTimeDifference;
            audioTimeDifference = videoTimeDifference;
        }
        // 如果videoTimeDifference比audioTimeDifference大出了一定的范围
        // 那么说明视频播放快了,应等待音频
        while (videoTimeDifference - audioTimeDifference > TIME_DIFFERENCE) {
            if (videoWrapper->father->isPausedForSeek) {
                return 0;
            }
            videoSleep(1);
        }

        if (videoWrapper->father->isHandling) {
            // 3.lock锁定下一个即将要绘制的Surface
            ANativeWindow_lock(pANativeWindow, &mANativeWindow_Buffer, NULL);
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
                memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
            }

            // timeDifference = 0.040000
            /*double timeDifference = videoTimeDifference - videoTimeDifferencePre;
            // 单位: 毫秒
            long tempSleep = timeDifference * 1000;
            tempSleep -= 30;
            tempSleep += step;
            if (videoSleepTime != tempSleep) {
                videoSleepTime = tempSleep;
            }
            //LOGW("handleVideoDataImpl() timeDifference     : %lf\n", timeDifference);
            //LOGW("handleVideoDataImpl() timeDifference     : %ld\n", tempSleep);
            //LOGW("handleVideoDataImpl() videoSleepTime     : %ld\n", videoSleepTime);
            if (videoSleepTime < 13 && videoSleepTime > 0) {
                videoSleep(videoSleepTime);
            } else {
                if (videoSleepTime > 0) {
                    // 好像是个比较合理的值
                    videoSleep(11);
                }
                // videoSleepTime <= 0时不需要sleep
            }*/

            /*int tempSleep = (int) ((videoTimeDifference - videoTimeDifferencePre) * 1000) - 30;
            if (videoSleepTime != tempSleep) {
                videoSleepTime = tempSleep;
            }
            LOGW("handleVideoDataImpl() ----------------------------------\n");
            LOGW("handleVideoDataImpl() audioTimeDifference: %lf\n", audioTimeDifference);
            LOGW("handleVideoDataImpl() videoTimeDifference: %lf\n", videoTimeDifference);
            LOGW("handleVideoDataImpl() videoSleepTime     : %ld\n", videoSleepTime);
            LOGW("handleVideoDataImpl() ----------------------------------\n");
            if (videoSleepTime < 13 && videoSleepTime > 0) {
                videoSleep(videoSleepTime);
            } else {
                if (videoSleepTime > 0) {
                    // 好像是个比较合理的值
                    videoSleep(11);
                }
                // videoSleepTime <= 0时不需要sleep
            }*/

            if (videoSleepTime > 0) {
                videoSleep(videoSleepTime);
            }

            ////////////////////////////////////////////////////////

            /*if ((pts = decodedAVFrame->best_effort_timestamp) == AV_NOPTS_VALUE) {
                pts = 0;
            }
            cur_play_time = pts * av_q2d(stream->time_base);
            // 纠正时间
            cur_play_time = synchronize(decodedAVFrame, cur_play_time);
            delay = cur_play_time - pre_play_time;
            if (delay <= 0 || delay > 1) {
                delay = last_delay;
            }
            // audio_clock = ffmpegVideo->ffmpegMusic->clock;
            last_delay = delay;
            pre_play_time = cur_play_time;
            // 音频与视频的时间差
            diff = video_clock - audio_clock;
            //        在合理范围外  才会延迟  加快
            sync_threshold = (delay > 0.01 ? 0.01 : delay);

            if (fabs(diff) < 10) {
                if (diff <= -sync_threshold) {
                    delay = 0;
                } else if (diff >= sync_threshold) {
                    delay = 2 * delay;
                }
            }
            start_time += delay;
            actual_delay = start_time - av_gettime() / 1000000.0;
            if (actual_delay < 0.01) {
                actual_delay = 0.01;
            }
            av_usleep(actual_delay * 1000000.0 + 6000);*/

            ////////////////////////////////////////////////////////

            // 6.unlock绘制
            ANativeWindow_unlockAndPost(pANativeWindow);
        }
    }

    int handleDataClose(Wrapper *wrapper) {
        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() for (;;) audio end\n");
            /*LOGD("handleData() audio readFramesCount   : %d\n", wrapper->readFramesCount);
            LOGD("handleData() audio handleFramesCount : %d\n", wrapper->handleFramesCount);
            if (wrapper->readFramesCount == wrapper->handleFramesCount) {
                LOGD("%s\n", "handleData() audio正常播放完毕");
            }*/

            if (videoWrapper != NULL && videoWrapper->father != NULL) {
                notifyToHandle(videoWrapper->father);
                if (videoWrapper->father->isHandling) {
                    // 音频暂停,等待视频结束
                    notifyToHandleWait(wrapper);
                }
            }

            // closeAudio();
            LOGD("%s\n", "handleData() audio end");
        } else {
            LOGW("handleData() for (;;) video end\n");
            /*LOGW("handleData() video readFramesCount   : %d\n", wrapper->readFramesCount);
            LOGW("handleData() video handleFramesCount : %d\n", wrapper->handleFramesCount);
            if (wrapper->readFramesCount == wrapper->handleFramesCount) {
                LOGW("%s\n", "handleData() video正常播放完毕");
            }*/

            // 让"读线程"退出
            LOGW("%s\n", "handleData() notifyToRead");
            notifyToRead();

            if (audioWrapper != NULL && audioWrapper->father != NULL) {
                notifyToHandle(audioWrapper->father);
                if (audioWrapper->father->isHandling) {
                    // 视频暂停,等待音频结束
                    notifyToHandleWait(wrapper);
                }
            }

            stop();
            closeAudio();
            closeVideo();
            onFinished();
            pthread_mutex_destroy(&readLockMutex);
            pthread_cond_destroy(&readLockCondition);
            LOGW("%s\n", "handleData() video end");
        }
    }

    void *handleData(void *opaque) {
        if (opaque == NULL) {
            return NULL;
        }
        Wrapper *wrapper = NULL;
        int *type = (int *) opaque;
        if (*type == TYPE_AUDIO) {
            wrapper = audioWrapper->father;
        } else {
            wrapper = videoWrapper->father;
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
        }

        audioTimeDifference = 0.0;
        videoTimeDifference = 0.0;
        totalTimeDifference = 0.0;
        totalTimeDifferenceCount = 0.0;
        onlyOneVideo = true;
        curAVFramePtsVideo = 0;
        preAVFramePtsVideo = 0;

        int ret = 0, out_buffer_size = 0;

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

        for (;;) {
            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            // 暂停装置
            if (wrapper->isPausedForUser
                || wrapper->isPausedForCache
                || wrapper->isPausedForSeek) {
                bool isPausedForUser = wrapper->isPausedForUser;
                bool isPausedForSeek = wrapper->isPausedForSeek;
                if (isPausedForSeek) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGD("handleData() wait() Seek  audio start\n");
                    } else {
                        LOGW("handleData() wait() Seek  video start\n");
                    }
                    wrapper->needToSeek = true;
                    wrapper->isHandleList1Full = false;
                    wrapper->list1->clear();
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
                if (wrapper->isPausedForUser || wrapper->isPausedForSeek) {
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
            }// 暂停装置 end

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (wrapper->list1->size() > 0) {
                srcAVPacket = &wrapper->list1->front();
                // 内容copy
                av_packet_ref(copyAVPacket, srcAVPacket);
                av_packet_unref(srcAVPacket);
                wrapper->list1->pop_front();
                wrapper->handleFramesCount++;
            }

            size_t list2Size = wrapper->list2->size();
            if (wrapper->isReading) {
                if (wrapper->list1->size() == 0) {
                    wrapper->isHandleList1Full = false;
                    if (list2Size > 0) {
                        pthread_mutex_lock(&readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        wrapper->isHandleList1Full = true;
                        pthread_mutex_unlock(&readLockMutex);

                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("handleData() audio 接下去要处理的数据有 list1: %d\n", wrapper->list1->size());
                        } else {
                            LOGW("handleData() video 接下去要处理的数据有 list1: %d\n", wrapper->list1->size());
                            notifyToRead(wrapper);
                        }
                    }
                }
            } else {
                if (wrapper->list1->size() > 0) {
                    // 还有数据,先用完再说
                } else {
                    if (list2Size > 0) {
                        // 把剩余的数据全部复制过来
                        pthread_mutex_lock(&readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        pthread_mutex_unlock(&readLockMutex);

                        if (wrapper->type == TYPE_AUDIO) {
                            LOGD("handleData() audio 最后要处理的数据还有 list1: %d\n", wrapper->list1->size());
                        } else {
                            LOGW("handleData() video 最后要处理的数据还有 list1: %d\n", wrapper->list1->size());
                            notifyToRead(wrapper);
                        }
                    }
                    // 读线程已经结束,所以不需要再暂停
                    wrapper->isHandleList1Full = true;
                }
            }

            if (wrapper->isReading
                && !wrapper->isHandleList1Full
                && wrapper->list2->size() == 0) {
                // 开始暂停
                onPaused();

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
            }

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            // 解码过程
            ret = avcodec_send_packet(wrapper->avCodecContext, copyAVPacket);
            av_packet_unref(copyAVPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("handleData() audio avcodec_send_packet   ret: %d\n", ret);
                    } else {
                        LOGE("handleData() video avcodec_send_packet   ret: %d\n", ret);
                    }
                    break;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGE("audio 发送数据包到解码器时出错 %d", ret);
                    } else {
                        LOGE("video 发送数据包到解码器时出错 %d", ret);
                    }
                    wrapper->isHandling = false;
                    break;
                case 0:
                default:
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
                    wrapper->isHandling = false;
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

            ///////////////////////////////////////////////////////////////////

            // 播放声音和渲染画面
            if (wrapper->type == TYPE_AUDIO) {
                handleAudioDataImpl(stream, decodedAVFrame);
            } else {
                handleVideoDataImpl(stream, decodedAVFrame);
                videoTimeDifferencePre = videoTimeDifference;
            }

            // 设置结束标志
            if (!wrapper->isReading
                && wrapper->list1->size() == 0
                && wrapper->list2->size() == 0) {
                wrapper->isHandling = false;
            }

            ///////////////////////////////////////////////////////////////////
        }//for(;;) end

        handleDataClose(wrapper);

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
        if (audioWrapper->father->avCodecContext != NULL) {
            avcodec_close(audioWrapper->father->avCodecContext);
            av_free(audioWrapper->father->avCodecContext);
            audioWrapper->father->avCodecContext = NULL;
        }
        pthread_mutex_destroy(&audioWrapper->father->readLockMutex);
        pthread_cond_destroy(&audioWrapper->father->readLockCondition);
        pthread_mutex_destroy(&audioWrapper->father->handleLockMutex);
        pthread_cond_destroy(&audioWrapper->father->handleLockCondition);

        //av_free(audioWrapper->father->list1);
        //av_free(audioWrapper->father->list2);
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
        if (videoWrapper->father->avCodecContext != NULL) {
            avcodec_close(videoWrapper->father->avCodecContext);
            av_free(videoWrapper->father->avCodecContext);
            videoWrapper->father->avCodecContext = NULL;
        }

        pthread_mutex_destroy(&videoWrapper->father->readLockMutex);
        pthread_cond_destroy(&videoWrapper->father->readLockCondition);
        pthread_mutex_destroy(&videoWrapper->father->handleLockMutex);
        pthread_cond_destroy(&videoWrapper->father->handleLockCondition);

        //av_free(videoWrapper->father->list1);
        //av_free(videoWrapper->father->list2);
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
            return -1;
        }
        if (findStreamIndex() < 0) {
            LOGE("findStreamIndex() failed\n");
            closeAudio();
            closeVideo();
            return -1;
        }
        if (findAndOpenAVCodecForAudio() < 0) {
            LOGE("findAndOpenAVCodecForAudio() failed\n");
            closeAudio();
            closeVideo();
            return -1;
        }
        if (findAndOpenAVCodecForVideo() < 0) {
            LOGE("findAndOpenAVCodecForVideo() failed\n");
            closeAudio();
            closeVideo();
            return -1;
        }
        if (createSwrContent() < 0) {
            LOGE("createSwrContent() failed\n");
            closeAudio();
            closeVideo();
            return -1;
        }
        if (createSwsContext() < 0) {
            LOGE("createSwsContext() failed\n");
            closeAudio();
            closeVideo();
            return -1;
        }

        LOGW("%s\n", "initPlayer() end");
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

        if (pANativeWindow != NULL) {
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = NULL;
        }
        // 1.获取一个关联Surface的NativeWindow窗体
        pANativeWindow = ANativeWindow_fromSurface(env, surfaceJavaObject);
        if (pANativeWindow == NULL) {
            LOGI("handleVideoData() pANativeWindow is NULL\n");
        } else {
            LOGI("handleVideoData() pANativeWindow isn't NULL\n");
        }
    }

    int play() {
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = false;
            notifyToHandle(audioWrapper->father);
        }
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = false;
            notifyToHandle(videoWrapper->father);
        }
        return 0;
    }

    int pause() {
        LOGI("pause() start\n");
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = true;
        }
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->isPausedForUser = true;
        }
        LOGI("pause() end\n");
        return 0;
    }

    int stop() {
        if (audioWrapper == NULL
            || audioWrapper->father == NULL
            || videoWrapper == NULL
            || videoWrapper->father == NULL) {
            LOGE("stop() return\n");
            return -1;
        }

        LOGI("stop() start\n");

        // audio
        audioWrapper->father->isStarted = false;
        audioWrapper->father->isReading = false;
        audioWrapper->father->isHandling = false;
        audioWrapper->father->isPausedForUser = false;
        audioWrapper->father->isPausedForCache = false;
        audioWrapper->father->isPausedForSeek = false;
        audioWrapper->father->isHandleList1Full = false;
        // video
        videoWrapper->father->isStarted = false;
        videoWrapper->father->isReading = false;
        videoWrapper->father->isHandling = false;
        videoWrapper->father->isPausedForUser = false;
        videoWrapper->father->isPausedForCache = false;
        videoWrapper->father->isPausedForSeek = false;
        videoWrapper->father->isHandleList1Full = false;

        notifyToRead(audioWrapper->father);
        notifyToHandle(audioWrapper->father);
        notifyToRead(videoWrapper->father);
        notifyToHandle(videoWrapper->father);

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
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioRunning = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling;
        }
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoRunning = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling;
        }
        return audioRunning || videoRunning;
    }

    // 有没有在播放,暂停状态不算播放状态
    bool isPlaying() {
        bool audioPlaying = false;
        bool videoPlaying = false;
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioPlaying = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling
                           && !audioWrapper->father->isPausedForUser
                           && !audioWrapper->father->isPausedForCache;
        }
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoPlaying = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling
                           && !videoWrapper->father->isPausedForUser
                           && !videoWrapper->father->isPausedForCache;
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
    int seekTo(int64_t timestamp) {
        LOGI("==================================================================\n");
        LOGI("seekTo() timestamp: %ld\n", timestamp);

        if (timestamp < 0) {
            return -1;
        }

        if (audioWrapper == NULL
            || audioWrapper->father == NULL
            || videoWrapper == NULL
            || videoWrapper->father == NULL) {
            return -1;
        }

        timeStamp = timestamp;

        LOGD("seekTo() signal() to Read and Handle\n");
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
        int64_t audioDuration = 0;
        int64_t videoDuration = 0;
        int64_t duration = 0;

        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioDuration = audioWrapper->father->duration;
        }
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoDuration = videoWrapper->father->duration;
        }
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
        ++videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepAdd()      videoSleepTime: %d\n", videoSleepTime);
    }

    void stepSubtract() {
        --videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepSubtract() videoSleepTime: %d\n", videoSleepTime);
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
