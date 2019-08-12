package com.weidi.usefragments.tool;

import android.view.Surface;

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

    public static native int simpleAudioPlayer(Surface surface);

}
