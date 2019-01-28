package com.weidi.usefragments.media;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.weidi.usefragments.test_fragment.scene2.IPlayerCallBack;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoPlayer {

    private static final String TAG = "VideoPlayer";
    private static final long TIMEOUT_US = 10000;
    private IPlayerCallBack callBack;
    private VideoThread videoThread;
    private AudioThread audioThread;
    private boolean isPlaying;
    private String mVideoPath;
    private Surface mSurface;

    public VideoPlayer(Surface mSurface, String mVideoPath) {
        this.mSurface = mSurface;
        this.mVideoPath = mVideoPath;
    }

    public VideoPlayer(Surface mSurface) {
        this.mSurface = mSurface;
    }

    public void setmVideoPath(String mVideoPath) {
        this.mVideoPath = mVideoPath;
    }

    public void setCallBack(IPlayerCallBack callBack) {
        this.callBack = callBack;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void start() {
        isPlaying = true;
        if (videoThread == null) {
            videoThread = new VideoThread();
            videoThread.start();
        }
        if (audioThread == null) {
            audioThread = new AudioThread();
            audioThread.start();
        }
    }

    public void play() {
        isPlaying = true;
        /*if (videoThread == null) {
            videoThread = new VideoThread();
            videoThread.start();
        }
        if (audioThread == null) {
            audioThread = new AudioThread();
            audioThread.start();
        }*/
    }

    public void stop() {
        isPlaying = false;
    }

    public void destroy() {
        stop();

        if (audioThread != null) audioThread.interrupt();
        if (videoThread != null) videoThread.interrupt();
    }

    // 获取指定类型媒体文件所在轨道
    private int getMediaTrackIndex(MediaExtractor videoExtractor, String MEDIA_TYPE) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(MEDIA_TYPE)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    // 延迟渲染
    private void sleepRender(MediaCodec.BufferInfo audioBufferInfo, long startMs) {
        while (audioBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /***
     将缓冲区传递至解码器
     如果到了文件末尾，返回true;否则返回false
     */
    private boolean putBufferToCoder(
            MediaExtractor extractor,
            MediaCodec decoder,
            ByteBuffer[] inputBuffers) {
        boolean isMediaEOS = false;
        // 在单位时间内找房间号
        int inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
        if (inputBufferIndex >= 0) {// 表示有房间空闲
            // 根据房间号找到房间
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            // 使用extractor对象把视频源数据存放到inputBuffer中,inputBuffer中的数据就是待解码的数据
            int sampleSize = extractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {// 到达文件末尾
                decoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isMediaEOS = true;
                Log.v(TAG, "media eos");
            } else {
                // 通知MediaCodec进行解码
                decoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        sampleSize,
                        extractor.getSampleTime(),
                        0);
                //
                extractor.advance();
            }
        }
        return isMediaEOS;
    }

    /***
     总结:
     一.解码流程
     总体步骤为:把源数据传给MediaCodec进行解码,从MediaCodec中获取解码后的数据.
     1.单位时间内查找房间号
     2.根据房间号找到房间
     3.打扫房间
     4.往房间里放东西(数据)
     5.通知有关部门东西已经放好,可以进行有关工作了
     */

    private class VideoThread extends Thread {
        @Override
        public void run() {
            if (mSurface == null || !mSurface.isValid()) {
                Log.e("TAG", "mSurface invalid!");
                return;
            }

            MediaExtractor videoExtractor = new MediaExtractor();
            MediaCodec videoDecode = null;
            try {
                videoExtractor.setDataSource(mVideoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int videoTrackIndex;
            // 获取视频所在轨道
            videoTrackIndex = getMediaTrackIndex(videoExtractor, "video/");
            if (videoTrackIndex >= 0) {
                MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
                int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                float time = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000;
                callBack.videoAspect(width, height, time);
                videoExtractor.selectTrack(videoTrackIndex);
                try {
                    videoDecode = MediaCodec.createDecoderByType(
                            mediaFormat.getString(MediaFormat.KEY_MIME));
                    videoDecode.configure(mediaFormat, mSurface, null, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (videoDecode == null) {
                Log.v(TAG, "MediaCodec null");
                return;
            }

            videoDecode.start();

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            // 先得到一幢房子
            ByteBuffer[] inputBuffers = videoDecode.getInputBuffers();
            // ByteBuffer[] outputBuffers = videoCodec.getOutputBuffers();
            boolean isVideoEOS = false;

            long startMs = System.currentTimeMillis();
            while (!Thread.interrupted()) {
                if (!isPlaying) {
                    // continue;
                    break;
                }

                // 将资源传递到解码器
                if (!isVideoEOS) {
                    isVideoEOS = putBufferToCoder(videoExtractor, videoDecode, inputBuffers);
                }

                int outputBufferIndex = videoDecode.dequeueOutputBuffer(videoBufferInfo,
                        TIMEOUT_US);
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.v(TAG, "超时");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.v(TAG, "format changed");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        //outputBuffers = videoCodec.getOutputBuffers();
                        Log.v(TAG, "output buffers changed");
                        break;
                    default:
                        //直接渲染到Surface时使用不到outputBuffer
                        //ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        //延时操作
                        //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                        sleepRender(videoBufferInfo, startMs);
                        //渲染
                        videoDecode.releaseOutputBuffer(outputBufferIndex, true);
                        break;
                }

                if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.v(TAG, "buffer stream end");
                    break;
                }
            }//end while

            videoDecode.stop();
            videoDecode.release();
            videoExtractor.release();
        }
    }

    private class AudioThread extends Thread {
        private int audioInputBufferSize;

        private AudioTrack audioTrack;

        @Override
        public void run() {
            MediaExtractor audioExtractor = new MediaExtractor();
            MediaCodec audioDecode = null;
            try {
                audioExtractor.setDataSource(mVideoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = audioExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i);
                    int audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int minBufferSize = AudioTrack.getMinBufferSize(
                            audioSampleRate,
                            (audioChannels == 1
                                    ? AudioFormat.CHANNEL_OUT_MONO
                                    : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT);
                    int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioInputBufferSize = minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
                    int frameSizeInBytes = audioChannels * 2;
                    audioInputBufferSize =
                            (audioInputBufferSize / frameSizeInBytes) * frameSizeInBytes;
                    audioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            audioSampleRate,
                            (audioChannels == 1
                                    ? AudioFormat.CHANNEL_OUT_MONO
                                    : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT,
                            audioInputBufferSize,
                            AudioTrack.MODE_STREAM);
                    audioTrack.play();
                    Log.v(TAG, "audio play");
                    //
                    try {
                        audioDecode = MediaCodec.createDecoderByType(mime);
                        audioDecode.configure(mediaFormat, null, null, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

            if (audioDecode == null) {
                Log.v(TAG, "audio decoder null");
                return;
            }

            audioDecode.start();
            //
            final ByteBuffer[] buffers = audioDecode.getOutputBuffers();
            int sz = buffers[0].capacity();
            if (sz <= 0)
                sz = audioInputBufferSize;
            byte[] mAudioOutTempBuf = new byte[sz];

            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = audioDecode.getInputBuffers();
            ByteBuffer[] outputBuffers = audioDecode.getOutputBuffers();
            boolean isAudioEOS = false;
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                if (!isPlaying) {
                    // continue;
                    break;
                }

                if (!isAudioEOS) {
                    isAudioEOS = putBufferToCoder(audioExtractor, audioDecode, inputBuffers);
                }

                //
                int outputBufferIndex = audioDecode.dequeueOutputBuffer(audioBufferInfo,
                        TIMEOUT_US);
                switch (outputBufferIndex) {
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.v(TAG, "超时");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.v(TAG, "format changed");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = audioDecode.getOutputBuffers();
                        Log.v(TAG, "output buffers changed");
                        break;
                    default:
                        // 解码后的数据
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        //延时操作
                        //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下
                        sleepRender(audioBufferInfo, startMs);
                        if (audioBufferInfo.size > 0) {
                            if (mAudioOutTempBuf.length < audioBufferInfo.size) {
                                mAudioOutTempBuf = new byte[audioBufferInfo.size];
                            }
                            outputBuffer.position(0);
                            outputBuffer.get(mAudioOutTempBuf, 0, audioBufferInfo.size);
                            outputBuffer.clear();
                            if (audioTrack != null)
                                // 播放mAudioOutTempBuf中的数据
                                audioTrack.write(mAudioOutTempBuf, 0, audioBufferInfo.size);
                        }
                        //
                        audioDecode.releaseOutputBuffer(outputBufferIndex, false);
                        break;
                }

                if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.v(TAG, "buffer stream end");
                    break;
                }
            }//end while

            audioDecode.stop();
            audioDecode.release();
            audioExtractor.release();
            audioTrack.stop();
            audioTrack.release();
        }

    }
}
