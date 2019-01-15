package com.weidi.usefragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.weidi.usefragments.fragment.FragOperManager;
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
    public void onRestart() {
        super.onRestart();
        if (DEBUG)
            MLog.d(TAG, "onRestart(): " + this);
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
        FragOperManager.getInstance().setCurUsedActivity(this);
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
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + this);
        exitActivity();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (DEBUG)
            MLog.d(TAG, "onBackPressed(): " + this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (DEBUG)
            MLog.d(TAG, "onWindowFocusChanged() hasFocus: " + hasFocus);

        /*if (hasFocus) {
            showWindow();
        } else {
            if (mWindowManager != null && mToolView != null) {
                mWindowManager.removeView(mToolView);
                mWMLayoutParams = null;
            }
        }*/
    }

    protected Context getContext() {
        if (this.mContext == null) {
            this.mContext = this.getApplicationContext();
        }
        return this.mContext;
    }

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

    private WindowManager.LayoutParams mWMLayoutParams;
    private WindowManager mWindowManager;
    private View mToolView;

    private void showWindow() {
        if (mWMLayoutParams != null) {
            return;
        }
        mToolView =
                View.inflate(this, R.layout.activity_title_view, null);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mWMLayoutParams = new WindowManager.LayoutParams();
        //悬浮窗参数设置
        mWMLayoutParams.format = PixelFormat.RGBA_8888;
        mWMLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWMLayoutParams.gravity = Gravity.RIGHT | Gravity.TOP;
        mWMLayoutParams.width = 80;//悬浮窗宽度
        mWMLayoutParams.height = 80;//悬浮窗高度
        mWMLayoutParams.x = 720;//悬浮窗位置
        mWMLayoutParams.y = 0;//悬浮窗位置
        //重点,类型设置为dialog类型,可无视权限!
        mWMLayoutParams.type =
                WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        //重点,必须设置此参数,用于窗口机制验证
        IBinder windowToken = this.getWindow().getDecorView().getWindowToken();
        mWMLayoutParams.token = windowToken;
        mWindowManager.addView(mToolView, mWMLayoutParams);

        mToolView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

}