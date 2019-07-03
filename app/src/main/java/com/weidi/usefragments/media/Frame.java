package com.weidi.usefragments.media;

/***
 Created by root on 19-7-3.
 一帧的数据
 */

public class Frame {
    public byte[] mData;
    public int offset;
    public int length;

    public Frame(byte[] data, int offset, int size) {
        this.mData = data;
        this.offset = offset;
        this.length = size;
    }

    public void setFrame(byte[] data, int offset, int size) {
        this.mData = data;
        this.offset = offset;
        this.length = size;
    }
}
