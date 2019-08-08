//
// Created by ex-wangliwei on 2016/2/14.
//

#include "SimpleAudioPlayer.h"
#include "SimpleVideoPlayer.h"

#define LOG "alexander" // 这个是自定义的LOG的标识


void ffmpeg() {
    LOGI("我是C函数...");
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_simpleAudioPlayer(JNIEnv *env, jobject object) {

    int in_2 = 888;
    return (jint) in_2;
}
