package com.weidi.usefragments.business.medical_record;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.media.PlayerView;
import com.weidi.usefragments.test_fragment.scene2.A2Fragment;
import com.weidi.usefragments.tool.MLog;

import java.util.ArrayList;
import java.util.List;

/***

 */
public class MedicalRecordFragment extends BaseFragment {

    private static final String TAG =
            MedicalRecordFragment.class.getSimpleName();

    private static final boolean DEBUG = true;

    public MedicalRecordFragment() {
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
        return R.layout.fragment_medical_record;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    private MedicalRecordAdapter mAdapter;
    @InjectView(R.id.medical_record_rv)
    private RecyclerView mRecyclerView;

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
        List<MedicalRecordBean> list = new ArrayList<MedicalRecordBean>();
        MedicalRecordBean bean = new MedicalRecordBean();
        bean.medicalRecordDate = "2019/09/16";
        bean.medicalRecordLeukocyteCount = "588.7";
        bean.medicalRecordNeutrophils = "1.90";
        bean.medicalRecordHemoglobin = "188";
        bean.medicalRecordPlateletCount = "105";
        bean.medicalRecordRemarks = "4";
        list.add(bean);

        bean = new MedicalRecordBean();
        bean.medicalRecordDate = "2019/10/05";
        bean.medicalRecordLeukocyteCount = "5.34";
        bean.medicalRecordNeutrophils = "1.90";
        bean.medicalRecordHemoglobin = "94";
        bean.medicalRecordPlateletCount = "321";
        bean.medicalRecordRemarks = "4";
        list.add(bean);

        bean = new MedicalRecordBean();
        bean.medicalRecordDate = "2019/10/12";
        bean.medicalRecordLeukocyteCount = "5.34";
        bean.medicalRecordNeutrophils = "1.90";
        bean.medicalRecordHemoglobin = "94";
        bean.medicalRecordPlateletCount = "321";
        bean.medicalRecordRemarks = "4";
        list.add(bean);

        bean = new MedicalRecordBean();
        bean.medicalRecordDate = "2019/10/19";
        bean.medicalRecordLeukocyteCount = "3.57";
        bean.medicalRecordNeutrophils = "1.55";
        bean.medicalRecordHemoglobin = "105";
        bean.medicalRecordPlateletCount = "180";
        bean.medicalRecordRemarks = "4";
        list.add(bean);

        bean = new MedicalRecordBean();
        bean.medicalRecordDate = "2019/10/25";
        bean.medicalRecordLeukocyteCount = "4.89";
        bean.medicalRecordNeutrophils = "3.0";
        bean.medicalRecordHemoglobin = "108";
        bean.medicalRecordPlateletCount = "105";
        bean.medicalRecordRemarks = "4";
        list.add(bean);

        bean = new MedicalRecordBean();
        bean.medicalRecordDate = "2019/11/02";
        bean.medicalRecordLeukocyteCount = "58.7";
        bean.medicalRecordNeutrophils = "1.90";
        bean.medicalRecordHemoglobin = "188";
        bean.medicalRecordPlateletCount = "105";
        bean.medicalRecordRemarks = "4";
        list.add(bean);

        mAdapter = new MedicalRecordAdapter(getContext());
        mAdapter.setData(list);
    }

    private void initView(View view, Bundle savedInstanceState) {
        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getContext()) {
                    @Override
                    public void onLayoutCompleted(RecyclerView.State state) {
                        super.onLayoutCompleted(state);
                        if (DEBUG)
                            MLog.d(TAG, "onLayoutCompleted()");
                    }
                };
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {

    }

    /*@InjectOnClick({R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new A2Fragment());
                break;
        }
    }*/

}
