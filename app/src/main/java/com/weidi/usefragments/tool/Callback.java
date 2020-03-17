package com.weidi.usefragments.tool;

/***
 Created by weidi on 2019/7/13.
 */
public interface Callback {

    int MSG_ON_READY = 0x001;
    int MSG_ON_CHANGE_WINDOW = 0x002;
    int MSG_ON_PLAYED = 0x003;
    int MSG_ON_PAUSED = 0x004;
    int MSG_ON_FINISHED = 0x005;
    int MSG_ON_INFO = 0x006;
    int MSG_ON_ERROR = 0x007;
    int MSG_ON_PROGRESS_UPDATED = 0x008;
    //int MSG_ON_PROGRESS_CHANGED = 0x008;

    void onReady();

    void onChangeWindow(int width, int height);

    void onPlayed();

    void onPaused();

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