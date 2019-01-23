package com.weidi.usefragments.fragment.base;

import android.animation.Animator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.weidi.usefragments.BaseActivity;
import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.inject.InjectUtils;
import com.weidi.usefragments.tool.MLog;
import com.weidi.usefragments.tool.PermissionsUtils;

import java.lang.reflect.Field;


/***
 如果在子类Fragment切换横竖屏时需要变换布局,
 那么在一开始的时候就要设计好代码的组织架构,
 弄成方法便于调用.
 */
public abstract class BaseFragment extends Fragment {

    private static final String TAG =
            BaseFragment.class.getSimpleName();

    private static final boolean DEBUG = false;
    private Activity mActivity;
    private Context mContext;
    private BackHandlerInterface mBackHandlerInterface;

    // 当前配置
    private Configuration mCurConfiguration;
    // 当前屏幕方向
    // Configuration.ORIENTATION_PORTRAIT  竖屏
    // Configuration.ORIENTATION_LANDSCAPE 横屏
    private int mOrientation = Configuration.ORIENTATION_UNDEFINED;

    /***
     要开启Fragment的Activity必须要实现这个接口
     */
    public interface BackHandlerInterface {

        void setSelectedFragment(BaseFragment selectedFragment, String fragmentTag);

    }

    public BaseFragment() {
        super();
    }

    /*********************************
     * Created
     *********************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context == null) {
            throw new NullPointerException("BaseFragment onAttach() context is null.");
        }
        if (DEBUG)
            MLog.d(TAG, "onAttach(): " + printThis() + " context: " + context);
        mContext = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity == null) {
            throw new NullPointerException("BaseFragment onAttach() activity is null.");
        }
        if (DEBUG)
            MLog.d(TAG, "onAttach(): " + printThis() + " activity: " + activity);

        mActivity = activity;
        if (!(activity instanceof BackHandlerInterface)) {
            throw new ClassCastException("Hosting Activity must implement BackHandlerInterface");
        } else {
            mBackHandlerInterface = (BackHandlerInterface) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);
        /**
         * 一旦我们设置 setRetainInstance(true)，意味着在 Activity 重绘时，
         * 我们的 BaseFragment 不会被重复绘制，也就是它会被“保留”。为了验证
         * 其作用，我们发现在设置为 true 状态时，旋转屏幕，BaseFragment 依然是
         * 之前的 BaseFragment。而如果将它设置为默认的 false，那么旋转屏幕时
         * BaseFragment 会被销毁，然后重新创建出另外一个 fragment 实例。并且
         * 如官方所说，如果 BaseFragment 不重复创建，意味着 BaseFragment 的
         * onCreate 和 onDestroy 方法不会被重复调用。所以在旋转屏
         * BaseFragment 中，我们经常会设置 setRetainInstance(true)，
         * 这样旋转时 BaseFragment 不需要重新创建。
         *
         * setRetainInstance(true)
         * 当旋转屏幕时Fragment的onCreate()和onDestroy()方法不会被调用,
         * 但是其他生命周期方法都会被调用到.
         * onCreateView()和onActivityCreated()方法中的Bundle参数一直为null.
         * 这行代码在Activity的配置发生变化时onCreate()和onDestroy()方法
         * 不执行的情况下才有用,如果执行的话处理不好反而会发生不好的情况.
         * 如果实现这句代码的话,那么初始化工作放到onAttach(Activity activity)方法中去.
         * 上面的意思就是调用setRetainInstance(true);这行代码的前提
         * 最好是Activity在旋转屏幕时onCreate()和onDestroy()方法不会执行，
         * 因此需要在AndroidManifest.xml中配置Activity。
         */
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreateView(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);

        this.inflater = inflater;
        this.container = container;
        this.savedInstanceState = savedInstanceState;
        // 如果写成inflater.inflate(provideLayout(), container)这样的话,
        // 那么会报异常,具体异常就是已经有一个子类的parent,添加之前先要移除这个parent.
        View view = inflater.inflate(provideLayout(), null);
        InjectUtils.inject(this, view);
        if (view == null) {
            throw new NullPointerException(
                    "BaseFragment onCreateView() view is null.");
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewCreated(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);

        // 在子类中给某些View设置监听事件
        // View的内容显示在onShow()方法中进行
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewStateRestored(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onActivityCreated(): " + printThis()
                    + " savedInstanceState: " + savedInstanceState);
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
            MLog.d(TAG, "onStart(): " + printThis());
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
            MLog.d(TAG, "onResume(): " + printThis());

        onShow();
    }

    /*********************************
     * Paused
     *
     * 总结:
     * onHide()中做的事跟onPause()和onStop()中做的事相当一样.
     *
     * 1.
     * 在Fragment中打开另外一个Fragment(不管这个Fragment是不是被嵌套的)时,
     * onPause()和onStop()不会被调用.
     * 只会调用onHide().
     * 2.
     * 在Fragment中打开一个Activity,那么
     * onPause()和onStop()才会被调用.
     * 不会调用onHide().
     * 3.
     * 当前Fragment被pop后,先调用onHide(),
     * 然后是onPause(),onStop()等生命周期方法.
     * 4.
     * 因此在onPause()和onStop()中加了isHidden()判断后,
     * onPause(),onStop()这两个方法里的代码就不需要再次执行了.
     *********************************/

