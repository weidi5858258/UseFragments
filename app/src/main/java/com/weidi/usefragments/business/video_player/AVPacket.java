package com.weidi.usefragments.business.video_player;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * Created by alexander on 2020/1/17.
 */

public class AVPacket implements Parcelable {

    public byte[] data;
    public int size;
    public long sampleTime;

    public AVPacket(int size, long sampleTime) {
        data = new byte[size];
        this.size = size;
        this.sampleTime = sampleTime;
    }

    public void clear() {
        Arrays.fill(data, (byte) 0);
        data = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        in.readByteArray(this.data);
        this.size = in.readInt();
        this.sampleTime = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.data);
        dest.writeInt(this.size);
        dest.writeLong(this.sampleTime);
    }

    protected AVPacket(Parcel in) {
        this.data = in.createByteArray();
        this.size = in.readInt();
        this.sampleTime = in.readLong();
    }

    public static final Creator<AVPacket> CREATOR = new Creator<AVPacket>() {
        @Override
        public AVPacket createFromParcel(Parcel source) {
            return new AVPacket(source);
        }

        @Override
        public AVPacket[] newArray(int size) {
            return new AVPacket[size];
        }
    };
}
