package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.BaseActivity;
import com.weidi.usefragments.PlayerActivity;
import com.weidi.usefragments.R;
import com.weidi.usefragments.adapter.ContentsAdapter;
import com.weidi.usefragments.adapter.FragmentTitleAdapter;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.service.DownloadFileService;
import com.weidi.usefragments.tool.Contents;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***

 */
public class ContentsFragment extends BaseFragment {

    private static final String TAG =
            ContentsFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public ContentsFragment() {
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
        return R.layout.fragment_contents;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    @InjectView(R.id.address_et)
    private EditText mAddressET;
    @InjectView(R.id.contents_rv)
    private RecyclerView mRecyclerView;

    private Handler mUiHandler;
    private long contentLength = -1;
    private ContentsAdapter mAdapter;
    private SharedPreferences mPreferences;

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

        contentLength = (Long) EventBusUtils.post(
                DownloadFileService.class,
                DownloadFileService.MSG_GET_CONTENT_LENGTH,
                null);
        EventBusUtils.post(
                DownloadFileService.class,
                DownloadFileService.MSG_SET_CALLBACK,
                new Object[]{mCallback});

        SharedPreferences preferences =
                getContext().getSharedPreferences(
                        DownloadFileService.PREFERENCES_NAME, Context.MODE_PRIVATE);
        String videoName = preferences.getString(DownloadFileService.VIDEO_NAME, "");
        if (!TextUtils.isEmpty(videoName)) {
            if (mAddressET != null) {
                mAddressET.setText(videoName);
            }
        }
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
        mPreferences = getContext().getSharedPreferences(
                DownloadFileService.PREFERENCES_NAME, Context.MODE_PRIVATE);

        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                ContentsFragment.this.uiHandleMessage(msg);
            }
        };
    }

    private void initView(View view, Bundle savedInstanceState) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> map : Contents.movieMap.entrySet()) {
            String name = map.getKey();
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            list.add(name);
        }

        mAdapter = new ContentsAdapter(getContext());
        mAdapter.setData(list);
        mAdapter.setOnItemClickListener(
                new ContentsAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(String name, int position, int viewId) {
                        MLog.d(TAG, "onItemClick(): " + name);

                        String path = Contents.movieMap.get(name);
                        if (TextUtils.isEmpty(path)) {
                            Toast.makeText(
                                    getContext(), "播放地址为null", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Contents.setTitle(name);

                        // 视频文件存在,并且已经下载好的,点击后才播放它,不然还是在线播放
                        boolean videoIsFinished = mPreferences.getBoolean(
                                DownloadFileService.VIDEO_IS_FINISHED, false);
                        String videoPath =
                                mPreferences.getString(DownloadFileService.VIDEO_PATH, "");
                        if (TextUtils.equals(videoPath, path) && videoIsFinished) {
                            File moviesFile = new File(DownloadFileService.PATH);
                            for (File file : moviesFile.listFiles()) {
                                if (file == null) {
                                    continue;
                                }
                                if (file.getName().contains("alexander_mylove.")) {
                                    // 需要指向那个文件,然后打开时才能播放
                                    Contents.setPath(file.getAbsolutePath());
                                    path = file.getAbsolutePath();
                                    break;
                                }
                            }
                        }

                        switch (viewId) {
                            case R.id.item_root_layout:
                                Intent intent = new Intent();
                                intent.setClass(getContext(), PlayerActivity.class);
                                intent.putExtra(PlayerActivity.CONTENT_PATH, path);
                                getAttachedActivity().startActivity(intent);
                                ((BaseActivity) getAttachedActivity()).enterActivity();
                                break;
                            case R.id.item_download_btn:
                                EventBusUtils.post(
                                        DownloadFileService.class,
                                        DownloadFileService.MSG_DOWNLOAD_START,
                                        null);

                                mUiHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        String videoName = mPreferences.getString(
                                                DownloadFileService.VIDEO_NAME, "");
                                        if (!TextUtils.isEmpty(videoName)) {
                                            mAddressET.setText(videoName);
                                        }
                                    }
                                }, 1000);
                                break;
                            default:
                                break;
                        }
                    }
                });
        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getContext()) {
                    @Override
                    public void onLayoutCompleted(RecyclerView.State state) {
                        super.onLayoutCompleted(state);
                    }
                };
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        // 文件下载完的才显示其文件名
        boolean videoIsFinished = mPreferences.getBoolean(
                DownloadFileService.VIDEO_IS_FINISHED, false);
        String videoName = mPreferences.getString(DownloadFileService.VIDEO_NAME, "");
        if (!TextUtils.isEmpty(videoName) && videoIsFinished) {
            mAddressET.setText(videoName);
        }
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {

    }

    @InjectOnClick({R.id.playback_btn, R.id.download_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.playback_btn:
                boolean videoIsFinished = mPreferences.getBoolean(
                        DownloadFileService.VIDEO_IS_FINISHED, false);
                String address = mAddressET.getText().toString();
                if (TextUtils.isEmpty(address) && videoIsFinished) {
                    // 如果要播放下载好的视频,那么把地址栏清空
                    boolean isExist = false;
                    File moviesFile = new File(DownloadFileService.PATH);
                    for (File file : moviesFile.listFiles()) {
                        if (file == null) {
                            continue;
                        }
                        if (file.getName().contains("alexander_mylove.")) {
                            // 需要指向那个文件,然后打开时才能播放
                            Contents.setPath(file.getAbsolutePath());
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        return;
                    }
                } else {
                    Contents.setTitle("");
                    Contents.setPath(address);
                }

                Intent intent = new Intent();
                intent.setClass(getContext(), PlayerActivity.class);
                intent.putExtra(PlayerActivity.CONTENT_PATH, address);
                getAttachedActivity().startActivity(intent);
                ((BaseActivity) getAttachedActivity()).enterActivity();
                break;
            case R.id.download_btn:
                address = mAddressET.getText().toString();
                if (TextUtils.isEmpty(address)) {
                    return;
                }

                Contents.setPath(address);

                EventBusUtils.post(
                        DownloadFileService.class,
                        DownloadFileService.MSG_DOWNLOAD_START,
                        new Object[]{address});
                break;
            case R.id.jump_btn:
                break;
            default:
                break;
        }
    }

    private static final int MSG_ON_PROGRESS_UPDATED = 1;

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_ON_PROGRESS_UPDATED:
                mAdapter.setProgress(mProgress + "%");
                break;
            default:
                break;
        }
    }

    private int mProgress = -1;
    private DownloadCallback mCallback = new DownloadCallback() {
        @Override
        public void onReady() {

        }

        @Override
        public void onPaused() {

        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onFinished() {

        }

        @Override
        public void onProgressUpdated(long readDataSize) {
            if (contentLength == -1) {
                contentLength = readDataSize;
                return;
            }

            int progress = (int) ((readDataSize / (contentLength * 1.00)) * 100);
            if (progress > mProgress || progress == 100) {
                mProgress = progress;
                //MLog.i(TAG, "onProgressUpdated() progress: " + mProgress + "%");
                mUiHandler.removeMessages(MSG_ON_PROGRESS_UPDATED);
                mUiHandler.sendEmptyMessage(MSG_ON_PROGRESS_UPDATED);
            }
        }

        @Override
        public void onError() {

        }

        @Override
        public void onInfo(String info) {

        }
    };

}
