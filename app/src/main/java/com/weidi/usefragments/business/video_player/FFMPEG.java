package com.weidi.usefragments.business.video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemClock;

import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.JniObject;
import com.weidi.usefragments.tool.MLog;

import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_IS_MUTE;
import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

/***
 Created by root on 19-8-8.
 */

public class FFMPEG {

    private static final String TAG =
            "player_alexander";
    //FFMPEG.class.getSimpleName();

    // status_t onTransact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags = 0)
    // public native String onTransact(int code, Parcel data, Parcel reply);
    // 从上面调到下面只定义这一个方法
    public native String onTransact(int code, JniObject jniObject);

    static {
        try {
            System.loadLibrary("crypto");
        } catch (java.lang.UnsatisfiedLinkError error) {
            MLog.e(TAG, "卧槽, crypto库加载失败了!!!");
            error.printStackTrace();
        }
        try {
            System.loadLibrary("ssl");
        } catch (java.lang.UnsatisfiedLinkError error) {
            MLog.e(TAG, "卧槽, ssl库加载失败了!!!");
            error.printStackTrace();
        }
        try {
            System.loadLibrary("ffmpeg");
        } catch (java.lang.UnsatisfiedLinkError error) {
            MLog.e(TAG, "卧槽, ffmpeg库加载失败了!!!");
            error.printStackTrace();
        }
    }

    private volatile static FFMPEG sFFMPEG;

    private FFMPEG() {
    }

    public static FFMPEG getDefault() {
        if (sFFMPEG == null) {
            synchronized (FFMPEG.class) {
                if (sFFMPEG == null) {
                    sFFMPEG = new FFMPEG();
                }
            }
        }
        return sFFMPEG;
    }

    private AudioTrack mAudioTrack;
    public static final float VOLUME_NORMAL = 1.0f;
    public static final float VOLUME_MUTE = 0.0f;

    // 更新视频流下载量
    public static final JniObject videoProducer = JniObject.obtain();
    // 更新视频流消耗量
    public static final JniObject videoConsumer = JniObject.obtain();
    public static final JniObject audioProducer = JniObject.obtain();
    public static final JniObject audioConsumer = JniObject.obtain();
    // 更新进度
    public static final JniObject processUpdate = JniObject.obtain();

    public static final int USE_MODE_MEDIA = 1;
    public static final int USE_MODE_ONLY_VIDEO = 2;
    public static final int USE_MODE_ONLY_AUDIO = 3;
    public static final int USE_MODE_AUDIO_VIDEO = 4;
    public static final int USE_MODE_AAC_H264 = 5;
    public static final int USE_MODE_MEDIA_4K = 6;
    public static final int USE_MODE_MEDIA_MEDIACODEC = 7;

    // 0(开始下载,边播放边下) 1(停止下载) 2(只下载音频,暂时不用) 3(只下载视频,暂时不用)
    // 4(只下载,不播放.不调用seekTo) 5(只提取音视频,不播放.调用seekTo到0)
    public static final int DO_SOMETHING_CODE_init = 1099;
    public static final int DO_SOMETHING_CODE_setMode = 1100;
    //public static final int DO_SOMETHING_CODE_setCallback = 1101;
    public static final int DO_SOMETHING_CODE_setSurface = 1102;
    public static final int DO_SOMETHING_CODE_initPlayer = 1103;
    public static final int DO_SOMETHING_CODE_readData = 1104;
    public static final int DO_SOMETHING_CODE_audioHandleData = 1105;
    public static final int DO_SOMETHING_CODE_videoHandleData = 1106;
    public static final int DO_SOMETHING_CODE_play = 1107;
    public static final int DO_SOMETHING_CODE_pause = 1108;
    public static final int DO_SOMETHING_CODE_stop = 1109;
    public static final int DO_SOMETHING_CODE_release = 1110;
    public static final int DO_SOMETHING_CODE_isRunning = 1111;
    public static final int DO_SOMETHING_CODE_isPlaying = 1112;
    public static final int DO_SOMETHING_CODE_isPausedForUser = 1113;
    public static final int DO_SOMETHING_CODE_stepAdd = 1114;
    public static final int DO_SOMETHING_CODE_stepSubtract = 1115;
    // 单位: 秒
    public static final int DO_SOMETHING_CODE_seekTo = 1116;
    // 单位: 秒
    public static final int DO_SOMETHING_CODE_getDuration = 1117;
    public static final int DO_SOMETHING_CODE_download = 1118;
    public static final int DO_SOMETHING_CODE_closeJni = 1119;
    public static final int DO_SOMETHING_CODE_videoHandleRender = 1120;
    public static final int DO_SOMETHING_CODE_handleAudioOutputBuffer = 1121;
    public static final int DO_SOMETHING_CODE_handleVideoOutputBuffer = 1122;

    private byte[] eof = new byte[]{-1, -1, -1, -1, -1};

