//
// Created by root on 19-8-8.
//

#include <string>
#include "AudioVideoPlayer.h"

#define LOG "player_alexander"

namespace alexander_audio_video {

    static char inVideoFilePath[2048];
    static char inAudioFilePath[2048];
    static AVFormatContext *videoAVFormatContext = NULL;
    static AVFormatContext *audioAVFormatContext = NULL;
    static struct AudioWrapper *audioWrapper = NULL;
    static struct VideoWrapper *videoWrapper = NULL;
    static bool isLocal = false;
    static bool isVideoReading = false;
    static bool isAudioReading = false;
    static bool isAudioHandling = false;
    static bool runOneTime = true;
    // seek时间
    static int64_t timeStamp = -1;
    static long curProgress = 0;
    static long preProgress = 0;
    // 视频播放时每帧之间的暂停时间,单位为ms
    static int videoSleepTime = 11;

    static double TIME_DIFFERENCE = 1.000000;// 0.180000
    // 当前音频时间戳
    static double audioPts = 0;
    // 当前视频时间戳
    static double videoPts = 0;
    // 上一个时间戳
    static double videoPtsPre = 0;

    static ANativeWindow *pANativeWindow = NULL;
    // 绘制时的缓冲区
    static ANativeWindow_Buffer mANativeWindow_Buffer;

#define RUN_COUNTS 88
    int runCounts = 0;
    double averageTimeDiff = 0;
    double timeDiff[RUN_COUNTS];

    char *getStrAVPixelFormat(AVPixelFormat format);

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

    // 已经不需要调用了
    void initAV() {
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avcodec_register_all();

        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        avformat_network_init();

        LOGW("ffmpeg [av_version_info()] version: %s\n", av_version_info());

        videoSleepTime = 11;
        preProgress = 0;
        audioPts = 0.0;
        videoPts = 0.0;
        videoPtsPre = 0;
        runOneTime = true;
        runCounts = 0;
        averageTimeDiff = 0.0;
        memset(timeDiff, '0', sizeof(timeDiff));
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

        // Android支持的目标像素格式
        // AV_PIX_FMT_RGB32
        // AV_PIX_FMT_RGBA
        videoWrapper->dstAVPixelFormat = AV_PIX_FMT_RGBA;
        videoWrapper->srcWidth = 0;
        videoWrapper->srcHeight = 0;
    }

    int openAndFindAVFormatContext() {
        LOGI("openAndFindAVFormatContext() start\n");
        if (videoAVFormatContext != NULL) {
            LOGI("openAndFindAVFormatContext() videoAVFormatContext isn't NULL\n");
            avformat_free_context(videoAVFormatContext);
            videoAVFormatContext = NULL;
        }
        if (audioAVFormatContext != NULL) {
            LOGI("openAndFindAVFormatContext() videoAVFormatContext isn't NULL\n");
            avformat_free_context(audioAVFormatContext);
            audioAVFormatContext = NULL;
        }
        videoAVFormatContext = avformat_alloc_context();
        audioAVFormatContext = avformat_alloc_context();
        if (videoAVFormatContext == NULL
            || audioAVFormatContext == NULL) {
            LOGE("videoAVFormatContext or audioAVFormatContext is NULL.\n");
            return -1;
        }
        if (avformat_open_input(&videoAVFormatContext,
                                inVideoFilePath,
                                NULL, NULL) != 0) {
            LOGE("Couldn't open video input stream.\n");
            return -1;
        }
        if (avformat_open_input(&audioAVFormatContext,
                                inAudioFilePath,
                                NULL, NULL) != 0) {
            LOGE("Couldn't open audio input stream.\n");
            return -1;
        }
        if (avformat_find_stream_info(videoAVFormatContext, NULL) != 0) {
            LOGE("Couldn't find video stream information.\n");
            return -1;
        }
        if (avformat_find_stream_info(audioAVFormatContext, NULL) != 0) {
            LOGE("Couldn't find audio stream information.\n");
            return -1;
        }
        LOGI("openAndFindAVFormatContext() end\n");
        return 0;
    }

