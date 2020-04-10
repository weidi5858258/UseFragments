package com.weidi.usefragments.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.weidi.usefragments.tool.MLog;

public class PhoneReceiver extends BroadcastReceiver {

    private static final String TAG = PhoneReceiver.class.getSimpleName();
    //"player_alexander";
    //PhoneReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String action = intent.getAction();
        MLog.i(TAG, "onReceive() action: " + action);
        if ("android.intent.action.PHONE_STATE".equals(action)) {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            switch (telephonyManager.getCallState()) {
                case TelephonyManager.CALL_STATE_IDLE:
                    // 挂断
                    MLog.i(TAG, "onReceive() TelephonyManager.CALL_STATE_IDLE");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    // 来电
                    MLog.i(TAG, "onReceive() TelephonyManager.CALL_STATE_RINGING");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // 接听
                    MLog.i(TAG, "onReceive() TelephonyManager.CALL_STATE_OFFHOOK");
                    break;
                default:
                    break;
            }
        } else if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            // 拨打电话
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            MLog.i(TAG, "onReceive() phoneNumber: " + phoneNumber);
        }
    }
}
