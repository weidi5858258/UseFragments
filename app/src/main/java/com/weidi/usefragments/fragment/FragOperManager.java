package com.weidi.usefragments.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.text.TextUtils;

import com.weidi.usefragments.BaseActivity;
import com.weidi.usefragments.R;
import com.weidi.usefragments.tool.MLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private static final boolean DEBUG = true;

    public static final int HIDE = 0;
    public static final int POP_BACK_STACK = 1;
    public static final int POP_BACK_STACK_ALL = 2;
    public static final int REMOVE_SOMEONE_FRAGMENT = 3;

    /***
     */
    public static final int SCENE_NO_OR_ONE_MAIN_FRAGMENT = 100;
    public static final int SCENE_MORE_MAIN_FRAGMENT = 101;

    private volatile static FragOperManager sFragOperManager;

    // 当前正在使用的Activity
    private Activity mCurShowActivity;
    // 一个Activity对应存放Fragment的布局文件
    private Map<Activity, Integer[]> mActivityContainersMap;

    // List<Fragment>有show, hide, pop
    // 在SCENE_NO_OR_ONE_MAIN_FRAGMENT这个场景使用
    private Map<Activity, List<Fragment>> mActivityFragmentsMap;

    // 在SCENE_MORE_MAIN_FRAGMENT这个场景使用
    private Map<Fragment, List<Fragment>> mMoreMainFragmentsMap;
    private Fragment mCurShowMainFragment;

    // List<Fragment>有show, hide, 没有pop
    // 这里的key是mParentFragmentsMap中value值的其中之一
    private Map<Fragment, List<Fragment>> mDirectNestedFragmentsMap;

    // List<Fragment>有show, hide, 没有pop
    private Map<Fragment, List<Fragment>> mIndirectNestedFragmentsMap;
    // 只记录总的Fragment数量
    //private List<Fragment> mAllFragmentsList;

    private FragOperManager() {
        mActivityContainersMap = new LinkedHashMap<Activity, Integer[]>();
        mActivityFragmentsMap = new LinkedHashMap<Activity, List<Fragment>>();
        mDirectNestedFragmentsMap = new LinkedHashMap<Fragment, List<Fragment>>();
        mMoreMainFragmentsMap = new LinkedHashMap<Fragment, List<Fragment>>();

        //mIndirectNestedFragmentsMap = new HashMap<Fragment, List<Fragment>>();
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
     * 在BaseActivity中的onResume()方法中调用
     * @param activity
     */
    public void setCurShowActivity(Activity activity) {
        mCurShowActivity = activity;
    }

    /***
     * 在SCENE_MORE_MAIN_FRAGMENT这个场景使用,切换MainFragment时调用
     * 在SCENE_NO_OR_ONE_MAIN_FRAGMENT这个场景不需要调用
     * @param fragment
     */
    public void setCurShowFragment(Fragment fragment) {
        mCurShowMainFragment = fragment;
    }

    /***
     * 1.
     * 在要使用Fragment的Activity的
     * onCreate(Bundle savedInstanceState)方法中调用
     * 2.
     * 添加activity后,表示要从这个activity开启fragment
     *
     * @param activity
     * @param containerId
     */
    public void addActivity(Activity activity, int containerId) {
        if (activity == null
                || containerId <= 0
                || mActivityContainersMap == null
                || mActivityFragmentsMap == null) {
            return;
        }

        mCurShowActivity = activity;
        if (!mActivityContainersMap.containsKey(activity)) {
            Integer[] container_scene = new Integer[2];
            container_scene[0] = containerId;
            container_scene[1] = SCENE_NO_OR_ONE_MAIN_FRAGMENT;
            mActivityContainersMap.put(activity, container_scene);
        }
        if (!mActivityFragmentsMap.containsKey(activity)) {
            List<Fragment> fragmentsList = new ArrayList<Fragment>();
            mActivityFragmentsMap.put(activity, fragmentsList);
        }
    }

    /***
     * 在要使用Fragment的Activity的onDestroy()方法中调用
     * @param activity
     */
    public void removeActivity(Activity activity) {
        if (activity == null
                || mActivityContainersMap == null) {
            return;
        }

        if (mActivityContainersMap.containsKey(activity)) {
            mActivityContainersMap.remove(activity);
        }
        if (mActivityFragmentsMap != null
                && mActivityFragmentsMap.containsKey(activity)) {
            mActivityFragmentsMap.remove(activity);
        }
    }

    public void removeFragment(Fragment mainFragment) {
        if (mainFragment == null
                || mMoreMainFragmentsMap == null
                || mMoreMainFragmentsMap.isEmpty()
                || !mMoreMainFragmentsMap.containsKey(mainFragment)) {
            return;
        }

        if (DEBUG)
            MLog.d(TAG, "removeFragment() mainFragment: " +
                    mainFragment.getClass().getSimpleName());
        mMoreMainFragmentsMap.remove(mainFragment);
    }

    public Integer[] getActivityMap(Activity activity) {
        if (activity == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(activity)) {
            return null;
        }

        return mActivityContainersMap.get(activity);
    }

    public List<Fragment> getParentFragmentsList(Activity activity) {
        if (activity == null
                || mActivityFragmentsMap == null
                || mActivityFragmentsMap.isEmpty()
                || !mActivityFragmentsMap.containsKey(activity)) {
            return null;
        }

        return mActivityFragmentsMap.get(activity);
    }

    public List<Fragment> getDirectChildFragmentsList(Fragment fragment) {
        if (fragment == null
                || mDirectNestedFragmentsMap == null
                || mDirectNestedFragmentsMap.isEmpty()
                || !mDirectNestedFragmentsMap.containsKey(fragment)) {
            return null;
        }

        return mDirectNestedFragmentsMap.get(fragment);
    }

    public Map<Fragment, List<Fragment>> getDirectChildFragmentsMap() {
        return mDirectNestedFragmentsMap;
    }

    public boolean isExitFragmentAtDirectChildFragments(Fragment fragment) {
        if (fragment == null
                || mDirectNestedFragmentsMap == null
                || mDirectNestedFragmentsMap.isEmpty()) {
            return false;
        }

        for (Fragment parentFragment : mDirectNestedFragmentsMap.keySet()) {
            if (mDirectNestedFragmentsMap.get(parentFragment) != null
                    && mDirectNestedFragmentsMap.get(parentFragment).contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    public boolean isExitFragmentAtMoreMainFragments(Fragment fragment) {
        if (fragment == null
                || mMoreMainFragmentsMap == null
                || mMoreMainFragmentsMap.isEmpty()) {
            return false;
        }

        for (Fragment mainFragment : mMoreMainFragmentsMap.keySet()) {
            if (mMoreMainFragmentsMap.get(mainFragment) != null
                    && mMoreMainFragmentsMap.get(mainFragment).contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    public List<Fragment> getMoreMainFragmentsMap(String mainFragmentTag) {
        if (TextUtils.isEmpty(mainFragmentTag)
                || mMoreMainFragmentsMap == null
                || mMoreMainFragmentsMap.isEmpty()) {
            return null;
        }

        for (Fragment mainFragment : mMoreMainFragmentsMap.keySet()) {
            if (mainFragment.getClass().getSimpleName().equals(mainFragmentTag)) {
                return mMoreMainFragmentsMap.get(mainFragment);
            }
        }
        return null;
    }

    /***
     * 在SCENE_NO_OR_ONE_MAIN_FRAGMENT这个场景使用
     * @param fragment
     */
    public int enter(Fragment fragment) {
        if (fragment == null
                || mCurShowActivity == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(mCurShowActivity)
                || mActivityFragmentsMap == null
                || mActivityFragmentsMap.isEmpty()
                || !mActivityFragmentsMap.containsKey(mCurShowActivity)) {
            return -1;
        }

        Integer[] container_scene = mActivityContainersMap.get(mCurShowActivity);
        List<Fragment> fragmentsList = mActivityFragmentsMap.get(mCurShowActivity);
        if (container_scene == null
                || container_scene.length != 2
                || fragmentsList == null) {
            return -1;
        }

        FragmentTransaction fTransaction =
                mCurShowActivity.getFragmentManager().beginTransaction();
        // 保证fragment在最后一个
        if (!fragmentsList.contains(fragment)) {
            fragmentsList.add(fragment);
            // 不用replace
            fTransaction.add(container_scene[0], fragment, fragment.getClass().getSimpleName());
            fTransaction.addToBackStack(fragment.getClass().getSimpleName());
        } else {
            // 这种情况一般不会出现
            fragmentsList.remove(fragment);
            fragmentsList.add(fragment);
        }

        int count = fragmentsList.size();
        for (int i = 0; i < count - 1; i++) {
            // i = count - 1是刚刚add的fragment
            Fragment shouldHideFragment = fragmentsList.get(i);
            if (!shouldHideFragment.isHidden()) {
                List<Fragment> directFragmentsList =
                        mDirectNestedFragmentsMap.get(shouldHideFragment);
                if (directFragmentsList != null
                        && !directFragmentsList.isEmpty()) {
                    FragmentManager fragmentManager =
                            shouldHideFragment.getChildFragmentManager();
                    FragmentTransaction fragmentTransaction =
                            fragmentManager.beginTransaction();
                    Iterator<Fragment> iterator = directFragmentsList.iterator();
                    while (iterator.hasNext()) {
                        Fragment directChildFragment = iterator.next();
                        if (DEBUG)
                            MLog.d(TAG, "enter() directChildFragment.getId(): " +
                                    directChildFragment.getId());
                        if (!directChildFragment.isHidden()) {
                            if (directChildFragment.getId() <= 0) {
                                // 没有布局文件可以显示的Fragment
                                fTransaction.hide(directChildFragment);
                            } else {
                                fragmentManager.popBackStack();
                                iterator.remove();
                            }
                        }
                    }
                    fragmentTransaction.commit();
                }
                // fragment隐藏时的动画
                // fTransaction.setCustomAnimations(R.anim.push_right_in, R.anim.push_left_out2);
                // 先把所有的Fragment给隐藏掉.
                fTransaction.hide(shouldHideFragment);
            }
        }
        /*for (int i = 0; i < count - 1; i++) {
            // i = count - 1是刚刚add的fragment
            Fragment shouldHideFragment = fragmentsList.get(i);
            if (!shouldHideFragment.isHidden()) {
                List<Fragment> directFragmentsList =
                        mDirectNestedFragmentsMap.get(shouldHideFragment);
                if (directFragmentsList != null
                        && !directFragmentsList.isEmpty()) {
                    for (Fragment directChildFragment : directFragmentsList) {
                        if (!directChildFragment.isHidden()) {
                            fTransaction.hide(directChildFragment);
                        }
                    }
                }
                // fragment隐藏时的动画
                // fTransaction.setCustomAnimations(R.anim.push_right_in, R.anim.push_left_out2);
                // 先把所有的Fragment给隐藏掉.
                if (!shouldHideFragment.isHidden()) {
                    fTransaction.hide(shouldHideFragment);
                }
            }
        }*/

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

    /***
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
     * @param parentFragment
     * @param childFragment
     * @param containerId
     */
    public int enter(Fragment parentFragment,
                     Fragment childFragment,
                     int containerId) {
        if (parentFragment == null
                || childFragment == null
                || mCurShowActivity == null
                || containerId <= 0
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(mCurShowActivity)
                || mDirectNestedFragmentsMap == null) {
            return -1;
        }

        if (DEBUG)
            MLog.d(TAG, "enter() mCurShowActivity: " +
                    mCurShowActivity.getClass().getSimpleName() +
                    " parentFragment: " + parentFragment.getClass().getSimpleName() +
                    " childFragment: " + childFragment.getClass().getSimpleName() +
                    " containerId: " + containerId);

        /*List<Fragment> fragmentsList = mActivityFragmentsMap.get(mCurShowActivity);
        if (fragmentsList == null
                || fragmentsList.isEmpty()
                || !fragmentsList.contains(parentFragment)) {
            return -1;
        }*/

        List<Fragment> directNestedFragmentsList = null;
        if (!mDirectNestedFragmentsMap.containsKey(parentFragment)) {
            directNestedFragmentsList = new ArrayList<Fragment>();
            mDirectNestedFragmentsMap.put(parentFragment, directNestedFragmentsList);
        } else {
            directNestedFragmentsList = mDirectNestedFragmentsMap.get(parentFragment);
        }
        if (directNestedFragmentsList == null) {
            return -1;
        }

        if (!directNestedFragmentsList.contains(childFragment)) {
            directNestedFragmentsList.add(childFragment);

            FragmentTransaction fragmentTransaction =
                    parentFragment.getChildFragmentManager().beginTransaction();
            fragmentTransaction.replace(
                    containerId,
                    childFragment,
                    childFragment.getClass().getSimpleName());
            fragmentTransaction.addToBackStack(childFragment.getClass().getSimpleName());
            // 嵌套的Fragment不要有动画
            // showFragmentUseAnimations(fragmentTransaction);
            fragmentTransaction.commit();
        }

        /*if (!directFragmentsList.contains(childFragment)) {
            FragmentTransaction fTransaction =
                    parentFragment.getFragmentManager().beginTransaction();
            directFragmentsList.add(childFragment);
            // 使用add和show,在第一次使用的时候正常,第二次就不显示了
            fTransaction.add(containerId,
                    childFragment,
                    childFragment.getClass().getSimpleName());
            fTransaction.addToBackStack(childFragment.getClass().getSimpleName());

            showFragmentUseAnimations(fTransaction);
            fTransaction.show(childFragment);
            fTransaction.commit();
        }*/

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
     * @param parentFragment
     * @param childFragment
     * @return
     */
    public int enter(Fragment parentFragment,
                     Fragment childFragment) {
        if (parentFragment == null
                || childFragment == null
                || mCurShowActivity == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(mCurShowActivity)
                || mDirectNestedFragmentsMap == null) {
            return -1;
        }

        if (DEBUG)
            MLog.d(TAG, "enter() mCurShowActivity: " +
                    mCurShowActivity.getClass().getSimpleName() +
                    " parentFragment: " + parentFragment.getClass().getSimpleName() +
                    " childFragment: " + childFragment.getClass().getSimpleName() +
                    " containerId: " + childFragment.getId());

        /*List<Fragment> fragmentsList = mActivityFragmentsMap.get(mCurShowActivity);
        if (fragmentsList == null
                || fragmentsList.isEmpty()
                || !fragmentsList.contains(parentFragment)) {
            return -1;
        }*/

        List<Fragment> directNestedFragmentsList = null;
        if (!mDirectNestedFragmentsMap.containsKey(parentFragment)) {
            directNestedFragmentsList = new ArrayList<Fragment>();
            mDirectNestedFragmentsMap.put(parentFragment, directNestedFragmentsList);
        } else {
            directNestedFragmentsList = mDirectNestedFragmentsMap.get(parentFragment);
        }
        if (directNestedFragmentsList == null) {
            return -1;
        }

        if (!directNestedFragmentsList.contains(childFragment)) {
            directNestedFragmentsList.add(childFragment);

            FragmentTransaction fragmentTransaction =
                    parentFragment.getChildFragmentManager().beginTransaction();
            fragmentTransaction.add(childFragment, childFragment.getClass().getSimpleName());
            fragmentTransaction.addToBackStack(childFragment.getClass().getSimpleName());
            // 不需要显示
            fragmentTransaction.commit();
        }

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

    ////////////////////////////////////////////////////////////////////////////////////

    public void addActivity2(Activity activity, int containerId) {
        if (activity == null
                || containerId <= 0
                || mActivityContainersMap == null) {
            return;
        }

        mCurShowActivity = activity;
        if (!mActivityContainersMap.containsKey(activity)) {
            Integer[] container_scene = new Integer[2];
            container_scene[0] = containerId;
            container_scene[1] = SCENE_MORE_MAIN_FRAGMENT;
            mActivityContainersMap.put(activity, container_scene);
        }
    }

    /***
     * 在SCENE_MORE_MAIN_FRAGMENT这个场景使用
     *
     * 在MainActivity中预告添加多个MainFragment的方法,
     * 其他地方不能调用.
     *
     * @param mainFragment
     * @return
     */
    public int enter2(Fragment mainFragment) {
        if (mainFragment == null
                || mCurShowActivity == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(mCurShowActivity)
                || mMoreMainFragmentsMap == null) {
            return -1;
        }

        if (DEBUG)
            MLog.d(TAG, "enter2() mCurShowActivity: " +
                    mCurShowActivity.getClass().getSimpleName() +
                    " mainFragment: " + mainFragment.getClass().getSimpleName());

        Integer[] container_scene = mActivityContainersMap.get(mCurShowActivity);
        FragmentTransaction fragmentTransaction = null;

        for (Fragment shouldHideFragment : mMoreMainFragmentsMap.keySet()) {
            if (!shouldHideFragment.isHidden()) {
                fragmentTransaction = mCurShowActivity.getFragmentManager().beginTransaction();
                // fragment隐藏时的动画
                // fTransaction.setCustomAnimations(R.anim.push_right_in, R.anim.push_left_out2);
                // 先把所有的Fragment给隐藏掉.
                fragmentTransaction.hide(shouldHideFragment);
                fragmentTransaction.commit();
            }
        }

        if (!mMoreMainFragmentsMap.containsKey(mainFragment)) {
            mMoreMainFragmentsMap.put(mainFragment, null);

            fragmentTransaction = mCurShowActivity.getFragmentManager().beginTransaction();
            fragmentTransaction.add(
                    container_scene[0],
                    mainFragment,
                    mainFragment.getClass().getSimpleName());
            fragmentTransaction.addToBackStack(
                    mainFragment.getClass().getSimpleName());
            fragmentTransaction.show(mainFragment);
            fragmentTransaction.commit();
        }

        /*if (mAllFragmentsList != null
                && !mAllFragmentsList.contains(fragment)) {
            mAllFragmentsList.add(fragment);
        }*/

        return 0;
    }

    /***
     * @param mainChildFragment
     * @return
     */
    public int enter3(Fragment mainChildFragment) {
        if (mainChildFragment == null
                || mCurShowActivity == null
                || mCurShowMainFragment == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(mCurShowActivity)
                || mMoreMainFragmentsMap == null
                || mMoreMainFragmentsMap.isEmpty()
                || !mMoreMainFragmentsMap.containsKey(mCurShowMainFragment)) {
            return -1;
        }

        if (DEBUG)
            MLog.d(TAG, "enter3() mCurShowActivity: " +
                    mCurShowActivity.getClass().getSimpleName() +
                    " mCurShowMainFragment: " + mCurShowMainFragment.getClass().getSimpleName() +
                    " mainChildFragment: " + mainChildFragment.getClass().getSimpleName());

        Integer[] container_scene = mActivityContainersMap.get(mCurShowActivity);
        List<Fragment> mainChildFragmentsList = mMoreMainFragmentsMap.get(mCurShowMainFragment);
        if (mainChildFragmentsList == null) {
            mainChildFragmentsList = new ArrayList<Fragment>();
            mMoreMainFragmentsMap.put(mCurShowMainFragment, mainChildFragmentsList);
        }

        FragmentTransaction fTransaction =
                mCurShowActivity.getFragmentManager().beginTransaction();
        // 保证fragment在最后一个
        if (!mainChildFragmentsList.contains(mainChildFragment)) {
            mainChildFragmentsList.add(mainChildFragment);

            // 不用replace
            fTransaction.add(
                    container_scene[0],
                    mainChildFragment,
                    mainChildFragment.getClass().getSimpleName());
            fTransaction.addToBackStack(
                    mainChildFragment.getClass().getSimpleName());
        } else {
            mainChildFragmentsList.remove(mainChildFragment);
            mainChildFragmentsList.add(mainChildFragment);
        }

        if (!mCurShowMainFragment.isHidden()) {
            fTransaction.hide(mCurShowMainFragment);
        }

        int count = mainChildFragmentsList.size();
        for (int i = 0; i < count - 1; i++) {
            Fragment shouldHideFragment = mainChildFragmentsList.get(i);
            if (!shouldHideFragment.isHidden()) {
                List<Fragment> directFragmentsList = mDirectNestedFragmentsMap.get
                        (shouldHideFragment);
                if (directFragmentsList != null
                        && !directFragmentsList.isEmpty()) {
                    FragmentManager fragmentManager =
                            shouldHideFragment.getChildFragmentManager();
                    FragmentTransaction fragmentTransaction =
                            fragmentManager.beginTransaction();
                    Iterator<Fragment> iterator = directFragmentsList.iterator();
                    while (iterator.hasNext()) {
                        Fragment directChildFragment = iterator.next();
                        if (DEBUG)
                            MLog.d(TAG, "enter3() directChildFragment.getId(): " +
                                    directChildFragment.getId() +
                                    " directChildFragment: " +
                                    directChildFragment.getClass().getSimpleName());
                        if (!directChildFragment.isHidden()) {
                            if (directChildFragment.getId() <= 0) {
                                fragmentTransaction.hide(directChildFragment);
                            } else {
                                fragmentManager.popBackStack();
                                iterator.remove();
                            }
                        }
                    }
                    fragmentTransaction.commit();
                }
                // fragment隐藏时的动画
                // fTransaction.setCustomAnimations(R.anim.push_right_in, R.anim.push_left_out2);
                // 先把所有的Fragment给隐藏掉.
                fTransaction.hide(shouldHideFragment);
            }
        }

        showFragmentUseAnimations(fTransaction);
        fTransaction.show(mainChildFragment);
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

    public int changeFragment() {
        if (mCurShowActivity == null
                || mCurShowMainFragment == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(mCurShowActivity)
                || mMoreMainFragmentsMap == null
                || mMoreMainFragmentsMap.isEmpty()
                || !mMoreMainFragmentsMap.containsKey(mCurShowMainFragment)) {
            return -1;
        }

        if (DEBUG)
            MLog.d(TAG, "changeFragment() mCurShowActivity: " +
                    mCurShowActivity.getClass().getSimpleName() +
                    " mCurShowMainFragment: " +
                    mCurShowMainFragment.getClass().getSimpleName());

        FragmentManager fManager = mCurShowActivity.getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        List<Fragment> mainChildFragmentsList = null;
        for (Fragment shouldHideMainFragment : mMoreMainFragmentsMap.keySet()) {
            if (shouldHideMainFragment == null) {
                continue;
            }
            mainChildFragmentsList = mMoreMainFragmentsMap.get(shouldHideMainFragment);
            if (mainChildFragmentsList != null
                    && !mainChildFragmentsList.isEmpty()) {
                for (Fragment shouldHideMainChildFragment : mainChildFragmentsList) {
                    if (shouldHideMainChildFragment == null
                            || shouldHideMainChildFragment.isHidden()) {
                        continue;
                    }
                    List<Fragment> directFragmentsList =
                            mDirectNestedFragmentsMap.get(shouldHideMainChildFragment);
                    if (directFragmentsList != null
                            && !directFragmentsList.isEmpty()) {
                        FragmentManager fragmentManager =
                                shouldHideMainChildFragment.getChildFragmentManager();
                        FragmentTransaction fragmentTransaction =
                                fragmentManager.beginTransaction();
                        Iterator<Fragment> iterator = directFragmentsList.iterator();
                        while (iterator.hasNext()) {
                            Fragment shouldHideMainChildChildFragment = iterator.next();
                            if (shouldHideMainChildChildFragment == null
                                    || shouldHideMainChildChildFragment.isHidden()) {
                                continue;
                            }
                            if (DEBUG)
                                MLog.d(TAG, "changeFragment() hide" +
                                        " shouldHideMainFragment: " +
                                        shouldHideMainFragment.getClass().getSimpleName() +
                                        " shouldHideMainChildFragment: " +
                                        shouldHideMainChildFragment.getClass().getSimpleName() +
                                        " mainChildChildFragment: " +
                                        shouldHideMainChildChildFragment.getClass()
                                                .getSimpleName());
                            if (shouldHideMainChildChildFragment.getId() <= 0) {
                                fragmentTransaction.hide(shouldHideMainChildChildFragment);
                            } else {
                                fragmentManager.popBackStack();
                                iterator.remove();
                            }
                        }
                        fragmentTransaction.commit();
                    }
                    if (DEBUG)
                        MLog.d(TAG, "changeFragment() hide" +
                                " shouldHideMainFragment: " +
                                shouldHideMainFragment.getClass().getSimpleName() +
                                " shouldHideMainChildFragment: " +
                                shouldHideMainChildFragment.getClass().getSimpleName());
                    fTransaction.hide(shouldHideMainChildFragment);
                }
            }
            if (!shouldHideMainFragment.isHidden()) {
                if (DEBUG)
                    MLog.d(TAG, "changeFragment() hide" +
                            " shouldHideMainFragment: " +
                            shouldHideMainFragment.getClass().getSimpleName());
                fTransaction.hide(shouldHideMainFragment);
            }
        }

        mainChildFragmentsList = mMoreMainFragmentsMap.get(mCurShowMainFragment);
        if (mainChildFragmentsList != null
                && !mainChildFragmentsList.isEmpty()) {
            int count = mainChildFragmentsList.size();
            Fragment shouldShowMainChildFragment = mainChildFragmentsList.get(count - 1);
            List<Fragment> directFragmentsList =
                    mDirectNestedFragmentsMap.get(shouldShowMainChildFragment);
            if (directFragmentsList != null
                    && !directFragmentsList.isEmpty()) {
                FragmentManager fragmentManager =
                        shouldShowMainChildFragment.getChildFragmentManager();
                FragmentTransaction fragmentTransaction =
                        fragmentManager.beginTransaction();
                for (Fragment shouldShowMainChildChildFragment : directFragmentsList) {
                    if (shouldShowMainChildChildFragment == null
                            || !shouldShowMainChildChildFragment.isHidden()) {
                        continue;
                    }
                    if (shouldShowMainChildChildFragment.getId() <= 0) {
                        if (DEBUG)
                            MLog.d(TAG, "changeFragment() show" +
                                    " mCurShowMainFragment: " +
                                    mCurShowMainFragment.getClass().getSimpleName() +
                                    " shouldShowMainChildFragment: " +
                                    shouldShowMainChildFragment.getClass().getSimpleName() +
                                    " shouldShowMainChildChildFragment: " +
                                    shouldShowMainChildChildFragment.getClass().getSimpleName());
                        fragmentTransaction.show(shouldShowMainChildChildFragment);
                    }
                }
                fragmentTransaction.commit();
            }
            if (shouldShowMainChildFragment.isHidden()) {
                if (DEBUG)
                    MLog.d(TAG, "changeFragment() show" +
                            " mCurShowMainFragment: " +
                            mCurShowMainFragment.getClass().getSimpleName() +
                            " shouldShowMainChildFragment: " +
                            shouldShowMainChildFragment.getClass().getSimpleName());
                fTransaction.show(shouldShowMainChildFragment);
            }
        } else {
            if (mCurShowMainFragment.isHidden()) {
                if (DEBUG)
                    MLog.d(TAG, "changeFragment() show" +
                            " mCurShowMainFragment: " +
                            mCurShowMainFragment.getClass().getSimpleName());
                //showFragmentUseAnimations(fTransaction);
                fTransaction.show(mCurShowMainFragment);
            }
        }
        fTransaction.commit();

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
                if (object == null) {
                    return what;
                }

                // 隐藏某个Fragment,而不是弹出.
                what = exit((Fragment) object[0], HIDE);
                break;

            case POP_BACK_STACK:
                if (object == null) {
                    return what;
                }
                // 弹出某个Fragment,而不是隐藏.
                what = exit((Fragment) object[0], POP_BACK_STACK);
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
     * 有一个现象,当一个Activity中所有的Fragment都被
     * pop掉后,Activity的生命周期不会发生变化,
     * 也就是onResume()不会被调用,
     * 这样可能在内容显示上面应有问题.
     *
     * @param fragment
     * @param exitType
     */
    /*private int exit(Activity activity,
                     Fragment fragment,
                     String mainFragmentTag,
                     int exitType) {
        if (activity == null
                || fragment == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(activity)) {
            return -1;
        }

        FragmentManager fManager = activity.getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        int result = -1;

        Integer[] container_scene = mActivityContainersMap.get(activity);
        switch (container_scene[1]) {
            case SCENE_NO_OR_ONE_MAIN_FRAGMENT:
                // 不需要先加载一个Fragment
                switch (exitType) {
                    case HIDE:
                        result = hide_scene_1(
                                activity,
                                fManager,
                                fTransaction,
                                fragment);
                        break;
                    case POP_BACK_STACK:
                        result = pop_back_stack_scene_1(
                                activity,
                                fManager,
                                fTransaction,
                                fragment);
                        break;
                    default:
                }
                break;

            case SCENE_MORE_MAIN_FRAGMENT:
                if (TextUtils.isEmpty(mainFragmentTag)
                        || mMoreMainFragmentsMap == null
                        || mMoreMainFragmentsMap.isEmpty()) {
                    return -1;
                }
                switch (exitType) {
                    case HIDE:

                        break;
                    case POP_BACK_STACK:
                        pop_back_stack_scene_2(
                                activity,
                                fManager,
                                fTransaction,
                                fragment);
                        break;
                    default:
                }
                break;
            default:
        }

        if (result == 0) {
            fTransaction.commit();
        }

        return 0;
    }*/

    /**
     * 有一个现象,当一个Activity中所有的Fragment都被
     * pop掉后,Activity的生命周期不会发生变化,
     * 也就是onResume()不会被调用,
     * 这样可能在内容显示上面应有问题.
     *
     * @param fragment
     * @param exitType
     */
    private int exit(Fragment fragment, int exitType) {
        if (fragment == null
                || mCurShowActivity == null
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || !mActivityContainersMap.containsKey(mCurShowActivity)) {
            return -1;
        }

        if (DEBUG)
            MLog.d(TAG, "exit() mCurShowActivity: " +
                    mCurShowActivity.getClass().getSimpleName());

        FragmentManager fManager = mCurShowActivity.getFragmentManager();
        FragmentTransaction fTransaction = fManager.beginTransaction();
        int result = -1;

        Integer[] container_scene = mActivityContainersMap.get(mCurShowActivity);
        switch (container_scene[1]) {
            case SCENE_NO_OR_ONE_MAIN_FRAGMENT:
                // 不需要先加载一个Fragment
                switch (exitType) {
                    case HIDE:
                        result = hide_scene_1(
                                fManager,
                                fTransaction,
                                fragment);
                        break;
                    case POP_BACK_STACK:
                        result = pop_back_stack_scene_1(
                                fManager,
                                fTransaction,
                                fragment);
                        break;
                    default:
                }
                break;

            case SCENE_MORE_MAIN_FRAGMENT:
                if (mCurShowMainFragment == null
                        || mMoreMainFragmentsMap == null
                        || mMoreMainFragmentsMap.isEmpty()
                        || !mMoreMainFragmentsMap.containsKey(mCurShowMainFragment)) {
                    return -1;
                }
                switch (exitType) {
                    case HIDE:

                        break;
                    case POP_BACK_STACK:
                        result = pop_back_stack_scene_2(
                                fManager,
                                fTransaction,
                                fragment);
                        break;
                    default:
                }
                break;
            default:
        }

        if (result == 0) {
            fTransaction.commit();
        }

        return 0;
    }

    private int hide_scene_1(
            FragmentManager fManager,
            FragmentTransaction fTransaction,
            Fragment fragment) {
        List<Fragment> parentFragmentsList = mActivityFragmentsMap.get(mCurShowActivity);
        if (parentFragmentsList == null
                || !parentFragmentsList.contains(fragment)) {
            return -1;
        }
        List<Fragment> directFragmentsList = mDirectNestedFragmentsMap.get(fragment);
        if (!fragment.isHidden()) {
            if (directFragmentsList != null
                    && !directFragmentsList.isEmpty()) {
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
            return 0;
        }
        Fragment showFragment = parentFragmentsList.get(count - 1);
        directFragmentsList = mDirectNestedFragmentsMap.get(showFragment);
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
        return 0;
    }

    private int pop_back_stack_scene_1(
            FragmentManager fManager,
            FragmentTransaction fTransaction,
            Fragment fragment) {
        List<Fragment> parentFragmentsList = mActivityFragmentsMap.get(mCurShowActivity);
        if (parentFragmentsList == null
                || !parentFragmentsList.contains(fragment)) {
            return -1;
        }
        // 处理当前这个fragmnet嵌套的fragments
        List<Fragment> directNestedFragmentsList = mDirectNestedFragmentsMap.get(fragment);
        if (directNestedFragmentsList != null
                && !directNestedFragmentsList.isEmpty()) {
            FragmentManager fragmentManager = fragment.getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            for (Fragment directNestedFragment : directNestedFragmentsList) {
                if (directNestedFragment == null) {
                    continue;
                }
                if (!directNestedFragment.isHidden()) {
                    fragmentManager.popBackStack();
                }
            }
            fragmentTransaction.commit();
        }

        FragmentManager fManager_ = mCurShowActivity.getFragmentManager();
        FragmentTransaction fTransaction_ = fManager_.beginTransaction();
        // pop掉要处理的Fragment
        // fManager_.popBackStack();
        fTransaction_.remove(fragment);
        fTransaction_.commit();

        parentFragmentsList.remove(fragment);
        mDirectNestedFragmentsMap.remove(fragment);

        int count = parentFragmentsList.size();
        if (count < 1) {
            if (mCurShowActivity instanceof BaseActivity) {
                ((BaseActivity) mCurShowActivity).onResume_();
            }
            return 0;
        }

        for (int i = 0; i < count - 1; i++) {
            Fragment shouldHideFragment = parentFragmentsList.get(i);
            if (!shouldHideFragment.isHidden()) {
                directNestedFragmentsList = mDirectNestedFragmentsMap.get(shouldHideFragment);
                if (directNestedFragmentsList != null
                        && !directNestedFragmentsList.isEmpty()) {
                    FragmentManager fragmentManager = shouldHideFragment.getChildFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    Iterator<Fragment> iterator = directNestedFragmentsList.iterator();
                    while (iterator.hasNext()) {
                        Fragment directNestedFragment = iterator.next();
                        if (directNestedFragment.getId() <= 0) {
                            if (!directNestedFragment.isHidden()) {
                                fragmentTransaction.hide(directNestedFragment);
                            }
                        } else {
                            fragmentManager.popBackStack();
                            iterator.remove();
                        }
                    }
                    fragmentTransaction.commit();
                }
                fTransaction.hide(shouldHideFragment);
            }
        }

        Fragment shouldShowFragment = parentFragmentsList.get(count - 1);
        directNestedFragmentsList = mDirectNestedFragmentsMap.get(shouldShowFragment);
        if (directNestedFragmentsList != null
                && !directNestedFragmentsList.isEmpty()) {
            FragmentManager fragmentManager = shouldShowFragment.getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            for (Fragment directNestedFragment : directNestedFragmentsList) {
                if (directNestedFragment.getId() <= 0) {
                    if (directNestedFragment.isHidden()) {
                        fragmentTransaction.show(directNestedFragment);
                    }
                }
            }
            fragmentTransaction.commit();
        }
        if (shouldShowFragment.isHidden()) {
            showFragmentUseAnimations(fTransaction);
            fTransaction.show(shouldShowFragment);
        }

        return 0;
    }

    private int hide_scene_2(Activity activity,
                             FragmentManager fManager,
                             FragmentTransaction fTransaction,
                             List<Fragment> directFragmentsList,
                             Fragment fragment,
                             String mainFragmentTag) {

        return 0;
    }

    private int pop_back_stack_scene_2(
            FragmentManager fManager,
            FragmentTransaction fTransaction,
            Fragment popMainChildFragment) {
        List<Fragment> mainChildFragmentsList = mMoreMainFragmentsMap.get(mCurShowMainFragment);
        if (mainChildFragmentsList == null
                || mainChildFragmentsList.isEmpty()
                || !mainChildFragmentsList.contains(popMainChildFragment)) {
            return -1;
        }

        // 先处理要pop的Fragment的子类
        List<Fragment> mainChildChildFragmentsList =
                mDirectNestedFragmentsMap.get(popMainChildFragment);
        if (mainChildChildFragmentsList != null
                && !mainChildChildFragmentsList.isEmpty()) {
            FragmentManager fragmentManager =
                    popMainChildFragment.getChildFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            for (Fragment popMainChildChildFragment : mainChildChildFragmentsList) {
                fragmentManager.popBackStack();
                if (DEBUG)
                    MLog.d(TAG, "pop_back_stack_scene_2() remove" +
                            " mCurShowMainFragment: " +
                            mCurShowMainFragment.getClass().getSimpleName() +
                            " popMainChildFragment: " +
                            popMainChildFragment.getClass().getSimpleName() +
                            " popMainChildChildFragment: " +
                            popMainChildChildFragment.getClass().getSimpleName());
            }
            fragmentTransaction.commit();
        }

        if (DEBUG)
            MLog.d(TAG, "pop_back_stack_scene_2() popBackStack" +
                    " mCurShowMainFragment: " +
                    mCurShowMainFragment.getClass().getSimpleName() +
                    " popMainChildFragment: " +
                    popMainChildFragment.getClass().getSimpleName());

        FragmentManager fManager_ = mCurShowActivity.getFragmentManager();
        FragmentTransaction fTransaction_ = fManager_.beginTransaction();
        // pop掉要处理的Fragment
        // fManager_.popBackStack();
        fTransaction_.remove(popMainChildFragment);
        fTransaction_.commit();

        mainChildFragmentsList.remove(popMainChildFragment);
        mDirectNestedFragmentsMap.remove(popMainChildFragment);

        int count = mainChildFragmentsList.size();
        if (count < 1) {
            // 当前MainFragment下面没有子类Fragment了
            if (mCurShowMainFragment.isHidden()) {
                if (DEBUG)
                    MLog.d(TAG, "pop_back_stack_scene_2() show" +
                            " mCurShowMainFragment: " +
                            mCurShowMainFragment.getClass().getSimpleName());
                fTransaction.show(mCurShowMainFragment);
            }
            return 0;
        }

        // 处理当前MainFragment下面的其他子类
        for (int i = 0; i < count - 1; i++) {
            Fragment shouldHideFragment = mainChildFragmentsList.get(i);
            if (!shouldHideFragment.isHidden()) {
                mainChildChildFragmentsList = mDirectNestedFragmentsMap.get(shouldHideFragment);
                if (mainChildChildFragmentsList != null
                        && !mainChildChildFragmentsList.isEmpty()) {
                    FragmentManager fragmentManager =
                            popMainChildFragment.getChildFragmentManager();
                    FragmentTransaction fragmentTransaction =
                            fragmentManager.beginTransaction();
                    Iterator<Fragment> iterator = mainChildChildFragmentsList.iterator();
                    while (iterator.hasNext()) {
                        Fragment mainChildChildFragment = iterator.next();
                        if (DEBUG)
                            MLog.d(TAG, "pop_back_stack_scene_2() hide" +
                                    " mCurShowMainFragment: " +
                                    mCurShowMainFragment.getClass().getSimpleName() +
                                    " shouldHideFragment: " +
                                    shouldHideFragment.getClass().getSimpleName() +
                                    " hideMainChildChildFragment: " +
                                    mainChildChildFragment.getClass().getSimpleName());
                        if (mainChildChildFragment.getId() <= 0) {
                            if (!mainChildChildFragment.isHidden()) {
                                fragmentTransaction.hide(mainChildChildFragment);
                            }
                        } else {
                            fragmentManager.popBackStack();
                            iterator.remove();
                        }
                    }
                    fragmentTransaction.commit();
                }

                if (DEBUG)
                    MLog.d(TAG, "pop_back_stack_scene_2() hide" +
                            " mCurShowMainFragment: " +
                            mCurShowMainFragment.getClass().getSimpleName() +
                            " shouldHideFragment: " +
                            shouldHideFragment.getClass().getSimpleName());
                fTransaction.hide(shouldHideFragment);
            }
        }

        //
        Fragment showFragment = mainChildFragmentsList.get(count - 1);
        mainChildChildFragmentsList = mDirectNestedFragmentsMap.get(showFragment);
        if (mainChildChildFragmentsList != null
                && !mainChildChildFragmentsList.isEmpty()) {
            FragmentManager fragmentManager =
                    popMainChildFragment.getChildFragmentManager();
            FragmentTransaction fragmentTransaction =
                    fragmentManager.beginTransaction();
            for (Fragment mainChildChildFragment : mainChildChildFragmentsList) {
                if (mainChildChildFragment.isHidden()) {
                    if (DEBUG)
                        MLog.d(TAG, "pop_back_stack_scene_2() show" +
                                " mCurShowMainFragment: " +
                                mCurShowMainFragment.getClass().getSimpleName() +
                                " showMainChildFragment: " +
                                showFragment.getClass().getSimpleName() +
                                " showMainChildChildFragment: " +
                                mainChildChildFragment.getClass().getSimpleName());
                    if (mainChildChildFragment.getId() <= 0) {
                        fragmentTransaction.show(mainChildChildFragment);
                    }
                }
            }
            fragmentTransaction.commit();
        }

        if (showFragment.isHidden()) {
            if (DEBUG)
                MLog.d(TAG, "pop_back_stack_scene_2() show" +
                        " mCurShowMainFragment: " +
                        mCurShowMainFragment.getClass().getSimpleName() +
                        " showMainChildFragment: " +
                        showFragment.getClass().getSimpleName());
            fTransaction.show(showFragment);
        }

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
     移除某个Fragment.什么场景下会用到这个方法呢?
     就是某个FragmentA页面中又有另一个FragmentB,那么再次添加FragmentA时,
     FragmentB会显示不了,需要把之前的FragmentA给删除掉才可以.
     @param fragmentTag
     */
    private void removeSomeOneFragment(Activity activity, String fragmentTag) {
        /*if (TextUtils.isEmpty(fragmentTag)
                || mFragmentsList == null
                || mFragmentsList.isEmpty()) {
            return;
        }
        Fragment fragmentTemp = null;
        FragmentTransaction fTransaction = activity.getFragmentManager().beginTransaction();
        for (Fragment fragment : mFragmentsList) {
            if (fragmentTag.equals(fragment.getClass().getSimpleName())) {
                fragmentTemp = fragment;
                fTransaction.remove(fragment);
                break;
            }
        }
        fTransaction.commit();
        if (fragmentTemp != null
                || mFragmentsList.contains(fragmentTemp)) {
            mFragmentsList.remove(fragmentTemp);
        }*/
    }

    public int enter2(Activity activity,
                      String mainFragmentTag,
                      Fragment fragment,
                      String tag) {
        if (activity == null
                || fragment == null
                || TextUtils.isEmpty(mainFragmentTag)
                || TextUtils.isEmpty(tag)
                || mActivityContainersMap == null
                || mActivityContainersMap.isEmpty()
                || mMoreMainFragmentsMap == null
                || mMoreMainFragmentsMap.isEmpty()) {
            return -1;
        }

        if (!mActivityContainersMap.containsKey(activity)) {
            return -1;
        }

        Fragment curMainFragment = null;
        for (Fragment mainFragment : mMoreMainFragmentsMap.keySet()) {
            if (mainFragment.getClass().getSimpleName().equals(mainFragmentTag)) {
                curMainFragment = mainFragment;
                break;
            }
        }
        if (curMainFragment == null) {
            return -1;
        }

        Integer[] container_scene = mActivityContainersMap.get(activity);
        List<Fragment> mainChildFragmentsList = mMoreMainFragmentsMap.get(curMainFragment);
        if (mainChildFragmentsList == null) {
            mainChildFragmentsList = new ArrayList<Fragment>();
        }
        mMoreMainFragmentsMap.put(curMainFragment, mainChildFragmentsList);

        FragmentTransaction fTransaction = activity.getFragmentManager().beginTransaction();
        // 保证fragment在最后一个
        if (!mainChildFragmentsList.contains(fragment)) {
            mainChildFragmentsList.add(fragment);
            // 不用replace
            fTransaction.add(container_scene[0], fragment, tag);
            fTransaction.addToBackStack(tag);
        } else {
            mainChildFragmentsList.remove(fragment);
            mainChildFragmentsList.add(fragment);
        }

        if (!curMainFragment.isHidden()) {
            fTransaction.hide(curMainFragment);
        }

        int count = mainChildFragmentsList.size();
        for (int i = 0; i < count - 1; i++) {
            Fragment hideFragment = mainChildFragmentsList.get(i);
            if (!hideFragment.isHidden()) {
                List<Fragment> directFragmentsList = mDirectNestedFragmentsMap.get(hideFragment);
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