    int findStreamIndex() {
        LOGI("findStreamIndex() start\n");
        // stream counts
        int streams = videoAVFormatContext->nb_streams;
        LOGI("videoAVFormatContext Stream counts   : %d\n", streams);
        for (int i = 0; i < streams; i++) {
            // 得到AVCodecParameters
            AVCodecParameters *avCodecParameters = videoAVFormatContext->streams[i]->codecpar;
            if (avCodecParameters != NULL) {
                AVMediaType mediaType = avCodecParameters->codec_type;
                switch (mediaType) {
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

        streams = audioAVFormatContext->nb_streams;
        LOGI("audioAVFormatContext Stream counts   : %d\n", streams);
        for (int i = 0; i < streams; i++) {
            // 得到AVCodecParameters
            AVCodecParameters *avCodecParameters = audioAVFormatContext->streams[i]->codecpar;
            if (avCodecParameters != NULL) {
                AVMediaType mediaType = avCodecParameters->codec_type;
                switch (mediaType) {
                    case AVMEDIA_TYPE_AUDIO: {
                        audioWrapper->father->streamIndex = i;
                        audioWrapper->father->avCodecParameters = avCodecParameters;
                        LOGD("audioStreamIndex: %d\n", audioWrapper->father->streamIndex);
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        if (audioWrapper->father->streamIndex == -1
            || videoWrapper->father->streamIndex == -1) {
            LOGE("Didn't find audio or video stream.\n");
            return -1;
        }

        videoWrapper->father->avStream = videoAVFormatContext->streams[videoWrapper->father->streamIndex];
        audioWrapper->father->avStream = audioAVFormatContext->streams[audioWrapper->father->streamIndex];
        LOGI("findStreamIndex() end\n");
        return 0;
    }

    int findAndOpenAVCodecForAudio() {
        LOGI("findAndOpenAVCodecForAudio() start\n");
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
                    audioWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name(
                            "hevc_mediacodec");
                    break;
                }
                case AV_CODEC_ID_H264: {
                    LOGD("findAndOpenAVCodecForAudio() h264_mediacodec\n");
                    // 硬解码264
                    audioWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name(
                            "h264_mediacodec");
                    break;
                }
                case AV_CODEC_ID_MPEG4: {
                    LOGD("findAndOpenAVCodecForAudio() mpeg4_mediacodec\n");
                    // 硬解码mpeg4
                    audioWrapper->father->decoderAVCodec = avcodec_find_decoder_by_name(
                            "mpeg4_mediacodec");
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

        LOGI("findAndOpenAVCodecForAudio() end\n");
        return 0;
    }

    int findAndOpenAVCodecForVideo() {
        LOGI("findAndOpenAVCodecForVideo() start\n");
        // video
        if (videoWrapper->father->streamIndex != -1) {
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
            // 有相应的so库时这句就不要执行了
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
        LOGI("createSwrContent() start\n");
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

        LOGI("createSwrContent() end\n");
        return 0;
    }

    int createSwsContext() {
        LOGI("createSwsContext() start\n");
        videoWrapper->srcWidth = videoWrapper->father->avCodecContext->width;
        videoWrapper->srcHeight = videoWrapper->father->avCodecContext->height;
        videoWrapper->srcAVPixelFormat = videoWrapper->father->avCodecContext->pix_fmt;
        //onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);
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

        LOGI("createSwsContext() end\n");
        return 0;
    }

    int seekToImpl(Wrapper *wrapper) {
        // seekTo

        if (wrapper->type == TYPE_AUDIO) {
            LOGI("seekToImpl() audio sleep start\n");
            while (!audioWrapper->father->needToSeek
                   || !videoWrapper->father->needToSeek) {
                if (!audioWrapper->father->isHandling
                    || !videoWrapper->father->isHandling) {
                    return 0;
                }
                av_usleep(1000);
            }
            LOGI("seekToImpl() audio sleep end\n");
            LOGI("seekToImpl() audio list2 size: %d\n", audioWrapper->father->list2->size());
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
            LOGI("seekToImpl() audio av_seek_frame start\n");
            av_seek_frame(audioAVFormatContext, -1,
                          timeStamp * AV_TIME_BASE,
                          AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
            avcodec_flush_buffers(audioWrapper->father->avCodecContext);
            audioWrapper->father->isPausedForSeek = false;
            audioWrapper->father->isStarted = false;
            videoWrapper->father->isStarted = false;
        } else {
            LOGI("seekToImpl() video sleep start\n");
            while (!audioWrapper->father->needToSeek
                   || !videoWrapper->father->needToSeek) {
                if (!audioWrapper->father->isHandling
                    || !videoWrapper->father->isHandling) {
                    return 0;
                }
                av_usleep(1000);
            }
            LOGI("seekToImpl() video sleep end\n");
            LOGI("seekToImpl() video list2 size: %d\n", videoWrapper->father->list2->size());
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
            LOGI("seekToImpl() video av_seek_frame start\n");
            av_seek_frame(videoAVFormatContext, -1,
                          timeStamp * AV_TIME_BASE,
                          AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
            avcodec_flush_buffers(videoWrapper->father->avCodecContext);
            videoWrapper->father->isPausedForSeek = false;
            videoWrapper->father->isStarted = false;
            audioWrapper->father->isStarted = false;
        }

        if (!audioWrapper->father->isPausedForSeek
            && !videoWrapper->father->isPausedForSeek) {
            timeStamp = -1;
            preProgress = 0;
            videoPtsPre = 0;
        }

        if (wrapper->type == TYPE_AUDIO) {
            LOGI("seekToImpl() audio av_seek_frame end\n");
        } else {
            LOGI("seekToImpl() video av_seek_frame end\n");
        }
        LOGI("==================================================================\n");
        return 0;
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

        if (!wrapper->isHandleList1Full
            && list2Size == wrapper->list1LimitCounts) {
            pthread_mutex_lock(&wrapper->readLockMutex);
            wrapper->list1->clear();
            wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
            wrapper->list2->clear();
            wrapper->isHandleList1Full = true;
            pthread_mutex_unlock(&wrapper->readLockMutex);
            notifyToHandle(wrapper);

            if (wrapper->type == TYPE_AUDIO) {
                LOGD("readDataImpl() audio 填满数据了\n");
            } else {
                LOGW("readDataImpl() video 填满数据了\n");
            }
        } else if (list2Size >= wrapper->list2LimitCounts) {
            if (wrapper->type == TYPE_AUDIO) {
                LOGD("readDataImpl() audio list1: %d\n", wrapper->list1->size());
                LOGD("readDataImpl() audio list2: %d\n", wrapper->list2->size());
                LOGD("readDataImpl() notifyToReadWait audio start\n");
            } else {
                LOGW("readDataImpl() video list1: %d\n", wrapper->list1->size());
                LOGW("readDataImpl() video list2: %d\n", wrapper->list2->size());
                LOGW("readDataImpl() notifyToReadWait video start\n");
            }
            notifyToReadWait(wrapper);
            if (wrapper->type == TYPE_AUDIO) {
                LOGD("readDataImpl() notifyToReadWait audio end\n");
            } else {
                LOGW("readDataImpl() notifyToReadWait video end\n");
            }
        }
        return 0;
    }

    void *readData(void *opaque) {
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
            onFinished();
            LOGF("%s\n", "readData() finish");
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

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("%s\n", "readData() audio start");
        } else {
            LOGW("%s\n", "readData() video start");
        }

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();

        // seekTo
        if (timeStamp > 0) {
            LOGI("readData() timeStamp: %ld\n", (long) timeStamp);
            audioWrapper->father->needToSeek = true;
            videoWrapper->father->needToSeek = true;
            audioWrapper->father->isPausedForSeek = true;
            videoWrapper->father->isPausedForSeek = true;
        }

        if (wrapper->type == TYPE_AUDIO) {
            isAudioReading = true;
            // seekTo
            if (timeStamp > 0) {
                LOGD("readData() timeStamp: %ld\n", (long) timeStamp);
                audioWrapper->father->needToSeek = true;
                audioWrapper->father->isPausedForSeek = true;
            }
        } else {
            isVideoReading = true;
            // seekTo
            if (timeStamp > 0) {
                LOGW("readData() timeStamp: %ld\n", (long) timeStamp);
                videoWrapper->father->needToSeek = true;
                videoWrapper->father->isPausedForSeek = true;
            }
        }
        int readFrame;
        /***
         有几种情况:
         1.list1中先存满n个,然后list2多次存取
         2.list1中先存满n个,然后list2一次性存满
         3.list1中还没满n个文件就读完了
         */
        for (;;) {
            // exit
            if (!wrapper->isReading) {
                // for (;;) end
                break;
            }

            if (wrapper->isPausedForSeek && timeStamp >= 0) {
                // seekTo
                seekToImpl(wrapper);
            }

            if (wrapper->type == TYPE_AUDIO) {
                readFrame = av_read_frame(audioAVFormatContext, srcAVPacket);
            } else {
                readFrame = av_read_frame(videoAVFormatContext, srcAVPacket);
            }

            // region readFrame < 0

            // 0 if OK, < 0 on error or end of file
            if (readFrame < 0) {
                if (readFrame != -12 && readFrame != AVERROR_EOF) {
                    LOGE("readData() readFrame  : %d\n", readFrame);
                    continue;
                }

                LOGF("readData() AVERROR_EOF: %d\n", AVERROR_EOF);
                LOGF("readData() readFrame  : %d\n", readFrame);
                if (wrapper->type == TYPE_AUDIO) {
                    LOGF("readData() audio list2: %d\n", wrapper->list2->size());
                    audioWrapper->father->isReading = false;
                    audioWrapper->father->isHandleList1Full = true;
                    notifyToHandle(audioWrapper->father);
                } else {
                    LOGF("readData() video list2: %d\n", wrapper->list2->size());
                    videoWrapper->father->isReading = false;
                    videoWrapper->father->isHandleList1Full = true;
                    notifyToHandle(videoWrapper->father);
                }

                // 不退出线程
                if (wrapper->type == TYPE_AUDIO) {
                    LOGI("readData() notifyToReadWait audio start\n");
                } else {
                    LOGI("readData() notifyToReadWait video start\n");
                }
                notifyToReadWait(wrapper);
                if (wrapper->type == TYPE_AUDIO) {
                    LOGI("readData() notifyToReadWait audio end\n");
                } else {
                    LOGI("readData() notifyToReadWait video end\n");
                }

                if (wrapper->isPausedForSeek) {
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGF("readData() audio start seek\n");
                    } else {
                        LOGF("readData() video start seek\n");
                    }
                    wrapper->isReading = true;
                    continue;
                } else {
                    // for (;;) end
                    break;
                }
            }// 文件已读完

            // endregion

            if (wrapper->type == TYPE_AUDIO) {
                if (srcAVPacket->stream_index == audioWrapper->father->streamIndex) {
                    if (audioWrapper->father->isReading) {
                        readDataImpl(audioWrapper->father, srcAVPacket, copyAVPacket);
                    }
                }
            } else {
                if (srcAVPacket->stream_index == videoWrapper->father->streamIndex) {
                    if (videoWrapper->father->isReading) {
                        readDataImpl(videoWrapper->father, srcAVPacket, copyAVPacket);
                    }
                }
            }
        }// for(;;) end

        if (srcAVPacket != NULL) {
            av_packet_unref(srcAVPacket);
            srcAVPacket = NULL;
        }

        if (wrapper->type == TYPE_AUDIO) {
            isAudioReading = false;
        } else {
            isVideoReading = false;
        }

        if (wrapper->type == TYPE_AUDIO) {
            LOGF("%s\n", "readData() audio end");
        } else {
            LOGF("%s\n", "readData() video end");
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
        if (ret >= 0) {
            audioWrapper->father->isStarted = true;
            while (!videoWrapper->father->isStarted) {
                if (audioWrapper->father->isPausedForSeek
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
            curProgress = (long) audioPts;// 秒
            if (curProgress > preProgress) {
                preProgress = curProgress;
                onProgressUpdated(curProgress);
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
                || !videoWrapper->father->isHandling) {
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
                    totleTimeDiff += timeDiff[i];
                }
                averageTimeDiff = totleTimeDiff / RUN_COUNTS;
                LOGI("handleVideoDataImpl() averageTimeDiff    : %lf\n", averageTimeDiff);
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
                    || !videoWrapper->father->isHandling) {
                    return 0;
                }
                av_usleep(1000);
            }
        }

        // 渲染画面
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
            ANativeWindow_unlockAndPost(pANativeWindow);
        }
    }

    int handleDataClose(Wrapper *wrapper) {
        // 让"读线程"退出
        notifyToRead(wrapper);

        if (wrapper->type == TYPE_AUDIO) {
            LOGD("handleData() for (;;) audio end\n");
            isAudioHandling = false;
            LOGF("%s\n", "handleData() audio end");
        } else {
            LOGW("handleData() for (;;) video end\n");
            while (isVideoReading || isAudioReading || isAudioHandling) {
                av_usleep(1000);
            }
            LOGF("%s\n", "handleData() video end");
            closeAudio();
            closeVideo();
            // 必须保证每次退出都要执行到
            onFinished();
            LOGF("%s\n", "Safe Exit");
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
            isAudioHandling = true;
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

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = NULL;
        if (wrapper->type == TYPE_AUDIO) {
            decodedAVFrame = audioWrapper->decodedAVFrame;
        } else {
            decodedAVFrame = videoWrapper->decodedAVFrame;
        }

        int ret;
        for (;;) {
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
            }// 暂停装置 end

            // endregion

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
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
                            LOGD("handleData() audio 接下去要处理的数据有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGW("handleData() video 接下去要处理的数据有 list1: %d\n",
                                 videoWrapper->father->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 videoWrapper->father->list2->size());
                        } else {
                            LOGW("handleData() video 接下去要处理的数据有 list1: %d\n",
                                 wrapper->list1->size());
                            LOGW("handleData() video                   list2: %d\n",
                                 wrapper->list2->size());
                            LOGD("handleData() audio 接下去要处理的数据有 list1: %d\n",
                                 audioWrapper->father->list1->size());
                            LOGD("handleData() audio                   list2: %d\n",
                                 audioWrapper->father->list2->size());
                        }
                    }
                    notifyToRead(wrapper);
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
                    } else {
                        wrapper->isHandling = false;
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
                handleAudioDataImpl(wrapper->avStream, decodedAVFrame);
            } else {
                handleVideoDataImpl(wrapper->avStream, decodedAVFrame);
            }

            ///////////////////////////////////////////////////////////////////

            // 设置结束标志
            if (!wrapper->isReading
                && wrapper->list1->size() == 0
                && wrapper->list2->size() == 0) {
                wrapper->isHandling = false;
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
        if (audioAVFormatContext != NULL) {
            avformat_free_context(audioAVFormatContext);
            audioAVFormatContext = NULL;
        }
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
        if (videoAVFormatContext != NULL) {
            avformat_free_context(videoAVFormatContext);
            videoAVFormatContext = NULL;
        }
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
            onError(0x100, "openAndFindAVFormatContext() failed");
            return -1;
        }
        if (findStreamIndex() < 0) {
            LOGE("findStreamIndex() failed\n");
            closeAudio();
            closeVideo();
            onError(0x100, "findStreamIndex() failed");
            return -1;
        }
        if (findAndOpenAVCodecForAudio() < 0) {
            LOGE("findAndOpenAVCodecForAudio() failed\n");
            closeAudio();
            closeVideo();
            onError(0x100, "findAndOpenAVCodecForAudio() failed");
            return -1;
        }
        if (findAndOpenAVCodecForVideo() < 0) {
            LOGE("findAndOpenAVCodecForVideo() failed\n");
            closeAudio();
            closeVideo();
            onError(0x100, "findAndOpenAVCodecForVideo() failed");
            return -1;
        }
        if (createSwrContent() < 0) {
            LOGE("createSwrContent() failed\n");
            closeAudio();
            closeVideo();
            onError(0x100, "createSwrContent() failed");
            return -1;
        }
        if (createSwsContext() < 0) {
            LOGE("createSwsContext() failed\n");
            closeAudio();
            closeVideo();
            onError(0x100, "createSwsContext() failed");
            return -1;
        }

        if (!audioWrapper->father->isReading
            || !audioWrapper->father->isHandling
            || !videoWrapper->father->isReading
            || !videoWrapper->father->isHandling) {
            closeAudio();
            closeVideo();
            onFinished();
            LOGW("%s\n", "initPlayer() finish");
            return -1;
        }

        int64_t videoDuration = videoAVFormatContext->duration / AV_TIME_BASE;
        int64_t audioDuration = audioAVFormatContext->duration / AV_TIME_BASE;
        LOGD("initPlayer() videoDuration: %ld\n", (long) videoDuration);
        LOGD("initPlayer() audioDuration: %ld\n", (long) audioDuration);
        if (videoAVFormatContext->duration != AV_NOPTS_VALUE) {
            // 得到的是秒数
            videoDuration = (videoAVFormatContext->duration + 5000) / AV_TIME_BASE;
        }
        if (audioAVFormatContext->duration != AV_NOPTS_VALUE) {
            // 得到的是秒数
            audioDuration = (audioAVFormatContext->duration + 5000) / AV_TIME_BASE;
        }
        if (videoDuration > 0 && audioDuration > 0) {
            if (videoDuration > audioDuration) {
                videoDuration = audioDuration;
            } else {
                audioDuration = videoDuration;
            }
            int hours, mins, seconds;
            seconds = audioDuration;
            mins = seconds / 60;
            seconds %= 60;
            hours = mins / 60;
            mins %= 60;
            // 00:54:16
            // 单位: 秒
            LOGD("initPlayer() media seconds: %d\n", (int) audioDuration);
            LOGD("initPlayer() media          %02d:%02d:%02d\n", hours, mins, seconds);
        }
        audioWrapper->father->duration =
        videoWrapper->father->duration = audioDuration;
        onChangeWindow(videoWrapper->srcWidth, videoWrapper->srcHeight);

        LOGW("%s\n", "initPlayer() end");
        return 0;
    }

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject) {
        LOGI("setJniParameters() start");
//        const char *src = "/storage/emulated/0/Movies/权力的游戏第三季05.mp4";
//        const char *src = "http://192.168.0.112:8080/tomcat_video/game_of_thrones/game_of_thrones_season_1/01.mp4";
//        const char *videoSrc = "/storage/1532-48AD/Videos/download/38289647/2/16/video.m4s";
//        const char *audioSrc = "/storage/1532-48AD/Videos/download/38289647/2/16/audio.m4s";
//        av_strlcpy(inVideoFilePath, videoSrc, sizeof(inVideoFilePath));
//        av_strlcpy(inAudioFilePath, audioSrc, sizeof(inAudioFilePath));

        // /storage/1532-48AD/Videos/download/38289647/2/16/audio.m4s
        // /storage/1532-48AD/Videos/download/38289647/2/16/video.m4s

        memset(inVideoFilePath, '\0', sizeof(inVideoFilePath));
        memset(inAudioFilePath, '\0', sizeof(inAudioFilePath));
        std::string videoPath = std::string(filePath);
        std::string audioPath = std::string(filePath);
        videoPath += "/video.m4s";
        audioPath += "/audio.m4s";
        av_strlcpy(inVideoFilePath, videoPath.c_str(), sizeof(inVideoFilePath));
        av_strlcpy(inAudioFilePath, audioPath.c_str(), sizeof(inAudioFilePath));
        LOGI("setJniParameters() videoFilePath: %s\n", inVideoFilePath);
        LOGI("setJniParameters() audioFilePath: %s\n", inAudioFilePath);

        isLocal = false;
        char *result = strstr(inVideoFilePath, "http://");
        if (result == NULL) {
            result = strstr(inVideoFilePath, "https://");
            if (result == NULL) {
                result = strstr(inVideoFilePath, "rtmp://");
                if (result == NULL) {
                    result = strstr(inVideoFilePath, "rtsp://");
                    if (result == NULL) {
                        isLocal = true;
                    }
                }
            }
        }
        LOGI("setJniParameters()       isLocal: %d\n", isLocal);

        if (pANativeWindow != NULL) {
            LOGI("setJniParameters() pANativeWindow != NULL");
            ANativeWindow_release(pANativeWindow);
            pANativeWindow = NULL;
        }
        LOGI("setJniParameters() ANativeWindow_fromSurface");
        // 1.获取一个关联Surface的NativeWindow窗体
        pANativeWindow = ANativeWindow_fromSurface(env, surfaceJavaObject);
        LOGI("setJniParameters() end");
    }

    int play() {
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
        LOGI("stop() start\n");
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
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
            // video
            videoWrapper->father->isStarted = false;
            videoWrapper->father->isReading = false;
            videoWrapper->father->isHandling = false;
            videoWrapper->father->isPausedForUser = false;
            videoWrapper->father->isPausedForCache = false;
            videoWrapper->father->isPausedForSeek = false;
            videoWrapper->father->isHandleList1Full = false;
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
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioRunning = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling;
        }
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoRunning = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling;
        }
        return audioRunning && videoRunning;
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

    bool isPausedForUser() {
        bool audioPlaying = false;
        bool videoPlaying = false;
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
            audioPlaying = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling
                           && audioWrapper->father->isPausedForUser;
        }
        if (videoWrapper != NULL
            && videoWrapper->father != NULL) {
            videoPlaying = videoWrapper->father->isStarted
                           && videoWrapper->father->isHandling
                           && videoWrapper->father->isPausedForUser;
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

        if ((long) timestamp > 0
            && (audioWrapper == NULL
                || audioWrapper->father == NULL
                || videoWrapper == NULL
                || videoWrapper->father == NULL)) {
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
            || getDuration() < 0
            || ((long) timestamp) > getDuration()) {
            return -1;
        }

        LOGD("seekTo() signal() to Read and Handle\n");
        timeStamp = timestamp;
        audioWrapper->father->isPausedForSeek = true;
        videoWrapper->father->isPausedForSeek = true;
        audioWrapper->father->needToSeek = false;
        videoWrapper->father->needToSeek = false;
        notifyToHandle(audioWrapper->father);
        notifyToHandle(videoWrapper->father);
        notifyToRead(audioWrapper->father);
        notifyToRead(videoWrapper->father);

        return 0;
    }

    // 返回值单位是秒
    long getDuration() {
        int64_t duration = -1;
        if (audioWrapper != NULL
            && audioWrapper->father != NULL) {
            duration = audioWrapper->father->duration;
        }

        return duration;
    }

    void stepAdd(int64_t addStep) {
        /*++videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepAdd()      videoSleepTime: %d\n", videoSleepTime);*/

        if (getDuration() > 0) {
            seekTo(curProgress + addStep);
        }
    }

    void stepSubtract(int64_t subtractStep) {
        /*--videoSleepTime;
        char dest[50];
        sprintf(dest, "videoSleepTime: %d\n", videoSleepTime);
        onInfo(dest);
        LOGF("stepSubtract() videoSleepTime: %d\n", videoSleepTime);*/

        if (getDuration() > 0) {
            seekTo(curProgress - subtractStep);
        }
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
