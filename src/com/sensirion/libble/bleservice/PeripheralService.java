package com.sensirion.libble.bleservice;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.sensirion.libble.Peripheral;

import java.util.UUID;

/**
 * Represents a {@link android.bluetooth.BluetoothGattService} as defined in org.bluetooth.service.*
 * or a proprietary implementation.
 */
public class PeripheralService {

    protected final Peripheral mParent;
    protected final BluetoothGattService mBluetoothGattService;

    public PeripheralService(Peripheral parent, BluetoothGattService bluetoothGattService) {
        mParent = parent;
        mBluetoothGattService = bluetoothGattService;
    }

    protected BluetoothGattCharacteristic getCharacteristicFor(String uuid) {
        return mBluetoothGattService.getCharacteristic(UUID.fromString(uuid));
    }

    public boolean onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
        return mBluetoothGattService.getCharacteristics().contains(characteristic);
    }

    public String getUUIDString() {
        return mBluetoothGattService.getUuid().toString();
    }
}