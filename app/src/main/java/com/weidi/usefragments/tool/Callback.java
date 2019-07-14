package com.weidi.usefragments.tool;

/***
 Created by weidi on 2019/7/13.
 */
public interface Callback {

    void onReady();

    void onPaused();

    void onStarted();

    void onFinished();

    /***
     录音时presentationTimeUs这个参数没有意义,
     只要知道回调一次,时间为一秒.
     * @param presentationTimeUs
     */
    void onProgressUpdated(long presentationTimeUs);

    void onError();

    void onInfo(String info);

}
