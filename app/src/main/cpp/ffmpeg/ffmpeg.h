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
    USE_MODE_AAC_H264 = 5,
    USE_MODE_MEDIA_4K = 6,
    USE_MODE_MEDIA_MEDIACODEC = 7
};

enum DO_SOMETHING_CODE {
    DO_SOMETHING_CODE_init = 1099,
    DO_SOMETHING_CODE_setMode = 1100,
    //DO_SOMETHING_CODE_setCallback = 1101,
    DO_SOMETHING_CODE_setSurface = 1102,
    DO_SOMETHING_CODE_initPlayer = 1103,
    DO_SOMETHING_CODE_readData = 1104,
    DO_SOMETHING_CODE_audioHandleData = 1105,
    DO_SOMETHING_CODE_videoHandleData = 1106,
    DO_SOMETHING_CODE_play = 1107,
    DO_SOMETHING_CODE_pause = 1108,
    DO_SOMETHING_CODE_stop = 1109,
    DO_SOMETHING_CODE_release = 1110,
    DO_SOMETHING_CODE_isRunning = 1111,
    DO_SOMETHING_CODE_isPlaying = 1112,
    DO_SOMETHING_CODE_isPausedForUser = 1113,
    DO_SOMETHING_CODE_stepAdd = 1114,
    DO_SOMETHING_CODE_stepSubtract = 1115,
    DO_SOMETHING_CODE_seekTo = 1116,
    DO_SOMETHING_CODE_getDuration = 1117,
    DO_SOMETHING_CODE_download = 1118,
    DO_SOMETHING_CODE_closeJni = 1119,
    DO_SOMETHING_CODE_videoHandleRender = 1120,
    DO_SOMETHING_CODE_handleAudioOutputBuffer = 1121,
    DO_SOMETHING_CODE_handleVideoOutputBuffer = 1122,
};

enum {
    MSG_ON_TRANSACT_VIDEO_PRODUCER = 0x1001,
    MSG_ON_TRANSACT_VIDEO_CONSUMER = 0x1002,
    MSG_ON_TRANSACT_AUDIO_PRODUCER = 0x1003,
    MSG_ON_TRANSACT_AUDIO_CONSUMER = 0x1004,
    MSG_ON_TRANSACT_INIT_VIDEO_MEDIACODEC = 0x1005,
};

bool initMediaCodec(int type,
                    int mimeType,
                    long long *parameters, int parameterSize,
                    unsigned char *csd0, int csd0Size,
                    unsigned char *csd1, int csd1Size);

bool feedInputBufferAndDrainOutputBuffer(int type,
                                         unsigned char *encodedData,
                                         int size,
                                         long long presentationTimeUs);

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
