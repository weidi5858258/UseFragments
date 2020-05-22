package com.weidi.usefragments.tool;

import java.util.Arrays;

public class JniObject {

    public Object valueObject = null;

    public String valueString = null;
    // 8
    public long valueLong = 0L;
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

    public Object[] valueObjectArray = null;

    public String[] valueStringArray = null;

    public long[] valueLongArray = null;

    public int[] valueIntArray = null;

    public short[] valueShortArray = null;

    public char[] valueCharArray = null;

    public boolean[] valueBooleanArray = null;

    public byte[] valueByteArray = null;

    public double[] valueDoubleArray = null;

    public float[] valueFloatArray = null;

    public void clear() {
        valueObject = null;
        valueString = null;
        valueLong = 0L;
        valueInt = 0;
        valueShort = 0;
        valueChar = '\0';
        valueBoolean = false;
        valueByte = 0;
        valueDouble = 0.0;
        valueFloat = 0.0f;
        valueObjectArray = null;
        valueStringArray = null;
        valueLongArray = null;
        valueIntArray = null;
        valueShortArray = null;
        valueCharArray = null;
        valueBooleanArray = null;
        valueByteArray = null;
        valueDoubleArray = null;
        valueFloatArray = null;
    }

    @Override
    public String toString() {
        return "JniObject{" +
                "valueObject=" + valueObject +
                ", valueString='" + valueString + '\'' +
                ", valueLong=" + valueLong +
                ", valueInt=" + valueInt +
                ", valueShort=" + valueShort +
                ", valueChar=" + valueChar +
                ", valueBoolean=" + valueBoolean +
                ", valueByte=" + valueByte +
                ", valueDouble=" + valueDouble +
                ", valueFloat=" + valueFloat +
                ", valueObjectArray=" + Arrays.toString(valueObjectArray) +
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
