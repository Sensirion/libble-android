package com.sensirion.libble;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

class BleDevice {
    private final BluetoothGatt mBluetoothGatt;
    private int mConnectionState;

    public BleDevice(@NonNull final BluetoothGatt bluetoothGatt, final int connectionState) {
        mBluetoothGatt = bluetoothGatt;
        mConnectionState = connectionState;
    }

    @NonNull
    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public void setConnectionState(final int connectionState) {
        mConnectionState = connectionState;
    }
}