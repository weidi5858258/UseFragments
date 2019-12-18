//
// Created by ex-wangliwei on 2016/2/14.
//

#include "SimpleAudioPlayer.h"

#include "SimpleVideoPlayer.h"

// 这个是自定义的LOG的标识
#define LOG "alexander"

static JavaVM *gJavaVm = NULL;
// 下面的jobject,jmethodID按照java的反射过程去理解,套路(jni层调用java层方法)跟反射是一样的
// java层FFMPEG对象
jobject ffmpegJavaObject = NULL;
jmethodID createAudioTrackMethodID = NULL;
jmethodID writeMethodID = NULL;
jmethodID sleepMethodID = NULL;
// java层Callback对象
jobject callbackJavaObject = NULL;
struct Callback {
    jmethodID onReadyMethodID = NULL;
    jmethodID onPausedMethodID = NULL;
    jmethodID onPlayedMethodID = NULL;
    jmethodID onFinishedMethodID = NULL;
    jmethodID onProgressUpdatedMethodID = NULL;
    jmethodID onErrorMethodID = NULL;
    jmethodID onInfoMethodID = NULL;
} callback;

/***
 called at the library loaded.
 这个方法只有放在这个文件里才有效,在其他文件不会被回调
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGD("JNI_OnLoad\n");
    gJavaVm = vm;
    return JNI_VERSION_1_6;
}

// 这个方法只有放在这个文件里才有效,在其他文件调用失效
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
    JNIEnv *audioEnv;
    bool isAttached = getEnv(&audioEnv);
    if (audioEnv != NULL
        && ffmpegJavaObject != NULL
        && createAudioTrackMethodID != NULL) {
        audioEnv->CallVoidMethod(ffmpegJavaObject, createAudioTrackMethodID,
                                 (jint) sampleRateInHz, (jint) channelCount, (jint) audioFormat);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void write(unsigned char *pcmData,
           int offsetInBytes,
           int sizeInBytes) {
    JNIEnv *audioEnv;
    bool audioIsAttached = getEnv(&audioEnv);
    if (audioEnv != NULL
        && ffmpegJavaObject != NULL
        && writeMethodID != NULL) {
        jbyteArray audioData = audioEnv->NewByteArray(sizeInBytes);
        // 拷贝数组需要对指针操作
        jbyte *cache = audioEnv->GetByteArrayElements(audioData, NULL);

        memcpy(cache, pcmData, (size_t) sizeInBytes);

        // 同步到audioData,并释放cache,与GetByteArrayElements对应
        // 如果不调用,audioData里面是空的,播放无声,并且会内存泄漏
        audioEnv->ReleaseByteArrayElements(audioData, cache, 0);

        // AudioTrack->write
        audioEnv->CallVoidMethod(ffmpegJavaObject, writeMethodID,
                                 audioData, (jint) offsetInBytes, (jint) sizeInBytes);

        // 释放局部引用,对应NewByteArray
        audioEnv->DeleteLocalRef(audioData);
    }
    if (audioIsAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void close() {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    env->DeleteGlobalRef(ffmpegJavaObject);
    env->DeleteGlobalRef(callbackJavaObject);
    ffmpegJavaObject = NULL;
    callbackJavaObject = NULL;
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void audioSleep(long ms) {
    JNIEnv *audioEnv;
    bool audioIsAttached = getEnv(&audioEnv);
    if (audioEnv != NULL
        && ffmpegJavaObject != NULL
        && sleepMethodID != NULL) {
        audioEnv->CallVoidMethod(ffmpegJavaObject, sleepMethodID, (jlong) ms);
    }
    if (audioIsAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void videoSleep(long ms) {
    JNIEnv *videoEnv;
    bool videoIsAttached = getEnv(&videoEnv);
    if (videoEnv != NULL
        && ffmpegJavaObject != NULL
        && sleepMethodID != NULL) {
        videoEnv->CallVoidMethod(ffmpegJavaObject, sleepMethodID, (jlong) ms);
    }
    if (videoIsAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onReady() {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onReadyMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onReadyMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onPaused() {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onPausedMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onPausedMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onPlayed() {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onPlayedMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onPlayedMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onFinished() {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onFinishedMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onFinishedMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onProgressUpdated(long seconds) {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onProgressUpdatedMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject,
                               callback.onProgressUpdatedMethodID,
                               (jlong) seconds);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onError() {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL && callbackJavaObject != NULL && callback.onErrorMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onErrorMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onInfo(char *info) {
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onInfoMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject,
                               callback.onInfoMethodID,
                               jniEnv->NewStringUTF(info));
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

/////////////////////////////////////////////////////////////////////////

/***
 setSurface方法在java层因为不是static方法,
 所以每个方法里至少有两个参数,即:
 JNIEnv *env和jobject ffmpegObject
 其中jobject ffmpegObject代表java层定义setSurface方法的类对象.
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_setSurface(JNIEnv *env,
                                                   jobject ffmpegObject,
                                                   jstring path,
                                                   jobject surfaceObject) {
    jclass FFMPEGClass = env->FindClass("com/weidi/usefragments/tool/FFMPEG");
    // 第三个参数: 括号中是java端方法的参数签名,括号后面是java端方法的返回值签名(V表示void)
    jmethodID createAudioTrack = env->GetMethodID(
            FFMPEGClass, "createAudioTrack", "(III)V");
    jmethodID write = env->GetMethodID(
            FFMPEGClass, "write", "([BII)V");
    jmethodID sleep = env->GetMethodID(
            FFMPEGClass, "sleep", "(J)V");

    const char *filePath = env->GetStringUTFChars(path, 0);
    alexander::setJniParameters(env, filePath, surfaceObject);
    env->ReleaseStringUTFChars(path, filePath);

    // 直接赋值是不OK的
    // ffmpegJavaObject = ffmpegObject;
    ffmpegJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(ffmpegObject));
    createAudioTrackMethodID = createAudioTrack;
    writeMethodID = write;
    sleepMethodID = sleep;

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_setCallback(JNIEnv *env,
                                                    jobject ffmpegObject,
                                                    jobject callbackObject) {
    jclass CallbackClass = env->FindClass("com/weidi/usefragments/tool/Callback");
    // 第三个参数: 括号中是java端方法的参数签名,括号后面是java端方法的返回值签名(V表示void)
    callback.onReadyMethodID = env->GetMethodID(
            CallbackClass, "onReady", "()V");
    callback.onPausedMethodID = env->GetMethodID(
            CallbackClass, "onPaused", "()V");
    callback.onPlayedMethodID = env->GetMethodID(
            CallbackClass, "onPlayed", "()V");
    callback.onFinishedMethodID = env->GetMethodID(
            CallbackClass, "onFinished", "()V");
    callback.onProgressUpdatedMethodID = env->GetMethodID(
            CallbackClass, "onProgressUpdated", "(J)V");
    callback.onErrorMethodID = env->GetMethodID(
            CallbackClass, "onError", "()V");
    callback.onInfoMethodID = env->GetMethodID(
            CallbackClass, "onInfo", "(Ljava/lang/String;)V");
    callbackJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(callbackObject));

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_initAudio(JNIEnv *env, jobject ffmpegObject) {
    return (jint) alexander::initAudioPlayer();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_initVideo(JNIEnv *env, jobject ffmpegObject) {
    onReady();
    alexander::initAV();
    return (jint) alexander::initVideoPlayer();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_audioReadData(JNIEnv *env, jobject ffmpegObject) {
#ifdef USE_AUDIO
    int type = 1;
    alexander::readData(&type);
#endif
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_audioHandleData(JNIEnv *env, jobject ffmpegObject) {
#ifdef USE_AUDIO
    alexander::handleAudioData(NULL);
#endif
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_videoReadData(JNIEnv *env, jobject ffmpegObject) {
#ifdef USE_VIDEO
    int type = 2;
    alexander::readData(&type);
#endif
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_videoHandleData(JNIEnv *env, jobject ffmpegObject) {
#ifdef USE_VIDEO
    alexander::handleVideoData(NULL);
#endif
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_play(JNIEnv *env, jobject ffmpegObject) {
    alexander::play();
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_pause(JNIEnv *env, jobject ffmpegObject) {
    alexander::pause();
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_stop(JNIEnv *env, jobject ffmpegObject) {
    alexander::stop();
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_release(JNIEnv *env, jobject ffmpegObject) {
    alexander::release();
    close();
    return (jint) 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_isRunning(JNIEnv *env, jobject instance) {
    return (jboolean) alexander::isRunning();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_isPlaying(JNIEnv *env, jobject instance) {
    return (jboolean) alexander::isPlaying();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_seekTo(JNIEnv *env, jobject instance, jlong timestamp) {
    return (jint) alexander::seekTo(timestamp);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_getDuration(JNIEnv *env, jobject instance) {
    return (jlong) alexander::getDuration();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_stepAdd(JNIEnv *env, jobject instance) {
    alexander::stepAdd();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_tool_FFMPEG_stepSubtract(JNIEnv *env, jobject instance) {
    alexander::stepSubtract();
}
