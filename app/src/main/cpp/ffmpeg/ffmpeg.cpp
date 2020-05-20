//
// Created by ex-wangliwei on 2016/2/14.
//

#include "ffmpeg.h"
#include "MediaPlayer.h"
#include "OnlyVideoPlayer.h"
#include "OnlyAudioPlayer.h"
#include "AudioVideoPlayer.h"

// 这个是自定义的LOG的标识
#define LOG "player_alexander"

/***
 全局变量的生命周期:
 普通的全局变量,不需要是static的全局变量
 (好像)
 只要进程不被kill掉,这些变量的值就会一直被保存着

 // jclass jobject jstring jobjectArray jintArray 等需要释放空间
 */

// 这个值在任何情况下都不要置为"NULL"
static JavaVM *gJavaVm = NULL;

// 下面的jobject,jmethodID按照java的反射过程去理解,套路(jni层调用java层方法)跟反射是一样的
// java层FFMPEG对象
jobject ffmpegJavaObject = NULL;
jmethodID createAudioTrackMethodID = NULL;
jmethodID writeMethodID = NULL;
jmethodID sleepMethodID = NULL;

jclass jniObjectClass = NULL;
jobject videoProducerObject = NULL;
jobject videoConsumerObject = NULL;
jobject audioProducerObject = NULL;
jobject audioConsumerObject = NULL;

// java层FFMPEG中定义的Callback对象
jobject callbackJavaObject = NULL;
struct Callback {
    jmethodID onTransactMethodID = NULL;
    jmethodID onReadyMethodID = NULL;
    jmethodID onChangeWindowMethodID = NULL;
    jmethodID onPlayedMethodID = NULL;
    jmethodID onPausedMethodID = NULL;
    jmethodID onFinishedMethodID = NULL;
    jmethodID onProgressUpdatedMethodID = NULL;
    jmethodID onErrorMethodID = NULL;
    jmethodID onInfoMethodID = NULL;
} callback;

int use_mode = USE_MODE_MEDIA;

// test
int runCount = 0;

