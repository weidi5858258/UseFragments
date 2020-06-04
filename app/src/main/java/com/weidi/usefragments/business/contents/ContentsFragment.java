package com.weidi.usefragments.business.contents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.recycler_view.VerticalLayoutManager;
import com.weidi.threadpool.ThreadPool;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.video_player.JniPlayerActivity;
import com.weidi.usefragments.business.video_player.PlayerService;
import com.weidi.usefragments.business.video_player.PlayerWrapper;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.service.DownloadFileService;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.MLog;
import com.weidi.utils.MD5Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                    " data: " + (data != null ? data.toString() : null));
        if (requestCode == REQUEST_CODE_SELECT_VIDEO
                && resultCode == Activity.RESULT_OK
                && null != data) {
            Uri uri = data.getData();
            MLog.i(TAG, "onActivityResult() path: " + uri.getPath());
            String[] filePathColumn = {MediaStore.Video.Media.DATA};
            Cursor cursor = getContext().getContentResolver().query(
                    uri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String videoPlaybackPath = cursor.getString(columnIndex);
            cursor.close();
            if (TextUtils.isEmpty(videoPlaybackPath)) {
                // /document/1532-48AD:Videos/传染病.mp4
                String path = uri.getPath();
                if (path.contains(":")) {
                    String[] paths = path.split(":");
                    if (paths.length == 2) {
                        videoPlaybackPath = "/storage" +
                                paths[0].substring(paths[0].lastIndexOf("/")) + "/" + paths[1];
                    }
                }
            }
            MLog.i(TAG, "onActivityResult() videoPlaybackPath: " + videoPlaybackPath);
            if (TextUtils.isEmpty(videoPlaybackPath)) {
                return;
            }

            //FFMPEG.getDefault().setMode(FFMPEG.USE_MODE_MEDIA);
            EventBusUtils.post(
                    PlayerService.class,
                    PlayerService.COMMAND_SHOW_WINDOW,
                    new Object[]{videoPlaybackPath, data.getType()});

            /*Intent intent = new Intent();
            intent.putExtra(PlayerActivity.CONTENT_PATH, videoPlaybackPath);
            //intent.setClass(getContext(), PlayerActivity.class);
            intent.setClass(getContext(), JniPlayerActivity.class);
            getAttachedActivity().startActivity(intent);
            ((BaseActivity) getAttachedActivity()).enterActivity();*/
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
    // 第一个存储视频地址,第二个存储标题
    //private LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

        Object object = EventBusUtils.post(
                DownloadFileService.class,
                DownloadFileService.MSG_GET_CONTENT_LENGTH,
                null);
        if (object != null) {
            contentLength = (Long) object;
        }
        EventBusUtils.post(
                DownloadFileService.class,
                DownloadFileService.MSG_SET_CALLBACK,
                new Object[]{mCallback});

        /*SharedPreferences preferences =
                getContext().getSharedPreferences(
                        DownloadFileService.PREFERENCES_NAME, Context.MODE_PRIVATE);
        String videoName = preferences.getString(DownloadFileService.VIDEO_NAME, "");
        if (!TextUtils.isEmpty(videoName)) {
            if (mAddressET != null) {
                mAddressET.setText(videoName);
            }
        }*/
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
        EventBusUtils.register(this);
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
        ThreadPool.getFixedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayerWrapper.mContentsMap.clear();
                File[] files = getContext().getExternalFilesDirs(Environment.MEDIA_SHARED);
                File file = null;
                for (File f : files) {
                    MLog.i(TAG, "Environment.MEDIA_SHARED    : " + f.getAbsolutePath());
                    file = f;
                }
                if (file != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(file.getAbsolutePath());
                    sb.append("/");
                    sb.append("contents.txt");
                    file = new File(sb.toString());
                    if (file.exists()) {
                        readContents(file, PlayerWrapper.mContentsMap);
                        mUiHandler.removeMessages(MSG_ON_INIT_ADAPTER);
                        mUiHandler.sendEmptyMessage(MSG_ON_INIT_ADAPTER);
                        return;
                    }
                }

                for (Map.Entry<String, String> tempMap : Contents.movieMap.entrySet()) {
                    if (!PlayerWrapper.mContentsMap.containsKey(tempMap.getValue())) {
                        PlayerWrapper.mContentsMap.put(tempMap.getValue(), tempMap.getKey());
                    }
                }
                mUiHandler.removeMessages(MSG_ON_INIT_ADAPTER);
                mUiHandler.sendEmptyMessage(MSG_ON_INIT_ADAPTER);
            }
        });

        // 文件下载完的才显示其文件名
        /*boolean videoIsFinished = mPreferences.getBoolean(
                DownloadFileService.VIDEO_IS_FINISHED, false);
        String videoName = mPreferences.getString(DownloadFileService.VIDEO_NAME, "");
        if (!TextUtils.isEmpty(videoName) && videoIsFinished) {
            mAddressET.setText(videoName);
        }*/
    }

    private void readContents(File file, LinkedHashMap<String, String> map) {
        final String TAG = "@@@@@@@@@@";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String aLineContent = null;
            //一次读一行，读入null时文件结束
            while ((aLineContent = reader.readLine()) != null) {
                if (aLineContent == null || aLineContent.length() == 0) {
                    continue;
                }

                if (aLineContent.contains(TAG) && !aLineContent.startsWith("#")) {
                    String[] contents = aLineContent.split(TAG);
                    if (contents.length > 1) {
                        if (!map.containsKey(contents[0])) {
                            map.put(contents[0], contents[1]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    private void initAdapter(Map<String, String> map) {
        mAdapter = new ContentsAdapter(getContext());
        mAdapter.setData(map);
        mAdapter.setOnItemClickListener(
                new ContentsAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(String key, int position, int viewId) {
                        MLog.d(TAG, "onItemClick(): " + key);

                        String videoPlaybackPath = key;
                        if (TextUtils.isEmpty(videoPlaybackPath)) {
                            Toast.makeText(
                                    getContext(), "播放地址为null", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Contents.setTitle(PlayerWrapper.mContentsMap.get(key));

                        Uri uri = Uri.parse(videoPlaybackPath);
                        String lastPath = uri.getLastPathSegment();
                        String suffixName = null;
                        if (lastPath != null) {
                            int index = lastPath.lastIndexOf(".");
                            if (index != -1) {
                                suffixName = lastPath.substring(index, lastPath.length());
                            }
                        }
                        String fileName = Contents.getTitle() + suffixName;

                        String httpPathMD5 = MD5Util.getMD5String(videoPlaybackPath);
                        // 视频文件存在,并且已经下载好的,点击后才播放它,不然还是在线播放
                        boolean videoIsFinished = mPreferences.getBoolean(httpPathMD5, false);
                        if (videoIsFinished) {
                            File moviesFile = new File(DownloadFileService.PATH);
                            for (File file : moviesFile.listFiles()) {
                                if (file == null) {
                                    continue;
                                }
                                if (TextUtils.equals(file.getName(), fileName)) {
                                    // 需要指向那个文件,然后打开时才能播放
                                    videoPlaybackPath = file.getAbsolutePath();
                                    Contents.setPath(videoPlaybackPath);
                                    break;
                                }
                            }
                        }
                        MLog.d(TAG, "onItemClick() videoPlaybackPath: " + videoPlaybackPath);

                        switch (viewId) {
                            case R.id.item_root_layout:
                                //FFMPEG.getDefault().setMode(FFMPEG.USE_MODE_MEDIA);

                                // Test
                                /*long position_ = mPreferences.getLong(PLAYBACK_POSITION, 0);
                                position_ = position_ - 600;
                                String curElapsedTime = DateUtils.formatElapsedTime(position_);
                                MLog.d("player_alexander", "onItemClick() position: " +
                                        position_ + " curElapsedTime: " + curElapsedTime);
                                FFMPEG.getDefault().seekTo(position_);*/

                                EventBusUtils.post(
                                        PlayerService.class,
                                        PlayerService.COMMAND_SHOW_WINDOW,
                                        new Object[]{videoPlaybackPath, "video/"});

                                /*Intent intent = new Intent();
                                intent.putExtra(PlayerActivity.CONTENT_PATH, videoPlaybackPath);
                                //intent.setClass(getContext(), PlayerActivity.class);
                                intent.setClass(getContext(), JniPlayerActivity.class);
                                getAttachedActivity().startActivity(intent);
                                ((BaseActivity) getAttachedActivity()).enterActivity();*/
                                break;
                            case R.id.item_download_btn:
                                EventBusUtils.post(
                                        DownloadFileService.class,
                                        DownloadFileService.MSG_DOWNLOAD_START,
                                        null);

                                /*mUiHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        String videoName = mPreferences.getString(
                                                DownloadFileService.VIDEO_NAME, "");
                                        if (!TextUtils.isEmpty(videoName)) {
                                            mAddressET.setText(videoName);
                                        }
                                    }
                                }, 1000);*/
                                break;
                            default:
                                break;
                        }
                    }
                });
        /*LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getContext()) {
                    @Override
                    public void onLayoutCompleted(RecyclerView.State state) {
                        super.onLayoutCompleted(state);
                    }
                };
        mRecyclerView.setLayoutManager(linearLayoutManager);*/
        mRecyclerView.setLayoutManager(new VerticalLayoutManager());
        mRecyclerView.setAdapter(mAdapter);
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        EventBusUtils.unregister(this);
    }

    @InjectOnClick({R.id.playback_btn, R.id.download_tv,
            R.id.jump_to_gallery_btn, R.id.jump_to_file_manager_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.playback_btn:
                boolean videoIsFinished = false;/*mPreferences.getBoolean(
                        DownloadFileService.VIDEO_IS_FINISHED, false);*/
                String videoPlaybackPath = mAddressET.getText().toString();
                if (TextUtils.isEmpty(videoPlaybackPath) && videoIsFinished) {
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
                    Contents.setPath(videoPlaybackPath);
                }

                //FFMPEG.getDefault().setMode(FFMPEG.USE_MODE_MEDIA);
                EventBusUtils.post(
                        PlayerService.class,
                        PlayerService.COMMAND_SHOW_WINDOW,
                        new Object[]{videoPlaybackPath, "video/"});

                /*Intent intent = new Intent();
                intent.setClass(getContext(), PlayerActivity.class);
                intent.putExtra(PlayerActivity.CONTENT_PATH, videoPlaybackPath);
                getAttachedActivity().startActivity(intent);
                ((BaseActivity) getAttachedActivity()).enterActivity();*/
                break;
            case R.id.download_tv:
                videoPlaybackPath = mAddressET.getText().toString();
                if (TextUtils.isEmpty(videoPlaybackPath)) {
                    return;
                }

                MLog.i(TAG, "onClick() address: " + videoPlaybackPath);
                Contents.setPath(videoPlaybackPath);

                EventBusUtils.post(
                        DownloadFileService.class,
                        DownloadFileService.MSG_DOWNLOAD_START,
                        new Object[]{videoPlaybackPath});
                break;
            case R.id.jump_to_gallery_btn:
                // 调用"图库"
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
                break;
            case R.id.jump_to_file_manager_btn:
                // 调用"文件管理"
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                // 设置类型(任意后缀)
                intent.setType("*/*");
                //intent.setType("video/*");
                //intent.setType("video/*;image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
                break;
            case R.id.jump_btn:
                // 视频播放后才可以打开这个Activity
                intent = new Intent();
                intent.putExtra(JniPlayerActivity.COMMAND_NO_FINISH, true);
                intent.setClass(getContext(), JniPlayerActivity.class);
                getAttachedActivity().startActivity(intent);
                break;
            default:
                break;
        }
    }

    private final int REQUEST_CODE_SELECT_VIDEO = 112;

    private static final int MSG_ON_PROGRESS_UPDATED = 1;
    private static final int MSG_ON_INIT_ADAPTER = 2;

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_ON_PROGRESS_UPDATED:
                mAdapter.setProgress(mProgress + "%");
                break;
            case MSG_ON_INIT_ADAPTER:
                initAdapter(PlayerWrapper.mContentsMap);
                break;
            default:
                break;
        }
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            default:
                break;
        }
        return result;
    }

    private int mProgress = -1;
    private DownloadCallback mCallback = new DownloadCallback() {
        @Override
        public void onReady() {
            contentLength = -1;
            mProgress = 0;
            mUiHandler.removeMessages(MSG_ON_PROGRESS_UPDATED);
            mUiHandler.sendEmptyMessage(MSG_ON_PROGRESS_UPDATED);
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
                MLog.i(TAG, "onProgressUpdated() progress: " + mProgress + "%");
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
