package com.weidi.usefragments.test_view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.github.johnkil.print.PrintView;
import com.weidi.usefragments.R;

public class TreeItemHolder extends
        TreeNode.BaseNodeViewHolder<TreeItemHolder.IconTreeItem> {
    private TextView tvValue;
    private PrintView arrowView;

    public static class IconTreeItem {
        public int icon;
        public String text;

        public IconTreeItem(int icon, String text) {
            this.icon = icon;
            this.text = text;
        }
    }

    public TreeItemHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(final TreeNode node, IconTreeItem value) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.layout_icon_node, null, false);

        final PrintView iconView = (PrintView) view.findViewById(R.id.icon);
        iconView.setIconText(context.getResources().getString(value.icon));

        tvValue = (TextView) view.findViewById(R.id.node_value);
        tvValue.setText(value.text);


        arrowView = (PrintView) view.findViewById(R.id.arrow_icon);

        /*view.findViewById(R.id.btn_addFolder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TreeNode newFolder = new TreeNode(new TreeItemHolder.IconTreeItem(R.string
                .ic_folder, "New Folder"));
                getTreeView().addNode(node, newFolder);
            }
        });

        view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTreeView().removeNode(node);
            }
        });

        //if My computer
        if (node.getLevel() == 1) {
            view.findViewById(R.id.btn_delete).setVisibility(View.GONE);
        }*/

        return view;
    }

    @Override
    public void toggle(boolean active) {
        arrowView.setIconText(
                context.getResources().getString(
                        active
                                ?
                                R.string.ic_keyboard_arrow_down
                                :
                                R.string.ic_keyboard_arrow_right));
    }

}
