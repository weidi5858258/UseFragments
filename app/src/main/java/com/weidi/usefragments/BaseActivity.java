package com.weidi.usefragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.weidi.usefragments.tool.MLog;


public abstract class BaseActivity extends Activity {

    private static final String TAG =
            BaseActivity.class.getSimpleName();

    private static final boolean DEBUG = false;
    private Context mContext = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + this
                    + " savedInstanceState: " + savedInstanceState);
        this.mContext = this.getApplicationContext();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            MLog.d(TAG, "onStart(): " + this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            MLog.d(TAG, "onResume(): " + this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            MLog.d(TAG, "onPause(): " + this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            MLog.d(TAG, "onStop(): " + this);
    }

    @Override
    public void onDestroy() {
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + this);
        super.onDestroy();
        exitActivity();
    }

    protected Context getContext() {
        if (this.mContext == null) {
            this.mContext = this.getApplicationContext();
        }
        return this.mContext;
    }

    /***
     目的:
     有一个现象,当一个Activity中所有的Fragment都被
     pop掉后,Activity的生命周期不会发生变化,
     也就是onResume()不会被调用,
     这样可能在内容显示上面应有问题.
     */
    public abstract void onResume_();

    /**
     * 打开页面时，页面从右往左滑入
     * 底下的页面不需要有动画
     */
    public void enterActivity() {
        try {
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        } catch (Exception e) {
        }
    }

    /**
     * 关闭页面时，页面从左往右滑出
     */
    public void exitActivity() {
        try {
            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        } catch (Exception e) {
        }
    }

}