//
// Created by ex-wangliwei on 2016/2/14.
//

#include "SimpleVideoPlayer3.h"
#include "OnlyVideoPlayer.h"
#include "OnlyAudioPlayer.h"

// 这个是自定义的LOG的标识
#define LOG "player_alexander"

/***
 全局变量的生命周期:
 普通的全局变量,不需要是static的全局变量
 (好像)
 只要进程不被kill掉,这些变量的值就会一直被保存着
 */

// 这个值在任何情况下都不要置为"NULL"
static JavaVM *gJavaVm = NULL;

// 下面的jobject,jmethodID按照java的反射过程去理解,套路(jni层调用java层方法)跟反射是一样的
// java层FFMPEG对象
jobject ffmpegJavaObject = NULL;
jmethodID createAudioTrackMethodID = NULL;
jmethodID writeMethodID = NULL;
jmethodID sleepMethodID = NULL;

// java层FFMPEG中定义的Callback对象
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

enum {
    USE_MODE_MEDIA = 1,
    USE_MODE_ONLY_VIDEO = 2,
    USE_MODE_ONLY_AUDIO = 3
};
int use_mode = USE_MODE_ONLY_AUDIO;

// test
int runCount = 0;

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

// 回调java端FFMPEG类中的有关方法
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
    LOGI("onReady()\n");
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
    LOGI("onPaused()\n");
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
    LOGI("onPlayed()\n");
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
    LOGI("onFinished()\n");
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
    LOGE("onError()\n");
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

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_setMode(JNIEnv *env, jobject thiz,
                                                                  jint mode) {
    use_mode = (int) mode;
    if (use_mode != USE_MODE_MEDIA
        && use_mode != USE_MODE_ONLY_VIDEO
        && use_mode != USE_MODE_ONLY_AUDIO) {
        use_mode = USE_MODE_ONLY_AUDIO;
    }
}

/***
 setSurface方法在java层因为不是static方法,
 所以每个方法里至少有两个参数,即:
 JNIEnv *env和jobject ffmpegObject
 其中jobject ffmpegObject代表java层定义setSurface方法的类对象.
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_setSurface(JNIEnv *env,
                                                                     jobject ffmpegObject,
                                                                     jstring path,
                                                                     jobject surfaceObject) {
    jclass FFMPEGClass = env->GetObjectClass(ffmpegObject);
    //jclass FFMPEGClass = env->FindClass("com/weidi/usefragments/business/video_player/FFMPEG");
    //CHECK(FFMPEGClass != NULL);

    // 第三个参数: 括号中是java端方法的参数签名,括号后面是java端方法的返回值签名(V表示void)
    jmethodID createAudioTrack = env->GetMethodID(
            FFMPEGClass, "createAudioTrack", "(III)V");
    jmethodID write = env->GetMethodID(
            FFMPEGClass, "write", "([BII)V");
    jmethodID sleep = env->GetMethodID(
            FFMPEGClass, "sleep", "(J)V");

    // 路径
    const char *filePath = env->GetStringUTFChars(path, 0);
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::setJniParameters(env, filePath, NULL);
            break;
        }
        default:
            break;
    }
    env->ReleaseStringUTFChars(path, filePath);

    // java层native方法所对应的类对象
    // 在java层的native方法不是static的,因此需要用到java层的对象
    // ffmpegJavaObject = ffmpegObject;// error 直接赋值是不OK的
    ffmpegJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(ffmpegObject));

    createAudioTrackMethodID = createAudioTrack;
    writeMethodID = write;
    sleepMethodID = sleep;

    LOGI("setSurface()       runCount: %d\n", (++runCount));

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_setCallback(JNIEnv *env,
                                                                      jobject ffmpegObject,
                                                                      jobject callbackObject) {
    jclass CallbackClass = env->FindClass("com/weidi/usefragments/tool/Callback");

    // 调用下面方法需要用到这个对象
    callbackJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(callbackObject));

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

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_initPlayer(JNIEnv *env,
                                                                     jobject instance) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            return (jint) alexander_media::initPlayer();
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jint) alexander_only_video::initPlayer();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jint) alexander_only_audio::initPlayer();
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_readData(JNIEnv *env, jobject instance) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::readData(NULL);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::readData(NULL);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::readData(NULL);
            break;
        }
        default:
            break;
    }
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_audioHandleData(JNIEnv *env,
                                                                          jobject ffmpegObject) {
    int type = TYPE_AUDIO;
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::handleData(&type);
            break;
        }
        default:
            break;
    }
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_videoHandleData(JNIEnv *env,
                                                                          jobject ffmpegObject) {
    int type = TYPE_VIDEO;
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::handleData(&type);
            break;
        }
        default:
            break;
    }
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_play(JNIEnv *env, jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::play();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::play();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::play();
            break;
        }
        default:
            break;
    }
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_pause(JNIEnv *env, jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::pause();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::pause();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::pause();
            break;
        }
        default:
            break;
    }
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_stop(JNIEnv *env, jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::stop();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stop();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stop();
            break;
        }
        default:
            break;
    }
    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_release(JNIEnv *env,
                                                                  jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::release();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::release();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::release();
            break;
        }
        default:
            break;
    }
    close();
    return (jint) 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_isRunning(JNIEnv *env,
                                                                    jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            return (jboolean) alexander_media::isRunning();
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jboolean) alexander_only_video::isRunning();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jboolean) alexander_only_audio::isRunning();
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_isPlaying(JNIEnv *env,
                                                                    jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            return (jboolean) alexander_media::isPlaying();
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jboolean) alexander_only_video::isPlaying();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jboolean) alexander_only_audio::isPlaying();
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_seekTo(JNIEnv *env, jobject ffmpegObject,
                                                                 jlong timestamp) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            return (jint) alexander_media::seekTo((int64_t) timestamp);
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jint) alexander_only_video::seekTo((int64_t) timestamp);
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jint) alexander_only_audio::seekTo((int64_t) timestamp);
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_getDuration(JNIEnv *env,
                                                                      jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            return (jlong) alexander_media::getDuration();
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jlong) alexander_only_video::getDuration();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jlong) alexander_only_audio::getDuration();
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_stepAdd(JNIEnv *env,
                                                                  jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::stepAdd();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stepAdd();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stepAdd();
            break;
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_stepSubtract(JNIEnv *env,
                                                                       jobject ffmpegObject) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::stepSubtract();
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stepSubtract();
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stepSubtract();
            break;
        }
        default:
            break;
    }
}
