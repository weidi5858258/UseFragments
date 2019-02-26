package com.weidi.usefragments.test_view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.weidi.usefragments.R;

/**
 * Created by weidi on 2019/2/26.
 */

public abstract class BaseNodeViewHolder<E> {

    protected Context mContext;
    protected AndroidTreeView tView;
    protected TreeNode mNode;
    protected int mContainerStyle;
    private View mView;

    public BaseNodeViewHolder(Context mContext) {
        this.mContext = mContext;
    }

    public View getView() {
        if (mView != null) {
            return mView;
        }
        final View nodeView = getNodeView();// android.widget.RelativeLayout{3d3ff4d V.E...... ......I. 0,0-0,0}
        final TreeNodeWrapperView nodeWrapperView =
                new TreeNodeWrapperView(nodeView.getContext(), getmContainerStyle());
        nodeWrapperView.insertNodeView(nodeView);
        mView = nodeWrapperView;
        mView.setClickable(true);
        mView.setFocusable(true);

        return mView;
    }

    public void setTreeViev(AndroidTreeView treeViev) {
        this.tView = treeViev;
    }

    public AndroidTreeView getTreeView() {
        return tView;
    }

    public void setmContainerStyle(int style) {
        mContainerStyle = style;
    }

    public View getNodeView() {
        return createNodeView(mNode, (E) mNode.getValue());
    }

    public ViewGroup getNodeItemsView() {
        return (ViewGroup) getView().findViewById(R.id.node_items);
    }

    public boolean isInitialized() {
        return mView != null;
    }

    public int getmContainerStyle() {
        return mContainerStyle;
    }

    public void toggle(boolean active) {
        // empty
    }

    public void toggleSelectionMode(boolean editModeEnabled) {
        // empty
    }

    public abstract View createNodeView(TreeNode node, E value);

}
