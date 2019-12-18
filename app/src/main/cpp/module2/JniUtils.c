//
// Created by ex-wangliwei on 2016/2/14.
//

#include "jni.h" // 必须得有
#include "android/log.h"

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/un.h>
// #include <cutils/sockets.h>
// #include <utils/Log.h>

#define SOCKET_NAME "weidi"
#define LOG "weidi" // 这个是自定义的LOG的标识
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG,__VA_ARGS__)  // 定义LOGI类型
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG,__VA_ARGS__)  // 定义LOGW类型
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG,__VA_ARGS__) // 定义LOGF类型


void test() {
    LOGI("我是C函数...");
}

/*************************************************************************************************/
/**
 * 从java端传递基本数据类型到C端，然后在C端打印这些值，再创造一个值返回到java端
 */

JNIEXPORT jint JNICALL Java_com_weidi_usefragments_tool_JniUtils_getIntFromC(
        JNIEnv *env, jclass obj, jint in) {
    test();
    int in_ = in;
    LOGI("int:%d\n", in_);
    int in_2 = 888;

    return (jint) in_2;
}

JNIEXPORT jbyte JNICALL Java_com_weidi_usefragments_tool_JniUtils_getByteFromC(
        JNIEnv *env, jclass obj, jbyte bt) {
    signed char bt_ = bt;
    LOGI("byte:%d\n", bt_);
    signed char bt_2 = 128;

    return (jbyte) bt_2;
}

JNIEXPORT jchar JNICALL Java_com_weidi_usefragments_tool_JniUtils_getCharFromC(
        JNIEnv *env, jclass obj, jchar ch) {
    unsigned char ch_ = ch;
    LOGI("char:%c\n", ch_);
    unsigned char ch_2 = '&';

    return (jchar) ch_2;
}

JNIEXPORT jboolean JNICALL Java_com_weidi_usefragments_tool_JniUtils_getBooleanFromC(
        JNIEnv *env, jclass obj, jboolean bool) {
    unsigned char bool_ = bool;
    LOGI("boolean:%d\n", bool_);
    unsigned char bool_2 = '%';

    return (jboolean) bool_2;
}

JNIEXPORT jshort JNICALL Java_com_weidi_usefragments_tool_JniUtils_getShortFromC(
        JNIEnv *env, jclass obj, jshort sh) {
    short sh_ = sh;
    LOGI("short:%d\n", sh_);
    short sh_2 = 555;

    return (jshort) sh_2;
}

JNIEXPORT jlong JNICALL Java_com_weidi_usefragments_tool_JniUtils_getLongFromC(
        JNIEnv *env, jclass obj, jlong lg) {
    long lg_ = lg;
    LOGI("long:%ld\n", lg_);
    long lg_2 = 77777777;

    return (jlong) lg_2;
}

JNIEXPORT jfloat JNICALL Java_com_weidi_usefragments_tool_JniUtils_getFloatFromC(
        JNIEnv *env, jclass obj, jfloat ft) {
    float ft_ = ft;
    LOGI("float:%f\n", ft_);
    float ft_2 = 3333.;

    return (jfloat) ft_2;
}

JNIEXPORT jdouble JNICALL Java_com_weidi_usefragments_tool_JniUtils_getDoubleFromC(
        JNIEnv *env, jclass obj, jdouble db) {
    double db_ = db;
    LOGI("double:%lf\n", db_);
    double db_2 = 9999999;

    return (jdouble) db_2;
}

/*************************************************************************************************/
/**
 * 处理字符串
 * 重点：
 * 1、GetStringUTFChars
 * 2、ReleaseStringUTFChars
 * 3、NewStringUTF
 */

JNIEXPORT jstring JNICALL Java_com_weidi_usefragments_tool_JniUtils_getStringFromC(
        JNIEnv *env, jclass obj, jstring str) {
    // 得到从java端传过来的字符串
    const char *str_ = (*env)->GetStringUTFChars(env, str, 0);
    LOGI("String:%s\n", str_);
    // 释放资源（不需要去理解啥意思）
    (*env)->ReleaseStringUTFChars(env, str, str_);

    // 创建一个字符串返回到java端
    jstring jstr = (*env)->NewStringUTF(env, "伟弟weidi5858258@gmail.com");

    return jstr;
}

/*************************************************************************************************/
/**
 * 处理对象数组（这里是String对象）
 * 重点：
 * 1、GetArrayLength
 * 2、GetObjectArrayElement
 * 3、SetObjectArrayElement
 */

