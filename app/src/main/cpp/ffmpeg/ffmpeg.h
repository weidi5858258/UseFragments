//
// Created by root on 19-8-13.
//

#ifndef USEFRAGMENTS_FFMPEG_H
#define USEFRAGMENTS_FFMPEG_H

enum {
    USE_MODE_MEDIA = 1,
    USE_MODE_ONLY_VIDEO = 2,
    USE_MODE_ONLY_AUDIO = 3,
    USE_MODE_AUDIO_VIDEO = 4,
    USE_MODE_AAC_H264 = 5
};

enum {
    MSG_ON_TRANSACT_VIDEO_PRODUCER = 0x1001,
    MSG_ON_TRANSACT_VIDEO_CONSUMER = 0x1002,
    MSG_ON_TRANSACT_AUDIO_PRODUCER = 0x1003,
    MSG_ON_TRANSACT_AUDIO_CONSUMER = 0x1004
};

void createAudioTrack(int sampleRateInHz,
                      int channelCount,
                      int audioFormat);

void write(unsigned char *pcmData,
           int offsetInBytes,
           int sizeInBytes);

void audioSleep(long ms);

void videoSleep(long ms);

int onLoadProgressUpdated(int code, int progress);

void onReady();

void onChangeWindow(int width, int height);

void onPaused();

void onPlayed();

void onFinished();

void onProgressUpdated(long seconds);

void onError(int error, char *errorInfo);

void onInfo(char *info);

#endif //USEFRAGMENTS_FFMPEG_H
