package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.weidi.usefragments.R;
import com.weidi.usefragments.tool.H264Player;
import com.weidi.usefragments.tool.SampleVideoPlayer;
import com.weidi.usefragments.tool.SampleVideoPlayer2;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.media.MediaUtils;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.SampleVideoPlayer3;
import com.weidi.usefragments.tool.SampleVideoPlayer4;
import com.weidi.usefragments.tool.SampleVideoPlayer5;
import com.weidi.usefragments.tool.SampleVideoPlayer6;
import com.weidi.usefragments.tool.SampleVideoPlayer7;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/***
 视频解码
 */
public class DecodeVideoFragment extends BaseFragment {

    private static final String TAG =
            DecodeVideoFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public DecodeVideoFragment() {
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

    // 顺序播放
    private static final int SEQUENTIAL_PLAYBACK = 0x001;
    // 随机播放
    private static final int RANDOM_PLAYBACK = 0x002;
    // 循环播放
    private static final int LOOP_PLAYBACK = 0x003;

    private int mPlayMode = RANDOM_PLAYBACK;

    @InjectView(R.id.surfaceView)
    private SurfaceView mSurfaceView;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    private static final String PATH = "/storage/37C8-3904/myfiles/video/";
    //    private String mVideoPath = "/storage/2430-1702/BaiduNetdisk/music/谭咏麟 - 水中花.mp3";
    //    private String mVideoPath = "/storage/2430-1702/BaiduNetdisk/video/权力的游戏第四季05.mp4";
    private String mVideoPath = "/storage/2430-1702/BaiduNetdisk/video/流浪的地球.mp4";
    //    private String mVideoPath = "/storage/2430-1702/Download/shape_of_my_heart.mp4";
    /*"/storage/2430-1702/Android/data/com.weidi.usefragments/files/" +
            "output.mp4";*/
    private Surface mSurface;
    private H264Player mH264Player;

    private List<File> videoFiles;
    private int mCurVideoIndex;
    private File mCurVideoFile;
    private int mProgress;
    private long mPresentationTimeUs;

    private Random mRandom = new Random();
    private List<Integer> mHasPlayed = new ArrayList<Integer>();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

        mJumpBtn.setVisibility(View.GONE);
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向
        if (ori == mConfiguration.ORIENTATION_LANDSCAPE) {
            // 横屏
            mJumpBtn.setText("跳\n转\n到");
            // 强制为竖屏
            /*getAttachedActivity().setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);*/
        } else if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
            // 竖屏
            mJumpBtn.setText("跳转到");
            // 强制为横屏
            /*getAttachedActivity().setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);*/
        }
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
        File file = new File(PATH);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                videoFiles = new ArrayList<File>();
                Collections.addAll(videoFiles, files);
            }
        }

        /*Activity activity = getAttachedActivity();
        if (activity != null
                && activity instanceof MainActivity1) {
            MainActivity1 mainActivity1 = (MainActivity1) activity;
            mainActivity1.setSampleVideoPlayer(mSampleVideoPlayer2);
        }*/

        // MediaUtils.lookAtMe();
    }

    private void initView(View view, Bundle savedInstanceState) {
        if (DEBUG)
            MLog.d(TAG, "initView(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        // 无效
        getAttachedActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(
                    SurfaceHolder holder) {
                mSurface = holder.getSurface();

                /*mSampleVideoPlayer.setSurface(mSurface);
                mSampleVideoPlayer.play();*/
                //next();

                /*SampleVideoPlayer mSampleVideoPlayer = new SampleVideoPlayer();
                mSampleVideoPlayer.setContext(getContext());
                mSampleVideoPlayer.setPath(mVideoPath);
                mSampleVideoPlayer.setSurface(mSurface);
                mSampleVideoPlayer.play();*/

                SampleVideoPlayer7 mSampleVideoPlayer7 = new SampleVideoPlayer7();
                mSampleVideoPlayer7.setContext(getContext());
                mSampleVideoPlayer7.setPath(null);
                mSampleVideoPlayer7.setSurface(mSurface);
                mSampleVideoPlayer7.play();

                /*mH264Player = new H264Player();
                mH264Player.setPath(null);
                mH264Player.setSurface(mSurface);
                mH264Player.start();*/
            }

            @Override
            public void surfaceChanged(
                    SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(
                    SurfaceHolder holder) {
            }
        });
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {

    }

    @InjectOnClick({R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new ThrowingScreenFragment());
                break;
        }
    }

    private void next() {
        if (videoFiles == null
                || videoFiles.isEmpty()) {
            return;
        }

        switch (mPlayMode) {
            case SEQUENTIAL_PLAYBACK:
                mCurVideoIndex += 1;
                if (mCurVideoIndex >= videoFiles.size()) {
                    mCurVideoIndex = 0;
                }
                mCurVideoFile = videoFiles.get(mCurVideoIndex);
                break;
            case RANDOM_PLAYBACK:
                int size = videoFiles.size();
                while (true) {
                    int randomNumber = mRandom.nextInt(size);
                    if (!mHasPlayed.contains(randomNumber)) {
                        mHasPlayed.add(randomNumber);
                        mCurVideoIndex = randomNumber;
                        mCurVideoFile = videoFiles.get(mCurVideoIndex);
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
        //mSampleVideoPlayer2.setPath(mCurVideoFile.getAbsolutePath());
        if (DEBUG)
            MLog.d(TAG, "next() mCurVideoIndex: " + mCurVideoIndex +
                    " " + mCurVideoFile.getAbsolutePath());
        //mSampleVideoPlayer2.next();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (DEBUG)
            Log.d(TAG, "onKeyDown() event: " + event);*/
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                next();
                return true;
            default:
                break;
        }

        return false;
    }

    private SampleVideoPlayer2.Callback mCallback = new SampleVideoPlayer2.Callback() {
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
        }

        @Override
        public void onProgressUpdated(long presentationTimeUs) {

        }

        @Override
        public void onPlaybackError() {

        }

        @Override
        public void onPlaybackInfo(String info) {

        }
    };

}
