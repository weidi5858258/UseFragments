#include <jni.h>
#include "android/log.h"

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/un.h>
#include <string>

/***
 每个jni函数都要带上
 extern "C"
 JNIEXPORT jstring JNICALL
 jstring也可能是void,jint,jbyte...

 JNIEnv *env, jobject object
 这两个参数是每个jni函数默认都有的.
 如果只有这两个参数,说明在java端是个无参函数.
 */

extern "C"
JNIEXPORT jstring JNICALL
Java_com_weidi_usefragments_MainActivity1_stringFromJNI(JNIEnv *env, jobject object) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
