//
// Created by ex-wangliwei on 2016/2/14.
//

#include "SimpleAudioPlayer.h"

#include "SimpleVideoPlayer.h"

// 这个是自定义的LOG的标识
#define LOG "alexander"

jobject ffmpegJavaObject = NULL;
jmethodID createAudioTrackMethodID = NULL;
jmethodID writeMethodID = NULL;
jmethodID sleepMethodID = NULL;

static JavaVM *gJavaVm = NULL;

/***
 called at the library loaded.
 这个方法只有放在这个文件里才有效,在其他文件不会被回调
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    gJavaVm = vm;
    LOGD("JNI_OnLoad\n");
    return JNI_VERSION_1_6;
}

// 这个方法只有放在这个文件里才有效,在其他文件调用失败
bool getEnv(JNIEnv **env) {
    bool isAttached = false;
    jint jResult = gJavaVm->GetEnv((void **) env, JNI_VERSION_1_6);
    if (jResult != JNI_OK) {
        if (jResult == JNI_EDETACHED) {
            if (gJavaVm->AttachCurrentThread(env, NULL) != JNI_OK) {
                LOGE("AttachCurrentThread Failed.\n");
                *env = NULL;
                return isAttached;
            }
            isAttached = true;
        } else {
            LOGE("GetEnv Failed.\n");
            *env = NULL;
            return isAttached;
        }
    }

    return isAttached;
}

void createAudioTrack(int sampleRateInHz,
                      int channelCount,
                      int audioFormat) {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    if (env != NULL && ffmpegJavaObject != NULL && createAudioTrackMethodID != NULL) {
        env->CallVoidMethod(ffmpegJavaObject, createAudioTrackMethodID,
                            (jint) sampleRateInHz, (jint) channelCount, (jint) audioFormat);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void write(unsigned char *pcmData,
           int offsetInBytes,
           int sizeInBytes) {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    if (env != NULL && ffmpegJavaObject != NULL && writeMethodID != NULL) {
        jbyteArray audioData = env->NewByteArray(sizeInBytes);
        // 拷贝数组需要对指针操作
        jbyte *cache = env->GetByteArrayElements(audioData, NULL);

        memcpy(cache, pcmData, (size_t) sizeInBytes);

        // 同步到audioData,并释放cache,与GetByteArrayElements对应
        // 如果不调用,audioData里面是空的,播放无声,并且会内存泄漏
        env->ReleaseByteArrayElements(audioData, cache, 0);

        // AudioTrack->write
        env->CallVoidMethod(ffmpegJavaObject, writeMethodID,
                            audioData, (jint) offsetInBytes, (jint) sizeInBytes);

        // 释放局部引用,对应NewByteArray
        env->DeleteLocalRef(audioData);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void close() {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    env->DeleteGlobalRef(ffmpegJavaObject);
    ffmpegJavaObject = NULL;
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void alexanderSleep(int ms) {
    JNIEnv *env;
    bool isAttached = getEnv(&env);

    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_setSurface(JNIEnv *env,
                                                   jobject ffmpegObject,
                                                   jobject surfaceObject) {
    jclass FFMPEGClass = env->FindClass("com/weidi/usefragments/tool/FFMPEG");
    // 第三个参数: 括号中是java端方法的参数签名,括号后面是java端方法的返回值签名(V表示void)
    jmethodID createAudioTrack = env->GetMethodID(
            FFMPEGClass, "createAudioTrack", "(III)V");
    jmethodID write = env->GetMethodID(
            FFMPEGClass, "write", "([BII)V");

    alexander::setJniParameters(env, ffmpegObject, surfaceObject, createAudioTrack, write);

    // 直接赋值是不行的
    // ffmpegJavaObject = ffmpegObject;
    ffmpegJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(ffmpegObject));
    createAudioTrackMethodID = createAudioTrack;
    writeMethodID = write;

    /*env->CallVoidMethod(ffmpegObject, createAudioTrack, (jint) 44100, (jint) 2, (jint) 16);
    jsize leng = 10;
    jbyteArray byteArray = env->NewByteArray(leng);
    signed char uc_[] = {'aB', '&', '315', '-', '-1'};
    env->SetByteArrayRegion(byteArray, 0, leng, uc_);
    env->CallVoidMethod(ffmpegObject, write, byteArray, (jint) 0, (jint) 1024);*/

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_play(JNIEnv *env, jobject ffmpegObject) {

    alexander::alexanderVideoPlayer();

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_pause(JNIEnv *env, jobject ffmpegObject) {

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_stop(JNIEnv *env, jobject ffmpegObject) {

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_release(JNIEnv *env, jobject ffmpegObject) {

    return (jint) 0;
}
