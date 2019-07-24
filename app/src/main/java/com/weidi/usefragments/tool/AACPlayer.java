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
import java.net.HttpURLConnection;
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

 本地与网络共用同一个播放器
 */

public class AACPlayer {

    private static final String TAG =
            AACPlayer.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static Map<Integer, Integer> sampleRateIndexMap = new HashMap<>();
    public static Map<Integer, Integer> channelConfigIndexMap = new HashMap<>();

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
    // 音频不要小于1MB
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

    public AACPlayer() {
        init();
    }

    public void setPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mPath = path;
    }

    public void start() {
        new Thread(mReadData).start();
    }

    private void init() {
        mData1 = new byte[CACHE];
        mData2 = new byte[CACHE];
    }

    private boolean prepareInputStream() {
        if (!mPath.endsWith(".aac")
                && !mPath.endsWith(".AAC")) {
            return false;
        }

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
                    MLog.d(TAG, "prepareMediaCodec() " +
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

        if (!prepareMediaCodec()) {
            MLog.e(TAG, "readData() prepareMediaCodec failed");
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
                // httpAccessor ---> readData1
                int readSize = mInputStream.read(buffer, 0, bufferLength);
                if (readSize < 0) {
                    MLog.i(TAG, "readData()     readSize: " + readSize);
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
                    // buffer ---> readData1
                    System.arraycopy(buffer, 0,
                            mData1, readDataSize, readSize);
                    readDataSize += readSize;
                    MLog.i(TAG, "readData() readDataSize: " + readDataSize);
                    continue;
                } else {
                    // 开启任务处理数据
                    // 如果任务是在这里被开启的,那么说明网络文件长度大于CACHE
                    if (!mIsHandling) {
                        mIsHandling = true;
                        new Thread(mHandleData).start();
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
        int preReadDataSize = 0;
        int restOfDataSize = 0;
        int frameDataLength = 1024;
        byte[] frameData = new byte[frameDataLength];
        // readData1 ---> handleData
        System.arraycopy(
                mData1, 0,
                mData2, 0, readDataSize);

        long startDecodeTime = System.nanoTime();
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
            } else if (offsetCounts == 0) {
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
                    MLog.i(TAG, "handleData() findHead readDataLock.notify()");
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
                        MLog.d(TAG, "handleData() 出现大体积帧 frameLength: " + frameLength);
                        frameDataLength = frameLength;
                        frameData = new byte[frameLength];
                    }
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

                    feedInputBufferAndDrainOutputBuffer(
                            frameData, 0, frameLength, startDecodeTime);
                } else {
                    // 处理集合中最后一个offset的位置
                    lastOffsetIndex = offsetList.get(i);
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
                    if (preReadDataSize != readDataSize) {
                        // 把mData2中剩下的数据移动到mData2的开头
                        Arrays.fill(mData2, (byte) 0);
                        System.arraycopy(
                                frameData, 0,
                                mData2, 0, restOfDataSize);
                        // readData1 ---> handleData
                        System.arraycopy(mData1, 0,
                                mData2, restOfDataSize, readDataSize);
                        break;
                    } else {
                        // 最后一帧
                        if (CACHE >= 1024 * 1024) {
                            int frameLength = restOfDataSize;
                            MLog.d(TAG, "handleData() last     offset: " + 0 +
                                    "    " + frameData[0] +
                                    " " + frameData[1] +
                                    " " + frameData[2] +
                                    " " + frameData[3] +
                                    " " + frameData[4] +
                                    " " + frameData[5] +
                                    " " + frameData[6]);
                            MLog.d(TAG, "handleData()     frameLength: " + frameLength);

                            feedInputBufferAndDrainOutputBuffer(
                                    frameData, 0, frameLength, startDecodeTime);

                            isHandlingData = false;
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

        if (DEBUG)
            MLog.w(TAG, "handleData() end");
    }

    private void feedInputBufferAndDrainOutputBuffer(
            byte[] data, int offset, int size, long startTime) {
        long presentationTimeUs =
                (System.nanoTime() - startTime) / 1000;
        // Input
        MediaUtils.feedInputBuffer(
                mAudioDecoderMediaCodec, data,
                offset, size, startTime);
        // Output
        MediaUtils.drainOutputBuffer(
                mAudioDecoderMediaCodec, false, callback);

        /*// Input
        SystemClock.sleep(1);
        // Output
        SystemClock.sleep(1);*/
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
            /*MLog.d(TAG, "onOutputBuffer() offset: " + offset +
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
        public void onInputBuffer(int roomIndex, ByteBuffer room, MediaUtils.InputBufferInfo info) {

        }

        @Override
        public void onOutputBuffer(
                int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
            byte[] pcmData = new byte[roomSize];
            room.get(pcmData, 0, pcmData.length);
            if (mAudioTrack != null) {
                int writeSize = mAudioTrack.write(pcmData, 0, roomSize);
                //MLog.d(TAG, "onOutputBuffer() writeSize: " + writeSize);
            }
        }
    };

}
