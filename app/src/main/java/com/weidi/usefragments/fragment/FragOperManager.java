package com.weidi.usefragments.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.text.TextUtils;

import com.weidi.usefragments.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentActivity;
//import android.support.v4.app.FragmentManager;
//import android.support.v4.app.FragmentTransaction;
/***
 * fragment在add,replace,hide,show时会调用哪些生命周期方法
 * <p>
 * class desc: Fragment操作类
 * 替换时删除id相同的fragment然后添加，只有一层，添加是多层
 * 对于fragment的使用基本有两种，
 * 一种是add方式后再进行show或者hide，
 * 这种方式切换fragment时不会让fragment重新刷新，
 * 而用replace方式会使fragment重新刷新，
 * 因为add方式是将fragment隐藏了而不是销毁再创建，
 * replace方式每次都是重新创建。
 */

/***
 * Fragment操作类
 * 1、有时候，我们需要在多个Fragment间切换，
 * 并且保存每个Fragment的状态。官方的方法是使用
 * replace()来替换Fragment，但是replace()的调用
 * 会导致Fragment的onCreteView()被调用，所以切换
 * 界面时会无法保存当前的状态。因此一般采用add()、hide()与show()配合，
 * 来达到保存Fragment的状态。
 * 2、第二个问题的出现正是因为使用了Fragment的状态保存，当系统内存不足，
 * Fragment的宿主Activity回收的时候，
 * Fragment的实例并没有随之被回收。
 * Activity被系统回收时，会主动调用onSaveInstance()
 * 方法来保存视图层（View Hierarchy），
 * 所以当Activity通过导航再次被重建时，
 * 之前被实例化过的Fragment依然会出现在Activity中，
 * 然而从上述代码中可以明显看出，再次重建了新的Fragment，
 * 综上这些因素导致了多个Fragment重叠在一起。
 * <p>
 * 在onSaveInstance()里面去remove()所有非空的Fragment，然后在onRestoreInstanceState()
 * 中去再次按照问题一的方式创建Activity。当我处于打开“不保留活动”的时候，效果非常令人满意，
 * 然而当我关闭“不保留活动”的时候，问题却出现了。当转跳到其他Activity
 * 、打开多任务窗口、使用Home回到主屏幕再返回时，发现根本没有Fragment了，一篇空白。
 * <p>
 * 于是跟踪下去，我调查了onSaveInstanceState()与onRestoreInstanceState()
 * 这两个方法。原本以为只有在系统因为内存回收Activity时才会调用的onSaveInstanceState()
 * ，居然在转跳到其他Activity、打开多任务窗口、使用Home回到主屏幕这些操作中也被调用，
 * 然而onRestoreInstanceState()
 * 并没有在再次回到Activity时被调用。而且我在onResume()发现之前的Fragment只是被移除，
 * 并不是空，所以就算你在onResume()
 * 中执行问题一中创建的Fragment的方法，同样无济于事。所以通过remove()宣告失败。
 * <p>
 * 接着通过调查资料发现Activity中的onSaveInstanceState()里面有一句
 * super.onRestoreInstanceState(savedInstanceState)，
 * Google对于这句话的解释是
 * “Always call the superclass so it can save the view hierarchystate”，
 * 大概意思是“总是执行这句代码来调用父类去保存视图层的状态”。
 * 其实到这里大家也就明白了，就是因为这句话导致了重影的出现，于是我删除了这句话，然后onCreate()
 * 与onRestoreInstanceState()中同时使用问题一中的创建Fragment方法，
 * 然后再通过保存切换的状态，发现结果非常完美。
 * <p>
 * 只能在v4包中才能使用
 * fTransaction.setCustomAnimations(R.anim.push_left_in, R.anim.push_left_out);
 */
public class FragOperManager implements Serializable {

    private static final String TAG =
            FragOperManager.class.getSimpleName();

    public static final int HIDE = 0;
    public static final int POP_BACK_STACK = 1;
    public static final int POP_BACK_STACK_ALL = 2;
    public static final int REMOVE_SOMEONE_FRAGMENT = 3;

