package com.weidi.usefragments.tool;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.weidi.usefragments.media.MediaUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
    private static final int CACHE = 1024 * 1024;// 1500
    private InputStream mInputStream;
    private boolean mIsReading = false;
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
    private int mAudioTrackIndex = -1;
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
                }
            } catch (IOException e) {
                e.printStackTrace();
                mInputStream = null;
            }
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
        if (mInputStream == null) {
            return;
        }
        new Thread(mReadData).start();
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

        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mAudioDecoderMediaFormat = mediaExtractor.getTrackFormat(i);
            if (DEBUG)
                MLog.d(TAG, "readData() mAudioDecoderMediaFormat: " +
                        mAudioDecoderMediaFormat.toString());
            String mime = mAudioDecoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                boolean hasException = false;
                try {
                    mAudioTrackIndex = i;
                    mediaExtractor.selectTrack(i);
                    mAudioDecoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
                    mAudioDecoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
                    //mAudioDecoderMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024);
                    List<byte[]> list = new ArrayList<>();
                    int sampleRateInHz =
                            mAudioDecoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount =
                            mAudioDecoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    if (sampleRateIndexMap.containsKey(sampleRateInHz)
                            && channelConfigIndexMap.containsKey(channelCount)) {
                        list.add(MediaUtils.buildAacAudioSpecificConfig(
                                sampleRateIndexMap.get(sampleRateInHz),
                                channelConfigIndexMap.get(channelCount)));
                        MediaUtils.setCsdBuffers(mAudioDecoderMediaFormat, list);
                        mAudioDecoderMediaCodec =
                                MediaUtils.getAudioDecoderMediaCodec(mime, mAudioDecoderMediaFormat);
                    }

                    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    mAudioTrack = MediaUtils.createAudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRateInHz, channelCount, audioFormat,
                            AudioTrack.MODE_STREAM);
                    if (mAudioTrack != null) {
                        setVolume();
                        mAudioTrack.play();
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException
                        | NullPointerException e) {
                    e.printStackTrace();
                    hasException = true;
                }
                if (hasException) {
                    if (mAudioDecoderMediaCodec != null) {
                        mAudioDecoderMediaCodec.release();
                    }
                    mAudioDecoderMediaCodec = null;
                    mAudioDecoderMediaFormat = null;
                    mAudioTrackIndex = -1;
                }
                break;
            }
        }
        mediaExtractor.release();
        if (mAudioDecoderMediaCodec == null) {
            return;
        }

        readDataSize = -1;
        lastOffsetIndex = -1;
        boolean onlyOne = true;
        mIsReading = true;
        while (mIsReading) {
            try {
                if (mIsLocalFile && mInputStream.available() <= 0) {
                    readDataSize = -1;
                    break;
                }

                Arrays.fill(mData1, (byte) 0);
                // mInputStream ---> mData1
                if (lastOffsetIndex == -1) {
                    readDataSize = mInputStream.read(mData1, 0, CACHE);
                    if (readDataSize > 0 && onlyOne) {
                        onlyOne = false;
                        new Thread(mHandleData).start();
                    } else {
                        break;
                    }
                } else {
                    readDataSize = mInputStream.read(mData1, 0, lastOffsetIndex);
                    if (readDataSize < 0) {
                        break;
                    }
                }

                if (DEBUG) {
                    MLog.e(TAG, "readData()    readDataSize: " + readDataSize);
                    MLog.e(TAG, "readData() lastOffsetIndex: " + lastOffsetIndex);
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
            MLog.w(TAG, "readData() end");
    }

    private void handleData() {
        if (DEBUG)
            MLog.w(TAG, "handleData() start");

        ArrayList<Integer> offsetList = new ArrayList<Integer>();
        int frameDataLength = 1024 * 30;
        byte[] frameData = new byte[frameDataLength];
        boolean isHandlingData = true;
        // mData1 ---> mData2
        System.arraycopy(
                mData1, 0,
                mData2, 0, readDataSize);

        // mData2中剩下的数据大小
        int restOfDataSize = 0;

        MediaUtils.Callback callback = new MediaUtils.Callback() {
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

        long startDecodeTime = System.nanoTime();
        while (isHandlingData) {
            MLog.d(TAG, "handleData() findHead start");
            if (readDataSize == CACHE
                    || readDataSize == lastOffsetIndex) {
                findHead(mData2, 0, CACHE, offsetList);
            } else {
                findHead(mData2, 0, readDataSize, offsetList);
            }
            MLog.d(TAG, "handleData() findHead end");
            int offsetCount = offsetList.size();
            MLog.d(TAG, "handleData() findHead     offsetCount: " + offsetCount);
            if (offsetCount > 1) {
                lastOffsetIndex = offsetList.get(offsetCount - 1);
                restOfDataSize = readDataSize - lastOffsetIndex;
            } else if (offsetCount == 0) {
                StringBuilder sb = new StringBuilder();
                for (byte bt : mData2) {
                    sb.append(" ");
                    sb.append(bt);
                }
                MLog.d(TAG, "handleData() " + sb.toString());

                break;
            }
            MLog.d(TAG, "handleData() findHead lastOffsetIndex: " + lastOffsetIndex);
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
                    if (frameLength > frameDataLength) {
                        MLog.d(TAG, "handleData() 大体积帧 frameLength: " + frameLength);
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
                    // Input
                    // SystemClock.sleep(1);
                    MediaUtils.feedInputBuffer(
                            mAudioDecoderMediaCodec, frameData,
                            0, frameLength, startDecodeTime);
                    // Output
                    // SystemClock.sleep(1);
                    MediaUtils.drainOutputBuffer(
                            mAudioDecoderMediaCodec, false, callback);
                } else {
                    // 集合中最后一个offset的位置
                    lastOffsetIndex = offsetList.get(i);
                    MLog.i(TAG, "handleData()    readDataSize: " + readDataSize);
                    MLog.i(TAG, "handleData() lastOffsetIndex: " + lastOffsetIndex);
                    MLog.i(TAG, "handleData()  restOfDataSize: " + restOfDataSize);
                    if (restOfDataSize > 0) {
                        System.arraycopy(
                                mData2, lastOffsetIndex,
                                frameData, 0, restOfDataSize);
                    }
                    if (readDataSize == lastOffsetIndex
                            || readDataSize > frameData.length
                            || (!mIsLocalFile && restOfDataSize > 0)) {
                        // 把mData2中剩下的数据移动到mData2的开头
                        Arrays.fill(mData2, (byte) 0);
                        System.arraycopy(
                                frameData, 0,
                                mData2, 0, restOfDataSize);
                        // mData1 ---> mData2
                        System.arraycopy(mData1, 0,
                                mData2, restOfDataSize, readDataSize);
                        readDataSize += restOfDataSize;
                        break;
                    } else {
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
                            // Input
                            // SystemClock.sleep(1);
                            MediaUtils.feedInputBuffer(
                                    mAudioDecoderMediaCodec, frameData,
                                    0, frameLength, startDecodeTime);
                            // Output
                            // SystemClock.sleep(1);
                            MediaUtils.drainOutputBuffer(
                                    mAudioDecoderMediaCodec, false, callback);

                            isHandlingData = false;
                        } else {
                            // 下面是CACHE = 1500时的处理
                            int frameLength = 0;
                            if (readDataSize != -1) {
                                frameLength = readDataSize + restOfDataSize;
                            } else {
                                frameLength = frameData.length - lastOffsetIndex;
                            }
                            MLog.d(TAG, "handleData() last frameLength: " + frameLength);
                            if (frameLength > frameData.length) {
                                Arrays.fill(mData2, (byte) 0);
                                System.arraycopy(
                                        frameData, 0,
                                        mData2, 0, restOfDataSize);
                                System.arraycopy(
                                        mData1, 0,
                                        mData2, restOfDataSize, readDataSize);
                                frameData = new byte[frameLength];
                                System.arraycopy(
                                        mData2, 0,
                                        frameData, 0, frameLength);
                                readDataSize = -1;
                            } else {
                                if (readDataSize != -1) {
                                    System.arraycopy(
                                            mData1, 0,
                                            frameData, restOfDataSize, readDataSize);
                                } else {
                                    Arrays.fill(frameData, (byte) 0);
                                    System.arraycopy(
                                            mData2, 0,
                                            frameData, restOfDataSize, frameLength);
                                }
                                MLog.d(TAG, "handleData() last      offset: " + 0 +
                                        "    " + frameData[0] +
                                        " " + frameData[1] +
                                        " " + frameData[2] +
                                        " " + frameData[3] +
                                        " " + frameData[4] +
                                        " " + frameData[5] +
                                        " " + frameData[6]);
                                // Input
                                SystemClock.sleep(1);
                                // Output
                                SystemClock.sleep(1);

                                isHandlingData = false;
                            }
                        }
                        // 剩下还有一帧
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

    /**
     * 判断aac帧头
     */
    private boolean isHead(byte[] data, int offset, int size) {
        boolean result = false;
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
            result = true;
        }
        return result;
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

}
