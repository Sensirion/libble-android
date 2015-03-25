package com.sensirion.libble.services.sensirion.shtc1;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.AbstractRHTService;
import com.sensirion.libble.utils.RHTDataPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SHTC1RHTService extends AbstractRHTService {

    public static final String SERVICE_UUID = "0000aa20-0000-1000-8000-00805f9b34fb";
    private static final String RHT_CHARACTERISTIC_UUID = "0000aa21-0000-1000-8000-00805f9b34fb";
    private final BluetoothGattCharacteristic mHumidityTemperatureCharacteristic;
    private RHTDataPoint mLastDatapoint;

    public SHTC1RHTService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService);
        mHumidityTemperatureCharacteristic = getCharacteristic(RHT_CHARACTERISTIC_UUID);
        peripheral.readCharacteristic(mHumidityTemperatureCharacteristic);
        bluetoothGattService.addCharacteristic(mHumidityTemperatureCharacteristic);
    }

    @NonNull
    private static RHTDataPoint convertToHumanReadableValues(@NonNull final byte[] rawData) {
        final short[] humidityAndTemperature = new short[2];

        ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

        final float temperature = ((float) humidityAndTemperature[0]) / 100f;
        final float humidity = ((float) humidityAndTemperature[1]) / 100f;
        final long timestamp = System.currentTimeMillis();

        return new RHTDataPoint(temperature, humidity, timestamp);
    }

    /**
     * This method checks if this service is able to handle the characteristic.
     * In case it's able to manage the characteristic it reads it and advice to this service listeners.
     *
     * @param updatedCharacteristic characteristic with new values coming from Peripheral.
     * @return <code>true</code> in case it managed correctly the new data - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        if (mHumidityTemperatureCharacteristic.getUuid().equals(updatedCharacteristic.getUuid())) {
            final byte[] rawData = updatedCharacteristic.getValue();
            mLastDatapoint = convertToHumanReadableValues(rawData);
            notifyRHTDatapoint(mLastDatapoint, false);
            return true;
        }
        return false;
    }

    /**
     * Enables the characteristic notifications of the service.
     */
    @Override
    public void registerDeviceCharacteristicNotifications() {
        registerNotification(mHumidityTemperatureCharacteristic);
    }

    @Override
    public boolean isServiceReady() {
        if (getLastDatapoint() == null) {
            registerDeviceCharacteristicNotifications();
        }
        return getLastDatapoint() != null;
    }

    /**
     * Obtains the sensor name of the service.
     *
     * @return {@link java.lang.String} with the sensor name - <code>null</code> if the sensor name is not known.
     */
    @Override
    public String getSensorName() {
        switch (mPeripheral.getAdvertisedName()) {
            case "SHTC1 smart gadget":
                return "SHTC1";
            case "SHT31 Smart Gadget":
                return "SHT31";
            default:
                return null;
        }
    }

    /**
     * Obtains the last obtained datapoint.
     *
     * @return {@link com.sensirion.libble.utils.RHTDataPoint} with the last RHT data obtained from the sensor.
     */
    @Nullable
    public RHTDataPoint getLastDatapoint() {
        return mLastDatapoint;
    }
}