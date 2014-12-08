package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.peripherals.Peripheral;

import java.util.UUID;

/**
 * Represents a {@link android.bluetooth.BluetoothGattService} as defined in org.bluetooth.service.*
 * or a proprietary implementation.
 */
public class PeripheralService<CharacteristicValueType> {

    protected final Peripheral mPeripheral;
    protected final BluetoothGattService mBluetoothGattService;

    public PeripheralService(final Peripheral parent, final BluetoothGattService bluetoothGattService) {
        mPeripheral = parent;
        mBluetoothGattService = bluetoothGattService;
    }

    protected BluetoothGattCharacteristic getCharacteristicFor(final String uuid) {
        return mBluetoothGattService.getCharacteristic(UUID.fromString(uuid));
    }

    /**
     * Method called when a characteristic is read.
     *
     * @param characteristic that was read.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    public boolean onCharacteristicRead(final BluetoothGattCharacteristic characteristic) {
        return mBluetoothGattService.getCharacteristics().contains(characteristic);
    }

    /**
     * Gets the UUID of the service.
     *
     * @return {@link java.lang.String} of the UUID.
     */
    public String getUUIDString() {
        return mBluetoothGattService.getUuid().toString();
    }

    /**
     * If the peripheral service knows how to decrypt a characteristic it returns the result.
     * This method should be overrided by a peripheral service in case we want to obtain one
     * characteristic, but it's not compulsory to do that.
     *
     * @param characteristicName of the characteristic wanted.
     * @return 'Object' with the given characteristic
     */
    public CharacteristicValueType getCharacteristicValue(final String characteristicName) {
        return null;
    }

    /**
     * Checks if the service implementation is from a given type.
     *
     * @param serviceDescription name or simple name of the class from the service.
     * @return <code>true</code> if the service implementation is from the given type - <code>false</code> otherwise.
     */
    public boolean isExplicitService(final String serviceDescription) {
        if (PeripheralService.class.getName().endsWith(serviceDescription)) {
            Log.w(PeripheralService.class.getSimpleName(), "A generic service can't be retrieved.");
            return false; //A generic service can't be retrieved.
        }
        return this.getClass().getName().endsWith(serviceDescription);
    }

    /**
     * Obtains the device address of the device.
     *
     * @return {@link java.lang.String} with the device address.
     */
    public String getDeviceAddress() {
        return mPeripheral.getAddress();
    }

    @Override
    public boolean equals(final Object otherService) {
        if (otherService instanceof PeripheralService) {
            return ((PeripheralService) otherService).getClass().getSimpleName().equals(this.getClass().getSimpleName());
        } else if (otherService instanceof String) {
            return isExplicitService((String) otherService);
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getName().concat(" of the device ").concat(mPeripheral.getAddress());
    }
}