package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.icu.text.SimpleDateFormat;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


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
            MLog.d(TAG, "onAttach() " + printThis() +
                    " context: " + context);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (DEBUG)
            MLog.d(TAG, "onAttach() " + printThis() +
                    " activity: " + activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        init();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG)
            MLog.d(TAG, "onCreateView() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewCreated() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewStateRestored() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onActivityCreated() " + printThis() +
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
            MLog.d(TAG, "onStart() " + printThis());
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
            MLog.d(TAG, "onResume() " + printThis());

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
            MLog.d(TAG, "onPause() " + printThis());
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
            MLog.d(TAG, "onStop() " + printThis());
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView() " + printThis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy() " + printThis());

        onHide();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach() " + printThis());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            MLog.d(TAG, "onSaveInstanceState() " + printThis());
    }

    public void handleConfigurationChangedEvent(
            Configuration newConfig,
            boolean needToDo,
            boolean override) {
        super.handleConfigurationChangedEvent(newConfig, needToDo, true);

        if (needToDo) {
            onShow();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG)
            MLog.d(TAG, "onLowMemory() " + printThis());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DEBUG)
            MLog.d(TAG, "onTrimMemory() " + printThis() +
                    " level: " + level);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (DEBUG)
            MLog.d(TAG, "onRequestPermissionsResult() " + printThis() +
                    " requestCode: " + requestCode);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (DEBUG)
            MLog.d(TAG, "onHiddenChanged() " + printThis() +
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


    private static final int audioSource = MediaRecorder.AudioSource.MIC;
    /***
     * 采样率
     * 现在能够保证在所有设备上使用的采样率是44100Hz,
     * 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用
     */
    private static final int sampleRateInHz = 44100;
    // private static final int sampleRateInHz = 11025;
    /***
     * 声道数
     * CHANNEL_IN_MONO
     * CHANNEL_IN_STEREO
     * 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的
     */
    // private static final int channelConfig_record = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    // private static final int channelConfig_track = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int channelConfig_record = AudioFormat.CHANNEL_IN_MONO;
    // 在我的手机上不能使用,一使用就出错
    private static final int channelConfig_track = AudioFormat.CHANNEL_OUT_MONO;
    /***
     * 返回的音频数据的格式。
     * ENCODING_PCM_8BIT
     * ENCODING_PCM_16BIT
     * ENCODING_PCM_FLOAT
     */
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private static final int mode = AudioTrack.MODE_STREAM;
    private static final int sessionId = AudioManager.AUDIO_SESSION_ID_GENERATE;

    @InjectView(R.id.control_btn)
    private Button mControlBtn;
    @InjectView(R.id.play_btn)
    private Button mPlayBtn;
    @InjectView(R.id.convert_btn)
    private Button mConvertBtn;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private byte[] mAudioData;
    private boolean mRecordRunning = false;
    private boolean mTrackRunning = false;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private void init() {
        initAudioRecord();
        initAudioTrack();
    }

    private void initAudioRecord() {
        if (mAudioRecord == null) {
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                    sampleRateInHz,
                    channelConfig_record,
                    audioFormat) * 2;
            if (DEBUG)
                MLog.d(TAG, "init() bufferSizeInBytes: " + bufferSizeInBytes);
            mAudioRecord = new AudioRecord(
                    audioSource,
                    sampleRateInHz,
                    channelConfig_record,
                    audioFormat,
                    bufferSizeInBytes);
        }
    }

    private void initAudioTrack() {
        if (mAudioTrack == null) {
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                    sampleRateInHz,
                    channelConfig_record,
                    audioFormat) * 2;
            if (DEBUG)
                MLog.d(TAG, "init() bufferSizeInBytes: " + bufferSizeInBytes);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(sampleRateInHz)
                    .setChannelMask(channelConfig_track)
                    .setEncoding(audioFormat)
                    .build();

            mAudioTrack = new AudioTrack(
                    attributes,
                    format,
                    bufferSizeInBytes,
                    mode,
                    sessionId);
        }
    }

    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

        if (mRecordRunning) {
            mControlBtn.setText("停止录音");
        } else {
            mControlBtn.setText("录音");
        }
        if (mTrackRunning) {
            mPlayBtn.setText("停止播放");
        } else {
            mPlayBtn.setText("播放");
        }
        mConvertBtn.setText("pcm To wav");
        mJumpBtn.setText("跳转到");
    }

    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide() " + printThis());

        if (mAudioRecord != null
                && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }

        if (mAudioTrack != null
                && mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    @InjectOnClick({R.id.control_btn, R.id.play_btn, R.id.convert_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.control_btn:
                startRecordOrStopRecord();
                break;
            case R.id.play_btn:
                playTrackOrStopTrack();
                break;
            case R.id.convert_btn:
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new A2Fragment());
                break;
            default:
        }
    }

    private void startRecordOrStopRecord() {
        mRecordRunning = !mRecordRunning;

        if (mRecordRunning) {
            mControlBtn.setText("停止录音");
            initAudioRecord();
            // start record
            if (mAudioRecord != null
                    && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                mAudioRecord.startRecording();
                startRecording();
            }
        } else {
            mControlBtn.setText("录音");
            // stop record
            if (mAudioRecord != null
                    && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
    }

    private void playTrackOrStopTrack() {
        mTrackRunning = !mTrackRunning;

        if (mTrackRunning) {
            mPlayBtn.setText("停止播放");
            initAudioTrack();
            // start track
            if (mAudioTrack != null
                    && mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
                mAudioTrack.play();
                play();
            }
        } else {
            mPlayBtn.setText("播放");
            // stop track
            if (mAudioTrack != null
                    && mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            }
        }
    }

    private void startRecording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // /data/user/0/com.weidi.usefragments/files/test.pcm
                File file = new File(
                        "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Music",
                        "test.pcm");
                // mSimpleDateFormat.format(new Date()) + ".pcm");
                /*if (!file.mkdirs()) {
                    MLog.e(TAG, "Directory not created");
                    return;
                }*/
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                        sampleRateInHz,
                        channelConfig_record,
                        audioFormat) * 2;
                mAudioData = new byte[bufferSizeInBytes];

                if (DEBUG)
                    MLog.d(TAG, "startRecording() start");

                while (mRecordRunning) {
                    // audioRecord把数据读到data中
                    int read = mAudioRecord.read(mAudioData, 0, bufferSizeInBytes);
                    // 如果读取音频数据没有出现错误，就将数据写入到文件
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        try {
                            // 把data数据写到os文件流中
                            os.write(mAudioData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    os.close();
                    os = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (DEBUG)
                    MLog.d(TAG, "startRecording() end");

                mAudioData = null;
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
                if (!file.exists()) {
                    return;
                }
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                        sampleRateInHz,
                        channelConfig_record,
                        audioFormat) * 2;
                // int bufferSizeInBytes = 4096;
                mAudioData = new byte[bufferSizeInBytes];

                if (DEBUG)
                    MLog.d(TAG, "play() start");

                try {
                    while (mTrackRunning && fis.available() > 0) {
                        int readCount = fis.read(mAudioData);
                        if (readCount == AudioTrack.ERROR_INVALID_OPERATION
                                || readCount == AudioTrack.ERROR_BAD_VALUE) {
                            continue;
                        }
                        if (readCount != 0 && readCount != -1) {
                            mAudioTrack.write(mAudioData, 0, readCount);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fis.close();
                    fis = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (DEBUG)
                    MLog.d(TAG, "play() end");

                mAudioData = null;
            }
        }).start();
    }

}
