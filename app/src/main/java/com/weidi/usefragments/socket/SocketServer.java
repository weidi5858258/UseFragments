package com.weidi.usefragments.socket;

import com.weidi.usefragments.tool.MLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/***
 Created by root on 19-7-3.
 */

public class SocketServer {

    private static final String TAG =
            SocketServer.class.getSimpleName();
    private static final boolean DEBUG = true;

    // 127.0.0.1
    public static final String IP = "192.168.1.105";
    public static final int PORT = 5858;
    private static volatile SocketServer sSocketServer;
    private static volatile ServerSocket sServerSocket;
    private Socket mSocket;

    private SocketServer() {
        if (sServerSocket == null) {
            try {
                sServerSocket = new ServerSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static SocketServer getInstance() {
        if (sSocketServer == null) {
            synchronized (SocketServer.class) {
                if (sSocketServer == null) {
                    sSocketServer = new SocketServer();
                }
            }
        }
        return sSocketServer;
    }

    public boolean bind() {
        if (DEBUG)
            MLog.d(TAG, "bind()");
        try {
            InetSocketAddress inetSocketAddress =
                    new InetSocketAddress(IP, PORT);
            sServerSocket.bind(inetSocketAddress);
            if (DEBUG)
                MLog.d(TAG, "bind() succeeded");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void accept() {
        if (DEBUG)
            MLog.d(TAG, "accept()");
        try {
            mSocket = sServerSocket.accept();
            if (mSocket != null && mSocket.isConnected()) {
                if (DEBUG)
                    MLog.d(TAG, "accept() " + mSocket.toString());
            }
        } catch (java.nio.channels.IllegalBlockingModeException e) {
            e.printStackTrace();
            mSocket = null;
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            mSocket = null;
        } catch (SecurityException e) {
            e.printStackTrace();
            mSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
            mSocket = null;
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void close() {
        if (sServerSocket != null
                && !sServerSocket.isClosed()) {
            try {
                sServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*public static void main(String args[]) {
        SocketServer.getInstance();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SocketServer.getInstance().accept();
            }
        }).start();
    }*/

}
