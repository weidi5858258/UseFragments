package com.weidi.usefragments.test_view;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class SimpleViewHolder extends BaseNodeViewHolder<Object> {

    public SimpleViewHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(TreeNode node, Object value) {
        final TextView tv = new TextView(mContext);
        tv.setText(String.valueOf(value));
        return tv;
    }

    @Override
    public void toggle(boolean active) {

    }

}
