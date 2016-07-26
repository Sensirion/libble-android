package com.sensirion.libsmartgadget.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.utils.LittleEndianExtractor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Date;

public class SensorTagTemperatureAndHumidityService extends SmartGadgetNotificationService {
    public static final String SERVICE_UUID = "f000aa20-0451-4000-b000-000000000000";
    private static final String RHT_CHARACTERISTIC_UUID = "f000aa21-0451-4000-b000-000000000000";
    private static final String CONFIG_CHARACTERISTIC_UUID = "f000aa22-0451-4000-b000-000000000000";
    public static final String UNIT_T = "°C";
    public static final String UNIT_RH = "%";

    /**
     * {@inheritDoc}
     */
    public SensorTagTemperatureAndHumidityService(@NonNull ServiceListener serviceListener,
                                                  @NonNull BleConnector bleConnector,
                                                  @NonNull String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, SERVICE_UUID, RHT_CHARACTERISTIC_UUID, "n/a");
    }

    @Override
    public void subscribe() {
        super.subscribe();
        final BluetoothGattCharacteristic confCharacteristic =
        mBleConnector.getCharacteristics(mDeviceAddress,
                Collections.singletonList(CONFIG_CHARACTERISTIC_UUID))
                .get(CONFIG_CHARACTERISTIC_UUID);
        if (confCharacteristic == null) return;

        confCharacteristic.setValue(new byte[] {1});
        mBleConnector.writeCharacteristic(mDeviceAddress, confCharacteristic);
    }

    @Override
    protected void handleLiveValue(final byte[] rawData) {
        if (rawData.length != 4) {
            return;
        }
        final Date timestamp = new Date();
        final short[] humidityAndTemperature = new short[2];
        ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);
        if (humidityAndTemperature[0] == 0 && humidityAndTemperature[1] == 0)
            return;
        float temp = humidityAndTemperature[0] - (humidityAndTemperature[0] % 4);
         // humi is an unsigned short but java doesn't support unsigned, so we box it in a (signed) int
        int shumi = ((int) humidityAndTemperature[1]) & 0xffff;
        float humi = shumi - (shumi % 4);
        temp = 175.72f * temp / 65536.0f - 46.85f;
        humi = 125.00f * humi / 65535.0f - 6.00f;

        mLastValues = new GadgetValue[]{
                new SmartGadgetValue(timestamp, temp, UNIT_T),
                new SmartGadgetValue(timestamp, humi, UNIT_RH)
        };
        mServiceListener.onGadgetValuesReceived(this, mLastValues);
    }
}
