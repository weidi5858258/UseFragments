package com.weidi.usefragments.tool;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Surface;

import com.weidi.usefragments.media.MediaUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 Created by root on 19-7-18.
 */

public class H264Player {
    private static final String TAG =
            H264Player.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int CACHE = 1024 * 1024 * 4;
    private static final int TIME_INTERNAL = 5;
    private int mFrameCounts;
    private InputStream mInputStream;
    private boolean mIsReading = false;
    private boolean mIsHandling = false;
    private boolean mIsLocalFile = true;
    private boolean mData1HasData = false;
    private byte[] mData1;
    private byte[] mData2;
    private Object mReadDataLock = new Object();
    private int readDataSize = -1;
    private int lastOffsetIndex = -1;
    private String mPath;
    private MediaCodec mVideoDecoderMediaCodec;
    private MediaFormat mVideoDecoderMediaFormat;
    private Surface mSurface;

    public H264Player() {
        init();
    }

    public void setPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mPath = path;
        mPath = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/video2.h264";
        mPath = "http://192.168.1.113:8080/tomcat_video/video2.h264";
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void start() {
        new Thread(mReadData).start();
    }

    private void init() {
        mData1 = new byte[CACHE];
        mData2 = new byte[CACHE];
    }

    private void prepareMediaFormat() {
        // http://192.168.1.113:8080/tomcat_video/video2.h264
        int width = 1024;
        int height = 576;
        MediaFormat format = MediaFormat.createVideoFormat(MediaUtils.VIDEO_MIME, width, height);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 154709);
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, width);
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, height);
        format.setInteger(MediaFormat.KEY_LEVEL, 512);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        format.setInteger(MediaFormat.KEY_PROFILE, 2);
        format.setLong(MediaFormat.KEY_DURATION, 3256503250L);
        byte[] csd_0 = new byte[]{
                0, 0, 0, 1, 103, 77, 64, 31,
                -24, -128, 32, 2, 77, -128, -87, 1,
                1, 1, 64, 0, 0, -6, 64, 0,
                46, -32, 58, -128, 2, 113, 0, 0,
                -101, -30, 36, -61, 0, -8, -63, -120, -112};
        byte[] csd_1 = new byte[]{0, 0, 0, 1, 104, -21, -20, -78};
        format.setByteBuffer("csd-0", ByteBuffer.wrap(csd_0));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(csd_1));
        mVideoDecoderMediaFormat = format;
    }

    private boolean prepareInputStream() {
        if (!mPath.endsWith(".h264")
                && !mPath.endsWith(".H264")) {
            return false;
        }

        mIsLocalFile = true;
        if (mPath.startsWith("http")
                || mPath.startsWith("https")
                || mPath.startsWith("HTTP")
                || mPath.startsWith("HTTPS")) {
            try {
                URL url = new URL(mPath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // 设置本次请求的方式,默认是GET方式,参数要求都是大写字母
                conn.setRequestMethod("GET");
                // 设置连接超时
                conn.setConnectTimeout(5000);
                // 是否打开输入流 ， 此方法默认为true
                conn.setDoInput(true);
                // 是否打开输出流， 此方法默认为false
                conn.setDoOutput(true);
                // 表示连接
                conn.connect();
                int code = conn.getResponseCode();
                if (code == 200) {
                    mInputStream = conn.getInputStream();
                    mIsLocalFile = false;
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mInputStream = null;
            }
        } else {
            File file = new File(mPath);
            if (!file.exists()
                    || !file.canRead()) {
                return false;
            }
            long length = file.length();
            MLog.d(TAG, "setPath() fileLength: " + length);
            try {
                mInputStream = new FileInputStream(file);
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                mInputStream = null;
            }
        }
        return false;
    }

    private boolean prepareMediaCodec() {
        if (DEBUG)
            MLog.d(TAG, "prepareMediaCodec() start");
        mVideoDecoderMediaCodec = null;
        if (mSurface == null) {
            return false;
        }

        String path = "/storage/2430-1702/BaiduNetdisk/video/05.mp4";
        MediaExtractor videoExtractor = new MediaExtractor();
        try {
            videoExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        String videoMime = null;
        int videoTrackIndex = -1;
        int trackCount = videoExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            //mVideoDecoderMediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mVideoDecoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                videoMime = mime;
                videoTrackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(videoMime)
                || videoTrackIndex == -1) {
            return false;
        }

        MLog.d(TAG, "internalPrepare() video mime: " + videoMime);
        MLog.d(TAG, "internalPrepare() mVideoDecoderMediaFormat: " +
                mVideoDecoderMediaFormat.toString());
        try {
            videoExtractor.selectTrack(videoTrackIndex);
            mVideoDecoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            videoMime, mVideoDecoderMediaFormat, mSurface);
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException e) {
            e.printStackTrace();
            if (mVideoDecoderMediaCodec != null) {
                mVideoDecoderMediaCodec.release();
            }
            mVideoDecoderMediaCodec = null;
        }
        if (mVideoDecoderMediaCodec == null) {
            return false;
        }

        if (DEBUG)
            MLog.d(TAG, "prepareMediaCodec() end");
        return true;
    }

    private void readData() {
        if (DEBUG)
            MLog.w(TAG, "readData() start");

        if (!prepareInputStream()) {
            MLog.e(TAG, "readData() prepareInputStream failed");
            return;
        }

        prepareMediaFormat();

        if (!prepareMediaCodec()) {
            MLog.e(TAG, "readData() prepareMediaCodec failed");
            return;
        }

        // 测试了一下,好像最大只能读取8192个byte
        int bufferLength = 1024 * 1024;
        byte[] buffer = new byte[bufferLength];
        // mData1中保存的实际数据大小
        readDataSize = 0;
        int readTotalSize = 0;
        Arrays.fill(mData1, (byte) 0);
        /***
         三种情况退出循环:
         1.异常
         2.要处理的数据有问题时不能继续往下走,然后通知这里结束
         3.readSize < 0
         */
        mIsReading = true;
        while (mIsReading) {
            try {
                Arrays.fill(buffer, (byte) 0);
                // httpAccessor ---> mData1
                int readSize = mInputStream.read(buffer, 0, bufferLength);
                if (readSize < 0) {
                    MLog.i(TAG, "readData()     readSize: " + readSize);
                    MLog.i(TAG, "readData() readDataSize: " + readDataSize);
                    // 开启任务处理数据(如果还没有开启任务过的话)
                    // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                    if (!mIsHandling) {
                        mIsHandling = true;
                        new Thread(mHandleData).start();
                    }
                    mIsReading = false;
                    break;
                }
                readTotalSize += readSize;
                if (readTotalSize <= CACHE) {
                    // buffer ---> mData1
                    System.arraycopy(buffer, 0,
                            mData1, readDataSize, readSize);
                    readDataSize += readSize;
                    continue;
                } else {
                    MLog.i(TAG, "readData() readDataSize: " + readDataSize);
                    // 开启任务处理数据
                    // 如果任务是在这里被开启的,那么说明网络文件长度大于CACHE
                    if (!mIsHandling) {
                        mIsHandling = true;
                        new Thread(mHandleData).start();
                    }
                    // wait
                    synchronized (mReadDataLock) {
                        MLog.i(TAG, "readData() mReadDataLock.wait() start");
                        try {
                            mReadDataLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        MLog.i(TAG, "readData() mReadDataLock.wait() end");
                    }
                    MLog.i(TAG, "readData() 继续读取数据 mData1HasData: " + mData1HasData);
                    if (!mData1HasData) {
                        Arrays.fill(mData1, (byte) 0);
                        readDataSize = readSize;
                        readTotalSize = readSize;
                        // 此时buffer中还有readSize个byte
                        System.arraycopy(buffer, 0,
                                mData1, 0, readSize);
                    } else {
                        mData1HasData = false;
                        readDataSize = readSize + (CACHE - lastOffsetIndex);
                        readTotalSize = readDataSize;
                        // 此时buffer中还有readSize个byte
                        System.arraycopy(buffer, 0,
                                mData1, CACHE - lastOffsetIndex, readSize);
                    }
                }
            } catch (IOException
                    | NullPointerException e) {
                e.printStackTrace();
                mIsReading = false;
                break;
            }
        }// while(...) end

        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (DEBUG)
            MLog.w(TAG, "readData() end");
    }

    private void handleData() {
        if (DEBUG)
            MLog.w(TAG, "handleData() start");

        // mData2中剩下的数据大小
        ArrayList<Integer> offsetList = new ArrayList<Integer>();
        long frameTotal = 0;
        int preReadDataSize = 0;
        int restOfDataSize = 0;
        int frameDataLength = 1024 * 100;
        byte[] frameData = new byte[frameDataLength];
        // mData1 ---> mData2
        System.arraycopy(
                mData1, 0,
                mData2, 0, readDataSize);

        long startDecodeTime = System.nanoTime();
        startTime = System.currentTimeMillis();
        boolean isHandlingData = true;
        while (isHandlingData) {
            MLog.d(TAG, "handleData() findHead start");
            if (readDataSize == CACHE
                    || readDataSize == lastOffsetIndex) {
                findHead(mData2, 0, CACHE, offsetList);
            } else {
                findHead(mData2, 0, readDataSize, offsetList);
            }
            MLog.d(TAG, "handleData() findHead end");
            int offsetCounts = offsetList.size();
            MLog.i(TAG, "handleData() findHead    offsetCounts: " + offsetCounts);
            if (offsetCounts > 1) {
                preReadDataSize = readDataSize;
                lastOffsetIndex = offsetList.get(offsetCounts - 1);
                restOfDataSize = readDataSize - lastOffsetIndex;
                MLog.i(TAG, "handleData() findHead    readDataSize: " + readDataSize);
                MLog.i(TAG, "handleData() findHead lastOffsetIndex: " + lastOffsetIndex);
                MLog.i(TAG, "handleData() findHead  restOfDataSize: " + restOfDataSize);
            } else {
                StringBuilder sb = new StringBuilder();
                for (byte bt : mData2) {
                    sb.append(" ");
                    sb.append(bt);
                }
                MLog.d(TAG, "handleData() " + sb.toString());

                break;
            }
            if (mIsReading) {
                // 此处发送消息后,readDataSize的大小可能会变化
                synchronized (mReadDataLock) {
                    MLog.i(TAG, "handleData() findHead mReadDataLock.notify()");
                    mReadDataLock.notify();
                }
            }

            for (int i = 0; i < offsetCounts; i++) {
                Arrays.fill(frameData, (byte) 0);
                if (i + 1 < offsetCounts) {
                    /***
                     集合中至少有两个offset才有一帧输出
                     各帧之间的offset很重要,比如有:0, 519, 1038, 1585, 2147 ...
                     知道了offset,那么就知道了要"喂"多少数据了.
                     两个offset的位置一减就是一帧的长度
                     */
                    int frameLength = offsetList.get(i + 1) - offsetList.get(i);
                    frameTotal += frameLength;
                    if (frameLength > frameDataLength) {
                        MLog.d(TAG, "handleData() 出现大体积帧 frameLength: " + frameLength);
                        frameDataLength = frameLength;
                        frameData = new byte[frameLength];
                    }
                    // frameData保存着一帧的数据(包括ADTS头和AAC ES(AAC音频数据))
                    System.arraycopy(
                            mData2, offsetList.get(i),
                            frameData, 0, frameLength);
                    /*MLog.d(TAG, "handleData() offset: " + offsetList.get(i) +
                            "    " + frameData[0] +
                            " " + frameData[1] +
                            " " + frameData[2] +
                            " " + frameData[3]);
                    MLog.d(TAG, "handleData() frameLength: " + frameLength);*/

                    feedInputBufferAndDrainOutputBuffer(
                            frameData, 0, frameLength, startDecodeTime);
                } else {
                    // 处理集合中最后一个offset的位置
                    MLog.i(TAG, "handleData() preReadDataSize: " + preReadDataSize);
                    MLog.i(TAG, "handleData()    readDataSize: " + readDataSize);
                    MLog.i(TAG, "handleData() lastOffsetIndex: " + lastOffsetIndex);
                    MLog.i(TAG, "handleData()  restOfDataSize: " + restOfDataSize);
                    if (restOfDataSize > 0) {
                        System.arraycopy(
                                mData2, lastOffsetIndex,
                                frameData, 0, restOfDataSize);
                    }

                    // mData1中还有数据
                    if (readDataSize != CACHE
                            && preReadDataSize == readDataSize) {
                        // 最后一帧
                        if (CACHE >= 1024 * 1024) {
                            int frameLength = restOfDataSize;
                            MLog.d(TAG, "handleData() last     offset: " + 0 +
                                    "    " + frameData[0] +
                                    " " + frameData[1] +
                                    " " + frameData[2] +
                                    " " + frameData[3]);
                            MLog.d(TAG, "handleData()     frameLength: " + frameLength);
                            frameTotal += frameLength;
                            feedInputBufferAndDrainOutputBuffer(
                                    frameData, 0, frameLength, startDecodeTime);

                            isHandlingData = false;
                        }
                        break;
                    } else {
                        // 把mData2中剩下的数据移动到mData2的开头
                        Arrays.fill(mData2, (byte) 0);
                        System.arraycopy(
                                frameData, 0,
                                mData2, 0, restOfDataSize);
                        // mData1 ---> mData2
                        if (readDataSize + restOfDataSize <= CACHE) {
                            System.arraycopy(mData1, 0,
                                    mData2, restOfDataSize, readDataSize);
                        } else {
                            mData1HasData = true;
                            // mData2已填满数据
                            System.arraycopy(mData1, 0,
                                    mData2, restOfDataSize, CACHE - restOfDataSize);
                            // mData1还剩下(CACHE - lastOffsetIndex)个字节
                            byte[] buffer = new byte[CACHE - lastOffsetIndex];
                            System.arraycopy(mData1, lastOffsetIndex,
                                    buffer, 0, CACHE - lastOffsetIndex);
                            Arrays.fill(mData1, (byte) 0);
                            System.arraycopy(buffer, 0,
                                    mData1, 0, CACHE - lastOffsetIndex);
                        }
                        break;
                    }
                }
            }// for(...) end
        }// while(true) end

        if (mIsReading) {
            synchronized (mReadDataLock) {
                mReadDataLock.notify();
            }
        }
        mIsReading = false;

        MLog.w(TAG, "handleData()      frameTotal: " + frameTotal);

        if (DEBUG)
            MLog.w(TAG, "handleData() end");
    }

    private long startTime;

    private void feedInputBufferAndDrainOutputBuffer(
            byte[] data, int offset, int size, long startTimeUs) {
        /*long presentationTimeUs =
                (System.nanoTime() - startTimeUs) / 1000;*/
        // Input
        MediaUtils.feedInputBuffer(
                mVideoDecoderMediaCodec, data, offset, size,
                mFrameCounts * TIME_INTERNAL);
        mFrameCounts++;
        // Output
        MediaUtils.drainOutputBuffer(
                mVideoDecoderMediaCodec, true, true, callback);

        /*//线程休眠
        sleepThread(startTime, System.currentTimeMillis());
        //重置开始时间
        startTime = System.currentTimeMillis();*/

        /*// Input
        SystemClock.sleep(1);
        // Output
        SystemClock.sleep(1);*/
    }

    /***
     * 寻找指定buffer中H264帧头的开始位置
     *
     * @param offset 开始的位置
     * @param data   数据
     * @param size   需要检测的最大值
     * @return
     */
    private void findHead(byte[] data, int offset, int size, List<Integer> list) {
        list.clear();
        int i = 0;
        for (i = offset; i < size; ) {
            // 发现帧头
            if (isHead(data, i, size)) {
                list.add(i);
                i += 3;
            } else {
                i++;
            }
        }
    }

    /***
     判断h264帧头
     00 00 00 01 65        (I帧)
     00 00 00 01 61 / 41   (P帧)
     */
    private boolean isHead(byte[] buffer, int offset, int size) {
        /*if (offset + 3 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x00
                && buffer[offset + 3] == 0x01) {
            // 00 00 00 01
            return true;
        } else if (offset + 2 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x01) {
            // 00 00 01
            return true;
        }*/

        if (offset + 3 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x00
                && buffer[3] == 0x01
                && isVideoFrameHeadType(buffer[offset + 4])) {
            // 00 00 00 01 x
            return true;
        } else if (offset + 2 < size
                && buffer[offset] == 0x00
                && buffer[offset + 1] == 0x00
                && buffer[offset + 2] == 0x01
                && isVideoFrameHeadType(buffer[offset + 3])) {
            // 00 00 01 x
            return true;
        }

        return false;
    }

    /***
     I帧或者P帧
     */
    private boolean isVideoFrameHeadType(byte head) {
        return head == (byte) 0x65
                || head == (byte) 0x61
                || head == (byte) 0x41;
    }

    // 根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 1000 / 25;

    private void sleepThread(long startTime, long endTime) {
        // 根据读文件和解码耗时,计算需要休眠的时间
        long time = PRE_FRAME_TIME - (endTime - startTime);
        if (time > 0) {
            SystemClock.sleep(time);
        }
    }

    private Runnable mReadData = new Runnable() {
        @Override
        public void run() {
            readData();
        }
    };

    private Runnable mHandleData = new Runnable() {
        @Override
        public void run() {
            handleData();
        }
    };

    private MediaUtils.Callback callback = new MediaUtils.Callback() {
        @Override
        public void onFormatChanged(
                MediaFormat newMediaFormat) {
            mVideoDecoderMediaFormat = newMediaFormat;
            MLog.d(TAG, "callback mVideoDecoderMediaFormat: " +
                    mVideoDecoderMediaFormat.toString());
        }

        @Override
        public void onInputBuffer(
                ByteBuffer room, MediaUtils.InputBufferInfo info) {

        }

        @Override
        public void onOutputBuffer(
                ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {

        }
    };

}
