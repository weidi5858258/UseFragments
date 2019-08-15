//
// Created by root on 19-8-8.
//

#include "SimpleVideoPlayer.h"

namespace alexander {

    char inFilePath[2048];
    ANativeWindow *nativeWindow = NULL;

    int getAVPacketFromQueue(struct AVPacketQueue *packet_queue,
                             AVPacket *avpacket);

    // 已经不需要调用了
    void initAV() {
        avcodec_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avformat_network_init();
        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
    }

    void initAudio() {
        if (audioWrapper != NULL) {
            av_free(audioWrapper);
            audioWrapper = NULL;
        }
        audioWrapper = (struct AudioWrapper *) av_mallocz(sizeof(struct AudioWrapper));
        memset(audioWrapper, 0, sizeof(struct AudioWrapper));
        audioWrapper->father = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(audioWrapper->father, 0, sizeof(struct Wrapper));

        audioWrapper->father->type = TYPE_AUDIO;
        audioWrapper->father->next = NEXT_QUEUE1;
        audioWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_AUDIO;

        audioWrapper->father->streamIndex = -1;
        audioWrapper->father->readFramesCount = 0;
        audioWrapper->father->handleFramesCount = 0;
        audioWrapper->father->isStarted = false;
        audioWrapper->father->isReading = false;
        audioWrapper->father->isHandling = false;
        audioWrapper->father->isPausedForUser = false;
        audioWrapper->father->isPausedForCache = false;
        audioWrapper->father->isReadQueue1Full = false;
        audioWrapper->father->isReadQueue2Full = false;
        audioWrapper->father->duration = -1;
        audioWrapper->father->timestamp = -1;

        audioWrapper->dstSampleRate = 44100;
        audioWrapper->dstAVSampleFormat = AV_SAMPLE_FMT_S16;
        audioWrapper->dstChannelLayout = AV_CH_LAYOUT_STEREO;

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
    }

    void initVideo() {
        if (videoWrapper != NULL) {
            av_free(videoWrapper);
            videoWrapper = NULL;
        }
        videoWrapper = (struct VideoWrapper *) av_mallocz(sizeof(struct VideoWrapper));
        memset(videoWrapper, 0, sizeof(struct VideoWrapper));
        videoWrapper->father = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(videoWrapper->father, 0, sizeof(struct Wrapper));

        videoWrapper->father->type = TYPE_VIDEO;
        videoWrapper->father->next = NEXT_QUEUE1;
        videoWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_VIDEO;

        videoWrapper->father->streamIndex = -1;
        videoWrapper->father->readFramesCount = 0;
        videoWrapper->father->handleFramesCount = 0;
        videoWrapper->father->isStarted = false;
        videoWrapper->father->isReading = false;
        videoWrapper->father->isHandling = false;
        videoWrapper->father->isPausedForUser = false;
        videoWrapper->father->isPausedForCache = false;
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
    }

    int openAndFindAVFormatContextForAudio() {
        // AVFormatContext初始化,里面设置结构体的一些默认信息
        // 相当于Java中创建对象
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

        // avPacket ---> srcAVFrame ---> dstAVFrame ---> 播放声音
        audioWrapper->father->srcAVFrame = av_frame_alloc();
        // audioWrapper->father->dstAVFrame = av_frame_alloc();
        // 16bit 44100 PCM 数据,16bit是2个字节
        audioWrapper->father->outBuffer1 = (unsigned char *) av_malloc(MAX_AUDIO_FRAME_SIZE);
        audioWrapper->father->outBufferSize = MAX_AUDIO_FRAME_SIZE;

        return 0;
    }

