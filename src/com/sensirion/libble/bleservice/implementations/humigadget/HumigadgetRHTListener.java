package com.sensirion.libble.bleservice.implementations.humigadget;

import com.sensirion.libble.NotificationListener;

public interface HumigadgetRHTListener extends NotificationListener {

    /**
     * Advices the listeners that the reading of a new datapoint was produced.
     *
     * @param dataPoint {@link com.sensirion.libble.bleservice.implementations.humigadget.RHTDataPoint} with the RHT_DATA.
     */
    public void onNewRHTValues(final RHTDataPoint dataPoint);
}