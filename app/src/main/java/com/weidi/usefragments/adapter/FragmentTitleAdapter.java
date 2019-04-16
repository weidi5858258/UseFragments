package com.weidi.usefragments.adapter;

import android.app.Fragment;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.weidi.usefragments.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 19-4-15.
 */

public class FragmentTitleAdapter extends RecyclerView.Adapter {

    private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public FragmentTitleAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        ViewGroup container = null;
        container = (ViewGroup) mLayoutInflater.inflate(
                R.layout.device_title_layout, parent, false);
        return new TitleViewHolder(container);
    }

    @Override
    public void onBindViewHolder(
            RecyclerView.ViewHolder holder, int position) {
        Fragment fragment = mFragments.get(position);
        String title = fragment.getClass().getSimpleName();
        TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
        titleViewHolder.mTitle.setText(title);
        titleViewHolder.mFragment = fragment;
    }

    @Override
    public int getItemCount() {
        return mFragments.size();
    }

    @Override
    final public int getItemViewType(int position) {
        return 0;
    }

    public void setData(List<Fragment> list) {
        if (list == null) {
            return;
        }
        mFragments.clear();
        for (Fragment fragment : list) {
            if (fragment == null) {
                continue;
            }

            mFragments.add(fragment);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Fragment fragment, int position);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private class TitleViewHolder extends RecyclerView.ViewHolder {

        private ImageView mIcon;
        private TextView mTitle;
        private Fragment mFragment;

        public TitleViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.fragment_icon);
            mTitle = itemView.findViewById(R.id.fragment_title);
            itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TitleViewHolder.this.onClick(view);
                        }
                    });
        }

        private void onClick(View view) {
            if (mOnItemClickListener != null) {
                int position = mFragments.indexOf(mFragment);
                mOnItemClickListener.onItemClick(mFragment, position);
            }
        }
    }

}
