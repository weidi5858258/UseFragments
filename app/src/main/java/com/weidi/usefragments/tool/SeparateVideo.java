package com.weidi.usefragments.tool;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.weidi.usefragments.media.MediaUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/***
 Created by root on 19-7-17.
 */

public class SeparateVideo {
    private static final String TAG =
            SeparateVideo.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int PREPARE = 0x0001;
    private static final int PLAY = 0x0002;
    private static final int PAUSE = 0x0003;
    private static final int STOP = 0x0004;
    private static final int RELEASE = 0x0005;
    private static final int PREV = 0x0006;
    private static final int NEXT = 0x0007;
    private static final int BUFFER = 1024 * 1024 * 2;

    // 被分离的文件路径
    private String mPath;
    // 分离后文件的输出目录
    private String OUTPUT_PATH;
    private String mAudioName;
    private String mVideoName;
    // 必须要有两个MediaExtractor对象,不能共用同一个
    private MediaExtractor mAudioExtractor;
    private MediaExtractor mVideoExtractor;
    private MediaCodec mAudioDncoderMediaCodec;
    private MediaCodec mVideoDncoderMediaCodec;
    private MediaFormat mAudioDncoderMediaFormat;
    private MediaFormat mVideoDncoderMediaFormat;
    private int mAudioTrackIndex = -1;
    private int mVideoTrackIndex = -1;

    private boolean mIsAudioWorking = false;
    private boolean mIsVideoWorking = false;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    // 是否需要保存
    private static final boolean mAudioNeedToSave = false;
    private static final boolean mVideoNeedToSave = false;
    // 是否需要打印日志
    private static final boolean mAudioNeedToPrint = false;
    private static final boolean mVideoNeedToPrint = false;
    private BufferedOutputStream mAudioOS;
    private BufferedOutputStream mVideoOS;

    public SeparateVideo() {
        init();
    }

    public SeparateVideo setPath(String path) {
        OUTPUT_PATH = "/data/data/com.weidi.usefragments/files/Movies/";
        OUTPUT_PATH = "/storage/37C8-3904/Android/data/com.weidi.usefragments/files/Movies/";
        OUTPUT_PATH = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";

        mPath = path;
        mPath = "/storage/37C8-3904/myfiles/video/Silent_Movie_321_AC4_H265_MP4_50fps.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/[HDR]4K_HDR_Technology_English.mp4";
        mPath = "/storage/2430-1702/BaiduNetdisk/video/test.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/Escape.Plan.2.mp4";
        mPath = "http://xunlei.jingpin88.com/20171028/6WQ5SFS2/mp4/6WQ5SFS2.mp4";
        mPath = "/storage/2430-1702/BaiduNetdisk/video/shape_of_my_heart.mp4";
        mPath = "/storage/2430-1702/BaiduNetdisk/video/" +
                "08_mm-MP4-H264_720x400_2997_AAC-LC_192_48.mp4";
        mPath = "/storage/2430-1702/BaiduNetdisk/video/test.mp4";

        mPath = "/storage/2430-1702/BaiduNetdisk/video/J速备战.mp4";
        mAudioName = "shape_of_my_heart.aac";
        mVideoName = "shape_of_my_heart.h264";

        if (DEBUG)
            MLog.d(TAG, "setPath() mPath: " + mPath);
        return this;
    }

    public void start() {
        mThreadHandler.removeMessages(PREPARE);
        mThreadHandler.sendEmptyMessage(PREPARE);
    }

