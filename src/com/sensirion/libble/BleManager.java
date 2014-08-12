package com.sensirion.libble;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleManager {

    private static final String TAG = BleManager.class.getSimpleName();
    private boolean mShouldStartScanning;
    private BlePeripheralService mBlePeripheralService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected() -> connected to BlePeripheralService");
            mBlePeripheralService = ((BlePeripheralService.LocalBinder) service).getService();

            if (mShouldStartScanning) {
                Log.i(TAG, "onServiceConnected() -> re-trigger startScanning()");
                startScanning();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "onServiceDisconnected() -> disconnected from BlePeripheralService");
            mBlePeripheralService = null;
        }
    };

    private static BleManager mInstance;
    private BleManager (){}
    public synchronized static BleManager getInstance(){
        if (mInstance == null){
            mInstance = new BleManager();
        }
        return mInstance;
    }

    /**
     * This method have should be called at the beginning of
     * the application that wants to implement the library.
     *
     * @param context application context of the implementing application.
     */
    public void init(Context context) {
        Log.i(TAG, "init() -> binding to BlePeripheralService");
        Intent intent = new Intent(context, BlePeripheralService.class);
        if (context.bindService(intent, mServiceConnection, context.BIND_AUTO_CREATE)) {
            Log.i(TAG, "init() -> successfully bound to BlePeripheralService!");
        } else {
            throw new IllegalStateException("init() -> unable to bind to BlePeripheralService!");
        }
    }

    public void release(Context context) {
        Log.w(TAG, "onDestroy() -> isFinishing() -> unbinding mServiceConnection");
        context.unbindService(mServiceConnection);
    }

    /**
     * Starts to scan for all bluetooth devices in range.
     *
     * @return true if it's scanning, false otherwise.
     */
    public boolean startScanning() {
        return startScanning(null);
    }

    /**
     * Start scanning devices in range using provided UUIDs.
     *
     * @param deviceUUIDs deviceUUIDs that we want want to use,
     *                    null if all devices have to be retrieved.
     * @return true if it's scanning, false otherwise.
     */
    public boolean startScanning(UUID[] deviceUUIDs) {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "startScanning() -> not yet connected to BlePeripheralService; try to re-trigger scanning when connected.");
            mShouldStartScanning = true;
            return false;
        }
        if (mBlePeripheralService.isScanning()) {
            Log.w(TAG, "startScanning() -> already scanning; ignoring this request.");
            return true;
        }
        if (deviceUUIDs == null) {
            Log.d(TAG, "startScanning() -> mBlePeripheralService.startLeScan()");
            return mBlePeripheralService.startLeScan();
        }
        Log.d(TAG, "startScanning() -> mBlePeripheralService.startLeScan(UUIDs)");
        return mBlePeripheralService.startLeScan(deviceUUIDs);
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
     * Get all discovered {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        if (mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with only one name in particular.
     *
     * @param validDeviceName device name needed by the application.
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(String validDeviceName) {
        if (mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals(validDeviceName);
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with valid names for the application.
     *
     * @param validDeviceNames List of devices names.
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(List<String> validDeviceNames) {
        if (mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals(validDeviceNames);
    }

    /**
     * Get all connected {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        if (mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getConnectedPeripherals();
    }

    /**
     * Returns the {@link com.sensirion.libble.BleDevice} belonging to the given address
     *
     * @param address MAC-Address of the desired {@link com.sensirion.libble.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.BleDevice}
     * or NULL if the device is not connected
     */
    public BleDevice getConnectedDevice(String address) {
        if (mBlePeripheralService == null) {
            return null;
        }

        return mBlePeripheralService.getConnectedDevice(address);
    }

    /**
     * Tries to establish a connection toa selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be connected
     */
    public boolean connectPeripheral(String address) {
        if (mBlePeripheralService == null) {
            Log.e(TAG, mBlePeripheralService.getClass().getSimpleName() + " is null -> could not connect peripheral!");
        } else {
            Log.d(TAG, "connectPeripheral() -> stopScanning()");
            stopScanning();
            return mBlePeripheralService.connect(address);
        }
        return false;
    }

    /**
     * Tries to disconnect a selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be disconnected
     */
    public void disconnectPeripheral(String address) {
        if (mBlePeripheralService != null) {
            mBlePeripheralService.disconnect(address);
        } else {
            Log.e(TAG, "Service not ready!");
        }
    }
}