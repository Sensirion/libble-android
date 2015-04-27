package com.sensirion.libble;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.devices.BlePeripheralService;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.BleService;

import java.util.List;
import java.util.UUID;

/**
 * This is the Activity all apps using the library can extend
 * instead of extending standard Activity
 * This is a convenience class and simplifies use of the library.
 */

public abstract class BleActivity extends Activity {

    private static final String TAG = BleActivity.class.getSimpleName();

    private BleManager mBleManager = BleManager.getInstance();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBleManager.init(getApplicationContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        mBleManager.stopScanning();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        if (isChangingConfigurations()) {
            Log.d(TAG, "onStop() -> isChangingConfigurations()");
        } else {
            mBleManager.release(getApplicationContext());
        }
        super.onStop();
    }

    /**
     * @see com.sensirion.libble.BleManager#isScanning()
     */
    @SuppressWarnings("unused")
    public boolean isScanning() {
        return mBleManager.isScanning();
    }

    /**
     * @see BleManager#startScanning()
     */
    @SuppressWarnings("unused")
    public boolean startScanning() {
        return mBleManager.startScanning();
    }

    /**
     * @see BleManager#startScanning(long)
     */
    @SuppressWarnings("unused")
    public synchronized boolean startScanning(final long scanDurationMs) {
        return mBleManager.startScanning(scanDurationMs);
    }

    /**
     * @see BleManager#startScanning(UUID...)
     */
    @SuppressWarnings("unused")
    public boolean startScanning(@NonNull final UUID[] deviceUUIDs) {
        return mBleManager.startScanning(deviceUUIDs);
    }

    /**
     * @see BleManager#startScanning(List of UUID))
     */
    @SuppressWarnings("unused")
    public boolean startScanning(@NonNull final List<UUID> deviceUUIDs) {
        return mBleManager.startScanning(deviceUUIDs);
    }

    /**
     * @see BleManager#startScanning(long, List of UUID)
     */
    @SuppressWarnings("unused")
    public boolean startScanning(final long scanDurationMs, @NonNull final List<UUID> deviceUUIDs) {
        return mBleManager.startScanning(scanDurationMs, deviceUUIDs);
    }

    /**
     * @see BleManager#startScanning(Long, UUID...)
     */
    @SuppressWarnings("unused")
    public boolean startScanning(@Nullable final Long scanDurationMs, @Nullable final UUID... deviceUUIDs) {
        return mBleManager.startScanning(scanDurationMs, deviceUUIDs);
    }

    /**
     * @see com.sensirion.libble.BleManager#stopScanning()
     */
    @SuppressWarnings("unused")
    public void stopScanning() {
        mBleManager.stopScanning();
    }

