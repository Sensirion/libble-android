package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import com.sensirion.libble.listeners.NotificationListener;

/**
 * Represents a {@link android.bluetooth.BluetoothGattService} as defined in org.bluetooth.service.*
 * or a proprietary implementation.
 */
public interface BleService extends Comparable<BleService> {

    /**
     * Method called when a characteristic is read.
     *
     * @param updatedCharacteristic that was updated.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    boolean onCharacteristicUpdate(@NonNull BluetoothGattCharacteristic updatedCharacteristic);

    /**
     * This method is called when a characteristic was written in the device.
     *
     * @param characteristic that was written in the device with success.
     * @return <code>true</code> if the service managed the given characteristic - <code>false</code> otherwise.
     */
    boolean onCharacteristicWrite(@NonNull BluetoothGattCharacteristic characteristic);

    /**
     * Method called when a descriptor is read.
     *
     * @param descriptor that was read by the device.
     * @return <code>true</code> if the descriptor was processed - <code>false</code> otherwise.
     */
    boolean onDescriptorRead(@NonNull BluetoothGattDescriptor descriptor);

    /**
     * Method called when a descriptor is written.
     *
     * @param descriptor that was written by the device.
     * @return <code>true</code> if the descriptor was processed - <code>false</code> otherwise.
     */
    boolean onDescriptorWrite(@NonNull BluetoothGattDescriptor descriptor);

    /**
     * Gets the UUID of the service.
     *
     * @return {@link java.lang.String} of the UUID.
     */
    @NonNull
    String getUUIDString();

    /**
     * Enables or disables the characteristic notification of a service.
     *
     * @param enabled <code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
    void setNotificationsEnabled(boolean enabled);

    /**
     * This method should only be called by the {@link com.sensirion.libble.devices.Peripheral} registered with this instance.
     * This method adds a listener to the list in case it's from the type specified in the ListenerType generics.
     *
     * @param listener the new candidate object for being a listener of this class.
     * @return <code>true</code> in case it's a valid listener, <code>false</code> otherwise.
     */
    boolean registerNotificationListener(@NonNull NotificationListener listener);

    /**
     * This method should only be called by the {@link com.sensirion.libble.devices.Peripheral} registered with this instance.
     * This method unregister a listener from the list in case of having it.
     *
     * @param listener the listener that doesn't want to hear from a device anymore.
     */
    boolean unregisterNotificationListener(@NonNull NotificationListener listener);

    /**
     * Check if the service implementation could match a given type, based on its class name.
     *
     * @param serviceDescription simple name of the class from the service.
     * @return <code>true</code> if the service implementation is from the given type - <code>false</code> otherwise.
     */
    boolean isExplicitService(@NonNull String serviceDescription);

    /**
     * Checks if a service is ready to use.
     *
     * @return <code>true</code> if the service is synchronized - <code>false</code> otherwise.
     */
    boolean isServiceReady();

    /**
     * Tries to synchronize a service in case some of its data is missing.
     */
    void synchronizeService();

    /**
     * Obtains the service priority towards synchronization.
     *
     * @return {@link BleServiceSynchronizationPriority} with the service priority.
     */
    @NonNull
    BleServiceSynchronizationPriority getServiceSynchronizationPriority();

    /**
     * Compares this object against other {@link BleService} using
     * their {@link BleServiceSynchronizationPriority}.
     *
     * {@inheritDoc}
     */
    @Override
    int compareTo(@NonNull final BleService otherService);
}
