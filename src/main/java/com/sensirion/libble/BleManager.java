package com.sensirion.libble;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.peripherals.BleDevice;
import com.sensirion.libble.peripherals.BleManagerService;
import com.sensirion.libble.peripherals.Peripheral;
import com.sensirion.libble.services.NotificationListener;
import com.sensirion.libble.services.PeripheralService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class BleManager {

    private static final String TAG = BleManager.class.getSimpleName();

    private static BleManager mInstance;

    private boolean mShouldStartScanning;

    private BleManagerService mBleManagerService;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            Log.i(TAG, "onServiceConnected() -> connected to BleManagerService");
            mBleManagerService = ((BleManagerService.LocalBinder) service).getService();

            if (mShouldStartScanning) {
                Log.i(TAG, "onServiceConnected() -> re-trigger startScanning()");
                startScanning();
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            Log.w(TAG, "onServiceDisconnected() -> disconnected from BleManagerService");
            mBleManagerService = null;
        }
    };

    private BleManager() {
    }

    public synchronized static BleManager getInstance() {
        if (mInstance == null) {
            mInstance = new BleManager();
        }
        return mInstance;
    }

    /**
     * This method should be called immediately just after the first getInstance()
     * call in the application that wants to use the library.
     *
     * @param ctx cannot be <code>null</code>
     */
    public void init(@NonNull final Context ctx) {
        Log.i(TAG, "init() -> binding to BleManagerService");
        final Intent intent = new Intent(ctx.getApplicationContext(), BleManagerService.class);

        if (ctx.getApplicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.i(TAG, "init() -> successfully bound to BleManagerService!");
        } else {
            throw new IllegalStateException(String.format("%s: init() -> unable to bind to BleManagerService!", TAG));
        }
    }

    /**
     * This method should be called at the end of the execution of the implementing application.
     *
     * @param ctx of the implementing application.
     */
    public void release(@NonNull final Context ctx) {
        try {
            Log.w(TAG, "release() -> unbinding mServiceConnection");
            ctx.getApplicationContext().unbindService(mServiceConnection);
        } catch (final Exception e) {
            Log.e(TAG, "An exception was produced when when trying to unbind from it.", e);
        }
    }

    /**
     * Starts to scan for all bluetooth devices in range.
     *
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    public boolean startScanning() {
        return startScanning(null);
    }

    /**
     * Start scanning devices in range using provided UUIDs.
     *
     * @param deviceUUIDs deviceUUIDs that we want want to use,
     *                    <code>null</code> if all devices have to be retrieved.
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    public boolean startScanning(@Nullable final UUID[] deviceUUIDs) {
        if (mBleManagerService == null) {
            Log.w(TAG, "startScanning() -> not yet connected to BleManagerService; try to re-trigger scanning when connected.");
            mShouldStartScanning = true;
            return false;
        }
        if (mBleManagerService.isScanning()) {
            Log.w(TAG, "startScanning() -> already scanning; ignoring this request.");
            return true;
        }
        if (deviceUUIDs == null) {
            Log.d(TAG, "startScanning() -> mBleManagerService.startLeScan()");
            return mBleManagerService.startLeScan();
        }
        Log.d(TAG, "startScanning() -> mBleManagerService.startLeScan(UUIDs)");
        return mBleManagerService.startLeScan(deviceUUIDs);
    }

    /**
     * Stops the scan of new devices.
     */
    public void stopScanning() {
        if (mBleManagerService == null) {
            Log.w(TAG, "stopScanning() -> not connected to BleManagerService");
            return;
        }

        if (mBleManagerService.isScanning()) {
            Log.d(TAG, "stopScanning() -> mBleManagerService.stopLeScan()");
            mBleManagerService.stopLeScan();
        }
    }

    /**
     * Ask if the peripheral service is scanning.
     *
     * @return <code>true</code> if it's scanning - <code>false</code> otherwise.
     */
    public boolean isScanning() {
        if (mBleManagerService == null) {
            Log.w(TAG, "isScanning() -> not connected to BleManagerService");
            return false;
        }
        return mBleManagerService.isScanning();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.peripherals.BleDevice}.
     *
     * @return Iterable with {@link com.sensirion.libble.peripherals.BleDevice}
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        if (mBleManagerService == null) {
            Log.w(TAG, "getDiscoveredBleDevices() -> not connected to BleManagerService");
            return new ArrayList<>();
        }
        return mBleManagerService.getDiscoveredPeripherals();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.peripherals.BleDevice} with valid names for the application.
     *
     * @param validDeviceNames {@link java.util.List} of devices names.
     * @return Iterable with {@link com.sensirion.libble.peripherals.BleDevice}
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(@Nullable final List<String> validDeviceNames) {
        if (mBleManagerService == null) {
            Log.w(TAG, "getDiscoveredBleDevices(List<String>) -> not connected to BleManagerService");
            return new LinkedList<>();
        }
        return mBleManagerService.getDiscoveredPeripherals(validDeviceNames);
    }

    /**
     * Returns the number of connected devices.
     *
     * @return <code>int</code> with the number of devices.
     */
    public int getConnectedBleDeviceCount() {
        if (mBleManagerService == null) {
            Log.w(TAG, "getConnectedDeviceCount() -> not connected to BleManagerService");
            return 0;
        }
        return mBleManagerService.getConnectedBleDeviceCount();
    }

    /**
     * Get all connected {@link com.sensirion.libble.peripherals.BleDevice}
     *
     * @return Iterable of {@link com.sensirion.libble.peripherals.BleDevice}
     */
    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        if (mBleManagerService == null) {
            Log.w(TAG, "getConnectedBleDevices() -> not connected to BleManagerService");
            return new LinkedList<>();
        }
        return mBleManagerService.getConnectedPeripherals();
    }

    /**
     * Returns the {@link com.sensirion.libble.peripherals.BleDevice} belonging to the given address
     *
     * @param deviceAddress MAC-Address of the desired {@link com.sensirion.libble.peripherals.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.peripherals.BleDevice} or <code>null</code> if the device is not connected
     */
    public BleDevice getConnectedDevice(final String deviceAddress) {
        if (mBleManagerService == null) {
            Log.w(TAG, "getConnectedDevice() -> not connected to BleManagerService.");
            return null;
        }
        return mBleManagerService.getConnectedDevice(deviceAddress);
    }

    /**
     * Tries to establish a connection to a selected peripheral (by address)
     *
     * @param deviceAddress of the peripheral that should be connected
     */
    public boolean connectPeripheral(final String deviceAddress) {
        if (mBleManagerService == null) {
            Log.e(TAG, String.format("connectPeripheral -> %s is null so peripheral can't be connected.", BleManagerService.class.getSimpleName()));
            return false;
        }
        Log.d(TAG, "connectPeripheral() -> stopScanning()");
        stopScanning();
        return mBleManagerService.connect(deviceAddress);
    }

    /**
     * Tries to disconnect a selected peripheral (by address)
     *
     * @param deviceAddress of the peripheral that should be disconnected
     */
    public void disconnectPeripheral(final String deviceAddress) {
        if (mBleManagerService == null) {
            Log.e(TAG, String.format("disconnectPeripheral -> %s is null so peripheral can't be disconnected.", BleManagerService.class.getSimpleName()));
        } else {
            mBleManagerService.disconnect(deviceAddress);
        }
    }

    /**
     * Looks for a connected peripheral.
     *
     * @param deviceAddress of the peripheral.
     * @return the {@link com.sensirion.libble.peripherals.Peripheral} with the given address
     */
    public Peripheral getConnectedPeripheral(final String deviceAddress) {
        final Iterator<? extends BleDevice> iterator = getConnectedBleDevices().iterator();
        while (iterator.hasNext()) {
            final BleDevice device = iterator.next();
            try {
                if (deviceAddress.equals(device.getAddress())) {
                    Log.i(TAG, String.format("getConnectedPeripheral -> Peripheral with address %s was found.", deviceAddress));
                    return (Peripheral) device;
                }
            } catch (NullPointerException e) {
                iterator.remove();
                Log.w(TAG, String.format("getConnectedPeripheral -> Device with address %s was disconnected.", deviceAddress));
            }
        }
        return null;
    }

    /**
     * Asks for a service with a particular name.
     *
     * @param deviceAddress of the peripheral.
     * @param serviceName   name of the service.
     * @return {@link com.sensirion.libble.services.PeripheralService}
     */
    public PeripheralService getServiceWithName(final String deviceAddress, final String serviceName) {
        final Peripheral device = getConnectedPeripheral(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getServiceWithName -> Device with address %s not found.", deviceAddress));
            return null;
        }
        return device.getPeripheralService(serviceName);
    }

    /**
     * Ask for the a characteristic of a service
     *
     * @param characteristicName name of the characteristic.
     * @return {@link java.lang.Object} with the characteristic parsed by the service - <code>null</code> if no service was able to parse it.
     */
    public Object getCharacteristicValue(final String deviceAddress, final String characteristicName) {
        final Peripheral device = getConnectedPeripheral(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getCharacteristicValue -> Device with address %s did not found the characteristic with name: %s", deviceAddress, characteristicName));
            return null;
        }
        return device.getCharacteristicValue(characteristicName);
    }

    /**
     * Register a listener in all connected peripherals.ho
     *
     * @param listener pretending to listen for notifications in all peripherals.
     */
    public void registerPeripheralListenerToAllConnected(final NotificationListener listener) {
        registerPeripheralListener(listener, null);
    }


    /**
     * Registers a listener in a connected peripheral.
     *
     * @param address  address of the peripheral we want to listen to,
     *                 <code>null</code> if we want to register a listener to all connected devices.
     * @param listener pretending to listen for notifications of a peripheral.
     */
    public void registerPeripheralListener(final NotificationListener listener, final String address) {
        for (final BleDevice device : getConnectedBleDevices()) {
            if (address == null || device.getAddress().equals(address)) {
                ((Peripheral) device).registerPeripheralListener(listener);
                if (address == null) {
                    continue;
                }
                break;
            }
        }
    }

    /**
     * Unregister a listener from all connected peripherals.
     *
     * @param listener that does not want to get notifications any more.
     */
    public void unregisterPeripheralListenerFromAllConnected(final NotificationListener listener) {
        unregisterPeripheralListener(listener, null);
    }

    /**
     * Unregister a listener from a connected peripheral.
     *
     * @param listener      that wants to unregister from the notifications of a peripheral.
     * @param deviceAddress of the peripheral you don't want to get notifications from anymore.
     */
    public void unregisterPeripheralListener(final NotificationListener listener, final String deviceAddress) {
        final Peripheral device = getConnectedPeripheral(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("unregisterPeripheralListener -> Device with address %s not found.", deviceAddress));
        } else {
            device.unregisterPeripheralListener(listener);
        }
    }

    /**
     * Checks if bluetooth connection is enabled on the device.
     *
     * @return <code>true</code> if it's enabled - <code>false</code> otherwise.
     */
    public boolean isBluetoothEnabled() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "isBluetoothEnabled -> Bluetooth adapter was not found.");
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param ctx of the requesting activity.
     */
    public void requestEnableBluetooth(@NonNull final Context ctx) {
        if (isBluetoothEnabled()) {
            Log.d(TAG, "requestEnableBluetooth -> Bluetooth is enabled");
        } else {
            Log.d(TAG, "requestEnableBluetooth -> Enabling bluetooth.");
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ctx.startActivity(enableBtIntent);
        }
    }

    /**
     * Checks if a device is connected.
     *
     * @param deviceAddress of the device.
     * @return <code>true</code> if connected - <code>false</code> otherwise.
     */
    public boolean isDeviceConnected(final String deviceAddress) {
        if (mBleManagerService == null) {
            Log.w(TAG, "isDeviceConnected -> not connected to BleManagerService");
            return false;
        }

        for (final BleDevice device : mBleManagerService.getConnectedPeripherals()) {
            if (device.getAddress().equals(deviceAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enables/disables notifications for all connected devices.
     *
     * @param enabled <code>true</code> for enabling notifications - <code>false</code> otherwise.
     */
    public void setAllNotificationsEnabled(final boolean enabled) {
        Log.i(TAG, String.format("%s all the notifications in all the devices.", (enabled) ? "Enabled" : "Disabled"));
        for (final BleDevice device : getConnectedBleDevices()) {
            ((Peripheral) device).setAllNotificationsEnabled(enabled);
        }
    }

    /**
     * Counts the number of services.
     *
     * @param deviceAddress of the peripheral.
     * @return number of discovered services.
     */
    public int getNumberOfDiscoveredServices(final String deviceAddress) {
        final Peripheral device = getConnectedPeripheral(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getNumberOfDiscoveredServices -> Device with address %s not found.", deviceAddress));
            return -1;
        }
        return device.getNumberOfDiscoveredServices();
    }

    /**
     * Obtains the names of each discovered service.
     *
     * @param deviceAddress of the peripheral.
     * @return {@link java.util.List} with the services names.
     */
    public List<String> getDiscoveredServicesNames(final String deviceAddress) {
        final Peripheral device = getConnectedPeripheral(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getDiscoveredServicesNames -> Device with address %s not found.", deviceAddress));
            return null;
        }
        return device.getDiscoveredServicesNames();
    }
}