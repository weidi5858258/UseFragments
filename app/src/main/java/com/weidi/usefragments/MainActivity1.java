package com.weidi.usefragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.test_fragment.scene1.AFragment;
import com.weidi.usefragments.test_fragment.scene1.BFragment;
import com.weidi.usefragments.test_fragment.scene1.CFragment;
import com.weidi.usefragments.test_fragment.scene1.DFragment;
import com.weidi.usefragments.test_fragment.scene1.EFragment;
import com.weidi.usefragments.test_fragment.scene2.A2Fragment;
import com.weidi.usefragments.test_fragment.scene2.B2Fragment;
import com.weidi.usefragments.test_fragment.scene2.C2Fragment;
import com.weidi.usefragments.test_fragment.scene2.D2Fragment;
import com.weidi.usefragments.test_fragment.scene2.E2Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main1Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main2Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main3Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main4Fragment;

import java.util.HashMap;

/***
 场景:
 一个MainActivity,预告加载4个Fragment.
 这4个Fragment相互之间是show或者hide,
 一直依附在MainActivity上,当MainActivity退出时
 4个Fragment跟着退出.不然一直不退出.
 从4个Fragment中可以开启其他任意的Fragment.
 */
public class MainActivity1 extends BaseActivity
        implements BaseFragment.BackHandlerInterface {

    private static final String TAG =
            MainActivity1.class.getSimpleName();

    private static final boolean DEBUG = true;
    private BaseFragment mBaseFragment;
    private Fragment mCurShowMainFragment;
    private Fragment mPreShowMainFragment;

    private Fragment main1Fragment;
    private Fragment main2Fragment;
    private Fragment main3Fragment;
    private Fragment main4Fragment;

    private static HashMap<String, Integer> sFragmentBackTypeSMap;

    static {
        // 如果有MainFragment(MainActivity中使用MainFragment,其他Fragment从MainFragment中被开启),
        // 那么不要把MainFragment加入Map中.
        sFragmentBackTypeSMap = new HashMap<String, Integer>();
        sFragmentBackTypeSMap.put(
                A2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                B2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                C2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                D2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                E2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);

        sFragmentBackTypeSMap.put(
                AFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                BFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                CFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                DFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                EFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (DEBUG)
            Log.d(TAG, "onCreate() savedInstanceState: " + savedInstanceState);
        FragOperManager.getInstance().addActivity2(this, R.id.root_layout);
        if (savedInstanceState != null) {

        } else {

        }

        main1Fragment = new Main1Fragment();
        main2Fragment = new Main2Fragment();
        main3Fragment = new Main3Fragment();
        main4Fragment = new Main4Fragment();
        mCurShowMainFragment = main1Fragment;
        mPreShowMainFragment = main1Fragment;
        FragOperManager.getInstance().setCurUsedFragment(
                mCurShowMainFragment);
        FragOperManager.getInstance().enter2(main4Fragment);
        FragOperManager.getInstance().enter2(main3Fragment);
        FragOperManager.getInstance().enter2(main2Fragment);
        FragOperManager.getInstance().enter2(main1Fragment);

        findViewById(R.id.main1_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.main2_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.main3_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.main4_btn).setOnClickListener(mViewOnClickListener);
        findViewById(R.id.back_btn).setOnClickListener(mViewOnClickListener);

        findViewById(R.id.main1_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_green_light));
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (DEBUG)
            Log.d(TAG, "onRestart()");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            Log.d(TAG, "onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            Log.d(TAG, "onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            Log.d(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            Log.d(TAG, "onDestroy()");
        FragOperManager.getInstance().removeActivity(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG)
            Log.d(TAG, "onLowMemory()");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DEBUG)
            Log.d(TAG, "onTrimMemory() level: " + level);
    }

    /***
     * 如果多个Fragment以底部Tab方式呈现的话,
     * 那么这些Fragment中的onBackPressed()方法最好返回true.
     * 这样就不需要在MainActivityController中处理onResume()方法了.
     * 如果不是以这种方式呈现,那么这些Fragment中的onBackPressed()方法最好返回false.
     * 然后需要在MainActivityController中处理onResume()方法了.
     */
    @Override
    public void onBackPressed() {
        if (DEBUG)
            Log.d(TAG, "onBackPressed()");
        if (mBaseFragment == null
                || mBaseFragment.onBackPressed()) {
            this.finish();
            // this.exitActivity();
            return;
        }

        // 实现后退功能(把当前Fragment进行pop或者hide)
        final String fragmentName = mBaseFragment.getClass().getSimpleName();
        if (DEBUG)
            Log.d(TAG, "onBackPressed() fragmentName: " + fragmentName);
        for (String key : sFragmentBackTypeSMap.keySet()) {
            if (key.equals(fragmentName)) {
                FragOperManager.getInstance().onEvent(
                        sFragmentBackTypeSMap.get(key),
                        new Object[]{mBaseFragment});
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            Log.d(TAG, "onActivityResult() requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            Log.d(TAG, "onSaveInstanceState() outState: " + outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onRestoreInstanceState() savedInstanceState: " + savedInstanceState);
    }

    /**
     * 当配置发生变化时，不会重新启动Activity.
     * 但是会回调此方法，用户自行进行对屏幕旋转后进行处理.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w640dp h336dp 320dpi nrml long land
        // finger -keyb/v/h -nav/h s.264}
        // newConfig: {1.0 460mcc1mnc [zh_CN] ldltr sw360dp w360dp h616dp 320dpi nrml long port
        // finger -keyb/v/h -nav/h s.265}
        if (DEBUG)
            Log.d(TAG, "onConfigurationChanged() newConfig: " + newConfig);
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment, String fragmentTag) {
        if (DEBUG)
            Log.d(TAG, "setSelectedFragment() selectedFragment: "
                    + selectedFragment.getClass().getSimpleName());
        mBaseFragment = selectedFragment;
    }

    private void restoreBackgroundColor() {
        findViewById(R.id.main1_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
        findViewById(R.id.main2_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
        findViewById(R.id.main3_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
        findViewById(R.id.main4_btn).setBackgroundColor(
                getResources().getColor(android.R.color.holo_orange_light));
    }

    private View.OnClickListener mViewOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getId() == R.id.back_btn) {
                        onBackPressed();
                        return;
                    }
                    restoreBackgroundColor();
                    switch (v.getId()) {
                        case R.id.main1_btn:
                            mCurShowMainFragment = main1Fragment;
                            findViewById(R.id.main1_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        case R.id.main2_btn:
                            mCurShowMainFragment = main2Fragment;
                            findViewById(R.id.main2_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        case R.id.main3_btn:
                            mCurShowMainFragment = main3Fragment;
                            findViewById(R.id.main3_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        case R.id.main4_btn:
                            mCurShowMainFragment = main4Fragment;
                            findViewById(R.id.main4_btn).setBackgroundColor(
                                    getResources().getColor(android.R.color.holo_green_light));
                            break;
                        default:
                    }
                    if (mPreShowMainFragment == null
                            || mCurShowMainFragment == null) {
                        return;
                    }
                    if (mPreShowMainFragment != null
                            && mCurShowMainFragment != null
                            && mPreShowMainFragment == mCurShowMainFragment) {
                        return;
                    }
                    mPreShowMainFragment = mCurShowMainFragment;
                    FragOperManager.getInstance().setCurUsedFragment(mCurShowMainFragment);
                    FragOperManager.getInstance().changeFragment();
                }
            };
}