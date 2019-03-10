package com.weidi.usefragments.test_view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import com.weidi.usefragments.tool.MLog;

/**
 * Created by weidi on 2019/3/10.
 */

public class MyTextView extends TextView {

    private static final String TAG =
            MyTextView.class.getSimpleName();
    private static final boolean DEBUG = true;

    public MyTextView(@NonNull Context context) {
        super(context);
    }

    public MyTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyTextView(@NonNull Context context, @Nullable AttributeSet attrs,
                      int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (DEBUG)
                MLog.d(TAG, " alexander onTouchEvent() MotionEvent.ACTION_DOWN");
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (DEBUG)
                MLog.d(TAG, " alexander onTouchEvent() MotionEvent.ACTION_UP");
        }
//        return false;
        return super.onTouchEvent(motionEvent);
    }
}
