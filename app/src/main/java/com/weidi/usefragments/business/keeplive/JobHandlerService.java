package com.weidi.usefragments.business.keeplive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.weidi.log.MLog;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.audio_player.MusicService;

/**
 * 定时器
 * 安卓5.0及以上
 */
@SuppressWarnings(value = {"unchecked", "deprecation"})
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class JobHandlerService extends JobService {

    private static final String TAG =
            JobHandlerService.class.getSimpleName();

    private int jobId = 100;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MLog.i(TAG, "onStartCommand()");
        startService(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler =
                    (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(jobId);

            JobInfo.Builder builder = new JobInfo.Builder(jobId,
                    new ComponentName(getPackageName(), JobHandlerService.class.getName()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //执行的最小延迟时间
                builder.setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
                //执行的最长延时时间
                builder.setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
                //线性重试方案
                builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS,
                        JobInfo.BACKOFF_POLICY_LINEAR);
            } else {
                builder.setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS);
                builder.setRequiresDeviceIdle(true);
            }
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            // 当插入充电器，执行该任务
            builder.setRequiresCharging(true);
            builder.setPersisted(true);
            jobScheduler.schedule(builder.build());
        }

        return START_STICKY;
    }


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        MLog.i(TAG, "onStartJob()");
        if (!KeepLive.isServiceRunning(getApplicationContext(),
                "com.weidi.usefragments.business.audio_player.MusicService")
                || !KeepLive.isRunningTaskExist(getApplicationContext(),
                getPackageName() + ":remote")) {
        }
        startService(this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        MLog.i(TAG, "onStopJob()");
        if (!KeepLive.isServiceRunning(getApplicationContext(),
                "com.weidi.usefragments.business.audio_player.MusicService")
                || !KeepLive.isRunningTaskExist(getApplicationContext(),
                getPackageName() + ":remote")) {
        }
        startService(this);
        return true;
    }

    private void startService(Context context) {
        startForeground(this, "Alexander", "JobHandlerService");

        //启动本地服务
        Intent localIntent = new Intent(context, MusicService.class);
        startService(localIntent);

        //启动守护进程
        Intent guardIntent = new Intent(context, RemoteService.class);
        startService(guardIntent);
    }

    public static void startForeground(Service context, String title, String text) {
        Notification notification = null;
        Intent intent = new Intent(
                context, NotificationClickReceiver.class);
        intent.setAction(NotificationClickReceiver.CLICK_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(
                    context.getPackageName(), context.getPackageName(),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{0});
            notificationChannel.setSound(null, null);
            notificationManager.createNotificationChannel(notificationChannel);

            // PendingIntent.FLAG_UPDATE_CURRENT这个类型才能传值
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder =
                    new Notification.Builder(context, context.getPackageName())
                            .setContentTitle(title)
                            .setContentText(text)
                            .setSmallIcon(R.drawable.a2)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);
            notification = builder.build();
        } else {
            // PendingIntent.FLAG_UPDATE_CURRENT这个类型才能传值
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, context.getPackageName())
                            .setContentTitle(title)
                            .setContentText(text)
                            .setSmallIcon(R.drawable.a2)
                            .setAutoCancel(true)
                            .setVibrate(new long[]{0})
                            .setContentIntent(pendingIntent);
            notification = builder.build();
        }
        context.startForeground(13691, notification);
    }

}
