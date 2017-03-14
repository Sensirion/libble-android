package com.sensirion.libsmartgadget.smartgadget;

import android.support.annotation.NonNull;

import com.sensirion.libsmartgadget.GadgetValue;

import java.util.Date;

public class BatteryService extends SmartGadgetNotificationService {
    public static final String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";

    private static final String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";

    public static final String UNIT = "%";

    /**
     * {@inheritDoc}
     */
    public BatteryService(@NonNull final ServiceListener serviceListener,
                          @NonNull final BleConnector bleConnector,
                          @NonNull final String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID, UNIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnectionStateChanged(final boolean connected) {
        if (connected) {
            requestValueUpdate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            final int batteryLevel = (int) rawData[0];
            mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), batteryLevel, UNIT)};
            mServiceListener.onGadgetValuesReceived(this, mLastValues);
        }
    }
}
