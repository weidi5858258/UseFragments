package com.weidi.usefragments.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import com.weidi.usefragments.tool.MLog;

/**
 * Created by alexander on 2019/12/31.
 */

public class AccessibilityImpl extends AccessibilityService {

    private static final String TAG =
            AccessibilityImpl.class.getSimpleName();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //MLog.d(TAG, "onAccessibilityEvent()");
    }

    @Override
    public void onInterrupt() {
        MLog.d(TAG, "onInterrupt()");
    }
}
