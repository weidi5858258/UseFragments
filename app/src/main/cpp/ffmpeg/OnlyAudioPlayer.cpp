//
// Created by root on 19-8-8.
//

#include "OnlyAudioPlayer.h"

#define LOG "player_alexander"

namespace alexander_only_audio {

    char inFilePath[2048];
    struct Wrapper *wrapper = NULL;
    struct AudioWrapper *audioWrapper = NULL;

    AVFormatContext *avFormatContext = NULL;
    bool isLocal = false;
    bool isReading = false;
    bool isReadFinished = true;
    // seek时间
    int64_t timeStamp = -1;
    int64_t fileLength = 0;
    long preProgress = 0;
    double audioTimeDifference = 0;

    /////////////////////////////////////////////////////

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
        LOGD("initAV() start\n");
        av_register_all();
        // 用于从网络接收数据,如果不是网络接收数据,可不用（如本例可不用）
        avcodec_register_all();
        // 注册复用器和编解码器,所有的使用ffmpeg,首先必须调用这个函数
        avformat_network_init();
        // 注册设备的函数,如用获取摄像头数据或音频等,需要此函数先注册
        // avdevice_register_all();
        LOGD("initAV() version: %s\n", av_version_info());
        LOGD("initAV() end\n");
    }

    void initAudio() {
        LOGD("initAudio() start\n");
        timeStamp = -1;
        fileLength = 0;
        preProgress = 0;
        audioTimeDifference = 0.0;
        isReading = true;
        isReadFinished = true;

        if (wrapper != NULL) {
            av_free(wrapper);
            wrapper = NULL;
        }
        wrapper = (struct Wrapper *) av_mallocz(sizeof(struct Wrapper));
        memset(wrapper, 0, sizeof(struct Wrapper));

        wrapper->type = TYPE_AUDIO;
        if (isLocal) {
            wrapper->list1LimitCounts = MAX_AVPACKET_COUNT_AUDIO_LOCAL;
        } else {
            wrapper->list1LimitCounts = MAX_AVPACKET_COUNT_AUDIO_HTTP;
        }
        wrapper->list2LimitCounts = MAX_AVPACKET_COUNT;
        LOGD("initAudio() list1LimitCounts: %d\n", wrapper->list1LimitCounts);
        LOGD("initAudio() list2LimitCounts: %d\n", wrapper->list2LimitCounts);
        wrapper->duration = -1;
        wrapper->timestamp = -1;
        wrapper->streamIndex = -1;
        wrapper->isStarted = false;
        wrapper->isReading = true;
        wrapper->isHandling = true;
        wrapper->isPausedForUser = false;
        wrapper->isPausedForCache = false;
        wrapper->isPausedForSeek = false;
        wrapper->needToSeek = false;
        wrapper->isReadList1Full = false;
        wrapper->list1 = new std::list<AVPacket>();
        wrapper->list2 = new std::list<AVPacket>();
        wrapper->readLockMutex = PTHREAD_MUTEX_INITIALIZER;
        wrapper->readLockCondition = PTHREAD_COND_INITIALIZER;
        wrapper->handleLockMutex = PTHREAD_MUTEX_INITIALIZER;
        wrapper->handleLockCondition = PTHREAD_COND_INITIALIZER;

        if (audioWrapper != NULL) {
            av_free(audioWrapper);
            audioWrapper = NULL;
        }
        audioWrapper = (struct AudioWrapper *) av_mallocz(sizeof(struct AudioWrapper));
        memset(audioWrapper, 0, sizeof(struct AudioWrapper));
        audioWrapper->father = wrapper;

        LOGD("initAudio() end\n");
    }

    int openAndFindAVFormatContext() {
        LOGD("openAndFindAVFormatContext() start\n");
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
        LOGD("openAndFindAVFormatContext() avformat_open_input\n");
        if (avformat_open_input(&avFormatContext,
                                inFilePath,
                                NULL, NULL) != 0) {
            LOGE("Couldn't open input stream.\n");
            return -1;
        }
        LOGD("openAndFindAVFormatContext() avformat_find_stream_info\n");
        if (avformat_find_stream_info(avFormatContext, NULL) != 0) {
            LOGE("Couldn't find stream information.\n");
            return -1;
        }
        LOGD("openAndFindAVFormatContext() end\n");

        return 0;
    }

    int findStreamIndex() {
        LOGD("findStreamIndex() start\n");
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
                        break;
                    }
                    default:
                        break;
                }
            }
        }
        if (audioWrapper->father->streamIndex == -1) {
            LOGE("Didn't find audio or video stream.\n");
            return -1;
        }
        LOGD("findStreamIndex() end\n");

        return 0;
    }

    int findAndOpenAVCodecForAudio() {
        LOGD("findAndOpenAVCodecForAudio() start\n");
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
        LOGD("findAndOpenAVCodecForAudio() end\n");

        return 0;
    }

    int createSwrContent() {
        LOGD("createSwrContent() start\n");
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

        // 采样深度
        // 这个对应关系还不知道怎么弄
        int audioFormat = 2;
        switch (audioWrapper->dstAVSampleFormat) {
            case AV_SAMPLE_FMT_NONE: {// -1
                LOGD("createSwrContent() AV_SAMPLE_FMT_NONE\n");
                break;
            }
            case AV_SAMPLE_FMT_U8: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_U8\n");
                break;
            }
            case AV_SAMPLE_FMT_S16: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_S16\n");
                audioFormat = 2;
                break;
            }
            case AV_SAMPLE_FMT_S32: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_S32\n");
                break;
            }
            case AV_SAMPLE_FMT_S64: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_S64\n");
                break;
            }
            case AV_SAMPLE_FMT_FLT: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_FLT\n");
                break;
            }
            case AV_SAMPLE_FMT_DBL: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_DBL\n");
                break;
            }
            case AV_SAMPLE_FMT_U8P: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_U8P\n");
                break;
            }
            case AV_SAMPLE_FMT_S16P: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_S16P\n");
                break;
            }
            case AV_SAMPLE_FMT_S32P: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_S32P\n");
                break;
            }
            case AV_SAMPLE_FMT_S64P: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_S64P\n");
                break;
            }
            case AV_SAMPLE_FMT_FLTP: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_FLTP\n");
                break;
            }
            case AV_SAMPLE_FMT_DBLP: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_DBLP\n");
                break;
            }
            case AV_SAMPLE_FMT_NB: {
                LOGD("createSwrContent() AV_SAMPLE_FMT_NB\n");
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
            // 00:54:16
            // 单位: 秒
            audioWrapper->father->duration = duration / AV_TIME_BASE;

            int hours, mins, seconds;
            // 得到的是秒数
            seconds = audioWrapper->father->duration;
            mins = seconds / 60;
            seconds %= 60;
            hours = mins / 60;
            mins %= 60;
            LOGD("audio duration      : %d\n", (int) audioWrapper->father->duration);
            LOGD("audio                 %02d:%02d:%02d\n", hours, mins, seconds);
        }
        LOGD("createSwrContent() end\n");

        return 0;
    }

    int seekToImpl() {
        if (!isReadFinished) {
            pthread_mutex_lock(&audioWrapper->father->readLockMutex);
            LOGI("readData() audio list2 clear\n");
            audioWrapper->father->list2->clear();
            pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
        }

        while (!audioWrapper->father->needToSeek) {
            // 休眠1毫秒
            // audioSleep(1);
            av_usleep(1000);// 单位:微秒(1000微秒=1毫秒)
        }
        LOGI("seekToImpl() av_seek_frame start\n");
        //LOGI("seekToImpl() timestamp: %" PRIu64 "\n", timestamp);
        LOGI("seekToImpl() fileLength: %ld\n", (long) fileLength);
        LOGI("seekToImpl() timeStamp : %ld\n", (long) timeStamp);
        if (isReadFinished) {
            int64_t tempLength = ((double) timeStamp / audioWrapper->father->duration) * fileLength;
            LOGI("seekToImpl() tempLength: %ld\n", (long) tempLength);
            int64_t size = 0;
            int index = -1;
            std::list<AVPacket>::iterator iter;
            AVPacket tempPkt;
            for (iter = audioWrapper->father->list2->begin();
                 iter != audioWrapper->father->list2->end();
                 iter++) {
                AVPacket srcPkt = *iter;
                size += srcPkt.size;
                index++;
                if (size >= tempLength) {
                    av_packet_ref(&tempPkt, &srcPkt);
                    wrapper->list1->push_back(tempPkt);
                    //LOGI("seekToImpl() pos      : %ld, index: %d\n", (long) avPacket.pos, index);
                    //break;
                }
            }
        } else {
            int ret = av_seek_frame(
                    avFormatContext,
                    //audioWrapper->father->streamIndex,
                    -1,
                    timeStamp * AV_TIME_BASE,
                    //AVSEEK_FLAG_ANY);
                    AVSEEK_FLAG_BACKWARD | AVSEEK_FLAG_FRAME);
            LOGI("seekToImpl() ret      : %d\n", ret);
        }

        timeStamp = -1;
        preProgress = 0;
        audioWrapper->father->isPausedForSeek = false;
        LOGI("seekToImpl() av_seek_frame end\n");
    }

    int readDataImpl(Wrapper *wrapper, AVPacket *srcAVPacket, AVPacket *copyAVPacket) {
        av_packet_ref(copyAVPacket, srcAVPacket);
        av_packet_unref(srcAVPacket);

        fileLength += copyAVPacket->size;
        pthread_mutex_lock(&wrapper->readLockMutex);
        // 存数据
        wrapper->list2->push_back(*copyAVPacket);
        size_t list2Size = wrapper->list2->size();
        pthread_mutex_unlock(&wrapper->readLockMutex);

        if (!wrapper->isReadList1Full
            && list2Size == wrapper->list1LimitCounts) {
            isReadFinished = false;
            // 把list2中的内容全部复制给list1
            wrapper->list1->clear();
            wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
            wrapper->list2->clear();

            wrapper->isReadList1Full = true;
            notifyToHandle(wrapper);
            LOGD("readDataImpl() audio 已填满数据可以播放了\n");
        } else if (list2Size >= wrapper->list2LimitCounts) {
            isReadFinished = false;
            LOGD("readDataImpl() audio list1: %d\n", audioWrapper->father->list1->size());
            LOGD("readDataImpl() audio list2: %d\n", audioWrapper->father->list2->size());
            LOGI("readDataImpl() notifyToReadWait start\n");
            notifyToReadWait(audioWrapper->father);
            LOGI("readDataImpl() notifyToReadWait end\n");
        }

        return 0;
    }

    void *readData(void *opaque) {
        LOGD("%s\n", "readData() start");

        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();

        int count_12 = 0;
        int readFrame = 0;
        /***
         有几种情况:
         1.list1中先存满n个,然后list2多次存取
         2.list1中先存满n个,然后list2一次性存满
         3.list1中还没满n个文件就读完了
         */
        for (;;) {
            // exit
            if (!audioWrapper->father->isReading) {
                // for (;;) end
                break;
            }

            readFrame = 0;

            // seekTo
            if (audioWrapper->father->isPausedForSeek
                && timeStamp != -1) {
                seekToImpl();
                if (isReadFinished) {
                    readFrame = AVERROR_EOF;
                }
            }

            // 0 if OK, < 0 on error or end of file
            if (readFrame == 0) {
                readFrame = av_read_frame(avFormatContext, srcAVPacket);
            }
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
                LOGF("readData() audio list2: %d\n", audioWrapper->father->list2->size());

                // 读到文件末尾了
                audioWrapper->father->isReading = false;
                audioWrapper->father->isReadList1Full = true;
                // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                notifyToHandle(audioWrapper->father);
                notifyToHandle(audioWrapper->father);

                // 不退出线程
                LOGD("readData() notifyToReadWait start\n");
                notifyToReadWait(audioWrapper->father);
                LOGD("readData() notifyToReadWait end\n");
                if (audioWrapper->father->isPausedForSeek) {
                    LOGF("readData() start seek\n");
                    audioWrapper->father->isReading = true;
                    continue;
                } else {
                    // for (;;) end
                    break;
                }
            }// 文件已读完

            if (srcAVPacket->stream_index == audioWrapper->father->streamIndex) {
                if (audioWrapper->father->isReading) {
                    readDataImpl(audioWrapper->father, srcAVPacket, copyAVPacket);
                }
            }
        }// for(;;) end

        if (srcAVPacket != NULL) {
            av_packet_unref(srcAVPacket);
            srcAVPacket = NULL;
        }

        isReading = false;

        LOGD("%s\n", "readData() end");
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
            if (!audioWrapper->father->isStarted) {
                audioWrapper->father->isStarted = true;
                LOGW("handleAudioDataImpl() 音频已经准备好,开始播放!!!\n");
                // 回调(通知到java层)
                onPlayed();
            }

            audioTimeDifference =
                    decodedAVFrame->pts * av_q2d(stream->time_base);
            //LOGD("handleData() audio audioTimeDifference: %lf\n", audioTimeDifference);

            //int64_t relativeTime = av_gettime_relative();
            //LOGD("handleAudioDataImpl() audio relativeTime: %d\n", (int) relativeTime);

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
        } else {
            LOGE("handleAudioDataImpl 转换时出错 %d", ret);
        }

        return ret;
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

        }
        if (wrapper == NULL) {
            LOGE("%s\n", "wrapper is NULL");
            return NULL;
        }

        // 线程等待
        LOGD("handleData() wait() audio start\n");
        notifyToHandleWait(wrapper);
        LOGD("handleData() wait() audio end\n");

        AVStream *stream = avFormatContext->streams[wrapper->streamIndex];
        AVPacket *srcAVPacket = av_packet_alloc();
        AVPacket *copyAVPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = audioWrapper->decodedAVFrame;

        int ret = 0;
        bool allowDecode = false;
        bool isFinished = false;
        LOGD("handleData() start\n");
        for (;;) {
            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            // 暂停装置
            if (wrapper->isPausedForUser
                || wrapper->isPausedForSeek) {
                bool isPausedForSeek = wrapper->isPausedForSeek;
                if (isPausedForSeek) {
                    LOGD("handleData() wait() Seek  audio start\n");
                    wrapper->needToSeek = true;
                    wrapper->isReadList1Full = false;
                    wrapper->list1->clear();
                } else {
                    LOGD("handleData() wait() User  audio start\n");
                }
                notifyToHandleWait(wrapper);
                if (wrapper->isPausedForUser || wrapper->isPausedForSeek) {
                    continue;
                }
                if (isPausedForSeek) {
                    LOGD("handleData() wait() Seek  audio end\n");
                } else {
                    LOGD("handleData() wait() User  audio end\n");
                }
            }// 暂停装置 end

            allowDecode = false;
            if (wrapper->list1->size() > 0) {
                srcAVPacket = &wrapper->list1->front();
                // 内容copy
                av_packet_ref(copyAVPacket, srcAVPacket);
                av_packet_unref(srcAVPacket);
                wrapper->list1->pop_front();
                allowDecode = true;
            }

            size_t list2Size = wrapper->list2->size();
            if (wrapper->isReading) {
                if (wrapper->list1->size() == 0) {
                    wrapper->isReadList1Full = false;
                    if (list2Size > 0) {
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        wrapper->list2->clear();
                        wrapper->isReadList1Full = true;
                        pthread_mutex_unlock(&wrapper->readLockMutex);

                        notifyToRead(wrapper);
                    }
                }
            } else {
                if (wrapper->list1->size() > 0) {
                    // 还有数据,先用完再说
                } else {
                    if (list2Size > 0) {
                        // 把剩余的数据全部复制过来
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        wrapper->list1->clear();
                        wrapper->list1->assign(wrapper->list2->begin(), wrapper->list2->end());
                        if (!isReadFinished) {
                            wrapper->list2->clear();
                        }
                        pthread_mutex_unlock(&wrapper->readLockMutex);

                        LOGD("handleData() audio 最后要处理的数据还有 list1: %d\n",
                             wrapper->list1->size());
                    } else {
                        wrapper->isHandling = false;
                        isFinished = true;
                    }
                }
            }

            if (wrapper->isReading
                && wrapper->isHandling
                && !wrapper->isReadList1Full
                && wrapper->list2->size() == 0) {
                // 开始暂停
                onPaused();

                audioWrapper->father->isPausedForCache = true;
                LOGE("handleData() wait() Cache audio start 主动暂停\n");
                notifyToHandleWait(audioWrapper->father);
                if (wrapper->isPausedForSeek) {
                    audioWrapper->father->isPausedForCache = false;
                    continue;
                }
                LOGE("handleData() wait() Cache audio end   主动暂停\n");
                audioWrapper->father->isPausedForCache = false;

                // 开始播放
                onPlayed();
            }

            if (!wrapper->isHandling) {
                // for (;;) end
                break;
            }

            if (!allowDecode) {
                continue;
            }

            // 解码过程
            ret = avcodec_send_packet(wrapper->avCodecContext, copyAVPacket);
            av_packet_unref(copyAVPacket);
            switch (ret) {
                case AVERROR(EAGAIN):
                    LOGE("handleData() audio avcodec_send_packet   ret: %d\n", ret);
                    break;
                case AVERROR(EINVAL):
                case AVERROR(ENOMEM):
                case AVERROR_EOF:
                    LOGE("handleData() audio avcodec_send_packet 发送数据包到解码器时出错 %d", ret);
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
                    LOGE("handleData() audio avcodec_receive_frame ret: %d\n", ret);
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
                    LOGE("handleData() audio avcodec_receive_frame 从解码器接收帧时出错 %d", ret);
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

            // 播放声音
            handleAudioDataImpl(stream, decodedAVFrame);

            // 设置结束标志
            if (!wrapper->isReading
                && wrapper->list1->size() == 0
                && wrapper->list2->size() == 0) {
                wrapper->isHandling = false;
            }

            ///////////////////////////////////////////////////////////////////
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

        // 让"读线程"退出
        LOGW("%s\n", "handleData() notifyToRead");
        notifyToRead(audioWrapper->father);

        while (isReading) {
            av_usleep(1000);
        }
        closeAudio();
        if (isFinished) {
            onFinished();
        }

        LOGD("handleData() end\n");
        return NULL;
    }

    void closeAudio() {
        // audio
        if (audioWrapper == NULL
            || wrapper == NULL
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

        //av_free(audioWrapper->father->list1);
        //av_free(audioWrapper->father->list2);
        audioWrapper->father->list1->clear();
        audioWrapper->father->list2->clear();
        delete (audioWrapper->father->list1);
        delete (audioWrapper->father->list2);
        audioWrapper->father->list1 = NULL;
        audioWrapper->father->list2 = NULL;

        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
        av_free(wrapper);
        wrapper = NULL;
        audioWrapper->father = NULL;
        av_free(audioWrapper);
        audioWrapper = NULL;

        LOGD("%s\n", "closeAudio() end");
    }

    int initPlayer() {
        LOGW("%s\n", "initPlayer() start");

        onReady();

        initAV();
        initAudio();
        if (openAndFindAVFormatContext() < 0) {
            LOGE("openAndFindAVFormatContext() failed\n");
            closeAudio();
            return -1;
        }
        if (findStreamIndex() < 0) {
            LOGE("findStreamIndex() failed\n");
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
    }

    int play() {
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = false;
            notifyToHandle(audioWrapper->father);
        }
        return 0;
    }

    int pause() {
        LOGI("pause() start\n");
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioWrapper->father->isPausedForUser = true;
        }
        LOGI("pause() end\n");
        return 0;
    }

    int stop() {
        if (audioWrapper != NULL
            && wrapper != NULL
            && audioWrapper->father != NULL) {
            LOGI("stop() start\n");
            timeStamp = -1;
            audioWrapper->father->isStarted = false;
            audioWrapper->father->isReading = false;
            audioWrapper->father->isHandling = false;
            audioWrapper->father->isPausedForUser = false;
            audioWrapper->father->isPausedForCache = false;
            audioWrapper->father->isPausedForSeek = false;
            audioWrapper->father->isReadList1Full = false;
            notifyToRead(audioWrapper->father);
            notifyToHandle(audioWrapper->father);
            LOGI("stop() end\n");
        }

        return 0;
    }

    int release() {
        stop();
        return 0;
    }

    // 有没有在运行,即使暂停状态也是运行状态
    bool isRunning() {
        bool audioRunning = false;
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioRunning = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling;
        }
        return audioRunning;
    }

    // 有没有在播放,暂停状态不算播放状态
    bool isPlaying() {
        bool audioPlaying = false;
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioPlaying = audioWrapper->father->isStarted
                           && audioWrapper->father->isHandling
                           && !audioWrapper->father->isPausedForUser
                           && !audioWrapper->father->isPausedForCache
                           && !audioWrapper->father->isPausedForSeek;
        }
        return audioPlaying;
    }

    int seekTo(int64_t timestamp) {
        LOGI("==================================================================\n");
        LOGI("seekTo() timestamp: %ld\n", (long) timestamp);
        //LOGI("seekTo() timestamp: %" PRIu64 "\n", timestamp);

        if (((long) timestamp) < 0
            || audioWrapper == NULL
            || audioWrapper->father == NULL
            || !isRunning()) {
            return -1;
        }

        LOGD("seekTo() signal() to Read and Handle\n");
        timeStamp = timestamp;
        audioWrapper->father->isPausedForSeek = true;
        audioWrapper->father->needToSeek = false;
        notifyToHandle(audioWrapper->father);
        notifyToRead(audioWrapper->father);

        return 0;
    }

    // 返回值单位是秒
    long getDuration() {
        int64_t audioDuration = 0;
        if (audioWrapper != NULL && audioWrapper->father != NULL) {
            audioDuration = audioWrapper->father->duration;
        }

        //return (long) audioDuration * 1000 * 1000;
        return audioDuration;
    }

    void stepAdd() {

    }

    void stepSubtract() {

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

}
