package com.weidi.usefragments.business.video_player;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;


import com.weidi.eventbus.EventBusUtils;
import com.weidi.usefragments.tool.MLog;

import java.util.ArrayList;
import java.util.List;

import static com.weidi.usefragments.business.video_player.SimpleVideoPlayer8.MAX_CACHE_AUDIO_COUNT;
import static com.weidi.usefragments.business.video_player.SimpleVideoPlayer8.MAX_CACHE_VIDEO_COUNT;

/**
 * Created by root on 19-7-1.
 */

public class MediaDataService extends Service {

    private static final String TAG =
            MediaDataService.class.getSimpleName();


    @Override
    public IBinder onBind(Intent intent) {
        MLog.d(TAG, "onBind()");
        return iMediaDataStub;
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
        initData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MLog.d(TAG, "onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        MLog.d(TAG, "onDestroy()");
        super.onDestroy();
        destroy();
    }

    ////////////////////////////////////////////////////////////////////////

    private boolean mIsAudioFull = false;
    private boolean mIsVideoFull = false;
    private List<AVPacket> mAudioData = new ArrayList<AVPacket>(MAX_CACHE_AUDIO_COUNT);
    private List<AVPacket> mVideoData = new ArrayList<AVPacket>(MAX_CACHE_VIDEO_COUNT);

    private void initData() {
        EventBusUtils.register(this);
    }

    private void destroy() {
        EventBusUtils.unregister(this);
    }

    private Object onEvent(int what, Object[] objArray) {
        Object result = null;
        switch (what) {
            default:
                break;
        }
        return result;
    }

    private final IMediaDataInterface.Stub iMediaDataStub = new IMediaDataInterface.Stub() {

        @Override
        public boolean isAudioFull() throws RemoteException {
            return mIsAudioFull;
        }

        @Override
        public boolean isVideoFull() throws RemoteException {
            return mIsVideoFull;
        }

        @Override
        public void setAudioFull(boolean isFull) throws RemoteException {
            mIsAudioFull = isFull;
        }

        @Override
        public void setVideoFull(boolean isFull) throws RemoteException {
            mIsVideoFull = isFull;
        }

        @Override
        public void addAudioData(AVPacket data) throws RemoteException {
            mAudioData.add(data);
        }

        @Override
        public void addVideoData(AVPacket data) throws RemoteException {
            mVideoData.add(data);
        }

        @Override
        public AVPacket getAudioData(int index) throws RemoteException {
            if (index < 0 || index >= mAudioData.size()) {
                return null;
            }

            return mAudioData.get(index);
        }

        @Override
        public AVPacket getVideoData(int index) throws RemoteException {
            if (index < 0 || index >= mVideoData.size()) {
                return null;
            }

            return mVideoData.get(index);
        }

        @Override
        public int audioSize() throws RemoteException {
            return mAudioData.size();
        }

        @Override
        public int videoSize() throws RemoteException {
            return mVideoData.size();
        }

        @Override
        public void audioClear() throws RemoteException {
            if (!mAudioData.isEmpty()) {
                for (AVPacket data : mAudioData) {
                    data.clear();
                    data = null;
                }
            }
            mAudioData.clear();
            System.gc();
        }

        @Override
        public void videoClear() throws RemoteException {
            if (!mVideoData.isEmpty()) {
                for (AVPacket data : mVideoData) {
                    data.clear();
                    data = null;
                }
            }
            mVideoData.clear();
            System.gc();
        }
    };

}
