package com.sensirion.libble.listeners.services;

import android.support.annotation.NonNull;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;

/**
 * This interface needs to be implemented by classes
 * listening for battery level notifications.
 */
public interface BatteryListener extends NotificationListener {

    /**
     * Advises the listeners that a new battery value was obtained.
     *
     * @param device                 {@link BleDevice} that sends the battery data.
     * @param batteryLevelPercentage {@link int} with the battery level percentage.
     */
    void onNewBatteryLevel(@NonNull BleDevice device, int batteryLevelPercentage);
}