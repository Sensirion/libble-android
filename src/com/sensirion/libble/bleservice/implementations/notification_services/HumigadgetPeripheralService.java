package com.sensirion.libble.bleservice.implementations.notification_services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.NotificationListener;
import com.sensirion.libble.Peripheral;
import com.sensirion.libble.bleservice.NotificationService;
import com.sensirion.libble.listeners.HumigadgetListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class HumigadgetPeripheralService extends NotificationService {

    public static final String RHT_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String RHT_SERVICE_UUID = "0000aa20-0000-1000-8000-00805f9b34fb";
    private static final String TAG = HumigadgetPeripheralService.class.getSimpleName();
    private static final String RHT_CHARACTERISTIC_UUID = "0000aa21-0000-1000-8000-00805f9b34fb";

    private float mLastHumidity;
    private float mLastTemperature;

    private final BluetoothGattCharacteristic mHumidityTemperatureCharacteristic;

    private HashSet<HumigadgetListener> mListeners = new HashSet<HumigadgetListener>();

    public HumigadgetPeripheralService(Peripheral parent, BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mHumidityTemperatureCharacteristic = getCharacteristicFor(RHT_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mHumidityTemperatureCharacteristic);
        bluetoothGattService.addCharacteristic(mHumidityTemperatureCharacteristic);
        registerNotification(mHumidityTemperatureCharacteristic);
    }

    @Override
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        mParent.setCharacteristicNotification(characteristic, enabled);
        if (UUID.fromString(RHT_CHARACTERISTIC_UUID).equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(RHT_DESCRIPTOR_UUID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mParent.writeDescriptor(descriptor);
        } else {
            Log.w(TAG, "found unhandled characteristic with UUID: " + characteristic.getUuid());
        }
    }

    /**
     * This method checks if this service is able to handle the characteristic.
     * In case it's able to manage the characteristic it reads it and advice to this service listeners.
     *
     * @param updatedCharacteristic characteristic with new values coming from Peripheral.
     * @return true in case it managed correctly the new data - false otherwise.
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

        mLastHumidity = ((float) humidityAndTemperature[0]) / 100.0f;
        mLastTemperature = ((float) humidityAndTemperature[1]) / 100.0f;
    }

    private void notifyListeners() {
        List<HumigadgetListener> listenersForRemove = new LinkedList<HumigadgetListener>();
        for (HumigadgetListener listener : mListeners) {
            try {
                listener.onNewRHTValues(mLastHumidity, mLastTemperature, mParent);
            } catch (Exception e){
                listenersForRemove.add(listener);
            }
        }
        mListeners.removeAll(listenersForRemove);
    }

    /**
     * This method should be called only from peripheral.
     * <p/>
     * This method adds a listener to the list in case it's a HumigadgetListener.
     *
     * @param newListener the new candidate object for being a listener of this class.
     * @return true in case it's a valid listener, false otherwise.
     */
    @Override
    public boolean registerNotificationListener(NotificationListener newListener) {
        if (newListener instanceof HumigadgetListener) {
            return mListeners.add((HumigadgetListener) newListener);
        }
        return false;
    }


    /**
     * This method should be called only from peripheral.
     *
     * This method unregister a listener from the list in case of having it.
     *
     * @param listenerForRemove the listener that doesn't want to hear from a device anymore.
     */
    @Override
    public void unregisterNotificationListener(NotificationListener listenerForRemove) {
        if (mListeners.contains(listenerForRemove)){
            mListeners.remove(listenerForRemove);
        }
    }
}