package com.weidi.usefragments.test_fragment.dialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.weidi.usefragments.R;
import com.weidi.usefragments.adapter.FragmentTitleAdapter;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseDialogFragment;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.tool.MLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***
 */
public class ShowTitleDialogFragment extends BaseDialogFragment {

    private static final String TAG =
            ShowTitleDialogFragment.class.getSimpleName();
    private static final boolean DEBUG = true;


    /*********************************
     * Created
     *********************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG)
            MLog.d(TAG, "onAttach() " + printThis() +
                    " mContext: " + context);
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
    }

    /**
     * onCreateView()方法和onCreateDialog()方法两选一,其他都一样
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
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

        // 在子类中给某些View设置监听事件
        // View的内容显示在onShow()方法中进行
        mRecyclerView = view.findViewById(R.id.title_rv);
        mCancelBtn = view.findViewById(R.id.cancel_btn);
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
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
        if (DEBUG)
            MLog.d(TAG, "onStart() " + printThis());
    }

    /*********************************
     * Resumed
     *********************************/

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            MLog.d(TAG, "onResume() " + printThis());

        List<Fragment> fragments = new ArrayList<>();
        Fragment fragment = FragOperManager.getInstance().getCurUsedFragment();
        if (fragment != null) {
            fragments.add(fragment);
        }
        Map<Fragment, List<Fragment>> map = FragOperManager.getInstance().getMainFragmentsMap();
        List<Fragment> list = map.get(fragment);
        if (list != null) {
            for (Fragment fragment1 : list) {
                if (fragment1 == null) {
                    continue;
                }
                fragments.add(fragment1);
            }
        }

        FragmentTitleAdapter adapter = new FragmentTitleAdapter(getContext());
        adapter.setData(fragments);
        adapter.setOnItemClickListener(
                new FragmentTitleAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(Fragment fragment, int position) {
                        if (fragment == null) {
                            return;
                        }
                        for (Fragment fragment1 : fragments) {
                            if (!fragment1.isHidden()) {
                                FragOperManager
                                        .getInstance()
                                        .changeFragment(
                                                (BaseFragment) fragment1,
                                                (BaseFragment) fragment);
                                dismiss();
                                return;
                            }
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
        mRecyclerView.setAdapter(adapter);
    }

    /*********************************
     * Paused
     *********************************/

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            MLog.d(TAG, "onPause() " + printThis());
    }

    /*********************************
     * Stopped
     *********************************/

    @Override
    public void onStop() {
        super.onStop();
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach() " + printThis());
    }

    protected int provideStyle() {
        return DialogFragment.STYLE_NO_TITLE;
    }

    protected int provideLayout() {
        return R.layout.fragment_test_dialog_recycleview;
    }

    private RecyclerView mRecyclerView;
    private Button mCancelBtn;

}