    public void releaseAll() {
        sampleRateInHz = 0;
        channelCount = 0;
        audioFormat = 0;
        if (AudioClient.mIsConnected) {
            ThreadPool.getFixedThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    AudioClient.getInstance().sendPcmData(eof, 0, 5);
                    AudioClient.getInstance().close();
                }
            });
        }
        if (mFfmpegUseMediaCodecDecode != null) {
            mFfmpegUseMediaCodecDecode.destroy();
        }
        onTransact(DO_SOMETHING_CODE_release, null);
        MediaUtils.releaseAudioTrack(mAudioTrack);
    }

    private FfmpegUseMediaCodecDecode mFfmpegUseMediaCodecDecode;

    public void setFfmpegUseMediaCodecDecode(FfmpegUseMediaCodecDecode decode) {
        mFfmpegUseMediaCodecDecode = decode;
    }

    // 供jni层调用
    private boolean initMediaCodec(int type, JniObject jniObject) {
        if (mFfmpegUseMediaCodecDecode != null) {
            switch (type) {
                case FfmpegUseMediaCodecDecode.TYPE_AUDIO:
                    //return mFfmpegUseMediaCodecDecode.initAudioMediaCodec(jniObject);
                case FfmpegUseMediaCodecDecode.TYPE_VIDEO:
                    return mFfmpegUseMediaCodecDecode.initVideoMediaCodec(jniObject);
                default:
                    break;
            }
        }
        return false;
    }

    // 供jni层调用
    private boolean feedInputBufferAndDrainOutputBuffer(
            int type, byte[] data, int size, long presentationTimeUs) {
        if (mFfmpegUseMediaCodecDecode != null) {
            switch (type) {
                case FfmpegUseMediaCodecDecode.TYPE_AUDIO:
                    mFfmpegUseMediaCodecDecode.mAudioWrapper.data = data;
                    mFfmpegUseMediaCodecDecode.mAudioWrapper.size = size;
                    mFfmpegUseMediaCodecDecode.mAudioWrapper.sampleTime = presentationTimeUs;
                    return mFfmpegUseMediaCodecDecode.feedInputBufferAndDrainOutputBuffer(
                            mFfmpegUseMediaCodecDecode.mAudioWrapper);
                case FfmpegUseMediaCodecDecode.TYPE_VIDEO:
                    mFfmpegUseMediaCodecDecode.mVideoWrapper.data = data;
                    mFfmpegUseMediaCodecDecode.mVideoWrapper.size = size;
                    mFfmpegUseMediaCodecDecode.mVideoWrapper.sampleTime = presentationTimeUs;
                    return mFfmpegUseMediaCodecDecode.feedInputBufferAndDrainOutputBuffer(
                            mFfmpegUseMediaCodecDecode.mVideoWrapper);
                default:
                    break;

            }
        }
        return false;
    }

    // 给AudioServer使用
    public int sampleRateInHz;
    public int channelCount;
    public int audioFormat;

    // 供jni层调用(不要改动方法名称,如改动了,jni层也要改动)
    private void createAudioTrack(int sampleRateInHz,
                                  int channelCount,
                                  int audioFormat) {
        // sampleRateInHz: 44100 channelCount: 2 audioFormat: 2
        MLog.i(TAG, "createAudioTrack()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);
        this.sampleRateInHz = sampleRateInHz;
        this.channelCount = channelCount;
        this.audioFormat = audioFormat;

        if (AudioClient.getInstance().connect()) {
            AudioClient.getInstance().startAudioServer();
            AudioClient.getInstance().sendAudioTrackInfo(sampleRateInHz, channelCount, audioFormat);
        }

        MediaUtils.releaseAudioTrack(mAudioTrack);
        MLog.i(TAG, "createAudioTrack() start");

        // AudioTrack: releaseBuffer() track 0xe55f0a00
        // disabled due to previous underrun,

        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);

        if (mAudioTrack != null) {
            if (mContext != null) {
                SharedPreferences sp =
                        mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean isMute = sp.getBoolean(PLAYBACK_IS_MUTE, false);
                if (!isMute) {
                    setVolume(VOLUME_NORMAL);
                } else {
                    setVolume(VOLUME_MUTE);
                }
            } else {
                setVolume(VOLUME_NORMAL);
            }

            mAudioTrack.play();
        } else {
            MLog.i(TAG, "createAudioTrack() mAudioTrack is null");
        }
        MLog.i(TAG, "createAudioTrack() end");
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
        if (AudioClient.mIsConnected) {
            AudioClient.getInstance().sendPcmData(audioData, offsetInBytes, sizeInBytes);
        }
        if (mAudioTrack != null
                && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
        }
    }

    // 供jni层调用
    private void sleep(long ms) {
        SystemClock.sleep(ms);
    }

    public void setVolume(float volume) {
        if (mAudioTrack == null
                || mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return;
        }
        if (volume < 0 || volume > 1.0f) {
            volume = VOLUME_NORMAL;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mAudioTrack.setVolume(volume);
        } else {
            mAudioTrack.setStereoVolume(volume, volume);
        }
    }

    private Handler mUiHandler;
    private Context mContext;

    public void setHandler(Handler handler) {
        mUiHandler = handler;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    // 供jni层调用(底层信息才是通过这个接口反映到java层的)
    public com.weidi.usefragments.tool.Callback mCallback =
            new com.weidi.usefragments.tool.Callback() {
                @Override
                public int onTransact(int code, Parcel data, Parcel reply) {
                    return 0;
                }

                @Override
                public int onTransact(int code, JniObject jniObject) {
                    //MLog.i(TAG, "onTransact() code: " + code + " " + jniObject.toString());
                    if (mUiHandler != null) {
                        Message msg = mUiHandler.obtainMessage();
                        msg.what = code;
                        msg.obj = jniObject;
                        mUiHandler.removeMessages(code);
                        mUiHandler.sendMessage(msg);
                    }
                    return 0;
                }

                @Override
                public void onReady() {
                    MLog.i(TAG, "FFMPEG onReady()");
                    if (mUiHandler != null) {
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_READY);
                        mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_READY);
                    }
                }

                @Override
                public void onChangeWindow(int width, int height) {
                    MLog.i(TAG, "FFMPEG onChangeWindow() width: " + width + " height: " + height);
                    if (mUiHandler != null) {
                        Message msg = mUiHandler.obtainMessage();
                        msg.what = Callback.MSG_ON_TRANSACT_CHANGE_WINDOW;
                        msg.arg1 = width;
                        msg.arg2 = height;
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_CHANGE_WINDOW);
                        mUiHandler.sendMessage(msg);
                    }
                }

                @Override
                public void onPlayed() {
                    MLog.i(TAG, "FFMPEG onPlayed()");
                    if (mUiHandler != null) {
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PLAYED);
                        mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_PLAYED);
                    }
                }

                @Override
                public void onPaused() {
                    MLog.i(TAG, "FFMPEG onPaused()");
                    if (mUiHandler != null) {
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PAUSED);
                        mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_PAUSED);
                    }
                }

                @Override
                public void onFinished() {
                    MLog.i(TAG, "FFMPEG onFinished()");
                    if (mUiHandler != null) {
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_FINISHED);
                        mUiHandler.sendEmptyMessage(Callback.MSG_ON_TRANSACT_FINISHED);
                    }
                }

                @Override
                public void onProgressUpdated(long presentationTime) {
                    // 视频时长小于0时(如直播节目),不回调
                    if (mUiHandler != null) {
                        Message msg = mUiHandler.obtainMessage();
                        msg.what = Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED;
                        msg.obj = presentationTime;
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED);
                        mUiHandler.sendMessage(msg);
                    }
                }

                @Override
                public void onError(int error, String errorInfo) {
                    MLog.e(TAG, "FFMPEG onError() error: " + error + " errorInfo: " + errorInfo);
                    if (mUiHandler != null) {
                        Message msg = mUiHandler.obtainMessage();
                        msg.what = Callback.MSG_ON_TRANSACT_ERROR;
                        msg.arg1 = error;
                        msg.obj = errorInfo;
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_ERROR);
                        mUiHandler.sendMessage(msg);
                    }
                }

                @Override
                public void onInfo(String info) {
                    MLog.i(TAG, "FFMPEG onInfo() info: " + info);
                    if (mUiHandler != null) {
                        Message msg = mUiHandler.obtainMessage();
                        msg.what = Callback.MSG_ON_TRANSACT_INFO;
                        msg.obj = info;
                        mUiHandler.removeMessages(Callback.MSG_ON_TRANSACT_INFO);
                        mUiHandler.sendMessage(msg);
                    }
                }
            };

    public static void decodeYUV420SP(byte[] rgbBuf, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        if (rgbBuf == null)
            throw new NullPointerException("buffer 'rgbBuf' is null");
        if (rgbBuf.length < frameSize * 3)
            throw new IllegalArgumentException("buffer 'rgbBuf' size "
                    + rgbBuf.length + " < minimum " + frameSize * 3);

        if (yuv420sp == null)
            throw new NullPointerException("buffer 'yuv420sp' is null");

        if (yuv420sp.length < frameSize * 3 / 2)
            throw new IllegalArgumentException("buffer 'yuv420sp' size " + yuv420sp.length
                    + " < minimum " + frameSize * 3 / 2);

        int i = 0, y = 0;
        int uvp = 0, u = 0, v = 0;
        int y1192 = 0, r = 0, g = 0, b = 0;

        for (int j = 0, yp = 0; j < height; j++) {
            uvp = frameSize + (j >> 1) * width;
            u = 0;
            v = 0;
            for (i = 0; i < width; i++, yp++) {
                y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                y1192 = 1192 * y;
                r = (y1192 + 1634 * v);
                g = (y1192 - 833 * v - 400 * u);
                b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgbBuf[yp * 3] = (byte) (r >> 10);
                rgbBuf[yp * 3 + 1] = (byte) (g >> 10);
                rgbBuf[yp * 3 + 2] = (byte) (b >> 10);
            }
        }
    }

}
