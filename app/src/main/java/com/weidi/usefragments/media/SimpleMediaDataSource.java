package com.weidi.usefragments.media;

import android.annotation.TargetApi;
import android.media.MediaDataSource;
import android.os.Build;

import java.io.IOException;

/***
 Created by root on 19-7-29.
 */

@TargetApi(Build.VERSION_CODES.M)
public class SimpleMediaDataSource extends MediaDataSource {

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size)
            throws IOException {
        return 0;
    }

    @Override
    public long getSize() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }

}
