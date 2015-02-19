package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.listeners.services.RHTListener;

abstract class SmartgadgetRHTService<ListenerType extends NotificationListener> extends SmartgadgetService<ListenerType> {

    protected SmartgadgetRHTService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService gatt, @NonNull final String valueCharacteristicUUID) {
        super(peripheral, gatt, valueCharacteristicUUID);
        registerNotificationListener(SmartgadgetRHTNotificationCenter.getInstance());
    }

    /**
     * This method should be called only from peripheral.
     * This method adds a listener to the list in case it's a TypeOfListener.
     *
     * @param listener the new candidate object for being a listener of this class.
     * @return <code>true</code> in case it's a valid listener, <code>false</code> otherwise.
     */
    @Override
    public boolean registerNotificationListener(@NonNull final NotificationListener listener) {
        boolean rhtListener = false;
        if (listener instanceof RHTListener) {
            SmartgadgetRHTNotificationCenter.getInstance().registerDownloadListener((RHTListener) listener, mPeripheral);
            rhtListener = true;
        }
        final boolean temperatureOrHumidityListener = super.registerNotificationListener(listener);
        return rhtListener || temperatureOrHumidityListener;
    }

    /**
     * This method should be called only from peripheral.
     * This method unregister a listener from the list in case it's registered.
     *
     * @param listener the listener that doesn't want to hear from a device anymore.
     */
    @Override
    public boolean unregisterNotificationListener(@NonNull final NotificationListener listener) {
        if (listener instanceof RHTListener) {
            SmartgadgetRHTNotificationCenter.getInstance().unregisterDownloadListener((RHTListener) listener, mPeripheral);
        }
        return super.unregisterNotificationListener(listener);
    }
}