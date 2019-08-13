package com.weidi.usefragments.tool;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.SystemClock;
import android.view.Surface;

import com.weidi.usefragments.media.MediaUtils;

/**
 * Created by root on 19-8-8.
 */

public class FFMPEG {

    private static final String TAG = FFMPEG.class.getSimpleName();

    static {
        try {
            System.loadLibrary("ffmpeg");
        } catch (java.lang.UnsatisfiedLinkError error) {
            error.printStackTrace();
        }
    }

    private AudioTrack mAudioTrack;
    private float mVolume = 1.0f;

    public native int setSurface(Surface surface);

    public native int play();

    public native int pause();

    public native int stop();

    public native int release();

    // 供jni层调用
    private void createAudioTrack(int sampleRateInHz,
                                  int channelCount,
                                  int audioFormat) {
        MLog.i(TAG, "createAudioTrack()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);

        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }

        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        setVolume();
        if (mAudioTrack != null) {
            mAudioTrack.play();
        }
    }

    // 供jni层调用
    private void write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        //MLog.i(TAG, "audioData.length: " + audioData.length);
        /*for (int i = 0; i < audioData.length; i++) {
            MLog.i(TAG, "" + audioData[i]);
        }
        MLog.i(TAG, "write()" +
                " offsetInBytes: " + offsetInBytes +
                " sizeInBytes: " + sizeInBytes);*/

        if (mAudioTrack != null
                && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
        }
    }

    private void sleep(long ms) {
        SystemClock.sleep(ms);
    }

    private void setVolume() {
        if (mAudioTrack == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioTrack.setVolume(mVolume);
        } else {
            mAudioTrack.setStereoVolume(mVolume, mVolume);
        }
    }

    /*private void stop() {
        MediaUtils.stopAudioTrack(mAudioTrack);
    }

    private void release() {
        MediaUtils.releaseAudioTrack(mAudioTrack);
    }*/

}
