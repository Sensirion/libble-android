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

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.devices.BlePeripheralService;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.listeners.devices.DeviceStateListener;
import com.sensirion.libble.listeners.devices.ScanListener;
import com.sensirion.libble.services.BleService;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BleManager {

    private static final String TAG = BleManager.class.getSimpleName();
    private static BleManager mInstance;

    //List with the listeners that are used when {@link com.sensirion.libble.devices.BlePeripheralService} is not connected.
    private final Set<NotificationListener> mNotificationListeners = Collections.synchronizedSet(new HashSet<NotificationListener>());
    private final Set<DeviceStateListener> mDeviceStateListeners = Collections.synchronizedSet(new HashSet<DeviceStateListener>());
    private final Set<ScanListener> mScanListeners = Collections.synchronizedSet(new HashSet<ScanListener>());

    //Manager values
    private boolean mShouldStartScanning;
    private BlePeripheralService mBlePeripheralService;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            Log.i(TAG, "onServiceConnected() -> connected to BlePeripheralService");
            mBlePeripheralService = ((BlePeripheralService.LocalBinder) service).getService();

            for (final DeviceStateListener listener : mDeviceStateListeners) {
                mBlePeripheralService.registerPeripheralStateListener(listener);
            }
            for (final ScanListener listener : mScanListeners) {
                mBlePeripheralService.registerScanListener(listener);
            }
            for (final NotificationListener listener : mNotificationListeners) {
                mBlePeripheralService.registerPeripheralListener(listener);
            }

            mDeviceStateListeners.clear();
            mScanListeners.clear();
            mNotificationListeners.clear();

            if (mShouldStartScanning) {
                Log.i(TAG, "onServiceConnected() -> re-trigger startScanning()");
                startScanning();
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            Log.w(TAG, "onServiceDisconnected() -> disconnected from BlePeripheralService");
            mBlePeripheralService = null;
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
     * @param context cannot be <code>null</code>
     */
    public void init(@NonNull final Context context) {
        Log.i(TAG, "init() -> binding to BlePeripheralService");
        final Intent intent = new Intent(context.getApplicationContext(), BlePeripheralService.class);

        if (context.getApplicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.i(TAG, "init() -> successfully bound to BlePeripheralService!");
        } else {
            throw new IllegalStateException(String.format("%s: init() -> unable to bind to BlePeripheralService!", TAG));
        }
    }

    /**
     * This method should be called at the end of the execution of the implementing application.
     *
     * @param context of the implementing application.
     */
    public void release(@NonNull final Context context) {
        try {
            Log.w(TAG, "release() -> unbinding mServiceConnection");
            context.getApplicationContext().unbindService(mServiceConnection);
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
        return startScanning(null, null);
    }


    /**
     * Starts to scan for all bluetooth devices in range.
     *
     * @param scanDurationMs that the device will be scanning. Needs to be a positive number.
     * @return <code>true</code> if scan has been started. <code>false</code> otherwise.
     */
    public synchronized boolean startScanning(final long scanDurationMs) {
        return startScanning(null, scanDurationMs);
    }

    /**
     * Start scanning devices in range for provided UUIDs.
     *
     * @param deviceUUIDs deviceUUIDs that we want want to use,
     *                    <code>null</code> if all devices have to be retrieved.
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    public boolean startScanning(@Nullable final UUID[] deviceUUIDs) {
        return startScanning(deviceUUIDs, null);
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
        if (mBlePeripheralService == null) {
            Log.w(TAG, "startScanning() -> not yet connected to BlePeripheralService; try to re-trigger scanning when connected.");
            mShouldStartScanning = true;
            return false;
        }
        if (mBlePeripheralService.isScanning()) {
            Log.w(TAG, "startScanning() -> already scanning; ignoring this request.");
            return true;
        }

        Log.d(TAG, "startScanning() -> mBlePeripheralService.startLeScan()");

        if (scanDurationMs == null) {
            return mBlePeripheralService.startLeScan(deviceUUIDs);
        }
        return mBlePeripheralService.startLeScan(deviceUUIDs, scanDurationMs);
    }

    /**
     * Stops the scan of new devices.
     */
    public void stopScanning() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "stopScanning() -> not connected to BlePeripheralService");
            return;
        }

        if (mBlePeripheralService.isScanning()) {
            Log.d(TAG, "stopScanning() -> mBlePeripheralService.stopLeScan()");
            mBlePeripheralService.stopLeScan();
        }
    }

    /**
     * Ask if the local device is scanning for new devices.
     *
     * @return <code>true</code> if it's scanning - <code>false</code> otherwise.
     */
    public boolean isScanning() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "isScanning() -> not connected to BlePeripheralService");
            return false;
        }
        return mBlePeripheralService.isScanning();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.devices.BleDevice}.
     *
     * @return Iterable with {@link com.sensirion.libble.devices.BleDevice}
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "getDiscoveredBleDevices() -> not connected to BlePeripheralService");
            return new LinkedList<>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.devices.BleDevice} with valid names for the application.
     *
     * @param validDeviceNames {@link java.util.List} of devices names.
     * @return Iterable with {@link com.sensirion.libble.devices.BleDevice}
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(@Nullable final List<String> validDeviceNames) {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "getDiscoveredBleDevices(List<String>) -> not connected to BlePeripheralService");
            return new LinkedList<>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals(validDeviceNames);
    }

    /**
     * Returns the number of connected devices.
     *
     * @return <code>int</code> with the number of devices.
     */
    public int getConnectedBleDeviceCount() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "getConnectedDeviceCount() -> not connected to BlePeripheralService");
            return 0;
        }
        return mBlePeripheralService.getConnectedBleDeviceCount();
    }

    /**
     * Get all connected {@link com.sensirion.libble.devices.BleDevice}
     *
     * @return Iterable of {@link com.sensirion.libble.devices.BleDevice}
     */
    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "getConnectedBleDevices() -> not connected to BlePeripheralService");
            return new LinkedList<>();
        }
        return mBlePeripheralService.getConnectedPeripherals();
    }

    /**
     * Returns the {@link com.sensirion.libble.devices.BleDevice} belonging to the given address
     *
     * @param deviceAddress MAC-Address of the desired {@link com.sensirion.libble.devices.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.devices.BleDevice} or <code>null</code> if the device is not connected
     */
    public BleDevice getConnectedDevice(@NonNull final String deviceAddress) {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "getConnectedDevice() -> not connected to BlePeripheralService.");
            return null;
        }
        return mBlePeripheralService.getConnectedDevice(deviceAddress);
    }

    /**
     * Tries to establish a connection to a selected peripheral (by address)
     *
     * @param deviceAddress of the device that should be connected
     */
    public boolean connectDevice(@NonNull final String deviceAddress) {
        if (mBlePeripheralService == null) {
            Log.e(TAG, String.format("connectDevice -> %s is null so peripheral can't be connected.", BlePeripheralService.class.getSimpleName()));
            return false;
        }
        Log.d(TAG, "connectDevice() -> stopScanning()");
        stopScanning();
        return mBlePeripheralService.connect(deviceAddress);
    }

    /**
     * Tries to disconnect a selected device (by address)
     *
     * @param deviceAddress of the device that will be disconnected
     */
    public void disconnectDevice(@NonNull final String deviceAddress) {
        if (mBlePeripheralService == null) {
            Log.e(TAG, String.format("disconnectDevice -> %s is null so the device can't be disconnected.", BlePeripheralService.class.getSimpleName()));
        } else {
            mBlePeripheralService.disconnect(deviceAddress);
        }
    }

    /**
     * Asks for a service with a particular name.
     *
     * @param deviceAddress of the device.
     * @param serviceName   name of the service.
     * @return {@link com.sensirion.libble.services.BleService}
     */
    public BleService getServiceWithName(@NonNull final String deviceAddress, @NonNull final String serviceName) {
        final BleDevice device = getConnectedDevice(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getServiceWithName -> Device with address %s not found.", deviceAddress));
            return null;
        }
        return device.getDeviceService(serviceName);
    }

    /**
     * Registers a listener in a connected device.
     * This listener will be registered to every service of a device.
     *
     * @param deviceAddress of the device we want to listen to - <code>null</code> if we want to register a listener to all connected devices.
     * @param listener      pretending to listen for notifications of a device.
     */
    public void registerDeviceListener(@NonNull final NotificationListener listener, @Nullable final String deviceAddress) {
        for (final BleDevice device : getConnectedBleDevices()) {
            if (deviceAddress == null || device.getAddress().equals(deviceAddress)) {
                device.registerDeviceListener(listener);
                if (deviceAddress == null) {
                    continue;
                }
                break;
            }
        }
    }

    /**
     * Unregister a listener from a connected device.
     *
     * @param listener      that wants to unregister from the notifications of a device.
     * @param deviceAddress of the device you don't want to get notifications from anymore.
     */
    public void unregisterDeviceListener(@NonNull final NotificationListener listener, @NonNull final String deviceAddress) {
        final BleDevice device = getConnectedDevice(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("unregisterDeviceListener -> Device with address %s not found.", deviceAddress));
        } else {
            device.unregisterDeviceListener(listener);
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
     * @param context {@link android.content.Context} of the requesting activity.
     */
    public void requestEnableBluetooth(@NonNull final Context context) {
        if (isBluetoothEnabled()) {
            Log.d(TAG, "requestEnableBluetooth -> Bluetooth is enabled");
        } else {
            Log.d(TAG, "requestEnableBluetooth -> Enabling bluetooth.");
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
        }
    }

    /**
     * Checks if a device is connected.
     *
     * @param deviceAddress of the device.
     * @return <code>true</code> if connected - <code>false</code> otherwise.
     */
    public boolean isDeviceConnected(@NonNull final String deviceAddress) {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "isDeviceConnected -> not connected to BlePeripheralService");
            return false;
        }
        for (final BleDevice device : mBlePeripheralService.getConnectedPeripherals()) {
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
            device.setAllNotificationsEnabled(enabled);
        }
    }

    /**
     * Counts the number of services.
     *
     * @param deviceAddress of the peripheral.
     * @return number of discovered services.
     */
    public int getNumberOfDiscoveredServices(@NonNull final String deviceAddress) {
        final BleDevice device = getConnectedDevice(deviceAddress);
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
    public Iterable<String> getDiscoveredServicesNames(@NonNull final String deviceAddress) {
        final BleDevice device = getConnectedDevice(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getDiscoveredServicesNames -> Device with address %s not found.", deviceAddress));
            return null;
        }
        return device.getDiscoveredServicesNames();
    }

    /**
     * Adds a listener to the library notification listener list.
     * This listener will be registered to every service of all the devices connected or that will be connected.
     *
     * @param listener that wants to be added - Cannot be <code>null</code>
     */
    public synchronized void registerNotificationListener(@NonNull final NotificationListener listener) {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "registerNotificationListener -> Peripheral Service is not enabled yet.");
            if (listener instanceof DeviceStateListener) {
                mDeviceStateListeners.add((DeviceStateListener) listener);
            }
            if (listener instanceof ScanListener) {
                mScanListeners.add((ScanListener) listener);
            }
            mNotificationListeners.add(listener);
        } else {
            if (listener instanceof DeviceStateListener) {
                mBlePeripheralService.registerPeripheralStateListener((DeviceStateListener) listener);
            }
            if (listener instanceof ScanListener) {
                mBlePeripheralService.registerScanListener((ScanListener) listener);
            }
            mBlePeripheralService.registerPeripheralListener(listener);
        }
    }

    /**
     * Removes a listener from the library listener list.
     *
     * @param listener that wants to be removed - Cannot be <code>null</code>
     */
    public synchronized void unregisterNotificationListener(@NonNull final NotificationListener listener) {
        if (mBlePeripheralService == null) {
            if (listener instanceof DeviceStateListener) {
                mDeviceStateListeners.remove(listener);
            }
            if (listener instanceof ScanListener) {
                mScanListeners.remove(listener);
            }
            mNotificationListeners.remove(listener);
        } else {
            if (listener instanceof DeviceStateListener) {
                mBlePeripheralService.unregisterPeripheralStateListener((DeviceStateListener) listener);
            }
            if (listener instanceof ScanListener) {
                mBlePeripheralService.unregisterScanListener((ScanListener) listener);
            }
            mBlePeripheralService.unregisterPeripheralListenerToAllConnected(listener);
        }
    }
}