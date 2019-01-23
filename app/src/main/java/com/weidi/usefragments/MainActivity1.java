package com.weidi.usefragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.test_fragment.scene2.A2Fragment;
import com.weidi.usefragments.test_fragment.scene2.AudioFragment;
import com.weidi.usefragments.test_fragment.scene2.B2Fragment;
import com.weidi.usefragments.test_fragment.scene2.C2Fragment;
import com.weidi.usefragments.test_fragment.scene2.Camera2Fragment;
import com.weidi.usefragments.test_fragment.scene2.CameraPreviewFragment;
import com.weidi.usefragments.test_fragment.scene2.D2Fragment;
import com.weidi.usefragments.test_fragment.scene2.E2Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main1Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main2Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main3Fragment;
import com.weidi.usefragments.test_fragment.scene2.Main4Fragment;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private View mRootView;

    private static HashMap<String, Integer> sFragmentBackTypeSMap;

    static {
        // 如果有MainFragment(MainActivity中使用MainFragment,其他Fragment从MainFragment中被开启),
        // 那么不要把MainFragment加入Map中.
        sFragmentBackTypeSMap = new HashMap<String, Integer>();
        sFragmentBackTypeSMap.clear();
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
                Camera2Fragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                CameraPreviewFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(
                AudioFragment.class.getSimpleName(),
                FragOperManager.POP_BACK_STACK);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRootView = View.inflate(this, R.layout.activity_main, null);
        FrameLayout contentLayout = getContentLayout(this);
        if (contentLayout != null) {
            contentLayout.addView(mRootView);
        } else {
            finish();
            return;
        }
        // setContentView(R.layout.activity_main);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);
        FragOperManager.getInstance().addActivity2(this, R.id.content_layout);
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

        handleConfigurationChangedEvent();
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment, String fragmentTag) {
        if (DEBUG)
            Log.d(TAG, "setSelectedFragment() selectedFragment: "
                    + selectedFragment.getClass().getSimpleName());
        mBaseFragment = selectedFragment;
    }

    private void handleConfigurationChangedEvent() {
        // 得到系统提供的用于显示用户界面的ContentLayout
        FrameLayout contentLayout = getContentLayout(this);
        if (contentLayout != null) {
            // 得到用于显示Fragment内容的容器布局
            FrameLayout fragmentContentLayout = mRootView.findViewById(R.id.content_layout);

            Map<Fragment, List<Fragment>> mapTemp =
                    FragOperManager.getInstance().getMainFragmentsMap();
            for (Map.Entry<Fragment, List<Fragment>> map : mapTemp.entrySet()) {
                String fragmentName = map.getKey().getClass().getSimpleName();
                if (TextUtils.isEmpty(fragmentName)) {
                    continue;
                }
                // remove mainFragment
                fragmentContentLayout.removeView(map.getKey().getView());
                List<Fragment> mainChildFragmentsList = map.getValue();
                if (mainChildFragmentsList == null
                        || mainChildFragmentsList.isEmpty()) {
                    continue;
                }
                for (Fragment mainChildFragment : mainChildFragmentsList) {
                    if (mainChildFragment == null) {
                        continue;
                    }
                    fragmentContentLayout.removeView(mainChildFragment.getView());
                }
            }
            contentLayout.removeView(mRootView);

            // 当前Activity的新布局
            mRootView = View.inflate(this, R.layout.activity_main, null);
            fragmentContentLayout = mRootView.findViewById(R.id.content_layout);
            for (Map.Entry<Fragment, List<Fragment>> map : mapTemp.entrySet()) {
                String fragmentName = map.getKey().getClass().getSimpleName();
                if (TextUtils.isEmpty(fragmentName)) {
                    continue;
                }
                fragmentContentLayout.addView(map.getKey().getView());
                List<Fragment> mainChildFragmentsList = map.getValue();
                if (mainChildFragmentsList == null
                        || mainChildFragmentsList.isEmpty()) {
                    continue;
                }
                for (Fragment mainChildFragment : mainChildFragmentsList) {
                    if (mainChildFragment == null) {
                        continue;
                    }
                    fragmentContentLayout.addView(mainChildFragment.getView());
                }
            }
            contentLayout.addView(mRootView);

            findViewById(R.id.main1_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.main2_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.main3_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.main4_btn).setOnClickListener(mViewOnClickListener);
            findViewById(R.id.back_btn).setOnClickListener(mViewOnClickListener);

            if (mCurShowMainFragment == null) {
                return;
            }
            String mainFragmentName = mCurShowMainFragment.getClass().getSimpleName();
            if (TextUtils.isEmpty(mainFragmentName)) {
                mCurShowMainFragment = main1Fragment;
                findViewById(R.id.main1_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
                mPreShowMainFragment = mCurShowMainFragment;
                FragOperManager.getInstance().setCurUsedFragment(mCurShowMainFragment);
                FragOperManager.getInstance().changeFragment();
                return;
            }
            if (mainFragmentName.equals(Main1Fragment.class.getSimpleName())) {
                findViewById(R.id.main1_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            } else if (mainFragmentName.equals(Main2Fragment.class.getSimpleName())) {
                findViewById(R.id.main2_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            } else if (mainFragmentName.equals(Main3Fragment.class.getSimpleName())) {
                findViewById(R.id.main3_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            } else if (mainFragmentName.equals(Main4Fragment.class.getSimpleName())) {
                findViewById(R.id.main4_btn).setBackgroundColor(
                        getResources().getColor(android.R.color.holo_green_light));
            }
        } else {
            finish();
            return;
        }
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

                        // test
                        /*FrameLayout fragmentLayout = mRootView.findViewById(R.id.content_layout);
                        FrameLayout fragmentLayoutTest = mRootView.findViewById(R.id
                        .content_layout_test);
                        LinearLayout fragmentRootLayout = mRootView.findViewById(R.id.root_layout);
                        fragmentLayout.removeView(fragmentRootLayout);
                        fragmentLayoutTest.addView(fragmentRootLayout);
                        fragmentRootLayout.invalidate();
                        fragmentLayoutTest.invalidate();*/
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