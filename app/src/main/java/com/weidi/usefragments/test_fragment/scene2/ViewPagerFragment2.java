package com.weidi.usefragments.test_fragment.scene2;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;


//import androidx.viewpager.widget.PagerAdapter;
//import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.weidi.usefragments.R;
import com.weidi.usefragments.business.media.CameraPreviewFragment;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.fragment.base.BaseFragment;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.inject.InjectView;
import com.weidi.usefragments.tool.MLog;

import java.util.ArrayList;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/***
 框架模板类
 */
public class ViewPagerFragment2 extends BaseFragment {

    private static final String TAG =
            ViewPagerFragment2.class.getSimpleName();
    private static final boolean DEBUG = true;

    public ViewPagerFragment2() {
        super();
    }

    /*********************************
     * Created
     *********************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG)
            MLog.d(TAG, "onAttach() " + printThis() +
                    " mContext: " + context);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (DEBUG)
            MLog.d(TAG, "onAttach() " + printThis() +
                    " activity: " + activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        initData();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG)
            MLog.d(TAG, "onCreateView() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewCreated() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        initView(view, savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewStateRestored() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onActivityCreated() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    /*********************************
     * Started
     *********************************/

    @Override
    public void onStart() {
        super.onStart();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onStart() " + printThis());
    }

    /*********************************
     * Resumed
     *********************************/

    @Override
    public void onResume() {
        super.onResume();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onResume() " + printThis());

        onShow();
    }

    /*********************************
     * Paused
     *********************************/

    @Override
    public void onPause() {
        super.onPause();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onPause() " + printThis());
    }

    /*********************************
     * Stopped
     *********************************/

    @Override
    public void onStop() {
        super.onStop();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onStop() " + printThis());

        onHide();
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView() " + printThis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy() " + printThis());

        destroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach() " + printThis());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            MLog.d(TAG, "onActivityResult(): " + printThis() +
                    " requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data.toString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            MLog.d(TAG, "onSaveInstanceState() " + printThis());
    }

    @Override
    public void handleConfigurationChangedEvent(
            Configuration newConfig,
            boolean needToDo,
            boolean override) {
        handleBeforeOfConfigurationChangedEvent();

        super.handleConfigurationChangedEvent(newConfig, needToDo, true);

        if (needToDo) {
            onShow();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG)
            MLog.d(TAG, "onLowMemory() " + printThis());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DEBUG)
            MLog.d(TAG, "onTrimMemory() " + printThis() +
                    " level: " + level);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (DEBUG)
            MLog.d(TAG, "onRequestPermissionsResult() " + printThis() +
                    " requestCode: " + requestCode);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (DEBUG)
            MLog.d(TAG, "onHiddenChanged() " + printThis() +
                    " hidden: " + hidden);

        if (hidden) {
            onHide();
        } else {
            onShow();
        }
    }

    @Override
    protected int provideLayout() {
        return R.layout.fragment_test_viewpager;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    @InjectView(R.id.test_viewpager)
    private ViewPager viewpager;
    private MyViewPagerAdapter myViewPagerAdapter;
    private int previous;//上一个页面的标记

    private int[] images = new int[]{
            R.drawable.a1, R.drawable.a2, R.drawable.a3, R.drawable.a4,
            R.drawable.a5, R.drawable.a6, R.drawable.a7, R.drawable.a9
    };

    private String[] imagesString = new String[]{//这个不用理，只是增加一些提示信息而已
            "巩俐不低俗，我就不能低俗",
            "扑树又回来啦！再唱经典老歌引万人大合唱",
            "揭秘北京电影如何升级",
            "乐视网TV版大派送",
            "热血屌丝的反杀",
            "扑树又回来啦！再唱经典老歌引万人大合唱",
            "揭秘北京电影如何升级",
            "乐视网TV版大派送"
    };
    private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();

    /*
     * 自动循环：
     * 1、定时器：Timer
     * 2、开子线程 while  true 循环
     * 3、ColckManager
     * 4、 用handler 发送延时信息，实现循环
     */
    private boolean isRunning = true;
    private Handler mUiHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            viewpager.setCurrentItem(viewpager.getCurrentItem() + 1);
            if (isRunning) {
                mUiHandler.sendEmptyMessageDelayed(0, 2000);
            }
        }
    };

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

    }

    /***
     代码执行的内容跟onPause(),onStop()一样,
     因此在某些情况下要么执行onPause(),onStop()方法,要么执行onHide()方法.
     一般做的事是视频的暂停,摄像头的关闭
     */
    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide() " + printThis());
    }

    private void initData() {

    }

    private A2Fragment mA2Fragment;
    private B2Fragment mB2Fragment;
    private C2Fragment mC2Fragment;
    private D2Fragment mD2Fragment;
    private E2Fragment mE2Fragment;

    private void initView(View view, Bundle savedInstanceState) {
        mA2Fragment = new A2Fragment();
        mB2Fragment = new B2Fragment();
        mC2Fragment = new C2Fragment();
        mD2Fragment = new D2Fragment();
        mE2Fragment = new E2Fragment();

        mFragments.add(mA2Fragment);
        mFragments.add(mB2Fragment);
        mFragments.add(mC2Fragment);
        mFragments.add(mD2Fragment);
        mFragments.add(mE2Fragment);

        // content_layout
        FragmentTransaction fragmentTransaction =
                getChildFragmentManager().beginTransaction();
        fragmentTransaction.add(
                R.id.test_viewpager,
                mA2Fragment,
                Fragment.class.getSimpleName());
        fragmentTransaction.addToBackStack(
                Fragment.class.getSimpleName());
        fragmentTransaction.show(mA2Fragment);
        fragmentTransaction.commit();

        myViewPagerAdapter = new MyViewPagerAdapter();
        viewpager.setAdapter(myViewPagerAdapter);
        //下面这句代码比较关键 设置当前页面的下标为一个很大的数，
        // 那么左右都可以滑动了 后面减去的部分是因为显示下标为0时的图片错开的数
        viewpager.setCurrentItem(
                Integer.MAX_VALUE / 2 - (Integer.MAX_VALUE / 2 % mFragments.size()));
        viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            /**
             * 页面切换后调用
             * arg0  新的页面位置
             */
            @Override
            public void onPageScrolled(int position,
                                       float positionOffset,
                                       int positionOffsetPixels) {
            }

            /**
             * 页面正在滑动的时候，回调
             */
            @Override
            public void onPageSelected(int position) {
                int recycle = position % mFragments.size();
                previous = recycle;

                Fragment fragment = mFragments.get(recycle);
                FragmentTransaction fragmentTransaction =
                        getChildFragmentManager().beginTransaction();
                fragmentTransaction.add(
                        R.id.test_viewpager,
                        fragment,
                        Fragment.class.getSimpleName());
                fragmentTransaction.addToBackStack(
                        Fragment.class.getSimpleName());
                fragmentTransaction.show(fragment);
                fragmentTransaction.commit();
            }

            /**
             * 当页面状态发生变化的时候，回调
             */
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        //        mUiHandler.sendEmptyMessageDelayed(0, 2000);
    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {

    }

    @InjectOnClick({R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.jump_btn:
                FragOperManager.getInstance().enter3(new CameraPreviewFragment());
                break;
        }
    }

    private class MyViewPagerAdapter extends PagerAdapter {
        /**
         * 获得页面的总数
         */
        @Override
        public int getCount() {//得到的数量并不一定要是集合中元素的个数
            return Integer.MAX_VALUE;
        }

        /**
         * 获得相应位置上的view
         * container  view的容器，其实就是viewpager自身
         * position   相应的位置
         */
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            int recycle = position % mFragments.size();
            //container.addView(mFragments.get(recycle));
            return mFragments.get(recycle);
        }

        /**
         * 判断 view和object的对应关系
         */
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        /**
         * 销毁对应位置上的object
         */
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //container.removeView((View) object);
            object = null;
        }

    }

}
