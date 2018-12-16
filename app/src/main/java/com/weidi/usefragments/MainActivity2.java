package com.weidi.usefragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.test_fragment.scene1.AFragment;
import com.weidi.usefragments.test_fragment.scene1.BFragment;
import com.weidi.usefragments.test_fragment.scene1.CFragment;
import com.weidi.usefragments.test_fragment.scene1.DFragment;
import com.weidi.usefragments.test_fragment.scene1.EFragment;

import java.util.HashMap;

/***
 场景:
 一个MainActivity,不加载MainFragment.
 经过某种操作后开启一个Fragment,
 然后从这个Fragment开启其他Fragment.
 */
public class MainActivity2 extends BaseActivity
        implements BaseFragment.BackHandlerInterface {

    private static final String TAG =
            MainActivity2.class.getSimpleName();

    private static final boolean DEBUG = true;
    private BaseFragment mBaseFragment;
    private static HashMap<String, Integer> sFragmentBackTypeSMap;

    static {
        // 如果有MainFragment(MainActivity中使用MainFragment,其他Fragment从MainFragment中被开启),
        // 那么不要把MainFragment加入Map中.
        sFragmentBackTypeSMap = new HashMap<String, Integer>();
        sFragmentBackTypeSMap.put(AFragment.class.getSimpleName(), FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(BFragment.class.getSimpleName(), FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(CFragment.class.getSimpleName(), FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(DFragment.class.getSimpleName(), FragOperManager.POP_BACK_STACK);
        sFragmentBackTypeSMap.put(EFragment.class.getSimpleName(), FragOperManager.POP_BACK_STACK);
    }

    private Button mJumpBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        if (DEBUG)
            Log.d(TAG, "onCreate() savedInstanceState: " + savedInstanceState);
        FragOperManager.getInstance().addActivity(this, R.id.root_layout);
        if (savedInstanceState != null) {

        } else {

        }

        mJumpBtn = findViewById(R.id.jump_btn);
        mJumpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mJumpBtn.setVisibility(View.GONE);
                Fragment fragment = new AFragment();
                FragOperManager.getInstance().enter(MainActivity2.this,
                        fragment,
                        AFragment.class.getSimpleName());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            Log.d(TAG, "onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (DEBUG)
            Log.d(TAG, "onRestart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume()");
        /***
         做这个事的原因:
         如果后退时Fragment是弹出的话,不需要这样的代码的;
         如果这些Fragment是像QQ那样实现的底部导航栏形式的,
         在任何一个页面都可以退出,那么也不需要实现这样的代码的.

         如果有多个Fragment开启着,并且相互之间是显示和隐藏,而不是弹出,
         那么页面在MainFragment时关闭屏幕,然后在点亮屏幕后,
         MainFragment的onResume()方法比其他Fragment的onResume()方法要先执行,
         最后执行的Fragment就得到了后退的"焦点",
         这样的话要后退时导致在MainFragment页面时就退不出去了.
         */
        /*if (this.mSavedInstanceState != null) {
            final String fragmentTag = this.mSavedInstanceState.getString("FragmentTag");
            List<Fragment> fragmentsList =
                    FragOperManager.getInstance().getParentFragmentsList(this);
            if (fragmentsList == null) {
                return;
            }
            FragOperManager.getInstance().enter(this, mBaseFragment, null);
            int count = fragmentsList.size();
            for (int i = 0; i < count; i++) {
                final Fragment fragment = fragmentsList.get(i);
                if (fragment != null && fragment.getClass().getSimpleName().equals(fragmentTag)) {
                    if (fragment instanceof BaseFragment) {
                        HandlerUtils.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (((BaseFragment) fragment).getBackHandlerInterface() != null) {
                                    ((BaseFragment) fragment).getBackHandlerInterface()
                                            .setSelectedFragment(
                                                    (BaseFragment) fragment,
                                                    fragmentTag);
                                }
                            }
                        }, 500);
                    }
                    break;
                }
            }
            this.mSavedInstanceState = null;
        }*/

        mJumpBtn.setVisibility(View.VISIBLE);
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
        if (DEBUG)
            Log.d(TAG, "onDestroy()");
        FragOperManager.getInstance().removeActivity(this);
        super.onDestroy();
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
                || mBaseFragment.onBackPressed()
                || FragOperManager.getInstance().getParentFragmentsList(this) == null
                || FragOperManager.getInstance().getParentFragmentsList(this).isEmpty()) {
            this.finish();
            this.exitActivity();
            return;
        }

        // 实现后退功能(把当前Fragment进行pop或者hide)
        final String fragmentName = mBaseFragment.getClass().getSimpleName();
        if (DEBUG)
            Log.d(TAG, "onBackPressed() fragmentName: " + fragmentName);
        for (String key : sFragmentBackTypeSMap.keySet()) {
            if (key.equals(fragmentName)) {
                int type = sFragmentBackTypeSMap.get(key);
                FragOperManager.getInstance().onEvent(
                        type,
                        new Object[]{this, mBaseFragment});
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
     * 当配置发生变化时，不会重新启动Activity。但是会回调此方法，用户自行进行对屏幕旋转后进行处理.
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

    public void onResume_() {
        mJumpBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment, String fragmentTag) {
        if (DEBUG)
            Log.d(TAG, "setSelectedFragment() selectedFragment: "
                    + selectedFragment.getClass().getSimpleName());
        mBaseFragment = selectedFragment;
    }
}
//记录Fragment的位置
//    private int position = 0;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_index);
//        setTabSelection(position);
//    }
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        position = savedInstanceState.getInt("position");
//        setTabSelection(position);
//        super.onRestoreInstanceState(savedInstanceState);
//    }
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//super.onSaveInstanceState(outState);   //将这一行注释掉，阻止activity保存fragment的状态
//        //记录当前的position
//        outState.putInt("position", position);
//    }                                                      