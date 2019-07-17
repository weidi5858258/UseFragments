package com.weidi.usefragments.tool;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;

import com.weidi.usefragments.media.MediaUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 Created by weidi on 2019/7/15.
 只考虑ADTS头为7个字节(9个字节的不考虑,CRC误码校验)
 */

public class AACPlayer {

    private static final String TAG =
            AACPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static Map<Integer, Integer> sampleRateIndexMap = new HashMap<>();
    private static Map<Integer, Integer> channelConfigIndexMap = new HashMap<>();

    static {
        // key是sampleRate
        sampleRateIndexMap.put(96000, 0);
        sampleRateIndexMap.put(88200, 1);
        sampleRateIndexMap.put(64000, 2);
        sampleRateIndexMap.put(48000, 3);
        sampleRateIndexMap.put(44100, 4);
        sampleRateIndexMap.put(32000, 5);
        sampleRateIndexMap.put(24000, 6);
        sampleRateIndexMap.put(22050, 7);
        sampleRateIndexMap.put(16000, 8);
        sampleRateIndexMap.put(12000, 9);
        sampleRateIndexMap.put(11025, 10);
        sampleRateIndexMap.put(8000, 11);
        sampleRateIndexMap.put(7350, 12);
    }

    static {
        // key是channelCount
        channelConfigIndexMap.put(0, 0);// Defined in AOT Specifc Config
        channelConfigIndexMap.put(1, 1);
        channelConfigIndexMap.put(2, 2);
        channelConfigIndexMap.put(3, 3);
        channelConfigIndexMap.put(4, 4);
        channelConfigIndexMap.put(5, 5);
        channelConfigIndexMap.put(6, 6);
        channelConfigIndexMap.put(8, 7);
    }

    // 如果CACHE能够一次性的装下所有数据的话,是没有问题的
    // 2MB
    private static final int CACHE = 1024 * 1024;
    private InputStream mInputStream;
    private boolean mIsReading = false;
    private boolean mIsHandling = false;
    private byte[] mData1;
    private byte[] mData2;
    private Object mReadDataLock = new Object();
    private int readDataSize = -1;
    private int lastOffsetIndex = -1;
    private String mPath;
    private MediaCodec mAudioDecoderMediaCodec;
    private MediaFormat mAudioDecoderMediaFormat;
    private AudioTrack mAudioTrack;
    private float mVolume = 1.0f;
    private boolean mIsLocalFile = true;

    public AACPlayer() {
        init();
    }

    public void setPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mPath = path;
        if (!mPath.endsWith(".aac")
                && !mPath.endsWith(".AAC")) {
            return;
        }

