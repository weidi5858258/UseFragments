//
// Created by alexander on 2019/12/6.
//

#ifndef USEFRAGMENTS_SOCKETCLIENT_H
#define USEFRAGMENTS_SOCKETCLIENT_H


class SocketClient {
public:
    SocketClient();

    ~SocketClient();

    int connect();
};


#endif //USEFRAGMENTS_SOCKETCLIENT_H

/***
 https://blog.csdn.net/abs625/article/details/79492934
 framework为服务端：
 private class ServerSocket implements Runnable {
        private boolean keepRunning = true;
        private LocalServerSocket serverSocket;
        private String socketName;
        InputStream inputStream = null;


        public ServerSocket(String socketName) {
            this.socketName = socketName;//这个名字必须和底层socket的名字一样
            Log.e("HarryCan", "socketName : " + socketName );
        }


        public void stopScoket() {
            keepRunning = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        @Override
        public void run() {
            try {
                serverSocket = new LocalServerSocket(socketName);
            } catch (IOException e) {
                e.printStackTrace();
                keepRunning = false;
            }
            Log.e(TAG, socketName + " wait for new client coming !");
            try {
                LocalSocket interactClientSocket = serverSocket.accept();
                if (keepRunning) {
                    Log.e(TAG, socketName + " new client coming !");
                    try {
                        inputStream = interactClientSocket.getInputStream();
                        byte[] buf = new byte[1024];
                        int readBytes = -1;
                        while ((readBytes = inputStream.read(buf)) != -1 && keepRunning) {
                            // buff is the result
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                keepRunning = false;
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.e(TAG, socketName + " finish running !");
        }

    }
 系统服务客户端代码：

导入头文件:
 #include <sys/socket.h>//socket相关
#include <pthread.h>//线程相关
//socketName必须和服务端相同，获取到socket句柄fd
int fd =  socket_local_client(socketName, ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
 Param *param = new Param();//结构体传参
    param->s = s;
    param->frame = frame;
    pthread_t thread_id;

    pthread_create( &thread_id, NULL, &threadFunc, param);



void *threadFunc(void *data) {
    ALOGE(" threadFunc enter in");
    int nbytes;
    Param *param = (Param *)data;
    int s = param->s;
    struct can_frame frame = param->frame;
    char dest[20];
    while(openCanFlag) {

        nbytes = read(s, &frame, sizeof(frame));//循环获取数据，这里通过一个socekt从硬件获取，不关注

        //对数据做转化

        if (nbytes > 0) {
            char a,b,c,d;
            a=(char)(frame.can_id&0xff);
            b=(char)((frame.can_id&0xff00)>>8);
            c=(char)((frame.can_id&0xff0000)>>16);
            d=(char)((frame.can_id&0xff000000)>>24);
            frame.can_id=(a<<24)|(b<<16)|(c<<8)|d;
            memcpy(&dest[0], (char*)&frame.can_id, sizeof(__u32));
            for(int i = 0; i < frame.can_dlc; i++){
                dest[i+4] = frame.data[i];

            }

            //将转化后的数据写入到socekt，并在framework接收

            write(fd, dest, sizeof(dest));
        }  
    } 
    ALOGE("threadFunc startCanSocket loop end");
    close(s);
    pthread_exit(0);
    return NULL;
}

 framework做客户端：
 private OutputStream mOutputStream;
   private LocalSocket socket;
    private void listenToSocket(String socketName) throws IOException {
        try {
            socket = new LocalSocket();
            socket.connect(new LocalSocketAddress(socketName));
            mOutputStream = socket1.getOutputStream();
            Log.e(TAG, socketName + " connect to server  ");
        } catch (IOException ex) {
            Log.e(TAG, socketName + " listenToSocket error : " + ex.toString());
        }

    }

private void closeSocket() throws IOException {
        if (mOutputStream1!= null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, " Failed closing output stream write_uart1_rt_socket");
            }
            mOutputStream = null;
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {
            Log.e(TAG, " Failed closing socket : write_uart1_rt_socket");
        }
    }

private void initSocketClient(final String socketName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    listenToSocket(socketName);
                } catch (Exception e) {
                    Log.e(TAG, socketName + " Error listenToSocket : " + e.toString());
                }
            }
        }).start ();

    }
 //发送数据到服务端

public void writeGPSUart1Data(byte[] data) throws Exception {
        
        mOutputStream.write(data);
        mOutputStream.flush();
        Log.e(TAG, "write flush data end ");
}
 系统服务中C代码实现服务端：
//获取socket句柄，socketName必须和客户端保持一致
 int uart2_fd = socket_local_server(socketName, ANDROID_SOCKET_NAMESPACE_ABSTRACT, SOCK_STREAM);
    pthread_t thread_id_2;

    pthread_create( &thread_id_2, NULL, &threadUart2Func, NULL);//开启线程接受数据



void *thread2Func(void *data) {
    ALOGE("threadUart2Func enter in");
    char buf2[512] = {0};
    int socketID = accept(uart2_fd,NULL,NULL);//等待客户端连接
    int ret;
    while((ret = read(socketID,buf2,sizeof(buf2))) >=0){//循环读取数据
        int result = uart2_write(buf2,ret);
        if(isInitGPSModule == 0){
            ALOGE("thread2Func exit thread\n");
            close(uart2_fd);
            pthread_exit(0);
        }
    }
    ALOGE("thread2Func loop end\n");
    close(uart2_fd);
    pthread_exit(0);
    return ((void *)data);

}
 服务端必须在客户端连接之前先初始化，否则客户端无法连接。
*/