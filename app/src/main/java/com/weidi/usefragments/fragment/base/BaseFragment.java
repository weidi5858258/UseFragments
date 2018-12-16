package com.weidi.usefragments.fragment.base;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.weidi.usefragments.R;
import com.weidi.usefragments.fragment.FragOperManager;
import com.weidi.usefragments.inject.InjectUtils;
import com.weidi.usefragments.tool.MLog;


/***
 *
 */
public abstract class BaseFragment extends Fragment {

    private static final String TAG =
            BaseFragment.class.getSimpleName();

    private static final boolean DEBUG = false;
    private Context mContext;
    private BackHandlerInterface mBackHandlerInterface;

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity == null) {
            throw new NullPointerException("BaseFragment onAttach():activity is null.");
        }
        if (DEBUG)
            MLog.d(TAG, "onAttach() activity: " + activity);
        mContext = activity.getApplicationContext();
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
            MLog.d(TAG, "onCreate(): " + this
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
            MLog.d(TAG, "onCreateView(): " + this
                    + " savedInstanceState: " + savedInstanceState);
        // 如果写成inflater.inflate(provideLayout(), container)这样的话,
        // 那么会报异常,具体异常就是已经有一个子类的parent,添加之前先要移除这个parent.
        View view = null;
        if (savedInstanceState == null) {
            view = inflater.inflate(provideLayout(), null);
            InjectUtils.inject(this, view);
            afterInitView(inflater, container, savedInstanceState);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewCreated(): " + this
                    + " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewStateRestored(): " + this
                    + " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onActivityCreated(): " + this
                    + " savedInstanceState: " + savedInstanceState);
    }

    /*********************************
     * Started
     *********************************/

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            MLog.d(TAG, "onStart(): " + this);
    }

    /*********************************
     * Resumed
     *********************************/

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            MLog.d(TAG, "onResume(): " + this);
        onShow();
    }

    /*********************************
     * Paused
     *********************************/

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            MLog.d(TAG, "onPause(): " + this);
    }

    /*********************************
     * Stopped
     *********************************/

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            MLog.d(TAG, "onStop(): " + this);
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView(): " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy(): " + this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach(): " + this);
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
            MLog.d(TAG, "onHiddenChanged(): " + this + " hidden: " + hidden);
        if (hidden) {
            onHide();
        } else {
            onShow();
        }
    }

    // 写这个方法只是为了不直接调用onResume()方法
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow(): " + this);
        if (mBackHandlerInterface != null) {
            if (FragOperManager.getInstance().getDirectChildFragmentsMap() == null
                    || FragOperManager.getInstance().getDirectChildFragmentsMap().isEmpty()
                    || !FragOperManager.getInstance().getDirectChildFragmentsMap().containsKey(this)) {
                return;
            }
            if (DEBUG)
                MLog.d(TAG, "onShow(): " + this.getClass().getName());
            // MainActivity.setSelectedFragment(...)
            mBackHandlerInterface.setSelectedFragment(
                    this,
                    this.getClass().getName());
        }
    }

    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide(): " + this);
    }

    public Context getContext() {
        if (mContext == null) {
            if (getActivity() != null) {
                mContext = getActivity().getApplicationContext();
            }
        }
        return mContext;
    }

    public BackHandlerInterface getBackHandlerInterface() {
        return mBackHandlerInterface;
    }

    protected abstract int provideLayout();

    /**
     * 供子类调用，初始化组件，统一接口
     *
     * @return
     */
    //    protected abstract void initViewBefore(Bundle savedInstanceState);

    /***
     * 供子类调用，初始化数据，统一接口
     *
     * @return
     */
    protected abstract void afterInitView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState);

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

    /**
     * 打开页面时，页面从右往左滑入
     * 底下的页面不需要有动画
     */
    public void startFragmentAnim() {
        try {
            getActivity().overridePendingTransition(
                    R.anim.push_left_in, R.anim.push_left_out);
        } catch (Exception e) {
        }
    }

    /**
     * 关闭页面时，页面从左往右滑出
     */
    public void finishFragmentAnim() {
        try {
            getActivity().overridePendingTransition(
                    R.anim.push_right_in, R.anim.push_right_out);
        } catch (Exception e) {
        }
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
