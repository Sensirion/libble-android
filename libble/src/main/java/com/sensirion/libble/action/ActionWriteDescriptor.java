package com.sensirion.libble.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

public class ActionWriteDescriptor extends GattAction {
    private final BluetoothGattDescriptor mGattDescriptor;

    public ActionWriteDescriptor(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor) {
        super(gatt);
        mGattDescriptor = descriptor;
    }

    @Override
    boolean execute() {
        return mGatt.writeDescriptor(mGattDescriptor);
    }

    public BluetoothGattDescriptor getGattDescriptor() {
        return mGattDescriptor;
    }
}