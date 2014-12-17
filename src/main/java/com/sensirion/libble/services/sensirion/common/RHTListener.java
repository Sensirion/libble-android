package com.sensirion.libble.services.sensirion.common;

import com.sensirion.libble.peripherals.BleDevice;
import com.sensirion.libble.services.NotificationListener;

public interface RHTListener extends NotificationListener {

    /**
     * Advices the listeners that the reading of a new datapoint was obtained.
     *
     * @param dataPoint {@link RHTDataPoint} with the RHT_DATA.
     */
    public void onNewRHTValues(final BleDevice device, final RHTDataPoint dataPoint);
}