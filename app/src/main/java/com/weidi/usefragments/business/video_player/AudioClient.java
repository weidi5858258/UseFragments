package com.weidi.usefragments.business.video_player;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class AudioClient {

    private static final String TAG =
            "player_alexander";

    private volatile static AudioClient sAudioClient;

    private Socket socket;
    private String ip;
    private OutputStream outputStream;
    public static boolean mIsConnected = false;

    private AudioClient() {
    }

    public static AudioClient getInstance() {
        if (sAudioClient == null) {
            synchronized (AudioClient.class) {
                if (sAudioClient == null) {
                    sAudioClient = new AudioClient();
                }
            }
        }
        return sAudioClient;
    }

    public void setIp(String ipAddr) {
        ip = ipAddr;
        // ip = "localhost";
        // ip = "192.168.0.100";
    }

    public boolean connect() {
        if (TextUtils.isEmpty(ip)
                || socket != null
                || outputStream != null) {
            Log.i(TAG, "AudioClient ip is empty");
            return false;
        }

        try {
            mIsConnected = false;
            socket = new Socket(ip, AudioServer.PORT);
            outputStream = socket.getOutputStream();
            mIsConnected = true;
        } catch (Exception e) {
            e.printStackTrace();
            close();
            return false;
        }

        return true;
    }

    public void startAudioServer() {
        if (socket == null
                || outputStream == null
                || !socket.isConnected()
                || socket.isClosed()) {
            return;
        }

        try {
            outputStream.write(200);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }
    }

    public void sendAudioTrackInfo(int sampleRateInHz,
                                   int channelCount,
                                   int audioFormat) {
        if (socket == null
                || outputStream == null
                || !socket.isConnected()
                || socket.isClosed()) {
            return;
        }

        try {
            PrintWriter pw = new PrintWriter(outputStream);
            StringBuffer sb = new StringBuffer();
            sb.append(sampleRateInHz);
            sb.append(AudioServer.AUDIOTRACK_INFO_TAG);
            sb.append(channelCount);
            sb.append(AudioServer.AUDIOTRACK_INFO_TAG);
            sb.append(audioFormat);
            sb.append("\n");
            pw.write(sb.toString());
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }
    }

    public void sendPcmData(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if (socket == null
                || outputStream == null
                || !socket.isConnected()
                || socket.isClosed()) {
            return;
        }

        try {
            //OutputStream outputStream = socket.getOutputStream();
            outputStream.write(audioData, offsetInBytes, sizeInBytes);
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }
    }

    public synchronized void close() {
        if (!mIsConnected
                && socket == null
                && outputStream == null) {
            Log.d(TAG, "AudioClient close() return");
            return;
        }

        Log.i(TAG, "AudioClient close() start");
        mIsConnected = false;
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
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
        Log.i(TAG, "AudioClient close() end");
    }

}
