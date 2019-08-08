package com.weidi.usefragments.tool;

import com.weidi.usefragments.javabean.Person;

/***
 Created by ex-wangliwei on 2016/2/14.
 用于存放本地方法
 */
public class JniUtils {

    static {
        try {
            System.loadLibrary("alexander_jni");
        } catch (java.lang.UnsatisfiedLinkError error) {
            error.printStackTrace();
        }
    }

    // 传递基本数据类型
    public static native String getStringFromC(String str);

    public static native int getIntFromC(int in);

    public static native byte getByteFromC(byte bt);

    public static native char getCharFromC(char ch);

    public static native boolean getBooleanFromC(boolean bool);

    public static native short getShortFromC(short sh);

    public static native long getLongFromC(long lg);

    public static native float getFloatFromC(float ft);

    public static native double getDoubleFromC(double db);

    // 传递基本数据类型的数组
    public static native String[] getStringArrayFromC(String[] strA);

    public static native int[] getIntArrayFromC(int[] inA);

    public static native byte[] getByteArrayFromC(byte[] btA);

    public static native char[] getCharArrayFromC(char[] chA);

    public static native boolean[] getBooleanArrayFromC(boolean[] boolA);

    public static native short[] getShortArrayFromC(short[] shA);

    public static native long[] getLongArrayFromC(long[] lgA);

    public static native float[] getFloatArrayFromC(float[] ftA);

    public static native double[] getDoubleArrayFromC(double[] dbA);

    // 传递java端的对象
    public static native Person getObjectFromC(Person person);

    public static native Person[] getObjectArrayFromC(Person[] persons);

}
