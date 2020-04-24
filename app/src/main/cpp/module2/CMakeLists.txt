cmake_minimum_required(VERSION 3.4.1)

add_library(alexander_jni

        SHARED

        JniUtils.c
        )

# 设置cpp目录路径
set(CPP_DIR ${CMAKE_SOURCE_DIR}/src/main/cpp)

# 设置jniLibs目录路径
set(LIBS_DIR ${CMAKE_SOURCE_DIR}/src/main/jniLibs)

# 设置CPU目录
# armeabi
if (${ANDROID_ABI} STREQUAL "armeabi")
    set(CPU_DIR armeabi)
endif (${ANDROID_ABI} STREQUAL "armeabi")

# armeabi-v7a
if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(CPU_DIR armeabi-v7a)
endif (${ANDROID_ABI} STREQUAL "armeabi-v7a")

# arm64-v8a
if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(CPU_DIR arm64-v8a)
endif (${ANDROID_ABI} STREQUAL "arm64-v8a")

# x86
if (${ANDROID_ABI} STREQUAL "x86")
    set(CPU_DIR x86)
endif (${ANDROID_ABI} STREQUAL "x86")

# x86_64
if (${ANDROID_ABI} STREQUAL "x86_64")
    set(CPU_DIR x86_64)
endif (${ANDROID_ABI} STREQUAL "x86_64")
#################################################################
# 引用头文件
include_directories(../include/)
#################################################################
find_library(log-lib
        log)
find_library(android-lib
        android)
#################################################################
target_link_libraries(alexander_jni

        ${log-lib}
        ${android-lib}

        )
#################################################################