    /***
     SCENE_NO_MAIN_FRAGMENT
     和
     SCENE_ONE_MAIN_FRAGMENT
     这两个场景没有区别.
     */
    public static final int SCENE_NO_MAIN_FRAGMENT = 100;
    public static final int SCENE_ONE_MAIN_FRAGMENT = 101;
    public static final int SCENE_MORE_MAIN_FRAGMENT = 102;

    private int mCurrentUsedScene = SCENE_NO_MAIN_FRAGMENT;

    private volatile static FragOperManager sFragOperManager;

    // 一个Activity对应存放Fragment的布局文件
    private Map<Activity, Integer> mContainerMap;
    // List<Fragment>有show, hide, pop
    private Map<Activity, List<Fragment>> mParentFragmentsMap;
    // SCENE_MORE_MAIN_FRAGMENT这个场景专用的数据结构
    private Map<Fragment, List<Fragment>> mMainFragmentsMap;
    // List<Fragment>有show, hide, 没有pop 这里的key是mParentFragmentsMap中value值的其中之一
    private Map<Fragment, List<Fragment>> mDirectChildFragmentsMap;
    // List<Fragment>有show, hide, 没有pop
    private Map<Fragment, List<Fragment>> mIndirectChildFragmentsMap;
    // 只记录总的Fragment数量
    //private List<Fragment> mAllFragmentsList;

    private FragOperManager() {
        mContainerMap = new HashMap<Activity, Integer>();
        mParentFragmentsMap = new HashMap<Activity, List<Fragment>>();
        mDirectChildFragmentsMap = new HashMap<Fragment, List<Fragment>>();
        mIndirectChildFragmentsMap = new HashMap<Fragment, List<Fragment>>();
        //mAllFragmentsList = new ArrayList<Fragment>();
    }

    public static FragOperManager getInstance() {
        if (sFragOperManager == null) {
            synchronized (FragOperManager.class) {
                if (sFragOperManager == null) {
                    sFragOperManager = new FragOperManager();
                }
            }
        }
        return sFragOperManager;
    }

    /***
     * 添加activity后,表示要从这个activity开启fragment.
     *
     * @param activity
     * @param containerId
     */
    public void addActivity(Activity activity, int containerId) {
        if (activity == null
                || containerId <= 0
                || mContainerMap == null
                || mParentFragmentsMap == null) {
            return;
        }

        if (!mContainerMap.containsKey(activity)) {
            mContainerMap.put(activity, containerId);
        }
        if (!mParentFragmentsMap.containsKey(activity)) {
            List<Fragment> fragmentsList = new ArrayList<Fragment>();
            mParentFragmentsMap.put(activity, fragmentsList);
        }
    }

    public void addActivity(Activity activity, int containerId, int scene) {
        if (activity == null
                || containerId <= 0
                || mContainerMap == null
                || mParentFragmentsMap == null) {
            return;
        }

        if (!mContainerMap.containsKey(activity)) {
            mContainerMap.put(activity, containerId);
        }
        if (!mParentFragmentsMap.containsKey(activity)) {
            List<Fragment> fragmentsList = new ArrayList<Fragment>();
            mParentFragmentsMap.put(activity, fragmentsList);
        }
    }

    public void removeActivity(Activity activity) {
        if (activity == null
                || mContainerMap == null
                || mContainerMap.isEmpty()) {
            return;
        }

        if (mContainerMap.containsKey(activity)) {
            mContainerMap.remove(activity);
        }
        if (mParentFragmentsMap.containsKey(activity)) {
            mParentFragmentsMap.remove(activity);
        }
    }

    /*public List<Fragment> getFragmentsList() {
        return mAllFragmentsList;
    }*/

    public List<Fragment> getParentFragmentsList(Activity activity) {
        if (activity == null
                || mParentFragmentsMap == null
                || mParentFragmentsMap.isEmpty()
                || !mParentFragmentsMap.containsKey(activity)) {
            return null;
        }

        return mParentFragmentsMap.get(activity);
    }

