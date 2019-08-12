//
// Created by root on 19-8-8.
//

#include "SimpleVideoPlayer.h"

namespace alexander {

    //char *inFilePath = "/storage/2430-1702/BaiduNetdisk/video/shape_of_my_heart.mp4";
    char *inFilePath = "http://xunlei.jingpin88.com/20171026/cQ7hsCrN/mp4/cQ7hsCrN.mp4";

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
        audioWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_AUDIO;

        audioWrapper->father->isReadQueue1Full = false;
        audioWrapper->father->isReadQueue2Full = false;
        audioWrapper->father->readFramesCount = 0;
        audioWrapper->father->handleFramesCount = 0;
        audioWrapper->father->isPausedForUser = false;
        audioWrapper->father->isPausedForCache = false;

        audioWrapper->father->queue1 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        audioWrapper->father->queue2 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        memset(audioWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
        memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
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
        videoWrapper->father->maxAVPacketsCount = MAX_AVPACKET_COUNT_VIDEO;

        videoWrapper->father->isReadQueue1Full = false;
        videoWrapper->father->isReadQueue2Full = false;
        videoWrapper->father->readFramesCount = 0;
        videoWrapper->father->handleFramesCount = 0;
        videoWrapper->father->isPausedForUser = false;
        videoWrapper->father->isPausedForCache = false;

        videoWrapper->father->queue1 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        videoWrapper->father->queue2 =
                (struct AVPacketQueue *) av_mallocz(sizeof(struct AVPacketQueue));
        memset(videoWrapper->father->queue1, 0, sizeof(struct AVPacketQueue));
        memset(videoWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
    }

    int audioDecodeFrame() {
        int ret, get_nb_samples_per_channel;
        int got_frame_ptr = 0;
        int64_t get_ch_layout_from_decoded_avframe;
        // 压缩数据
        AVPacket *avPacket = av_packet_alloc();
        unsigned char *audio_pkt_data = NULL;
        int audio_pkt_size = 0;
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = audioWrapper->father->srcAVFrame;

        for (;;) {
            while (audio_pkt_size > 0) {
                av_frame_unref(decodedAVFrame);
                /***
                 当AVPacket中装得是音频时,有可能一个AVPacket中有多个AVFrame,
                 而某些解码器只会解出第一个AVFrame,这种情况我们必须循环解码出后续AVFrame.
                 */
                ret = avcodec_decode_audio4(audioWrapper->father->avCodecContext,
                                            decodedAVFrame,
                                            &got_frame_ptr,
                                            avPacket);
                if (ret < 0) {
                    LOGI("ret = %d\n", ret);
                    // error, skip the frame
                    audio_pkt_size = 0;
                    break;
                }

                audio_pkt_data += ret;
                audio_pkt_size -= ret;

                if (!got_frame_ptr) {
                    //LOGI("got_frame_ptr = %d\n", got_frame_ptr);
                    continue;
                }

                // 执行到这里我们得到了一个AVFrame
                // 得到这个AVFrame的声音布局,比如立体声
                get_ch_layout_from_decoded_avframe =
                        (decodedAVFrame->channel_layout != 0
                         && decodedAVFrame->channels ==
                            av_get_channel_layout_nb_channels(decodedAVFrame->channel_layout))
                        ?
                        decodedAVFrame->channel_layout
                        :
                        av_get_default_channel_layout(decodedAVFrame->channels);

                if (audioWrapper->srcSampleRate != decodedAVFrame->sample_rate) {
                    LOGI("SampleRate变了 srcSampleRate: %d now: %d\n",
                         audioWrapper->srcSampleRate, decodedAVFrame->sample_rate);
                }
                if (audioWrapper->srcAVSampleFormat != decodedAVFrame->format) {
                    LOGI("AVSampleFormat变了 srcAVSampleFormat: %d now: %d\n",
                         audioWrapper->srcAVSampleFormat, decodedAVFrame->format);
                }
                if (audioWrapper->srcChannelLayout != get_ch_layout_from_decoded_avframe) {
                    LOGI("ChannelLayout变了 srcChannelLayout: %d now: %d\n",
                         audioWrapper->srcChannelLayout, get_ch_layout_from_decoded_avframe);
                }

                /***
                 接下来判断我们之前设置SDL时设置的声音格式(AV_SAMPLE_FMT_S16),声道布局,
                 采样频率,每个AVFrame的每个声道采样数与
                 得到的该AVFrame分别是否相同,如有任意不同,我们就需要swr_convert该AVFrame,
                 然后才能符合之前设置好的SDL的需要,才能播放.
                 */
                if (audioWrapper->srcSampleRate != decodedAVFrame->sample_rate
                    || audioWrapper->srcAVSampleFormat != decodedAVFrame->format
                    || audioWrapper->srcChannelLayout != get_ch_layout_from_decoded_avframe) {
                    LOGI("---------------------------------\n");
                    LOGI("nowSampleRate       : %d\n", decodedAVFrame->sample_rate);
                    LOGI("nowAVSampleFormat   : %d\n", decodedAVFrame->format);
                    LOGI("nowChannelLayout    : %d\n", get_ch_layout_from_decoded_avframe);
                    LOGI("nowNbChannels       : %d\n", decodedAVFrame->channels);
                    LOGI("nowNbSamples        : %d\n", decodedAVFrame->nb_samples);
                    LOGI("---------------------------------\n");

                    if (audioWrapper->swrContext) {
                        swr_free(&audioWrapper->swrContext);
                    }
                    LOGI("audio_state->audioSwrContext swr_alloc_set_opts.\n");
                    audioWrapper->swrContext = swr_alloc();
                    swr_alloc_set_opts(audioWrapper->swrContext,
                                       audioWrapper->dstChannelLayout,
                                       audioWrapper->dstAVSampleFormat,
                                       audioWrapper->dstSampleRate,
                                       get_ch_layout_from_decoded_avframe,
                                       (enum AVSampleFormat) decodedAVFrame->format,
                                       decodedAVFrame->sample_rate,
                                       0, NULL);
                    if (!audioWrapper->swrContext || swr_init(audioWrapper->swrContext) < 0) {
                        LOGI("swr_init() failed\n");
                        break;
                    } else {
                        LOGI("audio_state->audioSwrContext is created.\n");
                    }

                    audioWrapper->srcSampleRate = decodedAVFrame->sample_rate;
                    audioWrapper->srcNbChannels = decodedAVFrame->channels;
                    audioWrapper->srcAVSampleFormat = (enum AVSampleFormat) decodedAVFrame->format;
                    audioWrapper->srcNbSamples = decodedAVFrame->nb_samples;
                    audioWrapper->srcChannelLayout = get_ch_layout_from_decoded_avframe;
                }

                /***
                 转换该AVFrame到设置好的SDL需要的样子,有些旧的代码示例最主要就是少了这一部分,
                 往往一些音频能播,一些不能播,这就是原因,比如有些源文件音频恰巧是AV_SAMPLE_FMT_S16的.
                 swr_convert 返回的是转换后每个声道(channel)的采样数
                 */
                unsigned char *out[] = {audioWrapper->playBuffer};
                int out_count = sizeof(audioWrapper->playBuffer)
                                / audioWrapper->dstNbChannels
                                / av_get_bytes_per_sample(audioWrapper->dstAVSampleFormat);
                const unsigned char **in = (const unsigned char **) decodedAVFrame->extended_data;
                int in_count = decodedAVFrame->nb_samples;
                // 转换后的数据存在audioWrapper->outBuffer中,也就是要播放的数据
                // 大小为decodedAVFrame->nb_samples
                get_nb_samples_per_channel = swr_convert(audioWrapper->swrContext,
                                                         out,
                                                         out_count,
                                                         in,
                                                         in_count);
                if (get_nb_samples_per_channel < 0) {
                    LOGI("swr_convert() failed\n");
                    break;
                }

                // 声道数 x 每个声道采样数 x 每个样本字节数
                // We have data, return it and come back for more later
                return audioWrapper->dstNbChannels
                       * get_nb_samples_per_channel
                       * av_get_bytes_per_sample(audioWrapper->dstAVSampleFormat);
            }//while end

            if (avPacket->data) {
                av_free_packet(avPacket);
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
                    audioWrapper->father->isReadQueue2Full = true;
                    audioWrapper->father->next = NEXT_QUEUE2;
                    LOGI("audioDecodeFrame() audio Queue1用完了\n");
                    LOGI("audioDecodeFrame() audio pthread_cond_signal() readLockCondition\n");
                    pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                    pthread_cond_signal(&audioWrapper->father->readLockCondition);
                    pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                }
            } else if (audioWrapper->father->next == NEXT_QUEUE2
                       && audioWrapper->father->isReadQueue2Full
                       && audioWrapper->father->queue2->allAVPacketsCount > 0) {
                audioWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(audioWrapper->father->queue2, avPacket);
                if (audioWrapper->father->queue2->allAVPacketsCount == 0) {
                    memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
                    audioWrapper->father->isReadQueue1Full = true;
                    audioWrapper->father->isReadQueue2Full = false;
                    audioWrapper->father->next = NEXT_QUEUE1;
                    LOGI("audioDecodeFrame() audio Queue2用完了\n");
                    LOGI("audioDecodeFrame() audio pthread_cond_signal() readLockCondition\n");
                    pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                    pthread_cond_signal(&audioWrapper->father->readLockCondition);
                    pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                }
            } else if (!audioWrapper->father->isReadQueue1Full
                       && !audioWrapper->father->isReadQueue2Full) {
                // cache引起的暂停
                audioWrapper->father->isPausedForCache = true;
                LOGI("audioDecodeFrame() pthread_cond_wait() handleLockConditionForCache start\n");
                pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                                  &audioWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                LOGI("audioDecodeFrame() pthread_cond_wait() handleLockConditionForCache end\n");
                audioWrapper->father->isPausedForCache = false;
                continue;
            } else if (audioWrapper->father->queue1->allAVPacketsCount == 0
                       && audioWrapper->father->queue2->allAVPacketsCount == 0) {
                audioWrapper->father->isHandling = false;
                break;
            }

            audio_pkt_data = avPacket->data;
            audio_pkt_size = avPacket->size;
        }//for end
    }

