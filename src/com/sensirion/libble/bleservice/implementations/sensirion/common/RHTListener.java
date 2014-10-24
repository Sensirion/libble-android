package com.sensirion.libble.bleservice.implementations.sensirion.common;

import com.sensirion.libble.NotificationListener;

public interface RHTListener extends NotificationListener {

    /**
     * Advices the listeners that the reading of a new datapoint was obtained.
     *
     * @param dataPoint {@link RHTDataPoint} with the RHT_DATA.
     */
    public void onNewRHTValues(final RHTDataPoint dataPoint);
}