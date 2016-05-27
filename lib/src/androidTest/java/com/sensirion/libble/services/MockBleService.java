package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import com.sensirion.libble.listeners.NotificationListener;

import java.util.UUID;

public class MockBleService extends AbstractBleService {

    private final BleServiceSynchronizationPriority mServicePriority;

    public MockBleService() {
        this(BleServiceSynchronizationPriority.NORMAL_PRIORITY);
    }

    @SuppressWarnings("ConstantConditions")
    public MockBleService(@NonNull final BleServiceSynchronizationPriority priority) {
        super(null, null);
        mServicePriority = priority;
    }

    @Override
    public boolean onCharacteristicUpdate(@NonNull BluetoothGattCharacteristic updatedCharacteristic) {
        return true;
    }

    @Override
    public boolean onCharacteristicWrite(@NonNull BluetoothGattCharacteristic characteristic) {
        return true;
    }

    @Override
    public boolean onDescriptorRead(@NonNull BluetoothGattDescriptor descriptor) {
        return true;
    }

    @Override
    public boolean onDescriptorWrite(@NonNull BluetoothGattDescriptor descriptor) {
        return true;
    }

    @NonNull
    @Override
    public String getUUIDString() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void setNotificationsEnabled(boolean enabled) {
    }

    @Override
    public boolean registerNotificationListener(@NonNull NotificationListener listener) {
        return true;
    }

    @Override
    public boolean unregisterNotificationListener(@NonNull NotificationListener listener) {
        return true;
    }

    @Override
    public boolean isExplicitService(@NonNull String serviceDescription) {
        return false;
    }

    @Override
    public boolean isServiceReady() {
        return true;
    }

    @Override
    public void synchronizeService() {
    }

    @NonNull
    @Override
    public BleServiceSynchronizationPriority getServiceSynchronizationPriority() {
        return mServicePriority;
    }
}
