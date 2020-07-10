package com.weidi.usefragments.fragment.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.weidi.usefragments.R;
import com.weidi.usefragments.inject.InjectOnClick;
import com.weidi.usefragments.tool.MLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/***
 框架模板类
 */
public class UsbFragment extends BaseFragment {

    private static final String TAG =
            UsbFragment.class.getSimpleName();
    private static final boolean DEBUG = true;

    public UsbFragment() {
        super();
    }

    /*********************************
     * Created
     *********************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG)
            MLog.d(TAG, "onAttach() " + printThis() +
                    " mContext: " + context);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (DEBUG)
            MLog.d(TAG, "onAttach() " + printThis() +
                    " activity: " + activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onCreate() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        initData();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        if (DEBUG)
            MLog.d(TAG, "onCreateView() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewCreated() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);

        initView(view, savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onViewStateRestored() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG)
            MLog.d(TAG, "onActivityCreated() " + printThis() +
                    " savedInstanceState: " + savedInstanceState);
    }

    /*********************************
     * Started
     *********************************/

    @Override
    public void onStart() {
        super.onStart();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onStart() " + printThis());
    }

    /*********************************
     * Resumed
     *********************************/

    @Override
    public void onResume() {
        super.onResume();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onResume() " + printThis());

        onShow();
    }

    /*********************************
     * Paused
     *********************************/

    @Override
    public void onPause() {
        super.onPause();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onPause() " + printThis());
    }

    /*********************************
     * Stopped
     *********************************/

    @Override
    public void onStop() {
        super.onStop();
        if (isHidden()) {
            return;
        }
        if (DEBUG)
            MLog.d(TAG, "onStop() " + printThis());

        onHide();
    }

