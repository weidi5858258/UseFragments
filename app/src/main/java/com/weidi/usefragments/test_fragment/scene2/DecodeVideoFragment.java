package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.socket.SocketClient;
import com.weidi.usefragments.tool.MLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/***
 视频解码
 */
public class DecodeVideoFragment extends BaseFragment {

    private static final String TAG =
            DecodeVideoFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public DecodeVideoFragment() {
        super();
    }

    /*********************************
     * Created
     *********************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG)
            MLog.d(TAG, "onAttach(): " + printThis() +
                    " mContext: " + context);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (DEBUG)
            MLog.d(TAG, "onAttach(): " + printThis() +
                    " activity: " + activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        initData();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG)
            MLog.d(TAG, "onCreateView(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewCreated(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        initView(view, savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewStateRestored(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onActivityCreated(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    /*********************************
     * Started
     *********************************/

    @Override
    public void onStart() {
        super.onStart();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onStart(): " + printThis());
    }

    /*********************************
     * Resumed
     *********************************/

    @Override
    public void onResume() {
        super.onResume();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onResume(): " + printThis());

        onShow();
    }

    /*********************************
     * Paused
     *********************************/

    @Override
    public void onPause() {
        super.onPause();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onPause(): " + printThis());
    }

    /*********************************
     * Stopped
     *********************************/

    @Override
    public void onStop() {
        super.onStop();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onStop(): " + printThis());

        onHide();
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView(): " + printThis());
    }

    @Override
    public void onDestroy() {
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + printThis());

