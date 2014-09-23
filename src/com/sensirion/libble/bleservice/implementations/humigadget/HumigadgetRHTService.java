package com.sensirion.libble.bleservice.implementations.humigadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.Peripheral;
import com.sensirion.libble.bleservice.NotificationService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.UUID;

public class HumigadgetRHTService extends NotificationService<RHTDataPoint, HumigadgetRHTListener> {

    public static final String SERVICE_UUID = "0000aa20-0000-1000-8000-00805f9b34fb";

    public static final String RHT_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String PREFIX = HumigadgetRHTService.class.getName();
    public static final String RHT_CHARACTERISTIC_READ_NAME = PREFIX + ".getRHTData";
    private static final String RHT_CHARACTERISTIC_UUID = "0000aa21-0000-1000-8000-00805f9b34fb";

    private static final String TAG = HumigadgetRHTService.class.getSimpleName();

    private static final int TIMEOUT_MS = 1100;
    private static final int MAX_NUMBER_REQUESTS = 4;

    private final BluetoothGattCharacteristic mHumidityTemperatureCharacteristic;

    private RHTDataPoint mLastDatapoint;

    public HumigadgetRHTService(final Peripheral peripheral, final BluetoothGattService bluetoothGattService) {
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
    public boolean onChangeNotification(BluetoothGattCharacteristic updatedCharacteristic) {
        super.onCharacteristicRead(updatedCharacteristic);
        if (mHumidityTemperatureCharacteristic.getUuid().equals(updatedCharacteristic.getUuid())) {
            byte[] rawData = updatedCharacteristic.getValue();
            convertToHumanReadableValues(rawData);
            notifyListeners();
            return true;
        }
        return false;
    }

    private void convertToHumanReadableValues(byte[] rawData) {
        final short[] humidityAndTemperature = new short[rawData.length / 2];

        ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

        final float ambientHumidity = ((float) humidityAndTemperature[0]) / 100.0f;
        final float temperature = ((float) humidityAndTemperature[1]) / 100.0f;
        final long timestamp = System.currentTimeMillis();

        mLastDatapoint = new RHTDataPoint(mPeripheral, ambientHumidity, temperature, timestamp, false);
    }

    private void notifyListeners() {
        final Iterator<HumigadgetRHTListener> iterator = super.mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewRHTValues(mLastDatapoint);
            } catch (Exception e) {
                iterator.remove();
            }
        }
    }

    /**
     * This method will normally only be called from {@link com.sensirion.libble.bleservice.NotificationService}
     *
     * @param characteristic of notifications.
     * @param enabled        <code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
    @Override
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        mPeripheral.setCharacteristicNotification(characteristic, enabled);
        if (UUID.fromString(RHT_CHARACTERISTIC_UUID).equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(RHT_DESCRIPTOR_UUID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mPeripheral.writeDescriptor(descriptor);
        } else {
            Log.w(TAG, "found unhandled characteristic with UUID: " + characteristic.getUuid());
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
     * @return an {@link com.sensirion.libble.bleservice.implementations.humigadget.RHTDataPoint} with the latest received characteristic - <code>null</code> otherwise.
     */
    @Override
    public RHTDataPoint getCharacteristicValue(final String characteristicName) {
        if (characteristicName.equals(RHT_CHARACTERISTIC_READ_NAME)) {
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