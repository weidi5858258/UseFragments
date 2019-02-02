package com.weidi.usefragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;

import java.util.List;
import java.util.Map;

/***
 写代码的人需要考虑到某些关键性代码怎么调试.
 一般看日志.
 需要说明一下,哪些代码输入什么输出什么是正常的,不正常的.
 */
public abstract class BaseActivity extends Activity {

    private static final String TAG =
            BaseActivity.class.getSimpleName();

    private static final boolean DEBUG = true;
    private Context mContext = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);
        this.mContext = this.getApplicationContext();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (DEBUG)
            MLog.d(TAG, "onRestart(): " + printThis());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            MLog.d(TAG, "onStart(): " + printThis());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            MLog.d(TAG, "onResume(): " + printThis());
        FragOperManager.getInstance().setCurUsedActivity(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            MLog.d(TAG, "onPause(): " + printThis());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            MLog.d(TAG, "onStop(): " + printThis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + printThis());
        exitActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            Log.d(TAG, "onActivityResult(): " + printThis() +
                    " requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            Log.d(TAG, "onSaveInstanceState(): " + printThis() +
                    " outState: " + outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onRestoreInstanceState(): " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    /**
     * 当配置发生变化时，不会重新启动Activity。
     * 但是会回调此方法，用户自行进行对屏幕旋转后进行处理.
     * <p>
     * 横竖屏切换改变布局,参照MainActivity1
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w640dp h336dp 320dpi nrml long land
        // finger -keyb/v/h -nav/h s.264}
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w360dp h616dp 320dpi nrml long port
        // finger -keyb/v/h -nav/h s.265}
        if (DEBUG)
            Log.d(TAG, "onConfigurationChanged(): " + printThis() +
                    " newConfig: " + newConfig);

        // 通知所有的Fragment配置发生了变化
        Map<Fragment, List<Fragment>> mapTemp =
                FragOperManager.getInstance().getMainFragmentsMap();
        for (Map.Entry<Fragment, List<Fragment>> map : mapTemp.entrySet()) {
            // mainFragment
            if (map.getKey().isHidden()) {
                map.getKey().onConfigurationChanged(newConfig);
            }
            // mainChildFragment
            List<Fragment> mainChildFragmentsList = map.getValue();
            if (mainChildFragmentsList == null
                    || mainChildFragmentsList.isEmpty()) {
                continue;
            }
            for (Fragment mainChildFragment : mainChildFragmentsList) {
                if (mainChildFragment == null) {
                    continue;
                }
                if (mainChildFragment.isHidden()) {
                    mainChildFragment.onConfigurationChanged(newConfig);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        /*if (permissions != null) {
            for (String permission : permissions) {
                Log.i(TAG, "onRequestPermissionsResult(): " + permission);
            }
        }*/

        PermissionsUtils.onRequestPermissionsResult(
                this,
                permissions,
                grantResults);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (DEBUG)
            MLog.d(TAG, "onBackPressed(): " + printThis());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (DEBUG)
            MLog.d(TAG, "onWindowFocusChanged(): " + printThis() +
                    " hasFocus: " + hasFocus);

        /***
         界面默认情况下是全屏的，状态栏和导航栏都不会显示。
         而当我们需要用到状态栏或导航栏时，
         只需要在屏幕顶部向下拉，或者在屏幕右侧向左拉，
         状态栏和导航栏就会显示出来，
         此时界面上任何元素的显示或大小都不会受影响。
         过一段时间后如果没有任何操作，
         状态栏和导航栏又会自动隐藏起来，
         重新回到全屏状态。
         */
        /*if (hasFocus && Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }*/

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

    public static FrameLayout getContentLayout(Activity activity) {
        if (activity == null) {
            return null;
        }
        View view = activity.findViewById(android.R.id.content);
        if (view != null && view instanceof FrameLayout) {
            return (FrameLayout) view;

        }
        return null;
    }

    public static View getRootView(Activity activity) {
        if (activity == null) {
            return null;
        }
        View view = activity.findViewById(android.R.id.content);
        if (view != null && view instanceof FrameLayout) {
            FrameLayout contentView = (FrameLayout) view;
            if (contentView.getChildCount() > 0) {
                return contentView.getChildAt(0);
            }
        }
        return null;
    }

    protected String printThis() {
        // com.weidi.usefragments.MainActivity2@416c7b
        String temp = this.toString();
        int lastIndex = temp.lastIndexOf(".");
        temp = temp.substring(lastIndex + 1, temp.length());
        return temp;
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