/***
 called at the library loaded.
 这个方法只有放在这个文件里才有效,在其他文件不会被回调
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGD("JNI_OnLoad\n");
    gJavaVm = vm;
    //av_jni_set_java_vm(vm, NULL);
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
int onLoadProgressUpdated(int code, int progress) {
    //LOGI("onLoadProgressUpdated() code: %d progress: %d\n", code, progress);
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onTransactMethodID != NULL) {
        jobject *jniObject = NULL;
        switch (code) {
            case 0x1000:
                jniObject = &videoProducerObject;
                break;
            case 0x1002:
                jniObject = &videoConsumerObject;
                break;
            case 0x1003:
                jniObject = &audioProducerObject;
                break;
            case 0x1004:
                jniObject = &audioConsumerObject;
                break;
            default:
                break;
        }
        if (jniObject) {
            jfieldID valueIntFieldId = jniEnv->GetFieldID(jniObjectClass, "valueInt", "I");
            jniEnv->SetIntField(*jniObject, valueIntFieldId, progress);
            jniEnv->CallIntMethod(
                    callbackJavaObject, callback.onTransactMethodID, code, *jniObject);
        }
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
    return 0;
}

void onReady() {
    LOGI("onReady()\n");
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

void onChangeWindow(int width, int height) {
    LOGI("onChangeWindow()\n");
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onChangeWindowMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onChangeWindowMethodID,
                               (jint) width, (jint) height);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void onPlayed() {
    LOGI("onPlayed()\n");
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

void onPaused() {
    LOGI("onPaused()\n");
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

void onFinished() {
    LOGF("onFinished()\n");
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    LOGF("onFinished() isAttached: %d\n", isAttached);
    if (jniEnv == NULL) {
        LOGI("onFinished() jniEnv = NULL\n");
    }
    if (callbackJavaObject == NULL) {
        LOGI("onFinished() callbackJavaObject = NULL\n");
    }
    if (callback.onFinishedMethodID == NULL) {
        LOGI("onFinished() callback.onFinishedMethodID = NULL\n");
    }
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onFinishedMethodID != NULL) {
        LOGF("onFinished() callback.onFinishedMethodID\n");
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onFinishedMethodID);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
    //close();
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

void onError(int error, char *errorInfo) {
    LOGE("jni onError() error: %d errorInfo: %s\n", error, errorInfo);
    JNIEnv *jniEnv;
    bool isAttached = getEnv(&jniEnv);
    if (jniEnv != NULL
        && callbackJavaObject != NULL
        && callback.onErrorMethodID != NULL) {
        jniEnv->CallVoidMethod(callbackJavaObject, callback.onErrorMethodID,
                               error, jniEnv->NewStringUTF(errorInfo));
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

static jint onTransact_download(JNIEnv *env, jobject thiz, jint code, jobject jniObject) {
    if (use_mode != USE_MODE_MEDIA) {
        return -1;
    }

    jfieldID fieldID = env->GetFieldID(jniObjectClass, "valueStringArray", "[Ljava/lang/String;");
    jobjectArray objects = static_cast<jobjectArray>(env->GetObjectField(jniObject, fieldID));

    jstring element0 = static_cast<jstring>(env->GetObjectArrayElement(objects, 0));
    jstring element1 = static_cast<jstring>(env->GetObjectArrayElement(objects, 1));
    jstring element2 = static_cast<jstring>(env->GetObjectArrayElement(objects, 2));
    const char *flagStr = env->GetStringUTFChars(element0, 0);
    const char *filePath = env->GetStringUTFChars(element1, 0);
    const char *fileName = env->GetStringUTFChars(element2, 0);
    int flag = -1;
    if (!strcmp(flagStr, "0")) {
        flag = 0;
    } else if (!strcmp(flagStr, "1")) {
        flag = 1;
    } else if (!strcmp(flagStr, "2")) {
        flag = 2;
    } else if (!strcmp(flagStr, "3")) {
        flag = 3;
    } else if (!strcmp(flagStr, "4")) {
        flag = 4;
    } else if (!strcmp(flagStr, "5")) {
        flag = 5;
    }
    LOGI("onTransact() flag: %d path: %s fileName: %s\n", flag, filePath, fileName);

    alexander_media::download(flag, filePath, fileName);

    // release
    env->ReleaseStringUTFChars(element0, flagStr);
    env->ReleaseStringUTFChars(element1, filePath);
    env->ReleaseStringUTFChars(element2, fileName);
    env->DeleteLocalRef(objects);
    env->DeleteLocalRef(element0);
    env->DeleteLocalRef(element1);
    env->DeleteLocalRef(element2);

    return 0;
}

/////////////////////////////////////////////////////////////////////////

// 避免最常见的 10 大 JNI 编程错误的技巧和工具
// https://blog.csdn.net/lubeijing2008xu/article/details/37569809

/***
    jsize count = env->GetArrayLength(objects);
    for (jsize i = 0; i < count; i++) {
        jobject element = env->GetObjectArrayElement(objects, i);
        if (env->ExceptionOccurred()) {
            break;
        }

        env->DeleteLocalRef(element);
    }

    jclass integerClass = env->FindClass("java/lang/Integer");
    jobject element0 = env->GetObjectArrayElement(objects, 0);
    int flag = -1;
    if (env->IsInstanceOf(element0, integerClass)) {
        jint flag_ = (jint) element0;
        flag = (int) flag_;
    }
    env->DeleteLocalRef(integerClass);
    得到的flag跟java端传过来的值不一样
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_onTransact(JNIEnv *env, jobject thiz,
                                                                     jint code,
                                                                     jobject jniObject) {
    //
    switch (code) {
        case 1100:
            return onTransact_download(env, thiz, code, jniObject);

        default:
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_setMode(JNIEnv *env, jobject thiz,
                                                                  jint mode) {
    use_mode = (int) mode;
    LOGI("setMode() use_mode: %d\n", use_mode);
    if (use_mode != USE_MODE_MEDIA
        && use_mode != USE_MODE_ONLY_VIDEO
        && use_mode != USE_MODE_ONLY_AUDIO
        && use_mode != USE_MODE_AUDIO_VIDEO) {
        use_mode = USE_MODE_MEDIA;
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
    createAudioTrackMethodID = env->GetMethodID(
            FFMPEGClass, "createAudioTrack", "(III)V");
    writeMethodID = env->GetMethodID(
            FFMPEGClass, "write", "([BII)V");
    sleepMethodID = env->GetMethodID(
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
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::setJniParameters(env, filePath, surfaceObject);
            break;
        }
        default:
            break;
    }
    env->ReleaseStringUTFChars(path, filePath);

    if (ffmpegJavaObject != NULL) {
        env->DeleteGlobalRef(ffmpegJavaObject);
        ffmpegJavaObject = NULL;
    }
    // java层native方法所对应的类对象
    // 在java层的native方法不是static的,因此需要用到java层的对象
    // ffmpegJavaObject = ffmpegObject;// error 直接赋值是不OK的
    ffmpegJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(ffmpegObject));

    if (videoProducerObject) {
        env->DeleteGlobalRef(videoProducerObject);
        videoProducerObject = NULL;
    }
    if (videoConsumerObject) {
        env->DeleteGlobalRef(videoConsumerObject);
        videoConsumerObject = NULL;
    }
    if (audioProducerObject) {
        env->DeleteGlobalRef(audioProducerObject);
        audioProducerObject = NULL;
    }
    if (audioConsumerObject) {
        env->DeleteGlobalRef(audioConsumerObject);
        audioConsumerObject = NULL;
    }
    if (jniObjectClass) {
        env->DeleteGlobalRef(jniObjectClass);
        jniObjectClass = NULL;
    }
    jclass tempJniObjectClass = env->FindClass("com/weidi/usefragments/tool/JniObject");
    jniObjectClass = reinterpret_cast<jclass>(env->NewGlobalRef(tempJniObjectClass));
    env->DeleteLocalRef(tempJniObjectClass);

    jobject jniObject;
    jfieldID fieldID;
    //
    fieldID = env->GetStaticFieldID(FFMPEGClass, "videoProducer",
                                    "Lcom/weidi/usefragments/tool/JniObject;");
    jniObject = env->GetStaticObjectField(FFMPEGClass, fieldID);
    videoProducerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jniObject));
    env->DeleteLocalRef(jniObject);
    //
    fieldID = env->GetStaticFieldID(FFMPEGClass, "videoConsumer",
                                    "Lcom/weidi/usefragments/tool/JniObject;");
    jniObject = env->GetStaticObjectField(FFMPEGClass, fieldID);
    videoConsumerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jniObject));
    env->DeleteLocalRef(jniObject);
    //
    fieldID = env->GetStaticFieldID(FFMPEGClass, "audioProducer",
                                    "Lcom/weidi/usefragments/tool/JniObject;");
    jniObject = env->GetStaticObjectField(FFMPEGClass, fieldID);
    audioProducerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jniObject));
    env->DeleteLocalRef(jniObject);
    //
    fieldID = env->GetStaticFieldID(FFMPEGClass, "audioConsumer",
                                    "Lcom/weidi/usefragments/tool/JniObject;");
    jniObject = env->GetStaticObjectField(FFMPEGClass, fieldID);
    audioConsumerObject = reinterpret_cast<jobject>(env->NewGlobalRef(jniObject));
    env->DeleteLocalRef(jniObject);

    env->DeleteLocalRef(FFMPEGClass);
    FFMPEGClass = NULL;
    LOGI("setSurface()       runCount  : %d\n", (++runCount));

    return (jint) 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_setCallback(JNIEnv *env,
                                                                      jobject ffmpegObject,
                                                                      jobject callbackObject) {
    jclass CallbackClass = env->FindClass("com/weidi/usefragments/tool/Callback");

    if (callbackJavaObject != NULL) {
        env->DeleteGlobalRef(callbackJavaObject);
        callbackJavaObject = NULL;
    }
    // 调用下面方法需要用到这个对象
    callbackJavaObject = reinterpret_cast<jobject>(env->NewGlobalRef(callbackObject));

    // 第三个参数: 括号中是java端方法的参数签名,括号后面是java端方法的返回值签名(V表示void)
    callback.onTransactMethodID = env->GetMethodID(
            CallbackClass, "onTransact", "(ILcom/weidi/usefragments/tool/JniObject;)I");
    callback.onReadyMethodID = env->GetMethodID(
            CallbackClass, "onReady", "()V");
    callback.onChangeWindowMethodID = env->GetMethodID(
            CallbackClass, "onChangeWindow", "(II)V");
    callback.onPlayedMethodID = env->GetMethodID(
            CallbackClass, "onPlayed", "()V");
    callback.onPausedMethodID = env->GetMethodID(
            CallbackClass, "onPaused", "()V");
    callback.onFinishedMethodID = env->GetMethodID(
            CallbackClass, "onFinished", "()V");
    callback.onProgressUpdatedMethodID = env->GetMethodID(
            CallbackClass, "onProgressUpdated", "(J)V");
    callback.onErrorMethodID = env->GetMethodID(
            CallbackClass, "onError", "(ILjava/lang/String;)V");
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
        case USE_MODE_AUDIO_VIDEO: {
            return (jint) alexander_audio_video::initPlayer();
        }
        default:
            break;
    }
}

static int type;
static int COUNTS = 1;
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
        case USE_MODE_AUDIO_VIDEO: {
            if (COUNTS == 1) {
                type = TYPE_AUDIO;
                ++COUNTS;
                LOGI("readData() TYPE_AUDIO\n");
                alexander_audio_video::readData(&type);
            } else {
                type = TYPE_VIDEO;
                --COUNTS;
                LOGI("readData() TYPE_VIDEO\n");
                alexander_audio_video::readData(&type);
            }
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
            //alexander_only_video::handleData(&type);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::handleData(&type);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::handleData(&type);
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
            //alexander_only_audio::handleData(&type);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::handleData(&type);
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
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::play();
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
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::pause();
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
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::stop();
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
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::release();
            break;
        }
        default:
            break;
    }
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
        case USE_MODE_AUDIO_VIDEO: {
            return (jboolean) alexander_audio_video::isRunning();
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
        case USE_MODE_AUDIO_VIDEO: {
            return (jboolean) alexander_audio_video::isPlaying();
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_isPausedForUser(JNIEnv *env,
                                                                          jobject thiz) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            return (jboolean) alexander_media::isPausedForUser();
        }
        case USE_MODE_ONLY_VIDEO: {
            return (jboolean) alexander_only_video::isPausedForUser();
        }
        case USE_MODE_ONLY_AUDIO: {
            return (jboolean) alexander_only_audio::isPausedForUser();
        }
        case USE_MODE_AUDIO_VIDEO: {
            return (jboolean) alexander_audio_video::isPausedForUser();
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
        case USE_MODE_AUDIO_VIDEO: {
            return (jint) alexander_audio_video::seekTo((int64_t) timestamp);
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
        case USE_MODE_AUDIO_VIDEO: {
            return (jlong) alexander_audio_video::getDuration();
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_stepAdd(JNIEnv *env, jobject ffmpegObject,
                                                                  jlong addStep) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stepAdd((int64_t) addStep);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::stepAdd((int64_t) addStep);
            break;
        }
        default:
            break;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_weidi_usefragments_business_video_1player_FFMPEG_stepSubtract(JNIEnv *env,
                                                                       jobject ffmpegObject,
                                                                       jlong subtractStep) {
    switch (use_mode) {
        case USE_MODE_MEDIA: {
            alexander_media::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_ONLY_VIDEO: {
            alexander_only_video::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_ONLY_AUDIO: {
            alexander_only_audio::stepSubtract((int64_t) subtractStep);
            break;
        }
        case USE_MODE_AUDIO_VIDEO: {
            alexander_audio_video::stepSubtract((int64_t) subtractStep);
            break;
        }
        default:
            break;
    }
}
