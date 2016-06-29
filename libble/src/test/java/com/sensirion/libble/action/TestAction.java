package com.sensirion.libble.action;

import android.bluetooth.BluetoothGatt;

public abstract class TestAction extends GattAction {
    public int executeCount = 0;
    private String mDeviceAddress;

    TestAction(BluetoothGatt gatt, final String deviceAddress) {
        super(gatt);
        mDeviceAddress = deviceAddress;
    }

    @Override
    public String getDeviceAddress() {
        return mDeviceAddress;
    }
}
