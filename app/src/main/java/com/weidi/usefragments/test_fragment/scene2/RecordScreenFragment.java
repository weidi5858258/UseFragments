package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.view.LayoutInflater;
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
    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private static final int mWidth = 720;
    private static final int mHeight = 1280;
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
        //        mMediaProjection.createVirtualDisplay();

        /*if (mVideoEncoderMediaCodec != null) {
            mVideoEncoderMediaCodec.start();
        }*/
        if (mAudioEncoderMediaCodec != null) {
            mAudioEncoderMediaCodec.start();
        }
        if (mAudioRecord != null) {
            mAudioRecord.startRecording();
        }

        if (mVideoEncoderMediaCodec != null
                && mAudioEncoderMediaCodec != null
                && mAudioRecord != null) {
            /*mVideoEncoderThread = new VideoEncoderThread();
            new Thread(mVideoEncoderThread).start();*/
            mAudioEncoderThread = new AudioEncoderThread();
            new Thread(mAudioEncoderThread).start();

            // 相当于按了“Home”键
            getAttachedActivity().moveTaskToBack(true);
        }
    }

    private void stopRecordScreen() {
        if (!mIsRecording || mMediaProjection == null) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "stopRecordScreen() " + printThis());

        mIsRecording = false;

        mMediaProjection.stop();
        mMediaProjection.unregisterCallback(mMediaProjectionCallback);

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

        }
    }

    private class AudioEncoderThread implements Runnable {
        @Override
        public void run() {
            byte[] buffer = new byte[MediaUtils.getMinBufferSize() * 2];
            ByteBuffer[] inputBuffers = mAudioEncoderMediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mAudioEncoderMediaCodec.getOutputBuffers();
            MLog.d(TAG, "AudioEncoderThread start");
            while (mIsRecording) {
                //Arrays.fill(buffer, (byte) 0);
                int result = mAudioRecord.read(buffer, 0, buffer.length);
                if (result < 0) {
                    MLog.d(TAG, "AudioEncoderThread result: " + result);
                    mIsRecording = false;
                    break;
                }
                // Input过程
                try {
                    int roomIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(-1);
                    MLog.d(TAG, "AudioEncoderThread Input roomIndex: " + roomIndex);
                    if (roomIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        ByteBuffer room = inputBuffers[roomIndex];
                        int roomSize = room.limit();
                        // ByteBuffer空间够
                        if (roomSize >= buffer.length) {
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
                            break;
                        } else {
                            int putBufferLength = 0;
                            while (mIsRecording) {
                                if (roomIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                                    room = inputBuffers[roomIndex];
                                    // ByteBuffer空间不够
                                    room.clear();
                                    long presentationTimeUs = System.nanoTime() / 1000;
                                    // 这里还要考虑byteBuffer能不能装的下buffer
                                    if ((buffer.length - putBufferLength) >= roomSize) {
                                        room.put(buffer,
                                                putBufferLength,
                                                roomSize);
                                        mAudioEncoderMediaCodec.queueInputBuffer(
                                                roomIndex,
                                                0,
                                                roomSize,
                                                presentationTimeUs,
                                                0);
                                    } else {
                                        room.put(buffer,
                                                putBufferLength,
                                                buffer.length - putBufferLength);
                                        mAudioEncoderMediaCodec.queueInputBuffer(
                                                roomIndex,
                                                0,
                                                buffer.length - putBufferLength,
                                                presentationTimeUs,
                                                0);
                                    }
                                    putBufferLength += roomSize;
                                    if (buffer.length - putBufferLength <= 0) {
                                        MLog.d(TAG, "AudioEncoderThread Input break");
                                        break;
                                    }
                                    MLog.d(TAG, "AudioEncoderThread Input @ start roomIndex: " + roomIndex);
                                    roomIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(-1);
                                    MLog.d(TAG, "AudioEncoderThread Input @ end   roomIndex: " + roomIndex);
                                } else {
                                    MLog.d(TAG, "AudioEncoderThread Input else break");
                                    break;
                                }
                            }
                        }
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
                    int roomIndex = mAudioEncoderMediaCodec.dequeueOutputBuffer(
                            bufferInfo, 10000);
                    MLog.d(TAG, "AudioEncoderThread Output roomIndex: " + roomIndex);
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
                                // test
                                mMediaMuxer.start();
                            }
                            continue;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            outputBuffers = mAudioEncoderMediaCodec.getOutputBuffers();
                            continue;
                        default:
                            break;
                    }
                    ByteBuffer byteBuffer = outputBuffers[roomIndex];
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
                    if (/*mIsMuxerStarted
                            && */mOutputAudioTrack >= 0
                            && bufferInfo.size != 0) {
                        mMediaMuxer.writeSampleData(mOutputAudioTrack, byteBuffer, bufferInfo);
                    }
                    mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
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
