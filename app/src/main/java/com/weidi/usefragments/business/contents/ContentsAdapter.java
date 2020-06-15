package com.weidi.usefragments.business.contents;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.weidi.usefragments.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/***
 Created by root on 19-4-15.


 // test
 final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
 int i = 0;
 for (Map.Entry<String, String> tempMap : PlayerWrapper.mContentsMap.entrySet()) {
 i++;
 if (i < 11) {
 continue;
 }
 testCounts++;
 mContentsMap.put(tempMap.getKey(), tempMap.getValue());
 if (testCounts == 20) {
 break;
 }
 }
 mAdapter.addData(mContentsMap);


 */

public class ContentsAdapter extends RecyclerView.Adapter {

    private final LinkedHashMap<String, String> mContentsMap = new LinkedHashMap();
    private final ArrayList<String> mKeys = new ArrayList<String>();
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    //private String mPath;

    public ContentsAdapter(Context context) {
        mContentsMap.clear();
        mKeys.clear();
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
        if (mContentsMap.isEmpty() || mKeys.isEmpty()) {
            return;
        }

        TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
        String key = mKeys.get(position);
        String value = mContentsMap.get(key);
        titleViewHolder.title.setText(value);
        titleViewHolder.key = key;
    }

    @Override
    public int getItemCount() {
        return mContentsMap.size();
    }

    @Override
    final public int getItemViewType(int position) {
        return 0;
    }

    // map的key已经是唯一了
    public void setData(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        synchronized (ContentsAdapter.this) {
            mKeys.clear();
            mContentsMap.clear();
            for (Map.Entry<String, String> tempMap : map.entrySet()) {
                mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                mKeys.add(tempMap.getKey());
            }

            notifyDataSetChanged();
        }
    }

    // 外面的数据做好key的唯一性,且key,value都不为null
    public void addData(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        synchronized (ContentsAdapter.this) {
            for (Map.Entry<String, String> tempMap : map.entrySet()) {
                mContentsMap.put(tempMap.getKey(), tempMap.getValue());
                mKeys.add(tempMap.getKey());
            }

            notifyDataSetChanged();

            //  java.lang.IllegalArgumentException:
            //  Tmp detached view should be removed from RecyclerView before it can be recycled
            // 此异常的解决方案就是取消Item的动画执行，从代码8中只要设置mItemAnimator =null就可以了，
            // RecyclerView也提供的Api：setItemAnimator()
            // 使用这个有几个Exception
            /*notifyItemRangeChanged(mKeys.size() - ContentsFragment.ONE_TIME_ADD_COUNT,
                    ContentsFragment.ONE_TIME_ADD_COUNT);*/
        }
    }

    /*public void setDataSource(String path) {
        mPath = path;
    }*/

    public interface OnItemClickListener {
        void onItemClick(String key, int position, int viewId);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private Button mDownloadBtn;

    public void setProgress(String progress) {
        if (mDownloadBtn != null) {
            mDownloadBtn.setText(progress);
        }
    }

    private class TitleViewHolder extends RecyclerView.ViewHolder {

        private View itemView;
        private TextView title;
        private Button downloadBtn;
        // 保存了mContentsMap的key
        private String key;

        public TitleViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            title = itemView.findViewById(R.id.content_title);
            downloadBtn = itemView.findViewById(R.id.item_download_btn);
            itemView.setOnClickListener(onClickListener);
            downloadBtn.setOnClickListener(onClickListener);
        }

        private void onClick(View view) {
            if (mOnItemClickListener != null) {
                int position = mKeys.indexOf(key);
                switch (view.getId()) {
                    case R.id.item_root_layout:
                        mOnItemClickListener.onItemClick(
                                key, position, R.id.item_root_layout);
                        break;
                    case R.id.item_download_btn:
                        mOnItemClickListener.onItemClick(
                                key, position, R.id.item_download_btn);
                        mDownloadBtn = downloadBtn;
                        break;
                    default:
                        break;
                }
            }
        }

        private View.OnClickListener onClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ContentsAdapter.TitleViewHolder.this.onClick(view);
                    }
                };
    }

}
