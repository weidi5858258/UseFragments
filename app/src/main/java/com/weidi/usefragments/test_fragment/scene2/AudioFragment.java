package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.weidi.usefragments.tool.AACHelper;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.SimpleAudioRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 只要当前Fragment没有调用onDestroy()方法,
 那么就不要调用
 MediaUtils.releaseAudioRecord(mAudioRecord);
 MediaUtils.releaseAudioTrack(mAudioTrack);
 只要调用相应的stop方法就行了,这样再次使用时
 不需要创建对象,也不容易出错.
 */
public class AudioFragment extends BaseFragment {

    private static final String TAG =
            AudioFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public AudioFragment() {
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
        return R.layout.fragment_audio;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    private static final int PREPARE = 0x0001;
    private static final int STOP_RECORD = 0x0002;
    private static final int PCM_TO_WAV = 0x0003;
    private static final String PATH =
            "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Music";

    @InjectView(R.id.control_btn)
    private Button mControlBtn;
    @InjectView(R.id.pause_record_btn)
    private Button mPauseRecordBtn;
    @InjectView(R.id.play_btn)
    private Button mPlayBtn;
    @InjectView(R.id.convert_btn)
    private Button mConvertBtn;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private SimpleAudioRecorder mSimpleAudioRecorder;
    private AudioTrack mAudioTrack;
    private byte[] mPcmData;
    private boolean mIsTrackRunning = false;
    //private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private Object mPauseLock = new Object();
    private Object mStopLock = new Object();

    // 回声消除器
    private AcousticEchoCanceler mAcousticEchoCanceler;
    // 噪音抑制器
    private NoiseSuppressor mNoiseSuppressor;

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

        if (mSimpleAudioRecorder.isRecording()) {
            mControlBtn.setText("停止录音");
            if (mSimpleAudioRecorder.isPaused()) {
                mPauseRecordBtn.setText("继续录音");
            } else {
                mPauseRecordBtn.setText("暂停录音");
            }
        } else {
            mControlBtn.setText("开始录音");
            mPauseRecordBtn.setText("");
        }
        if (mIsTrackRunning) {
            mPlayBtn.setText("停止播放");
        } else {
            mPlayBtn.setText("播放");
        }
        mConvertBtn.setText("pcm To wav");
        mJumpBtn.setText("跳转到");
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
                AudioFragment.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                //super.handleMessage(msg);
                AudioFragment.this.uiHandleMessage(msg);
            }
        };

