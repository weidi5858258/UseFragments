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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.socket.SocketServer;
import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/***
 投屏
 */
public class ThrowingScreenFragment extends BaseFragment {

    private static final String TAG =
            ThrowingScreenFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public ThrowingScreenFragment() {
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
        return R.layout.fragment_throwing_screen;
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
    private static final int VIDEO_NOTIFY = 0x0004;
    private static final int AUDIO_NOTIFY = 0x0005;
    private static final int SOCKET_SEND = 0x0006;


    @InjectView(R.id.title_tv)
    private TextView mTitleView;
    @InjectView(R.id.start_btn)
    private Button mStartBtn;
    @InjectView(R.id.stop_btn)
    private Button mStopBtn;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private boolean mIsVideoRecording = false;
    private boolean mIsAudioRecording = false;
    private boolean mIsMuxerStarted = false;
    private boolean mIsServerStarted = false;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    // 竖屏时的分辨率
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
    private Socket mSocket;

    private Object mVideoEncoderLock = new Object();
    private Object mAudioEncoderLock = new Object();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

        mTitleView.setText(ThrowingScreenFragment.class.getSimpleName());
        if (mIsVideoRecording) {
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
        SocketServer.getInstance();
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
                ThrowingScreenFragment.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //super.handleMessage(msg);
                ThrowingScreenFragment.this.uiHandleMessage(msg);
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
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }

        mIsServerStarted = false;
        SocketServer.getInstance().close();
    }