    int createSwsContext() {
        videoWrapper->srcWidth = videoWrapper->father->avCodecContext->width;
        videoWrapper->srcHeight = videoWrapper->father->avCodecContext->height;
        videoWrapper->srcAVPixelFormat = videoWrapper->father->avCodecContext->pix_fmt;
        LOGI("---------------------------------\n");
        LOGI("srcWidth            : %d\n", videoWrapper->srcWidth);
        LOGI("srcHeight           : %d\n", videoWrapper->srcHeight);
        LOGI("srcAVPixelFormat    : %d\n", videoWrapper->srcAVPixelFormat);
        videoWrapper->dstWidth = videoWrapper->srcWidth;
        videoWrapper->dstHeight = videoWrapper->srcHeight;
        videoWrapper->dstAVPixelFormat = videoWrapper->srcAVPixelFormat;
        videoWrapper->srcArea = videoWrapper->srcWidth * videoWrapper->srcHeight;

        // avPacket ---> srcAVFrame ---> dstAVFrame ---> 渲染画面
        videoWrapper->father->srcAVFrame = av_frame_alloc();
        videoWrapper->father->dstAVFrame = av_frame_alloc();

        // srcXXX与dstXXX的参数必须要按照下面这样去设置,不然播放画面会有问题的
        // 根据视频源得到的AVPixelFormat,Width和Height计算出一帧视频所需要的空间大小
        int imageGetBufferSize = av_image_get_buffer_size(
                videoWrapper->dstAVPixelFormat, videoWrapper->srcWidth, videoWrapper->srcHeight, 1);
        LOGI("imageGetBufferSize  : %d\n", imageGetBufferSize);
        videoWrapper->father->outBufferSize = imageGetBufferSize;
        // 存储视频帧的原始数据
        videoWrapper->father->outBuffer1 = (unsigned char *) av_malloc(imageGetBufferSize);
        // 类似于格式化刚刚申请的内存(关联操作:dstAVFrame, outBuffer1, dstAVPixelFormat)
        int imageFillArrays = av_image_fill_arrays(
                // uint8_t *dst_data[4]
                videoWrapper->father->dstAVFrame->data,
                // int dst_linesize[4]
                videoWrapper->father->dstAVFrame->linesize,
                // const uint8_t *src
                videoWrapper->father->outBuffer1,
                videoWrapper->dstAVPixelFormat,
                videoWrapper->srcWidth, videoWrapper->srcHeight, 1);
        if (imageFillArrays < 0) {
            LOGI("imageFillArrays     : %d\n", imageFillArrays);
            return -1;
        }
        videoWrapper->swsContext = sws_getContext(
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->srcAVPixelFormat,
                videoWrapper->srcWidth, videoWrapper->srcHeight, videoWrapper->dstAVPixelFormat,
                SWS_BICUBIC,//flags
                NULL, NULL, NULL);
        if (videoWrapper->swsContext == NULL) {
            LOGI("%s\n", "videoSwsContext is NULL.");
            return -1;
        }
        LOGI("---------------------------------\n");

        if (videoWrapper->father->avFormatContext->duration != AV_NOPTS_VALUE) {
            int hours, mins, secs, us;
            int64_t duration = videoWrapper->father->avFormatContext->duration + 5000;
            LOGI("createSwsContext() duration: %ld\n", duration);
            // 得到的是秒数
            secs = duration / AV_TIME_BASE;
            LOGI("createSwsContext() secs    : %d\n", secs);
            us = duration % AV_TIME_BASE;
            mins = secs / 60;
            secs %= 60;
            hours = mins / 60;
            mins %= 60;
            // 00:54:16.50
            LOGI("%02d:%02d:%02d.%02d\n", hours, mins, secs, (100 * us) / AV_TIME_BASE);
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

    void *readData(void *opaque) {
        if (opaque == NULL) {
            return NULL;
        }
        Wrapper *wrapper = NULL;
        int *type = (int *) opaque;
        if (*type == 1) {
            wrapper = audioWrapper->father;
        } else {
            wrapper = videoWrapper->father;
        }
        //Wrapper *wrapper = (Wrapper *) opaque;
        if (wrapper == NULL) {
            LOGI("%s\n", "wrapper is NULL");
            return NULL;
        }

        if (wrapper->type == TYPE_AUDIO) {
            LOGI("%s\n", "readData() audio start");
        } else {
            LOGI("%s\n", "readData() video start");
        }

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *dstAVPacket = av_packet_alloc();

        int readFrame = -1;
        wrapper->isReading = true;
        for (;;) {
            if (wrapper->timestamp != -1) {
                if (wrapper->type == TYPE_AUDIO) {
                    LOGI("%s\n", "readData() audio seek start");
                } else {
                    LOGI("%s\n", "readData() video seek start");
                }

                pthread_mutex_lock(&wrapper->readLockMutex);
                pthread_cond_wait(&wrapper->readLockCondition, &wrapper->readLockMutex);
                pthread_mutex_unlock(&wrapper->readLockMutex);

                // avcodec_flush_buffers(wrapper->avCodecContext);
                if (wrapper->type == TYPE_AUDIO) {
                    LOGI("%s\n", "readData() audio seek end");
                } else {
                    LOGI("%s\n", "readData() video seek end");
                }
                wrapper->timestamp = -1;
            }


            if (!wrapper->isReading) {
                break;
            }

            av_packet_unref(srcAVPacket);

            while (1) {
                // 读取一帧压缩数据放到avPacket
                // 0 if OK, < 0 on error or end of file
                // 有时读一次跳出,有时读多次跳出
                readFrame = av_read_frame(wrapper->avFormatContext, srcAVPacket);
                // LOGI("readFrame           : %d\n", readFrame);
                if (readFrame < 0) {
                    wrapper->isReading = false;

                    if (wrapper->type == TYPE_AUDIO) {
                        LOGF("readData() audio readFrame: %d\n", readFrame);
                    } else {
                        LOGF("readData() video readFrame: %d\n", readFrame);
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
                            LOGI("readData() audio signal() handleLockCondition break\n");
                        } else {
                            LOGI("readData() video signal() handleLockCondition break\n");
                        }
                        // 唤醒线程
                        pthread_mutex_lock(&wrapper->handleLockMutex);
                        pthread_cond_signal(&wrapper->handleLockCondition);
                        pthread_mutex_unlock(&wrapper->handleLockMutex);
                    }

                    // 读到文件末尾了,退出
                    break;
                }// while(1) end

                if (srcAVPacket->stream_index == wrapper->streamIndex) {
                    if (!wrapper->isReadQueue1Full) {
                        wrapper->readFramesCount++;
                        // 非常非常非常必须的
                        av_copy_packet(dstAVPacket, srcAVPacket);
                        putAVPacketToQueue(wrapper->queue1, dstAVPacket);
                        if (wrapper->queue1->allAVPacketsCount == wrapper->maxAVPacketsCount) {
                            wrapper->isReadQueue1Full = true;
                            if (wrapper->type == TYPE_AUDIO) {
                                LOGI("readData() audio Queue1満了\n");
                                LOGI("readData() audio allAVPacketsSize : %ld\n",
                                     wrapper->queue1->allAVPacketsSize);
                                LOGI("readData() audio signal() handleLockCondition\n");
                            } else {
                                LOGI("readData() video Queue1満了\n");
                                LOGI("readData() video allAVPacketsSize : %ld\n",
                                     wrapper->queue1->allAVPacketsSize);
                                LOGI("readData() video signal() handleLockCondition\n");
                            }
                            // 唤醒线程
                            pthread_mutex_lock(&wrapper->handleLockMutex);
                            pthread_cond_signal(&wrapper->handleLockCondition);
                            pthread_mutex_unlock(&wrapper->handleLockMutex);
                        }
                    } else if (!wrapper->isReadQueue2Full) {
                        wrapper->readFramesCount++;
                        av_copy_packet(dstAVPacket, srcAVPacket);
                        putAVPacketToQueue(wrapper->queue2, dstAVPacket);
                        if (wrapper->queue2->allAVPacketsCount == wrapper->maxAVPacketsCount) {
                            wrapper->isReadQueue2Full = true;
                            if (wrapper->type == TYPE_AUDIO) {
                                LOGI("readData() audio Queue2満了\n");
                                LOGI("readData() audio allAVPacketsSize : %ld\n",
                                     wrapper->queue2->allAVPacketsSize);
                                if (!audioWrapper->father->isPausedForCache) {
                                    break;
                                }
                                LOGI("readData() audio signal() handleLockCondition\n");
                            } else {
                                LOGI("readData() video Queue2満了\n");
                                LOGI("readData() video allAVPacketsSize : %ld\n",
                                     wrapper->queue2->allAVPacketsSize);
                                LOGI("readData() video signal() handleLockCondition\n");
                            }
                            pthread_mutex_lock(&wrapper->handleLockMutex);
                            pthread_cond_signal(&wrapper->handleLockCondition);
                            pthread_mutex_unlock(&wrapper->handleLockMutex);
                        }
                    } else if (wrapper->isReadQueue1Full && wrapper->isReadQueue2Full) {
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGI("readData() audio Queue1和Queue2都満了,好开心( ^_^ )\n");
                            LOGI("readData() audio wait() readLockCondition start\n");
                        } else {
                            LOGI("readData() video Queue1和Queue2都満了,好开心( ^_^ )\n");
                            LOGI("readData() video wait() readLockCondition start\n");
                        }
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        pthread_cond_wait(&wrapper->readLockCondition, &wrapper->readLockMutex);
                        pthread_mutex_unlock(&wrapper->readLockMutex);
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGI("readData() audio wait() readLockCondition end\n");
                        } else {
                            LOGI("readData() video wait() readLockCondition end\n");
                        }

                        if (!wrapper->isReadQueue1Full) {
                            wrapper->readFramesCount++;
                            av_copy_packet(dstAVPacket, srcAVPacket);
                            putAVPacketToQueue(wrapper->queue1, dstAVPacket);
                        } else if (!wrapper->isReadQueue2Full) {
                            wrapper->readFramesCount++;
                            av_copy_packet(dstAVPacket, srcAVPacket);
                            putAVPacketToQueue(wrapper->queue2, dstAVPacket);
                        }
                    }
                    break;
                } else {
                    // 遇到其他流时释放
                    if (srcAVPacket->data) {
                        av_packet_unref(srcAVPacket);
                    }
                }
            }// while(1) end
        }// for(;;) end

