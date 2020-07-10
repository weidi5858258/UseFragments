package com.weidi.usefragments.business.keeplive;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


import com.weidi.usefragments.business.audio_player.MusicService;

import java.util.Iterator;
import java.util.List;

/**
 * 保活工具
 */
public final class KeepLive {

    /***
     启动保活
     我的华为手机是这样的情况:
     如果在不充电的情况下,把应用移到后台,后台任务就暂停
     如果在  充电的情况下,把应用移到后台,后台任务就运行
     */
    public static void startWork(Application application) {
        if (isMain(application)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //启动定时器，在定时器中启动本地服务和守护进程
                Intent intent = new Intent(application, JobHandlerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    application.startForegroundService(intent);
                } else {
                    application.startService(intent);
                }
            } else {
                //启动本地服务
                Intent localIntent = new Intent(application, MusicService.class);
                application.startService(localIntent);

                //启动守护进程
                Intent guardIntent = new Intent(application, RemoteService.class);
                application.startService(guardIntent);
            }
        }
    }

    private static boolean isMain(Application application) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager mActivityManager =
                (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos =
                mActivityManager.getRunningAppProcesses();
        if (runningAppProcessInfos != null) {
            for (ActivityManager.RunningAppProcessInfo
                    appProcess : mActivityManager.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    processName = appProcess.processName;
                    break;
                }
            }
            String packageName = application.getPackageName();
            if (processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServiceRunning(Context ctx, String className) {
        boolean isRunning = false;
        ActivityManager activityManager =
                (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> servicesList =
                activityManager.getRunningServices(Integer.MAX_VALUE);
        if (servicesList != null) {
            Iterator<ActivityManager.RunningServiceInfo> l = servicesList.iterator();
            while (l.hasNext()) {
                ActivityManager.RunningServiceInfo si = l.next();
                if (className.equals(si.service.getClassName())) {
                    isRunning = true;
                    break;
                }
            }
        }
        return isRunning;
    }

    public static boolean isRunningTaskExist(Context context, String processName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processList = am.getRunningAppProcesses();
        if (processList != null) {
            for (ActivityManager.RunningAppProcessInfo info : processList) {
                if (info.processName.equals(processName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
