package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.listeners.services.HumidityListener;
import com.sensirion.libble.listeners.services.RHTListener;
import com.sensirion.libble.listeners.services.TemperatureListener;
import com.sensirion.libble.utils.HumidityUnit;
import com.sensirion.libble.utils.RHTDataPoint;
import com.sensirion.libble.utils.TemperatureUnit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractRHTService extends AbstractBleService<RHTListener> {

    private static final Set<RHTListener> mRHTListeners = Collections.synchronizedSet(new HashSet<RHTListener>());
    private static final Set<TemperatureListener> mTemperatureListeners = Collections.synchronizedSet(new HashSet<TemperatureListener>());
    private static final Set<HumidityListener> mHumidityListeners = Collections.synchronizedSet(new HashSet<HumidityListener>());

    public AbstractRHTService(@NonNull final Peripheral servicePeripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        super(servicePeripheral, bluetoothGattService);
    }

    /**
     * Notifies the client of a (present or past, depending on the @param timestamp) temperature change.
     *
     * @param temperature that the device retrieved.
     * @param timestamp   when the temperature was obtained. <code>null</code> if it's live data.
     */
    protected void notifyTemperature(final float temperature, final Long timestamp, final TemperatureUnit unit) {
        final Iterator<TemperatureListener> iterator = mTemperatureListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final TemperatureListener listener = iterator.next();
                if (timestamp == null) {
                    listener.onNewTemperature(mPeripheral, temperature, getSensorName(), unit);
                } else {
                    listener.onNewHistoricalTemperature(mPeripheral, temperature, timestamp, getSensorName(), unit);
                }
            } catch (final Exception e) {
                Log.e(TAG, "notifyTemperature -> The following error was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies the client of a (present or past, depending on the @param timestamp) humidity change.
     *
     * @param humidity  that the device retrieved.
     * @param timestamp when the temperature was obtained. <code>null</code> if it's live data.
     */
    protected void notifyHumidity(final float humidity, final Long timestamp, final HumidityUnit unit) {
        final Iterator<HumidityListener> iterator = mHumidityListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final HumidityListener listener = iterator.next();
                if (timestamp == null) {
                    listener.onNewHumidity(mPeripheral, humidity, getSensorName(), unit);
                } else {
                    listener.onNewHistoricalHumidity(mPeripheral, humidity, timestamp, getSensorName(), unit);
                }
            } catch (final Exception e) {
                Log.e(TAG, "notifyHumidity -> The following error was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies the user of a (present or past, depending on @param timestamp) RHT data point.
     *
     * @param datapoint     that the device retrieved.
     * @param isFromHistory <code>true</code> if the data came from the historical values of the device - <code>false</code> otherwise.
     */
    protected void notifyRHTDatapoint(@NonNull final RHTDataPoint datapoint, final boolean isFromHistory) {
        final Long timestamp = (isFromHistory) ? datapoint.getTimestamp() : null;
        notifyTemperature(datapoint.getTemperatureCelsius(), timestamp, TemperatureUnit.CELSIUS);
        notifyHumidity(datapoint.getRelativeHumidity(), timestamp, HumidityUnit.RELATIVE_HUMIDITY);
        final Iterator<RHTListener> iterator = mRHTListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final RHTListener listener = iterator.next();
                if (isFromHistory) {
                    listener.onNewHistoricalRHTValue(mPeripheral, datapoint, getSensorName());
                } else {
                    listener.onNewRHTValue(mPeripheral, datapoint, getSensorName());
                }
            } catch (final Exception e) {
                Log.e(TAG, "notifyRHTDatapoint -> The following error was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerNotificationListener(@NonNull final NotificationListener newListener) {
        boolean listenerFound = false;
        if (newListener instanceof RHTListener) {
            mRHTListeners.add((RHTListener) newListener);
            Log.i(TAG, String.format("registerNotificationListener -> Peripheral %s received a new RHT listener: %s ", getDeviceAddress(), newListener));
            listenerFound = true;
        }
        if (newListener instanceof TemperatureListener) {
            mTemperatureListeners.add((TemperatureListener) newListener);
            Log.i(TAG, String.format("registerNotificationListener -> Peripheral %s received a new Temperature listener: %s ", getDeviceAddress(), newListener));
            listenerFound = true;
        }
        if (newListener instanceof HumidityListener) {
            mHumidityListeners.add((HumidityListener) newListener);
            Log.i(TAG, String.format("registerNotificationListener -> Peripheral %s received a new Humidity listener: %s ", getDeviceAddress(), newListener));
            listenerFound = true;
        }
        return listenerFound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unregisterNotificationListener(@NonNull final NotificationListener listenerForRemoval) {
        boolean listenerFound = false;
        if (listenerForRemoval instanceof RHTListener) {
            mRHTListeners.remove(listenerForRemoval);
            Log.i(TAG, String.format("unregisterNotificationListener -> Peripheral %s deleted %s listener from the RHTListener list.", getDeviceAddress(), listenerForRemoval));
            listenerFound = true;
        }
        if (listenerForRemoval instanceof TemperatureListener) {
            mTemperatureListeners.remove(listenerForRemoval);
            Log.i(TAG, String.format("unregisterNotificationListener -> Peripheral %s deleted %s listener from the Temperature list.", getDeviceAddress(), listenerForRemoval));
            listenerFound = true;
        }
        if (listenerForRemoval instanceof HumidityListener) {
            mHumidityListeners.remove(listenerForRemoval);
            Log.i(TAG, String.format("unregisterNotificationListener -> Peripheral %s deleted %s listener from the Humidity list.", getDeviceAddress(), listenerForRemoval));
            listenerFound = true;
        }
        return listenerFound;
    }

    /**
     * Obtains the sensor name of the device.
     *
     * @return {@link java.lang.String} with the sensor name.
     */
    public abstract String getSensorName();
}