//        mThreadHandler.sendEmptyMessage(PREPARE);
        prepare();
    }

    private void initView(View view, Bundle savedInstanceState) {

    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        mIsTrackRunning = false;
        synchronized (mPauseLock) {
            if (DEBUG)
                MLog.d(TAG, "destroy() mPauseLock.notify()");
            mPauseLock.notify();
        }
        MediaUtils.releaseAudioTrack(mAudioTrack);

        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
    }

    @InjectOnClick({R.id.control_btn,
            R.id.pause_record_btn,
            R.id.play_btn,
            R.id.convert_btn,
            R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.control_btn:
                if (mSimpleAudioRecorder.isRecording()) {
                    mSimpleAudioRecorder.stop();
                } else {
                    mSimpleAudioRecorder.play();
                }
                break;
            case R.id.pause_record_btn:
                mSimpleAudioRecorder.pause();
                break;
            case R.id.play_btn:
                playTrackOrStopTrack();
                break;
            case R.id.convert_btn:
                mThreadHandler.removeMessages(PCM_TO_WAV);
                mThreadHandler.sendEmptyMessage(PCM_TO_WAV);
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new DecodeAudioFragment());
                break;
            default:
        }
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }
        switch (msg.what) {
            case PREPARE:
                prepare();
                break;
            case STOP_RECORD:
                break;
            case PCM_TO_WAV:
                pcmTOwav();
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

    private void prepare() {
        mSimpleAudioRecorder = new SimpleAudioRecorder();
        mSimpleAudioRecorder.setContext(getContext());
        mSimpleAudioRecorder.setCallback(mCallback);

        mAudioDecoderMediaCodec = MediaUtils.getAudioDecoderMediaCodec();
        mAudioDecoderMediaFormat = MediaUtils.getAudioDecoderMediaFormat();
        if (mSimpleAudioRecorder.getAudioRecord() != null) {
            if (mAudioTrack == null) {
                mAudioTrack = MediaUtils.createAudioTrack(
                        mSimpleAudioRecorder.getAudioRecord().getAudioSessionId());
            }
        } else {
            if (mAudioTrack == null) {
                mAudioTrack = MediaUtils.createAudioTrack(MediaUtils.sessionId);
            }
        }
        if (DEBUG)
            if (mAudioTrack != null) {
                MLog.d(TAG, "onStarted() state: " + mAudioTrack.getState() +
                        " AudioTrackSessionId:  " + mAudioTrack.getAudioSessionId());
            }
        if (mAudioDecoderMediaCodec != null
                && mAudioDecoderMediaFormat != null) {
            // 0X02 0x04 0x02 0X00
            // 00010 0100 0010 000
            // 0001 0010 0001 0000
            mAudioDecoderMediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            mAudioDecoderMediaFormat.setInteger(MediaFormat.KEY_PRIORITY, 0 /* realtime priority */);
            // ByteBuffer key
            //byte[] data = new byte[]{(byte) 0x12, (byte) 0x10};
            //byte[] data = new byte[]{(byte) 0x18, (byte) 0x16};
            byte[] data = MediaUtils.buildAacAudioSpecificConfig();
            List<byte[]> list = new ArrayList<>();
            list.add(data);
            MediaUtils.setCsdBuffers(mAudioDecoderMediaFormat, list);
            /*ByteBuffer csd_0 = ByteBuffer.wrap(data);
            // ADT头的解码信息
            mAudioDecoderMediaFormat.setByteBuffer("csd-0", csd_0);*/
            MLog.d(TAG, "prepare() " + mAudioDecoderMediaFormat);
            /*mAudioDecoderMediaCodec.configure(
                    mAudioDecoderMediaFormat, null, null, 0);
            mAudioDecoderMediaCodec.start();*/
        }
    }

    private void playTrackOrStopTrack() {
        mIsTrackRunning = !mIsTrackRunning;

        if (mIsTrackRunning) {
            if (mAudioTrack != null) {
                mPlayBtn.setText("停止播放");
                mAudioTrack.play();
                playPcm();
            }
        } else {
            mPlayBtn.setText("播放");
        }
    }

    private void pcmTOwav() {
        MLog.d(TAG, "pcmTOwav() start");

        File pcmFile = new File(PATH, "test1.pcm");
        File wavFile = new File(PATH, "test1.wav");
        if (!pcmFile.exists()
                || pcmFile.length() <= 0) {
            return;
        }
        if (wavFile.exists()) {
            try {
                wavFile.delete();
            } catch (SecurityException e) {
                e.printStackTrace();
                return;
            }
        }
        if (!wavFile.exists()) {
            try {
                wavFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        FileInputStream pcmIS = null;
        FileOutputStream wavOS = null;
        try {
            pcmIS = new FileInputStream(pcmFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        try {
            wavOS = new FileOutputStream(wavFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // do something
        try {
            long pcmSize = pcmIS.getChannel().size();
            MediaUtils.addWaveHeaderToPcmFile(
                    wavOS,
                    pcmSize,
                    MediaUtils.sampleRateInHz,
                    MediaUtils.channelCount,
                    MediaUtils.AUDIO_BIT_RATE);
            byte[] data = new byte[MediaUtils.getMinBufferSize()];
            while (pcmIS.read(data) != -1) {
                wavOS.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (pcmIS != null) {
                pcmIS.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (wavOS != null) {
                wavOS.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (wavOS != null) {
                try {
                    wavOS.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "成功生成wav文件", Toast.LENGTH_SHORT).show();
            }
        });

        MLog.d(TAG, "pcmTOwav() end");
    }

    /**
     * 寻找指定buffer中AAC帧头的开始位置
     *
     * @param startIndex 开始的位置
     * @param data       数据
     * @param max        需要检测的最大值
     * @return
     */
    private int findHead(byte[] data, int startIndex, int max) {
        int i;
        for (i = startIndex; i <= max; i++) {
            //发现帧头
            if (isHead(data, i))
                break;
        }
        //检测到最大值，未发现帧头
        if (i == max) {
            i = -1;
        }
        return i;
    }

    /**
     * 判断aac帧头
     */
    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        if (data[offset] == (byte) 0xFF && data[offset + 1] == (byte) 0xF1
                && data[offset + 3] == (byte) 0x80) {
            result = true;
        }
        return result;
    }

    //修眠
    private void sleepThread(long startTime, long endTime) {
        //根据读文件和解码耗时，计算需要休眠的时间
        long time = PRE_FRAME_TIME - (endTime - startTime);
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //文件读取完成标识
    private boolean isFinish = false;
    //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 50;
    //一般AAC帧大小不超过200k,如果解码失败可以尝试增大这个值
    private static int FRAME_MAX_LEN = 100 * 1024;
    //根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 1000 / 50;

    private static final int TIME_OUT = 10000;
    private MediaCodec mAudioDecoderMediaCodec;
    private MediaFormat mAudioDecoderMediaFormat;

    private void playPcm() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioDecoderMediaCodec.configure(
                        mAudioDecoderMediaFormat, null, null, 0);
                mAudioDecoderMediaCodec.start();

                File file = new File(PATH, "test1.aac");
                if (!file.exists()
                        || !file.canRead()) {
                    return;
                }

                long length = file.length();
                long playLength = 0;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                int bufferSizeInBytes =
                        MediaUtils.getMinBufferSize() * 2;
                mPcmData = new byte[bufferSizeInBytes];

                if (DEBUG)
                    MLog.d(TAG, "playPcm() start");

                int readSize = -1;
                // 房间号
                int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
                // 房间
                ByteBuffer room = null;
                // 房间大小
                int roomSize = 0;
                // 房间信息
                MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();
                ByteBuffer[] inputBuffers = mAudioDecoderMediaCodec.getInputBuffers();
                ByteBuffer[] outputBuffers = mAudioDecoderMediaCodec.getOutputBuffers();
                AACHelper aacHelper = null;
                try {
                    aacHelper = new AACHelper(file.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                while (mIsTrackRunning) {
                    try {
                        if (fis.available() <= 0) {
                            mIsTrackRunning = false;
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    try {
                        readSize = fis.read(mPcmData, 0, mPcmData.length);
                        MLog.d(TAG, "playPcm() readSize: " + readSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (readSize <= 0) {
                        mIsTrackRunning = false;
                        break;
                    }

                    // Input
                    try {
                        roomIndex = mAudioDecoderMediaCodec.dequeueInputBuffer(-1);
                        if (roomIndex >= 0) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                room = mAudioDecoderMediaCodec.getInputBuffer(roomIndex);
                            } else {
                                room = inputBuffers[roomIndex];
                            }
                            if (room != null) {
                                room.clear();
                                room.put(mPcmData);
                                /*try {
                                    readSize = aacHelper.getSample(room);
                                    MLog.d(TAG, "playPcm() readSize: " + readSize);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }*/
                            }
                            long presentationTimeUs = System.nanoTime() / 1000;
                            mAudioDecoderMediaCodec.queueInputBuffer(
                                    roomIndex,
                                    0,
                                    readSize,
                                    presentationTimeUs,
                                    0);
                        }
                    } catch (MediaCodec.CryptoException
                            | IllegalStateException
                            | NullPointerException e) {
                        MLog.e(TAG, "playPcm() Input occur exception: " + e);
                        e.printStackTrace();
                        mIsTrackRunning = false;
                        break;
                    }

                    // Output
                    for (; ; ) {
                        try {
                            roomIndex = mAudioDecoderMediaCodec.dequeueOutputBuffer(
                                    roomInfo, TIME_OUT);
                            MLog.d(TAG, "playPcm() roomIndex: " + roomIndex);
                            switch (roomIndex) {
                                case MediaCodec.INFO_TRY_AGAIN_LATER:
                                    break;
                                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                    MLog.d(TAG, "playPcm() " +
                                            "Output MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                                    mAudioDecoderMediaFormat = mAudioDecoderMediaCodec.getOutputFormat();
                                    MLog.d(TAG, "playPcm() " + mAudioDecoderMediaFormat);
                                    break;
                                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                    MLog.d(TAG, "playPcm() " +
                                            "Output MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                        outputBuffers = mAudioDecoderMediaCodec.getOutputBuffers();
                                    }
                                    break;
                                default:
                                    break;
                            }
                            if (roomIndex < 0) {
                                break;
                            }

                            if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                MLog.d(TAG, "playPcm() " +
                                        "Output MediaCodec.BUFFER_FLAG_CODEC_CONFIG");
                                mAudioDecoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                                continue;
                            }
                            if ((roomInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                MLog.d(TAG, "playPcm() " +
                                        "Output MediaCodec.BUFFER_FLAG_END_OF_STREAM");
                                mIsTrackRunning = false;
                                break;
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                room = mAudioDecoderMediaCodec.getOutputBuffer(roomIndex);
                            } else {
                                room = outputBuffers[roomIndex];
                            }

                            if (room != null) {
                                roomSize = roomInfo.size;
                                room.position(roomInfo.offset);
                                room.limit(roomInfo.offset + roomSize);
                                byte[] pcmData = new byte[roomSize];
                                room.get(pcmData, 0, pcmData.length);
                                int writeSize = mAudioTrack.write(pcmData, 0, roomSize);
                                MLog.d(TAG, "playPcm() writeSize: " + writeSize);
                                playLength += writeSize;
                            }

                            mAudioDecoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                        } catch (IllegalStateException
                                | IllegalArgumentException
                                | NullPointerException e) {
                            MLog.e(TAG, "playPcm() Output occur exception: " + e);
                            e.printStackTrace();
                            mIsTrackRunning = false;
                            break;
                        }
                    }// for(;;) end




























                    /*int write = mAudioTrack.write(mPcmData, 0, readSize);
                    playLength += write;*/
                }

                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fis = null;
                }

                if (DEBUG)
                    MLog.d(TAG, "playPcm() end");

                MediaUtils.stopAudioTrack(mAudioTrack);
                // 内容播放完毕
                if (length == playLength) {
                    MLog.d(TAG, "playPcm() playLength: " + playLength);
                    mIsTrackRunning = false;
                }
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlayBtn.setText("播放");
                    }
                });

                mPcmData = null;
            }
        }).start();
    }

    private int count = 0;

    private Callback mCallback = new Callback() {
        @Override
        public void onReady() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    onShow();
                }
            });
        }

        @Override
        public void onPaused() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    onShow();
                }
            });
        }

        @Override
        public void onStarted() {
            if (mSimpleAudioRecorder.getAudioRecord() != null) {
                if (mAudioTrack == null) {
                    mAudioTrack = MediaUtils.createAudioTrack(
                            mSimpleAudioRecorder.getAudioRecord().getAudioSessionId());
                }
            } else {
                if (mAudioTrack == null) {
                    mAudioTrack = MediaUtils.createAudioTrack(MediaUtils.sessionId);
                }
            }
            if (DEBUG)
                if (mAudioTrack != null) {
                    MLog.d(TAG, "onStarted() state: " + mAudioTrack.getState() +
                            " AudioTrackSessionId:  " + mAudioTrack.getAudioSessionId());
                }
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    onShow();
                }
            });
        }

        @Override
        public void onFinished() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    onShow();
                }
            });
        }

        @Override
        public void onProgressUpdated(long presentationTimeUs) {
            MLog.d(TAG, "onProgressUpdated() time: " + (++count));
        }

        @Override
        public void onError() {

        }

        @Override
        public void onInfo(String info) {

        }
    };

}
