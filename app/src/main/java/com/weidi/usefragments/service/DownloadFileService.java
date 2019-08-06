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
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.tool.Contents;
import com.weidi.usefragments.tool.DownloadCallback;
import com.weidi.usefragments.tool.ExoPlaybackException;
import com.weidi.usefragments.tool.HttpAccessor;
import com.weidi.usefragments.tool.MLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
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
    private static final int BUFFER = 1024 * 1024 * 2;

    public static final String PATH =
            "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";
    /*PATH = "/data/data/com.weidi.usefragments/files/";
    PATH = "/storage/37C8-3904/Android/data/com.weidi.usefragments/files/Movies/";
    PATH = "/storage/2430-1702/Android/data/com.weidi.usefragments/files/Movies/";*/
    public static final String PREFERENCES_NAME = "alexander_preferences";
    public static final String VIDEO_NAME = "video_name";
    public static final String VIDEO_PATH = "video_path";
    private static final String VIDEO_LENGTH = "video_length";
    private static final String VIDEO_POSITION = "video_position";
    private static final String VIDEO_IS_FINISHED = "video_is_finished";
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

    private boolean isVideoExist() {
        boolean isExist = false;
        File moviesFile = new File(PATH);
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
                mUri = null;
                if (objArray != null && objArray.length > 0) {
                    mUri = (String) objArray[0];
                }

                if (mIsDownloading) {
                    mUiHandler.removeMessages(MSG_DOWNLOAD_STOP);
                    mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_STOP);

                    mThreadHandler.removeMessages(MSG_DOWNLOAD_START);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD_START, 1000);
                } else {
                    mThreadHandler.removeMessages(MSG_DOWNLOAD_START);
                    mThreadHandler.sendEmptyMessage(MSG_DOWNLOAD_START);
                }
                break;
            case MSG_DOWNLOAD_PAUSE_OR_START:
                mUiHandler.removeMessages(MSG_DOWNLOAD_PAUSE_OR_START);
                mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_PAUSE_OR_START);
                break;
            case MSG_DOWNLOAD_STOP:
                mUiHandler.removeMessages(MSG_DOWNLOAD_STOP);
                mUiHandler.sendEmptyMessage(MSG_DOWNLOAD_STOP);
                break;
            case MSG_IS_DOWNLOADING:
                return mIsDownloading;
            case MSG_SET_CALLBACK:
                if (objArray != null && objArray.length > 0) {
                    mCallback = (DownloadCallback) objArray[0];
                } else if (objArray == null) {
                    mCallback = null;
                }
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
        if (mCallback != null) {
            mCallback.onReady();
        }

        contentLength = -1;
        mIsDownloading = true;
        String httpPath = null;
        if (!TextUtils.isEmpty(mUri)) {
            httpPath = mUri;
        } else {
            httpPath = Contents.getUri();
        }

        long startPosition = 0;
        String videoPath = mPreferences.getString(VIDEO_PATH, "");
        if (TextUtils.equals(httpPath, videoPath)) {
            startPosition = mPreferences.getLong(VIDEO_POSITION, 0);
            boolean videoIsFinished = mPreferences.getBoolean(VIDEO_IS_FINISHED, false);
            if (videoIsFinished && isVideoExist()) {
                mIsDownloading = true;
                MLog.i(TAG, "downloadStart() 文件已经下载好了,不需要再下载");
                return;
            }
        }
        // 虽然断点下载是可以了,但是下载的文件不能播放
        startPosition = 0;
        MLog.i(TAG, "downloadStart() startPosition: " + startPosition);

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

        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(VIDEO_NAME, fileName);
        editor.commit();

        // Test
        fileName = "alexander_mylove" + suffixName;

        String filePath = PATH + fileName;
        Contents.setPath(filePath);
        MLog.i(TAG, "downloadStart() filePath: " + filePath);
        File videoFile = new File(filePath);
        if (videoFile.exists()) {
            videoFile.delete();
        }
        if (!videoFile.exists()) {
            try {
                videoFile.createNewFile();
            } catch (IOException e) {
                mIsDownloading = false;
                e.printStackTrace();
                return;
            }
        }
        HttpAccessor httpAccessor = null;
        try {
            httpAccessor = new HttpAccessor(new URL(httpPath), null);
            httpAccessor.open();
            httpAccessor.byteSeek(startPosition);
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
            httpAccessor = null;
            mIsDownloading = false;
            e.printStackTrace();
            return;
        }

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(videoFile, "rwd");
            /*if (contentLength != -1) {
                // 直接生成一个contentLength大小的文件
                randomAccessFile.setLength(contentLength);
            }*/
            randomAccessFile.seek(startPosition);
        } catch (IOException e) {
            if (httpAccessor != null) {
                try {
                    httpAccessor.close();
                } catch (ExoPlaybackException e1) {
                    e1.printStackTrace();
                }
            }
            httpAccessor = null;
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            randomAccessFile = null;
            mIsDownloading = false;
            e.printStackTrace();
            return;
        }

        editor = mPreferences.edit();
        editor.putString(VIDEO_PATH, httpPath);
        editor.putLong(VIDEO_LENGTH, contentLength);
        editor.putLong(VIDEO_POSITION, 0);
        editor.putBoolean(VIDEO_IS_FINISHED, false);
        editor.commit();

        byte[] buffer = new byte[BUFFER];
        int readSize = -1;
        long readDataSize = 0;
        readDataSize += startPosition;

        long startTime = SystemClock.elapsedRealtime();
        for (; ; ) {
            if (!mIsDownloading) {
                break;
            }

            if (mIsPaused) {
                if (mCallback != null) {
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
                    mCallback.onStarted();
                }
            }

            try {
                Arrays.fill(buffer, (byte) 0);
                readSize = httpAccessor.read(buffer, 0, BUFFER);
                // MLog.i(TAG, "downloadStart() readSize: " + readSize);
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
                randomAccessFile.write(buffer, 0, readSize);
            } catch (IOException e) {
                e.printStackTrace();
                mIsDownloading = false;
                break;
            }

            readDataSize += readSize;
            if (mCallback != null) {
                mCallback.onProgressUpdated(readDataSize);
            }
            long endTime = SystemClock.elapsedRealtime();
            if (endTime - startTime >= 1000) {
                // MLog.i(TAG, "downloadStart() readDataSize: " + readDataSize);
                startTime = endTime;
                editor = mPreferences.edit();
                editor.putLong(VIDEO_POSITION, readDataSize);
                editor.commit();
            }
        }// for(;;) end

        try {
            httpAccessor.close();
        } catch (ExoPlaybackException e) {
            e.printStackTrace();
        }

        try {
            randomAccessFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mIsDownloading = false;

        MLog.i(TAG, "downloadStart() contentLength: " + contentLength);
        MLog.i(TAG, "downloadStart()  readDataSize: " + readDataSize);
        if (mCallback != null
                && contentLength != -1
                && (contentLength == readDataSize
                || contentLength <= readDataSize + 1024 * 1024)) {
            editor = mPreferences.edit();
            editor.putBoolean(VIDEO_IS_FINISHED, true);
            editor.putLong(VIDEO_POSITION, contentLength);
            editor.commit();
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
