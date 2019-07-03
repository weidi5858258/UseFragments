package com.weidi.usefragments.socket;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.weidi.usefragments.media.Frame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/***
 Created by root on 19-7-3.
 */

public class Server {

    private static final String TAG = Server.class.getSimpleName();
    private final static int SERVER_HOST_PORT = 12580;
    private static Server sServer;
    private static ServerSocket sServerSocket;
    // 现在只对应一个客户端
    private Socket mSocket;
    /* 服务器端口 */
    private boolean release;


    public static Server getInstance() {
        if (sServer == null) {
            synchronized (Server.class) {
                if (sServer == null) {
                    sServer = new Server();
                }
            }
        }
        return sServer;
    }

    private static ServerSocket getServerSocket() {
        if (sServerSocket == null) {
            try {
                sServerSocket = new ServerSocket(SERVER_HOST_PORT);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sServerSocket;
    }

    /***
     放到子线程去
     */
    public void connect() {
        try {
            mSocket = getServerSocket().accept();
            if (mSocket != null && mSocket.isConnected()) {
            }
        } catch (java.nio.channels.IllegalBlockingModeException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (mSocket != null && mSocket.isConnected()) {
                mSocket.close();
            }
            getServerSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] readLength() {
        try {
            if (mSocket != null && !mSocket.isConnected()) {
                mSocket = getServerSocket().accept();
            }
            InputStream is = mSocket.getInputStream();
            return readBytes(is, 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] readSPSPPS(int length) {
        try {
            if (mSocket != null && !mSocket.isConnected()) {
                mSocket = getServerSocket().accept();
            }
            InputStream is = mSocket.getInputStream();
            return readBytes(is, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Frame readFrame(int frameLength) {
        try {
            if (mSocket != null && !mSocket.isConnected()) {
                mSocket = getServerSocket().accept();
            }
            InputStream is = mSocket.getInputStream();
            Frame frame = new Frame(readBytes(is, frameLength), 0, frameLength);
            return frame;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从socket读byte数组
     *
     * @param in
     * @param length
     * @return
     */
    public static byte[] readBytes(InputStream in, long length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = 0;
        while (read < length) {
            int cur = 0;
            try {
                cur = in.read(buffer, 0, (int) Math.min(1024, length - read));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (cur < 0) {
                break;
            }
            read += cur;
            baos.write(buffer, 0, cur);
        }
        return baos.toByteArray();
    }

    public boolean hasRelease() {
        return release;
    }

    public void setRelease(boolean release) {
        this.release = release;
    }

    private int bytesToInt(byte[] bytes) {
        int i = 0;
        i = (int) ((bytes[0] & 0xff)
                | ((bytes[1] & 0xff) << 8)
                | ((bytes[2] & 0xff) << 16)
                | ((bytes[3] & 0xff) << 24));
        return i;
    }

    /***
     * 等待客户端连接，解码器配置
     *
     * @return
     */
    public boolean prepare() {
        byte[] mSps = null;
        byte[] mPps = null;
        Surface mSurface = null;
        sServer.connect();
        int width = 1280;
        int height = 720;
        // 首先读取编码的视频的长度和宽度
        /*try {
            width = sServer.readInt();
            height = sServer.readInt();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }*/

        // 编码器那边会先发sps和pps来,头一帧就由sps和pps组成
        int spsLength = bytesToInt(sServer.readLength());
        byte[] sps = sServer.readSPSPPS(spsLength);
        mSps = Arrays.copyOfRange(sps, 4, spsLength);
        int ppsLength = bytesToInt(sServer.readLength());
        byte[] pps = sServer.readSPSPPS(ppsLength);
        mPps = Arrays.copyOfRange(pps, 4, ppsLength);
        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        // 房间的大小要大于等于width * height时,才能一次性存放需要解码的数据
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height);
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
        format.setByteBuffer("csd-0", ByteBuffer.wrap(mSps));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(mPps));
        MediaCodec decoder = null;
        try {
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        decoder.configure(format, mSurface, null, 0);
        decoder.start();
        return true;
    }

    private void decode() {
        MediaCodec decoder = null;
        int TIMEOUT_US = 10000;
        byte[] data = new byte[100000];
        byte[] frameData = new byte[200000];
        boolean isEOS = false;
        int roomIndex = -1;
        ByteBuffer room = null;
        MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
        while (!isEOS) {
            // 判断是否是流的结尾
            roomIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
            if (roomIndex >= 0) {
                int frameLength = bytesToInt(sServer.readLength());
                Frame frame = sServer.readFrame(frameLength);
                room = decoder.getInputBuffer(roomIndex);
                room.clear();
                if (frame == null) {
                    decoder.queueInputBuffer(
                            roomIndex, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isEOS = true;
                    sServer.disconnect();
                } else {
                    room.put(frame.mData, 0, frame.length);
                    room.limit(frame.length);
                    decoder.queueInputBuffer(
                            roomIndex, 0, frame.length, 0, 0);
                }
            }

            roomIndex = decoder.dequeueOutputBuffer(roomInfo, TIMEOUT_US);
            // Log.i(TAG, "video decoding .....");
            while (roomIndex >= 0) {
                room = decoder.getOutputBuffer(roomIndex);
                decoder.releaseOutputBuffer(roomIndex, true);
                roomIndex = decoder.dequeueOutputBuffer(roomInfo, TIMEOUT_US);//
            }
        }

    }

}
