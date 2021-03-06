//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_SIMPLEVIDEOPLAYER_H
#define USEFRAGMENTS_SIMPLEVIDEOPLAYER_H

#include "../ffmpeg/ffmpeg.h"
#include "MyHeader.h"
extern "C" {
#include <libavcodec/avcodec.h>
};

namespace alexander {

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

    long getDuration();

    void stepAdd();

    void stepSubtract();

    /*class SimpleVideoPlayer {

    private:
        // char *inVideoFilePath = "/storage/2430-1702/BaiduNetdisk/music/谭咏麟 - 水中花.mp3";
        char *inVideoFilePath = NULL;

        ANativeWindow *pANativeWindow = NULL;

    public:
    };*/

}


#endif //USEFRAGMENTS_SIMPLEVIDEOPLAYER_H
