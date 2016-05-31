package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.listeners.services.RHTListener;
import com.sensirion.libble.services.BleServiceSynchronizationPriority;

abstract class AbstractSmartgadgetRHTService<ListenerType extends NotificationListener> extends AbstractSmartgadgetService<ListenerType> {

    protected AbstractSmartgadgetRHTService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService gatt, @NonNull final String valueCharacteristicUUID) {
        super(peripheral, gatt, valueCharacteristicUUID);
        registerNotificationListener(SmartgadgetRHTNotificationCenter.getInstance());
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public boolean unregisterNotificationListener(@NonNull final NotificationListener listener) {
        if (listener instanceof RHTListener) {
            SmartgadgetRHTNotificationCenter.getInstance().unregisterDownloadListener((RHTListener) listener, mPeripheral);
        }
        return super.unregisterNotificationListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public BleServiceSynchronizationPriority getServiceSynchronizationPriority(){
        return BleServiceSynchronizationPriority.LOW_PRIORITY;
    }
}