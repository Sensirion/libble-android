package com.sensirion.libble.listeners;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.NotificationListener;

public interface HumigadgetListener extends NotificationListener {
    public void onNewRHTValues(final float humidity, final float temperature, final BleDevice device);
}