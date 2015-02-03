package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;

import java.util.UUID;

/**
 * Represents a {@link android.bluetooth.BluetoothGattService} as defined in org.bluetooth.service.*
 * or a proprietary implementation.
 */
public class BleService<CharacteristicValueType> {

    protected final String TAG = this.getClass().getSimpleName();

    protected final UUID USER_CHARACTERISTIC_DESCRIPTOR_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");

    protected final Peripheral mPeripheral;
    protected final BluetoothGattService mBluetoothGattService;

    public BleService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        mPeripheral = parent;
        mBluetoothGattService = bluetoothGattService;
    }


    /**
     * Asks the bluetooth service for a characteristic.
     *
     * @param uuid {@link java.lang.String} from the characteristic requested by the user.
     * @return {@link android.bluetooth.BluetoothGattCharacteristic} requested by the user.
     */
    protected BluetoothGattCharacteristic getCharacteristic(@NonNull final String uuid) {
        return mBluetoothGattService.getCharacteristic(UUID.fromString(uuid.trim().toLowerCase()));
    }

    /**
     * Method called when a characteristic is read.
     *
     * @param updatedCharacteristic that was updated.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        return mBluetoothGattService.getCharacteristics().contains(updatedCharacteristic);
    }

    /**
     * This method is called when a characteristic was written in the device.
     *
     * @param characteristic that was written in the device with success.
     *                       return <code>true</code> if the service managed the given characteristic - <code>false</code> otherwise.
     */
    public boolean onCharacteristicWrite(@NonNull final BluetoothGattCharacteristic characteristic) {
        return mBluetoothGattService.getCharacteristics().contains(characteristic);
    }

    /**
     * Method called when a descriptor is read.
     *
     * @param descriptor that was read by the device.
     * @return <code>true</code> if the descriptor was processed - <code>false</code> otherwise.
     */
    public boolean onDescriptorRead(@NonNull final BluetoothGattDescriptor descriptor) {
        return false; // This method needs to be overridden in order to do something with the descriptor.
    }

    /**
     * Method called when a descriptor is written.
     *
     * @param descriptor that was written by the device.
     * @return <code>true</code> if the descriptor was processed - <code>false</code> otherwise.
     */
    public boolean onDescriptorWrite(@NonNull final BluetoothGattDescriptor descriptor) {
        return false; // This method needs to be overridden in order to do something with the descriptor.
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
     * @return {@link java.lang.Object} with the given characteristic
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
        if (BleService.class.getName().endsWith(serviceDescription)) {
            Log.w(TAG, "A generic service can't be retrieved.");
            return false; //A generic service can't be retrieved.
        }
        return this.getClass().getName().endsWith(serviceDescription);
    }

    /**
     * Obtains the device address of the device.
     *
     * @return {@link java.lang.String} with the device address.
     */
    public String getAddress() {
        return mPeripheral.getAddress();
    }

    @Override
    public boolean equals(final Object otherService) {
        if (otherService instanceof BleService) {
            return ((BleService) otherService).getClass().getSimpleName().equals(TAG);
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