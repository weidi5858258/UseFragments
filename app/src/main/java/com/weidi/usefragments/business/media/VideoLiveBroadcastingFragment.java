package com.weidi.usefragments.business.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;


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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/***
 视频直播
 */
public class VideoLiveBroadcastingFragment extends BaseFragment {

    private static final String TAG =
            VideoLiveBroadcastingFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public VideoLiveBroadcastingFragment() {
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

        activityResult(requestCode, resultCode, data);
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

    private static final int REQUEST_CODE = 1000;

    private static final int PREPARE = 0x0001;
    private static final int START_RECORD_SCREEN = 0x0002;
    private static final int STOP_RECORD_SCREEN = 0x0003;

    @InjectView(R.id.surfaceView)
    private SurfaceView mSurfaceView;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private boolean mIsVideoDecoding = false;
    private boolean mIsAudioDecoding = false;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    // 竖屏时的分辨率
    private static final int mWidth = 720;
    private static final int mHeight = 1280;
    private Surface mSurface;
    private MediaCodec mVideoDecoderMediaCodec;
    private MediaCodec mAudioDecoderMediaCodec;
    private MediaFormat mVideoDecoderMediaFormat;
    private MediaFormat mAudioDecoderMediaFormat;
    private AudioTrack mAudioTrack;
    private int mOutputVideoTrack = -1;
    private int mOutputAudioTrack = -1;
    private Socket mSocket;

    private Object mMediaMuxerLock = new Object();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

        mJumpBtn.setText("跳转到");
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
                VideoLiveBroadcastingFragment.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                VideoLiveBroadcastingFragment.this.uiHandleMessage(msg);
            }
        };
    }

    private void initView(View view, Bundle savedInstanceState) {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurface = holder.getSurface();
                mThreadHandler.sendEmptyMessage(PREPARE);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

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
    }

    @InjectOnClick({R.id.start_btn, R.id.stop_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn:
                break;
            case R.id.stop_btn:
                mThreadHandler.sendEmptyMessage(STOP_RECORD_SCREEN);
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new RecordScreenFragment());
                break;
        }
    }

    private void prepare() {
        // test
        MLog.d(TAG, "Video Codec Name---------------------------------------------------");
        MediaCodecInfo[] mediaCodecInfos =
                MediaUtils.findAllEncodersByMime(MediaUtils.VIDEO_MIME);
        for (MediaCodecInfo info : mediaCodecInfos) {
            MLog.d(TAG, "prepare() " + printThis() +
                    " " + info.getName());
        }
        MLog.d(TAG, "Audio Codec Name---------------------------------------------------");
        mediaCodecInfos =
                MediaUtils.findAllEncodersByMime(MediaUtils.AUDIO_MIME);
        for (MediaCodecInfo info : mediaCodecInfos) {
            MLog.d(TAG, "prepare() " + printThis() +
                    " " + info.getName());
        }

        // 编码器那边会先发sps和pps来,头一帧就由sps和pps组成
        /*int spsLength = bytesToInt(sServer.readLength());
        byte[] sps = sServer.readSPSPPS(spsLength);
        mSps = Arrays.copyOfRange(sps, 4, spsLength);
        int ppsLength = bytesToInt(sServer.readLength());
        byte[] pps = sServer.readSPSPPS(ppsLength);
        mPps = Arrays.copyOfRange(pps, 4, ppsLength);*/

        /*MediaFormat format = MediaFormat.createVideoFormat(
                MediaUtils.VIDEO_MIME, mWidth, mHeight);
        // 房间的大小要大于等于width * height时,才能一次性存放需要解码的数据
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, mWidth * mHeight);
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, mWidth);
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, mHeight);
        format.setByteBuffer("csd-0", ByteBuffer.wrap(mSps));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(mPps));
        MediaCodec decoder = null;
        try {
            decoder = MediaCodec.createDecoderByType(MediaUtils.VIDEO_MIME);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(decoder==null){
            return;
        }
        decoder.configure(format, mSurface, null, 0);
        decoder.start();*/

        SocketClient.getInstance().connect();
        mSocket = SocketClient.getInstance().getSocket();
        if (mSocket == null || !mSocket.isConnected()) {
            return;
        }

        byte[] mSps = null;
        byte[] mPps = null;
        try {
            InputStream inputStream = mSocket.getInputStream();
            /*int spsLength = bytesToInt(readBytes(inputStream, 4));
            MLog.d(TAG, "startRecordScreen() spsLength: " + spsLength);
            int ppsLength = bytesToInt(readBytes(inputStream, 4));
            MLog.d(TAG, "startRecordScreen() ppsLength: " + ppsLength);
            byte[] sps = readBytes(inputStream, spsLength);
            mSps = Arrays.copyOfRange(sps, 4, spsLength);
            byte[] pps = readBytes(inputStream, ppsLength);
            mPps = Arrays.copyOfRange(pps, 4, ppsLength);*/

            int spsLength = inputStream.read();
            MLog.d(TAG, "prepare() spsLength: " + spsLength);
            int ppsLength = inputStream.read();
            MLog.d(TAG, "prepare() ppsLength: " + ppsLength);
            byte[] sps_pps_data = new byte[spsLength + ppsLength];
            inputStream.read(sps_pps_data, 0, sps_pps_data.length);

            mSps = Arrays.copyOfRange(sps_pps_data, 4, spsLength);
            mPps = Arrays.copyOfRange(sps_pps_data, spsLength + 4, spsLength + ppsLength);

            for (int i = 0; i < mSps.length; i++) {
                MLog.d(TAG, "prepare() " + mSps[i]);
            }
            MLog.d(TAG, "prepare()---------------------------------");
            for (int i = 0; i < mPps.length; i++) {
                MLog.d(TAG, "prepare() " + mPps[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // AudioRecord
        mAudioTrack = MediaUtils.createAudioTrack();
        if (mAudioTrack == null) {
            return;
        }

        // MediaCodec
        mVideoDecoderMediaCodec = MediaUtils.getVideoDecoderMediaCodec();
        mAudioDecoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec();
        // MediaFormat
        mVideoDecoderMediaFormat = MediaUtils.getVideoDecoderMediaFormat(mWidth, mHeight);
        if (mSps != null && mPps != null) {
            mVideoDecoderMediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(mSps));
            mVideoDecoderMediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(mPps));
        }
        mAudioDecoderMediaFormat = MediaUtils.getAudioDecoderMediaFormat();

        if (mVideoDecoderMediaCodec == null
                || mAudioDecoderMediaCodec == null) {
            return;
        }
        try {
            mVideoDecoderMediaCodec.configure(
                    mVideoDecoderMediaFormat,
                    mSurface,
                    null,
                    0);
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
            if (mVideoDecoderMediaCodec != null) {
                mVideoDecoderMediaCodec.release();
                mVideoDecoderMediaCodec = null;
            }
            return;
        }
        try {
            mAudioDecoderMediaCodec.configure(
                    mAudioDecoderMediaFormat,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
            if (mAudioDecoderMediaCodec != null) {
                mAudioDecoderMediaCodec.release();
                mAudioDecoderMediaCodec = null;
            }
            return;
        }

        mUiHandler.removeMessages(START_RECORD_SCREEN);
        mUiHandler.sendEmptyMessage(START_RECORD_SCREEN);
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

    private void startRecordScreen() {
        if (mIsVideoDecoding) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "startRecordScreen() " + printThis());

        mIsVideoDecoding = true;

        /*if (mAudioTrack != null) {
            mAudioTrack.play();
        }
        if (mAudioDecoderMediaCodec != null) {
            mAudioDecoderMediaCodec.start();
        }*/
        if (mVideoDecoderMediaCodec != null) {
            mVideoDecoderMediaCodec.start();
        }

        if (mVideoDecoderMediaCodec != null
                && mAudioDecoderMediaCodec != null
                && mAudioTrack != null
                && mSurface != null) {
            // 音频先启动,让音频的mOutputAudioTrack先得到值
            // new Thread(new AudioEncoderThread()).start();
            new Thread(new VideoDecoderThread()).start();
        }
    }

    private void stopRecordScreen() {
        if (!mIsVideoDecoding) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "stopRecordScreen() start " + printThis());

        mIsVideoDecoding = false;
        mOutputVideoTrack = -1;
        mOutputAudioTrack = -1;

        synchronized (mMediaMuxerLock) {
            try {
                MLog.d(TAG, "stopRecordScreen mMediaMuxerLock.wait() start");
                mMediaMuxerLock.wait();
                MLog.d(TAG, "stopRecordScreen mMediaMuxerLock.wait() end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //notifyVideoEndOfStream();
        if (mVideoDecoderMediaCodec != null) {
            mVideoDecoderMediaCodec.release();
            mVideoDecoderMediaCodec = null;
        }

        //notifyAudioEndOfStream();
        if (mAudioDecoderMediaCodec != null) {
            mAudioDecoderMediaCodec.release();
            mAudioDecoderMediaCodec = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "成功停止", Toast.LENGTH_SHORT).show();
                onShow();
            }
        });

        if (DEBUG)
            MLog.d(TAG, "stopRecordScreen() end   " + printThis());
    }

    private MediaProjection.Callback mMediaProjectionCallback =
            new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    if (DEBUG)
                        MLog.d(TAG, "MediaProjection.Callback onStop() " + printThis());
                }
            };

    private void activityResult(int requestCode, int resultCode, Intent data) {
        // requestCode: 1000 resultCode: -1 data: Intent { (has extras) }
        if (requestCode != REQUEST_CODE) {
            return;
        }

        mThreadHandler.sendEmptyMessage(START_RECORD_SCREEN);
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case PREPARE:
                prepare();
                break;
            case START_RECORD_SCREEN:
                startRecordScreen();
                break;
            case STOP_RECORD_SCREEN:
                stopRecordScreen();
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
            case START_RECORD_SCREEN:
                startRecordScreen();
                break;
            case STOP_RECORD_SCREEN:
                stopRecordScreen();
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
            byte[] data = new byte[mWidth * mHeight];
            while (mIsVideoDecoding) {
                // Input过程
                try {
                    if (mSocket == null || !mSocket.isConnected()) {
                        mIsVideoDecoding = false;
                        break;
                    }
                    InputStream inputStream = mSocket.getInputStream();
                    readSize = inputStream.read(data, 0, data.length);
                    MLog.i(TAG, "VideoDecoderThread Input readSize: " + readSize);
                    roomIndex = mVideoDecoderMediaCodec.dequeueInputBuffer(-1);
                    if (roomIndex >= 0) {
                        room = mVideoDecoderMediaCodec.getInputBuffer(roomIndex);
                        room.clear();
                        room.put(data);
                        long presentationTime = System.nanoTime() / 1000;
                        mVideoDecoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                readSize,
                                presentationTime,
                                0);
                    }
                } catch (MediaCodec.CryptoException e) {
                    MLog.e(TAG, "VideoEncoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                } catch (MediaCodec.CodecException e) {
                    MLog.e(TAG, "VideoEncoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                } catch (IllegalStateException e) {
                    MLog.e(TAG, "VideoEncoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                } catch (IOException e) {
                    MLog.e(TAG, "VideoEncoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                }

                // Output过程
                for (; ; ) {
                    try {
                        roomIndex = mVideoDecoderMediaCodec.dequeueOutputBuffer(
                                roomInfo, 33333);
                        MLog.i(TAG, "VideoDecoderThread Output roomIndex: " + roomIndex);
                        /*switch (roomIndex) {
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                MLog.d(TAG, "VideoDecoderThread " +
                                        "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                                mVideoDecoderMediaFormat =
                                        mVideoDecoderMediaCodec.getOutputFormat();
                                break;
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                MLog.d(TAG, "VideoDecoderThread " +
                                        "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                                break;
                            default:
                                break;
                        }*/

                        if (roomIndex < 0) {
                            break;
                        }
                        room = mVideoDecoderMediaCodec.getOutputBuffer(roomIndex);

                        /*if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            MLog.d(TAG, "VideoDecoderThread " +
                                    "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                            mIsVideoDecoding = false;
                            break;
                        }
                        if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            MLog.d(TAG, "VideoDecoderThread " +
                                    "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                            mVideoDecoderMediaCodec.releaseOutputBuffer(roomIndex, true);
                            continue;
                        }*/

                        mVideoDecoderMediaCodec.releaseOutputBuffer(roomIndex, true);
                    } catch (MediaCodec.CodecException e) {
                        MLog.e(TAG, "VideoDecoderThread Output occur exception: " + e);
                        mIsVideoDecoding = false;
                        break;
                    } catch (IllegalStateException e) {
                        MLog.e(TAG, "VideoDecoderThread Output occur exception: " + e);
                        mIsVideoDecoding = false;
                        break;
                    } catch (IllegalArgumentException e) {
                        MLog.e(TAG, "VideoEncoderThread Output occur exception: " + e);
                        mIsVideoDecoding = false;
                        break;
                    }
                }
                if (!mIsVideoDecoding) {
                    synchronized (mMediaMuxerLock) {
                        MLog.d(TAG, "VideoDecoderThread mMediaMuxerLock.notify()");
                        mMediaMuxerLock.notify();
                    }
                    break;
                }
            }
            MLog.i(TAG, "VideoDecoderThread end");
        }
    }

    private class AudioEncoderThread implements Runnable {
        @Override
        public void run() {
            MLog.d(TAG, "AudioEncoderThread start");
            int readSize = -1;
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            ByteBuffer room = null;
            /***
             下面所做的事(包括onlyOne代码)目的是为了让
             buffer空间大小跟room空间大小一致,
             这样就能一次性把buffer的数据放到room中去.
             如果buffer的空间大小大于room的空间大小,
             那么处理起来会很麻烦.
             */
            boolean onlyOne = true;
            byte[] buffer = new byte[MediaUtils.getMinBufferSize() * 2];
            MLog.d(TAG, "AudioEncoderThread first  setup buffer.length: " + buffer.length);
            ByteBuffer[] inputBuffers = mAudioDecoderMediaCodec.getInputBuffers();
            if (inputBuffers != null
                    && inputBuffers.length > 0
                    && inputBuffers[0] != null
                    && buffer.length > inputBuffers[0].limit()) {
                buffer = new byte[inputBuffers[0].limit()];
                MLog.d(TAG, "AudioEncoderThread second setup buffer.length: " + buffer.length);
            }

            while (mIsVideoDecoding) {
                // 取数据过程
                if (buffer != null) {
                    //Arrays.fill(buffer, (byte) 0);
                    //readSize = mAudioTrack.read(buffer, 0, buffer.length);
                    if (readSize < 0) {
                        MLog.d(TAG, "AudioEncoderThread readSize: " + readSize);
                        mIsVideoDecoding = false;
                        break;
                    }
                }

                // Input过程
                try {
                    roomIndex = mAudioDecoderMediaCodec.dequeueInputBuffer(-1);
                    if (roomIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        room = mAudioDecoderMediaCodec.getInputBuffer(roomIndex);

                        if (onlyOne) {
                            onlyOne = false;
                            int roomSize = room.limit();
                            if (roomSize < buffer.length) {
                                buffer = new byte[roomSize];
                                MLog.d(TAG, "AudioEncoderThread three  buffer.length: " +
                                        buffer.length);
                                //readSize = mAudioTrack.read(buffer, 0, buffer.length);
                                if (readSize < 0) {
                                    MLog.d(TAG, "AudioEncoderThread readSize: " + readSize);
                                    mIsVideoDecoding = false;
                                    break;
                                }
                            }
                        }

                        room.clear();
                        room.put(buffer);
                        long presentationTimeUs = System.nanoTime() / 1000;
                        // 通知MediaCodec进行编码
                        mAudioDecoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                buffer.length,
                                presentationTimeUs,
                                0);
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException e) {
                    MLog.e(TAG, "AudioEncoderThread Input occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                }

                // Output过程
                try {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    // dequeueOutputBuffer方法不能用于async mode
                    roomIndex = mAudioDecoderMediaCodec.dequeueOutputBuffer(
                            bufferInfo, 10000);
                    // 先处理负值
                    switch (roomIndex) {
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            // 请重试
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.INFO_TRY_AGAIN_LATER");
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            // 格式已经更改(首先执行)
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            mAudioDecoderMediaFormat = mAudioDecoderMediaCodec.getOutputFormat();
                            continue;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            // 输出缓冲区已经改变
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            //outputBuffers = mAudioDecoderMediaCodec.getOutputBuffers();
                            continue;
                        default:
                            break;
                    }
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // 会执行一次
                        MLog.d(TAG, "AudioEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                        mAudioDecoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                        continue;
                    }
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        MLog.d(TAG, "AudioEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                        mIsVideoDecoding = false;
                        break;
                    }
                    if (roomIndex < 0) {
                        continue;
                    }
                    room = mAudioDecoderMediaCodec.getOutputBuffer(roomIndex);
                    mAudioDecoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                    if (!mIsVideoDecoding) {
                        synchronized (mMediaMuxerLock) {
                            MLog.d(TAG, "AudioEncoderThread mMediaMuxerLock.notify()");
                            mMediaMuxerLock.notify();
                        }
                        break;
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException e) {
                    MLog.e(TAG, "AudioEncoderThread Output occur exception: " + e);
                    mIsVideoDecoding = false;
                    break;
                }
            }
            MLog.d(TAG, "AudioEncoderThread end");
        }
    }

}
