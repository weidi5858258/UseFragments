package com.weidi.usefragments.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.tool.Contents;
import com.weidi.usefragments.tool.ExoPlaybackException;
import com.weidi.usefragments.tool.HttpAccessor;
import com.weidi.usefragments.tool.MLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

/***
 Created by root on 19-8-5.
 */

public class DownloadFileService extends Service {

    private static final String TAG =
            DownloadFileService.class.getSimpleName();
    private static final boolean DEBUG = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        MLog.i(TAG, "onBind() intent: " + intent);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MLog.i(TAG, "onUnbind() intent: " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MLog.i(TAG, "onCreate()");
        initData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MLog.i(TAG, "onStartCommand() intent: " + intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        MLog.i(TAG, "onDestroy()");
        destroy();
        super.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////

    public static final int MSG_DOWNLOAD_START = 1;
    public static final int MSG_DOWNLOAD_PAUSE_OR_START = 2;
    public static final int MSG_DOWNLOAD_STOP = 3;
    private static final int BUFFER = 1024 * 1024 * 2;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private boolean mIsDownloading = false;
    private boolean mIsPaused = false;
    private Object readDataPauseLock = new Object();

    private void initData() {
        EventBusUtils.register(this);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                DownloadFileService.this.threadHandleMessage(msg);
            }
        };
        mUiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                DownloadFileService.this.uiHandleMessage(msg);
            }
        };
    }

    private void destroy() {
        EventBusUtils.unregister(this);

        mUiHandler.removeMessages(MSG_DOWNLOAD_STOP);
        mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_STOP);

        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    public Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case MSG_DOWNLOAD_START:
                mThreadHandler.removeMessages(MSG_DOWNLOAD_START);
                mThreadHandler.sendEmptyMessage(MSG_DOWNLOAD_START);
                break;
            case MSG_DOWNLOAD_PAUSE_OR_START:
                mUiHandler.removeMessages(MSG_DOWNLOAD_PAUSE_OR_START);
                mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_PAUSE_OR_START);
                break;
            case MSG_DOWNLOAD_STOP:
                mUiHandler.removeMessages(MSG_DOWNLOAD_STOP);
                mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_STOP);
                break;
            default:
                break;
        }
        return result;
    }

    private void threadHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_DOWNLOAD_START:
                if (!mIsDownloading) {
                    downloadStart();
                }
                break;
            default:
                break;
        }
    }

    private void uiHandleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        switch (msg.what) {
            case MSG_DOWNLOAD_PAUSE_OR_START:
                if (mIsDownloading) {
                    mIsPaused = !mIsPaused;
                    if (!mIsPaused) {
                        synchronized (readDataPauseLock) {
                            readDataPauseLock.notify();
                        }
                    }
                }
                break;
            case MSG_DOWNLOAD_STOP:
                if (mIsDownloading) {
                    mIsPaused = false;
                    mIsDownloading = false;
                    synchronized (readDataPauseLock) {
                        readDataPauseLock.notify();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void downloadStart() {
        MLog.i(TAG, "downloadStart() start");

        mIsDownloading = true;
        String httpPath = Contents.getUri();
        MLog.i(TAG, "downloadStart() httpPath: " + httpPath);
        if (TextUtils.isEmpty(httpPath)) {
            mIsDownloading = false;
            return;
        }
        if (!httpPath.startsWith("http://")
                && !httpPath.startsWith("HTTP://")
                && !httpPath.startsWith("https://")
                && !httpPath.startsWith("HTTPS://")) {
            mIsDownloading = false;
            return;
        }

        HttpAccessor httpAccessor = null;
        try {
            httpAccessor = new HttpAccessor(new URL(httpPath), null);
        } catch (MalformedURLException e) {
            httpAccessor = null;
            mIsDownloading = false;
            e.printStackTrace();
            return;
        }

        Uri uri = Uri.parse(httpPath);
        String lastPath = uri.getLastPathSegment();
        String suffixName = null;
        if (lastPath != null) {
            int index = lastPath.lastIndexOf(".");
            if (index != -1) {
                //lastPath = lastPath.substring(0, index);
                suffixName = lastPath.substring(index, lastPath.length());
            }
        }
        if (TextUtils.isEmpty(suffixName)) {
            suffixName = ".mp4";
        }
        String fileName = Contents.getTitle() + suffixName;

        String PATH = null;
        PATH = "/data/data/com.weidi.usefragments/files/";
        PATH = "/storage/37C8-3904/Android/data/com.weidi.usefragments/files/Movies/";
        PATH = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";

        LinkedList<String> list = new LinkedList<String>();
        list.add("/storage/2430-1702/");
        list = getUSBPathList(getApplicationContext(), list);
        String filePath = list.get(0) + "/BaiduNetdisk/video/" + fileName;

        File pathFile = new File(PATH);
        if (!pathFile.canWrite()) {
            mIsDownloading = false;
            return;
        }

        filePath = pathFile.getAbsolutePath() + "/" + fileName;
        MLog.i(TAG, "downloadStart() filePath: " + filePath);
        File videoFile = new File(filePath);
        if (videoFile.exists()) {
            videoFile.delete();
        }
        try {
            videoFile.createNewFile();
        } catch (IOException e) {
            mIsDownloading = false;
            e.printStackTrace();
            return;
        }
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(
                    new FileOutputStream(videoFile), BUFFER);
        } catch (Exception e) {
            outputStream = null;
            mIsDownloading = false;
            e.printStackTrace();
            return;
        }

        byte[] buffer = new byte[BUFFER];
        int readSize = -1;

        for (; ; ) {
            if (!mIsDownloading) {
                break;
            }

            if (mIsPaused) {
                synchronized (readDataPauseLock) {
                    MLog.i(TAG, "downloadStart() readDataPauseLock.wait() start");
                    try {
                        readDataPauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    MLog.i(TAG, "downloadStart() readDataPauseLock.wait() end");
                }
            }

            try {
                readSize = httpAccessor.read(buffer, 0, BUFFER);
            } catch (ExoPlaybackException e) {
                e.printStackTrace();
                mIsDownloading = false;
                break;
            }

            if (readSize < 0) {
                mIsDownloading = false;
                break;
            }

            try {
                outputStream.write(buffer, 0, readSize);
            } catch (IOException e) {
                e.printStackTrace();
                mIsDownloading = false;
                break;
            }
        }

        try {
            httpAccessor.close();
        } catch (ExoPlaybackException e) {
            e.printStackTrace();
        }

        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mIsDownloading = false;

        MLog.i(TAG, "downloadStart() end");
    }

    private static LinkedList<String> getUSBPathList(Context context, LinkedList<String> usbPaths) {
        LinkedList<String> newUsbPaths = new LinkedList<>();
        LinkedList<String> oldUsbPaths = new LinkedList(usbPaths);
        LinkedList<String> latestUsbPaths = new LinkedList(usbPaths);
        StorageManager storageManager =
                (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeList = storageManager.getClass().getDeclaredMethod("getVolumeList");

            Class classStorage = Class.forName("android.os.storage.StorageVolume");
            Field fieldPath = classStorage.getDeclaredField("mPath");
            fieldPath.setAccessible(true);
            Field fieldState = classStorage.getDeclaredField("mState");
            fieldState.setAccessible(true);
            Field fieldEmulated = classStorage.getDeclaredField("mEmulated");
            fieldEmulated.setAccessible(true);

            Object object = getVolumeList.invoke(storageManager);

            int length = Array.getLength(object);

            for (int i = 0; i < length; i++) {
                Object storageVolume = Array.get(object, i);
                String state = (String) fieldState.get(storageVolume);
                if (!state.equalsIgnoreCase("mounted")) {
                    continue;
                }
                Boolean emulated = fieldEmulated.getBoolean(storageVolume);
                if (emulated) {
                    continue;
                }
                String filePath = ((File) fieldPath.get(storageVolume)).toString();
                if (oldUsbPaths.indexOf(filePath) == -1) {
                    latestUsbPaths.add(filePath);
                }
                newUsbPaths.add(filePath);
            }

            String oldDevice = null;
            length = oldUsbPaths.size();

            for (int i = 0; i < length; i++) {
                oldDevice = oldUsbPaths.get(i);
                if (newUsbPaths.indexOf(oldDevice) == -1) {
                    latestUsbPaths.remove(oldDevice);
                }
            }

        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        MLog.i(TAG, "latestUsbPaths: " + latestUsbPaths);
        return latestUsbPaths;
    }

}
