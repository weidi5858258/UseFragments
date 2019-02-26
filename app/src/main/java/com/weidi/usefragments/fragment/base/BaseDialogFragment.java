package com.weidi.usefragments.fragment.base;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.weidi.usefragments.inject.InjectUtils;
import com.weidi.usefragments.tool.MLog;

/***
 * 在子类中只需要覆写这样一个周期方法就行了，其他周期方法没什么必要了：
 *
 * @Override public View onCreateView(LayoutInflater inflater,
 * ViewGroup container,
 * Bundle savedInstanceState) {
 * return super.onCreateView(inflater, container, savedInstanceState);
 * }
 */
public abstract class BaseDialogFragment extends DialogFragment {

    private static final String TAG =
            BaseDialogFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final String REQUESTCODE = "requtestCode";
    private Activity mActivity;
    private Context mContext;
    private int requestCode;
    private OnResultListener mOnResultListener;

    /*********************************
     * Created
     *********************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context == null) {
            throw new NullPointerException("BaseDialogFragment onAttach() mContext is null.");
        }

        mContext = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity == null) {
            throw new NullPointerException("BaseDialogFragment onAttach() activity is null.");
        }

        mActivity = activity;
        if (DEBUG)
            MLog.d(TAG, "onAttach(): activity = " + activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate(): savedInstanceState = " + savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);
        if (provideStyle() < 0 || provideStyle() > 3) {
            setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        } else {
            setStyle(provideStyle(), 0);
        }
        requestCode =
                getArguments() != null ? getArguments().getInt(REQUESTCODE) : -1;
    }

    /**
     * onCreateView()方法和onCreateDialog()方法两选一,其他都一样
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreateView(): savedInstanceState = " + savedInstanceState);
        View view = inflater.inflate(provideLayout(), container);
        view.setMinimumWidth(600);
        InjectUtils.inject(this, view);

        /*view.findViewById(R.id.cancel_btn).setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MLog.d(TAG, "dismiss()");
                dismiss();
            }
        });*/

        return view;
    }

    /*public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(provideLayout(), null);
        builder.setView(view);
        return builder.create();
    }*/

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
            MLog.d(TAG, "onActivityCreated(): savedInstanceState = " + savedInstanceState);
    }

    /*********************************
     * Started
     *********************************/

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            MLog.d(TAG, "onStart()");
    }

    /*********************************
     * Resumed
     *********************************/

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            MLog.d(TAG, "onResume()");
    }

    /*********************************
     * Paused
     *********************************/

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            MLog.d(TAG, "onPause()");
    }

    /*********************************
     * Stopped
     *********************************/

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG)
            MLog.d(TAG, "onStop()");
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach()");
    }

    public Activity getAttachedActivity() {
        return mActivity;
    }

    public Context getContext() {
        if (mContext == null) {
            if (getAttachedActivity() != null) {
                mContext = getAttachedActivity().getApplicationContext();
            } else if (getActivity() != null) {
                mContext = getActivity().getApplicationContext();
            }
        }
        if (mContext == null) {
            throw new NullPointerException(
                    "BaseDialogFragment getContext() mContext is null.");
        }
        return mContext;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setOnResultListener(OnResultListener listener) {
        mOnResultListener = listener;
    }

    public OnResultListener getOnResultListener() {
        return mOnResultListener;
    }

    protected String printThis() {
        // com.weidi.usefragments.MainActivity2@416c7b
        String temp = this.toString();
        int lastIndex = temp.lastIndexOf(".");
        temp = temp.substring(lastIndex + 1, temp.length());
        return temp;
    }

    /**
     * 样式选这么几种就行了
     * DialogFragment:
     * public static final int STYLE_NORMAL = 0;
     * public static final int STYLE_NO_TITLE = 1;
     * public static final int STYLE_NO_FRAME = 2;
     * public static final int STYLE_NO_INPUT = 3;
     * 使用: DialogFragment.STYLE_NO_TITLE(一般也是选择这个选项的)
     */
    protected abstract int provideStyle();

    protected abstract int provideLayout();

}
