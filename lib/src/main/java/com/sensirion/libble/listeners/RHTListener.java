package com.sensirion.libble.listeners;

import com.sensirion.libble.peripherals.BleDevice;
import com.sensirion.libble.services.NotificationListener;
import com.sensirion.libble.utils.RHTDataPoint;

/**
 * This interface needs to be implemented by classes that wants to listen for
 */
public interface RHTListener extends NotificationListener {

    public static String READ_LATEST_RHT_DATA_POINT = RHTListener.class.getName() + ".getRHTData";

    /**
     * Advices the listeners that the reading of a new datapoint was obtained.
     *
     * @param device     {@link com.sensirion.libble.peripherals.BleDevice} that send the RHT_DATA.
     * @param dataPoint  {@link com.sensirion.libble.utils.RHTDataPoint} with the RHT_DATA.
     * @param sensorName {@link java.lang.String} with the name of the sensor that send the RHT_DATA
     */
    public void onNewRHTValues(BleDevice device, RHTDataPoint dataPoint, String sensorName);
}