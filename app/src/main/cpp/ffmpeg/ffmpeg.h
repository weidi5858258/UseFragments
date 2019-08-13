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

void close();

void alexanderSleep(int ms);

#endif //USEFRAGMENTS_FFMPEG_H
