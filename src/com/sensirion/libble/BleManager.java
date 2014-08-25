package com.sensirion.libble;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class BleManager {

    private static final String TAG = BleManager.class.getSimpleName();
    private static BleManager mInstance;
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

    private BleManager() {
    }

    public synchronized static BleManager getInstance() {
        if (mInstance == null) {
            mInstance = new BleManager();
        }
        return mInstance;
    }

    /**
     * This method have should be called just after the first getInstance() call
     * in the application that wants to implement the library.
     *
     * @param context application context of the implementing application.
     */
    public void init(Context context) {
        if (context instanceof Application) {
            Log.i(TAG, "init() -> binding to BlePeripheralService");
            Intent intent = new Intent(context, BlePeripheralService.class);
            if (context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
                Log.i(TAG, "init() -> successfully bound to BlePeripheralService!");
            } else {
                throw new IllegalStateException("init() -> unable to bind to BlePeripheralService!");
            }
        } else {
            throw new IllegalArgumentException("init() -> BleManager has to be initialized with an application context");
        }
    }

    /**
     * This method should be called at the end of the execution of the implementing application.
     *
     * @param context application context of the implementing application.
     */
    public void release(Context context) {
        try {
            Log.w(TAG, "onDestroy() -> isFinishing() -> unbinding mServiceConnection");
            context.unbindService(mServiceConnection);
        } catch (Exception e) {
            Log.e(TAG, "The service produced an exception when trying to unbind from it.", e);
        }
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
     * Ask if the peripheral service is scanning.
     *
     * @return true if it's scanning - false otherwise.
     */
    public boolean isScanning() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "isScanning() -> not connected to BlePeripheralService");
            return false;
        }
        return mBlePeripheralService.isScanning();
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

    /**
     * Register a listener in all connected peripherals.
     *
     * @param listener pretending to listen for notifications in all peripherals.
     */
    public void registerPeripheralListenerToAllConnected(NotificationListener listener) {
        registerPeripheralListener(null, listener);
    }


    /**
     * Registers a listener in a connected peripheral.
     *
     * @param address  address of the peripheral we want to listen to,
     *                 null if we want to register a listener to all connected devices.
     * @param listener pretending to listen for notifications of a peripheral.
     *
     */
    public void registerPeripheralListener(String address, NotificationListener listener) {
        final Iterator<? extends BleDevice> iterator = getConnectedBleDevices().iterator();
        while (iterator.hasNext()) {
            BleDevice device = iterator.next();
            if (address == null || device.getAddress().equals(address)) {
                ((Peripheral) device).registerPeripheralListener(listener);
                if (address == null) {
                    continue;
                }
                return;
            }
        }
    }

    /**
     * Unregister a listener from all connected peripherals.
     *
     * @param listener that does not want to get notifications any more.
     */
    public void unregisterPeripheralListenerFromAllConnected(NotificationListener listener) {
        unregisterPeripheralListener(null, listener);
    }

    /**
     * Unregister a listener from a connected peripheral.
     *
     * @param address  of the peripheral you don't want to get notifications from anymore.
     * @param listener that wants to unregister from the notifications of a peripheral.
     */
    public void unregisterPeripheralListener(String address, NotificationListener listener) {
        final Iterator<? extends BleDevice> iterator = getConnectedBleDevices().iterator();
        while (iterator.hasNext()) {
            BleDevice device = iterator.next();
            if (address == null || device.getAddress().equals(address)) {
                ((Peripheral) device).unregisterPeripheralListener(listener);
                if (address == null) {
                    continue;
                }
                break;
            }
        }
    }

    /**
     * Checks if bluetooth connection is enabled on the device.
     */
    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param context of the requesting activity.
     */
    public void requestEnableBluetooth(Context context) {
        if (isBluetoothEnabled()) {
            Log.d(TAG, "Bluetooth is enabled");
        } else {
            Log.d(TAG, "Bluetooth is disabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
        }
    }
}