package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.weidi.usefragments.R;
import com.weidi.usefragments.business.media.Camera2Fragment;
import com.weidi.usefragments.business.media.RecordScreenFragment;
import com.weidi.usefragments.business.test_horizontal_card.HorizontalCardFragment;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.tool.MLog;


/***
 *
 */
public class Main4Fragment extends BaseFragment {

    private static final String TAG =
            Main4Fragment.class.getSimpleName();

    private static final boolean DEBUG = true;
    @InjectView(R.id.title_tv)
    private TextView mTitleView;
    @InjectView(R.id.jump_btn)
    private Button mJumpBtn;

    public Main4Fragment() {
        super();
    }

    /*********************************
     * Created
     *********************************/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (DEBUG)
            MLog.d(TAG, "onAttach() activity: " + activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + this
                    + " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG)
            MLog.d(TAG, "onCreateView(): " + this
                    + " savedInstanceState: " + savedInstanceState);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewCreated(): " + this
                    + " savedInstanceState: " + savedInstanceState);
        mJumpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 FragOperManager.getInstance().enter3(new RecordScreenFragment());
//                 FragOperManager.getInstance().enter3(new Camera2Fragment());
//                FragOperManager.getInstance().enter3(new HorizontalCardFragment());
            }
        });
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewStateRestored(): " + this
                    + " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onActivityCreated(): " + this
                    + " savedInstanceState: " + savedInstanceState);
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
            MLog.d(TAG, "onStart(): " + this);
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
            MLog.d(TAG, "onResume(): " + this);
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
            MLog.d(TAG, "onPause(): " + this);
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
            MLog.d(TAG, "onStop(): " + this);
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView(): " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach(): " + this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            MLog.d(TAG, "onSaveInstanceState(): " + printThis());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onConfigurationChanged(): " + printThis());
    }

    /**
     * Very important
     * true表示被隐藏了,false表示被显示了
     * Fragment:
     * 被show()或者hide()时才会回调这个方法,
     * 被add()或者popBackStack()时不会回调这个方法
     * 弹窗时不会被回调(是由当前的Fragment弹出的一个DialogFragment)
     * 如果是弹出一个DialogActivity窗口,则应该会被回调,
     * 因为当前Fragment所在的Activity的生命周期发生了变化,
     * 则当前Fragment的生命周期也会发生变化.
     *
     * @param hidden if true that mean hidden
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (DEBUG)
            MLog.d(TAG, "onHiddenChanged(): " + this + " hidden: " + hidden);
        if (hidden) {
            onHide();
        } else {
            onShow();
        }
    }

    // 写这个方法只是为了不直接调用onResume()方法
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + this);
        mTitleView.setText(Main4Fragment.class.getSimpleName());
        mJumpBtn.setText("跳转到");
        setStatusBar(getAttachedActivity(), true);
    }

    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide(): " + this);
    }

    @Override
    protected int provideLayout() {
        return R.layout.fragment_main4;
    }

    @Override
    public boolean onBackPressed() {
        return true;
    }
}