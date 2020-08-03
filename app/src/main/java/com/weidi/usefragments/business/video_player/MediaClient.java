package com.weidi.usefragments.business.video_player;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.AACPlayer;
import com.weidi.usefragments.tool.MLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_IS_MUTE;
import static com.weidi.usefragments.service.DownloadFileService.PREFERENCES_NAME;

public class MediaClient {

    private static final String TAG =
            "player_alexander";

    private volatile static MediaClient sMediaClient;

    private Socket socket;
    private String ip;
    private InputStream inputStream;
    public static boolean mIsConnected = false;

    private MediaCodec mVideoMC;
    private MediaCodec mAudioMC;
    private MediaFormat mVideoMF;
    private MediaFormat mAudioMF;
    private AudioTrack mAudioTrack;

    private MediaClient() {
    }

    public static MediaClient getInstance() {
        if (sMediaClient == null) {
            synchronized (MediaClient.class) {
                if (sMediaClient == null) {
                    sMediaClient = new MediaClient();
                }
            }
        }
        return sMediaClient;
    }

    public void setIp(String ipAddr) {
        ip = ipAddr;
        // ip = "localhost";
        // ip = "192.168.0.100";
    }

    public boolean connect() {
        if (TextUtils.isEmpty(ip)
                || socket != null
                || inputStream != null) {
            Log.i(TAG, "MediaClient ip is empty");
            return false;
        }

        try {
            mIsConnected = false;
            socket = new Socket(ip, MediaServer.PORT);
            inputStream = socket.getInputStream();
            mIsConnected = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "MediaClient connect() failure");
            close();
            return false;
        }

