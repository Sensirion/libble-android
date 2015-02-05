package com.sensirion.libble.listeners.services;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.utils.HumidityUnit;

/**
 * This interface needs to be implemented by classes that want to listen for humidity notifications.
 * NOTE: This class functionality is wrapped by: {@link RHTListener}.
 */
public interface HumidityListener extends NotificationListener {

    /**
     * Advises the listeners that a new humidity value was obtained.
     *
     * @param device     {@link com.sensirion.libble.devices.BleDevice} that sends the humidity data.
     * @param humidity   {@link java.lang.Float} with the humidity value.
     * @param sensorName {@link java.lang.String} with the name of the sensor that reported the humidity data.
     */
    void onNewHumidity(BleDevice device, float humidity, String sensorName, HumidityUnit unit);

    /**
     * Sends to the user the latest historical humidity.
     *
     * @param device           that sends the humidity historical value.
     * @param relativeHumidity from a moment in the past.
     * @param timestamp        in milliseconds that determines when the humidity was obtained.
     * @param sensorName       of the sensor that reported the humidity.
     */
    void onNewHistoricalHumidity(BleDevice device, float relativeHumidity, long timestamp, String sensorName, HumidityUnit unit);
}