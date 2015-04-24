package com.sensirion.libble.listeners.services;

import android.support.annotation.NonNull;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.utils.TemperatureUnit;

/**
 * This interface needs to be implemented by classes
 * that want to listen for temperature notifications.
 * <p/>
 * NOTE: This class functionality is wrapped by: {@link RHTListener}.
 */
public interface TemperatureListener extends NotificationListener {

    /**
     * Advises the listeners that a new temperature value was obtained.
     *
     * @param device      {@link BleDevice} that reported the temperature data.
     * @param temperature {@link Float} with the temperature value.
     * @param sensorName  {@link String} of the sensor that reported the temperature data.
     * @param unit        {@link TemperatureUnit} of the reported temperature.
     */
    void onNewTemperature(@NonNull BleDevice device, float temperature,
                          @NonNull String sensorName, @NonNull TemperatureUnit unit);

    /**
     * Sends to the user the latest historical temperature.
     *
     * @param device          {@link BleDevice} that reported the humidity historical value.
     * @param temperature     {@link float} from a moment in the past.
     * @param timestampMillis {@link long} that determines when the temperature was obtained.
     * @param sensorName      {@link String} of the sensor that reported the humidity.
     * @param unit            {@link TemperatureUnit} of the reported temperature.
     */
    void onNewHistoricalTemperature(@NonNull BleDevice device, float temperature,
                                    long timestampMillis, @NonNull String sensorName,
                                    @NonNull TemperatureUnit unit);
}