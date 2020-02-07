package com.weidi.usefragments.business.keeplive;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import com.weidi.log.MLog;

public final class OnePixelReceiver extends BroadcastReceiver {

    private static final String TAG =
            OnePixelReceiver.class.getSimpleName();

    public static boolean screenOn = true;

    public OnePixelReceiver() {
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        // 屏幕关闭的时候接受到广播
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            MLog.d(TAG, "onReceive() android.intent.action.SCREEN_OFF");
            screenOn = false;
            new android.os.Handler(Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (!screenOn) {
                                Intent intent2 = new Intent(context, OnePixelActivity.class);
                                intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                PendingIntent pendingIntent = PendingIntent.getActivity(
                                        context, 0, intent2, 0);
                                try {
                                    pendingIntent.send();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }, 1000);
            // 通知屏幕已关闭，开始播放无声音乐
            context.sendBroadcast(new Intent("_ACTION_SCREEN_OFF"));
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            // 屏幕打开的时候发送广播  结束一像素
            MLog.d(TAG, "onReceive() android.intent.action.SCREEN_ON");
            screenOn = true;
            // 通知屏幕已点亮，停止播放无声音乐
            context.sendBroadcast(new Intent("_ACTION_SCREEN_ON"));
        }
    }
}
