package com.weidi.usefragments.business.test_horizontal_card;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.R;
import com.weidi.usefragments.tool.MLog;

import java.util.ArrayList;
import java.util.List;

/***
 Created by root on 19-10-31.
 */

public class HorizontalCardAdapter extends RecyclerView.Adapter {

    private static final String TAG =
            HorizontalCardAdapter.class.getSimpleName();

    private ArrayList<Integer> mBeans = new ArrayList<Integer>();
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public static final int ON_CLICK = 0x0001;
    public static final int ON_LONG_CLICK = 0x0002;

    public interface OnItemClickListener {
        void onItemClick(int type, int position, int number);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public HorizontalCardAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        ViewGroup container = null;
        container = (ViewGroup) mLayoutInflater.inflate(
                R.layout.item_horizontal_card, parent, false);
        return new HorizontalCardViewHolder(container);
    }

    @Override
    public void onBindViewHolder(
            RecyclerView.ViewHolder holder, int position) {
        HorizontalCardViewHolder horizontalCardViewHolder = (HorizontalCardViewHolder) holder;
        int number = mBeans.get(position);
        MLog.d(TAG, "onBindViewHolder() position: " + position + " " + number);

        if (position % 2 != 0) {
            horizontalCardViewHolder.itemView.setBackgroundColor(
                    mContext.getResources().getColor(R.color.firebrick));
        }

        horizontalCardViewHolder.showNumberTv.setText(String.valueOf(number));
        horizontalCardViewHolder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MLog.d(TAG, "alexander onClick() position: " + position + " " + number);
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(ON_CLICK, position, number);
                        }
                    }
                });
        horizontalCardViewHolder.itemView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        MLog.d(TAG, "alexander onLongClick() position: " + position +
                                " " + number);
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(ON_LONG_CLICK, position, number);
                        }
                        return false;
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

    public void setData(List<Integer> list) {
        if (list == null) {
            return;
        }
        mBeans.clear();
        for (Integer number : list) {
            if (number == null) {
                continue;
            }

            mBeans.add(number);
        }
    }

    public void addItem(int position, int content) {
        if (position < 0
                || position >= getItemCount()
                || mBeans.contains(content)) {
            return;
        }

        mBeans.add(position, content);
        notifyItemChanged(position);
        notifyItemRangeChanged(position, getItemCount());

        // notifyItemChanged(position, content);
        // notifyItemRangeChanged(position, getItemCount(), content);
    }

    public void removeItem(int position) {
        if (position < 0
                || position >= getItemCount()
                || !mBeans.contains(position)) {
            return;
        }

        mBeans.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
    }

    public void register() {
        EventBusUtils.register(this);
    }

    public void unregister() {
        EventBusUtils.unregister(this);
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            default:
        }
        return result;
    }

    private static class HorizontalCardViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout rootLayout;
        private TextView showNumberTv;

        public HorizontalCardViewHolder(View itemView) {
            super(itemView);
            rootLayout = itemView.findViewById(R.id.item_root_layout);
            showNumberTv = itemView.findViewById(R.id.number_tv);
        }

    }// HorizontalCardViewHolder end

}
