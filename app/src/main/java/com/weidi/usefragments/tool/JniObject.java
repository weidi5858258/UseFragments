package com.weidi.usefragments.tool;

import java.util.Arrays;

public class JniObject {

    public String valueString = null;
    // 8
    public long valueLong = 0;
    // 4
    public int valueInt = 0;
    // 2
    public short valueShort = 0;
    // 2
    public char valueChar = '\0';
    // 1
    public boolean valueBoolean = false;
    // 1
    public byte valueByte = 0;
    // 8
    public double valueDouble = 0.0;
    // 4
    public float valueFloat = 0.0f;

    public String[] valueStringArray = null;

    public long[] valueLongArray = null;

    public int[] valueIntArray = null;

    public short[] valueShortArray = null;

    public char[] valueCharArray = null;

    public boolean[] valueBooleanArray = null;

    public byte[] valueByteArray = null;

    public double[] valueDoubleArray = null;

    public float[] valueFloatArray = null;

    @Override
    public String toString() {
        return "JniObject{" +
                "valueString='" + valueString + '\'' +
                ", valueLong=" + valueLong +
                ", valueInt=" + valueInt +
                ", valueShort=" + valueShort +
                ", valueChar=" + valueChar +
                ", valueBoolean=" + valueBoolean +
                ", valueByte=" + valueByte +
                ", valueDouble=" + valueDouble +
                ", valueFloat=" + valueFloat +
                ", valueStringArray=" + Arrays.toString(valueStringArray) +
                ", valueLongArray=" + Arrays.toString(valueLongArray) +
                ", valueIntArray=" + Arrays.toString(valueIntArray) +
                ", valueShortArray=" + Arrays.toString(valueShortArray) +
                ", valueCharArray=" + Arrays.toString(valueCharArray) +
                ", valueBooleanArray=" + Arrays.toString(valueBooleanArray) +
                ", valueByteArray=" + Arrays.toString(valueByteArray) +
                ", valueDoubleArray=" + Arrays.toString(valueDoubleArray) +
                ", valueFloatArray=" + Arrays.toString(valueFloatArray) +
                '}';
    }
}
