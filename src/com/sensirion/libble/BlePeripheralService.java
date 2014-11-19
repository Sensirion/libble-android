package com.sensirion.libble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.radiusnetworks.bluetooth.BluetoothCrashResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * This class offers the main functionality of the library to the user (app)
 */
public class BlePeripheralService extends Service implements BluetoothAdapter.LeScanCallback, BlePeripheralHandler {

    private static final String TAG = BlePeripheralService.class.getSimpleName();
    private static final String PREFIX = BlePeripheralService.class.getName();
    public static final String ACTION_PERIPHERAL_DISCOVERY = PREFIX + "/ACTION_PERIPHERAL_DISCOVERY";
    public static final String ACTION_PERIPHERAL_CONNECTION_CHANGED = PREFIX + "/ACTION_PERIPHERAL_CONNECTION_CHANGED";
    public static final String ACTION_SCANNING_STARTED = PREFIX + "/ACTION_SCANNING_STARTED";
    public static final String ACTION_SCANNING_STOPPED = PREFIX + "/ACTION_SCANNING_STOPPED";
    public static final String EXTRA_PERIPHERAL_ADDRESS = PREFIX + ".EXTRA_PERIPHERAL_ADDRESS";
    private static final long DEFAULT_SCAN_DURATION_MS = 10 * 1000;
    private final IBinder mBinder = new LocalBinder();
    private final Map<String, Peripheral> mDiscoveredPeripherals = Collections.synchronizedMap(new HashMap<String, Peripheral>());
    private final Map<String, Peripheral> mConnectedPeripherals = Collections.synchronizedMap(new HashMap<String, Peripheral>());
    private Timer mScanTimer;
    private boolean mIsScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothCrashResolver mBluetoothSharingCrashResolver;

    @Override
    public synchronized void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        final String address = device.getAddress();

        mBluetoothSharingCrashResolver.notifyScannedDevice(device, this);

        if (mDiscoveredPeripherals.containsKey(address)) {
            Log.d(TAG, "onLeScan() -> updating rssi for known device: " + address);
            mDiscoveredPeripherals.get(address).setReceivedSignalStrengthIndication(rssi);
        } else {
            mDiscoveredPeripherals.put(address, new Peripheral(this, device, rssi));
        }

