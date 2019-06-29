package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.MLog;

import java.io.IOException;
import java.nio.ByteBuffer;

/***

 */
public class DecodeAudioFragment extends BaseFragment {

    private static final String TAG =
            DecodeAudioFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public DecodeAudioFragment() {
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
            MLog.d(TAG, "onActivityResult(): " + printThis() +
                    " requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data.toString());
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
        return R.layout.fragment_decode_audio;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    private static final int PREPARE = 0x0001;
    private static final int PAUSE = 0x0002;
    private static final int STOP = 0x0003;
    private static final String PATH =
            "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Music/";

    @InjectView(R.id.start_btn)
    private Button mStartBtn;
    @InjectView(R.id.pause_btn)
    private Button mPauseBtn;
    @InjectView(R.id.stop_btn)
    private Button mStopBtn;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private MediaExtractor mMediaExtractor;
    private MediaCodec mAudioDncoderMediaCodec;
    private MediaFormat mAudioDncoderMediaFormat;
    private AudioTrack mAudioTrack;
    private byte[] mPcmData;
    private int mAudioTrackIndex = -1;
    private int sampleRateInHz;
    private int channelCount;
    private int channelConfig;
    private int audioFormat;
    private int bufferSizeInBytes;
    private boolean mIsRunning = false;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private Object mPauseLock = new Object();
    private Object mStopLock = new Object();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

//        mStartBtn.setText("播放");
//        mPauseBtn.setText("暂停");
//        mStopBtn.setText("停止");
//        mJumpBtn.setText("跳转到");
    }

    /***
     代码执行的内容跟onPause(),onStop()一样,
     因此在某些情况下要么执行onPause(),onStop()方法,要么执行onHide()方法.
     一般做的事是视频的暂停,摄像头的关闭
     */
    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide(): " + printThis());
    }

    private void initData() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //super.threadHandleMessage(msg);
                DecodeAudioFragment.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //super.handleMessage(msg);
                DecodeAudioFragment.this.uiHandleMessage(msg);
            }
        };

        mThreadHandler.sendEmptyMessage(PREPARE);
    }

    private void initView(View view, Bundle savedInstanceState) {

    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {

    }

    @InjectOnClick({R.id.play_btn, R.id.pause_btn, R.id.stop_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_btn:
                start();
                break;
            case R.id.pause_btn:
                mThreadHandler.removeMessages(PAUSE);
                mThreadHandler.sendEmptyMessage(PAUSE);
                break;
            case R.id.stop_btn:
                mThreadHandler.removeMessages(STOP);
                mThreadHandler.sendEmptyMessage(STOP);
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new A2Fragment());
                break;
            default:
                break;
        }
    }

    private void prepare() {
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(PATH + "DreamItPossible.mp3");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int trackCount = mMediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            mAudioDncoderMediaFormat = mMediaExtractor.getTrackFormat(i);
            String mime = mAudioDncoderMediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                try {
                    mAudioDncoderMediaCodec = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                mAudioTrackIndex = i;
                break;
            }
        }
        if (mAudioTrackIndex < 0
                || mAudioDncoderMediaFormat == null) {
            return;
        }

        mMediaExtractor.selectTrack(mAudioTrackIndex);
        sampleRateInHz = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        channelCount = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        // 在Android平台上录制音频时可能下面的值
        /*channelConfig = mAudioDncoderMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK);
        if (channelConfig <= 0) {
        }*/
        switch (channelCount) {
            case 1:
                // 如果是单声道的话还不能确定是哪个值
                channelConfig = AudioFormat.CHANNEL_IN_MONO;// 16
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;// 4
                break;
            case 2:
                channelConfig = AudioFormat.CHANNEL_IN_STEREO;// 12
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;// 12
                break;
            default:
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
        }
        // 还不知道怎样从一个音频中得到"数据位宽"
        audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        bufferSizeInBytes = MediaUtils.getMinBufferSize(
                sampleRateInHz, channelConfig, audioFormat);
        mPcmData = new byte[bufferSizeInBytes * 2];
        mAudioTrack = MediaUtils.createAudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelCount, audioFormat,
                AudioTrack.MODE_STREAM);
    }

    private void start() {
        if (mAudioDncoderMediaCodec == null
                || mAudioDncoderMediaFormat == null
                || mAudioTrack == null) {
            return;
        }

        mIsRunning = true;
        mAudioDncoderMediaCodec.configure(
                mAudioDncoderMediaFormat, null, null, 0);
        mAudioDncoderMediaCodec.start();
        mAudioTrack.play();
        new Thread(new DecodeAudioThread()).start();
    }

    private void pause() {

    }

    private void stop() {
        if (!mIsRunning) {
            return;
        }

        mIsRunning = false;

        synchronized (mStopLock) {
            try {
                mStopLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        MediaUtils.stopMediaCodec(mAudioDncoderMediaCodec);
        MediaUtils.stopAudioTrack(mAudioTrack);
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case PREPARE:
                prepare();
                break;
            case PAUSE:
                pause();
                break;
            case STOP:
                stop();
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

    private class DecodeAudioThread implements Runnable {
        @Override
        public void run() {
            if (DEBUG)
                MLog.w(TAG, "DecodeAudioThread start");

            int readSize = -1;
            // 房间编号
            int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            // 房间
            ByteBuffer room = null;
            // 用于保存房间信息
            MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
            while (mIsRunning) {
                // Input
                roomIndex = mAudioDncoderMediaCodec.dequeueInputBuffer(-1);
                if (roomIndex >= 0) {
                    room = mAudioDncoderMediaCodec.getInputBuffer(roomIndex);
                    room.clear();
                    readSize = mMediaExtractor.readSampleData(room, 0);
                    long presentationTimeUs = System.nanoTime() / 1000;
                    if (readSize < 0) {
                        mAudioDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                0,
                                presentationTimeUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                        if (DEBUG)
                            MLog.w(TAG, "DecodeAudioThread read end");
                        mIsRunning = false;
                        break;
                    } else {
                        mAudioDncoderMediaCodec.queueInputBuffer(
                                roomIndex,
                                0,
                                readSize,
                                presentationTimeUs,
                                0);
                        mMediaExtractor.advance();
                    }
                }
                // Output
                roomIndex = mAudioDncoderMediaCodec.dequeueOutputBuffer(
                        roomInfo, 10000);
                while (roomIndex >= 0) {
                    room = mAudioDncoderMediaCodec.getOutputBuffer(roomIndex);
                    // room.limit()与bufferInfo.size的大小是相同的
                    // 一帧AAC数据,大小大概为500~550这个范围(每次得到的room大小是不一样的)
                    int roomSize = roomInfo.size;
                    byte[] pcmData = new byte[roomSize];
                    room.get(pcmData, 0, pcmData.length);
                    mAudioTrack.write(pcmData, 0, pcmData.length);

                    try {
                        mAudioDncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                        roomIndex = mAudioDncoderMediaCodec.dequeueOutputBuffer(
                                roomInfo, 10000);
                    } catch (IllegalStateException e) {
                        MLog.e(TAG, "startRecording Output occur exception: " + e);
                        e.printStackTrace();
                    }
                }
                //
                if (!mIsRunning) {
                    synchronized (mStopLock) {
                        mStopLock.notify();
                    }
                }
            }
            if (DEBUG)
                MLog.w(TAG, "DecodeAudioThread end");
        }
    }

}
