package com.weidi.usefragments.business.video_player;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.utils.MyToast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/***
 数据是由客户端传递给服务端的,在服务端进行播放的
 */
public class AudioServer {

    private static final String TAG =
            "player_alexander";

    public static final int PORT = 8889;
    public static final String AUDIOTRACK_INFO_TAG = "@@@@@@";

    private volatile static AudioServer sAudioServer;

    private ServerSocket server;
    private Socket socket;
    private InputStream inputStream;

    private AudioTrack mAudioTrack;
    private boolean mIsReading;

    private AudioServer() {
    }

    public static AudioServer getInstance() {
        if (sAudioServer == null) {
            synchronized (AudioServer.class) {
                if (sAudioServer == null) {
                    sAudioServer = new AudioServer();
                }
            }
        }
        return sAudioServer;
    }

    public void sccept() {
        if (server != null
                || socket != null
                || inputStream != null) {
            Log.d(TAG, "AudioServer sccept() return");
            return;
        }

        try {
            server = new ServerSocket(PORT);
            Log.i(TAG, "AudioServer sccept() start");
            socket = server.accept();
            Log.i(TAG, "AudioServer sccept() end");
            inputStream = socket.getInputStream();
            int ret = inputStream.read();
            Log.i(TAG, "AudioServer sccept() ret: " + ret);
            if (ret >= 0) {
                recePcmData();
            }
        } catch (Exception e) {
            e.printStackTrace();
            close(true);
        }
    }

    public void recePcmData() {
        if (mIsReading
                || socket == null
                || inputStream == null
                || !socket.isConnected()
                || socket.isClosed()) {
            Log.i(TAG, "AudioServer recePcmData() return");
            return;
        }

        try {
            SystemClock.sleep(5000);
            Log.i(TAG, "AudioServer recePcmData()");
            mIsReading = true;
            //inputStream = socket.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String info = bufferedReader.readLine();
            if (TextUtils.isEmpty(info)
                    || !info.contains(AUDIOTRACK_INFO_TAG)) {
                close(true);
                return;
            }

            String[] infos = info.split(AUDIOTRACK_INFO_TAG);
            int sampleRateInHz = Integer.parseInt(infos[0]);
            int channelCount = Integer.parseInt(infos[1]);
            int audioFormat = Integer.parseInt(infos[2]);

            Log.i(TAG, "AudioServer" +
                    " sampleRateInHz: " + sampleRateInHz +
                    " channelCount: " + channelCount +
                    " audioFormat: " + audioFormat);
            MediaUtils.releaseAudioTrack(mAudioTrack);
            mAudioTrack = MediaUtils.createAudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRateInHz, channelCount, audioFormat,
                    AudioTrack.MODE_STREAM);
            if (mAudioTrack == null
                    || mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
                Log.e(TAG, "AudioServer mAudioTrack is null");
                close(true);
                return;
            }
            mAudioTrack.play();
            Log.i(TAG, "AudioServer mAudioTrack.play()");
        } catch (Exception e) {
            e.printStackTrace();
            close(true);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final int AUDIO_FRAME_MAX_LENGTH = 102400;
                final byte[] pcmData = new byte[AUDIO_FRAME_MAX_LENGTH];
                int readCount = 0;
                int count = 0;
                while (mIsReading) {
                    Arrays.fill(pcmData, (byte) 0);
                    try {
                        readCount = inputStream.read(pcmData, 0, AUDIO_FRAME_MAX_LENGTH);
                        if (readCount > 0) {
                            if (pcmData[0] == -1
                                    && pcmData[1] == -1
                                    && pcmData[2] == -1
                                    && pcmData[3] == -1
                                    && pcmData[4] == -1) {
                                Log.e(TAG, "AudioServer readCount: " + readCount);
                                break;
                            }
                            if (++count <= 250) {
                                continue;
                            }
                            write(pcmData, 0, readCount);
                        } else {
                            Log.e(TAG, "AudioServer readCount: " + readCount);
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "AudioServer " + e.toString());
                        break;
                    }
                }// while(...) end

                close(true);
            }
        }).start();
    }

    public void setIsReading(boolean isReading) {
        mIsReading = isReading;
    }

    public synchronized void close(boolean needToAccept) {
        if (!mIsReading
                && server == null
                && socket == null
                && inputStream == null) {
            Log.d(TAG, "AudioServer close() return");
            return;
        }

        Log.i(TAG, "AudioServer close() start");
        mIsReading = false;
        MediaUtils.releaseAudioTrack(mAudioTrack);
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
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
            socket = null;
        }
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server = null;
        }
        Log.i(TAG, "AudioServer close() end");

        if (needToAccept) {
            ThreadPool.getFixedThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    MyToast.show("AudioServer sccept");
                    sccept();
                }
            });
        }
    }

    private void write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if (mAudioTrack != null
                && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            /*Log.i(TAG, "write()" +
                    " sizeInBytes: " + sizeInBytes);*/
            mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
        }
    }

}
