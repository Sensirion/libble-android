package com.sensirion.libble.services.sensirion.shtc1;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.services.RHTListener;
import com.sensirion.libble.services.NotificationService;
import com.sensirion.libble.utils.RHTDataPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.UUID;

public class SHTC1RHTService extends NotificationService<RHTDataPoint, RHTListener> {

    public static final String SERVICE_UUID = "0000aa20-0000-1000-8000-00805f9b34fb";
    public static final String RHT_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String TAG = SHTC1RHTService.class.getSimpleName();

    private static final String SENSOR_NAME = "SHTC1";

    private static final String RHT_CHARACTERISTIC_UUID = "0000aa21-0000-1000-8000-00805f9b34fb";

    private static final short TIMEOUT_MS = 1100;

    private static final byte MAX_NUMBER_REQUESTS = 4;

    private final BluetoothGattCharacteristic mHumidityTemperatureCharacteristic;

    private RHTDataPoint mLastDatapoint;

    public SHTC1RHTService(final Peripheral peripheral, final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService);
        mHumidityTemperatureCharacteristic = getCharacteristicFor(RHT_CHARACTERISTIC_UUID);
        peripheral.readCharacteristic(mHumidityTemperatureCharacteristic);
        bluetoothGattService.addCharacteristic(mHumidityTemperatureCharacteristic);
    }

    /**
     * This method checks if this service is able to handle the characteristic.
     * In case it's able to manage the characteristic it reads it and advice to this service listeners.
     *
     * @param updatedCharacteristic characteristic with new values coming from Peripheral.
     * @return <code>true</code> in case it managed correctly the new data - <code>false</code> otherwise.
     */
    @Override
    public boolean onChangeNotification(final BluetoothGattCharacteristic updatedCharacteristic) {
        super.onCharacteristicRead(updatedCharacteristic);
        if (mHumidityTemperatureCharacteristic.getUuid().equals(updatedCharacteristic.getUuid())) {
            final byte[] rawData = updatedCharacteristic.getValue();
            mLastDatapoint = convertToHumanReadableValues(rawData);
            notifyListeners();
            return true;
        }
        return false;
    }

    private static RHTDataPoint convertToHumanReadableValues(final byte[] rawData) {
        final short[] humidityAndTemperature = new short[2];

        ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

        final float temperature = ((float) humidityAndTemperature[0]) / 100f;
        final float humidity = ((float) humidityAndTemperature[1]) / 100f;
        final long timestamp = System.currentTimeMillis();

        return new RHTDataPoint(temperature, humidity, timestamp);
    }

    private void notifyListeners() {
        final Iterator<RHTListener> iterator = super.mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewRHTValues(mPeripheral, mLastDatapoint, SENSOR_NAME);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListeners -> The following exception was produced: ", e);
                iterator.remove();
            }
        }
    }

    /**
     * This method will normally only be called from {@link com.sensirion.libble.services.NotificationService}
     *
     * @param characteristic of notifications.
     * @param enabled        <code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
    @Override
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        mPeripheral.setCharacteristicNotification(characteristic, enabled);
        if (UUID.fromString(RHT_CHARACTERISTIC_UUID).equals(characteristic.getUuid())) {
            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(RHT_DESCRIPTOR_UUID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mPeripheral.writeDescriptor(descriptor);
        } else {
            Log.d(TAG, String.format("setCharacteristicNotification -> Characteristic with UUID %s was found.", characteristic.getUuid()));
        }
    }

    /**
     * Enables the characteristic notifications of the service.
     */
    @Override
    public BluetoothGattCharacteristic readNotificationCharacteristic() {
        return mHumidityTemperatureCharacteristic;
    }

    /**
     * Return the last HUMIGADGET_RHT_NOTIFICATION_SERVICE data in case it's known.
     *
     * @param characteristicName name of the characteristic.
     * @return an {@link com.sensirion.libble.utils.RHTDataPoint} with the latest received characteristic - <code>null</code> otherwise.
     */
    @Override
    public RHTDataPoint getCharacteristicValue(final String characteristicName) {
        if (characteristicName.equals(RHTListener.READ_LATEST_RHT_DATA_POINT)) {
            if (mLastDatapoint == null) {
                mPeripheral.forceReadCharacteristic(mHumidityTemperatureCharacteristic, TIMEOUT_MS, MAX_NUMBER_REQUESTS);
                if (mLastDatapoint == null) {
                    return null;
                }
                mPeripheral.cleanCharacteristicCache();
                return mLastDatapoint;
            } else {
                mPeripheral.readCharacteristic(mHumidityTemperatureCharacteristic);
                return mLastDatapoint;
            }
        }
        return null;
    }
}