//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_SIMPLEVIDEOPLAYER_H
#define USEFRAGMENTS_SIMPLEVIDEOPLAYER_H

#include "ffmpeg.h"
#include "MyHeader.h"

#define LOG "alexander"

namespace alexander {

#define USE_AUDIO
#define USE_VIDEO

    static struct AudioWrapper *audioWrapper = NULL;
    static struct VideoWrapper *videoWrapper = NULL;

    void *readData(void *opaque);

    void *handleAudioData(void *opaque);

    void *handleVideoData(void *opaque);

    void initAV();

    void initAudio();

    void initVideo();

    int getAVPacketFromQueue(struct AVPacketQueue *packet_queue, AVPacket *avpacket);

    int putAVPacketToQueue(struct AVPacketQueue *packet_queue, AVPacket *avpacket);

    int openAndFindAVFormatContextForAudio();

    int openAndFindAVFormatContextForVideo();

    int findStreamIndexForAudio();

    int findStreamIndexForVideo();

    int findAndOpenAVCodecForAudio();

    int findAndOpenAVCodecForVideo();

    int createSwrContent();

    int createSwsContext();

    void closeAudio();

    void closeVideo();

    int initAudioPlayer();

    int initVideoPlayer();

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

        ANativeWindow *nativeWindow = NULL;

    public:
    };*/

}


#endif //USEFRAGMENTS_SIMPLEVIDEOPLAYER_H
