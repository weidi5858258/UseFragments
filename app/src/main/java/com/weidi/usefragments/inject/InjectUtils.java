package com.weidi.usefragments.inject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 使用： </br>
 * 1、@InjectLayout(R.layout.mainactivity)</br>
 * public class DevicesActivity extends Activity {...}</br>
 * 2、 在onCreate(Bundle savedInstanceState)方法中加InjectUtils.inject(this);</br>
 * 3、@InjectView(R.id.button)在控件上必须要有一个id</br>
 * private Button button;</br>
 * 4、onClick()方法和OnLongClickInject()方法中必须要有参数View
 * 5、单击条目事件的普通方法中的参数必须这样：(AdapterView<?> parent, View view, int position, long id)
 *
 * @OnClickInject({R.id.hello1, R.id.hello2})
 * void onClick(View v) {}方法名不需要一定是onClick
 * @OnLongClickInject({R.id.hello1, R.id.hello2})
 * boolean onLongClick(View v) {}方法名不需要一定是onLongClick
 * @OnItemClickInject(R.id.list_of_things) void onItemClick(AdapterView<?> parent, View view, int
 * position, long id) {}方法名不需要一定是onItemClick
 * <p>
 * Fullscreen
 */
public class InjectUtils {

    private static final String TAG = "InjectUtils";
    private static final boolean DEBUG = false;

    // 动态代理类（必须要实现InvocationHandler接口）
    public static class DynamicProxy implements InvocationHandler {
        Object target = null;// 要代理的对象（真实的对象）
        Method method = null;// 调用真实对象中的某个方法的Method对象

        public DynamicProxy(Object target, Method method) {
            super();
            this.target = target;
            this.method = method;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //            if ("OnClickListener".equals(setClickListener)) {
            //                this.method.invoke(target);
            //                return null;
            //            } else if ("OnLongClickListener".equals(setClickListener)) {
            //                this.method.invoke(target);
            //                return true;
            //            } else if ("OnItemClickListener".equals(setClickListener)) {
            //                this.method.invoke(target, args);
            //                return null;
            //            }
            // 调用真实对象中的某个方法后的返回值
            return this.method.invoke(target, args);
        }
    }

    /**
     * @param object Activity,BaseFragment,Adapter
     * @param view
     */
    public static void inject(Object object, View view) {
        if (object instanceof Activity) {
            injectLayout((Activity) object);
            injectView(object, null);
            injectExtra((Activity) object);
        } else {
            injectView(object, view);
        }
        injectCommonMethod(object, view, "OnClickListener");
        injectCommonMethod(object, view, "OnLongClickListener");
        injectCommonMethod(object, view, "OnItemClickListener");
    }