JNIEXPORT jobjectArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getStringArrayFromC(
        JNIEnv *env, jclass obj, jobjectArray objA) {
    // 从java端得到对象数组
    jsize leng = (*env)->GetArrayLength(env, objA);
    int i;
    for (i = 0; i < leng; i++) {
        jobject obj = (*env)->GetObjectArrayElement(env, objA, i);
        jstring jstr = (jstring) obj;
        // const char *cStr = (*env)->GetStringUTFChars(env, jstr, 0);
        const char *cStr = (*env)->GetStringUTFChars(env, jstr, 0);
        LOGI("jobjectArray:%s", cStr);
        (*env)->ReleaseStringChars(env, jstr, (jchar *)cStr);
    }

    // 创造对象数组返回到java端
    // 取得所属的Class对象
    jclass objectClass = (*env)->FindClass(env, "java/lang/String");
    // 创建对象数组
    jobjectArray objectArray = (*env)->NewObjectArray(env, 5, objectClass, 0);
    jstring jstr;
    char *sa[] = {"Hello,world!", "JNI", "好", "很好", "非常好"};
    int j = 0;
    for (; j < 5; j++) {
        jstr = (*env)->NewStringUTF(env, sa[j]);
        //把jstr存入对象数组中
        (*env)->SetObjectArrayElement(env, objectArray, j, jstr);// 必须放入jstring
    }

    return objectArray;
}

/*************************************************************************************************/
/**
 * 处理基本数据类型的数组
 * 重点：
 * 1、GetIntArrayRegion
 * 2、SetIntArrayRegion
 * 3、NewIntArray
 * 其他的类似
 */

JNIEXPORT jintArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getIntArrayFromC(
        JNIEnv *env, jclass obj, jintArray inA) {
    jsize jsize_len = (*env)->GetArrayLength(env, inA);
    // 这种方法也可以，但是没有下面那种方法好，所以用下面的方法好了。
    //    int i;
    //    for(i=0;i<leng;i++){
    //        jint *in_ = (*env) -> GetIntArrayElements(env, inA, 0);
    //        int in_2 = *(in_+i);
    //        LOGI("jint:%d",in_2);
    //        (*env) -> ReleaseIntArrayElements(env, inA, in_, 0);
    //    }

    int int_[jsize_len];
    // 用这个函数简单（直接把jni类型转化成了C类型）
    (*env)->GetIntArrayRegion(env, inA, 0, jsize_len, int_);
    int i = 0;
    for (; i < jsize_len; i++) {
        LOGI("------------->%d", int_[i]);
    }

    int in_[] = {11, 22, 33, 44, 55};
    int int_leng = sizeof(in_) / sizeof(int);
    // 创建一个整型数组
    jintArray intArray = (*env)->NewIntArray(env, int_leng);
    // 赋值用这个函数简单（直接把C类型转化成了jni类型，然后返回给java端）
    (*env)->SetIntArrayRegion(env, intArray, 0, int_leng, in_);

    return intArray;
}


JNIEXPORT jbyteArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getByteArrayFromC(
        JNIEnv *env, jclass obj, jbyteArray btA) {
    jsize leng = (*env)->GetArrayLength(env, btA);
    signed char byte_[leng];
    // jbyte byte_[leng];
    (*env)->GetByteArrayRegion(env, btA, 0, leng, byte_);
    int i = 0;
    for (; i < leng; i++) {
        // byte:-128~127（赋值128实际值-128,赋值129实际值-127,赋值-128实际值127,赋值-129实际值126）
        LOGI("------------->%d", byte_[i]);
    }

    jbyteArray byteArray = (*env)->NewByteArray(env, leng);
    /**
     * -10--->48 999/-999--->57 10(0)--->48 11(1)--->49 12(2)--->50
     * 几个字符在一起的时候，取的值就是最后一个字符，然后对应到ASCII编码中的编码输出。像“aB”实际取值为“B”，其对应的ASCII编码中的编码就是数值“66”。
     * 在这里的赋值就是ASCII编码中的字符，返回到java端后就是ASCII编码中的编码（编码就是0~127这个范围）
     */
    signed char uc_[] = {'aB', '&', '315', '-', '-1'};
    (*env)->SetByteArrayRegion(env, byteArray, 0, leng, uc_);

    return byteArray;
}

// 测试已可以，中文字符还不能正确显示
JNIEXPORT jcharArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getCharArrayFromC(
        JNIEnv *env, jclass obj, jcharArray chA) {
    jsize leng = (*env)->GetArrayLength(env, chA);
    // “unsigned short”不能换成“char”去接收，不然输出结果要出错的
    unsigned short char_[leng];
    (*env)->GetCharArrayRegion(env, chA, 0, leng, char_);
    int i = 0;
    for (; i < leng; i++) {
        LOGI("------------->%c", char_[i]);
    }

    jcharArray charArray = (*env)->NewCharArray(env, leng);
    /**
     * 'A'这个里面必须放一个字符，否则显示不正确；“unsigned short”这个类型也只能这样赋值，在java端才能正确显示。
     */
    unsigned short us_[] = {'G', '2', '哈', 'a', '-'};
    (*env)->SetCharArrayRegion(env, charArray, 0, leng, us_);

    return charArray;
}