    public List<Fragment> getDirectChildFragmentsList(Fragment fragment) {
        if (fragment == null
                || mDirectChildFragmentsMap == null
                || mDirectChildFragmentsMap.isEmpty()
                || !mDirectChildFragmentsMap.containsKey(fragment)) {
            return null;
        }

        return mDirectChildFragmentsMap.get(fragment);
    }

    public Map<Fragment, List<Fragment>> getDirectChildFragmentsMap() {
        return mDirectChildFragmentsMap;
    }

    /**
     * @param activity
     * @param fragment
     * @param tag      不要为null,需要使用到这个tag.
     */
    public int enter(Activity activity,
                     Fragment fragment,
                     String tag) {
        if (activity == null
                || fragment == null
                || TextUtils.isEmpty(tag)
                || mContainerMap == null
                || mContainerMap.isEmpty()
                || mParentFragmentsMap == null
                || mParentFragmentsMap.isEmpty()
                || mDirectChildFragmentsMap == null) {
            return -1;
        }

        if (!mContainerMap.containsKey(activity)
                || !mParentFragmentsMap.containsKey(activity)) {
            return -1;
        }

        int containerId = mContainerMap.get(activity);
        List<Fragment> fragmentsList = mParentFragmentsMap.get(activity);
        if (fragmentsList == null) {
            return -1;
        }
        if (!mDirectChildFragmentsMap.containsKey(fragment)) {
            List<Fragment> directFragmentsList = new ArrayList<Fragment>();
            mDirectChildFragmentsMap.put(fragment, directFragmentsList);
        }
        FragmentTransaction fTransaction = activity.getFragmentManager().beginTransaction();
        // 保证fragment在最后一个
        if (!fragmentsList.contains(fragment)) {
            fragmentsList.add(fragment);
            // 不用replace
            fTransaction.add(containerId, fragment, tag);
            fTransaction.addToBackStack(tag);
        } else {
            fragmentsList.remove(fragment);
            fragmentsList.add(fragment);
        }

        int count = fragmentsList.size();
        for (int i = 0; i < count - 1; i++) {
            Fragment hideFragment = fragmentsList.get(i);
            if (!hideFragment.isHidden()) {
                List<Fragment> directFragmentsList = mDirectChildFragmentsMap.get(hideFragment);
                if (directFragmentsList != null
                        && !directFragmentsList.isEmpty()) {
                    for (Fragment tempFragment : directFragmentsList) {
                        if (!tempFragment.isHidden()) {
                            fTransaction.hide(tempFragment);
                        }
                    }
                }
                // fragment隐藏时的动画
                // fTransaction.setCustomAnimations(R.anim.push_right_in, R.anim.push_left_out2);
                // 先把所有的Fragment给隐藏掉.
                fTransaction.hide(hideFragment);
            }
        }

        showFragmentUseAnimations(fTransaction);
        fTransaction.show(fragment);
        // 旋转屏幕,然后去添加一个Fragment,出现异常
        // 旋转屏幕后
        // java.lang.IllegalStateException:
        // Can not perform this action after onSaveInstanceState
        fTransaction.commit();

        /*if (mAllFragmentsList != null
                && !mAllFragmentsList.contains(fragment)) {
            mAllFragmentsList.add(fragment);
        }*/
        return 0;
    }