        av_packet_unref(srcAVPacket);

        if (wrapper->type == TYPE_AUDIO) {
            LOGI("readData() audio readFramesCount          : %d\n",
                 wrapper->readFramesCount);
            LOGI("%s\n", "readData() audio end");
        } else {
            LOGI("readData() video readFramesCount          : %d\n",
                 wrapper->readFramesCount);
            LOGI("%s\n", "readData() video end");
        }
        return NULL;
    }

    long audioTimeDifference = 0;
    long videoTimeDifference = 0;

    void *handleAudioData(void *opaque) {
        LOGD("%s\n", "handleAudioData() start");

        // 线程等待
        LOGD("handleAudioData() wait() start\n");
        pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
        pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                          &audioWrapper->father->handleLockMutex);
        pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
        LOGD("handleAudioData() wait() end\n");

        //audioSleep(3 * 1000);

        int ret = 0, out_buffer_size = 0;
        AVStream *stream =
                audioWrapper->father->avFormatContext->streams[audioWrapper->father->streamIndex];
        int64_t duration = stream->duration * av_q2d(stream->time_base);
        LOGD("handleAudioData() duration: %ld\n", duration);
        // 压缩数据
        AVPacket *avPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = audioWrapper->father->srcAVFrame;
        audioWrapper->father->isHandling = true;
        for (;;) {
            if (audioWrapper->father->isPausedForUser
                || audioWrapper->father->isPausedForCache) {
                LOGD("handleAudioData() User wait() start\n");
                pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                                  &audioWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                LOGD("handleAudioData() User wait() end\n");
            }

            if (!audioWrapper->father->isHandling) {
                break;
            }

            memset(avPacket, 0, sizeof(*avPacket));
            if (audioWrapper->father->next == NEXT_QUEUE1
                && audioWrapper->father->isReadQueue1Full
                && audioWrapper->father->queue1->allAVPacketsCount > 0) {
                audioWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(audioWrapper->father->queue1, avPacket);
                if (audioWrapper->father->queue1->allAVPacketsCount == 0) {
                    memset(audioWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
                    audioWrapper->father->isReadQueue1Full = false;
                    if (audioWrapper->father->isReadQueue2Full) {
                        audioWrapper->father->next = NEXT_QUEUE2;
                    }
                    LOGD("handleAudioData() Queue1 用完了\n");
                    LOGD("handleAudioData() Queue2 allAVPacketsCount: %d\n",
                         audioWrapper->father->queue2->allAVPacketsCount);
                    if (audioWrapper->father->isReading) {
                        LOGD("handleAudioData() signal() readLockCondition\n");
                        pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                        pthread_cond_signal(&audioWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                    }
                }
            } else if (audioWrapper->father->next == NEXT_QUEUE2
                       && audioWrapper->father->isReadQueue2Full
                       && audioWrapper->father->queue2->allAVPacketsCount > 0) {
                audioWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(audioWrapper->father->queue2, avPacket);
                if (audioWrapper->father->queue2->allAVPacketsCount == 0) {
                    memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
                    audioWrapper->father->isReadQueue2Full = false;
                    if (audioWrapper->father->isReadQueue1Full) {
                        audioWrapper->father->next = NEXT_QUEUE1;
                    }
                    LOGD("handleAudioData() Queue2 用完了\n");
                    LOGD("handleAudioData() Queue1 allAVPacketsCount: %d\n",
                         audioWrapper->father->queue1->allAVPacketsCount);
                    if (audioWrapper->father->isReading) {
                        LOGD("handleAudioData() signal() readLockCondition\n");
                        pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                        pthread_cond_signal(&audioWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                    }
                }
            } else if (audioWrapper->father->isReading
                       && !audioWrapper->father->isReadQueue1Full
                       && !audioWrapper->father->isReadQueue2Full) {
                // Cache引起的暂停
#ifdef USE_VIDEO
                if (videoWrapper != NULL && videoWrapper->father != NULL) {
                    videoWrapper->father->isPausedForCache = true;
                }
#endif
                audioWrapper->father->isPausedForCache = true;
                LOGD("handleAudioData() wait() Cache start\n");
                pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                                  &audioWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                LOGD("handleAudioData() wait() Cache end\n");
                audioWrapper->father->isPausedForCache = false;
#ifdef USE_VIDEO
                if (videoWrapper != NULL && videoWrapper->father != NULL) {
                    videoWrapper->father->isPausedForCache = false;
                    pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                    pthread_cond_signal(&videoWrapper->father->handleLockCondition);
                    pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                }
#endif
                continue;
            } else if (!audioWrapper->father->isReading
                       && audioWrapper->father->queue1->allAVPacketsCount == 0
                       && audioWrapper->father->queue2->allAVPacketsCount == 0) {
                audioWrapper->father->isHandling = false;
                break;
            }

            if (!avPacket) {
                if (!audioWrapper->father->isReading
                    && audioWrapper->father->queue1->allAVPacketsCount == 0
                    && audioWrapper->father->queue2->allAVPacketsCount == 0) {
                    break;
                }
                continue;
            }

            ret = avcodec_send_packet(audioWrapper->father->avCodecContext, avPacket);
            if (ret < 0) {
                LOGE("audio 发送数据包到解码器时出错 %d", ret);
                break;
            }

            while (1) {
                ret = avcodec_receive_frame(audioWrapper->father->avCodecContext, decodedAVFrame);
                switch (ret) {
                    // 输出是不可用的,必须发送新的输入
                    case AVERROR(EAGAIN):
                        break;
                        // 已经完全刷新,不会再有输出帧了
                    case AVERROR_EOF:
                        break;
                        // codec打不开,或者是一个encoder
                    case AVERROR(EINVAL):
                        break;
                        // 成功,返回一个输出帧
                    case 0:
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
                                   && !videoWrapper->father->isStarted
                                   && videoWrapper->father->isHandling) {
                                // usleep(1000);
                                audioSleep(1);
                            }
                            audioTimeDifference =
                                    decodedAVFrame->pts * av_q2d(stream->time_base) * AV_TIME_BASE;
                            /*LOGW("handleAudioData() audioTimeDifference : %ld\n",
                                 audioTimeDifference);*/
                            /*LOGW("handleAudioData() decodedAVFrame->pts : %lf\n",
                                 decodedAVFrame->pts * av_q2d(stream->time_base));*/
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
                                    // 缓冲区大小对齐（0 = 默认值，1 = 不对齐）
                                    1);

                            write(audioWrapper->father->outBuffer1, 0, out_buffer_size);
                        }
                        break;
                    default:// 合法的解码错误
                        LOGE("从解码器接收帧时出错 %d", ret);
                        break;
                }
                break;
            }//while(1) end
            av_packet_unref(avPacket);
        }//for(;;) end

        av_packet_unref(avPacket);
        avPacket = NULL;

        close();

        LOGD("handleAudioData() audio handleFramesCount : %d\n",
             audioWrapper->father->handleFramesCount);

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

        //videoSleep(3 * 1000);

        int64_t prePts = 0;
        int64_t nowPts = 0;
        double timeDifference = 0;
        long sleep = 0;
        int ret = 0;

        LOGW("handleVideoData() ANativeWindow_setBuffersGeometry() start\n");
        // 2.设置缓冲区的属性（宽、高、像素格式）,像素格式要和SurfaceView的像素格式一直
        ANativeWindow_setBuffersGeometry(nativeWindow,
                                         videoWrapper->srcWidth, videoWrapper->srcHeight,
                                         WINDOW_FORMAT_RGBA_8888);
        // 绘制时的缓冲区
        ANativeWindow_Buffer outBuffer;
        LOGW("handleVideoData() ANativeWindow_setBuffersGeometry() end\n");

        AVStream *stream =
                videoWrapper->father->avFormatContext->streams[videoWrapper->father->streamIndex];
        int64_t duration = stream->duration * av_q2d(stream->time_base);
        LOGW("handleVideoData() duration: %ld\n", duration);
        LOGW("handleVideoData() secs    : %d\n", stream->duration / AV_TIME_BASE);
        // 必须创建
        AVPacket *avPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = videoWrapper->father->srcAVFrame;
        AVFrame *rgbAVFrame = av_frame_alloc();
        videoWrapper->father->isHandling = true;
        for (;;) {
            if (videoWrapper->father->isPausedForUser
                || videoWrapper->father->isPausedForCache) {
                LOGW("handleVideoData() User wait() start\n");
                pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                                  &videoWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                LOGW("handleVideoData() User wait() end\n");
            }

            if (!videoWrapper->father->isHandling) {
                break;
            }

            memset(avPacket, 0, sizeof(*avPacket));
            if (videoWrapper->father->next == NEXT_QUEUE1
                && videoWrapper->father->isReadQueue1Full
                && videoWrapper->father->queue1->allAVPacketsCount > 0) {
                videoWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(videoWrapper->father->queue1, avPacket);
                if (videoWrapper->father->queue1->allAVPacketsCount == 0) {
                    memset(videoWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
                    videoWrapper->father->isReadQueue1Full = false;
                    if (videoWrapper->father->isReadQueue2Full) {
                        videoWrapper->father->next = NEXT_QUEUE2;
                    }
                    LOGW("handleVideoData() Queue1 用完了\n");
                    LOGW("handleVideoData() Queue2 allAVPacketsCount: %d\n",
                         videoWrapper->father->queue2->allAVPacketsCount);
                    if (videoWrapper->father->isReading) {
                        LOGW("handleVideoData() signal() readLockCondition\n");
                        pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                        pthread_cond_signal(&videoWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
                    }
                }
            } else if (videoWrapper->father->next == NEXT_QUEUE2
                       && videoWrapper->father->isReadQueue2Full
                       && videoWrapper->father->queue2->allAVPacketsCount > 0) {
                videoWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(videoWrapper->father->queue2, avPacket);
                if (videoWrapper->father->queue2->allAVPacketsCount == 0) {
                    memset(videoWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
                    videoWrapper->father->isReadQueue2Full = false;
                    if (videoWrapper->father->isReadQueue1Full) {
                        videoWrapper->father->next = NEXT_QUEUE1;
                    }
                    LOGW("handleVideoData() Queue2 用完了\n");
                    LOGW("handleVideoData() Queue1 allAVPacketsCount: %d\n",
                         videoWrapper->father->queue1->allAVPacketsCount);
                    if (videoWrapper->father->isReading) {
                        LOGW("handleVideoData() signal() readLockCondition\n");
                        pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                        pthread_cond_signal(&videoWrapper->father->readLockCondition);
                        pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
                    }
                }
            } else if (videoWrapper->father->isReading
                       && !videoWrapper->father->isReadQueue1Full
                       && !videoWrapper->father->isReadQueue2Full) {
                // cache引起的暂停
#ifdef USE_AUDIO
                if (audioWrapper != NULL && audioWrapper->father != NULL) {
                    audioWrapper->father->isPausedForCache = true;
                }
#endif
                videoWrapper->father->isPausedForCache = true;
                LOGE("handleVideoData() wait() Cache start\n");
                pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                                  &videoWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                LOGE("handleVideoData() wait() Cache end\n");
                videoWrapper->father->isPausedForCache = false;
#ifdef USE_AUDIO
                if (audioWrapper != NULL && audioWrapper->father != NULL) {
                    audioWrapper->father->isPausedForCache = false;
                    pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                    pthread_cond_signal(&audioWrapper->father->handleLockCondition);
                    pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                }
#endif
                continue;
            } else if (!videoWrapper->father->isReading
                       && videoWrapper->father->queue1->allAVPacketsCount == 0
                       && videoWrapper->father->queue2->allAVPacketsCount == 0) {
                videoWrapper->father->isHandling = false;
                LOGW("handleVideoData() 电影结束,散场\n");
                break;
            }

            if (!avPacket) {
                if (!videoWrapper->father->isReading
                    && videoWrapper->father->queue1->allAVPacketsCount == 0
                    && videoWrapper->father->queue2->allAVPacketsCount == 0) {
                    break;
                }
                continue;
            }

            ret = avcodec_send_packet(videoWrapper->father->avCodecContext, avPacket);
            if (ret < 0) {
                LOGE("video 发送数据包到解码器时出错 %d", ret);
                break;
            }
            while (1) {
                ret = avcodec_receive_frame(videoWrapper->father->avCodecContext, decodedAVFrame);
                if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                    //LOGD("avcodec_receive_frame：%d\n", ret);
                    break;
                } else if (ret < 0) {
                    //LOGW("avcodec_receive_frame：%d\n", AVERROR(ret));
                    break;  //end处进行资源释放等善后处理
                }

                if (ret >= 0) {
                    if (!videoWrapper->father->isStarted) {
                        videoWrapper->father->isStarted = true;
                    }
#ifdef USE_AUDIO
                    while (audioWrapper != NULL
                           && audioWrapper->father != NULL
                           && !audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling) {
                        videoSleep(1);
                    }
#endif
                    nowPts = decodedAVFrame->pts;
#ifdef USE_AUDIO
                    videoTimeDifference = nowPts * av_q2d(stream->time_base) * AV_TIME_BASE;
                    if (videoTimeDifference == audioTimeDifference) {
                        nowPts += 50000;

                        /*LOGI("handleVideoData() videoTimeDifference : %ld\n",
                             videoTimeDifference);
                        if (audioWrapper != NULL && audioWrapper->father != NULL) {
                            audioWrapper->father->isPausedForUser = true;
                        }
                        pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                        pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                                          &videoWrapper->father->handleLockMutex);
                        pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);*/
                    }
#endif
                    timeDifference = (nowPts - prePts) * av_q2d(stream->time_base);
                    prePts = nowPts;
#ifdef USE_AUDIO
                    /*LOGI("handleVideoData() videoTimeDifference : %ld\n",
                         videoTimeDifference);*/
                    if (videoTimeDifference < audioTimeDifference) {
                        // break后videoTimeDifference增长的速度会加快
                        break;
                    }
                    while (videoTimeDifference - audioTimeDifference > 70000) {
                        videoSleep(10);
                    }
#endif
                    if (videoWrapper->father->isHandling) {
                        // 3.lock锁定下一个即将要绘制的Surface
                        ANativeWindow_lock(nativeWindow, &outBuffer, NULL);
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

                        sleep = timeDifference * 1000;// 0.040000
                        //LOGI("handleVideoData() sleep1 : %ld\n", sleep);
                        sleep -= 30;
                        //LOGI("handleVideoData() sleep2 : %ld\n", sleep);
                        if (sleep < 1000 && sleep > 0) {
                            videoSleep(sleep);
                        } else {
                            videoSleep(10);
                        }

                        // 6.unlock绘制
                        ANativeWindow_unlockAndPost(nativeWindow);
                    }

                    break;
                }
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

        av_packet_unref(avPacket);
        avPacket = NULL;

        av_frame_free(&rgbAVFrame);
        rgbAVFrame = NULL;

        LOGW("handleVideoData() video handleFramesCount : %d\n",
             videoWrapper->father->handleFramesCount);

        closeVideo();

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
        if (audioWrapper->father->srcData[0] != NULL) {
            av_freep(&audioWrapper->father->srcData[0]);
            audioWrapper->father->srcData[0] = NULL;
        }
        if (audioWrapper->father->dstData[0] != NULL) {
            av_freep(&audioWrapper->father->dstData[0]);
            audioWrapper->father->dstData[0] = NULL;
        }
        if (audioWrapper->swrContext != NULL) {
            swr_free(&audioWrapper->swrContext);
            audioWrapper->swrContext = NULL;
        }
        if (audioWrapper->father->srcAVFrame != NULL) {
            av_frame_free(&audioWrapper->father->srcAVFrame);
            audioWrapper->father->srcAVFrame = NULL;
        }
        if (audioWrapper->father->dstAVFrame != NULL) {
            av_frame_free(&audioWrapper->father->dstAVFrame);
            audioWrapper->father->dstAVFrame = NULL;
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
        if (nativeWindow != NULL) {
            // 7.释放资源
            ANativeWindow_release(nativeWindow);
            nativeWindow = NULL;
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
        if (videoWrapper->father->srcData[0] != NULL) {
            av_freep(&videoWrapper->father->srcData[0]);
            videoWrapper->father->srcData[0] = NULL;
        }
        if (videoWrapper->father->dstData[0] != NULL) {
            av_freep(&videoWrapper->father->dstData[0]);
            videoWrapper->father->dstData[0] = NULL;
        }
        if (videoWrapper->swsContext != NULL) {
            sws_freeContext(videoWrapper->swsContext);
            videoWrapper->swsContext = NULL;
        }
        if (videoWrapper->father->srcAVFrame != NULL) {
            av_frame_free(&videoWrapper->father->srcAVFrame);
            videoWrapper->father->srcAVFrame = NULL;
        }
        if (videoWrapper->father->dstAVFrame != NULL) {
            av_frame_free(&videoWrapper->father->dstAVFrame);
            videoWrapper->father->dstAVFrame = NULL;
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

/*#ifdef USE_AUDIO
        pthread_t audioReadDataThread, audioHandleDataThread;
#endif
#ifdef USE_AUDIO
        // 创建线程
        pthread_create(&audioReadDataThread, NULL, readData, audioWrapper->father);
        pthread_create(&audioHandleDataThread, NULL, handleAudioData, NULL);
#endif
#ifdef USE_AUDIO
        // 等待线程执行完
        pthread_join(audioReadDataThread, NULL);
        pthread_join(audioHandleDataThread, NULL);
#endif
#ifdef USE_AUDIO
        // 取消线程
        //pthread_cancel(audioReadDataThread);
        //pthread_cancel(audioHandleDataThread);
        closeAudio();
#endif*/

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

/*#ifdef USE_VIDEO
        pthread_t videoReadDataThread, videoHandleDataThread;
#endif
#ifdef USE_VIDEO
        // 创建线程
        pthread_create(&videoReadDataThread, NULL, readData, videoWrapper->father);
        pthread_create(&videoHandleDataThread, NULL, handleVideoData, NULL);
#endif
#ifdef USE_VIDEO
        // 等待线程执行完
        pthread_join(videoReadDataThread, NULL);
        pthread_join(videoHandleDataThread, NULL);
#endif
#ifdef USE_VIDEO
        // 取消线程
        //pthread_cancel(videoReadDataThread);
        //pthread_cancel(videoHandleDataThread);
        closeVideo();
#endif*/

        LOGI("%s\n", "initVideoPlayer() end");

        return 0;
    }

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject) {
        memset(inFilePath, 0, sizeof(inFilePath));
        av_strlcpy(inFilePath, filePath, sizeof(inFilePath));
        LOGI("setJniParameters() inFilePath: %s", inFilePath);
#ifdef USE_VIDEO
        // 1.获取一个关联Surface的NativeWindow窗体
        nativeWindow = ANativeWindow_fromSurface(env, surfaceJavaObject);
        if (nativeWindow == NULL) {
            LOGI("handleVideoData() nativeWindow is NULL\n");
        } else {
            LOGI("handleVideoData() nativeWindow isn't NULL\n");
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
        if (timestamp < 0) {
            return -1;
        }
        LOGI("==================================================================\n");
#ifdef USE_AUDIO
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->timestamp = timestamp;

            pthread_mutex_lock(&audioWrapper->father->readLockMutex);
            pthread_cond_signal(&audioWrapper->father->readLockCondition);
            pthread_mutex_unlock(&audioWrapper->father->readLockMutex);

            audioWrapper->father->next = NEXT_QUEUE1;
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
            pthread_mutex_lock(&audioWrapper->father->readLockMutex);
            pthread_cond_signal(&audioWrapper->father->readLockCondition);
            pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
        }
#endif
#ifdef USE_VIDEO
        if (videoWrapper != NULL && videoWrapper->father != NULL) {
            videoWrapper->father->timestamp = timestamp;

            pthread_mutex_lock(&videoWrapper->father->readLockMutex);
            pthread_cond_signal(&videoWrapper->father->readLockCondition);
            pthread_mutex_unlock(&videoWrapper->father->readLockMutex);

            videoWrapper->father->next = NEXT_QUEUE1;
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
            pthread_mutex_lock(&videoWrapper->father->readLockMutex);
            pthread_cond_signal(&videoWrapper->father->readLockCondition);
            pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
        }
#endif
        return 0;
    }

}