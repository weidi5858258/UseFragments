package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.test_view.AndroidTreeView;
import com.weidi.usefragments.test_view.TreeItemHolder;
import com.weidi.usefragments.test_view.TreeNode;
import com.weidi.usefragments.tool.MLog;

/***
 框架模板类
 */
public class FolderStructureFragment extends BaseFragment {

    private static final String TAG =
            FolderStructureFragment.class.getSimpleName();

    private static final boolean DEBUG = true;

    public FolderStructureFragment() {
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

        initData();
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

        initView(view, savedInstanceState);
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

        onHide();
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


    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach() " + printThis());

        destroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            MLog.d(TAG, "onSaveInstanceState() " + printThis());
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
        return R.layout.fragment_folder;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    private AndroidTreeView tView;

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

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

    }

    private void initView(View view, Bundle savedInstanceState) {
        TreeNode root = TreeNode.root();

        TreeNode computerRoot = new TreeNode(
                new TreeItemHolder.IconTreeItem(R.string.ic_laptop, "My Computer"));

        TreeNode myDocuments = new TreeNode(
                new TreeItemHolder.IconTreeItem(R.string.ic_folder, "My Documents"));
        TreeNode downloads = new TreeNode(
                new TreeItemHolder.IconTreeItem(R.string.ic_folder, "Downloads"));
        TreeNode file1 = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_drive_file, "Folder 1"));
        TreeNode file2 = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_drive_file, "Folder 2"));
        TreeNode file3 = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_drive_file, "Folder 3"));
        TreeNode file4 = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_drive_file, "Folder 4"));
        fillDownloadsFolder(downloads);
        downloads.addChildren(file1, file2, file3, file4);
        myDocuments.addChild(downloads);

        TreeNode myMedia = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_photo_library, "Photos"));
        TreeNode photo1 = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_photo, "Folder 1"));
        TreeNode photo2 = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_photo, "Folder 2"));
        TreeNode photo3 = new TreeNode(new TreeItemHolder.IconTreeItem(R.string.ic_photo, "Folder 3"));
        myMedia.addChildren(photo1, photo2, photo3);

        computerRoot.addChildren(myDocuments, myMedia);

        // 给看不到的根节点添加一个能够看得到的"根节点"
        root.addChildren(computerRoot);

        tView = new AndroidTreeView(getActivity(), root);
        tView.setDefaultAnimation(true);
        tView.setDefaultContainerStyle(R.style.TreeNodeStyleCustom);
        tView.setDefaultViewHolder(TreeItemHolder.class);

        tView.setDefaultNodeClickListener(nodeClickListener);
        tView.setDefaultNodeLongClickListener(nodeLongClickListener);

        ViewGroup containerView = (ViewGroup) view.findViewById(R.id.folder_container);
        containerView.addView(tView.getView());
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {

    }

    @InjectOnClick({R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new DecodePlayFragment());
                break;
        }
    }

    private int counter = 0;
    private void fillDownloadsFolder(TreeNode node) {
        TreeNode downloads = new TreeNode(
                new TreeItemHolder.IconTreeItem(R.string.ic_folder, "Downloads" + (counter++)));
        node.addChild(downloads);
        if (counter < 5) {
            fillDownloadsFolder(downloads);
        }
    }

    private TreeNode.TreeNodeClickListener nodeClickListener =
            new TreeNode.TreeNodeClickListener() {
                @Override
                public void onClick(TreeNode node, Object value) {
                    TreeItemHolder.IconTreeItem item = (TreeItemHolder.IconTreeItem) value;
                }
            };

    private TreeNode.TreeNodeLongClickListener nodeLongClickListener =
            new TreeNode.TreeNodeLongClickListener() {
                @Override
                public boolean onLongClick(TreeNode node, Object value) {
                    TreeItemHolder.IconTreeItem item = (TreeItemHolder.IconTreeItem) value;
                    Toast.makeText(getActivity(), "Long click: " + item.text, Toast.LENGTH_SHORT).show();
                    return true;
                }
            };

    /***
     02-22 13:27:04.506 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles

     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/audio
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/music
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/picture
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/ROOT
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/TvCamera
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/video

     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app

     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/res
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/weidi_library
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/audio
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/music
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/picture
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/ROOT
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/ROOT/download
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/ROOT/picture
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/TvCamera
     02-22 13:27:04.507 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/TvCamera/res
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/video
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/res
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/res/drawable
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/res/drawable-hdpi
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/res/drawable-xxhdpi
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/res/raw
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/weidi_library
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/weidi_library/res
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/ROOT/download
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/ROOT/download/tv.danmaku.bili
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/ROOT/picture
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/TvCamera/res
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/TvCamera/res/raw
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build
     02-22 13:27:04.508 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/res/drawable
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/res/drawable-hdpi
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/res/drawable-xxhdpi
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/res/raw
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/weidi_library/res
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/Artifact/weidi_library/res/raw
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/ROOT/download/tv.danmaku.bili
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/TvCamera/res/raw
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/assets
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main/assets
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main/res
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/Artifact/weidi_library/res/raw
     02-22 13:27:04.509 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/assets
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/assets/debug
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res/merged
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main/assets
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main/res
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main/res/raw
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/assets/debug
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res/merged
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res/merged/debug
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/src/main/res/raw
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res/merged/debug
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander    subFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res/merged/debug/raw
     02-22 13:27:04.510 D/SmartMediaApp:HeaderMenuController: alexander parentFolderPath: /storage/37C8-3904/myfiles/APITestApp/app/build/intermediates/res/merged/debug/raw
     */

}
