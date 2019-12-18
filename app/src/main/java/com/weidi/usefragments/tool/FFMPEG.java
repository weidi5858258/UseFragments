package com.weidi.usefragments.tool;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Surface;

import com.weidi.usefragments.business.contents.JniPlayerActivity;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.utils.MyToast;

/***
 Created by root on 19-8-8.
 */

public class FFMPEG {

    private static final String TAG = FFMPEG.class.getSimpleName();

    static {
        try {
            System.loadLibrary("ffmpeg");
        } catch (java.lang.UnsatisfiedLinkError error) {
            MLog.e(TAG, "卧槽, ffmpeg库加载失败了!!!");
            error.printStackTrace();
        }
    }

    private AudioTrack mAudioTrack;
    private float mVolume = 1.0f;

    // 首先调用
    public native int setSurface(String filePath, Surface surface);

    public native int setCallback(Callback callback);

    public native int initAudio();

    public native int initVideo();
    // 开线程1
    public native int audioReadData();
    // 开线程2
    public native int audioHandleData();
    // 开线程3
    public native int videoReadData();
    // 开线程4
    public native int videoHandleData();

    public native int play();

    public native int pause();

    public native int stop();

    public native int release();

    public native boolean isRunning();

    public native boolean isPlaying();
    // 快进
    public native void stepAdd();
    // 快退
    public native void stepSubtract();
    // 单位: 秒
    public native int seekTo(long timestamp);

    public native long getDuration();

    public void releaseAll() {
        release();
        MediaUtils.releaseAudioTrack(mAudioTrack);
    }

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

    // 供jni层调用
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

    private Handler mUiHandler;

    public void setHandler(Handler handler) {
        mUiHandler = handler;
    }
    // 供jni层调用
    public Callback mCallback = new Callback() {
        @Override
        public void onReady() {
            if (mUiHandler != null) {
                mUiHandler.removeMessages(JniPlayerActivity.MSG_ON_READY);
                mUiHandler.sendEmptyMessage(JniPlayerActivity.MSG_ON_READY);
            }
        }

        @Override
        public void onPaused() {
            MLog.i(TAG, "onPaused()");
            if (mUiHandler != null) {
                mUiHandler.removeMessages(JniPlayerActivity.MSG_ON_PAUSED);
                mUiHandler.sendEmptyMessage(JniPlayerActivity.MSG_ON_PAUSED);
            }
        }

        @Override
        public void onPlayed() {
            MLog.i(TAG, "onPlayed()");
            if (mUiHandler != null) {
                mUiHandler.removeMessages(JniPlayerActivity.MSG_ON_PLAYED);
                mUiHandler.sendEmptyMessage(JniPlayerActivity.MSG_ON_PLAYED);
            }
        }

        @Override
        public void onFinished() {
            if (mUiHandler != null) {
                mUiHandler.removeMessages(JniPlayerActivity.MSG_ON_FINISHED);
                mUiHandler.sendEmptyMessage(JniPlayerActivity.MSG_ON_FINISHED);
            }
        }

        @Override
        public void onProgressUpdated(long presentationTime) {
            if (mUiHandler != null) {
                Message msg = mUiHandler.obtainMessage();
                msg.what = JniPlayerActivity.PLAYBACK_PROGRESS_UPDATED;
                msg.obj = presentationTime;
                mUiHandler.removeMessages(JniPlayerActivity.PLAYBACK_PROGRESS_UPDATED);
                mUiHandler.sendMessage(msg);
            }
        }

        @Override
        public void onError() {

        }

        @Override
        public void onInfo(String info) {
            if (!TextUtils.isEmpty(info)) {
                MyToast.show(info);
            }
        }
    };

}
