package com.sensirion.libble.devices;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import com.radiusnetworks.bluetooth.BluetoothCrashResolver;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.listeners.devices.DeviceStateListener;
import com.sensirion.libble.listeners.devices.ScanListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This class offers the main functionality of the library to the user (app)
 */
public class BlePeripheralService extends Service implements BluetoothAdapter.LeScanCallback {

    //Class tags.
    private static final String TAG = BlePeripheralService.class.getSimpleName();
    private static final String PREFIX = BlePeripheralService.class.getName();

    //Broadcast actions
    public static final String ACTION_PERIPHERAL_SERVICE_DISCOVERY = PREFIX + "/ACTION_PERIPHERAL_SERVICE_DISCOVERY";
    public static final String ACTION_PERIPHERAL_DISCOVERY = PREFIX + "/ACTION_PERIPHERAL_DISCOVERY";
    public static final String ACTION_PERIPHERAL_CONNECTION_CHANGED = PREFIX + "/ACTION_PERIPHERAL_CONNECTION_CHANGED";
    public static final String ACTION_SCANNING_STARTED = PREFIX + "/ACTION_SCANNING_STARTED";
    public static final String ACTION_SCANNING_STOPPED = PREFIX + "/ACTION_SCANNING_STOPPED";
    public static final String EXTRA_PERIPHERAL_ADDRESS = PREFIX + ".EXTRA_PERIPHERAL_ADDRESS";

    //Default Scan attributes
    private static final int ONE_SECOND_MS = 1000;
    private static final long DEFAULT_SCAN_DURATION_MS = 10 * ONE_SECOND_MS; //10 seconds

    //Binder
    private final IBinder mBinder = new LocalBinder();

    //List of peripherals
    private final Map<String, Peripheral> mDiscoveredPeripherals = Collections.synchronizedMap(new HashMap<String, Peripheral>());
    private final Map<String, Peripheral> mConnectedPeripherals = Collections.synchronizedMap(new HashMap<String, Peripheral>());

    //Listeners list.
    private final Set<DeviceStateListener> mPeripheralStateListeners = Collections.synchronizedSet(new HashSet<DeviceStateListener>());
    private final Set<ScanListener> mPeripheralScanListeners = Collections.synchronizedSet(new HashSet<ScanListener>());
    private final Set<NotificationListener> mPeripheralNotificationListeners = Collections.synchronizedSet(new HashSet<NotificationListener>());

    //Scan attributes.
    private long mScanDurationMs;
    private boolean mIsScanning = false;

    //Android connectors
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothCrashResolver mBluetoothSharingCrashResolver;

    @Override
    public synchronized void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        final String deviceAddress = device.getAddress();
        final Peripheral peripheral;

        mBluetoothSharingCrashResolver.notifyScannedDevice(device, this);