    private void init() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SeparateVideo.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SeparateVideo.this.uiHandleMessage(msg);
            }
        };
    }

    private boolean internalPrepare() {
        if (TextUtils.isEmpty(mPath)) {
            return false;
        }
        if (DEBUG)
            MLog.d(TAG, "internalPrepare() start");

        if (!mPath.startsWith("http://")
                && !mPath.startsWith("https://")) {
            File file = new File(mPath);
            if (!file.canRead()
                    || file.isDirectory()) {
                if (DEBUG)
                    MLog.e(TAG, "不能读取此文件: " + mPath);
                return false;
            }
            long fileSize = file.length();
            if (DEBUG)
                MLog.d(TAG, "internalPrepare() fileSize: " + fileSize);
        }

        mAudioOS = null;
        mVideoOS = null;

        if (mAudioNeedToSave) {
            File audioFile = new File(OUTPUT_PATH, mAudioName);
            if (audioFile.exists()) {
                try {
                    audioFile.delete();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            try {
                audioFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            try {
                mAudioOS = new BufferedOutputStream(
                        new FileOutputStream(audioFile), BUFFER);// 1024 * 10
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (mVideoNeedToSave) {
            File videoFile = new File(OUTPUT_PATH, mVideoName);
            if (videoFile.exists()) {
                try {
                    videoFile.delete();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            try {
                videoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            try {
                mVideoOS = new BufferedOutputStream(
                        new FileOutputStream(videoFile), BUFFER);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }

        mAudioExtractor = null;
        mVideoExtractor = null;
        mAudioDncoderMediaCodec = null;
        mVideoDncoderMediaCodec = null;
        mAudioDncoderMediaFormat = null;
        mVideoDncoderMediaFormat = null;
        mAudioTrackIndex = -1;
        mVideoTrackIndex = -1;

        String audioMime = null;
        String videoMime = null;

        // Audio
        mAudioExtractor = new MediaExtractor();
        try {
            mAudioExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int trackCount = mAudioExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mAudioDncoderMediaFormat = mAudioExtractor.getTrackFormat(i);
            String mime = mAudioDncoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                audioMime = mime;
                mAudioTrackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(audioMime)
                || mAudioTrackIndex == -1) {
            return false;
        }

        MLog.d(TAG, "internalPrepare() audio mime: " + audioMime);
        MLog.d(TAG, "internalPrepare() mAudioDncoderMediaFormat: " +
                mAudioDncoderMediaFormat.toString());
        try {
            mAudioExtractor.selectTrack(mAudioTrackIndex);
            if (!TextUtils.equals("audio/ac4", audioMime)) {
                mAudioDncoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec(
                        audioMime, mAudioDncoderMediaFormat);
            } else {
                MediaCodecInfo mediaCodecInfo = MediaUtils.getDecoderMediaCodecInfo(audioMime);
                String codecName = null;
                if (mediaCodecInfo != null) {
                    codecName = mediaCodecInfo.getName();
                } else {
                    if (TextUtils.equals("audio/ac4", audioMime)) {
                        codecName = "OMX.google.raw.decoder";
                        mAudioDncoderMediaFormat.setString(
                                MediaFormat.KEY_MIME, "audio/raw");
                    }
                }
                if (!TextUtils.isEmpty(codecName)) {
                    mAudioDncoderMediaCodec =
                            MediaCodec.createByCodecName(codecName);
                    mAudioDncoderMediaCodec.configure(
                            mAudioDncoderMediaFormat, null, null, 0);
                    mAudioDncoderMediaCodec.start();
                }
            }
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException
                | IOException e) {
            e.printStackTrace();
            if (mAudioDncoderMediaCodec != null) {
                mAudioDncoderMediaCodec.release();
            }
            mAudioDncoderMediaCodec = null;
        }
        if (mAudioDncoderMediaCodec == null) {
            return false;
        }

        if (mAudioDncoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mAudioDncoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "internalPrepare() audio csd-0: " + sb.toString());
        }
        if (mAudioDncoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mAudioDncoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "internalPrepare() audio csd-1: " + sb.toString());
        }

        // Video
        mVideoExtractor = new MediaExtractor();
        try {
            mVideoExtractor.setDataSource(mPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        trackCount = mVideoExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mVideoDncoderMediaFormat = mVideoExtractor.getTrackFormat(i);
            String mime = mVideoDncoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                videoMime = mime;
                mVideoTrackIndex = i;
                break;
            }
        }
        if (TextUtils.isEmpty(videoMime)
                || mVideoTrackIndex == -1) {
            return false;
        }

        MLog.d(TAG, "internalPrepare() video mime: " + videoMime);
        MLog.d(TAG, "internalPrepare() mVideoDncoderMediaFormat: " +
                mVideoDncoderMediaFormat.toString());
        try {
            mVideoExtractor.selectTrack(mVideoTrackIndex);
            mVideoDncoderMediaCodec =
                    MediaUtils.getVideoDecoderMediaCodec(
                            videoMime, mVideoDncoderMediaFormat);
        } catch (MediaCodec.CryptoException
                | IllegalStateException
                | IllegalArgumentException e) {
            e.printStackTrace();
            if (mVideoDncoderMediaCodec != null) {
                mVideoDncoderMediaCodec.release();
            }
            mVideoDncoderMediaCodec = null;
        }
        if (mVideoDncoderMediaCodec == null) {
            return false;
        }

        if (mVideoDncoderMediaFormat.containsKey("csd-0")) {
            ByteBuffer buffer = mVideoDncoderMediaFormat.getByteBuffer("csd-0");
            byte[] csd_0 = new byte[buffer.limit()];
            buffer.get(csd_0);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_0.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_0[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "internalPrepare() video csd-0: " + sb.toString());
        }
        if (mVideoDncoderMediaFormat.containsKey("csd-1")) {
            ByteBuffer buffer = mVideoDncoderMediaFormat.getByteBuffer("csd-1");
            byte[] csd_1 = new byte[buffer.limit()];
            buffer.get(csd_1);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            int count = csd_1.length;
            for (int i = 0; i < count; i++) {
                sb.append(csd_1[i]);
                if (i <= count - 2) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            MLog.d(TAG, "internalPrepare() video csd-1: " + sb.toString());
        }

        if (DEBUG)
            MLog.d(TAG, "internalPrepare() end");
        return true;
    }

    /***
     33 26 -107 37 -78 -119 8
     33 26 -107 45 -78 22 -123
     33 26 -108 -3 -70 20 -126
     33 26 -108 -11 -74 20 -93
     33 26 -113 -49 -1 -1 -7
     33 26 -113 127 -1 -2 63

     33
     0 11 43 77 107
     下面的意思是
     第一个数可能是
     33 32
     第二个数可能是
     28 -105 -108 -101 -107 -110 -98 -96 -95 -118 -109
     -111 44 78 124 -106 -114 -99 -102 -104 -113 -115
     -116 -112 -103 -100 -97 -123 -94 -93 -89 -91 -87
     -92 -90 -86 -84 -83 -82 -85 -88 -81 -119 -117 -121 -120 -122
     */
    private void audioWork() {
        MLog.w(TAG, "audioWork() start");

        long startDecodeTime = System.nanoTime();
        ByteBuffer room = ByteBuffer.allocate(BUFFER);
        byte[] audioData = new byte[BUFFER];
        int audioMaxSize = 0;
        mAudioExtractor.selectTrack(mAudioTrackIndex);
        ArrayList<Byte> oneIndex = new ArrayList<Byte>();
        ArrayList<Byte> twoIndex0 = new ArrayList<Byte>();
        ArrayList<Byte> twoIndex1 = new ArrayList<Byte>();
        ArrayList<Byte> twoIndex2 = new ArrayList<Byte>();
        ArrayList<Byte> twoIndex3 = new ArrayList<Byte>();
        ArrayList<Byte> twoIndex4 = new ArrayList<Byte>();
        while (true) {
            if (!mIsAudioWorking) {
                mIsAudioWorking = false;
                break;
            }

            room.clear();
            int readSize = mAudioExtractor.readSampleData(room, 0);
            // 250~465 1002
            if (readSize < 0) {
                MLog.d(TAG, "audioWork() readSize: " + readSize);
                mIsAudioWorking = false;
                break;
            }
            if (audioMaxSize < readSize) {
                audioMaxSize = readSize;
            }
            Arrays.fill(audioData, (byte) 0);
            room.get(audioData, 0, readSize);
            if (mAudioNeedToPrint) {
                MLog.d(TAG, "audioWork() " +
                        "    " + audioData[0] +
                        " " + audioData[1] +
                        " " + audioData[2] +
                        " " + audioData[3] +
                        " " + audioData[4] +
                        " " + audioData[5] +
                        " " + audioData[6]);
            }

            if (!oneIndex.contains(audioData[0])) {
                oneIndex.add(audioData[0]);
            }
            int count = oneIndex.size();
            for (int i = 0; i < count; i++) {
                if (oneIndex.get(i) == audioData[0]) {
                    switch (i) {
                        case 0:
                            if (!twoIndex0.contains(audioData[1])) {
                                twoIndex0.add(audioData[1]);
                            }
                            break;
                        case 1:
                            if (!twoIndex1.contains(audioData[1])) {
                                twoIndex1.add(audioData[1]);
                            }
                            break;
                        case 2:
                            if (!twoIndex2.contains(audioData[1])) {
                                twoIndex2.add(audioData[1]);
                            }
                            break;
                        case 3:
                            if (!twoIndex3.contains(audioData[1])) {
                                twoIndex3.add(audioData[1]);
                            }
                            break;
                        case 4:
                            if (!twoIndex4.contains(audioData[1])) {
                                twoIndex4.add(audioData[1]);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }

            if (mAudioNeedToSave) {
                try {
                    mAudioOS.write(audioData, 0, readSize);
                    //mAudioOS.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    mIsAudioWorking = false;
                    break;
                }
            }
            mAudioExtractor.advance();
        }// while(trur) end

        MLog.d(TAG, "audioWork() audioMaxSize: " + audioMaxSize);
        StringBuffer sb = new StringBuffer();
        for (byte one : oneIndex) {
            sb.append(" ");
            sb.append(one);
        }
        MLog.d(TAG, "audioWork()     oneIndex: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : twoIndex0) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "audioWork()    twoIndex0: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : twoIndex1) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "audioWork()    twoIndex1: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : twoIndex2) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "audioWork()    twoIndex2: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : twoIndex3) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "audioWork()    twoIndex3: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : twoIndex4) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "audioWork()    twoIndex4: " + sb.toString());

        mAudioExtractor.unselectTrack(mAudioTrackIndex);
        mAudioExtractor.release();
        if (mAudioNeedToSave) {
            try {
                if (mAudioOS != null) {
                    mAudioOS.flush();
                    mAudioOS.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MLog.w(TAG, "audioWork() end");
    }

    /***
     0 0 0 1 65 -101 6
     0 0 0 1 65 -101 23
     0 0 0 1 65 -97 39
     0 0 0 1 1 -97 54
     0 0 0 1 1 -97 -116
     0 0 0 1 1 -97 -83
     */
    private void videoWork() {
        MLog.w(TAG, "videoWork() start");
        long startDecodeTime = System.nanoTime();
        ByteBuffer room = ByteBuffer.allocate(BUFFER);
        byte[] videoData = new byte[BUFFER];
        int videoMaxSize = 0;
        mVideoExtractor.selectTrack(mVideoTrackIndex);
        ArrayList<Byte> oneIndex = new ArrayList<Byte>();
        ArrayList<Byte> twoIndex = new ArrayList<Byte>();
        ArrayList<Byte> threeIndex = new ArrayList<Byte>();
        ArrayList<Byte> fourIndex = new ArrayList<Byte>();
        while (true) {
            if (!mIsVideoWorking) {
                mIsVideoWorking = false;
                break;
            }

            room.clear();
            int readSize = mVideoExtractor.readSampleData(room, 0);
            // 21~106538 154689 71948 132333
            if (readSize < 0) {
                MLog.d(TAG, "videoWork() readSize: " + readSize);
                mIsVideoWorking = false;
                break;
            }
            if (videoMaxSize < readSize) {
                videoMaxSize = readSize;
            }
            Arrays.fill(videoData, (byte) 0);
            room.get(videoData, 0, readSize);
            if (mVideoNeedToPrint) {
                MLog.i(TAG, "videoWork() " +
                        "    " + videoData[0] +
                        " " + videoData[1] +
                        " " + videoData[2] +
                        " " + videoData[3] +
                        " " + videoData[4] +
                        " " + videoData[5] +
                        " " + videoData[6]);
            }

            if (!oneIndex.contains(videoData[0])) {
                oneIndex.add(videoData[0]);
            }
            if (!twoIndex.contains(videoData[1])) {
                twoIndex.add(videoData[1]);
            }
            if (!threeIndex.contains(videoData[2])) {
                threeIndex.add(videoData[2]);
            }
            if (!fourIndex.contains(videoData[3])) {
                fourIndex.add(videoData[3]);
            }

            if (mVideoNeedToSave) {
                try {
                    mVideoOS.write(videoData, 0, readSize);
                    //mVideoOS.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    mIsVideoWorking = false;
                    break;
                }
            }
            mVideoExtractor.advance();
        }// while(trur) end

        MLog.d(TAG, "videoWork() videoMaxSize: " + videoMaxSize);
        StringBuffer sb = new StringBuffer();
        for (byte one : oneIndex) {
            sb.append(" ");
            sb.append(one);
        }
        MLog.d(TAG, "videoWork()     oneIndex: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : twoIndex) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "videoWork()     twoIndex: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : threeIndex) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "videoWork()   threeIndex: " + sb.toString());
        sb = new StringBuffer();
        for (byte two : fourIndex) {
            sb.append(" ");
            sb.append(two);
        }
        MLog.d(TAG, "videoWork()    fourIndex: " + sb.toString());

        mVideoExtractor.unselectTrack(mVideoTrackIndex);
        mVideoExtractor.release();
        if (mVideoNeedToSave) {
            try {
                if (mVideoOS != null) {
                    mVideoOS.flush();
                    mVideoOS.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MLog.w(TAG, "videoWork() end");
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case PREPARE:
                if (internalPrepare()) {
                    mIsAudioWorking = true;
                    mIsVideoWorking = true;
                    new Thread(mAudioWork).start();
                    new Thread(mVideoWork).start();
                }
                break;
            default:
                break;
        }
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }
    }

    private Runnable mAudioWork = new Runnable() {
        @Override
        public void run() {
            audioWork();
        }
    };

    private Runnable mVideoWork = new Runnable() {
        @Override
        public void run() {
            videoWork();
        }
    };

    private MediaUtils.Callback mAudioCallback = new MediaUtils.Callback() {
        @Override
        public void onFormatChanged(MediaFormat newMediaFormat) {
            mAudioDncoderMediaFormat = newMediaFormat;
        }

        @Override
        public void onInputBuffer(int roomIndex, ByteBuffer room, MediaUtils.InputBufferInfo info) {
            mAudioExtractor.selectTrack(mAudioTrackIndex);
            info.size = mAudioExtractor.readSampleData(room, 0);
        }

        @Override
        public void onOutputBuffer(int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
            byte[] audioData = new byte[roomSize];
            room.get(audioData, 0, audioData.length);
            MLog.d(TAG, "mAudioCallback audioData.length: " + audioData.length);
            try {
                mAudioOS.write(audioData);
                mAudioOS.flush();
            } catch (IOException e) {
                e.printStackTrace();
                mIsAudioWorking = false;
            }
        }
    };

    private MediaUtils.Callback mVideoCallback = new MediaUtils.Callback() {
        @Override
        public void onFormatChanged(MediaFormat newMediaFormat) {
            mVideoDncoderMediaFormat = newMediaFormat;
        }

        @Override
        public void onInputBuffer(int roomIndex, ByteBuffer room, MediaUtils.InputBufferInfo info) {
            mVideoExtractor.selectTrack(mVideoTrackIndex);
            info.size = mVideoExtractor.readSampleData(room, 0);
        }

        @Override
        public void onOutputBuffer(int roomIndex, ByteBuffer room, MediaCodec.BufferInfo roomInfo, int roomSize) {
            byte[] videoData = new byte[roomSize];
            room.get(videoData, 0, videoData.length);
            MLog.d(TAG, "mVideoCallback videoData.length: " + videoData.length);
            try {
                mVideoOS.write(videoData);
                mVideoOS.flush();
            } catch (IOException e) {
                e.printStackTrace();
                mIsVideoWorking = false;
            }
        }
    };

}
