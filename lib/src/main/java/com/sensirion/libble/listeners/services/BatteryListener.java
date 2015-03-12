package com.sensirion.libble.listeners.services;

import android.support.annotation.NonNull;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;

public interface BatteryListener extends NotificationListener {

    /**
     * Advises the listeners that a new battery value was obtained.
     *
     * @param device                 {@link com.sensirion.libble.devices.BleDevice} that send the humidity data.
     * @param batteryLevelPercentage {@link java.lang.Integer} with the battery level.
     */
    void onNewBatteryLevel(@NonNull BleDevice device, int batteryLevelPercentage);

}
