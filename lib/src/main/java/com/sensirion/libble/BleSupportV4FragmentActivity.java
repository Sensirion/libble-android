package com.sensirion.libble;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.BleService;

import java.util.List;
import java.util.UUID;

/**
 * This is the the support FragmentActivity v4 all apps using the library can extend
 * instead of extending standard support FragmentActivity v4.
 * This is a convenience class and simplifies use of the library.
 */
public abstract class BleSupportV4FragmentActivity extends android.support.v4.app.FragmentActivity {

    private static final String TAG = BleSupportV4FragmentActivity.class.getSimpleName();

    private BleManager mBleManager = BleManager.getInstance();

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBleManager.init(getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBleManager.stopScanning();
    }

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
     * Ask if the local device is scanning for new devices.
     *
     * @return <code>true</code> if it's scanning - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean isScanning() {
        return mBleManager.isScanning();
    }

    /**
     * Starts to scan for all bluetooth devices in range.
     *
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean startScanning() {
        return startScanning(null, null);
    }

    /**
     * Starts to scan for all bluetooth devices in range.
     *
     * @param scanDurationMs that the device will be scanning. Needs to be a positive number.
     * @return <code>true</code> if scan has been started. <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public synchronized boolean startScanning(final long scanDurationMs) {
        return mBleManager.startScanning(scanDurationMs);
    }

    /**
     * Start scanning devices in range for provided UUIDs.
     *
     * @param deviceUUIDs deviceUUIDs that we want want to use,
     *                    <code>null</code> if all devices have to be retrieved.
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean startScanning(@Nullable final UUID[] deviceUUIDs) {
        return mBleManager.startScanning(deviceUUIDs);
    }

    /**
     * Start scanning devices in range for provided UUIDs.
     *
     * @param deviceUUIDs    deviceUUIDs that we want want to use,
     *                       <code>null</code> if all devices have to be retrieved.
     * @param scanDurationMs that the device will be scanning. Needs to be a positive number.
     *                       <code>null</code> if the default scan duration will be used.
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    public boolean startScanning(@Nullable final UUID[] deviceUUIDs, @Nullable final Long scanDurationMs) {
        return mBleManager.startScanning(deviceUUIDs, scanDurationMs);
    }

    /**
     * Stops the scan of new devices.
     */
    @SuppressWarnings("unused")
    public void stopScanning() {
        mBleManager.stopScanning();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.devices.BleDevice}.
     *
     * @return Iterable
     */
    @SuppressWarnings("unused")
    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        return mBleManager.getDiscoveredBleDevices();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.devices.BleDevice} with valid names for the application.
     *
     * @param deviceNames List of devices names.
     * @return Iterable
     */
    @SuppressWarnings("unused")
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(@NonNull final List<String> deviceNames) {
        return mBleManager.getDiscoveredBleDevices(deviceNames);
    }

    /**
     * Get all connected {@link com.sensirion.libble.devices.BleDevice}.
     *
     * @return Iterable
     */
    @SuppressWarnings("unused")
    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        return mBleManager.getConnectedBleDevices();
    }

    /**
     * Returns the {@link com.sensirion.libble.devices.BleDevice} belonging to the given address
     *
     * @param deviceAddress MAC-Address of the desired {@link com.sensirion.libble.devices.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.devices.BleDevice}
     * or NULL if the device is not connected
     */
    @SuppressWarnings("unused")
    public BleDevice getConnectedDevice(@NonNull final String deviceAddress) {
        return mBleManager.getConnectedDevice(deviceAddress);
    }

    /**
     * Tries to establish a connection to a selected device (by address)
     *
     * @param deviceAddress MAC-Address of the device that should be connected.
     */
    @SuppressWarnings("unused")
    public boolean connectDevice(@NonNull final String deviceAddress) {
        return mBleManager.connectDevice(deviceAddress);
    }

    /**
     * Tries to disconnect a selected device (by address)
     *
     * @param deviceAddress MAC-Address of the device that should be disconnected
     */
    @SuppressWarnings("unused")
    public void disconnectDevice(@NonNull final String deviceAddress) {
        mBleManager.disconnectDevice(deviceAddress);
    }

    /**
     * Returns the number of connected devices.
     *
     * @return <code>int</code> with the number of devices.
     */
    @SuppressWarnings("unused")
    public int getConnectedBleDeviceCount() {
        return mBleManager.getConnectedBleDeviceCount();
    }

    /**
     * Checks if a device is connected.
     *
     * @param deviceAddress of the device.
     * @return <code>true</code> if connected - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean isDeviceConnected(@NonNull final String deviceAddress) {
        return mBleManager.isDeviceConnected(deviceAddress);
    }

    /**
     * Registers a listener in a connected devices.
     *
     * @param listener pretending to listen for notifications of a devices.
     * @param address  address of the devices we want to listen to,
     *                 <code>null</code> if we want to register a listener to all connected devices.
     */
    @SuppressWarnings("unused")
    public void registerDeviceListener(@NonNull final NotificationListener listener, @Nullable final String address) {
        mBleManager.registerDeviceListener(listener, address);
    }

    /**
     * Unregister a listener from a connected devices.
     *
     * @param listener      that wants to unregister from the notifications of a device.
     * @param deviceAddress of the device you don't want to get notifications from anymore.
     */
    @SuppressWarnings("unused")
    public void unregisterDeviceListener(@NonNull final String deviceAddress, @NonNull final NotificationListener listener) {
        mBleManager.unregisterDeviceListener(listener, deviceAddress);
    }

    /**
     * Checks if bluetooth connection is enabled on the device.
     */
    @SuppressWarnings("unused")
    public boolean isBluetoothEnabled() {
        return mBleManager.isBluetoothEnabled();
    }

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param context {@link android.content.Context} of the requesting activity.
     */
    @SuppressWarnings("unused")
    public void requestEnableBluetooth(@NonNull final Context context) {
        mBleManager.requestEnableBluetooth(context);
    }

    /**
     * Enables or disables notifications in all the devices.
     *
     * @param enabled <code>true</code> for enabling notifications - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public void setAllNotificationsEnabled(final boolean enabled) {
        mBleManager.setAllNotificationsEnabled(enabled);
    }

    /**
     * Counts the number of services.
     *
     * @param deviceAddress of the device.
     * @return number of discovered services.
     */
    @SuppressWarnings("unused")
    public int getNumberOfDiscoveredServices(final String deviceAddress) {
        return mBleManager.getNumberOfDiscoveredServices(deviceAddress);
    }

    /**
     * Obtains the names of each discovered service.
     *
     * @param address of the device.
     * @return {@link java.util.LinkedList} with the services names.
     */
    @SuppressWarnings("unused")
    public Iterable<String> getDiscoveredServicesNames(@NonNull final String address) {
        return mBleManager.getDiscoveredServicesNames(address);
    }

    /**
     * Asks for a service with a particular name.
     *
     * @param deviceAddress of the device.
     * @param serviceName   name of the service.
     * @return {@link com.sensirion.libble.services.BleService}
     */
    @SuppressWarnings("unused")
    public BleService getServiceWithName(@NonNull final String deviceAddress, @NonNull final String serviceName) {
        return mBleManager.getServiceWithName(deviceAddress, serviceName);
    }

    /**
     * Adds a listener to the library notification listener list.
     * This listener will be registered to every service of all the devices connected or that will be connected.
     *
     * @param listener that wants to be added - Cannot be <code>null</code>
     */
    @SuppressWarnings("unused")
    public synchronized void registerNotificationListener(@NonNull final NotificationListener listener) {
        mBleManager.registerNotificationListener(listener);
    }

    /**
     * Removes a listener from the library listener list.
     *
     * @param listener that wants to be removed - Cannot be <code>null</code>
     */
    @SuppressWarnings("unused")
    public synchronized void unregisterNotificationListener(@NonNull final NotificationListener listener) {
        mBleManager.unregisterNotificationListener(listener);
    }

    @SuppressWarnings("unused")
    abstract public void onConnectedPeripheralSelected(String address);

    @SuppressWarnings("unused")
    abstract public void onDiscoveredPeripheralSelected(String address);
}