package com.sensirion.libble.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.BleManager;
import com.sensirion.libble.NotificationListener;

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
    public void onCreate(Bundle savedInstanceState) {
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
            //Do nothing.
        } else {
            mBleManager.release(getApplicationContext());
        }
        super.onStop();
    }

    /**
     * Starts to scan for all bluetooth devices in range.
     *
     * @return true if it's scanning, false otherwise.
     */
    protected boolean startScanning() {
        return mBleManager.startScanning();
    }

    /**
     * Start scanning devices in range using provided UUIDs.
     *
     * @param deviceUUIDs deviceUUIDs that we want want to use,
     *                    null if all devices have to be retrieved.
     * @return true if it's scanning, false otherwise.
     */
    protected boolean startScanning(UUID[] deviceUUIDs) {
        return mBleManager.startScanning(deviceUUIDs);
    }

    /**
     * Stops the scan of new devices.
     */
    protected void stopScanning() {
        mBleManager.stopScanning();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    protected Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        return mBleManager.getDiscoveredBleDevices();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with only one name in particular.
     *
     * @param deviceName device name needed by the application.
     * @return Iterable
     */
    protected Iterable<? extends BleDevice> getDiscoveredBleDevices(String deviceName) {
        return mBleManager.getDiscoveredBleDevices(deviceName);
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with valid names for the application.
     *
     * @param deviceNames List of devices names.
     * @return Iterable
     */
    protected Iterable<? extends BleDevice> getDiscoveredBleDevices(List<String> deviceNames) {
        return mBleManager.getDiscoveredBleDevices(deviceNames);
    }

    /**
     * Get all connected {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    protected Iterable<? extends BleDevice> getConnectedBleDevices() {
        return mBleManager.getConnectedBleDevices();
    }

    /**
     * Returns the {@link com.sensirion.libble.BleDevice} belonging to the given address
     *
     * @param address MAC-Address of the desired {@link com.sensirion.libble.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.BleDevice}
     * or NULL if the device is not connected
     */
    protected BleDevice getConnectedDevice(String address) {
        return mBleManager.getConnectedDevice(address);
    }

    /**
     * Tries to establish a connection toa selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be connected
     */
    protected boolean connectPeripheral(String address) {
        return mBleManager.connectPeripheral(address);
    }

    /**
     * Tries to disconnect a selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be disconnected
     */
    protected void disconnectPeripheral(String address) {
        mBleManager.disconnectPeripheral(address);
    }

    /**
     * Register a listener in all connected peripherals.
     *
     * @param listener pretending to listen for notifications in all peripherals.
     */
    public void registerPeripheralListenerToAllConnected(NotificationListener listener) {
        mBleManager.registerPeripheralListenerToAllConnected(listener);
    }

    /**
     * Registers a listener in a connected peripheral.
     *
     * @param address  address of the peripheral we want to listen to,
     *                 null if we want to register a listener to all connected devices.
     * @param listener pretending to listen for notifications of a peripheral.
     */
    public void registerPeripheralListener(String address, NotificationListener listener) {
        mBleManager.registerPeripheralListener(address, listener);
    }

    /**
     * Unregister a listener from all connected peripherals.
     *
     * @param listener that does not want to get notifications any more.
     */
    public void unregisterPeripheralListenerFromAllConnected(NotificationListener listener) {
        mBleManager.unregisterPeripheralListenerFromAllConnected(listener);
    }

    /**
     * Unregister a listener from a connected peripheral.
     *
     * @param address  of the peripheral you don't want to get notifications from anymore.
     * @param listener that wants to unregister from the notifications of a peripheral.
     */
    public void unregisterPeripheralListener(String address, NotificationListener listener) {
        mBleManager.unregisterPeripheralListener(address, listener);
    }

    /**
     * Checks if bluetooth connection is enabled on the device.
     */
    public boolean isBluetoothEnabled() {
        return mBleManager.isBluetoothEnabled();
    }

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param context of the requesting activity.
     */
    public void requestEnableBluetooth(Context context) {
        mBleManager.requestEnableBluetooth(context);
    }

    abstract public void onConnectedPeripheralSelected(String address);

    abstract public void onDiscoveredPeripheralSelected(String address);
}