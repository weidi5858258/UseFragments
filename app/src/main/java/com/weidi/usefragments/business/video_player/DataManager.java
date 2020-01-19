package com.weidi.usefragments.business.video_player;

import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import static com.weidi.usefragments.business.video_player.SimpleVideoPlayer8.MAX_CACHE_AUDIO_COUNT;
import static com.weidi.usefragments.business.video_player.SimpleVideoPlayer8.MAX_CACHE_VIDEO_COUNT;
import static com.weidi.usefragments.business.video_player.SimpleVideoPlayer8.TYPE_AUDIO;

/**
 * Created by alexander on 2020/1/19.
 */

public class DataManager {

    private static final boolean needToUseExtraData = true;

    public int type;

    // 可能有多个
    private IMediaDataInterface iMediaDataStub1;

    public int count1 = 0;
    public int count2 = 0;
    public boolean handleDataFull = false;
    public boolean readDataFull = false;
    private List<AVPacket> readData = new ArrayList<AVPacket>(MAX_CACHE_AUDIO_COUNT);
    private List<AVPacket> handleData = new ArrayList<AVPacket>(MAX_CACHE_VIDEO_COUNT);

    public void addData(AVPacket data) throws RemoteException {
        if (needToUseExtraData) {
            // 本地没满
            if (!readDataFull) {
                readData.add(data);

                int size = readData.size();
                if (size == count1) {
                    //
                    readDataFull = true;
                } else if (size == count2) {
                    // 通知进行handle操作
                }
            } else {
                // 本地满了
                if (type == TYPE_AUDIO) {
                    if (!iMediaDataStub1.isAudioFull()) {

                    }
                } else {

                }
            }
        }
    }

    public AVPacket getData() {
        return null;
    }

    public AVPacket getData(int index) {
        return null;
    }

    public boolean isFull() {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

}
