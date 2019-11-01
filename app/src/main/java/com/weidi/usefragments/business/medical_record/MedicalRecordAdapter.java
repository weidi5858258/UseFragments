package com.weidi.usefragments.business.medical_record;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.R;
import com.weidi.usefragments.tool.MLog;

import java.util.ArrayList;
import java.util.List;

/***
 Created by root on 19-10-31.
 */

public class MedicalRecordAdapter extends RecyclerView.Adapter {

    private static final String TAG =
            MedicalRecordAdapter.class.getSimpleName();

    private ArrayList<MedicalRecordBean> mBeans = new ArrayList<MedicalRecordBean>();
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public interface OnItemClickListener {
        void onItemClick(int position, MedicalRecordBean bean);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public MedicalRecordAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        ViewGroup container = null;
        container = (ViewGroup) mLayoutInflater.inflate(
                R.layout.item_medical_record, parent, false);
        return new MedicalRecordViewHolder(container);
    }

    @Override
    public void onBindViewHolder(
            RecyclerView.ViewHolder holder, int position) {
        MedicalRecordViewHolder medicalRecordViewHolder = (MedicalRecordViewHolder) holder;
        MedicalRecordBean bean = mBeans.get(position);
        MLog.d(TAG, "onBindViewHolder() position: " + position + " " + bean.toString());

        medicalRecordViewHolder.medicalRecordDateTV.setText(bean.medicalRecordDate);

        String medicalRecordLeukocyteCount = bean.medicalRecordLeukocyteCount;
        medicalRecordViewHolder.medicalRecordLeukocyteCountTV.setText(medicalRecordLeukocyteCount);
        double medicalRecordLeukocyteCount_ = Double.parseDouble(medicalRecordLeukocyteCount);
        if (medicalRecordLeukocyteCount_ > 9.15) {
            medicalRecordViewHolder.medicalRecordLeukocyteCountTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.red));
        } else if (medicalRecordLeukocyteCount_ < 3.97) {
            medicalRecordViewHolder.medicalRecordLeukocyteCountTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lime));
        }

        String medicalRecordNeutrophils = bean.medicalRecordNeutrophils;
        medicalRecordViewHolder.medicalRecordNeutrophilsTV.setText(medicalRecordNeutrophils);
        double medicalRecordNeutrophils_ = Double.parseDouble(medicalRecordNeutrophils);
        if (medicalRecordNeutrophils_ > 7.0) {
            medicalRecordViewHolder.medicalRecordNeutrophilsTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.red));
        } else if (medicalRecordNeutrophils_ < 2.0) {
            medicalRecordViewHolder.medicalRecordNeutrophilsTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lime));
        }

        String medicalRecordHemoglobin = bean.medicalRecordHemoglobin;
        medicalRecordViewHolder.medicalRecordHemoglobinTV.setText(medicalRecordHemoglobin);
        double medicalRecordHemoglobin_ = Double.parseDouble(medicalRecordHemoglobin);
        if (medicalRecordHemoglobin_ > 172) {
            medicalRecordViewHolder.medicalRecordHemoglobinTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.red));
        } else if (medicalRecordHemoglobin_ < 131) {
            medicalRecordViewHolder.medicalRecordHemoglobinTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lime));
        }

        String medicalRecordPlateletCount = bean.medicalRecordPlateletCount;
        medicalRecordViewHolder.medicalRecordPlateletCountTV.setText(medicalRecordPlateletCount);
        double medicalRecordPlateletCount_ = Double.parseDouble(medicalRecordPlateletCount);
        if (medicalRecordPlateletCount_ > 303) {
            medicalRecordViewHolder.medicalRecordPlateletCountTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.red));
        } else if (medicalRecordPlateletCount_ < 85) {
            medicalRecordViewHolder.medicalRecordPlateletCountTV.setBackgroundColor(
                    mContext.getResources().getColor(R.color.lime));
        }

        medicalRecordViewHolder.medicalRecordRemarksTV.setText(bean.medicalRecordRemarks);
        medicalRecordViewHolder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MLog.d(TAG, "onClick() position: " + position + " " + bean.toString());
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(position, bean);
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return mBeans.size();
    }

    @Override
    final public int getItemViewType(int position) {
        return 0;
    }

    public void setData(List<MedicalRecordBean> list) {
        if (list == null) {
            return;
        }
        mBeans.clear();
        for (MedicalRecordBean bean : list) {
            if (bean == null) {
                continue;
            }

            mBeans.add(bean);
        }
    }

    public void register() {
        EventBusUtils.register(this);
    }

    public void unregister() {
        EventBusUtils.unregister(this);
    }

    public static final int WHAT_ADD_ITEM = 1;
    public static final int WHAT_UPDATE_ITEM = 2;
    public static final int WHAT_NOTIFY_DATA_SET_CHANGED = 3;

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case WHAT_ADD_ITEM:
                if (objArray == null || objArray.length == 0) {
                    return result;
                }
                Object object = objArray[0];
                if (object instanceof MedicalRecordBean) {
                    MedicalRecordBean bean = (MedicalRecordBean) object;
                    mBeans.add(bean);
                }
                break;
            case WHAT_UPDATE_ITEM:
                if (objArray == null || objArray.length == 0) {
                    return result;
                }
                object = objArray[0];
                if (object instanceof MedicalRecordBean) {
                    MedicalRecordBean bean = (MedicalRecordBean) object;
                    MedicalRecordBean tempBean = null;
                    for (MedicalRecordBean bn : mBeans) {
                        if (bean._id == bn._id) {
                            tempBean = bn;
                            break;
                        }
                    }
                    if (tempBean != null) {
                        tempBean.medicalRecordDate = bean.medicalRecordDate;
                        tempBean.medicalRecordLeukocyteCount = bean.medicalRecordLeukocyteCount;
                        tempBean.medicalRecordNeutrophils = bean.medicalRecordNeutrophils;
                        tempBean.medicalRecordHemoglobin = bean.medicalRecordHemoglobin;
                        tempBean.medicalRecordPlateletCount = bean.medicalRecordPlateletCount;
                        tempBean.medicalRecordRemarks = bean.medicalRecordRemarks;
                        tempBean.medicalRecordOther = bean.medicalRecordOther;
                    }
                }
                break;
            case WHAT_NOTIFY_DATA_SET_CHANGED:
                notifyDataSetChanged();
                break;
            default:
        }
        return result;
    }

    private static class MedicalRecordViewHolder extends RecyclerView.ViewHolder {

        private TextView medicalRecordDateTV;
        private TextView medicalRecordLeukocyteCountTV;
        private TextView medicalRecordNeutrophilsTV;
        private TextView medicalRecordHemoglobinTV;
        private TextView medicalRecordPlateletCountTV;
        private TextView medicalRecordRemarksTV;

        public MedicalRecordViewHolder(View itemView) {
            super(itemView);
            medicalRecordDateTV = itemView.findViewById(
                    R.id.medical_record_date);
            medicalRecordLeukocyteCountTV = itemView.findViewById(
                    R.id.medical_record_leukocyte_count);
            medicalRecordNeutrophilsTV = itemView.findViewById(
                    R.id.medical_record_neutrophils);
            medicalRecordHemoglobinTV = itemView.findViewById(
                    R.id.medical_record_hemoglobin);
            medicalRecordPlateletCountTV = itemView.findViewById(
                    R.id.medical_record_platelet_count);
            medicalRecordRemarksTV = itemView.findViewById(
                    R.id.medical_record_remarks);
        }

    }// MedicalRecordViewHolder end

}