    /**
     * 在一个Fragment中的一个小区域再添加一个小的Fragment.
     * 不过,添加之前,先要移除掉之前的Fragment.
     * 就是先调用一下
     * removeSomeOneFragment(String fragmentTag)方法.
     * 使用EventBusUtils发送消息进行调用.
     * <p>
     * 针对两种情况:(需要测试)
     * 1.对于宿主Fragment来说,Back后POP_BACK_STACK
     * 2.对于宿主Fragment来说,Back后HIDE
     * <p>
     * 此种情况经测试发现当宿主Fragment进行show,hide或者pop后,
     * 自己的生命周期没有发生变化,因此这样子不太好,需要改进,
     * 使其也有生命周期(创建时是有生命周期的).
     *
     * @param activity
     * @param parentFragment
     * @param childFragment
     * @param tag
     * @param containerId
     */
    public int enter(Activity activity,
                     Fragment parentFragment,
                     Fragment childFragment,
                     String tag,
                     int containerId) {
        if (activity == null
                || parentFragment == null
                || childFragment == null
                || TextUtils.isEmpty(tag)
                || containerId <= 0
                || mContainerMap == null
                || mContainerMap.isEmpty()
                || mParentFragmentsMap == null
                || mParentFragmentsMap.isEmpty()
                || mDirectChildFragmentsMap == null) {
            return -1;
        }

        if (!mContainerMap.containsKey(activity)
                || !mParentFragmentsMap.containsKey(activity)
                || !mDirectChildFragmentsMap.containsKey(parentFragment)) {
            return -1;
        }

        List<Fragment> fragmentsList = mParentFragmentsMap.get(activity);
        if (fragmentsList == null
                || !fragmentsList.contains(parentFragment)) {
            return -1;
        }

        List<Fragment> directFragmentsList = mDirectChildFragmentsMap.get(parentFragment);
        if (directFragmentsList == null) {
            return -1;
        }
        FragmentTransaction fTransaction = activity.getFragmentManager().beginTransaction();
        if (!directFragmentsList.contains(childFragment)) {
            directFragmentsList.add(childFragment);
            fTransaction.add(containerId, childFragment, tag);
            fTransaction.addToBackStack(tag);

            showFragmentUseAnimations(fTransaction);
            fTransaction.show(childFragment);
        }
        fTransaction.commit();

        /*if (mAllFragmentsList != null
                && !mAllFragmentsList.contains(parentFragment)) {
            mAllFragmentsList.add(parentFragment);
        }
        if (mAllFragmentsList != null
                && !mAllFragmentsList.contains(childFragment)) {
            mAllFragmentsList.add(childFragment);
        }*/
        return 0;
    }

    /***
     * 也是在Fragment中嵌套Fragment.
     * 只是不需要提供布局文件,它只是用来执行任务,
     * 不需要在显示出来,生命周期跟其他显示出来的Fragment
     * 是一样的.
     * @param activity
     * @param parentFragment
     * @param childFragment
     * @param tag
     * @return
     */
    public int enter(Activity activity,
                     Fragment parentFragment,
                     Fragment childFragment,
                     String tag) {
        if (activity == null
                || parentFragment == null
                || childFragment == null
                || TextUtils.isEmpty(tag)
                || mContainerMap == null
                || mContainerMap.isEmpty()
                || mParentFragmentsMap == null
                || mParentFragmentsMap.isEmpty()
                || mDirectChildFragmentsMap == null) {
            return -1;
        }

        if (!mContainerMap.containsKey(activity)
                || !mParentFragmentsMap.containsKey(activity)
                || !mDirectChildFragmentsMap.containsKey(parentFragment)) {
            return -1;
        }

        List<Fragment> fragmentsList = mParentFragmentsMap.get(activity);
        if (fragmentsList == null
                || !fragmentsList.contains(parentFragment)) {
            return -1;
        }

        List<Fragment> directFragmentsList = mDirectChildFragmentsMap.get(parentFragment);
        if (directFragmentsList == null) {
            return -1;
        }
        FragmentTransaction fTransaction = activity.getFragmentManager().beginTransaction();
        if (!directFragmentsList.contains(childFragment)) {
            directFragmentsList.add(childFragment);
            fTransaction.add(childFragment, tag);
            fTransaction.addToBackStack(tag);

            showFragmentUseAnimations(fTransaction);
            fTransaction.show(childFragment);
        }
        fTransaction.commit();

        /*if (mAllFragmentsList != null
                && !mAllFragmentsList.contains(parentFragment)) {
            mAllFragmentsList.add(parentFragment);
        }
        if (mAllFragmentsList != null
                && !mAllFragmentsList.contains(childFragment)) {
            mAllFragmentsList.add(childFragment);
        }*/
        return 0;
    }

