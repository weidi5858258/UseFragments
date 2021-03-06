package com.weidi.usefragments.test_view;

import android.content.Context;


import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.weidi.usefragments.tool.MLog;

/**
 * Created by weidi on 2019/3/10.
 */

public class LayoutView1 extends LinearLayout {

    private static final String TAG =
            LayoutView1.class.getSimpleName();
    private static final boolean DEBUG = true;

    public LayoutView1(Context context) {
        super(context);
    }

    public LayoutView1(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LayoutView1(Context context, AttributeSet attrs,
                       int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LayoutView1(Context context, AttributeSet attrs,
                       int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (DEBUG)
                MLog.d(TAG, "alexander onInterceptTouchEvent() MotionEvent.ACTION_DOWN");
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (DEBUG)
                MLog.d(TAG, "alexander onInterceptTouchEvent() MotionEvent.ACTION_UP");
        }
//        return true;
        return super.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (DEBUG)
                MLog.d(TAG, "alexander onTouchEvent() MotionEvent.ACTION_DOWN");
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (DEBUG)
                MLog.d(TAG, "alexander onTouchEvent() MotionEvent.ACTION_UP");
        }
//        return false;
        return super.onTouchEvent(motionEvent);
    }
}
