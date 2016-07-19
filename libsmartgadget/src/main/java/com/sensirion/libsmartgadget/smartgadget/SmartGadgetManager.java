package com.sensirion.libsmartgadget.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.BleScanCallback;
import com.sensirion.libble.BleService;
import com.sensirion.libsmartgadget.Gadget;
import com.sensirion.libsmartgadget.GadgetManager;
import com.sensirion.libsmartgadget.GadgetManagerCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The GadgetManager is the main interface to interact with Sensirion Smart Gadgets. It provides
 * functions to initialize the communication stack and find gadgets in range. See {@link Gadget} for
 * more information on how to connect to the found gadgets. Note that only Gadget instance received
 * via {@link GadgetManagerCallback#onGadgetDiscovered(Gadget, int)} can be used to establish a
 * connection.
 */
class SmartGadgetManager extends BroadcastReceiver implements GadgetManager, BleConnector {
    private static final String TAG = SmartGadgetManager.class.getSimpleName();
    private final GadgetManagerCallback mGadgetManagerListener;
    private LibBleConnection mLibBleConnection;
    private BleService mBleService;
    private GadgetServiceFactory mGadgetServiceFactory;
    private GadgetDiscoveryListener mLocalDiscoveryListener;

    private Map<String, BleConnectorCallback> mGadgetsOfInterest;

    /**
     * {@inheritDoc}
     */
    public SmartGadgetManager(@NonNull final GadgetManagerCallback callback) {
        mGadgetManagerListener = callback;
        mGadgetsOfInterest = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(@NonNull final Context applicationContext) {
        if (mLocalDiscoveryListener != null) {
            Log.e(TAG, "GadgetManager already initialized");
            return;
        }

        applicationContext.registerReceiver(this, createLibBleIntentFilter());
        mGadgetServiceFactory = new GadgetServiceFactory(this);
        mLocalDiscoveryListener = new GadgetDiscoveryListener();
        mLibBleConnection = new LibBleConnection();

        // Launch libble
        final Intent bindLibBle = new Intent(applicationContext, BleService.class);
        if (!applicationContext.bindService(bindLibBle, mLibBleConnection, Context.BIND_AUTO_CREATE)) {
            release(applicationContext);
            mGadgetManagerListener.onGadgetManagerInitializationFailed();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release(@NonNull final Context applicationContext) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }

        applicationContext.unbindService(mLibBleConnection);
        applicationContext.unregisterReceiver(this);
        mGadgetServiceFactory = null;
        mLocalDiscoveryListener = null;
        mBleService = null;
        // don't erase the callback to notify the failed state if necessary.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        return mBleService != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startGadgetDiscovery(final long durationMs, final String[] advertisedNameFilter) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return false;
        }
        return mBleService.startScan(mLocalDiscoveryListener, durationMs, advertisedNameFilter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopGadgetDiscovery() {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }

        mBleService.stopScan(mLocalDiscoveryListener);
    }

    /*
        Implementation of {@link BleConnector}
     */
    @Override
    public boolean connect(final SmartGadget gadget) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return false;
        }
        mGadgetsOfInterest.put(gadget.getAddress(), gadget);
        return mBleService.connect(gadget.getAddress());
    }

    @Override
    public void disconnect(final SmartGadget gadget) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }
        mBleService.disconnect(gadget.getAddress());
    }

    @NonNull
    @Override
    public List<BluetoothGattService> getServices(final SmartGadget gadget) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return new ArrayList<>();
        }
        return mBleService.getSupportedGattServices(gadget.getAddress());
    }

    @NonNull
    @Override
    public Map<String, BluetoothGattCharacteristic> getCharacteristics(@NonNull String deviceAddress, List<String> uuids) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return new HashMap<>();
        }
        return mBleService.getCharacteristics(deviceAddress, uuids);
    }

    @Override
    public void readCharacteristic(@NonNull String deviceAddress, String characteristicUuid) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }
        mBleService.readCharacteristic(deviceAddress, characteristicUuid);
    }

    @Override
    public void writeCharacteristic(@NonNull String deviceAddress, BluetoothGattCharacteristic characteristic) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }
        mBleService.writeCharacteristic(deviceAddress, characteristic);
    }

    @Override
    public void setCharacteristicNotification(@NonNull String deviceAddress, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, boolean enabled) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }
        mBleService.setCharacteristicNotification(deviceAddress, characteristic, descriptor, enabled);
    }

    /*
        Implementation of the Broadcast receiver logic to receive libble callbacks
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        final String deviceAddress = intent.getStringExtra(BleService.EXTRA_DEVICE_ADDRESS);
        if (action == null || deviceAddress == null) {
            Log.e(TAG, "Invalid intent received from libble");
            return;
        }
        final BleConnectorCallback gadget = mGadgetsOfInterest.get(deviceAddress);
        if (gadget == null) {
            Log.e(TAG, "Intent received for unknown gadget");
            return;
        }

        final String characteristicUuid = intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID);
        switch (action) {
            case BleService.ACTION_GATT_CONNECTED:
                // Wait for service discovery
                break;
            case BleService.ACTION_GATT_SERVICES_DISCOVERED:
                gadget.onConnectionStateChanged(true);
                break;
            case BleService.ACTION_GATT_DISCONNECTED:
                mGadgetsOfInterest.remove(deviceAddress);
                gadget.onConnectionStateChanged(false);
                break;
            case BleService.ACTION_DATA_AVAILABLE:
                final byte[] rawData = intent.getByteArrayExtra(BleService.EXTRA_DATA);
                Log.i("TEST", "ACTION_DATA_AVAILABLE for " + deviceAddress + " and uuid " + characteristicUuid);
                gadget.onDataReceived(characteristicUuid, rawData);
                break;
            case BleService.ACTION_DID_WRITE_CHARACTERISTIC:
                Log.i("TEST", "ACTION_DID_WRITE_CHARACTERISTIC for " + deviceAddress + " and uuid " + characteristicUuid);
                gadget.onDataWritten(characteristicUuid);
                break;
            case BleService.ACTION_DID_FAIL:
                final boolean wasWriting = intent.getBooleanExtra(BleService.EXTRA_IS_WRITE_FAILURE, false);
                final byte[] data = intent.getByteArrayExtra(BleService.EXTRA_DATA);
                Log.i("TEST", "ACTION_DID_FAIL " + ((wasWriting) ? "writing" : "reading") + " for " + deviceAddress + " and uuid " + characteristicUuid);
                gadget.onFail(characteristicUuid, data, wasWriting);
                break;
        }
    }

    private static IntentFilter createLibBleIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleService.ACTION_DID_WRITE_CHARACTERISTIC);
        intentFilter.addAction(BleService.ACTION_DID_FAIL);
        return intentFilter;
    }

    /*
        LibBle Service Connection
    */
    private class LibBleConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBleService = ((BleService.LocalBinder) iBinder).getService();
            if (!mBleService.initialize()) {
                Log.e(TAG, "Unable to initialize libble and the Bluetooth stack - will terminate");
                release(mBleService);
                mGadgetManagerListener.onGadgetManagerInitializationFailed();
                return;
            }

            mGadgetManagerListener.onGadgetManagerInitialized();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "libble disconnected");
            mBleService = null;
        }
    }

    /*
        Scan Callback Proxy
     */
    private class GadgetDiscoveryListener extends BleScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            notifyScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                notifyScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scanning for BLE devices failed with error code " + errorCode);
            mGadgetManagerListener.onGadgetDiscoveryFailed();
        }

        @Override
        public void onScanStopped() {
            mGadgetManagerListener.onGadgetDiscoveryFinished();
        }

        void notifyScanResult(final ScanResult result) {
            final SmartGadget smartGadget = new SmartGadget(SmartGadgetManager.this,
                    mGadgetServiceFactory, result.getDevice().getName(),
                    result.getDevice().getAddress());
            mGadgetManagerListener.onGadgetDiscovered(smartGadget, result.getRssi());
        }
    }


}
