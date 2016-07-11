package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class BatteryService implements GadgetService, BleConnectorCallback {
    private static final String TAG = BatteryService.class.getSimpleName();

    public static final String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";

    private static final String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";

    public static final String UNIT = "%";

    private final ServiceListener mServiceListener;
    private final BleConnector mBleConnector;
    private final String mDeviceAddress;

    private final Set<String> mSupportedUuids;
    private GadgetValue[] mLastValues;

    public BatteryService(@NonNull final ServiceListener serviceListener,
                          @NonNull final BleConnector bleConnector,
                          @NonNull final String deviceAddress) {
        mDeviceAddress = deviceAddress;
        mBleConnector = bleConnector;
        mServiceListener = serviceListener;
        mLastValues = new GadgetValue[0];

        mSupportedUuids = new HashSet<>();
        mSupportedUuids.add(SERVICE_UUID);
        mSupportedUuids.add(BATTERY_LEVEL_CHARACTERISTIC_UUID);
    }

    @Override
    public GadgetValue[] getLastValues() {
        return mLastValues;
    }

    @Override
    public void onConnectionStateChanged(final boolean connected) {
        Log.d(TAG, "Connection State changed to " + connected);
        mBleConnector.readCharacteristic(mDeviceAddress, BATTERY_LEVEL_CHARACTERISTIC_UUID);
    }

    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            final int batteryLevel = (int) rawData[0];
            Log.d(TAG, "Received battery level: " + batteryLevel);
            mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), batteryLevel, UNIT)};
            mServiceListener.onGadgetValuesReceived(this, mLastValues);
        }
    }

    @Override
    public void onDataWritten(final String characteristicUuid) {
        // ignore ... no characteristic written in this service
    }

    private boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }
}
