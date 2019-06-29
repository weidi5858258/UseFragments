package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.icu.text.SimpleDateFormat;
import android.media.AudioRecord;
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
import android.support.annotation.RequiresApi;
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
import com.weidi.usefragments.tool.MLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/***
 只要当前Fragment没有调用onDestroy()方法,
 那么就不要调用
 MediaUtils.releaseAudioRecord(mAudioRecord);
 MediaUtils.releaseAudioTrack(mAudioTrack);
 只要调用相应的stop方法就行了,这样再次使用时
 不需要创建对象,也不容易出错.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
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

    private MediaCodec mAudioEncoderMediaCodec;
    private MediaFormat mAudioEncoderMediaFormat;
    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private byte[] mPcmData;
    private boolean mIsRecordRunning = false;
    private boolean mIsRecordPaused = false;
    private boolean mIsTrackRunning = false;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

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

        if (mIsRecordRunning) {
            mControlBtn.setText("停止录音");
            if (mIsRecordPaused) {
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

        mThreadHandler.sendEmptyMessage(PREPARE);
    }

    private void initView(View view, Bundle savedInstanceState) {

    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        mIsRecordRunning = false;
        mIsRecordPaused = false;
        mIsTrackRunning = false;
        synchronized (mPauseLock) {
            if (DEBUG)
                MLog.d(TAG, "destroy() mPauseLock.notify()");
            mPauseLock.notify();
        }
        MediaUtils.releaseAudioRecord(mAudioRecord);
        MediaUtils.releaseAudioTrack(mAudioTrack);
        MediaUtils.releaseMediaCodec(mAudioEncoderMediaCodec);
        releaseAcousticEchoCanceler();

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
                startRecordOrStopRecord();
                break;
            case R.id.pause_record_btn:
                pauseRecord();
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
                stopRecord();
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
        mAudioEncoderMediaCodec = MediaUtils.getAudioEncoderMediaCodec();
        mAudioEncoderMediaFormat = MediaUtils.getAudioEncoderMediaFormat();

        if (mAudioRecord == null) {
            mAudioRecord = MediaUtils.createAudioRecord();
            if (DEBUG)
                if (mAudioRecord != null) {
                    MLog.d(TAG, "prepare() state: " + mAudioRecord.getState() +
                            " AudioRecordSessionId: " + mAudioRecord.getAudioSessionId());
                }
        }

        if (mAudioRecord != null) {
            if (mAudioTrack == null) {
                mAudioTrack = MediaUtils.createAudioTrack(mAudioRecord.getAudioSessionId());
            }
        } else {
            if (mAudioTrack == null) {
                mAudioTrack = MediaUtils.createAudioTrack(MediaUtils.sessionId);
            }
        }
        if (DEBUG)
            if (mAudioTrack != null) {
                MLog.d(TAG, "prepare() state: " + mAudioTrack.getState() +
                        " AudioTrackSessionId:  " + mAudioTrack.getAudioSessionId());
            }

        // 我的手机不支持
        if (AcousticEchoCanceler.isAvailable()) {
            if (mAcousticEchoCanceler == null) {
                if (mAudioRecord != null) {
                    int audioSession = mAudioRecord.getAudioSessionId();
                    mAcousticEchoCanceler = AcousticEchoCanceler.create(audioSession);
                    if (mAcousticEchoCanceler != null) {
                        mAcousticEchoCanceler.setEnabled(true);
                        if (DEBUG)
                            MLog.d(TAG, "initData(): " + printThis() +
                                    " 此手机支持回声消除功能");
                    }
                }
            }
        }
    }

    private void startRecordOrStopRecord() {
        mIsRecordRunning = !mIsRecordRunning;

        if (mIsRecordRunning) {
            startRecord();
        } else {
            mThreadHandler.removeMessages(STOP_RECORD);
            mThreadHandler.sendEmptyMessage(STOP_RECORD);
        }
    }

    private void startRecord() {
        if (mAudioRecord == null
                || mAudioEncoderMediaCodec == null) {
            return;
        }

        onShow();
        // AudioRecord
        mAudioRecord.startRecording();
        // MediaCodec
        try {
            if (mAudioEncoderMediaCodec != null) {
                mAudioEncoderMediaCodec.configure(
                        mAudioEncoderMediaFormat,
                        null,
                        null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                mAudioEncoderMediaCodec.start();
            }
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
            if (mAudioEncoderMediaCodec != null) {
                mAudioEncoderMediaCodec.release();
                mAudioEncoderMediaCodec = null;
            }
        }
        startRecording();
    }

    private void pauseRecord() {
        if (!mIsRecordRunning) {
            return;
        }
        mIsRecordPaused = !mIsRecordPaused;

        onShow();
        if (!mIsRecordPaused) {
            synchronized (mPauseLock) {
                if (DEBUG)
                    MLog.d(TAG, "pauseRecord() mPauseLock.notify()");
                mPauseLock.notify();
            }
        }
    }

    private void stopRecord() {
        if (mAudioRecord == null
                || mAudioEncoderMediaCodec == null) {
            return;
        }

        if (DEBUG)
            MLog.w(TAG, "stopRecord() start");

        mIsRecordPaused = false;
        synchronized (mPauseLock) {
            if (DEBUG)
                MLog.d(TAG, "stopRecord() mPauseLock.notify()");
            mPauseLock.notify();
        }

        synchronized (mStopLock) {
            try {
                MLog.d(TAG, "stopRecord mStopLock.wait() start");
                mStopLock.wait();
                MLog.d(TAG, "stopRecord mStopLock.wait() end");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                onShow();
            }
        });
        MediaUtils.stopAudioRecord(mAudioRecord);
        MediaUtils.stopMediaCodec(mAudioEncoderMediaCodec);

        if (DEBUG)
            MLog.w(TAG, "stopRecord() end");
    }

    private void playTrackOrStopTrack() {
        mIsTrackRunning = !mIsTrackRunning;

        if (mIsTrackRunning) {
            if (mAudioTrack != null) {
                mPlayBtn.setText("停止播放");
                mAudioTrack.play();
                play();
            }
        } else {
            mPlayBtn.setText("播放");
        }
    }

    private void releaseAcousticEchoCanceler() {
        if (mAcousticEchoCanceler == null) {
            return;
        }
        mAcousticEchoCanceler.setEnabled(false);
        mAcousticEchoCanceler.release();
        mAcousticEchoCanceler = null;
    }

    private void pcmTOwav() {
        MLog.d(TAG, "pcmTOwav() start");

        File pcmFile = new File(PATH, "test.pcm");
        File wavFile = new File(PATH, "test.wav");
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

    private void startRecording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // /data/user/0/com.weidi.usefragments/files/test.pcm
                File pcmFile = new File(PATH, "test.pcm");
                File aacFile = new File(PATH, "test.aac");
                if (pcmFile.exists()) {
                    try {
                        pcmFile.delete();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
                if (aacFile.exists()) {
                    try {
                        aacFile.delete();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    pcmFile.createNewFile();
                    aacFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                FileOutputStream pcmOS = null;
                try {
                    pcmOS = new FileOutputStream(pcmFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                BufferedOutputStream aacOS = null;
                try {
                    aacOS = new BufferedOutputStream(
                            new FileOutputStream(aacFile), 200 * 1024);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                // 16384
                int bufferSizeInBytes =
                        MediaUtils.getMinBufferSize() * 2;
                if (DEBUG)
                    MLog.d(TAG, "startRecording() bufferSizeInBytes: " + bufferSizeInBytes);
                mPcmData = new byte[bufferSizeInBytes];
                int readSize = -1;
                // 房间编号
                int roomIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
                // 房间
                ByteBuffer room = null;
                // 用于保存房间信息
                MediaCodec.BufferInfo roomInfo = new MediaCodec.BufferInfo();

                if (DEBUG)
                    MLog.w(TAG, "startRecording() start");

                while (mIsRecordRunning) {
                    // 录音暂停装置
                    if (mIsRecordPaused) {
                        synchronized (mPauseLock) {
                            if (DEBUG)
                                MLog.i(TAG, "startRecording() mPauseLock.wait() start");
                            try {
                                mPauseLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (DEBUG)
                                MLog.i(TAG, "startRecording() mPauseLock.wait() end");
                        }
                    }
                    // audioRecord把数据读到data中
                    readSize = mAudioRecord.read(mPcmData, 0, bufferSizeInBytes);
                    if (readSize < 0) {
                        mIsRecordRunning = false;
                        break;
                    }
                    try {
                        // 把data数据写到os文件流中
                        pcmOS.write(mPcmData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Input
                    try {
                        roomIndex = mAudioEncoderMediaCodec.dequeueInputBuffer(-1);
                        if (roomIndex >= 0) {
                            room = mAudioEncoderMediaCodec.getInputBuffer(roomIndex);
                            room.clear();
                            room.put(mPcmData);
                            long presentationTimeUs = System.nanoTime() / 1000;
                            mAudioEncoderMediaCodec.queueInputBuffer(
                                    roomIndex,
                                    0,
                                    mPcmData.length,
                                    presentationTimeUs,
                                    0);
                        }
                    } catch (MediaCodec.CryptoException
                            | IllegalStateException e) {
                        MLog.e(TAG, "startRecording Input occur exception: " + e);
                        mIsRecordRunning = false;
                        break;
                    }

                    // Output
                    roomIndex = mAudioEncoderMediaCodec.dequeueOutputBuffer(
                            roomInfo, 10000);
                    // 会执行多次
                    while (roomIndex >= 0) {
                        room = mAudioEncoderMediaCodec.getOutputBuffer(roomIndex);

                        // room.limit()与bufferInfo.size的大小是相同的
                        // 一帧AAC数据,大小大概为500~550这个范围(每次得到的room大小是不一样的)
                        int roomSize = roomInfo.size;

                        //////////////////////////AAC编码操作//////////////////////////
                        /***
                         roomInfo.offset一直为0
                         置bufferInfo.offset这个位置,
                         意思就是:从room的第bufferInfo.offset个位置开始读数据
                         */
                        room.position(roomInfo.offset);
                        room.limit(roomInfo.offset + roomSize);
                        // 一帧AAC数据和ADTS头的总大小
                        int frameSize = roomSize + 7;
                        // 空间只能不断地new
                        byte[] aacData = new byte[frameSize];
                        // 先添加7个字节的头信息
                        MediaUtils.addADTStoFrame(aacData, frameSize);
                        room.get(aacData, 7, roomSize);
                        room.position(roomInfo.offset);
                        try {
                            aacOS.write(aacData, 0, aacData.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        /////////////////////////////////////////////////////////////

                        //////////////////////////其他编码操作//////////////////////////
                        // ......
                        //////////////////////////////////////////////////////////////

                        try {
                            mAudioEncoderMediaCodec.releaseOutputBuffer(roomIndex, false);
                            roomIndex = mAudioEncoderMediaCodec.dequeueOutputBuffer(
                                    roomInfo, 10000);
                        } catch (IllegalStateException e) {
                            MLog.e(TAG, "startRecording Output occur exception: " + e);
                            e.printStackTrace();
                        }
                    }
                    // 退出装置
                    if (!mIsRecordRunning) {
                        synchronized (mStopLock) {
                            if (DEBUG)
                                MLog.d(TAG, "startRecording() mStopLock.notify()");
                            mStopLock.notify();
                        }
                    }
                }

                try {
                    if (pcmOS != null) {
                        pcmOS.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (pcmOS != null) {
                        try {
                            pcmOS.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            pcmOS = null;
                        }
                    }
                }

                try {
                    if (aacOS != null) {
                        aacOS.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (aacOS != null) {
                        try {
                            aacOS.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            aacOS = null;
                        }
                    }
                }

                if (DEBUG)
                    MLog.w(TAG, "startRecording() end");

                mPcmData = null;
            }
        }).start();
    }

    private void play() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(
                        "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Music",
                        "test.pcm");
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
                    MLog.d(TAG, "play() start");

                int readCount = 0;
                while (mIsTrackRunning) {
                    try {
                        if (fis.available() <= 0) {
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    try {
                        readCount = fis.read(mPcmData);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (readCount <= 0) {
                        continue;
                    }
                    int write = mAudioTrack.write(mPcmData, 0, readCount);
                    playLength += write;
                }

                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fis = null;
                }

                if (DEBUG)
                    MLog.d(TAG, "play() end");

                MediaUtils.stopAudioTrack(mAudioTrack);
                // 内容播放完毕
                if (length == playLength) {
                    MLog.d(TAG, "play() playLength: " + playLength);
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

}
