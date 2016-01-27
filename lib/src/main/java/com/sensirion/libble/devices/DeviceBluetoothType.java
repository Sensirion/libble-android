package com.sensirion.libble.devices;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

/**
 * This class represents a Bluetooth device type, distinguished
 */
public enum DeviceBluetoothType {

    /** Unknown type */
    DEVICE_TYPE_UNKNOWN(BluetoothDevice.DEVICE_TYPE_UNKNOWN),
    /** Classic - BR/EDR devices */
    DEVICE_TYPE_CLASSIC(BluetoothDevice.DEVICE_TYPE_CLASSIC),
    /** Low Energy - LE-only */
    DEVICE_TYPE_LE(BluetoothDevice.DEVICE_TYPE_LE),
    /** Dual Mode - BR/EDR/LE */
    DEVICE_TYPE_DUAL(BluetoothDevice.DEVICE_TYPE_DUAL);

    private final int mId;

    DeviceBluetoothType(final int id) {
        mId = id;
    }

    /**
     * Obtains a Device Bluetooth type from a given ID.
     *
     * @param device whose type will be retrieved.
     * @return the {@link DeviceBluetoothType} if available - {@see DEVICE_TYPE_UNKNOWN} otherwise
     */
    @NonNull
    static DeviceBluetoothType getDeviceBluetoothType(@NonNull final BluetoothDevice device) {
        final int deviceClass = device.getBluetoothClass().getDeviceClass();
        for (final DeviceBluetoothType deviceBluetoothType : values()) {
            if (deviceClass == deviceBluetoothType.mId) {
                return deviceBluetoothType;
            }
        }
        return DEVICE_TYPE_UNKNOWN;
    }

    /**
     * Returns the {@link DeviceBluetoothType} name.
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String toString() {
        return this.name();
    }
}
