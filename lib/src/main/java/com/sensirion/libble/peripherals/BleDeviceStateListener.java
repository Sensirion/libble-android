package com.sensirion.libble.peripherals;

/**
 * This listener tells the user when a device state changes.
 */
public interface BleDeviceStateListener {
    /**
     * NOTE: The services and characteristics of this device are not connected yet.
     * NOTE: The connected device is removed from the library internal discovered list.
     * This method is called when a device is connected.
     *
     * @param device that was connected.
     */
    void onDeviceConnected(BleDevice device);

    /**
     * This method is called when a device becomes disconnected.
     *
     * @param device that was disconnected.
     */
    void onDeviceDisconnected(BleDevice device);

    /**
     * This method is called when the library discovers a new device.
     *
     * @param device that was discovered.
     */
    void onDeviceDiscovered(BleDevice device);
}