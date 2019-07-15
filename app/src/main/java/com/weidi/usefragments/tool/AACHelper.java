package com.weidi.usefragments.tool;

import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 Created by weidi on 2019/7/15.
 只考虑ADTS头为7个字节(9个字节的不考虑,CRC误码校验)
 */

public class AACHelper {

    private static final String TAG =
            AACHelper.class.getSimpleName();
    private static final boolean DEBUG = true;
    // 2MB
    private static final int CACHE = 1024 * 2;// 1024 * 1024 * 2
    private InputStream mInputStream;
    private boolean mIsWorking = false;
    private byte[] mData1;
    private byte[] mData2;
    private Object mReadDataLock = new Object();
    private int readDataSize = -1;
    private int lastOffsetIndex = -1;

    public AACHelper() {
        init();
    }

    public void setInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        mInputStream = inputStream;
        new Thread(mReadData).start();
    }

    private void init() {
        mData1 = new byte[CACHE];
        mData2 = new byte[CACHE];
    }

    private void readData() {
        if (DEBUG)
            MLog.w(TAG, "readData() start");

        readDataSize = -1;
        lastOffsetIndex = -1;
        while (true) {
            try {
                if (mInputStream.available() <= 0) {
                    break;
                }

                // mInputStream ---> mData1
                Arrays.fill(mData1, (byte) 0);
                if (lastOffsetIndex == -1) {
                    readDataSize = mInputStream.read(mData1, 0, CACHE);
                    if (readDataSize > 0) {
                        new Thread(mHandleData).start();
                    } else {
                        break;
                    }
                } else {
                    readDataSize = mInputStream.read(mData1, 0, lastOffsetIndex);
                }

                if (DEBUG)
                    MLog.d(TAG, "readData() readDataSize: " + readDataSize);

                synchronized (mReadDataLock) {
                    try {
                        mReadDataLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException
                    | NullPointerException e) {
                e.printStackTrace();
                break;
            }
        }

        try {
            mInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mInputStream = null;
        }

        if (DEBUG)
            MLog.w(TAG, "readData() end");
    }

    private void handleData() {
        if (DEBUG)
            MLog.w(TAG, "handleData() start");

        ArrayList<Integer> offsetList = new ArrayList<Integer>();
        byte[] frameData = new byte[1024];
        boolean isHandlingData = true;
        // mData1 ---> mData2
        System.arraycopy(
                mData1, 0,
                mData2, 0, readDataSize);
        while (isHandlingData) {
            findHead(mData2, 0, readDataSize, offsetList);
            int offsetCount = offsetList.size();
            lastOffsetIndex = offsetList.get(offsetCount - 1);
            synchronized (mReadDataLock) {
                mReadDataLock.notify();
            }

            for (int i = 0; i < offsetCount; i++) {
                Arrays.fill(frameData, (byte) 0);
                if (i + 1 < offsetCount) {
                    /***
                     各帧之间的offset很重要,比如有:0, 519, 1038, 1585, 2147 ...
                     知道了offset,那么就知道了要"喂"多少数据了.
                     */
                    int frameLength = offsetList.get(i + 1) - offsetList.get(i);
                    // frameData保存着一帧的数据(包括ADTS头和AAC ES(AAC音频数据))
                    System.arraycopy(
                            mData2, offsetList.get(i),
                            frameData, 0, frameLength);
                    MLog.d(TAG, "handleData() offset: " + offsetList.get(i) +
                            "    " + frameData[0] +
                            " " + frameData[1] +
                            " " + frameData[2] +
                            " " + frameData[3] +
                            " " + frameData[4] +
                            " " + frameData[5] +
                            " " + frameData[6]);
                    MLog.d(TAG, "handleData() frameLength: " + frameLength);
                    // Input

                } else {
                    // 集合中最后一个offset的位置
                    lastOffsetIndex = offsetList.get(i);
                    MLog.d(TAG, "handleData() lastOffsetIndex: " + lastOffsetIndex);
                    // mData2中剩下的数据大小
                    int restOfDataSize = readDataSize - lastOffsetIndex;
                    System.arraycopy(
                            mData2, lastOffsetIndex,
                            frameData, 0, restOfDataSize);
                    if (readDataSize == CACHE
                            || (readDataSize + restOfDataSize) > (offsetList.get(i) - offsetList.get(i - 1))) {
                        // 把mData2中剩下的数据移动到mData2的开头
                        Arrays.fill(mData2, (byte) 0);
                        System.arraycopy(
                                frameData, 0,
                                mData2, 0, restOfDataSize);
                        offsetList.clear();
                        // mData1 ---> mData2
                        System.arraycopy(mData1, 0,
                                mData2, CACHE - lastOffsetIndex, readDataSize);
                        // mPcmData的实际长度
                        readDataSize += (CACHE - lastOffsetIndex);
                        /*MLog.d(TAG, "playPcm() readSize: " + readSize);
                        MLog.d(TAG, "playPcm() mPcmData.length: " + mPcmData.length);*/
                        break;
                    } else {
                        // 剩下还有一帧
                        MLog.d(TAG, "handleData() last readDataSize: " + readDataSize);
                        MLog.d(TAG, "handleData() last offset: " + 0 +
                                "    " + frameData[0] +
                                " " + frameData[1] +
                                " " + frameData[2] +
                                " " + frameData[3] +
                                " " + frameData[4] +
                                " " + frameData[5] +
                                " " + frameData[6]);
                        // Input

                        isHandlingData = false;
                        synchronized (mReadDataLock) {
                            mReadDataLock.notify();
                        }
                        break;
                    }
                }
            }// for(...) end
        }// while(true) end

        if (DEBUG)
            MLog.w(TAG, "handleData() end");
    }

    /**
     * 寻找指定buffer中AAC帧头的开始位置
     *
     * @param offset 开始的位置
     * @param data   数据
     * @param size   需要检测的最大值
     * @return
     */
    private void findHead(byte[] data, int offset, int size, List<Integer> list) {
        int i = 0;
        for (i = offset; i < size; i++) {
            // 发现帧头
            if (isHead(data, i)) {
                list.add(i);
                i += 7;
            }
        }
    }

    /**
     * 判断aac帧头
     */
    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        if (data[offset] == (byte) 0xFF
                && data[offset + 1] == (byte) 0xF1
                && data[offset + 3] == (byte) 0x80
                && data[offset + 6] == (byte) 0xFC) {
            // -1 -15 80 -128 63 -97 -4
            /*MLog.d(TAG, "onBuffer() offset: " + offset +
                    "     " + data[offset] +
                    " " + data[offset + 1] +
                    " " + data[offset + 2] +
                    " " + data[offset + 3] +
                    " " + data[offset + 4] +
                    " " + data[offset + 5] +
                    " " + data[offset + 6]);*/
            result = true;
        }
        return result;
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

}
