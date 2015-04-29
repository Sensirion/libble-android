package com.sensirion.libble.listeners.services;

import android.support.annotation.NonNull;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.utils.RHTDataPoint;

/**
 * This interface needs to be implemented by classes that want to listen for RHT notifications.
 */
public interface RHTListener extends NotificationListener {

    /**
     * Advises the listeners that the reading of a new data point was obtained.
     *
     * @param device     {@link BleDevice} that reported the RHT data.
     * @param dataPoint  {@link RHTDataPoint} downloaded.
     * @param sensorName {@link String} with the name of the sensor that reported the RHT_DATA
     */
    void onNewRHTValue(@NonNull BleDevice device,
                       @NonNull RHTDataPoint dataPoint, @NonNull String sensorName);

    /**
     * Sends to the user a data point that was extracted from the historical data of the device.
     *
     * @param device     {@link BleDevice} that reported the historical RHT data.
     * @param dataPoint  {@link RHTDataPoint} downloaded.
     * @param sensorName {@link String} that reported the RHT data.
     */
    void onNewHistoricalRHTValue(@NonNull BleDevice device,
                                 @NonNull RHTDataPoint dataPoint, @NonNull String sensorName);
}