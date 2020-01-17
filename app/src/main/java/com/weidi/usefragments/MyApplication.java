package com.weidi.usefragments;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.weidi.application.WeidiApplication;
import com.weidi.dbutil.DbUtils;
import com.weidi.dbutil.SimpleDao2;
import com.weidi.usefragments.business.medical_record.MedicalRecordBean;
import com.weidi.usefragments.tool.MLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/***
 Created by root on 18-12-13.
 */

public class MyApplication extends WeidiApplication {

    private static final String TAG =
            MyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                MLog.d(TAG, "onCreate() start");
                Context context = getApplicationContext();
                Class[] beanClass = new Class[]{MedicalRecordBean.class};
                DbUtils.getInstance().createOrUpdateDBWithVersion(context, beanClass);

                // 数据库
                SimpleDao2.setContext(MyApplication.this);
                // 先调用一下,把对象给创建好
                SimpleDao2.getInstance();
                MLog.d(TAG, "onCreate() end");
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        /*String processName = this.getProcessName();
        // 判断进程名，保证只有主进程运行
        if (!TextUtils.isEmpty(processName) && processName.equals(this.getPackageName())) {
        }*/
    }

    public static String getProcessName() {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