    int openAndFindAVFormatContextForAudio() {
        // AVFormatContext初始化,里面设置结构体的一些默认信息
        // 相当于Java中创建对象
        audioWrapper->father->avFormatContext = avformat_alloc_context();
        if (audioWrapper->father->avFormatContext == NULL) {
            LOGI("audioWrapper->father->avFormatContext is NULL.\n");
            return -1;
        }
        // 获取基本的文件信息
        if (avformat_open_input(&audioWrapper->father->avFormatContext,
                                inFilePath, NULL, NULL) != 0) {
            LOGI("Couldn't open audio input stream.\n");
            return -1;
        }
        // 文件中的流信息
        if (avformat_find_stream_info(audioWrapper->father->avFormatContext, NULL) != 0) {
            LOGI("Couldn't find stream information.\n");
            return -1;
        }

        return 0;
    }

    int openAndFindAVFormatContextForVideo() {
        videoWrapper->father->avFormatContext = avformat_alloc_context();
        if (videoWrapper->father->avFormatContext == NULL) {
            LOGI("videoWrapper->father->avFormatContext is NULL.\n");
            return -1;
        }
        if (avformat_open_input(&videoWrapper->father->avFormatContext,
                                inFilePath, NULL, NULL) != 0) {
            LOGI("Couldn't open video input stream.\n");
            return -1;
        }
        if (avformat_find_stream_info(videoWrapper->father->avFormatContext, NULL) != 0) {
            LOGI("Couldn't find stream information.\n");
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
            LOGI("Didn't find audio stream.\n");
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
            LOGI("Didn't find video stream.\n");
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
                            LOGI("Could not open audio codec.\n");
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
                            LOGI("Could not open video codec.\n");
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
        LOGI("srcNbSamples        : %d\n", audioWrapper->srcNbSamples);
        LOGI("srcNbChannels       : %d\n", audioWrapper->srcNbChannels);
        LOGI("srcAVSampleFormat   : %d\n", audioWrapper->srcAVSampleFormat);
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
        audioWrapper->dstSampleRate = audioWrapper->srcSampleRate;
        audioWrapper->dstNbSamples = audioWrapper->srcNbSamples;
        audioWrapper->dstNbChannels = audioWrapper->srcNbChannels;
        if (!audioWrapper->dstChannelLayout
            || audioWrapper->dstChannelLayout !=
               av_get_channel_layout_nb_channels(audioWrapper->dstChannelLayout)) {
            audioWrapper->dstChannelLayout = av_get_default_channel_layout(
                    audioWrapper->dstNbChannels);
            LOGI("dstChannelLayout1   : %d\n", audioWrapper->dstChannelLayout);
            // why?
            audioWrapper->dstChannelLayout &= ~AV_CH_LAYOUT_STEREO_DOWNMIX;
            LOGI("dstChannelLayout2   : %d\n", audioWrapper->dstChannelLayout);
        }
        audioWrapper->dstNbChannels = av_get_channel_layout_nb_channels(
                audioWrapper->dstChannelLayout);

        LOGI("dstSampleRate       : %d\n", audioWrapper->dstSampleRate);
        LOGI("dstNbSamples        : %d\n", audioWrapper->dstNbSamples);
        LOGI("dstNbChannels       : %d\n", audioWrapper->dstNbChannels);
        LOGI("dstAVSampleFormat   : %d\n", audioWrapper->dstAVSampleFormat);
        LOGI("---------------------------------\n");

        // avPacket ---> srcAVFrame ---> dstAVFrame ---> 播放声音
        audioWrapper->father->srcAVFrame = av_frame_alloc();
        // audioWrapper->father->dstAVFrame = av_frame_alloc();

        /***
         struct SwrContext *s,
         int64_t out_ch_layout, enum AVSampleFormat out_sample_fmt, int out_sample_rate,
         int64_t  in_ch_layout, enum AVSampleFormat  in_sample_fmt, int  in_sample_rate,
         int log_offset, void *log_ctx
         */
        audioWrapper->swrContext = swr_alloc();
        //audioWrapper->swrContext =
        swr_alloc_set_opts(audioWrapper->swrContext,
                           audioWrapper->dstChannelLayout,  // out_ch_layout
                           audioWrapper->dstAVSampleFormat, // out_sample_fmt
                           audioWrapper->dstSampleRate,     // out_sample_rate
                           audioWrapper->srcChannelLayout,  // in_ch_layout
                           audioWrapper->srcAVSampleFormat, // in_sample_fmt
                           audioWrapper->srcSampleRate,     // in_sample_rate
                           0,                              // log_offset
                           NULL);                          // log_ctx
        if (audioWrapper->swrContext == NULL) {
            LOGI("%s\n", "audioSwrContext is NULL.");
            return -1;
        }

        int ret = swr_init(audioWrapper->swrContext);
        if (ret != 0) {
            LOGI("%s\n", "audioSwrContext swr_init failed.");
        } else {
            LOGI("%s\n", "audioSwrContext swr_init success.");
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
        LOGI("srcAVPixelFormat    : %d\n", videoWrapper->srcAVPixelFormat);
        // 播放视频时两个必须相同
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
        packet_queue->allAVPacketsSize += avpacket_list->pkt.size;

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
            packet_queue->allAVPacketsSize -= avpacket_list->pkt.size;

            av_free(avpacket_list);
            return 0;
        }

        return -1;
    }

    void *readData(void *opaque) {
        if (opaque == NULL) {
            return NULL;
        }
        Wrapper *wrapper = (Wrapper *) opaque;
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
                    if (wrapper->type == TYPE_AUDIO) {
                        LOGI("readData() audio readFrame: %d\n", readFrame);
                    } else {
                        LOGI("readData() video readFrame: %d\n", readFrame);
                    }
                    if (wrapper->queue1->allAVPacketsCount > 0) {
                        if (!wrapper->isReadQueue1Full
                            && !wrapper->isReadQueue2Full) {
                            wrapper->isReadQueue1Full = true;
                        }
                    }
                    if (wrapper->queue2->allAVPacketsCount > 0) {
                        if (!wrapper->isReadQueue2Full) {
                            wrapper->isReadQueue2Full = true;
                        }
                    }

                    // 说明歌曲长度比较短,达不到"规定值",因此处理数据线程还在等待
                    if (wrapper->isReadQueue1Full
                        && !wrapper->isReadQueue2Full
                        && wrapper->queue1->allAVPacketsCount > 0
                        && wrapper->queue2->allAVPacketsCount == 0) {
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGI("readData() audio pthread_cond_signal() handleLockCondition break\n");
                        } else {
                            LOGI("readData() video pthread_cond_signal() handleLockCondition break\n");
                        }
                        // 唤醒线程
                        pthread_mutex_lock(&wrapper->handleLockMutex);
                        pthread_cond_signal(&wrapper->handleLockCondition);
                        pthread_mutex_unlock(&wrapper->handleLockMutex);
                    }

                    wrapper->isReading = false;
                    break;
                }

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
                                LOGI("readData() audio pthread_cond_signal() handleLockCondition\n");
                            } else {
                                LOGI("readData() video Queue1満了\n");
                                LOGI("readData() video allAVPacketsSize : %ld\n",
                                     wrapper->queue1->allAVPacketsSize);
                                LOGI("readData() video pthread_cond_signal() handleLockCondition\n");
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
                                LOGI("readData() audio pthread_cond_signal() handleLockCondition\n");
                            } else {
                                LOGI("readData() video Queue2満了\n");
                                LOGI("readData() video allAVPacketsSize : %ld\n",
                                     wrapper->queue2->allAVPacketsSize);
                                LOGI("readData() video pthread_cond_signal() handleLockCondition\n");
                            }
                            pthread_mutex_lock(&wrapper->handleLockMutex);
                            pthread_cond_signal(&wrapper->handleLockCondition);
                            pthread_mutex_unlock(&wrapper->handleLockMutex);
                        }
                    } else if (wrapper->isReadQueue1Full && wrapper->isReadQueue2Full) {
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGI("readData() audio Queue1和Queue2都満了,好开心( ^_^ )\n");
                            LOGI("readData() audio pthread_cond_wait() readLockCondition start\n");
                        } else {
                            LOGI("readData() video Queue1和Queue2都満了,好开心( ^_^ )\n");
                            LOGI("readData() video pthread_cond_wait() readLockCondition start\n");
                        }
                        pthread_mutex_lock(&wrapper->readLockMutex);
                        pthread_cond_wait(&wrapper->readLockCondition, &wrapper->readLockMutex);
                        pthread_mutex_unlock(&wrapper->readLockMutex);
                        if (wrapper->type == TYPE_AUDIO) {
                            LOGI("readData() audio pthread_cond_wait() readLockCondition end\n");
                        } else {
                            LOGI("readData() video pthread_cond_wait() readLockCondition end\n");
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

    void *handleAudioData(void *opaque) {
        LOGI("%s\n", "handleAudioData() start");

        int ret, get_nb_samples_per_channel;
        int got_frame_ptr = 0;
        int64_t get_ch_layout_from_decoded_avframe;

        // 压缩数据
        AVPacket *avPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = audioWrapper->father->srcAVFrame;
        audioWrapper->father->isHandling = true;
        for (;;) {
            if (!audioWrapper->father->isHandling) {
                break;
            }

            if (avPacket->data) {
                av_packet_unref(avPacket);
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
                    audioWrapper->father->isReadQueue2Full = true;
                    audioWrapper->father->next = NEXT_QUEUE2;
                    LOGI("handleVideoData() video Queue1用完了\n");
                    LOGI("handleVideoData() video pthread_cond_signal() readLockCondition\n");
                    pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                    pthread_cond_signal(&audioWrapper->father->readLockCondition);
                    pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                }
            } else if (audioWrapper->father->next == NEXT_QUEUE2
                       && audioWrapper->father->isReadQueue2Full
                       && audioWrapper->father->queue2->allAVPacketsCount > 0) {
                audioWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(audioWrapper->father->queue2, avPacket);
                if (audioWrapper->father->queue2->allAVPacketsCount == 0) {
                    memset(audioWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
                    audioWrapper->father->isReadQueue1Full = true;
                    audioWrapper->father->isReadQueue2Full = false;
                    audioWrapper->father->next = NEXT_QUEUE1;
                    LOGI("handleVideoData() video Queue2用完了\n");
                    LOGI("handleVideoData() video pthread_cond_signal() readLockCondition\n");
                    pthread_mutex_lock(&audioWrapper->father->readLockMutex);
                    pthread_cond_signal(&audioWrapper->father->readLockCondition);
                    pthread_mutex_unlock(&audioWrapper->father->readLockMutex);
                }
            } else if (!audioWrapper->father->isReadQueue1Full
                       && !audioWrapper->father->isReadQueue2Full) {
                // cache引起的暂停
                audioWrapper->father->isPausedForCache = true;
                LOGI("handleVideoData() pthread_cond_wait() handleLockConditionForCache start\n");
                pthread_mutex_lock(&audioWrapper->father->handleLockMutex);
                pthread_cond_wait(&audioWrapper->father->handleLockCondition,
                                  &audioWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&audioWrapper->father->handleLockMutex);
                LOGI("handleVideoData() pthread_cond_wait() handleLockConditionForCache end\n");
                audioWrapper->father->isPausedForCache = false;
                continue;
            } else if (audioWrapper->father->queue1->allAVPacketsCount == 0
                       && audioWrapper->father->queue2->allAVPacketsCount == 0) {
                audioWrapper->father->isHandling = false;
                break;
            }

            while (1) {
                av_frame_unref(decodedAVFrame);
                /***
                 当AVPacket中装得是音频时,有可能一个AVPacket中有多个AVFrame,
                 而某些解码器只会解出第一个AVFrame,这种情况我们必须循环解码出后续AVFrame.
                 */
                ret = avcodec_decode_audio4(audioWrapper->father->avCodecContext,
                                            decodedAVFrame,
                                            &got_frame_ptr,
                                            avPacket);
                if (ret < 0) {
                    LOGI("ret = %d\n", ret);
                    // error, skip the frame
                    break;
                }

                if (!got_frame_ptr) {
                    continue;
                }

                // 执行到这里我们得到了一个AVFrame
                // 得到这个AVFrame的声音布局,比如立体声
                get_ch_layout_from_decoded_avframe =
                        (decodedAVFrame->channel_layout != 0
                         && decodedAVFrame->channels ==
                            av_get_channel_layout_nb_channels(decodedAVFrame->channel_layout))
                        ?
                        decodedAVFrame->channel_layout
                        :
                        av_get_default_channel_layout(decodedAVFrame->channels);

                if (audioWrapper->srcSampleRate != decodedAVFrame->sample_rate) {
                    LOGI("SampleRate变了 srcSampleRate: %d now: %d\n",
                         audioWrapper->srcSampleRate, decodedAVFrame->sample_rate);
                }
                if (audioWrapper->srcAVSampleFormat != decodedAVFrame->format) {
                    LOGI("AVSampleFormat变了 srcAVSampleFormat: %d now: %d\n",
                         audioWrapper->srcAVSampleFormat, decodedAVFrame->format);
                }
                if (audioWrapper->srcChannelLayout != get_ch_layout_from_decoded_avframe) {
                    LOGI("ChannelLayout变了 srcChannelLayout: %d now: %d\n",
                         audioWrapper->srcChannelLayout, get_ch_layout_from_decoded_avframe);
                }

                /***
                 接下来判断我们之前设置SDL时设置的声音格式(AV_SAMPLE_FMT_S16),声道布局,
                 采样频率,每个AVFrame的每个声道采样数与
                 得到的该AVFrame分别是否相同,如有任意不同,我们就需要swr_convert该AVFrame,
                 然后才能符合之前设置好的SDL的需要,才能播放.
                 */
                if (audioWrapper->srcSampleRate != decodedAVFrame->sample_rate
                    || audioWrapper->srcAVSampleFormat != decodedAVFrame->format
                    || audioWrapper->srcChannelLayout != get_ch_layout_from_decoded_avframe) {
                    LOGI("---------------------------------\n");
                    LOGI("nowSampleRate       : %d\n", decodedAVFrame->sample_rate);
                    LOGI("nowAVSampleFormat   : %d\n", decodedAVFrame->format);
                    LOGI("nowChannelLayout    : %d\n", get_ch_layout_from_decoded_avframe);
                    LOGI("nowNbChannels       : %d\n", decodedAVFrame->channels);
                    LOGI("nowNbSamples        : %d\n", decodedAVFrame->nb_samples);
                    LOGI("---------------------------------\n");

                    if (audioWrapper->swrContext) {
                        swr_free(&audioWrapper->swrContext);
                    }
                    LOGI("audio_state->audioSwrContext swr_alloc_set_opts.\n");
                    audioWrapper->swrContext = swr_alloc();
                    swr_alloc_set_opts(audioWrapper->swrContext,
                                       audioWrapper->dstChannelLayout,
                                       audioWrapper->dstAVSampleFormat,
                                       audioWrapper->dstSampleRate,
                                       get_ch_layout_from_decoded_avframe,
                                       (enum AVSampleFormat) decodedAVFrame->format,
                                       decodedAVFrame->sample_rate,
                                       0, NULL);
                    if (!audioWrapper->swrContext || swr_init(audioWrapper->swrContext) < 0) {
                        LOGI("swr_init() failed\n");
                        break;
                    } else {
                        LOGI("audio_state->audioSwrContext is created.\n");
                    }

                    audioWrapper->srcSampleRate = decodedAVFrame->sample_rate;
                    audioWrapper->srcNbChannels = decodedAVFrame->channels;
                    audioWrapper->srcAVSampleFormat = (enum AVSampleFormat) decodedAVFrame->format;
                    audioWrapper->srcNbSamples = decodedAVFrame->nb_samples;
                    audioWrapper->srcChannelLayout = get_ch_layout_from_decoded_avframe;
                }

                /***
                 转换该AVFrame到设置好的SDL需要的样子,有些旧的代码示例最主要就是少了这一部分,
                 往往一些音频能播,一些不能播,这就是原因,比如有些源文件音频恰巧是AV_SAMPLE_FMT_S16的.
                 swr_convert 返回的是转换后每个声道(channel)的采样数
                 */
                unsigned char *out[] = {audioWrapper->playBuffer};
                int out_count = sizeof(audioWrapper->playBuffer)
                                / audioWrapper->dstNbChannels
                                / av_get_bytes_per_sample(audioWrapper->dstAVSampleFormat);
                const unsigned char **in = (const unsigned char **) decodedAVFrame->extended_data;
                int in_count = decodedAVFrame->nb_samples;
                // 转换后的数据存在audioWrapper->outBuffer中,也就是要播放的数据
                // 大小为decodedAVFrame->nb_samples
                get_nb_samples_per_channel = swr_convert(audioWrapper->swrContext,
                                                         out,
                                                         out_count,
                                                         in,
                                                         in_count);
                if (get_nb_samples_per_channel < 0) {
                    LOGI("swr_convert() failed\n");
                    break;
                }

                // 声道数 x 每个声道采样数 x 每个样本字节数
                // We have data, return it and come back for more later
                /*return audioWrapper->dstNbChannels
                       * get_nb_samples_per_channel
                       * av_get_bytes_per_sample(audioWrapper->dstAVSampleFormat);*/

                // Test
                usleep(10);

                break;
            }//while(1) end
        }//for(;;) end

        LOGI("%s\n", "handleAudioData() end");
        return NULL;
    }

    void *handleVideoData(void *opaque) {
        LOGI("%s\n", "handleVideoData() start");

        // 线程等待
        LOGI("handleVideoData() pthread_cond_wait() start\n");
        pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
        pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                          &videoWrapper->father->handleLockMutex);
        pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
        LOGI("handleVideoData() pthread_cond_wait() end\n");

        int64_t prePts = 0;
        int64_t nowPts = 0;
        int sleep = 0;
        int ret = 0, got_frame_ptr = 0;

        // 2.设置缓冲区的属性（宽、高、像素格式）,像素格式要和SurfaceView的像素格式一直
        ANativeWindow_setBuffersGeometry(nativeWindow,
                                         videoWrapper->srcWidth, videoWrapper->srcHeight,
                                         WINDOW_FORMAT_RGBA_8888);
        // 绘制时的缓冲区
        ANativeWindow_Buffer outBuffer;

        videoWrapper->father->next = NEXT_QUEUE1;
        AVStream *stream =
                videoWrapper->father->avFormatContext->streams[videoWrapper->father->streamIndex];
        // 必须创建
        AVPacket *avPacket = av_packet_alloc();
        // decodedAVFrame为解码后的数据
        AVFrame *decodedAVFrame = videoWrapper->father->srcAVFrame;
        AVFrame *rgbAVFrame = av_frame_alloc();
        videoWrapper->father->isHandling = true;
        for (;;) {
            if (!videoWrapper->father->isHandling) {
                break;
            }

            if (videoWrapper->father->isPausedForUser) {
                LOGI("handleVideoData() pthread_cond_wait() handleLockConditionForUser start\n");
                pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                                  &videoWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                LOGI("handleVideoData() pthread_cond_wait() handleLockConditionForUser end\n");
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
                    videoWrapper->father->isReadQueue2Full = true;
                    videoWrapper->father->next = NEXT_QUEUE2;
                    LOGI("handleVideoData() video Queue1用完了\n");
                    LOGI("handleVideoData() video pthread_cond_signal() readLockCondition\n");
                    pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                    pthread_cond_signal(&videoWrapper->father->readLockCondition);
                    pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
                }
            } else if (videoWrapper->father->next == NEXT_QUEUE2
                       && videoWrapper->father->isReadQueue2Full
                       && videoWrapper->father->queue2->allAVPacketsCount > 0) {
                videoWrapper->father->handleFramesCount++;
                getAVPacketFromQueue(videoWrapper->father->queue2, avPacket);
                if (videoWrapper->father->queue2->allAVPacketsCount == 0) {
                    memset(videoWrapper->father->queue2, 0, sizeof(struct AVPacketQueue));
                    videoWrapper->father->isReadQueue1Full = true;
                    videoWrapper->father->isReadQueue2Full = false;
                    videoWrapper->father->next = NEXT_QUEUE1;
                    LOGI("handleVideoData() video Queue2用完了\n");
                    LOGI("handleVideoData() video pthread_cond_signal() readLockCondition\n");
                    pthread_mutex_lock(&videoWrapper->father->readLockMutex);
                    pthread_cond_signal(&videoWrapper->father->readLockCondition);
                    pthread_mutex_unlock(&videoWrapper->father->readLockMutex);
                }
            } else if (!videoWrapper->father->isReadQueue1Full
                       && !videoWrapper->father->isReadQueue2Full) {
                // cache引起的暂停
                videoWrapper->father->isPausedForCache = true;
                LOGI("handleVideoData() pthread_cond_wait() handleLockConditionForCache start\n");
                pthread_mutex_lock(&videoWrapper->father->handleLockMutex);
                pthread_cond_wait(&videoWrapper->father->handleLockCondition,
                                  &videoWrapper->father->handleLockMutex);
                pthread_mutex_unlock(&videoWrapper->father->handleLockMutex);
                LOGI("handleVideoData() pthread_cond_wait() handleLockConditionForCache end\n");
                videoWrapper->father->isPausedForCache = false;
                continue;
            } else if (videoWrapper->father->queue1->allAVPacketsCount == 0
                       && videoWrapper->father->queue2->allAVPacketsCount == 0) {
                videoWrapper->father->isHandling = false;
                LOGI("handleVideoData() 电影结束,散场\n");
                break;
            }

            if (!avPacket) {
                if (videoWrapper->father->queue1->allAVPacketsCount == 0
                    && videoWrapper->father->queue2->allAVPacketsCount == 0) {
                    break;
                }
                continue;
            }

            ret = avcodec_send_packet(videoWrapper->father->avCodecContext, avPacket);
            if (ret < 0) {
                LOGE("avcodec_send_packet: %d\n", ret);
                continue;
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
                    nowPts = decodedAVFrame->pts;
                    double timeDifference = (nowPts - prePts) * av_q2d(stream->time_base);
                    sleep = timeDifference * 1000000;
                    prePts = nowPts;

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
                    // 6.unlock绘制
                    ANativeWindow_unlockAndPost(nativeWindow);

                    if (sleep < 1000000) {
                        usleep(sleep);
                    } else {
                        LOGI("handleVideoData() sleep : 40 * 1000\n");
                        usleep(40 * 1000);
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

        // 7.释放资源
        ANativeWindow_release(nativeWindow);

        av_packet_unref(avPacket);
        avPacket = NULL;

        av_frame_free(&rgbAVFrame);
        rgbAVFrame = NULL;

        LOGI("handleVideoData() video handleFramesCount : %d\n",
             videoWrapper->father->handleFramesCount);

        LOGI("%s\n", "handleVideoData() end");
        return NULL;
    }

    void closeAudio() {
        LOGI("%s\n", "closeAudio() start");
        // audio
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
        LOGI("%s\n", "closeVideo() start");
        // video
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

    int pause() {
        videoWrapper->father->isPausedForUser = true;
    }

    /***
    
     */
    int alexanderAudioPlayer() {
        LOGI("%s\n", "alexanderAudioPlayer() start");

        /*initAudio();

        if (openAndFindAVFormatContextForAudio() < 0) {
            return -1;
        }
        if (findStreamIndexForAudio() < 0) {
            return -1;
        }
        if (findAndOpenAVCodecForAudio() < 0) {
            return -1;
        }
        if (createSwrContent() < 0) {
            LOGI("%s\n", "");
            return -1;
        }

        pthread_t readDataThread;
        pthread_t handleDataThread;
        // 创建线程
        pthread_create(&readDataThread, NULL, readData, audioWrapper->father);
        pthread_create(&handleDataThread, NULL, handleAudioData, NULL);
        // 等待线程执行完
        pthread_join(readDataThread, NULL);
        pthread_join(handleDataThread, NULL);
        // 取消线程
        *//*pthread_cancel(readDataThread);
        pthread_cancel(handleDataThread);*//*

        closeAudio();*/

        LOGI("%s\n", "alexanderAudioPlayer() end");
    }

//#define USE_AUDIO
#define USE_VIDEO

    /***
     run this method and playback video
     */
    int alexanderVideoPlayer() {
        LOGI("%s\n", "alexanderVideoPlayer() start");

#ifdef USE_AUDIO
        // audio
        initAudio();
        if (openAndFindAVFormatContextForAudio() < 0) {
            LOGE("openAndFindAVFormatContextForAudio() failed\n");
            return -1;
        }
        if (findStreamIndexForAudio() < 0) {
            LOGE("findStreamIndexForAudio() failed\n");
            return -1;
        }
        if (findAndOpenAVCodecForAudio() < 0) {
            LOGE("findAndOpenAVCodecForAudio() failed\n");
            return -1;
        }
        if (createSwrContent() < 0) {
            LOGE("createSwrContent() failed\n");
            return -1;
        }
#endif
#ifdef USE_VIDEO
        // video
        initVideo();
        if (openAndFindAVFormatContextForVideo() < 0) {
            LOGE("openAndFindAVFormatContextForVideo() failed\n");
            return -1;
        }
        if (findStreamIndexForVideo() < 0) {
            LOGE("findStreamIndexForVideo() failed\n");
            return -1;
        }
        if (findAndOpenAVCodecForVideo() < 0) {
            LOGE("findAndOpenAVCodecForVideo() failed\n");
            return -1;
        }
        if (createSwsContext() < 0) {
            LOGE("createSwsContext() failed\n");
            return -1;
        }
#endif

#ifdef USE_AUDIO
        pthread_t audioReadDataThread, audioHandleDataThread;
#endif
#ifdef USE_VIDEO
        pthread_t videoReadDataThread, videoHandleDataThread;
#endif

#ifdef USE_AUDIO
        // 创建线程
        pthread_create(&audioReadDataThread, NULL, readData, audioWrapper->father);
        pthread_create(&audioHandleDataThread, NULL, handleAudioData, NULL);
#endif
#ifdef USE_VIDEO
        // 创建线程
        pthread_create(&videoReadDataThread, NULL, readData, videoWrapper->father);
        pthread_create(&videoHandleDataThread, NULL, handleVideoData, NULL);
#endif

#ifdef USE_AUDIO
        // 等待线程执行完
        pthread_join(audioReadDataThread, NULL);
        pthread_join(audioHandleDataThread, NULL);
#endif
#ifdef USE_VIDEO
        // 等待线程执行完
        pthread_join(videoReadDataThread, NULL);
        pthread_join(videoHandleDataThread, NULL);
#endif

#ifdef USE_AUDIO
        // 取消线程
        /*pthread_cancel(audioReadDataThread);
        pthread_cancel(audioHandleDataThread);*/
        closeAudio();
#endif
#ifdef USE_VIDEO
        // 取消线程
        /*pthread_cancel(videoReadDataThread);
        pthread_cancel(videoHandleDataThread);*/
        closeVideo();
#endif

        LOGI("%s\n", "alexanderVideoPlayer() end");
        return 0;
    }

    void setSurface(JNIEnv *env, jobject surface) {
        // 1.获取一个关联Surface的NativeWindow窗体
        nativeWindow = ANativeWindow_fromSurface(env, surface);
        if (nativeWindow == NULL) {
            LOGI("setSurface() is NULL\n");
        } else {
            LOGI("setSurface() isn't NULL\n");
        }
    }


}