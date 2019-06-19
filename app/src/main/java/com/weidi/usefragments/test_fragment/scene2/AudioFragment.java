package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.icu.text.SimpleDateFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
    // 回声消除器
    private AcousticEchoCanceler mAcousticEchoCanceler;
    // 噪音抑制器
    private NoiseSuppressor mNoiseSuppressor;
    private byte[] mAudioData;
    private boolean mRecordRunning = false;
    private boolean mTrackRunning = false;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

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
        initAudioRecord();
        if (mAudioRecord != null) {
            initAudioTrack(mAudioRecord.getAudioSessionId());
        } else {
            initAudioTrack(MediaUtils.sessionId);
        }
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

    private void initView(View view, Bundle savedInstanceState) {

    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        mRecordRunning = false;
        mTrackRunning = false;
        MediaUtils.releaseAudioRecord(mAudioRecord);
        MediaUtils.releaseAudioTrack(mAudioTrack);
        releaseAcousticEchoCanceler();
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

    private void initAudioRecord() {
        if (mAudioRecord == null) {
            mAudioRecord = MediaUtils.createAudioRecord();
            if (DEBUG)
                MLog.d(TAG, "initAudioRecord() state: " + mAudioRecord.getState());
        }
    }

    private void initAudioTrack(int sessionId) {
        if (mAudioTrack == null) {
            mAudioTrack = MediaUtils.createAudioTrack(sessionId);
            if (DEBUG)
                MLog.d(TAG, "initAudioTrack() state: " + mAudioTrack.getState() +
                        " AudioRecordSessionId: " + sessionId +
                        " AudioTrackSessionId: " + mAudioTrack.getAudioSessionId());
        }
    }

    private void startRecordOrStopRecord() {
        mRecordRunning = !mRecordRunning;

        if (mRecordRunning) {
            if (mAudioRecord != null) {
                mControlBtn.setText("停止录音");
                mAudioRecord.startRecording();
                startRecording();
            }
        } else {
            mControlBtn.setText("录音");
        }
    }

    private void playTrackOrStopTrack() {
        mTrackRunning = !mTrackRunning;

        if (mTrackRunning) {
            if (mAudioTrack != null) {
                mPlayBtn.setText("停止播放");
                mAudioTrack.play();
                play();
            }
        } else {
            mPlayBtn.setText("播放");
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
                if (!file.canWrite()) {
                    return;
                }
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                int bufferSizeInBytes =
                        MediaUtils.getMinBufferSize() * 2;
                mAudioData = new byte[bufferSizeInBytes];

                if (DEBUG)
                    MLog.d(TAG, "startRecording() start");

                while (mRecordRunning) {
                    // audioRecord把数据读到data中
                    int read = mAudioRecord.read(mAudioData, 0, bufferSizeInBytes);
                    // MLog.d(TAG, "startRecording() read: " + read);
                    if (read <= 0) {
                        continue;
                    }
                    try {
                        // 把data数据写到os文件流中
                        os.write(mAudioData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    os = null;
                }

                if (DEBUG)
                    MLog.d(TAG, "startRecording() end");

                MediaUtils.stopAudioRecord(mAudioRecord);

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
                mAudioData = new byte[bufferSizeInBytes];

                if (DEBUG)
                    MLog.d(TAG, "play() start");

                int readCount = 0;
                while (mTrackRunning) {
                    try {
                        if (fis.available() <= 0) {
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    try {
                        readCount = fis.read(mAudioData);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (readCount <= 0) {
                        continue;
                    }
                    int write = mAudioTrack.write(mAudioData, 0, readCount);
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
                    mTrackRunning = false;
                }
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlayBtn.setText("播放");
                    }
                });

                mAudioData = null;
            }
        }).start();
    }

    private void releaseAcousticEchoCanceler() {
        if (mAcousticEchoCanceler == null) {
            return;
        }
        mAcousticEchoCanceler.setEnabled(false);
        mAcousticEchoCanceler.release();
        mAcousticEchoCanceler = null;
    }

}
