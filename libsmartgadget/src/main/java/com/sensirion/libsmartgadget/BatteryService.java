package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class BatteryService implements GadgetService, BleConnectorCallback {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestValueUpdate() {
        mBleConnector.readCharacteristic(mDeviceAddress, BATTERY_LEVEL_CHARACTERISTIC_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GadgetValue[] getLastValues() {
        return mLastValues;
    }

    @Override
    public void onConnectionStateChanged(final boolean connected) {
        if (connected) {
            requestValueUpdate();
        }
    }

    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            final int batteryLevel = (int) rawData[0];
            mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), batteryLevel, UNIT)};
            mServiceListener.onGadgetValuesReceived(this, mLastValues);
        }
    }

    @Override
    public void onDataWritten(final String characteristicUuid) {
        // ignore ... no characteristic written in this service
    }

    // TODO: Think about limiting the number of retries! After retry limit we could request the client
    // TODO:    To call requestValueUpdate again
    @Override
    public void onFail(final String characteristicUuid, final byte[] data,
                       final boolean isWriteFailure) {
        if (isUuidSupported(characteristicUuid) && !isWriteFailure) {
            mBleConnector.readCharacteristic(mDeviceAddress, characteristicUuid);  // Try again
        }
    }

    private boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }
}
