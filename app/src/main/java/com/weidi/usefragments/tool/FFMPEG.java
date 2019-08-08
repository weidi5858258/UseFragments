package com.weidi.usefragments.tool;

/**
 * Created by root on 19-8-8.
 */

public class FFMPEG {

    static {
        try {
            System.loadLibrary("ffmpeg");
        } catch (java.lang.UnsatisfiedLinkError error) {
            error.printStackTrace();
        }
    }

    private native int simpleAudioPlayer();

}
