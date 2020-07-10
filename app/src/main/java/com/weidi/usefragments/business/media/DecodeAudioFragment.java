package com.weidi.usefragments.business.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.audio_player.JniMusicService;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.tool.Callback;
import com.weidi.usefragments.tool.MLog;

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
        return R.layout.fragment_decode_audio;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    private static final int PLAYBACK_PROGRESS_CHANGED = 800;
    private static final int MSG_PREV = 9;
    private static final int MSG_NEXT = 10;

    // 顺序播放
    private static final int SEQUENTIAL_PLAYBACK = 0x001;
    // 随机播放
    private static final int RANDOM_PLAYBACK = 0x002;
    // 循环播放
    private static final int LOOP_PLAYBACK = 0x003;

    private int mPlayMode = RANDOM_PLAYBACK;

    @InjectView(R.id.scrollView)
    private ScrollView mScrollView;
    @InjectView(R.id.info_tv)
    private TextView mShowInfoTv;
    @InjectView(R.id.play_position_seekbar)
    private SeekBar mPlayPositionSB;
    @InjectView(R.id.process_time_tv)
    private TextView mShowProcessTimeTv;
    @InjectView(R.id.duration_time_tv)
    private TextView mShowDurationTimeTv;
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

    //private SimpleAudioPlayer mSimpleAudioPlayer;
    //private MusicService mSimpleAudioPlayer;
    private JniMusicService mSimpleAudioPlayer;

    private int mProgress;
    private long mPresentationTimeUs;

    private StringBuilder mShowInoSB = new StringBuilder();
    private Handler mUiHandler;

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

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
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                DecodeAudioFragment.this.uiHandleMessage(msg);
            }
        };

        Object object = EventBusUtils.post(
                JniMusicService.class,
                JniMusicService.GET_OBJECT_OF_MUSICSERVICE,
                new Object[]{null});
        //new Object[]{mCallback});
        if (object != null && object instanceof JniMusicService) {
            mSimpleAudioPlayer = (JniMusicService) object;
            //mSimpleAudioPlayer = new SimpleAudioPlayer();
            //mSimpleAudioPlayer.setContext(getContext());
            mSimpleAudioPlayer.setHandler(mUiHandler);
            //mSimpleAudioPlayer.setCallback(mCallback);
            mSimpleAudioPlayer.play();
        }

        //MediaUtils.lookAtMe();
    }

    // 刚进入时,横竖屏切换时,都会调用该方法
    private void initView(View view, Bundle savedInstanceState) {
        if (DEBUG)
            MLog.d(TAG, "initView(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        mShowInoSB.append(mSimpleAudioPlayer.getName());
        mShowInoSB.append("\n");
        setText(mShowInoSB);
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 500);

        String curElapsedTime = DateUtils.formatElapsedTime(
                (mPresentationTimeUs / 1000) / 1000);
        int durationUs = (int) mSimpleAudioPlayer.getDuration();
        String elapsedTime =
                DateUtils.formatElapsedTime(
                        (durationUs / 1000) / 1000);
        mShowProcessTimeTv.setText(curElapsedTime);
        mShowDurationTimeTv.setText(elapsedTime);

        int duration = (int) mSimpleAudioPlayer.getDuration() / 1000;
        int currentPosition = (int) mPresentationTimeUs / 1000;
        float pos = (float) currentPosition / duration;
        int target = Math.round(pos * mPlayPositionSB.getMax());
        mPlayPositionSB.setProgress(target);
        mPlayPositionSB.setSecondaryProgress(0);
        mPlayPositionSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // Tracking start
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            // Tracking
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (fromTouch) {
                    mProgress = progress;
                    mUiHandler.removeMessages(PLAYBACK_PROGRESS_CHANGED);
                    mUiHandler.sendEmptyMessageDelayed(PLAYBACK_PROGRESS_CHANGED, 50);
                }
            }

            // Tracking end
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        //mSimpleAudioPlayer.release();
        //mSimpleAudioPlayer.destroy();
        mSimpleAudioPlayer.setCallback(null);
    }

    @InjectOnClick({R.id.play_btn, R.id.pause_btn, R.id.stop_btn,
            R.id.prev_btn, R.id.next_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_btn:
                mSimpleAudioPlayer.play();
                break;
            case R.id.pause_btn:
                mSimpleAudioPlayer.pause();
                break;
            case R.id.stop_btn:
                mShowProcessTimeTv.setText("00:00");
                mShowDurationTimeTv.setText("00:00");
                mPlayPositionSB.setProgress(0);
                mSimpleAudioPlayer.stop();
                break;
            case R.id.prev_btn:
                mSimpleAudioPlayer.prev();
                break;
            case R.id.next_btn:
                mSimpleAudioPlayer.next();
                break;
            case R.id.jump_btn:
                //FragOperManager.getInstance().enter3(new A2Fragment());
                FragOperManager.getInstance().enter3(new AudioFragment());
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
            case Callback.MSG_ON_TRANSACT_READY:
                mShowProcessTimeTv.setText("00:00");
                mShowDurationTimeTv.setText("00:00");
                mPlayPositionSB.setProgress(0);
                break;
            case Callback.MSG_ON_TRANSACT_PLAYED:
                int duration = (int) mSimpleAudioPlayer.getDuration();
                String elapsedTime = DateUtils.formatElapsedTime(duration);
                // 单位是微秒的时候需要这样处理
                //DateUtils.formatElapsedTime((durationUs / 1000) / 1000);
                mShowDurationTimeTv.setText(elapsedTime);

                mShowInoSB.delete(0, mShowInoSB.length() - 1);
                mShowInoSB.append(mSimpleAudioPlayer.getName());
                mShowInoSB.append("\n");
                setText(mShowInoSB);
                break;
            case Callback.MSG_ON_TRANSACT_PAUSED:
                break;
            case Callback.MSG_ON_TRANSACT_FINISHED:
                mSimpleAudioPlayer.next();
                break;
            case Callback.MSG_ON_TRANSACT_INFO:
                mShowInoSB.append(msg.obj);
                mShowInoSB.append("\n");
                setText(mShowInoSB);
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
                break;
            case Callback.MSG_ON_TRANSACT_ERROR:
                mSimpleAudioPlayer.next();
                break;
            case Callback.MSG_ON_TRANSACT_PROGRESS_UPDATED:
                /*int progress =
                        (int) (mPresentationTimeUs * 3840 / mSimpleAudioPlayer.getDurationUs());*/
                // MLog.d(TAG, "threadHandleMessage() progress: " + progress);
                if (msg.obj == null) {
                    return;
                }
                mPresentationTimeUs = (Long) msg.obj;
                String curElapsedTime = DateUtils.formatElapsedTime(mPresentationTimeUs);
                mShowProcessTimeTv.setText(curElapsedTime);

                /*int duration = (int) mSimpleAudioPlayer.getDurationUs() / 1000;
                int currentPosition = (int) mPresentationTimeUs / 1000;
                float pos = (float) currentPosition / duration;
                int target = Math.round(pos * mPlayPositionSB.getMax());
                int progress = mPlayPositionSB.getProgress();
                if (progress != target) {
                    mPlayPositionSB.setProgress(target);
                }*/

                duration = (int) mSimpleAudioPlayer.getDuration();
                int currentPosition = (int) (mPresentationTimeUs);
                float pos = (float) currentPosition / duration;
                int target = Math.round(pos * mPlayPositionSB.getMax());
                mPlayPositionSB.setProgress(target);
                break;
            case PLAYBACK_PROGRESS_CHANGED:
                long process = (long) ((mProgress / 3840.00) * mSimpleAudioPlayer.getDuration());
                MLog.d(TAG, "player_alexander threadHandleMessage() process: " + process +
                        " " + DateUtils.formatElapsedTime(process));
                mSimpleAudioPlayer.setProgress(process);
                break;
            case MSG_PREV:
            case MSG_NEXT:
                /*mShowInoSB.delete(0, mShowInoSB.length());
                mShowInoSB.append(mSimpleAudioPlayer.getName());
                mShowInoSB.append("\n");
                setText(mShowInoSB);*/
                break;
            default:
                break;
        }
    }

    private void setText(StringBuilder infoSB) {
        if (mShowInfoTv != null) {
            mShowInfoTv.setText(infoSB.toString());
        }
    }

    /*private Callback mCallback = new Callback() {
        @Override
        public void onReady() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mShowProcessTimeTv.setText("00:00");
                    mShowDurationTimeTv.setText("00:00");
                    mPlayPositionSB.setProgress(0);
                }
            });
        }

        @Override
        public void onPaused() {

        }

        @Override
        public void onPlayed() {
            long durationUs = mSimpleAudioPlayer.getDurationUs();
            String elapsedTime =
                    DateUtils.formatElapsedTime(
                            (durationUs / 1000) / 1000);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mShowDurationTimeTv.setText(elapsedTime);
                }
            });
        }

        @Override
        public void onFinished() {
            mSimpleAudioPlayer.next();
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mShowInoSB.delete(0, mShowInoSB.length() - 1);
                    mShowInoSB.append(mSimpleAudioPlayer.getName());
                    mShowInoSB.append("\n");
                    setText(mShowInoSB);
                }
            });
        }

        @Override
        public void onProgressUpdated(long presentationTimeUs) {
            mPresentationTimeUs = presentationTimeUs;
            mUiHandler.removeMessages(PLAYBACK_PROGRESS_UPDATED);
            mUiHandler.sendEmptyMessage(PLAYBACK_PROGRESS_UPDATED);
        }

        @Override
        public void onError() {

        }

        @Override
        public void onInfo(String info) {
            Message msg = mUiHandler.obtainMessage();
            msg.obj = info;
            msg.what = PLAYBACK_INFO;
            mUiHandler.sendMessage(msg);
        }
    };*/

}
