package com.sensirion.libble;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * This is the Activity all apps using the library should extend
 * instead of extending standard Activity
 */
public abstract class BleActivity extends Activity {

    private static final String TAG = BleActivity.class.getSimpleName();

    private boolean mShouldStartScanning;
    private BlePeripheralService mBlePeripheralService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected() -> connected to BlePeripheralService");
            mBlePeripheralService = ((BlePeripheralService.LocalBinder) service).getService();
            Toast.makeText(getApplicationContext(), "connected to BlePeripheralService", Toast.LENGTH_SHORT).show();

            if (mShouldStartScanning) {
                Log.i(TAG, "onServiceConnected() -> re-trigger startScanning()");
                startScanning();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "onServiceDisconnected() -> disconnected from BlePeripheralService");
            mBlePeripheralService = null;
            Toast.makeText(getApplicationContext(), "disconnected from BlePeripheralService", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate() -> binding to BlePeripheralService");
        Intent intent = new Intent(this, BlePeripheralService.class);
        if (bindService(intent, mServiceConnection, BIND_AUTO_CREATE)) {
            Log.i(TAG, "onCreate() -> successfully bound to BlePeripheralService!");
        } else {
            throw new IllegalStateException("onCreate() -> unable to bind to BlePeripheralService!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() -> stopScanning()");
        stopScanning();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if (isFinishing()) {
            Log.w(TAG, "onDestroy() -> isFinishing() -> unbinding mServiceConnection");
            unbindService(mServiceConnection);
        }
        super.onDestroy();
    }

    public boolean startScanning() {
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
        return mBlePeripheralService.startLeScan();
    }

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

    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        if(mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals();
    }

    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        if(mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getConnectedPeripherals();
    }

    public BleDevice getConnectedDevice(String address) {
        if(mBlePeripheralService == null) {
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
            Log.d(TAG,"connectPeripheral() -> stopScanning()");
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
     * Method invoked as soon as a connected peripheral is selected
     *
     * @param address MAC-Address of the selected peripheral
     */
    public abstract void onConnectedPeripheralSelected(String address);

    /**
     * Method invoked as soon as a discovered peripheral is selected
     *
     * @param address MAC-Address of the selected peripheral
     */
    public abstract void onDiscoveredPeripheralSelected(String address);

}