    /***
     * 如果退出Fragment时打算隐藏那么就传HIDE;
     * 如果退出Fragment时弹出后退栈那么就传POP_BACK_STACK.
     * 如果退出时是隐藏的,那么在进入这个Fragment时它的对象不能再次new,只能new一次
     *  @param what
     * @param object
     */
    public Object onEvent(int what, Object[] object) {
        switch (what) {
            case HIDE:
                if (object != null && object.length >= 2) {
                    // 隐藏某个Fragment,而不是弹出.
                    what = exit((Activity) object[0], (Fragment) object[1], HIDE);
                }
                break;

            case POP_BACK_STACK:
                if (object != null && object.length >= 2) {
                    // 弹出某个Fragment,而不是隐藏.
                    what = exit((Activity) object[0], (Fragment) object[1], POP_BACK_STACK);
                }
                break;

            case POP_BACK_STACK_ALL:
                // 在某个Fragment时出现了某种情况,应用需要退出,那么需要先把所有的Fragment给移除掉.
                //popBackStackAll();
                break;

            case REMOVE_SOMEONE_FRAGMENT:
                if (object != null && object.length >= 2) {
                    //removeSomeOneFragment((Activity) object[0], (String) object[1]);
                }
                break;

            default:
        }
        return what;
    }

    /**
     * @param activity
     * @param fragment
     * @param exitType
     */
    private int exit(Activity activity, Fragment fragment, int exitType) {
        if (activity == null
                || fragment == null
                || mContainerMap == null
                || mContainerMap.isEmpty()
                || mParentFragmentsMap == null
                || mParentFragmentsMap.isEmpty()
                || mDirectChildFragmentsMap == null) {
            return -1;
        }

        if (!mContainerMap.containsKey(activity)
                || !mParentFragmentsMap.containsKey(activity)) {
            return -1;
        }

        List<Fragment> parentFragmentsList = mParentFragmentsMap.get(activity);
        if (parentFragmentsList == null
                || !parentFragmentsList.contains(fragment)) {
            return -1;
        }

        List<Fragment> directFragmentsList = mDirectChildFragmentsMap.get(fragment);
        if (directFragmentsList == null) {
            return -1;
        }

        FragmentManager fManager = activity.getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        // 不需要先加载一个Fragment
        switch (exitType) {
            case HIDE:
                if (!fragment.isHidden()) {
                    if (!directFragmentsList.isEmpty()) {
                        for (Fragment tempFragment : directFragmentsList) {
                            if (!tempFragment.isHidden()) {
                                fTransaction.hide(tempFragment);
                            }
                        }
                    }
                    fTransaction.hide(fragment);
                }
                int count = parentFragmentsList.size();
                if (count <= 1) {
                    break;
                }
                Fragment showFragment = parentFragmentsList.get(count - 1);
                directFragmentsList = mDirectChildFragmentsMap.get(showFragment);
                if (directFragmentsList != null && !directFragmentsList.isEmpty()) {
                    for (Fragment tempFragment : directFragmentsList) {
                        if (tempFragment.isHidden()) {
                            fTransaction.show(tempFragment);
                        }
                    }
                }
                showFragmentUseAnimations(fTransaction);
                fTransaction.show(showFragment);
                parentFragmentsList.remove(fragment);
                parentFragmentsList.add(0, fragment);
                break;

            case POP_BACK_STACK:
                if (!directFragmentsList.isEmpty()) {
                    for (Fragment tempFragment : directFragmentsList) {
                        if (!tempFragment.isHidden()) {
                            fManager.popBackStack();
                        }
                    }
                }
                fManager.popBackStack();
                parentFragmentsList.remove(fragment);
                mDirectChildFragmentsMap.remove(fragment);
                count = parentFragmentsList.size();
                if (count < 1) {
                    break;
                }
                for (int i = 0; i < count; i++) {
                    Fragment hideFragment = parentFragmentsList.get(i);
                    if (!hideFragment.isHidden()) {
                        directFragmentsList = mDirectChildFragmentsMap.get(hideFragment);
                        if (directFragmentsList != null
                                && !directFragmentsList.isEmpty()) {
                            for (Fragment tempFragment : directFragmentsList) {
                                if (!tempFragment.isHidden()) {
                                    fTransaction.hide(tempFragment);
                                }
                            }
                        }
                        fTransaction.hide(hideFragment);
                    }
                }
                showFragment = parentFragmentsList.get(count - 1);
                directFragmentsList = mDirectChildFragmentsMap.get(showFragment);
                if (directFragmentsList != null
                        && !directFragmentsList.isEmpty()) {
                    for (Fragment tempFragment : directFragmentsList) {
                        if (tempFragment.isHidden()) {
                            fTransaction.show(tempFragment);
                        }
                    }
                }
                if (showFragment.isHidden()) {
                    showFragmentUseAnimations(fTransaction);
                    fTransaction.show(showFragment);
                }
                break;

            default:
        }
        fTransaction.commit();

        return 0;
    }

