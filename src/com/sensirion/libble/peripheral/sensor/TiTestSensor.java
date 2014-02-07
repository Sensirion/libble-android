
package com.sensirion.libble.peripheral.sensor;

import com.sensirion.libble.BluetoothGattExecutor;

import android.bluetooth.BluetoothGattCharacteristic;

public class TiTestSensor extends AbstractSensor<Void> {

    private static final String TAG = TiTestSensor.class.getSimpleName();

    TiTestSensor() {
        super();
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public String getServiceUUID() {
        return "f000aa60-0451-4000-b000-000000000000";
    }

    @Override
    public String getDataUUID() {
        return "f000aa61-0451-4000-b000-000000000000";
    }

    @Override
    public String getConfigUUID() {
        return "f000aa62-0451-4000-b000-000000000000";
    }

    @Override
    public String getDataString() {
        return "";
    }

    @Override
    public BluetoothGattExecutor.ServiceAction[] enable(boolean enable) {
        return new BluetoothGattExecutor.ServiceAction[0];
    }

    @Override
    public BluetoothGattExecutor.ServiceAction notify(boolean start) {
        return BluetoothGattExecutor.ServiceAction.NULL;
    }

    @Override
    public Void parse(BluetoothGattCharacteristic c) {
        return null;
    }
}
