package com.sensirion.libble.listeners.services;

import android.support.annotation.NonNull;

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
     * @param device     {@link BleDevice} that sends the humidity data.
     * @param humidity   {@link float} with the humidity value.
     * @param sensorName {@link String} of the sensor that reported the humidity data.
     * @param unit       {@link HumidityUnit} of the reported humidity.
     */
    void onNewHumidity(@NonNull BleDevice device, float humidity,
                       @NonNull String sensorName, @NonNull HumidityUnit unit);

    /**
     * Sends to the user the latest historical humidity.
     *
     * @param device                {@link BleDevice} that sends the humidity historical value.
     * @param relativeHumidity      {@link float} from a moment in the past.
     * @param timestampMilliseconds {@link long} that determines when the humidity was obtained.
     * @param sensorName            {@link String} of the sensor that reported the humidity.
     * @param unit                  {@link HumidityUnit} of the reported humidity.
     */
    void onNewHistoricalHumidity(@NonNull BleDevice device, float relativeHumidity,
                                 long timestampMilliseconds, @NonNull String sensorName,
                                 @NonNull HumidityUnit unit);
}