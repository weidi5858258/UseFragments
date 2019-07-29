package com.weidi.usefragments.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.weidi.usefragments.R;

import java.util.ArrayList;
import java.util.List;

/***
 Created by root on 19-4-15.
 */

public class ContentsAdapter extends RecyclerView.Adapter {

    private ArrayList<String> mContents = new ArrayList<String>();
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public ContentsAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        ViewGroup container = null;
        container = (ViewGroup) mLayoutInflater.inflate(
                R.layout.content_layout, parent, false);
        return new TitleViewHolder(container);
    }

    @Override
    public void onBindViewHolder(
            RecyclerView.ViewHolder holder, int position) {
        TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
        String name = mContents.get(position);
        titleViewHolder.title.setText(name);
        titleViewHolder.name = name;
    }

    @Override
    public int getItemCount() {
        return mContents.size();
    }

    @Override
    final public int getItemViewType(int position) {
        return 0;
    }

    public void setData(List<String> list) {
        if (list == null) {
            return;
        }
        mContents.clear();
        for (String name : list) {
            if (TextUtils.isEmpty(name)) {
                continue;
            }

            mContents.add(name);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(String name, int position);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private class TitleViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private String name;

        public TitleViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.content_title);
            itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ContentsAdapter.TitleViewHolder.this.onClick(view);
                        }
                    });
        }

        private void onClick(View view) {
            if (mOnItemClickListener != null) {
                int position = mContents.indexOf(name);
                mOnItemClickListener.onItemClick(name, position);
            }
        }
    }

}
