package com.sensirion.libble.action;

import android.bluetooth.BluetoothGatt;

public abstract class GattAction {
    BluetoothGatt mGatt;

    public int failsTillDropOut = 10;

    GattAction(final BluetoothGatt gatt) {
        mGatt = gatt;
    }

    public String getDeviceAddress() {
        return mGatt.getDevice().getAddress();
    }

    abstract boolean execute();

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}