    @Override
    public void onPause() {
        super.onPause();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onPause(): " + printThis());
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
            MLog.d(TAG, "onStop(): " + printThis());
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView(): " + printThis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + printThis());

        FragOperManager.getInstance().removeFragment(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach(): " + printThis());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            MLog.d(TAG, "onSaveInstanceState(): " + printThis());
    }

    /***
     横竖屏切换改变布局,参照C2Fragment

     只能改变当前显示的Fragment的布局,
     没有显示的不会改变
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mCurConfiguration = newConfig;
        if (isHidden()
                || mOrientation == mCurConfiguration.orientation) {
            if (DEBUG)
                MLog.d(TAG, "onConfigurationChanged()" +
                        " return mOrientation: " + mOrientation +
                        " mCurConfiguration.mOrientation: " +
                        mCurConfiguration.orientation);
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onConfigurationChanged(): " + printThis() +
                    " newConfig: " + newConfig);

        handleConfigurationChangedEvent(newConfig, true, false);
        mOrientation = mCurConfiguration.orientation;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG)
            MLog.d(TAG, "onLowMemory(): " + printThis());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DEBUG)
            MLog.d(TAG, "onTrimMemory(): " + printThis() + " level: " + level);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (DEBUG)
            MLog.d(TAG, "onRequestPermissionsResult(): " + printThis() +
                    " requestCode: " + requestCode);

        PermissionsUtils.onRequestPermissionsResult(
                this,
                permissions,
                grantResults);
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (DEBUG)
            MLog.d(TAG, "onCreateAnimator(): " + printThis() +
                    " transit: " + transit + " enter: " + enter + " nextAnim: " + nextAnim);
        return super.onCreateAnimator(transit, enter, nextAnim);
    }

    /**
     * Very important
     * 子类必须重写这个方法,并调用
     * super.onHiddenChanged(hidden);
     * <p>
     * true表示被隐藏了,false表示被显示了
     * Fragment:
     * 被show()或者hide()时才会回调这个方法,
     * 被add()或者popBackStack()时不会回调这个方法
     * 弹窗时不会被回调(是由当前的Fragment弹出的一个DialogFragment)
     * 如果是弹出一个DialogActivity窗口,则应该会被回调,
     * 因为当前Fragment所在的Activity的生命周期发生了变化,
     * 则当前Fragment的生命周期也会发生变化.
     * <p>
     * 从BFragment返回到AFragment时,
     * AFragment的这个方法比onPause()要早执行.
     *
     * @param hidden if true that mean hidden
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (DEBUG)
            MLog.d(TAG, "onHiddenChanged(): " + printThis() + " hidden: " + hidden);

        if (hidden) {
            onHide();
        } else {
            onShow();
        }
    }

    // 写这个方法只是为了不直接调用onResume()方法
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + printThis());

        if (mCurConfiguration != null) {
            if (mOrientation == mCurConfiguration.orientation) {
                if (DEBUG)
                    // 不需要变换布局
                    MLog.d(TAG, "onShow() 1 " + printThis());
            } else {
                handleConfigurationChangedEvent(mCurConfiguration, false, false);
                mOrientation = mCurConfiguration.orientation;
                if (DEBUG)
                    MLog.d(TAG, "onShow() 2 " + printThis());
            }
        } else {
            if (DEBUG)
                // 第一次显示(不需要变换布局)
                MLog.d(TAG, "onShow() 3 " + printThis());
            if (mOrientation == Configuration.ORIENTATION_UNDEFINED) {
                Configuration configuration = getResources().getConfiguration();
                if (configuration != null) {
                    // 打开Fragment并且显示的时候记录屏幕方向
                    mOrientation = configuration.orientation;
                }
            }
        }

        if (DEBUG)
            MLog.d(TAG, "onShow() mOrientation: " + mOrientation);

        Integer[] container_scene =
                FragOperManager.getInstance().getActivityMap(mActivity);
        if (mBackHandlerInterface == null
                || container_scene == null) {
            return;
        }
        switch (container_scene[1]) {
            case FragOperManager.SCENE_NO_OR_ONE_MAIN_FRAGMENT:
                if (FragOperManager.getInstance().getParentFragmentsList(mActivity) == null
                        || FragOperManager.getInstance().getParentFragmentsList(mActivity)
                        .isEmpty()
                        || !FragOperManager.getInstance().getParentFragmentsList(mActivity)
                        .contains(this)) {
                    return;
                }
            case FragOperManager.SCENE_MORE_MAIN_FRAGMENT:
                if (FragOperManager.getInstance().isExitFragmentAtDirectChildFragments(this)) {
                    return;
                }
        }

        if (DEBUG)
            MLog.d(TAG, "onShow(): " + this.getClass().getName());
        // MainActivity.setSelectedFragment(...)
        mBackHandlerInterface.setSelectedFragment(
                this,
                this.getClass().getName());
    }

    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide(): " + printThis());

        if (getView() != null) {
            // 隐藏软键盘
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null
                    && inputMethodManager.isActive()) {
                inputMethodManager.hideSoftInputFromWindow(
                        getView().getWindowToken(), 0);
            }
        }
    }

    public Activity getAttachedActivity() {
        if (mActivity == null) {
            mActivity = getActivity();
        }
        if (mActivity == null) {
            throw new NullPointerException(
                    "BaseFragment getAttachedActivity() mActivity is null.");
        }
        return mActivity;
    }

    public Context getContext() {
        if (mContext == null) {
            if (getAttachedActivity() != null) {
                mContext = getAttachedActivity().getApplicationContext();
            }
        }
        if (mContext == null) {
            throw new NullPointerException(
                    "BaseFragment getContext() mContext is null.");
        }
        return mContext;
    }

    public BackHandlerInterface getBackHandlerInterface() {
        return mBackHandlerInterface;
    }

    protected abstract int provideLayout();

    /***
     * 所有继承BaseFragment的子类都将在这个方法中实现物理Back键按下后的逻辑
     * FragmentActivity捕捉到物理返回键点击事件后会首先询问Fragment是否消费该事件
     * 如果没有Fragment消息时FragmentActivity自己才会消费该事件
     * 除了像QQ那样有底部导航栏,并且是由Fragment组成的,那么这几个Fragment返回true外,
     * 其他的应该都返回false.
     *
     * 当子类返回true时,这种情况说明当前Fragment是一直依附在宿主Activity上的.
     */
    public abstract boolean onBackPressed();

    private LayoutInflater inflater;
    private ViewGroup container;
    private Bundle savedInstanceState;

    /***
     * 子类可能需要去实现
     * 子类覆写了这个方法,那么子类必须先调用父类这个方法,并且必须把override设为true.
     *
     * @param newConfig
     * @param needToDo
     * @param override true时表示子类已经覆写这个方法
     */
    protected void handleConfigurationChangedEvent(
            Configuration newConfig,
            boolean needToDo,
            boolean override) {
        if (override) {
            // 固定写法
            FrameLayout contentLayout = BaseActivity.getContentLayout(getAttachedActivity());
            if (contentLayout != null) {
                FragOperManager.getInstance().popDirectNestedFragments(this);
                // 得到放Fragment的容器布局
                FrameLayout fragmentContentLayout = contentLayout.findViewById(R.id.content_layout);
                fragmentContentLayout.removeView(getView());

                // 子类需要有onCreateView(...)方法
                View newView = onCreateView(inflater, container, savedInstanceState);
                setView(this, newView);
                // 子类可能需要有onViewCreated(...)方法
                // 在这个方法中做找控件,设置监听之类的事
                onViewCreated(newView, savedInstanceState);
                fragmentContentLayout.addView(newView);
            }
        }

        // needToDo为true时,需要为各种View重新赋值.见C2Fragment
    }

    protected static void setView(Fragment fragment, View newView) {
        try {
            Class clazz = Class.forName("android.app.Fragment");
            Field mView = clazz.getDeclaredField("mView");
            mView.setAccessible(true);
            mView.set(fragment, newView);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /***
     *
     * @param activity
     * @param isDark 想要状态栏中的内容能看到,需要设置为true
     */
    protected static void setStatusBar(Activity activity, boolean isDark) {
        try {
            if (activity == null) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 21) {
                Window window = activity.getWindow();
                if (window != null) {
                    View decorView = window.getDecorView();
                    if (decorView != null) {
                        // 使状态栏和导航栏都透明
                        int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                        decorView.setSystemUiVisibility(option);
                        window.setStatusBarColor(Color.TRANSPARENT);
                        window.setNavigationBarColor(Color.TRANSPARENT);

                        // 使得状态栏中的图标能够看清楚
                        option = decorView.getSystemUiVisibility();
                        if (isDark) {
                            option |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        } else {
                            option &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        }
                        decorView.setSystemUiVisibility(option);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String printThis() {
        // com.weidi.usefragments.MainActivity2@416c7b
        String temp = this.toString();
        int lastIndex = temp.lastIndexOf(".");
        temp = temp.substring(lastIndex + 1, temp.length());
        return temp;
    }

    /***
     代码备份:
     这个方法是在MainActivity中的.
     */
    /*@Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume()");
        *//***
     做这个事的原因:
     如果后退时Fragment是弹出的话,不需要这样的代码的;
     如果这些Fragment是像QQ那样实现的底部导航栏形式的,
     在任何一个页面都可以退出,那么也不需要实现这样的代码的.

     如果有多个Fragment开启着,并且相互之间是显示和隐藏,而不是弹出,
     那么页面在MainFragment时关闭屏幕,然后在点亮屏幕后,
     MainFragment的onResume()方法比其他Fragment的onResume()方法要先执行,
     最后执行的Fragment就得到了后退的"焦点",
     这样的话要后退时导致在MainFragment页面时就退不出去了.
     *//*
        if (this.mSavedInstanceState != null) {
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
        }
    }*/

}