    /**
     * @param activity
     */
    private static void injectLayout(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            Class<?> cls = activity.getClass();
            // 得到当前类上的注解类
            InjectLayout mInjectLayout = cls.getAnnotation(InjectLayout.class);
            if (mInjectLayout == null) {
                return;
            }
            int layoutResID = mInjectLayout.value();
            if (layoutResID <= 0) {
                return;
            }
            //            Class<?> clsActivity = Class.forName("android.app.Activity");
            Method setContentView = cls.getMethod("setContentView", int.class);
            // 调用setContentView()方法
            setContentView.invoke(activity, layoutResID);
            if (DEBUG)
                Log.d(TAG, "injectLayout():activity = " + activity
                        + " layoutResID = " + layoutResID);
        } catch (Exception e) {
            Log.e(TAG, "xml文件中可能自定义布局的包名没有更换或者有其他问题,请去查看!!!");
            e.printStackTrace();
        }
    }

    /**
     * 绑定组件id
     *
     * @param object Activity,BaseFragment,Adapter
     * @param view
     */
    private static void injectView(Object object, View view) {
        try {
            if (object == null) {
                return;
            }
            Class<?> cls = object.getClass();
            Field[] fields = cls.getDeclaredFields();// 得到当前类中所有的属性
            if (fields == null) {
                return;
            }
            for (Field field : fields) {
                field.setAccessible(true);// 设置属性为public
                InjectView mInjectView = field.getAnnotation(InjectView.class);// 得到属性上的注解类
                if (mInjectView == null) {
                    continue;
                }
                int id = mInjectView.value(); // 资源id
                if (id <= 0) {
                    continue;
                }
                Method findViewById = null;
                Object resView = null;
                if (view == null) {
                    findViewById = cls.getMethod("findViewById", int.class);
                    resView = findViewById.invoke(object, id);// 根据id得到相应的控件资源
                } else {
                    findViewById = view.getClass().getMethod("findViewById", int.class);
                    // 调用findViewById()方法得到一个控件
                    resView = findViewById.invoke(view, id);// 根据id得到相应的控件资源
                }
                if (findViewById == null || resView == null || !(resView instanceof View)) {
                    return;
                }
                field.set(object, resView);// 给属性设置为这个资源值
                if (DEBUG)
                    Log.d(TAG, "injectView():object = " + object + " field.name = "
                            + field.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void injectCommonMethod(Object object, View v, String clickListener) {
        try {
            if (object == null) {
                return;
            }
            Class<?> cls = object.getClass();
            Method findViewById = null;
            if (v == null) {
                findViewById = cls.getMethod("findViewById", int.class);
            } else {
                findViewById = v.getClass().getMethod("findViewById", int.class);
            }
            if (findViewById == null) {
                return;
            }
            // 得到onClick()方法
            // 得到Activity下所有的方法
            Method[] methods = cls.getDeclaredMethods();
            if (methods == null) {
                return;
            }
            InjectOnClick mInjectOnClick = null;
            InjectOnLongClick mInjectOnLongClick = null;
            InjectOnItemClick mInjectOnItemClick = null;
            Object view = null;
            Object proxy = null;
            Method setClickListener = null;
            int[] ids = null;
            for (Method method : methods) {
                method.setAccessible(true);

                // 得到方法上的注解类，判断是否为null，如果不为null，那么说明要找的方法已经找到了
                if ("OnClickListener".equals(clickListener)) {
                    mInjectOnClick = method.getAnnotation(InjectOnClick.class);
                } else if ("OnLongClickListener".equals(clickListener)) {
                    mInjectOnLongClick = method.getAnnotation(InjectOnLongClick.class);
                } else if ("OnItemClickListener".equals(clickListener)) {
                    mInjectOnItemClick = method.getAnnotation(InjectOnItemClick.class);
                }

                if ("OnClickListener".equals(clickListener)) {
                    if (mInjectOnClick == null) {
                        continue;
                    }
                } else if ("OnLongClickListener".equals(clickListener)) {
                    if (mInjectOnLongClick == null) {
                        continue;
                    }
                } else if ("OnItemClickListener".equals(clickListener)) {
                    if (mInjectOnItemClick == null) {
                        continue;
                    }
                }

                if ("OnClickListener".equals(clickListener)) {
                    ids = mInjectOnClick.value();
                } else if ("OnLongClickListener".equals(clickListener)) {
                    ids = mInjectOnLongClick.value();
                } else if ("OnItemClickListener".equals(clickListener)) {
                    ids = mInjectOnItemClick.value();
                }
                for (int id : ids) {
                    if (v == null) {
                        view = findViewById.invoke(object, id);
                    } else {
                        view = findViewById.invoke(v, id);
                    }
                    if (view == null || !(view instanceof View)) {
                        return;
                    }
                    // 得到setOnClickListener()方法
                    if ("OnClickListener".equals(clickListener)) {
                        /**
                         * 代理的对象只要一个就行了（要代理的是OnClickListener接口对象，所以要用到其定义的onClick()
                         * 方法的Method对象，不是其定义的不能用）</br>
                         * public static Object newProxyInstance(ClassLoader loader, Class<?>[]
                         * interfaces, InvocationHandler h) throws IllegalArgumentException</br>
                         loader:　　一个ClassLoader对象，定义了由哪个ClassLoader对象来对生成的代理对象进行加载</br>
                         interfaces:　　一个Interface对象的数组，表示的是我将要给我需要代理的对象提供一组什么接口，如果我提供了一组接口给它，那么这个代理对象就宣称实现了该接口(多态)，这样我就能调用这组接口中的方法了</br>
                         h:　　一个InvocationHandler对象，表示的是当我这个动态代理对象在调用方法的时候，会关联到哪一个InvocationHandler对象上</br>
                         http://www.cnblogs.com/xiaoluo501395377/p/3383130.html</br>
                         */
                        proxy = (Object) Proxy.newProxyInstance(View.OnClickListener.class
                                        .getClassLoader(), new Class<?>[]{View.OnClickListener
                                        .class},
                                new DynamicProxy(object, method));
                        setClickListener = view.getClass().getMethod("setOnClickListener", View
                                .OnClickListener.class);
                    } else if ("OnLongClickListener".equals(clickListener)) {
                        proxy = (Object) Proxy.newProxyInstance(View.OnLongClickListener.class
                                .getClassLoader(), new Class<?>[]{View.OnLongClickListener
                                .class}, new DynamicProxy(object, method));
                        setClickListener = view.getClass().getMethod("setOnLongClickListener",
                                View.OnLongClickListener.class);
                    } else if ("OnItemClickListener".equals(clickListener)) {
                        proxy = (Object) Proxy.newProxyInstance(AdapterView.OnItemClickListener
                                .class.getClassLoader(), new Class<?>[]{AdapterView
                                .OnItemClickListener.class}, new DynamicProxy(object, method));
                        setClickListener = view.getClass().getMethod("setOnItemClickListener",
                                AdapterView.OnItemClickListener.class);
                    }
                    if (proxy == null || setClickListener == null) {
                        return;
                    }

                    // 给view调用setOnClickListener()方法进行事件的绑定
                    //（proxy是参数，就是OnClickListener匿名对象，然后点击控件调用的方法就是实现InvocationHandler接口中invoke()
                    // 方法里的代码）
                    setClickListener.invoke(view, proxy);
                    // button.setOnClickListener(new OnClickListener() {...});
                    // 调用setOnClickListener()方法需要一个参数：OnClickListener对象 new OnClickListener()
                    // {...}这个东西就需要一个代理了

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void injectExtra(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            Class<?> cls = activity.getClass();
            Field[] fields = cls.getDeclaredFields();
            if (fields == null) {
                return;
            }
            Method method = cls.getMethod("getIntent");
            if (method == null) {
                return;
            }
            Intent intent = (Intent) method.invoke(activity);
            if (intent == null) {
                return;
            }
            InjectExtra mInjectExtra = null;
            for (Field field : fields) {
                field.setAccessible(true);
                // 得到当前类上的注解类
                mInjectExtra = field.getAnnotation(InjectExtra.class);
                if (mInjectExtra == null) {
                    continue;
                }
                String sBundle = mInjectExtra.bundle();
                String key = mInjectExtra.key();
                // 问题：要先知道传Bundle这个对象的key；然后是这个Bundle对象中的各个key
                Bundle bundle = null;
                bundle = intent.getBundleExtra(sBundle);
                if (bundle != null) {
                    if ("class android.os.Bundle".equals(field.getGenericType().toString())) {
                        field.set(activity, bundle);
                    }
                    if (bundle.containsKey(key)) {
                        Object obj = bundle.get(key);
                        field.set(activity, obj);
                    }
                } else {
                    // 只要传值的时候Intent对象用了putExtra()这个方法，那么bundle就不会为null，否则为null
                    bundle = intent.getExtras();
                    if (bundle == null) {
                        continue;
                    }
                    if (bundle.containsKey(key)) {
                        Object obj = bundle.get(key);
                        field.set(activity, obj);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
