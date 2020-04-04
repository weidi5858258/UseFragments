//
// Created by root on 19-8-8.
//

#ifndef USEFRAGMENTS_MYHEADER_H
#define USEFRAGMENTS_MYHEADER_H

// 必须得有
#include "jni.h"
#include "android/log.h"

#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <string>

// 定义了一些常用类型的最小值,最大值
#include <limits.h>
#include <unistd.h>
#include <errno.h>
#include <setjmp.h>
#include <libgen.h>
#include <inttypes.h>
#include <math.h>
//下面三个头文件使用open函数时用到
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mount.h>
#include <wchar.h>
#include <time.h>
#include <pthread.h>

#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/un.h>

#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG,__VA_ARGS__)  // 定义LOGI类型
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG,__VA_ARGS__)  // 定义LOGW类型
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG,__VA_ARGS__) // 定义LOGF类型

#define USE_AUDIO
#define USE_VIDEO

#endif //USEFRAGMENTS_MYHEADER_H