        return true;
    }

    public void playVideo() {
        // MediaCodec

    }

    public void playAudio() {
        Log.i(TAG, "MediaClient playAudio() start");
        if (!mIsConnected) {
            Log.e(TAG, "MediaClient playAudio() isn't connected");
            return;
        }

        mAudioMF = MediaUtils.getAudioDecoderMediaFormat();
        //mAudioMF.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        // Android MediaCodec解码AAC，AudioTrack播放PCM音频
        // https://blog.csdn.net/lavender1626/article/details/80431902
        byte[] csd0 = new byte[]{(byte) 0x12, (byte) 0x10};
        mAudioMF.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));
        mAudioMC = MediaUtils.getAudioDecoderMediaCodec(mAudioMF);
        if (mAudioMC == null) {
            Log.e(TAG, "MediaClient playAudio() mAudioMC is null");
            return;
        }

        /*// 创建AudioTrack
        // 1.
        int sampleRateInHz =
                mAudioMF.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        // 2.
        int channelCount =
                mAudioMF.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        // 3.
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        // sampleRateInHz: 48000 channelCount: 2 audioFormat: 2
        MLog.d(TAG, "MediaClient playAudio()" +
                " sampleRateInHz: " + sampleRateInHz +
                " channelCount: " + channelCount +
                " audioFormat: " + audioFormat);
        // create AudioTrack
        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioTrack != null) {
            mAudioTrack.play();
        } else {
            MLog.e(TAG, "MediaClient playAudio() AudioTrack is null");
            return;
        }*/

        final int AUDIO_FRAME_MAX_LENGTH = mAudioMF.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        final byte[] data = new byte[AUDIO_FRAME_MAX_LENGTH];
        int readCount = 0;
        int count = 0;
        while (mIsConnected) {
            Arrays.fill(data, (byte) 0);
            try {
                readCount = inputStream.read(data, 0, AUDIO_FRAME_MAX_LENGTH);
                if (readCount <= 0) {
                    Log.e(TAG, "MediaClient readCount: " + readCount);
                    break;
                }

                //mAudioTrack.write(data, 0, readCount);

                long presentationTimeUs = System.nanoTime() / 1000;
                EDMediaCodec.feedInputBufferAndDrainOutputBuffer(
                        mCallback,
                        EDMediaCodec.TYPE.TYPE_AUDIO,
                        mAudioMC,
                        data,
                        0,
                        readCount,
                        presentationTimeUs,
                        false,
                        true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "MediaClient " + e.toString());
                break;
            }
        }// while(...) end
        Log.i(TAG, "MediaClient playAudio() end");
    }

    public synchronized void close() {
        if (!mIsConnected
                && socket == null
                && inputStream == null) {
            Log.d(TAG, "MediaClient close() return");
            return;
        }

        Log.i(TAG, "MediaClient close() start");
        mIsConnected = false;
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.shutdownInput();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.shutdownOutput();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MediaUtils.releaseMediaCodec(mVideoMC);
        MediaUtils.releaseMediaCodec(mAudioMC);
        MediaUtils.releaseAudioTrack(mAudioTrack);
        inputStream = null;
        socket = null;
        mVideoMC = null;
        mAudioMC = null;
        mAudioTrack = null;
        Log.i(TAG, "MediaClient close() end");
    }

    private EDMediaCodec.Callback mCallback = new EDMediaCodec.Callback() {
        @Override
        public boolean isVideoFinished() {
            return !mIsConnected;
        }

        @Override
        public boolean isAudioFinished() {
            return !mIsConnected;
        }

        @Override
        public void handleVideoOutputFormat(MediaFormat mediaFormat) {
            try {
                MediaFormat newMediaFormat = mediaFormat;
                Class clazz = Class.forName("android.media.MediaFormat");
                Method method = clazz.getDeclaredMethod("getMap");
                method.setAccessible(true);
                Object newObject = method.invoke(newMediaFormat);
                Object oldObject = method.invoke(mVideoMF);
                if (newObject != null
                        && newObject instanceof Map
                        && oldObject != null
                        && oldObject instanceof Map) {
                    Map<String, Object> newMap = (Map) newObject;
                    Map<String, Object> oldMap = (Map) oldObject;
                    if (oldMap.containsKey("mime-old")) {
                        return;
                    }
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                MLog.w(TAG, "handleVideoOutputFormat() newMediaFormat: \n" + mVideoMF);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleAudioOutputFormat(MediaFormat mediaFormat) {
            try {
                MediaFormat newMediaFormat = mediaFormat;
                Class clazz = Class.forName("android.media.MediaFormat");
                Method method = clazz.getDeclaredMethod("getMap");
                method.setAccessible(true);
                Object newObject = method.invoke(newMediaFormat);
                Object oldObject = method.invoke(mAudioMF);
                if (newObject != null
                        && newObject instanceof Map
                        && oldObject != null
                        && oldObject instanceof Map) {
                    Map<String, Object> newMap = (Map) newObject;
                    Map<String, Object> oldMap = (Map) oldObject;
                    if (oldMap.containsKey("mime-old")) {
                        return;
                    }
                    String mime = (String) oldMap.get("mime");
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        oldMap.put(entry.getKey(), entry.getValue());
                    }
                    oldMap.put("mime-old", mime);
                }
                MLog.d(TAG, "handleAudioOutputFormat() newMediaFormat: \n" + mAudioMF);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MediaUtils.releaseAudioTrack(mAudioTrack);

            // 创建AudioTrack
            // 1.
            int sampleRateInHz =
                    mAudioMF.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            // 2.
            int channelCount =
                    mAudioMF.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // 3.
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            if (mAudioMF.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                // 关键参数(需要解码后才能知道)
                audioFormat = mAudioMF.getInteger(MediaFormat.KEY_PCM_ENCODING);
            }

            // sampleRateInHz: 48000 channelCount: 2 audioFormat: 2
            MLog.d(TAG, "handleAudioOutputFormat()" +
                    " sampleRateInHz: " + sampleRateInHz +
                    " channelCount: " + channelCount +
                    " audioFormat: " + audioFormat);

            // create AudioTrack
            mAudioTrack = MediaUtils.createAudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRateInHz, channelCount, audioFormat,
                    AudioTrack.MODE_STREAM);
            if (mAudioTrack != null) {
                mAudioTrack.play();
            } else {
                MLog.e(TAG, "handleAudioOutputFormat() AudioTrack is null");
                handleAudioOutputBuffer(-1, null, null, -1);
                return;
            }

            mAudioMF.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mAudioMF.setInteger(MediaFormat.KEY_PRIORITY, 0);
            if (AACPlayer.sampleRateIndexMap.containsKey(sampleRateInHz)
                    && AACPlayer.channelConfigIndexMap.containsKey(channelCount)) {
                List<byte[]> list = new ArrayList<>();
                list.add(MediaUtils.buildAacAudioSpecificConfig(
                        AACPlayer.sampleRateIndexMap.get(sampleRateInHz),
                        AACPlayer.channelConfigIndexMap.get(channelCount)));
                MediaUtils.setCsdBuffers(mAudioMF, list);
            }
        }

        @Override
        public int handleVideoOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {

            return 0;
        }

        @Override
        public int handleAudioOutputBuffer(int roomIndex, ByteBuffer room,
                                           MediaCodec.BufferInfo roomInfo, int roomSize) {
            if (mIsConnected
                    && mAudioTrack != null
                    && roomSize > 0) {
                byte[] audioData = new byte[roomSize];
                room.get(audioData, 0, audioData.length);
                mAudioTrack.write(audioData, roomInfo.offset, audioData.length);
            }

            return 0;
        }
    };

}
