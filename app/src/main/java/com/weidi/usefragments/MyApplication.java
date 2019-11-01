package com.weidi.usefragments;

import android.content.Context;

import com.weidi.application.WeidiApplication;
import com.weidi.dbutil.DbUtils;
import com.weidi.dbutil.SimpleDao2;
import com.weidi.usefragments.business.medical_record.MedicalRecordBean;
import com.weidi.usefragments.tool.MLog;

/***
 Created by root on 18-12-13.
 */

public class MyApplication extends WeidiApplication {

    private static final String TAG =
            MyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                MLog.d(TAG, "onCreate() start");
                Context context = getApplicationContext();
                Class[] beanClass = new Class[]{MedicalRecordBean.class};
                DbUtils.getInstance().createOrUpdateDBWithVersion(context, beanClass);

                // 数据库
                SimpleDao2.setContext(MyApplication.this);
                // 先调用一下,把对象给创建好
                SimpleDao2.getInstance();
                MLog.d(TAG, "onCreate() end");
            }
        }).start();
    }
}
