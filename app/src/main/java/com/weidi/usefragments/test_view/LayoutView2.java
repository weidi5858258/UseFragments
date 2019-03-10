package com.weidi.usefragments.test_view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.weidi.usefragments.tool.MLog;

/**
 * Created by weidi on 2019/3/10.
 */

public class LayoutView2 extends LinearLayout {

    private static final String TAG =
            LayoutView2.class.getSimpleName();
    private static final boolean DEBUG = true;

    public LayoutView2(@NonNull Context context) {
        super(context);
    }

    public LayoutView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LayoutView2(@NonNull Context context, @Nullable AttributeSet attrs,
                       int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LayoutView2(@NonNull Context context, @Nullable AttributeSet attrs,
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
//        boolean touchEventResult = super.onTouchEvent(motionEvent);
//        MLog.d(TAG, "alexander onTouchEvent() touchEventResult: " + touchEventResult);
//        return touchEventResult;
        return super.onTouchEvent(motionEvent);
    }
}
