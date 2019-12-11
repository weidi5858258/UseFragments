//
// Created by alexander on 2019/12/6.
//

#include "SocketClient.h"
#include "../include/Log.h"
#include <sys/socket.h>//socket相关
#include <pthread.h>//线程相关

#define LOG "alexander SocketClient"

static const volatile char SocketName[] = "\n";

SocketClient::SocketClient() {
    LOGD("SocketClient() created. %p\n", this);
}

SocketClient::~SocketClient() {
    LOGD("~SocketClient() destroyed. %p\n", this);
}

int SocketClient::connect() {
    LOGI("SocketClient::connect(). %p\n", this);
//    int fd = socket_local_client(socketName, ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
}