    /**
     * @see com.sensirion.libble.BleManager#getDiscoveredBleDevices()
     */
    @SuppressWarnings("unused")
    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        return mBleManager.getDiscoveredBleDevices();
    }

    /**
     * @see com.sensirion.libble.BleManager#getDiscoveredBleDevices(java.util.List)
     */
    @SuppressWarnings("unused")
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(@NonNull final List<String> deviceNames) {
        return mBleManager.getDiscoveredBleDevices(deviceNames);
    }

    /**
     * @see com.sensirion.libble.BleManager#getConnectedBleDevices()
     */
    @SuppressWarnings("unused")
    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        return mBleManager.getConnectedBleDevices();
    }

    /**
     * @see com.sensirion.libble.BleManager#getConnectedDevice(String)
     */
    @SuppressWarnings("unused")
    public BleDevice getConnectedDevice(@NonNull final String deviceAddress) {
        return mBleManager.getConnectedDevice(deviceAddress);
    }

    /**
     * @see com.sensirion.libble.BleManager#connectDevice(String)
     */
    @SuppressWarnings("unused")
    public boolean connectDevice(@NonNull final String deviceAddress) {
        return mBleManager.connectDevice(deviceAddress);
    }

    /**
     * @see com.sensirion.libble.BleManager#disconnectDevice(String)
     */
    @SuppressWarnings("unused")
    public void disconnectDevice(@NonNull final String deviceAddress) {
        mBleManager.disconnectDevice(deviceAddress);
    }

    /**
     * @see com.sensirion.libble.BleManager#getConnectedBleDeviceCount()
     */
    @SuppressWarnings("unused")
    public int getConnectedBleDeviceCount() {
        return mBleManager.getConnectedBleDeviceCount();
    }

    /**
     * @see com.sensirion.libble.BleManager#isDeviceConnected(String)
     */
    @SuppressWarnings("unused")
    public boolean isDeviceConnected(@NonNull final String deviceAddress) {
        return mBleManager.isDeviceConnected(deviceAddress);
    }

    /**
     * @see com.sensirion.libble.BleManager#registerDeviceListener(com.sensirion.libble.listeners.NotificationListener, String)
     */
    @SuppressWarnings("unused")
    public void registerDeviceListener(@NonNull final NotificationListener listener, @Nullable final String address) {
        mBleManager.registerDeviceListener(listener, address);
    }

    /**
     * @see com.sensirion.libble.BleManager#unregisterDeviceListener(com.sensirion.libble.listeners.NotificationListener, String)
     */
    @SuppressWarnings("unused")
    public void unregisterDeviceListener(@NonNull final String deviceAddress, @NonNull final NotificationListener listener) {
        mBleManager.unregisterDeviceListener(listener, deviceAddress);
    }

    /**
     * @see com.sensirion.libble.BleManager#isBluetoothEnabled()
     */
    @SuppressWarnings("unused")
    public boolean isBluetoothEnabled() {
        return mBleManager.isBluetoothEnabled();
    }

    /**
     * @see com.sensirion.libble.BleManager#requestEnableBluetooth(android.content.Context)
     */
    @SuppressWarnings("unused")
    public void requestEnableBluetooth(@NonNull final Context context) {
        mBleManager.requestEnableBluetooth(context);
    }

    /**
     * @see com.sensirion.libble.BleManager#setAllNotificationsEnabled(boolean)
     */
    @SuppressWarnings("unused")
    public void setAllNotificationsEnabled(final boolean enabled) {
        mBleManager.setAllNotificationsEnabled(enabled);
    }

    /**
     * @see com.sensirion.libble.BleManager#getNumberOfDiscoveredServices(String)
     */
    @SuppressWarnings("unused")
    public int getNumberOfDiscoveredServices(final String deviceAddress) {
        return mBleManager.getNumberOfDiscoveredServices(deviceAddress);
    }

    /**
     * @see com.sensirion.libble.BleManager#getDiscoveredServicesNames(String)
     */
    @SuppressWarnings("unused")
    public Iterable<String> getDiscoveredServicesNames(@NonNull final String address) {
        return mBleManager.getDiscoveredServicesNames(address);
    }

    /**
     * @see com.sensirion.libble.BleManager#getServiceWithName(String, String)
     */
    @SuppressWarnings("unused")
    public BleService getServiceWithName(@NonNull final String deviceAddress, @NonNull final String serviceName) {
        return mBleManager.getServiceWithName(deviceAddress, serviceName);
    }

    /**
     * @see com.sensirion.libble.BleManager#registerNotificationListener(com.sensirion.libble.listeners.NotificationListener)
     */
    @SuppressWarnings("unused")
    public synchronized void registerNotificationListener(@NonNull final NotificationListener listener) {
        mBleManager.registerNotificationListener(listener);
    }

    /**
     * @see com.sensirion.libble.BleManager#unregisterNotificationListener(com.sensirion.libble.listeners.NotificationListener)
     */
    @SuppressWarnings("unused")
    public synchronized void unregisterNotificationListener(@NonNull final NotificationListener listener) {
        mBleManager.unregisterNotificationListener(listener);
    }
}