package com.weidi.usefragments.tool;

/***
 Created by weidi on 2019/8/6.
 */
public interface DownloadCallback {

    void onReady();

    void onPaused();

    void onStarted();

    void onFinished();

    // 第一次回调的是文件总长度
    void onProgressUpdated(long readDataSize);

    void onError();

    void onInfo(String info);

}
