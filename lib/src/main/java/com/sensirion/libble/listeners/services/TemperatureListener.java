package com.sensirion.libble.listeners.services;

import android.support.annotation.NonNull;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.utils.TemperatureUnit;

/**
 * This interface needs to be implemented by classes that want to listen for temperature notifications.
 * NOTE: This class functionality is wrapped by: {@link RHTListener}.
 */
public interface TemperatureListener extends NotificationListener {

    /**
     * Advises the listeners that a new temperature value was obtained.
     *
     * @param device      {@link com.sensirion.libble.devices.BleDevice} that reported the temperature data.
     * @param temperature {@link java.lang.Float} with the temperature value.
     * @param sensorName  {@link java.lang.String} with the name of the sensor that reported the temperature data.
     */
    void onNewTemperature(@NonNull BleDevice device, float temperature, @NonNull String sensorName, @NonNull TemperatureUnit unit);

    /**
     * Sends to the user the latest historical temperature.
     *
     * @param device      that reported the humidity historical value.
     * @param temperature from a moment in the past.
     * @param timestamp   in milliseconds that determines when the temperature was obtained.
     * @param sensorName  of the sensor that reported the humidity.
     */
    void onNewHistoricalTemperature(@NonNull BleDevice device, float temperature, long timestamp, @NonNull String sensorName, @NonNull TemperatureUnit unit);
}