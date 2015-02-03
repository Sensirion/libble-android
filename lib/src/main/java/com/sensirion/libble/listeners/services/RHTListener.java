package com.sensirion.libble.listeners.services;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.utils.RHTDataPoint;

/**
 * This interface needs to be implemented by classes that wants to listen for
 */
public interface RHTListener extends NotificationListener {

    public static String READ_LATEST_RHT_DATA_POINT = String.format("%s.getRHTData", RHTListener.class.getName());

    /**
     * Advices the listeners that the reading of a new datapoint was obtained.
     *
     * @param device     {@link com.sensirion.libble.devices.BleDevice} that send the RHT_DATA.
     * @param dataPoint  {@link com.sensirion.libble.utils.RHTDataPoint} with the RHT_DATA.
     * @param sensorName {@link java.lang.String} with the name of the sensor that send the RHT_DATA
     */
    void onNewRHTValues(BleDevice device, RHTDataPoint dataPoint, String sensorName);

    /**
     * Sends to the user a datapoint that was extracted from the historical data of the device.
     *
     * @param dataPoint  downloaded.
     * @param sensorName of the historical data.
     */
    void onNewHistoricalRHTValues(BleDevice device, RHTDataPoint dataPoint, String sensorName);
}