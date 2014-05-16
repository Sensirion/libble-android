package com.sensirion.libble.old.peripheral.sensor;

import android.bluetooth.BluetoothGattCharacteristic;

import com.sensirion.libble.BluetoothGattExecutor;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class TiKeysSensor extends AbstractSensor<TiKeysSensor.SimpleKeysStatus> {

    private static final String TAG = TiKeysSensor.class.getSimpleName();

    TiKeysSensor() {
        super();
    }

    @Override
    public String getDataUUID() {
        return "0000ffe1-0000-1000-8000-00805f9b34fb";
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public String getConfigUUID() {
        return null;
    }

    @Override
    public String getDataString() {
        final SimpleKeysStatus data = getData();
        return data.name();
    }

    @Override
    public SimpleKeysStatus parse(BluetoothGattCharacteristic c) {
    /*
     * The key state is encoded into 1 unsigned byte.
     * bit 0 designates the right key.
     * bit 1 designates the left key.
     * bit 2 designates the side key.
     *
     * Weird, in the userguide left and right are opposite.
     */
        int encodedInteger = c.getIntValue(FORMAT_UINT8, 0);
        return SimpleKeysStatus.values()[encodedInteger % 4];
    }

    @Override
    public BluetoothGattExecutor.ServiceAction[] enable(boolean enable) {
        return new BluetoothGattExecutor.ServiceAction[]{
                notify(enable)
        };
    }

    @Override
    public String getServiceUUID() {
        return "0000ffe0-0000-1000-8000-00805f9b34fb";
    }

    public enum SimpleKeysStatus {
        // Warning: The order in which these are defined matters.
        OFF_OFF, OFF_ON, ON_OFF, ON_ON;
    }
}
