package com.weidi.usefragments.socket;

import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;

/***
 Created by root on 19-7-3.
 */

public class SocketClient {

    private static final String TAG =
            SocketClient.class.getSimpleName();
    private static final boolean DEBUG = true;

    private Socket mSocket;


    public void connect() {
        try {
            mSocket = new Socket(SocketServer.IP, SocketServer.PORT);
            if (mSocket.isConnected()) {

            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (mSocket != null
                    && mSocket.isConnected()
                    && !mSocket.isClosed()) {
                mSocket.close();
                mSocket = null;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