        mIsLocalFile = true;
        if (mPath.startsWith("http")
                || mPath.startsWith("https")
                || mPath.startsWith("HTTP")
                || mPath.startsWith("HTTPS")) {

        } else {
            File file = new File(mPath);
            if (!file.exists()
                    || !file.canRead()) {
                return;
            }
            long length = file.length();
            MLog.d(TAG, "setPath() fileLength: " + length);
            try {
                mInputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                mInputStream = null;
            }
        }
    }

    public void start() {
        if (mInputStream != null) {
            new Thread(mReadLocalData).start();
        } else {
            new Thread(mReadHttpData).start();
        }
    }

    public void setInputStream(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        mInputStream = inputStream;
        new Thread(mReadLocalData).start();
    }

    private void init() {
        mData1 = new byte[CACHE];
        mData2 = new byte[CACHE];
    }

    private boolean prepare() {
        if (DEBUG)
            MLog.d(TAG, "prepare() start");
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            mediaExtractor.release();
            return false;
        }
        int trackCount = mediaExtractor.getTrackCount();
        String mime = null;
        for (int i = 0; i < trackCount; i++) {
            mAudioDecoderMediaFormat = mediaExtractor.getTrackFormat(i);
            mime = mAudioDecoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mediaExtractor.selectTrack(i);
                break;
            }
        }
        if (TextUtils.isEmpty(mime)) {
            mediaExtractor.release();
            return false;
        }

        int sampleRateInHz =
                mAudioDecoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount =
                mAudioDecoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mAudioDecoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
        mAudioDecoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
        try {
            if (sampleRateIndexMap.containsKey(sampleRateInHz)
                    && channelConfigIndexMap.containsKey(channelCount)) {
                List<byte[]> list = new ArrayList<>();
                list.add(MediaUtils.buildAacAudioSpecificConfig(
                        sampleRateIndexMap.get(sampleRateInHz),
                        channelConfigIndexMap.get(channelCount)));
                MediaUtils.setCsdBuffers(mAudioDecoderMediaFormat, list);
                if (DEBUG)
                    MLog.d(TAG, "prepare() " +
                            mAudioDecoderMediaFormat.toString());
                mAudioDecoderMediaCodec =
                        MediaUtils.getAudioDecoderMediaCodec(
                                mime,
                                mAudioDecoderMediaFormat);
            }
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | NullPointerException e) {
            e.printStackTrace();
            if (mAudioDecoderMediaCodec != null) {
                mAudioDecoderMediaCodec.release();
            }
            mAudioDecoderMediaCodec = null;
            mAudioDecoderMediaFormat = null;
        }
        if (mAudioDecoderMediaCodec == null) {
            mediaExtractor.release();
            return false;
        }
        /***
         此时创建的AudioTrack在大多数情况下是可用的,
         个别情况要等到onFormatChanged的事件到来后再创建才可能有效
         */
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
        if (mAudioTrack != null) {
            setVolume();
            mAudioTrack.play();
        }
        mediaExtractor.release();
        if (DEBUG)
            MLog.d(TAG, "prepare() end");
        return true;
    }

    private void readLocalData() {
        if (DEBUG)
            MLog.w(TAG, "readLocalData() start");

        if (!prepare()) {
            return;
        }

        readDataSize = -1;
        lastOffsetIndex = -1;
        mIsReading = true;
        boolean onlyOne = true;
        while (mIsReading) {
            try {
                if (mInputStream.available() <= 0) {
                    readDataSize = -1;
                    break;
                }

                Arrays.fill(mData1, (byte) 0);
                // mInputStream ---> mData1
                if (lastOffsetIndex == -1) {
                    readDataSize = mInputStream.read(mData1, 0, CACHE);
                    if (readDataSize > 0 && onlyOne) {
                        onlyOne = false;
                        new Thread(mHandleLocalData).start();
                    }
                } else {
                    readDataSize = mInputStream.read(mData1, 0, lastOffsetIndex);
                }

                if (DEBUG) {
                    MLog.e(TAG, "readLocalData()    readDataSize: " + readDataSize);
                    MLog.e(TAG, "readLocalData() lastOffsetIndex: " + lastOffsetIndex);
                }
                if (readDataSize < 0) {
                    break;
                }

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
            MLog.w(TAG, "readLocalData() end");
    }

    private void readHttpData() {
        if (DEBUG)
            MLog.w(TAG, "readHttpData() start");

        if (!prepare()) {
            return;
        }

        HttpAccessor httpAccessor = null;
        try {
            httpAccessor = new HttpAccessor(new URL(mPath), null);
            httpAccessor.open();
        } catch (MalformedURLException
                | ExoPlaybackException e) {
            e.printStackTrace();
            httpAccessor = null;
        }
        if (httpAccessor == null) {
            return;
        }

        // 测试了一下,好像最大只能读取8192个byte
        int bufferLength = 1024 * 10;
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
                int readSize = httpAccessor.read(buffer, 0, bufferLength);
                MLog.i(TAG, "readHttpData()     readSize: " + readSize);
                if (readSize < 0) {
                    // 开启任务处理数据(如果还没有开启任务过的话)
                    // 如果任务是在这里被开启的,那么说明网络文件长度小于等于CACHE
                    if (!mIsHandling) {
                        mIsHandling = true;
                        new Thread(mHandleHttpData).start();
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
                    MLog.i(TAG, "readHttpData() readDataSize: " + readDataSize);
                    continue;
                } else {
                    // 开启任务处理数据
                    // 如果任务是在这里被开启的,那么说明网络文件长度大于CACHE
                    if (!mIsHandling) {
                        mIsHandling = true;
                        new Thread(mHandleHttpData).start();
                    }
                    // wait
                    synchronized (mReadDataLock) {
                        try {
                            mReadDataLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Arrays.fill(mData1, (byte) 0);
                    readDataSize = readSize;
                    readTotalSize = readSize;
                    // 此时buffer中还有readSize个byte
                    System.arraycopy(buffer, 0,
                            mData1, 0, readSize);
                }
            } catch (ExoPlaybackException
                    | NullPointerException e) {
                e.printStackTrace();
                mIsReading = false;
                break;
            }
        }// while(...) end

        try {
            if (httpAccessor != null) {
                httpAccessor.close();
            }
        } catch (ExoPlaybackException e) {
            e.printStackTrace();
        }

        if (DEBUG)
            MLog.w(TAG, "readHttpData() end");
    }

    private void handleLocalData() {
        if (DEBUG)
            MLog.w(TAG, "handleLocalData() start");

        // mData2中剩下的数据大小
        ArrayList<Integer> offsetList = new ArrayList<Integer>();
        int restOfDataSize = 0;
        int frameDataLength = 1024;
        byte[] frameData = new byte[frameDataLength];
        // mData1 ---> mData2
        System.arraycopy(
                mData1, 0,
                mData2, 0, readDataSize);

        long startDecodeTime = System.nanoTime();
        boolean isHandlingData = true;
        while (isHandlingData) {
            MLog.d(TAG, "handleLocalData() findHead start");
            if (readDataSize == CACHE
                    || readDataSize == lastOffsetIndex) {
                findHead(mData2, 0, CACHE, offsetList);
            } else {
                findHead(mData2, 0, readDataSize, offsetList);
            }
            MLog.d(TAG, "handleLocalData() findHead end");
            int offsetCount = offsetList.size();
            MLog.d(TAG, "handleLocalData() findHead     offsetCount: " + offsetCount);
            if (offsetCount > 1) {
                lastOffsetIndex = offsetList.get(offsetCount - 1);
                restOfDataSize = readDataSize - lastOffsetIndex;
            } else if (offsetCount == 0) {
                StringBuilder sb = new StringBuilder();
                for (byte bt : mData2) {
                    sb.append(" ");
                    sb.append(bt);
                }
                MLog.d(TAG, "handleLocalData() " + sb.toString());

                break;
            }
            MLog.d(TAG, "handleLocalData() findHead lastOffsetIndex: " + lastOffsetIndex);
            synchronized (mReadDataLock) {
                mReadDataLock.notify();
            }

            for (int i = 0; i < offsetCount; i++) {
                Arrays.fill(frameData, (byte) 0);
                if (i + 1 < offsetCount) {
                    /***
                     集合中至少有两个offset才有一帧输出
                     各帧之间的offset很重要,比如有:0, 519, 1038, 1585, 2147 ...
                     知道了offset,那么就知道了要"喂"多少数据了.
                     两个offset的位置一减就是一帧的长度
                     */
                    int frameLength = offsetList.get(i + 1) - offsetList.get(i);
                    if (frameLength > frameDataLength) {
                        MLog.d(TAG, "handleLocalData() 出现大体积帧 frameLength: " + frameLength);
                        frameDataLength = frameLength;
                        frameData = new byte[frameLength];
                    }
                    // frameData保存着一帧的数据(包括ADTS头和AAC ES(AAC音频数据))
                    System.arraycopy(
                            mData2, offsetList.get(i),
                            frameData, 0, frameLength);
                    MLog.d(TAG, "handleLocalData() offset: " + offsetList.get(i) +
                            "    " + frameData[0] +
                            " " + frameData[1] +
                            " " + frameData[2] +
                            " " + frameData[3] +
                            " " + frameData[4] +
                            " " + frameData[5] +
                            " " + frameData[6]);
                    MLog.d(TAG, "handleLocalData() frameLength: " + frameLength);

                    feedInputBufferAndDrainOutputBuffer(
                            frameData, 0, frameLength, startDecodeTime);
                } else {
                    // 处理集合中最后一个offset的位置
                    lastOffsetIndex = offsetList.get(i);
                    MLog.i(TAG, "handleLocalData()    readDataSize: " + readDataSize);
                    MLog.i(TAG, "handleLocalData() lastOffsetIndex: " + lastOffsetIndex);
                    MLog.i(TAG, "handleLocalData()  restOfDataSize: " + restOfDataSize);
                    if (restOfDataSize > 0) {
                        System.arraycopy(
                                mData2, lastOffsetIndex,
                                frameData, 0, restOfDataSize);
                    }
                    if (readDataSize == lastOffsetIndex
                            || readDataSize > frameData.length) {
                        // 把mData2中剩下的数据移动到mData2的开头
                        Arrays.fill(mData2, (byte) 0);
                        System.arraycopy(
                                frameData, 0,
                                mData2, 0, restOfDataSize);
                        // mData1 ---> mData2
                        System.arraycopy(mData1, 0,
                                mData2, restOfDataSize, readDataSize);
                        // mData2中有readDataSize个字节
                        readDataSize += restOfDataSize;
                        break;
                    } else {
                        // 剩下还有一帧
                        if (CACHE >= 1024 * 1024) {
                            int frameLength = restOfDataSize;
                            MLog.d(TAG, "handleLocalData() last     offset: " + 0 +
                                    "    " + frameData[0] +
                                    " " + frameData[1] +
                                    " " + frameData[2] +
                                    " " + frameData[3] +
                                    " " + frameData[4] +
                                    " " + frameData[5] +
                                    " " + frameData[6]);
                            MLog.d(TAG, "handleLocalData()     frameLength: " + frameLength);

                            feedInputBufferAndDrainOutputBuffer(
                                    frameData, 0, frameLength, startDecodeTime);

                            isHandlingData = false;
                        }
                        break;
                    }
                }
            }// for(...) end
        }// while(true) end

        mIsReading = false;
        synchronized (mReadDataLock) {
            mReadDataLock.notify();
        }

        if (DEBUG)
            MLog.w(TAG, "handleLocalData() end");
    }

    private void handleHttpData() {
        if (DEBUG)
            MLog.w(TAG, "handleHttpData() start");

        // mData2中剩下的数据大小
        ArrayList<Integer> offsetList = new ArrayList<Integer>();
        int preReadDataSize = 0;
        int restOfDataSize = 0;
        int frameDataLength = 1024;
        byte[] frameData = new byte[frameDataLength];
        // mData1 ---> mData2
        System.arraycopy(
                mData1, 0,
                mData2, 0, readDataSize);

        long startDecodeTime = System.nanoTime();
        boolean isHandlingData = true;
        while (isHandlingData) {
            MLog.d(TAG, "handleHttpData() findHead start");
            if (readDataSize == CACHE
                    || readDataSize == lastOffsetIndex) {
                findHead(mData2, 0, CACHE, offsetList);
            } else {
                findHead(mData2, 0, readDataSize, offsetList);
            }
            MLog.d(TAG, "handleHttpData() findHead end");
            int offsetCounts = offsetList.size();
            MLog.i(TAG, "handleHttpData() findHead    offsetCounts: " + offsetCounts);
            if (offsetCounts > 1) {
                preReadDataSize = readDataSize;
                lastOffsetIndex = offsetList.get(offsetCounts - 1);
                restOfDataSize = readDataSize - lastOffsetIndex;
                MLog.i(TAG, "handleHttpData() findHead    readDataSize: " + readDataSize);
                MLog.i(TAG, "handleHttpData() findHead lastOffsetIndex: " + lastOffsetIndex);
                MLog.i(TAG, "handleHttpData() findHead  restOfDataSize: " + restOfDataSize);
            } else if (offsetCounts == 0) {
                StringBuilder sb = new StringBuilder();
                for (byte bt : mData2) {
                    sb.append(" ");
                    sb.append(bt);
                }
                MLog.d(TAG, "handleHttpData() " + sb.toString());

                break;
            }
            if (mIsReading) {
                // 此处发送消息后,readDataSize的大小可能会变化
                synchronized (mReadDataLock) {
                    MLog.i(TAG, "handleHttpData() findHead mReadDataLock.notify()");
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
                    if (frameLength > frameDataLength) {
                        MLog.d(TAG, "handleHttpData() 出现大体积帧 frameLength: " + frameLength);
                        frameDataLength = frameLength;
                        frameData = new byte[frameLength];
                    }
                    // frameData保存着一帧的数据(包括ADTS头和AAC ES(AAC音频数据))
                    System.arraycopy(
                            mData2, offsetList.get(i),
                            frameData, 0, frameLength);
                    MLog.d(TAG, "handleHttpData() offset: " + offsetList.get(i) +
                            "    " + frameData[0] +
                            " " + frameData[1] +
                            " " + frameData[2] +
                            " " + frameData[3] +
                            " " + frameData[4] +
                            " " + frameData[5] +
                            " " + frameData[6]);
                    MLog.d(TAG, "handleHttpData() frameLength: " + frameLength);

                    feedInputBufferAndDrainOutputBuffer(
                            frameData, 0, frameLength, startDecodeTime);
                } else {
                    // 处理集合中最后一个offset的位置
                    lastOffsetIndex = offsetList.get(i);
                    MLog.i(TAG, "handleHttpData() preReadDataSize: " + preReadDataSize);
                    MLog.i(TAG, "handleHttpData()    readDataSize: " + readDataSize);
                    MLog.i(TAG, "handleHttpData() lastOffsetIndex: " + lastOffsetIndex);
                    MLog.i(TAG, "handleHttpData()  restOfDataSize: " + restOfDataSize);
                    if (restOfDataSize > 0) {
                        System.arraycopy(
                                mData2, lastOffsetIndex,
                                frameData, 0, restOfDataSize);
                    }

                    if (preReadDataSize != readDataSize) {
                        // 把mData2中剩下的数据移动到mData2的开头
                        Arrays.fill(mData2, (byte) 0);
                        System.arraycopy(
                                frameData, 0,
                                mData2, 0, restOfDataSize);
                        // mData1 ---> mData2
                        System.arraycopy(mData1, 0,
                                mData2, restOfDataSize, readDataSize);
                        break;
                    } else {
                        // 剩下还有一帧
                        if (CACHE >= 1024 * 1024) {
                            int frameLength = restOfDataSize;
                            MLog.d(TAG, "handleHttpData() last     offset: " + 0 +
                                    "    " + frameData[0] +
                                    " " + frameData[1] +
                                    " " + frameData[2] +
                                    " " + frameData[3] +
                                    " " + frameData[4] +
                                    " " + frameData[5] +
                                    " " + frameData[6]);
                            MLog.d(TAG, "handleHttpData()     frameLength: " + frameLength);

                            feedInputBufferAndDrainOutputBuffer(
                                    frameData, 0, frameLength, startDecodeTime);

                            isHandlingData = false;
                        }
                        break;
                    }
                }
            }// for(...) end
        }// while(true) end

        mIsReading = false;
        synchronized (mReadDataLock) {
            mReadDataLock.notify();
        }

        if (DEBUG)
            MLog.w(TAG, "handleHttpData() end");
    }

    private void feedInputBufferAndDrainOutputBuffer(
            byte[] data, int offset, int size, long startTime) {
        // Input
        // SystemClock.sleep(1);
        MediaUtils.feedInputBuffer(
                mAudioDecoderMediaCodec, data,
                offset, size, startTime);

        // Output
        // SystemClock.sleep(1);
        MediaUtils.drainOutputBuffer(
                mAudioDecoderMediaCodec, false, callback);
    }

    /***
     * 寻找指定buffer中AAC帧头的开始位置
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
                i += 7;
            } else {
                i++;
            }
        }
    }

    /***
     判断aac帧头
     */
    private boolean isHead(byte[] data, int offset, int size) {
        if (offset + 6 < size
                && data[offset] == (byte) 0xFF
                && data[offset + 1] == (byte) 0xF1
                && (data[offset + 3] == (byte) 0x80 || data[offset + 3] == 64)
                && data[offset + 6] == (byte) 0xFC) {
            // -1 -15 80 -128 63 -97 -4
            // -1 -15 84   64 3  -65 -4
            /*MLog.d(TAG, "onBuffer() offset: " + offset +
                    "     " + data[offset] +
                    " " + data[offset + 1] +
                    " " + data[offset + 2] +
                    " " + data[offset + 3] +
                    " " + data[offset + 4] +
                    " " + data[offset + 5] +
                    " " + data[offset + 6]);*/
            return true;
        }
        return false;
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

    private Runnable mReadLocalData = new Runnable() {
        @Override
        public void run() {
            readLocalData();
        }
    };

    private Runnable mReadHttpData = new Runnable() {
        @Override
        public void run() {
            readHttpData();
        }
    };

    private Runnable mHandleLocalData = new Runnable() {
        @Override
        public void run() {
            handleLocalData();
        }
    };

    private Runnable mHandleHttpData = new Runnable() {
        @Override
        public void run() {
            handleHttpData();
        }
    };

    private MediaUtils.Callback callback = new MediaUtils.Callback() {
        @Override
        public void onFormatChanged(MediaFormat newMediaFormat) {
            if (mAudioTrack != null) {
                mAudioTrack.release();
            }
            int sampleRateInHz =
                    newMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount =
                    newMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int audioFormat =
                    newMediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
            mAudioTrack = MediaUtils.createAudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRateInHz, channelCount, audioFormat,
                    AudioTrack.MODE_STREAM);
            if (mAudioTrack != null) {
                setVolume();
                mAudioTrack.play();
            }
        }

        @Override
        public void onBuffer(ByteBuffer room, int roomSize) {
            byte[] pcmData = new byte[roomSize];
            room.get(pcmData, 0, pcmData.length);
            if (mAudioTrack != null) {
                int writeSize = mAudioTrack.write(pcmData, 0, roomSize);
                //MLog.d(TAG, "onBuffer() writeSize: " + writeSize);
            }
        }
    };

}
