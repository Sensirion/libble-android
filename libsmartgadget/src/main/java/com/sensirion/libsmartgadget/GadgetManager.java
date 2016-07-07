package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.BleScanCallback;
import com.sensirion.libble.BleService;

import java.util.ArrayList;
import java.util.List;

/**
 * The GadgetManager is the main interface to interact with Sensirion Smart Gadgets. It provides
 * functions to initialize the communication stack and find gadgets in range. See {@link Gadget} for
 * more information on how to connect to the found gadgets. Note that only Gadget instance received
 * via {@link GadgetManagerCallback#onGadgetDiscovered(Gadget, int)} can be used to establish a
 * connection.
 */
public class GadgetManager implements BleConnector {
    private static final String TAG = GadgetManager.class.getSimpleName();
    private LibBleConnection mLibBleConnection;
    private GadgetScanCallback mScanCallback;
    private BleService mBleService;

    private GadgetManagerCallback mCallback;
    private BleBroadcastReceiver mBleBroadCastReceiver;

    public GadgetManager() {
        // TODO impl.
    }

    /**
     * Initialize the communication stack and register a {@link GadgetManagerCallback}.
     * You must call this method at least once to initialize the library. You will get notified
     * asynchronously as soon as the library has finished initializing via
     * {@link GadgetManagerCallback#onGadgetManagerInitialized()}.
     *
     * @param applicationContext the application context instance.
     * @param callback           the callback instance to be registered for state change notifications.
     */
    public void initialize(@NonNull final Context applicationContext,
                           @NonNull final GadgetManagerCallback callback) {
        if (mBleBroadCastReceiver != null) {
            Log.e(TAG, "GadgetManager already initialized");
            return;
        }

        mCallback = callback;

        mBleBroadCastReceiver = new BleBroadcastReceiver();
        mBleBroadCastReceiver.register(applicationContext);

        mScanCallback = new GadgetScanCallback();
        mLibBleConnection = new LibBleConnection();

        // Launch libble
        final Intent bindLibBle = new Intent(applicationContext, BleService.class);
        applicationContext.bindService(bindLibBle, mLibBleConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Call this method if you don't plan to use the library anymore. This makes sure all resources
     * of the library are properly freed.
     *
     * @param applicationContext the application context instance.
     */
    public void release(@NonNull final Context applicationContext) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }

        applicationContext.unbindService(mLibBleConnection);
        mBleService = null;
        mBleBroadCastReceiver.unregister(applicationContext);
        mBleBroadCastReceiver = null;
        mCallback = null;
    }

    /**
     * Check if the library is ready and was successfully initialized.
     *
     * @return true if the library is ready to be used.
     */
    public boolean isReady() {
        return mBleService != null;
    }

    /**
     * Starts a scan for Sensirion Smart Gadgets.
     *
     * @param durationMs The duration how long the library should scan for. Make sure not to scan
     *                   for too long to prevent a large battery drain.
     * @return true if the scan was successfully initiated.
     */
    public boolean startGadgetDiscovery(final long durationMs) { // TODO: ADD FILTERING CAPABILITIES
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return false;
        }

        return mBleService.startScan(mScanCallback, durationMs, null);
    }

    /**
     * Stops an ongoing scan for Smart Gadgets. Nothing happens if there is no scan running.
     */
    public void stopGadgetDiscovery() { // TODO: ADD FILTERING CAPABILITIES
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return;
        }

        mBleService.stopScan(mScanCallback);
    }

    /*
        Implementation of BleDelegate
     */
    @Override
    public boolean connect(final SmartGadget gadget) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return false;
        }
        mBleBroadCastReceiver.delegateMessages(gadget);
        return mBleService.connect(gadget.getAddress());
    }

    @Override
    public boolean disconnect(final SmartGadget gadget) {
        if (!isReady()) {
            Log.w(TAG, "GadgetManager not initialized");
            return false;
        }
        mBleService.disconnect(gadget.getAddress());
        return true; // TODO: think about removing the boolean return value for this method
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
                mCallback.onGadgetManagerInitializationFailed();
                return;
            }

            mCallback.onGadgetManagerInitialized();
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
    private class GadgetScanCallback extends BleScanCallback {
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
            mCallback.onGadgetSearchFailed();
        }

        @Override
        public void onScanStopped() {
            mCallback.onGadgetSearchFinished();
        }

        void notifyScanResult(final ScanResult result) {
            final SmartGadget smartGadget = new SmartGadget(GadgetManager.this,
                    result.getDevice().getName(), result.getDevice().getAddress());
            mCallback.onGadgetDiscovered(smartGadget, result.getRssi());
        }
    }


}
