package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class SHTC1TemperatureAndHumidityService extends SmartGadgetNotificationService {
    public static final String SERVICE_UUID = "0000aa20-0000-1000-8000-00805f9b34fb";
    private static final String RHT_CHARACTERISTIC_UUID = "0000aa21-0000-1000-8000-00805f9b34fb";
    public static final String UNIT_T = "°C";
    public static final String UNIT_RH = "%";

    /**
     * {@inheritDoc}
     */
    public SHTC1TemperatureAndHumidityService(@NonNull ServiceListener serviceListener,
                                              @NonNull BleConnector bleConnector,
                                              @NonNull String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, SERVICE_UUID, RHT_CHARACTERISTIC_UUID, "n/a");
    }

    @Override
    protected void handleLiveValue(final byte[] rawData) {
        final short[] humidityAndTemperature = new short[2];
        ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

        final float temperature = ((float) humidityAndTemperature[0]) / 100f;
        final float humidity = ((float) humidityAndTemperature[1]) / 100f;
        final Date timestamp = new Date();
        mLastValues = new GadgetValue[]{
                new SmartGadgetValue(timestamp, temperature, UNIT_T),
                new SmartGadgetValue(timestamp, humidity, UNIT_RH)
        };
        mServiceListener.onGadgetValuesReceived(this, mLastValues);
    }
}