// 测试已可以
JNIEXPORT jbooleanArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getBooleanArrayFromC(
        JNIEnv *env, jclass obj, jbooleanArray boolA) {
    jsize leng = (*env)->GetArrayLength(env, boolA);
    unsigned char boolean_[leng];
    // jboolean boolean_[leng];
    (*env)->GetBooleanArrayRegion(env, boolA, 0, leng, boolean_);
    int i = 0;
    for (; i < leng; i++) {
        // 打印正确（java端为true，在这里为1；java端为false，在这里为0）
        LOGI("------------->%d", boolean_[i]);
    }

    jbooleanArray booleanArray = (*env)->NewBooleanArray(env, leng);
    /**
     * 只能放数值型的值，不能放其他字符，如字母，不然编译不通过。因为只有数值型的值传到java端时，java端才可以根据数值进行判断是true还是false；
     * 如果传到java端是个非数值字符，那么就没法判断。
     * 负的小数（不管多少）、小于1的正小数返回到java端时都为false。
     */
    unsigned char uc_[] = {-10, -0.0, -9999.0, 1.0, 0};
    (*env)->SetBooleanArrayRegion(env, booleanArray, 0, leng, uc_);

    return booleanArray;
}

JNIEXPORT jshortArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getShortArrayFromC(
        JNIEnv *env, jclass obj, jshortArray shA) {

}

JNIEXPORT jlongArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getLongArrayFromC(
        JNIEnv *env, jclass obj, jlongArray lgA) {

}

JNIEXPORT jfloatArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getFloatArrayFromC(
        JNIEnv *env, jclass obj, jfloatArray ftA) {

}

JNIEXPORT jdoubleArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getDoubleArrayFromC(
        JNIEnv *env, jclass obj, jdoubleArray dbA) {

}

/*************************************************************************************************/
/***
 传递java端自定义对象
 用"反射"的思想去理解
 */

JNIEXPORT jobject JNICALL Java_com_weidi_usefragments_tool_JniUtils_getObjectFromC(
        JNIEnv *env, jclass obj, jobject javaObject) {
    // jclass objectClass = (*env)->GetObjectClass(env, javaObject);
    jclass objectClass = (*env)->FindClass(env, "com/weidi/usefragments/javabean/Person");
    // 得到java端对象下面的属性ID
    jfieldID fieldId_name = (*env)->GetFieldID(env, objectClass, "name", "Ljava/lang/String;");
    jfieldID fieldId_age = (*env)->GetFieldID(env, objectClass, "age", "I");
    // 用属性ID得到属性值，还要用上传进来的对象
    jobject object_ = (*env)->GetObjectField(env, javaObject, fieldId_name);
    jstring string_name = (jstring) object_;
    jint int_age = (*env)->GetIntField(env, javaObject, fieldId_age);

    // 打印属性值
    const char *char_ = (*env)->GetStringUTFChars(env, string_name, 0);
    LOGI("------------->%s", char_);
    LOGI("------------->%d", (int) int_age);
    // 现在不要释放，不然下面的语句就不能执行了
    // (*env) -> ReleaseStringUTFChars(env, javaObject, string_name);

    // // 得到构造方法ID
    // jmethodID methodId_constructor2 = (*env)->GetMethodID(env, objectClass, "<init>", "()V");
    // //  创建java对象
    // jobject object_4 = (*env)->NewObject(env, objectClass, methodId_constructor2, 0);
    // 调用方法
    jmethodID toString = (*env)->GetMethodID(env, objectClass, "toString", "()Ljava/lang/String;");
    jobject object_2 = (*env)->CallObjectMethod(env, javaObject, toString, 0);
    jstring string_ = (jstring) object_2;
    char_ = (*env)->GetStringUTFChars(env, string_, 0);
    LOGI("------------->%s", char_);
    // (*env) -> ReleaseStringUTFChars(env, javaObject, string_);

    // 还是使用这种方式创建java对象比较简单
    // jobject object_3 = (*env)->AllocObject(env, objectClass);
    jmethodID setName = (*env)->GetMethodID(
            env, objectClass, "setName", "(Ljava/lang/String;)V");
    // 得到无参的构造方法ID
    jmethodID methodId_constructor = (*env)->GetMethodID(env, objectClass, "<init>", "()V");
    // 创建java对象
    jobject object_3 = (*env)->NewObject(env, objectClass, methodId_constructor, 0);
    // object_3也可以换成obje，那么上面一句也不要了，返回obje就行了
    (*env)->CallVoidMethod(env, object_3, setName, (*env)->NewStringUTF(env, "王力伟weidi"));

    // (*env)->SetObjectField(env, object_3, fieldId_name, (*env)->NewStringUTF(env, "王力伟"));
    (*env)->SetIntField(env, object_3, fieldId_age, 31);
    // 在这里也不能调用ReleaseStringUTFChars函数

    return object_3;
}

