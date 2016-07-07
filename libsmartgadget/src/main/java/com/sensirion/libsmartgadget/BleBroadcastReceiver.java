package com.sensirion.libsmartgadget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.sensirion.libble.BleService;

import java.util.HashMap;
import java.util.Map;

/**
 * Broadcast receiver handling all asynchronous callbacks from libble.
 */
class BleBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = BleBroadcastReceiver.class.getSimpleName();
    private Map<String, SmartGadget> mConnectedGadgets;

    public BleBroadcastReceiver() {
        mConnectedGadgets = new HashMap<>();
    }

    // TODO rename method
    public void delegateMessages(final SmartGadget gadget) {
        mConnectedGadgets.put(gadget.getAddress(), gadget);
    }

    public void register(final Context applicationContext) {
        applicationContext.registerReceiver(this, BleBroadcastReceiver.createLibBleIntentFilter());
    }

    public void unregister(final Context applicationContext) {
        applicationContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        final String deviceAddress = intent.getStringExtra(BleService.EXTRA_DEVICE_ADDRESS);
        if (action == null || deviceAddress == null) {
            Log.e(TAG, "Invalid intent received from libble");
            return;
        }
        final SmartGadget gadget = mConnectedGadgets.get(deviceAddress);
        if (gadget == null) {
            Log.e(TAG, "Intent received for unknown gadget");
            return;
        }

        if (BleService.ACTION_GATT_CONNECTED.equals(action)) {
            // Wait for service discovery

        } else if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            gadget.onConnectionStateChanged(true);

        } else if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
            gadget.onConnectionStateChanged(false);
            mConnectedGadgets.remove(gadget.getAddress());

        } else if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
            final byte[] rawData = intent.getByteArrayExtra(BleService.EXTRA_DATA);
            final String characteristicUuid = intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID);
        } else if (BleService.ACTION_DID_WRITE_CHARACTERISTIC.equals(action)) {
            final String characteristicUuid = intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID);
        } else if (BleService.ACTION_DID_FAIL.equals(action)) {
            final String characteristicUuid = intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID);
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
}
