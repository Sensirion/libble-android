package com.example.libbledemo;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.sensirion.libble.BleService;

class LibBleServiceConnection implements ServiceConnection {

    private final MainActivity mCallback;

    LibBleServiceConnection(MainActivity activity) {
        mCallback = activity;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (service instanceof BleService.LocalBinder) {
            BleService bleservice = ((BleService.LocalBinder) service).getService();
            if (bleservice.initialize()) {
                mCallback.onServiceConnected(bleservice);
            } else {
                throw new IllegalStateException("BLE service was not successfully initialized");
            }
        }

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        System.out.println(name.toString());
        mCallback.onServiceDisconnected();
    }
}