JNIEXPORT jobjectArray JNICALL Java_com_weidi_usefragments_tool_JniUtils_getObjectArrayFromC(
        JNIEnv *env, jclass obj, jobjectArray objArray) {
    jsize size_len = (*env)->GetArrayLength(env, objArray);
    jclass objectClass = (*env)->FindClass(env, "com/weidi/usefragments/javabean/Person");
    jmethodID methodId_ = (*env)->GetMethodID(env, objectClass, "toString", "()Ljava/lang/String;");
    int i;
    for (i = 0; i < size_len; i++) {
        jobject object_ = (*env)->GetObjectArrayElement(env, objArray, i);
        jobject object_2 = (*env)->CallObjectMethod(env, object_, methodId_, 0);
        const char *char_ = (*env)->GetStringUTFChars(env, (jstring) object_2, 0);
        LOGI("------------->%s", char_);
        (*env)->ReleaseStringUTFChars(env, (jstring) object_2, char_);
    }

    jfieldID fieldId_name = (*env)->GetFieldID(env, objectClass, "name", "Ljava/lang/String;");
    jfieldID fieldId_age = (*env)->GetFieldID(env, objectClass, "age", "I");
    jobjectArray objectArray = (*env)->NewObjectArray(env, 3, objectClass, 0);
    jobject object_3 = (*env)->AllocObject(env, objectClass);
    (*env)->SetObjectField(env, object_3, fieldId_name, (*env)->NewStringUTF(env, "阿宝"));
    (*env)->SetIntField(env, object_3, fieldId_age, 40);
    jobject object_4 = (*env)->AllocObject(env, objectClass);
    (*env)->SetObjectField(env, object_4, fieldId_name, (*env)->NewStringUTF(env, "阿大"));
    (*env)->SetIntField(env, object_4, fieldId_age, 35);
    jobject object_5 = (*env)->AllocObject(env, objectClass);
    (*env)->SetObjectField(env, object_5, fieldId_name, (*env)->NewStringUTF(env, "阿二"));
    (*env)->SetIntField(env, object_5, fieldId_age, 34);
    (*env)->SetObjectArrayElement(env, objectArray, 0, object_3);
    (*env)->SetObjectArrayElement(env, objectArray, 1, object_4);
    (*env)->SetObjectArrayElement(env, objectArray, 2, object_5);

    return objectArray;
}

/*int socketTest () {
 char log[200];

 int connect_number = 6;
 int fdListen = -1, new_fd = -1;
 int ret;
 struct sockaddr_un peeraddr;
 socklen_t socklen = sizeof(peeraddr);
 int numbytes;
 char buff[256];
 //这一步很关键，就是获取init.rc中配置的名为 "htfsk" 的socket
 fdListen = android_get_control_socket(SOCKET_NAME);
 if (fdListen < 0) {
  sprintf(log, "Failed to get socket '" SOCKET_NAME "' errno:%d", errno);
  __android_log_write(ANDROID_LOG_DEBUG, "FTM_JNI", log);
  exit(-1);
 }
 //开始监听
 ret = listen(fdListen, connect_number);

 sprintf(log, "Listen result %d", ret);
 __android_log_write(ANDROID_LOG_DEBUG, "FTM_JNI", log);

 if (ret < 0) {
  perror("listen");
  exit(-1);
 }
 //等待Socket客户端发启连接请求
 new_fd = accept(fdListen, (struct sockaddr *) &peeraddr, &socklen);
 sprintf(log, "Accept_fd %d", new_fd);
 __android_log_write(ANDROID_LOG_DEBUG, "FTM_JNI", log);
 if (new_fd < 0) {
  sprintf(log, "%d", errno);
  __android_log_write(ANDROID_LOG_DEBUG, "FTM_JNI", log);
  perror("accept error");
  exit(-1);
 }

 while (1) {
  //循环等待Socket客户端发来消息
  __android_log_write(ANDROID_LOG_DEBUG, "FTM_JNI", "Waiting for receive");
  if ((numbytes = recv(new_fd, buff, sizeof(buff), 0)) == -1) {
   sprintf(log, "%d", errno);
   __android_log_write(ANDROID_LOG_DEBUG, "FTM_JNI", log);
   perror("recv");
   continue;
  }
  //发送消息回执给Socket客户端
  if (send(new_fd, buff, strlen(buff), 0) == -1) {
   perror("send");
   close(new_fd);
   exit(0);
  }
 }

 close(new_fd);
 close(fdListen);
 return 0;
}*/
