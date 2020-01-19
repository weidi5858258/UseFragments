// IMediaDataInterface.aidl
package com.weidi.usefragments.business.video_player;

import com.weidi.usefragments.business.video_player.AVPacket;
// Declare any non-default types here with import statements

interface IMediaDataInterface {

    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    /*void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);*/

    boolean isAudioFull();
    boolean isVideoFull();
    void setAudioFull(boolean isFull);
    void setVideoFull(boolean isFull);
    void addAudioData(inout AVPacket data);
    void addVideoData(inout AVPacket data);
    AVPacket getAudioData(int index);
    AVPacket getVideoData(int index);
    int audioSize();
    int videoSize();
    void audioClear();
    void videoClear();

}