    /*********************************
     * Destroyed
     *********************************/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG)
            MLog.d(TAG, "onDestroyView() " + printThis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG)
            MLog.d(TAG, "onDestroy() " + printThis());

        destroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG)
            MLog.d(TAG, "onDetach() " + printThis());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DEBUG)
            MLog.d(TAG, "onActivityResult(): " + printThis() +
                    " requestCode: " + requestCode +
                    " resultCode: " + resultCode +
                    " data: " + data.toString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG)
            MLog.d(TAG, "onSaveInstanceState() " + printThis());
    }

    @Override
    public void handleConfigurationChangedEvent(
            Configuration newConfig,
            boolean needToDo,
            boolean override) {
        handleBeforeOfConfigurationChangedEvent();

        super.handleConfigurationChangedEvent(newConfig, needToDo, true);

        if (needToDo) {
            onShow();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DEBUG)
            MLog.d(TAG, "onLowMemory() " + printThis());
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DEBUG)
            MLog.d(TAG, "onTrimMemory() " + printThis() +
                    " level: " + level);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (DEBUG)
            MLog.d(TAG, "onRequestPermissionsResult() " + printThis() +
                    " requestCode: " + requestCode);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (DEBUG)
            MLog.d(TAG, "onHiddenChanged() " + printThis() +
                    " hidden: " + hidden);

        if (hidden) {
            onHide();
        } else {
            onShow();
        }
    }

    @Override
    protected int provideLayout() {
        return R.layout.fragment_main;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /////////////////////////////////////////////////////////////////

    /***
     现在只针特定的某个设备
     一个设备(UsbDevice)有一个或多个接口(UsbInterface),
     一个接口(UsbInterface)有一个或多个节点(UsbEndpoint).
     设备之间的通讯是通过节点进行的.
     枚举设备->找到设备的接口->连接设备->分配相应的端点->在IN端点进行读操作,在OUT端点进行写操作.
     */
    private UsbManager mUsbManager;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbDevice mUsbDevice;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mEpOut;
    private UsbEndpoint mEpIn;

    /***
     代码执行的内容跟onStart(),onResume()一样,
     因此在某些情况下要么执行onStart(),onResume()方法,要么执行onShow()方法.
     一般做的事是设置View的内容
     */
    private void onShow() {
        if (DEBUG)
            MLog.d(TAG, "onShow() " + printThis());

    }

    /***
     代码执行的内容跟onPause(),onStop()一样,
     因此在某些情况下要么执行onPause(),onStop()方法,要么执行onHide()方法.
     一般做的事是视频的暂停,摄像头的关闭
     */
    private void onHide() {
        if (DEBUG)
            MLog.d(TAG, "onHide() " + printThis());
    }

    private void initData() {
        mUsbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = mUsbManager.getDeviceList();
        UsbAccessory[] usbAccessories = mUsbManager.getAccessoryList();

        if (DEBUG)
            MLog.d(TAG, "initData() " + printThis() +
                    " usb设备: " + String.valueOf(usbDevices.size()));
        // 找设备
        Iterator<UsbDevice> iterator = usbDevices.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();

            // 在这里添加处理设备的代码
            // USB设备VID\PID
            if (device.getVendorId() == 1155 && device.getProductId() == 22352) {
                mUsbDevice = device;
                break;
            }
        }

        // 找设备的接口
        if (mUsbDevice != null) {
            if (DEBUG)
                MLog.d(TAG, "initData() " + printThis() +
                        " interfaceCounts: " + mUsbDevice.getInterfaceCount());
            for (int i = 0; i < mUsbDevice.getInterfaceCount(); i++) {
                UsbInterface usbInterface = mUsbDevice.getInterface(i);
                // 根据手上的设备做一些判断，其实这些信息都可以在枚举到设备时打印出来
                if (usbInterface.getInterfaceClass() == 8
                        && usbInterface.getInterfaceSubclass() == 6
                        && usbInterface.getInterfaceProtocol() == 80) {
                    mUsbInterface = usbInterface;
                    break;
                }
            }
        }

        // 打开设备(用好后记得关闭)
        if (mUsbInterface != null) {
            // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限，可以查阅相关资料
            // 在打开USB设备的时候是需要申请USB连接权限的，而且申请权限的次数是根据连接的设备来确定。
            if (!mUsbManager.hasPermission(mUsbDevice)) {
                return;
            }
            mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
            if (mUsbDeviceConnection == null) {
                return;
            }
            // 到此你的android设备已经连上HID设备
            if (!mUsbDeviceConnection.claimInterface(mUsbInterface, true)) {
                mUsbDeviceConnection.close();
                return;
            }
        }

        /***
         分配端点，IN | OUT，即输入输出；此处我直接用1为OUT端点，0为IN，当然你也可以通过判断.
         #define USB_ENDPOINT_XFER_CONTROL 0 --控制传输
         #define USB_ENDPOINT_XFER_ISOC    1 --等时传输
         #define USB_ENDPOINT_XFER_BULK    2 --块传输
         #define USB_ENDPOINT_XFER_INT     3 --中断传输
         */
        // 这一句不加的话 很容易报错  导致很多人在各大论坛问:为什么报错呀
        if (mUsbInterface != null) {
            // 这里的代码替换了一下 按自己硬件属性判断吧
            for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
                UsbEndpoint ep = mUsbInterface.getEndpoint(i);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        mEpOut = ep;
                    } else {
                        mEpIn = ep;
                    }
                }
                switch (ep.getType()) {
                    case UsbConstants.USB_ENDPOINT_XFER_BULK://USB端口传输
                        if (UsbConstants.USB_DIR_OUT == ep.getDirection()) {//输出
                            Log.e(TAG, "获取发送数据的端点");
                        } else {
                            Log.e(TAG, "获取接受数据的端点");
                        }
                        break;
                    case UsbConstants.USB_ENDPOINT_XFER_CONTROL://控制
                        Log.e(TAG, "find the ControlEndPoint:" + "index:" + i +
                                "," + ep.getEndpointNumber());
                        break;
                    case UsbConstants.USB_ENDPOINT_XFER_INT://中断
                        if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {//输出
                            Log.e(TAG, "find the InterruptEndpointOut:" + "index:" + i +
                                    "," + ep.getEndpointNumber());
                        } else if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                            Log.e(TAG, "find the InterruptEndpointIn:" + "index:" + i +
                                    "," + ep.getEndpointNumber());
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        /***
         获取数据的代码
         int count = mUsbDeviceConnection.bulkTransfer(mEpIn, buffer, buffer.length, 100);
         这里注意数据读取的长度, 这个对某些硬件来说非常重要,  有的硬件小了或者大了立马死机重启, 要么就是一直返回-1
         这个数据的长度需要根据硬件属性来,
         有一种办法是通过int inMax = epIn.getMaxPacketSize()来获取;
         还有一种办法是通过windows下面的hid程序探测设备的数据长度;
         */

    }

    private void initView(View view, Bundle savedInstanceState) {

    }

    private void handleBeforeOfConfigurationChangedEvent() {

    }

    private void destroy() {

    }

    @InjectOnClick({R.id.jump_btn})
    private void onClick(View v) {
        switch (v.getId()) {
            case R.id.jump_btn:
                break;
        }
    }

    /***
     仅仅发送一个命令到HID设备，其实际进行了三次命令的发送接收，两OUT一IN，总共调用了三次bulkTransfer()方法
     */
    private void sendPackage(byte[] command) {
        int ret = -100;
        int len = command.length;

        // 组织准备命令
        // byte[] sendOut = Commands.OUT;
        // byte[] sendIn = Commands.IN;
        byte[] sendIN = new byte[20];
        byte[] sendOut = new byte[20];
        sendOut[8] = (byte) (len & 0xff);
        sendOut[9] = (byte) ((len >> 8) & 0xff);
        sendOut[10] = (byte) ((len >> 16) & 0xff);
        sendOut[11] = (byte) ((len >> 24) & 0xff);

        // 1,发送准备命令
        ret = mUsbDeviceConnection.bulkTransfer(mEpOut, sendOut, 31, 10000);
        if (ret != 31) {
            return;
        }

        // 2,发送COM
        ret = mUsbDeviceConnection.bulkTransfer(mEpOut, command, len, 10000);
        if (ret != len) {
            return;
        }

        // 3,接收发送成功信息
        ret = mUsbDeviceConnection.bulkTransfer(mEpIn, sendIN, 13, 10000);
        if (ret != 13) {
            return;
        }
    }

}
