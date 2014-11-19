package com.sensirion.libble.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.BleManager;
import com.sensirion.libble.NotificationListener;
import com.sensirion.libble.Peripheral;
import com.sensirion.libble.bleservice.PeripheralService;

import java.util.List;
import java.util.UUID;

/**
 * This is the Activity all apps using the library can extend
 * instead of extending standard Activity
 * <p/>
 * This is a convenience class and simplifies use of the library.
 */

public abstract class BleActivity extends Activity {

    private static final String TAG = BleActivity.class.getSimpleName();

    private BleManager mBleManager = BleManager.getInstance();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
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
     * Ask if the peripheral service is scanning.
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
     * @return true if it's scanning, false otherwise.
     */
    public boolean startScanning() {
        return mBleManager.startScanning();
    }

    /**
     * Start scanning devices in range using provided UUIDs.
     *
     * @param deviceUUIDs deviceUUIDs that we want want to use,
     *                    <code>null</code> if all devices have to be retrieved.
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean startScanning(final UUID[] deviceUUIDs) {
        return mBleManager.startScanning(deviceUUIDs);
    }

    /**
     * Stops the scan of new devices.
     */
    public void stopScanning() {
        mBleManager.stopScanning();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        return mBleManager.getDiscoveredBleDevices();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with valid names for the application.
     *
     * @param deviceNames List of devices names.
     * @return Iterable
     */
    @SuppressWarnings("unused")
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(final List<String> deviceNames) {
        return mBleManager.getDiscoveredBleDevices(deviceNames);
    }

    /**
     * Get all connected {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        return mBleManager.getConnectedBleDevices();
    }

    /**
     * Returns the {@link com.sensirion.libble.BleDevice} belonging to the given address
     *
     * @param address MAC-Address of the desired {@link com.sensirion.libble.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.BleDevice}
     * or NULL if the device is not connected
     */
    public BleDevice getConnectedDevice(final String address) {
        return mBleManager.getConnectedDevice(address);
    }

    /**
     * Tries to establish a connection toa selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be connected
     */
    @SuppressWarnings("unused")
    public boolean connectPeripheral(final String address) {
        return mBleManager.connectPeripheral(address);
    }

    /**
     * Tries to disconnect a selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be disconnected
     */
    @SuppressWarnings("unused")
    public void disconnectPeripheral(final String address) {
        mBleManager.disconnectPeripheral(address);
    }

    /**
     * Looks for a connected peripheral.
     *
     * @param deviceAddress of the peripheral.
     * @return the {@link com.sensirion.libble.Peripheral} with the given address
     */
    @SuppressWarnings("unused")
    public Peripheral getConnectedPeripheral(final String deviceAddress) {
        return mBleManager.getConnectedPeripheral(deviceAddress);
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
    public boolean isDeviceConnected(final String deviceAddress) {
        return mBleManager.isDeviceConnected(deviceAddress);
    }


    /**
     * Register a listener in all connected peripherals.
     *
     * @param listener pretending to listen for notifications in all peripherals.
     */
    @SuppressWarnings("unused")
    public void registerPeripheralListenerToAllConnected(final NotificationListener listener) {
        mBleManager.registerPeripheralListenerToAllConnected(listener);
    }

    /**
     * Registers a listener in a connected peripheral.
     *
     * @param address  address of the peripheral we want to listen to,
     *                 null if we want to register a listener to all connected devices.
     * @param listener pretending to listen for notifications of a peripheral.
     */
    @SuppressWarnings("unused")
    public void registerPeripheralListener(final NotificationListener listener, final String address) {
        mBleManager.registerPeripheralListener(listener, address);
    }

    /**
     * Unregister a listener from all connected peripherals.
     *
     * @param listener that does not want to get notifications any more.
     */
    @SuppressWarnings("unused")
    public void unregisterPeripheralListenerFromAllConnected(final NotificationListener listener) {
        mBleManager.unregisterPeripheralListenerFromAllConnected(listener);
    }

    /**
     * Unregister a listener from a connected peripheral.
     *
     * @param listener that wants to unregister from the notifications of a peripheral.
     * @param address  of the peripheral you don't want to get notifications from anymore.
     */
    @SuppressWarnings("unused")
    public void unregisterPeripheralListener(final String address, final NotificationListener listener) {
        mBleManager.unregisterPeripheralListener(listener, address);
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
     * @param context of the requesting activity.
     */
    @SuppressWarnings("unused")
    public void requestEnableBluetooth(final Context context) {
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
     * @param address of the peripheral.
     * @return number of discovered services.
     */
    @SuppressWarnings("unused")
    public int getNumberOfDiscoveredServices(final String address) {
        return mBleManager.getNumberOfDiscoveredServices(address);
    }

    /**
     * Obtains the names of each discovered service.
     *
     * @param address of the peripheral.
     * @return {@link java.util.LinkedList} with the services names.
     */
    @SuppressWarnings("unused")
    public List<String> getDiscoveredServicesNames(final String address) {
        return mBleManager.getDiscoveredServicesNames(address);
    }

    /**
     * Asks for a service with a particular name.
     *
     * @param deviceAddress of the peripheral.
     * @param serviceName   name of the service.
     * @return {@link com.sensirion.libble.bleservice.PeripheralService}
     */
    @SuppressWarnings("unused")
    public PeripheralService getServiceWithName(final String deviceAddress, final String serviceName) {
        return mBleManager.getServiceWithName(deviceAddress, serviceName);
    }

    /**
     * Ask for the a characteristic of a service
     *
     * @param characteristicName name of the characteristic.
     * @return {@link java.lang.Object} with the characteristic parsed by the service - <code>null</code> if no service was able to parse it.
     */
    @SuppressWarnings("unused")
    public Object getCharacteristicValue(final String deviceAddress, final String characteristicName) {
        return mBleManager.getCharacteristicValue(deviceAddress, characteristicName);
    }

    abstract public void onConnectedPeripheralSelected(String address);

    abstract public void onDiscoveredPeripheralSelected(String address);
}