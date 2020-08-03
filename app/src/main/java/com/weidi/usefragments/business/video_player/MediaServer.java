package com.weidi.usefragments.business.video_player;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/***
 2020/08/03
 */
public class MediaServer {

    private static final String TAG =
            "player_alexander";

    public static final int PORT = 8890;
    public static final String AUDIOTRACK_INFO_TAG = "@@@@@@";
    private ConcurrentHashMap<Socket, OutputStream> map =
            new ConcurrentHashMap<Socket, OutputStream>();

    private volatile static MediaServer sMediaServer;

    private ServerSocket server;
    public boolean mIsHandling = false;

    private MediaServer() {
    }

    public static MediaServer getInstance() {
        if (sMediaServer == null) {
            synchronized (MediaServer.class) {
                if (sMediaServer == null) {
                    sMediaServer = new MediaServer();
                }
            }
        }
        return sMediaServer;
    }

    // child thread
    public void sccept() {
        if (mIsHandling || server != null) {
            Log.d(TAG, "MediaServer sccept() return");
            return;
        }

        try {
            mIsHandling = false;
            server = new ServerSocket(PORT);
            mIsHandling = true;
        } catch (IOException e) {
            // java.net.BindException: bind failed: EADDRINUSE (Address already in use)
            // Caused by: android.system.ErrnoException:
            e.printStackTrace();
            Log.e(TAG, "MediaServer new ServerSocket failure");
            close();
            return;
        }

        clearMap();
        Socket socket = null;
        OutputStream outputStream = null;
        while (mIsHandling) {
            try {
                Log.i(TAG, "MediaServer sccept() start");
                socket = server.accept();
                Log.i(TAG, "MediaServer sccept() end");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "MediaServer sccept() failure");
                break;
            }
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                closeSocket(socket);
                socket = null;
                outputStream = null;
                Log.e(TAG, "MediaServer getOutputStream() failure");
                continue;
            }

            map.put(socket, outputStream);
        }
        clearMap();
    }

    public void sendData(byte[] data, int offsetInBytes, int sizeInBytes) {
        if (map == null || map.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Socket, OutputStream>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Socket, OutputStream> entry = iterator.next();
            Socket socket = entry.getKey();
            OutputStream outputStream = entry.getValue();
            if (socket == null
                    || outputStream == null
                    || !socket.isConnected()
                    || socket.isClosed()) {
                iterator.remove();
                continue;
            }

            try {
                outputStream.write(data, offsetInBytes, sizeInBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void close() {
        Log.i(TAG, "MediaServer close() start");
        mIsHandling = false;
        clearMap();
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        server = null;
        Log.i(TAG, "MediaServer close() end");
    }

    private void clearMap() {
        if (map.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Socket, OutputStream>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Socket, OutputStream> entry = iterator.next();
            Socket socket = entry.getKey();
            OutputStream outputStream = entry.getValue();
            if (socket == null
                    || outputStream == null
                    || !socket.isConnected()
                    || socket.isClosed()) {
                continue;
            }

            closeSocket(socket);
            socket = null;
            outputStream = null;

            iterator.remove();
        }
        map.clear();
    }

    private void closeSocket(Socket socket) {
        if (socket == null) {
            return;
        }

        try {
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = null;
    }

}
