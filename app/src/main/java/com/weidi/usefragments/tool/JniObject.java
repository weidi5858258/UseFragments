package com.weidi.usefragments.tool;

import java.util.ArrayList;
import java.util.Arrays;

public class JniObject {

    public Object valueObject = null;
    public Object[] valueObjectArray = null;

    public String valueString = null;
    public String[] valueStringArray = null;
    // 4 -2147483648~2147483647
    public int valueInt = 0;
    public int[] valueIntArray = null;
    // 8 -9223372036854775808~9223372036854775807
    public long valueLong = 0L;
    public long[] valueLongArray = null;
    // 1 -128~127
    public byte valueByte = 0;
    public byte[] valueByteArray = null;
    // 1 true false
    public boolean valueBoolean = false;
    public boolean[] valueBooleanArray = null;
    // 4
    public float valueFloat = 0.0f;
    public float[] valueFloatArray = null;
    // 8
    public double valueDouble = 0.0d;
    public double[] valueDoubleArray = null;
    // 2 \u0000(0)~\uffff(65535)
    public char valueChar = '\0';
    public char[] valueCharArray = null;
    // 2 -32768~32767
    public short valueShort = 0;
    public short[] valueShortArray = null;

    private ArrayList<Object> mList = null;
    private int index = -1;

    public static JniObject obtain() {
        return new JniObject();
    }

    // 调用readObject()之前需要调用一下此方法,这样就能按照存入的顺序一个一个取出值来.
    public JniObject resetIndex() {
        index = -1;
        return this;
    }

    public JniObject resetList() {
        if (mList != null) {
            mList.clear();
        } else {
            mList = new ArrayList<>();
        }
        return this;
    }

    // 线程不安全(因为使用的场景不是在多线程之间,而是java层向底层传递数据而已)
    public JniObject pushObject(Object object) {
        if (mList == null) {
            mList = new ArrayList<>();
        }
        mList.add(object);
        return this;
    }

    // 调用之前先调用resetIndex()方法
    public Object peekObject() {
        if (mList == null) {
            return null;
        }
        ++index;
        if (index >= 0 && index < mList.size()) {
            return mList.get(index);
        }
        return null;
    }

    public JniObject writeObject(Object value) {
        valueObject = value;
        return this;
    }

    public Object readObject() {
        return valueObject;
    }

    public JniObject writeString(String value) {
        valueString = value;
        return this;
    }

    public String readString() {
        return valueString;
    }

    public JniObject writeStringArray(String[] values) {
        valueStringArray = values;
        return this;
    }

    public String[] readStringArray() {
        return valueStringArray;
    }

    public JniObject writeInt(int value) {
        valueInt = value;
        return this;
    }

    public int readInt() {
        return valueInt;
    }

    public JniObject writeLong(long value) {
        valueLong = value;
        return this;
    }

    public long readLong() {
        return valueLong;
    }

    public JniObject writeByte(byte value) {
        valueByte = value;
        return this;
    }

    public byte readByte() {
        return valueByte;
    }

    public JniObject writeBoolean(boolean value) {
        valueBoolean = value;
        return this;
    }

    public boolean readBoolean() {
        return valueBoolean;
    }

    public JniObject writeFloat(float value) {
        valueFloat = value;
        return this;
    }

    public float readFloat() {
        return valueFloat;
    }

    public JniObject writeDouble(double value) {
        valueDouble = value;
        return this;
    }

    public double readDouble() {
        return valueDouble;
    }

    public JniObject writeChar(char value) {
        valueChar = value;
        return this;
    }

    public char readChar() {
        return valueChar;
    }

    public JniObject writeShort(short value) {
        valueShort = value;
        return this;
    }

    public short readShort() {
        return valueShort;
    }

    public JniObject clear() {
        valueObject = null;
        valueObjectArray = null;
        valueString = null;
        valueStringArray = null;
        valueInt = 0;
        valueIntArray = null;
        valueLong = 0L;
        valueLongArray = null;
        valueByte = 0;
        valueByteArray = null;
        valueBoolean = false;
        valueBooleanArray = null;
        valueFloat = 0.0f;
        valueFloatArray = null;
        valueDouble = 0.0d;
        valueDoubleArray = null;
        valueShort = 0;
        valueShortArray = null;
        valueChar = '\0';
        valueCharArray = null;
        if (mList != null) {
            mList.clear();
        }
        return this;
    }

    @Override
    public String toString() {
        return "JniObject{" +
                "valueObject=" + valueObject +
                ", valueObjectArray=" + Arrays.toString(valueObjectArray) +
                ", valueString='" + valueString + '\'' +
                ", valueStringArray=" + Arrays.toString(valueStringArray) +
                ", valueInt=" + valueInt +
                ", valueIntArray=" + Arrays.toString(valueIntArray) +
                ", valueLong=" + valueLong +
                ", valueLongArray=" + Arrays.toString(valueLongArray) +
                ", valueByte=" + valueByte +
                ", valueByteArray=" + Arrays.toString(valueByteArray) +
                ", valueBoolean=" + valueBoolean +
                ", valueBooleanArray=" + Arrays.toString(valueBooleanArray) +
                ", valueFloat=" + valueFloat +
                ", valueFloatArray=" + Arrays.toString(valueFloatArray) +
                ", valueDouble=" + valueDouble +
                ", valueDoubleArray=" + Arrays.toString(valueDoubleArray) +
                ", valueChar=" + valueChar +
                ", valueCharArray=" + Arrays.toString(valueCharArray) +
                ", valueShort=" + valueShort +
                ", valueShortArray=" + Arrays.toString(valueShortArray) +
                ", mList=" + mList +
                ", index=" + index +
                '}';
    }
}