    @InjectOnClick({R.id.start_btn, R.id.stop_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn:
                requestPermission();
                break;
            case R.id.stop_btn:
                Message msg = mThreadHandler.obtainMessage();
                msg.what = STOP_RECORD_SCREEN;
                mThreadHandler.sendMessageAtFrontOfQueue(msg);
                // mThreadHandler.sendEmptyMessage(STOP_RECORD_SCREEN);
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new DecodePlayFragment());
                break;
        }
    }

    private void prepare() {
        File file = new File(
                "/storage/2430-1702/Android/data/com.weidi.usefragments/files/",
                //                "/data/data/com.weidi.usefragments/files/",
                "test.mp4");
        if (file.exists()) {
            try {
                file.delete();
            } catch (SecurityException e) {
                e.printStackTrace();
                return;
            }
        }
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
        if (mIsVideoRecording
                || mIsAudioRecording
                || mMediaProjection == null) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "startRecordScreen() " + printThis());

        mIsVideoRecording = true;
        mIsAudioRecording = true;
        mIsServerStarted = true;

        mMediaProjection.registerCallback(mMediaProjectionCallback, mThreadHandler);

        if (mAudioRecord != null) {
            mAudioRecord.startRecording();
        }
        if (mAudioEncoderMediaCodec != null) {
            mAudioEncoderMediaCodec.start();
        }
        if (mVideoEncoderMediaCodec != null) {
            mVideoEncoderMediaCodec.start();
        }

        if (mVideoEncoderMediaCodec != null
                && mAudioEncoderMediaCodec != null
                && mAudioRecord != null
                && mSurface != null) {
            // 音频先启动,让音频的mOutputAudioTrack先得到值
            new Thread(new AudioEncoderThread()).start();
            new Thread(new VideoEncoderThread()).start();
            new Thread(new SocketAcceptThread()).start();

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

            // 60 * 1000
            // mThreadHandler.sendEmptyMessageDelayed(STOP_RECORD_SCREEN, 60 * 1000);

            // 相当于按了“Home”键
            getAttachedActivity().moveTaskToBack(true);
        }
    }

    private void stopRecordScreen() {
        if (!mIsVideoRecording
                || !mIsAudioRecording
                || mMediaProjection == null) {
            return;
        }

        if (DEBUG)
            MLog.w(TAG, "stopRecordScreen() start");

        mIsServerStarted = false;

        mIsVideoRecording = false;
        synchronized (mVideoEncoderLock) {
            try {
                mUiHandler.removeMessages(VIDEO_NOTIFY);
                mUiHandler.sendEmptyMessageDelayed(VIDEO_NOTIFY, 1000);
                MLog.d(TAG, "stopRecordScreen mVideoEncoderLock.wait() start");
                mVideoEncoderLock.wait();
                MLog.d(TAG, "stopRecordScreen mVideoEncoderLock.wait() end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mIsAudioRecording = false;
        synchronized (mAudioEncoderLock) {
            try {
                MLog.d(TAG, "stopRecordScreen mAudioEncoderLock.wait() start");
                mAudioEncoderLock.wait();
                MLog.d(TAG, "stopRecordScreen mAudioEncoderLock.wait() end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mOutputVideoTrack = -1;
        mOutputAudioTrack = -1;

        mMediaProjection.stop();
        mMediaProjection.unregisterCallback(mMediaProjectionCallback);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mMediaMuxer != null
                && mIsMuxerStarted) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
        mIsMuxerStarted = false;

        //notifyVideoEndOfStream();
        if (mVideoEncoderMediaCodec != null) {
            mVideoEncoderMediaCodec.release();
            mVideoEncoderMediaCodec = null;
        }

        //notifyAudioEndOfStream();
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
                Toast.makeText(getContext(), "成功停止", Toast.LENGTH_SHORT).show();
                onShow();
            }
        });

        if (DEBUG)
            MLog.w(TAG, "stopRecordScreen() end");
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
            case SOCKET_SEND:
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
            case VIDEO_NOTIFY:
                synchronized (mVideoEncoderLock) {
                    MLog.d(TAG, "VideoEncoderThread mVideoEncoderLock.notify()");
                    mVideoEncoderLock.notify();
                }
                mUiHandler.removeMessages(AUDIO_NOTIFY);
                mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                break;
            case AUDIO_NOTIFY:
                synchronized (mAudioEncoderLock) {
                    MLog.d(TAG, "AudioEncoderThread mAudioEncoderLock.notify()");
                    mAudioEncoderLock.notify();
                }
                break;
            default:
                break;
        }
    }

    private void notifyVideoEndOfStream() {
        if (DEBUG)
            MLog.d(TAG, "notifyVideoEndOfStream() " + printThis());
        //video end notify
        int inputBufferIndex = mVideoEncoderMediaCodec.dequeueInputBuffer(0);
        while (inputBufferIndex < 0) {
            inputBufferIndex = mVideoEncoderMediaCodec.dequeueInputBuffer(0);
        }

        long presentationTime = System.nanoTime() / 1000;
        mVideoEncoderMediaCodec.queueInputBuffer(
                inputBufferIndex,
                0,
                0,
                presentationTime,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
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

    private void sendLength(OutputStream os, byte[] bytes) {
        try {
            if (os != null) {
                os.write(bytes);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendSpsPps(OutputStream os, byte[] bytes) {
        try {
            if (os != null) {
                os.write(bytes);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFrame(OutputStream os, byte[] bytes) {
        try {
            if (os != null) {
                os.write(bytes, 0, bytes.length);
                os.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] intToBytes(int i) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (i & 0xff);
        bytes[1] = (byte) ((i >> 8) & 0xff);
        bytes[2] = (byte) ((i >> 16) & 0xff);
        bytes[3] = (byte) ((i >> 24) & 0xff);
        return bytes;
    }

    /***
     给每一帧添加一个头
     */
    private byte[] creatHead(byte[] out, int type) {
        StringBuffer head = new StringBuffer();
        if (type == 1) {
            head.append("start&video&");
            head.append(System.currentTimeMillis());
            head.append("&");
            head.append(out.length);
            head.append("&end");
        } else {
            head.append("start&music&");
            head.append(System.currentTimeMillis());
            head.append("&");
            head.append(out.length);
            head.append("&end");
        }
        byte[] headBytes = new byte[40];
        System.arraycopy(
                head.toString().getBytes(), 0,
                headBytes, 0, head.toString().getBytes().length);

        MLog.i(TAG, "creatHead() 头部长度: " + headBytes.length);
        if (headBytes[0] == 0x73
                && headBytes[1] == 0x74
                && headBytes[2] == 0x61
                && headBytes[3] == 0x72
                && headBytes[4] == 0x74) {
            Log.e("writeSteam", "确认是头部");
        }
        String outHead = new String(headBytes);
        MLog.i(TAG, "creatHead() 头部: " + outHead);
        byte[] headByteOut = new byte[out.length + 40];
        // 将头部拷入数组
        System.arraycopy(
                headBytes, 0, headByteOut, 0, headBytes.length);
        // 将帧数据拷入数组
        System.arraycopy(
                out, 0, headByteOut, headBytes.length, out.length);
        return headByteOut;
    }

    private class VideoEncoderThread implements Runnable {
        @Override
        public void run() {
            MLog.i(TAG, "VideoEncoderThread start");
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            ByteBuffer room = null;
            MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
            byte[] mSpsPps = new byte[0];
            boolean hasSendedSpsPps = false;
            boolean firstSendkeyFrame = false;
            while (mIsVideoRecording) {
                // Exit device
                if (!mIsVideoRecording) {
                    mUiHandler.removeMessages(VIDEO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(VIDEO_NOTIFY, 300);
                    break;
                }

                // 等到音频的mOutputAudioTrack >= 0时,才往下走
                // 先把音频的准备工作做好了,再准备视频的准备工作
                if (mOutputAudioTrack < 0) {
                    try {
                        Thread.sleep(1, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                // 没有Input过程

                // Output过程
                try {
                    roomIndex = mVideoEncoderMediaCodec.dequeueOutputBuffer(
                            roomInfo, 33333);
                    switch (roomIndex) {
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            // 录屏时roomIndex经常得到MediaCodec.INFO_TRY_AGAIN_LATER值
                            // 千万不要break;
                            continue;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            MLog.d(TAG, "VideoEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                            mVideoEncoderMediaFormat =
                                    mVideoEncoderMediaCodec.getOutputFormat();
                            if (mVideoEncoderMediaFormat != null) {
                                mOutputVideoTrack =
                                        mMediaMuxer.addTrack(mVideoEncoderMediaFormat);
                                MLog.d(TAG, "VideoEncoderThread mOutputVideoTrack: " +
                                        mOutputVideoTrack);
                            }
                            continue;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            MLog.d(TAG, "VideoEncoderThread " +
                                    "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                            continue;
                        default:
                            break;
                    }

                    if (roomIndex < 0) {
                        continue;
                    }
                    room = mVideoEncoderMediaCodec.getOutputBuffer(roomIndex);
                    int roomSize = roomInfo.size;

                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    byte[] encodedData = new byte[roomSize];
                    room.get(encodedData);

                    if (encodedData[0] == 0
                            && encodedData[1] == 0
                            && encodedData[2] == 0
                            && encodedData[3] == 1
                            && encodedData[4] == 101) {
                        // MLog.d(TAG, "VideoEncoderThread 关键帧");
                        // 在关键帧前面加上pps和sps数据
                        byte[] iFrameData = new byte[mSpsPps.length + encodedData.length];
                        System.arraycopy(
                                mSpsPps, 0, iFrameData, 0, mSpsPps.length);
                        System.arraycopy(
                                encodedData, 0, iFrameData, mSpsPps.length, encodedData.length);
                        encodedData = iFrameData;
                    } else if (encodedData[0] == 0
                            && encodedData[1] == 0
                            && encodedData[2] == 0
                            && encodedData[3] == 1
                            && encodedData[4] == 103) {
                        // 记录pps和sps
                        mSpsPps = encodedData;
                        for (byte sps_pps : mSpsPps) {
                            MLog.d(TAG, "VideoEncoderThread sps_pps: " + sps_pps);
                        }
                    }

                    if (mSocket != null && mSocket.isConnected()) {
                        OutputStream outputStream = mSocket.getOutputStream();
                        if (!hasSendedSpsPps && mSpsPps != null) {
                            hasSendedSpsPps = true;
                            outputStream.write(13);
                            outputStream.flush();
                            outputStream.write(8);
                            outputStream.flush();
                            outputStream.write(mSpsPps);
                            outputStream.flush();
                            // continue;
                        }

                        byte[] headOut = creatHead(encodedData, 1);
                        outputStream.write(headOut);
                        outputStream.flush();
                    }

                    if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        MLog.d(TAG, "VideoEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                        mIsVideoRecording = false;
                        break;
                    }
                    if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // 会产生sps和pps
                        MLog.d(TAG, "VideoEncoderThread " +
                                "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                        // 标记是I帧(关键帧)还是同步帧
                        boolean sync = (roomInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                        MLog.d(TAG, "VideoEncoderThread sync: " + sync);

                        if (!sync) {
                            // 同步帧,填充着sps和pps参数
                            // 0 0 0 1 103 66 -128 40 -23 1 104 20 50 0 0 0 1 104 -18 6 -14
                            mSpsPps = encodedData;
                            for (int i = 0; i < mSpsPps.length; i++) {
                                MLog.d(TAG, "VideoEncoderThread " + mSpsPps[i]);
                            }
                        }
                        mVideoEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                        continue;
                    }

                    /*if (!mIsMuxerStarted
                            && mOutputVideoTrack >= 0
                            && mOutputAudioTrack >= 0) {
                        mIsMuxerStarted = true;
                        mMediaMuxer.start();
                        MLog.d(TAG, "VideoEncoderThread mMediaMuxer.start()");
                    }*/

                    // 操作一
                    room.position(roomInfo.offset);
                    room.limit(roomInfo.offset + roomSize);
                    if (mIsVideoRecording
                            && mIsMuxerStarted) {
                        roomInfo.presentationTimeUs = System.nanoTime() / 1000;
                        mMediaMuxer.writeSampleData(mOutputVideoTrack, room, roomInfo);
                    }

                    // 操作二
                    /*if (mSocket != null && mSocket.isConnected()) {
                        OutputStream outputStream = mSocket.getOutputStream();
                        if (!hasSendedSpsPps && mSpsPps != null) {
                            hasSendedSpsPps = true;
                            outputStream.write(13);
                            outputStream.flush();
                            outputStream.write(8);
                            outputStream.flush();
                            outputStream.write(mSpsPps);
                            outputStream.flush();
                            continue;
                        }
                        // 关键帧
                        if ((encodedData[4] == 0x65) || (encodedData[4] == 0x25)) {
                            // MLog.d(TAG, "VideoEncoderThread 关键帧");
                            int totalSize = mSpsPps.length + roomSize;
                            encodedData = new byte[totalSize];
                            System.arraycopy(mSpsPps, 0, encodedData, 0, mSpsPps.length);
                            room.position(roomInfo.offset);
                            room.limit(roomInfo.offset + roomSize);
                            room.get(encodedData, mSpsPps.length, roomSize);
                            // 传递encodedData数据
                            outputStream.write(encodedData, 0, totalSize);
                            outputStream.flush();
                            firstSendkeyFrame = true;
                        } else {
                            // MLog.d(TAG, "VideoEncoderThread 非关键帧");
                            // 传递encodedData数据
                            if (firstSendkeyFrame) {
                                outputStream.write(encodedData, 0, roomSize);
                                outputStream.flush();
                            }
                        }
                    }*/

                    // 通知MediaCodec,房间的东西已经被消耗了,这样MediaCodec可以再次利用此房间
                    mVideoEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                } catch (MediaCodec.CodecException e) {
                    MLog.e(TAG, "VideoEncoderThread Output occur exception: " + e);
                    mIsVideoRecording = false;
                    stopRecordScreen();
                    mUiHandler.removeMessages(VIDEO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(VIDEO_NOTIFY, 300);
                    break;
                } catch (IllegalStateException e) {
                    MLog.e(TAG, "VideoEncoderThread Output occur exception: " + e);
                    mIsVideoRecording = false;
                    stopRecordScreen();
                    mUiHandler.removeMessages(VIDEO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(VIDEO_NOTIFY, 300);
                    break;
                } catch (IllegalArgumentException e) {
                    MLog.e(TAG, "VideoEncoderThread Output occur exception: " + e);
                    mIsVideoRecording = false;
                    stopRecordScreen();
                    mUiHandler.removeMessages(VIDEO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(VIDEO_NOTIFY, 300);
                    break;
                } catch (IOException e) {
                    MLog.e(TAG, "VideoEncoderThread Output occur exception: " + e);
                    /*mIsVideoRecording = false;
                    stopRecordScreen();
                    mUiHandler.removeMessages(VIDEO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(VIDEO_NOTIFY, 300);
                    break;*/
                }
            }// while end
            MLog.i(TAG, "VideoEncoderThread end");
        }
    }

    private class AudioEncoderThread implements Runnable {
        @Override
        public void run() {
            MLog.i(TAG, "AudioEncoderThread start");
            int readSize = -1;
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            ByteBuffer room = null;
            MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
            // 这里的大小需要跟MediaUtils中getAudioEncoderMediaFormat()方法中
            // MediaFormat.KEY_MAX_INPUT_SIZE设置的值一样
            byte[] pcmData = new byte[MediaUtils.getMinBufferSize() * 2];

            while (mIsAudioRecording) {
                // Exit device
                if (!mIsAudioRecording) {
                    mUiHandler.removeMessages(AUDIO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                    break;
                }

                // 取数据过程
                if (pcmData != null) {
                    //Arrays.fill(pcmData, (byte) 0);
                    readSize = mAudioRecord.read(pcmData, 0, pcmData.length);
                    if (readSize < 0) {
                        MLog.d(TAG, "AudioEncoderThread readSize: " + readSize);
                        mIsAudioRecording = false;
                        stopRecordScreen();
                        mUiHandler.removeMessages(AUDIO_NOTIFY);
                        mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                        break;
                    }
                }

                // Input过程
                try {
                    roomIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(-1);
                    if (roomIndex >= 0) {
                        room = mAudioEncoderMediaCodec.getInputBuffer(roomIndex);
                        room.clear();
                        room.put(pcmData);
                        // 注意点一
                        // 时间戳要这样写,先用变量保存,然后再给下面的函数
                        // 如果直接把"System.nanoTime() / 1000"赋给下面的函数会出现问题
                        long presentationTime = System.nanoTime() / 1000;
                        // 通知MediaCodec进行编码
                        mAudioEncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                readSize,
                                presentationTime,
                                0);
                    }
                } catch (MediaCodec.CryptoException e) {
                    MLog.e(TAG, "AudioEncoderThread Input occur exception: " + e);
                    mIsAudioRecording = false;
                    stopRecordScreen();
                    mUiHandler.removeMessages(AUDIO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                    break;
                } catch (MediaCodec.CodecException e) {
                    MLog.e(TAG, "AudioEncoderThread Input occur exception: " + e);
                    mIsAudioRecording = false;
                    stopRecordScreen();
                    mUiHandler.removeMessages(AUDIO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                    break;
                } catch (IllegalStateException e) {
                    MLog.e(TAG, "AudioEncoderThread Input occur exception: " + e);
                    mIsAudioRecording = false;
                    stopRecordScreen();
                    mUiHandler.removeMessages(AUDIO_NOTIFY);
                    mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                    break;
                }

                // Output过程
                // 注意点二
                // 如果有"Input过程",那么需要有个循环
                for (; ; ) {
                    // Exit device
                    if (!mIsAudioRecording) {
                        mUiHandler.removeMessages(AUDIO_NOTIFY);
                        mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                        return;
                    }
                    try {
                        // dequeueOutputBuffer方法不能用于async mode
                        roomIndex = mAudioEncoderMediaCodec.dequeueOutputBuffer(
                                roomInfo, 10000);
                        // 先处理负值
                        switch (roomIndex) {
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                // 请重试
                                // 注意点三
                                // 千万不要break;
                                continue;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                // 格式已经更改(首先执行)
                                MLog.d(TAG, "AudioEncoderThread " +
                                        "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                                mAudioEncoderMediaFormat =
                                        mAudioEncoderMediaCodec.getOutputFormat();
                                if (mAudioEncoderMediaFormat != null) {
                                    mOutputAudioTrack =
                                            mMediaMuxer.addTrack(mAudioEncoderMediaFormat);
                                    MLog.d(TAG, "AudioEncoderThread mOutputAudioTrack: " +
                                            mOutputAudioTrack);
                                }
                                break;
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                // 输出缓冲区已经改变
                                MLog.d(TAG, "AudioEncoderThread " +
                                        "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                                break;
                            default:
                                break;
                        }

                        if (roomIndex < 0) {
                            break;
                        }

                        if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                            mIsAudioRecording = false;
                            break;
                        }
                        if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // 会执行一次
                            // 含有同步帧,因此如果在这里continue的话,writeSampleData应该调到上面执行
                            MLog.d(TAG, "AudioEncoderThread " +
                                    "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                            mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                            // 注意点四
                            continue;
                        }

                        room = mAudioEncoderMediaCodec.getOutputBuffer(roomIndex);
                        if (mIsAudioRecording
                                && mIsMuxerStarted) {
                            mMediaMuxer.writeSampleData(mOutputAudioTrack, room, roomInfo);
                        }

                        mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                    } catch (MediaCodec.CodecException e) {
                        MLog.e(TAG, "AudioEncoderThread Output occur exception: " + e);
                        mIsAudioRecording = false;
                        stopRecordScreen();
                        mUiHandler.removeMessages(AUDIO_NOTIFY);
                        mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                        break;
                    } catch (IllegalStateException e) {
                        MLog.e(TAG, "AudioEncoderThread Output occur exception: " + e);
                        mIsAudioRecording = false;
                        stopRecordScreen();
                        mUiHandler.removeMessages(AUDIO_NOTIFY);
                        mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                        break;
                    } catch (IllegalArgumentException e) {
                        MLog.e(TAG, "AudioEncoderThread Output occur exception: " + e);
                        mIsAudioRecording = false;
                        stopRecordScreen();
                        mUiHandler.removeMessages(AUDIO_NOTIFY);
                        mUiHandler.sendEmptyMessageDelayed(AUDIO_NOTIFY, 200);
                        break;
                    }
                }
            }// while end
            MLog.i(TAG, "AudioEncoderThread end");
        }
    }

    private class SocketAcceptThread implements Runnable {
        @Override
        public void run() {
            if (SocketServer.getInstance().bind()) {
                while (mIsServerStarted) {
                    SocketServer.getInstance().accept();
                    mSocket = SocketServer.getInstance().getSocket();
                }
            }
        }
    }

    private void test() {

    }

}
