//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_MEDIAPLAYERFOR4K_H
#define USEFRAGMENTS_MEDIAPLAYERFOR4K_H

#include "MediaPlayer.h"
#include "ffmpeg.h"
#include "../include/Log.h"

namespace alexander_media_4k {

    void *readData(void *opaque);

    void *handleData(void *opaque);

    void handleVideoRender();

    void initAV();

    void initAudio();

    void initVideo();

    int openAndFindAVFormatContext();

    int findStreamIndex();

    int findAndOpenAVCodecForAudio();

    int findAndOpenAVCodecForVideo();

    int createSwrContent();

    int createSwsContext();

    void closeAudio();

    void closeVideo();

    int initPlayer();

    void setJniParameters(JNIEnv *env, const char *filePath, jobject surfaceJavaObject);

    int play();

    int pause();

    int stop();

    int release();

    bool isRunning();

    bool isPlaying();

    bool isPausedForUser();

    int seekTo(int64_t timestamp);

    long getDuration();

    void stepAdd(int64_t addStep);

    void stepSubtract(int64_t subtractStep);

    int download(int flag, const char *filePath, const char *fileName);

}

#endif //USEFRAGMENTS_MEDIAPLAYERFOR4K_H
