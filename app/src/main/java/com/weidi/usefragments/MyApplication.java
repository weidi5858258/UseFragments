package com.weidi.usefragments;

import android.app.Application;

import com.weidi.application.WeidiApplication;
import com.weidi.usefragments.fragment.FragOperManager;

/**
 * Created by root on 18-12-13.
 */

public class MyApplication extends WeidiApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        FragOperManager.getInstance();
//        HandlerUtils.init(getMainLooper());
//        HandlerThreadUtils.init();
//        EventBusUtils.init();
    }
}
