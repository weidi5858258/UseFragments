package com.weidi.usefragments.business.medical_record;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.weidi.dbutil.SimpleDao2;
import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.base.BaseDialogFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.tool.MLog;

import static com.weidi.usefragments.business.medical_record.MedicalRecordAdapter.WHAT_ADD_ITEM;
import static com.weidi.usefragments.business.medical_record.MedicalRecordAdapter
        .WHAT_NOTIFY_DATA_SET_CHANGED;
import static com.weidi.usefragments.business.medical_record.MedicalRecordAdapter.WHAT_UPDATE_ITEM;

/***
 */
public class MedicalRecordDialogFragment extends BaseDialogFragment {

    private static final String TAG =
            MedicalRecordDialogFragment.class.getSimpleName();
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
        return R.layout.fragment_medical_record_dialog;
    }

    ////////////////////////////////////////////////////////////

    private EditText medicalRecordDateET;
    private EditText medicalRecordLeukocyteCountET;
    private EditText medicalRecordNeutrophilsET;
    private EditText medicalRecordHemoglobinET;
    private EditText medicalRecordPlateletCountET;
    private EditText medicalRecordRemarksET;
    private EditText medicalRecordOtherET;
    private Button mOperateBtn;

    private String mOperateName = "增加";
    private MedicalRecordBean mBean;

    public void setName(String name) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        mOperateName = name;
    }

    public void setBean(MedicalRecordBean bean) {
        mBean = bean;
    }

    private void initView(View view, Bundle savedInstanceState) {
        ((TextView) (view.findViewById(R.id.input_layout_date)
                .findViewById(R.id.medical_record_title_tv)))
                .setText("日期");
        medicalRecordDateET = view.findViewById(R.id.input_layout_date)
                .findViewById(R.id.medical_record_value_et);

        ((TextView) (view.findViewById(R.id.input_layout_leukocyte_count)
                .findViewById(R.id.medical_record_title_tv)))
                .setText("白细胞计数");
        medicalRecordLeukocyteCountET = view.findViewById(R.id.input_layout_leukocyte_count)
                .findViewById(R.id.medical_record_value_et);

        ((TextView) (view.findViewById(R.id.input_layout_neutrophils)
                .findViewById(R.id.medical_record_title_tv)))
                .setText("嗜中性粒细胞绝对值");
        medicalRecordNeutrophilsET = view.findViewById(R.id.input_layout_neutrophils)
                .findViewById(R.id.medical_record_value_et);

        ((TextView) (view.findViewById(R.id.input_layout_hemoglobin)
                .findViewById(R.id.medical_record_title_tv)))
                .setText("血红蛋白");
        medicalRecordHemoglobinET = view.findViewById(R.id.input_layout_hemoglobin)
                .findViewById(R.id.medical_record_value_et);

        ((TextView) (view.findViewById(R.id.input_layout_platelet_count)
                .findViewById(R.id.medical_record_title_tv)))
                .setText("血小板计数");
        medicalRecordPlateletCountET = view.findViewById(R.id.input_layout_platelet_count)
                .findViewById(R.id.medical_record_value_et);

        ((TextView) (view.findViewById(R.id.input_layout_remarks)
                .findViewById(R.id.medical_record_title_tv)))
                .setText("备注");
        medicalRecordRemarksET = view.findViewById(R.id.input_layout_remarks)
                .findViewById(R.id.medical_record_value_et);

        ((TextView) (view.findViewById(R.id.input_layout_other)
                .findViewById(R.id.medical_record_title_tv)))
                .setText("其他");
        medicalRecordOtherET = view.findViewById(R.id.input_layout_other)
                .findViewById(R.id.medical_record_value_et);

        mOperateBtn = view.findViewById(R.id.medical_record_operate_btn);
        mOperateBtn.setText(mOperateName);

        if (mBean != null) {
            medicalRecordDateET.setText(mBean.medicalRecordDate);
            medicalRecordLeukocyteCountET.setText(mBean.medicalRecordLeukocyteCount);
            medicalRecordNeutrophilsET.setText(mBean.medicalRecordNeutrophils);
            medicalRecordHemoglobinET.setText(mBean.medicalRecordHemoglobin);
            medicalRecordPlateletCountET.setText(mBean.medicalRecordPlateletCount);
            medicalRecordRemarksET.setText(mBean.medicalRecordRemarks);
            medicalRecordOtherET.setText(mBean.medicalRecordOther);
        }
    }

    @InjectOnClick({
            R.id.medical_record_operate_btn,
            R.id.medical_record_cancel_btn
    })
    private void onClick(View v) {
        if (v.getId() == R.id.medical_record_cancel_btn) {
            dismiss();
            return;
        }

        // 日期
        String medicalRecordDate =
                medicalRecordDateET.getText().toString();
        // 白细胞计数
        String medicalRecordLeukocyteCount =
                medicalRecordLeukocyteCountET.getText().toString();
        // 嗜中性粒细胞绝对值
        String medicalRecordNeutrophils =
                medicalRecordNeutrophilsET.getText().toString();
        // 血红蛋白
        String medicalRecordHemoglobin =
                medicalRecordHemoglobinET.getText().toString();
        // 血小板计数
        String medicalRecordPlateletCount =
                medicalRecordPlateletCountET.getText().toString();
        // 备注
        String medicalRecordRemarks =
                medicalRecordRemarksET.getText().toString();
        // 其他
        String medicalRecordOther =
                medicalRecordOtherET.getText().toString();

        MLog.d(TAG, "onClick()" + "{" +
                "medicalRecordDate='" + medicalRecordDate + '\'' +
                ", medicalRecordLeukocyteCount='" + medicalRecordLeukocyteCount + '\'' +
                ", medicalRecordNeutrophils='" + medicalRecordNeutrophils + '\'' +
                ", medicalRecordHemoglobin='" + medicalRecordHemoglobin + '\'' +
                ", medicalRecordPlateletCount='" + medicalRecordPlateletCount + '\'' +
                ", medicalRecordRemarks='" + medicalRecordRemarks + '\'' +
                ", medicalRecordOther='" + medicalRecordOther + '\'' +
                '}'
        );

        MedicalRecordBean bean = new MedicalRecordBean();
        bean.medicalRecordDate = medicalRecordDate;
        bean.medicalRecordLeukocyteCount = medicalRecordLeukocyteCount;
        bean.medicalRecordNeutrophils = medicalRecordNeutrophils;
        bean.medicalRecordHemoglobin = medicalRecordHemoglobin;
        bean.medicalRecordPlateletCount = medicalRecordPlateletCount;
        bean.medicalRecordRemarks = medicalRecordRemarks;
        bean.medicalRecordOther = medicalRecordOther;

        SimpleDao2.getInstance().setClass(MedicalRecordBean.class);
        long index = -1;
        if (TextUtils.equals("增加", mOperateBtn.getText())) {
            // 加入到数据库
            index = SimpleDao2.getInstance().add(bean);

            EventBusUtils.post(
                    MedicalRecordAdapter.class, WHAT_ADD_ITEM, new Object[]{bean});
        } else {
            if (mBean != null) {
                bean._id = mBean._id;
                index = SimpleDao2.getInstance().update(bean, bean._id);

                EventBusUtils.post(
                        MedicalRecordAdapter.class, WHAT_UPDATE_ITEM, new Object[]{bean});
            }
        }
        MLog.d(TAG, "onClick() index: " + index);

        EventBusUtils.post(
                MedicalRecordAdapter.class, WHAT_NOTIFY_DATA_SET_CHANGED, null);
        dismiss();
    }


}