        onPeripheralChanged(ACTION_PERIPHERAL_DISCOVERY, address);
    }

    private void onPeripheralChanged(final String action, final String address) {
        Log.i(TAG, "onPeripheralChanged() for action: " + action);
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_PERIPHERAL_ADDRESS, address);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public synchronized void onPeripheralConnectionChanged(final Peripheral peripheral) {
        final String address = peripheral.getAddress();

        if (peripheral.isConnected()) {
            if (mDiscoveredPeripherals.containsKey(address)) {
                if (mConnectedPeripherals.containsKey(address)) {
                    Log.w(TAG, String.format("onPeripheralConnectionChanged -> Peripheral with address %s was already connected.", peripheral.getAddress()));
                } else {
                    mDiscoveredPeripherals.remove(address);
                    mConnectedPeripherals.put(address, peripheral);
                }
            }
        } else if (mConnectedPeripherals.containsKey(address)) {
            mConnectedPeripherals.remove(address);
        }
        onPeripheralChanged(ACTION_PERIPHERAL_CONNECTION_CHANGED, address);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate(): instantiating BluetoothManager and -Adapter");

        /*
         * For API level 18 and above, get a reference to BluetoothAdapter
         * through BluetoothManager.
         */
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
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
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Called when all clients have disconnected from a particular interface
        // published by the service.
        closeAll();
        return super.onUnbind(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    private void closeAll() {
        Log.i(TAG, "closeAll() -> closing all connections.");

        //After using a given device, you should make sure that BluetoothGatt.close()
        // is called such that resources are cleaned up properly.
        for (final Peripheral p : mConnectedPeripherals.values()) {
            p.close();
        }
        mConnectedPeripherals.clear();
    }

    /**
     * Requests scanning process for BLE devices in range. If the connection to the {@link BlePeripheralService}
     * has not been established yet, startScanning() will be re-triggered as soon as the connection is there.
     *
     * @return true if scan has been started. False otherwise.
     */
    public synchronized boolean startLeScan() {
        return startLeScan(null);
    }

    /**
     * NOTE: This method is buggy in some devices. Passing non 128 bit UUID will solve the bug.
     * Requests scanning process for BLE devices in range. If the connection to the {@link BlePeripheralService}
     * has not been established yet, startScanning() will be re-triggered as soon as the connection is there.
     *
     * @param UUIDs List of UUID that the scan can use. NULL in case the user is able to use any device.
     * @return <code>true</code> if scan has been started. <code>false</code> otherwise.
     */
    public synchronized boolean startLeScan(final UUID[] UUIDs) {
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
        throw new IllegalStateException("Bluetooth is not available!");
    }

    private void onStartLeScan() {
        Log.i(TAG, "onStartLeScan()");

        mBluetoothSharingCrashResolver = new BluetoothCrashResolver(this);
        mBluetoothSharingCrashResolver.start();

        mScanTimer = new Timer();
        mScanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "mScanTimer.TimerTask() -> stopLeScan()");
                stopLeScan();
            }
        }, DEFAULT_SCAN_DURATION_MS);

        Intent intent = new Intent(ACTION_SCANNING_STARTED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * Checks whether bluetooth is available on the current device.
     *
     * @return <code>true</code> if bluetooth is available - <code>false</code> otherwise.
     */
    public synchronized boolean isBluetoothAvailable() {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
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
        Log.i(TAG, "onStopLeScan() -> cancelling the scan timer");

        mScanTimer.cancel();
        Log.d(TAG, "onStopLeScan() -> purging tasks: " + mScanTimer.purge());
        mScanTimer = null;

        mBluetoothSharingCrashResolver.stop();
        Intent intent = new Intent(ACTION_SCANNING_STOPPED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable of {@link com.sensirion.libble.BleDevice}
     */
    public synchronized Iterable<? extends BleDevice> getDiscoveredPeripherals() {
        return new HashSet<BleDevice>(mDiscoveredPeripherals.values());
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with only one name in particular.
     *
     * @param validDeviceName device name needed by the application.
     * @return Iterable of {@link com.sensirion.libble.BleDevice}
     */
    public Iterable<? extends BleDevice> getDiscoveredPeripherals(final String validDeviceName) {
        return getDiscoveredPeripherals(new LinkedList<String>(Arrays.asList(validDeviceName)));
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with valid names for the application.
     *
     * @param validDeviceNames List of devices names.
     * @return Iterable of {@link com.sensirion.libble.BleDevice}
     */
    public synchronized Iterable<? extends BleDevice> getDiscoveredPeripherals(final List<String> validDeviceNames) {
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
     * Get all connected {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable of {@link com.sensirion.libble.BleDevice}
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
     * Returns the {@link com.sensirion.libble.BleDevice} belonging to the given address
     *
     * @param address MAC-Address of the desired {@link com.sensirion.libble.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.BleDevice}
     * or <code>null</code> if the device is not connected
     */
    public BleDevice getConnectedDevice(final String address) {
        return mConnectedPeripherals.get(address);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public synchronized boolean connect(final String address) {
        checkBluetooth();
        if (address == null || address.trim().isEmpty()) {
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
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public synchronized void disconnect(final String address) {
        checkBluetooth();
        if (address == null || address.equals("")) {
            Log.w(TAG, "disconnect() -> unspecified address.");
            return;
        }

        if (mConnectedPeripherals.containsKey(address)) {
            mConnectedPeripherals.get(address).disconnect();
            mConnectedPeripherals.remove(address);
        } else {
            Log.w(TAG, "disconnect() -> no connected device known with address: " + address);
        }
    }

    public synchronized boolean isScanning() {
        return mIsScanning;
    }

    public class LocalBinder extends Binder {
        public BlePeripheralService getService() {
            return BlePeripheralService.this;
        }
    }
}