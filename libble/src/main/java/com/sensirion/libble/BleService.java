package com.sensirion.libble;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.sensirion.libble.action.ActionFailureCallback;
import com.sensirion.libble.action.ActionReadCharacteristic;
import com.sensirion.libble.action.ActionScheduler;
import com.sensirion.libble.action.ActionWriteCharacteristic;
import com.sensirion.libble.action.ActionWriteDescriptor;
import com.sensirion.libble.action.GattAction;
import com.sensirion.libble.log.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BleService extends Service implements ActionFailureCallback {
    public final static String ACTION_GATT_CONNECTED =
            "com.sensirion.libble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.sensirion.libble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.sensirion.libble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.sensirion.libble.ACTION_DATA_AVAILABLE";
    public static final String ACTION_DID_WRITE_CHARACTERISTIC =
            "com.sensirion.libble.ACTION_DID_WRITE_CHARACTERISTIC";
    public static final String ACTION_DID_FAIL =
            "com.sensirion.libble.ACTION_DID_FAIL";
    public final static String EXTRA_DATA =
            "com.sensirion.libble.EXTRA_DATA";
    public final static String EXTRA_DEVICE_ADDRESS =
            "com.sensirion.libble.EXTRA_DEVICE_ADDRESS";
    public static final String EXTRA_CHARACTERISTIC_UUID =
            "com.sensirion.libble.EXTRA_CHARACTERISTIC_UUID";
    public static final String EXTRA_DESCRIPTOR_UUID =
            "com.sensirion.libble.EXTRA_CHARACTERISTIC_UUID";

    private final static String TAG = BleService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();
    private final Map<String, BleDevice> mDevices = Collections.synchronizedMap(new HashMap<String, BleDevice>());
    private final BluetoothGattCallback mGattCallback = new BleCallback();
    private final Handler mScanHandler = new Handler();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private ActionScheduler mActionScheduler;
    private Runnable mStopScanningRunnable;

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Initializes the BleService.
     *
     * @return Return true if the initialization is successful or if it has already been initialized,
     * false otherwise. Reasons for a failed initialization can be if BLE is not supported or if the
     * permissions are not given.
     */
    public boolean initialize() {
        if (mBluetoothAdapter != null && mActionScheduler != null) {
            Log.w(TAG, "BleService already initialized");
            return true;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Unable to initialize BluetoothManager. BLE not supported");
            return false;
        }

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if (mActionScheduler == null) {
            mActionScheduler = new ActionScheduler(this, new Handler());
        }

        return true;
    }

    /**
     * Starts a BLE Scan. Discovered devices are reported via the delivered callback.
     *
     * @param callback         An instance of the BleScanCallback, used to receive scan results.
     * @param durationMs       The duration in milliseconds, how long the scan should last. This parameter
     *                         must be greater or equal to 1000 ms.
     * @param deviceNameFilter A array of device names to filter for. Only BLE devices with these
     *                         names are reported to the callback.
     * @return true if a scan was triggered and false, if it was not possible to trigger a scan or
     * if there is already an ongoing scan running.
     */
    public boolean startScan(@NonNull final BleScanCallback callback, final long durationMs,
                             final String[] deviceNameFilter) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (durationMs < 1000) {
            Log.w(TAG, "The scan duration must be longer than 1 second");
            return false;
        }

        if (mStopScanningRunnable != null) {
            Log.w(TAG, "Already a scan running");
            return false;
        }

        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.w(TAG, "Failed to scan. Bluetooth appears to be unavailable");
            return false;
        }
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        final List<ScanFilter> filters = getScanFilters(deviceNameFilter);

        // Stops scanning after a pre-defined scan period.
        mStopScanningRunnable = new Runnable() {
            @Override
            public void run() {
                stopScan(callback);
            }
        };
        mScanHandler.postDelayed(mStopScanningRunnable, durationMs);

        bluetoothLeScanner.startScan(filters, settings, callback);
        return true;
    }

    /**
     * Stops an ongoing BLE Scan for the given callback.
     *
     * @param callback An instance of the BleScanCallback, used to start the scan.
     */
    public void stopScan(@NonNull final BleScanCallback callback) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return;
        }

        if (mStopScanningRunnable == null) return;

        mScanHandler.removeCallbacks(mStopScanningRunnable);
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.stopScan(callback);
        mStopScanningRunnable = null;
        callback.onScanStopped();
    }

    /**
     * Retrieve a list of device addresses of all the connected devices.
     *
     * @return List of all device addresses of the BLE devices currently connected.
     */
    public List<String> getConnectedDevices() {
        final ArrayList<String> result = new ArrayList<>();
        synchronized (mDevices) {
            for (final BleDevice device : mDevices.values()) {
                if (device.getConnectionState() == BluetoothProfile.STATE_CONNECTED) {
                    result.add(device.getBluetoothGatt().getDevice().getAddress());
                }
            }
        }
        return result;
    }

    /**
     * Retrieves a Map of Characteristic UUID Descriptor to BluetoothGattCharacteristic supported
     * by this device.
     *
     * @param deviceAddress The device address identifying this device.
     * @param uuids         A list of descriptor UUIDs of the desired characteristics.
     * @return A Mapping with the characteristic UUIDs provided as uuids parameter and the corresponding characteristics.
     */
    @NonNull
    public Map<String, BluetoothGattCharacteristic> getCharacteristics(@NonNull final String deviceAddress,
                                                                       final List<String> uuids) {
        final HashMap<String, BluetoothGattCharacteristic> result = new HashMap<>();

        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return result;
        }

        final BleDevice bleDevice = mDevices.get(deviceAddress);
        if (bleDevice == null) {
            Log.w(TAG, "Unknown BLE Device");
            return result;
        }

        for (BluetoothGattService service : getSupportedGattServices(deviceAddress)) {
            for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                final String uuid = characteristic.getUuid().toString();
                if (uuids.contains(uuid)) {
                    result.put(uuid, characteristic);
                }
            }
        }

        return result;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param deviceAddress The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the intent action {@code ACTION_GATT_CONNECTED}
     */
    public boolean connect(@NonNull final String deviceAddress) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return false;
        }

        // Don't reuse gatt of previously connected device. Always try fresh connect.
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        final BluetoothGatt bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");

        mDevices.put(deviceAddress, new BleDevice(bluetoothGatt,
                BluetoothProfile.STATE_CONNECTING));
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the intent action {@code ACTION_GATT_DISCONNECTED}
     *
     * @param deviceAddress The device address of the destination device.
     */
    public void disconnect(@NonNull final String deviceAddress) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return;
        }

        final BleDevice bleDevice = mDevices.get(deviceAddress);
        if (bleDevice == null) {
            Log.w(TAG, "Unknown BLE Device");
            return;
        }

        bleDevice.getBluetoothGatt().disconnect();
    }

    /**
     * Convenience method to request a read on a given {@code BluetoothGattCharacteristic}. See
     * {@code readCharacteristic} for more details.
     * The read result is reported asynchronously through the intent action {@code ACTION_DATA_AVAILABLE}.
     *
     * @param deviceAddress      The device address of the destination device.
     * @param characteristicUuid The uuid of the characteristic to read from.
     */
    public void readCharacteristic(@NonNull final String deviceAddress,
                                   final String characteristicUuid) {
        final BluetoothGattCharacteristic characteristic = getCharacteristics(deviceAddress,
                Collections.singletonList(characteristicUuid)).get(characteristicUuid);
        readCharacteristic(deviceAddress, characteristic);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the intent action {@code ACTION_DATA_AVAILABLE}.
     *
     * @param deviceAddress  The device address of the destination device.
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(@NonNull final String deviceAddress,
                                   final BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }

        final BleDevice bleDevice = mDevices.get(deviceAddress);
        if (bleDevice == null) {
            Log.w(TAG, "Unknown BLE Device");
            return;
        }

        if (characteristic == null) {
            Log.w(TAG, "Invalid characteristic");
            return;
        }

        mActionScheduler.schedule(new ActionReadCharacteristic(bleDevice.getBluetoothGatt(), characteristic));
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param deviceAddress  The device address of the destination device.
     * @param characteristic The characteristic to write to.
     */
    public void writeCharacteristic(@NonNull final String deviceAddress,
                                    final BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }

        final BleDevice bleDevice = mDevices.get(deviceAddress);
        if (bleDevice == null) {
            Log.w(TAG, "Unknown BLE Device");
            return;
        }

        mActionScheduler.schedule(new ActionWriteCharacteristic(bleDevice.getBluetoothGatt(), characteristic));
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param deviceAddress  The device address of the destination device.
     * @param characteristic Characteristic to act on.
     * @param descriptor     if there is a descriptor to write, else provide null.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(@NonNull final String deviceAddress,
                                              final BluetoothGattCharacteristic characteristic,
                                              final BluetoothGattDescriptor descriptor,
                                              final boolean enabled) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }

        final BleDevice bleDevice = mDevices.get(deviceAddress);
        if (bleDevice == null) {
            Log.w(TAG, "Unknown BLE Device");
            return;
        }

        Log.d(TAG, "setCharacteristicNotification " + ((enabled) ? "TRUE" : "FALSE"));
        bleDevice.getBluetoothGatt().setCharacteristicNotification(characteristic, enabled);

        if (descriptor != null) {
            mActionScheduler.schedule(new ActionWriteDescriptor(bleDevice.getBluetoothGatt(), descriptor));
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @param deviceAddress The device address of the destination device.
     * @return A {@code List} of supported services or an empty list if device not available or
     * discovery of services has not finished yet.
     */
    @NonNull
    public List<BluetoothGattService> getSupportedGattServices(@NonNull final String deviceAddress) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return new ArrayList<>();
        }

        final BleDevice bleDevice = mDevices.get(deviceAddress);
        if (bleDevice == null) {
            Log.w(TAG, "Unknown BLE Device");
            return new ArrayList<>();
        }

        return bleDevice.getBluetoothGatt().getServices();
    }

    @Override
    public void onActionFailed(final GattAction action) {
        if (action instanceof ActionReadCharacteristic) {
            final ActionReadCharacteristic actionRead = (ActionReadCharacteristic) action;
            final BluetoothGattCharacteristic characteristic = actionRead.getCharacteristic();
            broadcastUpdate(action.getDeviceAddress(), ACTION_DID_FAIL, characteristic);
        } else if (action instanceof ActionWriteCharacteristic) {
            final ActionWriteCharacteristic actionWrite = (ActionWriteCharacteristic) action;
            final BluetoothGattCharacteristic characteristic = actionWrite.getCharacteristic();
            broadcastUpdate(action.getDeviceAddress(), ACTION_DID_FAIL, characteristic);
        } else if (action instanceof ActionWriteDescriptor) {
            final ActionWriteDescriptor actionRead = (ActionWriteDescriptor) action;
            final BluetoothGattDescriptor descriptor = actionRead.getGattDescriptor();
            broadcastUpdate(action.getDeviceAddress(), ACTION_DID_FAIL, descriptor);
        }
    }

    // Private Helpers
    @NonNull
    private List<ScanFilter> getScanFilters(final String[] deviceNameFilter) {
        final List<ScanFilter> filters = new ArrayList<>();
        if (deviceNameFilter != null) {
            for (final String deviceName : deviceNameFilter) {
                final ScanFilter scanFilter = new ScanFilter.Builder()
                        .setDeviceName(deviceName)
                        .build();
                filters.add(scanFilter);
            }
        }
        return filters;
    }

    private void broadcastUpdate(final String deviceAddress, final String action) {
        sendBroadcast(createBaseIntent(deviceAddress, action));
    }

    private void broadcastUpdate(final String deviceAddress, final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = createBaseIntent(deviceAddress, action);
        intent.putExtra(EXTRA_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String deviceAddress, final String action,
                                 final BluetoothGattDescriptor descriptor) {
        final Intent intent = createBaseIntent(deviceAddress, action);
        intent.putExtra(EXTRA_CHARACTERISTIC_UUID, descriptor.getCharacteristic().getUuid().toString());
        intent.putExtra(EXTRA_DESCRIPTOR_UUID, descriptor.getUuid().toString());
        intent.putExtra(EXTRA_DATA, descriptor.getValue());
        sendBroadcast(intent);
    }

    @NonNull
    private Intent createBaseIntent(final String deviceAddress, final String action) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
        return intent;
    }

    class BleCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final String deviceAddress = gatt.getDevice().getAddress();
            final BleDevice bleDevice = mDevices.get(deviceAddress);
            if (bleDevice == null) return;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bleDevice.setConnectionState(newState);

                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery: " +
                        bleDevice.getBluetoothGatt().discoverServices());

                broadcastUpdate(deviceAddress, ACTION_GATT_CONNECTED);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bleDevice.setConnectionState(newState);

                // After using a given device, you should make sure that BluetoothGatt.close() is called
                // such that resources are cleaned up properly.
                bleDevice.getBluetoothGatt().close();
                mDevices.remove(deviceAddress);
                mActionScheduler.clear(deviceAddress);

                Log.i(TAG, "Disconnected from GATT server.");

                broadcastUpdate(deviceAddress, ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final String deviceAddress = gatt.getDevice().getAddress();
            Log.i(TAG, "onServicesDiscovered for device %s with status %d", deviceAddress, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(deviceAddress, ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                disconnect(deviceAddress);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            final String deviceAddress = gatt.getDevice().getAddress();
            Log.i(TAG, "onCharacteristicChanged for device %s", deviceAddress);

            broadcastUpdate(deviceAddress, ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            final String deviceAddress = gatt.getDevice().getAddress();
            Log.i(TAG, "onCharacteristicRead for device %s with status %d", deviceAddress, status);
            mActionScheduler.confirm(deviceAddress);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(deviceAddress, ACTION_DATA_AVAILABLE, characteristic);
            } else {
                broadcastUpdate(deviceAddress, ACTION_DID_FAIL, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            final String deviceAddress = gatt.getDevice().getAddress();
            Log.i(TAG, "onCharacteristicWrite for device %s with status %d", deviceAddress, status);
            mActionScheduler.confirm(deviceAddress);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(deviceAddress, ACTION_DID_WRITE_CHARACTERISTIC, characteristic);
            } else {
                broadcastUpdate(deviceAddress, ACTION_DID_FAIL, characteristic);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            final String deviceAddress = gatt.getDevice().getAddress();
            Log.i(TAG, "onDescriptorRead for device %s with status %d", deviceAddress, status);
            mActionScheduler.confirm(deviceAddress);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            final String deviceAddress = gatt.getDevice().getAddress();
            Log.i(TAG, "onDescriptorWrite for device %s with status %d", deviceAddress, status);
            mActionScheduler.confirm(deviceAddress);
        }
    }
}
