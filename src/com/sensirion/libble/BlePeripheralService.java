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

import junit.framework.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class offers the main functionality of the library to the user (app)
 */
public class BlePeripheralService extends Service implements BluetoothAdapter.LeScanCallback, BlePeripheralHandler {

    private static final String TAG = BlePeripheralService.class.getSimpleName();
    private static final String PREFIX = BlePeripheralService.class.getName();


    public class LocalBinder extends Binder {
        public BlePeripheralService getService() {
            return BlePeripheralService.this;
        }
    }

    public static final String ACTION_PERIPHERAL_DISCOVERY = PREFIX + "/ACTION_PERIPHERAL_DISCOVERY";
    public static final String ACTION_PERIPHERAL_CONNECTION_CHANGED = PREFIX + "/ACTION_PERIPHERAL_CONNECTION_CHANGED";
    public static final String ACTION_SCANNING_STARTED = PREFIX + "/ACTION_SCANNING_STARTED";
    public static final String ACTION_SCANNING_STOPPED = PREFIX + "/ACTION_SCANNING_STOPPED";

    private static final long DEFAULT_SCAN_DURATION_MS = 10 * 1000;

    private Timer mScanTimer;
    private boolean mIsScanning;
    private BluetoothAdapter mBluetoothAdapter;

    private Map<String, Peripheral> mDiscoveredPeripherals = Collections.synchronizedMap(new HashMap<String, Peripheral>());
    private Map<String, Peripheral> mConnectedPeripherals = Collections.synchronizedMap(new HashMap<String, Peripheral>());

    private final IBinder mBinder = new LocalBinder();

    @Override
    public synchronized void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        final String address = device.getAddress();

        if (mDiscoveredPeripherals.containsKey(address)) {
            Log.d(TAG, "onLeScan() -> updating rssi for known device: " + address);
            mDiscoveredPeripherals.get(address).setReceivedSignalStrengthIndication(rssi);
        } else {
            mDiscoveredPeripherals.put(address, new Peripheral(this, device, rssi));
        }

        onPeripheralCollectionChanged(ACTION_PERIPHERAL_DISCOVERY);
    }

    private void onPeripheralCollectionChanged(String action) {
        Log.i(TAG, "onPeripheralCollectionChanged() for action: " + action);
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public void onPeripheralConnectionStateChanged(Peripheral peripheral) {
        final String address = peripheral.getAddress();

        if (peripheral.isConnected()) {
            Assert.assertTrue(mDiscoveredPeripherals.containsKey(address));
            Assert.assertFalse(mConnectedPeripherals.containsKey(address));
            mDiscoveredPeripherals.remove(address);
            mConnectedPeripherals.put(address, peripheral);
        } else {
            Assert.assertTrue(mConnectedPeripherals.containsKey(address));
            mConnectedPeripherals.remove(address);
        }

        onPeripheralCollectionChanged(ACTION_PERIPHERAL_CONNECTION_CHANGED);
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
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                stopSelf();
            } else {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
        } else {
            Log.e(TAG, "Bluetooth Le not supported in this API version - stopping " + TAG);
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
    private synchronized void closeAll() {
        Log.i(TAG, "closeAll() -> closing all connections.");

        //After using a given device, you should make sure that BluetoothGatt.close()
        // is called such that resources are cleaned up properly.
        for (Peripheral p : mConnectedPeripherals.values()) {
            p.close();
        }
        mConnectedPeripherals.clear();
    }

    /**
     * Requests scanning process for BLE devices in range. If the connection to the {@link com.sensirion.libble.BlePeripheralService}
     * has not been established yet, startScanning() will be re-triggered as soon as the connection is there.
     *
     * @return true if scan has been started. False otherwise.
     */
    public synchronized boolean startLeScan() {
        if (mIsScanning) {
            Log.w(TAG, "startLeScan() -> scan already in progress");
            return mIsScanning;
        }
        Log.d(TAG, "startLeScan() -> clear discovered peripherals and add new scan results");
        checkBt();
        mDiscoveredPeripherals.clear();

        if (mBluetoothAdapter.startLeScan(this)) {
            mIsScanning = true;
            onStartLeScan();
        } else {
            throw new IllegalStateException("onStartLeScan() -> could not startLeScan on BluetoothAdapter!");
        }

        return mIsScanning;
    }

    private void checkBt() {
        if (isBluetoothAvailable()) {
            return;
        }
        throw new IllegalStateException("Bluetooth is not available!");
    }

    private void onStartLeScan() {
        Log.i(TAG, "onStartLeScan()");

        mScanTimer = new Timer();
        //TODO: scan for a specified UUID(s) only
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
     * @return True if bluetooth is available
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
            checkBt();
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

        Intent intent = new Intent(ACTION_SCANNING_STOPPED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    public synchronized Iterable<? extends BleDevice> getDiscoveredPeripherals() {
        return new HashSet<BleDevice>(mDiscoveredPeripherals.values());
    }

    /**
     * Get all connected {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    public synchronized Iterable<? extends BleDevice> getConnectedPeripherals() {
        return new HashSet<BleDevice>(mConnectedPeripherals.values());
    }

    /**
     * Returns the {@link com.sensirion.libble.BleDevice} belonging to the given address
     *
     * @param address MAC-Address of the desired {@link com.sensirion.libble.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.BleDevice}
     */
    public BleDevice getConnectedDevice(String address) {
        if (mConnectedPeripherals.containsKey(address)) {
            return mConnectedPeripherals.get(address);
        }
        throw new IllegalArgumentException("No connected peripheral found with address: " + address);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public synchronized boolean connect(String address) {
        checkBt();
        if (address == null || address.equals("")) {
            Log.e(TAG, "connect() -> unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mConnectedPeripherals.containsKey(address)) {
            Log.d(TAG, "connect() -> trying to use an existing BluetoothGatt for connection.");
            return mConnectedPeripherals.get(address).reconnect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "connect() -> device not found. Unable to connect.");
            return false;
        }

        Log.d(TAG, "connect() -> trying to create a new connection.");
        mDiscoveredPeripherals.get(address).connect(getApplicationContext());
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public synchronized void disconnect(String address) {
        checkBt();
        if (address == null || address.equals("")) {
            Log.w(TAG, "disconnect() -> unspecified address.");
            return;
        }

        if (mConnectedPeripherals.containsKey(address)) {
            mConnectedPeripherals.get(address).disconnect();
        } else {
            Log.w(TAG, "disconnect() -> no connected device known with address: " + address);
        }

    }

    public synchronized boolean isScanning() {
        return mIsScanning;
    }

}
