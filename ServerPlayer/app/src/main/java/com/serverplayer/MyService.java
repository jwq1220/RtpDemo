package com.serverplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.serverplayer.manager.ServerManager;

public class MyService extends Service {
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ServerManager clientManager = new ServerManager(8004,8005,8002,8003,"127.0.0.1");
        clientManager.setMode(ServerManager.SEVER);
        clientManager.setRunning(true);
        clientManager.setRecording(true);
        Thread sessionThread = new Thread(clientManager);
        sessionThread.start();
        return super.onStartCommand(intent, flags, startId);
    }
}
