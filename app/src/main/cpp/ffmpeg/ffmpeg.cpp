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

void write(unsigned char *audioData,
           int offsetInBytes,
           int sizeInBytes) {
    JNIEnv *env;
    bool isAttached = getEnv(&env);
    if (env != NULL && ffmpegJavaObject != NULL && writeMethodID != NULL) {
        /*jbyteArray byteArray = env->NewByteArray(sizeInBytes);
        env->SetByteArrayRegion(byteArray, 0, sizeInBytes, reinterpret_cast<jbyte *>(audioData));
        env->CallVoidMethod(ffmpegJavaObject, writeMethodID,
                            byteArray, (jint) offsetInBytes, (jint) sizeInBytes);*/

        // out_buffer缓冲区数据，转成byte数组

        // 调用AudioTrack.write()时，需要创建jbyteArray
        jbyteArray audio_sample_array = env->NewByteArray(sizeInBytes);

        // 拷贝数组需要对指针操作
        jbyte *sample_bytep = env->GetByteArrayElements(audio_sample_array,
                                                        NULL);

        // out_buffer的数据拷贝到sample_bytep
        memcpy(sample_bytep,// 目标dest所指的内存地址
               audioData,// 源src所指的内存地址的起始位置
               (size_t) sizeInBytes);// 拷贝字节的数据的大小

        // 同步到audio_sample_array，并释放sample_bytep，与GetByteArrayElements对应
        // 如果不调用，audio_sample_array里面是空的，播放无声，并且会内存泄漏
        env->ReleaseByteArrayElements(audio_sample_array,
                                      sample_bytep, 0);

        // 三、调用AudioTrack.write()
        /*env->CallIntMethod(audio_track, audio_track_write_mid,
                              audio_sample_array,// 需要播放的数据数组
                              0,
                           sizeInBytes);*/

        env->CallVoidMethod(ffmpegJavaObject, writeMethodID,
                            audio_sample_array, (jint) offsetInBytes, (jint) sizeInBytes);

        // 释放局部引用，对应NewByteArray
        env->DeleteLocalRef(audio_sample_array);
    }
    if (isAttached) {
        gJavaVm->DetachCurrentThread();
    }
}

void close() {
    /*if (isAttached) {
        LOGE("createAudioTrack() isAttached = %d", isAttached);
        gJavaVm->DetachCurrentThread();
    }*/
    /*env->DeleteGlobalRef(ffmpegJavaObject);
    ffmpegJavaObject = NULL;*/
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
