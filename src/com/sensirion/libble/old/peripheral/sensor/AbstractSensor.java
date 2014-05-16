package com.sensirion.libble.old.peripheral.sensor;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.sensirion.libble.BluetoothGattExecutor;

import java.util.UUID;

public abstract class AbstractSensor<T> {

    private static String CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private T data;

    protected AbstractSensor() {
    }

    public String getCharacteristicName(String uuid) {
        if (getDataUUID().equals(uuid))
            return getName() + " Data";
        else if (getConfigUUID().equals(uuid))
            return getName() + " Config";
        return "Unknown";
    }

    public abstract String getDataUUID();

    public abstract String getName();

    public abstract String getConfigUUID();

    public boolean isConfigUUID(String uuid) {
        return false;
    }

    public T getData() {
        return data;
    }

    public abstract String getDataString();

    public void onCharacteristicChanged(BluetoothGattCharacteristic c) {
        data = parse(c);
    }

    protected abstract T parse(BluetoothGattCharacteristic c);

    public boolean onCharacteristicRead(BluetoothGattCharacteristic c) {
        return false;
    }

    protected byte[] getConfigValues(boolean enable) {
        return new byte[]{
                (byte) (enable ? 1 : 0)
        };
    }

    public BluetoothGattExecutor.ServiceAction[] enable(final boolean enable) {
        return new BluetoothGattExecutor.ServiceAction[]{
                write(getConfigUUID(), getConfigValues(enable)),
                notify(enable)
        };
    }

    public BluetoothGattExecutor.ServiceAction update() {
        return BluetoothGattExecutor.ServiceAction.NULL;
    }

    public BluetoothGattExecutor.ServiceAction read(final String uuid) {
        return new BluetoothGattExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                final BluetoothGattCharacteristic characteristic = getCharacteristic(bluetoothGatt,
                        uuid);
                bluetoothGatt.readCharacteristic(characteristic);
                return false;
            }
        };
    }

    private BluetoothGattCharacteristic getCharacteristic(BluetoothGatt bluetoothGatt, String uuid) {
        final UUID serviceUuid = UUID.fromString(getServiceUUID());
        final UUID characteristicUuid = UUID.fromString(uuid);

        final BluetoothGattService service = bluetoothGatt.getService(serviceUuid);
        return service.getCharacteristic(characteristicUuid);
    }

    public abstract String getServiceUUID();

    public BluetoothGattExecutor.ServiceAction write(final String uuid, final byte[] value) {
        return new BluetoothGattExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                final BluetoothGattCharacteristic characteristic = getCharacteristic(bluetoothGatt,
                        uuid);
                characteristic.setValue(value);
                bluetoothGatt.writeCharacteristic(characteristic);
                return false;
            }
        };
    }

    public BluetoothGattExecutor.ServiceAction notify(final boolean start) {
        return new BluetoothGattExecutor.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                final UUID CCC = UUID.fromString(CHARACTERISTIC_CONFIG);

                final BluetoothGattCharacteristic dataCharacteristic = getCharacteristic(
                        bluetoothGatt, getDataUUID());
                final BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(CCC);
                if (config == null)
                    return true;

                // enable/disable locally
                bluetoothGatt.setCharacteristicNotification(dataCharacteristic, start);
                // enable/disable remotely
                config.setValue(start ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(config);
                return false;
            }
        };
    }
}
