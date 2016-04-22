package com.sensirion.libble.devices;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.AbstractBleService;
import com.sensirion.libble.services.AbstractHistoryService;
import com.sensirion.libble.services.BleService;

class MockPeripheral implements BleDevice {

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void connect(@NonNull Context context) {
    }

    @Override
    public boolean reconnect() {
        return true;
    }

    @Override
    public void disconnect() {
    }

    @NonNull
    @Override
    public String getAddress() {
        return "AA:BB:CC:DD:EE:FF";
    }

    @Nullable
    @Override
    public String getAdvertisedName() {
        return "Advertised Name";
    }

    @NonNull
    @Override
    public DeviceBluetoothType getBluetoothType() {
        return DeviceBluetoothType.DEVICE_TYPE_UNKNOWN;
    }

    @Override
    public int getRSSI() {
        return 41;
    }

    @Nullable
    @Override
    public <T extends AbstractBleService> T getDeviceService(@NonNull Class<T> type) {
        return null;
    }

    @Nullable
    @Override
    public BleService getDeviceService(@NonNull String serviceName) {
        return null;
    }

    @NonNull
    @Override
    public Iterable<BleService> getDiscoveredServices() {
        return null;
    }

    @NonNull
    @Override
    public Iterable<String> getDiscoveredServicesNames() {
        return null;
    }

    @Override
    public int getNumberServices() {
        return 0;
    }

    @Override
    public void setAllNotificationsEnabled(boolean enabled) {
    }

    @Override
    public boolean registerDeviceListener(@NonNull NotificationListener listener) {
        return false;
    }

    @Override
    public void unregisterDeviceListener(@NonNull NotificationListener listener) {
    }

    @Nullable
    @Override
    public AbstractHistoryService getHistoryService() {
        return null;
    }

    @Override
    public boolean synchronizeAllDeviceServices() {
        return true;
    }

    @Override
    public boolean areAllServicesReady() {
        return true;
    }
}