        destroy();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach(): " + printThis());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            if (data != null) {
                MLog.d(TAG, "onActivityResult(): " + printThis() +
                        " requestCode: " + requestCode +
                        " resultCode: " + resultCode +
                        " data: " + data.toString());
            }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            MLog.d(TAG, "onSaveInstanceState(): " + printThis());
    }

    @Override
    public void handleConfigurationChangedEvent(
            Configuration newConfig,
            boolean needToDo,
            boolean override) {
        handleBeforeOfConfigurationChangedEvent();

        super.handleConfigurationChangedEvent(newConfig, needToDo, true);

        if (needToDo) {
            onShow();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG)
            MLog.d(TAG, "onLowMemory(): " + printThis());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DEBUG)
            MLog.d(TAG, "onTrimMemory(): " + printThis() +
                    " level: " + level);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (DEBUG)
            MLog.d(TAG, "onRequestPermissionsResult(): " + printThis() +
                    " requestCode: " + requestCode);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (DEBUG)
            MLog.d(TAG, "onHiddenChanged(): " + printThis() +
                    " hidden: " + hidden);

        if (hidden) {
            onHide();
        } else {
            onShow();
        }
    }

    @Override
    protected int provideLayout() {
        return R.layout.fragment_video_live_broadcasting;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    private static final int PREPARE = 0x0001;
    private static final int START_PLAY = 0x0002;
    private static final int STOP_PLAY = 0x0003;
    private static final int RELEASE_PLAY = 0x0004;

    @InjectView(R.id.surfaceView)
    private SurfaceView mSurfaceView;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private boolean mIsVideoDecoding = false;
    private boolean mIsAudioDecoding = false;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    //    private String mVideoPath = "/storage/2430-1702/BaiduNetdisk/music/谭咏麟 - 水中花.mp3";
    private String mVideoPath = "/storage/2430-1702/Download/shape_of_my_heart.mp4";
    /*"/storage/2430-1702/Android/data/com.weidi.usefragments/files/" +
            "output.mp4";*/
    //"08_mm-MP4-H264_720x400_2997_AAC-LC_192_48.mp4";
    // 竖屏时的分辨率
    private static int mWidth = 720;
    private static int mHeight = 1280;
    private Surface mSurface;
    // 必须要有两个MediaExtractor对象,不能共用同一个
    private MediaExtractor mVideoMediaExtractor;
    private MediaExtractor mAudioMediaExtractor;
    private MediaCodec mVideoDecoderMediaCodec;
    private MediaCodec mAudioDecoderMediaCodec;
    private MediaFormat mVideoDecoderMediaFormat;
    private MediaFormat mAudioDecoderMediaFormat;
    private AudioTrack mAudioTrack;
    private int mOutputVideoTrack = -1;
    private int mOutputAudioTrack = -1;
    private int bufferSizeInBytes;

    private Object mVideoDecoderLock = new Object();
    private Object mAudioDecoderLock = new Object();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
            // 横屏
            mJumpBtn.setText("跳\n转\n到");
            // 强制为竖屏
            /*getAttachedActivity().setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);*/
        } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
            // 竖屏
            mJumpBtn.setText("跳转到");
            // 强制为横屏
            /*getAttachedActivity().setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);*/
        }
    }

    /***
     代码执行的内容跟onPause(),onStop()一样,
     因此在某些情况下要么执行onPause(),onStop()方法,要么执行onHide()方法.
     一般做的事是视频的暂停,摄像头的关闭
     */
    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide() " + printThis());
    }

    private void initData() {
        SocketClient.getInstance();

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                DecodeVideoFragment.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                DecodeVideoFragment.this.uiHandleMessage(msg);
            }
        };
    }

    private void initView(View view, Bundle savedInstanceState) {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(
                    SurfaceHolder holder) {
                mSurface = holder.getSurface();
                mThreadHandler.removeMessages(PREPARE);
                mThreadHandler.sendEmptyMessage(PREPARE);
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(
                    SurfaceHolder holder) {

            }
        });
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        if (mHandlerThread != null
                && mVideoDecoderMediaCodec == null
                && mAudioDecoderMediaCodec == null) {
            mHandlerThread.quit();
        }

        releasePlay();
    }

    @InjectOnClick({R.id.start_btn, R.id.stop_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn:
                break;
            case R.id.stop_btn:
                mThreadHandler.sendEmptyMessage(STOP_PLAY);
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new ThrowingScreenFragment());
                break;
        }
    }

    private void prepare() {
        // test
        MLog.d(TAG, "Video Codec Name---------------------------------------------------");
        MediaCodecInfo[] mediaCodecInfos =
                MediaUtils.findEncodersByMimeType(MediaUtils.VIDEO_MIME_TYPE);
        for (MediaCodecInfo info : mediaCodecInfos) {
            MLog.d(TAG, "prepare() " + printThis() +
                    " " + info.getName());
        }
        MLog.d(TAG, "Audio Codec Name---------------------------------------------------");
        mediaCodecInfos =
                MediaUtils.findEncodersByMimeType(MediaUtils.AUDIO_MIME_TYPE);
        for (MediaCodecInfo info : mediaCodecInfos) {
            MLog.d(TAG, "prepare() " + printThis() +
                    " " + info.getName());
        }

        if (DEBUG)
            MLog.d(TAG, "prepare() start");

        mIsVideoDecoding = false;
        mIsAudioDecoding = false;

        if (TextUtils.isEmpty(mVideoPath)) {
            return;
        }

        // video
        mVideoMediaExtractor = new MediaExtractor();
        try {
            mVideoMediaExtractor.setDataSource(mVideoPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int tracKCount = mVideoMediaExtractor.getTrackCount();
        for (int i = 0; i < tracKCount; i++) {
            MediaFormat format = mVideoMediaExtractor.getTrackFormat(i);
            if (format == null) {
                continue;
            }
            String mime = format.getString(MediaFormat.KEY_MIME);
            MLog.d(TAG, "prepare() mime: " + mime);
            if (TextUtils.isEmpty(mime)) {
                continue;
            }
            if (mime.startsWith("video/")) {
                mVideoMediaExtractor.selectTrack(i);
                mVideoDecoderMediaFormat = format;
                mOutputVideoTrack = i;
                MLog.d(TAG, "prepare() video: " + format);
                try {
                    mVideoDecoderMediaCodec =
                            MediaCodec.createDecoderByType(mime);
                    mVideoDecoderMediaCodec.configure(
                            mVideoDecoderMediaFormat,
                            mSurface,
                            null,
                            0);
                    mVideoDecoderMediaCodec.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mVideoDecoderMediaCodec != null) {
                        mVideoDecoderMediaCodec.release();
                        mVideoDecoderMediaCodec = null;
                    }
                    mVideoDecoderMediaFormat = null;
                    mOutputVideoTrack = -1;
                    return;
                }
                break;
            }
        }

        // audio
        mAudioMediaExtractor = new MediaExtractor();
        try {
            mAudioMediaExtractor.setDataSource(mVideoPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        tracKCount = mAudioMediaExtractor.getTrackCount();
        for (int i = 0; i < tracKCount; i++) {
            MediaFormat format = mAudioMediaExtractor.getTrackFormat(i);
            if (format == null) {
                continue;
            }
            String mime = format.getString(MediaFormat.KEY_MIME);
            MLog.d(TAG, "prepare() mime: " + mime);
            if (TextUtils.isEmpty(mime)) {
                continue;
            }
            if (mime.startsWith("audio/")) {
                mAudioMediaExtractor.selectTrack(i);
                mAudioDecoderMediaFormat = format;
                mOutputAudioTrack = i;
                MLog.d(TAG, "prepare() audio: " + format);
                try {
                    mAudioDecoderMediaCodec =
                            MediaCodec.createDecoderByType(mime);
                    mAudioDecoderMediaCodec.configure(
                            mAudioDecoderMediaFormat,
                            null,
                            null,
                            0);
                    mAudioDecoderMediaCodec.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mAudioDecoderMediaCodec != null) {
                        mAudioDecoderMediaCodec.release();
                        mAudioDecoderMediaCodec = null;
                    }
                    mAudioDecoderMediaFormat = null;
                    mOutputAudioTrack = -1;
                    return;
                }
                break;
            }
        }

        /***
         至于"csd-0"和"csd-1"是什么，对于H264视频的话，
         它对应的是sps和pps，对于AAC音频的话，对应的是ADTS，
         做音视频开发的人应该都知道，它一般存在于编码器生成的IDR帧之中。
         */
        if (mVideoDecoderMediaFormat != null) {
            ByteBuffer byteBuffer0 = mVideoDecoderMediaFormat.getByteBuffer("csd-0");
            ByteBuffer byteBuffer1 = mVideoDecoderMediaFormat.getByteBuffer("csd-1");
            byte[] sps = new byte[byteBuffer0.limit()];
            byte[] pps = new byte[byteBuffer1.limit()];
            byteBuffer0.get(sps);
            byteBuffer1.get(pps);
            for (int i = 0; i < sps.length; i++) {
                MLog.d(TAG, "prepare() sps: " + sps[i]);
            }
            for (int i = 0; i < pps.length; i++) {
                MLog.d(TAG, "prepare() pps: " + pps[i]);
            }
        }

        if (mAudioDecoderMediaFormat != null) {
            int streamType = AudioManager.STREAM_MUSIC;
            int sampleRateInHz =
                    mAudioDecoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount =
                    mAudioDecoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int mode = AudioTrack.MODE_STREAM;
            bufferSizeInBytes =
                    MediaUtils.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat) * 2;
            MLog.d(TAG, "prepare() bufferSizeInBytes: " + bufferSizeInBytes);
            if (bufferSizeInBytes > 0) {
                mAudioDecoderMediaFormat.setInteger(
                        MediaFormat.KEY_MAX_INPUT_SIZE, bufferSizeInBytes);
            }
            // AudioRecord
            mAudioTrack = MediaUtils.createAudioTrack(
                    streamType, sampleRateInHz, channelCount, audioFormat, mode);
            if (mAudioTrack != null) {
                mAudioTrack.play();
            }
        }

        mUiHandler.removeMessages(START_PLAY);
        mUiHandler.sendEmptyMessageDelayed(START_PLAY, 1000);

        if (DEBUG)
            MLog.d(TAG, "prepare() end");
    }

    /***
     * 从socket读byte数组
     *
     * @param in
     * @param length
     * @return
     */
    private static byte[] readBytes(InputStream in, long length) {
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

    private int bytesToInt(byte[] bytes) {
        int i = 0;
        i = (int) ((bytes[0] & 0xff)
                | ((bytes[1] & 0xff) << 8)
                | ((bytes[2] & 0xff) << 16)
                | ((bytes[3] & 0xff) << 24));
        return i;
    }

    private void startPlay() {
        if (mIsVideoDecoding
                && mIsAudioDecoding) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "startPlay() start");

        mIsVideoDecoding = true;
        mIsAudioDecoding = true;

        /*if (mAudioTrack != null) {
            mAudioTrack.play();
        }
        if (mAudioDecoderMediaCodec != null) {
            mIsAudioDecoding = true;
            mAudioDecoderMediaCodec.start();
        }
        if (mVideoDecoderMediaCodec != null) {
            mIsVideoDecoding = true;
            mVideoDecoderMediaCodec.start();
        }*/

        if (mVideoDecoderMediaCodec != null
                && mSurface != null) {
            new Thread(new VideoDecoderThread()).start();
        }
        if (mAudioDecoderMediaCodec != null
                && mAudioTrack != null) {
            new Thread(new AudioDecoderThread()).start();
        }

        if (DEBUG)
            MLog.d(TAG, "startPlay() end");
    }

    private void stopPlay() {
        if (DEBUG)
            MLog.d(TAG, "stopPlay() start");

        mIsVideoDecoding = false;
        mIsAudioDecoding = false;
        mOutputVideoTrack = -1;
        mOutputAudioTrack = -1;

        synchronized (mVideoDecoderLock) {
            try {
                MLog.d(TAG, "stopPlay mVideoDecoderLock.wait() start");
                mVideoDecoderLock.wait();
                MLog.d(TAG, "stopPlay mVideoDecoderLock.wait() end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        synchronized (mAudioDecoderLock) {
            try {
                MLog.d(TAG, "stopPlay mAudioDecoderLock.wait() start");
                mAudioDecoderLock.wait();
                MLog.d(TAG, "stopPlay mAudioDecoderLock.wait() end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // notifyVideoEndOfStream();
        // notifyAudioEndOfStream();
        MediaUtils.stopMediaCodec(mVideoDecoderMediaCodec);
        MediaUtils.stopMediaCodec(mAudioDecoderMediaCodec);
        MediaUtils.stopAudioTrack(mAudioTrack);

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "成功停止", Toast.LENGTH_SHORT).show();
                onShow();
            }
        });

        if (DEBUG)
            MLog.d(TAG, "stopPlay() end");
    }

    private void releasePlay() {
        MediaUtils.releaseMediaCodec(mVideoDecoderMediaCodec);
        MediaUtils.releaseMediaCodec(mAudioDecoderMediaCodec);
        MediaUtils.releaseAudioTrack(mAudioTrack);
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case PREPARE:
                prepare();
                break;
            case STOP_PLAY:
                stopPlay();
                break;
            case RELEASE_PLAY:
                releasePlay();
                break;
            default:
                break;
        }
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case START_PLAY:
                startPlay();
                break;
            default:
                break;
        }
    }

    private class VideoDecoderThread implements Runnable {
        @Override
        public void run() {
            MLog.i(TAG, "VideoDecoderThread start");
            int readSize = -1;
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            ByteBuffer room = null;
            MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
            long startMs = System.currentTimeMillis();
            // 保持纵横比
            // 此方法必须在configure和start之后执行才有效
            mVideoDecoderMediaCodec.setVideoScalingMode(
                    MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

            while (mIsVideoDecoding) {
                if (!mIsVideoDecoding) {
                    synchronized (mVideoDecoderLock) {
                        MLog.d(TAG, "VideoDecoderThread mVideoDecoderLock.notify()");
                        mVideoDecoderLock.notify();
                    }
                    break;
                }

                // Input过程
                try {
                    mVideoMediaExtractor.selectTrack(mOutputVideoTrack);
                    // 设置解码等待时间，0为不等待，-1为一直等待，其余为时间单位
                    roomIndex = mVideoDecoderMediaCodec.dequeueInputBuffer(10000);
                    if (roomIndex >= 0) {
                        room = mVideoDecoderMediaCodec.getInputBuffer(roomIndex);
                        room.clear();
                        readSize = mVideoMediaExtractor.readSampleData(room, 0);
                        if (readSize >= 0) {
                            mVideoDecoderMediaCodec.queueInputBuffer(
                                    roomIndex,
                                    0,
                                    readSize,
                                    mVideoMediaExtractor.getSampleTime(),
                                    0);
                            mVideoMediaExtractor.advance();
                        } else {
                            mVideoDecoderMediaCodec.queueInputBuffer(
                                    roomIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            mIsVideoDecoding = false;
                            break;
                        }
                    }
                } catch (MediaCodec.CryptoException e) {
                    e.printStackTrace();
                    MLog.e(TAG, "VideoDecoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                } catch (MediaCodec.CodecException e) {
                    e.printStackTrace();
                    MLog.e(TAG, "VideoDecoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    MLog.e(TAG, "VideoDecoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                }

                // Output过程
                for (; ; ) {
                    if (!mIsVideoDecoding) {
                        synchronized (mVideoDecoderLock) {
                            MLog.d(TAG, "VideoDecoderThread mVideoDecoderLock.notify()");
                            mVideoDecoderLock.notify();
                        }
                        break;
                    }
                    try {
                        roomIndex = mVideoDecoderMediaCodec.dequeueOutputBuffer(
                                roomInfo, 10000);
                        if (roomIndex < 0) {
                            break;
                        }
                        room = mVideoDecoderMediaCodec.getOutputBuffer(roomIndex);

                        /***
                         roomInfo.presentationTimeUs        ---> 微妙
                         roomInfo.presentationTimeUs / 1000 ---> 毫秒
                         roomInfo.presentationTimeUs / 1000 某帧将要显示的时间点
                         System.currentTimeMillis()         当前的时间点
                         这样去理解:
                         某个产品只需要在明天的某个时间点完成就行了,
                         但是实际是这个产品在今天的某个时间点就完成了,
                         这就说明了工作麻利,提前完成任务了,也就是"快"的意思.
                         类比现在的情况:
                         "某帧将要显示的时间点"比"当前的时间点"大,
                         说明显示过早了,应该等一等再显示.
                         */
                        while (roomInfo.presentationTimeUs / 1000
                                > System.currentTimeMillis() - startMs) {
                            SystemClock.sleep(10);
                        }

                        mVideoDecoderMediaCodec.releaseOutputBuffer(roomIndex, true);
                    } catch (MediaCodec.CodecException e) {
                        e.printStackTrace();
                        MLog.e(TAG, "VideoDecoderThread Output occur exception: " + e);
                        mIsVideoDecoding = false;
                        break;
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        MLog.e(TAG, "VideoDecoderThread Output occur exception: " + e);
                        mIsVideoDecoding = false;
                        break;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        MLog.e(TAG, "VideoEncoderThread Output occur exception: " + e);
                        mIsVideoDecoding = false;
                        break;
                    }
                }
            }
            MLog.i(TAG, "VideoDecoderThread end");
        }
    }

    private class AudioDecoderThread implements Runnable {
        @Override
        public void run() {
            MLog.i(TAG, "AudioDecoderThread start");
            int readSize = -1;
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            ByteBuffer room = null;
            MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
            mAudioMediaExtractor.selectTrack(mOutputAudioTrack);

            while (mIsAudioDecoding) {
                if (!mIsAudioDecoding) {
                    synchronized (mAudioDecoderLock) {
                        MLog.d(TAG, "AudioDecoderThread mAudioDecoderLock.notify()");
                        mAudioDecoderLock.notify();
                    }
                    break;
                }

                // Input过程
                try {
                    roomIndex = mAudioDecoderMediaCodec.dequeueInputBuffer(10000);
                    if (roomIndex >= 0) {
                        room = mAudioDecoderMediaCodec.getInputBuffer(roomIndex);
                        room.clear();
                        readSize = mAudioMediaExtractor.readSampleData(room, 0);
                        long presentationTimeUs = System.nanoTime() / 1000;
                        if (readSize >= 0) {
                            // 通知MediaCodec进行编码
                            mAudioDecoderMediaCodec.queueInputBuffer(
                                    roomIndex,
                                    0,
                                    readSize,
                                    presentationTimeUs,
                                    0);
                            mAudioMediaExtractor.advance();
                        } else {
                            mAudioDecoderMediaCodec.queueInputBuffer(
                                    roomIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            mIsAudioDecoding = false;
                            break;
                        }
                    }
                } catch (MediaCodec.CryptoException e) {
                    e.printStackTrace();
                    MLog.e(TAG, "AudioDecoderThread Input occur exception: " + e);
                    mIsAudioDecoding = false;
                    break;
                } catch (MediaCodec.CodecException e) {
                    e.printStackTrace();
                    MLog.e(TAG, "AudioDecoderThread Input occur exception: " + e);
                    mIsAudioDecoding = false;
                    break;
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    MLog.e(TAG, "AudioDecoderThread Input occur exception: " + e);
                    mIsAudioDecoding = false;
                    break;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    MLog.e(TAG, "AudioDecoderThread Input occur exception: " + e);
                    mIsAudioDecoding = false;
                    break;
                }

                // Output过程
                for (; ; ) {
                    if (!mIsAudioDecoding) {
                        synchronized (mAudioDecoderLock) {
                            MLog.d(TAG, "AudioDecoderThread mAudioDecoderLock.notify()");
                            mAudioDecoderLock.notify();
                        }
                        break;
                    }
                    try {
                        roomIndex = mAudioDecoderMediaCodec.dequeueOutputBuffer(
                                roomInfo, 10000);
                        if (roomIndex < 0) {
                            break;
                        }

                        room = mAudioDecoderMediaCodec.getOutputBuffer(roomIndex);
                        int roomSize = roomInfo.size;
                        room.position(roomInfo.offset);
                        room.limit(roomInfo.offset + roomSize);
                        byte[] pcmData = new byte[roomSize];
                        room.get(pcmData, 0, roomSize);
                        mAudioTrack.write(pcmData, 0, roomSize);

                        mAudioDecoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                    } catch (MediaCodec.CodecException e) {
                        e.printStackTrace();
                        MLog.e(TAG, "AudioDecoderThread Output occur exception: " + e);
                        mIsAudioDecoding = false;
                        break;
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        MLog.e(TAG, "AudioDecoderThread Output occur exception: " + e);
                        mIsAudioDecoding = false;
                        break;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        MLog.e(TAG, "AudioDecoderThread Output occur exception: " + e);
                        mIsAudioDecoding = false;
                        break;
                    }
                }
            }
            MLog.i(TAG, "AudioDecoderThread end");
        }
    }

}