        if (mDiscoveredPeripherals.containsKey(deviceAddress)) {
            Log.d(TAG, String.format("onLeScan() -> Device %s has a rssi of %d.", deviceAddress, rssi));
            peripheral = mDiscoveredPeripherals.get(deviceAddress);
            mDiscoveredPeripherals.get(deviceAddress).setRSSI(rssi);
        } else {
            peripheral = new Peripheral(BlePeripheralService.this, device, rssi);
            mDiscoveredPeripherals.put(deviceAddress, peripheral);
        }
        sendLocalBroadcast(ACTION_PERIPHERAL_DISCOVERY, EXTRA_PERIPHERAL_ADDRESS, deviceAddress);
        notifyDiscoveredPeripheralChanged(peripheral);
    }

    public synchronized void onPeripheralConnectionChanged(@NonNull final Peripheral peripheral) {
        final String address = peripheral.getAddress();
        if (peripheral.isConnected()) {
            if (mDiscoveredPeripherals.containsKey(address)) {
                if (mConnectedPeripherals.containsKey(address)) {
                    Log.w(TAG, String.format("onPeripheralStateChanged -> Peripheral with address %s was already connected.", peripheral.getAddress()));
                } else {
                    mDiscoveredPeripherals.remove(address);
                    mConnectedPeripherals.put(address, peripheral);
                }
            }
        } else if (mConnectedPeripherals.containsKey(address)) {
            mConnectedPeripherals.remove(address);
        }

        sendLocalBroadcast(ACTION_PERIPHERAL_CONNECTION_CHANGED, EXTRA_PERIPHERAL_ADDRESS, address);
        notifyPeripheralConnectionChanged(peripheral);
    }

    public synchronized void onPeripheralServiceDiscovery(@NonNull final Peripheral peripheral) {
        Log.i(TAG, String.format("onPeripheralServiceDiscovery -> Peripheral %s discovered %d services.", peripheral.getAddress(), peripheral.getNumberServices()));
        sendLocalBroadcast(ACTION_PERIPHERAL_SERVICE_DISCOVERY, EXTRA_PERIPHERAL_ADDRESS, peripheral.getAddress());
        notifyPeripheralServiceDiscovery(peripheral);
    }

    private void notifyPeripheralConnectionChanged(@NonNull final Peripheral peripheral) {
        final Iterator<DeviceStateListener> itr = mPeripheralStateListeners.iterator();
        while (itr.hasNext()) {
            final DeviceStateListener listener = itr.next();
            try {
                if (peripheral.isConnected()) {
                    listener.onDeviceConnected(peripheral);
                } else {
                    listener.onDeviceDisconnected(peripheral);
                }
            } catch (final Exception e) {
                Log.e(TAG, "notifyPeripheralConnectionChanged -> The following error was produced when notifying the peripheral states: ", e);
                itr.remove();
            }
        }
    }

    private void notifyDiscoveredPeripheralChanged(@NonNull final Peripheral peripheral) {
        final Iterator<DeviceStateListener> itr = mPeripheralStateListeners.iterator();
        while (itr.hasNext()) {
            final DeviceStateListener listener = itr.next();
            try {
                listener.onDeviceDiscovered(peripheral);
            } catch (final Exception e) {
                Log.e(TAG, "notifyDiscoveredPeripheralChanged -> The following error was produced when notifying the peripheral states: ", e);
                itr.remove();
            }
        }
    }

    private void notifyPeripheralServiceDiscovery(@NonNull final Peripheral peripheral) {
        final Iterator<DeviceStateListener> itr = mPeripheralStateListeners.iterator();
        while (itr.hasNext()) {
            final DeviceStateListener listener = itr.next();
            try {
                listener.onDeviceAllServicesDiscovered(peripheral);
            } catch (final Exception e) {
                Log.e(TAG, "notifyDiscoveredPeripheralChanged -> The following error was produced when notifying the discovery of the peripheral services. ", e);
                itr.remove();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate() -> instantiating BluetoothManager and Adapter");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "onCreate() -> Unable to initialize BluetoothManager.");
                stopSelf();
            } else {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
        } else {
            Log.e(TAG, "onCreate() -> Bluetooth Le not supported in this API version - stopping ");
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        closeAll();
        return super.onUnbind(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    private void closeAll() {
        Log.i(TAG, "closeAll() -> closing all connections.");

        for (final Peripheral peripheral : mConnectedPeripherals.values()) {
            peripheral.close();
        }
        mConnectedPeripherals.clear();
    }

    /**
     * Requests scanning process for BLE devices in range. If the connection to the {@link BlePeripheralService}
     * has not been established yet, startScanning() will be re-triggered as soon as the connection is there.
     *
     * @return <code>true</code> if scan has been started. <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public synchronized boolean startLeScan() {
        return startLeScan(null, DEFAULT_SCAN_DURATION_MS);
    }

    /**
     * Requests scanning process for BLE devices in range. If the connection to the {@link BlePeripheralService}
     * has not been established yet, startScanning() will be re-triggered as soon as the connection is there.
     *
     * @param scanDurationMs that the device will be scanning. Needs to be a positive number.
     * @return <code>true</code> if scan has been started. <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public synchronized boolean startLeScan(final long scanDurationMs) {
        return startLeScan(null, scanDurationMs);
    }

    /**
     * NOTE: This method is buggy in some devices. Passing non 128 bit UUID will solve the bug.
     * Requests scanning process for BLE devices in range. If the connection to the {@link BlePeripheralService}
     * has not been established yet, startScanning() will be re-triggered as soon as the connection is there.
     *
     * @param UUIDs List of UUID that the scan can use. <code>null</code> in case the user is able to use any device.
     * @return <code>true</code> if scan has been started. <code>false</code> otherwise.
     */
    public synchronized boolean startLeScan(@Nullable final UUID[] UUIDs) {
        return startLeScan(UUIDs, DEFAULT_SCAN_DURATION_MS);
    }

    /**
     * NOTE: This method is buggy in some devices. Passing non 128 bit UUID will solve the bug.
     * Requests scanning process for BLE devices in range. If the connection to the {@link BlePeripheralService}
     * has not been established yet, startScanning() will be re-triggered as soon as the connection is there.
     *
     * @param scanDurationMs that the device will be scanning. Needs to be a positive number.
     * @param UUIDs          List of UUID that the scan can use. <code>null</code> in case the user is able to use any device.
     * @return <code>true</code> if scan has been started. <code>false</code> otherwise.
     */
    public synchronized boolean startLeScan(@Nullable final UUID[] UUIDs, final long scanDurationMs) {
        if (scanDurationMs <= 0) {
            throw new IllegalArgumentException(String.format("%s: startLeScan -> Scan duration needs to be a positive number.", TAG));
        }

        mScanDurationMs = scanDurationMs;

        if (mIsScanning) {
            Log.w(TAG, "startLeScan() -> scan already in progress");
            return mIsScanning;
        }
        Log.d(TAG, "startLeScan() -> clear discovered peripherals and add new scan results");
        checkBluetooth();
        mDiscoveredPeripherals.clear();

        if ((UUIDs == null) ? mBluetoothAdapter.startLeScan(this) : mBluetoothAdapter.startLeScan(UUIDs, this)) {
            mIsScanning = true;
            onStartLeScan();
        } else {
            mIsScanning = false;
        }
        return mIsScanning;
    }

    /**
     * Checks if the bluetooth connection is available.
     */
    private void checkBluetooth() {
        if (isBluetoothAvailable()) {
            return;
        }
        throw new IllegalStateException(String.format("%s: checkBluetooth -> Bluetooth is not available!", TAG));
    }

    private void onStartLeScan() {
        Log.i(TAG, "onStartLeScan()");

        mBluetoothSharingCrashResolver = new BluetoothCrashResolver(this);
        mBluetoothSharingCrashResolver.start();

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "onStartLeScan -> handler.postDelayed() -> stopLeScan()");
                stopLeScan();
                handler.removeCallbacks(this);
            }
        }, mScanDurationMs);

        sendLocalBroadcast(ACTION_SCANNING_STARTED);
        notifyScanStateChange(true);
    }

    private void sendLocalBroadcast(@NonNull final String action) {
        sendLocalBroadcast(action, null);
    }

    private void sendLocalBroadcast(@NonNull final String action, @NonNull final String extraKey, @NonNull final String extraValue) {
        final Pair<String, String> peripheralExtra = new Pair<>(extraKey, extraValue);
        final List<Pair<String, String>> broadcastExtras = new LinkedList<>();
        broadcastExtras.add(peripheralExtra);
        sendLocalBroadcast(action, broadcastExtras);
    }

    private void sendLocalBroadcast(@NonNull final String action, @Nullable final List<Pair<String, String>> extras) {
        final Intent intent = new Intent(action);
        if (extras != null) {
            for (final Pair<String, String> extra : extras) {
                intent.putExtra(extra.first, extra.second);
            }
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void notifyScanStateChange(final boolean enabled) {
        final Iterator<ScanListener> itr = mPeripheralScanListeners.iterator();
        while (itr.hasNext()) {
            final ScanListener listener = itr.next();
            try {
                listener.onScanStateChanged(enabled);
            } catch (final Exception e) {
                itr.remove();
                Log.e(TAG, "notifyConnectionChange -> The following error was produced when notifying a peripheral connection change -> ", e);
            }
        }
    }

    /**
     * Checks whether bluetooth is available on the current device.
     *
     * @return <code>true</code> if bluetooth is available - <code>false</code> otherwise.
     */
    public synchronized boolean isBluetoothAvailable() {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "isBluetoothAvailable -> Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Stops scanning for BLE devices.
     */
    public synchronized void stopLeScan() {
        if (mIsScanning) {
            checkBluetooth();
            Log.d(TAG, "stopLeScan() -> mBluetoothAdapter.stopLeScan");
            mBluetoothAdapter.stopLeScan(this);
            mIsScanning = false;
            onStopLeScan();
        } else {
            Log.w(TAG, "stopLeScan() -> no scan in progress");
        }
    }

    private void onStopLeScan() {
        Log.i(TAG, "onStopLeScan() -> Scan was stopped.");
        mBluetoothSharingCrashResolver.stop();
        notifyScanStateChange(false);
        sendLocalBroadcast(ACTION_SCANNING_STOPPED);
    }

    /**
     * Get all discovered {@link com.sensirion.libble.devices.BleDevice}.
     *
     * @return Iterable of {@link com.sensirion.libble.devices.BleDevice}
     */
    public synchronized Iterable<? extends BleDevice> getDiscoveredPeripherals() {
        return new HashSet<BleDevice>(mDiscoveredPeripherals.values());
    }

    /**
     * Get all discovered {@link BleDevice} with valid names for the application.
     *
     * @param validDeviceNames List of devices names.
     * @return Iterable of {@link com.sensirion.libble.devices.BleDevice}
     */
    public synchronized Iterable<? extends BleDevice> getDiscoveredPeripherals(@Nullable final List<String> validDeviceNames) {
        final Set<BleDevice> discoveredPeripherals = new HashSet<BleDevice>(mDiscoveredPeripherals.values());
        if (validDeviceNames == null) { // returns all the devices.
            return discoveredPeripherals;
        }
        final Iterator<? extends BleDevice> iterator = discoveredPeripherals.iterator();
        while (iterator.hasNext()) {
            final BleDevice device = iterator.next();
            if (device == null || validDeviceNames.contains(device.getAdvertisedName())) {
                continue;
            }
            iterator.remove();
        }
        return discoveredPeripherals;
    }

    /**
     * Get all connected {@link BleDevice}.
     *
     * @return Iterable of {@link BleDevice}
     */
    public synchronized Iterable<? extends BleDevice> getConnectedPeripherals() {
        return new HashSet<BleDevice>(mConnectedPeripherals.values());
    }

    /**
     * Returns the number of connected devices.
     *
     * @return <code>int</code> with the number of devices.
     */
    public int getConnectedBleDeviceCount() {
        return mConnectedPeripherals.size();
    }

    /**
     * Returns the {@link BleDevice} belonging to the given address
     *
     * @param address MAC-Address of the desired {@link BleDevice}
     * @return Connected device as {@link com.sensirion.libble.devices.BleDevice} - <code>null</code> if the device is not connected
     */
    public BleDevice getConnectedDevice(@NonNull final String address) {
        return mConnectedPeripherals.get(address);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return <code>true</code> if the connection is initiated successfully. The connection result is reported asynchronously
     * through the {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public synchronized boolean connect(@NonNull final String address) {
        checkBluetooth();
        if (address.trim().isEmpty()) {
            Log.e(TAG, "connect() -> unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mConnectedPeripherals.containsKey(address)) {
            Log.d(TAG, "connect() -> trying to use an existing BluetoothGatt for connection.");
            return mConnectedPeripherals.get(address).reconnect();
        }

        Log.d(TAG, "connect() -> trying to create a new connection.");
        final Peripheral newPeripheral = mDiscoveredPeripherals.get(address);
        if (newPeripheral == null) {
            return false;
        }
        newPeripheral.connect(getApplicationContext());
        return true;
    }

    /**
     * Adds a listener to the peripheral state change notifying list.
     *
     * @param listener that wants to be added - Cannot be <code>null</code>
     */
    public synchronized void registerPeripheralStateListener(@NonNull final DeviceStateListener listener) {
        if (mPeripheralStateListeners.contains(listener)) {
            Log.w(TAG, String.format("registerPeripheralStateListener -> Listener %s was already added to the listener list", listener));
        } else {
            mPeripheralStateListeners.add(listener);
        }
    }

    /**
     * Adds a listener to the peripheral scan state change notifying list.
     *
     * @param listener that wants to be added - Cannot be <code>null</code>
     */
    public synchronized void registerScanListener(@NonNull final ScanListener listener) {
        if (mPeripheralScanListeners.contains(listener)) {
            Log.w(TAG, String.format("registerScanListener -> Listener %s was already added to the listener list", listener));
        } else {
            mPeripheralScanListeners.add(listener);
        }
    }

    /**
     * Removes a listener from the peripheral state change notifying list.
     *
     * @param listener that wants to be removed - Cannot be <code>null</code>
     */
    public synchronized void unregisterPeripheralStateListener(@NonNull final DeviceStateListener listener) {
        if (mPeripheralStateListeners.contains(listener)) {
            mPeripheralStateListeners.remove(listener);
            Log.i(TAG, String.format("unregisterPeripheralStateListener -> Listener %s was removed from the list.", listener));
        } else {
            Log.w(TAG, String.format("unregisterPeripheralStateListener -> Listener %s was already removed from the list.", listener));
        }
    }

    /**
     * Removes a listener from the scan state change notifying list.
     *
     * @param listener that wants to be removed - Cannot be <code>null</code>
     */
    public synchronized void unregisterScanListener(@NonNull final ScanListener listener) {
        if (mPeripheralScanListeners.contains(listener)) {
            mPeripheralScanListeners.remove(listener);
            Log.i(TAG, String.format("unregisterScanListener -> Listener %s was removed from the list.", listener));
        } else {
            Log.w(TAG, String.format("unregisterScanListener -> Listener %s was already removed from the list.", listener));
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public synchronized void disconnect(@NonNull final String deviceAddress) {
        checkBluetooth();
        if (deviceAddress.trim().isEmpty()) {
            Log.w(TAG, "disconnect() -> unspecified address.");
            return;
        }
        if (mConnectedPeripherals.containsKey(deviceAddress)) {
            mConnectedPeripherals.get(deviceAddress).disconnect();
            mConnectedPeripherals.remove(deviceAddress);
        } else {
            Log.w(TAG, String.format("disconnect() -> Device with address %s was not found.", deviceAddress));
        }
    }

    /**
     * Register for notifications automatically when a new device is connected.
     *
     * @param listener that wants to register automatically for notifications on new connected devices.
     */
    public void registerPeripheralListener(@NonNull final NotificationListener listener) {
        mPeripheralNotificationListeners.add(listener);
    }

    /**
     * Unregisters new devices automatic notifications registering.
     *
     * @param listener that doesn't want to register automatically for notifications on new connected devices.
     */
    public void unregisterPeripheralListenerToAllConnected(@NonNull final NotificationListener listener) {
        mPeripheralNotificationListeners.remove(listener);
    }

    /**
     * Checks if the peripheral is scanning for new devices.
     *
     * @return <code>true</code> if the service is scanning - <code>false</code> otherwise.
     */
    public synchronized boolean isScanning() {
        return mIsScanning;
    }

    public class LocalBinder extends Binder {
        public BlePeripheralService getService() {
            return BlePeripheralService.this;
        }
    }
}