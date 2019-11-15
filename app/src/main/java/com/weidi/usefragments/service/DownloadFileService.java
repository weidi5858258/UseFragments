package com.weidi.usefragments.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.business.contents.Contents;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.ExoPlaybackException;
import com.weidi.usefragments.tool.HttpAccessor;
import com.weidi.usefragments.tool.MLog;
import com.weidi.utils.MD5Util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
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
        isVideoExist();
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
    public static final int MSG_IS_DOWNLOADING = 4;
    public static final int MSG_SET_CALLBACK = 5;
    public static final int MSG_GET_CONTENT_LENGTH = 6;
    private static final int BUFFER = 1024 * 1024 * 2;

    public static String PATH =
            "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";
    //            "/storage/emulated/0/Movies/";
    /*PATH = "/data/data/com.weidi.usefragments/files/";
    PATH = "/storage/37C8-3904/Android/data/com.weidi.usefragments/files/Movies/";
    PATH = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";*/
    public static final String PREFERENCES_NAME = "alexander_preferences";
    //    public static final String VIDEO_NAME = "video_name";
    //    public static final String VIDEO_PATH = "video_path";
    //    public static final String VIDEO_LENGTH = "video_length";
    //    public static final String VIDEO_IS_FINISHED = "video_is_finished";
    //private static final String VIDEO_POSITION = "video_position";
    private SharedPreferences mPreferences;

    private HandlerThread mHandlerThread;
    private Handler mThreadHandler;
    private Handler mUiHandler;

    private boolean mIsDownloading = false;
    private boolean mIsPaused = false;
    private Object readDataPauseLock = new Object();
    private long contentLength = -1;
    private String mUri;
    private DownloadCallback mCallback;

    private void initData() {
        EventBusUtils.register(this);

        mPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

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

    private boolean isVideoExist(String fileName) {
        boolean isExist = false;
        File moviesFile = new File(PATH);
        File[] listFiles = moviesFile.listFiles();
        if (listFiles == null) {
            return isExist;
        }
        for (File file : moviesFile.listFiles()) {
            if (file == null) {
                continue;
            }
            if (TextUtils.equals(file.getName(), fileName)) {
                // 需要指向那个文件,然后打开时才能播放
                Contents.setPath(file.getAbsolutePath());
                isExist = true;
                break;
            }
        }
        return isExist;
    }

    private boolean isVideoExist() {
        boolean isExist = false;
        File moviesFile = new File(PATH);
        File[] listFiles = moviesFile.listFiles();
        if (listFiles == null) {
            return isExist;
        }
        for (File file : moviesFile.listFiles()) {
            if (file == null) {
                continue;
            }
            if (file.getName().contains("alexander_mylove.")) {
                // 需要指向那个文件,然后打开时才能播放
                Contents.setPath(file.getAbsolutePath());
                isExist = true;
                break;
            }
        }
        return isExist;
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

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            case MSG_DOWNLOAD_START:
                // 开始下载
                mUri = null;
                if (objArray != null && objArray.length > 0) {
                    mUri = (String) objArray[0];
                }

                if (mIsDownloading) {
                    mUiHandler.removeMessages(MSG_DOWNLOAD_STOP);
                    mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_STOP);
                }
                mThreadHandler.removeMessages(MSG_DOWNLOAD_START);
                mThreadHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD_START, 1000);
                break;
            case MSG_DOWNLOAD_PAUSE_OR_START:
                // 暂停或开始
                mUiHandler.removeMessages(MSG_DOWNLOAD_PAUSE_OR_START);
                mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_PAUSE_OR_START);
                break;
            case MSG_DOWNLOAD_STOP:
                // 停止下载
                mUiHandler.removeMessages(MSG_DOWNLOAD_STOP);
                mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_STOP);
                break;
            case MSG_IS_DOWNLOADING:
                // 判断是否还在下载
                return mIsDownloading;
            case MSG_SET_CALLBACK:
                if (objArray != null && objArray.length > 0) {
                    mCallback = (DownloadCallback) objArray[0];
                } else if (objArray == null) {
                    mCallback = null;
                }
                break;
            case MSG_GET_CONTENT_LENGTH:
                // 返回文件长度
                return contentLength;
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
        if (mCallback != null) {
            MLog.i(TAG, "downloadStart() mCallback.onReady()");
            mCallback.onReady();
        }

        mIsDownloading = false;
        contentLength = -1;
        String httpPath = null;
        if (!TextUtils.isEmpty(mUri)) {
            httpPath = mUri;
        } else {
            httpPath = Contents.getUri();
        }
        MLog.i(TAG, "downloadStart() httpPath: " + httpPath);
        if (TextUtils.isEmpty(httpPath)) {
            return;
        }

        if (!httpPath.startsWith("http://")
                && !httpPath.startsWith("HTTP://")
                && !httpPath.startsWith("https://")
                && !httpPath.startsWith("HTTPS://")) {
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
        String fileName = null;
        if (!TextUtils.isEmpty(mUri)) {
            fileName = uri.getLastPathSegment();
        } else {
            fileName = Contents.getTitle() + suffixName;
        }
        // 军情五处-利益之争.mp4
        MLog.i(TAG, "downloadStart() fileName: " + fileName);

        String httpPathMD5 = MD5Util.getMD5String(httpPath);
        boolean videoIsFinished = mPreferences.getBoolean(httpPathMD5, false);
        // 视频文件存在并且已经下载完成
        if (videoIsFinished && isVideoExist(fileName)) {
            MLog.i(TAG, "downloadStart() 文件已经下载好了,不需要再下载");
            return;
        }

        /*LinkedList<String> list = new LinkedList<String>();
        list.add("/storage/2430-1702/");
        list = getUSBPathList(getApplicationContext(), list);
        if (list != null && !list.isEmpty()) {
            PATH = list.get(0) + "/BaiduNetdisk/video/";
        }*/

        String filePath = PATH + fileName;
        Contents.setPath(filePath);
        // /storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/军情五处-利益之争.mp4
        MLog.i(TAG, "downloadStart() filePath: " + filePath);
        File videoFile = new File(filePath);
        if (!videoFile.exists()) {
            try {
                videoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        HttpAccessor httpAccessor = null;
        try {
            httpAccessor = new HttpAccessor(new URL(httpPath), null);
            httpAccessor.open();
            contentLength = httpAccessor.getSize();
            if (mCallback != null && contentLength != -1) {
                mCallback.onProgressUpdated(contentLength);
            }
        } catch (MalformedURLException
                | ExoPlaybackException e) {
            if (httpAccessor != null) {
                try {
                    httpAccessor.close();
                } catch (ExoPlaybackException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
            return;
        }

        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(
                    new FileOutputStream(videoFile), BUFFER);
        } catch (FileNotFoundException e) {
            if (httpAccessor != null) {
                try {
                    httpAccessor.close();
                } catch (ExoPlaybackException e1) {
                    e1.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
            return;
        }

        SharedPreferences.Editor editor = null;
        editor = mPreferences.edit();
        editor.putBoolean(httpPathMD5, false);
        editor.commit();

        byte[] buffer = new byte[BUFFER];
        int mProgress = -1;
        int readSize = -1;
        long readDataSize = 0;
        mIsDownloading = true;

        MLog.i(TAG, "downloadStart() for(;;) start");
        for (; ; ) {
            if (!mIsDownloading) {
                break;
            }

            if (mIsPaused) {
                if (mCallback != null) {
                    MLog.i(TAG, "downloadStart() mCallback.onPaused()");
                    mCallback.onPaused();
                }
                synchronized (readDataPauseLock) {
                    MLog.i(TAG, "downloadStart() readDataPauseLock.wait() start");
                    try {
                        readDataPauseLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    MLog.i(TAG, "downloadStart() readDataPauseLock.wait() end");
                }
                if (mCallback != null) {
                    MLog.i(TAG, "downloadStart() mCallback.onStarted()");
                    mCallback.onStarted();
                }
            }

            try {
                Arrays.fill(buffer, (byte) 0);
                readSize = httpAccessor.read(buffer, 0, BUFFER);
                // MLog.i(TAG, "downloadStart() readSize: " + readSize);
                if (readSize < 0) {
                    break;
                }
                if (outputStream != null) {
                    outputStream.write(buffer, 0, readSize);
                }
            } catch (ExoPlaybackException
                    | IOException e) {
                e.printStackTrace();
                break;
            }

            readDataSize += readSize;
            if (mCallback != null) {
                mCallback.onProgressUpdated(readDataSize);
            }

            /*int progress = (int) ((readDataSize / (contentLength * 1.00)) * 100);
            if (progress > mProgress || progress == 100) {
                mProgress = progress;
                MLog.i(TAG, "downloadStart() progress: " + mProgress + "%");
            }*/
        }// for(;;) end
        MLog.i(TAG, "downloadStart() for(;;) end");

        mIsDownloading = false;

        try {
            httpAccessor.close();
        } catch (ExoPlaybackException e) {
            e.printStackTrace();
        }

        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        MLog.i(TAG, "downloadStart() contentLength: " + contentLength);
        MLog.i(TAG, "downloadStart()  readDataSize: " + readDataSize);
        if (mCallback != null
                && contentLength != -1
                && (contentLength == readDataSize
                || contentLength <= readDataSize + 1024 * 1024)) {
            editor = mPreferences.edit();
            editor.putBoolean(httpPathMD5, true);
            editor.commit();
            MLog.i(TAG, "downloadStart() mCallback.onFinished()");
            mCallback.onFinished();
        }

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
