package com.weidi.usefragments.business.audio_player;

import android.content.Context;

import com.weidi.usefragments.tool.MLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by alexander on 2020/2/4.
 */

public class PlaybackQueue {

    private static final String TAG =
            PlaybackQueue.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String PATH = "/storage/37C8-3904/myfiles/music/";
    //private static final String PATH = "/storage/emulated/0/kgmusic/download/";
    //    private static final String PATH = "/storage/2430-1702/BaiduNetdisk/music/mylove/";
    //    private static final String PATH = "/storage/2430-1702/BaiduNetdisk/music/test_audio/";
    // /storage/2430-1702/BaiduNetdisk/music/谭咏麟 - 水中花.mp3

    // 顺序播放
    private static final int SEQUENTIAL_PLAYBACK = 0x001;
    // 随机播放
    private static final int RANDOM_PLAYBACK = 0x002;
    // 循环播放
    private static final int LOOP_PLAYBACK = 0x003;

    private int mPlayMode = RANDOM_PLAYBACK;

    private Context mContext = null;
    private int mCurMusicIndex = -1;
    private File mCurMusicFile = null;
    private List<File> musicFiles = new ArrayList<File>();
    private List<Integer> mHasPlayed = new ArrayList<Integer>();
    private Random mRandom = new Random();

    public PlaybackQueue(Context context) {
        mContext = context;

        File file = new File(PATH);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            ArrayList<File> tempList = new ArrayList<File>();
            for (File f : files) {
                if (f != null && f.isFile()) {
                    tempList.add(f);
                }
            }
            if (!tempList.isEmpty()) {
                musicFiles.addAll(tempList);
            }
        }
    }

    public String prev() {
        if (musicFiles == null
                || musicFiles.isEmpty()) {
            return null;
        }

        switch (mPlayMode) {
            case SEQUENTIAL_PLAYBACK:
                mCurMusicIndex -= 1;
                if (mCurMusicIndex < 0) {
                    mCurMusicIndex = musicFiles.size() - 1;
                }
                mCurMusicFile = musicFiles.get(mCurMusicIndex);
                break;
            case RANDOM_PLAYBACK:
                int size = musicFiles.size();
                while (true) {
                    int randomNumber = mRandom.nextInt(size);
                    if (!mHasPlayed.contains(randomNumber)) {
                        mHasPlayed.add(randomNumber);
                        mCurMusicIndex = randomNumber;
                        mCurMusicFile = musicFiles.get(mCurMusicIndex);
                        break;
                    } else {
                        if (mHasPlayed.size() == size) {
                            mHasPlayed.clear();
                        }
                    }
                }
                break;
            case LOOP_PLAYBACK:
                break;
            default:
                break;
        }
        if (DEBUG)
            MLog.d(TAG, "prev() mCurMusicIndex: " + mCurMusicIndex +
                    " " + mCurMusicFile.getAbsolutePath());
        return mCurMusicFile.getAbsolutePath();
    }

    public String next() {
        if (musicFiles == null
                || musicFiles.isEmpty()) {
            return null;
        }

        switch (mPlayMode) {
            case SEQUENTIAL_PLAYBACK:
                mCurMusicIndex += 1;
                if (mCurMusicIndex >= musicFiles.size()) {
                    mCurMusicIndex = 0;
                }
                mCurMusicFile = musicFiles.get(mCurMusicIndex);
                break;
            case RANDOM_PLAYBACK:
                int size = musicFiles.size();
                while (true) {
                    int randomNumber = mRandom.nextInt(size);
                    if (!mHasPlayed.contains(randomNumber)) {
                        mHasPlayed.add(randomNumber);
                        mCurMusicIndex = randomNumber;
                        mCurMusicFile = musicFiles.get(mCurMusicIndex);
                        break;
                    } else {
                        if (mHasPlayed.size() == size) {
                            mHasPlayed.clear();
                        }
                    }
                }
                break;
            case LOOP_PLAYBACK:
                break;
            default:
                break;
        }
        if (DEBUG)
            MLog.d(TAG, "nextHandle() mCurMusicIndex: " + mCurMusicIndex +
                    " " + mCurMusicFile.getAbsolutePath());
        return mCurMusicFile.getAbsolutePath();
    }

}
