package com.recoding;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.rtprecord.manager.ServerManager;
//import com.recoding.manager.ServerManager;

public class MainActivity extends AppCompatActivity {
    public static final int STOPPED = 0;
    public static final int RECORDING = 1;

    ServerManager clientManager = new ServerManager(8002,8003,8004,8005,"192.168.0.107");

    int status = STOPPED;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };



    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        clientManager.setMode(ServerManager.CLIENT);
        clientManager.setRunning(true);
        Thread sessionThread = new Thread(clientManager);
        sessionThread.start();
    }

    public void OnStartRecoding(View view) {
        if(status == STOPPED){
            clientManager.setRecording(true);
            status = RECORDING;
        }
        else if(status == RECORDING){
            clientManager.setRecording(false);
            status = STOPPED;
        }
    }
}
