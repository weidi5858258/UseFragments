package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/***

 */
public class RecordScreenFragment extends BaseFragment {

    private static final String TAG =
            RecordScreenFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public RecordScreenFragment() {
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
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
        return R.layout.fragment_record_screen;
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

    @InjectView(R.id.title_tv)
    private TextView mTitleView;
    @InjectView(R.id.start_btn)
    private Button mStartBtn;
    @InjectView(R.id.stop_btn)
    private Button mStopBtn;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private boolean mIsRecording = false;
    private boolean mIsMuxerStarted = false;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private static final int mWidth = 720;
    private static final int mHeight = 1280;
    private Surface mSurface;
    private MediaCodec mVideoEncoderMediaCodec;
    private MediaCodec mAudioEncoderMediaCodec;
    private MediaFormat mVideoEncoderMediaFormat;
    private MediaFormat mAudioEncoderMediaFormat;
    private AudioRecord mAudioRecord;
    private MediaMuxer mMediaMuxer;
    private int mOutputVideoTrack = -1;
    private int mOutputAudioTrack = -1;

    private VideoEncoderThread mVideoEncoderThread;
    private AudioEncoderThread mAudioEncoderThread;

    private Object mMediaMuxerLock = new Object();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

        mTitleView.setText(RecordScreenFragment.class.getSimpleName());
        if (mIsRecording) {
            mStartBtn.setText("正在录屏");
            mStopBtn.setText("停止录屏");
        } else {
            mStartBtn.setText("开始录屏");
            mStopBtn.setText("");
        }
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
        // API>=23
        /*MediaProjectionManager mediaProjectionManager =
                getContext().getSystemService(MediaProjectionManager.class);*/
        mMediaProjectionManager =
                (MediaProjectionManager) getContext().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //super.threadHandleMessage(msg);
                RecordScreenFragment.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //super.handleMessage(msg);
                RecordScreenFragment.this.uiHandleMessage(msg);
            }
        };

        mThreadHandler.sendEmptyMessage(PREPARE);
    }

    private void initView(View view, Bundle savedInstanceState) {

    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
        }
    }

    @InjectOnClick({R.id.start_btn, R.id.stop_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn:
                requestPermission();
                break;
            case R.id.stop_btn:
                mThreadHandler.sendEmptyMessage(STOP_RECORD_SCREEN);
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new A2Fragment());
                break;
        }
    }

    private void prepare() {
        File file = new File(
                "/storage/2430-1702/Android/data/com.weidi.usefragments/files",
                "test.mp4");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        if (!file.canWrite()) {
            return;
        }

        // AudioRecord
        mAudioRecord = MediaUtils.createAudioRecord();
        if (mAudioRecord == null) {
            return;
        }
        // MediaCodec
        mVideoEncoderMediaCodec = MediaUtils.getVideoEncoderMediaCodec();
        mAudioEncoderMediaCodec = MediaUtils.getAudioEncoderMediaCodec();
        if (mVideoEncoderMediaCodec == null
                || mAudioEncoderMediaCodec == null) {
            return;
        }
        // MediaFormat
        mVideoEncoderMediaFormat = MediaUtils.getVideoEncoderMediaFormat(mWidth, mHeight);
        mAudioEncoderMediaFormat = MediaUtils.getAudioEncoderMediaFormat();
        try {
            mVideoEncoderMediaCodec.configure(
                    mVideoEncoderMediaFormat,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mVideoEncoderMediaCodec.createInputSurface();
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
            if (mVideoEncoderMediaCodec != null) {
                mVideoEncoderMediaCodec.release();
                mVideoEncoderMediaCodec = null;
            }
            return;
        }
        try {
            mAudioEncoderMediaCodec.configure(
                    mAudioEncoderMediaFormat,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
            if (mAudioEncoderMediaCodec != null) {
                mAudioEncoderMediaCodec.release();
                mAudioEncoderMediaCodec = null;
            }
            return;
        }

        try {
            mMediaMuxer = new MediaMuxer(
                    file.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
            if (mVideoEncoderMediaCodec != null) {
                mVideoEncoderMediaCodec.release();
                mVideoEncoderMediaCodec = null;
            }
            if (mAudioEncoderMediaCodec != null) {
                mAudioEncoderMediaCodec.release();
                mAudioEncoderMediaCodec = null;
            }
            return;
        }

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
    }

    /***
     mSurface=Surface(name=Sys2003:com.android.systemui/com.android.systemui.media
     .MediaProjectionPermissionActivity)
     mSurface=Surface(name=com.android.systemui/com.android.systemui.media
     .MediaProjectionPermissionActivity)
     mSurface=Surface(name=com.weidi.usefragments/com.weidi.usefragments.MainActivity1)
     调用下面代码后的现象:
     弹出一个框,有两个按钮("取消"和"立即开始"),还有一个选择框("不再提示")
     1.只点击"立即开始"按钮
     那么会回调onActivityResult()方法.
     由于没有选择"不再提示",因此下次调用下面代码时还会弹出框让用户进行确认
     2.选择"不再提示",并点击"立即开始"按钮
     那么会回调onActivityResult()方法.
     由于选择过"不再提示",因此下次调用下面代码时不会再弹出框让用户进行确认
     只会回调onActivityResult()方法.
     3.只点击"取消"按钮
     不会回调onActivityResult()方法.
     下次调用下面代码时还会弹出框让用户进行确认
     4.选择"不再提示",并点击"取消"按钮
     不会回调onActivityResult()方法.
     下次调用下面代码时还会弹出框让用户进行确认
     */
    private void requestPermission() {
        if (mMediaProjectionManager != null) {
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_CODE);
        }
    }

    private void startRecordScreen() {
        if (mIsRecording || mMediaProjection == null) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "startRecordScreen() " + printThis());

        mIsRecording = true;

        mMediaProjection.registerCallback(mMediaProjectionCallback, mThreadHandler);

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                TAG + "-Display",
                mWidth,
                mHeight,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mSurface,
                null,
                null);
        if (DEBUG)
            MLog.d(TAG, "startRecordScreen() " + printThis() +
                    " created virtual display: " + mVirtualDisplay.getDisplay());

        /*if (mAudioRecord != null) {
            mAudioRecord.startRecording();
        }
        if (mAudioEncoderMediaCodec != null) {
            mAudioEncoderMediaCodec.start();
        }*/
        if (mVideoEncoderMediaCodec != null) {
            mVideoEncoderMediaCodec.start();
        }

        if (mVideoEncoderMediaCodec != null
                && mAudioEncoderMediaCodec != null
                && mAudioRecord != null
                && mSurface != null) {
            /*mAudioEncoderThread = new AudioEncoderThread();
            new Thread(mAudioEncoderThread).start();*/
            mVideoEncoderThread = new VideoEncoderThread();
            new Thread(mVideoEncoderThread).start();

            // 相当于按了“Home”键
            getAttachedActivity().moveTaskToBack(true);
        }
    }

    private void stopRecordScreen() {
        if (!mIsRecording || mMediaProjection == null) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "stopRecordScreen() start " + printThis());

        mIsRecording = false;
        mIsMuxerStarted = false;
        mOutputVideoTrack = -1;
        mOutputAudioTrack = -1;

        mMediaProjection.stop();
        mMediaProjection.unregisterCallback(mMediaProjectionCallback);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        synchronized (mMediaMuxerLock) {
            try {
                mMediaMuxerLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (mMediaMuxer != null) {
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMediaMuxer = null;
            }
        }

        if (mVideoEncoderMediaCodec != null) {
            mVideoEncoderMediaCodec.release();
            mVideoEncoderMediaCodec = null;
        }

        notifyAudioEndOfStream();
        if (mAudioEncoderMediaCodec != null) {
            mAudioEncoderMediaCodec.release();
            mAudioEncoderMediaCodec = null;
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord = null;
        }

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
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

        // MediaProjection对象是这样来的,所以要得到MediaProjection对象,必须同意权限
        mMediaProjection =
                mMediaProjectionManager.getMediaProjection(resultCode, data);

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

    }

    private void notifyAudioEndOfStream() {
        if (DEBUG)
            MLog.d(TAG, "notifyAudioEndOfStream() " + printThis());
        //audio end notify
        int inputAudioBufferIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(0);
        while (inputAudioBufferIndex < 0) {
            inputAudioBufferIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mAudioEncoderMediaCodec.queueInputBuffer(
                inputAudioBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
    }

    private class VideoEncoderThread implements Runnable {
        @Override
        public void run() {
            MLog.d(TAG, "VideoEncoderThread start");
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            ByteBuffer room = null;
            while (mIsRecording) {
                /*if (mOutputAudioTrack < 0) {
                    try {
                        Thread.sleep(1, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }*/
                try {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    MLog.d(TAG, "VideoEncoderThread roomIndex: " + roomIndex);
                    roomIndex = mVideoEncoderMediaCodec.dequeueOutputBuffer(
                            bufferInfo, 10000);// 33333
                    MLog.d(TAG, "VideoEncoderThread roomIndex: " + roomIndex);
                    switch (roomIndex) {
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            MLog.d(TAG, "VideoEncoderThread " +
                                    "Output MediaCodec.INFO_TRY_AGAIN_LATER");
                            mIsRecording = false;
                            return;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            MLog.d(TAG, "VideoEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            mVideoEncoderMediaFormat = mVideoEncoderMediaCodec.getOutputFormat();
                            if (mVideoEncoderMediaFormat != null) {
                                mOutputVideoTrack = mMediaMuxer.addTrack(mVideoEncoderMediaFormat);
                                MLog.d(TAG, "VideoEncoderThread mOutputVideoTrack: " +
                                        mOutputVideoTrack);
                            }
                            break;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            MLog.d(TAG, "VideoEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            //outputBuffers = mVideoEncoderMediaCodec.getOutputBuffers();
                            continue;
                        default:
                            break;

                    }
                    room = mVideoEncoderMediaCodec.getOutputBuffer(roomIndex);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        MLog.d(TAG, "VideoEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                        mVideoEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                        continue;
                    }
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        MLog.d(TAG, "VideoEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                        mIsRecording = false;
                        break;
                    }
                    if (!mIsMuxerStarted
                            && mOutputVideoTrack >= 0
                            && mOutputAudioTrack >= 0) {
                        mIsMuxerStarted = true;
                        mMediaMuxer.start();
                        MLog.d(TAG, "VideoEncoderThread mMediaMuxer.start()");
                    }
                    if (mIsMuxerStarted
                            && mOutputVideoTrack >= 0
                            && bufferInfo.size != 0) {
                        bufferInfo.presentationTimeUs = System.nanoTime() / 1000;
                        mMediaMuxer.writeSampleData(mOutputVideoTrack, room, bufferInfo);
                    }
                    mVideoEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                    if (!mIsRecording) {
                        synchronized (mMediaMuxerLock) {
                            mMediaMuxerLock.notify();
                        }
                        break;
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException e) {
                    MLog.e(TAG, "VideoEncoderThread Output occur exception: " + e);
                    mIsRecording = false;
                    break;
                }
            }
            MLog.d(TAG, "VideoEncoderThread end");
        }
    }

    private class AudioEncoderThread implements Runnable {
        @Override
        public void run() {
            MLog.d(TAG, "AudioEncoderThread start");
            int readSize = -1;
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            ByteBuffer room = null;
            boolean onlyOne = true;
            byte[] buffer = new byte[MediaUtils.getMinBufferSize() * 2];
            MLog.d(TAG, "AudioEncoderThread first  buffer.length: " + buffer.length);
            ByteBuffer[] inputBuffers = mAudioEncoderMediaCodec.getInputBuffers();
            if (inputBuffers != null
                    && inputBuffers.length > 0
                    && inputBuffers[0] != null
                    && buffer.length > inputBuffers[0].limit()) {
                // 目的:得到的房间能够一次性装下buffer里的数据
                buffer = new byte[inputBuffers[0].limit()];
                MLog.d(TAG, "AudioEncoderThread second buffer.length: " + buffer.length);
            }

            while (mIsRecording) {
                if (buffer != null) {
                    //Arrays.fill(buffer, (byte) 0);
                    readSize = mAudioRecord.read(buffer, 0, buffer.length);
                    if (readSize < 0) {
                        MLog.d(TAG, "AudioEncoderThread readSize: " + readSize);
                        mIsRecording = false;
                        break;
                    }
                }

                // Input过程
                try {
                    roomIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(-1);
                    if (roomIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        room = mAudioEncoderMediaCodec.getInputBuffer(roomIndex);

                        // 只做一次,目的还是为了得到合适大小的buffer
                        if (onlyOne) {
                            onlyOne = false;
                            int roomSize = room.limit();
                            if (roomSize < buffer.length) {
                                buffer = new byte[roomSize];
                                MLog.d(TAG, "AudioEncoderThread three  buffer.length: " +
                                        buffer.length);
                                readSize = mAudioRecord.read(buffer, 0, buffer.length);
                                if (readSize < 0) {
                                    MLog.d(TAG, "AudioEncoderThread readSize: " + readSize);
                                    mIsRecording = false;
                                    break;
                                }
                            }
                        }

                        room.clear();
                        // 这里还要考虑byteBuffer能不能装的下buffer
                        room.put(buffer);
                        long presentationTimeUs = System.nanoTime() / 1000;
                        mAudioEncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                buffer.length,
                                presentationTimeUs,
                                0);
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException e) {
                    MLog.e(TAG, "AudioEncoderThread Input occur exception: " + e);
                    mIsRecording = false;
                    break;
                }

                // Output过程
                try {
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    roomIndex = mAudioEncoderMediaCodec.dequeueOutputBuffer(
                            bufferInfo, 10000);
                    // 先处理负值
                    switch (roomIndex) {
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.INFO_TRY_AGAIN_LATER");
                            mIsRecording = false;
                            return;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            mAudioEncoderMediaFormat = mAudioEncoderMediaCodec.getOutputFormat();
                            if (mAudioEncoderMediaFormat != null) {
                                mOutputAudioTrack = mMediaMuxer.addTrack(mAudioEncoderMediaFormat);
                                MLog.d(TAG, "AudioEncoderThread mOutputAudioTrack: " +
                                        mOutputAudioTrack);
                            }
                            continue;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            //outputBuffers = mAudioEncoderMediaCodec.getOutputBuffers();
                            continue;
                        default:
                            break;
                    }
                    room = mAudioEncoderMediaCodec.getOutputBuffer(roomIndex);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        MLog.d(TAG, "AudioEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                        mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                        continue;
                    }
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        MLog.d(TAG, "AudioEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                        mIsRecording = false;
                        break;
                    }
                    if (mIsMuxerStarted
                            && mOutputAudioTrack >= 0
                            && bufferInfo.size != 0) {
                        mMediaMuxer.writeSampleData(mOutputAudioTrack, room, bufferInfo);
                    }
                    mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                    if (!mIsRecording) {
                        synchronized (mMediaMuxerLock) {
                            mMediaMuxerLock.notify();
                        }
                        break;
                    }
                } catch (MediaCodec.CryptoException
                        | IllegalStateException e) {
                    MLog.e(TAG, "AudioEncoderThread Output occur exception: " + e);
                    mIsRecording = false;
                    break;
                }
            }
            MLog.d(TAG, "AudioEncoderThread end");
        }
    }

}
