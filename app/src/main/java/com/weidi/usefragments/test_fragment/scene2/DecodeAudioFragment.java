package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.SampleAudioPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    // 顺序播放
    private static final int SEQUENTIAL_PLAYBACK = 0x001;
    // 随机播放
    private static final int RANDOM_PLAYBACK = 0x002;
    // 循环播放
    private static final int LOOP_PLAYBACK = 0x003;

    private int mPlayMode = RANDOM_PLAYBACK;

    @InjectView(R.id.info_tv)
    private TextView mShowInfoTv;
    @InjectView(R.id.play_btn)
    private Button mPlayBtn;
    @InjectView(R.id.pause_btn)
    private Button mPauseBtn;
    @InjectView(R.id.stop_btn)
    private Button mStopBtn;
    @InjectView(R.id.next_btn)
    private Button mNextBtn;
    @InjectView(R.id.prev_btn)
    private Button mPrevBtn;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private SampleAudioPlayer mSampleAudioPlayer;

    private List<File> musicFiles;
    private int mCurMusicIndex;
    private File mCurMusicFile;

    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private Random mRandom = new Random();
    private List<Integer> mHasPlayed = new ArrayList<Integer>();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

        if (mCurMusicFile != null) {
            String name = mCurMusicFile.getAbsolutePath();
            name = name.substring(name.lastIndexOf("/") + 1, name.length());
            mShowInfoTv.setText(name);
        }
        mPlayBtn.setText("播放");
        mPauseBtn.setText("暂停");
        mStopBtn.setText("停止");
        mPrevBtn.setText("上一首");
        mNextBtn.setText("下一首");
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
        File file = new File("/storage/2430-1702/BaiduNetdisk/music");
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                musicFiles = new ArrayList<File>();
                Collections.addAll(musicFiles, files);
            }
            // /storage/2430-1702/BaiduNetdisk/music/谭咏麟 - 水中花.mp3
            /*for (File tempFile : files) {
                if (DEBUG)
                    MLog.d(TAG, "initData(): " + printThis() + " " + tempFile.getAbsolutePath());
            }*/
        }

        mSampleAudioPlayer = new SampleAudioPlayer();
        mSampleAudioPlayer.setCallback(
                new SampleAudioPlayer.Callback() {
                    @Override
                    public void onPlaybackReady() {

                    }

                    @Override
                    public void onPlaybackPaused() {

                    }

                    @Override
                    public void onPlaybackStarted() {

                    }

                    @Override
                    public void onPlaybackFinished() {
                        next();
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onShow();
                            }
                        });
                    }

                    @Override
                    public void onProgressUpdated(int progress) {

                    }

                    @Override
                    public void onPlaybackError() {

                    }
                });

        if (musicFiles != null) {
            mCurMusicIndex = 0;
            mCurMusicFile = musicFiles.get(mCurMusicIndex);
            mSampleAudioPlayer.setPath(mCurMusicFile.getAbsolutePath());
        }
    }

    private void initView(View view, Bundle savedInstanceState) {

    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        mSampleAudioPlayer.release();
    }

    @InjectOnClick({R.id.play_btn, R.id.pause_btn, R.id.stop_btn,
            R.id.prev_btn, R.id.next_btn,
            R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_btn:
                mSampleAudioPlayer.play();
                break;
            case R.id.pause_btn:
                mSampleAudioPlayer.pause();
                break;
            case R.id.stop_btn:
                mSampleAudioPlayer.stop();
                break;
            case R.id.prev_btn:
                prev();
                onShow();
                break;
            case R.id.next_btn:
                next();
                onShow();
                break;
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new A2Fragment());
                break;
            default:
                break;
        }
    }

    private void prev() {
        if (musicFiles == null
                || musicFiles.isEmpty()) {
            return;
        }

        switch (mPlayMode) {
            case SEQUENTIAL_PLAYBACK:
                mCurMusicIndex -= 1;
                if (mCurMusicIndex < 0) {
                    mCurMusicIndex = musicFiles.size() - 1;
                }
                mCurMusicFile = musicFiles.get(mCurMusicIndex);
                break;
            case RANDOM_PLAYBACK:
                int size = musicFiles.size();
                while (true) {
                    int randomNumber = mRandom.nextInt(size);
                    if (!mHasPlayed.contains(randomNumber)) {
                        mHasPlayed.add(randomNumber);
                        mCurMusicIndex = randomNumber;
                        mCurMusicFile = musicFiles.get(mCurMusicIndex);
                        break;
                    } else {
                        if (mHasPlayed.size() == size) {
                            mHasPlayed.clear();
                        }
                    }
                }
                break;
            case LOOP_PLAYBACK:
                break;
            default:
                break;
        }
        mSampleAudioPlayer.setPath(mCurMusicFile.getAbsolutePath());
        if (DEBUG)
            MLog.d(TAG, "prev() mCurMusicIndex: " + mCurMusicIndex +
                    " " + mCurMusicFile.getAbsolutePath());
        mSampleAudioPlayer.prev();
    }

    private void next() {
        if (musicFiles == null
                || musicFiles.isEmpty()) {
            return;
        }

        switch (mPlayMode) {
            case SEQUENTIAL_PLAYBACK:
                mCurMusicIndex += 1;
                if (mCurMusicIndex >= musicFiles.size()) {
                    mCurMusicIndex = 0;
                }
                mCurMusicFile = musicFiles.get(mCurMusicIndex);
                break;
            case RANDOM_PLAYBACK:
                int size = musicFiles.size();
                while (true) {
                    int randomNumber = mRandom.nextInt(size);
                    if (!mHasPlayed.contains(randomNumber)) {
                        mHasPlayed.add(randomNumber);
                        mCurMusicIndex = randomNumber;
                        mCurMusicFile = musicFiles.get(mCurMusicIndex);
                        break;
                    } else {
                        if (mHasPlayed.size() == size) {
                            mHasPlayed.clear();
                        }
                    }
                }
                break;
            case LOOP_PLAYBACK:
                break;
            default:
                break;
        }
        mSampleAudioPlayer.setPath(mCurMusicFile.getAbsolutePath());
        if (DEBUG)
            MLog.d(TAG, "next() mCurMusicIndex: " + mCurMusicIndex +
                    " " + mCurMusicFile.getAbsolutePath());
        mSampleAudioPlayer.next();
    }

}
