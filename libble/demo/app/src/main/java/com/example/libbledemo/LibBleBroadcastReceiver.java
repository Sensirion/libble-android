package com.example.libbledemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sensirion.libble.BleService;


class LibBleBroadcastReceiver extends BroadcastReceiver {

    private final MainActivity mCallback;

    LibBleBroadcastReceiver(MainActivity activity) {
        mCallback = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String device_address = intent.getStringExtra(BleService.EXTRA_DEVICE_ADDRESS);
        switch (action) {
            case BleService.ACTION_GATT_CONNECTED:
                mCallback.onConnection(device_address);
                break;
            case BleService.ACTION_GATT_DISCONNECTED:
                mCallback.onDisconnection(device_address);
                break;
        }
    }
}
