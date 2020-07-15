package com.google.android.exoplayer2;

import android.media.MediaFormat;

public class ObjectHelper {

    private volatile static ObjectHelper sObjectHelper;

    private ObjectHelper() {

    }

    public static ObjectHelper getDefault() {
        if (sObjectHelper == null) {
            synchronized (ObjectHelper.class) {
                if (sObjectHelper == null) {
                    sObjectHelper = new ObjectHelper();
                }
            }
        }
        return sObjectHelper;
    }

    public interface MediaFormatCallback {
        void onCreated(MediaFormat mediaFormat);
    }

    private MediaFormatCallback mCallback;

    public void setMediaFormatCallback(MediaFormatCallback callback) {
        mCallback = callback;
    }

    public void setMediaFormat(MediaFormat mediaFormat) {
        if (mCallback != null) {
            mCallback.onCreated(mediaFormat);
        }
    }

}