    /*private void popBackStackAll() {
        if (mAllFragmentsList == null || mAllFragmentsList.isEmpty()) {
            return;
        }
        Iterator<Map<Activity, Integer>> iterator = mContainerMapList.iterator();
        while (iterator.hasNext()) {
            Map<Activity, Integer> map = iterator.next();
            for (Map.Entry<Activity, Integer> entry : map.entrySet()) {
                FragmentTransaction fTransaction =
                        entry.getKey().getFragmentManager().beginTransaction();
                Iterator<Fragment> iter = mAllFragmentsList.iterator();
                while (iter.hasNext()) {
                    Fragment fragment = iter.next();
                    fTransaction.remove(fragment);
                    iter.remove();
                }
                fTransaction.commit();
            }
        }
    }*/

    // fragment显示时的动画
    private void showFragmentUseAnimations(FragmentTransaction fTransaction) {
        fTransaction.setCustomAnimations(
                R.animator.push_left_in,
                R.animator.push_left_out);
    }

    @SuppressLint("ResourceType")
    private void popFragmentUseAnimations(FragmentTransaction fTransaction) {
        fTransaction.setCustomAnimations(
                R.animator.card_flip_right_in,
                R.animator.card_flip_left_out,
                R.animator.card_flip_left_in,
                R.animator.card_flip_right_out);
    }

    /***
     测试的几种情况:
     一.
     横竖屏时MainActivity的生命周期不发生重建.
     1.
     只有一个MainActivity,其他都是Fragment,Fragment中不嵌套Fragment,
     只是开启其他的Fragment.
     MainActivity中也不先加载一个MainFragment.
     按"Back"后每个Fragment都是pop.
     测试想要的结果:
     能够正常打开新的Fragment并显示没有问题,按"Back"后pop当前Fragment
     并显示上一个Fragment.当所有的Fragment都pop后,按"Back"才退出
     MainActivity.横竖屏时Fragment的生命周期不发生重建.
     按"Home"退出应用然后再次进入,看是否正常.

     2.
     只有一个MainActivity,其他都是Fragment,Fragment中不嵌套Fragment.
     只是开启其他的Fragment.
     MainActivity中预先加载一个MainFragment,然后从MainFragment开启
     其他Fragment.MainFragment始终依附于MainActivity,
     也就是说MainFragment退出时表示MainActivity退出.
     除了MainFragment,其他Fragment按"Back"后都是pop.
     测试想要的结果:
     能够正常打开新的Fragment并显示没有问题,按"Back"后pop当前Fragment
     并显示上一个Fragment.除了MainFragment,其他的Fragment都pop后,按"Back"退出
     MainFragment和MainActivity.横竖屏时Fragment的生命周期不发生重建.
     按"Home"退出应用然后再次进入,看是否正常.

     3.
     只有一个MainActivity,其他都是Fragment,Fragment中嵌套Fragment.
     MainActivity中也不先加载一个MainFragment.
     按"Back"后每个Fragment都是pop,被嵌套的Fragment当其宿主Fragment
     退出时退出,其宿主Fragment的生命周期不发生重建,那么被嵌套的Fragment
     的生命周期也不发生重建.不对这些被嵌套的Fragment进行show或者hide.
     按"Home"退出应用然后再次进入,看是否正常.



     二.
     横竖屏时MainActivity的生命周期发生重建.

     三.
     锁屏亮屏

     四.
     按"Home"键退出再进去


     */
}