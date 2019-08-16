//
// Created by root on 19-8-13.
//

#ifndef USEFRAGMENTS_FFMPEG_H
#define USEFRAGMENTS_FFMPEG_H

void createAudioTrack(int sampleRateInHz,
                      int channelCount,
                      int audioFormat);

void write(unsigned char *pcmData,
           int offsetInBytes,
           int sizeInBytes);

void audioSleep(long ms);

void videoSleep(long ms);

void onReady();

void onPaused();

void onPlayed();

void onFinished();

void onProgressUpdated(long seconds);

void onError();

void onInfo(char *info);

#endif //USEFRAGMENTS_FFMPEG_H
