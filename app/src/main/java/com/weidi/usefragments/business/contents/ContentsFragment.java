package com.weidi.usefragments.business.contents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.recycler_view.VerticalLayoutManager;
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
import com.weidi.utils.MyToast;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.weidi.usefragments.business.video_player.PlayerWrapper.PLAYBACK_ADDRESS;

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
    private VerticalLayoutManager mLayoutManager;
    private ContentsAdapter mAdapter;
    private SharedPreferences mPreferences;
    private int mContentsCount = 0;
    private final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
    public static final int ONE_TIME_ADD_COUNT = 20;

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
        /*LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getContext()) {
                    @Override
                    public void onLayoutCompleted(RecyclerView.State state) {
                        super.onLayoutCompleted(state);
                    }
                };
        mRecyclerView.setLayoutManager(linearLayoutManager);*/

        String path = mPreferences.getString(PLAYBACK_ADDRESS, null);
        if (!TextUtils.isEmpty(path) && PlayerWrapper.mContentsMap.containsKey(path)) {
            mAddressET.setText(PlayerWrapper.mContentsMap.get(path));
        }

        if (!PlayerWrapper.mContentsMap.isEmpty()) {
            initAdapter();
            mRecyclerView.setLayoutManager(mLayoutManager);
            mLayoutManager.setRecyclerView(mRecyclerView);
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addOnScrollListener(mOnScrollListener);
            MLog.d(TAG, "initView() PlayerWrapper.mContentsMap.size(): " +
                    PlayerWrapper.mContentsMap.size());

            if (PlayerWrapper.mContentsMap.size() > 500) {
                // 太多的先加载20个
                mContentsMap.clear();
                for (Map.Entry<String, String> tempMap : PlayerWrapper.mContentsMap.entrySet()) {
                    mContentsCount++;
                    mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                    if (mContentsCount == ONE_TIME_ADD_COUNT) {
                        break;
                    }
                }
                mAdapter.addData(mContentsMap);
            } else {
                mAdapter.setData(PlayerWrapper.mContentsMap);
            }
        } else {
            MyToast.show("没有数据可用!!!");
        }

        // 文件下载完的才显示其文件名
        /*boolean videoIsFinished = mPreferences.getBoolean(
                DownloadFileService.VIDEO_IS_FINISHED, false);
        String videoName = mPreferences.getString(DownloadFileService.VIDEO_NAME, "");
        if (!TextUtils.isEmpty(videoName) && videoIsFinished) {
            mAddressET.setText(videoName);
        }*/
    }

    private void initAdapter() {
        mLayoutManager = new VerticalLayoutManager(getContext());
        mAdapter = new ContentsAdapter(getContext());
        mAdapter.setOnItemClickListener(
                new ContentsAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(String key, int position, int viewId) {
                        MLog.d(TAG, "onItemClick()                  : " + key);

                        String videoPlaybackPath = key;
                        if (TextUtils.isEmpty(videoPlaybackPath)) {
                            Toast.makeText(
                                    getContext(), "播放地址为null", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Contents.setTitle(PlayerWrapper.mContentsMap.get(key));
                        mAddressET.setText(PlayerWrapper.mContentsMap.get(key));

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
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {
        EventBusUtils.unregister(this);
    }

    private RecyclerView.OnScrollListener mOnScrollListener =
            new RecyclerView.OnScrollListener() {

                //用来标记是否正在向最后一个滑动
                boolean isSlidingToLast = false;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    // 当不滚动时
                    switch (newState) {
                        case RecyclerView.SCROLL_STATE_IDLE:
                            int itemCount = mLayoutManager.getItemCount();
                            int visibleItemCount = mLayoutManager.getVisibleItemCount();
                            //int firstVisiblePosition = mLayoutManager.getFirstVisiblePosition();
                            int lastVisiblePosition = mLayoutManager.getLastVisiblePosition();
                            if (isSlidingToLast
                                    && lastVisiblePosition >= itemCount - visibleItemCount
                                    && lastVisiblePosition <= itemCount - 1) {
                                //&& lastVisiblePosition == (mLayoutManager.getItemCount() - 1)) {
                                MLog.d(TAG, "onScrollStateChanged() SCROLL_STATE_IDLE");
                                // 加载更多功能的代码
                                mContentsMap.clear();
                                int i = 0;
                                int addCount = 0;
                                for (Map.Entry<String, String> tempMap :
                                        PlayerWrapper.mContentsMap.entrySet()) {
                                    i++;
                                    if (i <= mContentsCount) {
                                        continue;
                                    }
                                    mContentsCount++;
                                    addCount++;
                                    mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                                    if (addCount == ONE_TIME_ADD_COUNT) {
                                        break;
                                    }
                                }
                                mAdapter.addData(mContentsMap);
                            }
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    // dx用来判断横向滑动方向,dy用来判断纵向滑动方向
                    // MLog.d(TAG, "onScrolled() dx: " + dx + " dy: " + dy);
                    if (dy > 0) {
                        // 表示手指由下往上滑动(内容往上滚动)
                        isSlidingToLast = true;
                    } else {
                        isSlidingToLast = false;
                    }
                }
            };

    @InjectOnClick({R.id.playback_btn, R.id.download_tv,
            R.id.jump_to_gallery_btn, R.id.jump_to_file_manager_btn, R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.playback_btn:
                mClickCount++;
                mUiHandler.removeMessages(MSG_ON_CLICK_PLAYBACK_BUTTOM);
                mUiHandler.sendEmptyMessageDelayed(MSG_ON_CLICK_PLAYBACK_BUTTOM, 500);
                break;
            case R.id.download_tv:
                String videoPlaybackPath = mAddressET.getText().toString();
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

    private void maybeJumpToPosition(String jumpToPosition) {
        MLog.i(TAG, "maybeJumpToPosition() jumpToPosition: " + jumpToPosition);
        int position = -1;
        try {
            position = Integer.parseInt(jumpToPosition);
        } catch (NumberFormatException e) {
            return;
        }
        MLog.i(TAG, "maybeJumpToPosition()       position: " + position);
        if (position < 0) {
            position = 1;
        } else if (position > PlayerWrapper.mContentsMap.size()) {
            position = PlayerWrapper.mContentsMap.size();
        }

        if (position <= mLayoutManager.getItemCount()) {
            // 跳到position的位置就行了
            if (!mLayoutManager.getVisiblePositions().contains(position - 1)) {
                //mRecyclerView.smoothScrollToPosition(position - 1);
                mLayoutManager.smoothScrollToPosition(position - 1);
            }
        } else {
            // 需要加载更多的数据
            int needToLoadCount = position - mLayoutManager.getItemCount();
            needToLoadCount += ONE_TIME_ADD_COUNT;
            mContentsMap.clear();
            int i = 0;
            int addCount = 0;
            for (Map.Entry<String, String> tempMap :
                    PlayerWrapper.mContentsMap.entrySet()) {
                i++;
                if (i <= mContentsCount) {
                    continue;
                }
                mContentsCount++;
                addCount++;
                mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                if (addCount == needToLoadCount) {
                    break;
                }
            }
            mAdapter.addData(mContentsMap);

            int finalPosition = position;
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //mRecyclerView.smoothScrollToPosition(finalPosition - 1);
                    mLayoutManager.smoothScrollToPosition(finalPosition - 1);
                }
            }, 500);
        }
    }

    private final int REQUEST_CODE_SELECT_VIDEO = 112;

    private static final int MSG_ON_PROGRESS_UPDATED = 1;
    private static final int MSG_ON_CLICK_PLAYBACK_BUTTOM = 2;
    private int mClickCount = 0;

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_ON_PROGRESS_UPDATED:
                mAdapter.setProgress(mProgress + "%");
                break;
            case MSG_ON_CLICK_PLAYBACK_BUTTOM:
                if (mClickCount > 2) {
                    mClickCount = 2;
                }

                String videoPlaybackPath = mAddressET.getText().toString().trim();
                if (TextUtils.isEmpty(videoPlaybackPath)) {
                    videoPlaybackPath = mPreferences.getString(PLAYBACK_ADDRESS, null);
                }
                if (TextUtils.isEmpty(videoPlaybackPath)) {
                    mClickCount = 0;
                    return;
                }
                String newPath = videoPlaybackPath.toLowerCase();
                if (!newPath.startsWith("http://")
                        && !newPath.startsWith("https://")
                        && !newPath.startsWith("rtmp://")
                        && !newPath.startsWith("rtsp://")
                        && !newPath.startsWith("/storage/")) {
                    int index = 0;
                    if (PlayerWrapper.mContentsMap.containsValue(videoPlaybackPath)) {
                        for (Map.Entry<String, String> entry :
                                PlayerWrapper.mContentsMap.entrySet()) {
                            index++;
                            if (TextUtils.equals(videoPlaybackPath, entry.getValue())) {
                                videoPlaybackPath = entry.getKey();
                                break;
                            }
                        }
                        MLog.i(TAG, "onClick() index: " + index);

                        switch (mClickCount) {
                            case 1:
                                EventBusUtils.post(
                                        PlayerService.class,
                                        PlayerService.COMMAND_SHOW_WINDOW,
                                        new Object[]{videoPlaybackPath, "video/"});
                                break;
                            case 2:
                                maybeJumpToPosition(String.valueOf(index));
                                break;
                            default:
                                break;
                        }
                    } else {
                        maybeJumpToPosition(videoPlaybackPath);
                    }
                    mClickCount = 0;
                } else {
                    EventBusUtils.post(
                            PlayerService.class,
                            PlayerService.COMMAND_SHOW_WINDOW,
                            new Object[]{videoPlaybackPath, "video/"});
                }
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
