package com.weidi.usefragments.business.keeplive;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;

//import android.support.v4.app.NotificationCompat;

import com.weidi.log.MLog;
import com.weidi.usefragments.R;
import com.weidi.usefragments.business.audio_player.MusicService;

import androidx.core.app.NotificationCompat;

/**
 * Created by root on 19-7-1.
 */

public class RemoteService extends Service {


    @Override
    public IBinder onBind(Intent intent) {
        MLog.d(TAG, "onBind() ---> MusicService");
        return iGuardAidlStub;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MLog.d(TAG, "onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        MLog.d(TAG, "onCreate()");
        super.onCreate();
        internalCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MLog.d(TAG, "onStartCommand() intent: " + intent +
                " flags: " + flags + " startId: " + startId);
        internalStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        MLog.d(TAG, "onDestroy()");
        internalDestroy();
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////

    private static final String TAG =
            RemoteService.class.getSimpleName();

    private boolean mIsBoundLocalService;

    private void internalCreate() {

    }

    private void internalStartCommand(Intent intent, int flags, int startId) {
        try {
            intent = new Intent(RemoteService.this, MusicService.class);
            //startService(intent);
            mIsBoundLocalService = bindService(
                    intent,
                    mServiceConnection,
                    Context.BIND_ABOVE_CLIENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void internalDestroy() {
        if (mIsBoundLocalService) {
            try {
                unbindService(mServiceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final IGuardAidl.Stub iGuardAidlStub = new IGuardAidl.Stub() {
        @Override
        public void wakeUp(String title, String discription, int iconRes) throws RemoteException {
            MLog.d(TAG, "wakeUp() title: " + title + " discription: " + discription);
            if (Build.VERSION.SDK_INT < 25) {
                Intent intent = new Intent(
                        getApplicationContext(), NotificationClickReceiver.class);
                intent.setAction(NotificationClickReceiver.CLICK_NOTIFICATION);
                // PendingIntent.FLAG_UPDATE_CURRENT这个类型才能传值
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        RemoteService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(
                                RemoteService.this, RemoteService.this.getPackageName())
                                .setContentTitle("Alexander")
                                .setContentText("RemoteService")
                                .setSmallIcon(R.drawable.a2)
                                .setAutoCancel(true)
                                .setVibrate(new long[]{0})
                                .setContentIntent(pendingIntent);
                Notification notification = builder.build();
                startForeground(13691, notification);
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MLog.d(TAG, "onServiceConnected()");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MLog.d(TAG, "onServiceDisconnected()");
            if (KeepLive.isRunningTaskExist(
                    getApplicationContext(), getPackageName() + ":remote")) {
                MLog.d(TAG, "onServiceDisconnected() RemoteService is alive");
            }
            JobHandlerService.startForeground(RemoteService.this, "Alexander", "RemoteService");

            Intent localService = new Intent(RemoteService.this, MusicService.class);
            startService(localService);
            mIsBoundLocalService = bindService(
                    localService, mServiceConnection, Context.BIND_ABOVE_CLIENT);
            PowerManager pm =
                    (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();
            if (isScreenOn) {
                sendBroadcast(new Intent("_ACTION_SCREEN_ON"));
            } else {
                sendBroadcast(new Intent("_ACTION_SCREEN_OFF"));
            }
        }
    };

}
